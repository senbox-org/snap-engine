package org.esa.snap.core.dataio.dimap;

import org.esa.snap.core.datamodel.ProductData;

import java.awt.Rectangle;
import java.awt.geom.Area;

class CacheBlock {

    private final int yOffset;
    private final int width;
    private final int height;
    private final Area unwrittenSpace;

    private ProductData data;

    CacheBlock(int yOffset, int width, int height, int dataType) {
        this.yOffset = yOffset;
        this.width = width;
        this.height = height;

        data = ProductData.createInstance(dataType, width * height);

        unwrittenSpace = new Area(getRegion());
    }

    int getYOffset() {
        return yOffset;
    }

    Rectangle getRegion() {
        return new Rectangle(0, yOffset, width, height);
    }

    ProductData getData() {
        return data;
    }

    public void dispose() {
        data = null;
    }

    public boolean isComplete() {
        return unwrittenSpace.isEmpty();
    }


    // update: int sourceOffsetX, int sourceOffsetY,
    //                                    int sourceWidth, int sourceHeight,
    //                                    ProductData sourceBuffer

}
