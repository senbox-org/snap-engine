package org.esa.beam.dataio.s3.util;

import org.esa.beam.framework.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * @author Tonio Fincke
 */
public class S3NetcdfReaderTest {

    private S3NetcdfReader reader;

    @Before
    public void setUp() throws IOException {
        String netDCFFilePath = S3NetcdfReader.class.getResource("../../s3/FRP_in.nc").getPath();
        reader = new S3NetcdfReader(netDCFFilePath);
    }

    @Test
    public void testReadProductType() throws IOException {
        final String productType = reader.readProductType();
        assertEquals("NetCDF", productType);
    }

    @Test
    public void testGetWidth() throws IOException {
        final int width = reader.getWidth();
        assertEquals(1568, width);
    }

    @Test
    public void testGetHeight() throws IOException {
        final int height = reader.getHeight();
        assertEquals(266, height);
    }

    @Test
    public void testReadProduct() throws Exception {
        final Product product = reader.readProduct();
        assertNotNull(product);
        assertEquals("FRP_in", product.getName());
        assertEquals("NetCDF", product.getProductType());
        assertEquals(1568, product.getSceneRasterWidth());
        assertEquals(266, product.getSceneRasterHeight());
    }

}
