package org.esa.s3tbx.olci.radiometry.operator;


import org.esa.s3tbx.olci.radiometry.gaseousabsorption.GaseousAbsorptionAlgorithm;
import org.esa.snap.core.gpf.OperatorException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;


/**
 * @author muhammad.bc.
 */
public class GaseousAbsorptionAlgorithmTest {

    GaseousAbsorptionAlgorithm gaseousAbsorptionAlgo;

    @Before
    public void setUp() throws Exception {
        gaseousAbsorptionAlgo = new GaseousAbsorptionAlgorithm();
    }

    @Test
    public void testGetExponential() {
        assertEquals(2.7182817459106445, gaseousAbsorptionAlgo.getExponential(1, -1, 1), 1e-8);
        assertEquals(0.36787945, gaseousAbsorptionAlgo.getExponential(1, 1, 1), 1e-8);
        assertEquals(1.0, gaseousAbsorptionAlgo.getExponential(1, 0, 1), 1e-8);
    }

    @Test
    public void testGetGasToComputeForABand() throws Exception {
        assertArrayEquals(new String[]{"NO2"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa01_radiance"));
        assertArrayEquals(new String[]{"NO2"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa02_radiance"));
        assertArrayEquals(new String[]{"NO2", "H2O", "O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa03_radiance"));
        assertArrayEquals(new String[]{"O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa04_radiance"));
        assertArrayEquals(new String[]{"H2O", "O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa05_radiance"));
        assertArrayEquals(new String[]{"O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa06_radiance"));
        assertArrayEquals(new String[]{"O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa07_radiance"));
        assertArrayEquals(new String[]{"H2O", "O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa08_radiance"));
        assertArrayEquals(new String[]{"O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa09_radiance"));
        assertArrayEquals(new String[]{"H2O", "O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa10_radiance"));
        assertArrayEquals(new String[]{"H2O", "O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa11_radiance"));
        assertArrayEquals(new String[]{"O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa12_radiance"));
        assertArrayEquals(new String[]{"O2", "O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa13_radiance"));
        assertArrayEquals(new String[]{"O2", "O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa14_radiance"));
        assertArrayEquals(new String[]{"O2", "O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa15_radiance"));
        assertArrayEquals(new String[]{"O2", "O3", "H2O"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa16_radiance"));
        assertArrayEquals(new String[]{"H2O"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa17_radiance"));
        assertArrayEquals(new String[]{"H2O"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa18_radiance"));
        assertArrayEquals(new String[]{"H2O"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa19_radiance"));
        assertArrayEquals(new String[]{"H2O"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa20_radiance"));
        assertArrayEquals(new String[]{"H2O"}, gaseousAbsorptionAlgo.gasToComputeForBand("Oa21_radiance"));
    }

    @Test
    public void testGasToComputeDoesNotExist() throws Exception {
        assertArrayEquals(null, gaseousAbsorptionAlgo.gasToComputeForBand("dummy1"));
    }


    @Test
    public void testGetMassAir() throws Exception {
        float[] massAir = gaseousAbsorptionAlgo.getMassAir(new float[]{1, 2}, new float[]{3, 4});
        assertEquals(2, massAir.length);
        assertArrayEquals(new float[]{0.84070706f, -3.9328835f}, massAir, 0);
    }

    @Test
    public void testGetMassAirNull() throws Exception {
        try {
            float[] massAir = gaseousAbsorptionAlgo.getMassAir(null, new float[]{3, 4});
            assertEquals(2, massAir.length);
            assertArrayEquals(new float[]{0.84070706f, -3.9328835f}, massAir, 0);
            fail("The sun zenith angel most not be null");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testGetMassAirZero() throws Exception {
        float[] massAir = gaseousAbsorptionAlgo.getMassAir(new float[]{0, 0}, new float[]{0, 0});
        assertEquals(2, massAir.length);
        assertArrayEquals(new float[]{2, 2}, massAir, 0);
    }

    @Test
    public void testGetTransmissionGasKnownBand() throws Exception {
        GaseousAbsorptionAlgorithm algorithm = new GaseousAbsorptionAlgorithm();
        float[] oza = {4, 5, 6};
        float[] sza = {1, 2, 3};
        float[] oa01_radiances = algorithm.getTransmissionGas("Oa01_radiance", sza, oza);
        assertEquals(3, oa01_radiances.length);
        assertEquals(0.725474f, oa01_radiances[0]);
        assertEquals(0.32552302f, oa01_radiances[1]);
        assertEquals(0.96911377f, oa01_radiances[2]);
    }

    @Test
    public void testGetTransmissionGasUnKnownBand() {
        GaseousAbsorptionAlgorithm algorithm = new GaseousAbsorptionAlgorithm();
        float[] oza = {4, 5, 6};
        float[] sza = {1, 2, 3};
        float[] dummies = algorithm.getTransmissionGas("dummy", sza, oza);
        assertNull(dummies);
    }
}