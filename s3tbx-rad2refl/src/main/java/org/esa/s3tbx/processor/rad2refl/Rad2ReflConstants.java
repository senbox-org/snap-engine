package org.esa.s3tbx.processor.rad2refl;

import org.esa.snap.dataio.envisat.EnvisatConstants;

/**
 * Constants for for Radiance/reflectancetance conversion.
 *
 * @author olafd
 */
public class Rad2ReflConstants {

    public final static String RAD_UNIT = "mw.m-2.sr-1.nm-1";
    public final static String REFL_UNIT = "dl";

    public final static int MERIS_NUM_SPECTRAL_BANDS = EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS;

    public final static String[] MERIS_RAD_BAND_NAMES = EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES;

    public final static String[] MERIS_REFL_BAND_NAMES = new String[]{
            "reflectance_1", "reflectance_2", "reflectance_3", "reflectance_4", "reflectance_5",
            "reflectance_6", "reflectance_7", "reflectance_8", "reflectance_9", "reflectance_10",
            "reflectance_11", "reflectance_12", "reflectance_13", "reflectance_14", "reflectance_15"
    };

    public final static String[] MERIS_SZA_BAND_NAMES = {"sun_zenith"};

    public static final String MERIS_AUTOGROUPING_RAD_STRING = "radiance";
    public static final String MERIS_AUTOGROUPING_REFL_STRING = "reflectance";

    public static final String MERIS_INVALID_PIXEL_EXPR = "l1_flags.INVALID";

    public final static float[] MERIS_SOLAR_FLUXES_DEFAULT = {
            1714.9084f, //  0
            1872.3961f, //  1
            1926.6102f, //  2
            1930.2483f, //  3
            1804.2762f, //  4
            1651.5836f, //  5
            1531.4067f, //  6
            1475.615f, //  7
            1408.9949f, //  8
            1265.5425f, //  9
            1255.4227f, // 10
            1178.0286f, // 11
            955.07043f, // 12
            914.18945f, // 13
            882.8275f   // 14
    };

    public final static float[] OLCI_WAVELENGHTS = {
            400.0f, 412.5f, 442.5f, 490.0f, 510.0f,
            560.0f, 620.0f, 665.0f, 673.75f, 681.25f,
            708.75f, 753.75f, 761.25f, 764.375f, 767.5f,
            778.75f, 865.0f, 885.0f, 900.0f, 940.0f, 1020.0f
    };

    public final static int[] OLCI_MERIS_EQUIVALENT_WVL_INDICES = {
           1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 15, 16, 17, 18
    };

    public final static int OLCI_NUM_SPECTRAL_BANDS = OLCI_WAVELENGHTS.length;

    public final static String[] OLCI_RAD_BAND_NAMES = new String[]{
            "Oa01_radiance", "Oa02_radiance", "Oa03_radiance", "Oa04_radiance", "Oa05_radiance",
            "Oa06_radiance", "Oa07_radiance", "Oa08_radiance", "Oa09_radiance", "Oa10_radiance",
            "Oa11_radiance", "Oa12_radiance", "Oa13_radiance", "Oa14_radiance", "Oa15_radiance",
            "Oa16_radiance", "Oa17_radiance", "Oa18_radiance", "Oa19_radiance", "Oa20_radiance", "Oa21_radiance"
    };

    public final static String[] OLCI_REFL_BAND_NAMES = new String[]{
            "Oa01_reflectance", "Oa02_reflectance", "Oa03_reflectance", "Oa04_reflectance", "Oa05_reflectance",
            "Oa06_reflectance", "Oa07_reflectance", "Oa08_reflectance", "Oa09_reflectance", "Oa10_reflectance",
            "Oa11_reflectance", "Oa12_reflectance", "Oa13_reflectance", "Oa14_reflectance", "Oa15_reflectance",
            "Oa16_reflectance", "Oa17_reflectance", "Oa18_reflectance", "Oa19_reflectance", "Oa20_reflectance", "Oa21_reflectance"
    };

    public final static String[] OLCI_SOLAR_FLUX_BAND_NAMES = new String[]{
            "solar_flux_band_1", "solar_flux_band_2", "solar_flux_band_3", "solar_flux_band_4", "solar_flux_band_5",
            "solar_flux_band_6", "solar_flux_band_7", "solar_flux_band_8", "solar_flux_band_9", "solar_flux_band_10",
            "solar_flux_band_11", "solar_flux_band_12", "solar_flux_band_13", "solar_flux_band_14", "solar_flux_band_15",
            "solar_flux_band_16", "solar_flux_band_17", "solar_flux_band_18", "solar_flux_band_19", "solar_flux_band_20",
            "solar_flux_band_21"
    };

    public final static String[] OLCI_SZA_BAND_NAMES = {"SZA"};

    public static final String OLCI_AUTOGROUPING_RAD_STRING = "Oa*_radiance:solar_flux";
    public static final String OLCI_AUTOGROUPING_REFL_STRING = "Oa*_reflectance:solar_flux";

    public static final String OLCI_INVALID_PIXEL_EXPR = "quality_flags.invalid";

    public final static float[] OLCI_SOLAR_FLUXES_DEFAULT = {
            1714.9084f, //  0
            1872.3961f, //  1
            1926.6102f, //  2
            1930.2483f, //  3
            1804.2762f, //  4
            1651.5836f, //  5
            1531.4067f, //  6
            1475.615f,  //  7
            1408.9949f, //  8
            1265.5425f, //  9
            1255.4227f, // 10
            1178.0286f, // 11
            955.07043f, // 12
            914.18945f, // 13
            882.8275f,  // 14
            882.8275f,  // 15
            882.8275f,  // 16
            882.8275f,  // 17
            882.8275f,  // 18
            882.8275f,  // 19
            882.8275f   // 20
    };

    public final static String[] SLSTR_RAD_BAND_NAMES = new String[]{
            "S1_radiance_an", "S2_radiance_an", "S3_radiance_an", "S4_radiance_an", "S5_radiance_an", "S6_radiance_an",
            "S1_radiance_ao", "S2_radiance_ao", "S3_radiance_ao", "S4_radiance_ao", "S5_radiance_ao", "S6_radiance_ao",
            "S4_radiance_bn", "S5_radiance_bn", "S6_radiance_bn",
            "S4_radiance_bo", "S5_radiance_bo", "S6_radiance_bo",
            "S4_radiance_cn", "S5_radiance_cn", "S6_radiance_cn",
            "S4_radiance_co", "S5_radiance_co", "S6_radiance_co"
    };

    public final static int SLSTR_NUM_SPECTRAL_BANDS = SLSTR_RAD_BAND_NAMES.length;

    public final static String[] SLSTR_REFL_BAND_NAMES = new String[]{
            "S1_reflectance_an", "S2_reflectance_an", "S3_reflectance_an", "S4_reflectance_an", "S5_reflectance_an", "S6_reflectance_an",
            "S1_reflectance_ao", "S2_reflectance_ao", "S3_reflectance_ao", "S4_reflectance_ao", "S5_reflectance_ao", "S6_reflectance_ao",
            "S4_reflectance_bn", "S5_reflectance_bn", "S6_reflectance_bn",
            "S4_reflectance_bo", "S5_reflectance_bo", "S6_reflectance_bo",
            "S4_reflectance_cn", "S5_reflectance_cn", "S6_reflectance_cn",
            "S4_reflectance_co", "S5_reflectance_co", "S6_reflectance_co"
    };

    public final static String[] SLSTR_SOLAR_FLUX_BAND_NAMES = null;  // todo: if available, clarify band names in latest test products
    public final static String[] SLSTR_SZA_BAND_NAMES = {"solar_zenith_tn", "solar_zenith_to"};

    public static final String SLSTR_AUTOGROUPING_RAD_STRING =
            "radiance_an:radiance_ao:radiance_bn:radiance_bo:radiance_cn:radiance_co";
    public static final String SLSTR_AUTOGROUPING_REFL_STRING =
            "reflectance_an:reflectance_ao:reflectance_bn:reflectance_bo:reflectance_cn:reflectance_co";

    public static final String SLSTR_INVALID_PIXEL_EXPR = "1 == 0"; // todo: define

    public final static float[] SLSTR_SOLAR_FLUXES_DEFAULT = {
            1837.39f,
            1525.94f,
            956.17f,
            365.90f,
            248.33f,
            78.33f
    };

}
