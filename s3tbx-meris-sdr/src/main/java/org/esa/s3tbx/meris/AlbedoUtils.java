/*
 * $Id: AlbedoUtils.java,v 1.1 2007/03/27 12:52:22 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris;


public class AlbedoUtils {

    private AlbedoUtils() {
    }

    /**
     * Computes the azimuth difference of the inversion model from the given
     *
     * @param vaa viewing azimuth angle [degree]
     * @param saa sun azimuth angle [degree]
     * @return the azimuth difference [degree]
     */
    public static double computeAzimuthDifference(final double vaa, final double saa) {
        double ada = vaa - saa;
        if (ada <= -180.0) {
            ada = +360.0 + ada;
        } else if (ada > +180.0) {
            ada = -360.0 + ada;
        }
        if (ada >= 0.0) {
            ada = +180.0 - ada;
        } else {
            ada = -180.0 - ada;
        }
        return ada;
    }
}
