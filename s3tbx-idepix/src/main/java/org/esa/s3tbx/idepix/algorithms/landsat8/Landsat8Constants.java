package org.esa.s3tbx.idepix.algorithms.landsat8;

import org.esa.s3tbx.idepix.core.util.IdepixUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Landsat 8 constants
 *
 * @author olafd
 */
public class Landsat8Constants {

    public static final int F_INVALID = 0;
    public static final int F_CLOUD_SHIMEZ = 1;
    public static final int F_CLOUD_SHIMEZ_BUFFER = 2;
    public static final int F_CLOUD_HOT = 3;
    public static final int F_CLOUD_HOT_BUFFER = 4;
    public static final int F_CLOUD_OTSU = 5;
    public static final int F_CLOUD_OTSU_BUFFER = 6;
    public static final int F_CLOUD_CLOST = 7;
    public static final int F_CLOUD_CLOST_BUFFER = 8;
    public static final int F_CLOUD_AMBIGUOUS = 9;
    public static final int F_CLOUD_SURE = 10;
    public static final int F_CLOUD_SHADOW = 11;
    public static final int F_BRIGHT = 12;
    public static final int F_WHITE = 13;
    public static final int F_SNOW_ICE = 14;
    public static final int F_GLINTRISK = 15;
    public static final int F_COASTLINE = 16;
    public static final int F_LAND = 17;

    public static final String F_INVALID_DESCR_TEXT = "Invalid pixel";
    public static final String F_CLOUD_DESCR_TEXT = "Cloudy pixel";
    public static final String F_CLOUD_BUFFER_DESCR_TEXT = "A buffer of n pixels around a cloud. n is a user supplied parameter. Applied to pixels masked as 'cloud'";
    public static final String F_CLOUD_AMBIGUOUS_DESCR_TEXT = IdepixUtils.F_CLOUD_AMBIGUOUS_DESCR_TEXT;
    public static final String F_CLOUD_SURE_DESCR_TEXT = IdepixUtils.F_CLOUD_SURE_DESCR_TEXT;
    public static final String F_SNOW_ICE_DESCR_TEXT = "Snow/Ice pixel";
    public static final String F_BRIGHT_DESCR_TEXT = "Bright pixel";
    public static final String F_WHITE_DESCR_TEXT = "White pixel";
    public static final String F_COASTLINE_DESCR_TEXT = "Pixel at a coastline";
    public static final String F_LAND_DESCR_TEXT = "Land pixel";

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
