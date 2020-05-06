package org.esa.snap.core.datamodel;

import org.esa.snap.core.dataop.maptransf.Datum;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Marco Peters
 * @since 6.0
 */
public class GcpGeoCodingTest {

    private GcpGeoCoding gcpGeoCoding;

    @Before
    public void setUp() throws Exception {
        int width = 10;
        int height = 10;

        Placemark[] gcps = {
                Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "p1", "p1", "", new PixelPos(0.5f, 0.5f), new GeoPos(10, -10), null),
                Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "p2", "p2", "", new PixelPos(width - 0.5f, 0.5f), new GeoPos(10, 10), null),
                Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "p3", "p3", "", new PixelPos(width - 0.5f, height - 0.5f), new GeoPos(-10, 10), null),
                Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "p4", "p4", "", new PixelPos(0.5f, height - 0.5f), new GeoPos(-10, -10), null),
        };
        gcpGeoCoding = new GcpGeoCoding(GcpGeoCoding.Method.POLYNOMIAL1, gcps, width, height, Datum.WGS_84);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testTransferGeoCoding() {
        Scene source = SceneFactory.createScene(new Band("source", ProductData.TYPE_INT8, 10, 10));
        Scene target = SceneFactory.createScene(new Band("target", ProductData.TYPE_INT8, 10, 10));

        PixelPos pixelPos = gcpGeoCoding.getPixelPos(new GeoPos(4.0, 2.0), null);
        assertEquals(5.9, pixelPos.x, 1e-8);
        assertEquals(3.2, pixelPos.y, 1e-8);

        boolean transferred = gcpGeoCoding.transferGeoCoding(source, target, null);

        assertTrue(transferred);

        final GeoCoding geoCoding = target.getGeoCoding();
        pixelPos = geoCoding.getPixelPos(new GeoPos(4.0, 2.0), null);
        assertEquals(5.9, pixelPos.x, 1e-8);
        assertEquals(3.2, pixelPos.y, 1e-8);

    }

    @Test
    public void testIsSegmentCrossingMeridianAt180() {
        assertTrue(GcpGeoCoding.isSegmentCrossingMeridianAt180(120, -158));
        assertTrue(GcpGeoCoding.isSegmentCrossingMeridianAt180(-110, 148));

        assertFalse(GcpGeoCoding.isSegmentCrossingMeridianAt180(110, 148));
        assertFalse(GcpGeoCoding.isSegmentCrossingMeridianAt180(-10, 148));
        assertFalse(GcpGeoCoding.isSegmentCrossingMeridianAt180(110, -8));
        assertFalse(GcpGeoCoding.isSegmentCrossingMeridianAt180(11, -8));
    }

    @Test
    public void testGetPixelPos() {
        PixelPos pixelPos = gcpGeoCoding.getPixelPos(new GeoPos(0.0, 0.0), null);
        assertEquals(5.0, pixelPos.x, 1e-8);
        assertEquals(5.0, pixelPos.y, 1e-8);

        pixelPos = gcpGeoCoding.getPixelPos(new GeoPos(-1.0, 0.0), null);
        assertEquals(5.0, pixelPos.x, 1e-8);
        assertEquals(5.45, pixelPos.y, 1e-8);

        pixelPos = gcpGeoCoding.getPixelPos(new GeoPos(0.0, 1.0), null);
        assertEquals(5.45, pixelPos.x, 1e-8);
        assertEquals(5.0, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_outside() {
        PixelPos pixelPos = gcpGeoCoding.getPixelPos(new GeoPos(-20.0, -20.0), null);
        assertEquals(Double.NaN, pixelPos.x, 1e-8);
        assertEquals(Double.NaN, pixelPos.y, 1e-8);

        pixelPos = gcpGeoCoding.getPixelPos(new GeoPos(20.0, 20.0), null);
        assertEquals(Double.NaN, pixelPos.x, 1e-8);
        assertEquals(Double.NaN, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_invalidInput() {
        final GeoPos geoPos = new GeoPos(0.0, 0.0);
        geoPos.setInvalid();

        final PixelPos pixelPos = gcpGeoCoding.getPixelPos(geoPos, null);
        assertEquals(Double.NaN, pixelPos.x, 1e-8);
        assertEquals(Double.NaN, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetGeoPos() {
        GeoPos geoPos = gcpGeoCoding.getGeoPos(new PixelPos(0.5, 0.5), null);
        assertEquals(-9.999999999999996, geoPos.lon, 1e-8);
        assertEquals(10.000000000000002, geoPos.lat, 1e-8);

        geoPos = gcpGeoCoding.getGeoPos(new PixelPos(3.5, 0.5), null);
        assertEquals(-3.3333333333333313, geoPos.lon, 1e-8);
        assertEquals(10.000000000000004, geoPos.lat, 1e-8);

        geoPos = gcpGeoCoding.getGeoPos(new PixelPos(0.5, 6.5), null);
        assertEquals(-9.999999999999996, geoPos.lon, 1e-8);
        assertEquals(-3.3333333333333357, geoPos.lat, 1e-8);
    }

    @Test
    public void testGetGeoPos_outside() {
        GeoPos geoPos = gcpGeoCoding.getGeoPos(new PixelPos(-1.5, 0.5), null);
        assertEquals(Double.NaN, geoPos.lon, 1e-8);
        assertEquals(Double.NaN, geoPos.lat, 1e-8);

        geoPos = gcpGeoCoding.getGeoPos(new PixelPos(3.5, -0.5), null);
        assertEquals(Double.NaN, geoPos.lon, 1e-8);
        assertEquals(Double.NaN, geoPos.lat, 1e-8);

        geoPos = gcpGeoCoding.getGeoPos(new PixelPos(14.5, 16.5), null);
        assertEquals(Double.NaN, geoPos.lon, 1e-8);
        assertEquals(Double.NaN, geoPos.lat, 1e-8);
    }

    @Test
    public void testGetGeoPos_invalidInput() {
        final PixelPos pixelPos = new PixelPos(0.5, 0.5);
        pixelPos.setInvalid();

        GeoPos geoPos = gcpGeoCoding.getGeoPos(pixelPos, null);
        assertEquals(Double.NaN, geoPos.lon, 1e-8);
        assertEquals(Double.NaN, geoPos.lat, 1e-8);
    }

    @Test
    public void testCanClone() {
        assertTrue(gcpGeoCoding.canClone());
    }

    @Test
    public void testClone() {
        final PixelPos pixelPos = new PixelPos(6.5, 2.5);

        GeoPos geoPos = gcpGeoCoding.getGeoPos(pixelPos, null);
        assertEquals(3.3333333333333326, geoPos.lon, 1e-8);
        assertEquals(5.555555555555558, geoPos.lat, 1e-8);

        final GeoCoding clone = gcpGeoCoding.clone();

        geoPos = clone.getGeoPos(pixelPos, null);
        assertEquals(3.3333333333333326, geoPos.lon, 1e-8);
        assertEquals(5.555555555555558, geoPos.lat, 1e-8);
    }

    @Test
    public void testClone_dispose() {
        final PixelPos pixelPos = new PixelPos(7.5, 3.5);

        GeoPos geoPos = gcpGeoCoding.getGeoPos(pixelPos, null);
        assertEquals(5.555555555555552, geoPos.lon, 1e-8);
        assertEquals(3.333333333333337, geoPos.lat, 1e-8);

        final GeoCoding clone = gcpGeoCoding.clone();
        gcpGeoCoding.dispose();

        geoPos = clone.getGeoPos(pixelPos, null);
        assertEquals(5.555555555555552, geoPos.lon, 1e-8);
        assertEquals(3.333333333333337, geoPos.lat, 1e-8);

        clone.dispose();
    }
}