

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

import static org.esa.snap.core.util.NamingConvention.COLOR_LOWER_CASE;
import static org.esa.snap.core.util.NamingConvention.COLOR_MIXED_CASE;

/**
 * Defines key property fields, variables, and variable constants for the Color Bar Legend Tool
 *
 * @author Daniel Knowles
 */



@LayerTypeMetadata(name = "ColorBarLayerType", aliasNames = {"org.esa.snap.core.layer.ColorBarLayerType"})
public class ColorBarLayerType extends LayerType {

    public static final String COLOR_BAR_LAYER_NAME = COLOR_MIXED_CASE + " Bar Legend";
    public static final String COLOR_BAR_LEGEND_NAME = COLOR_MIXED_CASE + " Bar Legend";
    public static final String COLOR_BAR_LEGEND_NAME_LOWER_CASE = COLOR_LOWER_CASE + " bar legend";

    public static final String OPTION_BEST_FIT = "Best Fit";
    public static final String OPTION_HORIZONTAL = "Horizontal";
    public static final String OPTION_VERTICAL = "Vertical";

    public static final String FONT_NAME_SANSERIF = "SanSerif";
    public static final String FONT_NAME_SERIF = "Serif";
    public static final String FONT_NAME_COURIER = "Courier";
    public static final String FONT_NAME_MONOSPACED = "Monospaced";
    public static final Object FONT_NAME_VALUE_SET[] = {FONT_NAME_SANSERIF, FONT_NAME_SERIF, FONT_NAME_COURIER, FONT_NAME_MONOSPACED};

    public static final String DISTRIB_EVEN_STR = "Generated Values";
    public static final String DISTRIB_EXACT_STR = "Palette Values";
    public static final String DISTRIB_MANUAL_STR = "Entered Values";

    public final static String DASHES = "----------";


    //--------------------------------------------------------------------------------------------------------------
    // Color Bar Legend parameters

    // Preferences property prefix
    private static final String PROPERTY_ROOT_KEY = "color.bar.legend.v9";
    private static final String PROPERTY_ROOT_ALIAS = "colorBarLegend";


    private static final String PROPERTY_SCHEME_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".scheme";
    private static final String PROPERTY_SCHEME_ALIAS_SUFFIX = PROPERTY_ROOT_ALIAS + "Scheme";

    public static final String PROPERTY_SCHEME_AUTO_APPLY_KEY = PROPERTY_SCHEME_KEY_SUFFIX + ".apply";
    public static final String PROPERTY_SCHEME_AUTO_APPLY_LABEL = "Apply Scheme Title/Units (Band Lookup)";
    public static final String PROPERTY_SCHEME_AUTO_APPLY_TOOLTIP = "Apply Scheme Title/Units (Band Lookup)";
    private static final String PROPERTY_SCHEME_AUTO_APPLY_ALIAS = PROPERTY_SCHEME_ALIAS_SUFFIX + "Apply";
    public static boolean PROPERTY_SCHEME_AUTO_APPLY_DEFAULT = true;
    public static final Class PROPERTY_SCHEME_AUTO_APPLY_TYPE = Boolean.class;


    public static final String PROPERTY_SCHEME_LABELS_APPLY_KEY = PROPERTY_SCHEME_KEY_SUFFIX + ".labels.apply";
    public static final String PROPERTY_SCHEME_LABELS_APPLY_LABEL = "Apply Scheme Tickmark Values (Band Lookup)";
    public static final String PROPERTY_SCHEME_LABELS_APPLY_TOOLTIP = "Apply Scheme Tickmark Values (Band Lookup)";
    private static final String PROPERTY_SCHEME_LABELS_APPLY_ALIAS = PROPERTY_SCHEME_ALIAS_SUFFIX + "LabelsApply";
    public static boolean PROPERTY_SCHEME_LABELS_APPLY_DEFAULT = true;
    public static final Class PROPERTY_SCHEME_LABELS_APPLY_TYPE = Boolean.class;


    public static final String PROPERTY_SCHEME_LABELS_RESTRICT_KEY = PROPERTY_SCHEME_KEY_SUFFIX + ".labels.restrict";
    public static final String PROPERTY_SCHEME_LABELS_RESTRICT_LABEL = "Restrict Scheme Labels";
    public static final String PROPERTY_SCHEME_LABELS_RESTRICT_TOOLTIP = "<html>Restrict Scheme Labels such that if scheme matches palette when scheme is" +
            " applied <br>and the user subsequently changes the palette such is no longer matches scheme<br>" +
            "then the scheme will be turned off so the color bar legend match the altered color palette</html>";
    private static final String PROPERTY_SCHEME_LABELS_RESTRICT_ALIAS = PROPERTY_SCHEME_ALIAS_SUFFIX + "LabelsRestrict";
    public static boolean PROPERTY_SCHEME_LABELS_RESTRICT_DEFAULT = true;
    public static final Class PROPERTY_SCHEME_LABELS_RESTRICT_TYPE = Boolean.class;


    // Header Title

    private static final String PROPERTY_HEADER_TITLE_ROOT_KEY = PROPERTY_ROOT_KEY + ".header.title";
    private static final String PROPERTY_HEADER_TITLE_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "HeaderTitle";

    public static final String PROPERTY_HEADER_TITLE_SECTION_KEY = PROPERTY_HEADER_TITLE_ROOT_KEY + ".section";
    public static final String PROPERTY_HEADER_TITLE_SECTION_LABEL = "Title";
    public static final String PROPERTY_HEADER_TITLE_SECTION_TOOLTIP = "Header title for the " + COLOR_LOWER_CASE + " bar legend";
    public static final String PROPERTY_HEADER_TITLE_SECTION_ALIAS = PROPERTY_HEADER_TITLE_ROOT_ALIAS + "Section";

    public static final String PROPERTY_TITLE_KEY = PROPERTY_HEADER_TITLE_ROOT_KEY;
    public static final String PROPERTY_TITLE_LABEL = "Title";
    public static final String PROPERTY_TITLE_TOOLTIP = "Title text for the header of the " + COLOR_LOWER_CASE + " bar";
    public static final String PROPERTY_TITLE_ALIAS = PROPERTY_HEADER_TITLE_ROOT_ALIAS;
    public static final String PROPERTY_TITLE_DEFAULT = "<PROPERTY=band>";
    public static final Class PROPERTY_TITLE_TYPE = String.class;



    // Header Units

    private static final String PROPERTY_HEADER_UNITS_ROOT_KEY = PROPERTY_ROOT_KEY + ".header.units";
    private static final String PROPERTY_HEADER_UNITS_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "HeaderUnits";

    public static final String PROPERTY_HEADER_UNITS_SECTION_KEY = PROPERTY_HEADER_UNITS_ROOT_KEY + ".section";
    public static final String PROPERTY_HEADER_UNITS_SECTION_LABEL = "Units";
    public static final String PROPERTY_HEADER_UNITS_SECTION_TOOLTIP = "Header units for the " + COLOR_LOWER_CASE + " bar legend";
    public static final String PROPERTY_HEADER_UNITS_SECTION_ALIAS = PROPERTY_HEADER_UNITS_ROOT_ALIAS + "Section";

    public static final String PROPERTY_UNITS_KEY = PROPERTY_HEADER_UNITS_ROOT_KEY;
    public static final String PROPERTY_UNITS_LABEL = "Units";
    public static final String PROPERTY_UNITS_TOOLTIP = "Units text for the header of the " + COLOR_LOWER_CASE + " bar";
    public static final String PROPERTY_UNITS_ALIAS = PROPERTY_HEADER_UNITS_ROOT_ALIAS;
    public static final String PROPERTY_UNITS_DEFAULT = "<PROPERTY=units>";
    public static final Class PROPERTY_UNITS_TYPE = String.class;


    public static final String PROPERTY_UNITS_NULL_KEY = PROPERTY_HEADER_UNITS_ROOT_KEY + ".null";
    public static final String PROPERTY_UNITS_NULL_LABEL = "Units Null Value";
    public static final String PROPERTY_UNITS_NULL_TOOLTIP = "Text to display as units when they are null";
    public static final String PROPERTY_UNITS_NULL_ALIAS = PROPERTY_HEADER_UNITS_ROOT_ALIAS + "Null";
    public static final String PROPERTY_UNITS_NULL_DEFAULT = "dimensionless";
    public static final Class PROPERTY_UNITS_NULL_TYPE = String.class;


    public static final String PROPERTY_CONVERT_CARET_KEY = PROPERTY_HEADER_UNITS_ROOT_KEY + ".convert.caret";
    public static final String PROPERTY_CONVERT_CARET_LABEL = "Convert Carets to Superscripts";
    public static final String PROPERTY_CONVERT_CARET_TOOLTIP = "Convert any caret (^) symbols found in the text into a formatted superscript";
    public static final String PROPERTY_CONVERT_CARET_ALIAS = PROPERTY_HEADER_UNITS_ROOT_ALIAS + "ConvertCaret";
    public static final boolean PROPERTY_CONVERT_CARET_DEFAULT = true;
    public static final Class PROPERTY_CONVERT_CARET_TYPE = Boolean.class;

    public static final String PROPERTY_UNITS_PARENTHESIS_KEY = PROPERTY_HEADER_UNITS_ROOT_KEY + ".parenthesis";
    public static final String PROPERTY_UNITS_PARENTHESIS_LABEL = "Add Units Parenthesis";
    public static final String PROPERTY_UNITS_PARENTHESIS_TOOLTIP = "Add Parenthesis around Units";
    public static final String PROPERTY_UNITS_PARENTHESIS_ALIAS = PROPERTY_HEADER_UNITS_ROOT_ALIAS + "Parenthesis";
    public static final boolean PROPERTY_UNITS_PARENTHESIS_DEFAULT = true;
    public static final Class PROPERTY_UNITS_PARENTHESIS_TYPE = Boolean.class;


    // Orientation

    private static final String PROPERTY_ORIENTATION_ROOT_KEY = PROPERTY_ROOT_KEY + ".orientation";
    private static final String PROPERTY_ORIENTATION_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "Orientation";

    public static final String PROPERTY_ORIENTATION_SECTION_KEY = PROPERTY_ORIENTATION_ROOT_KEY + ".section";
    public static final String PROPERTY_ORIENTATION_SECTION_LABEL = "Orientation";
    public static final String PROPERTY_ORIENTATION_SECTION_TOOLTIP = "Orientation options for the " + COLOR_LOWER_CASE + " bar legend";
    public static final String PROPERTY_ORIENTATION_SECTION_ALIAS = PROPERTY_ORIENTATION_ROOT_ALIAS + "Section";

    public static final String PROPERTY_ORIENTATION_KEY = PROPERTY_ORIENTATION_ROOT_KEY + ".orientation";
    public static final String PROPERTY_ORIENTATION_LABEL = "Angle";
    public static final String PROPERTY_ORIENTATION_TOOLTIP = "Orientation (vertical/horizontal) of the " + COLOR_LOWER_CASE + " bar legend";
    public static final Class PROPERTY_ORIENTATION_TYPE = String.class;
    public static final String PROPERTY_ORIENTATION_ALIAS = PROPERTY_ORIENTATION_ROOT_ALIAS + "Orientation";
    public static final String PROPERTY_ORIENTATION_OPTION1 = OPTION_BEST_FIT;
    public static final String PROPERTY_ORIENTATION_OPTION2 = OPTION_HORIZONTAL;
    public static final String PROPERTY_ORIENTATION_OPTION3 = OPTION_VERTICAL;
    public static final String PROPERTY_ORIENTATION_DEFAULT = OPTION_BEST_FIT;
    public static final Object PROPERTY_ORIENTATION_VALUE_SET[] = {PROPERTY_ORIENTATION_OPTION1, PROPERTY_ORIENTATION_OPTION2,PROPERTY_ORIENTATION_OPTION3};


    public static final String PROPERTY_SCENE_ASPECT_BEST_FIT_KEY = PROPERTY_ORIENTATION_ROOT_KEY + ".scene.aspect.best.fit";
    public static final String PROPERTY_SCENE_ASPECT_BEST_FIT_LABEL = "Scene Aspect Ratio";
    public static final String PROPERTY_SCENE_ASPECT_BEST_FIT_TOOLTIP = "For Best Fit: Scene aspect ratio (width/height) which triggers determination of horizontal or vertical color bar";
    private static final String PROPERTY_SCENE_ASPECT_BEST_FIT_ALIAS = PROPERTY_ORIENTATION_ROOT_ALIAS + "SceneAspectBestFit";
    public static final double PROPERTY_SCENE_ASPECT_BEST_FIT_DEFAULT = 1.0;
    public static final Class PROPERTY_SCENE_ASPECT_BEST_FIT_TYPE = Double.class;


    public static final String VERTICAL_TITLE_LEFT = "Left";
    public static final String VERTICAL_TITLE_RIGHT = "Right";
    public static final String VERTICAL_TITLE_TOP = "Top";
    public static final String VERTICAL_TITLE_BOTTOM = "Bottom";


//    public static Object[] VERTICAL_TITLE_LOCATION_VALUE_SET = {
//            VERTICAL_TITLE_LEFT,
//            VERTICAL_TITLE_RIGHT,
//            VERTICAL_TITLE_TOP,
//            VERTICAL_TITLE_BOTTOM
//    };

    public static Object[] VERTICAL_TITLE_LOCATION_VALUE_SET = {
            VERTICAL_TITLE_LEFT,
            VERTICAL_TITLE_RIGHT
    };


    public static final String PROPERTY_LOCATION_TITLE_VERTICAL_KEY = PROPERTY_ORIENTATION_ROOT_KEY + "title.vertical.anchor";
    public static final String PROPERTY_LOCATION_TITLE_VERTICAL_LABEL = "Title Placement";
    public static final String PROPERTY_LOCATION_TITLE_VERTICAL_TOOLTIP = "Where to place title on vertical legend";
    private static final String PROPERTY_LOCATION_TITLE_VERTICAL_ALIAS = PROPERTY_ORIENTATION_ROOT_ALIAS + "TitleAnchor";
    public static final String PROPERTY_LOCATION_TITLE_VERTICAL_DEFAULT = VERTICAL_TITLE_LEFT;
    public static final Class PROPERTY_LOCATION_TITLE_VERTICAL_TYPE = String.class;
    public static final Object PROPERTY_LOCATION_TITLE_VERTICAL_VALUE_SET[] = VERTICAL_TITLE_LOCATION_VALUE_SET;
    public static final boolean PROPERTY_LOCATION_TITLE_VERTICAL_ENABLED = false;

    public static final String PROPERTY_ORIENTATION_REVERSE_PALETTE_KEY = PROPERTY_ORIENTATION_ROOT_KEY + ".reverse.palette";
    public static final String PROPERTY_ORIENTATION_REVERSE_PALETTE_LABEL = "Reverse Palette & Labels";
    public static final String PROPERTY_ORIENTATION_REVERSE_PALETTE_TOOLTIP = "Reverse direction of palette and labels";
    private static final String PROPERTY_ORIENTATION_REVERSE_PALETTE_ALIAS = PROPERTY_ORIENTATION_ROOT_ALIAS + "ReversePalette";
    public static final boolean PROPERTY_ORIENTATION_REVERSE_PALETTE_DEFAULT = false;
    public static final Class PROPERTY_ORIENTATION_REVERSE_PALETTE_TYPE = Boolean.class;





    // Label (Values)

    private static final String PROPERTY_LABEL_VALUES_ROOT_KEY = PROPERTY_ROOT_KEY + ".label.values";
    private static final String PROPERTY_LABEL_VALUES_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "LabelValues";

    public static final String PROPERTY_LABEL_VALUES_SECTION_KEY = PROPERTY_LABEL_VALUES_ROOT_KEY + ".section";
    public static final String PROPERTY_LABEL_VALUES_SECTION_LABEL = "Labels";
    public static final String PROPERTY_LABEL_VALUES_SECTION_TOOLTIP = "Numeric value options for the " + COLOR_LOWER_CASE + " bar legend labels";
    public static final String PROPERTY_LABEL_VALUES_SECTION_ALIAS = PROPERTY_LABEL_VALUES_ROOT_ALIAS +"Section";

    public static final String PROPERTY_LABEL_VALUES_MODE_KEY = PROPERTY_LABEL_VALUES_ROOT_KEY + ".mode";
    public static final String PROPERTY_LABEL_VALUES_MODE_LABEL = "Label Value Mode";
    public static final String PROPERTY_LABEL_VALUES_MODE_TOOLTIP = "Mode for setting label values on the " + COLOR_LOWER_CASE + " bar legend";
    public static final Class PROPERTY_LABEL_VALUES_MODE_TYPE = String.class;
    public static final String PROPERTY_LABEL_VALUES_MODE_ALIAS = PROPERTY_LABEL_VALUES_ROOT_ALIAS + "Mode";
    public static final String PROPERTY_LABEL_VALUES_MODE_OPTION1 = DISTRIB_EVEN_STR;
    public static final String PROPERTY_LABEL_VALUES_MODE_OPTION2 = DISTRIB_MANUAL_STR;
    public static final String PROPERTY_LABEL_VALUES_MODE_OPTION3 = DISTRIB_EXACT_STR;
    public static final String PROPERTY_LABEL_VALUES_MODE_DEFAULT = DISTRIB_MANUAL_STR;
    public static final Object PROPERTY_LABEL_VALUES_MODE_VALUE_SET[] = {
            PROPERTY_LABEL_VALUES_MODE_OPTION1,
            PROPERTY_LABEL_VALUES_MODE_OPTION2,
            PROPERTY_LABEL_VALUES_MODE_OPTION3 };


    public static final String PROPERTY_LABEL_VALUES_COUNT_KEY = PROPERTY_LABEL_VALUES_ROOT_KEY + ".count";
    public static final String PROPERTY_LABEL_VALUES_COUNT_LABEL = "Label Count";
    public static final String PROPERTY_LABEL_VALUES_COUNT_TOOLTIP = "Number of tick marks and labels";
    public static final String PROPERTY_LABEL_VALUES_COUNT_ALIAS = PROPERTY_LABEL_VALUES_ROOT_ALIAS + "Count";
    public static final int PROPERTY_LABEL_VALUES_COUNT_DEFAULT = 5;
    public static final boolean PROPERTY_LABEL_VALUES_COUNT_ENABLED = false;
    public static final Class PROPERTY_LABEL_VALUES_COUNT_TYPE = Integer.class;
    public static final int PROPERTY_LABEL_VALUES_COUNT_MIN = 2;
    public static final int PROPERTY_LABEL_VALUES_COUNT_MAX = 50;
    public static final String PROPERTY_LABEL_VALUES_COUNT_INTERVAL = "[" + ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_MIN + "," + ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_MAX + "]";

    public static final String PROPERTY_LABEL_VALUES_ACTUAL_KEY = PROPERTY_LABEL_VALUES_ROOT_KEY + ".actual";
    public static final String PROPERTY_LABEL_VALUES_ACTUAL_LABEL = "Label Values";
    public static final String PROPERTY_LABEL_VALUES_ACTUAL_TOOLTIP = "Set actual values of the tick marks";
    private static final String PROPERTY_LABEL_VALUES_ACTUAL_ALIAS = PROPERTY_LABEL_VALUES_ROOT_ALIAS + "Actual";
    public static final boolean PROPERTY_LABEL_VALUES_ACTUAL_ENABLED = true;
    public static final String PROPERTY_LABEL_VALUES_ACTUAL_DEFAULT = "";
    public static final Class PROPERTY_LABEL_VALUES_ACTUAL_TYPE = String.class;



    public static final String PROPERTY_POPULATE_VALUES_TEXTFIELD_KEY = PROPERTY_LABEL_VALUES_ROOT_KEY + ".populate.values.textfield";
    public static final String PROPERTY_POPULATE_VALUES_TEXTFIELD_LABEL = "Auto-Fill Label Values Textfield";
    public static final String PROPERTY_POPULATE_VALUES_TEXTFIELD_TOOLTIP = "Auto-populate the values field with the generated values";
    private static final String PROPERTY_POPULATE_VALUES_TEXTFIELD_ALIAS = PROPERTY_LABEL_VALUES_ROOT_ALIAS + "PopulateValueTextfield";
    public static final boolean PROPERTY_POPULATE_VALUES_TEXTFIELD_DEFAULT = false;
    public static final Class PROPERTY_POPULATE_VALUES_TEXTFIELD_TYPE = Boolean.class;


    public static final String PROPERTY_LABEL_VALUES_SCALING_KEY = PROPERTY_LABEL_VALUES_ROOT_KEY + ".scaling.factor";
    public static final String PROPERTY_LABEL_VALUES_SCALING_LABEL = "Label Scaling";
    public static final String PROPERTY_LABEL_VALUES_SCALING_TOOLTIP = "Tick mark labels will be displayed after multiplication with this scaling factor";
    public static final String PROPERTY_LABEL_VALUES_SCALING_ALIAS = PROPERTY_LABEL_VALUES_ROOT_ALIAS + "ScalingFactor";
    public static final double PROPERTY_LABEL_VALUES_SCALING_DEFAULT = 1.0;
    public static final Class PROPERTY_LABEL_VALUES_SCALING_TYPE = Double.class;
    public static final double PROPERTY_LABEL_VALUES_SCALING_MIN = 0.0000000001;
    public static final double PROPERTY_LABEL_VALUES_SCALING_MAX = 1000000000;
    public static final String PROPERTY_LABEL_VALUES_SCALING_INTERVAL = "[" +
            ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_MIN + "," +
            ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_MAX + "]";


    public static final String PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY = PROPERTY_LABEL_VALUES_ROOT_KEY + ".decimal.places";
    public static final String PROPERTY_LABEL_VALUES_DECIMAL_PLACES_LABEL = "Decimal Places";
    public static final String PROPERTY_LABEL_VALUES_DECIMAL_PLACES_TOOLTIP = "Decimal places to display the numeric tick mark labels";
    public static final String PROPERTY_LABEL_VALUES_DECIMAL_PLACES_ALIAS = PROPERTY_LABEL_VALUES_ROOT_ALIAS + "DecimalPlaces";
    public static final int PROPERTY_LABEL_VALUES_DECIMAL_PLACES_DEFAULT = 3;
    public static final Class PROPERTY_LABEL_VALUES_DECIMAL_PLACES_TYPE = Integer.class;
    public static final int PROPERTY_LABEL_VALUES_DECIMAL_PLACES_MIN = 0;
    public static final int PROPERTY_LABEL_VALUES_DECIMAL_PLACES_MAX = 10;
    public static final String PROPERTY_LABEL_VALUES_DECIMAL_PLACES_INTERVAL = "[" +
            ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_MIN + "," +
            ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_MAX + "]";


    public static final String PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY = PROPERTY_LABEL_VALUES_ROOT_KEY + ".force.decimal.places";
    public static final String PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_LABEL = "Force Trailing Decimal Zeros";
    public static final String PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_TOOLTIP = "Force to exact decimal places by adding trailing zeros as needed";
    private static final String PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_ALIAS = PROPERTY_LABEL_VALUES_ROOT_ALIAS + "ForceDecimalPlaces";
    public static final boolean PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_DEFAULT = false;
    public static final Class PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_TYPE = Boolean.class;


    public static final String PROPERTY_WEIGHT_TOLERANCE_KEY = PROPERTY_LABEL_VALUES_ROOT_KEY + "weight.tolerance";
    public static final String PROPERTY_WEIGHT_TOLERANCE_LABEL = "Weight Tolerance";
    public static final String PROPERTY_WEIGHT_TOLERANCE_TOOLTIP = "Weight tolerance to keep desired auto-generated rounded values on the ends of the color bar";
    private static final String PROPERTY_WEIGHT_TOLERANCE_ALIAS = PROPERTY_LABEL_VALUES_ROOT_ALIAS + "WeightTolerance";
    public static final Double PROPERTY_WEIGHT_TOLERANCE_DEFAULT = 0.001;
    public static final Class PROPERTY_WEIGHT_TOLERANCE_TYPE = Double.class;





    // ColorBar Location Section

    private static final String PROPERTY_LOCATION_ROOT_KEY = PROPERTY_ROOT_KEY + ".location";
    private static final String PROPERTY_LOCATION_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "Location";

    public static final String PROPERTY_LOCATION_SECTION_KEY = PROPERTY_LOCATION_ROOT_KEY + ".section";
    public static final String PROPERTY_LOCATION_SECTION_LABEL = "Location";
    public static final String PROPERTY_LOCATION_SECTION_TOOLTIP = "Set placement location of " + COLOR_LOWER_CASE + " bar image legend on the scene image";
    public static final String PROPERTY_LOCATION_SECTION_ALIAS = PROPERTY_LOCATION_ROOT_ALIAS + "Section";

    public static final String PROPERTY_LOCATION_INSIDE_KEY = PROPERTY_LOCATION_ROOT_KEY + ".inside";
    public static final String PROPERTY_LOCATION_INSIDE_LABEL = "Inside Scene Image";
    public static final String PROPERTY_LOCATION_INSIDE_TOOLTIP = "Place " + COLOR_LOWER_CASE + " bar inside/outside scene image bounds";
    private static final String PROPERTY_LOCATION_INSIDE_ALIAS = PROPERTY_LOCATION_ROOT_ALIAS + "Inside";
    public static final boolean PROPERTY_LOCATION_INSIDE_DEFAULT = false;
    public static final Class PROPERTY_LOCATION_INSIDE_TYPE = Boolean.class;




    public static final String LOCATION_UPPER_LEFT = "Upper Left";
    public static final String LOCATION_UPPER_CENTER = "Upper Center";
    public static final String LOCATION_UPPER_RIGHT = "Upper Right";
    public static final String LOCATION_LOWER_LEFT = "Lower Left";
    public static final String LOCATION_LOWER_CENTER = "Lower Center";
    public static final String LOCATION_LOWER_RIGHT = "Lower Right";
    public static final String LOCATION_LEFT_CENTER = "Left Side Center";
    public static final String LOCATION_RIGHT_CENTER = "Right Side Center";


    public static String[] getColorBarLocationHorizontalArray() {
        return  new String[]{
                LOCATION_UPPER_LEFT,
                LOCATION_UPPER_CENTER,
                LOCATION_UPPER_RIGHT,
                LOCATION_LOWER_LEFT,
                LOCATION_LOWER_CENTER,
                LOCATION_LOWER_RIGHT
        };
    }
//    LOCATION_LEFT_CENTER,
//    LOCATION_RIGHT_CENTER

    public static final String PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_KEY = PROPERTY_LOCATION_ROOT_KEY + ".anchor.horizontal";
    public static final String PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_LABEL = "Location (Horizontal)";
    public static final String PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_TOOLTIP = "Where to place the horizontal " + COLOR_LOWER_CASE + " bar on image";
    private static final String PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_ALIAS = PROPERTY_LOCATION_ROOT_ALIAS + "AnchorHorizontal";
    public static final String PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_DEFAULT = LOCATION_LOWER_CENTER;
    public static final Class PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_TYPE = String.class;


    public static String[] getColorBarLocationVerticalArray() {
        return  new String[]{
                LOCATION_UPPER_LEFT,
                LOCATION_LEFT_CENTER,
                LOCATION_LOWER_LEFT,
                LOCATION_UPPER_RIGHT,
                LOCATION_RIGHT_CENTER,
                LOCATION_LOWER_RIGHT
        };
    }

    public static final String PROPERTY_LOCATION_PLACEMENT_VERTICAL_KEY = PROPERTY_LOCATION_ROOT_KEY + ".anchor.vertical";
    public static final String PROPERTY_LOCATION_PLACEMENT_VERTICAL_LABEL = "Location (Vertical)";
    public static final String PROPERTY_LOCATION_PLACEMENT_VERTICAL_TOOLTIP = "Where to place the vertical " + COLOR_LOWER_CASE + " bar on image";
    private static final String PROPERTY_LOCATION_PLACEMENT_VERTICAL_ALIAS = PROPERTY_LOCATION_ROOT_ALIAS + "AnchorVertical";
    public static final String PROPERTY_LOCATION_PLACEMENT_VERTICAL_DEFAULT = LOCATION_LOWER_RIGHT;
    public static final Class PROPERTY_LOCATION_PLACEMENT_VERTICAL_TYPE = String.class;



    public static final String PROPERTY_LOCATION_GAP_FACTOR_KEY = PROPERTY_LOCATION_ROOT_KEY + ".offset.outside";
    public static final String PROPERTY_LOCATION_GAP_FACTOR_LABEL = "Location Offset (Outside)";
    public static final String PROPERTY_LOCATION_GAP_FACTOR_TOOLTIP = "Percentage of scene size to move " + COLOR_LOWER_CASE + " bar legend outside of anchored axis";
    private static final String PROPERTY_LOCATION_GAP_FACTOR_ALIAS = PROPERTY_LOCATION_ROOT_ALIAS + "Offset (Outside)";
    public static final Double PROPERTY_LOCATION_GAP_FACTOR_DEFAULT = 6.0;
    public static final double PROPERTY_LOCATION_GAP_FACTOR_MIN = -1000;
    public static final double PROPERTY_LOCATION_GAP_FACTOR_MAX = 1000;
    public static final String PROPERTY_LOCATION_GAP_FACTOR_INTERVAL = "[" + ColorBarLayerType.PROPERTY_LOCATION_GAP_FACTOR_MIN + "," + ColorBarLayerType.PROPERTY_LOCATION_GAP_FACTOR_MAX + "]";
    public static final Class PROPERTY_LOCATION_GAP_FACTOR_TYPE = Double.class;


    public static final String PROPERTY_LOCATION_OFFSET_KEY = PROPERTY_LOCATION_ROOT_KEY + ".offset.inside";
    public static final String PROPERTY_LOCATION_OFFSET_LABEL = "Location Offset (Inside)";
    public static final String PROPERTY_LOCATION_OFFSET_TOOLTIP = "Percentage of scene size to move " + COLOR_LOWER_CASE + " bar legend inside of anchored axis";
    private static final String PROPERTY_LOCATION_OFFSET_ALIAS = PROPERTY_LOCATION_ROOT_ALIAS + "Offset";
    public static final Double PROPERTY_LOCATION_OFFSET_DEFAULT = 0.0;
    public static final Class PROPERTY_LOCATION_OFFSET_TYPE = Double.class;

    public static final String PROPERTY_LOCATION_SHIFT_KEY = PROPERTY_LOCATION_ROOT_KEY + ".shift";
    public static final String PROPERTY_LOCATION_SHIFT_LABEL = "Location Shift";
    public static final String PROPERTY_LOCATION_SHIFT_TOOLTIP = "Move " + COLOR_LOWER_CASE + " bar legend along the anchored axis (by percentage of " + COLOR_LOWER_CASE + " bar width)";
    private static final String PROPERTY_LOCATION_SHIFT_ALIAS = PROPERTY_LOCATION_ROOT_ALIAS + "Shift";
    public static final Double PROPERTY_LOCATION_SHIFT_DEFAULT = 0.0;
    public static final Class PROPERTY_LOCATION_SHIFT_TYPE = Double.class;








    // Image Scaling Section

    private static final String PROPERTY_IMAGE_SCALING_ROOT_KEY = PROPERTY_ROOT_KEY + ".scaling";
    private static final String PROPERTY_IMAGE_SCALING_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "Scaling";

    public static final String PROPERTY_IMAGE_SCALING_SECTION_KEY = PROPERTY_IMAGE_SCALING_ROOT_KEY + ".section";
    public static final String PROPERTY_IMAGE_SCALING_SECTION_LABEL = "Size Scaling";
    public static final String PROPERTY_IMAGE_SCALING_SECTION_TOOLTIP = "Set scaling and relative size of " + COLOR_LOWER_CASE + " bar image";
    public static final String PROPERTY_IMAGE_SCALING_SECTION_ALIAS = PROPERTY_IMAGE_SCALING_ROOT_ALIAS + "Section";

    public static final String SCENE_SCALING_OFF = "No Scaling";
    public static final String SCENE_SCALING_LENGTH = "Scale by Color Bar Length";
    public static final String SCENE_SCALING_WIDTH = "Scale by Color Bar Thickness";


    public static String[] getScalingOptionsArray() {
        return  new String[]{
                SCENE_SCALING_OFF,
                SCENE_SCALING_LENGTH,
                SCENE_SCALING_WIDTH
        };
    }
    public static final String PROPERTY_IMAGE_SCALING_APPLY_SIZE_KEY = PROPERTY_IMAGE_SCALING_ROOT_KEY + ".apply";
    public static final String PROPERTY_IMAGE_SCALING_APPLY_SIZE_LABEL = "Scale to Scene Size";
    public static final String PROPERTY_IMAGE_SCALING_APPLY_SIZE_TOOLTIP = "Scale the " + COLOR_LOWER_CASE + " bar legend size to percentage of the scene image size using Scene Size Scaling";
    private static final String PROPERTY_IMAGE_SCALING_APPLY_SIZE_ALIAS = PROPERTY_IMAGE_SCALING_ROOT_ALIAS + "Apply";
    public static final String PROPERTY_IMAGE_SCALING_APPLY_SIZE_DEFAULT = SCENE_SCALING_LENGTH;
    public static final Class PROPERTY_IMAGE_SCALING_APPLY_SIZE_TYPE = String.class;

    public static final String PROPERTY_IMAGE_SCALING_SIZE_KEY = PROPERTY_IMAGE_SCALING_ROOT_KEY + ".size";
    public static final String PROPERTY_IMAGE_SCALING_SIZE_LABEL = "Scene Size Scaling";
    public static final String PROPERTY_IMAGE_SCALING_SIZE_TOOLTIP = "Percent to scale " + COLOR_LOWER_CASE + " bar legend relative to the scene image size";
    private static final String PROPERTY_IMAGE_SCALING_SIZE_ALIAS = PROPERTY_IMAGE_SCALING_ROOT_ALIAS + "Size";
    public static final double PROPERTY_IMAGE_SCALING_SIZE_DEFAULT = 100.0;
    public static final Class PROPERTY_IMAGE_SCALING_SIZE_TYPE = Double.class;
    public static final double PROPERTY_IMAGE_SCALING_SIZE_MIN = 5;
    public static final double PROPERTY_IMAGE_SCALING_SIZE_MAX = 200;
    public static final String PROPERTY_IMAGE_SCALING_SIZE_INTERVAL = "[" +
            ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_MIN + "," +
            ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_MAX + "]";



    public static final String PROPERTY_COLORBAR_LENGTH_KEY = PROPERTY_IMAGE_SCALING_ROOT_KEY + ".colorbar.length";
    public static final String PROPERTY_COLORBAR_LENGTH_LABEL = COLOR_MIXED_CASE + " Bar Length";
    public static final String PROPERTY_COLORBAR_LENGTH_TOOLTIP = "Length in pixels of the " + COLOR_LOWER_CASE + " bar";
    private static final String PROPERTY_COLORBAR_LENGTH_ALIAS = PROPERTY_IMAGE_SCALING_ROOT_ALIAS + "ColorbarLength";
    public static final int PROPERTY_COLORBAR_LENGTH_VALUE_MIN = 500;
    public static final int PROPERTY_COLORBAR_LENGTH_VALUE_MAX = 8000;
    public static final String PROPERTY_COLORBAR_LENGTH_VALUE_INTERVAL = "[" + ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_VALUE_MIN + "," + ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_VALUE_MAX + "]";
    public static final int PROPERTY_COLORBAR_LENGTH_DEFAULT = 1200;
    public static final Class PROPERTY_COLORBAR_LENGTH_TYPE = Integer.class;

    public static final String PROPERTY_COLORBAR_WIDTH_KEY = PROPERTY_IMAGE_SCALING_ROOT_KEY + ".colorbar.width";
    public static final String PROPERTY_COLORBAR_WIDTH_LABEL = COLOR_MIXED_CASE + " Bar Width";
    public static final String PROPERTY_COLORBAR_WIDTH_TOOLTIP = "Width in pixels of the " + COLOR_LOWER_CASE + " bar";
    private static final String PROPERTY_COLORBAR_WIDTH_ALIAS = PROPERTY_IMAGE_SCALING_ROOT_ALIAS + "ColorWidth";
    public static final int PROPERTY_COLORBAR_WIDTH_MIN = 5;
    public static final int PROPERTY_COLORBAR_WIDTH_MAX = 1000;
    public static final String PROPERTY_COLORBAR_WIDTH_INTERVAL = "[" + ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_MIN + "," + ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_MAX + "]";
    public static final int PROPERTY_COLORBAR_WIDTH_DEFAULT = 55;
    public static final Class PROPERTY_COLORBAR_WIDTH_TYPE = Integer.class;






    // Title Section

    private static final String PROPERTY_TITLE_ROOT_KEY = PROPERTY_ROOT_KEY + ".title.parameter";
    private static final String PROPERTY_TITLE_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "TitleParameter";

    public static final String PROPERTY_TITLE_SECTION_KEY = PROPERTY_TITLE_ROOT_KEY + ".section";
    public static final String PROPERTY_TITLE_SECTION_LABEL = "Title Formatting";
    public static final String PROPERTY_TITLE_SECTION_TOOLTIP = "Set parameter options in title of " + COLOR_LOWER_CASE + " bar";
    public static final String PROPERTY_TITLE_SECTION_ALIAS = PROPERTY_TITLE_ROOT_ALIAS + "Section";

    public static final String PROPERTY_TITLE_SHOW_KEY = PROPERTY_TITLE_ROOT_KEY + ".show";
    public static final String PROPERTY_TITLE_SHOW_LABEL = "Show Title";
    public static final String PROPERTY_TITLE_SHOW_TOOLTIP = "Add title to the " + COLOR_LOWER_CASE + " bar";
    private static final String PROPERTY_TITLE_SHOW_ALIAS = PROPERTY_TITLE_ROOT_ALIAS + "Show";
    public static final boolean PROPERTY_TITLE_SHOW_DEFAULT = true;
    public static final Class PROPERTY_TITLE_SHOW_TYPE = Boolean.class;


    public static final String PROPERTY_TITLE_FONT_SIZE_KEY = PROPERTY_TITLE_ROOT_KEY + "font.size";
    public static final String PROPERTY_TITLE_FONT_SIZE_LABEL = "Title Size";
    public static final String PROPERTY_TITLE_FONT_SIZE_TOOLTIP = "Set size of the title parameter";
    private static final String PROPERTY_TITLE_FONT_SIZE_ALIAS = PROPERTY_TITLE_ROOT_ALIAS + "FontSize";
    public static final int PROPERTY_TITLE_FONT_SIZE_DEFAULT = 50;
    public static final Class PROPERTY_TITLE_FONT_SIZE_TYPE = Integer.class;
    public static final int PROPERTY_TITLE_FONT_SIZE_VALUE_MIN = 10;
    public static final int PROPERTY_TITLE_FONT_SIZE_VALUE_MAX = 200;
    public static final String PROPERTY_TITLE_FONT_SIZE_INTERVAL =
            "[" + ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_VALUE_MIN +
                    "," + ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_VALUE_MAX + "]";



    public static final String PROPERTY_TITLE_FONT_BOLD_KEY = PROPERTY_TITLE_ROOT_KEY + ".font.bold";
    public static final String PROPERTY_TITLE_FONT_BOLD_LABEL = "Title Font Bold";
    public static final String PROPERTY_TITLE_FONT_BOLD_TOOLTIP = "Format title parameter text font in bold";
    public static final String PROPERTY_TITLE_FONT_BOLD_ALIAS = PROPERTY_TITLE_ROOT_ALIAS + "FontBold";
    public static final boolean PROPERTY_TITLE_FONT_BOLD_DEFAULT = false;
    public static final Class PROPERTY_TITLE_FONT_BOLD_TYPE = Boolean.class;

    public static final String PROPERTY_TITLE_FONT_ITALIC_KEY = PROPERTY_TITLE_ROOT_KEY + ".font.italic";
    public static final String PROPERTY_TITLE_FONT_ITALIC_LABEL = "Title Font Italic";
    public static final String PROPERTY_TITLE_FONT_ITALIC_TOOLTIP = "Format title parameter text font in italic";
    public static final String PROPERTY_TITLE_FONT_ITALIC_ALIAS = PROPERTY_TITLE_ROOT_ALIAS + "FontItalic";
    public static final boolean PROPERTY_TITLE_FONT_ITALIC_DEFAULT = false;
    public static final Class PROPERTY_TITLE_FONT_ITALIC_TYPE = Boolean.class;

    public static final String PROPERTY_TITLE_FONT_NAME_KEY = PROPERTY_TITLE_ROOT_KEY + ".font.name";
    public static final String PROPERTY_TITLE_FONT_NAME_LABEL = "Title Font";
    public static final String PROPERTY_TITLE_FONT_NAME_TOOLTIP = "Set the text font of the title parameter";
    public static final String PROPERTY_TITLE_FONT_NAME_ALIAS = PROPERTY_TITLE_ROOT_ALIAS + "FontName";
    public static final String PROPERTY_TITLE_FONT_NAME_DEFAULT = FONT_NAME_SANSERIF;
    public static final Class PROPERTY_TITLE_FONT_NAME_TYPE = String.class;
    public static final Object PROPERTY_TITLE_FONT_NAME_VALUE_SET[] = FONT_NAME_VALUE_SET;

    public static final String PROPERTY_TITLE_COLOR_KEY = PROPERTY_TITLE_ROOT_KEY + "font.color";
    public static final String PROPERTY_TITLE_COLOR_LABEL = "Title " + COLOR_MIXED_CASE;
    public static final String PROPERTY_TITLE_COLOR_TOOLTIP = "Set " + COLOR_LOWER_CASE + " of the title";
    private static final String PROPERTY_TITLE_COLOR_ALIAS = PROPERTY_TITLE_ROOT_ALIAS + "FontColor";
    public static final Color PROPERTY_TITLE_COLOR_DEFAULT = Color.BLACK;
    public static final Class PROPERTY_TITLE_COLOR_TYPE = Color.class;





    // Units Section

    private static final String PROPERTY_UNITS_ROOT_KEY = PROPERTY_ROOT_KEY + ".units";
    private static final String PROPERTY_UNITS_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "Units";

    public static final String PROPERTY_UNITS_SECTION_KEY = PROPERTY_UNITS_ROOT_KEY + ".section";
    public static final String PROPERTY_UNITS_SECTION_LABEL = "Units Formatting";
    public static final String PROPERTY_UNITS_SECTION_TOOLTIP = "Set units of " + COLOR_LOWER_CASE + " bar";
    public static final String PROPERTY_UNITS_SECTION_ALIAS = PROPERTY_UNITS_ROOT_ALIAS + "Section";

    public static final String PROPERTY_UNITS_SHOW_KEY = PROPERTY_UNITS_ROOT_KEY + ".show";
    public static final String PROPERTY_UNITS_SHOW_LABEL = "Show Units";
    public static final String PROPERTY_UNITS_SHOW_TOOLTIP = "Add units to the title of the " + COLOR_LOWER_CASE + " bar";
    private static final String PROPERTY_UNITS_SHOW_ALIAS = PROPERTY_UNITS_ROOT_ALIAS + "Show";
    public static final boolean PROPERTY_UNITS_SHOW_DEFAULT = true;
    public static final Class PROPERTY_UNITS_SHOW_TYPE = Boolean.class;


    public static final String PROPERTY_UNITS_FONT_SIZE_KEY = PROPERTY_UNITS_ROOT_KEY + ".font.size";
    public static final String PROPERTY_UNITS_FONT_SIZE_LABEL = "Units Size";
    public static final String PROPERTY_UNITS_FONT_SIZE_TOOLTIP = "Set size of the title units";
    private static final String PROPERTY_UNITS_FONT_SIZE_ALIAS = PROPERTY_UNITS_ROOT_ALIAS + "FontSize";
    public static final int PROPERTY_UNITS_FONT_SIZE_DEFAULT = 40;
    public static final Class PROPERTY_UNITS_FONT_SIZE_TYPE = Integer.class;
    public static final int PROPERTY_UNITS_FONT_SIZE_VALUE_MIN = 10;
    public static final int PROPERTY_UNITS_FONT_SIZE_VALUE_MAX = 200;
    public static final String PROPERTY_UNITS_FONT_SIZE_INTERVAL =
            "[" + ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_VALUE_MIN +
                    "," + ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_VALUE_MAX + "]";

    public static final String PROPERTY_UNITS_FONT_BOLD_KEY = PROPERTY_UNITS_ROOT_KEY + ".font.bold";
    public static final String PROPERTY_UNITS_FONT_BOLD_LABEL = "Units Bold Font";
    public static final String PROPERTY_UNITS_FONT_BOLD_TOOLTIP = "Format title units text font in bold";
    public static final String PROPERTY_UNITS_FONT_BOLD_ALIAS = PROPERTY_UNITS_ROOT_ALIAS + "FontBold";
    public static final boolean PROPERTY_UNITS_FONT_BOLD_DEFAULT = false;
    public static final Class PROPERTY_UNITS_FONT_BOLD_TYPE = Boolean.class;

    public static final String PROPERTY_UNITS_FONT_ITALIC_KEY = PROPERTY_UNITS_ROOT_KEY + ".font.italic";
    public static final String PROPERTY_UNITS_FONT_ITALIC_LABEL = "Units Italic Font";
    public static final String PROPERTY_UNITS_FONT_ITALIC_TOOLTIP = "Format title units text font in italic";
    public static final String PROPERTY_UNITS_FONT_ITALIC_ALIAS = PROPERTY_UNITS_ROOT_ALIAS + "FontItalic";
    public static final boolean PROPERTY_UNITS_FONT_ITALIC_DEFAULT = false;
    public static final Class PROPERTY_UNITS_FONT_ITALIC_TYPE = Boolean.class;

    public static final String PROPERTY_UNITS_FONT_NAME_KEY = PROPERTY_UNITS_ROOT_KEY + ".font.name";
    public static final String PROPERTY_UNITS_FONT_NAME_LABEL = "Units Font";
    public static final String PROPERTY_UNITS_FONT_NAME_TOOLTIP = "Set the text font of the title units";
    public static final String PROPERTY_UNITS_FONT_NAME_ALIAS = PROPERTY_UNITS_ROOT_ALIAS + "FontName";
    public static final String PROPERTY_UNITS_FONT_NAME_DEFAULT = FONT_NAME_SANSERIF;
    public static final Class PROPERTY_UNITS_FONT_NAME_TYPE = String.class;
    public static final Object PROPERTY_UNITS_FONT_NAME_VALUE_SET[] = FONT_NAME_VALUE_SET;


    public static final String PROPERTY_UNITS_FONT_COLOR_KEY = PROPERTY_UNITS_ROOT_KEY + ".font.color";
    public static final String PROPERTY_UNITS_FONT_COLOR_LABEL = "Units " + COLOR_MIXED_CASE;
    public static final String PROPERTY_UNITS_FONT_COLOR_TOOLTIP = "Set " + COLOR_LOWER_CASE + " of the title units";
    private static final String PROPERTY_UNITS_FONT_COLOR_ALIAS = PROPERTY_UNITS_ROOT_ALIAS + "FontColor";
    public static final Color PROPERTY_UNITS_FONT_COLOR_DEFAULT = Color.BLACK;
    public static final Class PROPERTY_UNITS_FONT_COLOR_TYPE = Color.class;






    // Tick-Mark Labels Section

    private static final String PROPERTY_LABELS_ROOT_KEY = PROPERTY_ROOT_KEY + ".labels";
    private static final String PROPERTY_LABELS_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "Labels";

    public static final String PROPERTY_LABELS_SECTION_KEY = PROPERTY_LABELS_ROOT_KEY + ".section";
    public static final String PROPERTY_LABELS_SECTION_LABEL = "Labels Formatting";
    public static final String PROPERTY_LABELS_SECTION_TOOLTIP = "Configuration options for the labels";
    public static final String PROPERTY_LABELS_SECTION_ALIAS = PROPERTY_LABELS_ROOT_ALIAS + "Section";

    public static final String PROPERTY_LABELS_SHOW_KEY = PROPERTY_LABELS_ROOT_KEY + ".show";
    public static final String PROPERTY_LABELS_SHOW_LABEL = "Show Tick Labels";
    public static final String PROPERTY_LABELS_SHOW_TOOLTIP = "Show the tick-mark labels";
    private static final String PROPERTY_LABELS_SHOW_ALIAS = PROPERTY_LABELS_ROOT_ALIAS + "Show";
    public static final boolean PROPERTY_LABELS_SHOW_DEFAULT = true;
    public static final Class PROPERTY_LABELS_SHOW_TYPE = Boolean.class;

    public static final String PROPERTY_LABELS_FONT_SIZE_KEY = PROPERTY_LABELS_ROOT_KEY + ".font.size";
    public static final String PROPERTY_LABELS_FONT_SIZE_LABEL = "Labels Size";
    public static final String PROPERTY_LABELS_FONT_SIZE_TOOLTIP = "Set the size of the tick-mark labels";
    private static final String PROPERTY_LABELS_FONT_SIZE_ALIAS = PROPERTY_LABELS_ROOT_ALIAS + "FontSize";
    public static final int PROPERTY_LABELS_FONT_SIZE_DEFAULT = 30;
    public static final Class PROPERTY_LABELS_FONT_SIZE_TYPE = Integer.class;
    public static final int PROPERTY_LABELS_FONT_SIZE_VALUE_MIN = 10;
    public static final int PROPERTY_LABELS_FONT_SIZE_VALUE_MAX = 200;
    public static final String PROPERTY_LABELS_FONT_SIZE_VALUE_INTERVAL = "[" + ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_VALUE_MIN + "," + ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_VALUE_MAX + "]";

    public static final String PROPERTY_LABELS_FONT_BOLD_KEY = PROPERTY_LABELS_ROOT_KEY + ".font.bold";
    public static final String PROPERTY_LABELS_FONT_BOLD_LABEL = "Labels Font Bold";
    public static final String PROPERTY_LABELS_FONT_BOLD_TOOLTIP = "Format tick-mark label text font in bold";
    public static final String PROPERTY_LABELS_FONT_BOLD_ALIAS = PROPERTY_LABELS_ROOT_ALIAS + "FontBold";
    public static final boolean PROPERTY_LABELS_FONT_BOLD_DEFAULT = false;
    public static final Class PROPERTY_LABELS_FONT_BOLD_TYPE = Boolean.class;

    public static final String PROPERTY_LABELS_FONT_ITALIC_KEY = PROPERTY_LABELS_ROOT_KEY + ".font.italic";
    public static final String PROPERTY_LABELS_FONT_ITALIC_LABEL = "Labels Font Italic";
    public static final String PROPERTY_LABELS_FONT_ITALIC_TOOLTIP = "Format tick-mark label text font in italic";
    public static final String PROPERTY_LABELS_FONT_ITALIC_ALIAS = PROPERTY_LABELS_ROOT_ALIAS + "FontItalic";
    public static final boolean PROPERTY_LABELS_FONT_ITALIC_DEFAULT = false;
    public static final Class PROPERTY_LABELS_FONT_ITALIC_TYPE = Boolean.class;

    public static final String PROPERTY_LABELS_FONT_NAME_KEY = PROPERTY_LABELS_ROOT_KEY + ".font.name";
    public static final String PROPERTY_LABELS_FONT_NAME_LABEL = "Labels Font";
    public static final String PROPERTY_LABELS_FONT_NAME_TOOLTIP = "Set the font of the tick-mark labels";
    public static final String PROPERTY_LABELS_FONT_NAME_ALIAS = PROPERTY_LABELS_ROOT_ALIAS + "FontName";
    public static final String PROPERTY_LABELS_FONT_NAME_DEFAULT = FONT_NAME_SANSERIF;
    public static final Class PROPERTY_LABELS_FONT_NAME_TYPE = String.class;
    public static final Object PROPERTY_LABELS_FONT_NAME_VALUE_SET[] = FONT_NAME_VALUE_SET;

    public static final String PROPERTY_LABELS_FONT_COLOR_KEY = PROPERTY_LABELS_ROOT_KEY + ".font.color";
    public static final String PROPERTY_LABELS_FONT_COLOR_LABEL = "Labels " + COLOR_MIXED_CASE;
    public static final String PROPERTY_LABELS_FONT_COLOR_TOOLTIP = "Set the " + COLOR_LOWER_CASE + " of the tick-mark labels";
    private static final String PROPERTY_LABELS_FONT_COLOR_ALIAS = PROPERTY_LABELS_ROOT_ALIAS + "FontColor";
    public static final Color PROPERTY_LABELS_FONT_COLOR_DEFAULT = Color.BLACK;
    public static final Class PROPERTY_LABELS_FONT_COLOR_TYPE = Color.class;









    // Tickmarks Section

    private static final String PROPERTY_TICKMARKS_ROOT_KEY = PROPERTY_ROOT_KEY + ".tickmarks";
    private static final String PROPERTY_TICKMARKS_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "TickMarks";

    public static final String PROPERTY_TICKMARKS_SECTION_KEY = PROPERTY_TICKMARKS_ROOT_KEY + ".section";
    public static final String PROPERTY_TICKMARKS_SECTION_LABEL = "Tickmarks";
    public static final String PROPERTY_TICKMARKS_SECTION_TOOLTIP = "Format options for the " + COLOR_LOWER_CASE + " bar legend tickmarks";
    public static final String PROPERTY_TICKMARKS_SECTION_ALIAS = PROPERTY_TICKMARKS_ROOT_ALIAS + "Section";

    public static final String PROPERTY_TICKMARKS_SHOW_KEY = PROPERTY_TICKMARKS_ROOT_KEY + ".show";
    public static final String PROPERTY_TICKMARKS_SHOW_LABEL = "Show Tickmarks";
    public static final String PROPERTY_TICKMARKS_SHOW_TOOLTIP = "Display tickmarks";
    public static final String PROPERTY_TICKMARKS_SHOW_ALIAS = PROPERTY_TICKMARKS_ROOT_ALIAS + "Show";
    public static final boolean PROPERTY_TICKMARKS_SHOW_DEFAULT = true;
    public static final Class PROPERTY_TICKMARKS_SHOW_TYPE = Boolean.class;

    public static final String PROPERTY_TICKMARKS_LENGTH_KEY = PROPERTY_TICKMARKS_ROOT_KEY + ".length";
    public static final String PROPERTY_TICKMARKS_LENGTH_LABEL = "Tickmark Length";
    public static final String PROPERTY_TICKMARKS_LENGTH_TOOLTIP = "Set length of tickmarks";
    public static final String PROPERTY_TICKMARKS_LENGTH_ALIAS = PROPERTY_TICKMARKS_ROOT_ALIAS + "Length";
    public static final int PROPERTY_TICKMARKS_LENGTH_DEFAULT = 12;
    public static final Class PROPERTY_TICKMARKS_LENGTH_TYPE = Integer.class;

    public static final String PROPERTY_TICKMARKS_WIDTH_KEY = PROPERTY_TICKMARKS_ROOT_KEY + ".width";
    public static final String PROPERTY_TICKMARKS_WIDTH_LABEL = "Tickmark Width";
    public static final String PROPERTY_TICKMARKS_WIDTH_TOOLTIP = "Set width of tickmarks";
    public static final String PROPERTY_TICKMARKS_WIDTH_ALIAS = PROPERTY_TICKMARKS_ROOT_ALIAS + "Width";
    public static final int PROPERTY_TICKMARKS_WIDTH_DEFAULT = 4;
    public static final Class PROPERTY_TICKMARKS_WIDTH_TYPE = Integer.class;

    public static final String PROPERTY_TICKMARKS_COLOR_KEY = PROPERTY_TICKMARKS_ROOT_KEY + ".color";
    public static final String PROPERTY_TICKMARKS_COLOR_LABEL = "Tickmark " + COLOR_MIXED_CASE;
    public static final String PROPERTY_TICKMARKS_COLOR_TOOLTIP = "Set " + COLOR_LOWER_CASE + " of the tick marks";
    private static final String PROPERTY_TICKMARKS_COLOR_ALIAS = PROPERTY_TICKMARKS_ROOT_ALIAS + "Color";
    public static final Color PROPERTY_TICKMARKS_COLOR_DEFAULT = Color.BLACK;
    public static final Class PROPERTY_TICKMARKS_COLOR_TYPE = Color.class;




    // Palette Border Section

    private static final String PROPERTY_PALETTE_BORDER_ROOT_KEY = PROPERTY_ROOT_KEY + ".palette.border";
    private static final String PROPERTY_PALETTE_BORDER_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "PaletteBorder";

    public static final String PROPERTY_PALETTE_BORDER_SECTION_KEY = PROPERTY_PALETTE_BORDER_ROOT_KEY + ".section";
    public static final String PROPERTY_PALETTE_BORDER_SECTION_ALIAS = PROPERTY_PALETTE_BORDER_ROOT_ALIAS + "Section";
    public static final String PROPERTY_PALETTE_BORDER_SECTION_LABEL = "Colorbar Border";
    public static final String PROPERTY_PALETTE_BORDER_SECTION_TOOLTIP = "Configuration options for adding a border around the palette (colorbar)";

    public static final String PROPERTY_PALETTE_BORDER_SHOW_KEY = PROPERTY_PALETTE_BORDER_ROOT_KEY + ".show";
    public static final String PROPERTY_PALETTE_BORDER_SHOW_LABEL = "Show Colorbar Border";
    public static final String PROPERTY_PALETTE_BORDER_SHOW_TOOLTIP = "Display a border around the palette (color bar) image";
    private static final String PROPERTY_PALETTE_BORDER_SHOW_ALIAS = PROPERTY_PALETTE_BORDER_ROOT_ALIAS + "Show";
    public static final boolean PROPERTY_PALETTE_BORDER_SHOW_DEFAULT = true;
    public static final Class PROPERTY_PALETTE_BORDER_SHOW_TYPE = Boolean.class;

    public static final String PROPERTY_PALETTE_BORDER_WIDTH_KEY = PROPERTY_PALETTE_BORDER_ROOT_KEY + ".width";
    public static final String PROPERTY_PALETTE_BORDER_WIDTH_LABEL = "Colorbar Border Width";
    public static final String PROPERTY_PALETTE_BORDER_WIDTH_TOOLTIP = "Width of palette (color bar) border line";
    private static final String PROPERTY_PALETTE_BORDER_WIDTH_ALIAS = PROPERTY_PALETTE_BORDER_ROOT_ALIAS + "Width";
    public static final int PROPERTY_PALETTE_BORDER_WIDTH_DEFAULT = 1;
    public static final Class PROPERTY_PALETTE_BORDER_WIDTH_TYPE = Integer.class;

    public static final String PROPERTY_PALETTE_BORDER_COLOR_KEY = PROPERTY_PALETTE_BORDER_ROOT_KEY + ".color";
    public static final String PROPERTY_PALETTE_BORDER_COLOR_LABEL = "Colorbar Border " + COLOR_MIXED_CASE;
    public static final String PROPERTY_PALETTE_BORDER_COLOR_TOOLTIP = COLOR_MIXED_CASE + " of the palette (color bar) border line";
    private static final String PROPERTY_PALETTE_BORDER_COLOR_ALIAS = PROPERTY_PALETTE_BORDER_ROOT_ALIAS + "Color";
    public static final Color PROPERTY_PALETTE_BORDER_COLOR_DEFAULT = Color.BLACK;
    public static final Class PROPERTY_PALETTE_BORDER_COLOR_TYPE = Color.class;


    // Legend Border Section

    private static final String PROPERTY_LEGEND_BORDER_ROOT_KEY = PROPERTY_ROOT_KEY + ".legend.border";
    private static final String PROPERTY_LEGEND_BORDER_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "LegendBorder";

    public static final String PROPERTY_LEGEND_BORDER_SECTION_KEY = PROPERTY_LEGEND_BORDER_ROOT_KEY + ".section";
    public static final String PROPERTY_LEGEND_BORDER_SECTION_ALIAS = PROPERTY_LEGEND_BORDER_ROOT_ALIAS + "Section";
    public static final String PROPERTY_LEGEND_BORDER_SECTION_LABEL = "Legend Border";
    public static final String PROPERTY_LEGEND_BORDER_SECTION_TOOLTIP = "Configuration options for adding a border around the full legend";

    public static final String PROPERTY_LEGEND_BORDER_SHOW_KEY = PROPERTY_LEGEND_BORDER_ROOT_KEY + ".border.show";
    public static final String PROPERTY_LEGEND_BORDER_SHOW_LABEL = "Show Legend Border";
    public static final String PROPERTY_LEGEND_BORDER_SHOW_TOOLTIP = "Display a border around the full legend";
    private static final String PROPERTY_LEGEND_BORDER_SHOW_ALIAS = PROPERTY_LEGEND_BORDER_ROOT_ALIAS + "BorderShow";
    public static final boolean PROPERTY_LEGEND_BORDER_SHOW_DEFAULT = false;
    public static final Class PROPERTY_LEGEND_BORDER_SHOW_TYPE = Boolean.class;

    public static final String PROPERTY_LEGEND_BORDER_WIDTH_KEY = PROPERTY_LEGEND_BORDER_ROOT_KEY + ".border.width";
    public static final String PROPERTY_LEGEND_BORDER_WIDTH_LABEL = "Legend Border Width";
    public static final String PROPERTY_LEGEND_BORDER_WIDTH_TOOLTIP = "Width of border line around the full legend";
    private static final String PROPERTY_LEGEND_BORDER_WIDTH_ALIAS = PROPERTY_LEGEND_BORDER_ROOT_ALIAS + "BorderWidth";
    public static final int PROPERTY_LEGEND_BORDER_WIDTH_DEFAULT = 3;
    public static final Class PROPERTY_LEGEND_BORDER_WIDTH_TYPE = Integer.class;

    public static final String PROPERTY_LEGEND_BORDER_COLOR_KEY = PROPERTY_LEGEND_BORDER_ROOT_KEY + ".border.color";
    public static final String PROPERTY_LEGEND_BORDER_COLOR_LABEL = "Legend Border " + COLOR_MIXED_CASE;
    public static final String PROPERTY_LEGEND_BORDER_COLOR_TOOLTIP = COLOR_MIXED_CASE + " of border line around the full legend";
    private static final String PROPERTY_LEGEND_BORDER_COLOR_ALIAS = PROPERTY_LEGEND_BORDER_ROOT_ALIAS + "BorderColor";
    public static final Color PROPERTY_LEGEND_BORDER_COLOR_DEFAULT = Color.BLACK;
    public static final Class PROPERTY_LEGEND_BORDER_COLOR_TYPE = Color.class;


    // Sizing Section

    private static final String PROPERTY_LEGEND_SIZING_ROOT_KEY = PROPERTY_ROOT_KEY + ".legend.sizing";
    private static final String PROPERTY_LEGEND_SIZING_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "LegendSizing";

    public static final String PROPERTY_LEGEND_SIZING_SECTION_KEY = PROPERTY_LEGEND_SIZING_ROOT_KEY + ".section";
    public static final String PROPERTY_LEGEND_SIZING_SECTION_ALIAS = PROPERTY_LEGEND_SIZING_ROOT_ALIAS + "Section";
    public static final String PROPERTY_LEGEND_SIZING_SECTION_LABEL = "General Sizing";
    public static final String PROPERTY_LEGEND_SIZING_SECTION_TOOLTIP = "Configuration options for sizing of components of the legend";



    // Margins Section

    private static final String PROPERTY_LEGEND_BORDER_GAP_ROOT_KEY = PROPERTY_ROOT_KEY + ".margins";
    private static final String PROPERTY_LEGEND_BORDER_GAP_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "Margins";

    public static final String PROPERTY_LEGEND_BORDER_GAP_SECTION_KEY = PROPERTY_LEGEND_BORDER_GAP_ROOT_KEY + ".section";
    public static final String PROPERTY_LEGEND_BORDER_GAP_SECTION_ALIAS = PROPERTY_LEGEND_BORDER_GAP_ROOT_ALIAS + "Section";
    public static final String PROPERTY_LEGEND_BORDER_GAP_SECTION_LABEL = "Margins";
    public static final String PROPERTY_LEGEND_BORDER_GAP_SECTION_TOOLTIP = "Configuration options for adding gap within the border around the full legend";

    public static final String PROPERTY_LEGEND_BORDER_GAP_TOP_KEY = PROPERTY_LEGEND_BORDER_GAP_ROOT_KEY + ".top";
    public static final String PROPERTY_LEGEND_BORDER_GAP_TOP_LABEL = "Top Margin";
    public static final String PROPERTY_LEGEND_BORDER_GAP_TOP_TOOLTIP = "Adjusts the top margin (relative to color bar orientation)";
    private static final String PROPERTY_LEGEND_BORDER_GAP_TOP_ALIAS = PROPERTY_LEGEND_BORDER_GAP_ROOT_ALIAS + "Top";
    public static final double PROPERTY_LEGEND_BORDER_GAP_TOP_DEFAULT = 0.3;
    public static final Class PROPERTY_LEGEND_BORDER_GAP_TOP_TYPE = Double.class;

    public static final String PROPERTY_LEGEND_BORDER_GAP_BOTTOM_KEY = PROPERTY_LEGEND_BORDER_GAP_ROOT_KEY + ".bottom";
    public static final String PROPERTY_LEGEND_BORDER_GAP_BOTTOM_LABEL = "Bottom Margin";
    public static final String PROPERTY_LEGEND_BORDER_GAP_BOTTOM_TOOLTIP = "Adjusts the bottom margin (relative to color bar orientation)d";
    private static final String PROPERTY_LEGEND_BORDER_GAP_BOTTOM_ALIAS = PROPERTY_LEGEND_BORDER_GAP_ROOT_ALIAS + "Bottom";
    public static final double PROPERTY_LEGEND_BORDER_GAP_BOTTOM_DEFAULT = 0.3;
    public static final Class PROPERTY_LEGEND_BORDER_GAP_BOTTOM_TYPE = Double.class;

    public static final String PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_KEY = PROPERTY_LEGEND_BORDER_GAP_ROOT_KEY + ".leftside";
    public static final String PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_LABEL = "Left Margin";
    public static final String PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_TOOLTIP = "Adjusts the left-side margin (relative to color bar orientation)";
    private static final String PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_ALIAS = PROPERTY_LEGEND_BORDER_GAP_ROOT_ALIAS + "LeftSide";
    public static final double PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_DEFAULT = 0.3;
    public static final Class PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_TYPE = Double.class;

    public static final String PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_KEY = PROPERTY_LEGEND_BORDER_GAP_ROOT_KEY + ".rightside";
    public static final String PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_LABEL = "Right Margin";
    public static final String PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_TOOLTIP = "Adjusts the right-side margin (relative to color bar orientation)";
    private static final String PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_ALIAS = PROPERTY_LEGEND_BORDER_GAP_ROOT_ALIAS + "RightSide";
    public static final double PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_DEFAULT = 0.3;
    public static final Class PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_TYPE = Double.class;

    public static final String PROPERTY_LEGEND_TITLE_GAP_KEY = PROPERTY_LEGEND_BORDER_GAP_ROOT_KEY + ".title.gap";
    public static final String PROPERTY_LEGEND_TITLE_GAP_LABEL = "Title Gap";
    public static final String PROPERTY_LEGEND_TITLE_GAP_TOOLTIP = "Gap between title and color bar palette";
    private static final String PROPERTY_LEGEND_TITLE_GAP_ALIAS = PROPERTY_LEGEND_BORDER_GAP_ROOT_ALIAS + "TitleGap";
    public static final double PROPERTY_LEGEND_TITLE_GAP_DEFAULT = 0.6;
    public static final Class PROPERTY_LEGEND_TITLE_GAP_TYPE = Double.class;

    public static final String PROPERTY_LEGEND_LABEL_GAP_KEY = PROPERTY_LEGEND_BORDER_GAP_ROOT_KEY + ".label.gap";
    public static final String PROPERTY_LEGEND_LABEL_GAP_LABEL = "Label Gap";
    public static final String PROPERTY_LEGEND_LABEL_GAP_TOOLTIP = "Gap between labels and color bar palette";
    private static final String PROPERTY_LEGEND_LABEL_GAP_ALIAS = PROPERTY_LEGEND_BORDER_GAP_ROOT_ALIAS + "LabelGap";
    public static final double PROPERTY_LEGEND_LABEL_GAP_DEFAULT = 0.6;
    public static final Class PROPERTY_LEGEND_LABEL_GAP_TYPE = Double.class;



    // Backdrop Section

    private static final String PROPERTY_BACKDROP_ROOT_KEY = PROPERTY_ROOT_KEY + ".backdrop";
    private static final String PROPERTY_BACKDROP_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "Backdrop";

    public static final String PROPERTY_BACKDROP_SECTION_KEY = PROPERTY_BACKDROP_ROOT_KEY + ".section";
    public static final String PROPERTY_BACKDROP_SECTION_ALIAS = PROPERTY_BACKDROP_ROOT_ALIAS + "Section";
    public static final String PROPERTY_BACKDROP_SECTION_LABEL = "Legend Backdrop";
    public static final String PROPERTY_BACKDROP_SECTION_TOOLTIP = "Configuration options for the " + COLOR_LOWER_CASE + " bar legend backdrop";

    public static final String PROPERTY_BACKDROP_SHOW_KEY = PROPERTY_BACKDROP_ROOT_KEY + ".show";
    public static final String PROPERTY_BACKDROP_SHOW_LABEL = "Show Backdrop";
    public static final String PROPERTY_BACKDROP_SHOW_TOOLTIP = "Show the " + COLOR_LOWER_CASE + " bar legend backdrop";
    private static final String PROPERTY_BACKDROP_SHOW_ALIAS = PROPERTY_BACKDROP_ROOT_ALIAS + "Show";
    public static final boolean PROPERTY_BACKDROP_SHOW_DEFAULT = true;
    public static final Class PROPERTY_BACKDROP_SHOW_TYPE = Boolean.class;


    public static final String PROPERTY_BACKDROP_TRANSPARENCY_KEY = PROPERTY_BACKDROP_ROOT_KEY + "transparency";
    public static final String PROPERTY_BACKDROP_TRANSPARENCY_LABEL = "Backdrop Transparency";
    public static final String PROPERTY_BACKDROP_TRANSPARENCY_TOOLTIP = "Set transparency of the " + COLOR_LOWER_CASE + " bar legend backdrop";
    private static final String PROPERTY_BACKDROP_TRANSPARENCY_ALIAS = PROPERTY_BACKDROP_ROOT_ALIAS + "Transparency";
    public static final double PROPERTY_BACKDROP_TRANSPARENCY_DEFAULT = 0.0;
    public static final Class PROPERTY_BACKDROP_TRANSPARENCY_TYPE = Double.class;

    public static final String PROPERTY_BACKDROP_COLOR_KEY = PROPERTY_BACKDROP_ROOT_KEY + ".color";
    public static final String PROPERTY_BACKDROP_COLOR_LABEL = "Backdrop " + COLOR_MIXED_CASE;
    public static final String PROPERTY_BACKDROP_COLOR_TOOLTIP = "Set " + COLOR_LOWER_CASE + " of the backdrop of the " + COLOR_LOWER_CASE + " bar legend backdrop";
    private static final String PROPERTY_BACKDROP_COLOR_ALIAS = PROPERTY_BACKDROP_ROOT_ALIAS + "Color";
    public static final Color PROPERTY_BACKDROP_COLOR_DEFAULT = Color.WHITE;
    public static final Class PROPERTY_BACKDROP_COLOR_TYPE = Color.class;



    //  Legend Export Section

    private static final String PROPERTY_LEGEND_EXPORT_ROOT_KEY = PROPERTY_ROOT_KEY + ".legend.export";
    private static final String PROPERTY_LEGEND_EXPORT_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "LegendExport";

    public static final String PROPERTY_LEGEND_EXPORT_SECTION_KEY = PROPERTY_LEGEND_EXPORT_ROOT_KEY + ".section";
    public static final String PROPERTY_LEGEND_EXPORT_SECTION_ALIAS = PROPERTY_LEGEND_EXPORT_ROOT_ALIAS + "Section";
    public static final String PROPERTY_LEGEND_EXPORT_SECTION_LABEL = COLOR_MIXED_CASE + " Bar Export Tool";
    public static final String PROPERTY_LEGEND_EXPORT_SECTION_TOOLTIP = "Configuration options for the " + COLOR_LOWER_CASE + " bar legend export tool";

    public static final String PROPERTY_EXPORT_EDITOR_SHOW_KEY = PROPERTY_LEGEND_EXPORT_ROOT_KEY + ".editor.show";
    public static final String PROPERTY_EXPORT_EDITOR_SHOW_LABEL = "Show Editor";
    public static final String PROPERTY_EXPORT_EDITOR_SHOW_TOOLTIP = "Display editor for " + COLOR_LOWER_CASE + " bar export tool";
    private static final String PROPERTY_EXPORT_EDITOR_SHOW_ALIAS = PROPERTY_LEGEND_EXPORT_ROOT_ALIAS + "EditorShow";
    public static final boolean PROPERTY_EXPORT_EDITOR_SHOW_DEFAULT = false;
    public static final Class PROPERTY_EXPORT_EDITOR_SHOW_TYPE = Boolean.class;

    public static final String PROPERTY_EXPORT_USE_BW_COLOR_KEY = PROPERTY_LEGEND_EXPORT_ROOT_KEY + ".use.blackwhite.color";
    public static final String PROPERTY_EXPORT_USE_BW_COLOR_LABEL = "Black/White " + COLOR_MIXED_CASE + " Override";
    public static final String PROPERTY_EXPORT_USE_BW_COLOR_TOOLTIP = "Overrides " + COLOR_LOWER_CASE + "s and uses black & white";
    private static final String PROPERTY_EXPORT_USE_BW_COLOR_ALIAS = PROPERTY_LEGEND_EXPORT_ROOT_ALIAS + "UseBlackWhiteColor";
    public static final boolean PROPERTY_EXPORT_USE_BW_COLOR_DEFAULT = false;
    public static final Class PROPERTY_EXPORT_USE_BW_COLOR_TYPE = Boolean.class;

    public static final String PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY = PROPERTY_LEGEND_EXPORT_ROOT_KEY + "use.legend.size";
    public static final String PROPERTY_EXPORT_USE_LEGEND_WIDTH_LABEL = "Scale to File Size";
    public static final String PROPERTY_EXPORT_USE_LEGEND_WIDTH_TOOLTIP = "Resize to desired legend size";
    private static final String PROPERTY_EXPORT_USE_LEGEND_WIDTH_ALIAS = PROPERTY_LEGEND_EXPORT_ROOT_ALIAS + "UseLegendSize";
    public static final boolean PROPERTY_EXPORT_USE_LEGEND_WIDTH_DEFAULT = true;
    public static final Class PROPERTY_EXPORT_USE_LEGEND_WIDTH_TYPE = Boolean.class;

    public static final String PROPERTY_EXPORT_LEGEND_WIDTH_KEY = PROPERTY_LEGEND_EXPORT_ROOT_KEY + ".legend.size";
    public static final String PROPERTY_EXPORT_LEGEND_WIDTH_LABEL = "File Size";
    public static final String PROPERTY_EXPORT_LEGEND_WIDTH_TOOLTIP = "Width (in pixels) of legend image file (height if vertical image)";
    private static final String PROPERTY_EXPORT_LEGEND_WIDTH_ALIAS = PROPERTY_LEGEND_EXPORT_ROOT_ALIAS + "LegendSize";
    public static final int PROPERTY_EXPORT_LEGEND_WIDTH_DEFAULT = 2400;
    public static final Class PROPERTY_EXPORT_LEGEND_WIDTH_TYPE = Integer.class;





    // ---------------------------------------------------------

    public static final String PROPERTY_NAME_RASTER = "raster";




    // Property Setting: Restore Defaults

    private static final String PROPERTY_RESTORE_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".restore.defaults";

    public static final String PROPERTY_RESTORE_SECTION_KEY = PROPERTY_RESTORE_KEY_SUFFIX + ".section";
    public static final String PROPERTY_RESTORE_SECTION_LABEL = "Restore";
    public static final String PROPERTY_RESTORE_SECTION_TOOLTIP = "Restores preferences to the package defaults";

    public static final String PROPERTY_RESTORE_DEFAULTS_KEY = PROPERTY_RESTORE_KEY_SUFFIX + ".apply";
    public static final String PROPERTY_RESTORE_DEFAULTS_LABEL = "Default (" + COLOR_MIXED_CASE + " Bar Legend Preferences)";
    public static final String PROPERTY_RESTORE_DEFAULTS_TOOLTIP = "Restore all " + COLOR_LOWER_CASE + " bar legend preferences to the original default";
    public static final boolean PROPERTY_RESTORE_DEFAULTS_DEFAULT = false;









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


        final Property autoApplySchemesModel = Property.create(PROPERTY_SCHEME_AUTO_APPLY_KEY, Boolean.class, PROPERTY_SCHEME_AUTO_APPLY_DEFAULT, true);
        autoApplySchemesModel.getDescriptor().setAlias(PROPERTY_SCHEME_AUTO_APPLY_ALIAS);
        vc.addProperty(autoApplySchemesModel);

        final Property schemeLabelsApplyModel = Property.create(PROPERTY_SCHEME_LABELS_APPLY_KEY, Boolean.class, PROPERTY_SCHEME_LABELS_APPLY_DEFAULT, true);
        schemeLabelsApplyModel.getDescriptor().setAlias(PROPERTY_SCHEME_LABELS_APPLY_ALIAS);
        vc.addProperty(schemeLabelsApplyModel);

        final Property schemeLabelsRestrictModel = Property.create(PROPERTY_SCHEME_LABELS_RESTRICT_KEY, Boolean.class, PROPERTY_SCHEME_LABELS_RESTRICT_DEFAULT, true);
        schemeLabelsRestrictModel.getDescriptor().setAlias(PROPERTY_SCHEME_LABELS_RESTRICT_ALIAS);
        vc.addProperty(schemeLabelsRestrictModel);




        // Title Section

        final Property titleSectionModel = Property.create(PROPERTY_HEADER_TITLE_SECTION_KEY, Boolean.class, true, true);
        titleSectionModel.getDescriptor().setAlias(PROPERTY_HEADER_TITLE_SECTION_ALIAS);
        vc.addProperty(titleSectionModel);




        final Property titleModel = Property.create(PROPERTY_TITLE_KEY,
                PROPERTY_TITLE_TYPE,
                PROPERTY_TITLE_DEFAULT,
                true);
        titleModel.getDescriptor().setAlias(PROPERTY_TITLE_ALIAS);
        vc.addProperty(titleModel);



        final Property headerUnitsSectionModel = Property.create(PROPERTY_HEADER_UNITS_SECTION_KEY, Boolean.class, true, true);
        headerUnitsSectionModel.getDescriptor().setAlias(PROPERTY_HEADER_UNITS_SECTION_ALIAS);
        vc.addProperty(headerUnitsSectionModel);

        final Property unitsModel = Property.create(PROPERTY_UNITS_KEY,
                PROPERTY_UNITS_TYPE,
                PROPERTY_UNITS_DEFAULT,
                true);
        unitsModel.getDescriptor().setAlias(PROPERTY_UNITS_ALIAS);
        vc.addProperty(unitsModel);

        final Property unitsNullModel = Property.create(PROPERTY_UNITS_NULL_KEY,
                PROPERTY_UNITS_NULL_TYPE,
                PROPERTY_UNITS_NULL_DEFAULT,
                true);
        unitsNullModel.getDescriptor().setAlias(PROPERTY_UNITS_NULL_ALIAS);
        vc.addProperty(unitsNullModel);

        final Property convertCaretModel = Property.create(PROPERTY_CONVERT_CARET_KEY,
                PROPERTY_CONVERT_CARET_TYPE,
                PROPERTY_CONVERT_CARET_DEFAULT,
                true);
        convertCaretModel.getDescriptor().setAlias(PROPERTY_CONVERT_CARET_ALIAS);
        vc.addProperty(convertCaretModel);

        final Property unitsParenthesisModel = Property.create(PROPERTY_UNITS_PARENTHESIS_KEY,
                PROPERTY_UNITS_PARENTHESIS_TYPE,
                PROPERTY_UNITS_PARENTHESIS_DEFAULT,
                true);
        unitsParenthesisModel.getDescriptor().setAlias(PROPERTY_UNITS_PARENTHESIS_ALIAS);
        vc.addProperty(unitsParenthesisModel);






        // Orientation Section

        final Property formattingSectionModel = Property.create(PROPERTY_ORIENTATION_SECTION_KEY, Boolean.class, true, true);
        formattingSectionModel.getDescriptor().setAlias(PROPERTY_ORIENTATION_SECTION_ALIAS);
        vc.addProperty(formattingSectionModel);

        final Property formattingOrientationModel = Property.create(PROPERTY_ORIENTATION_KEY, PROPERTY_ORIENTATION_TYPE, true, true);
        formattingOrientationModel.getDescriptor().setAlias(PROPERTY_ORIENTATION_ALIAS);
        vc.addProperty(formattingOrientationModel);

        final Property sceneAspectBestFitModel = Property.create(PROPERTY_SCENE_ASPECT_BEST_FIT_KEY, PROPERTY_SCENE_ASPECT_BEST_FIT_TYPE, true, true);
        sceneAspectBestFitModel.getDescriptor().setAlias(PROPERTY_SCENE_ASPECT_BEST_FIT_ALIAS);
        vc.addProperty(sceneAspectBestFitModel);

        final Property reversePaletteModel = Property.create(PROPERTY_ORIENTATION_REVERSE_PALETTE_KEY, PROPERTY_ORIENTATION_REVERSE_PALETTE_TYPE, true, true);
        reversePaletteModel.getDescriptor().setAlias(PROPERTY_ORIENTATION_REVERSE_PALETTE_ALIAS);
        vc.addProperty(reversePaletteModel);



        // Tick Label Values

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

        final Property populateLabelValesTextfieldModel = Property.create(PROPERTY_POPULATE_VALUES_TEXTFIELD_KEY, Boolean.class, PROPERTY_POPULATE_VALUES_TEXTFIELD_DEFAULT , true);
        populateLabelValesTextfieldModel.getDescriptor().setAlias(PROPERTY_POPULATE_VALUES_TEXTFIELD_ALIAS);
        vc.addProperty(populateLabelValesTextfieldModel);

        final Property labelValuesScalingFactorModel = Property.create(PROPERTY_LABEL_VALUES_SCALING_KEY, Double.class, PROPERTY_LABEL_VALUES_SCALING_DEFAULT, true);
        labelValuesScalingFactorModel.getDescriptor().setAlias(PROPERTY_LABEL_VALUES_SCALING_ALIAS);
        vc.addProperty(labelValuesScalingFactorModel);

        final Property labelValuesDecimalPlacesModel = Property.create(PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY, Integer.class, PROPERTY_LABEL_VALUES_DECIMAL_PLACES_DEFAULT, true);
        labelValuesDecimalPlacesModel.getDescriptor().setAlias(PROPERTY_LABEL_VALUES_DECIMAL_PLACES_ALIAS);
        vc.addProperty(labelValuesDecimalPlacesModel);

        final Property labelValuesForceDecimalPlacesModel = Property.create(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY, Boolean.class, PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_DEFAULT , true);
        labelValuesForceDecimalPlacesModel.getDescriptor().setAlias(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_ALIAS);
        vc.addProperty(labelValuesForceDecimalPlacesModel);



        final Property weightToleranceModel = Property.create(PROPERTY_WEIGHT_TOLERANCE_KEY, Double.class, PROPERTY_WEIGHT_TOLERANCE_DEFAULT, true);
        weightToleranceModel.getDescriptor().setAlias(PROPERTY_WEIGHT_TOLERANCE_ALIAS);
        vc.addProperty(weightToleranceModel);


        // Placement Section

        final Property locationSectionModel = Property.create(PROPERTY_LOCATION_SECTION_KEY, Boolean.class, true, true);
        locationSectionModel.getDescriptor().setAlias(PROPERTY_LOCATION_SECTION_ALIAS);
        vc.addProperty(locationSectionModel);

        final Property locationInsideModel = Property.create(PROPERTY_LOCATION_INSIDE_KEY, PROPERTY_LOCATION_INSIDE_TYPE, true, true);
        locationInsideModel.getDescriptor().setAlias(PROPERTY_LOCATION_INSIDE_ALIAS);
        vc.addProperty(locationInsideModel);

        final Property locationHorizontalModel = Property.create(PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_KEY,
                PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_TYPE, true, true);
        locationHorizontalModel.getDescriptor().setAlias(PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_ALIAS);
        vc.addProperty(locationHorizontalModel);

        final Property locationVerticalModel = Property.create(PROPERTY_LOCATION_PLACEMENT_VERTICAL_KEY,
                PROPERTY_LOCATION_PLACEMENT_VERTICAL_TYPE, true, true);
        locationVerticalModel.getDescriptor().setAlias(PROPERTY_LOCATION_PLACEMENT_VERTICAL_ALIAS);
        vc.addProperty(locationVerticalModel);

        final Property locationGapFactorModel = Property.create(PROPERTY_LOCATION_GAP_FACTOR_KEY, PROPERTY_LOCATION_GAP_FACTOR_TYPE, true, true);
        locationGapFactorModel.getDescriptor().setAlias(PROPERTY_LOCATION_GAP_FACTOR_ALIAS);
        vc.addProperty(locationGapFactorModel);



        final Property locationOffsetModel = Property.create(PROPERTY_LOCATION_OFFSET_KEY, PROPERTY_LOCATION_OFFSET_TYPE, true, true);
        locationOffsetModel.getDescriptor().setAlias(PROPERTY_LOCATION_OFFSET_ALIAS);
        vc.addProperty(locationOffsetModel);

        final Property locationShiftModel = Property.create(PROPERTY_LOCATION_SHIFT_KEY, PROPERTY_LOCATION_SHIFT_TYPE, true, true);
        locationShiftModel.getDescriptor().setAlias(PROPERTY_LOCATION_SHIFT_ALIAS);
        vc.addProperty(locationShiftModel);

        final Property titleVerticalAnchorModel = Property.create(PROPERTY_LOCATION_TITLE_VERTICAL_KEY,
                PROPERTY_LOCATION_TITLE_VERTICAL_TYPE, true, true);
        titleVerticalAnchorModel.getDescriptor().setAlias(PROPERTY_LOCATION_TITLE_VERTICAL_ALIAS);
        vc.addProperty(titleVerticalAnchorModel);



        // Size & Scaling Section

        final Property scalingSectionModel = Property.create(PROPERTY_IMAGE_SCALING_SECTION_KEY, Boolean.class, true, true);
        scalingSectionModel.getDescriptor().setAlias(PROPERTY_IMAGE_SCALING_SECTION_ALIAS);
        vc.addProperty(scalingSectionModel);

        final Property locationApplySizeScalingModel = Property.create(PROPERTY_IMAGE_SCALING_APPLY_SIZE_KEY, PROPERTY_IMAGE_SCALING_APPLY_SIZE_TYPE, true, true);
        locationApplySizeScalingModel.getDescriptor().setAlias(PROPERTY_IMAGE_SCALING_APPLY_SIZE_ALIAS);
        vc.addProperty(locationApplySizeScalingModel);

        final Property locationSizeScalingModel = Property.create(PROPERTY_IMAGE_SCALING_SIZE_KEY, ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_TYPE, true, true);
        locationSizeScalingModel.getDescriptor().setAlias(PROPERTY_IMAGE_SCALING_SIZE_ALIAS);
        vc.addProperty(locationSizeScalingModel);

        final Property colorbarLengthModel = Property.create(PROPERTY_COLORBAR_LENGTH_KEY, ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_TYPE, true, true);
        colorbarLengthModel.getDescriptor().setAlias(PROPERTY_COLORBAR_LENGTH_ALIAS);
        vc.addProperty(colorbarLengthModel);

        final Property colorbarWidthModel = Property.create(PROPERTY_COLORBAR_WIDTH_KEY, ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_TYPE, true, true);
        colorbarWidthModel.getDescriptor().setAlias(PROPERTY_COLORBAR_WIDTH_ALIAS);
        vc.addProperty(colorbarWidthModel);




        // Title Format Section

        final Property titleParameterSectionModel = Property.create(PROPERTY_TITLE_SECTION_KEY,
                Boolean.class,
                true,
                true);
        titleParameterSectionModel.getDescriptor().setAlias(PROPERTY_TITLE_SECTION_ALIAS);
        vc.addProperty(titleParameterSectionModel);


        final Property titleParameterShowModel = Property.create(PROPERTY_TITLE_SHOW_KEY,
                PROPERTY_TITLE_SHOW_TYPE,
                PROPERTY_TITLE_SHOW_DEFAULT,
                true);
        titleParameterShowModel.getDescriptor().setAlias(PROPERTY_TITLE_SHOW_ALIAS);
        vc.addProperty(titleParameterShowModel);


        final Property titleParameterFontSizeModel = Property.create(PROPERTY_TITLE_FONT_SIZE_KEY,
                PROPERTY_TITLE_FONT_SIZE_TYPE,
                PROPERTY_TITLE_FONT_SIZE_DEFAULT,
                true);
        titleParameterFontSizeModel.getDescriptor().setAlias(PROPERTY_TITLE_FONT_SIZE_ALIAS);
        vc.addProperty(titleParameterFontSizeModel);

        final Property titleParameterBoldModel = Property.create(PROPERTY_TITLE_FONT_BOLD_KEY,
                PROPERTY_TITLE_FONT_BOLD_TYPE,
                PROPERTY_TITLE_FONT_BOLD_DEFAULT,
                true);
        titleParameterBoldModel.getDescriptor().setAlias(PROPERTY_TITLE_FONT_BOLD_ALIAS);
        vc.addProperty(titleParameterBoldModel);


        final Property titleParameterItalicModel = Property.create(PROPERTY_TITLE_FONT_ITALIC_KEY,
                PROPERTY_TITLE_FONT_ITALIC_TYPE,
                PROPERTY_TITLE_FONT_ITALIC_DEFAULT,
                true);
        titleParameterItalicModel.getDescriptor().setAlias(PROPERTY_TITLE_FONT_ITALIC_ALIAS);
        vc.addProperty(titleParameterItalicModel);


        final Property titleParameterFontNameModel = Property.create(PROPERTY_TITLE_FONT_NAME_KEY,
                PROPERTY_TITLE_FONT_NAME_TYPE,
                PROPERTY_TITLE_FONT_NAME_DEFAULT,
                true);
        titleParameterFontNameModel.getDescriptor().setAlias(PROPERTY_TITLE_FONT_NAME_ALIAS);
        vc.addProperty(titleParameterFontNameModel);


        final Property titleParameterColorModel = Property.create(PROPERTY_TITLE_COLOR_KEY,
                PROPERTY_TITLE_COLOR_TYPE,
                PROPERTY_TITLE_COLOR_DEFAULT,
                true);
        titleParameterColorModel.getDescriptor().setAlias(PROPERTY_TITLE_COLOR_ALIAS);
        vc.addProperty(titleParameterColorModel);







        // Units Section

        final Property titleUnitsSectionModel = Property.create(PROPERTY_UNITS_SECTION_KEY,
                Boolean.class,
                true,
                true);
        titleUnitsSectionModel.getDescriptor().setAlias(PROPERTY_UNITS_SECTION_ALIAS);
        vc.addProperty(titleUnitsSectionModel);


        final Property titleUnitsShowModel = Property.create(PROPERTY_UNITS_SHOW_KEY,
                PROPERTY_UNITS_SHOW_TYPE,
                PROPERTY_UNITS_SHOW_DEFAULT,
                true);
        titleUnitsShowModel.getDescriptor().setAlias(PROPERTY_UNITS_SHOW_ALIAS);
        vc.addProperty(titleUnitsShowModel);


        final Property titleUnitsFontSizeModel = Property.create(PROPERTY_UNITS_FONT_SIZE_KEY,
                PROPERTY_UNITS_FONT_SIZE_TYPE,
                PROPERTY_UNITS_FONT_SIZE_DEFAULT,
                true);
        titleUnitsFontSizeModel.getDescriptor().setAlias(PROPERTY_UNITS_FONT_SIZE_ALIAS);
        vc.addProperty(titleUnitsFontSizeModel);


        final Property titleUnitsBoldModel = Property.create(PROPERTY_UNITS_FONT_BOLD_KEY,
                PROPERTY_UNITS_FONT_BOLD_TYPE,
                PROPERTY_UNITS_FONT_BOLD_DEFAULT,
                true);
        titleUnitsBoldModel.getDescriptor().setAlias(PROPERTY_UNITS_FONT_BOLD_ALIAS);
        vc.addProperty(titleUnitsBoldModel);


        final Property titleUnitsItalicModel = Property.create(PROPERTY_UNITS_FONT_ITALIC_KEY,
                PROPERTY_UNITS_FONT_ITALIC_TYPE,
                PROPERTY_UNITS_FONT_ITALIC_DEFAULT,
                true);
        titleUnitsItalicModel.getDescriptor().setAlias(PROPERTY_UNITS_FONT_ITALIC_ALIAS);
        vc.addProperty(titleUnitsItalicModel);


        final Property titleUnitsFontNameModel = Property.create(PROPERTY_UNITS_FONT_NAME_KEY,
                PROPERTY_UNITS_FONT_NAME_TYPE,
                PROPERTY_UNITS_FONT_NAME_DEFAULT,
                true);
        titleUnitsFontNameModel.getDescriptor().setAlias(PROPERTY_UNITS_FONT_NAME_ALIAS);
        vc.addProperty(titleUnitsFontNameModel);


        final Property titleUnitsColorModel = Property.create(PROPERTY_UNITS_FONT_COLOR_KEY,
                PROPERTY_UNITS_FONT_COLOR_TYPE,
                PROPERTY_UNITS_FONT_COLOR_DEFAULT,
                true);
        titleUnitsColorModel.getDescriptor().setAlias(PROPERTY_UNITS_FONT_COLOR_ALIAS);
        vc.addProperty(titleUnitsColorModel);




        // Tick Label Format Section

        final Property labelsSectionModel = Property.create(PROPERTY_LABELS_SECTION_KEY, Boolean.class, true, true);
        labelsSectionModel.getDescriptor().setAlias(PROPERTY_LABELS_SECTION_ALIAS);
        vc.addProperty(labelsSectionModel);

        final Property labelsShowModel = Property.create(PROPERTY_LABELS_SHOW_KEY,
                PROPERTY_LABELS_SHOW_TYPE,
                PROPERTY_LABELS_SHOW_DEFAULT,
                true);
        labelsShowModel.getDescriptor().setAlias(PROPERTY_LABELS_SHOW_ALIAS);
        vc.addProperty(labelsShowModel);

        final Property textFontSizeModel = Property.create(PROPERTY_LABELS_FONT_SIZE_KEY, Integer.class, PROPERTY_LABELS_FONT_SIZE_DEFAULT, true);
        textFontSizeModel.getDescriptor().setAlias(PROPERTY_LABELS_FONT_SIZE_ALIAS);
        vc.addProperty(textFontSizeModel);

        final Property textFontBoldModel = Property.create(PROPERTY_LABELS_FONT_BOLD_KEY, Boolean.class, PROPERTY_LABELS_FONT_BOLD_DEFAULT, true);
        textFontBoldModel.getDescriptor().setAlias(PROPERTY_LABELS_FONT_BOLD_ALIAS);
        vc.addProperty(textFontBoldModel);
        final Property textFontItalicModel = Property.create(PROPERTY_LABELS_FONT_ITALIC_KEY, Boolean.class, PROPERTY_LABELS_FONT_ITALIC_DEFAULT, true);
        textFontItalicModel.getDescriptor().setAlias(PROPERTY_LABELS_FONT_ITALIC_ALIAS);
        vc.addProperty(textFontItalicModel);

        final Property textFontModel = Property.create(PROPERTY_LABELS_FONT_NAME_KEY, String.class, PROPERTY_LABELS_FONT_NAME_DEFAULT, true);
        textFontModel.getDescriptor().setAlias(PROPERTY_LABELS_FONT_NAME_ALIAS);
        vc.addProperty(textFontModel);

        final Property textFgColorModel = Property.create(PROPERTY_LABELS_FONT_COLOR_KEY, Color.class, PROPERTY_LABELS_FONT_COLOR_DEFAULT, true);
        textFgColorModel.getDescriptor().setAlias(PROPERTY_LABELS_FONT_COLOR_ALIAS);
        vc.addProperty(textFgColorModel);



        // Tickmarks Section

        final Property tickmarksSectionModel = Property.create(PROPERTY_TICKMARKS_SECTION_KEY, Boolean.class, true, true);
        tickmarksSectionModel.getDescriptor().setAlias(PROPERTY_TICKMARKS_SECTION_ALIAS);
        vc.addProperty(tickmarksSectionModel);

        final Property tickMarkEnabledModel = Property.create(PROPERTY_TICKMARKS_SHOW_KEY, PROPERTY_TICKMARKS_SHOW_TYPE, PROPERTY_TICKMARKS_SHOW_DEFAULT, true);
        tickMarkEnabledModel.getDescriptor().setAlias(PROPERTY_TICKMARKS_SHOW_ALIAS);
        vc.addProperty(tickMarkEnabledModel);

        final Property tickMarkLengthModel = Property.create(PROPERTY_TICKMARKS_LENGTH_KEY, PROPERTY_TICKMARKS_LENGTH_TYPE, PROPERTY_TICKMARKS_LENGTH_DEFAULT, true);
        tickMarkLengthModel.getDescriptor().setAlias(PROPERTY_TICKMARKS_LENGTH_ALIAS);
        vc.addProperty(tickMarkLengthModel);

        final Property tickMarkWidthModel = Property.create(PROPERTY_TICKMARKS_WIDTH_KEY, PROPERTY_TICKMARKS_WIDTH_TYPE, PROPERTY_TICKMARKS_LENGTH_DEFAULT, true);
        tickMarkWidthModel.getDescriptor().setAlias(PROPERTY_TICKMARKS_WIDTH_ALIAS);
        vc.addProperty(tickMarkWidthModel);

        final Property tickmarkColorModel = Property.create(PROPERTY_TICKMARKS_COLOR_KEY, PROPERTY_TICKMARKS_COLOR_TYPE, PROPERTY_TICKMARKS_COLOR_DEFAULT, true);
        tickmarkColorModel.getDescriptor().setAlias(PROPERTY_TICKMARKS_COLOR_ALIAS);
        vc.addProperty(tickmarkColorModel);


        // Palette Border Section

        final Property borderSectionModel = Property.create(PROPERTY_PALETTE_BORDER_SECTION_KEY, Boolean.class, true, true);
        borderSectionModel.getDescriptor().setAlias(PROPERTY_PALETTE_BORDER_SECTION_ALIAS);
        vc.addProperty(borderSectionModel);

        final Property borderEnabledModel = Property.create(PROPERTY_PALETTE_BORDER_SHOW_KEY, Boolean.class, PROPERTY_PALETTE_BORDER_SHOW_DEFAULT, true);
        borderEnabledModel.getDescriptor().setAlias(PROPERTY_PALETTE_BORDER_SHOW_ALIAS);
        vc.addProperty(borderEnabledModel);

        final Property borderWidthModel = Property.create(PROPERTY_PALETTE_BORDER_WIDTH_KEY, PROPERTY_PALETTE_BORDER_WIDTH_TYPE, PROPERTY_PALETTE_BORDER_WIDTH_DEFAULT, true);
        borderWidthModel.getDescriptor().setAlias(PROPERTY_PALETTE_BORDER_WIDTH_ALIAS);
        vc.addProperty(borderWidthModel);

        final Property borderColorModel = Property.create(PROPERTY_PALETTE_BORDER_COLOR_KEY, Color.class, PROPERTY_PALETTE_BORDER_COLOR_DEFAULT, true);
        borderColorModel.getDescriptor().setAlias(PROPERTY_PALETTE_BORDER_COLOR_ALIAS);
        vc.addProperty(borderColorModel);




        // Backdrop Section

        final Property insideLabelsSectionModel = Property.create(PROPERTY_BACKDROP_SECTION_KEY, Boolean.class, true, true);
        insideLabelsSectionModel.getDescriptor().setAlias(PROPERTY_BACKDROP_SECTION_ALIAS);
        vc.addProperty(insideLabelsSectionModel);

        final Property backdropShowModel = Property.create(PROPERTY_BACKDROP_SHOW_KEY, Boolean.class, PROPERTY_BACKDROP_SHOW_DEFAULT, true);
        backdropShowModel.getDescriptor().setAlias(PROPERTY_BACKDROP_SHOW_ALIAS);
        vc.addProperty(backdropShowModel);

        final Property backdropTransparencyModel = Property.create(PROPERTY_BACKDROP_TRANSPARENCY_KEY, Double.class, PROPERTY_BACKDROP_TRANSPARENCY_DEFAULT, true);
        backdropTransparencyModel.getDescriptor().setAlias(PROPERTY_BACKDROP_TRANSPARENCY_ALIAS);
        vc.addProperty(backdropTransparencyModel);

        final Property backdropColorModel = Property.create(PROPERTY_BACKDROP_COLOR_KEY, Color.class, PROPERTY_BACKDROP_COLOR_DEFAULT, true);
        backdropColorModel.getDescriptor().setAlias(PROPERTY_BACKDROP_COLOR_ALIAS);
        vc.addProperty(backdropColorModel);



        // Legend Border Section

        final Property backdropBorderSectionModel = Property.create(PROPERTY_LEGEND_BORDER_SECTION_KEY, Boolean.class, true, true);
        backdropBorderSectionModel.getDescriptor().setAlias(PROPERTY_LEGEND_BORDER_SECTION_ALIAS);
        vc.addProperty(backdropBorderSectionModel);

        final Property backdropBorderShowModel = Property.create(PROPERTY_LEGEND_BORDER_SHOW_KEY, Boolean.class, PROPERTY_LEGEND_BORDER_SHOW_DEFAULT, true);
        backdropBorderShowModel.getDescriptor().setAlias(PROPERTY_LEGEND_BORDER_SHOW_ALIAS);
        vc.addProperty(backdropBorderShowModel);

        final Property backdropBorderWidthModel = Property.create(PROPERTY_LEGEND_BORDER_WIDTH_KEY, Integer.class, PROPERTY_LEGEND_BORDER_WIDTH_DEFAULT, true);
        backdropBorderWidthModel.getDescriptor().setAlias(PROPERTY_LEGEND_BORDER_WIDTH_ALIAS);
        vc.addProperty(backdropBorderWidthModel);

        final Property backdropBorderColorModel = Property.create(PROPERTY_LEGEND_BORDER_COLOR_KEY, Color.class, PROPERTY_LEGEND_BORDER_COLOR_DEFAULT, true);
        backdropBorderColorModel.getDescriptor().setAlias(PROPERTY_LEGEND_BORDER_COLOR_ALIAS);
        vc.addProperty(backdropBorderColorModel);


        // Legend Sizing Section

        final Property sizingSectionModel = Property.create(PROPERTY_LEGEND_SIZING_SECTION_KEY, Boolean.class, true, true);
        sizingSectionModel.getDescriptor().setAlias(PROPERTY_LEGEND_SIZING_SECTION_ALIAS);
        vc.addProperty(sizingSectionModel);


        // Legend Border Gap Section

        final Property borderGapSectionModel = Property.create(PROPERTY_LEGEND_BORDER_GAP_SECTION_KEY, Boolean.class, true, true);
        borderGapSectionModel.getDescriptor().setAlias(PROPERTY_LEGEND_BORDER_GAP_SECTION_ALIAS);
        vc.addProperty(borderGapSectionModel);

        final Property borderGapLeftsideModel = Property.create(PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_KEY, Double.class, PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_DEFAULT, true);
        borderGapLeftsideModel.getDescriptor().setAlias(PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_ALIAS);
        vc.addProperty(borderGapLeftsideModel);

        final Property borderGapRightsideModel = Property.create(PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_KEY, Double.class, PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_DEFAULT, true);
        borderGapRightsideModel.getDescriptor().setAlias(PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_ALIAS);
        vc.addProperty(borderGapRightsideModel);

        final Property borderGapTopModel = Property.create(PROPERTY_LEGEND_BORDER_GAP_TOP_KEY, Double.class, PROPERTY_LEGEND_BORDER_GAP_TOP_DEFAULT, true);
        borderGapTopModel.getDescriptor().setAlias(PROPERTY_LEGEND_BORDER_GAP_TOP_ALIAS);
        vc.addProperty(borderGapTopModel);

        final Property borderGapBottomModel = Property.create(PROPERTY_LEGEND_BORDER_GAP_BOTTOM_KEY, Double.class, PROPERTY_LEGEND_BORDER_GAP_BOTTOM_DEFAULT, true);
        borderGapBottomModel.getDescriptor().setAlias(PROPERTY_LEGEND_BORDER_GAP_BOTTOM_ALIAS);
        vc.addProperty(borderGapBottomModel);

        final Property titleGapModel = Property.create(PROPERTY_LEGEND_TITLE_GAP_KEY, Double.class, PROPERTY_LEGEND_TITLE_GAP_DEFAULT, true);
        titleGapModel.getDescriptor().setAlias(PROPERTY_LEGEND_TITLE_GAP_ALIAS);
        vc.addProperty(titleGapModel);

        final Property labelGapModel = Property.create(PROPERTY_LEGEND_LABEL_GAP_KEY, Double.class, PROPERTY_LEGEND_LABEL_GAP_DEFAULT, true);
        labelGapModel.getDescriptor().setAlias(PROPERTY_LEGEND_LABEL_GAP_ALIAS);
        vc.addProperty(labelGapModel);





        // Legend Border Section

        final Property exportSectionModel = Property.create(PROPERTY_LEGEND_EXPORT_SECTION_KEY, Boolean.class, true, true);
        exportSectionModel.getDescriptor().setAlias(PROPERTY_LEGEND_EXPORT_SECTION_ALIAS);
        vc.addProperty(exportSectionModel);

        final Property exportEditorShowModel = Property.create(PROPERTY_EXPORT_EDITOR_SHOW_KEY, Boolean.class, PROPERTY_EXPORT_EDITOR_SHOW_DEFAULT, true);
        exportEditorShowModel.getDescriptor().setAlias(PROPERTY_EXPORT_EDITOR_SHOW_ALIAS);
        vc.addProperty(exportEditorShowModel);

        final Property exportBWColorShowModel = Property.create(PROPERTY_EXPORT_USE_BW_COLOR_KEY, Boolean.class, PROPERTY_EXPORT_USE_BW_COLOR_DEFAULT, true);
        exportBWColorShowModel.getDescriptor().setAlias(PROPERTY_EXPORT_USE_BW_COLOR_ALIAS);
        vc.addProperty(exportBWColorShowModel);

        final Property exportUseLegendWidthModel = Property.create(PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY, Boolean.class, PROPERTY_EXPORT_USE_LEGEND_WIDTH_DEFAULT, true);
        exportUseLegendWidthModel.getDescriptor().setAlias(PROPERTY_EXPORT_USE_LEGEND_WIDTH_ALIAS);
        vc.addProperty(exportUseLegendWidthModel);

        final Property exportLegendWidthModel = Property.create(PROPERTY_EXPORT_LEGEND_WIDTH_KEY, Integer.class, PROPERTY_EXPORT_LEGEND_WIDTH_DEFAULT, true);
        exportLegendWidthModel.getDescriptor().setAlias(PROPERTY_EXPORT_LEGEND_WIDTH_ALIAS);
        vc.addProperty(exportLegendWidthModel);








        // Other

        final Property rasterModel = Property.create(PROPERTY_NAME_RASTER, RasterDataNode.class);
        rasterModel.getDescriptor().setNotNull(true);
        vc.addProperty(rasterModel);

        final Property transformModel = Property.create(PROPERTY_NAME_TRANSFORM, new AffineTransform());
        transformModel.getDescriptor().setTransient(true);
        vc.addProperty(transformModel);




        return vc;
    }
}
