/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.owt;

// todo (mp) - configuration of AuxdataFactory should be turned into a configuration object
// todo (mp) - the actually used wavelength could be a parameter to getAuxdataFactory(); this way it would be configurable by the user
// todo        and it would be better separated

public enum OWT_TYPE {
    COASTAL {
        private float[] wavelength = new float[]{410, 443, 490, 510, 555};

        @Override
        AuxdataFactory getAuxdataFactory() {
            return new CoastalAuxdataFactory("/auxdata/coastal/owt16_meris_stats_101119_5band.hdf");
        }

        @Override
        int getClassCount() {
            return 9;
        }

        @Override
        double[] mapMembershipsToClasses(double[] memberships) {
            double[] classes = new double[getClassCount()];
            System.arraycopy(memberships, 0, classes, 0, 8);
            // setting the value for the 9th class to the sum of the last 8 classes
            for (int i = 8; i < memberships.length; i++) {
                classes[8] += memberships[i];
            }
            return classes;
        }

        @Override
        float[] getWavelengths() {
            return wavelength;
        }
    },
    INLAND {
        private final float[] ALL_WAVELENGTHS = new float[]{412, 443, 490, 510, 531, 547, 555, 560, 620, 665, 667, 670, 678, 680, 709, 748, 754};
        private final String COVARIANCE_MATRIX_RESOURCE = "/auxdata/inland/rrs_owt_cov_inland.hdf";
        private final String SPECTRAL_MEANS_RESOURCE = "/auxdata/inland/rrs_owt_means_inland.hdf";
        private String covariance = "rrs_cov";
        private String owt_means = "class_means";
        private float[] wavelength = new float[]{412, 443, 490, 510, 560, 620, 665, 680, 709, 754};

        @Override
        AuxdataFactory getAuxdataFactory() {
            return new HyperspectralAuxdataFactory(wavelength, ALL_WAVELENGTHS, 10, COVARIANCE_MATRIX_RESOURCE, covariance, SPECTRAL_MEANS_RESOURCE,
                                                   owt_means);
        }

        @Override
        float[] getWavelengths() {
            return wavelength;

        }

        @Override
        int getClassCount() {
            return 7;
        }

    },
    INLAND_NO_BLUE_BAND {

        private final float[] ALL_WAVELENGTHS = new float[]{412, 443, 490, 510, 531, 547, 555, 560, 620, 665, 667, 670, 678, 680, 709, 748, 754};
        private final String COVARIANCE_MATRIX_RESOURCE = "/auxdata/inland/rrs_owt_cov_inland.hdf";
        private final String SPECTRAL_MEANS_RESOURCE = "/auxdata/inland/rrs_owt_means_inland.hdf";
        private String covariance = "rrs_cov";
        private String owt_means = "class_means";
        private float[] wavelength = new float[]{443, 490, 510, 560, 620, 665, 680, 709, 754};

        @Override
        AuxdataFactory getAuxdataFactory() {
            return new HyperspectralAuxdataFactory(wavelength, ALL_WAVELENGTHS, 10, COVARIANCE_MATRIX_RESOURCE, covariance, SPECTRAL_MEANS_RESOURCE,
                                                   owt_means);
        }

        @Override
        float[] getWavelengths() {
            return wavelength;

        }

        @Override
        int getClassCount() {
            return 7;
        }

    },
    GLASS_5C {
        private final float[] ALL_WAVELENGTHS = new float[]{
                400, 403, 406, 409, 412, 415, 418, 421, 424, 427, 430, 433, 436, 439, 442,
                445, 448, 451, 454, 457, 460, 463, 466, 469, 472, 475, 478, 481, 484, 487,
                490, 493, 496, 499, 502, 505, 508, 511, 514, 517, 520, 523, 526, 529, 532,
                535, 538, 541, 544, 547, 550, 553, 556, 559, 562, 565, 568, 571, 574, 577,
                580, 583, 586, 589, 592, 595, 598, 601, 604, 607, 610, 613, 616, 619, 622,
                625, 628, 631, 634, 637, 640, 643, 646, 649, 652, 655, 658, 661, 664, 667,
                670, 673, 676, 679, 682, 685, 688, 691, 694, 697, 700, 703, 706, 709, 712,
                715, 718, 721, 724, 727, 730, 733, 736, 739, 742, 745, 748, 751, 754, 757,
                760, 763, 766, 769, 772, 775, 778, 781, 784, 787, 790, 793, 796, 799
        };
        private final float MAX_DISTANCE = 1.5f;

        private float[] wavelength = new float[]{442.6f, 489.9f, 509.8f, 559.7f, 619.6f, 664.6f, 680.8f, 708.3f, 753.4f};
        private String auxdataResource = "/auxdata/glass/Rrs_Glass_5C_owt_stats_140912.hdf";
        private String covariance = "covariance";
        private String owt_means = "owt_means";

        @Override
        AuxdataFactory getAuxdataFactory() {
            return new HyperspectralAuxdataFactory(wavelength, ALL_WAVELENGTHS, MAX_DISTANCE,
                                                   auxdataResource, covariance, auxdataResource, owt_means
            );
        }

        @Override
        int getClassCount() {
            return 5;
        }

        @Override
        float[] getWavelengths() {
            return wavelength;
        }
    },
    GLASS_6C {
        private final float[] ALL_WAVELENGTHS = new float[]{
                400, 403, 406, 409, 412, 415, 418, 421, 424, 427, 430, 433, 436, 439, 442,
                445, 448, 451, 454, 457, 460, 463, 466, 469, 472, 475, 478, 481, 484, 487,
                490, 493, 496, 499, 502, 505, 508, 511, 514, 517, 520, 523, 526, 529, 532,
                535, 538, 541, 544, 547, 550, 553, 556, 559, 562, 565, 568, 571, 574, 577,
                580, 583, 586, 589, 592, 595, 598, 601, 604, 607, 610, 613, 616, 619, 622,
                625, 628, 631, 634, 637, 640, 643, 646, 649, 652, 655, 658, 661, 664, 667,
                670, 673, 676, 679, 682, 685, 688, 691, 694, 697, 700, 703, 706, 709, 712,
                715, 718, 721, 724, 727, 730, 733, 736, 739, 742, 745, 748, 751, 754, 757,
                760, 763, 766, 769, 772, 775, 778, 781, 784, 787, 790, 793, 796, 799
        };
        private final float MAX_DISTANCE = 1.5f;
        private float[] wavelength = new float[]{442.6f, 489.9f, 509.8f, 559.7f, 619.6f, 664.6f, 680.8f, 708.3f, 753.4f};
        private String auxdataResource = "/auxdata/glass/Rrs_Glass_6C_owt_stats_140912.hdf";
        private String covariance = "covariance";
        private String owt_means = "owt_means";

        @Override
        AuxdataFactory getAuxdataFactory() {
            return new HyperspectralAuxdataFactory(wavelength, ALL_WAVELENGTHS, MAX_DISTANCE,
                                                   auxdataResource, covariance, auxdataResource, owt_means);
        }

        @Override
        int getClassCount() {
            return 6;
        }

        @Override
        float[] getWavelengths() {
            return wavelength;
        }

    },
    GLASS_6C_NORMALISED {
        private final float[] ALL_WAVELENGTHS = new float[]{442, 490, 511, 559, 619, 664, 682, 709, 754};
        private final float MAX_DISTANCE = 1.5f;
        private float[] wavelength = new float[]{442.6f, 489.9f, 509.8f, 559.7f, 619.6f, 664.6f, 680.8f, 708.3f, 753.4f};
        private String auxdataResource = "/auxdata/glass/Rrs_Glass_norm6C_owt_stats_140918.hdf";
        private String covariance = "inverted_covariance";
        private String owt_means = "owt_means";

        @Override
        AuxdataFactory getAuxdataFactory() {
            return new HyperspectralAuxdataFactory(wavelength, ALL_WAVELENGTHS, MAX_DISTANCE,
                                                   auxdataResource, covariance, false,
                                                   auxdataResource, owt_means);
        }

        @Override
        int getClassCount() {
            return 6;
        }

        @Override
        float[] getWavelengths() {
            return wavelength;
        }

        @Override
        boolean mustNormalizeSpectra() {
            return true;
        }

    },
    CALIMNOS {
        private final float[] ALL_WAVELENGTHS = new float[]{
                400, 403, 406, 409, 412, 415, 418, 421, 424, 427, 430, 433, 436, 439, 442,
                445, 448, 451, 454, 457, 460, 463, 466, 469, 472, 475, 478, 481, 484, 487,
                490, 493, 496, 499, 502, 505, 508, 511, 514, 517, 520, 523, 526, 529, 532,
                535, 538, 541, 544, 547, 550, 553, 556, 559, 562, 565, 568, 571, 574, 577,
                580, 583, 586, 589, 592, 595, 598, 601, 604, 607, 610, 613, 616, 619, 622,
                625, 628, 631, 634, 637, 640, 643, 646, 649, 652, 655, 658, 661, 664, 667,
                670, 673, 676, 679, 682, 685, 688, 691, 694, 697, 700, 703, 706, 709, 712,
                715, 718, 721, 724, 727, 730, 733, 736, 739, 742, 745, 748, 751, 754, 757,
                760, 763, 766, 769, 772, 775, 778, 781, 784, 787, 790, 793, 796, 799
        };
        private final float MAX_DISTANCE = 1.5f;

        private float[] wavelength = new float[]{442.6f, 489.9f, 509.8f, 559.7f, 619.6f, 664.6f, 680.8f, 708.3f, 753.4f};
        private String auxdataResource = "/auxdata/calimnos/Rrs_Globo_14C_owt_stats_050416.hdf";
        private String covariance = "covariance_globo";
        private String owt_means = "owt_means_globo";

        @Override
        AuxdataFactory getAuxdataFactory() {
            return new HyperspectralAuxdataFactory(wavelength, ALL_WAVELENGTHS, MAX_DISTANCE,
                                                   auxdataResource, covariance, auxdataResource, owt_means
            );
        }

        @Override
        int getClassCount() {
            return 13;
        }

        @Override
        float[] getWavelengths() {
            return wavelength;
        }

    };

    abstract AuxdataFactory getAuxdataFactory();

    abstract float[] getWavelengths();

    abstract int getClassCount();

    double[] mapMembershipsToClasses(double[] memberships) {
        return memberships;
    }

    boolean mustNormalizeSpectra() {
        return false;
    }

}
