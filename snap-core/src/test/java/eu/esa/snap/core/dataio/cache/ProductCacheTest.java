package eu.esa.snap.core.dataio.cache;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ProductCacheTest {

    @Test
    @STTM("SNAP-4107")
    public void testGetSizeInBytes() throws IOException {
        final ProductCache productCache = new ProductCache(new MockProvider(ProductData.TYPE_INT32));

        assertEquals(0, productCache.getSizeInBytes());

        ProductData targetBuffer = ProductData.createInstance(ProductData.TYPE_INT32, 200);

        // trigger reading the upper left tile
        int[] offsets = new int[]{0, 0};
        int[] shapes = new int[]{20, 10};
        DataBuffer dataBuffer = new DataBuffer(targetBuffer, offsets, shapes);
        productCache.read("whatever", offsets, shapes, dataBuffer);

        assertEquals(1568, productCache.getSizeInBytes());

        // trigger reading the lower right tile
        targetBuffer = ProductData.createInstance(ProductData.TYPE_INT32, 20);
        offsets = new int[]{35, 15};
        shapes = new int[]{4, 5};
        dataBuffer = new DataBuffer(targetBuffer, offsets, shapes);
        productCache.read("whatever", offsets, shapes, dataBuffer);

        assertEquals(2368, productCache.getSizeInBytes());

        productCache.dispose();
        assertEquals(0, productCache.getSizeInBytes());
    }

    @Test
    @STTM("SNAP-4107")
    public void testRead_3DTargetBuffer() throws IOException {
        final ProductCache productCache = new ProductCache(new MockProvider(ProductData.TYPE_INT64, new int[]{20, 100, 50}, new int[]{10, 20, 10}));

        int[] offsets = new int[]{0, 0, 0};
        int[] shapes = new int[]{10, 20, 10};
        final DataBuffer dataBuffer = new DataBuffer(ProductData.TYPE_INT64, offsets, shapes);

        productCache.read("whatever", offsets, shapes, dataBuffer);
        assertEquals(1, dataBuffer.getData().getElemIntAt(1));
        assertEquals(27, dataBuffer.getData().getElemIntAt(27));
    }

    @Test
    @STTM("SNAP-4107")
    public void testRead_2DTargetBuffer() throws IOException {
        final ProductCache productCache = new ProductCache(new MockProvider(ProductData.TYPE_UINT8, new int[]{6, 100, 50}, new int[]{6, 20, 10}));

        int[] bufferOffsets = new int[]{0, 0};
        int[] bufferShapes = new int[]{20, 10};
        final DataBuffer dataBuffer = new DataBuffer(ProductData.TYPE_UINT8, bufferOffsets, bufferShapes);

        int[] offsets = new int[]{0, 0, 0};
        int[] shapes = new int[]{1, 20, 10};
        productCache.read("whatever", offsets, shapes, dataBuffer);
        assertEquals(1, dataBuffer.getData().getElemIntAt(1));
        assertEquals(27, dataBuffer.getData().getElemIntAt(27));
    }
}
