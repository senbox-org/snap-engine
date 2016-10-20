package org.esa.s3tbx.idepix.core.util;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for class {@link IdepixIO}.
 *
 * @author Olaf Danne
 */
public class IdepixIOTest {

    @Test
    public void testCorrectSaturatedReflectances() {
        float[] reflOrig = new float[]{Float.NaN, Float.NaN, Float.NaN, 12.3f};

        float[] reflCorr = IdepixUtils.correctSaturatedReflectances(reflOrig);
        assertNotNull(reflCorr);
        assertEquals(4, reflCorr.length);
        assertEquals(12.3f, reflCorr[3], 1.0e-6f);
        assertEquals(12.3f, reflCorr[2], 1.0e-6f);
        assertEquals(12.3f, reflCorr[1], 1.0e-6f);
        assertEquals(12.3f, reflCorr[0], 1.0e-6f);

        reflOrig = new float[]{Float.NaN, Float.NaN, Float.NaN, Float.NaN};
        reflCorr = IdepixUtils.correctSaturatedReflectances(reflOrig);
        assertEquals(true, Float.isNaN(reflCorr[3]));
        assertEquals(true, Float.isNaN(reflCorr[2]));
        assertEquals(true, Float.isNaN(reflCorr[1]));
        assertEquals(true, Float.isNaN(reflCorr[0]));

        reflOrig = new float[]{9.2f, 10.3f, 11.4f, 12.5f};
        reflCorr = IdepixUtils.correctSaturatedReflectances(reflOrig);
        assertEquals(12.5f, reflCorr[3], 1.0e-6f);
        assertEquals(11.4f, reflCorr[2], 1.0e-6f);
        assertEquals(10.3f, reflCorr[1], 1.0e-6f);
        assertEquals(9.2f, reflCorr[0], 1.0e-6f);

        reflOrig = new float[]{9.2f, Float.NaN, Float.NaN, 12.3f};
        reflCorr = IdepixUtils.correctSaturatedReflectances(reflOrig);
        assertEquals(12.3f, reflCorr[3], 1.0e-6f);
        assertEquals(12.3f, reflCorr[2], 1.0e-6f);
        assertEquals(12.3f, reflCorr[1], 1.0e-6f);
        assertEquals(9.2f, reflCorr[0], 1.0e-6f);

        reflOrig = new float[]{9.2f, Float.NaN, Float.NaN, Float.NaN};
        reflCorr = IdepixUtils.correctSaturatedReflectances(reflOrig);
        assertEquals(9.2f, reflCorr[3], 1.0e-6f);
        assertEquals(9.2f, reflCorr[2], 1.0e-6f);
        assertEquals(9.2f, reflCorr[1], 1.0e-6f);
        assertEquals(9.2f, reflCorr[0], 1.0e-6f);

        reflOrig = new float[]{9.2f, Float.NaN, 12.3f, Float.NaN};
        reflCorr = IdepixUtils.correctSaturatedReflectances(reflOrig);
        assertEquals(12.3f, reflCorr[3], 1.0e-6f);
        assertEquals(12.3f, reflCorr[2], 1.0e-6f);
        assertEquals(12.3f, reflCorr[1], 1.0e-6f);
        assertEquals(9.2f, reflCorr[0], 1.0e-6f);

        reflOrig = new float[]{Float.NaN, 9.2f, Float.NaN, 12.3f};
        reflCorr = IdepixUtils.correctSaturatedReflectances(reflOrig);
        assertEquals(12.3f, reflCorr[3], 1.0e-6f);
        assertEquals(12.3f, reflCorr[2], 1.0e-6f);
        assertEquals(9.2f, reflCorr[1], 1.0e-6f);
        assertEquals(9.2f, reflCorr[0], 1.0e-6f);
    }

    @Test
    public void testAreAllReflectancesValid() {
        float[] reflOrig = new float[]{12.3f, 12.3f, 12.3f, 12.3f};
        assertTrue(IdepixIO.areAllReflectancesValid(reflOrig));

        reflOrig = new float[]{Float.NaN, 12.3f, Float.NaN, 12.3f};
        assertFalse(IdepixIO.areAllReflectancesValid(reflOrig));
    }

    @Test
    public void testIsNoReflectanceValid() {
        float[] reflOrig = new float[]{Float.NaN, Float.NaN, Float.NaN, 12.3f};
        assertFalse(IdepixUtils.isNoReflectanceValid(reflOrig));

        reflOrig = new float[]{Float.NaN, Float.NaN, Float.NaN, Float.NaN};
        assertTrue(IdepixUtils.isNoReflectanceValid(reflOrig));
    }

    @Test
    public void testSpectralSlope() {
        float wvl1 = 450.0f;
        float wvl2 = 460.0f;
        float refl1 = 50.0f;
        float refl2 = 100.0f;
        assertEquals(5.0f, IdepixUtils.spectralSlope(refl1, refl2, wvl1, wvl2), 1.0e-6f);

        wvl1 = 450.0f;
        wvl2 = 460.0f;
        refl1 = 500.0f;
        refl2 = 100.0f;
        assertEquals(-40.0f, IdepixUtils.spectralSlope(refl1, refl2, wvl1, wvl2), 1.0e-6f);

        wvl1 = 450.0f;
        wvl2 = 450.0f;
        refl1 = 50.0f;
        refl2 = 100.0f;
        final float slope = IdepixUtils.spectralSlope(refl1, refl2, wvl1, wvl2);
        assertTrue(Float.isInfinite(slope));
    }

    @Test
    public void testSetNewBandProperties() {
        Band band1 = new Band("test", ProductData.TYPE_FLOAT32, 10, 10);
        IdepixIO.setNewBandProperties(band1, "bla", "km", -999.0, false);
        assertEquals("bla", band1.getDescription());
        assertEquals("km", band1.getUnit());
        assertEquals(-999.0, band1.getNoDataValue(), 1.0e-8);
        assertEquals(false, band1.isNoDataValueUsed());

        Band band2 = new Band("test2", ProductData.TYPE_INT32, 10, 10);
        IdepixIO.setNewBandProperties(band2, "blubb", "ton", -1, true);
        assertEquals("blubb", band2.getDescription());
        assertEquals("ton", band2.getUnit());
        assertEquals(-1.0, band2.getNoDataValue(), 1.0e-8);
        assertEquals(true, band2.isNoDataValueUsed());
    }

    @Test
    public void testConvertGeophysicalToMathematicalAngle() {
        double geoAngle = IdepixUtils.convertGeophysicalToMathematicalAngle(31.0);
        assertEquals(59.0, geoAngle, 1.0);
        geoAngle = IdepixUtils.convertGeophysicalToMathematicalAngle(134.0);
        assertEquals(316.0, geoAngle, 1.0);
        geoAngle = IdepixUtils.convertGeophysicalToMathematicalAngle(213.0);
        assertEquals(237.0, geoAngle, 1.0);
        geoAngle = IdepixUtils.convertGeophysicalToMathematicalAngle(301.0);
        assertEquals(149.0, geoAngle, 1.0);
        geoAngle = IdepixUtils.convertGeophysicalToMathematicalAngle(3100.0);
        assertTrue(Double.isNaN(geoAngle));
    }

    @Test
    public void testIsValidLandsat8Product() {
        final Product product = new Product("l8", "l8", 1, 1);
        product.addBand("coastal_aerosol", ProductData.TYPE_FLOAT32);
        product.addBand("blue", ProductData.TYPE_FLOAT32);
        product.addBand("green", ProductData.TYPE_FLOAT32);
        product.addBand("red", ProductData.TYPE_FLOAT32);
        product.addBand("near_infrared", ProductData.TYPE_FLOAT32);
        product.addBand("swir_1", ProductData.TYPE_FLOAT32);
        product.addBand("swir_2", ProductData.TYPE_FLOAT32);
        product.addBand("panchromatic", ProductData.TYPE_FLOAT32);
        product.addBand("cirrus", ProductData.TYPE_FLOAT32);
        product.addBand("thermal_infrared_(tirs)_2", ProductData.TYPE_FLOAT32);

        assertFalse(IdepixIO.isValidLandsat8Product(product));

        product.addBand("thermal_infrared_(tirs)_1", ProductData.TYPE_FLOAT32);

        assertTrue(IdepixIO.isValidLandsat8Product(product));
    }

}