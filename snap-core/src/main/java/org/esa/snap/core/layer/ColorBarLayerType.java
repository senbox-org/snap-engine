

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

    public static final String DISTRIB_EVEN_STR = "Auto Values";
    public static final String DISTRIB_EXACT_STR = "Palette Values";
    public static final String DISTRIB_MANUAL_STR = "Custom Values";


    //--------------------------------------------------------------------------------------------------------------
    // Color Bar Legend Preferences parameters

    // Preferences property prefix
    private static final String PROPERTY_ROOT_KEY = "color.bar.legend";
    private static final String PROPERTY_ROOT_ALIAS = "colorBarLegend";


    // Label (Values)

    private static final String PROPERTY_ROOT_LABEL_VALUES_KEY = PROPERTY_ROOT_KEY + ".label.values";
    private static final String PROPERTY_ROOT_LABEL_VALUES_ALIAS = PROPERTY_ROOT_ALIAS + "LabelValues";

    public static final String PROPERTY_LABEL_VALUES_SECTION_KEY = PROPERTY_ROOT_LABEL_VALUES_KEY + ".section";
    public static final String PROPERTY_LABEL_VALUES_SECTION_LABEL = "Label Values";
    public static final String PROPERTY_LABEL_VALUES_SECTION_TOOLTIP = "Numeric value options for the color bar legend labels";
    public static final String PROPERTY_LABEL_VALUES_SECTION_ALIAS = PROPERTY_ROOT_LABEL_VALUES_ALIAS +"Section";

    public static final String PROPERTY_LABEL_VALUES_MODE_KEY = PROPERTY_ROOT_LABEL_VALUES_KEY + ".mode";
    public static final String PROPERTY_LABEL_VALUES_MODE_LABEL = "Mode";
    public static final String PROPERTY_LABEL_VALUES_MODE_TOOLTIP = "Mode for setting label values on the color bar legend";
    public static final Class PROPERTY_LABEL_VALUES_MODE_TYPE = String.class;
    public static final String PROPERTY_LABEL_VALUES_MODE_ALIAS = PROPERTY_ROOT_LABEL_VALUES_ALIAS + "Mode";
    public static final String PROPERTY_LABEL_VALUES_MODE_OPTION1 = DISTRIB_EVEN_STR;
    public static final String PROPERTY_LABEL_VALUES_MODE_OPTION2 = DISTRIB_MANUAL_STR;
    public static final String PROPERTY_LABEL_VALUES_MODE_OPTION3 = DISTRIB_EXACT_STR;
    public static final String PROPERTY_LABEL_VALUES_MODE_DEFAULT = DISTRIB_MANUAL_STR;
    public static final Object PROPERTY_LABEL_VALUES_MODE_VALUE_SET[] = {
            PROPERTY_LABEL_VALUES_MODE_OPTION1,
            PROPERTY_LABEL_VALUES_MODE_OPTION2,
            PROPERTY_LABEL_VALUES_MODE_OPTION3 };


    public static final String PROPERTY_LABEL_VALUES_COUNT_KEY = PROPERTY_ROOT_LABEL_VALUES_KEY + ".count";
    public static final String PROPERTY_LABEL_VALUES_COUNT_LABEL = "Tickmark Count (Auto)";
    public static final String PROPERTY_LABEL_VALUES_COUNT_TOOLTIP = "Number of tickmarks";
    public static final String PROPERTY_LABEL_VALUES_COUNT_ALIAS = PROPERTY_ROOT_LABEL_VALUES_ALIAS + "Count";
    public static final int PROPERTY_LABEL_VALUES_COUNT_DEFAULT = 5;
    public static final boolean PROPERTY_LABEL_VALUES_COUNT_ENABLED = true;
    public static final Class PROPERTY_LABEL_VALUES_COUNT_TYPE = Integer.class;
    public static final int PROPERTY_LABEL_VALUES_COUNT_MIN = 2;
    public static final int PROPERTY_LABEL_VALUES_COUNT_MAX = 20;
    public static final String PROPERTY_LABEL_VALUES_COUNT_INTERVAL = "[" + ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_MIN + "," + ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_MAX + "]";

    public static final String PROPERTY_LABEL_VALUES_ACTUAL_KEY = PROPERTY_ROOT_LABEL_VALUES_KEY + ".actual";
    public static final String PROPERTY_LABEL_VALUES_ACTUAL_LABEL = "Custom Values";
    public static final String PROPERTY_LABEL_VALUES_ACTUAL_TOOLTIP = "Set actual values of the tickmarks";
    private static final String PROPERTY_LABEL_VALUES_ACTUAL_ALIAS = PROPERTY_ROOT_LABEL_VALUES_ALIAS + "Actual";
    public static final String PROPERTY_LABEL_VALUES_ACTUAL_DEFAULT = "";
    public static final Class PROPERTY_LABEL_VALUES_ACTUAL_TYPE = String.class;



    // Formatting

    private static final String PROPERTY_ROOT_FORMATTING_KEY = PROPERTY_ROOT_KEY + ".formatting";
    private static final String PROPERTY_ROOT_FORMATTING_ALIAS = PROPERTY_ROOT_ALIAS + "Formatting";

    public static final String PROPERTY_FORMATTING_SECTION_KEY = PROPERTY_ROOT_FORMATTING_KEY + ".section";
    public static final String PROPERTY_FORMATTING_SECTION_LABEL = "Formatting";
    public static final String PROPERTY_FORMATTING_SECTION_TOOLTIP = "Formatting options for the color bar legend";
    public static final String PROPERTY_FORMATTING_SECTION_ALIAS = PROPERTY_ROOT_FORMATTING_ALIAS + "Section";

    public static final String PROPERTY_FORMATTING_ORIENTATION_KEY = PROPERTY_ROOT_FORMATTING_KEY + ".orientation";
    public static final String PROPERTY_FORMATTING_ORIENTATION_LABEL = "Orientation";
    public static final String PROPERTY_FORMATTING_ORIENTATION_TOOLTIP = "Orientation of the color bar legend";
    public static final Class PROPERTY_FORMATTING_ORIENTATION_TYPE = String.class;
    public static final String PROPERTY_FORMATTING_ORIENTATION_ALIAS = PROPERTY_ROOT_FORMATTING_ALIAS + "Orientation";
    public static final String PROPERTY_FORMATTING_ORIENTATION_OPTION1 = OPTION_HORIZONTAL;
    public static final String PROPERTY_FORMATTING_ORIENTATION_OPTION2 = OPTION_VERTICAL;
    public static final String PROPERTY_FORMATTING_ORIENTATION_DEFAULT = OPTION_HORIZONTAL;
    public static final Object PROPERTY_FORMATTING_ORIENTATION_VALUE_SET[] = {PROPERTY_FORMATTING_ORIENTATION_OPTION1, PROPERTY_FORMATTING_ORIENTATION_OPTION2};












    // Property Settings: ColorBar Location Section

    public static final String PROPERTY_COLORBAR_LOCATION_SECTION_NAME = "colorbar.location.section";
    public static final String PROPERTY_COLORBAR_LOCATION_SECTION_LABEL = "Location";
    public static final String PROPERTY_COLORBAR_LOCATION_SECTION_TOOLTIP = "Set location and relative size of color bar image";
    public static final String PROPERTY_COLORBAR_LOCATION_SECTION_ALIAS = "colorbarLocationSection";

    public static final String PROPERTY_COLORBAR_LOCATION_INSIDE_NAME = "colorbar.location.inside";
    public static final String PROPERTY_COLORBAR_LOCATION_INSIDE_LABEL = "Inside";
    public static final String PROPERTY_COLORBAR_LOCATION_INSIDE_TOOLTIP = "Place color bar inside/outside image bounds";
    private static final String PROPERTY_COLORBAR_LOCATION_INSIDE_ALIAS = "colorbarLocationInside";
    public static final boolean PROPERTY_COLORBAR_LOCATION_INSIDE_DEFAULT = true;
    public static final Class PROPERTY_COLORBAR_LOCATION_INSIDE_TYPE = Boolean.class;


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

    public static final String PROPERTY_COLORBAR_LOCATION_PLACEMENT_NAME = "colorbar.location.placement";
    public static final String PROPERTY_COLORBAR_LOCATION_PLACEMENT_LABEL = "Placement";
    public static final String PROPERTY_COLORBAR_LOCATION_PLACEMENT_TOOLTIP = "Where to place color bar on image";
    private static final String PROPERTY_COLORBAR_LOCATION_PLACEMENT_ALIAS = "colorbarLocationPlacement";
    public static final String PROPERTY_COLORBAR_LOCATION_PLACEMENT_DEFAULT = LOCATION_LOWER_RIGHT;
    public static final Class PROPERTY_COLORBAR_LOCATION_PLACEMENT_TYPE = String.class;

    public static final String PROPERTY_COLORBAR_LOCATION_OFFSET_NAME = "colorbar.location.offset";
    public static final String PROPERTY_COLORBAR_LOCATION_OFFSET_LABEL = "Offset";
    public static final String PROPERTY_COLORBAR_LOCATION_OFFSET_TOOLTIP = "Move color bar away from axis (by percentage of color bar height)";
    private static final String PROPERTY_COLORBAR_LOCATION_OFFSET_ALIAS = "colorbarLocationOffset";
    public static final Double PROPERTY_COLORBAR_LOCATION_OFFSET_DEFAULT = 0.0;
    public static final Class PROPERTY_COLORBAR_LOCATION_OFFSET_TYPE = Double.class;

    public static final String PROPERTY_COLORBAR_LOCATION_SHIFT_NAME = "colorbar.location.shift";
    public static final String PROPERTY_COLORBAR_LOCATION_SHIFT_LABEL = "Shift";
    public static final String PROPERTY_COLORBAR_LOCATION_SHIFT_TOOLTIP = "Move color bar along the axis (by percentage of color bar width)";
    private static final String PROPERTY_COLORBAR_LOCATION_SHIFT_ALIAS = "colorbarLocationShift";
    public static final Double PROPERTY_COLORBAR_LOCATION_SHIFT_DEFAULT = 0.0;
    public static final Class PROPERTY_COLORBAR_LOCATION_SHIFT_TYPE = Double.class;



    // Property Settings: ColorBar Scaling Section

    public static final String PROPERTY_COLORBAR_SCALING_SECTION_NAME = "colorbar.scaling.section";
    public static final String PROPERTY_COLORBAR_SCALING_SECTION_LABEL = "Scaling";
    public static final String PROPERTY_COLORBAR_SCALING_SECTION_TOOLTIP = "Set scaling and relative size of color bar image";
    public static final String PROPERTY_COLORBAR_SCALING_SECTION_ALIAS = "colorbarLocationSection";

    public static final String PROPERTY_COLORBAR_SCALING_APPLY_SIZE_SCALING_NAME = "colorbar.scaling.apply.size.scaling";
    public static final String PROPERTY_COLORBAR_SCALING_APPLY_SIZE_SCALING_LABEL = "Scale size to image size";
    public static final String PROPERTY_COLORBAR_SCALING_APPLY_SIZE_SCALING_TOOLTIP = "Scale the color bar size relative to the scene image size";
    private static final String PROPERTY_COLORBAR_SCALING_APPLY_SIZE_SCALING_ALIAS = "colorbarScalingApplySizeScaling";
    public static final boolean PROPERTY_COLORBAR_SCALING_APPLY_SIZE_SCALING_DEFAULT = true;
    public static final Class PROPERTY_COLORBAR_SCALING_APPLY_SIZE_SCALING_TYPE = Boolean.class;

    public static final String PROPERTY_COLORBAR_SCALING_SIZE_SCALING_NAME = "colorbar.scaling.size.scaling";
    public static final String PROPERTY_COLORBAR_SCALING_SIZE_SCALING_LABEL = "Scaling Percent";
    public static final String PROPERTY_COLORBAR_SCALING_SIZE_SCALING_TOOLTIP = "Percent to scale color bar relative to the scene image size";
    private static final String PROPERTY_COLORBAR_SCALING_SIZE_SCALING_ALIAS = "colorbarScalingSizeScaling";
    public static final double PROPERTY_COLORBAR_SCALING_SIZE_SCALING_DEFAULT = 50.0;
    public static final Class PROPERTY_COLORBAR_SCALING_SIZE_SCALING_TYPE = Double.class;

    

    // Property Settings: ColorBar Title Section

    private static final String PROPERTY_ROOT_TITLE_KEY = PROPERTY_ROOT_KEY + ".title";
    private static final String PROPERTY_ROOT_TITLE_ALIAS = PROPERTY_ROOT_ALIAS + "Title";

    public static final String PROPERTY_COLORBAR_TITLE_SECTION_KEY = PROPERTY_ROOT_TITLE_KEY + ".section";
    public static final String PROPERTY_COLORBAR_TITLE_SECTION_LABEL = "Title";
    public static final String PROPERTY_COLORBAR_TITLE_SECTION_TOOLTIP = "Set title of color bar";
    public static final String PROPERTY_COLORBAR_TITLE_SECTION_ALIAS = PROPERTY_ROOT_TITLE_ALIAS + "Section";

    public static final String PROPERTY_COLORBAR_TITLE_SHOW_TITLE_KEY = PROPERTY_ROOT_TITLE_KEY + ".show";
    public static final String PROPERTY_COLORBAR_TITLE_SHOW_TITLE_LABEL = "Show Title";
    public static final String PROPERTY_COLORBAR_TITLE_SHOW_TITLE_TOOLTIP = "Add title to the color bar";
    private static final String PROPERTY_COLORBAR_TITLE_SHOW_TITLE_ALIAS = PROPERTY_ROOT_TITLE_ALIAS + "Show";
    public static final boolean PROPERTY_COLORBAR_TITLE_SHOW_TITLE_DEFAULT = true;
    public static final Class PROPERTY_COLORBAR_TITLE_SHOW_TITLE_TYPE = Boolean.class;

    public static final String PROPERTY_COLORBAR_TITLE_TITLE_KEY = PROPERTY_ROOT_TITLE_KEY + ".title";
    public static final String PROPERTY_COLORBAR_TITLE_TITLE_LABEL = "Title";
    public static final String PROPERTY_COLORBAR_TITLE_TITLE_TOOLTIP = "Add title to the color bar";
    public static final String PROPERTY_COLORBAR_TITLE_TITLE_ALIAS = PROPERTY_ROOT_TITLE_ALIAS + "Title";
    public static final String PROPERTY_COLORBAR_TITLE_TITLE_DEFAULT = "";
    public static final Class PROPERTY_COLORBAR_TITLE_TITLE_TYPE = String.class;

    public static final String PROPERTY_COLORBAR_TITLE_UNITS_KEY = PROPERTY_ROOT_TITLE_KEY + ".units";
    public static final String PROPERTY_COLORBAR_TITLE_UNITS_LABEL = "Units";
    public static final String PROPERTY_COLORBAR_TITLE_UNITS_TOOLTIP = "Add units to the title of the color bar";
    public static final String PROPERTY_COLORBAR_TITLE_UNITS_ALIAS = PROPERTY_ROOT_TITLE_ALIAS + "Units";
    public static final String PROPERTY_COLORBAR_TITLE_UNITS_DEFAULT = "";
    public static final Class PROPERTY_COLORBAR_TITLE_UNITS_TYPE = String.class;

    public static final String PROPERTY_TITLE_COLOR_KEY = PROPERTY_ROOT_TITLE_KEY + ".color";
    public static final String PROPERTY_TITLE_COLOR_LABEL = "Title Color";
    public static final String PROPERTY_TITLE_COLOR_TOOLTIP = "Set color of the title";
    private static final String PROPERTY_TITLE_COLOR_ALIAS = PROPERTY_ROOT_TITLE_ALIAS + "Color";
    public static final Color PROPERTY_TITLE_COLOR_DEFAULT = Color.YELLOW;
    public static final Class PROPERTY_TITLE_COLOR_TYPE = Color.class;




    // Property Settings: ColorBar Tickmarks Section

    private static final String PROPERTY_ROOT_TICKMARKS_KEY = PROPERTY_ROOT_KEY + ".tickmarks";
    private static final String PROPERTY_ROOT_TICKMARKS_ALIAS = PROPERTY_ROOT_ALIAS + "TickMarks";

    public static final String PROPERTY_TICKMARKS_SECTION_KEY = PROPERTY_ROOT_TICKMARKS_KEY + ".section";
    public static final String PROPERTY_TICKMARKS_SECTION_LABEL = "Tickmarks";
    public static final String PROPERTY_TICKMARKS_SECTION_TOOLTIP = "Format options for the color bar legend tickmarks";
    public static final String PROPERTY_TICKMARKS_SECTION_ALIAS = PROPERTY_ROOT_TICKMARKS_ALIAS + "Section";

    public static final String PROPERTY_TICKMARKS_SHOW_KEY = PROPERTY_ROOT_TICKMARKS_KEY + ".show";
    public static final String PROPERTY_TICKMARKS_SHOW_LABEL = "Show";
    public static final String PROPERTY_TICKMARKS_SHOW_TOOLTIP = "Display tickmarks";
    public static final String PROPERTY_TICKMARKS_SHOW_ALIAS = PROPERTY_ROOT_TICKMARKS_ALIAS + "Show";
    public static final boolean PROPERTY_TICKMARKS_SHOW_DEFAULT = true;
    public static final Class PROPERTY_TICKMARKS_SHOW_TYPE = Boolean.class;

    public static final String PROPERTY_TICKMARKS_COLOR_KEY = PROPERTY_ROOT_TICKMARKS_KEY + ".color";
    public static final String PROPERTY_TICKMARKS_COLOR_LABEL = "Tickmark Color";
    public static final String PROPERTY_TICKMARKS_COLOR_TOOLTIP = "Set color of the tickmarks";
    private static final String PROPERTY_TICKMARKS_COLOR_ALIAS = PROPERTY_ROOT_TICKMARKS_ALIAS + "Color";
    public static final Color PROPERTY_TICKMARKS_COLOR_DEFAULT = Color.YELLOW;
    public static final Class PROPERTY_TICKMARKS_COLOR_TYPE = Color.class;

    public static final String PROPERTY_TICKMARKS_LENGTH_KEY = PROPERTY_ROOT_TICKMARKS_KEY + ".length";
    public static final String PROPERTY_TICKMARKS_LENGTH_LABEL = "Length";
    public static final String PROPERTY_TICKMARKS_LENGTH_TOOLTIP = "Set length of tickmarks";
    public static final String PROPERTY_TICKMARKS_LENGTH_ALIAS = PROPERTY_ROOT_TICKMARKS_ALIAS + "Length";
    public static final int PROPERTY_TICKMARKS_LENGTH_DEFAULT = 12;
    public static final Class PROPERTY_TICKMARKS_LENGTH_TYPE = Integer.class;

    public static final String PROPERTY_TICKMARKS_WIDTH_KEY = PROPERTY_ROOT_TICKMARKS_KEY + ".width";
    public static final String PROPERTY_TICKMARKS_WIDTH_LABEL = "Width";
    public static final String PROPERTY_TICKMARKS_WIDTH_TOOLTIP = "Set width of tickmarks";
    public static final String PROPERTY_TICKMARKS_WIDTH_ALIAS = PROPERTY_ROOT_TICKMARKS_ALIAS + "Width";
    public static final int PROPERTY_TICKMARKS_WIDTH_DEFAULT = 4;
    public static final Class PROPERTY_TICKMARKS_WIDTH_TYPE = Integer.class;



    // Property Settings: Border Section

    public static final String PROPERTY_BORDER_SECTION_KEY = "colorbar.border.section";
    public static final String PROPERTY_BORDER_SECTION_ALIAS = "colorbarBorderSection";
    public static final String PROPERTY_BORDER_SECTION_LABEL = "Border";
    public static final String PROPERTY_BORDER_SECTION_TOOLTIP = "Configuration options for adding a border around the data image";

    public static final String PROPERTY_BORDER_SHOW_KEY = "colorbar.border.show";
    public static final String PROPERTY_BORDER_SHOW_LABEL = "Show";
    public static final String PROPERTY_BORDER_SHOW_TOOLTIP = "Display a border around the data image";
    private static final String PROPERTY_BORDER_SHOW_ALIAS = "colorbarBorderShow";
    public static final boolean PROPERTY_BORDER_SHOW_DEFAULT = true;
    public static final Class PROPERTY_BORDER_SHOW_TYPE = Boolean.class;

    public static final String PROPERTY_BORDER_WIDTH_KEY = "colorbar.border.width";
    public static final String PROPERTY_BORDER_WIDTH_LABEL = "Width";
    public static final String PROPERTY_BORDER_WIDTH_TOOLTIP = "Width of border line";
    private static final String PROPERTY_BORDER_WIDTH_ALIAS = "colorbarBorderWidth";
    public static final int PROPERTY_BORDER_WIDTH_DEFAULT = 1;
    public static final Class PROPERTY_BORDER_WIDTH_TYPE = Integer.class;

    public static final String PROPERTY_BORDER_COLOR_KEY = "colorbar.border.color";
    public static final String PROPERTY_BORDER_COLOR_LABEL = "Color";
    public static final String PROPERTY_BORDER_COLOR_TOOLTIP = "Color of border line";
    private static final String PROPERTY_BORDER_COLOR_ALIAS = "colorbarBorderColor";
    public static final Color PROPERTY_BORDER_COLOR_DEFAULT = Color.BLACK;
    public static final Class PROPERTY_BORDER_COLOR_TYPE = Color.class;




    // Property Settings: Backdrop Section

    private static final String PROPERTY_ROOT_BACKDROP_KEY = PROPERTY_ROOT_KEY + ".backdrop";
    private static final String PROPERTY_ROOT_BACKDROP_ALIAS = PROPERTY_ROOT_ALIAS + "Backdrop";

    public static final String PROPERTY_BACKDROP_SECTION_KEY = PROPERTY_ROOT_BACKDROP_KEY + ".section";
    public static final String PROPERTY_BACKDROP_SECTION_ALIAS = PROPERTY_ROOT_BACKDROP_ALIAS + "Section";
    public static final String PROPERTY_BACKDROP_SECTION_LABEL = "Backdrop";
    public static final String PROPERTY_BACKDROP_SECTION_TOOLTIP = "Configuration options for the color bar legend backdrop";

    public static final String PROPERTY_BACKDROP_SHOW_KEY = PROPERTY_ROOT_BACKDROP_KEY + ".show";
    public static final String PROPERTY_BACKDROP_SHOW_LABEL = "Show";
    public static final String PROPERTY_BACKDROP_SHOW_TOOLTIP = "Show the color bar legend backdrop";
    private static final String PROPERTY_BACKDROP_SHOW_ALIAS = PROPERTY_ROOT_BACKDROP_ALIAS + "Show";
    public static final boolean PROPERTY_BACKDROP_SHOW_DEFAULT = true;
    public static final Class PROPERTY_BACKDROP_SHOW_TYPE = Boolean.class;

    public static final String PROPERTY_BACKDROP_COLOR_KEY = PROPERTY_ROOT_BACKDROP_KEY + ".color";
    public static final String PROPERTY_BACKDROP_COLOR_LABEL = "Color";
    public static final String PROPERTY_BACKDROP_COLOR_TOOLTIP = "Set color of the backdrop of the color bar legend backdrop";
    private static final String PROPERTY_BACKDROP_COLOR_ALIAS = PROPERTY_ROOT_BACKDROP_ALIAS + "Color";
    public static final Color PROPERTY_BACKDROP_COLOR_DEFAULT = Color.BLACK;
    public static final Class PROPERTY_BACKDROP_COLOR_TYPE = Color.class;

    public static final String PROPERTY_BACKDROP_TRANSPARENCY_KEY = PROPERTY_ROOT_BACKDROP_KEY + "transparency";
    public static final String PROPERTY_BACKDROP_TRANSPARENCY_LABEL = "Transparency";
    public static final String PROPERTY_BACKDROP_TRANSPARENCY_TOOLTIP = "Set transparency of the color bar legend backdrop";
    private static final String PROPERTY_BACKDROP_TRANSPARENCY_ALIAS = PROPERTY_ROOT_BACKDROP_ALIAS + "Transparency";
    public static final double PROPERTY_BACKDROP_TRANSPARENCY_DEFAULT = 0.5;
    public static final Class PROPERTY_BACKDROP_TRANSPARENCY_TYPE = Double.class;




    // Property Settings: Labels Section

    private static final String PROPERTY_ROOT_LABELS_KEY = PROPERTY_ROOT_KEY + ".labels";
    private static final String PROPERTY_ROOT_LABELS_ALIAS = PROPERTY_ROOT_ALIAS + "Labels";


    public static final String PROPERTY_LABELS_SECTION_NAME = PROPERTY_ROOT_LABELS_KEY + ".section";
    public static final String PROPERTY_LABELS_SECTION_LABEL = "Labels";
    public static final String PROPERTY_LABELS_SECTION_TOOLTIP = "Configuration options for the labels";
    public static final String PROPERTY_LABELS_SECTION_ALIAS = PROPERTY_ROOT_LABELS_ALIAS + "Section";

    public static final String PROPERTY_LABELS_SHOW_KEY = PROPERTY_ROOT_LABELS_KEY + ".show";
    public static final String PROPERTY_LABELS_SHOW_LABEL = "Show";
    public static final String PROPERTY_LABELS_SHOW_TOOLTIP = "Show labels";
    private static final String PROPERTY_LABELS_SHOW_ALIAS = PROPERTY_ROOT_LABELS_ALIAS + "Show";
    public static final boolean PROPERTY_LABELS_SHOW_DEFAULT = true;
    public static final Class PROPERTY_LABELS_SHOW_TYPE = Boolean.class;

    public static final String PROPERTY_LABELS_SIZE_NAME = PROPERTY_ROOT_LABELS_KEY + ".size";
    public static final String PROPERTY_LABELS_SIZE_LABEL = "Size";
    public static final String PROPERTY_LABELS_SIZE_TOOLTIP = "Set size of the label text";
    private static final String PROPERTY_LABELS_SIZE_ALIAS = PROPERTY_ROOT_LABELS_ALIAS + "Size";
    public static final int PROPERTY_LABELS_SIZE_DEFAULT = 35;
    public static final Class PROPERTY_LABELS_SIZE_TYPE = Integer.class;
    public static final int PROPERTY_LABELS_SIZE_VALUE_MIN = 10;
    public static final int PROPERTY_LABELS_SIZE_VALUE_MAX = 200;
    public static final String PROPERTY_LABELS_SIZE_INTERVAL = "[" + ColorBarLayerType.PROPERTY_LABELS_SIZE_VALUE_MIN + "," + ColorBarLayerType.PROPERTY_LABELS_SIZE_VALUE_MAX + "]";

    public static final String PROPERTY_LABELS_ITALIC_NAME = PROPERTY_ROOT_LABELS_KEY + ".font.italic";
    public static final String PROPERTY_LABELS_ITALIC_LABEL = "Italic";
    public static final String PROPERTY_LABELS_ITALIC_TOOLTIP = "Format label text font in italic";
    public static final String PROPERTY_LABELS_ITALIC_ALIAS = PROPERTY_ROOT_LABELS_ALIAS + "FontItalic";
    public static final boolean PROPERTY_LABELS_ITALIC_DEFAULT = false;
    public static final Class PROPERTY_LABELS_ITALIC_TYPE = Boolean.class;

    public static final String PROPERTY_LABELS_BOLD_NAME = PROPERTY_ROOT_LABELS_KEY + ".font.bold";
    public static final String PROPERTY_LABELS_BOLD_LABEL = "Bold";
    public static final String PROPERTY_LABELS_BOLD_TOOLTIP = "Format label text font in bold";
    public static final String PROPERTY_LABELS_BOLD_ALIAS = PROPERTY_ROOT_LABELS_ALIAS + "FontBold";
    public static final boolean PROPERTY_LABELS_BOLD_DEFAULT = false;
    public static final Class PROPERTY_LABELS_BOLD_TYPE = Boolean.class;

    public static final String PROPERTY_LABELS_FONT_NAME = PROPERTY_ROOT_LABELS_KEY + ".font.name";
    public static final String PROPERTY_LABELS_FONT_LABEL = "Font";
    public static final String PROPERTY_LABELS_FONT_TOOLTIP = "Set the text font of the labels";
    public static final String PROPERTY_LABELS_FONT_ALIAS = PROPERTY_ROOT_LABELS_ALIAS + "FontName";
    public static final String PROPERTY_LABELS_FONT_DEFAULT = "SanSerif";
    public static final Class PROPERTY_LABELS_FONT_TYPE = String.class;
    public static final String PROPERTY_LABELS_FONT_VALUE_1 = "SanSerif";
    public static final String PROPERTY_LABELS_FONT_VALUE_2 = "Serif";
    public static final String PROPERTY_LABELS_FONT_VALUE_3 = "Courier";
    public static final String PROPERTY_LABELS_FONT_VALUE_4 = "Monospaced";
    public static final Object PROPERTY_LABELS_FONT_VALUE_SET[] = {PROPERTY_LABELS_FONT_VALUE_1, PROPERTY_LABELS_FONT_VALUE_2, PROPERTY_LABELS_FONT_VALUE_3, PROPERTY_LABELS_FONT_VALUE_4};

    public static final String PROPERTY_LABELS_COLOR_NAME = PROPERTY_ROOT_LABELS_KEY + ".color";
    public static final String PROPERTY_LABELS_COLOR_LABEL = "Color";
    public static final String PROPERTY_LABELS_COLOR_TOOLTIP = "Set color of the label text";
    private static final String PROPERTY_LABELS_COLOR_ALIAS = PROPERTY_ROOT_LABELS_ALIAS + "Color";
    public static final Color PROPERTY_LABELS_COLOR_DEFAULT = Color.YELLOW;
    public static final Class PROPERTY_LABELS_COLOR_TYPE = Color.class;











    // Property Settings: Grid Spacing Section


    public static final String PROPERTY_GRID_SPACING_LAT_NAME = "colorbar.spacing.lat";

    public static final String PROPERTY_GRID_SPACING_LON_NAME = "colorbar.spacing.lon";






    // ---------------------------------------------------------

    public static final String PROPERTY_NAME_RASTER = "raster";


    public static final String PROPERTY_NUM_GRID_LINES_NAME = "colorbar.num.grid.lines"; // todo Danny changed this to number of lines so need to change variable names
    public static final int PROPERTY_NUM_GRID_LINES_DEFAULT = 4;
    private static final String PROPERTY_NUM_GRID_LINES_ALIAS = "numGridLines";
    public static final Class PROPERTY_NUM_GRID_LINES_TYPE = Integer.class;


    // Property Setting: Restore Defaults
    public static final String PROPERTY_RESTORE_DEFAULTS_NAME = "colorbar.restoreDefaults";
    public static final String PROPERTY_RESTORE_TO_DEFAULTS_LABEL = "RESTORE DEFAULTS (Map Gridline Preferences)";
    public static final String PROPERTY_RESTORE_TO_DEFAULTS_TOOLTIP = "Restore all map gridline preferences to the default";
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




        // Formatting Section

        final Property formattingSectionModel = Property.create(PROPERTY_FORMATTING_SECTION_KEY, Boolean.class, true, true);
        formattingSectionModel.getDescriptor().setAlias(PROPERTY_FORMATTING_SECTION_ALIAS);
        vc.addProperty(formattingSectionModel);

        final Property formattingOrientationModel = Property.create(PROPERTY_FORMATTING_ORIENTATION_KEY, PROPERTY_FORMATTING_ORIENTATION_TYPE, true, true);
        formattingOrientationModel.getDescriptor().setAlias(PROPERTY_FORMATTING_ORIENTATION_ALIAS);
        vc.addProperty(formattingOrientationModel);










        // ColorBar Location Section

        final Property locationSectionModel = Property.create(PROPERTY_COLORBAR_LOCATION_SECTION_NAME, Boolean.class, true, true);
        locationSectionModel.getDescriptor().setAlias(PROPERTY_COLORBAR_LOCATION_SECTION_ALIAS);
        vc.addProperty(locationSectionModel);

        final Property locationInsideModel = Property.create(PROPERTY_COLORBAR_LOCATION_INSIDE_NAME, PROPERTY_COLORBAR_LOCATION_INSIDE_TYPE, true, true);
        locationInsideModel.getDescriptor().setAlias(PROPERTY_COLORBAR_LOCATION_INSIDE_ALIAS);
        vc.addProperty(locationInsideModel);

        final Property locationEdgeModel = Property.create(PROPERTY_COLORBAR_LOCATION_PLACEMENT_NAME, PROPERTY_COLORBAR_LOCATION_PLACEMENT_TYPE, true, true);
        locationEdgeModel.getDescriptor().setAlias(PROPERTY_COLORBAR_LOCATION_PLACEMENT_ALIAS);
        vc.addProperty(locationEdgeModel);


        final Property locationOffsetModel = Property.create(PROPERTY_COLORBAR_LOCATION_OFFSET_NAME, PROPERTY_COLORBAR_LOCATION_OFFSET_TYPE, true, true);
        locationOffsetModel.getDescriptor().setAlias(PROPERTY_COLORBAR_LOCATION_OFFSET_ALIAS);
        vc.addProperty(locationOffsetModel);

        final Property locationShiftModel = Property.create(PROPERTY_COLORBAR_LOCATION_SHIFT_NAME, PROPERTY_COLORBAR_LOCATION_SHIFT_TYPE, true, true);
        locationShiftModel.getDescriptor().setAlias(PROPERTY_COLORBAR_LOCATION_SHIFT_ALIAS);
        vc.addProperty(locationShiftModel);







        final Property scalingSectionModel = Property.create(PROPERTY_COLORBAR_SCALING_SECTION_NAME, Boolean.class, true, true);
        scalingSectionModel.getDescriptor().setAlias(PROPERTY_COLORBAR_SCALING_SECTION_ALIAS);
        vc.addProperty(scalingSectionModel);

        final Property locationApplySizeScalingModel = Property.create(PROPERTY_COLORBAR_SCALING_APPLY_SIZE_SCALING_NAME, PROPERTY_COLORBAR_SCALING_APPLY_SIZE_SCALING_TYPE, true, true);
        locationApplySizeScalingModel.getDescriptor().setAlias(PROPERTY_COLORBAR_SCALING_APPLY_SIZE_SCALING_ALIAS);
        vc.addProperty(locationApplySizeScalingModel);

        final Property locationSizeScalingModel = Property.create(PROPERTY_COLORBAR_SCALING_SIZE_SCALING_NAME, ColorBarLayerType.PROPERTY_COLORBAR_SCALING_SIZE_SCALING_TYPE, true, true);
        locationSizeScalingModel.getDescriptor().setAlias(PROPERTY_COLORBAR_SCALING_SIZE_SCALING_ALIAS);
        vc.addProperty(locationSizeScalingModel);


        final Property titleSectionModel = Property.create(PROPERTY_COLORBAR_TITLE_SECTION_KEY, Boolean.class, true, true);
        titleSectionModel.getDescriptor().setAlias(PROPERTY_COLORBAR_TITLE_SECTION_ALIAS);
        vc.addProperty(titleSectionModel);

        final Property titleShowModel = Property.create(PROPERTY_COLORBAR_TITLE_SHOW_TITLE_KEY,
                PROPERTY_COLORBAR_TITLE_SHOW_TITLE_TYPE, true, true);
        titleShowModel.getDescriptor().setAlias(PROPERTY_COLORBAR_TITLE_SHOW_TITLE_ALIAS);
        vc.addProperty(titleShowModel);

        final Property titleValueModel = Property.create(PROPERTY_COLORBAR_TITLE_TITLE_KEY,
                PROPERTY_COLORBAR_TITLE_TITLE_TYPE, true, true);
        titleValueModel.getDescriptor().setAlias(PROPERTY_COLORBAR_TITLE_TITLE_ALIAS);
        vc.addProperty(titleValueModel);

        final Property titleUnitsModel = Property.create(PROPERTY_COLORBAR_TITLE_UNITS_KEY,
                PROPERTY_COLORBAR_TITLE_UNITS_TYPE, true, true);
        titleUnitsModel.getDescriptor().setAlias(PROPERTY_COLORBAR_TITLE_UNITS_ALIAS);
        vc.addProperty(titleUnitsModel);



        final Property titleColorModel = Property.create(PROPERTY_TITLE_COLOR_KEY, PROPERTY_TITLE_COLOR_TYPE, true, true);
        titleColorModel.getDescriptor().setAlias(PROPERTY_TITLE_COLOR_ALIAS);
        vc.addProperty(titleColorModel);




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

        final Property labelsSectionModel = Property.create(PROPERTY_LABELS_SECTION_NAME, Boolean.class, true, true);
        labelsSectionModel.getDescriptor().setAlias(PROPERTY_LABELS_SECTION_ALIAS);
        vc.addProperty(labelsSectionModel);












        final Property textFgColorModel = Property.create(PROPERTY_LABELS_COLOR_NAME, Color.class, PROPERTY_LABELS_COLOR_DEFAULT, true);
        textFgColorModel.getDescriptor().setAlias(PROPERTY_LABELS_COLOR_ALIAS);
        vc.addProperty(textFgColorModel);






        final Property textFontSizeModel = Property.create(PROPERTY_LABELS_SIZE_NAME, Integer.class, PROPERTY_LABELS_SIZE_DEFAULT, true);
        textFontSizeModel.getDescriptor().setAlias(PROPERTY_LABELS_SIZE_ALIAS);
        vc.addProperty(textFontSizeModel);


        final Property textFontItalicModel = Property.create(PROPERTY_LABELS_ITALIC_NAME, Boolean.class, PROPERTY_LABELS_ITALIC_DEFAULT, true);
        textFontItalicModel.getDescriptor().setAlias(PROPERTY_LABELS_ITALIC_ALIAS);
        vc.addProperty(textFontItalicModel);

        final Property textFontBoldModel = Property.create(PROPERTY_LABELS_BOLD_NAME, Boolean.class, PROPERTY_LABELS_BOLD_DEFAULT, true);
        textFontBoldModel.getDescriptor().setAlias(PROPERTY_LABELS_BOLD_ALIAS);
        vc.addProperty(textFontBoldModel);

        final Property textFontModel = Property.create(PROPERTY_LABELS_FONT_NAME, String.class, PROPERTY_LABELS_FONT_DEFAULT, true);
        textFontModel.getDescriptor().setAlias(PROPERTY_LABELS_FONT_ALIAS);
        vc.addProperty(textFontModel);




        return vc;
    }
}
