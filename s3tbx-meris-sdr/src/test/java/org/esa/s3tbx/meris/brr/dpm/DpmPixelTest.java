package org.esa.s3tbx.meris.brr.dpm;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DpmPixelTest {

    @Test
    public void testReset() {
        final DpmPixel dpmPixel = new DpmPixel(0,0);

        dpmPixel.x = 3;
        dpmPixel.y = 4;
        dpmPixel.i = 5;
        dpmPixel.j = 6;
        dpmPixel.detector = 7;
        dpmPixel.view_zenith = 8.0;
        dpmPixel.sun_zenith = 9.0;
        dpmPixel.delta_azimuth = 10.0;
        dpmPixel.sun_azimuth = 11.0;
        dpmPixel.mus = 12.0;
        dpmPixel.muv = 13.0;
        dpmPixel.airMass = 1.0;
        dpmPixel.altitude = 2.0;
        dpmPixel.windu = 14.0;
        dpmPixel.windv = 15.0;
        dpmPixel.press_ecmwf = 16.0;
        dpmPixel.ozone_ecmwf = 17.0;
        dpmPixel.l1flags = 18;
        dpmPixel.l2flags = 19;
        dpmPixel.SATURATED_F = 20;
        dpmPixel.ANNOT_F = 21;
        for (int i = 0; i < dpmPixel.TOAR.length; i++) {
            dpmPixel.TOAR[i] = i+1;
        }
        for (int i = 0; i < dpmPixel.rho_ag.length; i++) {
            dpmPixel.rho_ag[i] = i+1;
        }
        for (int i = 0; i < dpmPixel.rho_toa.length; i++) {
            dpmPixel.rho_toa[i] = i+1;
        }
        for (int i = 0; i < dpmPixel.rho_top.length; i++) {
            dpmPixel.rho_top[i] = i+1;
        }
        for (int i = 0; i < dpmPixel.rhoR.length; i++) {
            dpmPixel.rhoR[i] = i+1;
        }
        for (int i = 0; i < dpmPixel.transRv.length; i++) {
            dpmPixel.transRv[i] = i+1;
        }
        for (int i = 0; i < dpmPixel.transRs.length; i++) {
            dpmPixel.transRs[i] = i+1;
        }
        for (int i = 0; i < dpmPixel.sphalbR.length; i++) {
            dpmPixel.sphalbR[i] = i+1;
        }

        dpmPixel.reset(567, 665);

        assertEquals(567, dpmPixel.i);
        assertEquals(665, dpmPixel.j);

        assertEquals(0, dpmPixel.x);
        assertEquals(0, dpmPixel.y);
        assertEquals(0, dpmPixel.detector);
        assertEquals(0.0, dpmPixel.view_zenith, 1e-8);
        assertEquals(0.0, dpmPixel.sun_zenith, 1e-8);
        assertEquals(0.0, dpmPixel.delta_azimuth, 1e-8);
        assertEquals(0.0, dpmPixel.sun_azimuth, 1e-8);
        assertEquals(0.0, dpmPixel.mus, 1e-8);
        assertEquals(0.0, dpmPixel.muv, 1e-8);
        assertEquals(0.0, dpmPixel.airMass, 1e-8);
        assertEquals(0.0, dpmPixel.altitude, 1e-8);
        assertEquals(0.0, dpmPixel.windu, 1e-8);
        assertEquals(0.0, dpmPixel.windv, 1e-8);
        assertEquals(0.0, dpmPixel.press_ecmwf, 1e-8);
        assertEquals(0.0, dpmPixel.ozone_ecmwf, 1e-8);
        assertEquals(0, dpmPixel.l1flags);
        assertEquals(0, dpmPixel.l2flags);
        assertEquals(0, dpmPixel.SATURATED_F);
        assertEquals(0, dpmPixel.ANNOT_F);
        for (int i = 0; i < dpmPixel.TOAR.length; i++) {
            assertEquals(0.0, dpmPixel.TOAR[i], 1e-8);
        }
        for (int i = 0; i < dpmPixel.rho_ag.length; i++) {
            assertEquals(0.0, dpmPixel.rho_ag[i], 1e-8);
        }
        for (int i = 0; i < dpmPixel.rho_toa.length; i++) {
            assertEquals(0.0, dpmPixel.rho_toa[i], 1e-8);
        }
        for (int i = 0; i < dpmPixel.rho_top.length; i++) {
            assertEquals(0.0, dpmPixel.rho_top[i], 1e-8);
        }
        for (int i = 0; i < dpmPixel.rhoR.length; i++) {
            assertEquals(0.0, dpmPixel.rhoR[i], 1e-8);
        }
        for (int i = 0; i < dpmPixel.transRv.length; i++) {
            assertEquals(0.0, dpmPixel.transRv[i], 1e-8);
        }
        for (int i = 0; i < dpmPixel.transRs.length; i++) {
            assertEquals(0.0, dpmPixel.transRs[i], 1e-8);
        }
        for (int i = 0; i < dpmPixel.sphalbR.length; i++) {
            assertEquals(0.0, dpmPixel.sphalbR[i], 1e-8);
        }
    }
}
