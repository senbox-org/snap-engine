
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


@LayerTypeMetadata(name = "MetaDataLayerType", aliasNames = {"org.esa.snap.core.layer.MetaDataLayerType"})
public class MetaDataLayerType extends LayerType {


    public static final String LOCATION_TOP_LEFT = "Top Left";
    public static final String LOCATION_TOP_CENTER = "Top Center (Centered)";
    public static final String LOCATION_TOP_CENTER_JUSTIFY_LEFT = "Top Center";
    public static final String LOCATION_TOP_RIGHT = "Top Right";
    public static final String LOCATION_BOTTOM_LEFT = "Bottom Left";
    public static final String LOCATION_BOTTOM_CENTER = "Bottom Center (Centered)";
    public static final String LOCATION_BOTTOM_CENTER_JUSTIFY_LEFT = "Bottom Center";
    public static final String LOCATION_BOTTOM_RIGHT = "Bottom Right";
    public static final String LOCATION_RIGHT = "Margin: Upper Right";
    public static final String LOCATION_RIGHT_CENTER = "Margin: Center Right";
    public static final String LOCATION_RIGHT_BOTTOM = "Margin: Lower Right";
    public static final String LOCATION_LEFT = "Margin: Upper Left";
    public static final String LOCATION_LEFT_CENTER = "Margin: Center Left";
    public static final String LOCATION_LEFT_BOTTOM = "Margin: Lower Left";




    public static String[] getMarginLocationArray() {
        return  new String[]{
                LOCATION_RIGHT,
                LOCATION_RIGHT_CENTER,
                LOCATION_RIGHT_BOTTOM,
                LOCATION_LEFT,
                LOCATION_LEFT_CENTER,
                LOCATION_LEFT_BOTTOM,
                LOCATION_BOTTOM_LEFT,
                LOCATION_BOTTOM_CENTER_JUSTIFY_LEFT,
                LOCATION_BOTTOM_CENTER,
                LOCATION_BOTTOM_RIGHT
        };
    }

    public static String[] getHeaderLocationArray() {
        return  new String[]{
                LOCATION_TOP_LEFT,
                LOCATION_TOP_CENTER_JUSTIFY_LEFT,
                LOCATION_TOP_CENTER,
                LOCATION_TOP_RIGHT
        };
    }

    public static String[] getFooter2LocationArray() {
        return  new String[]{
                LOCATION_BOTTOM_LEFT,
                LOCATION_BOTTOM_CENTER_JUSTIFY_LEFT,
                LOCATION_BOTTOM_CENTER,
                LOCATION_BOTTOM_RIGHT
        };
    }


    public static final String PROPERTY_FONT_STYLE_1 = "SansSerif";
    public static final String PROPERTY_FONT_STYLE_2 = "Serif";
    public static final String PROPERTY_FONT_STYLE_3 = "Courier";
    public static final String PROPERTY_FONT_STYLE_4 = "Monospaced";


    private static final String PROPERTY_ROOT_KEY = "annotation.layer.v8.5";
    private static final String PROPERTY_ROOT_ALIAS = "AnnotationLayer";





    // Margin Contents Section

    private static final String PROPERTY_MARGIN_ROOT_KEY = PROPERTY_ROOT_KEY + ".margin.contents";
    private static final String PROPERTY_MARGIN_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "marginContents";

    public static final String PROPERTY_MARGIN_SECTION_KEY = PROPERTY_MARGIN_ROOT_KEY + ".section";
    public static final String PROPERTY_MARGIN_SECTION_LABEL = "Margin Annotation";
    public static final String PROPERTY_MARGIN_SECTION_TOOLTIP = "Contents of metadata and notes section";
    public static final String PROPERTY_MARGIN_SECTION_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "Section";

    public static final String PROPERTY_MARGIN_SHOW_KEY = PROPERTY_MARGIN_ROOT_KEY + ".show";
    public static final String PROPERTY_MARGIN_SHOW_LABEL = "Show Margin Annotation";
    public static final String PROPERTY_MARGIN_SHOW_TOOLTIP = "Show metadata annotation section";
    public static final String PROPERTY_MARGIN_SHOW_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "Show";
    public static final boolean PROPERTY_MARGIN_SHOW_DEFAULT = true;
    public static final Class PROPERTY_MARGIN_SHOW_TYPE = Boolean.class;

    public static final String PROPERTY_MARGIN_LOCATION_KEY = PROPERTY_MARGIN_ROOT_KEY + ".location";
    public static final String PROPERTY_MARGIN_LOCATION_LABEL = "Margin Location";
    public static final String PROPERTY_MARGIN_LOCATION_TOOLTIP = "Where to place the footer on the image";
    private static final String PROPERTY_MARGIN_LOCATION_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "Location";
    public static final String PROPERTY_MARGIN_LOCATION_DEFAULT = LOCATION_RIGHT;
    public static final Class PROPERTY_MARGIN_LOCATION_TYPE = String.class;

    public static final String PROPERTY_MARGIN_GAP_KEY = PROPERTY_MARGIN_ROOT_KEY + ".offset";
    public static final String PROPERTY_MARGIN_GAP_LABEL = "Margin Gap";
    public static final String PROPERTY_MARGIN_GAP_TOOLTIP = "Percentage of scene size to place metadata away from the edge of the scene image";
    private static final String PROPERTY_MARGIN_GAP_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "Offset";
    public static final Double PROPERTY_MARGIN_GAP_DEFAULT = 70.0;
    public static final double PROPERTY_MARGIN_GAP_MIN = -200;
    public static final double PROPERTY_MARGIN_GAP_MAX = 200;
    public static final String PROPERTY_MARGIN_GAP_INTERVAL = "[" + MetaDataLayerType.PROPERTY_MARGIN_GAP_MIN + "," + MetaDataLayerType.PROPERTY_MARGIN_GAP_MAX + "]";
    public static final Class PROPERTY_MARGIN_GAP_TYPE = Double.class;


    public static final String PROPERTY_MARGIN_TEXTFIELD_KEY = PROPERTY_MARGIN_ROOT_KEY + ".textfield";
    public static final String PROPERTY_MARGIN_TEXTFIELD_LABEL = "Margin Text";
    public static final String PROPERTY_MARGIN_TEXTFIELD_TOOLTIP = "Adds a line to the Metadata & Notes section";
    public static final String PROPERTY_MARGIN_TEXTFIELD_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "Textfield";
    public static final String PROPERTY_MARGIN_TEXTFIELD_DEFAULT = "";
    public static final Class PROPERTY_MARGIN_TEXTFIELD_TYPE = String.class;

    public static final String PROPERTY_MARGIN_TEXTFIELD2_KEY = PROPERTY_MARGIN_ROOT_KEY + ".textfield2";
    public static final String PROPERTY_MARGIN_TEXTFIELD2_LABEL = "Margin Text";
    public static final String PROPERTY_MARGIN_TEXTFIELD2_TOOLTIP = "Adds a line to the Metadata & Notes section";
    public static final String PROPERTY_MARGIN_TEXTFIELD2_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "Textfield2";
    public static final String PROPERTY_MARGIN_TEXTFIELD2_DEFAULT = "";
    public static final Class PROPERTY_MARGIN_TEXTFIELD2_TYPE = String.class;


    public static final String PROPERTY_MARGIN_PROPERTY_HEADING_KEY = PROPERTY_MARGIN_ROOT_KEY + ".property.heading";
    public static final String PROPERTY_MARGIN_PROPERTY_HEADING_LABEL = "PROPERTIES Section Heading";
    public static final String PROPERTY_MARGIN_PROPERTY_HEADING_TOOLTIP = "Heading of the PROPERTY section";
    public static final String PROPERTY_MARGIN_PROPERTY_HEADING_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "PropertyHeading";
    public static final String PROPERTY_MARGIN_PROPERTY_HEADING_DEFAULT = "File/Band (Properties):";
    public static final Class PROPERTY_MARGIN_PROPERTY_HEADING_TYPE = String.class;

    public static final String PROPERTY_MARGIN_GLOBAL_HEADING_KEY = PROPERTY_MARGIN_ROOT_KEY + ".global.attrib.heading";
    public static final String PROPERTY_MARGIN_GLOBAL_HEADING_LABEL = "GLOBAL_ATTRIBUTES Section Heading";
    public static final String PROPERTY_MARGIN_GLOBAL_HEADING_TOOLTIP = "Heading of the GLOBAL_ATTRIBUTE section";
    public static final String PROPERTY_MARGIN_GLOBAL_HEADING_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "GlobalAttribHeading";
    public static final String PROPERTY_MARGIN_GLOBAL_HEADING_DEFAULT = "File Metadata (Global_Attributes):";
    public static final Class PROPERTY_MARGIN_GLOBAL_HEADING_TYPE = String.class;

    public static final String PROPERTY_MARGIN_BAND_HEADING_KEY = PROPERTY_MARGIN_ROOT_KEY + ".band.attrib.heading";
    public static final String PROPERTY_MARGIN_BAND_HEADING_LABEL = "BAND_ATTRIBUTES Section Heading";
    public static final String PROPERTY_MARGIN_BAND_HEADING_TOOLTIP = "Heading of the BAND_ATTRIBUTE section";
    public static final String PROPERTY_MARGIN_BAND_HEADING_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "BandAttribHeading";
    public static final String PROPERTY_MARGIN_BAND_HEADING_DEFAULT = "Band Metadata (Band_Attributes): <PROPERTY=band>";
    public static final Class PROPERTY_MARGIN_BAND_HEADING_TYPE = String.class;


    public static final String PROPERTY_MARGIN_METADATA_KEY = PROPERTY_MARGIN_ROOT_KEY + ".metadata";
    public static final String PROPERTY_MARGIN_METADATA_LABEL = "PROPERTY List";
    public static final String PROPERTY_MARGIN_METADATA_TOOLTIP = "Adds information properties";
    public static final String PROPERTY_MARGIN_METADATA_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "Metadata";
    public static final String PROPERTY_MARGIN_METADATA_DEFAULT = "file,product_type,scene_date_long,file_location";
    public static final Class PROPERTY_MARGIN_METADATA_TYPE = String.class;

    public static final String PROPERTY_MARGIN_METADATA2_KEY = PROPERTY_MARGIN_ROOT_KEY + ".metadata2";
    public static final String PROPERTY_MARGIN_METADATA2_LABEL = "PROPERTIES List";
    public static final String PROPERTY_MARGIN_METADATA2_TOOLTIP = "Adds information properties";
    public static final String PROPERTY_MARGIN_METADATA2_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "Metadata2";
    public static final String PROPERTY_MARGIN_METADATA2_DEFAULT = "band,band_description,units,scene_size";
    public static final Class PROPERTY_MARGIN_METADATA2_TYPE = String.class;

    public static final String PROPERTY_MARGIN_METADATA3_KEY = PROPERTY_MARGIN_ROOT_KEY + ".metadata3";
    public static final String PROPERTY_MARGIN_METADATA3_LABEL = "GLOBAL_ATTRIBUTES List";
    public static final String PROPERTY_MARGIN_METADATA3_TOOLTIP = "Adds metadata based on a key list";
    public static final String PROPERTY_MARGIN_METADATA3_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "Metadata3";
    public static final String PROPERTY_MARGIN_METADATA3_DEFAULT = "title, instrument, platform";
    public static final Class PROPERTY_MARGIN_METADATA3_TYPE = String.class;

    public static final String PROPERTY_MARGIN_METADATA4_KEY = PROPERTY_MARGIN_ROOT_KEY + ".metadata4";
    public static final String PROPERTY_MARGIN_METADATA4_LABEL = "GLOBAL_ATTRIBUTES List";
    public static final String PROPERTY_MARGIN_METADATA4_TOOLTIP = "Adds metadata based on a key list";
    public static final String PROPERTY_MARGIN_METADATA4_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "Metadata4";
    public static final String PROPERTY_MARGIN_METADATA4_DEFAULT = "processing_version, processing_control_software_name";
    public static final Class PROPERTY_MARGIN_METADATA4_TYPE = String.class;

    public static final String PROPERTY_MARGIN_METADATA5_KEY = PROPERTY_MARGIN_ROOT_KEY + ".metadata5";
    public static final String PROPERTY_MARGIN_METADATA5_LABEL = "BAND_ATTRIBUTES List";
    public static final String PROPERTY_MARGIN_METADATA5_TOOLTIP = "Adds band-metadata based on a key list";
    public static final String PROPERTY_MARGIN_METADATA5_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "Metadata5";
    public static final String PROPERTY_MARGIN_METADATA5_DEFAULT = "long_name, units, valid_min, valid_max";
    public static final Class PROPERTY_MARGIN_METADATA5_TYPE = String.class;

    public static final String PROPERTY_MARGIN_METADATA_KEYS_SHOW_KEY = PROPERTY_MARGIN_ROOT_KEY + ".metadata.keys.show";
    public static final String PROPERTY_MARGIN_METADATA_KEYS_SHOW_LABEL = "Show Missing";
    public static final String PROPERTY_MARGIN_METADATA_KEYS_SHOW_TOOLTIP = "Shows params even if missing a value";
    public static final String PROPERTY_MARGIN_METADATA_KEYS_SHOW_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "MetadataKeysShow";
    public static final boolean PROPERTY_MARGIN_METADATA_KEYS_SHOW_DEFAULT = true;
    public static final Class PROPERTY_MARGIN_METADATA_KEYS_SHOW_TYPE = Boolean.class;

    public static final String PROPERTY_MARGIN_METADATA_DELIMITER_KEY = PROPERTY_MARGIN_ROOT_KEY + ".metadata.delimiter";
    public static final String PROPERTY_MARGIN_METADATA_DELIMITER_LABEL = "Keys Delimiter";
    public static final String PROPERTY_MARGIN_METADATA_DELIMITER_TOOLTIP = "Delimiter to use when auto-displaying metadata key-value pairs";
    public static final String PROPERTY_MARGIN_METADATA_DELIMITER_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "MetadataDelimiter";
    public static final String PROPERTY_MARGIN_METADATA_DELIMITER_DEFAULT = ": ";
    public static final Class PROPERTY_MARGIN_METADATA_DELIMITER_TYPE = String.class;


    public static final String PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_KEY = PROPERTY_MARGIN_ROOT_KEY + ".show.all.info";
    public static final String PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_LABEL = "PROPERTIES Show All";
    public static final String PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_TOOLTIP = "Display all info keys";
    public static final String PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "ShowAllInfo";
    public static final boolean PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_DEFAULT = false;
    public static final Class PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_TYPE = Boolean.class;


    public static final String PROPERTY_MARGIN_METADATA_SHOW_ALL_KEY = PROPERTY_MARGIN_ROOT_KEY + ".metadata.show.all";
    public static final String PROPERTY_MARGIN_METADATA_SHOW_ALL_LABEL = "GLOBAL_ATTRIBUTES Show All *";
    public static final String PROPERTY_MARGIN_METADATA_SHOW_ALL_TOOLTIP = "Display all metadata keys (does NOT include all processing control params)";
    public static final String PROPERTY_MARGIN_METADATA_SHOW_ALL_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "MetadataShowAll";
    public static final boolean PROPERTY_MARGIN_METADATA_SHOW_ALL_DEFAULT = false;
    public static final Class PROPERTY_MARGIN_METADATA_SHOW_ALL_TYPE = Boolean.class;

    public static final String PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_KEY = PROPERTY_MARGIN_ROOT_KEY + ".metadata.process.control.show.all";
    public static final String PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_LABEL = "GLOBAL_ATTRIBUTES Show All (Processing Control Params)";
    public static final String PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_TOOLTIP = "Display all metadata processing control params keys";
    public static final String PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "MetadataProcessControlShowAll";
    public static final boolean PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_DEFAULT = false;
    public static final Class PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_TYPE = Boolean.class;

    public static final String PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_KEY = PROPERTY_MARGIN_ROOT_KEY + ".band.metadata.show.all";
    public static final String PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_LABEL = "BAND_ATTRIBUTES Show All";
    public static final String PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_TOOLTIP = "Display all band metadata keys";
    public static final String PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "BandMetadataShow";
    public static final boolean PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_DEFAULT = false;
    public static final Class PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_TYPE = Boolean.class;


    public static final String PROPERTY_MARGIN_CONVERT_CARET_KEY = PROPERTY_MARGIN_ROOT_KEY + ".convert.caret";
    public static final String PROPERTY_MARGIN_CONVERT_CARET_LABEL = "Convert Margin Carets to Superscripts";
    public static final String PROPERTY_MARGIN_CONVERT_CARET_TOOLTIP = "Convert any caret (^) symbols found in the text into a formatted superscript";
    public static final String PROPERTY_MARGIN_CONVERT_CARET_ALIAS = PROPERTY_MARGIN_ROOT_ALIAS + "ConvertCaret";
    public static final boolean PROPERTY_MARGIN_CONVERT_CARET_DEFAULT = true;
    public static final Class PROPERTY_MARGIN_CONVERT_CARET_TYPE = Boolean.class;






    // Margin Format Section

    private static final String PROPERTY_MARGIN_FORMAT_ROOT_KEY = PROPERTY_ROOT_KEY + ".margin.formatting";
    private static final String PROPERTY_MARGIN_FORMAT_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "MarginFormatting";

    public static final String PROPERTY_MARGIN_FORMATTING_SECTION_KEY = PROPERTY_MARGIN_FORMAT_ROOT_KEY + ".section";
    public static final String PROPERTY_MARGIN_FORMATTING_SECTION_LABEL = "Margin Font Formatting";
    public static final String PROPERTY_MARGIN_FORMATTING_SECTION_TOOLTIP = "Set  location of matadata on the scene image";
    public static final String PROPERTY_MARGIN_FORMATTING_SECTION_ALIAS = PROPERTY_MARGIN_FORMAT_ROOT_ALIAS + "SubSection";

    public static final String PROPERTY_MARGIN_FONT_SIZE_KEY = PROPERTY_MARGIN_FORMAT_ROOT_KEY + ".size";
    public static final String PROPERTY_MARGIN_FONT_SIZE_LABEL = "Margin Font Size";
    public static final String PROPERTY_MARGIN_FONT_SIZE_TOOLTIP = "Set size of the footer text";
    private static final String PROPERTY_MARGIN_FONT_SIZE_ALIAS =  PROPERTY_MARGIN_FORMAT_ROOT_ALIAS + "Size";
    public static final int PROPERTY_MARGIN_FONT_SIZE_DEFAULT = 20;
    public static final Class PROPERTY_MARGIN_FONT_SIZE_TYPE = Integer.class;
    public static final int PROPERTY_MARGIN_FONT_SIZE_MIN = 6;
    public static final int PROPERTY_MARGIN_FONT_SIZE_MAX = 70;
    public static final String PROPERTY_MARGIN_FONT_SIZE_INTERVAL = "[" + PROPERTY_MARGIN_FONT_SIZE_MIN + "," + PROPERTY_MARGIN_FONT_SIZE_MAX + "]";

    public static final String PROPERTY_MARGIN_FONT_COLOR_KEY = PROPERTY_MARGIN_FORMAT_ROOT_KEY + ".font.color";
    public static final String PROPERTY_MARGIN_FONT_COLOR_LABEL = "Margin Font Color";
    public static final String PROPERTY_MARGIN_FONT_COLOR_TOOLTIP = "Set color of the footer text";
    private static final String PROPERTY_MARGIN_FONT_COLOR_ALIAS = PROPERTY_MARGIN_FORMAT_ROOT_ALIAS + "FontColor";
    public static final Color PROPERTY_MARGIN_FONT_COLOR_DEFAULT = Color.BLACK;
    public static final Class PROPERTY_MARGIN_FONT_COLOR_TYPE = Color.class;

    public static final String PROPERTY_MARGIN_FONT_STYLE_KEY = PROPERTY_MARGIN_FORMAT_ROOT_KEY + ".font.style";
    public static final String PROPERTY_MARGIN_FONT_STYLE_LABEL = "Margin Font Type";
    public static final String PROPERTY_MARGIN_FONT_STYLE_TOOLTIP = "Set the font style of the footer";
    public static final String PROPERTY_MARGIN_FONT_STYLE_ALIAS = PROPERTY_MARGIN_FORMAT_ROOT_ALIAS + "FontName";
    public static final String PROPERTY_MARGIN_FONT_STYLE_DEFAULT = "SansSerif";
    public static final Class PROPERTY_MARGIN_FONT_STYLE_TYPE = String.class;
    public static final Object PROPERTY_MARGIN_FONT_STYLE_VALUE_SET[] = {PROPERTY_FONT_STYLE_1, PROPERTY_FONT_STYLE_2, PROPERTY_FONT_STYLE_3, PROPERTY_FONT_STYLE_4};

    public static final String PROPERTY_MARGIN_FONT_ITALIC_KEY = PROPERTY_MARGIN_FORMAT_ROOT_KEY + ".font.italic";
    public static final String PROPERTY_MARGIN_FONT_ITALIC_LABEL = "Margin Font Italic";
    public static final String PROPERTY_MARGIN_FONT_ITALIC_TOOLTIP = "Format footer text font in italic";
    public static final String PROPERTY_MARGIN_FONT_ITALIC_ALIAS = PROPERTY_MARGIN_FORMAT_ROOT_ALIAS + "FontItalic";
    public static final boolean PROPERTY_MARGIN_FONT_ITALIC_DEFAULT = false;
    public static final Class PROPERTY_MARGIN_FONT_ITALIC_TYPE = Boolean.class;

    public static final String PROPERTY_MARGIN_FONT_BOLD_KEY = PROPERTY_MARGIN_FORMAT_ROOT_KEY + ".font.bold";
    public static final String PROPERTY_MARGIN_FONT_BOLD_LABEL = "Margin Font Bold";
    public static final String PROPERTY_MARGIN_FONT_BOLD_TOOLTIP = "Format footer text font in bold";
    public static final String PROPERTY_MARGIN_FONT_BOLD_ALIAS = PROPERTY_MARGIN_FORMAT_ROOT_ALIAS + "FontBold";
    public static final boolean PROPERTY_MARGIN_FONT_BOLD_DEFAULT = false;
    public static final Class PROPERTY_MARGIN_FONT_BOLD_TYPE = Boolean.class;





    // Header Section

    private static final String PROPERTY_HEADER_CONTENTS_ROOT_KEY = PROPERTY_ROOT_KEY + ".header.contents";
    private static final String PROPERTY_HEADER_CONTENTS_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "headerContents";

    public static final String PROPERTY_HEADER_SECTION_KEY = PROPERTY_HEADER_CONTENTS_ROOT_KEY + ".section";
    public static final String PROPERTY_HEADER_SECTION_LABEL = "Header Annotation";
    public static final String PROPERTY_HEADER_SECTION_TOOLTIP = "create header";
    public static final String PROPERTY_HEADER_SECTION_ALIAS = PROPERTY_HEADER_CONTENTS_ROOT_ALIAS + "Section";

    public static final String PROPERTY_HEADER_SHOW_KEY = PROPERTY_HEADER_CONTENTS_ROOT_KEY + ".show";
    public static final String PROPERTY_HEADER_SHOW_LABEL = "Show Header Annotation";
    public static final String PROPERTY_HEADER_SHOW_TOOLTIP = "Show header";
    public static final String PROPERTY_HEADER_SHOW_ALIAS = PROPERTY_HEADER_CONTENTS_ROOT_ALIAS + "Show";
    public static final boolean PROPERTY_HEADER_SHOW_DEFAULT = true;
    public static final Class PROPERTY_HEADER_SHOW_TYPE = Boolean.class;


    public static final String PROPERTY_HEADER_TEXTFIELD_KEY = PROPERTY_HEADER_CONTENTS_ROOT_KEY + ".textfield";
    public static final String PROPERTY_HEADER_TEXTFIELD_LABEL = "Header Text";
    public static final String PROPERTY_HEADER_TEXTFIELD_TOOLTIP = "Adds a title/header to the Header-Footer Layer";
    public static final String PROPERTY_HEADER_TEXTFIELD_ALIAS = PROPERTY_HEADER_CONTENTS_ROOT_ALIAS + "Textfield";
//    public static final String PROPERTY_HEADER_TEXTFIELD_DEFAULT = "Band: <PROPERTY=band> (<PROPERTY=band_description>)<br><GLOBAL_ATTR=title>";
//    public static final String PROPERTY_HEADER_TEXTFIELD_DEFAULT = "<PROPERTY=band_description> - Band: '<PROPERTY=band>'";
//    public static final String PROPERTY_HEADER_TEXTFIELD_DEFAULT = "<PROPERTY=mission_level_info> <PROPERTY=temporal_range_parenthesis>";
    public static final String PROPERTY_HEADER_TEXTFIELD_DEFAULT = "<PROPERTY=mission_level_info>";
    public static final Class PROPERTY_HEADER_TEXTFIELD_TYPE = String.class;

    public static final String PROPERTY_HEADER_TEXTFIELD2_KEY = PROPERTY_HEADER_CONTENTS_ROOT_KEY + ".textfield2";
    public static final String PROPERTY_HEADER_TEXTFIELD2_LABEL = "Header Text";
    public static final String PROPERTY_HEADER_TEXTFIELD2_TOOLTIP = "Adds a line to title/header to the Header-Footer Layer";
    public static final String PROPERTY_HEADER_TEXTFIELD2_ALIAS = PROPERTY_HEADER_CONTENTS_ROOT_ALIAS + "Textfield2";
//    public static final String PROPERTY_HEADER_TEXTFIELD2_DEFAULT = "<PROPERTY=scene_date_MONTHDDYYYY><br>File: <PROPERTY=file>";
//    public static final String PROPERTY_HEADER_TEXTFIELD2_DEFAULT = "<GLOBAL_ATTR=platform>-<GLOBAL_ATTR=sensor>: <PROPERTY=product_type> <PROPERTY=temporal_range_parenthesis>";
//    public static final String PROPERTY_HEADER_TEXTFIELD2_DEFAULT = "<PROPERTY=mission_level_info> <PROPERTY=temporal_range_parenthesis>";
    public static final String PROPERTY_HEADER_TEXTFIELD2_DEFAULT = "<PROPERTY=band_description>";
    public static final Class PROPERTY_HEADER_TEXTFIELD2_TYPE = String.class;

    public static final String PROPERTY_HEADER_TEXTFIELD3_KEY = PROPERTY_HEADER_CONTENTS_ROOT_KEY + ".textfield3";
    public static final String PROPERTY_HEADER_TEXTFIELD3_LABEL = "Header Text";
    public static final String PROPERTY_HEADER_TEXTFIELD3_TOOLTIP = "Adds a line to title/header to the Header-Footer Layer";
    public static final String PROPERTY_HEADER_TEXTFIELD3_ALIAS = PROPERTY_HEADER_CONTENTS_ROOT_ALIAS + "Textfield3";
//    public static final String PROPERTY_HEADER_TEXTFIELD3_DEFAULT = "<PROPERTY=scene_date_ddmmmyyyy><br>File: <PROPERTY=file>";
//    public static final String PROPERTY_HEADER_TEXTFIELD3_DEFAULT = "<PROPERTY=scene_date_ddmmmyyyy> <PROPERTY=temporal_range_parenthesis>";
    public static final String PROPERTY_HEADER_TEXTFIELD3_DEFAULT = "<PROPERTY=scene_date_info>";
    public static final Class PROPERTY_HEADER_TEXTFIELD3_TYPE = String.class;

    public static final String PROPERTY_HEADER_TEXTFIELD4_KEY = PROPERTY_HEADER_CONTENTS_ROOT_KEY + ".textfield4";
    public static final String PROPERTY_HEADER_TEXTFIELD4_LABEL = "Header Text";
    public static final String PROPERTY_HEADER_TEXTFIELD4_TOOLTIP = "Adds a line to title/header to the Header-Footer Layer";
    public static final String PROPERTY_HEADER_TEXTFIELD4_ALIAS = PROPERTY_HEADER_CONTENTS_ROOT_ALIAS + "Textfield4";
    public static final String PROPERTY_HEADER_TEXTFIELD4_DEFAULT = "File: <PROPERTY=file> - Band: <PROPERTY=band>";
    public static final Class PROPERTY_HEADER_TEXTFIELD4_TYPE = String.class;




    public static final String PROPERTY_HEADER_CONVERT_CARET_KEY = PROPERTY_HEADER_CONTENTS_ROOT_KEY + ".convert.caret";
    public static final String PROPERTY_HEADER_CONVERT_CARET_LABEL = "Convert Header Carets to Superscripts";
    public static final String PROPERTY_HEADER_CONVERT_CARET_TOOLTIP = "Convert any caret (^) symbols found in the text into a formatted superscript";
    public static final String PROPERTY_HEADER_CONVERT_CARET_ALIAS = PROPERTY_HEADER_CONTENTS_ROOT_ALIAS + "ConvertCaret";
    public static final boolean PROPERTY_HEADER_CONVERT_CARET_DEFAULT = true;
    public static final Class PROPERTY_HEADER_CONVERT_CARET_TYPE = Boolean.class;






    // Header Formatting Section

    private static final String PROPERTY_HEADER_FORMAT_ROOT_KEY = PROPERTY_ROOT_KEY + ".header.format";
    private static final String PROPERTY_HEADER_FORMAT_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "HeaderFormat";

    public static final String PROPERTY_HEADER_FORMAT_SECTION_KEY = PROPERTY_HEADER_FORMAT_ROOT_KEY + ".section";
    public static final String PROPERTY_HEADER_FORMAT_SECTION_LABEL = "Header Font Formatting";
    public static final String PROPERTY_HEADER_FORMAT_SECTION_TOOLTIP = "Formatting parameters for the header";
    public static final String PROPERTY_HEADER_FORMAT_SECTION_ALIAS = PROPERTY_HEADER_FORMAT_ROOT_ALIAS + "SubSection";


    private static final String PROPERTY_HEADER_LOCATION_ROOT_KEY = PROPERTY_ROOT_KEY + ".header.location";
    private static final String PROPERTY_HEADER_LOCATION_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "HeaderLocation";

    public static final String PROPERTY_HEADER_LOCATION_SECTION_KEY = PROPERTY_HEADER_LOCATION_ROOT_KEY + ".subsection";
    public static final String PROPERTY_HEADER_LOCATION_SECTION_LABEL = "Annotation Contents";
    public static final String PROPERTY_HEADER_LOCATION_SECTION_TOOLTIP = "Formatting parameters for the header";
    public static final String PROPERTY_HEADER_LOCATION_SECTION_ALIAS = PROPERTY_HEADER_LOCATION_ROOT_ALIAS + "Section";

    public static final String PROPERTY_HEADER_LOCATION_KEY = PROPERTY_HEADER_FORMAT_ROOT_KEY + ".location";
    public static final String PROPERTY_HEADER_LOCATION_LABEL = "Header Location";
    public static final String PROPERTY_HEADER_LOCATION_TOOLTIP = "Where to place the header on the image";
    private static final String PROPERTY_HEADER_LOCATION_ALIAS = PROPERTY_HEADER_FORMAT_ROOT_ALIAS + "HeaderLocation";
    public static final String PROPERTY_HEADER_LOCATION_DEFAULT = LOCATION_TOP_CENTER;
    public static final Class PROPERTY_HEADER_LOCATION_TYPE = String.class;

    public static final String PROPERTY_HEADER_GAP_KEY = PROPERTY_HEADER_FORMAT_ROOT_KEY + ".gap";
    public static final String PROPERTY_HEADER_GAP_LABEL = "Header Gap";
    public static final String PROPERTY_HEADER_GAP_TOOLTIP = "Percentage of scene size to place header away from the edge of the scene image";
    private static final String PROPERTY_HEADER_GAP_ALIAS = PROPERTY_HEADER_FORMAT_ROOT_ALIAS + "Offset";
    public static final Double PROPERTY_HEADER_GAP_DEFAULT = 5.0;
    public static final double PROPERTY_HEADER_GAP_MIN = -100;
    public static final double PROPERTY_HEADER_GAP_MAX = 100;
    public static final String PROPERTY_HEADER_GAP_INTERVAL = "[" + PROPERTY_HEADER_GAP_MIN + "," + PROPERTY_HEADER_GAP_MAX + "]";
    public static final Class PROPERTY_HEADER_GAP_TYPE = Double.class;

    public static final String PROPERTY_HEADER_FONT_SIZE_KEY = PROPERTY_HEADER_FORMAT_ROOT_KEY + ".font.size";
    public static final String PROPERTY_HEADER_FONT_SIZE_LABEL = "Header Font Size";
    public static final String PROPERTY_HEADER_FONT_SIZE_TOOLTIP = "Set size of the header font";
    private static final String PROPERTY_HEADER_FONT_SIZE_ALIAS =  PROPERTY_HEADER_FORMAT_ROOT_ALIAS + "FontSize";
    public static final int PROPERTY_HEADER_FONT_SIZE_DEFAULT = 40;
    public static final Class PROPERTY_HEADER_FONT_SIZE_TYPE = Integer.class;
    public static final int PROPERTY_HEADER_FONT_SIZE_VALUE_MIN = 6;
    public static final int PROPERTY_HEADER_FONT_SIZE_VALUE_MAX = 70;
    public static final String PROPERTY_HEADER_FONT_SIZE_INTERVAL = "[" + GraticuleLayerType.PROPERTY_LABELS_SIZE_VALUE_MIN + "," + GraticuleLayerType.PROPERTY_LABELS_SIZE_VALUE_MAX + "]";

    public static final String PROPERTY_HEADER_FONT_COLOR_KEY = PROPERTY_HEADER_FORMAT_ROOT_KEY + ".font.color";
    public static final String PROPERTY_HEADER_FONT_COLOR_LABEL = "Header Font Color";
    public static final String PROPERTY_HEADER_FONT_COLOR_TOOLTIP = "Set color of the header text";
    private static final String PROPERTY_HEADER_FONT_COLOR_ALIAS = PROPERTY_HEADER_FORMAT_ROOT_ALIAS + "FontColor";
    public static final Color PROPERTY_HEADER_FONT_COLOR_DEFAULT = Color.BLACK;
    public static final Class PROPERTY_HEADER_FONT_COLOR_TYPE = Color.class;

    public static final String PROPERTY_HEADER_FONT_STYLE_KEY = PROPERTY_HEADER_FORMAT_ROOT_KEY + ".font.name";
    public static final String PROPERTY_HEADER_FONT_STYLE_LABEL = "Header Font Type";
    public static final String PROPERTY_HEADER_FONT_STYLE_TOOLTIP = "Set the font name of the header";
    public static final String PROPERTY_HEADER_FONT_STYLE_ALIAS = PROPERTY_HEADER_FORMAT_ROOT_ALIAS + "FontName";
    public static final String PROPERTY_HEADER_FONT_STYLE_DEFAULT = "Serif";
    public static final Class PROPERTY_HEADER_FONT_STYLE_TYPE = String.class;
    public static final Object PROPERTY_HEADER_FONT_STYLE_VALUE_SET[] = {PROPERTY_FONT_STYLE_1, PROPERTY_FONT_STYLE_2, PROPERTY_FONT_STYLE_3, PROPERTY_FONT_STYLE_4};

    public static final String PROPERTY_HEADER_FONT_ITALIC_KEY = PROPERTY_HEADER_FORMAT_ROOT_KEY + ".font.italic";
    public static final String PROPERTY_HEADER_FONT_ITALIC_LABEL = "Header Font Italic";
    public static final String PROPERTY_HEADER_FONT_ITALIC_TOOLTIP = "Format header text font in italic";
    public static final String PROPERTY_HEADER_FONT_ITALIC_ALIAS = PROPERTY_HEADER_FORMAT_ROOT_ALIAS + "FontItalic";
    public static final boolean PROPERTY_HEADER_FONT_ITALIC_DEFAULT = true;
    public static final Class PROPERTY_HEADER_FONT_ITALIC_TYPE = Boolean.class;

    public static final String PROPERTY_HEADER_FONT_BOLD_KEY = PROPERTY_HEADER_FORMAT_ROOT_KEY + ".font.bold";
    public static final String PROPERTY_HEADER_FONT_BOLD_LABEL = "Header Font Bold";
    public static final String PROPERTY_HEADER_FONT_BOLD_TOOLTIP = "Format header text font in bold";
    public static final String PROPERTY_HEADER_FONT_BOLD_ALIAS = PROPERTY_HEADER_FORMAT_ROOT_ALIAS + "FontBold";
    public static final boolean PROPERTY_HEADER_FONT_BOLD_DEFAULT = false;
    public static final Class PROPERTY_HEADER_FONT_BOLD_TYPE = Boolean.class;







    // Footer2 Contents

    private static final String PROPERTY_FOOTER2_ROOT_KEY = PROPERTY_ROOT_KEY + ".footer2.contents";
    private static final String PROPERTY_FOOTER2_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "Footer2Contents";

    public static final String PROPERTY_FOOTER2_SECTION_KEY = PROPERTY_FOOTER2_ROOT_KEY + ".section";
    public static final String PROPERTY_FOOTER2_SECTION_LABEL = "Footer Annotation";
    public static final String PROPERTY_FOOTER2_SECTION_TOOLTIP = "Contents of footer";
    public static final String PROPERTY_FOOTER2_SECTION_ALIAS = PROPERTY_FOOTER2_ROOT_ALIAS + "Section";

    public static final String PROPERTY_FOOTER2_SHOW_KEY = PROPERTY_FOOTER2_ROOT_KEY + ".show";
    public static final String PROPERTY_FOOTER2_SHOW_LABEL = "Show Footer Annotation";
    public static final String PROPERTY_FOOTER2_SHOW_TOOLTIP = "Show footer";
    public static final String PROPERTY_FOOTER2_SHOW_ALIAS = PROPERTY_FOOTER2_ROOT_ALIAS + "Show";
    public static final boolean PROPERTY_FOOTER2_SHOW_DEFAULT = false;
    public static final Class PROPERTY_FOOTER2_SHOW_TYPE = Boolean.class;

    public static final String PROPERTY_FOOTER2_TEXTFIELD_KEY = PROPERTY_FOOTER2_ROOT_KEY + ".textfield";
    public static final String PROPERTY_FOOTER2_TEXTFIELD_LABEL = "Footer Text";
    public static final String PROPERTY_FOOTER2_TEXTFIELD_TOOLTIP = "Adds a footer to the Header-Footer Layer";
    public static final String PROPERTY_FOOTER2_TEXTFIELD_ALIAS = PROPERTY_FOOTER2_ROOT_ALIAS + "Textfield";
    public static final String PROPERTY_FOOTER2_TEXTFIELD_DEFAULT = "File: <PROPERTY=file>";
    public static final Class PROPERTY_FOOTER2_TEXTFIELD_TYPE = String.class;

    public static final String PROPERTY_FOOTER2_TEXTFIELD2_KEY = PROPERTY_FOOTER2_ROOT_KEY + ".textfield2";
    public static final String PROPERTY_FOOTER2_TEXTFIELD2_LABEL = "Footer Text";
    public static final String PROPERTY_FOOTER2_TEXTFIELD2_TOOLTIP = "Adds a footer to the Header-Footer Layer";
    public static final String PROPERTY_FOOTER2_TEXTFIELD2_ALIAS = PROPERTY_FOOTER2_ROOT_ALIAS + "Textfield2";
    public static final String PROPERTY_FOOTER2_TEXTFIELD2_DEFAULT = "";
    public static final Class PROPERTY_FOOTER2_TEXTFIELD2_TYPE = String.class;

    public static final String PROPERTY_FOOTER2_TEXTFIELD3_KEY = PROPERTY_FOOTER2_ROOT_KEY + ".textfield3";
    public static final String PROPERTY_FOOTER2_TEXTFIELD3_LABEL = "Footer Text";
    public static final String PROPERTY_FOOTER2_TEXTFIELD3_TOOLTIP = "Adds a footer to the Header-Footer Layer";
    public static final String PROPERTY_FOOTER2_TEXTFIELD3_ALIAS = PROPERTY_FOOTER2_ROOT_ALIAS + "Textfield3";
    public static final String PROPERTY_FOOTER2_TEXTFIELD3_DEFAULT = "";
    public static final Class PROPERTY_FOOTER2_TEXTFIELD3_TYPE = String.class;

    public static final String PROPERTY_FOOTER2_TEXTFIELD4_KEY = PROPERTY_FOOTER2_ROOT_KEY + ".textfield4";
    public static final String PROPERTY_FOOTER2_TEXTFIELD4_LABEL = "Footer Text";
    public static final String PROPERTY_FOOTER2_TEXTFIELD4_TOOLTIP = "Adds a footer to the Header-Footer Layer";
    public static final String PROPERTY_FOOTER2_TEXTFIELD4_ALIAS = PROPERTY_FOOTER2_ROOT_ALIAS + "Textfield4";
    public static final String PROPERTY_FOOTER2_TEXTFIELD4_DEFAULT = "";
    public static final Class PROPERTY_FOOTER2_TEXTFIELD4_TYPE = String.class;



    public static final String PROPERTY_FOOTER_CONVERT_CARET_KEY = PROPERTY_FOOTER2_ROOT_KEY + ".convert.caret";
    public static final String PROPERTY_FOOTER_CONVERT_CARET_LABEL = "Convert Footer Carets to Superscripts";
    public static final String PROPERTY_FOOTER_CONVERT_CARET_TOOLTIP = "Convert any caret (^) symbols found in the text into a formatted superscript";
    public static final String PROPERTY_FOOTER_CONVERT_CARET_ALIAS = PROPERTY_FOOTER2_ROOT_ALIAS + "ConvertCaret";
    public static final boolean PROPERTY_FOOTER_CONVERT_CARET_DEFAULT = true;
    public static final Class PROPERTY_FOOTER_CONVERT_CARET_TYPE = Boolean.class;



    public static final String PROPERTY_FOOTER2_MY_INFO_SHOW_KEY = PROPERTY_FOOTER2_ROOT_KEY + ".show.my.info";
    public static final String PROPERTY_FOOTER2_MY_INFO_SHOW_LABEL = "Show My Info";
    public static final String PROPERTY_FOOTER2_MY_INFO_SHOW_TOOLTIP = "Show my_info";
    public static final String PROPERTY_FOOTER2_MY_INFO_SHOW_ALIAS = PROPERTY_FOOTER2_ROOT_ALIAS + "ShowMyInfo";
    public static final boolean PROPERTY_FOOTER2_MY_INFO_SHOW_DEFAULT = false;
    public static final Class PROPERTY_FOOTER2_MY_INFO_SHOW_TYPE = Boolean.class;



    // Footer2 Format Section

    private static final String PROPERTY_FOOTER2_FORMAT_ROOT_KEY = PROPERTY_ROOT_KEY + ".footer2.formatting";
    private static final String PROPERTY_FOOTER2_FORMAT_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "Footer2Formatting";

    public static final String PROPERTY_FOOTER2_FORMATTING_SECTION_KEY = PROPERTY_FOOTER2_FORMAT_ROOT_KEY + ".section";
    public static final String PROPERTY_FOOTER2_FORMATTING_SECTION_LABEL = "Footer Font Formatting";
    public static final String PROPERTY_FOOTER2_FORMATTING_SECTION_TOOLTIP = "Set  location of footer on the scene image";
    public static final String PROPERTY_FOOTER2_FORMATTING_SECTION_ALIAS = PROPERTY_FOOTER2_FORMAT_ROOT_ALIAS + "SubSection";

    public static final String PROPERTY_FOOTER2_LOCATION_KEY = PROPERTY_FOOTER2_FORMAT_ROOT_KEY + ".location";
    public static final String PROPERTY_FOOTER2_LOCATION_LABEL = "Footer Location";
    public static final String PROPERTY_FOOTER2_LOCATION_TOOLTIP = "Where to place the footer on the image";
    private static final String PROPERTY_FOOTER2_LOCATION_ALIAS = PROPERTY_FOOTER2_FORMAT_ROOT_ALIAS + "Location";
    public static final String PROPERTY_FOOTER2_LOCATION_DEFAULT = LOCATION_BOTTOM_CENTER;
    public static final Class PROPERTY_FOOTER2_LOCATION_TYPE = String.class;

    public static final String PROPERTY_FOOTER2_GAP_KEY = PROPERTY_FOOTER2_FORMAT_ROOT_KEY + ".offset";
    public static final String PROPERTY_FOOTER2_GAP_LABEL = "Footer Gap";
    public static final String PROPERTY_FOOTER2_GAP_TOOLTIP = "Percentage of scene size to place footer away from the edge of the scene image";
    private static final String PROPERTY_FOOTER2_GAP_ALIAS = PROPERTY_FOOTER2_FORMAT_ROOT_ALIAS + "Offset";
    public static final Double PROPERTY_FOOTER2_GAP_DEFAULT = 50.0;
    public static final double PROPERTY_FOOTER2_GAP_MIN = -100;
    public static final double PROPERTY_FOOTER2_GAP_MAX = 100;
    public static final String PROPERTY_FOOTER2_GAP_INTERVAL = "[" + MetaDataLayerType.PROPERTY_MARGIN_GAP_MIN + "," + MetaDataLayerType.PROPERTY_MARGIN_GAP_MAX + "]";
    public static final Class PROPERTY_FOOTER2_GAP_TYPE = Double.class;


    public static final String PROPERTY_FOOTER2_FONT_SIZE_KEY = PROPERTY_FOOTER2_FORMAT_ROOT_KEY + ".size";
    public static final String PROPERTY_FOOTER2_FONT_SIZE_LABEL = "Footer Font Size";
    public static final String PROPERTY_FOOTER2_FONT_SIZE_TOOLTIP = "Set size of the footer text";
    private static final String PROPERTY_FOOTER2_FONT_SIZE_ALIAS =  PROPERTY_FOOTER2_FORMAT_ROOT_ALIAS + "Size";
    public static final int PROPERTY_FOOTER2_FONT_SIZE_DEFAULT = 20;
    public static final Class PROPERTY_FOOTER2_FONT_SIZE_TYPE = Integer.class;
    public static final int PROPERTY_FOOTER2_FONT_SIZE_MIN = 6;
    public static final int PROPERTY_FOOTER2_FONT_SIZE_MAX = 70;
    public static final String PROPERTY_FOOTER2_FONT_SIZE_INTERVAL = "[" + PROPERTY_FOOTER2_FONT_SIZE_MIN + "," + PROPERTY_FOOTER2_FONT_SIZE_MAX + "]";

    public static final String PROPERTY_FOOTER2_FONT_COLOR_KEY = PROPERTY_FOOTER2_FORMAT_ROOT_KEY + ".font.color";
    public static final String PROPERTY_FOOTER2_FONT_COLOR_LABEL = "Footer Font Color";
    public static final String PROPERTY_FOOTER2_FONT_COLOR_TOOLTIP = "Set color of the footer text";
    private static final String PROPERTY_FOOTER2_FONT_COLOR_ALIAS = PROPERTY_FOOTER2_FORMAT_ROOT_ALIAS + "FontColor";
    public static final Color PROPERTY_FOOTER2_FONT_COLOR_DEFAULT = Color.BLACK;
    public static final Class PROPERTY_FOOTER2_FONT_COLOR_TYPE = Color.class;

    public static final String PROPERTY_FOOTER2_FONT_STYLE_KEY = PROPERTY_FOOTER2_FORMAT_ROOT_KEY + ".font.style";
    public static final String PROPERTY_FOOTER2_FONT_STYLE_LABEL = "Footer Font Type";
    public static final String PROPERTY_FOOTER2_FONT_STYLE_TOOLTIP = "Set the font style of the footer";
    public static final String PROPERTY_FOOTER2_FONT_STYLE_ALIAS = PROPERTY_FOOTER2_FORMAT_ROOT_ALIAS + "FontName";
    public static final String PROPERTY_FOOTER2_FONT_STYLE_DEFAULT = "SansSerif";
    public static final Class PROPERTY_FOOTER2_FONT_STYLE_TYPE = String.class;
    public static final Object PROPERTY_FOOTER2_FONT_STYLE_VALUE_SET[] = {PROPERTY_FONT_STYLE_1, PROPERTY_FONT_STYLE_2, PROPERTY_FONT_STYLE_3, PROPERTY_FONT_STYLE_4};

    public static final String PROPERTY_FOOTER2_FONT_ITALIC_KEY = PROPERTY_FOOTER2_FORMAT_ROOT_KEY + ".font.italic";
    public static final String PROPERTY_FOOTER2_FONT_ITALIC_LABEL = "Footer Font Italic";
    public static final String PROPERTY_FOOTER2_FONT_ITALIC_TOOLTIP = "Format footer text font in italic";
    public static final String PROPERTY_FOOTER2_FONT_ITALIC_ALIAS = PROPERTY_FOOTER2_FORMAT_ROOT_ALIAS + "FontItalic";
    public static final boolean PROPERTY_FOOTER2_FONT_ITALIC_DEFAULT = false;
    public static final Class PROPERTY_FOOTER2_FONT_ITALIC_TYPE = Boolean.class;

    public static final String PROPERTY_FOOTER2_FONT_BOLD_KEY = PROPERTY_FOOTER2_FORMAT_ROOT_KEY + ".font.bold";
    public static final String PROPERTY_FOOTER2_FONT_BOLD_LABEL = "Footer Font Bold";
    public static final String PROPERTY_FOOTER2_FONT_BOLD_TOOLTIP = "Format footer text font in bold";
    public static final String PROPERTY_FOOTER2_FONT_BOLD_ALIAS = PROPERTY_FOOTER2_FORMAT_ROOT_ALIAS + "FontBold";
    public static final boolean PROPERTY_FOOTER2_FONT_BOLD_DEFAULT = false;
    public static final Class PROPERTY_FOOTER2_FONT_BOLD_TYPE = Boolean.class;


    // My Info Section

    private static final String PROPERTY_MY_INFO_ROOT_KEY = PROPERTY_ROOT_KEY + ".my.info.contents";
    private static final String PROPERTY_MY_INFO_ROOT_ALIAS = PROPERTY_ROOT_ALIAS + "myInfoContents";

    public static final String PROPERTY_MY_INFO_SECTION_KEY = PROPERTY_MY_INFO_ROOT_KEY + ".section";
    public static final String PROPERTY_MY_INFO_SECTION_LABEL = "Contents of annotation parameter 'My Info'";
    public static final String PROPERTY_MY_INFO_SECTION_TOOLTIP = "Establishes annotation parameter 'My Info'";
    public static final String PROPERTY_MY_INFO_SECTION_ALIAS = PROPERTY_MY_INFO_ROOT_ALIAS + "Section";

    public static final String PROPERTY_MY_INFO_TEXTFIELD1_KEY = PROPERTY_MY_INFO_ROOT_KEY + ".textfield1";
    public static final String PROPERTY_MY_INFO_TEXTFIELD1_LABEL = "My Info Text";
    public static final String PROPERTY_MY_INFO_TEXTFIELD1_TOOLTIP = "First line of 'My Info'";
    public static final String PROPERTY_MY_INFO_TEXTFIELD1_ALIAS = PROPERTY_MY_INFO_ROOT_ALIAS + "HeaderTextfield1";
    public static final String PROPERTY_MY_INFO_TEXTFIELD1_DEFAULT = "{Add user info in Preferences > Layer > Annotation}";
    public static final Class PROPERTY_MY_INFO_TEXTFIELD1_TYPE = String.class;

    public static final String PROPERTY_MY_INFO_TEXTFIELD2_KEY = PROPERTY_MY_INFO_ROOT_KEY + ".textfield2";
    public static final String PROPERTY_MY_INFO_TEXTFIELD2_LABEL = "My Info Text";
    public static final String PROPERTY_MY_INFO_TEXTFIELD2_TOOLTIP = "Second line of 'My Info'";
    public static final String PROPERTY_MY_INFO_TEXTFIELD2_ALIAS = PROPERTY_MY_INFO_ROOT_ALIAS + "HeaderTextfield2";
    public static final String PROPERTY_MY_INFO_TEXTFIELD2_DEFAULT = "";
    public static final Class PROPERTY_MY_INFO_TEXTFIELD2_TYPE = String.class;

    public static final String PROPERTY_MY_INFO_TEXTFIELD3_KEY = PROPERTY_MY_INFO_ROOT_KEY + ".textfield3";
    public static final String PROPERTY_MY_INFO_TEXTFIELD3_LABEL = "My Info Text";
    public static final String PROPERTY_MY_INFO_TEXTFIELD3_TOOLTIP = "Third line of 'My Info'";
    public static final String PROPERTY_MY_INFO_TEXTFIELD3_ALIAS = PROPERTY_MY_INFO_ROOT_ALIAS + "HeaderTextfield3";
    public static final String PROPERTY_MY_INFO_TEXTFIELD3_DEFAULT = "";
    public static final Class PROPERTY_MY_INFO_TEXTFIELD3_TYPE = String.class;

    public static final String PROPERTY_MY_INFO_TEXTFIELD4_KEY = PROPERTY_MY_INFO_ROOT_KEY + ".textfield4";
    public static final String PROPERTY_MY_INFO_TEXTFIELD4_LABEL = "My Info Text";
    public static final String PROPERTY_MY_INFO_TEXTFIELD4_TOOLTIP = "Fourth line of 'My Info'";
    public static final String PROPERTY_MY_INFO_TEXTFIELD4_ALIAS = PROPERTY_MY_INFO_ROOT_ALIAS + "HeaderTextfield4";
    public static final String PROPERTY_MY_INFO_TEXTFIELD4_DEFAULT = "";
    public static final Class PROPERTY_MY_INFO_TEXTFIELD4_TYPE = String.class;




    // ---------------------------------------------------------

    public static final String PROPERTY_NAME_RASTER = "raster";



    // Property Setting: Restore Defaults
    public static final String PROPERTY_RESTORE_DEFAULTS_NAME = "metadata.layer.restoreDefaults";
    public static final String PROPERTY_RESTORE_TO_DEFAULTS_LABEL = "RESTORE DEFAULTS (Metadata Layer Preferences)";
    public static final String PROPERTY_RESTORE_TO_DEFAULTS_TOOLTIP = "Restore all metadata layer preferences to the default";
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
//        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();

        return new MetaDataLayer(this, (RasterDataNode) configuration.getValue(PROPERTY_NAME_RASTER),
                configuration);
    }

    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertyContainer vc = new PropertyContainer();

        // Parameters Section

        final Property headerSectionModel = Property.create(PROPERTY_HEADER_SECTION_KEY, Boolean.class, true, true);
        headerSectionModel.getDescriptor().setAlias(PROPERTY_HEADER_SECTION_ALIAS);
        vc.addProperty(headerSectionModel);

        final Property headerShowModel = Property.create(PROPERTY_HEADER_SHOW_KEY, PROPERTY_HEADER_SHOW_TYPE, true, true);
        headerShowModel.getDescriptor().setAlias(PROPERTY_HEADER_SHOW_ALIAS);
        vc.addProperty(headerShowModel);

        final Property headerModel = Property.create(PROPERTY_HEADER_TEXTFIELD_KEY, PROPERTY_HEADER_TEXTFIELD_TYPE, true, true);
        headerModel.getDescriptor().setAlias(PROPERTY_HEADER_TEXTFIELD_ALIAS);
        vc.addProperty(headerModel);

        final Property header2Model = Property.create(PROPERTY_HEADER_TEXTFIELD2_KEY, PROPERTY_HEADER_TEXTFIELD2_TYPE, true, true);
        header2Model.getDescriptor().setAlias(PROPERTY_HEADER_TEXTFIELD2_ALIAS);
        vc.addProperty(header2Model);

        final Property header3Model = Property.create(PROPERTY_HEADER_TEXTFIELD3_KEY, PROPERTY_HEADER_TEXTFIELD3_TYPE, true, true);
        header3Model.getDescriptor().setAlias(PROPERTY_HEADER_TEXTFIELD3_ALIAS);
        vc.addProperty(header3Model);

        final Property header4Model = Property.create(PROPERTY_HEADER_TEXTFIELD4_KEY, PROPERTY_HEADER_TEXTFIELD4_TYPE, true, true);
        header4Model.getDescriptor().setAlias(PROPERTY_HEADER_TEXTFIELD4_ALIAS);
        vc.addProperty(header4Model);







        final Property footerParametersSectionModel = Property.create(PROPERTY_MARGIN_SECTION_KEY, Boolean.class, true, true);
        footerParametersSectionModel.getDescriptor().setAlias(PROPERTY_MARGIN_SECTION_ALIAS);
        vc.addProperty(footerParametersSectionModel);

        final Property footerShowModel = Property.create(PROPERTY_MARGIN_SHOW_KEY, PROPERTY_MARGIN_SHOW_TYPE, true, true);
        footerShowModel.getDescriptor().setAlias(PROPERTY_MARGIN_SHOW_ALIAS);
        vc.addProperty(footerShowModel);

        final Property displayAllMetadataModel = Property.create(PROPERTY_MARGIN_METADATA_SHOW_ALL_KEY, PROPERTY_MARGIN_METADATA_SHOW_ALL_TYPE, true, true);
        displayAllMetadataModel.getDescriptor().setAlias(PROPERTY_MARGIN_METADATA_SHOW_ALL_ALIAS);
        vc.addProperty(displayAllMetadataModel);

        final Property displayAllMetadataProcessControlParamsModel = Property.create(PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_KEY, PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_TYPE, true, true);
        displayAllMetadataProcessControlParamsModel.getDescriptor().setAlias(PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_ALIAS);
        vc.addProperty(displayAllMetadataProcessControlParamsModel);

        final Property displayAllBandMetadataModel = Property.create(PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_KEY, PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_TYPE, true, true);
        displayAllBandMetadataModel.getDescriptor().setAlias(PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_ALIAS);
        vc.addProperty(displayAllBandMetadataModel);

        final Property convertCaretHeaderModel = Property.create(PROPERTY_HEADER_CONVERT_CARET_KEY, PROPERTY_HEADER_CONVERT_CARET_TYPE, true, true);
        convertCaretHeaderModel.getDescriptor().setAlias(PROPERTY_HEADER_CONVERT_CARET_ALIAS);
        vc.addProperty(convertCaretHeaderModel);

        final Property convertCaretFooterModel = Property.create(PROPERTY_FOOTER_CONVERT_CARET_KEY, PROPERTY_FOOTER_CONVERT_CARET_TYPE, true, true);
        convertCaretFooterModel.getDescriptor().setAlias(PROPERTY_FOOTER_CONVERT_CARET_ALIAS);
        vc.addProperty(convertCaretFooterModel);

        final Property convertCaretMarginModel = Property.create(PROPERTY_MARGIN_CONVERT_CARET_KEY, PROPERTY_MARGIN_CONVERT_CARET_TYPE, true, true);
        convertCaretMarginModel.getDescriptor().setAlias(PROPERTY_MARGIN_CONVERT_CARET_ALIAS);
        vc.addProperty(convertCaretMarginModel);

        final Property footerModel = Property.create(PROPERTY_MARGIN_TEXTFIELD_KEY, PROPERTY_MARGIN_TEXTFIELD_TYPE, true, true);
        footerModel.getDescriptor().setAlias(PROPERTY_MARGIN_TEXTFIELD_ALIAS);
        vc.addProperty(footerModel);

        final Property footer2Model = Property.create(PROPERTY_MARGIN_TEXTFIELD2_KEY, PROPERTY_MARGIN_TEXTFIELD2_TYPE, true, true);
        footer2Model.getDescriptor().setAlias(PROPERTY_MARGIN_TEXTFIELD2_ALIAS);
        vc.addProperty(footer2Model);

        final Property footerMetadataModel = Property.create(PROPERTY_MARGIN_METADATA_KEY, PROPERTY_MARGIN_METADATA_TYPE, true, true);
        footerMetadataModel.getDescriptor().setAlias(PROPERTY_MARGIN_METADATA_ALIAS);
        vc.addProperty(footerMetadataModel);

        final Property footerMetadata2Model = Property.create(PROPERTY_MARGIN_METADATA2_KEY, PROPERTY_MARGIN_METADATA2_TYPE, true, true);
        footerMetadata2Model.getDescriptor().setAlias(PROPERTY_MARGIN_METADATA2_ALIAS);
        vc.addProperty(footerMetadata2Model);

        final Property footerMetadata3Model = Property.create(PROPERTY_MARGIN_METADATA3_KEY, PROPERTY_MARGIN_METADATA3_TYPE, true, true);
        footerMetadata3Model.getDescriptor().setAlias(PROPERTY_MARGIN_METADATA3_ALIAS);
        vc.addProperty(footerMetadata3Model);



        final Property marginPropertyHeadingModel = Property.create(PROPERTY_MARGIN_PROPERTY_HEADING_KEY, PROPERTY_MARGIN_PROPERTY_HEADING_TYPE, true, true);
        marginPropertyHeadingModel.getDescriptor().setAlias(PROPERTY_MARGIN_PROPERTY_HEADING_ALIAS);
        vc.addProperty(marginPropertyHeadingModel);

        final Property marginGlobalHeadingModel = Property.create(PROPERTY_MARGIN_GLOBAL_HEADING_KEY, PROPERTY_MARGIN_GLOBAL_HEADING_TYPE, true, true);
        marginGlobalHeadingModel.getDescriptor().setAlias(PROPERTY_MARGIN_GLOBAL_HEADING_ALIAS);
        vc.addProperty(marginGlobalHeadingModel);

        final Property marginBandHeadingModel = Property.create(PROPERTY_MARGIN_BAND_HEADING_KEY, PROPERTY_MARGIN_BAND_HEADING_TYPE, true, true);
        marginBandHeadingModel.getDescriptor().setAlias(PROPERTY_MARGIN_BAND_HEADING_ALIAS);
        vc.addProperty(marginBandHeadingModel);



        final Property footerMetadata4Model = Property.create(PROPERTY_MARGIN_METADATA4_KEY, PROPERTY_MARGIN_METADATA4_TYPE, true, true);
        footerMetadata4Model.getDescriptor().setAlias(PROPERTY_MARGIN_METADATA4_ALIAS);
        vc.addProperty(footerMetadata4Model);

        final Property footerMetadata5Model = Property.create(PROPERTY_MARGIN_METADATA5_KEY, PROPERTY_MARGIN_METADATA5_TYPE, true, true);
        footerMetadata5Model.getDescriptor().setAlias(PROPERTY_MARGIN_METADATA5_ALIAS);
        vc.addProperty(footerMetadata5Model);

        final Property footerMetadataKeysShowModel = Property.create(PROPERTY_MARGIN_METADATA_KEYS_SHOW_KEY, PROPERTY_MARGIN_METADATA_KEYS_SHOW_TYPE, true, true);
        footerMetadataKeysShowModel.getDescriptor().setAlias(PROPERTY_MARGIN_METADATA_KEYS_SHOW_ALIAS);
        vc.addProperty(footerMetadataKeysShowModel);

        final Property footerInfoKeysShowAllModel = Property.create(PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_KEY, PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_TYPE, true, true);
        footerInfoKeysShowAllModel.getDescriptor().setAlias(PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_ALIAS);
        vc.addProperty(footerInfoKeysShowAllModel);


        final Property footerMetadataDelimiterModel = Property.create(PROPERTY_MARGIN_METADATA_DELIMITER_KEY, PROPERTY_MARGIN_METADATA_DELIMITER_TYPE, true, true);
        footerMetadataDelimiterModel.getDescriptor().setAlias(PROPERTY_MARGIN_METADATA_DELIMITER_ALIAS);
        vc.addProperty(footerMetadataDelimiterModel);


        // Footer2 Contents Section

        final Property footer2ParametersSectionModel = Property.create(PROPERTY_FOOTER2_SECTION_KEY, Boolean.class, true, true);
        footer2ParametersSectionModel.getDescriptor().setAlias(PROPERTY_FOOTER2_SECTION_ALIAS);
        vc.addProperty(footer2ParametersSectionModel);

        final Property footer2ShowModel = Property.create(PROPERTY_FOOTER2_SHOW_KEY, PROPERTY_FOOTER2_SHOW_TYPE, true, true);
        footer2ShowModel.getDescriptor().setAlias(PROPERTY_FOOTER2_SHOW_ALIAS);
        vc.addProperty(footer2ShowModel);

        final Property footer2MyInfoShowModel = Property.create(PROPERTY_FOOTER2_MY_INFO_SHOW_KEY, PROPERTY_FOOTER2_MY_INFO_SHOW_TYPE, true, true);
        footer2MyInfoShowModel.getDescriptor().setAlias(PROPERTY_FOOTER2_MY_INFO_SHOW_ALIAS);
        vc.addProperty(footer2MyInfoShowModel);



        final Property footer2TextfieldModel = Property.create(PROPERTY_FOOTER2_TEXTFIELD_KEY, PROPERTY_FOOTER2_TEXTFIELD_TYPE, true, true);
        footer2TextfieldModel.getDescriptor().setAlias(PROPERTY_FOOTER2_TEXTFIELD_ALIAS);
        vc.addProperty(footer2TextfieldModel);

        final Property footer2Textfield2Model = Property.create(PROPERTY_FOOTER2_TEXTFIELD2_KEY, PROPERTY_FOOTER2_TEXTFIELD2_TYPE, true, true);
        footer2Textfield2Model.getDescriptor().setAlias(PROPERTY_FOOTER2_TEXTFIELD2_ALIAS);
        vc.addProperty(footer2Textfield2Model);

        final Property footer2Textfield3Model = Property.create(PROPERTY_FOOTER2_TEXTFIELD3_KEY, PROPERTY_FOOTER2_TEXTFIELD3_TYPE, true, true);
        footer2Textfield3Model.getDescriptor().setAlias(PROPERTY_FOOTER2_TEXTFIELD3_ALIAS);
        vc.addProperty(footer2Textfield3Model);

        final Property footer2Textfield4Model = Property.create(PROPERTY_FOOTER2_TEXTFIELD4_KEY, PROPERTY_FOOTER2_TEXTFIELD4_TYPE, true, true);
        footer2Textfield4Model.getDescriptor().setAlias(PROPERTY_FOOTER2_TEXTFIELD4_ALIAS);
        vc.addProperty(footer2Textfield4Model);


        // Header Formatting Section

        final Property headerFormatSectionModel = Property.create(PROPERTY_HEADER_FORMAT_SECTION_KEY, Boolean.class, true, true);
        headerFormatSectionModel.getDescriptor().setAlias(PROPERTY_HEADER_FORMAT_SECTION_ALIAS);
        vc.addProperty(headerFormatSectionModel);

        final Property headerLocationSectionModel = Property.create(PROPERTY_HEADER_LOCATION_SECTION_KEY, Boolean.class, true, true);
        headerLocationSectionModel.getDescriptor().setAlias(PROPERTY_HEADER_LOCATION_SECTION_ALIAS);
        vc.addProperty(headerLocationSectionModel);

        final Property locationModel = Property.create(PROPERTY_HEADER_LOCATION_KEY, PROPERTY_HEADER_LOCATION_TYPE, true, true);
        locationModel.getDescriptor().setAlias(PROPERTY_HEADER_LOCATION_ALIAS);
        vc.addProperty(locationModel);

        final Property headerGapFactorModel = Property.create(PROPERTY_HEADER_GAP_KEY, PROPERTY_HEADER_GAP_TYPE, true, true);
        headerGapFactorModel.getDescriptor().setAlias(PROPERTY_HEADER_GAP_ALIAS);
        vc.addProperty(headerGapFactorModel);

        final Property textFontSizeModel = Property.create(PROPERTY_HEADER_FONT_SIZE_KEY, Integer.class, PROPERTY_HEADER_FONT_SIZE_DEFAULT, true);
        textFontSizeModel.getDescriptor().setAlias(PROPERTY_HEADER_FONT_SIZE_ALIAS);
        vc.addProperty(textFontSizeModel);

        final Property textFgColorModel = Property.create(PROPERTY_HEADER_FONT_COLOR_KEY, Color.class, PROPERTY_HEADER_FONT_COLOR_DEFAULT, true);
        textFgColorModel.getDescriptor().setAlias(PROPERTY_HEADER_FONT_COLOR_ALIAS);
        vc.addProperty(textFgColorModel);

        final Property textFontModel = Property.create(PROPERTY_HEADER_FONT_STYLE_KEY, String.class, PROPERTY_HEADER_FONT_STYLE_DEFAULT, true);
        textFontModel.getDescriptor().setAlias(PROPERTY_HEADER_FONT_STYLE_ALIAS);
        vc.addProperty(textFontModel);

        final Property textFontItalicModel = Property.create(PROPERTY_HEADER_FONT_ITALIC_KEY, Boolean.class, PROPERTY_HEADER_FONT_ITALIC_DEFAULT, true);
        textFontItalicModel.getDescriptor().setAlias(PROPERTY_HEADER_FONT_ITALIC_ALIAS);
        vc.addProperty(textFontItalicModel);

        final Property textFontBoldModel = Property.create(PROPERTY_HEADER_FONT_BOLD_KEY, Boolean.class, PROPERTY_HEADER_FONT_BOLD_DEFAULT, true);
        textFontBoldModel.getDescriptor().setAlias(PROPERTY_HEADER_FONT_BOLD_ALIAS);
        vc.addProperty(textFontBoldModel);




        // Footer Formatting Section

        final Property locationSectionModel = Property.create(PROPERTY_MARGIN_FORMATTING_SECTION_KEY, Boolean.class, true, true);
        locationSectionModel.getDescriptor().setAlias(PROPERTY_MARGIN_FORMATTING_SECTION_ALIAS);
        vc.addProperty(locationSectionModel);

        final Property footerLocationModel = Property.create(PROPERTY_MARGIN_LOCATION_KEY, PROPERTY_MARGIN_LOCATION_TYPE, true, true);
        footerLocationModel.getDescriptor().setAlias(PROPERTY_MARGIN_LOCATION_ALIAS);
        vc.addProperty(footerLocationModel);

        final Property footerGapFactorModel = Property.create(PROPERTY_MARGIN_GAP_KEY, PROPERTY_MARGIN_GAP_TYPE, true, true);
        footerGapFactorModel.getDescriptor().setAlias(PROPERTY_MARGIN_GAP_ALIAS);
        vc.addProperty(footerGapFactorModel);

        final Property footerFontSizeModel = Property.create(PROPERTY_MARGIN_FONT_SIZE_KEY, Integer.class, PROPERTY_MARGIN_FONT_SIZE_DEFAULT, true);
        footerFontSizeModel.getDescriptor().setAlias(PROPERTY_MARGIN_FONT_SIZE_ALIAS);
        vc.addProperty(footerFontSizeModel);

        final Property footerFontColorModel = Property.create(PROPERTY_MARGIN_FONT_COLOR_KEY, Color.class, PROPERTY_MARGIN_FONT_COLOR_DEFAULT, true);
        footerFontColorModel.getDescriptor().setAlias(PROPERTY_MARGIN_FONT_COLOR_ALIAS);
        vc.addProperty(footerFontColorModel);

        final Property footerFontStyleModel = Property.create(PROPERTY_MARGIN_FONT_STYLE_KEY, String.class, PROPERTY_MARGIN_FONT_STYLE_DEFAULT, true);
        footerFontStyleModel.getDescriptor().setAlias(PROPERTY_MARGIN_FONT_STYLE_ALIAS);
        vc.addProperty(footerFontStyleModel);

        final Property footerFontItalicModel = Property.create(PROPERTY_MARGIN_FONT_ITALIC_KEY, Boolean.class, PROPERTY_MARGIN_FONT_ITALIC_DEFAULT, true);
        footerFontItalicModel.getDescriptor().setAlias(PROPERTY_MARGIN_FONT_ITALIC_ALIAS);
        vc.addProperty(footerFontItalicModel);

        final Property footerFontBoldModel = Property.create(PROPERTY_MARGIN_FONT_BOLD_KEY, Boolean.class, PROPERTY_MARGIN_FONT_BOLD_DEFAULT, true);
        footerFontBoldModel.getDescriptor().setAlias(PROPERTY_MARGIN_FONT_BOLD_ALIAS);
        vc.addProperty(footerFontBoldModel);


        final Property footer2GapFactorModel = Property.create(PROPERTY_FOOTER2_GAP_KEY, PROPERTY_FOOTER2_GAP_TYPE, true, true);
        footer2GapFactorModel.getDescriptor().setAlias(PROPERTY_FOOTER2_GAP_ALIAS);
        vc.addProperty(footer2GapFactorModel);

        final Property footer2FontSizeModel = Property.create(PROPERTY_FOOTER2_FONT_SIZE_KEY, Integer.class, PROPERTY_FOOTER2_FONT_SIZE_DEFAULT, true);
        footer2FontSizeModel.getDescriptor().setAlias(PROPERTY_FOOTER2_FONT_SIZE_ALIAS);
        vc.addProperty(footer2FontSizeModel);

        final Property footer2FontColorModel = Property.create(PROPERTY_FOOTER2_FONT_COLOR_KEY, Color.class, PROPERTY_FOOTER2_FONT_COLOR_DEFAULT, true);
        footer2FontColorModel.getDescriptor().setAlias(PROPERTY_FOOTER2_FONT_COLOR_ALIAS);
        vc.addProperty(footer2FontColorModel);

        final Property footer2FontStyleModel = Property.create(PROPERTY_FOOTER2_FONT_STYLE_KEY, String.class, PROPERTY_FOOTER2_FONT_STYLE_DEFAULT, true);
        footer2FontStyleModel.getDescriptor().setAlias(PROPERTY_FOOTER2_FONT_STYLE_ALIAS);
        vc.addProperty(footer2FontStyleModel);

        final Property footer2FontItalicModel = Property.create(PROPERTY_FOOTER2_FONT_ITALIC_KEY, Boolean.class, PROPERTY_FOOTER2_FONT_ITALIC_DEFAULT, true);
        footer2FontItalicModel.getDescriptor().setAlias(PROPERTY_FOOTER2_FONT_ITALIC_ALIAS);
        vc.addProperty(footer2FontItalicModel);

        final Property footer2FontBoldModel = Property.create(PROPERTY_FOOTER2_FONT_BOLD_KEY, Boolean.class, PROPERTY_FOOTER2_FONT_BOLD_DEFAULT, true);
        footer2FontBoldModel.getDescriptor().setAlias(PROPERTY_FOOTER2_FONT_BOLD_ALIAS);
        vc.addProperty(footer2FontBoldModel);




        final Property myInfoSectionModel = Property.create(PROPERTY_MY_INFO_SECTION_KEY, Boolean.class, true, true);
        myInfoSectionModel.getDescriptor().setAlias(PROPERTY_MY_INFO_SECTION_ALIAS);
        vc.addProperty(myInfoSectionModel);

        final Property myInfo1Model = Property.create(PROPERTY_MY_INFO_TEXTFIELD1_KEY, String.class, PROPERTY_MY_INFO_TEXTFIELD1_DEFAULT, true);
        myInfo1Model.getDescriptor().setAlias(PROPERTY_MY_INFO_TEXTFIELD1_ALIAS);
        vc.addProperty(myInfo1Model);

        final Property myInfo2Model = Property.create(PROPERTY_MY_INFO_TEXTFIELD2_KEY, String.class, PROPERTY_MY_INFO_TEXTFIELD2_DEFAULT, true);
        myInfo2Model.getDescriptor().setAlias(PROPERTY_MY_INFO_TEXTFIELD2_ALIAS);
        vc.addProperty(myInfo2Model);

        final Property myInfo3Model = Property.create(PROPERTY_MY_INFO_TEXTFIELD3_KEY, String.class, PROPERTY_MY_INFO_TEXTFIELD3_DEFAULT, true);
        myInfo3Model.getDescriptor().setAlias(PROPERTY_MY_INFO_TEXTFIELD3_ALIAS);
        vc.addProperty(myInfo3Model);

        final Property myInfo4Model = Property.create(PROPERTY_MY_INFO_TEXTFIELD4_KEY, String.class, PROPERTY_MY_INFO_TEXTFIELD4_DEFAULT, true);
        myInfo4Model.getDescriptor().setAlias(PROPERTY_MY_INFO_TEXTFIELD4_ALIAS);
        vc.addProperty(myInfo4Model);



        // Footer2 Formatting Section

        final Property footer2LocationSectionModel = Property.create(PROPERTY_FOOTER2_FORMATTING_SECTION_KEY, Boolean.class, true, true);
        footer2LocationSectionModel.getDescriptor().setAlias(PROPERTY_FOOTER2_FORMATTING_SECTION_ALIAS);
        vc.addProperty(footer2LocationSectionModel);

        final Property footer2LocationModel = Property.create(PROPERTY_FOOTER2_LOCATION_KEY, PROPERTY_FOOTER2_LOCATION_TYPE, true, true);
        footer2LocationModel.getDescriptor().setAlias(PROPERTY_FOOTER2_LOCATION_ALIAS);
        vc.addProperty(footer2LocationModel);




        final Property rasterModel = Property.create(PROPERTY_NAME_RASTER, RasterDataNode.class);
        rasterModel.getDescriptor().setNotNull(true);
        vc.addProperty(rasterModel);

        final Property transformModel = Property.create(PROPERTY_NAME_TRANSFORM, new AffineTransform());
        transformModel.getDescriptor().setTransient(true);
        vc.addProperty(transformModel);





        return vc;
    }
}
