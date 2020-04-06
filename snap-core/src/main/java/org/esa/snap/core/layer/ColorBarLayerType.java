

package org.esa.snap.core.layer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.annotations.LayerTypeMetadata;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * @author Daniel Knowles
 */


@LayerTypeMetadata(name = "ColorBarLayerType", aliasNames = {"org.esa.snap.core.layer.ColorBarLayerType"})
public class ColorBarLayerType extends LayerType {


    public static final String OPTION_HORIZONTAL = "Horizontal";
    public static final String OPTION_VERTICAL = "Vertical";


    public static final String FONT_NAME_VALUE_1 = "SanSerif";
    public static final String FONT_NAME_VALUE_2 = "Serif";
    public static final String FONT_NAME_VALUE_3 = "Courier";
    public static final String FONT_NAME_VALUE_4 = "Monospaced";
    public static final Object FONT_NAME_VALUE_SET[] = {FONT_NAME_VALUE_1, FONT_NAME_VALUE_2, FONT_NAME_VALUE_3, FONT_NAME_VALUE_4};



    public static final String DISTRIB_EVEN_STR = "Auto Values";
    public static final String DISTRIB_EXACT_STR = "Palette Values";
    public static final String DISTRIB_MANUAL_STR = "Custom Values";


    //--------------------------------------------------------------------------------------------------------------
    // Color Bar Legend parameters

    // Preferences property prefix
    private static final String PROPERTY_ROOT_KEY = "color.bar.legend";
    private static final String PROPERTY_ROOT_ALIAS = "colorBarLegend";


    // Label (Values)

    private static final String PROPERTY_LABEL_VALUES_ROOT_KEY = PROPERTY_ROOT_KEY + ".label.values";
    private static final String PROPERTY_LABEL_VALUES_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "LabelValues";

    public static final String PROPERTY_LABEL_VALUES_SECTION_KEY = PROPERTY_LABEL_VALUES_ROOT_KEY + ".section";
    public static final String PROPERTY_LABEL_VALUES_SECTION_LABEL = "Labels";
    public static final String PROPERTY_LABEL_VALUES_SECTION_TOOLTIP = "Numeric value options for the color bar legend labels";
    public static final String PROPERTY_LABEL_VALUES_SECTION_ALIAS = PROPERTY_LABEL_VALUES_ROOT_ALIAS +"Section";

    public static final String PROPERTY_LABEL_VALUES_MODE_KEY = PROPERTY_LABEL_VALUES_ROOT_KEY + ".mode";
    public static final String PROPERTY_LABEL_VALUES_MODE_LABEL = "Mode";
    public static final String PROPERTY_LABEL_VALUES_MODE_TOOLTIP = "Mode for setting label values on the color bar legend";
    public static final Class PROPERTY_LABEL_VALUES_MODE_TYPE = String.class;
    public static final String PROPERTY_LABEL_VALUES_MODE_ALIAS = PROPERTY_LABEL_VALUES_ROOT_ALIAS + "Mode";
    public static final String PROPERTY_LABEL_VALUES_MODE_OPTION1 = DISTRIB_EVEN_STR;
    public static final String PROPERTY_LABEL_VALUES_MODE_OPTION2 = DISTRIB_MANUAL_STR;
    public static final String PROPERTY_LABEL_VALUES_MODE_OPTION3 = DISTRIB_EXACT_STR;
    public static final String PROPERTY_LABEL_VALUES_MODE_DEFAULT = DISTRIB_EVEN_STR;
    public static final Object PROPERTY_LABEL_VALUES_MODE_VALUE_SET[] = {
            PROPERTY_LABEL_VALUES_MODE_OPTION1,
            PROPERTY_LABEL_VALUES_MODE_OPTION2,
            PROPERTY_LABEL_VALUES_MODE_OPTION3 };


    public static final String PROPERTY_LABEL_VALUES_COUNT_KEY = PROPERTY_LABEL_VALUES_ROOT_KEY + ".count";
    public static final String PROPERTY_LABEL_VALUES_COUNT_LABEL = "Tick Mark Count";
    public static final String PROPERTY_LABEL_VALUES_COUNT_TOOLTIP = "Number of tickmarks";
    public static final String PROPERTY_LABEL_VALUES_COUNT_ALIAS = PROPERTY_LABEL_VALUES_ROOT_ALIAS + "Count";
    public static final int PROPERTY_LABEL_VALUES_COUNT_DEFAULT = 5;
    public static final boolean PROPERTY_LABEL_VALUES_COUNT_ENABLED = true;
    public static final Class PROPERTY_LABEL_VALUES_COUNT_TYPE = Integer.class;
    public static final int PROPERTY_LABEL_VALUES_COUNT_MIN = 2;
    public static final int PROPERTY_LABEL_VALUES_COUNT_MAX = 20;
    public static final String PROPERTY_LABEL_VALUES_COUNT_INTERVAL = "[" + ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_MIN + "," + ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_MAX + "]";

    public static final String PROPERTY_LABEL_VALUES_ACTUAL_KEY = PROPERTY_LABEL_VALUES_ROOT_KEY + ".actual";
    public static final String PROPERTY_LABEL_VALUES_ACTUAL_LABEL = "Custom Values";
    public static final String PROPERTY_LABEL_VALUES_ACTUAL_TOOLTIP = "Set actual values of the tickmarks";
    private static final String PROPERTY_LABEL_VALUES_ACTUAL_ALIAS = PROPERTY_LABEL_VALUES_ROOT_ALIAS + "Actual";
    public static final boolean PROPERTY_LABEL_VALUES_ACTUAL_ENABLED = false;

    public static final String PROPERTY_LABEL_VALUES_ACTUAL_DEFAULT = "";
    public static final Class PROPERTY_LABEL_VALUES_ACTUAL_TYPE = String.class;

    public static final String PROPERTY_LABEL_VALUES_SCALING_KEY = PROPERTY_LABEL_VALUES_ROOT_KEY + ".scaling.factor";
    public static final String PROPERTY_LABEL_VALUES_SCALING_LABEL = "Scaling Factor";
    public static final String PROPERTY_LABEL_VALUES_SCALING_TOOLTIP = "Tickmark labels will be displayed after multiplication with this scaling factor";
    public static final String PROPERTY_LABEL_VALUES_SCALING_ALIAS = PROPERTY_LABEL_VALUES_ROOT_ALIAS + "ScalingFactor";
    public static final double PROPERTY_LABEL_VALUES_SCALING_DEFAULT = 1.0;
    public static final Class PROPERTY_LABEL_VALUES_SCALING_TYPE = Double.class;
    public static final double PROPERTY_LABEL_VALUES_SCALING_MIN = 0.0000001;
    public static final double PROPERTY_LABEL_VALUES_SCALING_MAX = 1000000000;
    public static final String PROPERTY_LABEL_VALUES_SCALING_INTERVAL = "[" +
            ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_MIN + "," +
            ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_MAX + "]";


    public static final String PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY = PROPERTY_LABEL_VALUES_ROOT_KEY + ".decimal.places";
    public static final String PROPERTY_LABEL_VALUES_DECIMAL_PLACES_LABEL = "Decimal Places";
    public static final String PROPERTY_LABEL_VALUES_DECIMAL_PLACES_TOOLTIP = "Decimal places to display the numeric tick mark labels";
    public static final String PROPERTY_LABEL_VALUES_DECIMAL_PLACES_ALIAS = PROPERTY_LABEL_VALUES_ROOT_ALIAS + "DecimalPlaces";
    public static final int PROPERTY_LABEL_VALUES_DECIMAL_PLACES_DEFAULT = 2;
    public static final Class PROPERTY_LABEL_VALUES_DECIMAL_PLACES_TYPE = Integer.class;
    public static final int PROPERTY_LABEL_VALUES_DECIMAL_PLACES_MIN = 0;
    public static final int PROPERTY_LABEL_VALUES_DECIMAL_PLACES_MAX = 10;
    public static final String PROPERTY_LABEL_VALUES_DECIMAL_PLACES_INTERVAL = "[" +
            ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_MIN + "," +
            ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_MAX + "]";


    public static final String PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY = PROPERTY_LABEL_VALUES_ROOT_KEY + ".force.decimal.places";
    public static final String PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_LABEL = "Force Decimal Places";
    public static final String PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_TOOLTIP = "Force to exact decimal places";
    private static final String PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_ALIAS = PROPERTY_LABEL_VALUES_ROOT_ALIAS + "ForceDecimalPlaces";
    public static final boolean PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_DEFAULT = true;
    public static final Class PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_TYPE = Boolean.class;




    // Orientation

    private static final String PROPERTY_FORMATTING_ROOT_KEY = PROPERTY_ROOT_KEY + ".orientation";
    private static final String PROPERTY_FORMATTING_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "Orientation";

    public static final String PROPERTY_FORMATTING_SECTION_KEY = PROPERTY_FORMATTING_ROOT_KEY + ".section";
    public static final String PROPERTY_FORMATTING_SECTION_LABEL = "Orientation & Direction";
    public static final String PROPERTY_FORMATTING_SECTION_TOOLTIP = "Orientation options for the color bar legend";
    public static final String PROPERTY_FORMATTING_SECTION_ALIAS = PROPERTY_FORMATTING_ROOT_ALIAS + "Section";

    public static final String PROPERTY_FORMATTING_ORIENTATION_KEY = PROPERTY_FORMATTING_ROOT_KEY + ".orientation";
    public static final String PROPERTY_FORMATTING_ORIENTATION_LABEL = "Orientation";
    public static final String PROPERTY_FORMATTING_ORIENTATION_TOOLTIP = "Orientation of the color bar legend";
    public static final Class PROPERTY_FORMATTING_ORIENTATION_TYPE = String.class;
    public static final String PROPERTY_FORMATTING_ORIENTATION_ALIAS = PROPERTY_FORMATTING_ROOT_ALIAS + "Orientation";
    public static final String PROPERTY_FORMATTING_ORIENTATION_OPTION1 = OPTION_HORIZONTAL;
    public static final String PROPERTY_FORMATTING_ORIENTATION_OPTION2 = OPTION_VERTICAL;
    public static final String PROPERTY_FORMATTING_ORIENTATION_DEFAULT = OPTION_HORIZONTAL;
    public static final Object PROPERTY_FORMATTING_ORIENTATION_VALUE_SET[] = {PROPERTY_FORMATTING_ORIENTATION_OPTION1, PROPERTY_FORMATTING_ORIENTATION_OPTION2};


    public static final String PROPERTY_FORMATTING_REVERSE_PALETTE_KEY = PROPERTY_FORMATTING_ROOT_KEY + ".reverse.palette";
    public static final String PROPERTY_FORMATTING_REVERSE_PALETTE_LABEL = "Reverse Palette & Labels";
    public static final String PROPERTY_FORMATTING_REVERSE_PALETTE_TOOLTIP = "Reverse direction of palette and labels";
    private static final String PROPERTY_FORMATTING_REVERSE_PALETTE_ALIAS = PROPERTY_FORMATTING_ROOT_ALIAS + "ReversePalette";
    public static final boolean PROPERTY_FORMATTING_REVERSE_PALETTE_DEFAULT = false;
    public static final Class PROPERTY_FORMATTING_REVERSE_PALETTE_TYPE = Boolean.class;









    // ColorBar Location Section

    private static final String PROPERTY_LOCATION_ROOT_KEY = PROPERTY_ROOT_KEY + ".location";
    private static final String PROPERTY_LOCATION_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "Location";

    public static final String PROPERTY_LOCATION_SECTION_KEY = PROPERTY_LOCATION_ROOT_KEY + ".section";
    public static final String PROPERTY_LOCATION_SECTION_LABEL = "Location";
    public static final String PROPERTY_LOCATION_SECTION_TOOLTIP = "Set location and relative size of color bar image";
    public static final String PROPERTY_LOCATION_SECTION_ALIAS = PROPERTY_LOCATION_ROOT_ALIAS + "Section";

    public static final String PROPERTY_LOCATION_INSIDE_KEY = PROPERTY_LOCATION_ROOT_KEY + ".inside";
    public static final String PROPERTY_LOCATION_INSIDE_LABEL = "Place Color Bar Inside Image";
    public static final String PROPERTY_LOCATION_INSIDE_TOOLTIP = "Place color bar inside/outside image bounds";
    private static final String PROPERTY_LOCATION_INSIDE_ALIAS = PROPERTY_LOCATION_ROOT_ALIAS + "Inside";
    public static final boolean PROPERTY_LOCATION_INSIDE_DEFAULT = true;
    public static final Class PROPERTY_LOCATION_INSIDE_TYPE = Boolean.class;


    public static final String LOCATION_UPPER_LEFT = "Upper Left";
    public static final String LOCATION_UPPER_CENTER = "Upper Center";
    public static final String LOCATION_UPPER_RIGHT = "Upper Right";
    public static final String LOCATION_LOWER_LEFT = "Lower Left";
    public static final String LOCATION_LOWER_CENTER = "Lower Center";
    public static final String LOCATION_LOWER_RIGHT = "Lower Right";
    public static final String LOCATION_LEFT_CENTER = "Left Side Center";
    public static final String LOCATION_RIGHT_CENTER = "Right Side Center";
    public static String[] getColorBarLocationArray() {
        return  new String[]{
                LOCATION_UPPER_LEFT,
                LOCATION_UPPER_CENTER,
                LOCATION_UPPER_RIGHT,
                LOCATION_LOWER_LEFT,
                LOCATION_LOWER_CENTER,
                LOCATION_LOWER_RIGHT,
                LOCATION_LEFT_CENTER,
                LOCATION_RIGHT_CENTER
        };
    }

    public static final String PROPERTY_LOCATION_PLACEMENT_KEY = PROPERTY_LOCATION_ROOT_KEY + ".anchor";
    public static final String PROPERTY_LOCATION_PLACEMENT_LABEL = "Anchor";
    public static final String PROPERTY_LOCATION_PLACEMENT_TOOLTIP = "Where to place color bar on image";
    private static final String PROPERTY_LOCATION_PLACEMENT_ALIAS = PROPERTY_LOCATION_ROOT_ALIAS + "Anchor";
    public static final String PROPERTY_LOCATION_PLACEMENT_DEFAULT = LOCATION_LOWER_RIGHT;
    public static final Class PROPERTY_LOCATION_PLACEMENT_TYPE = String.class;

    public static final String PROPERTY_LOCATION_OFFSET_KEY = PROPERTY_LOCATION_ROOT_KEY + ".offset";
    public static final String PROPERTY_LOCATION_OFFSET_LABEL = "Anchor Offset";
    public static final String PROPERTY_LOCATION_OFFSET_TOOLTIP = "Move color bar away from axis (by percentage of color bar height)";
    private static final String PROPERTY_LOCATION_OFFSET_ALIAS = PROPERTY_LOCATION_ROOT_ALIAS + "Offset";
    public static final Double PROPERTY_LOCATION_OFFSET_DEFAULT = 0.0;
    public static final Class PROPERTY_LOCATION_OFFSET_TYPE = Double.class;

    public static final String PROPERTY_LOCATION_SHIFT_KEY = PROPERTY_LOCATION_ROOT_KEY + ".shift";
    public static final String PROPERTY_LOCATION_SHIFT_LABEL = "Anchor Shift";
    public static final String PROPERTY_LOCATION_SHIFT_TOOLTIP = "Move color bar along the axis (by percentage of color bar width)";
    private static final String PROPERTY_LOCATION_SHIFT_ALIAS = PROPERTY_LOCATION_ROOT_ALIAS + "Shift";
    public static final Double PROPERTY_LOCATION_SHIFT_DEFAULT = 0.0;
    public static final Class PROPERTY_LOCATION_SHIFT_TYPE = Double.class;


    public static final String VERTICAL_TITLE_LEFT = "Left";
    public static final String VERTICAL_TITLE_RIGHT = "Right";
    public static final String VERTICAL_TITLE_TOP = "Top";
    public static final String VERTICAL_TITLE_BOTTOM = "Bottom";


    public static Object[] VERTICAL_TITLE_LOCATION_VALUE_SET = {
            VERTICAL_TITLE_LEFT,
            VERTICAL_TITLE_RIGHT,
            VERTICAL_TITLE_TOP,
            VERTICAL_TITLE_BOTTOM
    };

    public static final String PROPERTY_TITLE_VERTICAL_LOCATION_KEY = PROPERTY_LOCATION_ROOT_KEY + "title.vertical.anchor";
    public static final String PROPERTY_TITLE_VERTICAL_LOCATION_LABEL = "Title Anchor";
    public static final String PROPERTY_TITLE_VERTICAL_LOCATION_TOOLTIP = "Where to place title on vertical legend";
    private static final String PROPERTY_TITLE_VERTICAL_LOCATION_ALIAS = PROPERTY_LOCATION_ROOT_ALIAS + "TitleVerticalAnchor";
    public static final String PROPERTY_TITLE_VERTICAL_LOCATION_DEFAULT = LOCATION_LOWER_RIGHT;
    public static final Class PROPERTY_TITLE_VERTICAL_LOCATION_TYPE = String.class;
    public static final Object PROPERTY_TITLE_VERTICAL_LOCATION_VALUE_SET[] = VERTICAL_TITLE_LOCATION_VALUE_SET;




    // Image Scaling Section

    private static final String PROPERTY_IMAGE_SCALING_ROOT_KEY = PROPERTY_ROOT_KEY + ".scaling";
    private static final String PROPERTY_IMAGE_SCALING_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "Scaling";

    public static final String PROPERTY_IMAGE_SCALING_SECTION_NAME = PROPERTY_IMAGE_SCALING_ROOT_KEY + ".section";
    public static final String PROPERTY_IMAGE_SCALING_SECTION_LABEL = "Size & Scaling";
    public static final String PROPERTY_IMAGE_SCALING_SECTION_TOOLTIP = "Set scaling and relative size of color bar image";
    public static final String PROPERTY_IMAGE_SCALING_SECTION_ALIAS = PROPERTY_IMAGE_SCALING_ROOT_ALIAS + "Section";

    public static final String PROPERTY_IMAGE_SCALING_APPLY_SIZE_NAME = PROPERTY_IMAGE_SCALING_ROOT_KEY + ".apply";
    public static final String PROPERTY_IMAGE_SCALING_APPLY_SIZE_LABEL = "Scale size relative to image size";
    public static final String PROPERTY_IMAGE_SCALING_APPLY_SIZE_TOOLTIP = "Scale the color bar size relative to the scene image size";
    private static final String PROPERTY_IMAGE_SCALING_APPLY_SIZE_ALIAS = PROPERTY_IMAGE_SCALING_ROOT_ALIAS + "Apply";
    public static final boolean PROPERTY_IMAGE_SCALING_APPLY_SIZE_DEFAULT = true;
    public static final Class PROPERTY_IMAGE_SCALING_APPLY_SIZE_TYPE = Boolean.class;

    public static final String PROPERTY_IMAGE_SCALING_SIZE_KEY = PROPERTY_IMAGE_SCALING_ROOT_KEY + ".size";
    public static final String PROPERTY_IMAGE_SCALING_SIZE_LABEL = "Image Scaling (%)";
    public static final String PROPERTY_IMAGE_SCALING_SIZE_TOOLTIP = "Percent to scale color bar relative to the scene image size";
    private static final String PROPERTY_IMAGE_SCALING_SIZE_ALIAS = PROPERTY_IMAGE_SCALING_ROOT_ALIAS + "Size";
    public static final double PROPERTY_IMAGE_SCALING_SIZE_DEFAULT = 50.0;
    public static final Class PROPERTY_IMAGE_SCALING_SIZE_TYPE = Double.class;

    public static final String PROPERTY_LEGEND_LENGTH_KEY = PROPERTY_IMAGE_SCALING_ROOT_KEY + ".legend.length";
    public static final String PROPERTY_LEGEND_LENGTH_LABEL = "Color Bar Length";
    public static final String PROPERTY_LEGEND_LENGTH_TOOLTIP = "Length in pixels of the color bar";
    private static final String PROPERTY_LEGEND_LENGTH_ALIAS = PROPERTY_IMAGE_SCALING_ROOT_ALIAS + "Legend Length";
    public static final int PROPERTY_LEGEND_LENGTH_DEFAULT = 1200;
    public static final Class PROPERTY_LEGEND_LENGTH_TYPE = Integer.class;

    public static final String PROPERTY_LEGEND_WIDTH_KEY = PROPERTY_IMAGE_SCALING_ROOT_KEY + ".legend.width";
    public static final String PROPERTY_LEGEND_WIDTH_LABEL = "Color Bar Width";
    public static final String PROPERTY_LEGEND_WIDTH_TOOLTIP = "Width in pixels of the color bar";
    private static final String PROPERTY_LEGEND_WIDTH_ALIAS = PROPERTY_IMAGE_SCALING_ROOT_ALIAS + "LegendWidth";
    public static final int PROPERTY_LEGEND_WIDTH_DEFAULT = 60;
    public static final Class PROPERTY_LEGEND_WIDTH_TYPE = Integer.class;


    



    // Title Parameter Section

    private static final String PROPERTY_TITLE_PARAMETER_ROOT_KEY = PROPERTY_ROOT_KEY + ".title.parameter";
    private static final String PROPERTY_TITLE_PARAMETER_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "TitleParameter";

    public static final String PROPERTY_TITLE_PARAMETER_SECTION_KEY = PROPERTY_TITLE_PARAMETER_ROOT_KEY + ".section";
    public static final String PROPERTY_TITLE_PARAMETER_SECTION_LABEL = "Title Parameter";
    public static final String PROPERTY_TITLE_PARAMETER_SECTION_TOOLTIP = "Set parameter options in title of color bar";
    public static final String PROPERTY_TITLE_PARAMETER_SECTION_ALIAS = PROPERTY_TITLE_PARAMETER_ROOT_ALIAS + "Section";

    public static final String PROPERTY_TITLE_PARAMETER_SHOW_KEY = PROPERTY_TITLE_PARAMETER_ROOT_KEY + ".show";
    public static final String PROPERTY_TITLE_PARAMETER_SHOW_LABEL = "Show";
    public static final String PROPERTY_TITLE_PARAMETER_SHOW_TOOLTIP = "Add title to the color bar";
    private static final String PROPERTY_TITLE_PARAMETER_SHOW_ALIAS = PROPERTY_TITLE_PARAMETER_ROOT_ALIAS + "Show";
    public static final boolean PROPERTY_TITLE_PARAMETER_SHOW_DEFAULT = true;
    public static final Class PROPERTY_TITLE_PARAMETER_SHOW_TYPE = Boolean.class;

    public static final String PROPERTY_TITLE_PARAMETER_TEXT_KEY = PROPERTY_TITLE_PARAMETER_ROOT_KEY + ".text";
    public static final String PROPERTY_TITLE_PARAMETER_TEXT_LABEL = "Text";
    public static final String PROPERTY_TITLE_PARAMETER_TEXT_TOOLTIP = "Add title parameter to the color bar";
    public static final String PROPERTY_TITLE_PARAMETER_TEXT_ALIAS = PROPERTY_TITLE_PARAMETER_ROOT_ALIAS + "Text";
    public static final String PROPERTY_TITLE_PARAMETER_TEXT_DEFAULT = "";
    public static final Class PROPERTY_TITLE_PARAMETER_TEXT_TYPE = String.class;

    public static final String PROPERTY_TITLE_PARAMETER_FONT_BOLD_KEY = PROPERTY_TITLE_PARAMETER_ROOT_KEY + ".font.bold";
    public static final String PROPERTY_TITLE_PARAMETER_FONT_BOLD_LABEL = "Font Bold";
    public static final String PROPERTY_TITLE_PARAMETER_FONT_BOLD_TOOLTIP = "Format title parameter text font in bold";
    public static final String PROPERTY_TITLE_PARAMETER_FONT_BOLD_ALIAS = PROPERTY_TITLE_PARAMETER_ROOT_ALIAS + "FontBold";
    public static final boolean PROPERTY_TITLE_PARAMETER_FONT_BOLD_DEFAULT = false;
    public static final Class PROPERTY_TITLE_PARAMETER_FONT_BOLD_TYPE = Boolean.class;

    public static final String PROPERTY_TITLE_PARAMETER_FONT_ITALIC_KEY = PROPERTY_TITLE_PARAMETER_ROOT_KEY + ".font.italic";
    public static final String PROPERTY_TITLE_PARAMETER_FONT_ITALIC_LABEL = "Font Italic";
    public static final String PROPERTY_TITLE_PARAMETER_FONT_ITALIC_TOOLTIP = "Format title parameter text font in italic";
    public static final String PROPERTY_TITLE_PARAMETER_FONT_ITALIC_ALIAS = PROPERTY_TITLE_PARAMETER_ROOT_ALIAS + "FontItalic";
    public static final boolean PROPERTY_TITLE_PARAMETER_FONT_ITALIC_DEFAULT = false;
    public static final Class PROPERTY_TITLE_PARAMETER_FONT_ITALIC_TYPE = Boolean.class;

    public static final String PROPERTY_TITLE_PARAMETER_FONT_NAME_KEY = PROPERTY_TITLE_PARAMETER_ROOT_KEY + ".font.name";
    public static final String PROPERTY_TITLE_PARAMETER_FONT_NAME_LABEL = "Font Name";
    public static final String PROPERTY_TITLE_PARAMETER_FONT_NAME_TOOLTIP = "Set the text font of the title parameter";
    public static final String PROPERTY_TITLE_PARAMETER_FONT_NAME_ALIAS = PROPERTY_TITLE_PARAMETER_ROOT_ALIAS + "FontName";
    public static final String PROPERTY_TITLE_PARAMETER_FONT_NAME_DEFAULT = FONT_NAME_VALUE_1;
    public static final Class PROPERTY_TITLE_PARAMETER_FONT_NAME_TYPE = String.class;
    public static final Object PROPERTY_TITLE_PARAMETER_FONT_NAME_VALUE_SET[] = FONT_NAME_VALUE_SET;


    public static final String PROPERTY_TITLE_PARAMETER_FONT_SIZE_KEY = PROPERTY_TITLE_PARAMETER_ROOT_KEY + "font.size";
    public static final String PROPERTY_TITLE_PARAMETER_FONT_SIZE_LABEL = "Title Size";
    public static final String PROPERTY_TITLE_PARAMETER_FONT_SIZE_TOOLTIP = "Set size of the title parameter";
    private static final String PROPERTY_TITLE_PARAMETER_FONT_SIZE_ALIAS = PROPERTY_TITLE_PARAMETER_ROOT_ALIAS + "FontSize";
    public static final int PROPERTY_TITLE_PARAMETER_FONT_SIZE_DEFAULT = 35;
    public static final Class PROPERTY_TITLE_PARAMETER_FONT_SIZE_TYPE = Integer.class;
    public static final int PROPERTY_TITLE_PARAMETER_FONT_SIZE_VALUE_MIN = 10;
    public static final int PROPERTY_TITLE_PARAMETER_FONT_SIZE_VALUE_MAX = 200;
    public static final String PROPERTY_TITLE_PARAMETER_FONT_SIZE_INTERVAL =
            "[" + ColorBarLayerType.PROPERTY_TITLE_PARAMETER_FONT_SIZE_VALUE_MIN +
                    "," + ColorBarLayerType.PROPERTY_TITLE_PARAMETER_FONT_SIZE_VALUE_MAX + "]";


    public static final String PROPERTY_TITLE_PARAMETER_COLOR_KEY = PROPERTY_TITLE_PARAMETER_ROOT_KEY + "font.color";
    public static final String PROPERTY_TITLE_PARAMETER_COLOR_LABEL = "Font Color";
    public static final String PROPERTY_TITLE_PARAMETER_COLOR_TOOLTIP = "Set color of the title";
    private static final String PROPERTY_TITLE_PARAMETER_COLOR_ALIAS = PROPERTY_TITLE_PARAMETER_ROOT_ALIAS + "FontColor";
    public static final Color PROPERTY_TITLE_PARAMETER_COLOR_DEFAULT = Color.YELLOW;
    public static final Class PROPERTY_TITLE_PARAMETER_COLOR_TYPE = Color.class;





    // Title Units Section

    private static final String PROPERTY_TITLE_UNITS_ROOT_KEY = PROPERTY_ROOT_KEY + ".title.units";
    private static final String PROPERTY_TITLE_UNITS_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "TitleUnits";

    public static final String PROPERTY_TITLE_UNITS_SECTION_KEY = PROPERTY_TITLE_UNITS_ROOT_KEY + ".section";
    public static final String PROPERTY_TITLE_UNITS_SECTION_LABEL = "Title Units";
    public static final String PROPERTY_TITLE_UNITS_SECTION_TOOLTIP = "Set title units of color bar";
    public static final String PROPERTY_TITLE_UNITS_SECTION_ALIAS = PROPERTY_TITLE_UNITS_ROOT_ALIAS + "Section";

    public static final String PROPERTY_TITLE_UNITS_SHOW_KEY = PROPERTY_TITLE_UNITS_ROOT_KEY + ".show";
    public static final String PROPERTY_TITLE_UNITS_SHOW_LABEL = "Show";
    public static final String PROPERTY_TITLE_UNITS_SHOW_TOOLTIP = "Add title units to the color bar";
    private static final String PROPERTY_TITLE_UNITS_SHOW_ALIAS = PROPERTY_TITLE_UNITS_ROOT_ALIAS + "Show";
    public static final boolean PROPERTY_TITLE_UNITS_SHOW_DEFAULT = true;
    public static final Class PROPERTY_TITLE_UNITS_SHOW_TYPE = Boolean.class;

    public static final String PROPERTY_TITLE_UNITS_TEXT_KEY = PROPERTY_TITLE_UNITS_ROOT_KEY + ".text";
    public static final String PROPERTY_TITLE_UNITS_TEXT_LABEL = "Text";
    public static final String PROPERTY_TITLE_UNITS_TEXT_TOOLTIP = "Add units to the title of the color bar";
    public static final String PROPERTY_TITLE_UNITS_TEXT_ALIAS = PROPERTY_TITLE_UNITS_ROOT_ALIAS + "Text";
    public static final String PROPERTY_TITLE_UNITS_TEXT_DEFAULT = "";
    public static final Class PROPERTY_TITLE_UNITS_TEXT_TYPE = String.class;

    public static final String PROPERTY_TITLE_UNITS_FONT_BOLD_KEY = PROPERTY_TITLE_UNITS_ROOT_KEY + ".font.bold";
    public static final String PROPERTY_TITLE_UNITS_FONT_BOLD_LABEL = "Font Bold";
    public static final String PROPERTY_TITLE_UNITS_FONT_BOLD_TOOLTIP = "Format title units text font in bold";
    public static final String PROPERTY_TITLE_UNITS_FONT_BOLD_ALIAS = PROPERTY_TITLE_UNITS_ROOT_ALIAS + "FontBold";
    public static final boolean PROPERTY_TITLE_UNITS_FONT_BOLD_DEFAULT = false;
    public static final Class PROPERTY_TITLE_UNITS_FONT_BOLD_TYPE = Boolean.class;

    public static final String PROPERTY_TITLE_UNITS_FONT_ITALIC_KEY = PROPERTY_TITLE_UNITS_ROOT_KEY + ".font.italic";
    public static final String PROPERTY_TITLE_UNITS_FONT_ITALIC_LABEL = "Font Italic";
    public static final String PROPERTY_TITLE_UNITS_FONT_ITALIC_TOOLTIP = "Format title units text font in italic";
    public static final String PROPERTY_TITLE_UNITS_FONT_ITALIC_ALIAS = PROPERTY_TITLE_UNITS_ROOT_ALIAS + "FontItalic";
    public static final boolean PROPERTY_TITLE_UNITS_FONT_ITALIC_DEFAULT = false;
    public static final Class PROPERTY_TITLE_UNITS_FONT_ITALIC_TYPE = Boolean.class;

    public static final String PROPERTY_TITLE_UNITS_FONT_NAME_KEY = PROPERTY_TITLE_UNITS_ROOT_KEY + ".font.name";
    public static final String PROPERTY_TITLE_UNITS_FONT_NAME_LABEL = "Font Name";
    public static final String PROPERTY_TITLE_UNITS_FONT_NAME_TOOLTIP = "Set the text font of the title units";
    public static final String PROPERTY_TITLE_UNITS_FONT_NAME_ALIAS = PROPERTY_TITLE_UNITS_ROOT_ALIAS + "FontName";
    public static final String PROPERTY_TITLE_UNITS_FONT_NAME_DEFAULT = FONT_NAME_VALUE_1;
    public static final Class PROPERTY_TITLE_UNITS_FONT_NAME_TYPE = String.class;
    public static final Object PROPERTY_TITLE_UNITS_FONT_NAME_VALUE_SET[] = FONT_NAME_VALUE_SET;

    public static final String PROPERTY_TITLE_UNITS_FONT_SIZE_KEY = PROPERTY_TITLE_UNITS_ROOT_KEY + ".font.size";
    public static final String PROPERTY_TITLE_UNITS_FONT_SIZE_LABEL = "Units Size";
    public static final String PROPERTY_TITLE_UNITS_FONT_SIZE_TOOLTIP = "Set size of the title units";
    private static final String PROPERTY_TITLE_UNITS_FONT_SIZE_ALIAS = PROPERTY_TITLE_UNITS_ROOT_ALIAS + "FontSize";
    public static final int PROPERTY_TITLE_UNITS_FONT_SIZE_DEFAULT = 35;
    public static final Class PROPERTY_TITLE_UNITS_FONT_SIZE_TYPE = Integer.class;
    public static final int PROPERTY_TITLE_UNITS_FONT_SIZE_VALUE_MIN = 10;
    public static final int PROPERTY_TITLE_UNITS_FONT_SIZE_VALUE_MAX = 200;
    public static final String PROPERTY_TITLE_UNITS_FONT_SIZE_INTERVAL =
            "[" + ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_VALUE_MIN +
                    "," + ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_VALUE_MAX + "]";

    public static final String PROPERTY_TITLE_UNITS_FONT_COLOR_KEY = PROPERTY_TITLE_UNITS_ROOT_KEY + ".font.color";
    public static final String PROPERTY_TITLE_UNITS_FONT_COLOR_LABEL = "Font Color";
    public static final String PROPERTY_TITLE_UNITS_FONT_COLOR_TOOLTIP = "Set color of the title units";
    private static final String PROPERTY_TITLE_UNITS_FONT_COLOR_ALIAS = PROPERTY_TITLE_UNITS_ROOT_ALIAS + "FontColor";
    public static final Color PROPERTY_TITLE_UNITS_FONT_COLOR_DEFAULT = Color.YELLOW;
    public static final Class PROPERTY_TITLE_UNITS_FONT_COLOR_TYPE = Color.class;






    // Tick-Mark Labels Section

    private static final String PROPERTY_LABELS_ROOT_KEY = PROPERTY_ROOT_KEY + ".labels";
    private static final String PROPERTY_LABELS_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "Labels";

    public static final String PROPERTY_LABELS_SECTION_KEY = PROPERTY_LABELS_ROOT_KEY + ".section";
    public static final String PROPERTY_LABELS_SECTION_LABEL = "Labels Disabled";
    public static final String PROPERTY_LABELS_SECTION_TOOLTIP = "Configuration options for the labels";
    public static final String PROPERTY_LABELS_SECTION_ALIAS = PROPERTY_LABELS_ROOT_ALIAS + "Section";

    public static final String PROPERTY_LABELS_SHOW_KEY = PROPERTY_LABELS_ROOT_KEY + ".show";
    public static final String PROPERTY_LABELS_SHOW_LABEL = "Show";
    public static final String PROPERTY_LABELS_SHOW_TOOLTIP = "Show the tick-mark labels";
    private static final String PROPERTY_LABELS_SHOW_ALIAS = PROPERTY_LABELS_ROOT_ALIAS + "Show";
    public static final boolean PROPERTY_LABELS_SHOW_DEFAULT = true;
    public static final Class PROPERTY_LABELS_SHOW_TYPE = Boolean.class;

    public static final String PROPERTY_LABELS_FONT_BOLD_KEY = PROPERTY_LABELS_ROOT_KEY + ".font.bold";
    public static final String PROPERTY_LABELS_FONT_BOLD_LABEL = "Font Bold";
    public static final String PROPERTY_LABELS_FONT_BOLD_TOOLTIP = "Format tick-mark label text font in bold";
    public static final String PROPERTY_LABELS_FONT_BOLD_ALIAS = PROPERTY_LABELS_ROOT_ALIAS + "FontBold";
    public static final boolean PROPERTY_LABELS_FONT_BOLD_DEFAULT = false;
    public static final Class PROPERTY_LABELS_FONT_BOLD_TYPE = Boolean.class;

    public static final String PROPERTY_LABELS_FONT_ITALIC_KEY = PROPERTY_LABELS_ROOT_KEY + ".font.italic";
    public static final String PROPERTY_LABELS_FONT_ITALIC_LABEL = "Font Italic";
    public static final String PROPERTY_LABELS_FONT_ITALIC_TOOLTIP = "Format tick-mark label text font in italic";
    public static final String PROPERTY_LABELS_FONT_ITALIC_ALIAS = PROPERTY_LABELS_ROOT_ALIAS + "FontItalic";
    public static final boolean PROPERTY_LABELS_FONT_ITALIC_DEFAULT = false;
    public static final Class PROPERTY_LABELS_FONT_ITALIC_TYPE = Boolean.class;

    public static final String PROPERTY_LABELS_FONT_NAME_KEY = PROPERTY_LABELS_ROOT_KEY + ".font.name";
    public static final String PROPERTY_LABELS_FONT_NAME_LABEL = "Font Name";
    public static final String PROPERTY_LABELS_FONT_NAME_TOOLTIP = "Set the font of the tick-mark labels";
    public static final String PROPERTY_LABELS_FONT_NAME_ALIAS = PROPERTY_LABELS_ROOT_ALIAS + "FontName";
    public static final String PROPERTY_LABELS_FONT_NAME_DEFAULT = FONT_NAME_VALUE_1;
    public static final Class PROPERTY_LABELS_FONT_NAME_TYPE = String.class;
    public static final Object PROPERTY_LABELS_FONT_NAME_VALUE_SET[] = FONT_NAME_VALUE_SET;

    public static final String PROPERTY_LABELS_FONT_SIZE_KEY = PROPERTY_LABELS_ROOT_KEY + ".font.size";
    public static final String PROPERTY_LABELS_FONT_SIZE_LABEL = "Labels Size";
    public static final String PROPERTY_LABELS_FONT_SIZE_TOOLTIP = "Set the size of the tick-mark labels";
    private static final String PROPERTY_LABELS_FONT_SIZE_ALIAS = PROPERTY_LABELS_ROOT_ALIAS + "FontSize";
    public static final int PROPERTY_LABELS_FONT_SIZE_DEFAULT = 35;
    public static final Class PROPERTY_LABELS_FONT_SIZE_TYPE = Integer.class;
    public static final int PROPERTY_LABELS_FONT_SIZE_VALUE_MIN = 10;
    public static final int PROPERTY_LABELS_FONT_SIZE_VALUE_MAX = 200;
    public static final String PROPERTY_LABELS_FONT_SIZE_VALUE_INTERVAL = "[" + ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_VALUE_MIN + "," + ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_VALUE_MAX + "]";

    public static final String PROPERTY_LABELS_FONT_COLOR_KEY = PROPERTY_LABELS_ROOT_KEY + ".font.color";
    public static final String PROPERTY_LABELS_FONT_COLOR_LABEL = "Font Color";
    public static final String PROPERTY_LABELS_FONT_COLOR_TOOLTIP = "Set the color of the tick-mark labels";
    private static final String PROPERTY_LABELS_FONT_COLOR_ALIAS = PROPERTY_LABELS_ROOT_ALIAS + "FontColor";
    public static final Color PROPERTY_LABELS_FONT_COLOR_DEFAULT = Color.YELLOW;
    public static final Class PROPERTY_LABELS_FONT_COLOR_TYPE = Color.class;









    // Tickmarks Section

    private static final String PROPERTY_TICKMARKS_ROOT_KEY = PROPERTY_ROOT_KEY + ".tickmarks";
    private static final String PROPERTY_TICKMARKS_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "TickMarks";

    public static final String PROPERTY_TICKMARKS_SECTION_KEY = PROPERTY_TICKMARKS_ROOT_KEY + ".section";
    public static final String PROPERTY_TICKMARKS_SECTION_LABEL = "Tick Marks";
    public static final String PROPERTY_TICKMARKS_SECTION_TOOLTIP = "Format options for the color bar legend tickmarks";
    public static final String PROPERTY_TICKMARKS_SECTION_ALIAS = PROPERTY_TICKMARKS_ROOT_ALIAS + "Section";

    public static final String PROPERTY_TICKMARKS_SHOW_KEY = PROPERTY_TICKMARKS_ROOT_KEY + ".show";
    public static final String PROPERTY_TICKMARKS_SHOW_LABEL = "Show";
    public static final String PROPERTY_TICKMARKS_SHOW_TOOLTIP = "Display tickmarks";
    public static final String PROPERTY_TICKMARKS_SHOW_ALIAS = PROPERTY_TICKMARKS_ROOT_ALIAS + "Show";
    public static final boolean PROPERTY_TICKMARKS_SHOW_DEFAULT = true;
    public static final Class PROPERTY_TICKMARKS_SHOW_TYPE = Boolean.class;

    public static final String PROPERTY_TICKMARKS_COLOR_KEY = PROPERTY_TICKMARKS_ROOT_KEY + ".color";
    public static final String PROPERTY_TICKMARKS_COLOR_LABEL = "Tickmark Color";
    public static final String PROPERTY_TICKMARKS_COLOR_TOOLTIP = "Set color of the tickmarks";
    private static final String PROPERTY_TICKMARKS_COLOR_ALIAS = PROPERTY_TICKMARKS_ROOT_ALIAS + "Color";
    public static final Color PROPERTY_TICKMARKS_COLOR_DEFAULT = Color.YELLOW;
    public static final Class PROPERTY_TICKMARKS_COLOR_TYPE = Color.class;

    public static final String PROPERTY_TICKMARKS_LENGTH_KEY = PROPERTY_TICKMARKS_ROOT_KEY + ".length";
    public static final String PROPERTY_TICKMARKS_LENGTH_LABEL = "Length";
    public static final String PROPERTY_TICKMARKS_LENGTH_TOOLTIP = "Set length of tickmarks";
    public static final String PROPERTY_TICKMARKS_LENGTH_ALIAS = PROPERTY_TICKMARKS_ROOT_ALIAS + "Length";
    public static final int PROPERTY_TICKMARKS_LENGTH_DEFAULT = 12;
    public static final Class PROPERTY_TICKMARKS_LENGTH_TYPE = Integer.class;

    public static final String PROPERTY_TICKMARKS_WIDTH_KEY = PROPERTY_TICKMARKS_ROOT_KEY + ".width";
    public static final String PROPERTY_TICKMARKS_WIDTH_LABEL = "Width";
    public static final String PROPERTY_TICKMARKS_WIDTH_TOOLTIP = "Set width of tickmarks";
    public static final String PROPERTY_TICKMARKS_WIDTH_ALIAS = PROPERTY_TICKMARKS_ROOT_ALIAS + "Width";
    public static final int PROPERTY_TICKMARKS_WIDTH_DEFAULT = 4;
    public static final Class PROPERTY_TICKMARKS_WIDTH_TYPE = Integer.class;



    // Border Section

    private static final String PROPERTY_BORDER_ROOT_KEY = PROPERTY_ROOT_KEY + ".border";
    private static final String PROPERTY_BORDER_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "Border";

    public static final String PROPERTY_BORDER_SECTION_KEY = PROPERTY_BORDER_ROOT_KEY + ".section";
    public static final String PROPERTY_BORDER_SECTION_ALIAS = PROPERTY_BORDER_ROOT_ALIAS + "Section";
    public static final String PROPERTY_BORDER_SECTION_LABEL = "Border";
    public static final String PROPERTY_BORDER_SECTION_TOOLTIP = "Configuration options for adding a border around the data image";

    public static final String PROPERTY_BORDER_SHOW_KEY = PROPERTY_BORDER_ROOT_KEY + ".show";
    public static final String PROPERTY_BORDER_SHOW_LABEL = "Show";
    public static final String PROPERTY_BORDER_SHOW_TOOLTIP = "Display a border around the data image";
    private static final String PROPERTY_BORDER_SHOW_ALIAS = PROPERTY_BORDER_ROOT_ALIAS + "Show";
    public static final boolean PROPERTY_BORDER_SHOW_DEFAULT = true;
    public static final Class PROPERTY_BORDER_SHOW_TYPE = Boolean.class;

    public static final String PROPERTY_BORDER_WIDTH_KEY = PROPERTY_BORDER_ROOT_KEY + ".width";
    public static final String PROPERTY_BORDER_WIDTH_LABEL = "Width";
    public static final String PROPERTY_BORDER_WIDTH_TOOLTIP = "Width of border line";
    private static final String PROPERTY_BORDER_WIDTH_ALIAS = PROPERTY_BORDER_ROOT_ALIAS + "Width";
    public static final int PROPERTY_BORDER_WIDTH_DEFAULT = 1;
    public static final Class PROPERTY_BORDER_WIDTH_TYPE = Integer.class;

    public static final String PROPERTY_BORDER_COLOR_KEY = PROPERTY_BORDER_ROOT_KEY + ".color";
    public static final String PROPERTY_BORDER_COLOR_LABEL = "Color";
    public static final String PROPERTY_BORDER_COLOR_TOOLTIP = "Color of border line";
    private static final String PROPERTY_BORDER_COLOR_ALIAS = PROPERTY_BORDER_ROOT_ALIAS + "Color";
    public static final Color PROPERTY_BORDER_COLOR_DEFAULT = Color.BLACK;
    public static final Class PROPERTY_BORDER_COLOR_TYPE = Color.class;




    // Backdrop Section

    private static final String PROPERTY_BACKDROP_ROOT_KEY = PROPERTY_ROOT_KEY + ".backdrop";
    private static final String PROPERTY_BACKDROP_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "Backdrop";

    public static final String PROPERTY_BACKDROP_SECTION_KEY = PROPERTY_BACKDROP_ROOT_KEY + ".section";
    public static final String PROPERTY_BACKDROP_SECTION_ALIAS = PROPERTY_BACKDROP_ROOT_ALIAS + "Section";
    public static final String PROPERTY_BACKDROP_SECTION_LABEL = "Backdrop";
    public static final String PROPERTY_BACKDROP_SECTION_TOOLTIP = "Configuration options for the color bar legend backdrop";

    public static final String PROPERTY_BACKDROP_SHOW_KEY = PROPERTY_BACKDROP_ROOT_KEY + ".show";
    public static final String PROPERTY_BACKDROP_SHOW_LABEL = "Show";
    public static final String PROPERTY_BACKDROP_SHOW_TOOLTIP = "Show the color bar legend backdrop";
    private static final String PROPERTY_BACKDROP_SHOW_ALIAS = PROPERTY_BACKDROP_ROOT_ALIAS + "Show";
    public static final boolean PROPERTY_BACKDROP_SHOW_DEFAULT = true;
    public static final Class PROPERTY_BACKDROP_SHOW_TYPE = Boolean.class;

    public static final String PROPERTY_BACKDROP_COLOR_KEY = PROPERTY_BACKDROP_ROOT_KEY + ".color";
    public static final String PROPERTY_BACKDROP_COLOR_LABEL = "Color";
    public static final String PROPERTY_BACKDROP_COLOR_TOOLTIP = "Set color of the backdrop of the color bar legend backdrop";
    private static final String PROPERTY_BACKDROP_COLOR_ALIAS = PROPERTY_BACKDROP_ROOT_ALIAS + "Color";
    public static final Color PROPERTY_BACKDROP_COLOR_DEFAULT = Color.BLACK;
    public static final Class PROPERTY_BACKDROP_COLOR_TYPE = Color.class;

    public static final String PROPERTY_BACKDROP_TRANSPARENCY_KEY = PROPERTY_BACKDROP_ROOT_KEY + "transparency";
    public static final String PROPERTY_BACKDROP_TRANSPARENCY_LABEL = "Transparency";
    public static final String PROPERTY_BACKDROP_TRANSPARENCY_TOOLTIP = "Set transparency of the color bar legend backdrop";
    private static final String PROPERTY_BACKDROP_TRANSPARENCY_ALIAS = PROPERTY_BACKDROP_ROOT_ALIAS + "Transparency";
    public static final double PROPERTY_BACKDROP_TRANSPARENCY_DEFAULT = 0.5;
    public static final Class PROPERTY_BACKDROP_TRANSPARENCY_TYPE = Double.class;








    // ---------------------------------------------------------

    public static final String PROPERTY_NAME_RASTER = "raster";




    // Property Setting: Restore Defaults
    public static final String PROPERTY_RESTORE_DEFAULTS_NAME = "colorbar.restoreDefaults";
    public static final String PROPERTY_RESTORE_TO_DEFAULTS_LABEL = "RESTORE DEFAULTS (Color Bar Legend Preferences)";
    public static final String PROPERTY_RESTORE_TO_DEFAULTS_TOOLTIP = "Restore all color bar legend preferences to the default";
    public static final boolean PROPERTY_RESTORE_TO_DEFAULTS_DEFAULT = false;


    /**
     * @deprecated since BEAM 4.7, no replacement; kept for compatibility of sessions
     */
    @Deprecated
    private static final String PROPERTY_NAME_TRANSFORM = "imageToModelTransform";


    @Override
    public boolean isValidFor(LayerContext ctx) {
        return true;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        return new ColorBarLayer(this, (RasterDataNode) configuration.getValue(PROPERTY_NAME_RASTER),
                configuration);
    }

    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertyContainer vc = new PropertyContainer();


        // Label Values

        final Property labelValuesSectionModel = Property.create(PROPERTY_LABEL_VALUES_SECTION_KEY, Boolean.class, true, true);
        labelValuesSectionModel.getDescriptor().setAlias(PROPERTY_LABEL_VALUES_SECTION_ALIAS);
        vc.addProperty(labelValuesSectionModel);

        final Property labelValuesModeModel = Property.create(PROPERTY_LABEL_VALUES_MODE_KEY, String.class, PROPERTY_LABEL_VALUES_MODE_DEFAULT, true);
        labelValuesModeModel.getDescriptor().setAlias(PROPERTY_LABEL_VALUES_MODE_ALIAS);
        vc.addProperty(labelValuesModeModel);


        final Property labelValuesCountModel = Property.create(PROPERTY_LABEL_VALUES_COUNT_KEY, Integer.class, PROPERTY_LABEL_VALUES_COUNT_DEFAULT, true);
        labelValuesCountModel.getDescriptor().setAlias(PROPERTY_LABEL_VALUES_COUNT_ALIAS);
        vc.addProperty(labelValuesCountModel);

        final Property labelValuesActualModel = Property.create(PROPERTY_LABEL_VALUES_ACTUAL_KEY, String.class, PROPERTY_LABEL_VALUES_ACTUAL_DEFAULT, true);
        labelValuesActualModel.getDescriptor().setAlias(PROPERTY_LABEL_VALUES_ACTUAL_ALIAS);
        vc.addProperty(labelValuesActualModel);

        final Property labelValuesScalingFactorModel = Property.create(PROPERTY_LABEL_VALUES_SCALING_KEY, Double.class, PROPERTY_LABEL_VALUES_SCALING_DEFAULT, true);
        labelValuesScalingFactorModel.getDescriptor().setAlias(PROPERTY_LABEL_VALUES_SCALING_ALIAS);
        vc.addProperty(labelValuesScalingFactorModel);

        final Property labelValuesDecimalPlacesModel = Property.create(PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY, Integer.class, PROPERTY_LABEL_VALUES_DECIMAL_PLACES_DEFAULT, true);
        labelValuesDecimalPlacesModel.getDescriptor().setAlias(PROPERTY_LABEL_VALUES_DECIMAL_PLACES_ALIAS);
        vc.addProperty(labelValuesDecimalPlacesModel);

        final Property labelValuesForceDecimalPlacesModel = Property.create(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY, Boolean.class, PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_DEFAULT , true);
        labelValuesForceDecimalPlacesModel.getDescriptor().setAlias(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_ALIAS);
        vc.addProperty(labelValuesForceDecimalPlacesModel);








        // Formatting Section

        final Property formattingSectionModel = Property.create(PROPERTY_FORMATTING_SECTION_KEY, Boolean.class, true, true);
        formattingSectionModel.getDescriptor().setAlias(PROPERTY_FORMATTING_SECTION_ALIAS);
        vc.addProperty(formattingSectionModel);

        final Property formattingOrientationModel = Property.create(PROPERTY_FORMATTING_ORIENTATION_KEY, PROPERTY_FORMATTING_ORIENTATION_TYPE, true, true);
        formattingOrientationModel.getDescriptor().setAlias(PROPERTY_FORMATTING_ORIENTATION_ALIAS);
        vc.addProperty(formattingOrientationModel);

        final Property reversePaletteModel = Property.create(PROPERTY_FORMATTING_REVERSE_PALETTE_KEY, PROPERTY_FORMATTING_REVERSE_PALETTE_TYPE, true, true);
        reversePaletteModel.getDescriptor().setAlias(PROPERTY_FORMATTING_REVERSE_PALETTE_ALIAS);
        vc.addProperty(reversePaletteModel);








        // ColorBar Location Section

        final Property locationSectionModel = Property.create(PROPERTY_LOCATION_SECTION_KEY, Boolean.class, true, true);
        locationSectionModel.getDescriptor().setAlias(PROPERTY_LOCATION_SECTION_ALIAS);
        vc.addProperty(locationSectionModel);

        final Property locationInsideModel = Property.create(PROPERTY_LOCATION_INSIDE_KEY, PROPERTY_LOCATION_INSIDE_TYPE, true, true);
        locationInsideModel.getDescriptor().setAlias(PROPERTY_LOCATION_INSIDE_ALIAS);
        vc.addProperty(locationInsideModel);

        final Property locationEdgeModel = Property.create(PROPERTY_LOCATION_PLACEMENT_KEY,
                PROPERTY_LOCATION_PLACEMENT_TYPE, true, true);
        locationEdgeModel.getDescriptor().setAlias(PROPERTY_LOCATION_PLACEMENT_ALIAS);
        vc.addProperty(locationEdgeModel);

        final Property titleVerticalAnchorModel = Property.create(PROPERTY_TITLE_VERTICAL_LOCATION_KEY,
                PROPERTY_TITLE_VERTICAL_LOCATION_TYPE, true, true);
        titleVerticalAnchorModel.getDescriptor().setAlias(PROPERTY_TITLE_VERTICAL_LOCATION_ALIAS);
        vc.addProperty(titleVerticalAnchorModel);





        final Property locationOffsetModel = Property.create(PROPERTY_LOCATION_OFFSET_KEY, PROPERTY_LOCATION_OFFSET_TYPE, true, true);
        locationOffsetModel.getDescriptor().setAlias(PROPERTY_LOCATION_OFFSET_ALIAS);
        vc.addProperty(locationOffsetModel);

        final Property locationShiftModel = Property.create(PROPERTY_LOCATION_SHIFT_KEY, PROPERTY_LOCATION_SHIFT_TYPE, true, true);
        locationShiftModel.getDescriptor().setAlias(PROPERTY_LOCATION_SHIFT_ALIAS);
        vc.addProperty(locationShiftModel);







        final Property scalingSectionModel = Property.create(PROPERTY_IMAGE_SCALING_SECTION_NAME, Boolean.class, true, true);
        scalingSectionModel.getDescriptor().setAlias(PROPERTY_IMAGE_SCALING_SECTION_ALIAS);
        vc.addProperty(scalingSectionModel);

        final Property locationApplySizeScalingModel = Property.create(PROPERTY_IMAGE_SCALING_APPLY_SIZE_NAME, PROPERTY_IMAGE_SCALING_APPLY_SIZE_TYPE, true, true);
        locationApplySizeScalingModel.getDescriptor().setAlias(PROPERTY_IMAGE_SCALING_APPLY_SIZE_ALIAS);
        vc.addProperty(locationApplySizeScalingModel);

        final Property locationSizeScalingModel = Property.create(PROPERTY_IMAGE_SCALING_SIZE_KEY, ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_TYPE, true, true);
        locationSizeScalingModel.getDescriptor().setAlias(PROPERTY_IMAGE_SCALING_SIZE_ALIAS);
        vc.addProperty(locationSizeScalingModel);

        final Property legendLengthModel = Property.create(PROPERTY_LEGEND_LENGTH_KEY, ColorBarLayerType.PROPERTY_LEGEND_LENGTH_TYPE, true, true);
        legendLengthModel.getDescriptor().setAlias(PROPERTY_LEGEND_LENGTH_ALIAS);
        vc.addProperty(legendLengthModel);

        final Property legendWidthModel = Property.create(PROPERTY_LEGEND_WIDTH_KEY, ColorBarLayerType.PROPERTY_LEGEND_WIDTH_TYPE, true, true);
        legendWidthModel.getDescriptor().setAlias(PROPERTY_LEGEND_WIDTH_ALIAS);
        vc.addProperty(legendWidthModel);









        // Title Parameter Section

        final Property titleParameterSectionModel = Property.create(PROPERTY_TITLE_PARAMETER_SECTION_KEY,
                Boolean.class,
                true,
                true);
        titleParameterSectionModel.getDescriptor().setAlias(PROPERTY_TITLE_PARAMETER_SECTION_ALIAS);
        vc.addProperty(titleParameterSectionModel);


        final Property titleParameterShowModel = Property.create(PROPERTY_TITLE_PARAMETER_SHOW_KEY,
                PROPERTY_TITLE_PARAMETER_SHOW_TYPE,
                PROPERTY_TITLE_PARAMETER_SHOW_DEFAULT,
                true);
        titleParameterShowModel.getDescriptor().setAlias(PROPERTY_TITLE_PARAMETER_SHOW_ALIAS);
        vc.addProperty(titleParameterShowModel);


        final Property titleParameterTextModel = Property.create(PROPERTY_TITLE_PARAMETER_TEXT_KEY,
                PROPERTY_TITLE_PARAMETER_TEXT_TYPE,
                PROPERTY_TITLE_PARAMETER_TEXT_DEFAULT,
                true);
        titleParameterTextModel.getDescriptor().setAlias(PROPERTY_TITLE_PARAMETER_TEXT_ALIAS);
        vc.addProperty(titleParameterTextModel);


        final Property titleParameterBoldModel = Property.create(PROPERTY_TITLE_PARAMETER_FONT_BOLD_KEY,
                PROPERTY_TITLE_PARAMETER_FONT_BOLD_TYPE,
                PROPERTY_TITLE_PARAMETER_FONT_BOLD_DEFAULT,
                true);
        titleParameterBoldModel.getDescriptor().setAlias(PROPERTY_TITLE_PARAMETER_FONT_BOLD_ALIAS);
        vc.addProperty(titleParameterBoldModel);


        final Property titleParameterItalicModel = Property.create(PROPERTY_TITLE_PARAMETER_FONT_ITALIC_KEY,
                PROPERTY_TITLE_PARAMETER_FONT_ITALIC_TYPE,
                PROPERTY_TITLE_PARAMETER_FONT_ITALIC_DEFAULT,
                true);
        titleParameterItalicModel.getDescriptor().setAlias(PROPERTY_TITLE_PARAMETER_FONT_ITALIC_ALIAS);
        vc.addProperty(titleParameterItalicModel);


        final Property titleParameterFontNameModel = Property.create(PROPERTY_TITLE_PARAMETER_FONT_NAME_KEY,
                PROPERTY_TITLE_PARAMETER_FONT_NAME_TYPE,
                PROPERTY_TITLE_PARAMETER_FONT_NAME_DEFAULT,
                true);
        titleParameterFontNameModel.getDescriptor().setAlias(PROPERTY_TITLE_PARAMETER_FONT_NAME_ALIAS);
        vc.addProperty(titleParameterFontNameModel);


        final Property titleParameterColorModel = Property.create(PROPERTY_TITLE_PARAMETER_COLOR_KEY,
                PROPERTY_TITLE_PARAMETER_COLOR_TYPE,
                PROPERTY_TITLE_PARAMETER_COLOR_DEFAULT,
                true);
        titleParameterColorModel.getDescriptor().setAlias(PROPERTY_TITLE_PARAMETER_COLOR_ALIAS);
        vc.addProperty(titleParameterColorModel);


        final Property titleParameterFontSizeModel = Property.create(PROPERTY_TITLE_PARAMETER_FONT_SIZE_KEY,
                PROPERTY_TITLE_PARAMETER_FONT_SIZE_TYPE,
                PROPERTY_TITLE_PARAMETER_FONT_SIZE_DEFAULT,
                true);
        titleParameterFontSizeModel.getDescriptor().setAlias(PROPERTY_TITLE_PARAMETER_FONT_SIZE_ALIAS);
        vc.addProperty(titleParameterFontSizeModel);





        // Title Units Section

        final Property titleUnitsSectionModel = Property.create(PROPERTY_TITLE_UNITS_SECTION_KEY,
                Boolean.class,
                true,
                true);
        titleUnitsSectionModel.getDescriptor().setAlias(PROPERTY_TITLE_UNITS_SECTION_ALIAS);
        vc.addProperty(titleUnitsSectionModel);


        final Property titleUnitsShowModel = Property.create(PROPERTY_TITLE_UNITS_SHOW_KEY,
                PROPERTY_TITLE_UNITS_SHOW_TYPE,
                PROPERTY_TITLE_UNITS_SHOW_DEFAULT,
                true);
        titleUnitsShowModel.getDescriptor().setAlias(PROPERTY_TITLE_UNITS_SHOW_ALIAS);
        vc.addProperty(titleUnitsShowModel);


        final Property titleUnitsTextModel = Property.create(PROPERTY_TITLE_UNITS_TEXT_KEY,
                PROPERTY_TITLE_UNITS_TEXT_TYPE,
                PROPERTY_TITLE_UNITS_TEXT_DEFAULT,
                true);
        titleUnitsTextModel.getDescriptor().setAlias(PROPERTY_TITLE_UNITS_TEXT_ALIAS);
        vc.addProperty(titleUnitsTextModel);


        final Property titleUnitsBoldModel = Property.create(PROPERTY_TITLE_UNITS_FONT_BOLD_KEY,
                PROPERTY_TITLE_UNITS_FONT_BOLD_TYPE,
                PROPERTY_TITLE_UNITS_FONT_BOLD_DEFAULT,
                true);
        titleUnitsBoldModel.getDescriptor().setAlias(PROPERTY_TITLE_UNITS_FONT_BOLD_ALIAS);
        vc.addProperty(titleUnitsBoldModel);


        final Property titleUnitsItalicModel = Property.create(PROPERTY_TITLE_UNITS_FONT_ITALIC_KEY,
                PROPERTY_TITLE_UNITS_FONT_ITALIC_TYPE,
                PROPERTY_TITLE_UNITS_FONT_ITALIC_DEFAULT,
                true);
        titleUnitsItalicModel.getDescriptor().setAlias(PROPERTY_TITLE_UNITS_FONT_ITALIC_ALIAS);
        vc.addProperty(titleUnitsItalicModel);


        final Property titleUnitsFontNameModel = Property.create(PROPERTY_TITLE_UNITS_FONT_NAME_KEY,
                PROPERTY_TITLE_UNITS_FONT_NAME_TYPE,
                PROPERTY_TITLE_UNITS_FONT_NAME_DEFAULT,
                true);
        titleUnitsFontNameModel.getDescriptor().setAlias(PROPERTY_TITLE_UNITS_FONT_NAME_ALIAS);
        vc.addProperty(titleUnitsFontNameModel);


        final Property titleUnitsColorModel = Property.create(PROPERTY_TITLE_UNITS_FONT_COLOR_KEY,
                PROPERTY_TITLE_UNITS_FONT_COLOR_TYPE,
                PROPERTY_TITLE_UNITS_FONT_COLOR_DEFAULT,
                true);
        titleUnitsColorModel.getDescriptor().setAlias(PROPERTY_TITLE_UNITS_FONT_COLOR_ALIAS);
        vc.addProperty(titleUnitsColorModel);


        final Property titleUnitsFontSizeModel = Property.create(PROPERTY_TITLE_UNITS_FONT_SIZE_KEY,
                PROPERTY_TITLE_UNITS_FONT_SIZE_TYPE,
                PROPERTY_TITLE_UNITS_FONT_SIZE_DEFAULT,
                true);
        titleUnitsFontSizeModel.getDescriptor().setAlias(PROPERTY_TITLE_UNITS_FONT_SIZE_ALIAS);
        vc.addProperty(titleUnitsFontSizeModel);





















        // Tickmarks Section

        final Property tickmarksSectionModel = Property.create(PROPERTY_TICKMARKS_SECTION_KEY, Boolean.class, true, true);
        tickmarksSectionModel.getDescriptor().setAlias(PROPERTY_TICKMARKS_SECTION_ALIAS);
        vc.addProperty(tickmarksSectionModel);

        final Property tickMarkEnabledModel = Property.create(PROPERTY_TICKMARKS_SHOW_KEY, PROPERTY_TICKMARKS_SHOW_TYPE, PROPERTY_TICKMARKS_SHOW_DEFAULT, true);
        tickMarkEnabledModel.getDescriptor().setAlias(PROPERTY_TICKMARKS_SHOW_ALIAS);
        vc.addProperty(tickMarkEnabledModel);

        final Property tickmarkColorModel = Property.create(PROPERTY_TICKMARKS_COLOR_KEY, PROPERTY_TICKMARKS_COLOR_TYPE, PROPERTY_TICKMARKS_COLOR_DEFAULT, true);
        tickmarkColorModel.getDescriptor().setAlias(PROPERTY_TICKMARKS_COLOR_ALIAS);
        vc.addProperty(tickmarkColorModel);

        final Property tickMarkLengthModel = Property.create(PROPERTY_TICKMARKS_LENGTH_KEY, PROPERTY_TICKMARKS_LENGTH_TYPE, PROPERTY_TICKMARKS_LENGTH_DEFAULT, true);
        tickMarkLengthModel.getDescriptor().setAlias(PROPERTY_TICKMARKS_LENGTH_ALIAS);
        vc.addProperty(tickMarkLengthModel);

        final Property tickMarkWidthModel = Property.create(PROPERTY_TICKMARKS_WIDTH_KEY, PROPERTY_TICKMARKS_WIDTH_TYPE, PROPERTY_TICKMARKS_LENGTH_DEFAULT, true);
        tickMarkWidthModel.getDescriptor().setAlias(PROPERTY_TICKMARKS_WIDTH_ALIAS);
        vc.addProperty(tickMarkWidthModel);



        // Border Section

        final Property borderSectionModel = Property.create(PROPERTY_BORDER_SECTION_KEY, Boolean.class, true, true);
        borderSectionModel.getDescriptor().setAlias(PROPERTY_BORDER_SECTION_ALIAS);
        vc.addProperty(borderSectionModel);

        final Property borderEnabledModel = Property.create(PROPERTY_BORDER_SHOW_KEY, Boolean.class, PROPERTY_BORDER_SHOW_DEFAULT, true);
        borderEnabledModel.getDescriptor().setAlias(PROPERTY_BORDER_SHOW_ALIAS);
        vc.addProperty(borderEnabledModel);

        final Property borderWidthModel = Property.create(PROPERTY_BORDER_WIDTH_KEY, PROPERTY_BORDER_WIDTH_TYPE, PROPERTY_BORDER_WIDTH_DEFAULT, true);
        borderWidthModel.getDescriptor().setAlias(PROPERTY_BORDER_WIDTH_ALIAS);
        vc.addProperty(borderWidthModel);

        final Property borderColorModel = Property.create(PROPERTY_BORDER_COLOR_KEY, Color.class, PROPERTY_BORDER_COLOR_DEFAULT, true);
        borderColorModel.getDescriptor().setAlias(PROPERTY_BORDER_COLOR_ALIAS);
        vc.addProperty(borderColorModel);




        // Backdrop Section

        final Property insideLabelsSectionModel = Property.create(PROPERTY_BACKDROP_SECTION_KEY, Boolean.class, true, true);
        insideLabelsSectionModel.getDescriptor().setAlias(PROPERTY_BACKDROP_SECTION_ALIAS);
        vc.addProperty(insideLabelsSectionModel);

        final Property backdropShowModel = Property.create(PROPERTY_BACKDROP_SHOW_KEY, Boolean.class, PROPERTY_BACKDROP_SHOW_DEFAULT, true);
        backdropShowModel.getDescriptor().setAlias(PROPERTY_BACKDROP_SHOW_ALIAS);
        vc.addProperty(backdropShowModel);

        final Property backdropColorModel = Property.create(PROPERTY_BACKDROP_COLOR_KEY, Color.class, PROPERTY_BACKDROP_COLOR_DEFAULT, true);
        backdropColorModel.getDescriptor().setAlias(PROPERTY_BACKDROP_COLOR_ALIAS);
        vc.addProperty(backdropColorModel);

        final Property backdropTransparencyModel = Property.create(PROPERTY_BACKDROP_TRANSPARENCY_KEY, Double.class, PROPERTY_BACKDROP_TRANSPARENCY_DEFAULT, true);
        backdropTransparencyModel.getDescriptor().setAlias(PROPERTY_BACKDROP_TRANSPARENCY_ALIAS);
        vc.addProperty(backdropTransparencyModel);











        final Property rasterModel = Property.create(PROPERTY_NAME_RASTER, RasterDataNode.class);
        rasterModel.getDescriptor().setNotNull(true);
        vc.addProperty(rasterModel);

        final Property transformModel = Property.create(PROPERTY_NAME_TRANSFORM, new AffineTransform());
        transformModel.getDescriptor().setTransient(true);
        vc.addProperty(transformModel);










        // Labels Section

        final Property labelsSectionModel = Property.create(PROPERTY_LABELS_SECTION_KEY, Boolean.class, true, true);
        labelsSectionModel.getDescriptor().setAlias(PROPERTY_LABELS_SECTION_ALIAS);
        vc.addProperty(labelsSectionModel);


        final Property labelsShowModel = Property.create(PROPERTY_LABELS_SHOW_KEY,
                PROPERTY_LABELS_SHOW_TYPE,
                PROPERTY_LABELS_SHOW_DEFAULT,
                true);
        labelsShowModel.getDescriptor().setAlias(PROPERTY_LABELS_SHOW_ALIAS);
        vc.addProperty(labelsShowModel);


        final Property textFgColorModel = Property.create(PROPERTY_LABELS_FONT_COLOR_KEY, Color.class, PROPERTY_LABELS_FONT_COLOR_DEFAULT, true);
        textFgColorModel.getDescriptor().setAlias(PROPERTY_LABELS_FONT_COLOR_ALIAS);
        vc.addProperty(textFgColorModel);






        final Property textFontSizeModel = Property.create(PROPERTY_LABELS_FONT_SIZE_KEY, Integer.class, PROPERTY_LABELS_FONT_SIZE_DEFAULT, true);
        textFontSizeModel.getDescriptor().setAlias(PROPERTY_LABELS_FONT_SIZE_ALIAS);
        vc.addProperty(textFontSizeModel);


        final Property textFontItalicModel = Property.create(PROPERTY_LABELS_FONT_ITALIC_KEY, Boolean.class, PROPERTY_LABELS_FONT_ITALIC_DEFAULT, true);
        textFontItalicModel.getDescriptor().setAlias(PROPERTY_LABELS_FONT_ITALIC_ALIAS);
        vc.addProperty(textFontItalicModel);

        final Property textFontBoldModel = Property.create(PROPERTY_LABELS_FONT_BOLD_KEY, Boolean.class, PROPERTY_LABELS_FONT_BOLD_DEFAULT, true);
        textFontBoldModel.getDescriptor().setAlias(PROPERTY_LABELS_FONT_BOLD_ALIAS);
        vc.addProperty(textFontBoldModel);

        final Property textFontModel = Property.create(PROPERTY_LABELS_FONT_NAME_KEY, String.class, PROPERTY_LABELS_FONT_NAME_DEFAULT, true);
        textFontModel.getDescriptor().setAlias(PROPERTY_LABELS_FONT_NAME_ALIAS);
        vc.addProperty(textFontModel);




        return vc;
    }
}
