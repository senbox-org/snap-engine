package org.esa.snap.core.dataio.geocoding.inverse;

import org.esa.snap.core.dataio.geocoding.AMSR2;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.InverseCoding;
import org.esa.snap.core.dataio.geocoding.TestData;
import org.esa.snap.core.dataio.geocoding.util.XYInterpolator;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Properties;

import static java.lang.Double.NaN;
import static org.esa.snap.core.dataio.geocoding.TestData.get_SLSTR_OL;
import static org.esa.snap.core.dataio.geocoding.util.XYInterpolator.SYSPROP_GEOCODING_INTERPOLATOR;
import static org.junit.Assert.*;

public class PixelQuadTreeInverseTest {

    @Test
    public void testGetPixelPos_SLSTR_OL() {
        final GeoRaster geoRaster = get_SLSTR_OL();
        PixelQuadTreeInverse inverse = new PixelQuadTreeInverse();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(45.836541, -130.33507), null);
        assertEquals(6.5, pixelPos.x, 1e-8);
        assertEquals(6.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(45.819392, -130.310602), null);
        assertEquals(14.5, pixelPos.x, 1e-8);
        assertEquals(11.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(45.8391669999999962, -130.245281), null);
        assertEquals(31.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(45.774808, -130.265089), null);
        assertEquals(31.5, pixelPos.x, 1e-8);
        assertEquals(25.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(45.790684, -130.37037999999998), null);
        assertEquals(0.5, pixelPos.x, 1e-8);
        assertEquals(25.5, pixelPos.y, 1e-8);
    }

    // @todo 1 tb/tb reactivate and solve geo-location fill value issue 2021-03-17
    /*
    @Test
    public void testGeoPixelPos_SYN_AOD_fillValues() {
        final GeoRaster geoRaster = TestData.get_SYN_AOD();
        PixelQuadTreeInverse inverse = new PixelQuadTreeInverse();
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
    }*/

    @Test
    public void testGetPixelPos_SLSTR_OL_outside() {
        final GeoRaster geoRaster = get_SLSTR_OL();
        PixelQuadTreeInverse inverse = new PixelQuadTreeInverse();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(45.856, -130.358), null);
        assertEquals(NaN, pixelPos.x, 1e-8);
        assertEquals(NaN, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(45.838, -130.23), null);
        assertEquals(NaN, pixelPos.x, 1e-8);
        assertEquals(NaN, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(45.773, -130.24), null);
        assertEquals(NaN, pixelPos.x, 1e-8);
        assertEquals(NaN, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(45.791, -130.38), null);
        assertEquals(NaN, pixelPos.x, 1e-8);
        assertEquals(NaN, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_AMSR2() {
        final GeoRaster geoRaster = TestData.get_AMSR_2_anti_meridian();
        PixelQuadTreeInverse inverse = new PixelQuadTreeInverse();
        inverse.initialize(geoRaster, true, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(-70.659836, -176.61472), null);
        assertEquals(0.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(-70.275215, 179.66467), null);
        assertEquals(31.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(-68.75776, 174.80211), null);
        assertEquals(31.5, pixelPos.x, 1e-8);
        assertEquals(25.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(-69.24617, 178.1685), null);
        assertEquals(0.5, pixelPos.x, 1e-8);
        assertEquals(25.5, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_OLCI_interpolating_geodetic_distance() {
        final Properties properties = createProperties(XYInterpolator.Type.GEODETIC);
        PixelQuadTreeInverse inverse = new PixelQuadTreeInverse(true, properties);

        final GeoRaster geoRaster = TestData.get_OLCI();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(66.495834, -24.168955), null);
        assertEquals(5.5, pixelPos.x, 1e-8);
        assertEquals(12.5, pixelPos.y, 1e-8);

        // interpolate in y direction between replicated pixels
        pixelPos = inverse.getPixelPos(new GeoPos(66.49456, -24.169599), null);
        assertEquals(6, pixelPos.x, 1e-8);
        assertEquals(13.750093939267787, pixelPos.y, 1e-7);

        // interpolate in lat-direction
        pixelPos = inverse.getPixelPos(new GeoPos(66.44213, -24.22587), null);
        assertEquals(0.5, pixelPos.x, 1e-8);    // duplicated pixel, the left of a pair
        assertEquals(34.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.4419375, -24.22587), null);
        assertEquals(1.0, pixelPos.x, 1e-8);
        assertEquals(34.574172929186844, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.441745, -24.22587), null);
        assertEquals(1.0, pixelPos.x, 1e-8);
        assertEquals(34.64807215085877, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.4415525, -24.22587), null);
        assertEquals(1.0, pixelPos.x, 1e-8);
        assertEquals(34.72162155510092, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.44136, -24.22587), null);
        assertEquals(1.0, pixelPos.x, 1e-8);
        assertEquals(34.794716527858334, pixelPos.y, 1e-8);

        // interpolate in lon-direction
        pixelPos = inverse.getPixelPos(new GeoPos(66.44213, -24.22587), null);
        assertEquals(0.5, pixelPos.x, 1e-8);    // duplicated pixel, the left of a pair
        assertEquals(34.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.44213, -24.2234825), null);
        assertEquals(1.0, pixelPos.x, 1e-8);
        assertEquals(34.74547419281143, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.44213, -24.221095), null);
        assertEquals(1.0, pixelPos.x, 1e-8);
        assertEquals(34.85228235368744, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.44213, -24.2187075), null);
        assertEquals(3.0, pixelPos.x, 1e-8);
        assertEquals(34.15289103293384, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.44213, -24.21632), null);
        assertEquals(3.0, pixelPos.x, 1e-8);
        assertEquals(34.20636166925025, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_OLCI_interpolating_euclidian_distance() {
        final Properties properties = createProperties(XYInterpolator.Type.EUCLIDIAN);
        PixelQuadTreeInverse inverse = new PixelQuadTreeInverse(true, properties);

        final GeoRaster geoRaster = TestData.get_OLCI();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(66.495834, -24.168955), null);
        assertEquals(5.5, pixelPos.x, 1e-8);
        assertEquals(12.5, pixelPos.y, 1e-8);

        // interpolate in y direction between replicated pixels
        pixelPos = inverse.getPixelPos(new GeoPos(66.49456, -24.169599), null);
        assertEquals(6, pixelPos.x, 1e-8);
        assertEquals(13.600075105777332, pixelPos.y, 1e-8);

        // interpolate in lat-direction
        pixelPos = inverse.getPixelPos(new GeoPos(66.44213, -24.22587), null);
        assertEquals(0.5, pixelPos.x, 1e-8);    // duplicated pixel, the left of a pair
        assertEquals(34.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.4419375, -24.22587), null);
        assertEquals(1.0, pixelPos.x, 1e-8);
        assertEquals(34.50515290169002, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.441745, -24.22587), null);
        assertEquals(1.0, pixelPos.x, 1e-8);
        assertEquals(34.523031417822665, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.4415525, -24.22587), null);
        assertEquals(1.0, pixelPos.x, 1e-8);
        assertEquals(34.557220534187024, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.44136, -24.22587), null);
        assertEquals(1.0, pixelPos.x, 1e-8);
        assertEquals(34.61042358404912, pixelPos.y, 1e-8);

        // interpolate in lon-direction
        pixelPos = inverse.getPixelPos(new GeoPos(66.44213, -24.22587), null);
        assertEquals(0.5, pixelPos.x, 1e-8);    // duplicated pixel, the left of a pair
        assertEquals(34.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.44213, -24.2234825), null);
        assertEquals(1.0, pixelPos.x, 1e-8);
        assertEquals(34.722514707184274, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.44213, -24.221095), null);
        assertEquals(1.0, pixelPos.x, 1e-8);
        assertEquals(34.8457811173924, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.44213, -24.2187075), null);
        assertEquals(3.0, pixelPos.x, 1e-8);
        assertEquals(34.22551392238872, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.44213, -24.21632), null);
        assertEquals(3.0, pixelPos.x, 1e-8);
        assertEquals(34.39030170084996, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_AMSRE_interpolating_geodetic_distance() {
        final Properties properties = createProperties(XYInterpolator.Type.GEODETIC);
        PixelQuadTreeInverse inverse = new PixelQuadTreeInverse(true, properties);

        final GeoRaster geoRaster = TestData.get_AMSRE();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(-0.8298334, 18.600895), null);
        assertEquals(3.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(-0.7098208, 18.553856), null);
        assertEquals(4.5, pixelPos.x, 1e-8);
        assertEquals(1.5, pixelPos.y, 1e-8);

        // @todo 1 tb/tb this is suspicious! 2020-01-10
        pixelPos = inverse.getPixelPos(new GeoPos(-0.7698271, 18.5773755), null);
        assertEquals(2.9929638622796446, pixelPos.x, 1e-8);
        assertEquals(1.7308288269813148, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_AMSRE_interpolating_euclidian_distance() {
        final Properties properties = createProperties(XYInterpolator.Type.EUCLIDIAN);
        PixelQuadTreeInverse inverse = new PixelQuadTreeInverse(true, properties);

        final GeoRaster geoRaster = TestData.get_AMSRE();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(-0.8298334, 18.600895), null);
        assertEquals(3.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(-0.7098208, 18.553856), null);
        assertEquals(4.5, pixelPos.x, 1e-8);
        assertEquals(1.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(-0.7698271, 18.5773755), null);
        assertEquals(3.01067027735143, pixelPos.x, 1e-8);
        assertEquals(1.584114535168603, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_SLSTR_OL_invalid_geo_pos() {
        final GeoRaster geoRaster = get_SLSTR_OL();
        PixelQuadTreeInverse inverse = new PixelQuadTreeInverse();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final PixelPos pixelPos = inverse.getPixelPos(new GeoPos(NaN, -130.33507), null);
        assertEquals(NaN, pixelPos.x, 1e-8);
        assertEquals(NaN, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetGeoPos_AMSR2() {
        final GeoRaster geoRaster = new GeoRaster(AMSR2.AMSR2_ANTI_MERID_LON, AMSR2.AMSR2_ANTI_MERID_LAT, null, null, 32, 26,
                32, 26, 0.3, 0.5, 0.5, 1.0, 1.0);
        PixelQuadTreeInverse inverse = new PixelQuadTreeInverse();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final GeoPos geoPos = new GeoPos();
        inverse.getGeoPos(3, 5, geoPos);
        assertEquals(-178.06575, geoPos.lon, 1e-8);
        assertEquals(-70.34471, geoPos.lat, 1e-8);
    }

    @Test
    public void testGetGeoPos_SLST_OL() {
        final GeoRaster geoRaster = get_SLSTR_OL();
        final PixelQuadTreeInverse inverse = new PixelQuadTreeInverse();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final GeoPos geoPos = new GeoPos();
        inverse.getGeoPos(1, 3, geoPos);
        assertEquals(-130.34898099999998, geoPos.lon, 1e-8);
        assertEquals(45.846712, geoPos.lat, 1e-8);
    }

    @Test
    public void testMSI_projected_L1() {
        final GeoRaster geoRaster = TestData.get_MSI_L1();

        final PixelQuadTreeInverse inverse = new PixelQuadTreeInverse();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        // check segmentation - this was failing before the fix tb 2021-07-28
        final ArrayList<Segment> segmentList = inverse.getSegmentList();
        assertEquals(3, segmentList.size());

        Segment segment = segmentList.get(0);
        assertEquals(0, segment.x_min);
        assertEquals(3, segment.x_max);
        assertEquals(0, segment.y_min);
        assertEquals(15, segment.y_max);
        assertEquals(28.9297100620607, segment.lon_min, 1e-8);
        assertEquals(29.98484832633402, segment.lon_max, 1e-8);
        assertEquals(44.85622580870736, segment.lat_min, 1e-8);
        assertEquals(47.86594330232314, segment.lat_max, 1e-8);

        segment = segmentList.get(1);
        assertEquals(4, segment.x_min);
        assertEquals(14, segment.x_max);
        assertEquals(0, segment.y_min);
        assertEquals(15, segment.y_max);
        assertEquals(30.11143503969883, segment.lon_min, 1e-8);
        assertEquals(33.07239877701666, segment.lon_max, 1e-8);
        assertEquals(44.88855397295411, segment.lat_min, 1e-8);
        assertEquals(47.91009255076986, segment.lat_max, 1e-8);

        segment = segmentList.get(2);
        assertEquals(15, segment.x_min);
        assertEquals(24, segment.x_max);
        assertEquals(0, segment.y_min);
        assertEquals(15, segment.y_max);
        assertEquals(33.34907077400352, segment.lon_min, 1e-8);
        assertEquals(36.03306268790175, segment.lon_max, 1e-8);
        assertEquals(44.88519183667283, segment.lat_min, 1e-8);
        assertEquals(47.90952354992115, segment.lat_max, 1e-8);

        // convert some points on the first scanline
        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(47.83791144390204, 28.9297100620607), null);
        assertEquals(0.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(47.86594330232314, 29.81579432123888), null);
        assertEquals(3.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        // this one in the second segment
        pixelPos = inverse.getPixelPos(new GeoPos(47.87376876944839, 30.11143503969883), null);
        assertEquals(4.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetEpsilon_AMSR2() {
        final GeoRaster geoRaster = new GeoRaster(AMSR2.AMSR2_ANTI_MERID_LON, AMSR2.AMSR2_ANTI_MERID_LAT, null, null, 32, 26,
                32, 26, 5.0, 0.5, 0.5, 1.0, 1.0);
        PixelQuadTreeInverse inverse = new PixelQuadTreeInverse();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final double epsilon = inverse.getEpsilon(5.0);
        assertEquals(0.08993220524744582, epsilon, 1e-8);
    }

    @Test
    public void testGetEpsilon_SLSTR_OL() {
        PixelQuadTreeInverse inverse = new PixelQuadTreeInverse();
        final double epsilon = inverse.getEpsilon(0.3);
        assertEquals(0.005395932176867367, epsilon, 1e-8);
    }

    @Test
    public void testGetPositiveLonMin() {
        final double[] lons = new double[4];
        lons[0] = 160.0;
        lons[1] = 150.0;
        lons[2] = -169.0;
        lons[3] = 165.0;
        double result = PixelQuadTreeInverse.getPositiveLonMin(lons);
        assertEquals(150.0, result, 0.0);

        lons[0] = -175.0;
        lons[1] = 170.0;
        lons[2] = -169.0;
        lons[3] = -170.0;
        result = PixelQuadTreeInverse.getPositiveLonMin(lons);
        assertEquals(170.0, result, 0.0);

        lons[0] = 170.0;
        lons[1] = 160.0;
        lons[2] = -175.0;
        lons[3] = -165.0;
        result = PixelQuadTreeInverse.getPositiveLonMin(lons);
        assertEquals(160.0, result, 0.0);

        final double[] lonsLong = new double[8];
        lonsLong[0] = -150.0;
        lonsLong[1] = 160.0;
        lonsLong[2] = -140.0;
        lonsLong[3] = -170.0;
        lonsLong[4] = -180.0;
        lonsLong[5] = 170.0;
        lonsLong[6] = -140.0;
        lonsLong[7] = 180.0;
        result = PixelQuadTreeInverse.getPositiveLonMin(lonsLong);
        assertEquals(160.0, result, 0.0);

        lonsLong[0] = 140.0;
        lonsLong[1] = 160.0;
        lonsLong[2] = -150.0;
        lonsLong[3] = -170.0;
        lonsLong[4] = -163.0;
        lonsLong[5] = 170.0;
        lonsLong[6] = -155.0;
        lonsLong[7] = 138.0;
        result = PixelQuadTreeInverse.getPositiveLonMin(lonsLong);
        assertEquals(138.0, result, 0.0);
    }

    @Test
    public void testGetNegativeLonMax() {
        final double[] lons = new double[4];
        lons[0] = 160.0;
        lons[1] = -150.0;
        lons[2] = -169.0;
        lons[3] = 165.0;
        double result = PixelQuadTreeInverse.getNegativeLonMax(lons);
        assertEquals(-150.0, result, 0.0);

        lons[0] = -175.0;
        lons[1] = 170.0;
        lons[2] = -169.0;
        lons[3] = -170.0;
        result = PixelQuadTreeInverse.getNegativeLonMax(lons);
        assertEquals(-169.0, result, 0.0);

        lons[0] = 170.0;
        lons[1] = 160.0;
        lons[2] = -175.0;
        lons[3] = -165.0;
        result = PixelQuadTreeInverse.getNegativeLonMax(lons);
        assertEquals(-165.0, result, 0.0);

        final double[] lonsLong = new double[8];
        lonsLong[0] = -150.0;
        lonsLong[1] = 160.0;
        lonsLong[2] = -140.0;
        lonsLong[3] = -170.0;
        lonsLong[4] = -180.0;
        lonsLong[5] = 170.0;
        lonsLong[6] = -140.0;
        lonsLong[7] = 180.0;
        result = PixelQuadTreeInverse.getNegativeLonMax(lonsLong);
        assertEquals(-140.0, result, 0.0);

        lonsLong[0] = 140.0;
        lonsLong[1] = 160.0;
        lonsLong[2] = -150.0;
        lonsLong[3] = -170.0;
        lonsLong[4] = -163.0;
        lonsLong[5] = 170.0;
        lonsLong[6] = -155.0;
        lonsLong[7] = 138.0;
        result = PixelQuadTreeInverse.getNegativeLonMax(lonsLong);
        assertEquals(-150.0, result, 0.0);
    }

    @Test
    public void testSq() {
        assertEquals(4.0, PixelQuadTreeInverse.sq(2.0, 0.0), 1e-8);
        assertEquals(13.0, PixelQuadTreeInverse.sq(2.0, 3.0), 1e-8);
        assertEquals(16.0, PixelQuadTreeInverse.sq(0.0, 4.0), 1e-8);
    }

    @Test
    public void testIsCrossingAntiMeridianInsideQuad() {
        final double[] longitudes = new double[4];
        longitudes[0] = 176.0;
        longitudes[1] = 174.44;
        longitudes[2] = -172.98;
        longitudes[3] = -174.88;
        assertTrue(PixelQuadTreeInverse.isCrossingAntiMeridianInsideQuad(longitudes));

        longitudes[0] = 176.0;
        longitudes[1] = 174.44;
        longitudes[2] = 172.98;
        longitudes[3] = 174.88;
        assertFalse(PixelQuadTreeInverse.isCrossingAntiMeridianInsideQuad(longitudes));
    }

    @Test
    public void testGetMin() {
        final double[] latitudes = new double[4];
        latitudes[0] = -74.0;
        latitudes[1] = -71.0;
        latitudes[2] = -82.0;
        latitudes[3] = -79.0;

        double min = PixelQuadTreeInverse.getMin(latitudes, 0.02);
        assertEquals(-82.02, min, 1e-8);

        latitudes[0] = 23.0;
        latitudes[1] = 27.0;
        latitudes[2] = 19.0;
        latitudes[3] = 22.0;

        min = PixelQuadTreeInverse.getMin(latitudes, 0.02);
        assertEquals(18.98, min, 1e-8);
    }

    @Test
    public void testGetMax() {
        final double[] latitudes = new double[4];
        latitudes[0] = -75.0;
        latitudes[1] = -72.0;
        latitudes[2] = -81.0;
        latitudes[3] = -80.0;

        double min = PixelQuadTreeInverse.getMax(latitudes, 0.02);
        assertEquals(-71.98, min, 1e-8);

        latitudes[0] = 22.0;
        latitudes[1] = 26.0;
        latitudes[2] = 18.0;
        latitudes[3] = 21.0;

        min = PixelQuadTreeInverse.getMax(latitudes, 0.02);
        assertEquals(26.02, min, 1e-8);
    }

    @Test
    public void testDispose() {
        PixelQuadTreeInverse inverse = new PixelQuadTreeInverse();
        inverse.dispose();

        final GeoRaster geoRaster = get_SLSTR_OL();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        inverse.dispose();
    }

    @Test
    public void testClone() {
        final GeoRaster geoRaster = TestData.get_OLCI();
        final Properties properties = createProperties(XYInterpolator.Type.GEODETIC);
        PixelQuadTreeInverse inverse = new PixelQuadTreeInverse(true, properties);
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final GeoPos geoPos = new GeoPos(66.497604, -24.16638);
        PixelPos pixelPos = inverse.getPixelPos(geoPos, null);
        assertEquals(6.0, pixelPos.x, 1e-8);
        assertEquals(11.81158685631008, pixelPos.y, 1e-8);

        final InverseCoding clone = inverse.clone();
        pixelPos = clone.getPixelPos(geoPos, null);
        assertEquals(6.0, pixelPos.x, 1e-8);
        assertEquals(11.81158685631008, pixelPos.y, 1e-8);
    }

    @Test
    public void testClone_disposeOriginal() {
        final GeoRaster geoRaster = TestData.get_OLCI();
        PixelQuadTreeInverse inverse = new PixelQuadTreeInverse();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final GeoPos geoPos = new GeoPos(66.498, -24.168);

        PixelPos pixelPos = inverse.getPixelPos(geoPos, null);
        assertEquals(5.5, pixelPos.x, 1e-8);
        assertEquals(11.5, pixelPos.y, 1e-8);

        final InverseCoding clone = inverse.clone();
        inverse.dispose();

        pixelPos = clone.getPixelPos(geoPos, null);
        assertEquals(5.5, pixelPos.x, 1e-8);
        assertEquals(11.5, pixelPos.y, 1e-8);
    }

    @Test
    public void testClone_interpolating() {
        PixelQuadTreeInverse inverse = new PixelQuadTreeInverse(true);

        final GeoRaster geoRaster = TestData.get_AMSRE();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final GeoPos geoPos = new GeoPos(-0.8298334, 18.600895);

        PixelPos pixelPos = inverse.getPixelPos(geoPos, null);
        assertEquals(3.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        final InverseCoding clone = inverse.clone();
        pixelPos = clone.getPixelPos(geoPos, null);
        assertEquals(3.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);
    }

    @Test
    public void testClone_interpolating_disposeOriginal() {
        PixelQuadTreeInverse inverse = new PixelQuadTreeInverse(true);

        final GeoRaster geoRaster = TestData.get_AMSRE();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final GeoPos geoPos = new GeoPos(-0.8298334, 18.600895);

        PixelPos pixelPos = inverse.getPixelPos(geoPos, null);
        assertEquals(3.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        final InverseCoding clone = inverse.clone();
        inverse.dispose();

        pixelPos = clone.getPixelPos(geoPos, null);
        assertEquals(3.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);
    }

    @Test
    public void testPlugin_create() {
        final PixelQuadTreeInverse.Plugin plugin = new PixelQuadTreeInverse.Plugin(false);

        final InverseCoding inverseCoding = plugin.create();
        assertTrue(inverseCoding instanceof PixelQuadTreeInverse);
    }

    @Test
    public void testPlugin_create_interpolating() {
        final PixelQuadTreeInverse.Plugin plugin = new PixelQuadTreeInverse.Plugin(true);

        final InverseCoding inverseCoding = plugin.create();
        assertTrue(inverseCoding instanceof PixelQuadTreeInverse);
    }

    @Test
    public void testLongitudeEpsilonCreation() {
        //preparation
        final int epsilon = 20;

        //execution
        final double[] epsilonLongitude = PixelQuadTreeInverse.createEpsilonLongitude(epsilon);

        //verification
        assertEquals(901, epsilonLongitude.length);
        assertEquals(epsilon, epsilonLongitude[0], 1e-8);
        assertEquals(23.09401076758503, epsilonLongitude[300], 1e-8);
        assertEquals(39.99999999999999, epsilonLongitude[600], 1e-8);
        assertEquals(11459.161720383016, epsilonLongitude[899], 1e-8);
        // @todo 2 tb/** this is a ridiculous value - we should revise the way this is calculated
        assertEquals(3.2662478706390739E17, epsilonLongitude[900], 1e-8);
    }

    private Properties createProperties(XYInterpolator.Type type) {
        final Properties properties = new Properties();
        properties.setProperty(SYSPROP_GEOCODING_INTERPOLATOR, type.name());
        return properties;
    }

    @Test
    public void testGetPoleSegment_oneLocation() {
        final PixelPos[] poleLocations = {new PixelPos(200, 800)};

        final Segment poleSegment = PixelQuadTreeInverse.getPoleSegment(poleLocations, 1200, 2000);
        assertEquals(198, poleSegment.x_min);
        assertEquals(202, poleSegment.x_max);
        assertEquals(798, poleSegment.y_min);
        assertEquals(802, poleSegment.y_max);
    }

    @Test
    public void testGetPoleSegment_threeLocations() {
        final PixelPos[] poleLocations = {new PixelPos(300, 900), new PixelPos(299, 900), new PixelPos(300, 901)};

        final Segment poleSegment = PixelQuadTreeInverse.getPoleSegment(poleLocations, 1300, 2100);
        assertEquals(297, poleSegment.x_min);
        assertEquals(302, poleSegment.x_max);
        assertEquals(898, poleSegment.y_min);
        assertEquals(903, poleSegment.y_max);
    }

    @Test
    public void testGetPoleSegment_threeLocations_leftBorder() {
        final PixelPos[] poleLocations = {new PixelPos(3, 1000), new PixelPos(4, 1000), new PixelPos(3, 999)};

        final Segment poleSegment = PixelQuadTreeInverse.getPoleSegment(poleLocations, 1400, 2200);
        assertEquals(0, poleSegment.x_min);
        assertEquals(6, poleSegment.x_max);
        assertEquals(997, poleSegment.y_min);
        assertEquals(1002, poleSegment.y_max);
    }

    @Test
    public void testGetPoleSegment_threeLocations_upperBorder() {
        final PixelPos[] poleLocations = {new PixelPos(100, 3), new PixelPos(99, 3), new PixelPos(101, 3)};

        final Segment poleSegment = PixelQuadTreeInverse.getPoleSegment(poleLocations, 1400, 2200);
        assertEquals(97, poleSegment.x_min);
        assertEquals(103, poleSegment.x_max);
        assertEquals(0, poleSegment.y_min);
        assertEquals(5, poleSegment.y_max);
    }

    @Test
    public void testGetPoleSegment_threeLocations_rightBorder() {
        final PixelPos[] poleLocations = {new PixelPos(1498, 1800), new PixelPos(1497, 1801), new PixelPos(1496, 1799)};

        final Segment poleSegment = PixelQuadTreeInverse.getPoleSegment(poleLocations, 1500, 2300);
        assertEquals(1494, poleSegment.x_min);
        assertEquals(1499, poleSegment.x_max);
        assertEquals(1797, poleSegment.y_min);
        assertEquals(1803, poleSegment.y_max);
    }

    @Test
    public void testGetPoleSegment_threeLocations_lowerBorder() {
        final PixelPos[] poleLocations = {new PixelPos(1098, 2296), new PixelPos(1097, 2297), new PixelPos(1096, 2295)};

        final Segment poleSegment = PixelQuadTreeInverse.getPoleSegment(poleLocations, 1500, 2300);
        assertEquals(1094, poleSegment.x_min);
        assertEquals(1100, poleSegment.x_max);
        assertEquals(2293, poleSegment.y_min);
        assertEquals(2299, poleSegment.y_max);
    }

    @Test
    public void testRemoveSegment_fullyInside() {
        final Segment poleSegment = new Segment(10, 19, 100, 110);
        final Segment orbitSegment = new Segment(0, 999, 0, 1499);

        final Segment[] remaining = PixelQuadTreeInverse.removeSegment(poleSegment, orbitSegment, 1000, 1500);
        assertEquals(4, remaining.length);

        final Segment upper = remaining[0];
        assertEquals(0, upper.x_min);
        assertEquals(999, upper.x_max);
        assertEquals(0, upper.y_min);
        assertEquals(99, upper.y_max);

        final Segment lower = remaining[1];
        assertEquals(0, lower.x_min);
        assertEquals(999, lower.x_max);
        assertEquals(111, lower.y_min);
        assertEquals(1499, lower.y_max);

        final Segment left = remaining[2];
        assertEquals(0, left.x_min);
        assertEquals(9, left.x_max);
        assertEquals(100, left.y_min);
        assertEquals(110, left.y_max);

        final Segment right = remaining[3];
        assertEquals(20, right.x_min);
        assertEquals(999, right.x_max);
        assertEquals(100, right.y_min);
        assertEquals(110, right.y_max);
    }

    @Test
    public void testRemoveSegment_atLeftBorder() {
        final Segment poleSegment = new Segment(0, 9, 110, 119);
        final Segment orbitSegment = new Segment(0, 999, 0, 1499);

        final Segment[] remaining = PixelQuadTreeInverse.removeSegment(poleSegment, orbitSegment, 1000, 1500);
        assertEquals(3, remaining.length);

        final Segment upper = remaining[0];
        assertEquals(0, upper.x_min);
        assertEquals(999, upper.x_max);
        assertEquals(0, upper.y_min);
        assertEquals(109, upper.y_max);

        final Segment lower = remaining[1];
        assertEquals(0, lower.x_min);
        assertEquals(999, lower.x_max);
        assertEquals(120, lower.y_min);
        assertEquals(1499, lower.y_max);

        final Segment right = remaining[2];
        assertEquals(10, right.x_min);
        assertEquals(999, right.x_max);
        assertEquals(110, right.y_min);
        assertEquals(119, right.y_max);
    }

    @Test
    public void testRemoveSegment_atTopBorder() {
        final Segment poleSegment = new Segment(400, 409, 0, 9);
        final Segment orbitSegment = new Segment(0, 999, 0, 1499);

        final Segment[] remaining = PixelQuadTreeInverse.removeSegment(poleSegment, orbitSegment, 1000, 1500);
        assertEquals(3, remaining.length);

        final Segment lower = remaining[0];
        assertEquals(0, lower.x_min);
        assertEquals(999, lower.x_max);
        assertEquals(10, lower.y_min);
        assertEquals(1499, lower.y_max);

        final Segment left = remaining[1];
        assertEquals(0, left.x_min);
        assertEquals(399, left.x_max);
        assertEquals(0, left.y_min);
        assertEquals(9, left.y_max);

        final Segment right = remaining[2];
        assertEquals(410, right.x_min);
        assertEquals(999, right.x_max);
        assertEquals(0, right.y_min);
        assertEquals(9, right.y_max);
    }

    @Test
    public void testRemoveSegment_atRightBorder() {
        final Segment poleSegment = new Segment(990, 999, 120, 129);
        final Segment orbitSegment = new Segment(0, 999, 0, 1499);

        final Segment[] remaining = PixelQuadTreeInverse.removeSegment(poleSegment, orbitSegment, 1000, 1500);
        assertEquals(3, remaining.length);

        final Segment upper = remaining[0];
        assertEquals(0, upper.x_min);
        assertEquals(999, upper.x_max);
        assertEquals(0, upper.y_min);
        assertEquals(119, upper.y_max);

        final Segment lower = remaining[1];
        assertEquals(0, lower.x_min);
        assertEquals(999, lower.x_max);
        assertEquals(130, lower.y_min);
        assertEquals(1499, lower.y_max);

        final Segment left = remaining[2];
        assertEquals(0, left.x_min);
        assertEquals(989, left.x_max);
        assertEquals(120, left.y_min);
        assertEquals(129, left.y_max);
    }

    @Test
    public void testRemoveSegment_atBottomBorder() {
        final Segment poleSegment = new Segment(410, 419, 1489, 1499);
        final Segment orbitSegment = new Segment(0, 999, 0, 1499);

        final Segment[] remaining = PixelQuadTreeInverse.removeSegment(poleSegment, orbitSegment, 1000, 1500);
        assertEquals(3, remaining.length);

        final Segment upper = remaining[0];
        assertEquals(0, upper.x_min);
        assertEquals(999, upper.x_max);
        assertEquals(0, upper.y_min);
        assertEquals(1488, upper.y_max);

        final Segment left = remaining[1];
        assertEquals(0, left.x_min);
        assertEquals(409, left.x_max);
        assertEquals(1489, left.y_min);
        assertEquals(1499, left.y_max);

        final Segment right = remaining[2];
        assertEquals(420, right.x_min);
        assertEquals(999, right.x_max);
        assertEquals(1489, right.y_min);
        assertEquals(1499, right.y_max);
    }

    @Test
    public void testRemoveSegment_atTopLeftCorner() {
        final Segment poleSegment = new Segment(0, 9, 0, 9);
        final Segment orbitSegment = new Segment(0, 999, 0, 1499);

        final Segment[] remaining = PixelQuadTreeInverse.removeSegment(poleSegment, orbitSegment, 1000, 1500);
        assertEquals(2, remaining.length);

        final Segment bottom = remaining[0];
        assertEquals(0, bottom.x_min);
        assertEquals(999, bottom.x_max);
        assertEquals(10, bottom.y_min);
        assertEquals(1499, bottom.y_max);

        final Segment right = remaining[1];
        assertEquals(10, right.x_min);
        assertEquals(999, right.x_max);
        assertEquals(0, right.y_min);
        assertEquals(9, right.y_max);
    }

    @Test
    public void testRemoveSegment_atTopRightCorner() {
        final Segment poleSegment = new Segment(990, 999, 0, 9);
        final Segment orbitSegment = new Segment(0, 999, 0, 1499);

        final Segment[] remaining = PixelQuadTreeInverse.removeSegment(poleSegment, orbitSegment, 1000, 1500);
        assertEquals(2, remaining.length);

        final Segment bottom = remaining[0];
        assertEquals(0, bottom.x_min);
        assertEquals(999, bottom.x_max);
        assertEquals(10, bottom.y_min);
        assertEquals(1499, bottom.y_max);

        final Segment left = remaining[1];
        assertEquals(0, left.x_min);
        assertEquals(989, left.x_max);
        assertEquals(0, left.y_min);
        assertEquals(9, left.y_max);
    }

    @Test
    public void testRemoveSegment_atBottomRightCorner() {
        final Segment poleSegment = new Segment(990, 999, 1490, 1499);
        final Segment orbitSegment = new Segment(0, 999, 0, 1499);

        final Segment[] remaining = PixelQuadTreeInverse.removeSegment(poleSegment, orbitSegment, 1000, 1500);
        assertEquals(2, remaining.length);

        final Segment top = remaining[0];
        assertEquals(0, top.x_min);
        assertEquals(999, top.x_max);
        assertEquals(0, top.y_min);
        assertEquals(1489, top.y_max);

        final Segment right = remaining[1];
        assertEquals(0, right.x_min);
        assertEquals(989, right.x_max);
        assertEquals(1490, right.y_min);
        assertEquals(1499, right.y_max);
    }

    @Test
    public void testRemoveSegment_atBottomLeftCorner() {
        final Segment poleSegment = new Segment(0, 9, 1490, 1499);
        final Segment orbitSegment = new Segment(0, 999, 0, 1499);

        final Segment[] remaining = PixelQuadTreeInverse.removeSegment(poleSegment, orbitSegment, 1000, 1500);
        assertEquals(2, remaining.length);

        final Segment top = remaining[0];
        assertEquals(0, top.x_min);
        assertEquals(999, top.x_max);
        assertEquals(0, top.y_min);
        assertEquals(1489, top.y_max);

        final Segment left = remaining[1];
        assertEquals(10, left.x_min);
        assertEquals(999, left.x_max);
        assertEquals(1490, left.y_min);
        assertEquals(1499, left.y_max);
    }

    @Test
    public void testGetKey() {
        final PixelQuadTreeInverse nonInterpolating = new PixelQuadTreeInverse(false);
        assertEquals("INV_PIXEL_QUAD_TREE", nonInterpolating.getKey());

        final PixelQuadTreeInverse interpolating = new PixelQuadTreeInverse(true);
        assertEquals("INV_PIXEL_QUAD_TREE_INTERPOLATING", interpolating.getKey());
    }

    @Test
    public void testSplitAtOutsidePoint_AcrossTrack() {
        final Segment segment = new Segment(0, 99, 0, 399);
        segment.lon_min = 0.0;
        segment.lon_max = 30.0;
        segment.lat_min = 0.0;
        segment.lat_max = 16.0;

        final Segment[] segments = PixelQuadTreeInverse.splitAtOutsidePoint(segment, SegmentCoverage.ACROSS, new MockCalculator());
        assertEquals(2, segments.length);

        assertEquals(0, segments[0].x_min);
        assertEquals(99, segments[0].x_max);
        assertEquals(0, segments[0].y_min);
        assertEquals(160, segments[0].y_max);

        assertEquals(0, segments[1].x_min);
        assertEquals(99, segments[1].x_max);
        assertEquals(161, segments[1].y_min);
        assertEquals(399, segments[1].y_max);
    }

    @Test
    public void testSplitAtOutsidePoint_AcrossTrack_distanceTooSmall() {
        final Segment segment = new Segment(0, 99, 0, 399);
        segment.lon_min = 0.0;
        segment.lon_max = 30.0;
        segment.lat_min = 0.0;
        segment.lat_max = 0.2;

        // first segment would be too small, so split at half of the segment height
        final Segment[] segments = PixelQuadTreeInverse.splitAtOutsidePoint(segment, SegmentCoverage.ACROSS, new MockCalculator());
        assertEquals(2, segments.length);

        assertEquals(0, segments[0].x_min);
        assertEquals(99, segments[0].x_max);
        assertEquals(0, segments[0].y_min);
        assertEquals(200, segments[0].y_max);

        assertEquals(0, segments[1].x_min);
        assertEquals(99, segments[1].x_max);
        assertEquals(201, segments[1].y_min);
        assertEquals(399, segments[1].y_max);
    }

    @Test
    public void testSplitAtOutsidePoint_AlongTrack() {
        final Segment segment = new Segment(0, 99, 0, 399);
        segment.lon_min = 0.0;
        segment.lon_max = 4.0;
        segment.lat_min = -10.0;
        segment.lat_max = 45.0;

        final Segment[] segments = PixelQuadTreeInverse.splitAtOutsidePoint(segment, SegmentCoverage.ALONG, new MockCalculator());
        assertEquals(2, segments.length);

        assertEquals(0, segments[0].x_min);
        assertEquals(40, segments[0].x_max);
        assertEquals(0, segments[0].y_min);
        assertEquals(399, segments[0].y_max);

        assertEquals(41, segments[1].x_min);
        assertEquals(99, segments[1].x_max);
        assertEquals(0, segments[1].y_min);
        assertEquals(399, segments[1].y_max);
    }

    @Test
    public void testSplitAtOutsidePoint_AlongTrack_distanceTooSmall() {
        final Segment segment = new Segment(0, 99, 0, 399);
        segment.lon_min = 0.0;
        segment.lon_max = 0.2;
        segment.lat_min = -10.0;
        segment.lat_max = 45.0;

        // first segment would be too small, so split at half of the segment width
        final Segment[] segments = PixelQuadTreeInverse.splitAtOutsidePoint(segment, SegmentCoverage.ALONG, new MockCalculator());
        assertEquals(2, segments.length);

        assertEquals(0, segments[0].x_min);
        assertEquals(50, segments[0].x_max);
        assertEquals(0, segments[0].y_min);
        assertEquals(399, segments[0].y_max);

        assertEquals(51, segments[1].x_min);
        assertEquals(99, segments[1].x_max);
        assertEquals(0, segments[1].y_min);
        assertEquals(399, segments[1].y_max);
    }

    @Test
    public void testSplitAtOutsidePoint_Invalid() {
        final Segment segment = new Segment(0, 99, 0, 399);

        try {
            PixelQuadTreeInverse.splitAtOutsidePoint(segment, SegmentCoverage.INSIDE, new MockCalculator());
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testSplitAtAntMeridian_AlongTrack() {
        final Segment segment = new Segment(0, 199, 0, 399);
        segment.lon_min = 170.0;
        segment.lon_max = 30.0;
        segment.lat_min = 0.0;
        segment.lat_max = 60.0;

        final Segment[] segments = PixelQuadTreeInverse.splitAtAntiMeridian(segment, SegmentCoverage.ALONG, new MockCalculator(170.0));
        assertEquals(2, segments.length);

        assertEquals(0, segments[0].x_min);
        assertEquals(100, segments[0].x_max);
        assertEquals(0, segments[0].y_min);
        assertEquals(399, segments[0].y_max);

        assertEquals(101, segments[1].x_min);
        assertEquals(199, segments[1].x_max);
        assertEquals(0, segments[1].y_min);
        assertEquals(399, segments[1].y_max);
    }

    @Test
    public void testSplitAtAntiMeridian_AlongTrack_tooSmallSegment() {
        final Segment segment = new Segment(0, 199, 0, 399);
        segment.lon_min = 170.0;
        segment.lon_max = 30.0;
        segment.lat_min = 0.0;
        segment.lat_max = 60.0;

        // splitpoint is at x = 3
        final Segment[] segments = PixelQuadTreeInverse.splitAtAntiMeridian(segment, SegmentCoverage.ALONG, new MockCalculator(179.8));
        assertEquals(0, segments.length);
    }

    @Test
    public void testSplitAtAntiMeridian_AcrossTrack_tooSmallSegment() {
        final Segment segment = new Segment(0, 199, 0, 399);
        segment.lon_min = 170.0;
        segment.lon_max = 30.0;
        segment.lat_min = 0.0;
        segment.lat_max = 60.0;

        final Segment[] segments = PixelQuadTreeInverse.splitAtAntiMeridian(segment, SegmentCoverage.ACROSS, new MockCalculator(160.3));
        assertEquals(0, segments.length);
    }

    @Test
    public void testSplitAtAntiMeridian_Invalid() {
        final Segment segment = new Segment(0, 99, 0, 399);

        try {
            PixelQuadTreeInverse.splitAtAntiMeridian(segment, SegmentCoverage.INSIDE, new MockCalculator());
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }
}