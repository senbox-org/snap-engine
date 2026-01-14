package eu.esa.snap.core.dataio.cache;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.*;

public class DataBufferTest {

    @Test
    @STTM("SNAP-4121")
    public void testConstruct_2D() {
        int productDataType = ProductData.TYPE_FLOAT32;
        int[] offsets = {400, 0};
        int[] shapes = {512, 1024};

        final DataBuffer dataBuffer = new DataBuffer(productDataType, offsets, shapes);
        ProductData data = dataBuffer.getData();
        assertNotNull(data);
        assertEquals(524288, data.getNumElems());

        assertEquals(0, dataBuffer.getOffsetX());
        assertEquals(400, dataBuffer.getOffsetY());
        assertEquals(-1, dataBuffer.getOffsetLayer());
        assertEquals(1024, dataBuffer.getWidth());
        assertEquals(512, dataBuffer.getHeight());
        assertEquals(-1, dataBuffer.getNumLayers());
    }

    @Test
    @STTM("SNAP-4121")
    public void testConstruct_3D() {
        int productDataType = ProductData.TYPE_FLOAT64;
        int[] offsets = {100, 400, 0};
        int[] shapes = {10, 64, 256};

        final DataBuffer dataBuffer = new DataBuffer(productDataType, offsets, shapes);
        ProductData data = dataBuffer.getData();
        assertNotNull(data);
        assertEquals(163840, data.getNumElems());

        assertEquals(0, dataBuffer.getOffsetX());
        assertEquals(400, dataBuffer.getOffsetY());
        assertEquals(100, dataBuffer.getOffsetLayer());
        assertEquals(256, dataBuffer.getWidth());
        assertEquals(64, dataBuffer.getHeight());
        assertEquals(10, dataBuffer.getNumLayers());
    }

    @Test
    @STTM("SNAP-4121")
    public void testConstruct_invalidVectorSizes() {
        try {
            new DataBuffer(12, new int[]{0, 1, 2, 3}, new int[]{44, 55});
            fail("Exception expected");
        } catch (Exception expected) {
        }
    }

    @Test
    @STTM("SNAP-4121")
    public void testConstruct_bufferSizeMismatch() {
        try {
            new DataBuffer(ProductData.createInstance(ProductData.TYPE_INT16, 100), new int[]{1, 2, 3}, new int[]{5, 5, 5});
            // because 5*5*5 != 100
            fail("Exception expected");
        } catch (Exception expected) {
        }
    }

    @Test
    @STTM("SNAP-4121")
    public void testConstruct_withGivenData_2D() {
        ProductData productData = ProductData.createInstance(ProductData.TYPE_INT8, 300);

        DataBuffer dataBuffer = new DataBuffer(productData, new int[]{100, 200}, new int[]{20, 15});

        assertSame(productData, dataBuffer.getData());

        assertEquals(200, dataBuffer.getOffsetX());
        assertEquals(100, dataBuffer.getOffsetY());
        assertEquals(-1, dataBuffer.getOffsetLayer());
        assertEquals(15, dataBuffer.getWidth());
        assertEquals(20, dataBuffer.getHeight());
        assertEquals(-1, dataBuffer.getNumLayers());
    }

    @Test
    @STTM("SNAP-4121")
    public void testConstruct_withGivenData_3D() {
        ProductData productData = ProductData.createInstance(ProductData.TYPE_INT16, 3000);

        DataBuffer dataBuffer = new DataBuffer(productData, new int[]{14, 100, 200}, new int[]{10, 20, 15});

        assertSame(productData, dataBuffer.getData());

        assertEquals(200, dataBuffer.getOffsetX());
        assertEquals(100, dataBuffer.getOffsetY());
        assertEquals(14, dataBuffer.getOffsetLayer());
        assertEquals(15, dataBuffer.getWidth());
        assertEquals(20, dataBuffer.getHeight());
        assertEquals(10, dataBuffer.getNumLayers());
    }

    @Test
    @STTM("SNAP-4121")
    public void testGetSize() {
        assertEquals(1, DataBuffer.getSize(new int[0]));
        assertEquals(5, DataBuffer.getSize(new int[] {5}));
        assertEquals(50, DataBuffer.getSize(new int[] {10, 5}));
        assertEquals(450, DataBuffer.getSize(new int[] {10, 5, 9}));
    }
}
