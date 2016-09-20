package org.esa.s3tbx.aerosol.math;

/**
 * Interface providing a function.
 *
 * @author Andreas Heckel, Olaf Danne
 * @version $Revision: 6368 $ $Date: 2009-10-02 15:51:14 +0200 (Fr, 02 Okt 2009) $
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
