package org.esa.snap.engine_utilities.gpf;

import com.bc.ceres.annotation.STTM;

import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.geotools.referencing.CRS;
import org.junit.Assume;
import org.junit.Test;

/**
 * Micro benchmark of the CrsGeoCoding tile georeferencing: exact per-pixel transform
 * ({@link CrsGeoCoding#getPixels}) versus {@link CrsTileGeoPosInterpolator}.
 * <p>
 * Skipped unless run with {@code -Dsnap.benchmark=true}, e.g.
 * {@code mvn -o test -pl snap-engine-utilities -Dtest=TileGeoreferencingBenchmarkTest -Dsnap.benchmark=true}
 */
public class TileGeoreferencingBenchmarkTest {

    @Test
    @STTM("SNAP-4390")
    public void benchmarkUtmTile() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("snap.benchmark"));
        // typical Range-Doppler terrain correction target tile (512 + 2 border) in UTM
        final int w = 514, h = 514;
        for (double pixelSize : new double[]{10.0, 40.0, 100.0}) {
            for (double latC : new double[]{45.0, 70.0}) {
                final CrsGeoCoding gc = CrsTileGeoPosInterpolatorTest.utmGeoCoding("EPSG:32632", 10.5, latC, pixelSize, 20000, 20000);
                run(String.format("UTM %3.0f m lat %2.0f", pixelSize, latC), gc, 2000, 3000, w, h);
            }
        }
        // geographic CRS (not interpolated in production): isolates the interpolation overhead
        run("geographic (overhead)", new CrsGeoCoding(org.geotools.referencing.crs.DefaultGeographicCRS.WGS84,
                                                      20000, 20000, 10.0, 50.0, 1e-4, 1e-4, 0.0, 0.0), 2000, 3000, w, h);
        // worst case: polar stereographic tile around the pole, everything ends up exact
        final CrsGeoCoding polar = new CrsGeoCoding(CRS.decode("EPSG:3413", true), 20000, 20000,
                                                    -10000 * 100.0, 10000 * 100.0, 100.0, 100.0, 0.0, 0.0);
        run("polar stereo at pole", polar, 9743 - 20 * w / 2, 9743, w, h);
    }

    private static void run(String label, CrsGeoCoding gc, int x0, int y0, int w, int h) {
        final double[] lat = new double[w * h], lon = new double[w * h];
        final int rounds = 40;
        double exactNs = 0, interpNs = 0;
        long nExact = 0;
        // warm up: the interpolator code paths need many more invocations than the single batch loop to get C2-compiled
        for (int i = 0; i < 10; i++) {
            gc.getPixels(x0 + i * w, y0, w, h, lat, lon);
        }
        for (int i = 0; i < 200; i++) {
            CrsTileGeoPosInterpolator.computePixels(gc, x0 + (i % 20) * w, y0, w, h, lat, lon);
        }
        for (int i = 0; i < rounds; i++) {
            final int x1 = x0 + (i % 20) * w, y1 = y0 + (i / 20) * h;
            final long t0 = System.nanoTime();
            gc.getPixels(x1, y1, w, h, lat, lon);
            final long t1 = System.nanoTime();
            nExact += CrsTileGeoPosInterpolator.computePixels(gc, x1, y1, w, h, lat, lon);
            final long t2 = System.nanoTime();
            exactNs += t1 - t0;
            interpNs += t2 - t1;
        }
        final double px = (double) rounds * w * h;
        // per-point transform cost, as used for interpolation nodes
        final org.esa.snap.core.datamodel.PixelPos pp = new org.esa.snap.core.datamodel.PixelPos();
        final org.esa.snap.core.datamodel.GeoPos gp = new org.esa.snap.core.datamodel.GeoPos();
        final long t3 = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            pp.setLocation(x0 + (i % 1000) + 0.5, y0 + (i / 1000) + 0.5);
            gc.getGeoPos(pp, gp);
        }
        final double pointNs = (System.nanoTime() - t3) / 1e5;
        System.out.printf("TileGeoreferencing %-22s %dx%d: exact %6.1f ns/px, interpolated %6.1f ns/px, speedup %5.1fx" +
                          " (exact fraction %.4f, getGeoPos %.0f ns/pt)%n",
                          label, w, h, exactNs / px, interpNs / px, exactNs / interpNs, nExact / px, pointNs);
    }
}
