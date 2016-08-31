package org.esa.s3tbx.idepix.core;

import java.util.regex.Pattern;

/**
 * IDEPIX constants
 *
 * @author Olaf Danne
 */
public class IdepixConstants {

    public static final int F_INVALID = 0;
    public static final int F_CLOUD = 1;
    public static final int F_CLOUD_AMBIGUOUS = 2;
    public static final int F_CLOUD_SURE = 3;
    public static final int F_CLOUD_BUFFER = 4;
    public static final int F_CLOUD_SHADOW = 5;
    public static final int F_COASTLINE = 6;
    public static final int F_CLEAR_SNOW = 7;
    public static final int F_CLEAR_LAND = 8;
    public static final int F_CLEAR_WATER = 9;
    public static final int F_LAND = 10;
    public static final int F_WATER = 11;
    public static final int F_BRIGHT = 12;
    public static final int F_WHITE = 13;
    public static final int F_BRIGHTWHITE = 14;
    public static final int F_HIGH = 15;
    public static final int F_VEG_RISK = 16;
    public static final int F_SEAICE = 17;
    public static final int F_HAZE = 18;

    public static final String SPOT_VGT_PRODUCT_TYPE_PREFIX = "VGT";
    public static final String PROBAV_PRODUCT_TYPE_PREFIX = "PROBA-V";

    public static final String AVHRR_L1b_PRODUCT_TYPE = "AVHRR";
    public static final String AVHRR_L1b_USGS_PRODUCT_TYPE = "NOAA_POD_AVHRR_HRPT";

    public static final int PRODUCT_TYPE_INVALID = -1;
    public static final int PRODUCT_TYPE_MERIS = 0;
    public static final int PRODUCT_TYPE_AATSR = 1;
    public static final int PRODUCT_TYPE_VGT = 2;

    public static final String VGT_RADIANCE_0_BAND_NAME = "B0";
    public static final String VGT_RADIANCE_2_BAND_NAME = "B2";
    public static final String VGT_RADIANCE_3_BAND_NAME = "B3";
    public static final String VGT_RADIANCE_MIR_BAND_NAME = "MIR";

    public static final String PROBAV_BLUE_BAND_NAME = "TOA_REFL_BLUE";
    public static final String PROBAV_RED_BAND_NAME = "TOA_REFL_RED";
    public static final String PROBAV_NIR_BAND_NAME = "TOA_REFL_NIR";
    public static final String PROBAV_SWIR_BAND_NAME = "TOA_REFL_SWIR";

    public static final int NO_DATA_VALUE = -1;

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

    public static final float[] MERIS_WAVELENGTHS =
            {
                    412.7f, 442.5f, 489.9f, 509.8f, 559.7f, 619.6f, 664.6f, 680.8f,
                    708.3f, 753.3f, 761.5f, 778.4f, 864.9f, 884.9f, 900.0f
            };

    public static final String[] MERIS_BRR_BAND_NAMES = {
            "brr_1",
            "brr_2",
            "brr_3",
            "brr_4",
            "brr_5",
            "brr_6",
            "brr_7",
            "brr_8",
            "brr_9",
            "brr_10",
            "brr_12",
            "brr_13",
            "brr_14"
    };

    public static final String INPUT_INCONSISTENCY_ERROR_MESSAGE =
            "Selected cloud screening algorithm cannot be used with given input product. \n\n" +
                    "Valid are: MERIS, VGT, AATSR, AVHRR, MODIS, Landsat8, SeaWIFS, colocated MERIS/AATSR L1b products.";

    public static final String CTP_MODE_DEFAULT = "Derive from Neural Net";

    // constants for spectral unmixing
    public static final String[] SMA_SOURCE_BAND_NAMES = {"brr_5_n", "brr_7_n", "brr_9_n", "brr_10_n", "brr_12_n", "brr_13_n"};
    public static final String[] SMA_ENDMEMBER_NAMES = {"Land", "Water", "Coast", "Cloud"};
    public static final double[] SMA_ENDMEMBER_WAVELENGTHS =
            {559.694, 664.57306, 708.32904, 753.37103, 778.40906, 864.87604};
    public static final double[] SMA_ENDMEMBER_BANDWIDTHS =
            {9.97, 9.985, 9.992, 7.495, 15.01, 20.047};
    public static final double[][] SMA_ENDMEMBER_RADIATIONS =
            {{0.06874453, 0.05234256, 0.10713479, 0.2107095, 0.22287288, 0.24322398},
            {0.026597029, 0.014183232, 0.012450832, 0.011182333, 0.01058279, 0.008555549},
            {0.061452672, 0.03917208, 0.046320472, 0.06117781, 0.06220935, 0.061626144},
            {0.4057965, 0.41043115, 0.43384373, 0.47499827, 0.48148763, 0.49312785}};
    public static final String[] SMA_ABUNDANCE_BAND_NAMES = {"Land_abundance", "Water_abundance", "Cloud_abundance"};
    public static final String SMA_SUMMARY_BAND_NAME = "summary_error";

    /**
     * A pattern which matches MERIS CC L1P product types
     *
     * @see java.util.regex.Matcher
     */
    public static final Pattern MERIS_CCL1P_TYPE_PATTERN = Pattern.compile("MER_..._CCL1P");

    private IdepixConstants() {
    }
}
