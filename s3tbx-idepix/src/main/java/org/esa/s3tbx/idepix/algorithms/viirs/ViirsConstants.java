package org.esa.s3tbx.idepix.algorithms.viirs;

/**
 * Constants for Idepix VIIRS algorithm
 *
 * @author olafd
 */
public class ViirsConstants {


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
}
