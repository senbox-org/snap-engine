package org.esa.snap.core.datamodel;

public class RgbDefaults {

    private static final String PROPERTY_ROOT_KEY = "rgb.image";

//    public static final String PROPERTY_RGB_OPTIONS_SECTION_KEY = PROPERTY_ROOT_KEY + ".section";
//    public static final String PROPERTY_RGB_OPTIONS_SECTION_LABEL = "RGB Options";
//    public static final String PROPERTY_RGB_OPTIONS_SECTION_TOOLTIP = "Options for the RGB Image";

    public static final String PROPERTY_RGB_OPTIONS_MIN_MAX_RANGE_KEY = PROPERTY_ROOT_KEY + ".button.use.min.max.range";
    public static final String PROPERTY_RGB_OPTIONS_MIN_MAX_RANGE_LABEL = "Use RGB Range";
    public static final String PROPERTY_RGB_OPTIONS_MIN_MAX_RANGE_TOOLTIP = "Set default RGB range with the min and max  below (otherwise use statistics of the bands as default)";
    public static boolean PROPERTY_RGB_OPTIONS_MIN_MAX_RANGE_DEFAULT = false;

    public static final String PROPERTY_RGB_OPTIONS_MIN_KEY = PROPERTY_ROOT_KEY + ".button.min";
    public static final String PROPERTY_RGB_OPTIONS_MIN_LABEL = "Range Button (Min)";
    public static final String PROPERTY_RGB_OPTIONS_MIN_TOOLTIP = "The min value to use in the RGB (A..B) range button";
    public static double PROPERTY_RGB_OPTIONS_MIN_DEFAULT = 0.0;

    public static final String PROPERTY_RGB_OPTIONS_MAX_KEY = PROPERTY_ROOT_KEY + ".button.max";
    public static final String PROPERTY_RGB_OPTIONS_MAX_LABEL = "Range Button (Max)";
    public static final String PROPERTY_RGB_OPTIONS_MAX_TOOLTIP = "The max value to use in the RGB (A..B) range button";
    public static double PROPERTY_RGB_OPTIONS_MAX_DEFAULT = 1.0;



    // Restore to defaults


    private static final String PROPERTY_RESTORE_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".restore.defaults";

    public static final String PROPERTY_RESTORE_SECTION_KEY = PROPERTY_RESTORE_KEY_SUFFIX + ".section";
    public static final String PROPERTY_RESTORE_SECTION_LABEL = "Restore";
    public static final String PROPERTY_RESTORE_SECTION_TOOLTIP = "Restores preferences to the package defaults";

    public static final String PROPERTY_RESTORE_DEFAULTS_NAME = PROPERTY_RESTORE_KEY_SUFFIX + ".apply";
    public static final String PROPERTY_RESTORE_DEFAULTS_LABEL = "Default (RGB Image Preferences)";
    public static final String PROPERTY_RESTORE_DEFAULTS_TOOLTIP = "Restore all RGB Image preferences to the original default";
    public static final boolean PROPERTY_RESTORE_DEFAULTS_DEFAULT = false;

}
