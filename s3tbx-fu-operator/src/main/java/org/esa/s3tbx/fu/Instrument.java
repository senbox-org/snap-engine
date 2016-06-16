/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s3tbx.fu;

/**
 * @author muhammad.bc
 */
enum Instrument {
    AUTO_DETECT(new double[]{}, new String[]{""}, false),
    MERIS(new double[]{412.691, 442.55902, 489.88202, 509.81903, 559.69403, 619.601, 664.57306, 680.82104, 708.32904},
          new String[]{"l2_flags.WATER", "not l1p_flags.CC_LAND and not l1p_flags.CC_CLOUD", "NOT l1_flags.LAND_OCEAN"}, true),
    OLCI(new double[]{400.0, 412.5, 442.5, 490.0, 510.0, 560.0, 620.0, 665.0, 673.75, 681.25, 708.75},
         new String[]{"WQSF_lsb.WATER and not WQSF_lsb.CLOUD", "LQSF.WATER"}, false),
    MODIS(new double[]{412.0, 443.0, 488.0, 531.0, 555.0, 667.0, 678.0},
          new String[]{"not l2_flags.LAND and not l2_flags.CLDICE"}, false),
    SEAWIFS(new double[]{412.0, 443.0, 490.0, 510.0, 555.0, 670.0},
            new String[]{"not l2_flags.LAND and not l2_flags.CLDICE"}, false);


    private final double[] wavelengths;
    private final String[] validExpression;
    private final boolean isIrradiance;

    Instrument(double[] wavelengths, String validExpression[], boolean isIrradiance) {
        this.wavelengths = wavelengths;
        this.validExpression = validExpression;
        this.isIrradiance = isIrradiance;
    }

    public String[] getValidExpression() {
        return validExpression;
    }

    public double[] getWavelengths() {
        return wavelengths;
    }

    public boolean isIrradiance() {
        return isIrradiance;
    }
}
