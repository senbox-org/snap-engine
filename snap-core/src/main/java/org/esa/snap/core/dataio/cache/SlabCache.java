package org.esa.snap.core.dataio.cache;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

class SlabCache {

    private final int rasterWidth;
    private final int rasterHeight;
    private final int tileWidth;
    private final int tileHeight;

    private final List<Slab> cache;

    SlabCache(int rasterWidth, int rasterHeight, int tileWidth, int tileHeight) {
        this.rasterWidth = rasterWidth;
        this.rasterHeight = rasterHeight;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;

        cache = new ArrayList<>();
    }

    Slab[] get(int x, int y, int width, int height) {
        final ArrayList<Slab> resultList = new ArrayList<>(4);  // @todo 2 tb/tb check if this is best starting value


        final Slab[] slabs = new Slab[1];
        final Slab slab = new Slab(new Rectangle(0, 0, tileWidth, tileHeight));
        slab.setLastAccess(System.currentTimeMillis());
        slabs[0] = slab;
        return slabs;
    }
}
