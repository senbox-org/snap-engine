package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

import java.util.stream.DoubleStream;
import org.esa.s3tbx.olci.radiometry.smilecorr.SmileUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author muhammad.bc.
 */
public class RayleighCorrAlgorithmTest {

    private RayleighCorrAlgorithm algo;

    @Before
    public void setUp() throws Exception {
        algo = new RayleighCorrAlgorithm();
    }

    @Test
    public void testConvertToRadian() throws Exception {
        double[] convertDegreesToRadians = SmileUtils.convertDegreesToRadians(new double[]{50, 30, -1});
        assertEquals(0.872664626, convertDegreesToRadians[0], 1e-8);
        assertEquals(0.5235987756, convertDegreesToRadians[1], 1e-8);
        assertEquals(-0.017453292519943295, convertDegreesToRadians[2], 1e-8);

    }

    @Test
    public void testCalculateRayleighCrossSection() throws Exception {
        double[] sectionSigma = algo.getCrossSection(new double[]{1., 2.});
        assertEquals(2, sectionSigma.length);
        assertEquals(1.004158010817489E-9, sectionSigma[0], 1e-8);
        assertEquals(3.9154039638717356E-12, sectionSigma[1], 1e-8);
    }

    @Test
    public void testCheckIn() throws Exception {
        double[] n = new double[]{2.840904951095581,
                17.638418197631836,
                28.7684268951416,
                36.189727783203125,
                43.61144256591797,
                51.033390045166016,
                58.45547866821289,
                65.87765502929688,
                69.58876037597656,
                73.29988098144531,
                77.0110092163086,
                80.7221450805664};

        boolean b = DoubleStream.of(n).anyMatch(x -> x < 2.82);
        assertFalse(b);
        boolean c = DoubleStream.of(n).anyMatch(x -> x > 2.82 && x < 80.7221450805664);
        assertTrue(c);
    }

    @Test
    public void testCorrectOzone() throws Exception {
        int cosViewAngle = 1;
        int cosSunAngel = 1;
        int ozone = 1;
        int absorpO = 1;
        int rayThickness = 1;

        double corrOzone = algo.getCorrOzone(rayThickness, absorpO, ozone, cosSunAngel, cosViewAngle);
        assertEquals(1.002, corrOzone, 1e-3);

        try {
            cosSunAngel = 0;
            cosViewAngle = 0;
            corrOzone = algo.getCorrOzone(rayThickness, absorpO, ozone, cosSunAngel, cosViewAngle);
            assertEquals(1.002, corrOzone, 1e-3);
            fail("The sun angel and the view angle must not be zero.");
        } catch (ArithmeticException e) {

        }


    }

    @Test
    public void testCorrectOzoneZeroDenominator() throws Exception {
        try {
            int cosViewAngle = 1;
            int cosSunAngel = 1;
            int ozone = 1;
            int absorpO = 1;
            int rayThickness = 1;
            cosSunAngel = 0;
            cosViewAngle = 0;
            double corrOzone = algo.getCorrOzone(rayThickness, absorpO, ozone, cosSunAngel, cosViewAngle);
            assertEquals(1.002, corrOzone, 1e-3);
            fail("The sun angel and the view angle must not be zero.");
        } catch (ArithmeticException e) {

        }
    }

    @Test
    public void testCorrectOzoneZero() throws Exception {
        int cosViewAngle = 0;
        int cosSunAngel = 0;
        int ozone = 0;
        int absorpO = 0;
        int rayThickness = 0;
        cosSunAngel = 1;
        cosViewAngle = 1;
        double corrOzone = algo.getCorrOzone(rayThickness, absorpO, ozone, cosSunAngel, cosViewAngle);
        assertEquals(0.0, corrOzone, 1e-3);
    }

    @Test
    public void testWaterVapor() throws Exception {
        double[] bWVTile = {1.0};
        double[] bWVRefTile = {1.0};
        double[] reflectances = {1.0};
        double[] vaporCorrection709 = algo.waterVaporCorrection709(reflectances, bWVRefTile, bWVTile);
        assertArrayEquals(new double[]{0.996}, vaporCorrection709, 1e-3);
    }

    @Test
    public void testThickness() throws Exception {
        double latitude = 1.25;
        double altitude = 20.90;
        double seaLevelPressure = 0.8;
        double sigma = 0.5;
        double rayThickness = algo.getRayleighOpticalThickness(sigma, seaLevelPressure, altitude, latitude);
        assertEquals(8.497244949908416E21, rayThickness, 1e-8);
    }

    @Test
    public void testCrossSectionSigma() throws Exception {
        Product product = new Product("dummy", "dummy");
        Band b1 = createBand("radiance_1", 1);
        Band b2 = createBand("radiance_2", 2);
        Band b3 = createBand("radiance_3", 3);
        Band b4 = createBand("radiance_4", 4);

        product.addBand(b1);
        product.addBand(b2);
        product.addBand(b3);
        product.addBand(b4);

        double[] allWavelengths = algo.getCrossSectionSigma(product, 3, "radiance_%d");
        assertArrayEquals(new double[]{1.0041580107718594E-9, 3.915403961025194E-12, 1.5231224042681756E-13}, allWavelengths, 1e-8);

    }

    private Band createBand(String bandName, float waveLength) {
        Band b1 = new Band(bandName, ProductData.TYPE_INT8, 1, 1);
        b1.setSpectralWavelength(waveLength);
        return b1;
    }
}