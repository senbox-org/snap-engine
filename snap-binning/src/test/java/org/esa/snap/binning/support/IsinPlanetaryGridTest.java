package org.esa.snap.binning.support;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.esa.snap.binning.Reprojector;
import org.esa.snap.core.util.grid.isin.IsinPoint;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class IsinPlanetaryGridTest {

    @Test
    public void testConstruct_incorrectNumberOfRows() {

        try {
            new IsinPlanetaryGrid(11);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            new IsinPlanetaryGrid(-4);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            new IsinPlanetaryGrid(18 * 1200 - 1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            new IsinPlanetaryGrid(18 * 2400 + 1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetBinIndex_1km() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 1200);

        long binIndex = grid.getBinIndex(0.0, 0.0);
        assertEquals(91800000000L, binIndex);

        // Hamburg
        binIndex = grid.getBinIndex(53.551086, 9.993682);
        assertEquals(31807730712L, binIndex);

        // Cape of Good Hope
        binIndex = grid.getBinIndex(-34.357203, -18.49755);
        assertEquals(121605220567L, binIndex);

        // Dome C
        binIndex = grid.getBinIndex(-75, -125);
        assertEquals(161406000918L, binIndex);
    }

    @Test
    public void testGetBinIndex_500m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 2400);

        long binIndex = grid.getBinIndex(0.0, 0.0);
        assertEquals(91800000000L, binIndex);

        // Hamburg
        binIndex = grid.getBinIndex(53.551086, 9.993682);
        assertEquals(31815471424L, binIndex);

        // Cape of Good Hope
        binIndex = grid.getBinIndex(-34.357203, -18.49755);
        assertEquals(121610451134L, binIndex);

        // Dome C
        binIndex = grid.getBinIndex(-75, -125);
        assertEquals(161412001835L, binIndex);
    }

    @Test
    public void testGetBinIndex_250m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 4800);

        long binIndex = grid.getBinIndex(0.0, 0.0);
        assertEquals(91800000000L, binIndex);

        // Hamburg
        binIndex = grid.getBinIndex(53.551086, 9.993682);
        assertEquals(31830952849L, binIndex);

        // Cape of Good Hope
        binIndex = grid.getBinIndex(-34.357203, -18.49755);
        assertEquals(121620912270L, binIndex);

        // Dome C
        binIndex = grid.getBinIndex(-75, -125);
        assertEquals(161424003671L, binIndex);
    }

    @Test
    public void testToBinIndex() {
        IsinPoint isinPoint = new IsinPoint(0, 0, 0, 0);
        assertEquals(0, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(2599, 0, 0, 0);
        assertEquals(2599, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 1111, 0, 0);
        assertEquals(11110000, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(2222, 1111, 0, 0);
        assertEquals(11112222, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(2222, 1111, 33, 0);
        assertEquals(3311112222L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(2222, 1111, 33, 44);
        assertEquals(443311112222L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 0, 0, 1);
        assertEquals(10000000000L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 0, 0, 17);
        assertEquals(170000000000L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 0, 1, 0);
        assertEquals(100000000L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 0, 16, 0);
        assertEquals(1600000000L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 0, 16, 5);
        assertEquals(51600000000L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 156, 0, 0);
        assertEquals(1560000, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 157, 17, 6);
        assertEquals(61701570000L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(158, 0, 0, 0);
        assertEquals(158L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(4799, 4799, 35, 17);
        assertEquals(173547994799L, IsinPlanetaryGrid.toBinIndex(isinPoint));
    }

    @Test
    public void testToIsinPoint() {
        IsinPoint point = IsinPlanetaryGrid.toIsinPoint(0L);
        assertEquals(0.0, point.getX(), 1e-8);
        assertEquals(0.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(2599L);
        assertEquals(2599.0, point.getX(), 1e-8);
        assertEquals(0.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(11110000L);
        assertEquals(0.0, point.getX(), 1e-8);
        assertEquals(1111.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(11112222L);
        assertEquals(2222.0, point.getX(), 1e-8);
        assertEquals(1111.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(3311112222L);
        assertEquals(2222.0, point.getX(), 1e-8);
        assertEquals(1111.0, point.getY(), 1e-8);
        assertEquals(33, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(443311112222L);
        assertEquals(2222.0, point.getX(), 1e-8);
        assertEquals(1111.0, point.getY(), 1e-8);
        assertEquals(33, point.getTile_col());
        assertEquals(44, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(1600L);
        assertEquals(1600.0, point.getX(), 1e-8);
        assertEquals(0.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(1560000L);
        assertEquals(0.0, point.getX(), 1e-8);
        assertEquals(156.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(1571807L);
        assertEquals(1807.0, point.getX(), 1e-8);
        assertEquals(157.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(15800000000L);
        assertEquals(0.0, point.getX(), 1e-8);
        assertEquals(0.0, point.getY(), 1e-8);
        assertEquals(58, point.getTile_col());
        assertEquals(1, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(173547994799L);
        assertEquals(4799.0, point.getX(), 1e-8);
        assertEquals(4799.0, point.getY(), 1e-8);
        assertEquals(35, point.getTile_col());
        assertEquals(17, point.getTile_line());
    }

    @Test
    public void testGetRowIndex_1km() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 1200);

        assertEquals(0, grid.getRowIndex(0));
        assertEquals(20400, grid.getRowIndex(170000000000L));
        assertEquals(7200, grid.getRowIndex(61600000000L));
        assertEquals(156, grid.getRowIndex(1560001));
        assertEquals(157, grid.getRowIndex(1571808));
        assertEquals(21599, grid.getRowIndex(173511991199L));
    }

    @Test
    public void testGetRowIndex_500m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 2400);

        assertEquals(0, grid.getRowIndex(0));
        assertEquals(40800, grid.getRowIndex(170000000000L));
        assertEquals(14400, grid.getRowIndex(61600000000L));
        assertEquals(156, grid.getRowIndex(1560001));
        assertEquals(157, grid.getRowIndex(1571808));
        assertEquals(43199, grid.getRowIndex(173523992399L));
    }

    @Test
    public void testGetRowIndex_250m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 4800);

        assertEquals(0, grid.getRowIndex(0));
        assertEquals(81600, grid.getRowIndex(170000000000L));
        assertEquals(28800, grid.getRowIndex(61600000000L));
        assertEquals(156, grid.getRowIndex(1560001));
        assertEquals(157, grid.getRowIndex(1571808));
        assertEquals(86399, grid.getRowIndex(173547994799L));
    }

    @Test
    public void testGetNumBins_1km() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 1200);

        assertEquals(933120000, grid.getNumBins());
    }

    @Test
    public void testGetNumBins_500m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 2400);

        assertEquals(3732480000L, grid.getNumBins());
    }

    @Test
    public void testGetNumBins_250m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 4800);

        assertEquals(14929920000L, grid.getNumBins());
    }

    @Test
    public void testGetNumRows_1km() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 1200);

        assertEquals(21600, grid.getNumRows());
    }

    @Test
    public void testGetNumRows_500m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 2400);

        assertEquals(43200, grid.getNumRows());
    }

    @Test
    public void testGetNumRows_250m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 4800);

        assertEquals(86400, grid.getNumRows());
    }

    @Test
    public void testGetNumCols_1km() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 1200);

        assertEquals(43200, grid.getNumCols(12));
    }

    @Test
    public void testGetNumCols_500m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 2400);

        assertEquals(86400, grid.getNumCols(13));
    }

    @Test
    public void testGetNumCols_250m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 4800);

        assertEquals(172800, grid.getNumCols(14));
    }

    @Test
    public void testGetFirstBinIndex_1km() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 1200);

        assertEquals(0, grid.getFirstBinIndex(0));
        assertEquals(140000, grid.getFirstBinIndex(14));
        assertEquals(10006760000L, grid.getFirstBinIndex(1876));
        assertEquals(170011990000L, grid.getFirstBinIndex(21599));
    }

    @Test
    public void testGetFirstBinIndex_500m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 2400);

        assertEquals(0, grid.getFirstBinIndex(0));
        assertEquals(150000, grid.getFirstBinIndex(15));
        assertEquals(18770000, grid.getFirstBinIndex(1877));
        assertEquals(170023990000L, grid.getFirstBinIndex(43199));
    }

    @Test
    public void testGetFirstBinIndex_250m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 4800);

        assertEquals(0, grid.getFirstBinIndex(0));
        assertEquals(160000, grid.getFirstBinIndex(16));
        assertEquals(18780000, grid.getFirstBinIndex(1878));
        assertEquals(170047990000L, grid.getFirstBinIndex(86399));
    }

    @Test
    public void testWhyBinningLooksStrange() throws ParseException {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 1200);

        double[] longitudes = new double[]{26.591326970628398, 26.59068144632278, 26.59003592201747, 26.589390397712634, 26.588744873408427, 26.588099349105022, 26.587453824802584, 26.586808300501268, 26.58616277620124, 26.58551725190267, 26.584871727605716, 26.584226203310546, 26.58358067901732, 26.582935154726204, 26.582289630437366, 26.58457289162023, 26.583927260656154, 26.583281629694852, 26.58263599873649, 26.581990367781223, 26.581344736829227, 26.58069910588066, 26.58005347493569, 26.57940784399447, 26.57876221305718, 26.57811658212396, 26.577470951195004, 26.57682532027046, 26.57682532027046, 26.576179689350486, 26.57553405843526, 26.57488987806818, 26.574245697696185, 26.573601517319442, 26.572957336938106, 26.572313156552347, 26.57166897616232, 26.5710247957682, 26.570380615370134, 26.569736434968302, 26.56909225456285, 26.56844807415396, 26.567803893741775, 26.567159713326465, 26.566515532908202, 26.565871352487136, 26.56815279348597, 26.56750850863958, 26.566864223790876, 26.566219938940016, 26.565575654087173, 26.564931369232504, 26.564287084376165, 26.56364279951833, 26.562998514659157, 26.562354229798807, 26.561709944937444, 26.561065660075233, 26.56042137521234, 26.559777090348916, 26.559132805485138, 26.558488520621164, 26.557844235757152, 26.55719995089327, 26.55655566602968, 26.555911381166545, 26.555267096304025, 26.554622811442286, 26.553978526581492, 26.553334241721803, 26.552689956863386, 26.552045672006397, 26.551401387151007};
        double[] latitudes = new double[]{38.62726306915283, 38.62465977668762, 38.62205648422241, 38.6194531917572, 38.61684989929199, 38.61424660682678, 38.61164331436157, 38.60904002189636, 38.60643672943115, 38.60383343696594, 38.60123014450073, 38.59862685203552, 38.59602355957031, 38.5934202671051, 38.59081697463989, 38.58776557445526, 38.58516228199005, 38.58255898952484, 38.57995569705963, 38.57735240459442, 38.57474911212921, 38.572145819664, 38.56954252719879, 38.56693923473358, 38.56433594226837, 38.56173264980316, 38.55912935733795, 38.55652606487274, 38.55652606487274, 38.55392277240753, 38.55131947994232, 38.548716026358306, 38.54611257277429, 38.543509119190276, 38.54090566560626, 38.538302212022245, 38.53569875843823, 38.533095304854214, 38.5304918512702, 38.52788839768618, 38.52528494410217, 38.52268149051815, 38.52007803693414, 38.51747458335012, 38.51487112976611, 38.51226767618209, 38.509216129779816, 38.50661267712712, 38.50400922447443, 38.50140577182174, 38.498802319169044, 38.49619886651635, 38.49359541386366, 38.490991961210966, 38.48838850855827, 38.48578505590558, 38.48318160325289, 38.480578150600195, 38.4779746979475, 38.47537124529481, 38.47276779264212, 38.470164339989424, 38.46756088733673, 38.46495743468404, 38.462353982031345, 38.45975052937865, 38.45714707672596, 38.45454362407327, 38.451940171420574, 38.44933671876788, 38.44673326611519, 38.444129813462496, 38.4415263608098};
        long[] binIndizes = new long[]{52001640092L, 52001650093L, 52001650092L, 52001660093L, 52001660092L, 52001670093L, 52001680093L, 52001690093L, 52001700093L, 52001710093L, 52001720093L, 52001730093L, 52001740093L, 52001750093L, 52001760093L, 52001770093L, 52001780093L, 52001780094L, 52001790094L, 52001800094L, 52001810094L, 52001820094L, 52001820095L, 52001830095L, 52001840095L, 52001850095L, 52001860095L, 52001870095L};

        for (int i = 0; i < longitudes.length; i++) {
            System.out.println(grid.getBinIndex(latitudes[i], longitudes[i]));
        }

//        final WKTReader wktReader = new WKTReader();
//        final Geometry geometry = wktReader.read("POLYGON((26.591326970628398 38.62726306915283, 26.591326970628398 38.48318160325289, 26.561709944937444 38.48318160325289, 26.561709944937444 38.62726306915283, 26.591326970628398 38.62726306915283))");
//        final Rectangle rasterRegion = Reprojector.computeRasterSubRegion(grid, geometry);
//
//        final int x1 = rasterRegion.x;
//        final int x2 = x1 + rasterRegion.width - 1;
//        final int y1 = rasterRegion.y;
//        final int y2 = y1 + rasterRegion.height - 1;
//
//        int yUltimate = -1;
//        for (int i = 0; i < binIndizes.length; i++) {
//            final int y = grid.getRowIndex(binIndizes[i]);
//            if (y != yUltimate) {
//                System.out.println("process row " + y);
//                final double lat = 90.0 - (y + 0.5) * 180.0 / 21600;
//
//                for (int x = x1; x <= x2; x++) {
//                    double lon = -180.0 + (x + 0.5) * 360.0 / 43200;
//                    long wantedBinIndex = grid.getBinIndex(lat, lon);
//                    System.out.println(lon + " " + lat + " " + wantedBinIndex);
//                }
//
//                yUltimate = y;
//            }
//        }
    }
}
