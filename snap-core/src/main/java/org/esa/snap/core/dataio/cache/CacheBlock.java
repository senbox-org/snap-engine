package org.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.awt.Rectangle;
import java.awt.geom.Area;

class CacheBlock {

    private final int yStart;
    private final int width;
    private final int height;
    private final Area unwrittenSpace;

    private ProductData bufferData;

    CacheBlock(int yStart, int width, int height, int dataType, double noDataValue) {
        this.yStart = yStart;
        this.width = width;
        this.height = height;

        bufferData = ProductData.createInstance(dataType, width * height);
        for (int i = 0; i < bufferData.getNumElems(); i++) {
            bufferData.setElemDoubleAt(i, noDataValue);
        }

        unwrittenSpace = new Area(getRegion());
    }

    int getYOffset() {
        return yStart;
    }

    Rectangle getRegion() {
        return new Rectangle(0, yStart, width, height);
    }

    ProductData getBufferData() {
        return bufferData;
    }

    public void dispose() {
        bufferData = null;
    }

    public boolean isComplete() {
        return unwrittenSpace.isEmpty();
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    public void update(int xOffset, int yReadOff, int yWriteOff, int width, int height, ProductData data) {
        final Object srcData = data.getElems();
        final Object destData = bufferData.getElems();
        final int writeLineOffset = yWriteOff - this.yStart;

        for (int line = 0; line < height; line++) {
            final int srcPos = (yReadOff + line) * width;
            final int destPos = xOffset + (writeLineOffset + line) * this.width;
            System.arraycopy(srcData, srcPos, destData, destPos, width);
        }

        this.unwrittenSpace.subtract(new Area(new Rectangle(xOffset, yWriteOff, width, height)));
    }
}
