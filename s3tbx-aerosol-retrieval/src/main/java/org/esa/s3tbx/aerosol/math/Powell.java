package org.esa.s3tbx.aerosol.math;

/**
 * This class provides a multivariate mimimisation
 * (see Num. Recip., pp. 412)
 *
 * @author Andreas Heckel, Olaf Danne
 */
public class Powell {

    private static final int ITMAX = 1000;

    /**
     * This method provides a minimisation of a function of n variables
     *
     * @param p    array of variables (has length n)
     * @param xi   initial matrix
     * @param ftol fractional tolerance in function value
     * @param func function to be minimised
     * @return the minimum
     *
     * @throws IllegalMonitorStateException Description of Exception
     * @throws IllegalArgumentException     Description of Exception
     */
    public static double fmin(double[] p, double[][] xi, double ftol, MvFunction func)
            throws IllegalMonitorStateException,
            IllegalArgumentException {

        Linmin linmin = new Linmin();

        if (p.length != xi.length || xi.length != xi[0].length) {
            throw new IllegalArgumentException("dimentions must agree");
        }
        final int n = p.length;
        double[] pt = new double[n];
        double[] ptt = new double[n];
        double[] xit = new double[n];

        double fret = func.f(p);

        System.arraycopy(p, 0, pt, 0, n);

        for (int iter = 1; true; ++iter) {
            double fp = fret;
            int ibig = 0;
            double del = 0.0;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    xit[j] = xi[j][i];
                }
                double fptt = fret;
                linmin.linmin(p, xit, func);
                fret = linmin.getFret();
                if (Math.abs(fptt - fret) > del) {
                    del = Math.abs(fptt - fret);
                    ibig = i;
                }
            }
            if (2.0 * Math.abs(fp - fret) <= ftol * (Math.abs(fp) + Math.abs(fret))) {
                return fret;
            }
            if (iter == ITMAX) {
                throw new IllegalMonitorStateException("powell exceeding maximum iterations.");
            }
            for (int j = 0; j < n; j++) {
                ptt[j] = 2.0 * p[j] - pt[j];
                xit[j] = p[j] - pt[j];
                pt[j] = p[j];
            }
            double fptt = func.f(ptt);
            if (fptt < fp) {
                double t = 2.0 * (fp - 2.0 * fret + fptt) * (fp - fret - del) * (fp - fret - del) -
                        del * (fp - fptt) * (fp - fptt);
                if (t < 0.0) {
                    linmin = new Linmin(p, xit, func);
                    fret = linmin.getFret();
                    for (int j = 0; j < n; j++) {
                        xi[j][ibig] = xi[j][n - 1];
                        xi[j][n - 1] = xit[j];
                    }
                }
            }
        }
    }

}
