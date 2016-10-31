package org.esa.s3tbx.idepix.algorithms.probav;

import org.esa.s3tbx.idepix.core.IdepixConstants;

/**
 * Constants for Idepix Proba-V algorithm
 *
 * @author olafd
 */
public class ProbaVConstants {
    public static final int IDEPIX_WATER = IdepixConstants.NUM_DEFAULT_FLAGS + 1;
    public static final int IDEPIX_CLEAR_LAND = IdepixConstants.NUM_DEFAULT_FLAGS + 2;
    public static final int IDEPIX_CLEAR_WATER = IdepixConstants.NUM_DEFAULT_FLAGS + 3;

    public static final String IDEPIX_CLEAR_LAND_DESCR_TEXT = "Clear land pixels";
    public static final String IDEPIX_CLEAR_WATER_DESCR_TEXT = "Clear water pixels";
    public static final String IDEPIX_WATER_DESCR_TEXT = "Water pixels";
}
