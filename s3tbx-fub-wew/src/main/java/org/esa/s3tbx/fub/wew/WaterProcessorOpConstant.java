package org.esa.s3tbx.fub.wew;

import org.esa.snap.dataio.envisat.EnvisatConstants;

/**
 * @author muhammad.bc.
 */
class WaterProcessorOpConstant {

    public static final String[] OUTPUT_CONCENTRATION_BAND_NAMES = {
            "algal_2",
            "yellow_subs",
            "total_susp"
    };
    public static final String[] OUTPUT_OPTICAL_DEPTH_BAND_NAMES = {
            "aero_opt_thick_440",
            "aero_opt_thick_550",
            "aero_opt_thick_670",
            "aero_opt_thick_870"
    };
    public static final String[] OUTPUT_REFLECTANCE_BAND_NAMES = {
            "reflec_1",
            "reflec_2",
            "reflec_3",
            "reflec_4",
            "reflec_5",
            "reflec_6",
            "reflec_7",
            "reflec_9"
    };
    public static final int[] RESULT_ERROR_VALUES = {
            0x00000001,
            0x00000002,
            0x00000004,
            0x00000008,
            0x00000010,
            0x00000020,
            0x00000040,
            0x00000080,
            0x00000100
    };
    public static final String[] SOURCE_RASTER_NAMES_MERIS = new String[]{
            EnvisatConstants.MERIS_L1B_RADIANCE_1_BAND_NAME,  // source sample index  0   radiance_1
            EnvisatConstants.MERIS_L1B_RADIANCE_2_BAND_NAME,  // source sample index  1   radiance_2
            EnvisatConstants.MERIS_L1B_RADIANCE_3_BAND_NAME,  // source sample index  2   radiance_3
            EnvisatConstants.MERIS_L1B_RADIANCE_4_BAND_NAME,  // source sample index  3   radiance_4
            EnvisatConstants.MERIS_L1B_RADIANCE_5_BAND_NAME,  // source sample index  4   radiance_5
            EnvisatConstants.MERIS_L1B_RADIANCE_6_BAND_NAME,  // source sample index  5   radiance_6
            EnvisatConstants.MERIS_L1B_RADIANCE_7_BAND_NAME,  // source sample index  6   radiance_7
            EnvisatConstants.MERIS_L1B_RADIANCE_8_BAND_NAME,  // source sample index  7   radiance_8
            EnvisatConstants.MERIS_L1B_RADIANCE_9_BAND_NAME,  // source sample index  8   radiance_9
            EnvisatConstants.MERIS_L1B_RADIANCE_10_BAND_NAME, // source sample index  9   radiance_10
            EnvisatConstants.MERIS_L1B_RADIANCE_11_BAND_NAME, // source sample index 10   radiance_11
            EnvisatConstants.MERIS_L1B_RADIANCE_12_BAND_NAME, // source sample index 11   radiance_12
            EnvisatConstants.MERIS_L1B_RADIANCE_13_BAND_NAME, // source sample index 12   radiance_13
            EnvisatConstants.MERIS_L1B_RADIANCE_14_BAND_NAME, // source sample index 13   radiance_14
            EnvisatConstants.MERIS_L1B_RADIANCE_15_BAND_NAME, // source sample index 14   radiance_15
            EnvisatConstants.MERIS_L1B_FLAGS_DS_NAME,         // source sample index 15   l1_flags
            EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[6],   // source sample index 16   sun_zenith
            EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[7],   // source sample index 17   sun_azimuth
            EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[8],   // source sample index 18   view_zenith
            EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[9],   // source sample index 19   view_azimuth
            EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[10],  // source sample index 20   zonal_wind
            EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[11],  // source sample index 21   merid_wind
            EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[12],  // source sample index 22   atm_press
            EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[13]   // source sample index 23   ozone
    };

    public static final String[] SOURCE_RASTER_NAMES_OLCI = new String[]{
            "Oa02_radiance",
            "Oa03_radiance",
            "Oa04_radiance",
            "Oa05_radiance",
            "Oa06_radiance",
            "Oa07_radiance",
            "Oa08_radiance",
            "Oa10_radiance",
            "Oa11_radiance",
            "Oa12_radiance",
            "Oa13_radiance",
            "Oa16_radiance",
            "Oa17_radiance",
            "Oa18_radiance",
            "Oa19_radiance",
            "quality_flags",
            "SZA",
            "SAA",
            "OZA",
            "OAA",
            "horizontal_wind_vector_1",
            "horizontal_wind_vector_2",
            "sea_level_pressure",
            "total_ozone"
    };

    public static final int SOURCE_SAMPLE_INDEX_SUN_ZENITH = 16;
    public static final int SOURCE_SAMPLE_INDEX_SUN_AZIMUTH = 17;
    public static final int SOURCE_SAMPLE_INDEX_VIEW_ZENITH = 18;
    public static final int SOURCE_SAMPLE_INDEX_VIEW_AZIMUTH = 19;
    public static final int SOURCE_SAMPLE_INDEX_ZONAL_WIND = 20;
    public static final int SOURCE_SAMPLE_INDEX_MERID_WIND = 21;
    public static final int SOURCE_SAMPLE_INDEX_ATM_PRESS = 22;
    public static final int SOURCE_SAMPLE_INDEX_OZONE = 23;
    static final String SUSPECT_FLAG_NAME = "l1_flags.SUSPECT";
    static final String SUSPECT_EXPRESSION_TERM = "and not " + SUSPECT_FLAG_NAME;
    static final String result_flags_name = "result_flags";
    // Mask value to be written if inversion fails
    static final float RESULT_MASK_VALUE = 5.0f;
    static final String[] RESULT_ERROR_TEXTS = {
            "Pixel was a priori masked out",
            "CHL retrieval failure (input)",
            "CHL retrieval failure (output)",
            "YEL retrieval failure (input)",
            "YEL retrieval failure (output)",
            "TSM retrieval failure (input)",
            "TSM retrieval failure (output)",
            "Atmospheric correction failure (input)",
            "Atmospheric correction failure (output)"
    };
    static final String[] RESULT_ERROR_NAMES = {
            "LEVEL1b_MASKED",
            "CHL_IN",
            "CHL_OUT",
            "YEL_IN",
            "YEL_OUT",
            "TSM_IN",
            "TSM_OUT",
            "ATM_IN",
            "ATM_OUT"
    };
    static final float[] RESULT_ERROR_TRANSPARENCIES = {
            0.0f,
            0.5f,
            0.5f,
            0.5f,
            0.5f,
            0.5f,
            0.5f,
            0.5f,
            0.5f
    };
    public static String[] OUTPUT_CONCENTRATION_BAND_DESCRIPTIONS = {
            "Chlorophyll 2 content",
            "Yellow substance",
            "Total suspended matter"
    };
    static String[] OUTPUT_OPTICAL_DEPTH_BAND_DESCRIPTIONS = {
            "Aerosol optical thickness",
            "Aerosol optical thickness",
            "Aerosol optical thickness",
            "Aerosol optical thickness"
    };
    static String[] OUTPUT_REFLECTANCE_BAND_DESCRIPTIONS = {
            "RS reflectance",
            "RS reflectance",
            "RS reflectance",
            "RS reflectance",
            "RS reflectance",
            "RS reflectance",
            "RS reflectance",
            "RS reflectance"
    };
    static String[] OUTPUT_CONCENTRATION_BAND_UNITS = {
            "log10(mg/m^3)",
            "log10(1/m)",
            "log10(g/m^3)"
    };
    static String[] OUTPUT_OPTICAL_DEPTH_BAND_UNITS = {
            "1",
            "1",
            "1",
            "1"
    };
    static String[] OUTPUT_REFLECTANCE_BAND_UNITS = {
            "1/sr",
            "1/sr",
            "1/sr",
            "1/sr",
            "1/sr",
            "1/sr",
            "1/sr",
            "1/sr"
    };
    // Wavelengths for the water leaving reflectances rho_w
    static float[] TAU_LAMBDA = {
            440.00f, 550.00f, 670.00f, 870.00f
    };
    // Wavelengths for the water leaving reflectances rho_w
    static float[] RHO_W_LAMBDA = {
            412.50f, 442.50f, 490.00f, 510.00f,
            560.00f, 620.00f, 665.00f, 708.75f
    };
    // Bandwidth for the water leaving reflectances rho_w
    static float[] RHO_W_BANDW = {
            10.00f, 10.00f, 10.00f, 10.00f,
            10.00f, 10.00f, 10.00f, 10.00f
    };
}
