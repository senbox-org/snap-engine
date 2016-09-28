package org.esa.s3tbx.olci.radiometry.rayleigh;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.esa.s3tbx.olci.radiometry.smilecorr.SmileCorrectionUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Tile;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.DoubleStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

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
        double[] convertDegreesToRadians = SmileCorrectionUtils.convertDegreesToRadians(new double[]{50, 30, -1});
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

    @Ignore
    @Test
    public void testGetRhoWithRayleighAux() throws Exception {
        RayleighAux rayleighAux = getRayleighAux();
        double[] corrOzoneRefl = {1.0, 2.2};
        double[] rayleighOpticalThickness = {1.0, 2};

        double[] expectedRhoBrr = algo.getRhoBrr(rayleighAux, rayleighOpticalThickness, corrOzoneRefl);
        assertEquals(2, expectedRhoBrr.length);
//        assertArrayEquals(new double[]{1}, expectedRhoBrr, 1e-4);

    }

    @Test
    public void testGetRho() throws Exception {
        ArrayList<double[]> interpolateValues = getInterpolationValues();

        double rayleighOpticalThickness = 1.0;
        double massAir = 1.0;
        double cosOZARad = 1.5;
        double cosSZARad = 1.5;
        double[] fourierSeriesCof = {1.2, 2.2, 3.0};
        double[] fourierSeriesExpected = algo.getFourierSeries(rayleighOpticalThickness, massAir, cosOZARad, cosSZARad, interpolateValues, fourierSeriesCof);

        assertEquals(3, fourierSeriesExpected.length);
        assertEquals(0.6321, fourierSeriesExpected[0], 1e-4);

    }

    private ArrayList<double[]> getInterpolationValues() {
        ArrayList<double[]> interpolateValues = new ArrayList<>();
        interpolateValues.add(new double[]{1.0, 2.0, 3.0, 4.0});
        interpolateValues.add(new double[]{5.0, 6.0, 7.0, 8.0});
        interpolateValues.add(new double[]{9.0, 10.0, 11.0, 12.0});
        return interpolateValues;
    }

    @Test
    public void testGetRayleighReflectance() throws Exception {
        RayleighInput rayleighInput = getRayleighSample();
        RayleighAux rayleighAux = getRayleighAux();
        int indexOfArray = 1;
        double[] absorptionOfBand = {1.0, 1.2};
        double[] crossSectionSigma = {1.0, 1.2};

       /* RayleighOutput rrExpected = algo.getRayleighReflectance(rayleighInput, rayleighAux, indexOfArray, absorptionOfBand, crossSectionSigma);
        assertNotEquals(getRayleighSample().getSourceReflectance(), rrExpected.getSourceRayRefl());
        assertNotEquals(getRayleighSample().getLowerReflectance(), rrExpected.getLowerRayRefl());
        assertNotEquals(getRayleighSample().getUpperReflectance(), rrExpected.getUpperRayRefl());*/
    }


    private RayleighAux getRayleighAux() {
        Tile mockSourceTile = getSourceTile();
        RayleighAux rayleighAux = new RayleighAux();


        rayleighAux.setSunZenithAngles(mockSourceTile);
        rayleighAux.setViewZenithAngles(mockSourceTile);
        rayleighAux.setSunAzimuthAngles(mockSourceTile);
        rayleighAux.setViewAzimuthAngles(mockSourceTile);
        rayleighAux.setSeaLevels(mockSourceTile);
        rayleighAux.setTotalOzones(mockSourceTile);
        rayleighAux.setLatitudes(mockSourceTile);
        rayleighAux.setLongitude(mockSourceTile);
        rayleighAux.setAltitudes(mockSourceTile);
        List<double[]> interpolationValues = getInterpolationValues();
        HashMap<Integer, List<double[]>> listHashMap = new HashMap<>();
        listHashMap.put(1, interpolationValues);
        rayleighAux.setInterpolation(listHashMap);
        RayleighAux.linearInterpolate = new LinearInterpolator().interpolate(new double[]{0, 2.0}, new double[]{0, 2.0});
        RayleighAux.tau_ray = new double[]{1.0, 1.0, 1.0};
        return rayleighAux;
    }

    private Tile getSourceTile() {
        Tile mockTile = Mockito.mock(Tile.class);
        when(mockTile.getHeight()).thenReturn(2);
        when(mockTile.getWidth()).thenReturn(2);
        when(mockTile.getMaxX()).thenReturn(1);
        when(mockTile.getMinX()).thenReturn(0);
        when(mockTile.getMaxY()).thenReturn(1);
        when(mockTile.getMinY()).thenReturn(0);
        when(mockTile.getSampleDouble(0, 0)).thenReturn(1.0);
        return mockTile;
    }

    private RayleighInput getRayleighSample() {
        int sourceReflectance = 5;
        int lowerReflectance = 10;
        int upperReflectance = 15;
        int sourceIndex = 1;
        int lowerWaterIndex = 1;
        int upperWaterIndex = 1;

        return new RayleighInput(sourceReflectance, lowerReflectance, upperReflectance, sourceIndex, lowerWaterIndex, upperWaterIndex);
    }
}