package org.esa.s3tbx.olci.radiometry.gasabsorption;


import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;


/**
 * @author muhammad.bc.
 */
public class GaseousAbsorptionAlgoTest {

    GaseousAbsorptionAlgo gaseousAbsorptionAlgo;

    @Before
    public void setUp() throws Exception {
        gaseousAbsorptionAlgo = new GaseousAbsorptionAlgo();
    }

    @Test
    public void testGetExponential() {
        assertEquals(2.7182817459106445, gaseousAbsorptionAlgo.getExponential(1, -1, 1), 1e-8);
        assertEquals(0.36787945, gaseousAbsorptionAlgo.getExponential(1, 1, 1), 1e-8);
        assertEquals(1.0, gaseousAbsorptionAlgo.getExponential(1, 0, 1), 1e-8);
    }

    @Test
    public void testGetGasToComputeForABand() throws Exception {
        assertArrayEquals(new String[]{"NO2"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_01"));
        assertArrayEquals(new String[]{"NO2"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_02"));
        assertArrayEquals(new String[]{"NO2", "H2O", "O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_03"));
        assertArrayEquals(new String[]{"O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_04"));
        assertArrayEquals(new String[]{"H2O", "O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_05"));
        assertArrayEquals(new String[]{"O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_06"));
        assertArrayEquals(new String[]{"O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_07"));
        assertArrayEquals(new String[]{"H2O", "O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_08"));
        assertArrayEquals(new String[]{"O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_09"));
        assertArrayEquals(new String[]{"H2O", "O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_10"));
        assertArrayEquals(new String[]{"H2O", "O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_11"));
        assertArrayEquals(new String[]{"O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_12"));
        assertArrayEquals(new String[]{"O2", "O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_13"));
        assertArrayEquals(new String[]{"O2", "O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_14"));
        assertArrayEquals(new String[]{"O2", "O3"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_15"));
        assertArrayEquals(new String[]{"O2", "O3", "H2O"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_16"));
        assertArrayEquals(new String[]{"H2O"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_17"));
        assertArrayEquals(new String[]{"H2O"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_18"));
        assertArrayEquals(new String[]{"H2O"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_19"));
        assertArrayEquals(new String[]{"H2O"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_20"));
        assertArrayEquals(new String[]{"H2O"}, gaseousAbsorptionAlgo.gasToComputeForBand("gaseous_absorp_21"));
    }

    @Test
    public void testGasToComputeDoesNotExist() throws Exception {
        try {
            assertArrayEquals(null, gaseousAbsorptionAlgo.gasToComputeForBand("dummy1"));
            fail();
        } catch (IllegalArgumentException e) {

        }
    }


    @Test
    public void testGetMassAir() throws Exception {
        float[] massAir = gaseousAbsorptionAlgo.getMassAir(new float[]{1, 2}, new float[]{3, 4});
        assertEquals(2, massAir.length);
        assertArrayEquals(new float[]{2.0015247f, 2.0030515f}, massAir, 0);
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
        GaseousAbsorptionAlgo algorithm = new GaseousAbsorptionAlgo();
        float[] oza = {4, 5, 6};
        float[] sza = {1, 2, 3};
        float[] oa01_radians = algorithm.getTransmissionGas("gaseous_absorp_01", sza, oza);
        assertEquals(3, oa01_radians.length);
        assertEquals(0.13498464f, oa01_radians[0]);
        assertEquals(0.13473716f, oa01_radians[1]);
        assertEquals(0.1344073f, oa01_radians[2]);
    }

    @Test
    @Ignore
    public void testGetTransmissionGasUnKnownBand() {
        GaseousAbsorptionAlgo algorithm = new GaseousAbsorptionAlgo();
        float[] oza = {4, 5, 6};
        float[] sza = {1, 2, 3};
        float[] dummies = algorithm.getTransmissionGas("dummy", sza, oza);
        assertNull(dummies);
    }
}