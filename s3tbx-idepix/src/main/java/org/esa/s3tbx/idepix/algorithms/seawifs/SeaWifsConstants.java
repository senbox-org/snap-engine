package org.esa.s3tbx.idepix.algorithms.seawifs;

import org.esa.s3tbx.idepix.core.IdepixConstants;

/**
 * Constants for Idepix SeaWiFS algorithm
 *
 * @author olafd
 */
public class SeaWifsConstants {

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

    public static final int IDEPIX_MIXED_PIXEL = IdepixConstants.NUM_DEFAULT_FLAGS + 1;

    public static final String IDEPIX_MIXED_PIXEL_DESCR_TEXT = "Mixed pixel";
}
