package org.esa.s3tbx.meris.brr.operator;


import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

import static org.junit.Assert.*;

public class BrrOpIntegrationTest {

    private File testOutDirectory;

    @BeforeClass
    public static void beforeClass() throws ParseException {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new BrrOp.Spi());
    }

    @AfterClass
    public static void afterClass() throws ParseException {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(new BrrOp.Spi());
    }

    @Before
    public void setUp() {
        testOutDirectory = new File("output");
        if (!testOutDirectory.mkdirs()) {
            fail("unable to create test directory: " + testOutDirectory);
        }
    }

    @After
    public void tearDown() {
        if (testOutDirectory != null) {
            if (!FileUtils.deleteTree(testOutDirectory)) {
                fail("Unable to delete test directory: " + testOutDirectory);
            }
        }
    }

    @Test
    @Ignore // todo: Reactivate when auxiliary data setup is clarified.
    public void testProcessMerisL1B() throws IOException {
        final Product merisL1BProduct = MerisL1BProduct.create();
       // ProductIO.writeProduct(merisL1BProduct, testOutDirectory.getAbsolutePath() + File.separator + "meris_l1b.dim", "BEAM-DIMAP");

        Product savedProduct = null;
        final Product target = GPF.createProduct("Meris.Brr", createParameter(), merisL1BProduct);
        try {
            final String targetProductPath = testOutDirectory.getAbsolutePath() + File.separator + "meris_brr.dim";
            ProductIO.writeProduct(target, targetProductPath, "BEAM-DIMAP");

            savedProduct = ProductIO.readProduct(targetProductPath);
            assertNotNull(savedProduct);

            assertCorrectBand("brr_1", new float[]{0.03289416432380676f, 0.032959673553705215f}, savedProduct);
            assertCorrectBand("brr_2", new float[]{0.031884822994470596f, 0.032448794692754745f}, savedProduct);
            assertCorrectBand("brr_3", new float[]{0.033055514097213745f, 0.032872218638658524f}, savedProduct);
            assertCorrectBand("brr_4", new float[]{0.03199249505996704f, 0.031583573669195175f}, savedProduct);
            assertCorrectBand("brr_5", new float[]{0.025552496314048767f, 0.025396578013896942f}, savedProduct);
            assertCorrectBand("brr_6", new float[]{0.01606384664773941f, 0.015916751697659492f}, savedProduct);
            assertCorrectBand("brr_7", new float[]{0.014079447835683823f, 0.013955960981547832f}, savedProduct);
            assertCorrectBand("brr_8", new float[]{0.01331784762442112f, 0.013692145235836506f}, savedProduct);
            assertCorrectBand("brr_9", new float[]{0.012169723398983479f, 0.012201455421745777f}, savedProduct);
            assertCorrectBand("brr_10", new float[]{0.010856962762773037f, 0.010678501799702644f}, savedProduct);
            assertCorrectBand("brr_12", new float[]{0.01044550258666277f, 0.010495727881789207f}, savedProduct);
            assertCorrectBand("brr_13", new float[]{0.0091937854886055f, 0.008930962532758713f}, savedProduct);
            assertCorrectBand("brr_14", new float[]{0.008603006601333618f, 0.0082590002566576f}, savedProduct);
        } finally {
            if (savedProduct != null) {
                savedProduct.dispose();
            }
        }
    }

    @Test
    @Ignore
    public void testProcessL1FSG() throws IOException {
        SystemUtils.init3rdPartyLibs(getClass());
        final Product product = ProductIO.readProduct(new File("C:/Data/DIVERSITY/MER_FSG_1PNBCG20030605_160024_000000172017_00040_06607_0001.N1"));

        final Product target = GPF.createProduct("Meris.Brr", createParameter(), product);

        final String targetProductPath = "C:/Data/DIVERSITY/meris_FSG_brr.dim";
//        final long t0 = System.currentTimeMillis();
        GPF.writeProduct(target, new File(targetProductPath), "BEAM-DIMAP", false, ProgressMonitor.NULL);
//        final long t1 = System.currentTimeMillis();
        //System.out.println("delta_t = " + (t1-t0));
        //ProductIO.writeProduct(target, targetProductPath, "BEAM-DIMAP");
    }

    private void assertCorrectBand(String bandName, float[] data, Product savedProduct) {
        final Band brr_1 = savedProduct.getBand(bandName);
        assertNotNull(brr_1);
        assertEquals(data[0], brr_1.getSampleFloat(0, 0), 1e-8);
        assertEquals(data[1], brr_1.getSampleFloat(1, 0), 1e-8);
    }

    private HashMap<String, Object> createParameter() {
        final HashMap<String, Object> parametermap = new HashMap<>();

        parametermap.put("correctWater", "true");
        return parametermap;
    }
}
