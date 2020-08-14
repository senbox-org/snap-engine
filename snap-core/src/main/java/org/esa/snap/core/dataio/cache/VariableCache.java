package org.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;

import javax.imageio.stream.ImageOutputStream;
import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VariableCache {

    private static final int LINES_PER_BUFFER = 128;
    // package access for testing only in this first version - refactor later tb 2020-06-29
    final CacheBlock[] cacheBlocks;
    private final List<Integer> completedIndices;

    private final int width;
    private final int rasterHeight;
    private final int dataType;
    private final double noDataValue;

    VariableCache(Band variable) {
        final Dimension rasterSize = variable.getRasterSize();
        width = rasterSize.width;
        rasterHeight = rasterSize.height;
        dataType = variable.getDataType();
        noDataValue = variable.getNoDataValue();

        final int numCacheBlocks = (rasterSize.height - 1) / LINES_PER_BUFFER + 1;
        cacheBlocks = new CacheBlock[numCacheBlocks];
        completedIndices = new ArrayList<>();
    }

    public boolean update(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, ProductData sourceBuffer) {
        final int firstIdx = sourceOffsetY / LINES_PER_BUFFER;
        final int lastIdx = (sourceOffsetY + sourceHeight - 1) / LINES_PER_BUFFER;

        boolean canWrite = false;
        int yReadOff = 0;
        for (int index = firstIdx; index <= lastIdx; index++) {
            final int cacheBufferStartY = index * LINES_PER_BUFFER;
            final int writeOffset = Math.max(0, sourceOffsetY - cacheBufferStartY);

            final int remainingHeight = sourceHeight - yReadOff;
            final int heightToNextCacheBlock = LINES_PER_BUFFER - writeOffset;
            final int blockHeight = Math.min(remainingHeight, heightToNextCacheBlock);

            synchronized (cacheBlocks) {
                if (cacheBlocks[index] == null) {
                    final int cacheBufferHeight = (index + 1) * LINES_PER_BUFFER < rasterHeight ? LINES_PER_BUFFER : rasterHeight - cacheBufferStartY;
                    cacheBlocks[index] = new CacheBlock(cacheBufferStartY, width, cacheBufferHeight, dataType, noDataValue);
                }
                cacheBlocks[index].update(sourceOffsetX, yReadOff, cacheBufferStartY + writeOffset, sourceWidth, blockHeight, sourceBuffer);
                final boolean complete = cacheBlocks[index].isComplete();
                if (complete) {
                    completedIndices.add(index);
                }
                canWrite |= complete;
                yReadOff += blockHeight;
            }
        }

        return canWrite;
    }

    public void writeCompletedBlocks(ImageOutputStream outputStream) throws IOException {
        synchronized (cacheBlocks) {
            for (int index : completedIndices) {
                writeCacheBlock(outputStream, index);
            }

            completedIndices.clear();
        }
    }

    public void flush(ImageOutputStream outputStream) throws IOException {
        synchronized (cacheBlocks) {
            for (int i = 0; i < cacheBlocks.length; i++) {
                writeCacheBlock(outputStream, i);
            }
        }
    }

    private void writeCacheBlock(ImageOutputStream outputStream, int index) throws IOException {
        final CacheBlock cacheBlock = cacheBlocks[index];
        if (cacheBlock != null) {
            final ProductData bufferData = cacheBlock.getBufferData();
            final long outputPos = getStreamOutputPos(cacheBlock);
            bufferData.writeTo(0, bufferData.getNumElems(), outputStream, outputPos);

            cacheBlock.dispose();
            cacheBlocks[index] = null;
        }
    }

    // package public for testing purpose only
    static long getStreamOutputPos(CacheBlock cacheBlock) {
        return ((long) cacheBlock.getYOffset()) * cacheBlock.getRegion().width;
    }
}
