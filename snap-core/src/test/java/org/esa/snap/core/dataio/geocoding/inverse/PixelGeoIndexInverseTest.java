package org.esa.snap.core.dataio.geocoding.inverse;

import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.InverseCoding;
import org.esa.snap.core.dataio.geocoding.TestData;
import org.esa.snap.core.dataio.geocoding.util.XYInterpolator;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.esa.snap.core.dataio.geocoding.util.XYInterpolator.SYSPROP_GEOCODING_INTERPOLATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PixelGeoIndexInverseTest {

    private PixelGeoIndexInverse inverse;

    @Before
    public void setUp() {
        inverse = new PixelGeoIndexInverse();
    }

    @Test
    public void testGetPixelPos_AMSRE() {
        final GeoRaster geoRaster = TestData.get_AMSRE();

        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(-0.9204868, 18.683336), null);
        assertEquals(0.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(0.045474913, 17.999533), null);
        assertEquals(21.5, pixelPos.x, 1e-8);
        assertEquals(4.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(-0.23428795, 17.984755), null);
        assertEquals(24.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(1.9302529, 17.523684), null);
        assertEquals(24.5, pixelPos.x, 1e-8);
        assertEquals(24.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(1.241535, 18.219492), null);
        assertEquals(0.5, pixelPos.x, 1e-8);
        assertEquals(24.5, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_AMSRE_outside() {
        final GeoRaster geoRaster = TestData.get_AMSRE();

        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(-0.83, 18.54), null);
        assertEquals(Double.NaN, pixelPos.x, 1e-8);
        assertEquals(Double.NaN, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(0.34, 17.83), null);
        assertEquals(Double.NaN, pixelPos.x, 1e-8);
        assertEquals(Double.NaN, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(1.91, 17.62), null);
        assertEquals(Double.NaN, pixelPos.x, 1e-8);
        assertEquals(Double.NaN, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(0.76, 18.35), null);
        assertEquals(Double.NaN, pixelPos.x, 1e-8);
        assertEquals(Double.NaN, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_OLCI() {
        final GeoRaster geoRaster = TestData.get_OLCI();

        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(66.52871, -24.182217), null);
        assertEquals(0.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.51401, -24.001337), null);
        assertEquals(31.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.42491, -24.046906), null);
        assertEquals(31.5, pixelPos.x, 1e-8);
        assertEquals(35.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.43959, -24.227152), null);
        assertEquals(0.5, pixelPos.x, 1e-8);
        assertEquals(35.5, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_SYN_AOD_fillValues() {
        final GeoRaster geoRaster = TestData.get_SYN_AOD();

        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(59.2421, -136.13405), null);
        assertEquals(9.5, pixelPos.x, 1e-8);
        assertEquals(1.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(59.19238, -135.08754), null);
        assertEquals(24.5, pixelPos.x, 1e-8);
        assertEquals(1.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(58.232895, -135.29134), null);
        assertEquals(24.5, pixelPos.x, 1e-8);
        assertEquals(26.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(58.28423, -136.32547), null);
        assertEquals(9.5, pixelPos.x, 1e-8);
        assertEquals(26.5, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_AMSRE_interpolating_geodetic_distance() {
        final Properties properties = createProperties(XYInterpolator.Type.GEODETIC);
        inverse = new PixelGeoIndexInverse(true, properties);

        final GeoRaster geoRaster = TestData.get_AMSRE();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(-0.8298334, 18.600895), null);
        assertEquals(3.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(-0.7098208, 18.553856), null);
        assertEquals(4.5, pixelPos.x, 1e-8);
        assertEquals(1.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(-0.7698271, 18.5773755), null);
        assertEquals(3.9999678930515468, pixelPos.x, 1e-8);
        assertEquals(1.0000318067920972, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(0.220249, 18.332468), null);
        assertEquals(5.5, pixelPos.x, 1e-8);
        assertEquals(11.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(0.249989, 18.321519), null);
        assertEquals(6.089020455253739, pixelPos.x, 1e-8);
        assertEquals(11.728944031938612, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(0.277447, 18.310131), null);
        assertEquals(6.019331641460078, pixelPos.x, 1e-8);
        assertEquals(11.96583342011602, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(0.310039, 18.296767), null);
        assertEquals(5.898035553271856, pixelPos.x, 1e-8);
        assertEquals(12.275567238309627, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(0.33992004, 18.284939), null);
        assertEquals(6.5, pixelPos.x, 1e-8);
        assertEquals(12.5, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_AMSRE_interpolating_euclidian_distance() {
        final Properties properties = createProperties(XYInterpolator.Type.EUCLIDIAN);
        inverse = new PixelGeoIndexInverse(true, properties);

        final GeoRaster geoRaster = TestData.get_AMSRE();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(-0.8298334, 18.600895), null);
        assertEquals(3.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(-0.7098208, 18.553856), null);
        assertEquals(4.5, pixelPos.x, 1e-8);
        assertEquals(1.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(-0.7698271, 18.5773755), null);
        assertEquals(3.9999229960689684, pixelPos.x, 1e-8);
        assertEquals(1.0000770039310343, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(0.220249, 18.332468), null);
        assertEquals(5.5, pixelPos.x, 1e-8);
        assertEquals(11.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(0.249989, 18.321519), null);
        assertEquals(6.235813880526195, pixelPos.x, 1e-8);
        assertEquals(11.578498905965025, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(0.277447, 18.310131), null);
        assertEquals(6.055665307086948, pixelPos.x, 1e-8);
        assertEquals(11.927563349258604, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(0.310039, 18.296767), null);
        assertEquals(5.740880542320658, pixelPos.x, 1e-8);
        assertEquals(12.426339834063208, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(0.33992004, 18.284939), null);
        assertEquals(6.5, pixelPos.x, 1e-8);
        assertEquals(12.5, pixelPos.y, 1e-8);
    }

    @Test
    public void testDispose() {
        // un-initialized
        inverse.dispose();

        final GeoRaster geoRaster = TestData.get_AMSRE();
        inverse.initialize(geoRaster, false, new PixelPos[0]);
        inverse.dispose();
    }

    @Test
    public void testClone() {
        final GeoRaster geoRaster = TestData.get_OLCI();

        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final GeoPos geoPos = new GeoPos(66.42871, -24.082217);

        PixelPos pixelPos = inverse.getPixelPos(geoPos, null);
        assertEquals(25.5, pixelPos.x, 1e-8);
        assertEquals(35.5, pixelPos.y, 1e-8);

        final InverseCoding clone = inverse.clone();
        pixelPos = clone.getPixelPos(geoPos, null);
        assertEquals(25.5, pixelPos.x, 1e-8);
        assertEquals(35.5, pixelPos.y, 1e-8);
    }

    @Test
    public void testClone_disposeOriginal() {
        final GeoRaster geoRaster = TestData.get_OLCI();

        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final GeoPos geoPos = new GeoPos(66.51111, -24.15865);

        PixelPos pixelPos = inverse.getPixelPos(geoPos, null);
        assertEquals(5.5, pixelPos.x, 1e-8);
        assertEquals(6.5, pixelPos.y, 1e-8);

        final InverseCoding clone = inverse.clone();
        inverse.dispose();

        pixelPos = clone.getPixelPos(geoPos, null);
        assertEquals(5.5, pixelPos.x, 1e-8);
        assertEquals(6.5, pixelPos.y, 1e-8);
    }

    @Test
    public void testClone_interpolating_geodetic_distance() {
        final Properties properties = createProperties(XYInterpolator.Type.GEODETIC);
        inverse = new PixelGeoIndexInverse(true, properties);

        final GeoRaster geoRaster = TestData.get_AMSRE();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final GeoPos geoPos = new GeoPos(-0.8398334, 18.610895);

        PixelPos pixelPos = inverse.getPixelPos(geoPos, null);
        assertEquals(3.1083222743108525, pixelPos.x, 1e-8);
        assertEquals(0.6808134209816874, pixelPos.y, 1e-8);

        final InverseCoding clone = inverse.clone();
        pixelPos = clone.getPixelPos(geoPos, null);
        assertEquals(3.1083222743108525, pixelPos.x, 1e-8);
        assertEquals(0.6808134209816874, pixelPos.y, 1e-8);
    }

    @Test
    public void testClone_interpolating_euclidian_distance() {
        final Properties properties = createProperties(XYInterpolator.Type.EUCLIDIAN);
        inverse = new PixelGeoIndexInverse(true, properties);

        final GeoRaster geoRaster = TestData.get_AMSRE();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final GeoPos geoPos = new GeoPos(-0.8398334, 18.610895);

        PixelPos pixelPos = inverse.getPixelPos(geoPos, null);
        assertEquals(3.260397013190891, pixelPos.x, 1e-8);
        assertEquals(0.5441815032869551, pixelPos.y, 1e-8);

        final InverseCoding clone = inverse.clone();
        pixelPos = clone.getPixelPos(geoPos, null);
        assertEquals(3.260397013190891, pixelPos.x, 1e-8);
        assertEquals(0.5441815032869551, pixelPos.y, 1e-8);
    }

    @Test
    public void testClone_interpolating_geodetic_distance_disposeOriginal() {
        final Properties properties = createProperties(XYInterpolator.Type.GEODETIC);
        inverse = new PixelGeoIndexInverse(true, properties);

        final GeoRaster geoRaster = TestData.get_AMSRE();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final GeoPos geoPos = new GeoPos(-0.8398334, 18.610895);

        PixelPos pixelPos = inverse.getPixelPos(geoPos, null);
        assertEquals(3.1083222743108525, pixelPos.x, 1e-8);
        assertEquals(0.6808134209816874, pixelPos.y, 1e-8);

        final InverseCoding clone = inverse.clone();
        inverse.dispose();

        pixelPos = clone.getPixelPos(geoPos, null);
        assertEquals(3.1083222743108525, pixelPos.x, 1e-8);
        assertEquals(0.6808134209816874, pixelPos.y, 1e-8);
    }

    @Test
    public void testClone_interpolating_euclidian_distance_disposeOriginal() {
        final Properties properties = createProperties(XYInterpolator.Type.EUCLIDIAN);
        inverse = new PixelGeoIndexInverse(true, properties);

        final GeoRaster geoRaster = TestData.get_AMSRE();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final GeoPos geoPos = new GeoPos(-0.8398334, 18.610895);

        PixelPos pixelPos = inverse.getPixelPos(geoPos, null);
        assertEquals(3.260397013190891, pixelPos.x, 1e-8);
        assertEquals(0.5441815032869551, pixelPos.y, 1e-8);

        final InverseCoding clone = inverse.clone();
        inverse.dispose();

        pixelPos = clone.getPixelPos(geoPos, null);
        assertEquals(3.260397013190891, pixelPos.x, 1e-8);
        assertEquals(0.5441815032869551, pixelPos.y, 1e-8);
    }

    @Test
    public void testGeoPixelPos_OLCI_invalid_geoPos() {
        final GeoRaster geoRaster = TestData.get_OLCI();

        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final PixelPos pixelPos = inverse.getPixelPos(new GeoPos(Double.NaN, -24.046906), null);
        assertEquals(Double.NaN, pixelPos.x, 1e-8);
        assertEquals(Double.NaN, pixelPos.y, 1e-8);
    }

    @Test
    public void testToIndex_50km() {
        final GeoRaster geoRaster = new GeoRaster(new double[0], new double[0], null, null, 0, 0,
                                                  50.0);
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        assertEquals(18000090L, inverse.toIndex(0.0, 0.0));
        assertEquals(90L, inverse.toIndex(-180.0, 0.0));
        assertEquals(0L, inverse.toIndex(-180.0, -90.0));
        assertEquals(36000180L, inverse.toIndex(180.0, 90.0));
        assertEquals(16500090L, inverse.toIndex(-14.53, 0.0));
    }

    @Test
    public void testToIndex_5km() {
        final GeoRaster geoRaster = new GeoRaster(new double[0], new double[0], null, null, 0, 0,
                                                  5.0);
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        assertEquals(120000600L, inverse.toIndex(0.0, 0.0));
        assertEquals(600L, inverse.toIndex(-180.0, 0.0));
        assertEquals(0L, inverse.toIndex(-180.0, -90.0));
        assertEquals(240001200L, inverse.toIndex(180.0, 90.0));
        assertEquals(110300600L, inverse.toIndex(-14.53, 0.0));
    }

    @Test
    public void testToIndex_300m() {
        final GeoRaster geoRaster = new GeoRaster(new double[0], new double[0], null, null, 0, 0,
                                                  0.3);
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        assertEquals(1800009000L, inverse.toIndex(0.0, 0.0));
        assertEquals(9000L, inverse.toIndex(-180.0, 0.0));
        assertEquals(0L, inverse.toIndex(-180.0, -90.0));
        assertEquals(3600018000L, inverse.toIndex(180.0, 90.0));
        assertEquals(1654709000L, inverse.toIndex(-14.53, 0.0));
    }

    @Test
    public void testGetMultiplicator() {
        double multiplicator = PixelGeoIndexInverse.getMultiplicator(36.8);
        assertEquals(1.0, multiplicator, 1e-8);

        multiplicator = PixelGeoIndexInverse.getMultiplicator(13.6);
        assertEquals(2.450980392156863, multiplicator, 1e-8);

        multiplicator = PixelGeoIndexInverse.getMultiplicator(9.76);
        assertEquals(3.4153005464480874, multiplicator, 1e-8);

        multiplicator = PixelGeoIndexInverse.getMultiplicator(5.46);
        assertEquals(6.1050061050061055, multiplicator, 1e-8);

        multiplicator = PixelGeoIndexInverse.getMultiplicator(0.94);
        assertEquals(35.46099290780142, multiplicator, 1e-8);

        multiplicator = PixelGeoIndexInverse.getMultiplicator(0.35);
        assertEquals(95.23809523809526, multiplicator, 1e-8);

        multiplicator = PixelGeoIndexInverse.getMultiplicator(0.28);
        assertEquals(100.0, multiplicator, 1e-8);
    }

    @Test
    public void testRasterRegion_constructor() {
        final PixelGeoIndexInverse.RasterRegion rasterRegion = new PixelGeoIndexInverse.RasterRegion(14, 17);
        assertEquals(14, rasterRegion.min_x);
        assertEquals(14, rasterRegion.max_x);
        assertEquals(17, rasterRegion.min_y);
        assertEquals(17, rasterRegion.max_y);
    }

    @Test
    public void testRasterRegion_extend() {
        final PixelGeoIndexInverse.RasterRegion rasterRegion = new PixelGeoIndexInverse.RasterRegion(100, 200);

        rasterRegion.extend(100, 202);
        assertEquals(100, rasterRegion.min_x);
        assertEquals(100, rasterRegion.max_x);
        assertEquals(200, rasterRegion.min_y);
        assertEquals(202, rasterRegion.max_y);

        rasterRegion.extend(100, 199);
        assertEquals(100, rasterRegion.min_x);
        assertEquals(100, rasterRegion.max_x);
        assertEquals(199, rasterRegion.min_y);
        assertEquals(202, rasterRegion.max_y);

        rasterRegion.extend(98, 200);
        assertEquals(98, rasterRegion.min_x);
        assertEquals(100, rasterRegion.max_x);
        assertEquals(199, rasterRegion.min_y);
        assertEquals(202, rasterRegion.max_y);

        rasterRegion.extend(101, 200);
        assertEquals(98, rasterRegion.min_x);
        assertEquals(101, rasterRegion.max_x);
        assertEquals(199, rasterRegion.min_y);
        assertEquals(202, rasterRegion.max_y);
    }

    @Test
    public void testRasterRegion_isPoint() {
        final PixelGeoIndexInverse.RasterRegion rasterRegion = new PixelGeoIndexInverse.RasterRegion(200, 270);

        assertTrue(rasterRegion.isPoint());

        rasterRegion.extend(200, 271);
        assertFalse(rasterRegion.isPoint());
    }

    @Test
    public void testPlugin_create() {
        final PixelGeoIndexInverse.Plugin plugin = new PixelGeoIndexInverse.Plugin(false);

        final InverseCoding inverseCoding = plugin.create();
        assertTrue(inverseCoding instanceof PixelGeoIndexInverse);
    }

    @Test
    public void testPlugin_create_interpolating() {
        final PixelGeoIndexInverse.Plugin plugin = new PixelGeoIndexInverse.Plugin(true);

        final InverseCoding inverseCoding = plugin.create();
        assertTrue(inverseCoding instanceof PixelGeoIndexInverse);
    }

    private Properties createProperties(XYInterpolator.Type type) {
        final Properties properties = new Properties();
        properties.setProperty(SYSPROP_GEOCODING_INTERPOLATOR, type.name());
        return properties;
    }

}
