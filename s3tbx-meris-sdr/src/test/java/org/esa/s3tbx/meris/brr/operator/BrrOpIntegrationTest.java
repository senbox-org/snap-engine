package org.esa.s3tbx.meris.brr.operator;


import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BrrOpIntegrationTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        boolean internetAvailable;
        try {
            URLConnection urlConnection = new URL("http://www.google.com").openConnection();
            urlConnection.setConnectTimeout(5);
            urlConnection.getContent();
            internetAvailable = true;
        } catch (IOException e) {
            internetAvailable = false;
        }

        Assume.assumeTrue("Internet connection not available, skipping BrrOpIntegrationTest", internetAvailable);
    }

    @Test
    public void testProcessMerisL1B() throws IOException {
        final Product merisL1BProduct = MerisL1BProduct.create();

        final Product target = GPF.createProduct("Meris.Brr", createParameter(), merisL1BProduct);
        try {
            assertCorrectBand("brr_1", new float[]{0.03289416432380676f, 0.032959673553705215f}, target);
            assertCorrectBand("brr_2", new float[]{0.031884822994470596f, 0.032448794692754745f}, target);
            assertCorrectBand("brr_3", new float[]{0.033055514097213745f, 0.032872218638658524f}, target);
            assertCorrectBand("brr_4", new float[]{0.03199249505996704f, 0.031583573669195175f}, target);
            assertCorrectBand("brr_5", new float[]{0.025552496314048767f, 0.025396578013896942f}, target);
            assertCorrectBand("brr_6", new float[]{0.01606384664773941f, 0.015916751697659492f}, target);
            assertCorrectBand("brr_7", new float[]{0.014079447835683823f, 0.013955960981547832f}, target);
            assertCorrectBand("brr_8", new float[]{0.01331784762442112f, 0.013692145235836506f}, target);
            assertCorrectBand("brr_9", new float[]{0.012169723398983479f, 0.012201455421745777f}, target);
            assertCorrectBand("brr_10", new float[]{0.010856962762773037f, 0.010678501799702644f}, target);
            assertCorrectBand("brr_12", new float[]{0.01044550258666277f, 0.010495727881789207f}, target);
            assertCorrectBand("brr_13", new float[]{0.0091937854886055f, 0.008930962532758713f}, target);
            assertCorrectBand("brr_14", new float[]{0.008603006601333618f, 0.0082590002566576f}, target);
        } finally {
            if (target != null) {
                target.dispose();
            }
        }
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
