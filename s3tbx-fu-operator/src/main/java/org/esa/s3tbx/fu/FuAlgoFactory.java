
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
class FuAlgoFactory {
    private static final double[] MERIS_XFACTOR = new double[]{2.957, 10.861, 3.744, 3.750, 34.687, 41.853, 7.619, 0.844, 0.189};
    private static final double[] MERIS_YFACTOR = new double[]{0.112, 1.711, 5.672, 23.263, 48.791, 23.949, 2.944, 0.307, 0.068};
    private static final double[] MERIS_ZFACTOR = new double[]{14.354, 58.356, 28.227, 4.022, 0.618, 0.026, 0.000, 0.000, 0.000};
    private static final double[] MERIS_POLYFACTOR = new double[]{-12.0506, 88.9325, -244.6960, 305.2361, -164.6960, 28.5255};

    private static final double[] OLCI_XFACTOR = new double[]{0.154, 2.957, 10.861, 3.744, 3.750, 34.687, 41.853, 7.323, 0.591, 0.549, 0.189};
    private static final double[] OLCI_YFACTOR = new double[]{0.004, 0.112, 1.711, 5.672, 23.263, 48.791, 23.949, 2.836, 0.216, 0.199, 0.068};
    private static final double[] OLCI_ZFACTOR = new double[]{0.731, 14.354, 58.356, 28.227, 4.022, 0.618, 0.026, 0.000, 0.000, 0.000, 0.000};
    private static final double[] OLCI_POLYFACTOR = new double[]{-12.5076, 91.6345, -249.848, 308.6561, -165.4818, 28.5608};

    private static final double[] MODIS_XFACTOR = new double[]{2.957, 10.861, 4.031, 3.989, 49.037, 34.586, 0.829};
    private static final double[] MODIS_YFACTOR = new double[]{0.112, 1.711, 11.106, 22.579, 51.477, 19.452, 0.301};
    private static final double[] MODIS_ZFACTOR = new double[]{14.354, 58.356, 29.993, 2.618, 0.262, 0.000, 0.000};
    private static final double[] MODIS_POLYFACTOR = new double[]{-48.0880, 362.6179, -1011.7151, 1262.0348, -666.5981, 113.9215};

    private static final double[] SEA_WI_FS_XFACTOR = new double[]{2.957, 10.861, 3.744, 3.455, 52.304, 32.825};
    private static final double[] SEA_WI_FS_YFACTOR = new double[]{0.112, 1.711, 5.672, 21.929, 59.454, 17.810};
    private static final double[] SEA_WI_FS_ZFACTOR = new double[]{14.354, 58.356, 28.227, 3.967, 0.682, 0.018};
    private static final double[] SEA_WI_FS_POLYFACTOR = new double[]{-49.4377, 363.2770, -978.1648, 1154.6030, -552.2701, 78.2940};

    private Instrument instrument;

    FuAlgoFactory(Instrument instrument) {
        this.instrument = instrument;
    }

    FuAlgo create() {
        final FuAlgoImpl fuAlgo = new FuAlgoImpl();
        switch (instrument) {
            case MERIS:
                fuAlgo.setX3Factors(MERIS_XFACTOR);
                fuAlgo.setY3Factors(MERIS_YFACTOR);
                fuAlgo.setZ3Factors(MERIS_ZFACTOR);
                fuAlgo.setPolyCoeffs(MERIS_POLYFACTOR);
                break;
            case MODIS:
                fuAlgo.setX3Factors(MODIS_XFACTOR);
                fuAlgo.setY3Factors(MODIS_YFACTOR);
                fuAlgo.setZ3Factors(MODIS_ZFACTOR);
                fuAlgo.setPolyCoeffs(MODIS_POLYFACTOR);
                break;
            case OLCI:
                fuAlgo.setX3Factors(OLCI_XFACTOR);
                fuAlgo.setY3Factors(OLCI_YFACTOR);
                fuAlgo.setZ3Factors(OLCI_ZFACTOR);
                fuAlgo.setPolyCoeffs(OLCI_POLYFACTOR);

                break;
            case SEAWIFS:
                fuAlgo.setX3Factors(SEA_WI_FS_XFACTOR);
                fuAlgo.setY3Factors(SEA_WI_FS_YFACTOR);
                fuAlgo.setZ3Factors(SEA_WI_FS_ZFACTOR);
                fuAlgo.setPolyCoeffs(SEA_WI_FS_POLYFACTOR);
                break;
        }
        return fuAlgo;
    }
}
