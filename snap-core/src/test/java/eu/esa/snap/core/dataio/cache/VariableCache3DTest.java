package eu.esa.snap.core.dataio.cache;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class VariableCache3DTest {

    @Test
    @STTM("SNAP-4121")
    public void testInitiateCache() {
        final int[] cacheSizes = {8, 10, 10};
        final int[] productSizes = new int[]{30, 100, 65};
        final VariableDescriptor descriptor = createDescriptor(productSizes, cacheSizes);

        CacheData3D[][][] data = VariableCache3D.initiateCache(descriptor);
        assertEquals(4, data.length);
        assertEquals(10, data[0].length);
        assertEquals(7, data[0][0].length);

        CacheData3D cacheData3D = data[0][0][0];
        assertEquals(0, cacheData3D.getxMin());
        assertEquals(9, cacheData3D.getxMax());
        assertEquals(0, cacheData3D.getyMin());
        assertEquals(9, cacheData3D.getyMax());
        assertEquals(0, cacheData3D.getzMin());
        assertEquals(7, cacheData3D.getzMax());

        cacheData3D = data[1][1][1];
        assertEquals(10, cacheData3D.getxMin());
        assertEquals(19, cacheData3D.getxMax());
        assertEquals(10, cacheData3D.getyMin());
        assertEquals(19, cacheData3D.getyMax());
        assertEquals(8, cacheData3D.getzMin());
        assertEquals(15, cacheData3D.getzMax());

        cacheData3D = data[3][9][6];
        assertEquals(60, cacheData3D.getxMin());
        assertEquals(64, cacheData3D.getxMax());
        assertEquals(90, cacheData3D.getyMin());
        assertEquals(99, cacheData3D.getyMax());
        assertEquals(24, cacheData3D.getzMin());
        assertEquals(29, cacheData3D.getzMax());
    }

    @Test
    @STTM("SNAP-4121")
    public void testDispose() {
        final int[] cacheSizes = {6, 8, 10};
        final int[] productSizes = new int[]{50, 50, 65};
        final VariableDescriptor descriptor = createDescriptor(productSizes, cacheSizes);
        final VariableCache3D cache = new VariableCache3D(descriptor, new MockProvider(ProductData.TYPE_UINT16));

        final CacheData3D[][][] cacheData = cache.getCacheData();
        assertEquals(9, cacheData.length);

        cache.dispose();
        assertNull(cache.getCacheData());
    }

    @Test
    @STTM("SNAP-4121")
    public void testGetAffectedCacheLocations_cacheHit() {
        final int[] cacheSizes = {12, 12, 20};
        final int[] productSizes = new int[]{100, 180, 226};
        final VariableDescriptor descriptor = createDescriptor(productSizes, cacheSizes);
        final VariableCache3D cache = new VariableCache3D(descriptor, new MockProvider(ProductData.TYPE_UINT16));

        CacheIndex[] affectedTileLocations = cache.getAffectedCacheLocations(new int[]{0, 0, 0}, new int[]{10, 10, 10});
        assertEquals(1, affectedTileLocations.length);
        assertEquals(0, affectedTileLocations[0].getCacheCol());
        assertEquals(0, affectedTileLocations[0].getCacheRow());
        assertEquals(0, affectedTileLocations[0].getCacheLayer());

        affectedTileLocations = cache.getAffectedCacheLocations(new int[]{0, 0, 0}, new int[]{10, 50, 50});
        assertEquals(15, affectedTileLocations.length);
        assertEquals(0, affectedTileLocations[0].getCacheCol());
        assertEquals(0, affectedTileLocations[0].getCacheRow());
        assertEquals(0, affectedTileLocations[0].getCacheLayer());

        assertEquals(0, affectedTileLocations[6].getCacheCol());
        assertEquals(2, affectedTileLocations[6].getCacheRow());
        assertEquals(0, affectedTileLocations[6].getCacheLayer());

        assertEquals(2, affectedTileLocations[14].getCacheCol());
        assertEquals(4, affectedTileLocations[14].getCacheRow());
        assertEquals(0, affectedTileLocations[14].getCacheLayer());

        affectedTileLocations = cache.getAffectedCacheLocations(new int[]{90, 170, 200}, new int[]{10, 10, 10});
        assertEquals(2, affectedTileLocations.length);
        assertEquals(10, affectedTileLocations[1].getCacheCol());
        assertEquals(14, affectedTileLocations[1].getCacheRow());
        assertEquals(8, affectedTileLocations[1].getCacheLayer());
    }

    @Test
    @STTM("SNAP-4121")
    public void testGetAffectedCacheLocations_cacheMiss() {
        final int[] cacheSizes = {12, 12, 20};
        final int[] productSizes = new int[]{100, 100, 200};
        final VariableDescriptor descriptor = createDescriptor(productSizes, cacheSizes);
        final VariableCache3D cache = new VariableCache3D(descriptor, new MockProvider(ProductData.TYPE_UINT16));

        // front outside
        CacheIndex[] affectedTileLocations = cache.getAffectedCacheLocations(new int[]{0, -20, 0}, new int[]{10, 10, 10});
        assertEquals(0, affectedTileLocations.length);

        // left outside
        affectedTileLocations = cache.getAffectedCacheLocations(new int[]{0, 0, -20}, new int[]{10, 10, 10});
        assertEquals(0, affectedTileLocations.length);

        // bottom outside
        affectedTileLocations = cache.getAffectedCacheLocations(new int[]{-20, 0, 0}, new int[]{10, 10, 10});
        assertEquals(0, affectedTileLocations.length);

        // top outside
        affectedTileLocations = cache.getAffectedCacheLocations(new int[]{110, 0, 0}, new int[]{10, 10, 10});
        assertEquals(0, affectedTileLocations.length);

        // back outside
        affectedTileLocations = cache.getAffectedCacheLocations(new int[]{0, 110, 0}, new int[]{10, 10, 10});
        assertEquals(0, affectedTileLocations.length);

        // right outside
        affectedTileLocations = cache.getAffectedCacheLocations(new int[]{0, 0, 210}, new int[]{10, 10, 10});
        assertEquals(0, affectedTileLocations.length);
    }

    @Test
    @STTM("SNAP-4121")
    public void testGetSizeInBytes() throws IOException {
        final int[] cacheSizes = {10, 10, 10};
        final int[] productSizes = new int[]{100, 100, 100};
        final VariableDescriptor descriptor = createDescriptor(productSizes, cacheSizes);
        final VariableCache3D cache = new VariableCache3D(descriptor, new MockProvider(ProductData.TYPE_UINT16));

        assertEquals(384000, cache.getSizeInBytes());

        // read fake data to memory
        final DataBuffer dataBuffer = new DataBuffer(ProductData.TYPE_UINT16, new int[]{0, 0, 0}, new int[]{10, 10, 10});
        cache.read(new int[]{0, 0, 0}, new int[]{10, 10, 10}, dataBuffer);
        // default size plus 1000 * uint_16
        assertEquals(386000, cache.getSizeInBytes());
    }

    static VariableDescriptor createDescriptor(int[] productSizes, int[] cacheSizes) {
        VariableDescriptor variableDescriptor = new VariableDescriptor();
        variableDescriptor.layers = productSizes[0];
        variableDescriptor.height = productSizes[1];
        variableDescriptor.width = productSizes[2];
        variableDescriptor.tileLayers = cacheSizes[0];
        variableDescriptor.tileHeight = cacheSizes[1];
        variableDescriptor.tileWidth = cacheSizes[2];
        return variableDescriptor;
    }
}
