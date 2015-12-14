package org.esa.s3tbx.meris.dpm;

import org.esa.s3tbx.meris.l2auxdata.L2AuxData;
import org.esa.s3tbx.util.math.FractIndex;
import org.esa.s3tbx.util.math.Interp;

import static org.esa.s3tbx.meris.l2auxdata.Constants.*;

@SuppressWarnings("JavaDoc")
public class PixelId {

    public static class Pressure {
        public double value;
        public boolean error;
    }

    private final L2AuxData auxData;

    public PixelId(L2AuxData auxData) {
        this.auxData = auxData;
    }

    /**
     * Computes the pressure.
     * <b>DPM ref.:</b> section 3.5 step 2.1.4<br>
     * <b>MEGS ref.:</b> <code>pixelid.c</code>, function <code>Comp_Pressure</code><br>
     *
     * @param rhoToa753
     * @param rhoToa760
     * @param airMass
     * @param detectorIndex
     * @return pressure
     */
    public Pressure computePressure(double rhoToa753, double rhoToa760, double airMass, int detectorIndex) {
        double eta; // Ratio TOAR(11)/TOAR(10)
        Pressure press = new Pressure();
        final FractIndex spectralShiftIndex = new FractIndex();
        final FractIndex[] cIndex = FractIndex.createArray(2);

        /* DPM #2.1.3-1 */
        /* get spectral_shift from detector id in order to use pressure polynomials */
        Interp.interpCoord(auxData.central_wavelength[bb760][detectorIndex],
                           auxData.spectral_shift_wavelength,
                           spectralShiftIndex);

        // DPM #2.1.3-2, DPM #2.1.3-3, DPM #2.1.3-4
        // when out of bands, spectral_shift is set to 0 or PPOL_NUM_SHIFT with a null weight,
        // so it works fine with the following.

        /* reflectance ratio computation, DPM #2.1.12-2 */
        if (rhoToa753 > 0.0) {
            eta = rhoToa760 / rhoToa753;
        } else {
            // DPM section 3.5.3
            eta = 0.0;        /* DPM #2.1.12-3 */
            press.error = true;                /* raise PCD */
        }
        // DPM #2.1.12-4
        Interp.interpCoord(airMass, auxData.C.getTab(1), cIndex[0]);
        Interp.interpCoord(rhoToa753, auxData.C.getTab(2), cIndex[1]);

        float[][][] c_lut = (float[][][]) auxData.C.getJavaArray();
        // coefficient used in the pressure estimation
        double C_res = Interp.interpolate(c_lut[VOLC_NONE], cIndex);

        // DPM #2.1.12-5, etha * C
        double ethaC = eta * C_res;
        // DPM #2.1.12-5a
        Pressure press_1 = computeSurfacePressure(ethaC, airMass, spectralShiftIndex.index);
        // DPM #2.1.12-5b
        Pressure press_2 = computeSurfacePressure(ethaC, airMass, spectralShiftIndex.index + 1);
        if (press_1.error) {
            press.value = press_2.value; /* corrected by LB as DPM is flawed: press_1 --> press_2 */
        } else if (press_2.error) {
            press.value = press_1.value; /* corrected by LB as DPM is flawed: press_2 --> press_1 */
        } else {
            /* DPM #2.1.12-5c */
            press.value = (1 - spectralShiftIndex.fraction) * press_1.value + spectralShiftIndex.fraction * press_2.value;
        }

        /* DPM #2.1.12-12 */
        press.error = press.error || press_1.error || press_2.error;
        return press;
    }

    /**
     * Computes surface pressure from corrected ratio b11/b10.
     * <p/>
     * <b>DPM ref.:</b> section 3.5 (step 2.1.12)<br> <b>MEGS ref.:</b> <code>pixelid.c</code>, function
     * <code>pressure_func</code><br>
     *
     * @param eta_C         ratio TOAR(B11)/TOAR(B10) corrected
     * @param airMass       air mass
     * @param spectralShift
     * @return press         the resulting pressure
     */
    public Pressure computeSurfacePressure(double eta_C, double airMass, int spectralShift) {
        double P; /* polynomial accumulator */
        double koeff; /* powers of eta_c */
        Pressure press = new Pressure();
        press.error = false;
        final FractIndex polcoeffShiftIndex = new FractIndex();

        /* Interpoate polcoeff from spectral shift dependent table - DPM #2.1.16-1 */
        Interp.interpCoord(spectralShift, auxData.polcoeff.getTab(0), polcoeffShiftIndex);
        /* nearest neighbour interpolation */
        if (polcoeffShiftIndex.fraction > 0.5) {
            polcoeffShiftIndex.index++;
        }
        float[][] polcoeff = (float[][]) auxData.polcoeff.getArray().getJavaArray();

        /* DPM #2.1.16-2 */
        P = polcoeff[polcoeffShiftIndex.index][0];
        koeff = 1.0;
        for (int i = 1; i < PPOL_NUM_ORDER; i++) {
            koeff *= eta_C;
            P += polcoeff[polcoeffShiftIndex.index][i] * koeff;
        }
        /* CHANGED v7.0: polynomial now gives log10(m*P^2) (LB 15/12/2003) */
        if ((P <= 308.0) && (P >= -308.0)) {  /* MP2 would be out of double precision range  */
            double MP2 = Math.pow(10.0, P); /* retrieved product of air mass times square of pressure */
            press.value = Math.sqrt(MP2 / airMass); /* DPM 2.1.16-3 */
            if (press.value > auxData.maxPress) {
                press.value = auxData.maxPress; /* DPM #2.1.16-5 */
                press.error = true;     /* DPM #2.1.16-4  */
            }
        } else {
            press.value = 0.0;    /* DPM #2.1.16-6 */
            press.error = true;  /* DPM #2.1.16-7 */
        }
        return press;
    }

    /**
     * get proper threshold
     * DPM #2.1.2-2
     */
    public double getPressureThreshold(double sza, double vza, boolean isLand) {
        FractIndex[] DP_Index = FractIndex.createArray(2);
        double delta_press_thresh;
        if (isLand) {
            Interp.interpCoord(sza, auxData.DPthresh_land.getTab(0), DP_Index[0]);
            Interp.interpCoord(vza, auxData.DPthresh_land.getTab(1), DP_Index[1]);
            delta_press_thresh = Interp.interpolate(auxData.DPthresh_land.getJavaArray(), DP_Index);
        } else {
            Interp.interpCoord(sza, auxData.DPthresh_ocean.getTab(0), DP_Index[0]);
            Interp.interpCoord(vza, auxData.DPthresh_ocean.getTab(1), DP_Index[1]);
            delta_press_thresh = Interp.interpolate(auxData.DPthresh_ocean.getJavaArray(), DP_Index);
        }
        return  delta_press_thresh;
    }

    /**
     * Compares pressure estimates with ECMWF data.
     *
     * @param ecmwfPressure      the pixel ECMWF pressure
     * @param pressure           the pressure of the pixel
     * @param inputPressure      can be either cloud top pressure from CloudTopPressureOp,
     *                           or PScatt from {@see LisePressureOp} (new!), or -1 if not given
     * @param delta_press_thresh absolute threshold on pressure difference
     * @return result_flags  the return values, <code>resultFlags[0]</code> contains low NN pressure flag (low_P_nn),
     *         <code>resultFlags[1]</code> contains low polynomial pressure flag (low_P_poly),
     *         <code>resultFlags[2]</code> contains pressure range flag (delta_p).
     */
    public boolean[] getPressureThreshFlags(double ecmwfPressure, double pressure, double inputPressure, double delta_press_thresh) {
        boolean[] result_flags = new boolean[3];
        /* test NN pressure- DPM #2.1.2-4 */ // low_P_nn
        if (inputPressure != -1) {
            result_flags[0] = (inputPressure < ecmwfPressure - delta_press_thresh); //changed in V7
        } else {
            result_flags[0] = (ecmwfPressure < ecmwfPressure - delta_press_thresh); //changed in V7
        }
        /* test polynomial pressure- DPM #2.1.2-3 */ // low_P_poly
        result_flags[1] = (pressure < ecmwfPressure - delta_press_thresh);  //changed in V7
        /* test pressure range - DPM #2.1.2-5 */   // delta_p
        result_flags[2] = (Math.abs(ecmwfPressure - pressure) > auxData.press_confidence); //changed in V7
        return result_flags;
    }

    /**
     * Computes the threshold on rayleigh corrected reflectance
     * DPM #2.1.7-9
     */
    public double getRhoRC442thr(double sza, double vza, double delta_azimuth, boolean isLand) {
        double rhorc_442_thr;
        final FractIndex[] rhoRC442index = FractIndex.createArray(3);
        if (isLand) {   /* land pixel */
            Interp.interpCoord(sza, auxData.Rhorc_442_land_LUT.getTab(0), rhoRC442index[0]);
            Interp.interpCoord(vza, auxData.Rhorc_442_land_LUT.getTab(1), rhoRC442index[1]);
            Interp.interpCoord(delta_azimuth, auxData.Rhorc_442_land_LUT.getTab(2), rhoRC442index[2]);
            rhorc_442_thr = Interp.interpolate(auxData.Rhorc_442_land_LUT.getJavaArray(), rhoRC442index);
        } else {    /* water  pixel */
            Interp.interpCoord(sza, auxData.Rhorc_442_ocean_LUT.getTab(0), rhoRC442index[0]);
            Interp.interpCoord(vza, auxData.Rhorc_442_ocean_LUT.getTab(1), rhoRC442index[1]);
            Interp.interpCoord(delta_azimuth, auxData.Rhorc_442_ocean_LUT.getTab(2), rhoRC442index[2]);
            rhorc_442_thr = Interp.interpolate(auxData.Rhorc_442_ocean_LUT.getJavaArray(), rhoRC442index);
        }
        return rhorc_442_thr;
    }

    /**
     * Derive bright flag by reflectance comparison to threshold
     * DPM #2.1.7-10
     */
    public boolean isBrightFlag(double[] rhoAg, double rhorc_442_thr, double radianceBrightN) {
        boolean bright_f = (rhoAg[auxData.band_bright_n] >= rhorc_442_thr)
                || isSaturated(radianceBrightN, auxData.band_bright_n);
        return bright_f;
    }

    /**
     * Spectral slope processor.brr 1
     */
    public boolean isSpectraSlope1Flag(double[] rhoAg, double radianceSlopeN1) {
        boolean slope1_f;
        if (rhoAg[auxData.band_slope_d_1] <= 0.0) {
            /* negative reflectance exception */
            slope1_f = false; /* DPM #2.1.7-6 */
        } else {
            /* DPM #2.1.7-5 */
            double slope1 = rhoAg[auxData.band_slope_n_1] / rhoAg[auxData.band_slope_d_1];
            slope1_f = ((slope1 >= auxData.slope_1_low_thr) && (slope1 <= auxData.slope_1_high_thr))
                    || isSaturated(radianceSlopeN1, auxData.band_slope_n_1);
        }
        return slope1_f;
    }

    /**
     * Spectral slope processor.brr 2
     */
    public boolean isSpectraSlope2Flag(double[] rhoAg, double radianceSlopeN2) {
        boolean slope2_f;
        if (rhoAg[auxData.band_slope_d_2] <= 0.0) {
            /* negative reflectance exception */
            slope2_f = false; /* DPM #2.1.7-8 */
        } else {
            /* DPM #2.1.7-7 */
            double slope2 = rhoAg[auxData.band_slope_n_2] / rhoAg[auxData.band_slope_d_2];
            slope2_f = ((slope2 >= auxData.slope_2_low_thr) && (slope2 <= auxData.slope_2_high_thr))
                    || isSaturated(radianceSlopeN2, auxData.band_slope_n_2);
        }
        return slope2_f;
    }

    public boolean isSaturated(double radiance, int bandId) {
        return radiance > auxData.Saturation_L[bandId];
    }
}
