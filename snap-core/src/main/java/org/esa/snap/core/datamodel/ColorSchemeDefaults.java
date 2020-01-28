package org.esa.snap.core.datamodel;

import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.SystemUtils;

import java.nio.file.Path;

/**
 * Created by knowles on 11/20/19.
 */

public class ColorSchemeDefaults {

    //    public static final String DEFAULT_CPD_FILENAME = "gray_scale.cpd";
    public static final String DEFAULT_CPD_FILENAME = "oceancolor_standard.cpd";

    public static final String COLOR_SCHEME_LUT_FILENAME = "color_palette_scheme_defaults.txt";
    public static final String COLOR_SCHEMES_FILENAME = "color_palette_schemes.txt";

    public static final double DOUBLE_NULL = -Double.MAX_VALUE;
    public static final String NULL_ENTRY = "null";



    public static final String GRAY_SCALE = "GRAY SCALE";
    public static final String STANDARD_COLOR = "STANDARD";
    public static final String UNIVERSAL_COLOR = "UNIVERSAL";
    public static final String OTHER_COLOR = "OTHER";
    public static final String STANDARD_SCHEME = "From Scheme STANDARD";
    public static final String UNIVERSAL_SCHEME = "From Scheme UNIVERSAL";


    public static final String RANGE_FROM_SCHEME = "From Scheme";
    public static final String RANGE_FROM_DATA = "From Data";
    public static final String RANGE_FROM_CPD = "From Cpd";

    public static final String LOG_TRUE = "TRUE";
    public static final String LOG_FALSE = "FALSE";
    public static final String LOG_FROM_CPD = "From Cpd";
    public static final String LOG_FROM_SCHEME = "From Scheme";


    public static final String PROPERTY_DEFAULT_CPD_SECTION_KEY = "color.manipulation.default.cpd.section";
    public static final String PROPERTY_DEFAULT_CPD_SECTION_LABEL = "Default Palettes";
    public static final String PROPERTY_DEFAULT_CPD_SECTION_TOOLTIP = "Default palettes";
    public static final String PROPERTY_DEFAULT_CPD_SECTION_ALIAS = "colorManipulationDefaultCpdSection";

    public static final String PROPERTY_STANDARD_COLOR_CPD_KEY = "color.manipulation.standard.color.cpd";
    public static final String PROPERTY_STANDARD_COLOR_CPD_LABEL = STANDARD_COLOR;
    public static final String PROPERTY_STANDARD_COLOR_CPD_TOOLTIP = "The cpd file to use when STANDARD COLOR is selected";
    public static final String PROPERTY_STANDARD_COLOR_CPD_ALIAS = "colorManipulationStandardColorCpd";
    public static final String PROPERTY_STANDARD_COLOR_CPD_DEFAULT = "oceancolor_standard.cpd";
    public static final Class PROPERTY_STANDARD_COLOR_CPD_TYPE = String.class;

    public static final String PROPERTY_COLOR_BLIND_CPD_KEY = "color.manipulation.color.blind.cpd";
    public static final String PROPERTY_COLOR_BLIND_CPD_LABEL = UNIVERSAL_COLOR;
    public static final String PROPERTY_COLOR_BLIND_CPD_TOOLTIP = "The color blind compliant cpd file to use when UNIVERSAL COLOR is selected";
    public static final String PROPERTY_COLOR_BLIND_CPD_ALIAS = "colorManipulationColorBlindCpd";
    public static final String PROPERTY_COLOR_BLIND_CPD_DEFAULT = "universal_bluered.cpd";
    public static final Class PROPERTY_COLOR_BLIND_CPD_TYPE = String.class;

    public static final String PROPERTY_GRAY_SCALE_CPD_KEY = "color.manipulation.gray.scale.cpd";
    public static final String PROPERTY_GRAY_SCALE_CPD_LABEL = GRAY_SCALE;
    public static final String PROPERTY_GRAY_SCALE_CPD_TOOLTIP = "The cpd file to use when GRAY SCALE is selected";
    public static final String PROPERTY_GRAY_SCALE_CPD_ALIAS = "colorManipulationGrayScaleCpd";
    public static final String PROPERTY_GRAY_SCALE_CPD_DEFAULT = "gray_scale.cpd";
    public static final Class PROPERTY_GRAY_SCALE_CPD_TYPE = String.class;


    public static final String PROPERTY_OTHER_CPD_KEY = "color.manipulation.other.cpd";
    public static final String PROPERTY_OTHER_CPD_LABEL = OTHER_COLOR;
    public static final String PROPERTY_OTHER_CPD_TOOLTIP = "The cpd file to use when OTHER is selected";
    public static final String PROPERTY_OTHER_CPD_ALIAS = "colorManipulationOtherCpd";
    public static final String PROPERTY_OTHER_CPD_DEFAULT = "gradient_red_white_blue.cpd";
    public static final Class PROPERTY_OTHER_CPD_TYPE = String.class;


    public static final String PROPERTY_GENERAL_BEHAVIOR_SECTION_KEY = "color.manipulation.general.behavior.section";
    public static final String PROPERTY_GENERAL_BEHAVIOR_SECTION_LABEL = "General (No Scheme) Options";
    public static final String PROPERTY_GENERAL_BEHAVIOR_SECTION_TOOLTIP = "General behavior";
    public static final String PROPERTY_GENERAL_BEHAVIOR_SECTION_ALIAS = "colorManipulationGeneralBehaviorSection";


    public static final String PROPERTY_SCHEME_BEHAVIOR_SECTION_KEY = "color.manipulation.scheme.behavior.section";
    public static final String PROPERTY_SCHEME_BEHAVIOR_SECTION_LABEL = "Scheme Options";
    public static final String PROPERTY_SCHEME_BEHAVIOR_SECTION_TOOLTIP = "Scheme behavior";
    public static final String PROPERTY_SCHEME_BEHAVIOR_SECTION_ALIAS = "colorManipulationSchemeBehaviorSection";

    public static final String PROPERTY_GENERAL_CPD_KEY = "color.manipulation.general.cpd";
    public static final String PROPERTY_GENERAL_CPD_LABEL = "Palette";
    public static final String PROPERTY_GENERAL_CPD_TOOLTIP = "The cpd file to use when no color scheme";
    public static final String PROPERTY_GENERAL_CPD_ALIAS = "colorManipulationGeneralCpd";
    public static final String PROPERTY_GENERAL_CPD_OPTION1 = GRAY_SCALE;
    public static final String PROPERTY_GENERAL_CPD_OPTION2 = STANDARD_COLOR;
    public static final String PROPERTY_GENERAL_CPD_OPTION3 = UNIVERSAL_COLOR;
    public static final String PROPERTY_GENERAL_CPD_OPTION4 = OTHER_COLOR;
    public static final String PROPERTY_GENERAL_CPD_DEFAULT = GRAY_SCALE;
    public static final Class PROPERTY_GENERAL_CPD_TYPE = String.class;

    public static final String PROPERTY_GENERAL_RANGE_KEY = "color.manipulation.general.range";
    public static final String PROPERTY_GENERAL_RANGE_LABEL = "Range";
    public static final String PROPERTY_GENERAL_RANGE_TOOLTIP = "range options for the no scheme case";
    public static final String PROPERTY_GENERAL_RANGE_ALIAS = "colorManipulationGeneralRange";
    public static final String PROPERTY_GENERAL_RANGE_OPTION1 = RANGE_FROM_DATA;
    public static final String PROPERTY_GENERAL_RANGE_OPTION2 = RANGE_FROM_CPD;
    public static final String PROPERTY_GENERAL_RANGE_DEFAULT = RANGE_FROM_DATA;
    public static final Class PROPERTY_GENERAL_RANGE_TYPE = String.class;





    public static final String PROPERTY_GENERAL_LOG_KEY = "color.manipulation.general.log";
    public static final String PROPERTY_GENERAL_LOG_LABEL = "Log Scaling";
    public static final String PROPERTY_GENERAL_LOG_TOOLTIP = "log options for the no scheme case";
    public static final String PROPERTY_GENERAL_LOG_ALIAS = "colorManipulationGeneralLog";
    public static final String PROPERTY_GENERAL_LOG_OPTION1 = LOG_TRUE;
    public static final String PROPERTY_GENERAL_LOG_OPTION2 = LOG_FALSE;
    public static final String PROPERTY_GENERAL_LOG_OPTION3 = LOG_FROM_CPD;
    public static final String PROPERTY_GENERAL_LOG_DEFAULT = LOG_FALSE;
    public static final Class PROPERTY_GENERAL_LOG_TYPE = String.class;


    public static final String PROPERTY_USE_COLOR_BLIND_CPD_KEY = "color.manipulation.use.color.blind.cpd";
    public static final String PROPERTY_USE_COLOR_BLIND_CPD_LABEL = "Use Color Blind Palettes";
    public static final String PROPERTY_USE_COLOR_BLIND_CPD_TOOLTIP = "Use the color blind compliant palettes";
    public static final String PROPERTY_USE_COLOR_BLIND_CPD_ALIAS = "colorManipulationUseColorBlindCpd";
    public static final boolean PROPERTY_USE_COLOR_BLIND_CPD_DEFAULT = false;
    public static final Class PROPERTY_USE_COLOR_BLIND_CPD_TYPE = Boolean.class;


    public static final String PROPERTY_AUTO_APPLY_SCHEMES_KEY = "color.manipulation.auto.apply.schemes";
    public static final String PROPERTY_AUTO_APPLY_SCHEMES_LABEL = "Automatically Apply Color Schemes";
    public static final String PROPERTY_AUTO_APPLY_SCHEMES_TOOLTIP = "Automatically apply color schemes when opening a band based on its name";
    public static final String PROPERTY_AUTO_APPLY_SCHEMES_ALIAS = "colorManipulationAutoApplySchemes";
    public static final boolean PROPERTY_AUTO_APPLY_SCHEMES_DEFAULT = true;
    public static final Class PROPERTY_AUTO_APPLY_SCHEMES_TYPE = Boolean.class;


    public static final String PROPERTY_USE_SCHEME_PALETTE_STX_KEY = "color.manipulation.use.scheme.palette.stx";
    public static final String PROPERTY_USE_SCHEME_PALETTE_STX_LABEL = "Use Scheme Palette and Band Data Range";
    public static final String PROPERTY_USE_SCHEME_PALETTE_STX_TOOLTIP = "Only apply the color palette of a scheme and set min and max based on band data";
    public static final String PROPERTY_USE_SCHEME_PALETTE_STX_ALIAS = "colorManipulationUseSchemePaletteStx";
    public static final boolean PROPERTY_USE_SCHEME_PALETTE_STX_DEFAULT = false;
    public static final Class PROPERTY_USE_SCHEME_PALETTE_STX_TYPE = Boolean.class;


    public static final String PROPERTY_SCHEME_CPD_KEY = "color.manipulation.scheme.cpd";
    public static final String PROPERTY_SCHEME_CPD_LABEL = "Palette";
    public static final String PROPERTY_SCHEME_CPD_TOOLTIP = "The cpd file to use when for the scheme";
    public static final String PROPERTY_SCHEME_CPD_ALIAS = "colorManipulationSchemeCpd";
    public static final String PROPERTY_SCHEME_CPD_OPTION1 = STANDARD_SCHEME;
    public static final String PROPERTY_SCHEME_CPD_OPTION2 = UNIVERSAL_SCHEME;
    public static final String PROPERTY_SCHEME_CPD_OPTION3 = GRAY_SCALE;
    public static final String PROPERTY_SCHEME_CPD_OPTION4 = STANDARD_COLOR;
    public static final String PROPERTY_SCHEME_CPD_OPTION5 = UNIVERSAL_COLOR;
    public static final String PROPERTY_SCHEME_CPD_OPTION6 = OTHER_COLOR;
    public static final String PROPERTY_SCHEME_CPD_DEFAULT = STANDARD_SCHEME;
    public static final Class PROPERTY_SCHEME_CPD_TYPE = String.class;


    public static final String PROPERTY_SCHEME_RANGE_KEY = "color.manipulation.scheme.range";
    public static final String PROPERTY_SCHEME_RANGE_LABEL = "Range";
    public static final String PROPERTY_SCHEME_RANGE_TOOLTIP = "range options for the scheme";
    public static final String PROPERTY_SCHEME_RANGE_ALIAS = "colorManipulationSchemeRange";
    public static final String PROPERTY_SCHEME_RANGE_OPTION1 = RANGE_FROM_SCHEME;
    public static final String PROPERTY_SCHEME_RANGE_OPTION2 = RANGE_FROM_DATA;
    public static final String PROPERTY_SCHEME_RANGE_OPTION3 = RANGE_FROM_CPD;
    public static final String PROPERTY_SCHEME_RANGE_DEFAULT = RANGE_FROM_SCHEME;
    public static final Class PROPERTY_SCHEME_RANGE_TYPE = String.class;


    public static final String PROPERTY_SCHEME_LOG_KEY = "color.manipulation.scheme.log";
    public static final String PROPERTY_SCHEME_LOG_LABEL = "Log Scaling";
    public static final String PROPERTY_SCHEME_LOG_TOOLTIP = "log options for the scheme case";
    public static final String PROPERTY_SCHEME_LOG_ALIAS = "colorManipulationSchemeLog";
    public static final String PROPERTY_SCHEME_LOG_OPTION1 = LOG_FROM_SCHEME;
    public static final String PROPERTY_SCHEME_LOG_OPTION2 = LOG_FROM_CPD;
    public static final String PROPERTY_SCHEME_LOG_OPTION3 = LOG_TRUE;
    public static final String PROPERTY_SCHEME_LOG_OPTION4 = LOG_FALSE;
    public static final String PROPERTY_SCHEME_LOG_DEFAULT = LOG_FROM_SCHEME;
    public static final Class PROPERTY_SCHEME_LOG_TYPE = String.class;


    public static final String PROPERTY_RESTORE_SECTION_KEY = "color.manipulation.restoreDefaults.section";
    public static final String PROPERTY_RESTORE_SECTION_LABEL = "---";
    public static final String PROPERTY_RESTORE_SECTION_TOOLTIP = "---";
    public static final String PROPERTY_RESTORE_SECTION_ALIAS = "colorManipulationDefaultCpdSection";

    // Property Setting: Restore Defaults
    public static final String PROPERTY_RESTORE_DEFAULTS_NAME = "color.manipulation.restoreDefaults";
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


    public static Path getColorPalettesDir() {
        return SystemUtils.getAuxDataPath().resolve("color_palettes");
    }


    public static Path getColorSchemeDir() {
        return SystemUtils.getAuxDataPath().resolve("color_schemes");
    }

}
