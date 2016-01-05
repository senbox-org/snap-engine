package org.esa.s3tbx.idepix.algorithms.modis;

/**
 * Idepix MODIS consztants
 *
 * @author olafd
 */
class ModisConstants {

    public static final String CLASSIF_BAND_NAME = "pixel_classif_flags";
    public static final String LAND_WATER_FRACTION_BAND_NAME = "land_water_fraction";

    // debug bands:
    public static final String SCHILLER_NN_OUTPUT_BAND_NAME = "schiller_nn_value";

    public static final int MODIS_SRC_RAD_OFFSET = 22;

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

    public static final String MODIS_L1B_REFLECTANCE_1_BAND_NAME = "EV_250_Aggr1km_RefSB.1";           // 645
    public static final String MODIS_L1B_REFLECTANCE_2_BAND_NAME = "EV_250_Aggr1km_RefSB.2";           // 859
    public static final String MODIS_L1B_REFLECTANCE_3_BAND_NAME = "EV_500_Aggr1km_RefSB.3";           // 469
    public static final String MODIS_L1B_REFLECTANCE_4_BAND_NAME = "EV_500_Aggr1km_RefSB.4";           // 555
    public static final String MODIS_L1B_REFLECTANCE_5_BAND_NAME = "EV_500_Aggr1km_RefSB.5";           // 1240
    public static final String MODIS_L1B_REFLECTANCE_6_BAND_NAME = "EV_500_Aggr1km_RefSB.6";           // 1640
    public static final String MODIS_L1B_REFLECTANCE_7_BAND_NAME = "EV_500_Aggr1km_RefSB.7";           // 2130
    public static final String MODIS_L1B_REFLECTANCE_8_BAND_NAME = "EV_1KM_RefSB.8";                    // 412
    public static final String MODIS_L1B_REFLECTANCE_9_BAND_NAME = "EV_1KM_RefSB.9";                    // 443
    public static final String MODIS_L1B_REFLECTANCE_10_BAND_NAME = "EV_1KM_RefSB.10";                   // 488
    public static final String MODIS_L1B_REFLECTANCE_11_BAND_NAME = "EV_1KM_RefSB.11";                   // 531
    public static final String MODIS_L1B_REFLECTANCE_12_BAND_NAME = "EV_1KM_RefSB.12";                   // 547
    public static final String MODIS_L1B_REFLECTANCE_13_BAND_NAME = "EV_1KM_RefSB.13lo";                 // 667
    public static final String MODIS_L1B_REFLECTANCE_14_BAND_NAME = "EV_1KM_RefSB.13hi";                 // 667
    public static final String MODIS_L1B_REFLECTANCE_15_BAND_NAME = "EV_1KM_RefSB.14lo";                 // 678
    public static final String MODIS_L1B_REFLECTANCE_16_BAND_NAME = "EV_1KM_RefSB.14hi";                 // 678
    public static final String MODIS_L1B_REFLECTANCE_17_BAND_NAME = "EV_1KM_RefSB.15";                   // 748
    public static final String MODIS_L1B_REFLECTANCE_18_BAND_NAME = "EV_1KM_RefSB.16";                   // 869
    public static final String MODIS_L1B_REFLECTANCE_19_BAND_NAME = "EV_1KM_RefSB.17";                   // 869
    public static final String MODIS_L1B_REFLECTANCE_20_BAND_NAME = "EV_1KM_RefSB.18";                   // 869
    public static final String MODIS_L1B_REFLECTANCE_21_BAND_NAME = "EV_1KM_RefSB.19";                   // 869
    public static final String MODIS_L1B_REFLECTANCE_22_BAND_NAME = "EV_1KM_RefSB.26";                   // 869

    public static final String[] MODIS_L1B_SPECTRAL_BAND_NAMES = {
            MODIS_L1B_REFLECTANCE_1_BAND_NAME,
            MODIS_L1B_REFLECTANCE_2_BAND_NAME,
            MODIS_L1B_REFLECTANCE_3_BAND_NAME,
            MODIS_L1B_REFLECTANCE_4_BAND_NAME,
            MODIS_L1B_REFLECTANCE_5_BAND_NAME,
            MODIS_L1B_REFLECTANCE_6_BAND_NAME,
            MODIS_L1B_REFLECTANCE_7_BAND_NAME,
            MODIS_L1B_REFLECTANCE_8_BAND_NAME,
            MODIS_L1B_REFLECTANCE_9_BAND_NAME,
            MODIS_L1B_REFLECTANCE_10_BAND_NAME,
            MODIS_L1B_REFLECTANCE_11_BAND_NAME,
            MODIS_L1B_REFLECTANCE_12_BAND_NAME,
            MODIS_L1B_REFLECTANCE_13_BAND_NAME,
            MODIS_L1B_REFLECTANCE_14_BAND_NAME,
            MODIS_L1B_REFLECTANCE_15_BAND_NAME,
            MODIS_L1B_REFLECTANCE_16_BAND_NAME,
            MODIS_L1B_REFLECTANCE_17_BAND_NAME,
            MODIS_L1B_REFLECTANCE_18_BAND_NAME,
            MODIS_L1B_REFLECTANCE_19_BAND_NAME,
            MODIS_L1B_REFLECTANCE_20_BAND_NAME,
            MODIS_L1B_REFLECTANCE_21_BAND_NAME,
            MODIS_L1B_REFLECTANCE_22_BAND_NAME,
    };
    public static final int MODIS_L1B_NUM_SPECTRAL_BANDS = MODIS_L1B_SPECTRAL_BAND_NAMES.length;

    public static final String MODIS_L1B_EMISSIVITY_1_BAND_NAME = "EV_1KM_Emissive.20";                    // 3750
    public static final String MODIS_L1B_EMISSIVITY_2_BAND_NAME = "EV_1KM_Emissive.21";                    // 3959
    public static final String MODIS_L1B_EMISSIVITY_3_BAND_NAME = "EV_1KM_Emissive.22";                    // 3959
    public static final String MODIS_L1B_EMISSIVITY_4_BAND_NAME = "EV_1KM_Emissive.23";                    // 4050
    public static final String MODIS_L1B_EMISSIVITY_5_BAND_NAME = "EV_1KM_Emissive.24";                    // 4465
    public static final String MODIS_L1B_EMISSIVITY_6_BAND_NAME = "EV_1KM_Emissive.25";                    // 4515
    public static final String MODIS_L1B_EMISSIVITY_7_BAND_NAME = "EV_1KM_Emissive.27";                    // 6715
    public static final String MODIS_L1B_EMISSIVITY_8_BAND_NAME = "EV_1KM_Emissive.28";                    // 7325
    public static final String MODIS_L1B_EMISSIVITY_9_BAND_NAME = "EV_1KM_Emissive.29";                    // 8550
    public static final String MODIS_L1B_EMISSIVITY_10_BAND_NAME = "EV_1KM_Emissive.30";                    // 9730
    public static final String MODIS_L1B_EMISSIVITY_11_BAND_NAME = "EV_1KM_Emissive.31";                    // 11030
    public static final String MODIS_L1B_EMISSIVITY_12_BAND_NAME = "EV_1KM_Emissive.32";                    // 12020
    public static final String MODIS_L1B_EMISSIVITY_13_BAND_NAME = "EV_1KM_Emissive.33";                    // 13335
    public static final String MODIS_L1B_EMISSIVITY_14_BAND_NAME = "EV_1KM_Emissive.34";                    // 13635
    public static final String MODIS_L1B_EMISSIVITY_15_BAND_NAME = "EV_1KM_Emissive.35";                    // 13935
    public static final String MODIS_L1B_EMISSIVITY_16_BAND_NAME = "EV_1KM_Emissive.36";                    // 14235

    public static final String[] MODIS_L1B_EMISSIVE_BAND_NAMES = {
            MODIS_L1B_EMISSIVITY_1_BAND_NAME,
            MODIS_L1B_EMISSIVITY_2_BAND_NAME,
            MODIS_L1B_EMISSIVITY_3_BAND_NAME,
            MODIS_L1B_EMISSIVITY_4_BAND_NAME,
            MODIS_L1B_EMISSIVITY_5_BAND_NAME,
            MODIS_L1B_EMISSIVITY_6_BAND_NAME,
            MODIS_L1B_EMISSIVITY_7_BAND_NAME,
            MODIS_L1B_EMISSIVITY_8_BAND_NAME,
            MODIS_L1B_EMISSIVITY_9_BAND_NAME,
            MODIS_L1B_EMISSIVITY_10_BAND_NAME,
            MODIS_L1B_EMISSIVITY_11_BAND_NAME,
            MODIS_L1B_EMISSIVITY_12_BAND_NAME,
            MODIS_L1B_EMISSIVITY_13_BAND_NAME,
            MODIS_L1B_EMISSIVITY_14_BAND_NAME,
            MODIS_L1B_EMISSIVITY_15_BAND_NAME,
            MODIS_L1B_EMISSIVITY_16_BAND_NAME,
    };

    public static final int MODIS_L1B_NUM_EMISSIVE_BANDS = MODIS_L1B_EMISSIVE_BAND_NAMES.length;

}
