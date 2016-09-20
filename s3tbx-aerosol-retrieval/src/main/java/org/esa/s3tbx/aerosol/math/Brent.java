package org.esa.s3tbx.aerosol.math;

/**
 * This class provides a 1D minimisation of a function in a given interval
 * (see Num. Recip., pp. 402)
 *
 * @author Andreas Heckel, Olaf Danne
 */
public class Brent {

    private static final int ITMAX = 100;
    private static final double CGOLD = 0.3819660;
    private static final double ZEPS = 1.0e-10;

    /**
     * Given a function f and a bracket triplet of abscissas, ax, bx, cx, this method
     * isolates the minimum (f(bx) < f(ax) and f(bx) < f(cx)) to a fractional precision of about tol.
     *
     * @param ax  left bracket
     * @param bx  inner value
     * @param cx  right bracket
     * @param fun the function
     * @param tol tolerance
     * @return An array containing xmin and fx
     *
     * @throws IllegalStateException Description of Exception
     */
    public static double[] brent(double ax, double bx, double cx, Function fun, double tol) throws IllegalStateException {
        double e = 0.0;
        double d = 0.0;
        double a = (ax < cx ? ax : cx);
        double b = (ax > cx ? ax : cx);
        double x = bx;
        double w = bx;
        double v = bx;
        double fw = fun.f(x);
        double fv = fun.f(x);
        double fx = fun.f(x);
        for (int iter = 0; iter < ITMAX; iter++) {
            double xm = 0.5 * (a + b);
            double tol1 = tol * Math.abs(x) + ZEPS;
            double tol2 = 2.0 * tol1;
            if (Math.abs(x - xm) <= (tol2 - 0.5 * (b - a))) {
                return new double[]{x, fx};
            }
            if (Math.abs(e) > tol1) {
                double r = (x - w) * (fx - fv);
                double q = (x - v) * (fx - fw);
                double p = (x - v) * q - (x - w) * r;
                q = 2.0 * (q - r);
                if (q > 0.0) {
                    p = -p;
                }
                q = Math.abs(q);
                double etemp = e;
                e = d;
                if (Math.abs(p) >= Math.abs(0.5 * q * etemp) || p <= q * (a - x)
                        || p >= q * (b - x)) {
                    e = (x >= xm ? a - x : b - x);
                    d = CGOLD * e;
                } else {
                    d = p / q;
                    double u = x + d;
                    if (u - a < tol2 || b - u < tol2) {
                        d = (xm - x >= 0 ? Math.abs(tol1) : -Math.abs(tol1));
                    }
                }
            } else {
                d = CGOLD * (e = (x >= xm ? a - x : b - x));
            }
            double u = (Math.abs(d) >= tol1 ? x + d : x + (d < 0 ? -Math.abs(tol1) : Math.abs(tol1)));
            double fu = fun.f(u);
            if (fu <= fx) {
                if (u >= x) {
                    a = x;
                } else {
                    b = x;
                }
                v = w;
                w = x;
                x = u;
                fv = fw;
                fw = fx;
                fx = fu;
            } else {
                if (u < x) {
                    a = u;
                } else {
                    b = u;
                }
                if (fu <= fw || w == x) {
                    v = w;
                    w = u;
                    fv = fw;
                    fw = fu;
                } else if (fu <= fv || v == x || v == w) {
                    v = u;
                    fv = fu;
                }
            }
        }
        throw new IllegalStateException("Too many iterations in brent");
    }
}
