package org.esa.s3tbx.idepix.algorithms.meris;

import org.esa.s3tbx.idepix.core.IdepixConstants;

/**
 * Constants for Idepix MERIS algorithm
 *
 * @author olafd
 */
public class MerisConstants {

    public static final int IDEPIX_GLINT_RISK = IdepixConstants.NUM_DEFAULT_FLAGS + 1;

    public static final String IDEPIX_GLINT_RISK_DESCR_TEXT = "Glint risk pixel";

    /* Level 1 Flags Positions */
    static final int L1_F_LAND = 4;
    static final int L1_F_INVALID = 7;
}
