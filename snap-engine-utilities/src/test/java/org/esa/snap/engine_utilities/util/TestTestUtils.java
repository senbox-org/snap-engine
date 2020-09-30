package org.esa.snap.engine_utilities.util;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestTestUtils {

    @Test
    public void testCreateTestProduct() throws Exception {
        final Product product = TestUtils.createProduct("test", 100, 100);
        assertNotNull(product);
        assertNotNull(product.getDescription());
        assertNotNull(product.getSceneGeoCoding());
        assertNotNull(product.getStartTime());
        assertNotNull(product.getEndTime());
        assertNotNull(product.getMetadataRoot());
        assertEquals("test", product.getProductType());
        assertEquals(100, product.getSceneRasterWidth());
        assertEquals(100, product.getSceneRasterHeight());

        final Band srcBand1 = TestUtils.createBand(product, "band1", 100, 100);
        assertTrue(product.containsBand("band1"));
        assertNotNull(srcBand1.getUnit());
        assertEquals(100, srcBand1.getRasterWidth());
        assertEquals(100, srcBand1.getRasterHeight());

        TestUtils.verifyProduct(product, true, true, true);
    }

    @Test
    public void testCreateMultiSizeTestProduct() throws Exception {
        final Product product = TestUtils.createProduct("test", 100, 100);
        final Band srcBand1 = TestUtils.createBand(product, "band1", 100, 100);
        final Band srcBand2 = TestUtils.createBand(product, "band2", 150, 150);

        assertTrue(product.containsBand("band1"));
        assertTrue(product.containsBand("band2"));

        assertEquals(100, product.getSceneRasterWidth());
        assertEquals(100, product.getSceneRasterHeight());

        assertEquals(100, srcBand1.getRasterWidth());
        assertEquals(100, srcBand1.getRasterHeight());

        assertEquals(150, srcBand2.getRasterWidth());
        assertEquals(150, srcBand2.getRasterHeight());

        TestUtils.verifyProduct(product, true, true, true);
    }
}
