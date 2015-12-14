/*
 * $Id: Utils.java,v 1.1 2007/03/27 12:51:41 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.l2auxdata;

import org.esa.snap.core.datamodel.Product;

public class Utils {

    private Utils() {
    }

    public static double computeSeasonalFactor(double daysSince2000, double sunEarthDistanceSquare) {
        // Semi-major axis of ellipse Earth orbit around Sun in meters
        final double a = 149597870.0 * 1000.0;
        // Eccentricity of ellipse Earth orbit around Sun
        final double e = 0.017;
        // Perihelion in 2000 was the 03.01.2000 05:00
        final double daysToPerihelionIn2000 = 3.0 + 5.0 / 24.0;
        final double daysOfYear = 365.25;
        final double theta = 2 * Math.PI * ((daysSince2000 - daysToPerihelionIn2000) / daysOfYear);
        final double r = a * (1.0 - e * e) / (1.0 + e * Math.cos(theta));
        return r * r / sunEarthDistanceSquare;
    }

    public static boolean isProductRR(Product product) {
        return product.getProductType().indexOf("_RR") > 0;
    }

    public static boolean isProductFR(Product product) {
        return (product.getProductType().indexOf("_FR") > 0) ||
                (product.getProductType().indexOf("_FS") > 0);
    }
}
