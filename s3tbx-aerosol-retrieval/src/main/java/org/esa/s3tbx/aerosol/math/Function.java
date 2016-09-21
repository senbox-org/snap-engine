package org.esa.s3tbx.aerosol.math;

/**
 * Interface providing a function.
 *
 * @author Andreas Heckel, Olaf Danne
 */
public interface Function {

    /**
     *  Univariate function definition
     *
     *@param  x  - input value
     *@return      return value
     */
    double f(double x);
}
