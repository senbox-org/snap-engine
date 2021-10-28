package org.esa.snap.core.dataio.geocoding.forward;

import org.esa.snap.core.dataio.geocoding.ForwardCoding;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.TestData;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PixelInterpolatingForwardTest {

    private ForwardCoding pixelForward;

    @Before
    public void setUp() {
        pixelForward = new PixelInterpolatingForward();
    }

    @Test
    public void testGetGeoPos_SLSTR_OL() {
        final GeoRaster geoRaster = TestData.get_SLSTR_OL();

        pixelForward.initialize(geoRaster, false, new PixelPos[0]);

        GeoPos geoPos = pixelForward.getGeoPos(new PixelPos(0.5, 0.5), null);
        assertEquals(-130.350693, geoPos.lon, 1e-8);
        assertEquals(45.855048, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(0.4, 0.4), null);
        assertEquals(-130.35102159, geoPos.lon, 1e-8);
        assertEquals(45.85536669999999, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(0.6, 0.6), null);
        assertEquals(-130.35036439, geoPos.lon, 1e-8);
        assertEquals(45.8547293, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(0.5, 0.1), null);
        assertEquals(-130.35037780000002, geoPos.lon, 1e-8);
        assertEquals(45.856078, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(0.5, 0.9), null);
        assertEquals(-130.3510082, geoPos.lon, 1e-8);
        assertEquals(45.854017999999996, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(31.5, 0.5), null);
        assertEquals(-130.245281, geoPos.lon, 1e-8);
        assertEquals(45.839166999999996, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(31.9, 0.5), null);
        assertEquals(-130.24366540000003, geoPos.lon, 1e-8);
        assertEquals(45.838922999999994, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(31.9, 0.95), null);
        assertEquals(-130.2440218, geoPos.lon, 1e-8);
        assertEquals(45.837764699999994, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(30.5, 25.5), null);
        assertEquals(-130.269123, geoPos.lon, 1e-8);
        assertEquals(45.775419, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(0.5, 25.5), null);
        assertEquals(-130.37037999999998, geoPos.lon, 1e-8);
        assertEquals(45.790684, geoPos.lat, 1e-8);
    }

    @Test
    public void testGetGeoPos_AMSUB_anti_meridian() {
        final GeoRaster geoRaster = TestData.get_AMSUB();

        pixelForward.initialize(geoRaster, true, new PixelPos[0]);

        GeoPos geoPos = pixelForward.getGeoPos(new PixelPos(24.5, 4.5), null);
        assertEquals(179.8498, geoPos.lon, 1e-8);
        assertEquals(-71.8596, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(24.7, 4.5), null);
        assertEquals(179.9314, geoPos.lon, 1e-8);
        assertEquals(-71.84114, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(24.9, 4.5), null);
        assertEquals(-179.987, geoPos.lon, 1e-8);
        assertEquals(-71.82268, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(25.1, 4.5), null);
        assertEquals(-179.9054, geoPos.lon, 1e-8);
        assertEquals(-71.80422, geoPos.lat, 1e-8);
    }

    @Test
    public void testGetGeoPos_AMSUB_border() {
        final GeoRaster geoRaster = TestData.get_AMSUB();

        pixelForward.initialize(geoRaster, true, new PixelPos[0]);

        GeoPos geoPos = pixelForward.getGeoPos(new PixelPos(0.0, 0.0), null);
        assertEquals(170.53682499999996, geoPos.lon, 1e-8);
        assertEquals(-74.3491, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(0.0, 31.0), null);
        assertEquals(164.019525, geoPos.lon, 1e-8);
        assertEquals(-70.02935, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(31.0, 31.0), null);
        assertEquals(175.30530108032227, geoPos.lon, 1e-8);
        assertEquals(-68.0219246887207, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(31.0, 0.0), null);
        assertEquals(-176.44617499999998, geoPos.lon, 1e-8);
        assertEquals(-71.87672500000001, geoPos.lat, 1e-8);
    }

    @Test
    public void testGetGeoPos_SLSTR_OL_outside() {
        final GeoRaster geoRaster = TestData.get_SLSTR_OL();

        pixelForward.initialize(geoRaster, false, new PixelPos[0]);

        GeoPos geoPos = pixelForward.getGeoPos(new PixelPos(-0.1, 0.45), null);
        assertEquals(Double.NaN, geoPos.lon, 1e-8);
        assertEquals(Double.NaN, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(1.75, -0.01), null);
        assertEquals(Double.NaN, geoPos.lon, 1e-8);
        assertEquals(Double.NaN, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(32.001, 1.6), null);
        assertEquals(Double.NaN, geoPos.lon, 1e-8);
        assertEquals(Double.NaN, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(1.5, 27.01), null);
        assertEquals(Double.NaN, geoPos.lon, 1e-8);
        assertEquals(Double.NaN, geoPos.lat, 1e-8);
    }

    @Test
    public void testGetGeoPos_SLSTR_OL_invalid_pixelpos() {
        final GeoRaster geoRaster = TestData.get_SLSTR_OL();

        pixelForward.initialize(geoRaster, false, new PixelPos[0]);

        GeoPos geoPos = pixelForward.getGeoPos(new PixelPos(Double.NaN, 1.3), null);
        assertEquals(Double.NaN, geoPos.lon, 1e-8);
        assertEquals(Double.NaN, geoPos.lat, 1e-8);
    }

    @Test
    public void testDispose() {
        pixelForward.dispose();

        final GeoRaster geoRaster = TestData.get_SLSTR_OL();
        pixelForward.initialize(geoRaster, false, new PixelPos[0]);
        pixelForward.dispose();
    }

    @Test
    public void testClone() {
        final GeoRaster geoRaster = TestData.get_AMSR_2_anti_meridian();

        pixelForward.initialize(geoRaster, true, new PixelPos[0]);

        GeoPos geoPos = pixelForward.getGeoPos(new PixelPos(1.0, 1.0), null);
        assertEquals(-176.78545499999998, geoPos.lon, 1e-8);
        assertEquals(-70.62587400000001, geoPos.lat, 1e-8);

        final ForwardCoding clone = pixelForward.clone();
        geoPos = clone.getGeoPos(new PixelPos(1.0, 1.0), null);
        assertEquals(-176.78545499999998, geoPos.lon, 1e-8);
        assertEquals(-70.62587400000001, geoPos.lat, 1e-8);
    }

    @Test
    public void testClone_disposeOriginal() {
        final GeoRaster geoRaster = TestData.get_AMSR_2_anti_meridian();

        pixelForward.initialize(geoRaster, true, new PixelPos[0]);

        GeoPos geoPos = pixelForward.getGeoPos(new PixelPos(2.0, 2.0), null);
        assertEquals(-177.125365, geoPos.lon, 1e-8);
        assertEquals(-70.55756975, geoPos.lat, 1e-8);

        final ForwardCoding clone = pixelForward.clone();
        pixelForward.dispose();

        geoPos = clone.getGeoPos(new PixelPos(2.0, 2.0), null);
        assertEquals(-177.125365, geoPos.lon, 1e-8);
        assertEquals(-70.55756975, geoPos.lat, 1e-8);
    }

    @Test
    public void testPlugin_create() {
        final PixelInterpolatingForward.Plugin plugin = new PixelInterpolatingForward.Plugin();

        final ForwardCoding forwardCoding = plugin.create();
        assertTrue(forwardCoding instanceof PixelInterpolatingForward);
    }

    @Test
    public void testInterpolationX() {
        final GeoRaster geoRaster = TestData.get_FlatEarth();

        pixelForward.initialize(geoRaster, false, new PixelPos[0]);

        final double[] pixelPosX = {
                5.5, 5.6, 5.7, 5.8, 5.9, 6.0, 6.1, 6.2, 6.3, 6.4, 6.5
        };
        final double[] expGeoLon = {
                25.0, 25.1, 25.2, 25.3, 25.4, 25.5, 25.6, 25.7, 25.8, 25.9, 26.0
        };
        GeoPos geoPos = null;
        for (int i = 0; i <= 10; i++) {
            final double x = pixelPosX[i];
            geoPos = pixelForward.getGeoPos(new PixelPos(x, 5.5), null);
            assertEquals(expGeoLon[i], geoPos.lon, 1e-8);
            assertEquals(44.0, geoPos.lat, 1e-8);
        }
    }

    @Test
    public void testInterpolationY() {
        final GeoRaster geoRaster = TestData.get_FlatEarth();

        pixelForward.initialize(geoRaster, false, new PixelPos[0]);

        final double[] pixelPosY = {
                5.5, 5.6, 5.7, 5.8, 5.9, 6.0, 6.1, 6.2, 6.3, 6.4, 6.5
        };
        final double[] expGeoLat = {
                44.0, 43.9, 43.8, 43.7, 43.6, 43.5, 43.4, 43.3, 43.2, 43.1, 43.0
        };
        GeoPos geoPos = null;
        for (int i = 0; i <= 10; i++) {
            final double y = pixelPosY[i];
            geoPos = pixelForward.getGeoPos(new PixelPos(5.5, y), null);
            assertEquals(25.0, geoPos.lon, 1e-8);
            assertEquals(expGeoLat[i], geoPos.lat, 1e-8);
        }
    }
}
