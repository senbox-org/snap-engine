package org.esa.s3tbx.slstr.pdu.stitching;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.util.ArrayUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Tonio Fincke
 */
public class PDUStitchingOpTest {

    public static String EXPECTED_STITCHED_FILE_NAME_PATTERN =
            "S3A_SL_1_RBT____20130707T153252_20130707T154752_2[0-9]{7}T[0-9]{6}_0299_158_182______SVL_O_NR_001.SEN3";

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
    @Ignore //takes a few seconds
    public void testOperator() throws IOException {
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("targetDir", targetDirectory);
        String[] productPaths = new String[3];
        productPaths[0]= getResource(TestUtils.FIRST_FILE_NAME).getAbsolutePath();
        productPaths[1] = getResource(TestUtils.SECOND_FILE_NAME).getAbsolutePath();
        productPaths[2]= getResource(TestUtils.THIRD_FILE_NAME).getAbsolutePath();
        parameterMap.put("sourceProductPaths", productPaths);

        assertEquals(0, targetDirectory.list().length);

        GPF.createProduct("PduStitchingOp", parameterMap);

        assertProductHasBeenCreated();
    }

    @Test
    @Ignore //takes a few seconds
    public void testOperator_wildcards() throws IOException {
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("targetDir", targetDirectory);
        String[] productPaths = new String[1];
        productPaths[0] = PDUStitchingOpTest.class.getResource("").getFile() + "*/xfdumanifest.xml" ;
        parameterMap.put("sourceProductPaths", productPaths);

        assertEquals(0, targetDirectory.list().length);

        GPF.createProduct("PduStitchingOp", parameterMap);

        assertProductHasBeenCreated();
    }

    private void assertProductHasBeenCreated() {
        final Pattern pattern = Pattern.compile(EXPECTED_STITCHED_FILE_NAME_PATTERN);
        final File[] stitchedProducts = targetDirectory.listFiles();
        assertNotNull(stitchedProducts);
        assertEquals(1, stitchedProducts.length);
        assert(pattern.matcher(stitchedProducts[0].getName()).matches());
        final String[] productContents = stitchedProducts[0].list();
        assertNotNull(productContents);
        assertEquals(3, productContents.length);
        assert(ArrayUtils.isMemberOf("F1_BT_io.nc", productContents));
        assert(ArrayUtils.isMemberOf("met_tx.nc", productContents));
        assert(ArrayUtils.isMemberOf("viscal.nc", productContents));
        assert(ArrayUtils.isMemberOf("xfdumanifest.xml", productContents));
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