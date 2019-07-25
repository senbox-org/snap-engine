package org.esa.snap.core.util.grid.isin;

import org.junit.Before;
import org.junit.Test;

import static org.esa.snap.core.util.grid.isin.IsinAPI.TO_RAD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class IsinInverseTest {

    private IsinInverse inverse;

    @Before
    public void setUp() {
        inverse = new IsinInverse();
    }

    @Test
    public void testInit_radiusTooSmall() {
        try {
            // -----------I--------------------
            inverse.init(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testInit_centerLon_out_of_range() {
        try {
            // --------------------I--------------------
            inverse.init(10776.4, -6.33, 0.0, 0.0, 108.0, 1.0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            // ------------------I--------------------
            inverse.init(10776.4, 6.91, 0.0, 0.0, 108.0, 1.0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testInit_zoneOutOfRange() {
        try {
            // -----------------------------I--------
            inverse.init(13, 0.0, 0.0, 0.0, 1.86, 1.0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            // ---------------------------------I--------
            inverse.init(13, 0.0, 0.0, 0.0, 361 * 3600, 1.0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testInit_invalidNZone() {
        try {
            // ------------------------------I--------
            inverse.init(12, 0.0, 0.0, 0.0, 21.83, 1.0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testInit_oddNZone() {
        try {
            // -------------------------------I--------
            inverse.init(12, 0.0, 0.0, 0.0, 23.0004, 1.0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testInit_invalidDJustify() {
        try {
            // -----------------------------------I----
            inverse.init(13, 0.0, 0.0, 0.0, 22, -0.03);    // below -eps
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            // ----------------------------------I----
            inverse.init(14, 0.0, 0.0, 0.0, 22, 2.1);  // above 2 + eps
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            // ----------------------------------I----
            inverse.init(15, 0.0, 0.0, 0.0, 22, 0.6);  // too far away from integer value
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testInit() {
        inverse.init(6371007.181, 0.0, 12.0, 14.0, 21600.0, 1.0);

        assertEquals(12.0, inverse.false_east, 1e-8);
        assertEquals(14.0, inverse.false_north, 1e-8);
        assertEquals(6371007.181, inverse.sphere, 1e-8);
        assertEquals(1.5696105365918595E-7, inverse.sphere_inv, 1e-8);
        assertEquals(6875.493541569879, inverse.ang_size_inv, 1e-8);
        assertEquals(21600, inverse.nrow);
        assertEquals(10800, inverse.nrow_half);
        assertEquals(0.0, inverse.lon_cen_mer, 1e-8);
        assertEquals(-3.141592653589793, inverse.ref_lon, 1e-8);
        assertEquals(1, inverse.ijustify);

        assertEquals(10800, inverse.row.length);

        assertEquals(3, inverse.row[0].ncol);
        assertEquals(2, inverse.row[0].icol_cen);
        assertEquals(0.3333333333333333, inverse.row[0].ncol_inv, 1e-8);

        assertEquals(179, inverse.row[28].ncol);
        assertEquals(90, inverse.row[28].icol_cen);
        assertEquals(0.00558659217877095, inverse.row[28].ncol_inv, 1e-8);

        assertEquals(1680, inverse.row[267].ncol);
        assertEquals(840, inverse.row[267].icol_cen);
        assertEquals(5.952380952380953E-4, inverse.row[267].ncol_inv, 1e-8);

        assertEquals(33842, inverse.row[6188].ncol);
        assertEquals(16921, inverse.row[6188].icol_cen);
        assertEquals(2.9549081023580167E-5, inverse.row[6188].ncol_inv, 1e-8);

        assertEquals(43200, inverse.row[10799].ncol);
        assertEquals(21600, inverse.row[10799].icol_cen);
        assertEquals(2.3148148148148147E-5, inverse.row[10799].ncol_inv, 1e-8);
    }

    @Test
    public void testInit_ref_lon_corrected() {
        inverse.init(6371007.181, -1.0, 12.0, 14.0, 21600.0, 2.0);

        assertEquals(12.0, inverse.false_east, 1e-8);
        assertEquals(14.0, inverse.false_north, 1e-8);
        assertEquals(6371007.181, inverse.sphere, 1e-8);
        assertEquals(1.5696105365918595E-7, inverse.sphere_inv, 1e-8);
        assertEquals(6875.493541569879, inverse.ang_size_inv, 1e-8);
        assertEquals(21600, inverse.nrow);
        assertEquals(10800, inverse.nrow_half);
        assertEquals(-1.0, inverse.lon_cen_mer, 1e-8);
        assertEquals(2.141592653589793, inverse.ref_lon, 1e-8);
        assertEquals(2, inverse.ijustify);
        assertEquals(926.6254331387694, inverse.col_dist, 1e-8);
        assertEquals(0.0010791847107117362, inverse.col_dist_inv, 1e-8);

        assertEquals(10800, inverse.row.length);

        assertEquals(4, inverse.row[0].ncol);
        assertEquals(2, inverse.row[0].icol_cen);
        assertEquals(0.25, inverse.row[0].ncol_inv, 1e-8);

        assertEquals(186, inverse.row[29].ncol);
        assertEquals(93, inverse.row[29].icol_cen);
        assertEquals(0.005376344086021506, inverse.row[29].ncol_inv, 1e-8);

        assertEquals(1686, inverse.row[268].ncol);
        assertEquals(843, inverse.row[268].icol_cen);
        assertEquals(5.931198102016608E-4, inverse.row[268].ncol_inv, 1e-8);

        assertEquals(33846, inverse.row[6189].ncol);
        assertEquals(16923, inverse.row[6189].icol_cen);
        assertEquals(2.9545588843585653E-5, inverse.row[6189].ncol_inv, 1e-8);

        assertEquals(43200, inverse.row[10799].ncol);
        assertEquals(21600, inverse.row[10799].icol_cen);
        assertEquals(2.3148148148148147E-5, inverse.row[10799].ncol_inv, 1e-8);
    }

    @Test
    public void testTransform_ISIN_K() {
        inverse.init(6371007.181, 0.0, 0.0, 0.0, 10800.0, 1.0);

        // front pole
        IsinPoint point = new IsinPoint(0.0, 0.0);

        IsinPoint result = inverse.transform(point);
        assertEquals(0.0, result.getX(), 1e-8);
        assertEquals(0.0, result.getY(), 1e-8);

        // Cap d'Ambre
        point = new IsinPoint(5357942.558001757, -1329117.8543977137);

        result = inverse.transform(point);
        assertEquals(49.2630611 * TO_RAD, result.getX(), 1e-8);
        assertEquals(-11.95303056 * TO_RAD, result.getY(), 1e-8);

        // Mount Everest
        point = new IsinPoint(8533911.62188331, 3112140.5248458134);

        result = inverse.transform(point);
        assertEquals(86.9249751 * TO_RAD, result.getX(), 1e-8);
        assertEquals(27.9881206 * TO_RAD, result.getY(), 1e-8);

        // Labuan Dabu
        point = new IsinPoint(1.474273084810617E7, -586270.9700387894);

        result = inverse.transform(point);
        assertEquals(133.1537194 * TO_RAD, result.getX(), 1e-8);
        assertEquals(-5.27245556 * TO_RAD, result.getY(), 1e-8);
    }

    @Test
    public void testTransform_ISIN_H() {
        inverse.init(6371007.181, 0.0, 0.0, 0.0, 21600.0, 1.0);

        // front pole
        IsinPoint point = new IsinPoint(0.0, 0.0);

        IsinPoint result = inverse.transform(point);
        assertEquals(0.0, result.getX(), 1e-8);
        assertEquals(0.0, result.getY(), 1e-8);

        // Hamburg
        point = new IsinPoint(660163.620386195, 5954615.7911761785);

        result = inverse.transform(point);
        assertEquals(9.993682 * TO_RAD, result.getX(), 1e-8);
        assertEquals(53.551086 * TO_RAD, result.getY(), 1e-8);

        // GITZ
        point = new IsinPoint(690345.0908636916, 5938753.817011709);

        result = inverse.transform(point);
        assertEquals(10.423067 * TO_RAD, result.getX(), 1e-8);
        assertEquals(53.408436 * TO_RAD, result.getY(), 1e-8);

        // Canteen
        point = new IsinPoint(691308.0680468029, 5938378.978491495);

        result = inverse.transform(point);
        assertEquals(10.428581 * TO_RAD, result.getX(), 1e-8);
        assertEquals(53.405065 * TO_RAD, result.getY(), 1e-8);
    }

    @Test
    public void testTransform_ISIN_Q() {
        inverse.init(6371007.181, 0.0, 0.0, 0.0, 43200.0, 1.0);

        // front pole
        IsinPoint point = new IsinPoint(0.0, 0.0);

        IsinPoint result = inverse.transform(point);
        assertEquals(0.0, result.getX(), 1e-8);
        assertEquals(0.0, result.getY(), 1e-8);

        // Lauthala cape
        point = new IsinPoint(-1.9134966934145495E7, -1861352.6637856164);

        result = inverse.transform(point);
        assertEquals(-179.6968528 * TO_RAD, result.getX(), 1e-8);
        assertEquals(-16.7395278 * TO_RAD, result.getY(), 1e-8);

        // Mauna Loa
        point = new IsinPoint(-1.6312420249643698E7, 2165885.9440831593);

        result = inverse.transform(point);
        assertEquals(-155.6050194 * TO_RAD, result.getX(), 1e-8);
        assertEquals(19.4782583 * TO_RAD, result.getY(), 1e-8);

        // Isla Herschel
        point = new IsinPoint(-4201674.277359434, -6216284.635440809);

        result = inverse.transform(point);
        assertEquals(-67.4067333 * TO_RAD, result.getX(), 1e-8);
        assertEquals(-55.9043278 * TO_RAD, result.getY(), 1e-8);
    }
}
