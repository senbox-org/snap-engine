package org.esa.snap.core.layer;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Daniel Knowles
 */

public class MetaDataLayer extends Layer {

    private static final MetaDataLayerType LAYER_TYPE = LayerTypeRegistry.getLayerType(MetaDataLayerType.class);

    private RasterDataNode raster;

    private ProductNodeHandler productNodeHandler;
    private MetaDataOnImage headerFooter;

    private double NULL_DOUBLE = -1.0;
    private double ptsToPixelsMultiplier = NULL_DOUBLE;


    private final String INFO_PARAM_FILE = "FILE";
    private final String INFO_PARAM_PROCESSING_VERSION = "PROCESSING_VERSION";
    private final String INFO_PARAM_SENSOR = "SENSOR";
    private final String INFO_PARAM_PLATFORM = "PLATFORM";
    private final String INFO_PARAM_PROJECTION = "PROJECTION";
    private final String INFO_PARAM_RESOLUTION = "RESOLUTION";
    private final String INFO_PARAM_DAY_NIGHT = "DAY_NIGHT";
    private final String INFO_PARAM_ORBIT = "ORBIT";
    private final String INFO_PARAM_START_ORBIT = "START_ORBIT";
    private final String INFO_PARAM_END_ORBIT = "END_ORBIT";
    private final String INFO_PARAM_BAND = "BAND";
    private final String INFO_PARAM_UNIT = "UNIT";
    private final String INFO_PARAM_BAND_DESCRIPTION = "BAND_DESCRIPTION";
    private final String INFO_PARAM_FILE_LOCATION = "FILE_LOCATION";
    private final String INFO_PARAM_PRODUCT_TYPE = "PRODUCT_TYPE";
    private final String INFO_PARAM_SCENE_START_TIME = "SCENE_START_TIME";
    private final String INFO_PARAM_SCENE_END_TIME = "SCENE_END_TIME";
    private final String INFO_PARAM_SCENE_HEIGHT = "SCENE_HEIGHT";
    private final String INFO_PARAM_SCENE_WIDTH = "SCENE_WIDTH";
    private final String INFO_PARAM_SCENE_SIZE = "SCENE_SIZE";


    private final String INFO_PARAM_WAVE = "WAVE";
    private final String INFO_PARAM_ANGLE = "ANGLE";
    private final String INFO_PARAM_FLAG_CODING = "FLAG_CODING";
    private final String INFO_PARAM_VALID_PIXEL_EXPRESSION = "VALID_PIXEL_EXPRESSION";
    private final String INFO_PARAM_NO_DATA_VALUE = "NO_DATA_VALUE";
    private final String INFO_PARAM_IS_NO_DATA_VALUE_SET = "IS_NO_DATA_VALUE_SET";
    private final String INFO_PARAM_IS_NO_DATA_VALUE_USED = "IS_NO_DATA_VALUE_USED";
    private final String INFO_PARAM_IS_SCALING_APPLIED = "IS_SCALING_APPLIED";
    private final String INFO_PARAM_SCALING_FACTOR = "SCALING_FACTOR";
    private final String INFO_PARAM_SCALING_OFFSET = "SCALING_OFFSET";
    private final String INFO_PARAM_IS_LOG_SCALED = "IS_LOG_SCALED";
    private final String INFO_PARAM_IS_PALETTE_LOG_SCALED = "IS_PALETTE_LOG_SCALED";
    private final String INFO_PARAM_NODE_DISPLAY_NAMES = "NODE_DISPLAY_NAMES";
    private final String INFO_PARAM_NODE_NAMES = "NODE_NAMES";


    boolean showNullKeys = true;


    private String[] INFO_PARAMS = {
            INFO_PARAM_FILE,
//            INFO_PARAM_PROCESSING_VERSION,
//            INFO_PARAM_SENSOR,
//            INFO_PARAM_PLATFORM,
//            INFO_PARAM_PROJECTION,
//            INFO_PARAM_RESOLUTION,
//            INFO_PARAM_DAY_NIGHT,
//            INFO_PARAM_ORBIT,
//            INFO_PARAM_START_ORBIT,
//            INFO_PARAM_END_ORBIT,
            INFO_PARAM_BAND,
            INFO_PARAM_UNIT,
            INFO_PARAM_BAND_DESCRIPTION,
            INFO_PARAM_FILE_LOCATION,
            INFO_PARAM_PRODUCT_TYPE,
            INFO_PARAM_SCENE_START_TIME,
            INFO_PARAM_SCENE_END_TIME,
            INFO_PARAM_SCENE_HEIGHT,
            INFO_PARAM_SCENE_WIDTH,
            INFO_PARAM_SCENE_SIZE,

            INFO_PARAM_WAVE,
            INFO_PARAM_ANGLE,
            INFO_PARAM_FLAG_CODING,
            INFO_PARAM_VALID_PIXEL_EXPRESSION,
            INFO_PARAM_NO_DATA_VALUE,
            INFO_PARAM_IS_NO_DATA_VALUE_SET,
            INFO_PARAM_IS_NO_DATA_VALUE_USED,
            INFO_PARAM_IS_SCALING_APPLIED,
            INFO_PARAM_SCALING_FACTOR,
            INFO_PARAM_SCALING_OFFSET,
            INFO_PARAM_IS_LOG_SCALED,
            INFO_PARAM_IS_PALETTE_LOG_SCALED,
            INFO_PARAM_NODE_DISPLAY_NAMES,
            INFO_PARAM_NODE_NAMES
    };


//    public static enum InfoFields {
//        FILE("FILE"),
//        BAND("BAND");
//
//        final  String fieldName;
//
//        InfoFields(String fieldName) {
//            this.fieldName = fieldName;
//        }
//
//        public String getFieldName() {
//            return fieldName;
//        }
//    }

    public MetaDataLayer(RasterDataNode raster) {
        this(LAYER_TYPE, raster, initConfiguration(LAYER_TYPE.createLayerConfig(null), raster));
    }

    public MetaDataLayer(MetaDataLayerType type, RasterDataNode raster, PropertySet configuration) {
        super(type, configuration);
        setName("Annotation Metadata Layer");
        this.raster = raster;

        productNodeHandler = new ProductNodeHandler();
        raster.getProduct().addProductNodeListener(productNodeHandler);

        setTransparency(0.0);
    }

    private static PropertySet initConfiguration(PropertySet configurationTemplate, RasterDataNode raster) {
        configurationTemplate.setValue(MetaDataLayerType.PROPERTY_NAME_RASTER, raster);
        return configurationTemplate;
    }

    private Product getProduct() {
        return getRaster().getProduct();
    }

    RasterDataNode getRaster() {
        return raster;
    }

    @Override
    public void renderLayer(Rendering rendering) {

        getUserValues();

        if (headerFooter == null) {
            final List<String> headerList = new ArrayList<String>();
            final List<String> marginList = new ArrayList<String>();
            final List<String> footer2List = new ArrayList<String>();


            for (String curr : getHeaderFooterLinesArray(getHeader())) {
                headerList.add(curr);
            }

            for (String curr : getHeaderFooterLinesArray(getHeader2())) {
                headerList.add(curr);
            }

            for (String curr : getHeaderFooterLinesArray(getHeader3())) {
                headerList.add(curr);
            }

            for (String curr : getHeaderFooterLinesArray(getHeader4())) {
                headerList.add(curr);
            }

//            ArrayList<String> headerMetadataCombinedArrayList = new ArrayList<String>();
//
//            for (String curr : getMetadataArrayList(getHeader4())) {
//                headerMetadataCombinedArrayList.add(curr);
//            }
//            addFromMetadataList(headerMetadataCombinedArrayList, headerList, true, true);


            for (String curr : getHeaderFooterLinesArray(getMarginTextfield1())) {
                marginList.add(curr);
            }

            for (String curr : getHeaderFooterLinesArray(getMarginTextfield2())) {
                marginList.add(curr);
            }


            ArrayList<String> marginMetadataCombinedArrayList = new ArrayList<String>();
            ArrayList<String> marginBandMetadataCombinedArrayList = new ArrayList<String>();
            ArrayList<String> marginInfoCombinedArrayList = new ArrayList<String>();

            for (String curr : getMetadataArrayList(getMarginMetadata1())) {
                marginInfoCombinedArrayList.add(curr);
            }

            for (String curr : getMetadataArrayList(getMarginMetadata2())) {
                marginInfoCombinedArrayList.add(curr);
            }


            for (String curr : getMetadataArrayList(getMarginMetadata3())) {
                for (String key : getAllPossibleRelatedKeys(curr)) {
                    if (ProductUtils.isMetadataKeyExists(raster.getProduct(), key)) {
                        marginMetadataCombinedArrayList.add(key);
                    }
                }
            }

            for (String curr : getMetadataArrayList(getMarginMetadata4())) {
                for (String key : getAllPossibleRelatedKeys(curr)) {
                    if (ProductUtils.isMetadataKeyExists(raster.getProduct(), key)) {
                        marginMetadataCombinedArrayList.add(key);
                    }
                }
            }

            for (String curr : getMetadataArrayList(getMarginMetadata5())) {
                marginBandMetadataCombinedArrayList.add(curr);
            }

            if (displayAllInfo()) {
                marginInfoCombinedArrayList.clear();
                for (String infoField : INFO_PARAMS) {
                    marginInfoCombinedArrayList.add(infoField.toLowerCase());
                }
            }


            if (displayAllMetadata() || displayAllMetadataProcessControlParams()) {
                try {
                    String[] allAttributes = getProduct().getMetadataRoot().getElement("Global_Attributes").getAttributeNames();
                    marginMetadataCombinedArrayList.clear();
                    for (String curr : allAttributes) {
                        if (curr != null) {
                            if (displayAllMetadata() && displayAllMetadataProcessControlParams()) {
                                marginMetadataCombinedArrayList.add(curr);
                            } else if (displayAllMetadata() && !displayAllMetadataProcessControlParams()) {
                                if (curr.startsWith("processing_control")) {
                                    if (curr.equals("processing_control_software_name") ||
                                            curr.equals("processing_control_software_version") ||
                                            curr.equals("processing_control_mask_names")
                                    ) {
                                        marginMetadataCombinedArrayList.add(curr);
                                    }
                                } else {
                                    marginMetadataCombinedArrayList.add(curr);
                                }
                            } else if (!displayAllMetadata() && displayAllMetadataProcessControlParams()) {
                                if (curr.startsWith("processing_control")) {
                                    marginMetadataCombinedArrayList.add(curr);
                                }
                            }
                        }
                    }
                } catch (Exception ignore) {
                }
            }

            if (displayAllBandMetadata()) {
                try {
                    String[] allAttributes = getProduct().getMetadataRoot().getElement("Band_Attributes").getElement(raster.getName()).getAttributeNames();
                    marginBandMetadataCombinedArrayList.clear();
                    for (String curr : allAttributes) {
                        marginBandMetadataCombinedArrayList.add(curr);
                    }
                } catch (Exception ignore) {
                }

            }

            if (marginList.size() > 0) {
                marginList.add("");
            }
            if (marginInfoCombinedArrayList.size() > 0) {
                marginList.add("File-Band Info:");
                addFromMetadataList(marginInfoCombinedArrayList, marginList, false, false);
                marginList.add("");
            }

            if (marginMetadataCombinedArrayList.size() > 0) {
                marginList.add("File Metadata: (Global_Attributes)");
                addFromMetadataList(marginMetadataCombinedArrayList, marginList, true, true);
                marginList.add("");
            }

            if (marginBandMetadataCombinedArrayList.size() > 0) {
                marginList.add("Band Metadata '" + raster.getName() + "' (Band_Attributes):");
                addFromMetadataList(marginBandMetadataCombinedArrayList, marginList, true, false);
                marginList.add("");
            }


            for (String curr : getHeaderFooterLinesArray(getFooter2Textfield())) {
                footer2List.add(curr);
            }

            for (String curr : getHeaderFooterLinesArray(getFooter2Textfield2())) {
                footer2List.add(curr);
            }

            for (String curr : getHeaderFooterLinesArray(getFooter2Textfield3())) {
                footer2List.add(curr);
            }

            for (String curr : getHeaderFooterLinesArray(getFooter2Textfield4())) {
                footer2List.add(curr);
            }

            if (getFooter2MyInfoShow()) {
                for (String curr : getHeaderFooterLinesArray(getMyInfo1())) {
                    footer2List.add(curr);
                }
                for (String curr : getHeaderFooterLinesArray(getMyInfo2())) {
                    footer2List.add(curr);
                }
                for (String curr : getHeaderFooterLinesArray(getMyInfo3())) {
                    footer2List.add(curr);
                }
                for (String curr : getHeaderFooterLinesArray(getMyInfo4())) {
                    footer2List.add(curr);
                }
            }


            headerFooter = MetaDataOnImage.create(raster, headerList, marginList, footer2List);
        }
        if (headerFooter != null) {

            final Graphics2D g2d = rendering.getGraphics();
            // added this to improve text
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            final Viewport vp = rendering.getViewport();
            final AffineTransform transformSave = g2d.getTransform();
            try {
                final AffineTransform transform = new AffineTransform();
                transform.concatenate(transformSave);
                transform.concatenate(vp.getModelToViewTransform());
                transform.concatenate(raster.getSourceImage().getModel().getImageToModelTransform(0));
                g2d.setTransform(transform);

                final MetaDataOnImage.TextGlyph[] textGlyphHeader = headerFooter.getTextGlyphsHeader();
                final MetaDataOnImage.TextGlyph[] textGlyphsFooter = headerFooter.get_textGlyphsFooter();
                final MetaDataOnImage.TextGlyph[] textGlyphsFooter2 = headerFooter.get_textGlyphsFooter2();

                if (getHeaderShow()) {
                    drawTextHeaderFooter(g2d, textGlyphHeader, true, false, raster);
                }
                if (getMarginShow()) {
                    drawTextHeaderFooter(g2d, textGlyphsFooter, false, false, raster);
                }
                if (getFooter2Show()) {
                    drawTextHeaderFooter(g2d, textGlyphsFooter2, false, true, raster);
                }

            } finally {
                g2d.setTransform(transformSave);
            }
        }
    }

    private String[] getAllPossibleRelatedKeys(String key) {
        ArrayList<String[]> keySets = new ArrayList<String[]>();

        keySets.add(ProductUtils.METADATA_POSSIBLE_PROJECTION_KEYS);
        keySets.add(ProductUtils.METADATA_POSSIBLE_SENSOR_KEYS);
        keySets.add(ProductUtils.METADATA_POSSIBLE_PLATFORM_KEYS);
        keySets.add(ProductUtils.METADATA_POSSIBLE_PROCESSING_VERSION_KEYS);
        keySets.add(ProductUtils.METADATA_POSSIBLE_DAY_NIGHT_KEYS);
        keySets.add(ProductUtils.METADATA_POSSIBLE_ORBIT_KEYS);
        keySets.add(ProductUtils.METADATA_POSSIBLE_START_ORBIT_KEYS);
        keySets.add(ProductUtils.METADATA_POSSIBLE_END_ORBIT_KEYS);

        for (String[] keySet : keySets) {
            for (String keyInSet : keySet) {
                if (keyInSet.equals(key)) {
                    return keySet;
                }
            }
        }

        return new String[]{key};
    }

    private void addFromMetadataList(ArrayList<String> footerMetadataCombinedArrayList, List<String> footerList, boolean isMeta, boolean globalAttributes) {
        for (String currKey : footerMetadataCombinedArrayList) {
            if (currKey != null && currKey.length() > 0) {
                String currParam = null;
                if (!isMeta) {
                    int length = currKey.length();
                    if (length > 2) {
                        currParam = getDerivedMeta(currKey.toUpperCase());

                        if (getMarginMetadataKeysShow()) {
                            currParam = currKey + getMarginMetadataDelimiter() + currParam;
                        }
                    }
                } else {
                    String key = currKey;

                    if (globalAttributes) {
                        currParam = ProductUtils.getMetaData(raster.getProduct(), currKey);
                    } else {
                        currParam = ProductUtils.getBandMetaData(raster.getProduct(), currKey, raster.getName());
                    }

                    if (getMarginMetadataKeysShow()) {
                        currParam = key + getMarginMetadataDelimiter() + currParam;
                    }
                }

                if (currParam != null && currParam.trim() != null) {
                    if (currParam.length() > 0 || showNullKeys) {
                        footerList.add(currParam);
                    }
                }
            }
        }

    }

    private String replaceStringVariablesCase(String inputString, String key, String replacement) {
        if (inputString != null && inputString.length() > 0 && key != null && key.length() > 0 && replacement != null) {
            inputString = inputString.replace(key, replacement);
            inputString = inputString.replace(key.toLowerCase(), replacement);
            inputString = inputString.replace(key.toLowerCase(), replacement);
            String keyTitleCase = convertToTitleCase(key);
            inputString = inputString.replace(keyTitleCase.toLowerCase(), replacement);
        }

        return inputString;
    }


    public static String convertToTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder converted = new StringBuilder();

        boolean convertNext = true;
        for (char ch : text.toCharArray()) {
            if (Character.isSpaceChar(ch)) {
                convertNext = true;
            } else if (convertNext) {
                ch = Character.toTitleCase(ch);
                convertNext = false;
            } else {
                ch = Character.toLowerCase(ch);
            }
            converted.append(ch);
        }

        return converted.toString();
    }

    private String replaceStringVariables(String inputString, boolean showKeys, String replaceKey) {
        if (inputString != null && inputString.length() > 0) {
////            inputString = inputString.replace("[FILE]", raster.getProduct().getName());
////            inputString = inputString.replace("[File]", raster.getProduct().getName());
//            inputString = replaceStringVariablesCase(inputString, "[FILE]", raster.getProduct().getName());
//
//            inputString = inputString.replace("[BAND]", raster.getName());
//            inputString = inputString.replace("[BAND_DESCRIPTION]", raster.getDescription());
//
//            inputString = inputString.replace("[PROCESSING_VERSION]", ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_PROCESSING_VERSION_KEYS));
//            inputString = inputString.replace("[SENSOR]", ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_SENSOR_KEYS));
//            inputString = inputString.replace("[PLATFORM]", ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_PLATFORM_KEYS));
//            inputString = inputString.replace("[PROJECTION]", ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_PROJECTION_KEYS));
//            inputString = inputString.replace("[RESOLUTION]", ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_RESOLUTION_KEYS));
//
//            inputString = inputString.replace("[DAY_NIGHT]", ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_DAY_NIGHT_KEYS));
//            inputString = inputString.replace("[ORBIT]", ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_ORBIT_KEYS));
//            inputString = inputString.replace("[START_ORBIT]", ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_START_ORBIT_KEYS));
//            inputString = inputString.replace("[END_ORBIT]", ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_END_ORBIT_KEYS));
//
//
//            inputString = inputString.replace("[ID]", ProductUtils.getMetaData(raster.getProduct(), "id"));
//            inputString = inputString.replace("[L2_FLAG_NAMES]", ProductUtils.getMetaData(raster.getProduct(), "l2_flag_names"));
//
//            inputString = inputString.replace("[TITLE]", raster.getProduct().toString());
//            inputString = inputString.replace("[FILE_LOCATION]", raster.getProduct().getFileLocation().toString());
//            inputString = inputString.replace("[PRODUCT_TYPE]", raster.getProduct().getProductType());
//            inputString = inputString.replace("[SCENE_START_TIME]", raster.getProduct().getStartTime().toString());
//            inputString = inputString.replace("[SCENE_END_TIME]", raster.getProduct().getEndTime().toString());
//            inputString = inputString.replace("[SCENE_HEIGHT]", Integer.toString(raster.getRasterHeight()));
//            inputString = inputString.replace("[SCENE_WIDTH]", Integer.toString(raster.getRasterWidth()));
//            String sceneSize = "(w x h) " + raster.getRasterWidth() + " pixels x " + raster.getRasterHeight() + " pixels";
//            inputString = inputString.replace("[SCENE_SIZE]", sceneSize);
//
//            raster.getImageInfo().getColorPaletteDef().isLogScaled();
//            raster.getValidPixelExpression();
//            raster.getUnit();
//            raster.getOverlayMaskGroup().getNodeDisplayNames();
//            raster.getOverlayMaskGroup().getNodeNames();
//            raster.getProduct().getBand(raster.getName()).getSpectralWavelength();
//            raster.getProduct().getBand(raster.getName()).getAngularValue();
//            raster.getProduct().getBand(raster.getName()).getFlagCoding();
//            raster.getNoDataValue();
//            raster.isNoDataValueSet();
//            raster.isNoDataValueUsed();
//            raster.isScalingApplied();
//            raster.getScalingFactor();
//            raster.getScalingOffset();
//            raster.getProduct().getMetadataRoot().getElement("Band_Attributes").getElement(raster.getName()).getAttribute("reference").getData().getElemString();
//            raster.getProduct().getMetadataRoot().getElement("Band_Attributes").getElement(raster.getName()).getAttribute("valid_min").getData().getElemString();
//            raster.getProduct().getMetadataRoot().getElement("Band_Attributes").getElement(raster.getName()).getAttribute("valid_max").getData().getElemString();


            String metaId = null;
            String beforeMetaData = "";
            String afterMetaData = "";
            String metaStart = "";


            String META_START = replaceKey;


            String META_END = ">";


            switch (META_START) {
                case "<M=":
                    inputString = inputString.replace("<m=", META_START);
                    break;

                case "<G=":
                    inputString = inputString.replace("<g=", META_START);
                    break;

                case "<MG=":
                    inputString = inputString.replace("<mg=", META_START);
                    inputString = inputString.replace("<Mg=", META_START);
                    break;

                case "<META=":
                    inputString = inputString.replace("<Meta=", META_START);
                    inputString = inputString.replace("<meta=", META_START);
                    break;

                case "<FILE_META=":
                    inputString = inputString.replace("<File_Meta=", META_START);
                    inputString = inputString.replace("<file_meta=", META_START);
                    break;

                case "<B=":
                    inputString = inputString.replace("<b=", META_START);
                    break;

                case "<MB=":
                    inputString = inputString.replace("<Mb=", META_START);
                    inputString = inputString.replace("<mb=", META_START);
                    break;

                case "<BAND_META=":
                    inputString = inputString.replace("<Band_Meta=", META_START);
                    inputString = inputString.replace("<band_meta=", META_START);
                    break;

                case "<I=":
                    inputString = inputString.replace("<i=", META_START);
                    break;

                case "<INFO=":
                    inputString = inputString.replace("<Info=", META_START);
                    inputString = inputString.replace("<info=", META_START);
                    break;

                case "<BAND_INFO=":
                    inputString = inputString.replace("<Band_Info=", META_START);
                    inputString = inputString.replace("<band_info=", META_START);
                    break;

                case "<FILE_INFO=":
                    inputString = inputString.replace("<File_Info=", META_START);
                    inputString = inputString.replace("<file_info=", META_START);
                    break;
            }


            int whileCnt = 0;
            boolean hasMetaData = (inputString.contains(META_START) && inputString.contains(META_START)) ? true : false;


            while (hasMetaData && whileCnt < 10) {
                String[] arr1 = inputString.split(META_START, 2);

                if (arr1 != null) {
                    if (arr1.length == 1) {
                        beforeMetaData = arr1[0];
                        metaStart = "";
                    } else if (arr1.length == 2) {
                        beforeMetaData = arr1[0];
                        metaStart = arr1[1];
                    }
                } else {
                    beforeMetaData = "";
                    metaStart = "";
                }

                if (metaStart != null && metaStart.length() > 0) {
                    String[] arr2 = metaStart.split(META_END, 2);

                    if (arr2 != null && arr2.length == 2) {
                        metaId = arr2[0];
                        afterMetaData = arr2[1];
                    }
                }

                if (metaId != null && metaId.length() > 0) {
                    String value = "";


                    switch (META_START) {
                        case "<META=":
                            value = getFileMetaWithPossibleVariantKeys(metaId);
                            break;

                        case "<M=":
                            value = getFileMetaWithPossibleVariantKeys(metaId);
                            break;

                        case "<G=":
                            value = getFileMetaWithPossibleVariantKeys(metaId);
                            break;

                        case "<MG=":
                            value = getFileMetaWithPossibleVariantKeys(metaId);
                            break;

                        case "<FILE_META=":
                            value = getFileMetaWithPossibleVariantKeys(metaId);
                            break;


                        case "<B=":
                            value = ProductUtils.getBandMetaData(raster.getProduct(), metaId, raster.getName());
                            break;

                        case "<MB=":
                            value = ProductUtils.getBandMetaData(raster.getProduct(), metaId, raster.getName());
                            break;

                        case "<BAND_META=":
                            value = ProductUtils.getBandMetaData(raster.getProduct(), metaId, raster.getName());
                            break;

                        case "<I=":
                            value = getDerivedMeta(metaId.toUpperCase());
                            break;

                        case "<INFO=":
                            value = getDerivedMeta(metaId.toUpperCase());
                            break;

                        case "<FILE_INFO=":
                            value = getDerivedMeta(metaId.toUpperCase());
                            break;

                        case "<BAND_INFO=":
                            value = getDerivedMeta(metaId.toUpperCase());
                            break;


                    }

                    if (showKeys) {
                        inputString = beforeMetaData + metaId + getMarginMetadataDelimiter() + value + afterMetaData;
                    } else {
                        inputString = beforeMetaData + value + afterMetaData;
                    }
                }

                hasMetaData = (inputString.contains(META_START) && inputString.contains(META_END)) ? true : false;

                whileCnt++;
            }

        }

        return inputString;
    }


    private String getFileMetaWithPossibleVariantKeys(String metaId) {
        String value = ProductUtils.getMetaData(raster.getProduct(), metaId);
        if (value == null || value.length() == 0) {
            for (String keyInSet : getAllPossibleRelatedKeys(metaId)) {
                value = ProductUtils.getMetaData(raster.getProduct(), keyInSet);
                if (value != null && value.length() > 0) {
                    metaId = keyInSet;
                    break;
                }
            }
        }

        return value;
    }

    private String getDerivedMeta(String inputString) {
        String value = "";


        if (inputString != null && inputString.length() > 0) {
            inputString = inputString.toUpperCase();

            switch (inputString) {


                case INFO_PARAM_FILE:
                    try {
                        value = raster.getProduct().getName();
                    } catch (Exception e) {
                    }
                    break;

                case INFO_PARAM_FILE_LOCATION:
                    try {
                        value = raster.getProduct().getFileLocation().toString();
                    } catch (Exception e) {
                    }
                    break;

                case INFO_PARAM_PRODUCT_TYPE:
                    try {
                        value = raster.getProduct().getProductType();
                    } catch (Exception e) {
                    }
                    break;

                case INFO_PARAM_SCENE_START_TIME:
                    try {
                        value = raster.getProduct().getStartTime().toString();
                    } catch (Exception e) {
                    }
                    break;

                case INFO_PARAM_SCENE_END_TIME:
                    try {
                        value = raster.getProduct().getEndTime().toString();
                    } catch (Exception e) {
                    }
                    break;

//                case INFO_PARAM_PROCESSING_VERSION:
//                    value = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_PROCESSING_VERSION_KEYS);
//                    break;
//
//                case INFO_PARAM_SENSOR:
//                    value = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_SENSOR_KEYS);
//                    break;
//
//                case INFO_PARAM_PLATFORM:
//                    value = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_PLATFORM_KEYS);
//                    break;
//
//                case INFO_PARAM_PROJECTION:
//                    value = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_PROJECTION_KEYS);
//                    break;
//
//                case INFO_PARAM_RESOLUTION:
//                    value = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_RESOLUTION_KEYS);
//                    break;
//
//                case INFO_PARAM_DAY_NIGHT:
//                    value = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_DAY_NIGHT_KEYS);
//                    break;
//
//                case INFO_PARAM_ORBIT:
//                    value = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_ORBIT_KEYS);
//                    break;
//
//                case INFO_PARAM_START_ORBIT:
//                    value = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_START_ORBIT_KEYS);
//                    break;
//
//                case INFO_PARAM_END_ORBIT:
//                    value = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_END_ORBIT_KEYS);
//                    break;

                case INFO_PARAM_BAND:
                    value = raster.getName();
                    break;


                case INFO_PARAM_UNIT:
                    value = raster.getUnit();
                    break;

                case INFO_PARAM_BAND_DESCRIPTION:
                    value = raster.getDescription();
                    break;

                case INFO_PARAM_SCENE_HEIGHT:
                    value = Integer.toString(raster.getRasterHeight());
                    break;

                case INFO_PARAM_SCENE_WIDTH:
                    value = Integer.toString(raster.getRasterWidth());
                    break;

                case INFO_PARAM_SCENE_SIZE:
                    value = "(w x h) " + raster.getRasterWidth() + " pixels x " + raster.getRasterHeight() + " pixels";
                    break;

                case INFO_PARAM_WAVE:
                    value = String.valueOf(raster.getProduct().getBand(raster.getName()).getSpectralWavelength());
                    break;

                case INFO_PARAM_ANGLE:
                    value = String.valueOf(raster.getProduct().getBand(raster.getName()).getAngularValue());
                    break;

                case INFO_PARAM_FLAG_CODING:
                    try {
                        value = String.valueOf(raster.getProduct().getBand(raster.getName()).getFlagCoding());
                    } catch (Exception e) {
                    }
                    break;

                case INFO_PARAM_VALID_PIXEL_EXPRESSION:
                    value = raster.getValidPixelExpression();
                    break;

                case INFO_PARAM_NO_DATA_VALUE:
                    value = String.valueOf(raster.getNoDataValue());
                    break;

                case INFO_PARAM_IS_NO_DATA_VALUE_SET:
                    value = String.valueOf(raster.isNoDataValueSet());
                    break;

                case INFO_PARAM_IS_NO_DATA_VALUE_USED:
                    value = String.valueOf(raster.isNoDataValueUsed());
                    break;

                case INFO_PARAM_IS_SCALING_APPLIED:
                    value = String.valueOf(raster.isScalingApplied());
                    break;

                case INFO_PARAM_SCALING_FACTOR:
                    value = String.valueOf(raster.getScalingFactor());
                    break;

                case INFO_PARAM_SCALING_OFFSET:
                    value = String.valueOf(raster.getScalingOffset());
                    break;

                case INFO_PARAM_IS_LOG_SCALED:
                    value = String.valueOf(raster.isLog10Scaled());
                    break;

                case INFO_PARAM_IS_PALETTE_LOG_SCALED:
                    try {
                        value = String.valueOf(raster.getImageInfo().getColorPaletteDef().isLogScaled());
                    } catch (Exception e) {
                    }
                    break;

                case INFO_PARAM_NODE_DISPLAY_NAMES:
                    try {
                        value = "";
                        String[] nodeDisplayNames = raster.getOverlayMaskGroup().getNodeDisplayNames();
                        for (String nodeDisplayName : nodeDisplayNames) {
                            if (value.length() == 0) {
                                value = nodeDisplayName;
                            } else {
                                value = value + ", " + nodeDisplayName;
                            }
                        }
                    } catch (Exception e) {
                    }

                    break;

                case INFO_PARAM_NODE_NAMES:
                    try {
                        value = "";
                        String[] getNodeNames = raster.getOverlayMaskGroup().getNodeNames();
                        for (String getNodeName : getNodeNames) {
                            if (value.length() == 0) {
                                value = getNodeName;
                            } else {
                                value = value + ", " + getNodeName;
                            }
                        }
                    } catch (Exception e) {
                    }
                    break;


                case "MY_INFO":
                    value = getMyInfo();
                    break;

                case "MY_INFO1":
                    value = getMyInfo1();
                    break;

                case "MY_INFO2":
                    value = getMyInfo2();
                    break;

                case "MY_INFO3":
                    value = getMyInfo3();
                    break;

                case "MY_INFO4":
                    value = getMyInfo4();
                    break;
            }


        }


        return value;
    }

    private void getUserValues() {


    }


    private void drawTextHeaderFooter(Graphics2D g2d,
                                      final MetaDataOnImage.TextGlyph[] textGlyphs,
                                      boolean isHeader,
                                      boolean isFooter2,
                                      RasterDataNode raster) {


        Color origColor = (Color) g2d.getPaint();
        AffineTransform origTransform = g2d.getTransform();
        Font origFont = g2d.getFont();

        if (isHeader) {
            Font font = new Font(getHeaderFontStyle(), getHeaderFontType(), getHeaderFontSizePixels());
            g2d.setFont(font);
            g2d.setPaint(getHeaderFontColor());
        } else if (isFooter2) {
            Font font = new Font(getFooter2FontStyle(), getFooter2FontType(), getFooter2FontSizePixels());
            g2d.setFont(font);
            g2d.setPaint(getFooter2FontColor());
        } else {
            Font font = new Font(getMarginFontStyle(), getMarginFontType(), getMarginFontSizePixels());
            g2d.setFont(font);
            g2d.setPaint(getMarginFontColor());
        }


//
//        Rectangle2D singleLetter = g2d.getFontMetrics().getStringBounds("W", g2d);
//        double letterWidth = singleLetter.getWidth();

        double heightInformationBlock = 0.0;
        double maxWidthInformationBlock = 0.0;


        for (MetaDataOnImage.TextGlyph glyph : textGlyphs) {
            Rectangle2D labelBounds = g2d.getFontMetrics().getStringBounds(glyph.getText(), g2d);
            maxWidthInformationBlock = Math.max(labelBounds.getWidth(), maxWidthInformationBlock);
            heightInformationBlock += labelBounds.getHeight();
        }


        double yTopTranslateFirstLine;
        double yBottomTranslateFirstLine;

        double avgSideLength = (raster.getRasterWidth() + raster.getRasterHeight())/ 2.0;

        if (isHeader) {
            yTopTranslateFirstLine = -heightInformationBlock - avgSideLength * (getHeaderGapFactor() / 100);
            yBottomTranslateFirstLine = avgSideLength * (getHeaderGapFactor() / 100);
        } else if (isFooter2) {
            yTopTranslateFirstLine = -heightInformationBlock - avgSideLength * (getFooter2GapFactor() / 100);
            yBottomTranslateFirstLine = avgSideLength * (getFooter2GapFactor() / 100);
        } else {
            yTopTranslateFirstLine = -heightInformationBlock - avgSideLength * (getMarginGapFactor() / 100);
            yBottomTranslateFirstLine = avgSideLength * (getMarginGapFactor() / 100);
        }


        for (MetaDataOnImage.TextGlyph glyph : textGlyphs) {

            g2d.translate(glyph.getX(), glyph.getY());

            g2d.rotate(glyph.getAngle());

            double rotation = 90.0;
            double theta = (rotation / 180) * Math.PI;
            g2d.rotate(-1 * Math.PI + theta);

            Rectangle2D labelBounds = g2d.getFontMetrics().getStringBounds(glyph.getText(), g2d);

            String location;
            if (isHeader) {
                location = getHeaderLocation();
            } else if (isFooter2) {
                location = getFooter2Location();
            } else {
                location = getMarginLocation();
            }

            float xOffset = 0;
            float yOffset = 0;
            switch (location) {

                case MetaDataLayerType.LOCATION_TOP_LEFT:
                    xOffset = 0;
                    yOffset = 0 + (float) yTopTranslateFirstLine;
                    break;

                case MetaDataLayerType.LOCATION_TOP_CENTER_JUSTIFY_LEFT:
                    xOffset = (float) (-(maxWidthInformationBlock / 2.0) + (raster.getRasterWidth() / 2.0));
                    yOffset = 0 + (float) yTopTranslateFirstLine;
                    break;

                case MetaDataLayerType.LOCATION_TOP_CENTER:
                    xOffset = (float) (-(labelBounds.getWidth() / 2.0) + (raster.getRasterWidth() / 2.0));
                    yOffset = 0 + (float) yTopTranslateFirstLine;
                    break;

                case MetaDataLayerType.LOCATION_TOP_RIGHT:
                    xOffset = (float) (raster.getRasterWidth() - maxWidthInformationBlock);
                    yOffset = 0 + (float) yTopTranslateFirstLine;
                    break;

                case MetaDataLayerType.LOCATION_BOTTOM_LEFT:
                    xOffset = 0;
                    yOffset = (float) (raster.getRasterHeight() + labelBounds.getHeight() + yBottomTranslateFirstLine);
                    break;

                case MetaDataLayerType.LOCATION_BOTTOM_CENTER_JUSTIFY_LEFT:
                    xOffset = (float) (-(maxWidthInformationBlock / 2.0) + (raster.getRasterWidth() / 2.0));
                    yOffset = (float) (raster.getRasterHeight() + labelBounds.getHeight() + yBottomTranslateFirstLine);
                    break;

                case MetaDataLayerType.LOCATION_BOTTOM_CENTER:
                    xOffset = (float) (-(labelBounds.getWidth() / 2.0) + (raster.getRasterWidth() / 2.0));
                    yOffset = (float) (raster.getRasterHeight() + labelBounds.getHeight() + yBottomTranslateFirstLine);
                    break;

                case MetaDataLayerType.LOCATION_BOTTOM_RIGHT:
                    xOffset = (float) (raster.getRasterWidth() - maxWidthInformationBlock);
                    yOffset = (float) (raster.getRasterHeight() + labelBounds.getHeight() + yBottomTranslateFirstLine);
                    break;

                case MetaDataLayerType.LOCATION_RIGHT:
                    xOffset = (float) (raster.getRasterWidth() + avgSideLength * (getMarginGapFactor() / 100));
                    ;
                    yOffset = 0;
                    break;

                case MetaDataLayerType.LOCATION_RIGHT_CENTER:
                    xOffset = (float) (raster.getRasterWidth() + avgSideLength * (getMarginGapFactor() / 100));
                    ;
                    yOffset = (float) (raster.getRasterHeight() / 2.0 + labelBounds.getHeight() - heightInformationBlock);
                    break;

                case MetaDataLayerType.LOCATION_RIGHT_BOTTOM:
                    xOffset = (float) (raster.getRasterWidth() + avgSideLength * (getMarginGapFactor() / 100));
                    ;
                    yOffset = (float) (raster.getRasterHeight() + labelBounds.getHeight() - heightInformationBlock);
                    break;

                case MetaDataLayerType.LOCATION_LEFT:
                    xOffset = (float) (-maxWidthInformationBlock - avgSideLength * (getMarginGapFactor() / 100));
                    ;
                    yOffset = 0;
                    break;

                case MetaDataLayerType.LOCATION_LEFT_CENTER:
                    xOffset = (float) (-maxWidthInformationBlock - avgSideLength * (getMarginGapFactor() / 100));
                    ;
                    yOffset = (float) (raster.getRasterHeight() / 2.0 + labelBounds.getHeight() - heightInformationBlock);
                    break;

                case MetaDataLayerType.LOCATION_LEFT_BOTTOM:
                    xOffset = (float) (-maxWidthInformationBlock - avgSideLength * (getMarginGapFactor() / 100));
                    ;
                    yOffset = (float) (raster.getRasterHeight() + labelBounds.getHeight() - heightInformationBlock);
                    break;

//                default:
//                    xOffset = 0;
//                    yOffset = 0;
            }


            float xMod = (float) (Math.cos(theta));
            float yMod = -1 * (float) (Math.sin(theta));

            g2d.drawString(glyph.getText(), xMod + xOffset, yMod + yOffset);

            g2d.rotate(1 * Math.PI - theta);
            g2d.rotate(-glyph.getAngle());
//            g2d.translate(-glyph.getX(), -glyph.getY());

            g2d.translate(0, labelBounds.getHeight());
        }
        g2d.setTransform(origTransform);

        g2d.setPaint(origColor);
        g2d.setFont(origFont);
    }


    private AlphaComposite getAlphaComposite(double itemTransparancy) {
        double combinedAlpha = (1.0 - getTransparency()) * (1.0 - itemTransparancy);
        return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) combinedAlpha);
    }

    @Override
    public void disposeLayer() {
        final Product product = getProduct();
        if (product != null) {
            product.removeProductNodeListener(productNodeHandler);
            headerFooter = null;
            raster = null;
        }
    }

    @Override
    protected void fireLayerPropertyChanged(PropertyChangeEvent event) {
        String propertyName = event.getPropertyName();
        if (
                propertyName.equals(MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD2_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD3_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD4_KEY) ||

                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD2_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_METADATA_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_METADATA2_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_METADATA3_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_METADATA4_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_METADATA5_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_METADATA_KEYS_SHOW_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_METADATA_DELIMITER_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_METADATA_SHOW_ALL_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_KEY) ||

                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_MY_INFO_SHOW_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD2_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD3_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD4_KEY) ||

                        propertyName.equals(MetaDataLayerType.PROPERTY_HEADER_LOCATION_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_HEADER_GAP_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_HEADER_FONT_SIZE_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_HEADER_FONT_COLOR_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_HEADER_FONT_ITALIC_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_HEADER_FONT_BOLD_KEY) ||

                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_LOCATION_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_GAP_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_FONT_SIZE_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_FONT_COLOR_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_FONT_ITALIC_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_FONT_BOLD_KEY) ||

                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_GAP_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_FONT_SIZE_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_FONT_COLOR_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_FONT_ITALIC_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_FONT_BOLD_KEY) ||

                        propertyName.equals(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD1_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD2_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD3_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD4_KEY)
        ) {
            headerFooter = null;
        }
        if (getConfiguration().getProperty(propertyName) != null) {
            getConfiguration().setValue(propertyName, event.getNewValue());
        }
        super.fireLayerPropertyChanged(event);
    }


    private String getHeader() {
        String header = getConfigurationProperty(MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD_KEY,
                MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD_DEFAULT);
        return header;
    }

    private String getHeader2() {
        String header2 = getConfigurationProperty(MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD2_KEY,
                MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD2_DEFAULT);
        return header2;
    }

    private String getHeader3() {
        String header3 = getConfigurationProperty(MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD3_KEY,
                MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD3_DEFAULT);
        return header3;
    }

    private String getHeader4() {
        String header4 = getConfigurationProperty(MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD4_KEY,
                MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD4_DEFAULT);
        return header4;
    }


    private boolean getHeaderShow() {
        boolean header = getConfigurationProperty(MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY,
                MetaDataLayerType.PROPERTY_HEADER_SHOW_DEFAULT);
        return header;
    }


    private boolean getMarginMetadataKeysShow() {
        boolean footerMetadataKeysShow = getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_METADATA_KEYS_SHOW_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA_KEYS_SHOW_DEFAULT);
        return footerMetadataKeysShow;
    }


    private String getMarginTextfield1() {
        String footer = getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD_DEFAULT);
        return footer;
    }

    private String getMarginTextfield2() {
        String footer = getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD2_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD2_DEFAULT);
        return footer;
    }


    private boolean getMarginShow() {
        boolean footer = getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_SHOW_DEFAULT);
        return footer;
    }

    private String getMarginMetadata1() {
        String footerMetadata = getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_METADATA_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA_DEFAULT);
        return footerMetadata;
    }

    private String getMarginMetadata2() {
        String footerMetadata2 = getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_METADATA2_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA2_DEFAULT);
        return footerMetadata2;
    }

    private String getMarginMetadata3() {
        String footerMetadata = getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_METADATA3_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA3_DEFAULT);
        return footerMetadata;
    }

    private String getMarginMetadata4() {
        String footerMetadata = getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_METADATA4_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA4_DEFAULT);
        return footerMetadata;
    }

    private String getMarginMetadata5() {
        String footerMetadata = getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_METADATA5_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA5_DEFAULT);
        return footerMetadata;
    }


    private String getMarginMetadataDelimiter() {
        String footerMetadataDelimiter = getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_METADATA_DELIMITER_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA_DELIMITER_DEFAULT);
        return footerMetadataDelimiter;
    }


    private boolean getFooter2Show() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_SHOW_DEFAULT);
    }

    private boolean getFooter2MyInfoShow() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_FOOTER2_MY_INFO_SHOW_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_MY_INFO_SHOW_DEFAULT);
    }


    private String getFooter2Textfield() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD_DEFAULT);
    }

    private String getFooter2Textfield2() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD2_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD2_DEFAULT);
    }

    private String getFooter2Textfield3() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD3_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD3_DEFAULT);
    }

    private String getFooter2Textfield4() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD4_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD4_DEFAULT);
    }


    private ArrayList<String> getFooterMetadataArrayList() {

        ArrayList<String> footerMetadataArrayList = new ArrayList<String>();
        String footerMetadata = getMarginMetadata1();
        if (footerMetadata != null && footerMetadata.trim() != null && footerMetadata.trim().length() > 0) {
            String[] paramsArray = footerMetadata.split("[ ,]+");
            for (String currentParam : paramsArray) {
                if (currentParam != null && currentParam.trim() != null && currentParam.trim().length() > 0) {
                    footerMetadataArrayList.add(currentParam.trim());
                }
            }
        }

//        return (String[]) footerMetadataArrayList.toArray();
        return footerMetadataArrayList;
    }

    private ArrayList<String> getMetadataArrayList(String metadataList) {

        ArrayList<String> footerMetadataArrayList = new ArrayList<String>();
        if (metadataList != null && metadataList.trim() != null && metadataList.trim().length() > 0) {
            String[] paramsArray = metadataList.split("[ ,]+");
            for (String currentParam : paramsArray) {
                if (currentParam != null && currentParam.trim() != null && currentParam.trim().length() > 0) {
                    footerMetadataArrayList.add(currentParam.trim());
                }
            }
        }

        return footerMetadataArrayList;
    }


    private ArrayList<String> getHeaderFooterLinesArray(String text) {
        ArrayList<String> lineArrayList = new ArrayList<String>();

        if (text != null && text.length() > 0) {
            String[] linesArray = text.split("(\\n|<br>)");
            for (String currentLine : linesArray) {
                if (currentLine != null && currentLine.length() > 0) {
                    currentLine = replaceStringVariables(currentLine, false, "<I=");
                    currentLine = replaceStringVariables(currentLine, false, "<INFO=");
                    currentLine = replaceStringVariables(currentLine, false, "<FILE_INFO=");
                    currentLine = replaceStringVariables(currentLine, false, "<BAND_INFO=");
                    currentLine = replaceStringVariables(currentLine, false, "<M=");
                    currentLine = replaceStringVariables(currentLine, false, "<MG=");
                    currentLine = replaceStringVariables(currentLine, false, "<G=");
                    currentLine = replaceStringVariables(currentLine, false, "<META=");
                    currentLine = replaceStringVariables(currentLine, false, "<FILE_META=");
                    currentLine = replaceStringVariables(currentLine, false, "<MB=");
                    currentLine = replaceStringVariables(currentLine, false, "<B=");
                    currentLine = replaceStringVariables(currentLine, false, "<BAND_META=");
                    lineArrayList.add(currentLine);
                }
            }
        }

        return lineArrayList;
    }


    private boolean displayAllInfo() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_DEFAULT);
    }

    private boolean displayAllMetadata() {
        boolean displayAllMetadata = getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_METADATA_SHOW_ALL_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA_SHOW_ALL_DEFAULT);
        return displayAllMetadata;
    }

    private boolean displayAllMetadataProcessControlParams() {
        boolean displayAllMetadataProcessControlParams = getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_DEFAULT);
        return displayAllMetadataProcessControlParams;
    }

    private boolean displayAllBandMetadata() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_DEFAULT);
    }


    private double getMarginGapFactor() {
        double locationGapFactor = getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_GAP_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_GAP_DEFAULT);
        return locationGapFactor;
    }

    private double getFooter2GapFactor() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_FOOTER2_GAP_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_GAP_DEFAULT);
    }


    private double getHeaderGapFactor() {
        double headerGapFactor = getConfigurationProperty(MetaDataLayerType.PROPERTY_HEADER_GAP_KEY,
                MetaDataLayerType.PROPERTY_HEADER_GAP_DEFAULT);
        return headerGapFactor;
    }


    private String getHeaderLocation() {
        String location = getConfigurationProperty(MetaDataLayerType.PROPERTY_HEADER_LOCATION_KEY,
                MetaDataLayerType.PROPERTY_HEADER_LOCATION_DEFAULT);
        return location;
    }

    private String getMarginLocation() {
        String footerLocation = getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_LOCATION_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_LOCATION_DEFAULT);
        return footerLocation;
    }

    private String getFooter2Location() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_DEFAULT);
    }


    private int getHeaderFontSizePixels() {
        int fontSizePts = getConfigurationProperty(MetaDataLayerType.PROPERTY_HEADER_FONT_SIZE_KEY,
                MetaDataLayerType.PROPERTY_HEADER_FONT_SIZE_DEFAULT);

        return (int) Math.round(getPtsToPixelsMultiplier() * fontSizePts);
    }

    private int getMarginFontSizePixels() {
        int fontSizePts = getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_FONT_SIZE_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_FONT_SIZE_DEFAULT);

        return (int) Math.round(getPtsToPixelsMultiplier() * fontSizePts);
    }

    private int getFooter2FontSizePixels() {
        int fontSizePts = getConfigurationProperty(MetaDataLayerType.PROPERTY_FOOTER2_FONT_SIZE_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_FONT_SIZE_DEFAULT);

        return (int) Math.round(getPtsToPixelsMultiplier() * fontSizePts);
    }

    private Color getHeaderFontColor() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_HEADER_FONT_COLOR_KEY,
                MetaDataLayerType.PROPERTY_HEADER_FONT_COLOR_DEFAULT);
    }

    private Color getMarginFontColor() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_FONT_COLOR_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_FONT_COLOR_DEFAULT);
    }

    private Color getFooter2FontColor() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_FOOTER2_FONT_COLOR_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_FONT_COLOR_DEFAULT);
    }

    private double getPtsToPixelsMultiplier() {

        if (ptsToPixelsMultiplier == NULL_DOUBLE) {
            double maxSideSize = Math.max(raster.getRasterHeight(), raster.getRasterWidth());
            double avgSideSize = (raster.getRasterHeight() + raster.getRasterWidth()) / 2.0;

            ptsToPixelsMultiplier = avgSideSize * 0.001;
        }


        return ptsToPixelsMultiplier;
    }


    private String getHeaderFontStyle() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_KEY,
                MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_DEFAULT);
    }

    private String getMarginFontStyle() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_DEFAULT);
    }

    private String getFooter2FontStyle() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_DEFAULT);
    }


    private Boolean isHeaderFontItalic() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_HEADER_FONT_ITALIC_KEY,
                MetaDataLayerType.PROPERTY_HEADER_FONT_ITALIC_DEFAULT);
    }

    private Boolean isHeaderFontBold() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_HEADER_FONT_BOLD_KEY,
                MetaDataLayerType.PROPERTY_HEADER_FONT_BOLD_DEFAULT);
    }

    private int getHeaderFontType() {
        if (isHeaderFontItalic() && isHeaderFontBold()) {
            return Font.ITALIC | Font.BOLD;
        } else if (isHeaderFontItalic()) {
            return Font.ITALIC;
        } else if (isHeaderFontBold()) {
            return Font.BOLD;
        } else {
            return Font.PLAIN;
        }
    }


    private Boolean isMarginFontItalic() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_FONT_ITALIC_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_FONT_ITALIC_DEFAULT);
    }

    private Boolean isMarginFontBold() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_FONT_BOLD_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_FONT_BOLD_DEFAULT);
    }

    private int getMarginFontType() {
        if (isMarginFontItalic() && isMarginFontBold()) {
            return Font.ITALIC | Font.BOLD;
        } else if (isMarginFontItalic()) {
            return Font.ITALIC;
        } else if (isMarginFontBold()) {
            return Font.BOLD;
        } else {
            return Font.PLAIN;
        }
    }

    private Boolean isFooter2FontItalic() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_FOOTER2_FONT_ITALIC_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_FONT_ITALIC_DEFAULT);
    }

    private Boolean isFooter2FontBold() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_FOOTER2_FONT_BOLD_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_FONT_BOLD_DEFAULT);
    }

    private int getFooter2FontType() {
        if (isFooter2FontItalic() && isFooter2FontBold()) {
            return Font.ITALIC | Font.BOLD;
        } else if (isMarginFontItalic()) {
            return Font.ITALIC;
        } else if (isMarginFontBold()) {
            return Font.BOLD;
        } else {
            return Font.PLAIN;
        }
    }


    private String getMyInfo1() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD1_KEY,
                MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD1_DEFAULT);
    }

    private String getMyInfo2() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD2_KEY,
                MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD2_DEFAULT);
    }

    private String getMyInfo3() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD3_KEY,
                MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD3_DEFAULT);
    }

    private String getMyInfo4() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD4_KEY,
                MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD4_DEFAULT);
    }

    private String getMyInfo() {
        StringBuilder sb = new StringBuilder();

        String myInfo1 = getMyInfo1();
        String myInfo2 = getMyInfo2();
        String myInfo3 = getMyInfo3();
        String myInfo4 = getMyInfo4();

        if (myInfo1 != null && myInfo1.length() > 1) {
            sb.append(myInfo1);
            sb.append(" ");
        }

        if (myInfo2 != null && myInfo2.length() > 1) {
            sb.append(myInfo2);
            sb.append(" ");
        }

        if (myInfo3 != null && myInfo3.length() > 1) {
            sb.append(myInfo3);
            sb.append(" ");
        }

        if (myInfo4 != null && myInfo4.length() > 1) {
            sb.append(myInfo4);
            sb.append(" ");
        }

        String myInfo = sb.toString();
        if (myInfo != null) {
            myInfo = myInfo.trim();
        }

        return myInfo;
    }


    private class ProductNodeHandler extends ProductNodeListenerAdapter {

        /**
         * Overwrite this method if you want to be notified when a node changed.
         *
         * @param event the product node which the listener to be notified
         */
        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (event.getSourceNode() == getProduct() && Product.PROPERTY_NAME_SCENE_GEO_CODING.equals(
                    event.getPropertyName())) {
                // Force recreation
                headerFooter = null;
                fireLayerDataChanged(getModelBounds());
            }
        }
    }

}
