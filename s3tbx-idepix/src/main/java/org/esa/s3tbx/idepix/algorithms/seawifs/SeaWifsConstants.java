package org.esa.s3tbx.idepix.algorithms.seawifs;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 18.10.2016
 * Time: 14:43
 *
 * @author olafd
 */
public class SeaWifsConstants {

    public static final String CLASSIF_BAND_NAME = "pixel_classif_flags";
    public static final String LAND_WATER_FRACTION_BAND_NAME = "land_water_fraction";

    // debug bands:
    public static final String BRIGHTNESS_BAND_NAME = "brightness_value";
    public static final String NDSI_BAND_NAME = "ndsi_value";
    public static final String SCHILLER_NN_OUTPUT_BAND_NAME = "nn_value";

    static final int SRC_SZA = 0;
    static final int SRC_SAA = 1;
    static final int SRC_VZA = 2;
    static final int SRC_VAA = 3;
    static final int SEAWIFS_SRC_RAD_OFFSET = 8;

    private static final String SEAWIFS_L1B_RADIANCE_1_BAND_NAME = "412";
    private static final String SEAWIFS_L1B_RADIANCE_2_BAND_NAME = "443";
    private static final String SEAWIFS_L1B_RADIANCE_3_BAND_NAME = "490";
    private static final String SEAWIFS_L1B_RADIANCE_4_BAND_NAME = "510";
    private static final String SEAWIFS_L1B_RADIANCE_5_BAND_NAME = "555";
    private static final String SEAWIFS_L1B_RADIANCE_6_BAND_NAME = "670";
    private static final String SEAWIFS_L1B_RADIANCE_7_BAND_NAME = "765";
    private static final String SEAWIFS_L1B_RADIANCE_8_BAND_NAME = "865";

    static final String[] SEAWIFS_L1B_SPECTRAL_BAND_NAMES = {
            SEAWIFS_L1B_RADIANCE_1_BAND_NAME,
            SEAWIFS_L1B_RADIANCE_2_BAND_NAME,
            SEAWIFS_L1B_RADIANCE_3_BAND_NAME,
            SEAWIFS_L1B_RADIANCE_4_BAND_NAME,
            SEAWIFS_L1B_RADIANCE_5_BAND_NAME,
            SEAWIFS_L1B_RADIANCE_6_BAND_NAME,
            SEAWIFS_L1B_RADIANCE_7_BAND_NAME,
            SEAWIFS_L1B_RADIANCE_8_BAND_NAME,
    };

    static final int SEAWIFS_L1B_NUM_SPECTRAL_BANDS = SEAWIFS_L1B_SPECTRAL_BAND_NAMES.length;

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
}
