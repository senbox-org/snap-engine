/*
 * $Id: PixelIdentification.java,v 1.1 2007/03/27 12:52:22 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.brr.dpm;

import org.esa.s3tbx.meris.brr.operator.CorrectionSurfaceEnum;
import org.esa.s3tbx.meris.l2auxdata.Constants;
import org.esa.s3tbx.meris.l2auxdata.L2AuxData;
import org.esa.s3tbx.util.math.FractIndex;
import org.esa.s3tbx.util.math.Interp;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.core.util.math.MathUtils;

/**
 * The MERIS Level 2 pixel identification module.
 * <p/>
 * <b>DPM ref.:</b> Chapter 5 <br> <b>MEGS ref.:</b> <code>pixelid.c</code>, <code>pixel_classification.c</code> <br>
 *
 * @author Norman Fomferra
 */
@SuppressWarnings("JavaDoc")
public class PixelIdentification implements Constants {

//    private boolean correctWater = false;
    private CorrectionSurfaceEnum correctionSurface;
    private GaseousAbsorptionCorrection gaseousCorr;
    private LocalHelperVariables lh;
    private L2AuxData auxData;

    /**
     * Constructs the module
     */
    public PixelIdentification(L2AuxData auxData, GaseousAbsorptionCorrection gasCorr) {
        this.auxData = auxData;
        lh = new LocalHelperVariables();
        gaseousCorr = gasCorr;
    }

//    public void setCorrectWater(boolean correctLandOnly) {
//        this.correctWater = correctLandOnly;
//    }
    public void setCorrectionSurface(CorrectionSurfaceEnum correctionSurface) {
        this.correctionSurface = correctionSurface;
    }

/*----------------------------------------------------------------*\
 * Function pixel_classification: schedule pixels classification operations
 * inputs:
 *    pPixel1       pixel structure
 *    Head.f0       Sun irradiance
 *    r7thresh      threshold on toa reflectance @665 (global)
 *    alpha_thresh  aerosol model by properties (global)
 * outputs:
 *    pPixel1       pixel structure
 *       rho_ag
 *       l2flags_1
 *       l2flags_2
 * reference: DPM L2 5.5.5, 5.5.6: Pixel Identification
 * called by: step2_l2
 * calls:
 *    read_flag_l2
 *    set_flag_l2
 *    trans_o3
 *    InterpCoord
 *    GenericInterp
 *    gas_correction
 *    uncertain
 *    inland_waters
 *    islands
 *    land_cons
 *
 * V.2    07/07/98 fm  new architecture: rad2reflec, strato_corr are
 *                     done upstream
 *                     run on a 4x4 sub-window
 *                     correction for gaseous absorption is done here
 * V2.1   26/10/98     change i/f to gas correction
 * v4.2   24/08/99 fm  call 'uncertain' to switch land-water reclassification
 *        23/09/99     change logic and i/f to gas_correction
 *        25/11/99     save gas transmission to breakpoints file
 * v4.4   25/10/00     take delta_azimuth into account for threshold interpol
 *        03/01/01     take PCD_POL_F into account for gas correction
 * v4.5   29/05/02     work in reflectance instead of radiances L2 DPM 5.0 red mark
\*----------------------------------------------------------------*/

    public int pixel_classification(DpmPixel[][] pixelBlock,
                                    int ic0, int ic1, int il0, int il1) {
        int status = 0;
        boolean correctPixel = false;
        boolean correctWaterPixel = false;
        FractIndex[] r7thresh_Index = FractIndex.createArray(3);  /* v4.4 */  // todo - rm new
        boolean iOrinp0 = false;

        boolean[][] is_L1bland = new boolean[4][4];  // todo - rm new
        boolean[][] do_corr = new boolean[4][4];  // todo - rm new
        boolean is_water;
        boolean is_land;

        double[] dSumrho = new double[L1_BAND_NUM]; /* accumulator for rho above water */ // todo - rm new
        double etaAverageForWater, x2AverageForWater; /* ratio of averaged rho */

        double[] T_o3 = new double[L1_BAND_NUM];   /* ozone transmission */     // todo - rm new
        double r7thresh_val, r13thresh_val;

        etaAverageForWater = 0.;
        x2AverageForWater = 0.;

        for (int il = il0; il <= il1; il++) {
            for (int ic = ic0; ic <= ic1; ic++) {
                DpmPixel pixel = pixelBlock[il][ic];
                long flags = pixel.l2flags;

                if (!BitSetter.isFlagSet(flags, F_INVALID) /*&& !AlbedoUtils.isFlagSet(flags, F_CLOUD)*/) {
                    if (correctionSurface == CorrectionSurfaceEnum.LAND &&
                            pixel.altitude < -50.0 && !BitSetter.isFlagSet(pixel.l1flags, L1_F_LAND)) {
//                    if (!correctWater && pixel.altitude < -50.0 && !BitSetter.isFlagSet(pixel.l1flags, L1_F_LAND)) {
                        do_corr[il - il0][ic - ic0] = false;
                    } else {
                        correctPixel = true;
                        do_corr[il - il0][ic - ic0] = true;
                        is_L1bland[il - il0][ic - ic0] = BitSetter.isFlagSet(flags, F_LAND);

                        /* v4.2: average radiances for water pixels */
                        if (!is_L1bland[il - il0][ic - ic0]) {
                            correctWaterPixel = true;
                            for (int bandId = bb753; bandId <= bb900; bandId++) {
                                dSumrho[bandId] += pixel.rho_toa[bandId];
                            }
                        }
                    }
                } else {
                    do_corr[il - il0][ic - ic0] = false;
                }
            }
        }

        /* v4.2 average TOA radiance */
        if (correctWaterPixel) {
            if ((dSumrho[bb753] > 0.) && (dSumrho[bb760] > 0.)) {
                etaAverageForWater = dSumrho[bb760] / dSumrho[bb753];
            } else {
                iOrinp0 = true;
                etaAverageForWater = 1.;
            }

            if ((dSumrho[bb890] > 0.) && (dSumrho[bb900] > 0.)) {
                x2AverageForWater = dSumrho[bb900] / dSumrho[bb890];
            } else {
                iOrinp0 = true;
                x2AverageForWater = 1.;
            }
        }

        if (correctPixel) {

            DpmPixel pixel0 = pixelBlock[il0][ic1];

            /* v7: compute Glint reflectance here (only if there are water/land pixels) */
            /* first wind modulus at window corner */
            double windm = 0.0;
            windm += pixel0.windu * pixel0.windu;
            windm += pixel0.windv * pixel0.windv;
            windm = Math.sqrt(windm);
            /* then wind azimuth */
            double phiw = azimuth(pixel0.windu, pixel0.windv);
            /* and "scattering" angle */
            double chiw = MathUtils.RTOD * (Math.acos(Math.cos(pixel0.sun_azimuth - phiw)));
            /* allows to retrieve Glint reflectance for wurrent geometry and wind */
            double rhoGlint = glintRef(pixel0.sun_zenith,
                                       pixel0.view_zenith, pixel0.delta_azimuth, windm, chiw);

            /* V.2 APPLY GASEOUS ABSORPTION CORRECTION - DPM Step 2.6.12 */

            /* ozone transmittance on 4x4 window - step 2.6.12.1 */
            trans_o3(pixel0.airMass, pixel0.ozone_ecmwf, T_o3);

            /* set up threshold for land-water discrimination */
            Interp.interpCoord(pixel0.sun_zenith, auxData.r7thresh.getTab(0), r7thresh_Index[0]);
            Interp.interpCoord(pixel0.view_zenith, auxData.r7thresh.getTab(1), r7thresh_Index[1]);
            /* take azimuth difference into account - v4.4 */
            Interp.interpCoord(pixel0.delta_azimuth, auxData.r7thresh.getTab(2), r7thresh_Index[2]);
            /* DPM #2.6.26-1a */
            r7thresh_val = Interp.interpolate(auxData.r7thresh.getJavaArray(), r7thresh_Index);
            r13thresh_val = Interp.interpolate(auxData.r13thresh.getJavaArray(), r7thresh_Index);

            /* process each pixel */
            for (int il = il0; il <= il1; il++) {
                for (int ic = ic0; ic <= ic1; ic++) {
                    DpmPixel pixel = pixelBlock[il][ic];
                    if (do_corr[il - il0][ic - ic0]) {
                        double eta, x2;       /* band ratios eta, x2 */

                        /* test SZA - v4.2 */
                        if (pixel.sun_zenith > auxData.TETAS_LIM) {
                            pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_SUN70, true);
                        }

                        /* gaseous transmittance correction : writes rho-ag field - v4.2 */
                        /* do band ratio for land pixels with full exception handling */
                        if (is_L1bland[il - il0][ic - ic0]) {
                            if ((pixel.rho_toa[bb753] > 0.) && (pixel.rho_toa[bb760] > 0.)) {
                                eta = pixel.rho_toa[bb760] / pixel.rho_toa[bb753];    //o2
                            } else {
                                eta = 1.;
                                pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_ORINP0, true);
                            }
                            /* DPM #2.6.12.3-1 */
                            if ((pixel.rho_toa[bb890] > 0.) && (pixel.rho_toa[bb900] > 0.)) {
                                x2 = pixel.rho_toa[bb900] / pixel.rho_toa[bb890];   //h2o
                            } else {
                                x2 = 1.;
                                pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_ORINP0, true);
                            }
                        } else { /* water pixels */
                            eta = etaAverageForWater;
                            x2 = x2AverageForWater;
                            pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_ORINP0, iOrinp0);
                        }

                        status = gaseousCorr.gas_correction(T_o3, eta, x2,
                                                            pixel.rho_toa,
                                                            pixel.detector,
                                                            pixel.rho_ag,
                                                            BitSetter.isFlagSet(pixel.l2flags, F_PCD_POL_P));

                        /* exception handling */
                        pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_OROUT0, status != 0);

                        boolean is_land_consolidated = false;
                        if (!BitSetter.isFlagSet(pixel.l2flags, F_CLOUD)) {
                            /* Land /Water re-classification - v4.2, updated for v7 */
                            /* DPM step 2.6.26 */
                            /* TODO: restrict land-water reclassification to altitude > -50 */

                            int b_thresh;           /*added V7 to manage 2 bands reclassif threshold LUT */
                            double a_thresh;  /*added V7 to manage 2 bands reclassif threshold LUT */
                            double rThresh;

                            /* test if pixel is water */
                            b_thresh = auxData.lap_b_thresh[0];
                            a_thresh = auxData.alpha_thresh[0];
                            is_water = inland_waters(r7thresh_val, pixel, b_thresh, a_thresh);
                            /* the is_water flag is available in the output product as F_LOINLD */

                            /* test if pixel is land */
                            final float thresh_medg = 0.2f;
                            boolean isGlint = (rhoGlint >= thresh_medg * pixel.rho_ag[bb865]);
                            if (isGlint) {
                                pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_MEGLINT, isGlint);
                                b_thresh = auxData.lap_b_thresh[0];
                                a_thresh = auxData.alpha_thresh[0];
                                rThresh = r7thresh_val;
                            } else {
                                b_thresh = auxData.lap_b_thresh[1];
                                a_thresh = auxData.alpha_thresh[1];
                                rThresh = r13thresh_val;
                            }
                            is_land = island(rThresh, pixel, b_thresh, a_thresh);
                            /* the is_land flag is available in the output product as F_ISLAND */

                            // DPM step 2.6.26-7
                            // DPM #2.6.26-6
                            // TODO: reconsider to user the is_land flag in decision; define logic in ambiguous cases!
                            // the water test is less severe than the land test
                            is_land_consolidated = !is_water;
                            // the land test is more severe than the water test
                            if (isGlint && !BitSetter.isFlagSet(pixel.l1flags, L1_F_LAND)) {
                                is_land_consolidated = is_land;
                            }
                        }
                        pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_LANDCONS, is_land_consolidated);

                        if (is_land_consolidated) {
                            /* DPM #2.1.6-1 */
                            applySmileCorrection(pixel.rho_ag, auxData.land_smile_params,
                                                 pixel.detector);
                        } else {
                            /* DPM #2.1.6-2 */
                            applySmileCorrection(pixel.rho_ag, auxData.water_smile_params,
                                                 pixel.detector);
                        }
                    } /* if do_corr */
                } /* for ic */
            } /* for il */
        } /* if np */

        return (status);

    }

    /**
     * Converts TOA radiance to reflectance.
     * <p/>
     * <b>Input:</b> {@link DpmPixel#TOAR}, {@link DpmPixel#detector}, {@link DpmPixel#mus} {@link
     * DpmPixel#rho_toa}<br> <b>DPM ref.:</b> section 3.5 step 2.1.4<br> <b>MEGS ref.:</b> <code>pixel_classification.c</code>,
     * function <code>rad2reflect</code><br>
     *
     * @param pixel the pixel structure
     */
    public void rad2reflect(DpmPixel pixel) {
        final double constantTerm = (Math.PI / pixel.mus) * auxData.seasonal_factor;
        for (int bandId = 0; bandId < L1_BAND_NUM; bandId++) {
            // DPM #2.1.4-1
            pixel.rho_toa[bandId] = (pixel.TOAR[bandId] * constantTerm)
                    / auxData.detector_solar_irradiance[bandId][pixel.detector];
        }
    }

    private void applySmileCorrection(double[] rho, L2AuxData.SmileParams params, int detector) {
        for (int bandId = 0; bandId < L1_BAND_NUM; bandId++) {
            if (params.enabled[bandId]) {
                /* DPM #2.1.6-3 */
                int bandMin = params.derivative_band_id[bandId][0];
                int bandMax = params.derivative_band_id[bandId][1];
                double derive = (rho[bandMax] - rho[bandMin]) / (auxData.central_wavelength[bandMax][detector] - auxData.central_wavelength[bandMin][detector]);
                /* DPM #2.1.6-4 */
                lh.smileCorrectedRho[bandId] = rho[bandId] + derive * (auxData.theoretical_wavelength[bandId] - auxData.central_wavelength[bandId][detector]);
            } else {
                /* DPM #2.1.6-5 */
                lh.smileCorrectedRho[bandId] = rho[bandId];
            }
        }
        System.arraycopy(lh.smileCorrectedRho, 0, rho, 0, L1_BAND_NUM);
    }

    /**
     * Detects inland water.
     * Called by  {@link #pixel_classification}.
     * Reference: DPM L2 step 2.6.11.
     * Uses<br>
     * {@link L2AuxData#lap_beta_l}
     *
     * @param r7thresh_val threshold at 665nm
     * @param pixel        the pixel
     * @param b_thresh
     * @param a_thresh
     * @return inland water flag
     */
    private boolean inland_waters(double r7thresh_val, DpmPixel pixel, int b_thresh, double a_thresh) {
        /* DPM #2.6.26-4 */
        boolean status = (pixel.rho_ag[b_thresh] <= a_thresh * r7thresh_val) &&
                (auxData.lap_beta_l * pixel.rho_ag[bb865] < pixel.rho_ag[bb665]);

        pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_LOINLD, status);
        return status;
    }

    private boolean island(double r7thresh_val, DpmPixel pixel, int b_thresh, double a_thresh) {
        boolean status = (pixel.rho_ag[b_thresh] > a_thresh * r7thresh_val) &&
                (auxData.lap_beta_w * pixel.rho_ag[bb865] > pixel.rho_ag[bb665]);

        pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_ISLAND, status);
        return status;
    }

    /*----------------------------------------------------------------*\
    * Function glint_ref: interpolate glint reflectance from look-up table
    * inputs:
    * output:
    * return value:
    *    success code: 0 OK
    * Reference: DPM L2 section 7.3.1 step 2.6.5.1.1
    * called by:
    *    confidence
    * calls:
    *    InterpCoord
    *    GenericInterp
    *
    \*----------------------------------------------------------------*/
    private double glintRef(double thetas, double thetav, double delta, double windm, double chiw) {
        FractIndex[] rogIndex = FractIndex.createArray(5);

        Interp.interpCoord(chiw, auxData.rog.getTab(0), rogIndex[0]);
        Interp.interpCoord(thetav, auxData.rog.getTab(1), rogIndex[1]);
        Interp.interpCoord(delta, auxData.rog.getTab(2), rogIndex[2]);
        Interp.interpCoord(windm, auxData.rog.getTab(3), rogIndex[3]);
        Interp.interpCoord(thetas, auxData.rog.getTab(4), rogIndex[4]);
        return Interp.interpolate(auxData.rog.getJavaArray(), rogIndex);
    }

    /*----------------------------------------------------------------------*\
    * Function azimuth: compute the azimuth (in local topocentric coordinates)
    * of a vector
    * inputs:
    *   x: component of vector along X (Eastward parallel) axis
    *   y: component of vector along Y (Northward meridian) axis
    * return value:
    *   azimuth of vector in degrees
    * references:
    *  mission convention document PO-IS-ESA-GS-0561, para 6.3.4
    *  L2 DPM step 2.6.5.1.1
    \*----------------------------------------------------------------------*/
    private double azimuth(double x, double y) {
        if (y > 0.0) {
            return (MathUtils.RTOD * Math.atan(x / y)); /* DPM #2.6.5.1.1-1 */
        } else if (y < 0.0) {
            return (180.0 + MathUtils.RTOD * Math.atan(x / y)); /* DPM #2.6.5.1.1-5 */
        } else {
            return (x >= 0.0 ? 90.0 : 270.0); /* DPM #2.6.5.1.1-6 */
        }
    }

    /**
     * Computes the ozone transmittance for a given pixel. This routine should be called every 4x4 pixels.
     * <p/>
     * Reference: DPM equation #2.6.12.1-2<br>
     * Uses: <br>
     * {@link L2AuxData#tauO3_norm variables.tauO3_norm} <br>
     *
     * @param airMass air mass
     * @param ozone   total ozone contents
     * @param T_o3    ozone optical thickness in 15 bands
     */
    private void trans_o3(double airMass, double ozone, double[] T_o3) {
        for (int bandId = 0; bandId < L1_BAND_NUM; bandId++) {
            /* DPM #2.6.12.1-2 */
            T_o3[bandId] = Math.exp(-ozone / 1000.0 * airMass * auxData.tauO3_norm[bandId]);
        }
    }

    private static class LocalHelperVariables {
        /**
         * Local helper variable for {@link PixelIdentification#applySmileCorrection}
         */
        double[] smileCorrectedRho = new double[L1_BAND_NUM];
    }
}