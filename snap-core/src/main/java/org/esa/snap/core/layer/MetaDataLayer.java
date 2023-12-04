package org.esa.snap.core.layer;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.MetadataUtils;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


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



    static enum Annotation {
        HEADER,
        FOOTER,
        MARGIN
    }

    static enum META_TYPE {
        PROPERTY,
        GLOBAL_ATTRIBUTE,
        BAND_ATTRIBUTE
    }





    boolean showNullKeys = true;





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
                for (String key : MetadataUtils.getAllPossibleRelatedKeys(curr)) {
                    if (ProductUtils.isMetadataKeyExists(raster.getProduct(), key)) {
                        marginMetadataCombinedArrayList.add(key);
                    }
                }
            }

            for (String curr : getMetadataArrayList(getMarginMetadata4())) {
                for (String key : MetadataUtils.getAllPossibleRelatedKeys(curr)) {
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
                for (String infoField : MetadataUtils.INFO_PARAMS) {
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

//            if (marginList != null) {
//                marginList.add("");
//            }

            if (marginInfoCombinedArrayList.size() > 0) {
                if (getMarginPropertyHeading() != null && getMarginPropertyHeading().length() > 0) {
                    if (marginList.size() > 0) {
                        marginList.add("");
                    }
                    marginList.add(getMarginPropertyHeading());
                }
                addFromMetadataList(marginInfoCombinedArrayList, marginList, META_TYPE.PROPERTY);
            }

            if (marginMetadataCombinedArrayList.size() > 0) {
                if (getMarginGlobalHeading() != null && getMarginGlobalHeading().length() > 0) {
                    if (marginList.size() > 0) {
                        marginList.add("");
                    }
                    marginList.add(getMarginGlobalHeading());
                }
                addFromMetadataList(marginMetadataCombinedArrayList, marginList, META_TYPE.GLOBAL_ATTRIBUTE);
            }

            if (marginBandMetadataCombinedArrayList.size() > 0) {
                if (getMarginBandHeading() != null && getMarginBandHeading().length() > 0) {
                    if (marginList.size() > 0) {
                        marginList.add("");
                    }
//                    marginList.add(getMarginBandHeading()+ " " + raster.getName());
                    marginList.add(getMarginBandHeading());
                }
                addFromMetadataList(marginBandMetadataCombinedArrayList, marginList, META_TYPE.BAND_ATTRIBUTE);
            }


//
//            if (marginMetadataCombinedArrayList.size() > 0) {
//                marginList.add("File Metadata: (Global_Attributes)");
//                addFromMetadataList(marginMetadataCombinedArrayList, marginList, true, true);
//                marginList.add("");
//            }
//
//            if (marginBandMetadataCombinedArrayList.size() > 0) {
//                marginList.add("Band Metadata '" + raster.getName() + "' (Band_Attributes):");
//                addFromMetadataList(marginBandMetadataCombinedArrayList, marginList, true, false);
//                marginList.add("");
//            }


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
                    drawTextHeaderFooter(g2d, textGlyphHeader, Annotation.HEADER, raster);
                }
                if (getMarginShow()) {
                    drawTextHeaderFooter(g2d, textGlyphsFooter, Annotation.MARGIN, raster);
                }
                if (getFooter2Show()) {
                    drawTextHeaderFooter(g2d, textGlyphsFooter2, Annotation.FOOTER, raster);
                }

            } finally {
                g2d.setTransform(transformSave);
            }
        }
    }



    private void addFromMetadataList(ArrayList<String> footerMetadataCombinedArrayList, List<String> footerList, META_TYPE meta_type) {
        for (String currKey : footerMetadataCombinedArrayList) {
            if (currKey != null && currKey.length() > 0) {
                String currParam = null;
                if (META_TYPE.PROPERTY == meta_type) {
                    int length = currKey.length();
                    if (length > 2) {
                        currParam = MetadataUtils.getDerivedMeta(currKey.toUpperCase(), raster, MetadataUtils.INFO_PARAM_WAVE);

                        if (getMarginMetadataKeysShow()) {
                            currParam = currKey + getMarginMetadataDelimiter() + currParam;
                        }
                    }
                } else {
                    String key = currKey;

                    if (META_TYPE.GLOBAL_ATTRIBUTE == meta_type) {
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




    private void getUserValues() {


    }


    private void drawTextHeaderFooter(Graphics2D g2d,
                                      final MetaDataOnImage.TextGlyph[] textGlyphs,
                                      Annotation annotation,
                                      RasterDataNode raster) {


        Color origColor = (Color) g2d.getPaint();
        AffineTransform origTransform = g2d.getTransform();
        Font origFont = g2d.getFont();

        if (annotation == Annotation.HEADER) {
            Font font = new Font(getHeaderFontStyle(), getHeaderFontType(), getHeaderFontSizePixels());
            g2d.setFont(font);
            g2d.setPaint(getHeaderFontColor());
        } else if (annotation == Annotation.FOOTER) {
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
            Font origFontPreBounds = g2d.getFont();

            int fontSize;

            if (glyph.getText().contains("<small>") || glyph.getText().contains("<SMALL>") || glyph.getText().contains("</small>") || glyph.getText().contains("</SMALL>")) {
                fontSize = (int) Math.ceil(g2d.getFont().getSize() * DrawingUtils.REDUCE_FONT_FACTOR);
                Font boundsFont = new Font(g2d.getFont().getName(), g2d.getFont().getStyle(), fontSize);
                g2d.setFont(boundsFont);
            }

            String textTagsRemoved =  DrawingUtils.getTextTagsRemoved(glyph.getText());

            Rectangle2D labelBounds = g2d.getFontMetrics().getStringBounds(textTagsRemoved, g2d);
            maxWidthInformationBlock = Math.max(labelBounds.getWidth(), maxWidthInformationBlock);
            heightInformationBlock += labelBounds.getHeight();
            g2d.setFont(origFontPreBounds);
        }


        double yTopTranslateFirstLine;
        double yBottomTranslateFirstLine;

        double avgSideLength = (raster.getRasterWidth() + raster.getRasterHeight())/ 2.0;

        if (annotation == Annotation.HEADER) {
            yTopTranslateFirstLine = -heightInformationBlock - avgSideLength * (getHeaderGapFactor() / 100);
            yBottomTranslateFirstLine = avgSideLength * (getHeaderGapFactor() / 100);
        } else if (annotation == Annotation.FOOTER) {
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

            String textTagsRemoved =  DrawingUtils.getTextTagsRemoved(glyph.getText());
            Rectangle2D labelBounds = g2d.getFontMetrics().getStringBounds(textTagsRemoved, g2d);
//            Rectangle2D labelBounds = g2d.getFontMetrics().getStringBounds(glyph.getText(), g2d);

            String location;
            if (annotation == Annotation.HEADER) {
                location = getHeaderLocation();
            } else if (annotation == Annotation.FOOTER) {
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



//            g2d.drawString(glyph.getText(), xMod + xOffset, yMod + yOffset);
            AffineTransform transform1 = g2d.getTransform();
            g2d.translate(xMod + xOffset, yMod + yOffset);

            if (annotation == Annotation.HEADER) {
                DrawingUtils.drawText(g2d, glyph.getText(), isHeaderConvertCaret());
            } else if (annotation == Annotation.FOOTER) {
                DrawingUtils.drawText(g2d, glyph.getText(), isFooterConvertCaret());
            } else {
                DrawingUtils.drawText(g2d, glyph.getText(), isMarginConvertCaret());
            }

            g2d.setTransform(transform1);


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
                        propertyName.equals(MetaDataLayerType.PROPERTY_HEADER_CONVERT_CARET_KEY) ||

                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD2_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_METADATA_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_METADATA2_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_METADATA3_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_PROPERTY_HEADING_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_GLOBAL_HEADING_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_BAND_HEADING_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_METADATA4_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_METADATA5_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_METADATA_KEYS_SHOW_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_METADATA_DELIMITER_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_METADATA_SHOW_ALL_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_MARGIN_CONVERT_CARET_KEY) ||

                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_MY_INFO_SHOW_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD2_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD3_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD4_KEY) ||
                        propertyName.equals(MetaDataLayerType.PROPERTY_FOOTER_CONVERT_CARET_KEY) ||


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

    private String getMarginPropertyHeading() {
        String marginPropertyHeading = getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_PROPERTY_HEADING_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_PROPERTY_HEADING_DEFAULT);

        return MetadataUtils.getReplacedStringAllVariables(marginPropertyHeading, raster, "", "");
    }

    private String getMarginGlobalHeading() {
        String marginGlobalHeading = getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_GLOBAL_HEADING_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_GLOBAL_HEADING_DEFAULT);

        return MetadataUtils.getReplacedStringAllVariables(marginGlobalHeading, raster, "", "");
    }

    private String getMarginBandHeading() {
        String marginBandHeading = getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_BAND_HEADING_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_BAND_HEADING_DEFAULT);

        return MetadataUtils.getReplacedStringAllVariables(marginBandHeading, raster, "", "");
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
        String delimiter = getMarginMetadataDelimiter();

        if (text != null && text.length() > 0) {
            String[] linesArray = text.split("(\\n|<br>)");
            for (String currentLine : linesArray) {
                if (currentLine != null && currentLine.length() > 0) {
                    currentLine = MetadataUtils.getReplacedStringAllVariables(currentLine, raster, delimiter, MetadataUtils.INFO_PARAM_WAVE);
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

    private boolean isHeaderConvertCaret() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_HEADER_CONVERT_CARET_KEY,
                MetaDataLayerType.PROPERTY_HEADER_CONVERT_CARET_DEFAULT);
    }

    private boolean isMarginConvertCaret() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_MARGIN_CONVERT_CARET_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_CONVERT_CARET_DEFAULT);
    }

    private boolean isFooterConvertCaret() {
        return getConfigurationProperty(MetaDataLayerType.PROPERTY_FOOTER_CONVERT_CARET_KEY,
                MetaDataLayerType.PROPERTY_FOOTER_CONVERT_CARET_DEFAULT);
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
            double aspectRatio = raster.getRasterWidth() / raster.getRasterHeight();

            double maxSideSize = Math.max(raster.getRasterHeight(), raster.getRasterWidth());
            double avgSideSize = (raster.getRasterHeight() + raster.getRasterWidth()) / 2.0;

            double size;
            if (aspectRatio > 1.5) {
                size = avgSideSize;
            } else {
                size = raster.getRasterHeight();
            }

            ptsToPixelsMultiplier = size * 0.001;
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
