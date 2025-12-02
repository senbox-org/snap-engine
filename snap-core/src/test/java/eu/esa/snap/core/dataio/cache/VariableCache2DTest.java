package eu.esa.snap.core.dataio.cache;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class VariableCache2DTest {

    @Test
    public void testInitiateCache() {
        VariableDescriptor variableDescriptor = createDescriptor(100, 500, 60, 110);
        CacheData2D[][] data = VariableCache2D.initiateCache(variableDescriptor);
        assertEquals(2, data[0].length);
        assertEquals(2, data[3].length);
        assertEquals(5, data.length);

        variableDescriptor = createDescriptor(100, 500, 100, 140);
        data = VariableCache2D.initiateCache(variableDescriptor);
        assertEquals(1, data[0].length);
        assertEquals(1, data[2].length);
        assertEquals(4, data.length);
    }

    @Test
    public void testDispose() {
        final VariableDescriptor variableDescriptor = createDescriptor(100, 500, 60, 110);
        final VariableCache2D cache = new VariableCache2D(variableDescriptor);
        CacheData2D[][] cacheData = cache.getCacheData();
        assertEquals(5, cacheData.length);

        cache.dispose();
        assertNull(cache.getCacheData());
    }

    @Test
    public void testGetAffectedTileLocations_nothingCached() {
        final VariableDescriptor variableDescriptor = createDescriptor(100, 500, 60, 110);
        final VariableCache2D cache = new VariableCache2D(variableDescriptor);

        final RowCol[] affectedTileLocations = cache.getAffectedTileLocations(new int[]{0, 0}, new int[]{50, 50});
        assertEquals(0, affectedTileLocations.length);
    }

    @Test
    public void testGetAffectedTileLocations_cacheHit() {
        final VariableDescriptor variableDescriptor = createDescriptor(100, 500, 60, 110);
        final VariableCache2D cache = new VariableCache2D(variableDescriptor);

        cache.getCacheData()[0][0] = new CacheData2D(0, 99, 0, 109);

        final RowCol[] affectedTileLocations = cache.getAffectedTileLocations(new int[]{0, 0}, new int[]{50, 50});
        assertEquals(1, affectedTileLocations.length);
    }

    @Test
    public void testGetAffectedTileLocations_cacheMiss() {
        final VariableDescriptor variableDescriptor = createDescriptor(100, 500, 60, 110);
        final VariableCache2D cache = new VariableCache2D(variableDescriptor);

        cache.getCacheData()[0][0] = new CacheData2D(0, 59, 0, 109);
        cache.getCacheData()[0][1] = new CacheData2D(60, 99, 0, 109);

        final RowCol[] affectedTileLocations = cache.getAffectedTileLocations(new int[]{200, 0}, new int[]{50, 50});
        assertEquals(0, affectedTileLocations.length);
    }

    private static VariableDescriptor createDescriptor(int width, int height, int tileWidth, int tileHeight) {
        VariableDescriptor variableDescriptor = new VariableDescriptor();
        variableDescriptor.width = width;
        variableDescriptor.height = height;
        variableDescriptor.tileWidth = tileWidth;
        variableDescriptor.tileHeight = tileHeight;
        return variableDescriptor;
    }
}
