package org.esa.s3tbx.idepix.core;

import java.util.regex.Pattern;

/**
 * IDEPIX constants
 *
 * @author Olaf Danne
 */
public class IdepixConstants {

    public static final String CLASSIF_BAND_NAME = "pixel_classif_flags";
    public static final String LAND_WATER_FRACTION_BAND_NAME = "land_water_fraction";

    public static final int F_INVALID = 0;
    public static final int F_CLOUD = 1;
    public static final int F_CLOUD_AMBIGUOUS = 2;
    public static final int F_CLOUD_SURE = 3;
    public static final int F_CLOUD_BUFFER = 4;
    public static final int F_CLOUD_SHADOW = 5;
    public static final int F_SNOW_ICE = 6;
    public static final int F_CLEAR_LAND = 7;
    public static final int F_CLEAR_WATER = 8;
    public static final int F_LAND = 9;
    public static final int F_WATER = 10;
    public static final int F_COASTLINE = 11;
    public static final int F_MIXED_PIXEL = 12;
    public static final int F_BRIGHT = 13;
    public static final int F_WHITE = 14;
    public static final int F_GLINT_RISK = 15;

    public static final int NUM_DEFAULT_FLAGS = 16;

    public static final String F_INVALID_DESCR_TEXT = "Invalid pixels";
    public static final String F_CLOUD_DESCR_TEXT = "Pixels which are either cloud_sure or cloud_ambiguous";
    public static final String F_CLOUD_AMBIGUOUS_DESCR_TEXT =
            "Semi transparent clouds, or clouds where the detection level is uncertain";
    public static final String F_CLOUD_SURE_DESCR_TEXT = "Fully opaque clouds with full confidence of their detection";
    public static final String F_CLOUD_BUFFER_DESCR_TEXT =
            "A buffer of n pixels around a cloud. n is a user supplied parameter. Applied to pixels masked as 'cloud'";
    public static final String F_CLOUD_SHADOW_DESCR_TEXT = "Pixels is affect by a cloud shadow";
    public static final String F_SNOW_ICE_DESCR_TEXT = "Clear snow/ice pixels";
    public static final String F_CLEAR_LAND_DESCR_TEXT = "Clear land pixels";
    public static final String F_CLEAR_WATER_DESCR_TEXT = "Clear water pixels";
    public static final String F_LAND_DESCR_TEXT = "Land pixels";
    public static final String F_WATER_DESCR_TEXT = "Water pixels";
    public static final String F_COASTLINE_DESCR_TEXT = "Pixels at a coastline";
    public static final String F_MIXED_PIXEL_DESCR_TEXT = "Mixed pixel";
    public static final String F_BRIGHT_DESCR_TEXT = "Bright pixels";
    public static final String F_WHITE_DESCR_TEXT = "White pixels";
    public static final String F_GLINT_RISK_DESCR_TEXT = "Glint risk pixel";

    public static final int LAND_WATER_MASK_RESOLUTION = 50;
    public static final int OVERSAMPLING_FACTOR_X = 3;
    public static final int OVERSAMPLING_FACTOR_Y = 3;

    public static final String SPOT_VGT_PRODUCT_TYPE_PREFIX = "VGT";
    public static final String PROBAV_PRODUCT_TYPE_PREFIX = "PROBA-V";

    public static final String AVHRR_L1b_PRODUCT_TYPE = "AVHRR";
    public static final String AVHRR_L1b_USGS_PRODUCT_TYPE = "NOAA_POD_AVHRR_HRPT";

    public static final String VGT_RADIANCE_0_BAND_NAME = "B0";
    public static final String VGT_RADIANCE_2_BAND_NAME = "B2";
    public static final String VGT_RADIANCE_3_BAND_NAME = "B3";
    public static final String VGT_RADIANCE_MIR_BAND_NAME = "MIR";

    public static final String PROBAV_BLUE_BAND_NAME = "TOA_REFL_BLUE";
    public static final String PROBAV_RED_BAND_NAME = "TOA_REFL_RED";
    public static final String PROBAV_NIR_BAND_NAME = "TOA_REFL_NIR";
    public static final String PROBAV_SWIR_BAND_NAME = "TOA_REFL_SWIR";

    public static final int NO_DATA_VALUE = -1;

    public static final String NN_OUTPUT_BAND_NAME = "nn_value";

    /**
     * The names of the VGT spectral band names.
     */
    public static final String[] VGT_REFLECTANCE_BAND_NAMES = {
            VGT_RADIANCE_0_BAND_NAME, // 0
            VGT_RADIANCE_2_BAND_NAME, // 1
            VGT_RADIANCE_3_BAND_NAME, // 2
            VGT_RADIANCE_MIR_BAND_NAME // 3
    };

    public static final String[] VGT_ANNOTATION_BAND_NAMES = {
            "VZA",
            "SZA",
            "VAA",
            "SAA",
            "WVG",
            "OG",
            "AG"
    };

    public static final float[] VGT_WAVELENGTHS = {450.0f, 645.0f, 835.0f, 1670.0f};

    public static final String[] PROBAV_REFLECTANCE_BAND_NAMES = {
            PROBAV_BLUE_BAND_NAME,
            PROBAV_RED_BAND_NAME,
            PROBAV_NIR_BAND_NAME,
            PROBAV_SWIR_BAND_NAME
    };

    public static final String[] PROBAV_ANNOTATION_BAND_NAMES = {
            "VZA_SWIR",
            "VZA_VNIR",
            "SZA",
            "VAA_SWIR",
            "VAA_VNIR",
            "SAA",
            "NDVI"
    };

    public static final float[] PROBAV_WAVELENGTHS = {462.0f, 655.5f, 843.0f, 1599.0f};

    public static final String INPUT_INCONSISTENCY_ERROR_MESSAGE =
            "Selected cloud screening algorithm cannot be used with given input product. \n\n" +
                    "Valid are: MERIS, VGT, AATSR, AVHRR, MODIS, Landsat8, SeaWIFS, colocated MERIS/AATSR L1b products.";

    /**
     * A pattern which matches MERIS CC L1P product types
     *
     * @see java.util.regex.Matcher
     */
    public static final Pattern MERIS_CCL1P_TYPE_PATTERN = Pattern.compile("MER_..._CCL1P");

    private IdepixConstants() {
    }
}
