package org.esa.s3tbx.idepix.algorithms.meris;

/**
 * Constants for Idepix MERIS algorithm
 *
 * @author olafd
 */
public class MerisConstants {

    /* Level 1 Flags Positions */
//    public static final int L1_F_COSMETIC = 0;
//    public static final int L1_F_DUPLICATED = 1;
//    public static final int L1_F_GLINTRISK = 2;
//    public static final int L1_F_SUSPECT = 3;
    public static final int L1_F_LAND = 4;
//    public static final int L1_F_BRIGHT = 5;
//    public static final int L1_F_COAST = 6;
    public static final int L1_F_INVALID = 7;

    /* Idepix Flags Positions */
    public static final int F_INVALID = 0;
    public static final int F_CLOUD = 1;
    public static final int F_CLOUD_AMBIGUOUS = 2;
    public static final int F_CLOUD_SURE = 3;
    public static final int F_CLOUD_BUFFER = 4;
    public static final int F_CLOUD_SHADOW = 5;
    public static final int F_SNOW_ICE = 6;
    public static final int F_GLINTRISK = 7;
    public static final int F_COASTLINE = 8;
    public static final int F_LAND = 9;

    public static final String F_INVALID_DESCR_TEXT = "Invalid pixels";
    public static final String F_CLOUD_DESCR_TEXT = "Pixels which are either cloud_sure or cloud_ambiguous";
    public static final String F_CLOUD_AMBIGUOUS_DESCR_TEXT = "Semi transparent clouds, or clouds where the detection level is uncertain";
    public static final String F_CLOUD_SURE_DESCR_TEXT = "Fully opaque clouds with full confidence of their detection";
    public static final String F_CLOUD_BUFFER_DESCR_TEXT = "A buffer of n pixels around a cloud. n is a user supplied parameter. Applied to pixels masked as 'cloud'";
    public static final String F_CLOUD_SHADOW_DESCR_TEXT = "Pixels is affect by a cloud shadow";
    public static final String F_SNOW_ICE_DESCR_TEXT = "Snow/ice pixels";
    public static final String F_GLINTRISK_DESCR_TEXT = "Pixels with glint risk";
    public static final String F_COASTLINE_DESCR_TEXT = "Pixels at a coastline";
    public static final String F_LAND_DESCR_TEXT = "Land pixels";

    public static final String SCHILLER_NN_OUTPUT_BAND_NAME = "nn_value";
}
