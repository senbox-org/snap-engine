/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.s3tbx.aerosol;

import Jama.Matrix;
import org.esa.s3tbx.aerosol.math.Brent;
import org.esa.s3tbx.aerosol.math.Function;

/**
 * Provides aerosol retrieval class
 * currently not thread safe !!!
 *     subsequent calls to Brent and Powell.
 *     Somewhere along that line thread safety is broken
 * --> instantiate locally in computeTileStack()
 * 
 * @author akheckel
 */
class PointRetrieval {

    private final Function brentFitFct;

    public PointRetrieval(Function brentFitFct) {
        this.brentFitFct = brentFitFct;
    }

// public methods

    public synchronized RetrievalResults runRetrieval(double maxAOT) {
        double[] brent = Brent.brent(0.001, 0.5 * maxAOT, maxAOT, brentFitFct, 5e-6);
        float optAOT = (float) brent[0];
        float optErr = (float) brent[1];
        boolean failed = (optAOT <= 0.003);
        double curv = calcCurvature(optAOT, optErr, maxAOT);
        failed = failed || (curv < 0);// || (optErr > 0.001);
        float retrievalErr = (float) calcErrFromCurv(optErr, curv);

        return new RetrievalResults(failed, optAOT, optErr, retrievalErr, (float) curv);
    }

// private methods

    private double calcErrFromCurv(double optErr, double a) {
        if (a < 0) {
            return Math.sqrt(optErr / 0.8 * 2 / 1e-4) + 0.03;
        }
        else {
            return Math.sqrt(optErr / 0.8 * 2 / a) + 0.03;
        }
    }

    private double calcCurvature(double optAOT, double optErr, double maxAOT) {
        double p1 = 0.33*maxAOT;
        double p2 = 0.66*maxAOT;
        final double[] x0 = {1, 1, 1};
        final double[] x1 = {p1, optAOT, p2};
        final double[] x2 = {x1[0] * x1[0], x1[1] * x1[1], x1[2] * x1[2]};
        double optErrLow = brentFitFct.f(x1[0]);
        double optErrHigh = brentFitFct.f(x1[2]);
        final double[][] y = {{optErrLow}, {optErr}, {optErrHigh}};
        final double[][] xArr = {{x2[0], x1[0], x0[0]}, {x2[1], x1[1], x0[1]}, {x2[2], x1[2], x0[2]}};
        Matrix A = new Matrix(xArr);
        Matrix c = new Matrix(y);
        Matrix result = A.solve(c);
        final double[][] resultArr = result.getArray();
        return resultArr[0][0];
    }

}
