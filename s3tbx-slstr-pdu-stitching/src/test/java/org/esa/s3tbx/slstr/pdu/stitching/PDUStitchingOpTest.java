package org.esa.s3tbx.slstr.pdu.stitching;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Tonio Fincke
 */
public class PDUStitchingOpTest {

    File targetDirectory;

    @Before
    public void setUp() {
        targetDirectory = new File("test_out");
        if (!targetDirectory.mkdirs()) {
            fail("Unable to create test target directory");
        }
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new PDUStitchingOp.Spi());
    }

    @After
    public void tearDown() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(new PDUStitchingOp.Spi());
        if (targetDirectory.isDirectory()) {
            if (!FileUtils.deleteTree(targetDirectory)) {
                fail("Unable to delete test directory");
            }
        }
    }

    @Test
    public void testOperator() throws IOException {
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("targetDir", targetDirectory);
        Product[] products = new Product[3];
        products[0] = ProductIO.readProduct(getResource(TestConstants.FIRST_FILE_NAME));
        products[1] = ProductIO.readProduct(getResource(TestConstants.SECOND_FILE_NAME));
        products[2] = ProductIO.readProduct(getResource(TestConstants.THIRD_FILE_NAME));

        final Product product = GPF.createProduct("PduStitchingOp", parameterMap, products);

        if (product != null) {
            product.dispose();
            product.closeIO();
        }
    }

    @Test
    public void testSpi() {
        final OperatorSpi spi = new PDUStitchingOp.Spi();

        assertTrue(spi.getOperatorClass().isAssignableFrom(PDUStitchingOp.class));
    }

    private static File getResource(String fileName) {
        final String fullFileName = fileName + "/xfdumanifest.xml";
        final URL resource = PDUStitchingOpTest.class.getResource(fullFileName);
        return new File(resource.getFile());
    }

}