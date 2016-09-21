/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.s3tbx.aerosol.math;

import org.esa.s3tbx.aerosol.InputPixelData;
import org.esa.s3tbx.aerosol.lut.MomoLut;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.Guardian;

/**
 *
 * @author akheckel
 */
public class BrentFitFunction implements Function {

    public static final int ANGULAR_MODEL = 1;
    public static final int SPECTRAL_MODEL = 2;
    public static final int SYNERGY_MODEL = 3;

    private static final float PENALTY = 1000f;
    private static final float LLIMIT = 5e-6f;

    private final int model;
    private final InputPixelData[] inPixField;
    private final MomoLut lut;
    private final double[] specWeights;
    private final double[] specSoil;
    private final double[] specVeg;

    public BrentFitFunction(int modelType, InputPixelData[] inPixField, MomoLut lut, double[] specWeights) {
        Guardian.assertEquals("modelType", modelType, ANGULAR_MODEL);
        this.model = modelType;
        this.inPixField = inPixField;
        this.lut = lut;
        this.specWeights = specWeights;
        this.specSoil = null;
        this.specVeg = null;
    }

    public BrentFitFunction(int modelType, InputPixelData[] inPixField, MomoLut lut, double[] specWeights, double[] specSoil, double[] specVeg) {
        this.model = modelType;
        this.inPixField = inPixField;
        this.lut = lut;
        this.specWeights = specWeights;
        this.specSoil = specSoil;
        this.specVeg = specVeg;
    }

    @Override
    public synchronized double f(double tau) {
        double fmin = 0;
        for (InputPixelData anInPixField : inPixField) {
            fmin += fPix(tau, anInPixField);
        }
        return fmin;
    }

    public synchronized double getMaxAOT() {
        int min = 0;
        for (int i = 0; i < inPixField.length; i++) {
            if (inPixField[i].getToaReflec()[0] < inPixField[min].getToaReflec()[0]) {
                min = i;
            }
        }
        return lut.getMaxAOT(inPixField[min]);
    }

    //private methods

    private double fPix(double tau, InputPixelData inPixData){
        lut.getSdrAndDiffuseFrac(inPixData, tau);
        double fmin = isSdrNegativ(inPixData.getSurfReflec());

        if ( !(fmin > 0) ) {
            double[] p = initStartVector(model);
            double xi[][] = initParameterBasis(p.length);
            double ftol = 2e-3;   // limit for optimization
            MvFunction surfModel;
            switch (model){
                case 1:
                    surfModel = new EmodAng(inPixData.getDiffuseFrac(), inPixData.getSurfReflec(), specWeights);
                    break;
                case 2:
                    surfModel = new EmodSpec(specSoil, specVeg, inPixData.getSurfReflec()[0], specWeights);
                    break;
                case 3:
                default: throw new OperatorException("invalid surface reflectance model");
            }

            fmin = Powell.fmin(p, xi, ftol, surfModel);
        }
        else {
            fmin += 1e-8;
        }
        return fmin;
    }

//     inversion can lead to overcorrection of atmosphere
//     and thus to too small surface reflectances
//     this function defines a steep but smooth function
//     to guide the optimization
    private static double isSdrNegativ(double[][] sdr) {
        double fmin = 0;
        for (double[] aSdr : sdr) {
            for (int iWvl = 0; iWvl < sdr[0].length; iWvl++) {
                if (sdr[0][iWvl] < LLIMIT) {
                    fmin += (aSdr[iWvl] - LLIMIT) * (aSdr[iWvl] - LLIMIT) * PENALTY;
                }
            }
        }
        return fmin;
    }

//     define initial vector p to start Powell optimization
//     according to the selected surface spectral model
    private static double[] initStartVector(int model) {
        switch (model) {
            case ANGULAR_MODEL:
                return new double[]{0.1f, 0.1f, 0.1f, 0.1f, 0.5f, 0.3f};
            case SPECTRAL_MODEL:
                return new double[]{0.9, 0.1};
            case SYNERGY_MODEL:
            default:
                throw new OperatorException("Surface Model not implemented");
        }
    }

//     defining unit matrix as base of the parameter space
//     needed for Powell
    private static double[][] initParameterBasis(int length) {
        double xi[][] = new double[length][length];
        for (int i = 0; i < length; i++) xi[i][i] = 1.0;
        return xi;
    }

}
