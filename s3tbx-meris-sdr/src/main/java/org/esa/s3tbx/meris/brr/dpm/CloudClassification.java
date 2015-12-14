/*
 * $Id: CloudClassification.java,v 1.1 2007/03/27 12:52:22 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.brr.dpm;

import org.esa.s3tbx.meris.dpm.PixelId;
import org.esa.s3tbx.meris.l2auxdata.Constants;
import org.esa.s3tbx.meris.l2auxdata.L2AuxData;
import org.esa.snap.core.util.BitSetter;


/**
 * The MERIS Level 2 cloud classification module.
 */
@SuppressWarnings("JavaDoc")
public class CloudClassification implements Constants {

    private L2AuxData auxData;

    private LocalHelperVariables lh;

    private RayleighCorrection rayleighCorrection;
    private final PixelId pixelId;

    /**
     * Constructs the module
     */
    public CloudClassification(L2AuxData auxData, RayleighCorrection rayCorr) {
        this.auxData = auxData;
        pixelId = new PixelId(auxData);
        lh = new LocalHelperVariables();
        rayleighCorrection = rayCorr;
    }

    public void classify_cloud(DpmPixel pixel) {

        //boolean pcd_poly = Comp_Pressure(pixel) != 0;
        PixelId.Pressure press = pixelId.computePressure(pixel.rho_toa[bb753],
                                                         pixel.rho_toa[bb760],
                                                         pixel.airMass,
                                                         pixel.detector);
        boolean pcd_poly = press.error;

        /* apply thresholds on pressure- step 2.1.2 */
        double delta_press_thresh = pixelId.getPressureThreshold(pixel.sun_zenith, pixel.view_zenith,
                                                                 BitSetter.isFlagSet(pixel.l2flags, F_LAND));
        boolean[] pressureThreshFlags = pixelId.getPressureThreshFlags(pixel.press_ecmwf, press.value, -1, delta_press_thresh);

        boolean low_P_nn = pressureThreshFlags[0];
        boolean low_P_poly = pressureThreshFlags[1];
        boolean delta_p = pressureThreshFlags[2];

        /* keep for display-debug - added for v2.1 */
        pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_LOW_NN_P, low_P_nn);
        pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_PCD_NN_P, true);    /* DPM #2.1.5-25 */
        pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_LOW_POL_P, low_P_poly);
        pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_PCD_POL_P, pcd_poly); /* DPM #2.1.12-12 */
        pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_CONFIDENCE_P, delta_p);

        // Compute slopes- step 2.1.7
        spec_slopes(pixel, lh.resultFlags);
        boolean bright_f = lh.resultFlags[0];
        boolean slope_1_f = lh.resultFlags[1];
        boolean slope_2_f = lh.resultFlags[2];
        pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_BRIGHT, bright_f);
        pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_SLOPE_1, slope_1_f);
        pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_SLOPE_2, slope_2_f);

        // table-driven classification- step 2.1.8
        // DPM #2.1.8-1
        boolean land_f = BitSetter.isFlagSet(pixel.l2flags, F_LAND);
        boolean is_cloud = is_cloudy(land_f,
                                     bright_f,
                                     low_P_nn, low_P_poly, delta_p,
                                     slope_1_f, slope_2_f,
                                     true, pcd_poly);

        pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_CLOUD, is_cloud);
    }

    /**
     * Computes the slope of Rayleigh-corrected reflectance.
     *
     * @param pixel        the pixel structure
     * @param result_flags the return values, <code>resultFlags[0]</code> contains low NN pressure flag (low_P_nn),
     *                     <code>resultFlags[1]</code> contains low polynomial pressure flag (low_P_poly),
     *                     <code>resultFlags[2]</code> contains pressure range flag (delta_p).
     */
    private void spec_slopes(DpmPixel pixel, boolean[] result_flags) {

        /* shorthand for access */
        long flags = pixel.l2flags;

        double sins = Math.sin(RAD * pixel.sun_zenith);
        double sinv = Math.sin(RAD * pixel.view_zenith);

        /* Rayleigh phase function Fourier decomposition */
        rayleighCorrection.phase_rayleigh(pixel.mus, pixel.muv, sins, sinv, lh.phaseR);

        double press = pixel.press_ecmwf; /* DPM #2.1.7-1 v1.1 */

        /* Rayleigh optical thickness */
        rayleighCorrection.tau_rayleigh(press, lh.tauR); /* DPM #2.1.7-2 */

        /* Rayleigh reflectance - DPM #2.1.7-3 - v1.3 */
        rayleighCorrection.ref_rayleigh(pixel.delta_azimuth, pixel.sun_zenith, pixel.view_zenith,
                                        pixel.mus, pixel.muv, pixel.airMass, lh.phaseR, lh.tauR, lh.rhoRay);

        /* DPM #2.1.7-4 */
        for (int band = bb412; band <= bb900; band++) {
            lh.rhoAg[band] = pixel.rho_toa[band] - lh.rhoRay[band];
        }

        /* Interpolate threshold on rayleigh corrected reflectance - DPM #2.1.7-9 */
        double rhorc_442_thr = pixelId.getRhoRC442thr(pixel.sun_zenith, pixel.view_zenith, pixel.delta_azimuth, BitSetter.isFlagSet(flags, F_LAND));

        boolean bright_f = pixelId.isBrightFlag(lh.rhoAg, rhorc_442_thr, pixel.TOAR[auxData.band_bright_n]);

        /* Spectral slope processor.brr 1 */
        boolean slope1_f = pixelId.isSpectraSlope1Flag(lh.rhoAg, pixel.TOAR[auxData.band_slope_n_1]);
        /* Spectral slope processor.brr 2 */
        boolean slope2_f = pixelId.isSpectraSlope2Flag(lh.rhoAg, pixel.TOAR[auxData.band_slope_n_2]);

        result_flags[0] = bright_f;
        result_flags[1] = slope1_f;
        result_flags[2] = slope2_f;
    }

    /**
     * Table driven cloud classification decision.
     * <p/>
     * <b>DPM Ref.:</b> Level 2, Step 2.1.8 <br> <b>MEGS Ref.:</b> file classcloud.c, function class_cloud  <br>
     *
     * @param land_f
     * @param bright_f
     * @param low_P_nn
     * @param low_P_poly
     * @param delta_p
     * @param slope_1_f
     * @param slope_2_f
     * @param pcd_nn
     * @param pcd_poly
     * @return <code>true</code> if cloud flag shall be set
     */
    private boolean is_cloudy(boolean land_f, boolean bright_f,
                              boolean low_P_nn, boolean low_P_poly,
                              boolean delta_p, boolean slope_1_f,
                              boolean slope_2_f, boolean pcd_nn,
                              boolean pcd_poly) {
        boolean is_cloud;
        int index = 0;

        /* set bits of index according to inputs */
        index = BitSetter.setFlag(index, CC_BRIGHT, bright_f);
        index = BitSetter.setFlag(index, CC_LOW_P_NN, low_P_nn);
        index = BitSetter.setFlag(index, CC_LOW_P_PO, low_P_poly);
        index = BitSetter.setFlag(index, CC_DELTA_P, delta_p);
        index = BitSetter.setFlag(index, CC_PCD_NN, pcd_nn);
        index = BitSetter.setFlag(index, CC_PCD_PO, pcd_poly);
        index = BitSetter.setFlag(index, CC_SLOPE_1, slope_1_f);
        index = BitSetter.setFlag(index, CC_SLOPE_2, slope_2_f);
        index &= 0xff;

        /* readRecord decision table */
        if (land_f) {
            is_cloud = auxData.land_decision_table[index]; /* DPM #2.1.8-1 */
        } else {
            is_cloud = auxData.water_decision_table[index]; /* DPM #2.1.8-2 */
        }

        return is_cloud;
    }

    private static class LocalHelperVariables {
        /**
         * Rayleigh phase function coefficients, PR in DPM. Local helper variable used in {@link CloudClassification#spec_slopes}.
         */
        final double[] phaseR = new double[RAYSCATT_NUM_SER];
        /**
         * Rayleigh optical thickness, tauR0 in DPM . Local helper variable used in {@link CloudClassification#spec_slopes}.
         */
        final double[] tauR = new double[L1_BAND_NUM];
        /**
         * Rayleigh corrected reflectance. Local helper variable used in {@link CloudClassification#spec_slopes}.
         */
        final double[] rhoAg = new double[L1_BAND_NUM];
        /**
         * Rayleigh correction. Local helper variable used in {@link CloudClassification#spec_slopes}.
         */
        final double[] rhoRay = new double[L1_BAND_NUM];
        /**
         * Array of flags used as return value by some functions. Local helper variable used in {@link CloudClassification#spec_slopes}.
         */
        final boolean[] resultFlags = new boolean[3];
    }

}
