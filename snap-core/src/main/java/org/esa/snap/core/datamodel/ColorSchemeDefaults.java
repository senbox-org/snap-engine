package org.esa.snap.core.datamodel;

import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.SystemUtils;

import java.nio.file.Path;

/**
 * Created by knowles on 11/20/19.
 */

public class ColorSchemeDefaults {

    public static final String COLOR_MANIPULATION = "Color Manipulation";

    public static final String COLOR_SCHEMES_AUX_DATA = "color_schemes";
    public static final String COLOR_PALETTES_AUX_DATA = "color_palettes";
    public static final String AUX_DATA = "auxdata";

    public static final String STANDARD_COLOR_CPD_DEFAULT = "oceancolor_standard.cpd";
    public static final String COLOR_BLIND_CPD_DEFAULT = "universal_bluered.cpd";
    public static final String GRAY_SCALE_CPD_DEFAULT = "gray_scale.cpd";
    public static final String OTHER_CPD_DEFAULT = "gradient_red_white_blue.cpd";
    public static final String DEFAULT_CPD_FILENAME = GRAY_SCALE_CPD_DEFAULT;


    public static final String PROPERTY_ROOT_KEY = "color.manipulation";



    public static final String COLOR_SCHEME_LUT_FILENAME = "color_palette_scheme_defaults.xml";
    public static final String COLOR_SCHEMES_FILENAME = "color_palette_schemes.xml";

    public static final double DOUBLE_NULL = -Double.MAX_VALUE;
    public static final String NULL_ENTRY = "null";



    public static final String GRAY_SCALE = "GRAY SCALE";
    public static final String STANDARD_COLOR = "STANDARD";
    public static final String UNIVERSAL_COLOR = "UNIVERSAL";
    public static final String OTHER_COLOR = "ANOMALIES";
    public static final String STANDARD_SCHEME = "From Scheme STANDARD";
    public static final String UNIVERSAL_SCHEME = "From Scheme UNIVERSAL";


    public static final String RANGE_FROM_SCHEME = "From Scheme";
    public static final String RANGE_FROM_DATA = "From Data";
    public static final String RANGE_FROM_CPD = "From Cpd";

    public static final String LOG_TRUE = "TRUE";
    public static final String LOG_FALSE = "FALSE";
    public static final String LOG_FROM_CPD = "From Cpd";
    public static final String LOG_FROM_SCHEME = "From Scheme";


    public static final String PROPERTY_DEFAULT_CPD_SECTION_KEY = PROPERTY_ROOT_KEY + ".default.cpd.section";
    public static final String PROPERTY_DEFAULT_CPD_SECTION_LABEL = "Default Palettes";
    public static final String PROPERTY_DEFAULT_CPD_SECTION_TOOLTIP = "Default palettes";

    public static final String PROPERTY_STANDARD_COLOR_CPD_KEY = PROPERTY_ROOT_KEY + ".standard.color.cpd";
    public static final String PROPERTY_STANDARD_COLOR_CPD_LABEL = STANDARD_COLOR;
    public static final String PROPERTY_STANDARD_COLOR_CPD_TOOLTIP = "The cpd file to use when STANDARD COLOR is selected";
    public static final String PROPERTY_STANDARD_COLOR_CPD_DEFAULT = STANDARD_COLOR_CPD_DEFAULT;

    public static final String PROPERTY_COLOR_BLIND_CPD_KEY = PROPERTY_ROOT_KEY + ".color.blind.cpd";
    public static final String PROPERTY_COLOR_BLIND_CPD_LABEL = UNIVERSAL_COLOR;
    public static final String PROPERTY_COLOR_BLIND_CPD_TOOLTIP = "The color blind compliant cpd file to use when UNIVERSAL COLOR is selected";
    public static final String PROPERTY_COLOR_BLIND_CPD_DEFAULT = COLOR_BLIND_CPD_DEFAULT;

    public static final String PROPERTY_GRAY_SCALE_CPD_KEY = PROPERTY_ROOT_KEY + ".gray.scale.cpd";
    public static final String PROPERTY_GRAY_SCALE_CPD_LABEL = GRAY_SCALE;
    public static final String PROPERTY_GRAY_SCALE_CPD_TOOLTIP = "The cpd file to use when GRAY SCALE is selected";
    public static final String PROPERTY_GRAY_SCALE_CPD_DEFAULT = GRAY_SCALE_CPD_DEFAULT;

    public static final String PROPERTY_OTHER_CPD_KEY = PROPERTY_ROOT_KEY + ".other.cpd";
    public static final String PROPERTY_OTHER_CPD_LABEL = OTHER_COLOR;
    public static final String PROPERTY_OTHER_CPD_TOOLTIP = "The cpd file to use when OTHER is selected";
    public static final String PROPERTY_OTHER_CPD_DEFAULT = OTHER_CPD_DEFAULT;



    public static final String PROPERTY_GENERAL_BEHAVIOR_SECTION_KEY = PROPERTY_ROOT_KEY + ".general.behavior.section";
    public static final String PROPERTY_GENERAL_BEHAVIOR_SECTION_LABEL = "General (No Scheme) Options";
    public static final String PROPERTY_GENERAL_BEHAVIOR_SECTION_TOOLTIP = "General behavior";

    public static final String PROPERTY_SCHEME_BEHAVIOR_SECTION_KEY = PROPERTY_ROOT_KEY + ".scheme.behavior.section";
    public static final String PROPERTY_SCHEME_BEHAVIOR_SECTION_LABEL = "Scheme Options";
    public static final String PROPERTY_SCHEME_BEHAVIOR_SECTION_TOOLTIP = "Scheme behavior";

    public static final String PROPERTY_GENERAL_CPD_KEY = PROPERTY_ROOT_KEY + ".general.cpd";
    public static final String PROPERTY_GENERAL_CPD_LABEL = "Palette";
    public static final String PROPERTY_GENERAL_CPD_TOOLTIP = "The cpd file to use when no color scheme";
    public static final String PROPERTY_GENERAL_CPD_OPTION1 = GRAY_SCALE;
    public static final String PROPERTY_GENERAL_CPD_OPTION2 = STANDARD_COLOR;
    public static final String PROPERTY_GENERAL_CPD_OPTION3 = UNIVERSAL_COLOR;
    public static final String PROPERTY_GENERAL_CPD_OPTION4 = OTHER_COLOR;
    public static final String PROPERTY_GENERAL_CPD_DEFAULT = GRAY_SCALE;

    public static final String PROPERTY_GENERAL_RANGE_KEY = PROPERTY_ROOT_KEY + ".general.range";
    public static final String PROPERTY_GENERAL_RANGE_LABEL = "Range";
    public static final String PROPERTY_GENERAL_RANGE_TOOLTIP = "range options for the no scheme case";
    public static final String PROPERTY_GENERAL_RANGE_OPTION1 = RANGE_FROM_DATA;
    public static final String PROPERTY_GENERAL_RANGE_OPTION2 = RANGE_FROM_CPD;
    public static final String PROPERTY_GENERAL_RANGE_DEFAULT = RANGE_FROM_DATA;



    public static final String PROPERTY_GENERAL_LOG_KEY = PROPERTY_ROOT_KEY + ".general.log";
    public static final String PROPERTY_GENERAL_LOG_LABEL = "Log Scaling";
    public static final String PROPERTY_GENERAL_LOG_TOOLTIP = "log options for the no scheme case";
    public static final String PROPERTY_GENERAL_LOG_OPTION1 = LOG_TRUE;
    public static final String PROPERTY_GENERAL_LOG_OPTION2 = LOG_FALSE;
    public static final String PROPERTY_GENERAL_LOG_OPTION3 = LOG_FROM_CPD;
    public static final String PROPERTY_GENERAL_LOG_DEFAULT = LOG_FALSE;



    public static final String PROPERTY_AUTO_APPLY_SCHEMES_KEY = PROPERTY_ROOT_KEY + ".auto.apply.schemes";
    public static final String PROPERTY_AUTO_APPLY_SCHEMES_LABEL = "Automatically Apply Color Schemes";
    public static final String PROPERTY_AUTO_APPLY_SCHEMES_TOOLTIP = "Automatically apply color schemes when opening a band based on its name";
    public static final boolean PROPERTY_AUTO_APPLY_SCHEMES_DEFAULT = true;



    public static final String PROPERTY_SCHEME_CPD_KEY = PROPERTY_ROOT_KEY + ".scheme.cpd";
    public static final String PROPERTY_SCHEME_CPD_LABEL = "Palette";
    public static final String PROPERTY_SCHEME_CPD_TOOLTIP = "The cpd file to use for the scheme";
    public static final String PROPERTY_SCHEME_CPD_OPTION1 = STANDARD_SCHEME;
    public static final String PROPERTY_SCHEME_CPD_OPTION2 = UNIVERSAL_SCHEME;
    public static final String PROPERTY_SCHEME_CPD_OPTION3 = GRAY_SCALE;
    public static final String PROPERTY_SCHEME_CPD_OPTION4 = STANDARD_COLOR;
    public static final String PROPERTY_SCHEME_CPD_OPTION5 = UNIVERSAL_COLOR;
    public static final String PROPERTY_SCHEME_CPD_OPTION6 = OTHER_COLOR;
    public static final String PROPERTY_SCHEME_CPD_DEFAULT = STANDARD_SCHEME;


    public static final String PROPERTY_SCHEME_RANGE_KEY = PROPERTY_ROOT_KEY + ".scheme.range";
    public static final String PROPERTY_SCHEME_RANGE_LABEL = "Range";
    public static final String PROPERTY_SCHEME_RANGE_TOOLTIP = "range options for the scheme";
    public static final String PROPERTY_SCHEME_RANGE_OPTION1 = RANGE_FROM_SCHEME;
    public static final String PROPERTY_SCHEME_RANGE_OPTION2 = RANGE_FROM_DATA;
    public static final String PROPERTY_SCHEME_RANGE_OPTION3 = RANGE_FROM_CPD;
    public static final String PROPERTY_SCHEME_RANGE_DEFAULT = RANGE_FROM_SCHEME;


    public static final String PROPERTY_SCHEME_LOG_KEY = PROPERTY_ROOT_KEY + ".scheme.log";
    public static final String PROPERTY_SCHEME_LOG_LABEL = "Log Scaling";
    public static final String PROPERTY_SCHEME_LOG_TOOLTIP = "log options for the scheme case";
    public static final String PROPERTY_SCHEME_LOG_OPTION1 = LOG_FROM_SCHEME;
    public static final String PROPERTY_SCHEME_LOG_OPTION2 = LOG_FROM_CPD;
    public static final String PROPERTY_SCHEME_LOG_OPTION3 = LOG_TRUE;
    public static final String PROPERTY_SCHEME_LOG_OPTION4 = LOG_FALSE;
    public static final String PROPERTY_SCHEME_LOG_DEFAULT = LOG_FROM_SCHEME;


    public static final String PROPERTY_RESTORE_SECTION_KEY = PROPERTY_ROOT_KEY + ".restoreDefaults.section";
    public static final String PROPERTY_RESTORE_SECTION_LABEL = "---";
    public static final String PROPERTY_RESTORE_SECTION_TOOLTIP = "---";

    // Property Setting: Restore Defaults
    public static final String PROPERTY_RESTORE_DEFAULTS_NAME = PROPERTY_ROOT_KEY + ".restoreDefaults";
    public static final String PROPERTY_RESTORE_TO_DEFAULTS_LABEL = "RESTORE DEFAULTS (Color Preferences)";
    public static final String PROPERTY_RESTORE_TO_DEFAULTS_TOOLTIP = "Restore all color preferences to the default";
    public static final boolean PROPERTY_RESTORE_TO_DEFAULTS_DEFAULT = false;





    static public String getPreferencesRangeNonScheme(PropertyMap configuration) {
        if (configuration != null) {
            return configuration.getPropertyString(PROPERTY_GENERAL_RANGE_KEY, PROPERTY_GENERAL_RANGE_DEFAULT);
        } else {
            return null;
        }
    }

    static public String getPreferencesLogScaledNonScheme(PropertyMap configuration) {
        if (configuration != null) {
            return configuration.getPropertyString(PROPERTY_GENERAL_LOG_KEY, PROPERTY_GENERAL_LOG_DEFAULT);
        } else {
            return null;
        }
    }

    public static boolean isPreferencesAutoApplyScheme(PropertyMap configuration) {
        if (configuration != null) {
            return configuration.getPropertyBool(PROPERTY_AUTO_APPLY_SCHEMES_KEY, PROPERTY_AUTO_APPLY_SCHEMES_DEFAULT);
        } else {
            return PROPERTY_AUTO_APPLY_SCHEMES_DEFAULT;
        }
    }






    public static Path getColorPalettesAuxData() {
        return SystemUtils.getAuxDataPath().resolve(COLOR_PALETTES_AUX_DATA);
    }

    public static Path getColorSchemesAuxData() {
        return SystemUtils.getAuxDataPath().resolve(COLOR_SCHEMES_AUX_DATA);
    }



}
