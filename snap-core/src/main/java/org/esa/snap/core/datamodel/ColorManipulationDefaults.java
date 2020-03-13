package org.esa.snap.core.datamodel;

import org.esa.snap.core.util.NamingConvention;
import static org.esa.snap.core.util.NamingConvention.*;


/**
 * Configuration which contains many key parameters with defaults and labels for the color manipulation tool
 *
 * @author Daniel Knowles
 * @date 2/1/2020
 */

public class ColorManipulationDefaults {

    public static final boolean COLOR_MANIPULATION_DEBUG = false;

    public static final String TOOLNAME_COLOR_MANIPULATION = NamingConvention.COLOR_MIXED_CASE + " Manipulation";


    // Directory names
    public static final String DIR_NAME_COLOR_SCHEMES = "color_schemes";
    public static final String DIR_NAME_RGB_PROFILES = "rgb_profiles";
    public static final String DIR_NAME_COLOR_PALETTES = "color_palettes";
    public static final String DIR_NAME_AUX_DATA = "auxdata";

    // Palette files
    public static final String PALETTE_STANDARD_DEFAULT = "oceancolor_standard.cpd";
    public static final String PALETTE_UNIVERSAL_DEFAULT = "universal_bluered.cpd";
    public static final String PALETTE_GRAY_SCALE_DEFAULT = "gray_scale.cpd";
    public static final String PALETTE_ANOMALIES_DEFAULT = "gradient_red_white_blue.cpd";
    public static final String PALETTE_DEFAULT = PALETTE_GRAY_SCALE_DEFAULT;

    // xml files used by the color scheme manager
    public static final String COLOR_SCHEME_LOOKUP_FILENAME = "color_palette_scheme_lookup.xml";
    public static final String COLOR_SCHEMES_FILENAME = "color_palette_schemes.xml";

    // Indicates which color palette contained within the color scheme xml to use
    public static final String OPTION_COLOR_STANDARD_SCHEME = "From Scheme STANDARD";
    public static final String OPTION_COLOR_UNIVERSAL_SCHEME = "From Scheme UNIVERSAL";

    // Color palette selections
    public static final String OPTION_COLOR_GRAY_SCALE = "GRAY SCALE";
    public static final String OPTION_COLOR_STANDARD = "STANDARD";
    public static final String OPTION_COLOR_UNIVERSAL = "UNIVERSAL";
    public static final String OPTION_COLOR_ANOMALIES = "ANOMALIES";

    // Range options
    public static final String OPTION_RANGE_FROM_SCHEME = "From Scheme";
    public static final String OPTION_RANGE_FROM_DATA = "From Data";
    public static final String OPTION_RANGE_FROM_PALETTE = "From Palette File";

    // Log scaling options
    public static final String OPTION_LOG_TRUE = "TRUE";
    public static final String OPTION_LOG_FALSE = "FALSE";
    public static final String OPTION_LOG_FROM_PALETTE = "From Palette File";
    public static final String OPTION_LOG_FROM_SCHEME = "From Scheme";

    // general usage values
    public static final double DOUBLE_NULL = -Double.MAX_VALUE;


    //--------------------------------------------------------------------------------------------------------------
    // Color Manipulation Preferences parameters

    // Preferences property prefix
    private static final String PROPERTY_ROOT_KEY = "color.manipulation";


    // General (Non-Scheme) Options

    private static final String PROPERTY_GENERAL_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".general";

    public static final String PROPERTY_GENERAL_SECTION_KEY = PROPERTY_GENERAL_KEY_SUFFIX + ".section";
    public static final String PROPERTY_GENERAL_SECTION_LABEL = "Standard Options";
    public static final String PROPERTY_GENERAL_SECTION_TOOLTIP = "General behavior when not using a " + COLOR_LOWER_CASE + " scheme";

    public static final String PROPERTY_GENERAL_PALETTE_KEY = PROPERTY_GENERAL_KEY_SUFFIX + ".palette";
    public static final String PROPERTY_GENERAL_PALETTE_LABEL = "Palette";
    public static final String PROPERTY_GENERAL_PALETTE_TOOLTIP = "The color palette file to use when NOT using a " + COLOR_LOWER_CASE + " scheme";
    public static final String PROPERTY_GENERAL_PALETTE_OPTION1 = OPTION_COLOR_GRAY_SCALE;
    public static final String PROPERTY_GENERAL_PALETTE_OPTION2 = OPTION_COLOR_STANDARD;
    public static final String PROPERTY_GENERAL_PALETTE_OPTION3 = OPTION_COLOR_UNIVERSAL;
    public static final String PROPERTY_GENERAL_PALETTE_OPTION4 = OPTION_COLOR_ANOMALIES;
    public static final String PROPERTY_GENERAL_PALETTE_DEFAULT = OPTION_COLOR_GRAY_SCALE;

    public static final String PROPERTY_GENERAL_RANGE_KEY = PROPERTY_GENERAL_KEY_SUFFIX + ".range";
    public static final String PROPERTY_GENERAL_RANGE_LABEL = "Range";
    public static final String PROPERTY_GENERAL_RANGE_TOOLTIP = "Range options to use when NOT using a " + COLOR_LOWER_CASE + "scheme";
    public static final String PROPERTY_GENERAL_RANGE_OPTION1 = OPTION_RANGE_FROM_DATA;
    public static final String PROPERTY_GENERAL_RANGE_OPTION2 = OPTION_RANGE_FROM_PALETTE;
    public static final String PROPERTY_GENERAL_RANGE_DEFAULT = OPTION_RANGE_FROM_DATA;

    public static final String PROPERTY_GENERAL_LOG_KEY = PROPERTY_GENERAL_KEY_SUFFIX + ".log";
    public static final String PROPERTY_GENERAL_LOG_LABEL = "Log Scaling";
    public static final String PROPERTY_GENERAL_LOG_TOOLTIP = "Log scaling options to use when NOT using a " + COLOR_LOWER_CASE + " scheme";
    public static final String PROPERTY_GENERAL_LOG_OPTION1 = OPTION_LOG_TRUE;
    public static final String PROPERTY_GENERAL_LOG_OPTION2 = OPTION_LOG_FALSE;
    public static final String PROPERTY_GENERAL_LOG_OPTION3 = OPTION_LOG_FROM_PALETTE;
    public static final String PROPERTY_GENERAL_LOG_DEFAULT = OPTION_LOG_FALSE;



    // Scheme option

    private static final String PROPERTY_SCHEME_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".scheme";

    public static final String PROPERTY_SCHEME_SECTION_KEY = PROPERTY_SCHEME_KEY_SUFFIX + ".section";
    public static final String PROPERTY_SCHEME_SECTION_LABEL = "Scheme Options";
    public static final String PROPERTY_SCHEME_SECTION_TOOLTIP = "<html>Behavior when using a " + COLOR_LOWER_CASE + " scheme as configured in<br>" +
            " color_palette_schemes.xml and color_palette_scheme_lookup.xml</html>";

    public static final String PROPERTY_SCHEME_AUTO_APPLY_KEY = PROPERTY_SCHEME_KEY_SUFFIX + ".auto.apply";
    public static final String PROPERTY_SCHEME_AUTO_APPLY_LABEL = "Apply " + NamingConvention.COLOR_MIXED_CASE + " Schemes Automatically";
    public static final String PROPERTY_SCHEME_AUTO_APPLY_TOOLTIP = "<html>Apply " + NamingConvention.COLOR_LOWER_CASE +" schemes automatically<br>" +
            " when opening a band based on its name</html>";
    public static final boolean PROPERTY_SCHEME_AUTO_APPLY_DEFAULT = false;

    public static final String PROPERTY_SCHEME_PALETTE_KEY = PROPERTY_SCHEME_KEY_SUFFIX + ".palette";
    public static final String PROPERTY_SCHEME_PALETTE_LABEL = "Palette";
    public static final String PROPERTY_SCHEME_PALETTE_TOOLTIP = "The color palette file to use for the scheme";
    public static final String PROPERTY_SCHEME_PALETTE_OPTION1 = OPTION_COLOR_STANDARD_SCHEME;
    public static final String PROPERTY_SCHEME_PALETTE_OPTION2 = OPTION_COLOR_UNIVERSAL_SCHEME;
    public static final String PROPERTY_SCHEME_PALETTE_OPTION3 = OPTION_COLOR_GRAY_SCALE;
    public static final String PROPERTY_SCHEME_PALETTE_OPTION4 = OPTION_COLOR_STANDARD;
    public static final String PROPERTY_SCHEME_PALETTE_OPTION5 = OPTION_COLOR_UNIVERSAL;
    public static final String PROPERTY_SCHEME_PALETTE_OPTION6 = OPTION_COLOR_ANOMALIES;
    public static final String PROPERTY_SCHEME_PALETTE_DEFAULT = OPTION_COLOR_STANDARD_SCHEME;

    public static final String PROPERTY_SCHEME_RANGE_KEY = PROPERTY_SCHEME_KEY_SUFFIX + ".range";
    public static final String PROPERTY_SCHEME_RANGE_LABEL = "Range";
    public static final String PROPERTY_SCHEME_RANGE_TOOLTIP = "Range options (min, max) to use for the scheme";
    public static final String PROPERTY_SCHEME_RANGE_OPTION1 = OPTION_RANGE_FROM_SCHEME;
    public static final String PROPERTY_SCHEME_RANGE_OPTION2 = OPTION_RANGE_FROM_DATA;
    public static final String PROPERTY_SCHEME_RANGE_OPTION3 = OPTION_RANGE_FROM_PALETTE;
    public static final String PROPERTY_SCHEME_RANGE_DEFAULT = OPTION_RANGE_FROM_SCHEME;

    public static final String PROPERTY_SCHEME_LOG_KEY = PROPERTY_SCHEME_KEY_SUFFIX + ".log";
    public static final String PROPERTY_SCHEME_LOG_LABEL = "Log Scaling";
    public static final String PROPERTY_SCHEME_LOG_TOOLTIP = "log scaling options to use for the scheme";
    public static final String PROPERTY_SCHEME_LOG_OPTION1 = OPTION_LOG_FROM_SCHEME;
    public static final String PROPERTY_SCHEME_LOG_OPTION2 = OPTION_LOG_FROM_PALETTE;
    public static final String PROPERTY_SCHEME_LOG_OPTION3 = OPTION_LOG_TRUE;
    public static final String PROPERTY_SCHEME_LOG_OPTION4 = OPTION_LOG_FALSE;
    public static final String PROPERTY_SCHEME_LOG_DEFAULT = OPTION_LOG_FROM_SCHEME;



    // Scheme Selector Options

    private static final String PROPERTY_SCHEME_SELECTOR_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".scheme.selector";

    public static final String PROPERTY_SCHEME_SELECTOR_SECTION_KEY = PROPERTY_SCHEME_SELECTOR_KEY_SUFFIX + ".section";
    public static final String PROPERTY_SCHEME_SELECTOR_SECTION_LABEL = "Scheme Selector Options";
    public static final String PROPERTY_SCHEME_SELECTOR_SECTION_TOOLTIP = "<html>Format options for the color schemes listed<br>" +
            " within the Scheme Selector</html>";

    public static final String PROPERTY_SCHEME_VERBOSE_KEY = PROPERTY_SCHEME_SELECTOR_KEY_SUFFIX + ".verbose";
    public static final String PROPERTY_SCHEME_VERBOSE_LABEL = "Verbose";
    public static final String PROPERTY_SCHEME_VERBOSE_TOOLTIP = "<html>Scheme selector will show the verbose VERBOSE_NAME field<br>" +
            " from the color_palette_schemes.xml</html>";
    public static final boolean PROPERTY_SCHEME_VERBOSE_DEFAULT = false;

    public static final String PROPERTY_SCHEME_SHOW_DISABLED_KEY = PROPERTY_SCHEME_SELECTOR_KEY_SUFFIX + ".show.disabled";
    public static final String PROPERTY_SCHEME_SHOW_DISABLED_LABEL = "Show Disabled";
    public static final String PROPERTY_SCHEME_SHOW_DISABLED_TOOLTIP = "<html>Scheme selector will display all schemes <br>" +
            "including schemes with missing cpd files</html>";
    public static final boolean PROPERTY_SCHEME_SHOW_DISABLED_DEFAULT = false;

    public static final String PROPERTY_SCHEME_SORT_KEY = PROPERTY_SCHEME_SELECTOR_KEY_SUFFIX + ".sort";
    public static final String PROPERTY_SCHEME_SORT_LABEL = "Sort";
    public static final String PROPERTY_SCHEME_SORT_TOOLTIP = "<html>Scheme selector will display all schemes alphabetically sorted<br>" +
            " as opposed to the original xml order</html>";
    public static final boolean PROPERTY_SCHEME_SORT_DEFAULT = true;

    public static final String PROPERTY_SCHEME_CATEGORIZE_DISPLAY_KEY = PROPERTY_SCHEME_SELECTOR_KEY_SUFFIX + ".split";
    public static final String PROPERTY_SCHEME_CATEGORIZE_DISPLAY_LABEL = "Categorize";
    public static final String PROPERTY_SCHEME_CATEGORIZE_DISPLAY_TOOLTIP = "<html>Scheme selector will display all schemes categorized into<br>" +
            "primary and additional categories by the PRIMARY field<br> of the color_palette_schemes.xml</html>";
    public static final boolean PROPERTY_SCHEME_CATEGORIZE_DISPLAY_DEFAULT = true;


    // Sliders Editor Options

    private static final String PROPERTY_SLIDER_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".slider.options";

    public static final String PROPERTY_SLIDERS_SECTION_KEY = PROPERTY_SLIDER_KEY_SUFFIX + ".section";
    public static final String PROPERTY_SLIDERS_SECTION_LABEL = "Sliders Editor Options";
    public static final String PROPERTY_SLIDERS_SECTION_TOOLTIP = "Options within the \"Sliders\" Editor of the " + TOOLNAME_COLOR_MANIPULATION + " GUI";

    public static final String PROPERTY_SLIDERS_SHOW_INFORMATION_KEY = PROPERTY_SLIDER_KEY_SUFFIX + ".extra.info";
    public static final String PROPERTY_SLIDERS_SHOW_INFORMATION_LABEL = "Show Information";
    public static final String PROPERTY_SLIDERS_SHOW_INFORMATION_TOOLTIP = "Display information in the histogram/slider view by default";
    public static final boolean PROPERTY_SLIDERS_SHOW_INFORMATION_DEFAULT = true;

    public static final String PROPERTY_SLIDERS_ZOOM_IN_KEY = PROPERTY_SLIDER_KEY_SUFFIX + ".zoom.in";
    public static final String PROPERTY_SLIDERS_ZOOM_IN_LABEL = "Histogram Zoom";
    public static final String PROPERTY_SLIDERS_ZOOM_IN_TOOLTIP = "Display histogram slider view zoomed in by default";
    public static final boolean PROPERTY_SLIDERS_ZOOM_IN_DEFAULT = true;



    // Button Enablement Options

    private static final String PROPERTY_BUTTONS_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".button.enablement";

    public static final String PROPERTY_BUTTONS_SECTION_KEY = PROPERTY_BUTTONS_KEY_SUFFIX + ".section";
    public static final String PROPERTY_BUTTONS_SECTION_LABEL = "Button Enablement";
    public static final String PROPERTY_BUTTONS_SECTION_TOOLTIP = "Button enablement options in the " + TOOLNAME_COLOR_MANIPULATION + " GUI";

    public static final String PROPERTY_100_PERCENT_BUTTON_KEY = PROPERTY_BUTTONS_KEY_SUFFIX + ".100.button";
    public static final String PROPERTY_100_PERCENT_BUTTON_LABEL = "100% Button";
    public static final String PROPERTY_100_PERCENT_BUTTON_TOOLTIP = "Enable 100% range button in the sliders editor";
    public static final boolean PROPERTY_100_PERCENT_BUTTON_DEFAULT = true;

    public static final String PROPERTY_95_PERCENT_BUTTON_KEY = PROPERTY_BUTTONS_KEY_SUFFIX + ".95.button";
    public static final String PROPERTY_95_PERCENT_BUTTON_LABEL = "95% Button";
    public static final String PROPERTY_95_PERCENT_BUTTON_TOOLTIP = "Enable 95% range button in the sliders editor";
    public static final boolean PROPERTY_95_PERCENT_BUTTON_DEFAULT = false;

    public static final String PROPERTY_1_SIGMA_BUTTON_KEY = PROPERTY_BUTTONS_KEY_SUFFIX + ".1.sigma.button";
    public static final String PROPERTY_1_SIGMA_BUTTON_LABEL = "<html>1&sigma; (68.27%) Button</html>";
    public static final String PROPERTY_1_SIGMA_BUTTON_TOOLTIP = "Enable 68.27% range button in the sliders editor";
    public static final boolean PROPERTY_1_SIGMA_BUTTON_DEFAULT = false;

    public static final String PROPERTY_2_SIGMA_BUTTON_KEY = PROPERTY_BUTTONS_KEY_SUFFIX + ".2.sigma.button";
    public static final String PROPERTY_2_SIGMA_BUTTON_LABEL = "<html>2&sigma; (95.45%) Button</html>";
    public static final String PROPERTY_2_SIGMA_BUTTON_TOOLTIP = "Enable 95.45% range button in the sliders editor";
    public static final boolean PROPERTY_2_SIGMA_BUTTON_DEFAULT = true;

    public static final String PROPERTY_3_SIGMA_BUTTON_KEY = PROPERTY_BUTTONS_KEY_SUFFIX + ".3.sigma.button";
    public static final String PROPERTY_3_SIGMA_BUTTON_LABEL = "<html>3&sigma; (99.73%) Button</html>";
    public static final String PROPERTY_3_SIGMA_BUTTON_TOOLTIP = "Enable 99.73% range button in the sliders editor";
    public static final boolean PROPERTY_3_SIGMA_BUTTON_DEFAULT = true;

    public static final String PROPERTY_ZOOM_VERTICAL_BUTTONS_KEY = PROPERTY_BUTTONS_KEY_SUFFIX + ".zoom.vertical.buttons";
    public static final String PROPERTY_ZOOM_VERTICAL_BUTTONS_LABEL = "Vertical Zoom Buttons";
    public static final String PROPERTY_ZOOM_VERTICAL_BUTTONS_TOOLTIP = "Enable zoom vertical buttons in the sliders editor";
    public static final boolean PROPERTY_ZOOM_VERTICAL_BUTTONS_DEFAULT = true;

    public static final String PROPERTY_INFORMATION_BUTTON_KEY = PROPERTY_BUTTONS_KEY_SUFFIX + ".extra.info.button";
    public static final String PROPERTY_INFORMATION_BUTTON_LABEL = "Information Button";
    public static final String PROPERTY_INFORMATION_BUTTON_TOOLTIP = "Enable histogram overlay information button in the sliders editor";
    public static final boolean PROPERTY_INFORMATION_BUTTON_DEFAULT = true;




    // Default Palettes

    private static final String PROPERTY_PALETTE_DEFAULT_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".default.palette";

    public static final String PROPERTY_PALETTE_DEFAULT_SECTION_KEY = PROPERTY_PALETTE_DEFAULT_KEY_SUFFIX + ".section";
    public static final String PROPERTY_PALETTE_DEFAULT_SECTION_LABEL = "Default Palettes";
    public static final String PROPERTY_PALETTE_DEFAULT_SECTION_TOOLTIP = "Palettes to use";

    public static final String PROPERTY_PALETTE_DEFAULT_GRAY_SCALE_KEY = PROPERTY_PALETTE_DEFAULT_KEY_SUFFIX + ".gray.scale";
    public static final String PROPERTY_PALETTE_DEFAULT_GRAY_SCALE_LABEL = OPTION_COLOR_GRAY_SCALE;
    public static final String PROPERTY_PALETTE_DEFAULT_GRAY_SCALE_TOOLTIP = "The palette file to use when GRAY SCALE is selected";
    public static final String PROPERTY_PALETTE_DEFAULT_GRAY_SCALE_DEFAULT = PALETTE_GRAY_SCALE_DEFAULT;

    public static final String PROPERTY_PALETTE_DEFAULT_STANDARD_KEY = PROPERTY_PALETTE_DEFAULT_KEY_SUFFIX + ".standard";
    public static final String PROPERTY_PALETTE_DEFAULT_STANDARD_LABEL = OPTION_COLOR_STANDARD;
    public static final String PROPERTY_PALETTE_DEFAULT_STANDARD_TOOLTIP = "The palette file to use when STANDARD " + COLOR_UPPER_CASE +" is selected";
    public static final String PROPERTY_PALETTE_DEFAULT_STANDARD_DEFAULT = PALETTE_STANDARD_DEFAULT;

    public static final String PROPERTY_PALETTE_DEFAULT_UNIVERSAL_KEY = PROPERTY_PALETTE_DEFAULT_KEY_SUFFIX + ".universal";
    public static final String PROPERTY_PALETTE_DEFAULT_UNIVERSAL_LABEL = OPTION_COLOR_UNIVERSAL;
    public static final String PROPERTY_PALETTE_DEFAULT_UNIVERSAL_TOOLTIP = "<html>The color blind compliant palette file to use when <br>" +
            "UNIVERSAL " + COLOR_UPPER_CASE + " is selected</html>";
    public static final String PROPERTY_PALETTE_DEFAULT_UNIVERSAL_DEFAULT = PALETTE_UNIVERSAL_DEFAULT;

    public static final String PROPERTY_PALETTE_DEFAULT_ANOMALIES_KEY = PROPERTY_PALETTE_DEFAULT_KEY_SUFFIX + ".anomalies";
    public static final String PROPERTY_PALETTE_DEFAULT_ANOMALIES_LABEL = OPTION_COLOR_ANOMALIES;
    public static final String PROPERTY_PALETTE_DEFAULT_ANOMALIES_TOOLTIP = "The palette file to use when ANOMALIES is selected";
    public static final String PROPERTY_PALETTE_DEFAULT_ANOMALIES_DEFAULT = PALETTE_ANOMALIES_DEFAULT;


    // RGB Options

    private static final String PROPERTY_RGB_OPTIONS_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".rgb.options";

    public static final String PROPERTY_RGB_OPTIONS_SECTION_KEY = PROPERTY_RGB_OPTIONS_KEY_SUFFIX + ".section";
    public static final String PROPERTY_RGB_OPTIONS_SECTION_LABEL = "RGB Options";
    public static final String PROPERTY_RGB_OPTIONS_SECTION_TOOLTIP = "Options for the RGB Image";

    public static final String PROPERTY_RGB_OPTIONS_MIN_KEY = PROPERTY_RGB_OPTIONS_KEY_SUFFIX + ".button.min";
    public static final String PROPERTY_RGB_OPTIONS_MIN_LABEL = "Range Button (Min)";
    public static final String PROPERTY_RGB_OPTIONS_MIN_TOOLTIP = "The min value to use in the RGB (A..B) range button";
    public static final double PROPERTY_RGB_OPTIONS_MIN_DEFAULT = 0.0;

    public static final String PROPERTY_RGB_OPTIONS_MAX_KEY = PROPERTY_RGB_OPTIONS_KEY_SUFFIX + "button.min";
    public static final String PROPERTY_RGB_OPTIONS_MAX_LABEL = "Range Button (Max)";
    public static final String PROPERTY_RGB_OPTIONS_MAX_TOOLTIP = "The max value to use in the RGB (A..B) range button";
    public static final double PROPERTY_RGB_OPTIONS_MAX_DEFAULT = 1.0;



    // Restore to defaults

    private static final String PROPERTY_RESTORE_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".restore.defaults";

    public static final String PROPERTY_RESTORE_SECTION_KEY = PROPERTY_RESTORE_KEY_SUFFIX + ".section";
    public static final String PROPERTY_RESTORE_SECTION_LABEL = "Restore";
    public static final String PROPERTY_RESTORE_SECTION_TOOLTIP = "Restores preferences to the package defaults";

    public static final String PROPERTY_RESTORE_DEFAULTS_NAME = PROPERTY_RESTORE_KEY_SUFFIX + ".apply";
    public static final String PROPERTY_RESTORE_DEFAULTS_LABEL = "Default (" + TOOLNAME_COLOR_MANIPULATION + " Preferences)";
    public static final String PROPERTY_RESTORE_DEFAULTS_TOOLTIP = "Restore all " + NamingConvention.COLOR_LOWER_CASE + " preferences to the original default";
    public static final boolean PROPERTY_RESTORE_DEFAULTS_DEFAULT = false;



    public static void debug(String message) {
        if (COLOR_MANIPULATION_DEBUG) {
            System.out.println(message);
        }
    }

}
