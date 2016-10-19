package org.esa.s3tbx.idepix.algorithms.viirs;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 19.10.2016
 * Time: 12:25
 *
 * @author olafd
 */
public class ViirsConstants {

    public static final String CLASSIF_BAND_NAME = "pixel_classif_flags";
    public static final String LAND_WATER_FRACTION_BAND_NAME = "land_water_fraction";

    public static final int F_INVALID = 0;
    public static final int F_CLOUD = 1;
    public static final int F_CLOUD_AMBIGUOUS = 2;
    public static final int F_CLOUD_SURE = 3;
    public static final int F_CLOUD_BUFFER = 4;
    public static final int F_CLOUD_SHADOW = 5;
    public static final int F_SNOW_ICE = 6;
    public static final int F_MIXED_PIXEL = 7;
    public static final int F_GLINT_RISK = 8;
    public static final int F_COASTLINE = 9;
    public static final int F_LAND = 10;
    public static final int F_BRIGHT = 11;

    public static final String F_INVALID_DESCR_TEXT = "Invalid pixel";
    public static final String F_CLOUD_DESRC_TEXT = "Cloudy pixel (sure or ambiguous)";
    public static final String F_CLOUD_AMBIGUOUS_DESRC_TEXT = "Cloudy pixel (ambiguous)";
    public static final String F_CLOUD_SURE_DESCR_TEXT = "Cloudy pixel (sure)";
    public static final String F_CLOUD_BUFFER_DESCR_TEXT = "Cloud buffer pixel";
    public static final String F_CLOUD_SHADOW_DESCR_TEXT = "Cloud shadow pixel";
    public static final String F_SNOW_ICE_DESCR_TEXT = "Snow/ice pixel";
    public static final String F_MIXED_PIXEL_DESCR_TEXT = "Mixed pixel";
    public static final String F_GLINT_RISK_DESCR_TEXT = "Glint risk pixel";
    public static final String F_COASTLINE_DESCR_TEXT = "Coastline pixel";
    public static final String F_LAND_DESCR_TEXT = "Land pixel";
    public static final String F_BRIGHT_DESCR_TEXT = "Bright pixel";

    public final static String VIIRS_REFLECTANCE_1_BAND_NAME = "rhot_410";
    public final static String VIIRS_REFLECTANCE_2_BAND_NAME = "rhot_443";
    public final static String VIIRS_REFLECTANCE_3_BAND_NAME = "rhot_486";
    public final static String VIIRS_REFLECTANCE_4_BAND_NAME = "rhot_551";
    public final static String VIIRS_REFLECTANCE_5_BAND_NAME = "rhot_671";
    public final static String VIIRS_REFLECTANCE_6_BAND_NAME = "rhot_745";
    public final static String VIIRS_REFLECTANCE_7_BAND_NAME = "rhot_862";
    public final static String VIIRS_REFLECTANCE_8_BAND_NAME = "rhot_1238";
    public final static String VIIRS_REFLECTANCE_9_BAND_NAME = "rhot_1601";
    public final static String VIIRS_REFLECTANCE_10_BAND_NAME = "rhot_2257";

    public static String[] VIIRS_SPECTRAL_BAND_NAMES = {
            VIIRS_REFLECTANCE_1_BAND_NAME,
            VIIRS_REFLECTANCE_2_BAND_NAME,
            VIIRS_REFLECTANCE_3_BAND_NAME,
            VIIRS_REFLECTANCE_4_BAND_NAME,
            VIIRS_REFLECTANCE_5_BAND_NAME,
            VIIRS_REFLECTANCE_6_BAND_NAME,
            VIIRS_REFLECTANCE_7_BAND_NAME,
            VIIRS_REFLECTANCE_8_BAND_NAME,
            VIIRS_REFLECTANCE_9_BAND_NAME,
            VIIRS_REFLECTANCE_10_BAND_NAME,
    };
    public final static int VIIRS_L1B_NUM_SPECTRAL_BANDS = VIIRS_SPECTRAL_BAND_NAMES.length;

    // debug bands:
    public static final String BRIGHTNESS_BAND_NAME = "brightness_value";
    public static final String NDSI_BAND_NAME = "ndsi_value";
    public static final String SCHILLER_NN_OUTPUT_BAND_NAME = "nn_value";
}
