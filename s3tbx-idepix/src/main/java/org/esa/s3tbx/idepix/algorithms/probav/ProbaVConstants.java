package org.esa.s3tbx.idepix.algorithms.probav;

/**
 * Constants for Idepix Proba-V algorithm
 *
 * @author olafd
 */
public class ProbaVConstants {
    static final int LAND_WATER_MASK_RESOLUTION = 50;
    static final int OVERSAMPLING_FACTOR_X = 3;
    static final int OVERSAMPLING_FACTOR_Y = 3;

    static final String F_INVALID_DESCR_TEXT = "Invalid pixels";
    static final String F_CLOUD_DESCR_TEXT = "Pixels which are either cloud_sure or cloud_ambiguous";
    static final String F_CLOUD_AMBIGUOUS_DESCR_TEXT = "Semi transparent clouds, or clouds where the detection level is uncertain";
    static final String F_CLOUD_SURE_DESCR_TEXT = "Fully opaque clouds with full confidence of their detection";
    static final String F_CLOUD_BUFFER_DESCR_TEXT = "A buffer of n pixels around a cloud. n is a user supplied parameter. Applied to pixels masked as 'cloud'";
    static final String F_CLOUD_SHADOW_DESCR_TEXT = "Cloud shadow pixels";
    static final String F_SNOW_ICE_DESCR_TEXT = "Snow/ice pixels";
    static final String F_LAND_DESCR_TEXT = "Land pixels";
}
