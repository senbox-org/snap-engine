package org.esa.s3tbx.idepix.algorithms.landsat8;

import org.esa.s3tbx.idepix.core.IdepixConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Landsat 8 constants
 *
 * @author olafd
 */
public class Landsat8Constants {

    // additional flags after the 16 default ones
    public static final int F_CLOUD_SHIMEZ = IdepixConstants.NUM_DEFAULT_FLAGS + 1;
    public static final int F_CLOUD_SHIMEZ_BUFFER = IdepixConstants.NUM_DEFAULT_FLAGS + 2;
    public static final int F_CLOUD_HOT = IdepixConstants.NUM_DEFAULT_FLAGS + 3;
    public static final int F_CLOUD_HOT_BUFFER = IdepixConstants.NUM_DEFAULT_FLAGS + 4;
    public static final int F_CLOUD_OTSU = IdepixConstants.NUM_DEFAULT_FLAGS + 5;
    public static final int F_CLOUD_OTSU_BUFFER = IdepixConstants.NUM_DEFAULT_FLAGS + 6;
    public static final int F_CLOUD_CLOST = IdepixConstants.NUM_DEFAULT_FLAGS + 7;
    public static final int F_CLOUD_CLOST_BUFFER =IdepixConstants.NUM_DEFAULT_FLAGS + 8;

    public static final String F_CLOUD_BUFFER_SHIMEZ_DESCR_TEXT =
            "A buffer of n pixels around a cloud. Applied to pixels classified as 'cloud' with SHIMEZ test";
    public static final String F_CLOUD_BUFFER_HOT_DESCR_TEXT =
            "A buffer of n pixels around a cloud. Applied to pixels classified as 'cloud' with HOT test";
    public static final String F_CLOUD_BUFFER_OTSU_DESCR_TEXT =
            "A buffer of n pixels around a cloud. Applied to pixels classified as 'cloud' with OTSU test";
    public static final String F_CLOUD_BUFFER_CLOST_DESCR_TEXT =
            "A buffer of n pixels around a cloud. Applied to pixels classified as 'cloud' with CLOST test";

    public static final String Landsat8_FLAGS_NAME = "flags";

    public static final Map<Integer, Integer> LANDSAT8_SPECTRAL_WAVELENGTH_MAP;
    static
    {
        LANDSAT8_SPECTRAL_WAVELENGTH_MAP = new HashMap<>();
        LANDSAT8_SPECTRAL_WAVELENGTH_MAP.put(440, 0);
        LANDSAT8_SPECTRAL_WAVELENGTH_MAP.put(480, 1);
        LANDSAT8_SPECTRAL_WAVELENGTH_MAP.put(560, 2);
        LANDSAT8_SPECTRAL_WAVELENGTH_MAP.put(655, 3);
        LANDSAT8_SPECTRAL_WAVELENGTH_MAP.put(865, 4);
        LANDSAT8_SPECTRAL_WAVELENGTH_MAP.put(1610, 5);
        LANDSAT8_SPECTRAL_WAVELENGTH_MAP.put(2200, 6);
        LANDSAT8_SPECTRAL_WAVELENGTH_MAP.put(590, 7);
        LANDSAT8_SPECTRAL_WAVELENGTH_MAP.put(1370, 8);
        LANDSAT8_SPECTRAL_WAVELENGTH_MAP.put(10895, 9);
        LANDSAT8_SPECTRAL_WAVELENGTH_MAP.put(12005, 10);
    }

    public static final String LANDSAT8_COASTAL_AEROSOL_BAND_NAME = "coastal_aerosol";
    public static final String LANDSAT8_BLUE_BAND_NAME = "blue";
    public static final String LANDSAT8_GREEN_BAND_NAME = "green";
    public static final String LANDSAT8_RED_BAND_NAME = "red";
    public static final String LANDSAT8_NEAR_INFRARED_BAND_NAME = "near_infrared";
    public static final String LANDSAT8_SWIR1_BAND_NAME = "swir_1";
    public static final String LANDSAT8_SWIR2_BAND_NAME = "swir_2";
    public static final String LANDSAT8_PANCHROMATIC_BAND_NAME = "panchromatic";
    public static final String LANDSAT8_CIRRUS_BAND_NAME = "cirrus";
    public static final String LANDSAT8_THERMAL_INFRARED_TIRS_1_BAND_NAME = "thermal_infrared_(tirs)_1";
    public static final String LANDSAT8_THERMAL_INFRARED_TIRS_2_BAND_NAME = "thermal_infrared_(tirs)_2";

    public static final String[] LANDSAT8_SPECTRAL_BAND_NAMES = {
            LANDSAT8_COASTAL_AEROSOL_BAND_NAME,           // 0  (440nm)
            LANDSAT8_BLUE_BAND_NAME,                      // 1  (480nm)
            LANDSAT8_GREEN_BAND_NAME,                     // 2  (560nm)
            LANDSAT8_RED_BAND_NAME,                       // 3  (655nm)
            LANDSAT8_NEAR_INFRARED_BAND_NAME,             // 4  (865nm)
            LANDSAT8_SWIR1_BAND_NAME,                     // 5  (1610nm)
            LANDSAT8_SWIR2_BAND_NAME,                     // 6  (2200nm)
            LANDSAT8_PANCHROMATIC_BAND_NAME,              // 7  (590nm)
            LANDSAT8_CIRRUS_BAND_NAME,                    // 8  (1370nm)
            LANDSAT8_THERMAL_INFRARED_TIRS_1_BAND_NAME,   // 9  (10895nm)
            LANDSAT8_THERMAL_INFRARED_TIRS_2_BAND_NAME,   // 10 (12005nm)
    };
    public static final int LANDSAT8_NUM_SPECTRAL_BANDS = LANDSAT8_SPECTRAL_BAND_NAMES.length;

    private Landsat8Constants() {
    }
}
