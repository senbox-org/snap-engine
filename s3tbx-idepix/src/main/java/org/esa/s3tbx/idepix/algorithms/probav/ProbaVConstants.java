package org.esa.s3tbx.idepix.algorithms.probav;

import org.esa.s3tbx.idepix.core.IdepixConstants;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 31.08.2016
 * Time: 15:18
 *
 * @author olafd
 */
public class ProbaVConstants {
    static final int LAND_WATER_MASK_RESOLUTION = 50;
    static final int OVERSAMPLING_FACTOR_X = 3;
    static final int OVERSAMPLING_FACTOR_Y = 3;

    /* Idepix Flags Positions */
    static final int F_INVALID = IdepixConstants.F_INVALID;
    static final int F_CLOUD = IdepixConstants.F_CLOUD;
    static final int F_CLOUD_AMBIGUOUS = IdepixConstants.F_CLOUD_AMBIGUOUS;
    static final int F_CLOUD_SURE = IdepixConstants.F_CLOUD_SURE;
    static final int F_CLOUD_BUFFER = IdepixConstants.F_CLOUD_BUFFER;
    static final int F_SNOW_ICE = 5;
    static final int F_LAND = 6;

    static final String F_INVALID_DESCR_TEXT = "Invalid pixels";
    static final String F_CLOUD_DESCR_TEXT = "Pixels which are either cloud_sure or cloud_ambiguous";
    static final String F_CLOUD_AMBIGUOUS_DESCR_TEXT = "Semi transparent clouds, or clouds where the detection level is uncertain";
    static final String F_CLOUD_SURE_DESCR_TEXT = "Fully opaque clouds with full confidence of their detection";
    static final String F_CLOUD_BUFFER_DESCR_TEXT = "A buffer of n pixels around a cloud. n is a user supplied parameter. Applied to pixels masked as 'cloud'";
    static final String F_SNOW_ICE_DESCR_TEXT = "Snow/ice pixels";
    static final String F_LAND_DESCR_TEXT = "Land pixels";

    static final String SCHILLER_NN_OUTPUT_BAND_NAME = "nn_value";
}
