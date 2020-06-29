package org.esa.snap.core.dataio.dimap;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;

import java.awt.Dimension;

class VariableCache {

    private static int LINES_PER_BUFFER = 128;
    // package access for testing only in this first version - refactor later tb 2020-06-29
    final CacheBlock[] cacheBlocks;

    private final int width;
    private final int dataType;

    VariableCache(Band variable) {
        final Dimension rasterSize = variable.getRasterSize();
        width = rasterSize.width;
        dataType = variable.getDataType();

        final int numCacheBlocks = (rasterSize.height - 1) / LINES_PER_BUFFER + 1;
        cacheBlocks = new CacheBlock[numCacheBlocks];
    }

    boolean update(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, ProductData sourceBuffer) {
        final int firstIdx = sourceOffsetY / LINES_PER_BUFFER;
        final int lastIdx = (sourceOffsetY + sourceHeight - 1) / LINES_PER_BUFFER;

        boolean canWrite = false;

        for (int index = firstIdx; index <= lastIdx; index++) {
            final int blockCount = index - firstIdx;
            final int coveredHeight = blockCount * LINES_PER_BUFFER;
            final int remainingHeight = sourceHeight - coveredHeight;
            final int blockHeight = remainingHeight > LINES_PER_BUFFER ? LINES_PER_BUFFER : remainingHeight;
            final int bufferYOffset = sourceOffsetY - coveredHeight;
            if (cacheBlocks[index] == null) {
                cacheBlocks[index] = new CacheBlock(bufferYOffset, width, blockHeight, dataType);
            }
            cacheBlocks[index].update(sourceOffsetX, sourceOffsetY, sourceWidth, blockHeight, sourceBuffer);
            canWrite |= cacheBlocks[index].isComplete();
        }

        return canWrite;
    }

    // update xOff, yOff, width, height, data -> return array of affected CacheBlocks

    // remove cacheBlock


}
