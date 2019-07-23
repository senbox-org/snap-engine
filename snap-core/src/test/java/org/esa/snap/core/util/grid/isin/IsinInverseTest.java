package org.esa.snap.core.util.grid.isin;

import org.junit.Before;
import org.junit.Test;

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
}
