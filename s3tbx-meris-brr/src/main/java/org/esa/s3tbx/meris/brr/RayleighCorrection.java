/*
 * $Id: RayleighCorrection.java,v 1.1 2007/03/27 12:51:41 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.brr;

import org.esa.s3tbx.meris.l2auxdata.Constants;
import org.esa.s3tbx.meris.l2auxdata.L2AuxData;
import org.esa.s3tbx.util.math.FractIndex;
import org.esa.s3tbx.util.math.Interp;
import org.esa.snap.core.gpf.Tile;

public class RayleighCorrection implements Constants {

    static final int[] BANDS_TO_CORRECT = new int[]{bb1, bb2, bb3, bb4, bb5, bb6, bb7, bb8, bb9, bb10, bb12, bb13, bb14};
    static final int[] BANDS_TO_NOT_CORRECT = new int[]{bb11, bb15};

    private L2AuxData auxdata;

    /**
     * Constructs the module
     * @param auxData level 2 auxdata
     */
    public RayleighCorrection(L2AuxData auxData) {
        auxdata = auxData;
    }

    /**
     * Computes Rayleigh reflectance for all bands for a given geometry and pressure.
     * <p/>
     * <b>Input:</b> all parameters without <code>refRayl</code>, {@link L2AuxData#Rayscatt_coeff_s}
     * <br> <b>Output:</b> parameter <code>refRayl</code><br> <b>DPM ref.:</b> DPM L2, section 7.3.3.3.2 <br> <b>MEGS
     * ref.:</b> <code>ray_cor.c</code>, function <code>ref_rayleigh</code><br>
     *
     * @param delta_azimuth Azimuth difference (deltaphi)
     * @param sun_zenith    Sun zenith angle (thetas)
     * @param view_zenith   View zenith angle (thetav)
     * @param mus           Cosine of Sun zenith angle
     * @param muv           Cosine of view zenith angle
     * @param airMass       Air mass (M)
     * @param phaseRayl     Rayleigh phase function Fourier components (PR(s))
     * @param tauRayl       Rayleigh optical thickness (tauR0)
     * @param refRayl       Rayleigh reflectance for all bands (rhoR_4x4)
     */
    public void ref_rayleigh(double delta_azimuth, double sun_zenith, double view_zenith,
                             double mus, double muv, double airMass,
                             double[] phaseRayl, double[] tauRayl, double[] refRayl) {

        /**
         * Rayleigh reflectance Fourier components.
         */
        final double[] rhoRayl = new double[RAYSCATT_NUM_SER];
        /**
         * Polynomial coeff for computation, a(s) in DPM.
         */
        final double[][] abcd = new double[RAYSCATT_NUM_SER][RAYSCATT_NUM_ORD];
        /**
         * Interp. coordinates into table {@link L2AuxData#Rayscatt_coeff_s}.
         */
        final FractIndex[] ref_rayleigh_i = FractIndex.createArray(2);

        FractIndex tsi = ref_rayleigh_i[0];         /* interp coordinates for thetas in LUT scale */
        FractIndex tvi = ref_rayleigh_i[1];          /* interp coordinates for thetav in LUT scale */

        double mud = Math.cos(RAD * delta_azimuth); /* used for all bands, compute once */
        double mu2d = 2.0 * mud * mud - 1.0;

        /* angle interpolation coordinates */
        Interp.interpCoord(sun_zenith, auxdata.Rayscatt_coeff_s.getTab(2), tsi); /* fm 15/5/97 */
        Interp.interpCoord(view_zenith, auxdata.Rayscatt_coeff_s.getTab(3), tvi);

        float[][][][] Rayscatt_coeff_s = (float[][][][]) auxdata.Rayscatt_coeff_s.getJavaArray();
        /* pre-computation of multiple scatt coefficients, wavelength independent */
        for (int is = 0; is < RAYSCATT_NUM_SER; is++) {
            /* DPM #2.1.17-4 to 2.1.17-7 */
            final double[] lhLocal_abcd_is = abcd[is];
            for (int ik = 0; ik < RAYSCATT_NUM_ORD; ik++) {
                lhLocal_abcd_is[ik] = Interp.interpolate(Rayscatt_coeff_s[ik][is], ref_rayleigh_i);
            }
        }

        for(int bandId : BANDS_TO_CORRECT) {
            final double tauRayl_bandId = tauRayl[bandId];
            double constTerm = (1.0 - Math.exp(-tauRayl_bandId * airMass)) / (4.0 * (mus + muv));
            for (int is = 0; is < RAYSCATT_NUM_SER; is++) {
                /* primary scattering reflectance */
                rhoRayl[is] = phaseRayl[is] * constTerm; /* DPM #2.1.17-8 CORRECTED */

                final double[] abcd_is = abcd[is];
                /* coefficient for multiple scattering correction */
                double multiScatteringCoeff = 0.0;
                for (int ik = RAYSCATT_NUM_ORD - 1; ik >= 0; ik--) {
                    multiScatteringCoeff = tauRayl_bandId * multiScatteringCoeff + abcd_is[ik]; /* DPM #2.1.17.9 */
                }

                /* Fourier component of Rayleigh reflectance */
                rhoRayl[is] *= multiScatteringCoeff; /* DPM #2.1.17-10 */
            }

            /* Rayleigh reflectance */
            refRayl[bandId] = rhoRayl[0] +
                    2.0 * mud * rhoRayl[1] +
                    2.0 * mu2d * rhoRayl[2]; /* DPM #2.1.17-11 */
        }
        for(int bandId : BANDS_TO_NOT_CORRECT) {
            refRayl[bandId] = 0.0;
        }
    }

    /**
     * Computes three Fourier components of Rayleigh function.
     * <p/>
     * <p/>
     * <b>Input:</b> all arguments and {@link L2AuxData#AB}<br> <b>Output:</b>
     * <code>phaseRayl</code><br> <b>DPM ref.:</b> DPM L2, section 7.3.3.3.2 <br> <b>MEGS ref.:</b>
     * <code>ray_cor.c</code>, function <code>phase_rayleigh</code><br>
     *
     * @param mus       cosine of sun zenith angle
     * @param muv       cosine of view zenith angle
     * @param sins      sine of sun zenith angle
     * @param sinv      sine of view zenith angle
     * @param phaseRayl Fourier components of Rayleigh phase function
     */
    public void phase_rayleigh(double mus, double muv,
                               double sins, double sinv,
                               double[] phaseRayl) {
        final double sinsSquared = sins * sins;
        final double sinvSquared = sinv * sinv;
        phaseRayl[0] = 0.75 * auxdata.AB[0] * (1.0 + mus * mus * muv * muv +
                                               0.5 * sinsSquared * sinvSquared) + auxdata.AB[1]; /* DPM #2.1.17-1 corrected */
        phaseRayl[1] = -0.75 * auxdata.AB[0] * mus * muv * sins * sinv; /* DPM #2.1.17-2 corrected */
        phaseRayl[2] = 0.1875 * auxdata.AB[0] * sinsSquared * sinvSquared; /* DPM #2.1.17.3 corrected */
    }

    /**
     * Computes Rayleigh optical thickness.
     * <p/>
     * <b>Input:</b> variable <code>press</code>, {@link L2AuxData#Pstd},{@link
     * L2AuxData#tau_R} <br> <b>Output:</b> <code>tauRayl</code> <br> <b>DPM
     * ref.:</b> L2 DPM section 7.3.3.3.3.2 <br> <b>MEGS ref.:</b> <code>ray_cor.c</code>, function
     * <code>tau_rayleigh</code><br>
     *
     * @param press   average pressure in 4x4 window (P_4x4)
     * @param tauRayl rayleigh opt. thick (tauR0)
     */
    public void tau_rayleigh(double press, double[] tauRayl) {
        double ratio = press / auxdata.Pstd;

        for(int bandId : BANDS_TO_CORRECT) {
            tauRayl[bandId] = auxdata.tau_R[bandId] * ratio; /* DPM #2.6.15.2-5 */
        }
        for(int bandId : BANDS_TO_NOT_CORRECT) {
            tauRayl[bandId] = 0.0;
        }
    }

    /*-----------------------------------------------------------------------------*\
 * Function trans_rayleigh: compute Rayleigh transmittance
 * for all bands for a given zenith angle
 * inputs:
 *   mu            cosine of zenith angle
 *   tauRayl       Rayleigh optical thickness
 *   Raytrans      transmittance correction coeffs
 * outputs:
 *   transRayl     Rayleigh transmittance for all bands
 * Reference: DPM L2, section 7.3.3.3.2
 * called by: landAtmCor
\*-----------------------------------------------------------------------------*/

    public void trans_rayleigh(double mu, double[] tauRayl, double[] transRayl) {
        final double twoThird = 2.0 / 3.0;
        final double fourThird = 4.0 / 3.0;
        for(int bandId : BANDS_TO_CORRECT) {
            double tr = (twoThird + mu + (twoThird - mu) *
                                         Math.exp(-tauRayl[bandId] / mu)) / (fourThird + tauRayl[bandId]); /* DPM #2.6.15.2-1, -3 */
            transRayl[bandId] = auxdata.Raytrans[0] + auxdata.Raytrans[1] * tr + auxdata.Raytrans[2] * tr * tr; /* DPM #2.6.15.2-2, -4 */
        }
        for(int bandId : BANDS_TO_NOT_CORRECT) {
            transRayl[bandId] = 1.0;
        }
    }
/*-----------------------------------------------------------------------------*\
 * Function sphalb_rayleigh: compute Rayleigh spherical albedo
 * for all bands
 * inputs:
 *   tauRayl       Rayleigh optical thickness
 *   Rayalb        Rayleigh spherical albedo LUT (global)
 * outputs:
 *   sphalbRayl     Rayleigh spherical albedo for all bands
 * Reference: DPM L2, section 7.3.3.3.2
 * called by: landAtmCor
 * calls:
 *    InterpCoord
 *    GenericInterp
\*-----------------------------------------------------------------------------*/

    public void sphAlb_rayleigh(double[] tauRayl, double[] sphalbRayl) {
        FractIndex[] indexes = FractIndex.createArray(1);
        for (int bandId : BANDS_TO_CORRECT) {
            Interp.interpCoord(tauRayl[bandId], auxdata.Rayalb.getTab(0), indexes[0]);
            sphalbRayl[bandId] = Interp.interpolate(auxdata.Rayalb.getJavaArray(), indexes); /* DPM #2.6.15.3-1 */
        }
        for(int bandId : BANDS_TO_NOT_CORRECT) {
            sphalbRayl[bandId] = 0.0;
        }
    }

/*-----------------------------------------------------------------------------*\
 * Function corr_rayleigh: compute Rayleigh correction for a pixel
 * for all bands
 * inputs:
 *   refRayl       Rayleigh reflectance
 *   sphalbRayl    Rayleigh spherical albedo
 *   transRs       Rayleigh transmittance (down)
 *   transRv       Rayleigh transmittance (up)
 *   rho           reflectance (uncorrected)
 * outputs:
 *   rho_ag        reflectance (corrected)
 * Reference: DPM L2, section 7.3.3.3.2
 * called by: landAtmCor
\*-----------------------------------------------------------------------------*/

    public void corr_rayleigh(double[] refRayl, double[] sphalbRayl, double[] transRs, double[] transRv,
                              Tile[] rhoNg, Tile[] brr, int x, int y) {
        for (int bandId : BANDS_TO_CORRECT) {
            double dum = (rhoNg[bandId].getSampleFloat(x, y) - refRayl[bandId]) /
                    (transRs[bandId] * transRv[bandId]);      /* DPM 2.6.15.4-5 */
            brr[bandId].setSample(x, y, dum / (1.0 + sphalbRayl[bandId] * dum)); /* DPM 2.6.15.4-6 */
        }
    }

}
