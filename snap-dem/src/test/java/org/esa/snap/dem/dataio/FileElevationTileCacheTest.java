/*
 * Copyright (C) 2026 by SkyWatch Space Applications Inc. http://www.skywatch.com
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.dem.dataio;

import com.bc.ceres.annotation.STTM;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.CachingObjectArray;
import org.esa.snap.core.util.ObjectArray;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The external-DEM line cache must stay bounded: before the fix, eviction cleared the wrong slot
 * (the list position instead of the line number) through a setter that only fills empty slots, so
 * every line read stayed on the heap.
 */
public class FileElevationTileCacheTest {

    private static final int WIDTH = 16;
    private static final int HEIGHT = 3 * FileElevationTile.getMaxCachedLines() + 7;

    @Test
    @STTM("SNAP-4391")
    public void lineCacheIsBoundedAndEvictedLinesAreReloadedCorrectly() throws Exception {
        final Product product = createProduct();
        final FileElevationTile tile = new FileElevationTile(product, -9999.0);
        tile.applyEarthGravitionalModel(false);
        try {
            for (int y = 0; y < HEIGHT; y++) {
                assertEquals(value(1, y), tile.getSample(1, y), 0f);
            }
            final int cached = countCachedLines(tile);
            assertTrue("cached lines " + cached + " must be <= " + FileElevationTile.getMaxCachedLines(),
                       cached <= FileElevationTile.getMaxCachedLines());
            assertTrue("recently read lines stay cached", cached > 0);

            // an evicted line is read again with the right content
            assertEquals(value(5, 0), tile.getSample(5, 0), 0f);
            assertTrue(countCachedLines(tile) <= FileElevationTile.getMaxCachedLines());
        } finally {
            tile.dispose();
        }
    }

    @Test
    @STTM("SNAP-4391")
    public void clearCacheEmptiesTheCache() throws Exception {
        final FileElevationTile tile = new FileElevationTile(createProduct(), -9999.0);
        tile.applyEarthGravitionalModel(false);
        try {
            for (int y = 0; y < 10; y++) {
                tile.getSample(0, y);
            }
            tile.clearCache();
            assertEquals(0, countCachedLines(tile));
            assertEquals(value(3, 4), tile.getSample(3, 4), 0f);
            assertEquals(1, countCachedLines(tile));
        } finally {
            tile.dispose();
        }
    }

    private static float value(int x, int y) {
        return x + 100f * y;
    }

    private static Product createProduct() throws Exception {
        final Product product = new Product("dem", "test", WIDTH, HEIGHT);
        final Band band = product.addBand("elevation", ProductData.TYPE_FLOAT32);
        final float[] data = new float[WIDTH * HEIGHT];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                data[y * WIDTH + x] = value(x, y);
            }
        }
        band.setDataElems(data);
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, WIDTH, HEIGHT, 10.0, 50.0, 0.01, 0.01));
        return product;
    }

    private static int countCachedLines(FileElevationTile tile) throws Exception {
        final CachingObjectArray linesCache = (CachingObjectArray) field(FileElevationTile.class, "linesCache").get(tile);
        final ObjectArray objectArray = (ObjectArray) field(CachingObjectArray.class, "objectArray").get(linesCache);
        int count = 0;
        for (int i = objectArray.getMinIndex(); i <= objectArray.getMaxIndex(); i++) {
            if (objectArray.getObject(i) != null) {
                count++;
            }
        }
        return count;
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        final Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
}
