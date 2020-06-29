package org.esa.snap.core.dataio.dimap;

import org.esa.snap.core.datamodel.ProductData;

import java.awt.Rectangle;
import java.awt.geom.Area;

class CacheBlock {

    private final int yOffset;
    private final int width;
    private final int height;
    private final Area unwrittenSpace;

    private ProductData bufferData;

    CacheBlock(int yOffset, int width, int height, int dataType) {
        this.yOffset = yOffset;
        this.width = width;
        this.height = height;

        bufferData = ProductData.createInstance(dataType, width * height);

        unwrittenSpace = new Area(getRegion());
    }

    int getYOffset() {
        return yOffset;
    }

    Rectangle getRegion() {
        return new Rectangle(0, yOffset, width, height);
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

    public void update(int xOffset, int yOffset, int width, int height, ProductData data) {
        final Object srcData = data.getElems();
        final Object destData = bufferData.getElems();
        final int targetLineOffset = yOffset - this.yOffset;

        for (int line = 0; line < height; line++) {
            final int srcPos = line * width;
            final int destPos = xOffset + (targetLineOffset + line) * this.width;
            System.arraycopy(srcData, srcPos, destData, destPos, width);
        }

        this.unwrittenSpace.subtract(new Area(new Rectangle(xOffset, yOffset, width, height)));
    }
}
