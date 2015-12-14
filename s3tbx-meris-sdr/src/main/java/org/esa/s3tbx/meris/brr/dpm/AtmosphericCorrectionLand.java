package org.esa.s3tbx.meris.brr.dpm;

import org.esa.s3tbx.meris.brr.operator.CorrectionSurfaceEnum;
import org.esa.s3tbx.meris.l2auxdata.Constants;
import org.esa.snap.core.util.BitSetter;

/**
 * Land atmospheric correction algorithm
 *
 * @author marcoz, olafd
 */
public class AtmosphericCorrectionLand implements Constants {

    private RayleighCorrection rayleighCorrection;

    private LocalHelperVariables lh;

    private CorrectionSurfaceEnum correctionSurface;

    /**
     * Constructs the module
     */
    public AtmosphericCorrectionLand(RayleighCorrection rayCorr) {
        lh = new LocalHelperVariables();
        rayleighCorrection = rayCorr;
    }

    public void setCorrectionSurface(CorrectionSurfaceEnum correctionSurface) {
        this.correctionSurface = correctionSurface;
    }

    public void landAtmCor(DpmPixel[][] pixels, int ic0, int ic1, int il0, int il1) {
        double sun_zenith, view_zenith, delta_azimuth; /* average geometry */
        double mus, muv;        /* cosine of zenith angles */
        double sins, sinv;      /* sine of zenith angles */
        double press;           /* average pressure, P in DPM */

        int il, ic, ib;        /* line, column, band loop indices */
        boolean correctPixel = false;          /* should pixels be corrected */
        DpmPixel pixel;
        long flags;

        for (il = il0; il <= il1; il++) {
            for (ic = ic0; ic <= ic1; ic++) {
                /* for each pixel set flag when we do the atm correction, i.e
                 * not invalid, land consolidated
                 */
                pixel = pixels[il][ic];
                flags = pixel.l2flags;

                final boolean landCorrOk = BitSetter
                        .isFlagSet(flags, F_LANDCONS) ||
                        (BitSetter.isFlagSet(flags, F_LAND) && BitSetter.isFlagSet(flags, F_CLOUD));
                final boolean waterCorrOk = !landCorrOk;

                // new: correct either over land, water, or both (CB/OD, 20140331)
                if ((landCorrOk && correctionSurface != CorrectionSurfaceEnum.WATER) ||
                        (waterCorrOk && correctionSurface != CorrectionSurfaceEnum.LAND) ||
                        correctionSurface == CorrectionSurfaceEnum.ALL_SURFACES) {
                    correctPixel = true;
                    lh.do_corr[il - il0][ic - ic0] = true;
                } else {
                    lh.do_corr[il - il0][ic - ic0] = false;
                }
            }
        }

        if (correctPixel) { /* there exist some pixels to correct */
            final DpmPixel pixel0 = pixels[il0][ic1];

            /* average geometry, ozone for window DPM : just use corner pixel ! */
            sun_zenith = pixel0.sun_zenith;
            view_zenith = pixel0.view_zenith;
            delta_azimuth = pixel0.delta_azimuth; /* v1.3 */

            /* useful geometry quantities */
            mus = pixel0.mus;
            muv = pixel0.muv;
            sins = Math.sin(RAD * sun_zenith);
            sinv = Math.sin(RAD * view_zenith);

            /*
             * 2. Rayleigh corrections (DPM section 7.3.3.3.2, step 2.6.15)
             */

            press = pixel0.press_ecmwf; /* DPM #2.6.15.1-3 */

            /* Rayleigh phase function Fourier decomposition */
            rayleighCorrection.phase_rayleigh(mus, muv, sins, sinv, lh.phaseR);

            /* Rayleigh optical thickness */
            rayleighCorrection.tau_rayleigh(press, lh.tauR);

            /* Rayleigh reflectance*/
            rayleighCorrection.ref_rayleigh(delta_azimuth, sun_zenith, view_zenith, mus, muv,
                                            pixel0.airMass, lh.phaseR, lh.tauR, pixel0.rhoR);

            /* Rayleigh transmittance */
            rayleighCorrection.trans_rayleigh(mus, lh.tauR, pixel0.transRs);
            rayleighCorrection.trans_rayleigh(muv, lh.tauR, pixel0.transRv);

            /* Rayleigh spherical albedo */
            rayleighCorrection.sphalb_rayleigh(lh.tauR, pixel0.sphalbR);

            /* Rayleigh correction for each pixel */
            for (il = il0; il <= il1; il++) {
                for (ic = ic0; ic <= ic1; ic++) {
                    if (lh.do_corr[il - il0][ic - ic0]) {

                        rayleighCorrection.corr_rayleigh(pixel0.rhoR, pixel0.sphalbR, pixel0.transRs, pixel0.transRv,
                                                         pixels[il][ic].rho_ag, pixels[il][ic].rho_top); /*  (2.6.15.4) */
                    }
                }
            }

            /* flag negative Rayleigh-corrected reflectance */
            for (il = il0; il <= il1; il++) {
                for (ic = ic0; ic <= ic1; ic++) {
                    if (lh.do_corr[il - il0][ic - ic0]) {
                        for (ib = 0; ib < L1_BAND_NUM; ib++) {
                            if (pixels[il][ic].rho_top[ib] <= 0.) {
                                switch (ib) {
                                    case bb412:
                                    case bb442:
                                    case bb490:
                                    case bb510:
                                    case bb560:
                                    case bb620:
                                    case bb665:
                                    case bb681:
                                    case bb705:
                                    case bb753:
                                    case bb775:
                                    case bb865:
                                    case bb890:
                                        /* set annotation flag for reflectance product - v4.2 */
                                        pixels[il][ic].ANNOT_F = Set_annot_flag(A_RWNEG + (ib <= bb760 ? ib : ib - 1),
                                                                                pixels[il][ic].ANNOT_F);
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private int Set_annot_flag(int A_FLAG, int annot) {
        annot |= (1 << A_FLAG);
        return annot;
    }

    /**
     * Local helper variables used in {@link AtmosphericCorrectionLand#landAtmCor}.
     */
    private static class LocalHelperVariables {
        /**
         * rayleigh phase function coefficients, PR in DPM
         */
        double[] phaseR = new double[3];
        /**
         * rayleigh optical thickness, tauR0 in DPM
         */
        double[] tauR = new double[L1_BAND_NUM];

        boolean[][] do_corr = new boolean[4][4];
    }
}
