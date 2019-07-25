package org.esa.snap.core.util.grid.isin;

import org.junit.Before;
import org.junit.Test;

import static org.esa.snap.core.util.grid.isin.IsinAPI.TO_RAD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class IsinForwardTest {

    private IsinForward forward;

    @Before
    public void setUp() {
        forward = new IsinForward();
    }

    @Test
    public void testInit_radiusTooSmall() {
        try {
            // -----------I--------------------
            forward.init(0.0, 0.0, 0.0, 0.0, 108.0, 1.0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testInit_centerLon_out_of_range() {
        try {
            // --------------------I--------------------
            forward.init(208.4, -6.32, 0.0, 0.0, 108.0, 1.0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            // ------------------I--------------------
            forward.init(208.4, 6.9, 0.0, 0.0, 108.0, 1.0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testInit_invalidNZone() {
        try {
            // ------------------------------I--------
            forward.init(12, 0.0, 0.0, 0.0, 108.3, 1.0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testInit_oddNZone() {
        try {
            // -------------------------------I--------
            forward.init(12, 0.0, 0.0, 0.0, 107.0004, 1.0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testInit_zoneOutOfRange() {
        try {
            // -----------------------------I--------
            forward.init(12, 0.0, 0.0, 0.0, 0, 1.0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            // ---------------------------------I--------
            forward.init(12, 0.0, 0.0, 0.0, 361 * 3600, 1.0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testInit_invalidDJustify() {
        try {
            // -----------------------------------I----
            forward.init(12, 0.0, 0.0, 0.0, 22, -0.03);    // below -eps
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            // ----------------------------------I----
            forward.init(12, 0.0, 0.0, 0.0, 22, 2.1);  // above 2 + eps
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            // ----------------------------------I----
            forward.init(12, 0.0, 0.0, 0.0, 22, 0.6);  // too far away from integer value
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testInit() {
        forward.init(6371007.181, 0.0, 0.0, 0.0, 21600.0, 1.0);

        assertEquals(0.0, forward.false_east, 1e-8);
        assertEquals(0.0, forward.false_north, 1e-8);
        assertEquals(6371007.181, forward.sphere, 1e-8);
        assertEquals(1.5696105365918595E-7, forward.sphere_inv, 1e-8);
        assertEquals(6875.493541569879, forward.ang_size_inv, 1e-8);
        assertEquals(21600, forward.nrow);
        assertEquals(10800, forward.nrow_half);
        assertEquals(0.0, forward.lon_cen_mer, 1e-8);
        assertEquals(-3.141592653589793, forward.ref_lon, 1e-8);
        assertEquals(926.6254331387694, forward.col_dist, 1e-8);
        assertEquals(0.0010791847107117362, forward.col_dist_inv, 1e-8);
        assertEquals(1, forward.ijustify);

        assertEquals(10800, forward.row.length);

        assertEquals(3, forward.row[0].ncol);
        assertEquals(2, forward.row[0].icol_cen);
        assertEquals(0.3333333333333333, forward.row[0].ncol_inv, 1e-8);

        assertEquals(110, forward.row[17].ncol);
        assertEquals(55, forward.row[17].icol_cen);
        assertEquals(0.00909090909090909, forward.row[17].ncol_inv, 1e-8);

        assertEquals(1680, forward.row[267].ncol);
        assertEquals(840, forward.row[267].icol_cen);
        assertEquals(5.952380952380953E-4, forward.row[267].ncol_inv, 1e-8);

        assertEquals(10234, forward.row[1644].ncol);
        assertEquals(5117, forward.row[1644].icol_cen);
        assertEquals(9.771350400625366E-5, forward.row[1644].ncol_inv, 1e-8);

        assertEquals(43200, forward.row[10799].ncol);
        assertEquals(21600, forward.row[10799].icol_cen);
        assertEquals(2.3148148148148147E-5, forward.row[10799].ncol_inv, 1e-8);
    }

    @Test
    public void testTransform_ISIN_K() {
        forward.init(6371007.181, 0.0, 0.0, 0.0, 10800.0, 1.0);

        // front pole
        IsinPoint point = new IsinPoint(0.0, 0.0);

        IsinPoint result = forward.transform(point);
        assertEquals(0.0, result.getX(), 1e-8);
        assertEquals(0.0, result.getY(), 1e-8);

        // Cap d'Ambre
        point = new IsinPoint(49.2630611 * TO_RAD, -11.95303056 * TO_RAD);

        result = forward.transform(point);
        assertEquals(5357942.558001757, result.getX(), 1e-8);
        assertEquals(-1329117.8543977137, result.getY(), 1e-8);

        // Mount Everest
        point = new IsinPoint(86.9249751 * TO_RAD, 27.9881206 * TO_RAD);

        result = forward.transform(point);
        assertEquals(8533911.62188331, result.getX(), 1e-8);
        assertEquals(3112140.5248458134, result.getY(), 1e-8);

        // Labuan Dabu
        point = new IsinPoint(133.1537194 * TO_RAD, -5.27245556 * TO_RAD);

        result = forward.transform(point);
        assertEquals(1.474273084810617E7, result.getX(), 1e-8);
        assertEquals(-586270.9700387894, result.getY(), 1e-8);
    }

    @Test
    public void testTransform_ISIN_H() {
        forward.init(6371007.181, 0.0, 0.0, 0.0, 21600.0, 1.0);

        // front pole
        IsinPoint point = new IsinPoint(0.0, 0.0);

        IsinPoint result = forward.transform(point);
        assertEquals(0.0, result.getX(), 1e-8);
        assertEquals(0.0, result.getY(), 1e-8);

        // Hamburg
        point = new IsinPoint(9.993682 *TO_RAD, 53.551086 * TO_RAD);

        result = forward.transform(point);
        assertEquals(660163.620386195, result.getX(), 1e-8);
        assertEquals(5954615.7911761785, result.getY(), 1e-8);

        // GITZ
        point = new IsinPoint(10.423067 * TO_RAD, 53.408436 * TO_RAD);

        result = forward.transform(point);
        assertEquals(690345.0908636916, result.getX(), 1e-8);
        assertEquals(5938753.817011709, result.getY(), 1e-8);

        // Canteen
        point = new IsinPoint(10.428581 * TO_RAD, 53.405065 * TO_RAD);

        result = forward.transform(point);
        assertEquals(691308.0680468029, result.getX(), 1e-8);
        assertEquals(5938378.978491495, result.getY(), 1e-8);
    }

    @Test
    public void testTransform_ISIN_Q() {
        forward.init(6371007.181, 0.0, 0.0, 0.0, 43200.0, 1.0);

        // front pole
        IsinPoint point = new IsinPoint(0.0, 0.0);

        IsinPoint result = forward.transform(point);
        assertEquals(0.0, result.getX(), 1e-8);
        assertEquals(0.0, result.getY(), 1e-8);

        // Lauthala cape
        point = new IsinPoint(-179.6968528 * TO_RAD, -16.7395278 * TO_RAD);

        result = forward.transform(point);
        assertEquals(-1.9134966934145495E7, result.getX(), 1e-8);
        assertEquals(-1861352.6637856164, result.getY(), 1e-8);

        // Mauna Loa
        point = new IsinPoint(-155.6050194 * TO_RAD, 19.4782583 * TO_RAD);

        result = forward.transform(point);
        assertEquals(-1.6312420249643698E7, result.getX(), 1e-8);
        assertEquals(2165885.9440831593, result.getY(), 1e-8);

        // Isla Herschel
        point = new IsinPoint(-67.4067333 * TO_RAD, -55.9043278 * TO_RAD);

        result = forward.transform(point);
        assertEquals(-4201674.277359434, result.getX(), 1e-8);
        assertEquals(-6216284.635440809, result.getY(), 1e-8);
    }
}
