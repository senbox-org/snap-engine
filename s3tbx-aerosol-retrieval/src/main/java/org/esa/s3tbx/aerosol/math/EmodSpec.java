/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.s3tbx.aerosol.math;

/**
 * This class provides the spectrum model function to be minimised by Powell.
 * (see ATBD (4), (6))
 *
 *
 */
public class EmodSpec implements MvFunction {

    private final double[] specSoil;
    private final double[] specVeg;
    private final double[] surfReflec;
    private final double[] specWeights;
    private final int nSpecChannels;


    public EmodSpec(double[] specSoil, double[] specVeg, double[] surfReflec, double[] specWeights) {
        this.specSoil = specSoil;
        this.specVeg = specVeg;
        this.surfReflec = surfReflec;
        this.specWeights = specWeights;
        this.nSpecChannels = surfReflec.length;
    }

    @Override
    public double f(double[] p) {
        double resid = 0.0;
        for (int iwvl = 0; iwvl < nSpecChannels; iwvl++) {
            // mval: rho_spec_mod in ATBD (p. 22) (model function)
            //mval[iwvl] = p[0] * specVeg[iwvl] + p[1] * specSoil[iwvl] + p[2];

            double mval = p[0] * specVeg[iwvl] + p[1] * specSoil[iwvl];

            // difference to measurement:
            double k = surfReflec[iwvl] - mval;
            // residual:
            resid += specWeights[iwvl] * k * k;
        }

        // constraints for fit parameter p
        // specSoil and specVeg should not be scaled negative
        if (p[0] < 0.0) resid = resid + p[0] * p[0] * 1000;
        if (p[1] < 0.0) resid = resid + p[1] * p[1] * 1000;
        return(resid);
    }

}

