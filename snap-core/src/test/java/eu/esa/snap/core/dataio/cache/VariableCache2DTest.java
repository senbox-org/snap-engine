package eu.esa.snap.core.dataio.cache;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class VariableCache2DTest {

    @Test
    @STTM("SNAP-4107")
    public void testInitiateCache() {
        VariableDescriptor variableDescriptor = createDescriptor(100, 500, 60, 110);
        CacheData2D[][] data = VariableCache2D.initiateCache(variableDescriptor);
        assertEquals(2, data[0].length);
        assertEquals(2, data[3].length);
        assertEquals(5, data.length);

        assertEquals(0, data[0][0].getxMin());
        assertEquals(59, data[0][0].getxMax());
        assertEquals(0, data[0][0].getyMin());
        assertEquals(109, data[0][0].getyMax());

        // second in a tile row is clipped
        assertEquals(60, data[0][1].getxMin());
        assertEquals(99, data[0][1].getxMax());
        assertEquals(0, data[0][0].getyMin());
        assertEquals(109, data[0][0].getyMax());


        variableDescriptor = createDescriptor(100, 500, 100, 140);
        data = VariableCache2D.initiateCache(variableDescriptor);
        assertEquals(1, data[0].length);
        assertEquals(1, data[2].length);
        assertEquals(4, data.length);

        assertEquals(0, data[1][0].getxMin());
        assertEquals(99, data[1][0].getxMax());
        assertEquals(140, data[1][0].getyMin());
        assertEquals(279, data[1][0].getyMax());

        // last row data has less pixel-rows
        assertEquals(0, data[3][0].getxMin());
        assertEquals(99, data[3][0].getxMax());
        assertEquals(420, data[3][0].getyMin());
        assertEquals(499, data[3][0].getyMax());
    }

    @Test
    @STTM("SNAP-4107")
    public void testDispose() {
        final VariableDescriptor variableDescriptor = createDescriptor(100, 500, 60, 110);
        final VariableCache2D cache = new VariableCache2D(variableDescriptor, null);
        CacheData2D[][] cacheData = cache.getCacheData();
        assertEquals(5, cacheData.length);

        cache.dispose();
        assertNull(cache.getCacheData());
    }

    @Test
    @STTM("SNAP-4107")
    public void testGetAffectedCacheLocations_cacheHit() {
        final VariableDescriptor variableDescriptor = createDescriptor(100, 500, 60, 110);
        final VariableCache2D cache = new VariableCache2D(variableDescriptor, null);

        CacheIndex[] affectedTileLocations = cache.getAffectedCacheLocations(new int[]{0, 0}, new int[]{50, 50});
        assertEquals(1, affectedTileLocations.length);
        assertEquals(0, affectedTileLocations[0].getCacheCol());
        assertEquals(0, affectedTileLocations[0].getCacheRow());

        affectedTileLocations = cache.getAffectedCacheLocations(new int[]{410, 30}, new int[]{50, 20});
        assertEquals(2, affectedTileLocations.length);
        assertEquals(0, affectedTileLocations[0].getCacheCol());
        assertEquals(3, affectedTileLocations[0].getCacheRow());
        assertEquals(0, affectedTileLocations[1].getCacheCol());
        assertEquals(4, affectedTileLocations[1].getCacheRow());
    }

    @Test
    @STTM("SNAP-4107")
    public void testGetAffectedCacheLocations_cacheMiss() {
        final VariableDescriptor variableDescriptor = createDescriptor(100, 500, 60, 110);
        final VariableCache2D cache = new VariableCache2D(variableDescriptor, null);

        // right outside
        CacheIndex[] affectedTileLocations = cache.getAffectedCacheLocations(new int[]{0, 200}, new int[]{50, 50});
        assertEquals(0, affectedTileLocations.length);

        // left outside
        affectedTileLocations = cache.getAffectedCacheLocations(new int[]{0, -100}, new int[]{50, 50});
        assertEquals(0, affectedTileLocations.length);

        // bottom outside
        affectedTileLocations = cache.getAffectedCacheLocations(new int[]{600, 0}, new int[]{50, 50});
        assertEquals(0, affectedTileLocations.length);

        // top outside
        affectedTileLocations = cache.getAffectedCacheLocations(new int[]{-200, 0}, new int[]{50, 50});
        assertEquals(0, affectedTileLocations.length);
    }

    @Test
    @STTM("SNAP-4107")
    public void testGetSizeInBytes() throws IOException {
        final VariableDescriptor variableDescriptor = createDescriptor(100, 500, 60, 110);
        final VariableCache2D cache = new VariableCache2D(variableDescriptor, new MockProvider(ProductData.TYPE_FLOAT32));

        assertEquals(1920, cache.getSizeInBytes());

        // read fake data to memory
        DataBuffer dataBuffer = new DataBuffer(ProductData.TYPE_FLOAT32, new int[]{0, 0}, new int[]{100, 50});
        cache.read(new int[]{30, 30}, new int[] {100, 50}, dataBuffer);
        assertEquals(28320, cache.getSizeInBytes());

        dataBuffer = new DataBuffer(ProductData.TYPE_FLOAT32, new int[]{370, 30}, new int[] {100, 50});
        cache.read(new int[]{370, 30}, new int[] {100, 50}, dataBuffer);
        assertEquals(96320, cache.getSizeInBytes());
    }

    private static VariableDescriptor createDescriptor(int width, int height, int tileWidth, int tileHeight) {
        VariableDescriptor variableDescriptor = new VariableDescriptor();
        variableDescriptor.width = width;
        variableDescriptor.height = height;
        variableDescriptor.tileWidth = tileWidth;
        variableDescriptor.tileHeight = tileHeight;
        variableDescriptor.dataType = ProductData.TYPE_FLOAT32;
        return variableDescriptor;
    }
}
