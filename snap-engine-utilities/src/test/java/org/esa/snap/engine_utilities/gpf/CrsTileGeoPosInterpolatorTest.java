package org.esa.snap.engine_utilities.gpf;

import com.bc.ceres.annotation.STTM;

import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Compares the interpolated tile geo-positions against the exact per-pixel transform.
 */
public class CrsTileGeoPosInterpolatorTest {

    private static final double TOL = CrsTileGeoPosInterpolator.TOLERANCE_DEG;

    @Test
    @STTM("SNAP-4390")
    public void utmTilesAtSeveralLatitudes() throws Exception {
        final double[] lats = {0.5, 30.0, 45.0, 60.0, 70.0, 80.0, 83.5, -45.0, -79.0};
        for (double lat : lats) {
            final String epsg = lat >= 0 ? "EPSG:32632" : "EPSG:32732";
            // tile centred on 10.5E (1.5 deg east of the central meridian)
            final CrsGeoCoding gc = utmGeoCoding(epsg, 10.5, lat, 10.0, 20000, 20000);
            final Stats s = compare(gc, 9000, 9000, 514, 514);
            assertTrue("lat " + lat + ": max error " + s.maxErr, s.maxErr <= TOL);
            assertEquals("lat " + lat, 0, s.nanMismatch);
            // the point of the exercise: few exact evaluations
            assertTrue("lat " + lat + ": exact fraction " + s.exactFraction, s.exactFraction < 0.02);
        }
    }

    @Test
    @STTM("SNAP-4390")
    public void utmTileSpanningZoneEdge() throws Exception {
        // zone 32 is 6E..12E; tile centred on 12E extends into zone 33
        CrsGeoCoding gc = utmGeoCoding("EPSG:32632", 12.0, 48.0, 10.0, 2000, 2000);
        Stats s = compare(gc, 700, 700, 600, 600);
        assertTrue("max error " + s.maxErr, s.maxErr <= TOL);

        // coarse pixels, a tile far outside its zone at high latitude: forces refinement
        gc = utmGeoCoding("EPSG:32632", 20.0, 72.0, 500.0, 600, 600);
        s = compare(gc, 0, 0, 600, 600);
        assertTrue("max error " + s.maxErr, s.maxErr <= TOL);
        assertEquals(0, s.nanMismatch);
    }

    @Test
    @STTM("SNAP-4390")
    public void utmTileCrossingAntimeridian() throws Exception {
        // zone 60 central meridian 177E; tile around 180 so longitudes wrap to -180
        final CrsGeoCoding gc = utmGeoCoding("EPSG:32660", 180.0, -16.0, 20.0, 1000, 1000);
        final Stats s = compare(gc, 200, 200, 600, 600);
        assertTrue("max error " + s.maxErr, s.maxErr <= TOL);
        assertEquals(0, s.nanMismatch);
    }

    @Test
    @STTM("SNAP-4390")
    public void polarStereographicTileContainingPole() throws Exception {
        final CoordinateReferenceSystem crs = CRS.decode("EPSG:3413", true);
        // centre the image on the pole (0, 0 in map coordinates)
        final int w = 600, h = 600;
        final double pix = 100.0;
        final CrsGeoCoding gc = new CrsGeoCoding(crs, w, h, -w / 2.0 * pix, h / 2.0 * pix, pix, pix, 0.0, 0.0);
        final Stats s = compare(gc, 0, 0, w, h);
        assertTrue("max error " + s.maxErr, s.maxErr <= TOL);
        assertEquals(0, s.nanMismatch);
    }

    @Test
    @STTM("SNAP-4390")
    public void geographicCrsKeepsExactPath() throws Exception {
        final CrsGeoCoding gc = new CrsGeoCoding(DefaultGeographicCRS.WGS84, 2000, 2000, 10.0, 50.0, 1e-4, 1e-4, 0.0, 0.0);
        assertFalse(CrsTileGeoPosInterpolator.isApplicable(gc, 512, 512));

        final int w = 300, h = 200;
        final double[] expLat = new double[w * h], expLon = new double[w * h];
        gc.getPixels(100, 50, w, h, expLat, expLon);
        final TileGeoreferencing tgr = new TileGeoreferencing(gc, 100, 50, w, h);
        final GeoPos geo = new GeoPos();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                tgr.getGeoPos(100 + x, 50 + y, geo);
                assertEquals(expLat[y * w + x], geo.lat, 0.0);
                assertEquals(expLon[y * w + x], geo.lon, 0.0);
            }
        }
    }

    @Test
    @STTM("SNAP-4390")
    public void smallTilesUseExactPath() throws Exception {
        final CrsGeoCoding gc = utmGeoCoding("EPSG:32632", 9.0, 45.0, 10.0, 1000, 1000);
        assertFalse(CrsTileGeoPosInterpolator.isApplicable(gc, 4, 100));
        assertFalse(CrsTileGeoPosInterpolator.isApplicable(gc, 100, 3));
        assertTrue(CrsTileGeoPosInterpolator.isApplicable(gc, 5, 5));
        // odd, tiny and strip shaped tiles through the full TileGeoreferencing path
        final int[][] sizes = {{1, 1}, {3, 7}, {5, 5}, {6, 300}, {300, 6}, {33, 65}, {257, 129}};
        for (int[] sz : sizes) {
            final Stats s = compareViaTileGeoreferencing(gc, 17, 23, sz[0], sz[1]);
            assertTrue(sz[0] + "x" + sz[1] + " max error " + s.maxErr, s.maxErr <= TOL);
        }
    }

    @Test
    @STTM("SNAP-4390")
    public void tileGeoreferencingMatchesExactForUtm() throws Exception {
        final CrsGeoCoding gc = utmGeoCoding("EPSG:32633", 15.0, 52.0, 10.0, 3000, 3000);
        final Stats s = compareViaTileGeoreferencing(gc, 511, 1023, 258, 258);
        assertTrue("max error " + s.maxErr, s.maxErr <= TOL);
    }

    // ------------------------------------------------------------------------------------------------

    static CrsGeoCoding utmGeoCoding(String epsg, double lonCentre, double latCentre, double pixelSize,
                                     int w, int h) throws Exception {
        final CoordinateReferenceSystem crs = CRS.decode(epsg, true);
        final MathTransform g2m = CRS.findMathTransform(DefaultGeographicCRS.WGS84, crs, true);
        final double[] en = new double[2];
        g2m.transform(new double[]{lonCentre, latCentre}, 0, en, 0, 1);
        final double easting = en[0] - w / 2.0 * pixelSize;
        final double northing = en[1] + h / 2.0 * pixelSize;
        return new CrsGeoCoding(crs, w, h, easting, northing, pixelSize, pixelSize, 0.0, 0.0);
    }

    static final class Stats {
        double maxErr;
        int nanMismatch;
        double exactFraction;
    }

    private static Stats compare(CrsGeoCoding gc, int x1, int y1, int w, int h) {
        final double[] expLat = new double[w * h], expLon = new double[w * h];
        exactPerPixel(gc, x1, y1, w, h, expLat, expLon);
        final double[] lat = new double[w * h], lon = new double[w * h];
        final int nExact = CrsTileGeoPosInterpolator.computePixels(gc, x1, y1, w, h, lat, lon);
        final Stats s = diff(expLat, expLon, lat, lon);
        s.exactFraction = nExact / (double) (w * h);
        System.out.printf("CrsTileGeoPosInterpolator %s %dx%d: max err %.3e deg, exact fraction %.4f%n",
                          gc.getMapCRS().getName().getCode(), w, h, s.maxErr, s.exactFraction);
        return s;
    }

    private static Stats compareViaTileGeoreferencing(CrsGeoCoding gc, int x1, int y1, int w, int h) {
        final double[] expLat = new double[w * h], expLon = new double[w * h];
        exactPerPixel(gc, x1, y1, w, h, expLat, expLon);
        final TileGeoreferencing tgr = new TileGeoreferencing(gc, x1, y1, w, h);
        assertTrue(tgr.isCached());
        final double[] lat = new double[w * h], lon = new double[w * h];
        final GeoPos geo = new GeoPos();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                tgr.getGeoPos(x1 + x, y1 + y, geo);
                lat[y * w + x] = geo.lat;
                lon[y * w + x] = geo.lon;
            }
        }
        return diff(expLat, expLon, lat, lon);
    }

    private static void exactPerPixel(CrsGeoCoding gc, int x1, int y1, int w, int h, double[] lat, double[] lon) {
        // the original TileGeoreferencing path
        gc.getPixels(x1, y1, w, h, lat, lon);
        // sanity: the batch transform agrees with the per-point transform used for interpolation nodes
        final GeoPos g = gc.getGeoPos(new PixelPos(x1 + 0.5, y1 + 0.5), null);
        assertEquals(lat[0], g.lat, 1e-12);
    }

    private static Stats diff(double[] expLat, double[] expLon, double[] lat, double[] lon) {
        final Stats s = new Stats();
        for (int i = 0; i < lat.length; i++) {
            final boolean expNaN = Double.isNaN(expLat[i]) || Double.isNaN(expLon[i]);
            final boolean actNaN = Double.isNaN(lat[i]) || Double.isNaN(lon[i]);
            if (expNaN || actNaN) {
                if (expNaN != actNaN) {
                    s.nanMismatch++;
                }
                continue;
            }
            s.maxErr = Math.max(s.maxErr, Math.max(Math.abs(expLat[i] - lat[i]), Math.abs(expLon[i] - lon[i])));
        }
        return s;
    }
}
