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

    public final static  int MERIS_NUM_SPECTRAL_BANDS = EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS;

    public final static String[] MERIS_RAD_BAND_NAMES = EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES;

    public final static String[] MERIS_REFL_BAND_NAMES = new String[]{
            "reflectance_1", "reflectance_2", "reflectance_3", "reflectance_4", "reflectance_5",
            "reflectance_6", "reflectance_7", "reflectance_8", "reflectance_9", "reflectance_10",
            "reflectance_11", "reflectance_12", "reflectance_13", "reflectance_14", "reflectance_15"
    };

    public final static String[] MERIS_SZA_BAND_NAMES = {"sun_zenith"};

    public static final String MERIS_AUTOGROUPING_RAD_STRING = "radiance";
    public static final String MERIS_AUTOGROUPING_REFL_STRING = "reflectance";

    public final static float[] OLCI_WAVELENGHTS = {
            400.0f, 412.5f, 442.5f, 490.0f, 510.0f,
            560.0f, 620.0f, 665.0f, 673.75f, 681.25f,
            708.75f, 753.75f, 761.25f, 764.375f, 767.5f,
            778.75f, 865.0f, 885.0f, 900.0f, 940.0f, 1020.0f
    };

    public final static  int OLCI_NUM_SPECTRAL_BANDS = OLCI_WAVELENGHTS.length;

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

    public static final String OLCI_AUTOGROUPING_RAD_STRING = "Oa*_radiance";
    public static final String OLCI_AUTOGROUPING_REFL_STRING = "Oa*_reflectance";

    public final static String[] SLSTR_RAD_BAND_NAMES = new String[]{
            "S1_radiance-an", "S2_radiance-an", "S3_radiance-an", "S4_radiance-an", "S5_radiance-an", "S6_radiance-an",
            "S1_radiance-ao", "S2_radiance-ao", "S3_radiance-ao", "S4_radiance-ao", "S5_radiance-ao", "S6_radiance-ao",
            "S4_radiance-bn", "S5_radiance-bn", "S6_radiance-bn",
            "S4_radiance-bo", "S5_radiance-bo", "S6_radiance-bo",
            "S4_radiance-cn", "S5_radiance-cn", "S6_radiance-cn",
            "S4_radiance-co", "S5_radiance-co", "S6_radiance-co"
    };

    public final static  int SLSTR_NUM_SPECTRAL_BANDS = SLSTR_RAD_BAND_NAMES.length;

    public final static String[] SLSTR_REFL_BAND_NAMES = new String[]{
            "S1_reflectance-an", "S2_reflectance-an", "S3_reflectance-an", "S4_reflectance-an", "S5_reflectance-an", "S6_reflectance-an",
            "S1_reflectance-ao", "S2_reflectance-ao", "S3_reflectance-ao", "S4_reflectance-ao", "S5_reflectance-ao", "S6_reflectance-ao",
            "S4_reflectance-bn", "S5_reflectance-bn", "S6_reflectance-bn",
            "S4_reflectance-bo", "S5_reflectance-bo", "S6_reflectance-bo",
            "S4_reflectance-cn", "S5_reflectance-cn", "S6_reflectance-cn",
            "S4_reflectance-co", "S5_reflectance-co", "S6_reflectance-co"
    };

    public final static String[] SLSTR_SOLAR_FLUX_BAND_NAMES = null;  // todo: if available, clarify band names in latest test products
    public final static String[] SLSTR_SZA_BAND_NAMES = {"solar_zenith_tn", "solar_zenith_to"};

    public static final String SLSTR_AUTOGROUPING_RAD_STRING = "radiance";
    public static final String SLSTR_AUTOGROUPING_REFL_STRING = "reflectance";

}
