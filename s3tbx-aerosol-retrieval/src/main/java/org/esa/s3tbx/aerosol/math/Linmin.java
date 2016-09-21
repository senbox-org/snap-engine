package org.esa.s3tbx.aerosol.math;

/**
 * This class provides the 'linmin' implementation used within Powell minimisation.
 * (see Num. Recip., pp. 413)
 *
 * @author Andreas Heckel, Olaf Danne
 */
class Linmin implements Function {

    private static final double TOL = 2.0e-4;

    private double fret;
    private double[] pcom;
    private double[] xicom;

    private MvFunction fun;


    /**
     * put your documentation comment here
     */
    Linmin() {
    }

    /**
     * Constructor for the linmin object
     *
     * @param p  Description of Parameter
     * @param xi Description of Parameter
     * @param f  Description of Parameter
     */
    Linmin(double[] p, double xi[], MvFunction f) {
        linmin(p, xi, f);
    }


    /**
     * This method searches for a minimum in one distinct direction
     *
     * @param p  array of variables (has length n)
     * @param xi initial matrix
     * @param f  the function
     * @throws IllegalArgumentException Description of Exception
     */
    void linmin(double[] p, double xi[], MvFunction f) throws IllegalArgumentException {
        int n = 0;
        if (p.length != xi.length) {
            throw new IllegalArgumentException("dimentions must agree");
        }
        if (n != p.length) {
            n = p.length;
            pcom = new double[n];
            xicom = new double[n];
        }
        fun = f;
        for (int j = 0; j < n; j++) {
            pcom[j] = p[j];
            xicom[j] = xi[j];
        }
        double ax = 0.0;
        double xx = 1.0;
        double[] axbxcx = Mnbrak.compute(ax, xx, this);
        ax = axbxcx[0];
        xx = axbxcx[1];
        double bx = axbxcx[2];
        double[] brent = Brent.brent(ax, xx, bx, this, TOL);
        double xmin = brent[0];
        fret = brent[1];
        for (int j = 0; j < n; j++) {
            xi[j] *= xmin;
            p[j] += xi[j];
        }
    }

    public double f(double x) {
        double[] xt = new double[pcom.length];
        for (int j = 0; j < xt.length; j++) {
            xt[j] = pcom[j] + x * xicom[j];
        }
        return fun.f(xt);
    }

    double getFret() {
        return fret;
    }
}
