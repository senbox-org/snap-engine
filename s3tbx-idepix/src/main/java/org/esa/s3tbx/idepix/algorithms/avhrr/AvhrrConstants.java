package org.esa.s3tbx.idepix.algorithms.avhrr;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 28.11.2014
 * Time: 11:17
 *
 * @author olafd
 */
public class AvhrrConstants {

    public static final String AVHRR_AC_ALBEDO_1_BAND_NAME = "albedo_1";
    public static final String AVHRR_AC_ALBEDO_2_BAND_NAME = "albedo_2";

    public static final String[] AVHRR_AC_ALBEDO_BAND_NAMES = {
            AVHRR_AC_ALBEDO_1_BAND_NAME,
            AVHRR_AC_ALBEDO_2_BAND_NAME,
    };

    public static final String AVHRR_AC_RADIANCE_1_BAND_NAME = "radiance_1";
    public static final String AVHRR_AC_RADIANCE_2_BAND_NAME = "radiance_2";
    public static final String AVHRR_AC_RADIANCE_3_BAND_NAME = "radiance_3";
    public static final String AVHRR_AC_RADIANCE_4_BAND_NAME = "radiance_4";
    public static final String AVHRR_AC_RADIANCE_5_BAND_NAME = "radiance_5";

    public static final String[] AVHRR_AC_RADIANCE_BAND_NAMES = {
            AVHRR_AC_RADIANCE_1_BAND_NAME,
            AVHRR_AC_RADIANCE_2_BAND_NAME,
            AVHRR_AC_RADIANCE_3_BAND_NAME,
            AVHRR_AC_RADIANCE_4_BAND_NAME,
            AVHRR_AC_RADIANCE_5_BAND_NAME
    };

    static final int SRC_USGS_SZA = 0;
    static final int SRC_USGS_LAT = 1;
    static final int SRC_USGS_LON = 2;
    static final int SRC_USGS_ALBEDO_1 = 3;
    static final int SRC_USGS_ALBEDO_2 = 4;
    static final int SRC_USGS_RADIANCE_3 = 5;
    static final int SRC_USGS_RADIANCE_4 = 6;
    static final int SRC_USGS_RADIANCE_5 = 7;
    static final int SRC_USGS_WATERFRACTION = 8;

    public static final double SOLAR_3b = 4.448;
    // first value of the following constants is for NOAA11, second value for NOAA14
    public static final double[] EW_3b = {278.85792,284.69366};
    public static final double[] A0 = {6.34384,4.00162};
    public static final double[] B0 = {2.68468,0.98107};
    public static final double[] C0 = {-1.70931,1.9789};
    public static final double[] a1_3b = {-1.738973,-1.88533};
    public static final double[] a2_3b = {1.003354,1.003839};
    public static final double c1 = 1.1910659*1.E-5; // mW/(m^2 sr cm^-4)
    public static final double c2 = 1.438833;

    // different central wave numbers for AVHRR Channel3b, 4, 5 correspond to the temperature ranges & to NOAA11 and NOAA14
    // NOAA 11_3b: 180-225	2663.500, 225-275	2668.150, 275-320	2671.400, 270-310	2670.960
    // NOAA 11_4:  180-225	 926.810, 225-275	 927.360, 275-320	 927.830, 270-310	 927.750
    // NOAA 11_5:  180-225	 841.400, 225-275	 841.810, 275-320	 842.200, 270-310	 842.140
    // NOAA 14_3b: 190-230	2638.652, 230-270	2642.807, 270-310	2645.899, 290-330	2647.169
    // NOAA 14_4:  190-230	928.2603, 230-270	928.8284, 270-310	929.3323, 290-330	929.5878
    // NOAA 14_5:  190-230	834.4496, 230-270	834.8066, 270-310	835.1647, 290-330	 835.374


    public static final double TGCT_THRESH = 260.0;

    public static final double EMISSIVITY_THRESH = 0.022;
    public static final double LAT_MAX_THRESH = 60.0;

    public static double[] fmftTestThresholds = new double[] {
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.01, 0.03, 0.05, 0.08, 0.11, 0.14, 0.18, 0.23, 0.28,
        0.34, 0.41, 0.48, 0.57, 0.66, 0.76, 0.87, 1.0, 1.13, 1.27,
        1.42, 1.59, 1.76, 1.94, 2.14, 2.34, 2.55, 2.77, 3.0, 3.24,
        3.48, 3.73, 3.99, 4.26, 4.52, 4.80, 5.0, 5.35, 5.64, 5.92,
        6.20, 6.48, 6.76, 7.03, 7.30, 7.8, 7.8, 7.8, 7.8, 7.8,
        7.8, 7.8, 7.8, 7.8, 7.8, 7.8, 7.8, 7.8, 7.8, 7.8,
        7.8
    };

    public static double[] tmftTestMaxThresholds = new double[] {
            2.635, 2.505, 3.395, 3.5,
            2.635, 2.505, 3.395, 3.5,
            2.635, 2.505, 3.395, 3.5,
            2.635, 2.505, 3.395, 3.5,
            2.615, 2.655, 2.685, 2.505,
            1.865, 1.835, 1.845, 1.915,
            1.815, 1.785, 1.815, 1.795,
            1.885, 1.885, 1.875, 1.875,
            2.135, 2.115, 2.095, 2.105,
            6.825, 7.445, 8.305, 7.125,
            19.055, 18.485, 17.795, 17.025,
            20.625, 19.775, 19.355, 19.895,
            18.115, 15.935, 20.395, 16.025,
            18.115, 15.935, 20.395, 16.025
    };

    public static double[] tmftTestMinThresholds = new double[] {
            0.145, -0.165, -0.075, -0.075,
            0.145, -0.165, -0.075, -0.075,
            0.145, -0.165, -0.075, -0.075,
            0.145, -0.165, -0.075, -0.075,
            -0.805, -0.975, -0.795, -1.045,
            -1.195, -1.065, -1.125, -1.175,
            -1.225, -1.285, -1.285, -1.285,
            -2.425, -1.325, -2.105, -1.975,
            -1.685, -1.595, -1.535, -2.045,
            -4.205, -4.145, -3.645, -3.585,
            -2.425, -1.715, -2.275, -2.105,
            0.585, -0.585, 0.825, 0.345,
            0.655, 1.905, 0.475, 1.385,
            0.655, 1.905, 0.475, 1.385
    };
}
