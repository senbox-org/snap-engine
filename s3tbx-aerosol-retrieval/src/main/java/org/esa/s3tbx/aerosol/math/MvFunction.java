package org.esa.s3tbx.aerosol.math;

/**
 * @author Andreas Heckel, Olaf Danne
 * @version $Revision: 5452 $ $Date: 2009-06-08 11:25:20 +0200 (Mo, 08 Jun 2009) $
 */
public interface MvFunction {

    /**
     *  multivariate function
     *
     * @param  x - point at which function should be calculated
     * @return     value of the function at x
     * @throws   UnsupportedOperationException -
     */
    double f(double[] x);

}
