package org.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.awt.*;

class Slab {

    // the rectangle with 4 ints and the last access long
    private static final long SELF_SIZE = 24;

    private final Rectangle region;

    private long lastAccess;
    private ProductData productData;

    Slab(Rectangle region) {
        this.region = region;
        this.lastAccess = -1L;
    }

    Rectangle getRegion() {
        return region;
    }

    long getLastAccess() {
        return lastAccess;
    }

    void setLastAccess(long lastAccess) {
        this.lastAccess = lastAccess;
    }

    public ProductData getData() {
        return productData;
    }

    public void setData(ProductData productData) {
        this.productData = productData;
    }

    public long getSizeInBytes() {
        long size = SELF_SIZE;
        if (productData != null) {
            size += productData.getNumElems() * productData.getElemSize();
        }
        return size;
    }

    public void dispose() {
        if (productData != null) {
            productData.dispose();
            productData = null;
        }
    }
}
