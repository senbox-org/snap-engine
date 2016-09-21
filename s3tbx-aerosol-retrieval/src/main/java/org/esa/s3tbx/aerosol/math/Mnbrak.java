package org.esa.s3tbx.aerosol.math;

/**
 * This class provides a method for initially bracketing a minimum.
 *
 * @author Andreas Heckel, Olaf Danne
 */
public class Mnbrak {

    private static final double GOLD = 1.618034;
    private static final int GLIMIT = 100;
    private static final double TINY = 1.0e-20;

    /**
     * Given a function fun and distinct initial points Ax, Bx, this method searches in the downhill direction
     * and computes new points ax, bx, cx that bracket a minimum of the function.
     *
     * @param Ax  left initial point
     * @param Bx  right initial point
     * @param fun the function
     * @return An array containing ax, bx, cx
     */
    public static double[] compute(double Ax, double Bx, Function fun) {
        double ax = Ax;
        double bx = Bx;
        double fa = fun.f(ax);
        double fb = fun.f(bx);
        if (fb > fa) {
            double dum = ax;
            ax = bx;
            bx = dum;
            dum = fb;
            fb = fa;
            fa = dum;
        }
        double cx = bx + GOLD * (bx - ax);
        double fc = fun.f(cx);
        while (fb > fc) {
            double r = (bx - ax) * (fb - fc);
            double q = (bx - cx) * (fb - fa);
            double u = bx - ((bx - cx) * q - (bx - ax) * r) / (2.0 * (q - r < 0 ? -1 : 1) * Math.max(Math.abs(
                    q - r), TINY));
            double ulim = bx + GLIMIT * (cx - bx);
            double fu;
            if ((bx - u) * (u - cx) > 0.0) {
                fu = fun.f(u);
                if (fu < fc) {
                    ax = bx;
                    bx = u;
                    return new double[]{ax, bx, cx};
                } else if (fu > fb) {
                    cx = u;
                    return new double[]{ax, bx, cx};
                }
                u = cx + GOLD * (cx - bx);
                fu = fun.f(u);
            } else if ((cx - u) * (u - ulim) > 0.0) {
                fu = fun.f(u);
                if (fu < fc) {
                    bx = cx;
                    cx = u;
                    u = cx + GOLD * (cx - bx);
                    fb = fc;
                    fc = fu;
                    fu = fun.f(u);
                }
            } else if ((u - ulim) * (ulim - cx) >= 0.0) {
                u = ulim;
                fu = fun.f(u);
            } else {
                u = (cx) + GOLD * (cx - bx);
                fu = fun.f(u);
            }
            ax = bx;
            bx = cx;
            cx = u;
            fa = fb;
            fb = fc;
            fc = fu;
        }
        return new double[]{ax, bx, cx};
    }
}
