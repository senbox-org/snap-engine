package org.esa.snap.core.datamodel;

import org.esa.snap.core.util.PropertyMap;


/**
 * Contains many key parameters and defaults for the color manipulation tool and it's related color scheme manager
 *
 * @author Daniel Knowles
 * @date 2/1/2020
 */

public class ColorSchemeDefaults {

    public static final boolean COLOR_SCHEME_CODE_DEBUG = true;

    public static final String TOOLNAME_COLOR_MANIPULATION = "Color Manipulation";

    // Directory names
    public static final String DIR_NAME_COLOR_SCHEMES = "color_schemes";
    public static final String DIR_NAME_COLOR_PALETTES = "color_palettes";
    public static final String DIR_NAME_AUX_DATA = "auxdata";

    // CPD files
    public static final String CPD_STANDARD_DEFAULT = "oceancolor_standard.cpd";
    public static final String CPD_UNIVERSAL_DEFAULT = "universal_bluered.cpd";
    public static final String CPD_GRAY_SCALE_DEFAULT = "gray_scale.cpd";
    public static final String CPD_ANOMALIES_DEFAULT = "gradient_red_white_blue.cpd";
    public static final String CPD_DEFAULT = CPD_GRAY_SCALE_DEFAULT;

    // xml files used by the color scheme manager
    public static final String COLOR_SCHEME_LUT_FILENAME = "color_palette_scheme_defaults.xml";
    public static final String COLOR_SCHEMES_FILENAME = "color_palette_schemes.xml";

    // Indicates which color palette contained within the color scheme xml to use
    public static final String OPTION_COLOR_STANDARD_SCHEME = "From Scheme STANDARD";
    public static final String OPTION_COLOR_UNIVERSAL_SCHEME = "From Scheme UNIVERSAL";

    // Color category selections
    public static final String OPTION_COLOR_GRAY_SCALE = "GRAY SCALE";
    public static final String OPTION_COLOR_STANDARD = "STANDARD";
    public static final String OPTION_COLOR_UNIVERSAL = "UNIVERSAL";
    public static final String OPTION_COLOR_ANOMALIES = "ANOMALIES";

    // Range options
    public static final String OPTION_RANGE_FROM_SCHEME = "From Scheme";
    public static final String OPTION_RANGE_FROM_DATA = "From Data";
    public static final String OPTION_RANGE_FROM_CPD = "From Cpd";

    // Log options
    public static final String OPTION_LOG_TRUE = "TRUE";
    public static final String OPTION_LOG_FALSE = "FALSE";
    public static final String OPTION_LOG_FROM_CPD = "From Cpd";
    public static final String OPTION_LOG_FROM_SCHEME = "From Scheme";

    // general usage values
    public static final double DOUBLE_NULL = -Double.MAX_VALUE;
    public static final String NULL_ENTRY = "null";


    //--------------------------------------------------------------------------------------------------------------
    // Color Manipulation Preferences parameters

    // Preferences property prefix
    private static final String PROPERTY_ROOT_KEY = "color.manipulation";





    // General (Non-Scheme) Options

    private static final String PROPERTY_GENERAL_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".general";

    public static final String PROPERTY_GENERAL_SECTION_KEY = PROPERTY_GENERAL_KEY_SUFFIX + ".section";
    public static final String PROPERTY_GENERAL_SECTION_LABEL = "General (Non-Scheme) Options";
    public static final String PROPERTY_GENERAL_SECTION_TOOLTIP = "General behavior when not using a color scheme";

    public static final String PROPERTY_GENERAL_CPD_KEY = PROPERTY_GENERAL_KEY_SUFFIX + ".cpd";
    public static final String PROPERTY_GENERAL_CPD_LABEL = "Palette";
    public static final String PROPERTY_GENERAL_CPD_TOOLTIP = "The cpd file to use when not using a color scheme";
    public static final String PROPERTY_GENERAL_CPD_OPTION1 = OPTION_COLOR_GRAY_SCALE;
    public static final String PROPERTY_GENERAL_CPD_OPTION2 = OPTION_COLOR_STANDARD;
    public static final String PROPERTY_GENERAL_CPD_OPTION3 = OPTION_COLOR_UNIVERSAL;
    public static final String PROPERTY_GENERAL_CPD_OPTION4 = OPTION_COLOR_ANOMALIES;
    public static final String PROPERTY_GENERAL_CPD_DEFAULT = OPTION_COLOR_GRAY_SCALE;

    public static final String PROPERTY_GENERAL_RANGE_KEY = PROPERTY_GENERAL_KEY_SUFFIX + ".range";
    public static final String PROPERTY_GENERAL_RANGE_LABEL = "Range";
    public static final String PROPERTY_GENERAL_RANGE_TOOLTIP = "range options to use when not using a color scheme";
    public static final String PROPERTY_GENERAL_RANGE_OPTION1 = OPTION_RANGE_FROM_DATA;
    public static final String PROPERTY_GENERAL_RANGE_OPTION2 = OPTION_RANGE_FROM_CPD;
    public static final String PROPERTY_GENERAL_RANGE_DEFAULT = OPTION_RANGE_FROM_DATA;

    public static final String PROPERTY_GENERAL_LOG_KEY = PROPERTY_GENERAL_KEY_SUFFIX + ".log";
    public static final String PROPERTY_GENERAL_LOG_LABEL = "Log Scaling";
    public static final String PROPERTY_GENERAL_LOG_TOOLTIP = "log options to use when not using a color scheme";
    public static final String PROPERTY_GENERAL_LOG_OPTION1 = OPTION_LOG_TRUE;
    public static final String PROPERTY_GENERAL_LOG_OPTION2 = OPTION_LOG_FALSE;
    public static final String PROPERTY_GENERAL_LOG_OPTION3 = OPTION_LOG_FROM_CPD;
    public static final String PROPERTY_GENERAL_LOG_DEFAULT = OPTION_LOG_FALSE;

    //--------------------------



    // Scheme option

    private static final String PROPERTY_SCHEME_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".scheme";

    public static final String PROPERTY_SCHEME_SECTION_KEY = PROPERTY_SCHEME_KEY_SUFFIX + ".section";
    public static final String PROPERTY_SCHEME_SECTION_LABEL = "Scheme Options";
    public static final String PROPERTY_SCHEME_SECTION_TOOLTIP = "Behavior when using a color scheme";

    public static final String PROPERTY_SCHEME_AUTO_APPLY_KEY = PROPERTY_SCHEME_KEY_SUFFIX + ".auto.apply";
    public static final String PROPERTY_SCHEME_AUTO_APPLY_LABEL = "Automatically Apply Color Schemes";
    public static final String PROPERTY_SCHEME_AUTO_APPLY_TOOLTIP = "Automatically apply color schemes when opening a band based on its name";
    public static final boolean PROPERTY_SCHEME_AUTO_APPLY_DEFAULT = true;

    public static final String PROPERTY_SCHEME_CPD_KEY = PROPERTY_SCHEME_KEY_SUFFIX + ".cpd";
    public static final String PROPERTY_SCHEME_CPD_LABEL = "Palette";
    public static final String PROPERTY_SCHEME_CPD_TOOLTIP = "The cpd file to use for the scheme";
    public static final String PROPERTY_SCHEME_CPD_OPTION1 = OPTION_COLOR_STANDARD_SCHEME;
    public static final String PROPERTY_SCHEME_CPD_OPTION2 = OPTION_COLOR_UNIVERSAL_SCHEME;
    public static final String PROPERTY_SCHEME_CPD_OPTION3 = OPTION_COLOR_GRAY_SCALE;
    public static final String PROPERTY_SCHEME_CPD_OPTION4 = OPTION_COLOR_STANDARD;
    public static final String PROPERTY_SCHEME_CPD_OPTION5 = OPTION_COLOR_UNIVERSAL;
    public static final String PROPERTY_SCHEME_CPD_OPTION6 = OPTION_COLOR_ANOMALIES;
    public static final String PROPERTY_SCHEME_CPD_DEFAULT = OPTION_COLOR_STANDARD_SCHEME;

    public static final String PROPERTY_SCHEME_RANGE_KEY = PROPERTY_SCHEME_KEY_SUFFIX + ".range";
    public static final String PROPERTY_SCHEME_RANGE_LABEL = "Range";
    public static final String PROPERTY_SCHEME_RANGE_TOOLTIP = "range options for the scheme";
    public static final String PROPERTY_SCHEME_RANGE_OPTION1 = OPTION_RANGE_FROM_SCHEME;
    public static final String PROPERTY_SCHEME_RANGE_OPTION2 = OPTION_RANGE_FROM_DATA;
    public static final String PROPERTY_SCHEME_RANGE_OPTION3 = OPTION_RANGE_FROM_CPD;
    public static final String PROPERTY_SCHEME_RANGE_DEFAULT = OPTION_RANGE_FROM_SCHEME;

    public static final String PROPERTY_SCHEME_LOG_KEY = PROPERTY_SCHEME_KEY_SUFFIX + ".log";
    public static final String PROPERTY_SCHEME_LOG_LABEL = "Log Scaling";
    public static final String PROPERTY_SCHEME_LOG_TOOLTIP = "log options for the scheme case";
    public static final String PROPERTY_SCHEME_LOG_OPTION1 = OPTION_LOG_FROM_SCHEME;
    public static final String PROPERTY_SCHEME_LOG_OPTION2 = OPTION_LOG_FROM_CPD;
    public static final String PROPERTY_SCHEME_LOG_OPTION3 = OPTION_LOG_TRUE;
    public static final String PROPERTY_SCHEME_LOG_OPTION4 = OPTION_LOG_FALSE;
    public static final String PROPERTY_SCHEME_LOG_DEFAULT = OPTION_LOG_FROM_SCHEME;

    public static final String PROPERTY_SCHEME_VERBOSE_KEY = PROPERTY_SCHEME_KEY_SUFFIX + ".verbose";
    public static final String PROPERTY_SCHEME_VERBOSE_LABEL = "Verbose Schemes";
    public static final String PROPERTY_SCHEME_VERBOSE_TOOLTIP = "Scheme selector will display the DISPLAY field instead of the scheme name";
    public static final boolean PROPERTY_SCHEME_VERBOSE_DEFAULT = true;

    public static final String PROPERTY_SCHEME_SHOW_DISABLED_KEY = PROPERTY_SCHEME_KEY_SUFFIX + ".show.disabled";
    public static final String PROPERTY_SCHEME_SHOW_DISABLED_LABEL = "Show Disabled Schemes";
    public static final String PROPERTY_SCHEME_SHOW_DISABLED_TOOLTIP = "Scheme selector will display all schemes including schemes with missing cpd files";
    public static final boolean PROPERTY_SCHEME_SHOW_DISABLED_DEFAULT = false;

    public static final String PROPERTY_SCHEME_SORT_KEY = PROPERTY_SCHEME_KEY_SUFFIX + ".sort";
    public static final String PROPERTY_SCHEME_SORT_LABEL = "Sort Schemes";
    public static final String PROPERTY_SCHEME_SORT_TOOLTIP = "Scheme selector will display all schemes sorted versus original xml order";
    public static final boolean PROPERTY_SCHEME_SORT_DEFAULT = false;


    // ----------------------------




    // Default Palettes

    private static final String PROPERTY_CPD_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".default.cpd";

    public static final String PROPERTY_CPD_SECTION_KEY = PROPERTY_CPD_KEY_SUFFIX + ".section";
    public static final String PROPERTY_CPD_SECTION_LABEL = "Palettes";
    public static final String PROPERTY_CPD_SECTION_TOOLTIP = "Palettes to use";

    public static final String PROPERTY_CPD_GRAY_SCALE_KEY = PROPERTY_CPD_KEY_SUFFIX + ".gray.scale";
    public static final String PROPERTY_CPD_GRAY_SCALE_LABEL = OPTION_COLOR_GRAY_SCALE;
    public static final String PROPERTY_CPD_GRAY_SCALE_TOOLTIP = "The cpd file to use when GRAY SCALE is selected";
    public static final String PROPERTY_CPD_GRAY_SCALE_DEFAULT = CPD_GRAY_SCALE_DEFAULT;

    public static final String PROPERTY_CPD_STANDARD_KEY = PROPERTY_CPD_KEY_SUFFIX + ".standard";
    public static final String PROPERTY_CPD_STANDARD_LABEL = OPTION_COLOR_STANDARD;
    public static final String PROPERTY_CPD_STANDARD_TOOLTIP = "The cpd file to use when STANDARD COLOR is selected";
    public static final String PROPERTY_CPD_STANDARD_DEFAULT = CPD_STANDARD_DEFAULT;

    public static final String PROPERTY_CPD_UNIVERSAL_KEY = PROPERTY_CPD_KEY_SUFFIX + ".universal";
    public static final String PROPERTY_CPD_UNIVERSAL_LABEL = OPTION_COLOR_UNIVERSAL;
    public static final String PROPERTY_CPD_UNIVERSAL_TOOLTIP = "The color blind compliant cpd file to use when UNIVERSAL COLOR is selected";
    public static final String PROPERTY_CPD_UNIVERSAL_DEFAULT = CPD_UNIVERSAL_DEFAULT;

    public static final String PROPERTY_CPD_ANOMALIES_KEY = PROPERTY_CPD_KEY_SUFFIX + ".anomalies";
    public static final String PROPERTY_CPD_ANOMALIES_LABEL = OPTION_COLOR_ANOMALIES;
    public static final String PROPERTY_CPD_ANOMALIES_TOOLTIP = "The cpd file to use when ANOMALIES is selected";
    public static final String PROPERTY_CPD_ANOMALIES_DEFAULT = CPD_ANOMALIES_DEFAULT;

    //-------------------








    // Restore to defaults

    private static final String PROPERTY_RESTORE_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".restore.defaults";

    public static final String PROPERTY_RESTORE_SECTION_KEY = PROPERTY_RESTORE_KEY_SUFFIX + ".section";
    public static final String PROPERTY_RESTORE_SECTION_LABEL = "---";
    public static final String PROPERTY_RESTORE_SECTION_TOOLTIP = "Restores preferences to the package defaults";

    public static final String PROPERTY_RESTORE_DEFAULTS_NAME = PROPERTY_RESTORE_KEY_SUFFIX + ".apply";
    public static final String PROPERTY_RESTORE_DEFAULTS_LABEL = "RESTORE DEFAULTS (Color Preferences)";
    public static final String PROPERTY_RESTORE_DEFAULTS_TOOLTIP = "Restore all color preferences to the default";
    public static final boolean PROPERTY_RESTORE_DEFAULTS_DEFAULT = false;

    //-------------------------------



    static public String getPropertyGeneralRange(PropertyMap configuration) {
        if (configuration != null) {
            return configuration.getPropertyString(PROPERTY_GENERAL_RANGE_KEY, PROPERTY_GENERAL_RANGE_DEFAULT);
        } else {
            return null;
        }
    }

    static public String getPropertyLogScaledOption(PropertyMap configuration) {
        if (configuration != null) {
            return configuration.getPropertyString(PROPERTY_GENERAL_LOG_KEY, PROPERTY_GENERAL_LOG_DEFAULT);
        } else {
            return null;
        }
    }

    public static boolean isPropertySchemeAutoApply(PropertyMap configuration) {
        if (configuration != null) {
            return configuration.getPropertyBool(PROPERTY_SCHEME_AUTO_APPLY_KEY, PROPERTY_SCHEME_AUTO_APPLY_DEFAULT);
        } else {
            return PROPERTY_SCHEME_AUTO_APPLY_DEFAULT;
        }
    }

    public static void debug(String message) {
        if (COLOR_SCHEME_CODE_DEBUG) {
            System.out.println(message);
        }
    }


}
