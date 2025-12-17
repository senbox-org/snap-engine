package eu.esa.snap.core.dataio.cache;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VariableCache3DTest {

    @Test
    public void testInitiateCache() {
        final int[] cacheSizes = {8, 10, 10};
        final int[] productSizes = new int[]{30, 100, 65};
        VariableDescriptor descriptor = createDescriptor(productSizes, cacheSizes);

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
