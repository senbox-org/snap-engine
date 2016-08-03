package org.esa.s3tbx.dataio.s3.util;

import org.esa.snap.core.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.*;

/**
 * @author Tonio Fincke
 */
public class S3NetcdfReaderTest {

    private static final String NETCDF_FILE_PATH = S3NetcdfReader.class.getResource("../../s3/FRP_in.nc").getPath();
    private S3NetcdfReader reader;

    @Before
    public void setUp() throws IOException {
        reader = new S3NetcdfReader();
    }

    @Test
    public void testReadProduct() throws Exception {
        final Product product = reader.readProductNodes(NETCDF_FILE_PATH, null);
        assertNotNull(product);
        assertEquals("FRP_in", product.getName());
        assertEquals("NetCDF", product.getProductType());
        assertEquals(1568, product.getSceneRasterWidth());
        assertEquals(266, product.getSceneRasterHeight());
    }

}
