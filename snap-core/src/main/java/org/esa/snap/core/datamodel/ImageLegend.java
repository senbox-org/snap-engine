/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.datamodel;

import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.layer.ColorBarLayer;
import org.esa.snap.core.layer.ColorBarLayerType;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

import static org.esa.snap.core.layer.ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_DEFAULT;


/**
 * The <code>ImageLegend</code> class is used to generate an image legend from a <code>{@link
 * ImageInfo}</code> instance.
 *
 * @author Norman Fomferra
 * @author Daniel Knowles
 * @version $Revision$ $Date$
 */
// MAY2021 - Daniel Knowles - Major revisions to Color Bar Legend
// todo there is extra unused methods and some commented out code which relates so SeaDAS-BEAM code for defining preset colorbar labels based on a bandname lookup, the commented out and unused methods may be useful in implementing that mechanism later if it is desired.

public class ImageLegend {

    public static final String PROPERTY_NAME_COLORBAR_TITLE_OVERRIDE = "palettes.colorbar.Title.Override";
    public static final boolean DEFAULT_COLORBAR_TITLE_OVERRIDE = false;
    public static final String PROPERTY_NAME_COLORBAR_LABELS_OVERRIDE = "palettes.colorbar.Labels.Override";
    public static final boolean DEFAULT_COLORBAR_LABELS_OVERRIDE = false;
    public static final String PROPERTY_NAME_COLORBAR_ALLOW_RESET = "palettes.colorbar.Allow.Reset";
    public static final boolean DEFAULT_COLORBAR_ALLOW_RESET = false;

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    public static final int NULL_INT = -999;

    public static final String DISTRIB_EVEN_STR = ColorBarLayerType.DISTRIB_EVEN_STR;
    public static final String DISTRIB_EXACT_STR = ColorBarLayerType.DISTRIB_EXACT_STR;
    public static final String DISTRIB_MANUAL_STR = ColorBarLayerType.DISTRIB_MANUAL_STR;

    public static final double HORIZONTAL_TITLE_PARAMETER_UNITS_GAP_FACTOR = 2;
    public static final double HORIZONTAL_INTER_LABEL_GAP_FACTOR = 3;
    public static final double VERTICAL_INTER_LABEL_GAP_FACTOR = 0.75;

    public static final double LEFT_SIDE_BORDER_GAP = 0.6;  // left side gap is oriented and is relative to colorbar and not to the scene image
    public static final double RIGHT_SIDE_BORDER_GAP = 0.6; // right side gap is oriented and is relative to colorbar and not to the scene image
    public static final double TOP_BORDER_GAP = 0.3; // top gap is oriented and is relative to colorbar and not to the scene image
    public static final double BOTTOM_BORDER_GAP = 0.5; // bottom gap is oriented and is relative to colorbar and not to the scene image
//    public static final double LEFT_SIDE_BORDER_GAP = 5.0;  // left-right is oriented and is relative to colorbar and not the scene image
//    public static final double RIGHT_SIDE_BORDER_GAP = 0.0; // left-right is oriented and is relative to colorbar and not the scene image
//    public static final double TOP_BORDER_GAP = 5.0; // top-bottom is oriented and is relative to colorbar and not the scene image
//    public static final double BOTTOM_BORDER_GAP = 0.0; // top-bottom is oriented and is relative to colorbar and not the scene image


    public static final double FORCED_CHANGE_FACTOR = 0.0001;

    private double weightTolerance;

    private double titleToUnitsVerticalGap;
    private double titleToUnitsHorizontalGap;
    private double titleHeight;
    private double titleWidth;
    private double titleSingleLetterWidth;
    private double unitsHeight;
    private double unitsWidth;
    private double unitsSingleLetterWidth;
    private double labelHeight;
    private double labelLongestWidth;
    private double labelSingleLetterWidth;


    // Independent attributes (Properties)
    private final ImageInfo imageInfo;
    private final RasterDataNode raster;
    private boolean showTitle;
    private boolean showUnits;
    private boolean unitsAltUse;
    private boolean titleAltUse;
    private String titleText;
    private String titleAlt;
    private String unitsText;
    private String unitsAlt;
    private String unitsNull;
    private boolean convertCaret;
    private boolean unitsParenthesis;
    private String orientation;
    private String distributionType;
    private int tickMarkCount;


    private String titleVerticalAnchor;
    private boolean reversePalette = true;
    private double sceneAspectBestFit;

    private Color tickmarkColor;
    private int tickmarkLength = NULL_INT;
    private int tickmarkWidth = NULL_INT;
    private boolean tickmarkShow;

    private int borderWidth;
    private Color borderColor;
    private boolean borderShow;

    private Color backdropColor;
    private boolean transparencyEnabled;
    private float backdropTransparency;
    private boolean backdropShow;

    private Color backdropBorderColor;
    private int backdropBorderWidth;
    private boolean backdropBorderShow;

    private String labelsFontName;
    private int labelsFontType;

    private String titleFontName;
    private int titleFontType;
    private String unitsFontName;
    private int unitsFontType;

    private Color labelsColor;
    private boolean labelsShow;
    private Color titleColor;
    private Color unitsColor;

    private boolean antialiasing;
    private int decimalPlaces;
    private boolean decimalPlacesForce;
    private String customLabelValues;

    private double scalingFactor;
    private int titleFontSize;
    private int unitsFontSize;
    private int labelsFontSize;
    private int colorBarLength;
    private int colorBarWidth;
    private double layerScaling;

    private String titleOverRide = null;

    private int leftSideBorderGap = NULL_INT;   // TITLE_TO_PALETTE_GAP
    private int rightSideBorderGap = NULL_INT;   // TITLE_TO_PALETTE_GAP
    private int topBorderGap = NULL_INT;   // TITLE_TO_PALETTE_GAP
    private int bottomBorderGap = NULL_INT;   // TITLE_TO_PALETTE_GAP

    private double leftSideBorderGapFactor;
    private double rightSideBorderGapFactor;
    private double topBorderGapFactor;
    private double bottomBorderGapFactor;
    private double titleGapFactor;
    private double labelGapFactor;

    private int labelGap = NULL_INT;      // LABEL_TO_COLORBAR BORDER_GAP
    private int titleGap = NULL_INT;      // HEADER_TO_COLORBAR BORDER_GAP

    // Dependent, internal attributes
    private Rectangle paletteRect;
    private Rectangle legendRect;
    private Dimension legendSize;
    private Shape tickMarkShape;
    private int palettePosStart;
    private int palettePosEnd;
    private ArrayList<ColorBarInfo> colorBarInfos = new ArrayList<ColorBarInfo>();

    public enum FONT_SCRIPT {
        NORMAL,
        SUPER_SCRIPT,
        SUBSCRIPT
    }

    public ImageLegend(ImageInfo imageInfo, RasterDataNode raster) {
        this.imageInfo = imageInfo;
        this.raster = raster;

        antialiasing = true;

//        setCustomLabelValues("");

    }


    public ImageLegend getCopyOfImageLegend() {

        ImageLegend imageLegendCopy = new ImageLegend(raster.getImageInfo(), raster);

        imageLegendCopy.setOrientation(getOrientation());
        imageLegendCopy.setReversePalette(isReversePalette());
        imageLegendCopy.setSceneAspectBestFit(getSceneAspectBestFit());

        imageLegendCopy.setShowTitle(isShowTitle());
        imageLegendCopy.setTitle(getTitleText());
        imageLegendCopy.setTitleAlt(getTitleAlt());
        imageLegendCopy.setTitleAltUse(isTitleAltUse());
        imageLegendCopy.setTitleFontSize(getTitleFontSize());
        imageLegendCopy.setTitleColor(getTitleColor());
        imageLegendCopy.setTitleFontName(getTitleFontName());
        imageLegendCopy.setTitleFontType(getTitleFontType());

        imageLegendCopy.setShowUnits(isShowUnits());
        imageLegendCopy.setUnits(getUnitsText());
        imageLegendCopy.setUnitsAlt(getUnitsAlt());
        imageLegendCopy.setUnitsNull(getUnitsNull());
        imageLegendCopy.setUnitsParenthesis(isUnitsParenthesis());
        imageLegendCopy.setConvertCaret(isConvertCaret());
        imageLegendCopy.setUnitsAltUse(isUnitsAltUse());
        imageLegendCopy.setUnitsFontSize(getUnitsFontSize());
        imageLegendCopy.setUnitsColor(getUnitsColor());
        imageLegendCopy.setUnitsFontName(getUnitsFontName());
        imageLegendCopy.setUnitsFontType(getUnitsFontType());

        imageLegendCopy.setTickMarkCount(getTickMarkCount());
        imageLegendCopy.setDistributionType(getDistributionType());
        imageLegendCopy.setCustomLabelValues(getCustomLabelValues());
        imageLegendCopy.setScalingFactor(getScalingFactor());
        imageLegendCopy.setDecimalPlaces(getDecimalPlaces());
        imageLegendCopy.setDecimalPlacesForce(isDecimalPlacesForce());

        imageLegendCopy.setTitleVerticalAnchor(getTitleVerticalAnchor());
        imageLegendCopy.setColorBarLength(getColorBarLength());
        imageLegendCopy.setColorBarWidth(getColorBarWidth());

        imageLegendCopy.setLayerScaling(getLayerScaling());
        //            imageLegend.setBackgroundTransparencyEnabled(true);

        imageLegendCopy.setAntialiasing((Boolean) true);

        imageLegendCopy.setLabelsShow(isLabelsShow());
        imageLegendCopy.setLabelsFontName(getLabelsFontName());
        imageLegendCopy.setLabelsFontType(getLabelsFontType());
        imageLegendCopy.setLabelsFontSize(getLabelsFontSize());
        imageLegendCopy.setLabelsColor(getLabelsColor());

        imageLegendCopy.setTickmarkColor(getTickmarkColor());
        imageLegendCopy.setTickmarkLength(getTickmarkLength());
        imageLegendCopy.setTickmarkWidth(getTickmarkWidth());
        imageLegendCopy.setTickmarkShow(isTickmarkShow());

        imageLegendCopy.setBackdropColor(getBackdropColor());
        imageLegendCopy.setBackdropTransparency(getBackdropTransparency());
        imageLegendCopy.setBackdropShow(isBackdropShow());

        imageLegendCopy.setBorderShow(isBorderShow());
        imageLegendCopy.setBorderWidth(getBorderWidth());
        imageLegendCopy.setBorderColor(getBorderColor());

        imageLegendCopy.setBackdropBorderColor(getBackdropBorderColor());
        imageLegendCopy.setBackdropBorderWidth(getBackdropBorderWidth());
        imageLegendCopy.setBackdropBorderShow(isBackdropBorderShow());


        imageLegendCopy.setBottomBorderGapFactor(getBottomBorderGapFactor());
        imageLegendCopy.setTopBorderGapFactor(getTopBorderGapFactor());
        imageLegendCopy.setLeftSideBorderGapFactor(getLeftSideBorderGapFactor());
        imageLegendCopy.setRightSideBorderGapFactor(getRightSideBorderGapFactor());
        imageLegendCopy.setTitleGapFactor(getTitleGapFactor());
        imageLegendCopy.setLabelGapFactor(getLabelGapFactor());

        imageLegendCopy.setWeightTolerance(getWeightTolerance());

        return imageLegendCopy;
    }


    public void initLegendWithPreferences(PropertyMap configuration, RasterDataNode raster) {

        setTitle(configuration.getPropertyString(ColorBarLayerType.PROPERTY_TITLE_KEY, ColorBarLayerType.PROPERTY_TITLE_DEFAULT));
        setTitleAltUse(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_TITLE_ALT_USE_KEY, ColorBarLayerType.PROPERTY_TITLE_ALT_USE_DEFAULT));
        setTitleAlt(configuration.getPropertyString(ColorBarLayerType.PROPERTY_TITLE_ALT_KEY, ColorBarLayerType.PROPERTY_TITLE_ALT_DEFAULT));

        setUnits(configuration.getPropertyString(ColorBarLayerType.PROPERTY_UNITS_KEY, ColorBarLayerType.PROPERTY_UNITS_DEFAULT));
        setUnitsAltUse(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_UNITS_ALT_USE_KEY, ColorBarLayerType.PROPERTY_UNITS_ALT_USE_DEFAULT));
        setUnitsAlt(configuration.getPropertyString(ColorBarLayerType.PROPERTY_UNITS_ALT_KEY, ColorBarLayerType.PROPERTY_UNITS_ALT_DEFAULT));

        setUnitsNull(configuration.getPropertyString(ColorBarLayerType.PROPERTY_UNITS_NULL_KEY, ColorBarLayerType.PROPERTY_UNITS_NULL_DEFAULT));

        setConvertCaret(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_CONVERT_CARET_KEY, ColorBarLayerType.PROPERTY_CONVERT_CARET_DEFAULT));
        setUnitsParenthesis(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_UNITS_PARENTHESIS_KEY, ColorBarLayerType.PROPERTY_UNITS_PARENTHESIS_DEFAULT));


        // Orientation Parameters

        setOrientation(configuration.getPropertyString(ColorBarLayerType.PROPERTY_ORIENTATION_KEY,
                ColorBarLayerType.PROPERTY_ORIENTATION_DEFAULT));

        setSceneAspectBestFit(configuration.getPropertyDouble(ColorBarLayerType.PROPERTY_SCENE_ASPECT_BEST_FIT_KEY,
                ColorBarLayerType.PROPERTY_SCENE_ASPECT_BEST_FIT_DEFAULT));

        setReversePalette(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_KEY,
                ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_DEFAULT));


        // Label Distribution and Values

        setTickMarkCount(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_DEFAULT));

        setDistributionType(configuration.getPropertyString(ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_DEFAULT));

        setCustomLabelValues(configuration.getPropertyString(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_DEFAULT));

        setScalingFactor(configuration.getPropertyDouble(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_DEFAULT));

        setDecimalPlaces(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_DEFAULT));

        setDecimalPlacesForce(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_DEFAULT));

        setWeightTolerance(configuration.getPropertyDouble(ColorBarLayerType.PROPERTY_WEIGHT_TOLERANCE_KEY,
                ColorBarLayerType.PROPERTY_WEIGHT_TOLERANCE_DEFAULT));


        // Sizing and Location
        setColorBarLength(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_KEY,
                ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_DEFAULT));

        setColorBarWidth(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_DEFAULT));

        setTitleVerticalAnchor(configuration.getPropertyString(ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_DEFAULT));


        // Title parameters

        setShowTitle(
                configuration.getPropertyBool(ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY,
                        ColorBarLayerType.PROPERTY_TITLE_SHOW_DEFAULT));

//        setTitleText(
//                configuration.getPropertyString(ColorBarLayerType.PROPERTY_TITLE_KEY,
//                        ColorBarLayerType.PROPERTY_TITLE_DEFAULT)
//        );


        setTitleFontSize(
                configuration.getPropertyInt(ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_KEY,
                        ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_DEFAULT));

        setTitleColor(
                configuration.getPropertyColor(ColorBarLayerType.PROPERTY_TITLE_COLOR_KEY,
                        ColorBarLayerType.PROPERTY_TITLE_COLOR_DEFAULT));

        setTitleFontName(
                configuration.getPropertyString(ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_KEY,
                        ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_DEFAULT));


        boolean titleParameterBold = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_KEY,
                ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_DEFAULT);

        boolean titleParameterItalic = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_KEY,
                ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_DEFAULT);

        int titleFontType = ColorBarLayer.getFontType(titleParameterItalic, titleParameterBold);

        setTitleFontType(titleFontType);


        // Units parameters

        setShowUnits(
                configuration.getPropertyBool(ColorBarLayerType.PROPERTY_UNITS_SHOW_KEY,
                        ColorBarLayerType.PROPERTY_UNITS_SHOW_DEFAULT));


//        setUnitsText(
//                configuration.getPropertyString(ColorBarLayerType.PROPERTY_UNITS_KEY,
//                        ColorBarLayerType.PROPERTY_UNITS_DEFAULT)
//        );
//

        setUnitsFontSize(
                configuration.getPropertyInt(ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_KEY,
                        ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_DEFAULT));

        setUnitsColor(
                configuration.getPropertyColor(ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_KEY,
                        ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_DEFAULT));

        setUnitsFontName(
                configuration.getPropertyString(ColorBarLayerType.PROPERTY_UNITS_FONT_NAME_KEY,
                        ColorBarLayerType.PROPERTY_UNITS_FONT_NAME_DEFAULT));


        boolean unitsBold = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_UNITS_FONT_BOLD_KEY,
                ColorBarLayerType.PROPERTY_UNITS_FONT_BOLD_DEFAULT);

        boolean unitsItalic = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_UNITS_FONT_ITALIC_KEY,
                ColorBarLayerType.PROPERTY_UNITS_FONT_ITALIC_DEFAULT);

        int unitsFontType = ColorBarLayer.getFontType(unitsItalic, unitsBold);

        setUnitsFontType(unitsFontType);


        // Labels Parameters

        setLabelsShow(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY,
                ColorBarLayerType.PROPERTY_LABELS_SHOW_DEFAULT));

        setLabelsFontName(configuration.getPropertyString(ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_KEY,
                ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_DEFAULT));

        boolean labelsFontBold = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_KEY,
                ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_DEFAULT);

        boolean labelsFontItalic = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_KEY,
                ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_DEFAULT);

        setLabelsFontType(ColorBarLayer.getFontType(labelsFontItalic, labelsFontBold));

        setLabelsFontSize(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_KEY,
                PROPERTY_LABELS_FONT_SIZE_DEFAULT));

        setLabelsColor(configuration.getPropertyColor(ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_KEY,
                ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_DEFAULT));


        // Tick Marks Section

        setTickmarkShow(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY,
                ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_DEFAULT));

        setTickmarkLength(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_KEY,
                ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_DEFAULT));

        setTickmarkWidth(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_DEFAULT));

        setTickmarkColor(configuration.getPropertyColor(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY,
                ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT));


        // Backdrop Section

        setBackdropShow(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_BACKDROP_SHOW_KEY,
                ColorBarLayerType.PROPERTY_BACKDROP_SHOW_DEFAULT));

        setBackdropColor(configuration.getPropertyColor(ColorBarLayerType.PROPERTY_BACKDROP_COLOR_KEY,
                ColorBarLayerType.PROPERTY_BACKDROP_COLOR_DEFAULT));

        double backdropTrans = configuration.getPropertyDouble(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_KEY,
                ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_DEFAULT);

        setBackdropTransparency(((Number) backdropTrans).floatValue());


        // Palette Border Section

        setBorderShow(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_KEY,
                ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_DEFAULT));

        setBorderWidth(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_DEFAULT));

        setBorderColor(configuration.getPropertyColor(ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_KEY,
                ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_DEFAULT));


        // Legend Border Section

        setBackdropBorderShow(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_DEFAULT));

        setBackdropBorderWidth(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_DEFAULT));

        setBackdropBorderColor(configuration.getPropertyColor(ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_DEFAULT));


        // Legend Border Gap

        setLeftSideBorderGapFactor(configuration.getPropertyDouble(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_DEFAULT));

        setRightSideBorderGapFactor(configuration.getPropertyDouble(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_DEFAULT));

        setTopBorderGapFactor(configuration.getPropertyDouble(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_DEFAULT));

        setBottomBorderGapFactor(configuration.getPropertyDouble(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_DEFAULT));

        setTitleGapFactor(configuration.getPropertyDouble(ColorBarLayerType.PROPERTY_LEGEND_TITLE_GAP_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_TITLE_GAP_DEFAULT));

        setLabelGapFactor(configuration.getPropertyDouble(ColorBarLayerType.PROPERTY_LEGEND_LABEL_GAP_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_LABEL_GAP_DEFAULT));

        setLayerScaling(100.0);


        setAntialiasing((Boolean) true);

        //  imageLegend.setBackgroundTransparencyEnabled(true);
    }


    // todo This block from SeaDAS 7 may contain useful info for later implementing a color bar scheme
//    public void initDefaults(PropertyMap configuration) {
//
//
//        //  configuration.getPropertyBool(PROPERTY_NAME_COLORBAR_TITLE_OVERRIDE, DEFAULT_COLORBAR_TITLE_OVERRIDE);
//        ColorPaletteSourcesInfo colorPaletteSourcesInfo = raster.getImageInfo().getColorPaletteSourcesInfo();
//
//        if (colorPaletteSourcesInfo != null) {
//            final String schemeName = colorPaletteSourcesInfo.getSchemeName();
//            final double min = getImageInfo().getColorPaletteDef().getMinDisplaySample();
//            final double max = getImageInfo().getColorPaletteDef().getMaxDisplaySample();
//
//            final boolean logScaled = getImageInfo().isLogScaled();
//
//            String labels = colorPaletteSourcesInfo.getColorBarLabels();
//
//
//            setHeaderText(raster.getName());
//            if (allowTitleOverride(configuration)) {
//                String overrideColorBarTitle = colorPaletteSourcesInfo.getColorBarTitle();
//                if (overrideColorBarTitle != null && overrideColorBarTitle.length() > 0) {
//                    setHeaderText(overrideColorBarTitle);
//                }
//            }
//
//
//            if (!colorPaletteSourcesInfo.isAlteredScheme(min, max, logScaled) && labels != null && labels.length() > 0 && allowLabelsOverride(configuration)) {
//
//                setDistributionType(ImageLegend.DISTRIB_MANUAL_STR);
//                setFullCustomAddThesePoints(labels);
//
//            } else {
//
//                setNumberOfTicks(5);
//                setScalingFactor(1.0);
//                setDecimalPlaces(2);
//                distributeEvenly();
//                setDistributionType(ImageLegend.DISTRIB_MANUAL_STR);
//            }
//        }
//    }

    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    public RasterDataNode getRaster() {
        return raster;
    }


    public boolean isShowTitle() {
        return showTitle;
    }

    public void setShowTitle(boolean usingHeader) {
        this.showTitle = usingHeader;
    }

    public boolean isTitleAltUse() {
        return titleAltUse;
    }

    public void setTitleAltUse(boolean titleAltUse) {
        this.titleAltUse = titleAltUse;
    }

    public String getTitleText() {
        return (titleText == null) ? "null-test" : titleText;
    }

    public void setTitle(String titleText) {
        this.titleText = titleText;
    }

    public String getTitleAlt() {
        if (titleAlt == null || titleAlt.length() == 0) {
            return "";
        } else {
            return titleAlt;
        }
    }

    public void setTitleAlt(String titleAlt) {
        this.titleAlt = titleAlt;
    }


    public boolean isUnitsAltUse() {
        return unitsAltUse;
    }

    public void setUnitsAltUse(boolean unitsAltUse) {
        this.unitsAltUse = unitsAltUse;
    }

    public String getUnitsText() {
        if (unitsText == null || unitsText.length() == 0) {
            return "";
        } else {
            return unitsText;
        }
    }

    public void setUnits(String unitsText) {
        this.unitsText = unitsText;
    }

    public String getUnitsAlt() {
        if (unitsAlt == null || unitsAlt.length() == 0) {
            return "";
        } else {
            return unitsAlt;
        }
    }

    public void setUnitsAlt(String unitsAlt) {
        this.unitsAlt = unitsAlt;
    }


    public String getUnitsNull() {
        if (unitsNull == null || unitsNull.length() == 0) {
            return "";
        } else {
            return unitsNull;
        }
    }

    public void setUnitsNull(String unitsNull) {
        this.unitsNull = unitsNull;
    }





    public boolean isConvertCaret() {return convertCaret;}
    public void setConvertCaret(boolean convertCaret) {this.convertCaret = convertCaret;}

    public boolean isUnitsParenthesis() {return unitsParenthesis;}
    public void setUnitsParenthesis(boolean unitsParenthesis) {this.unitsParenthesis = unitsParenthesis;}


    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public boolean isHorizontalColorBar() {
        if (ColorBarLayerType.OPTION_BEST_FIT.equals(getOrientation())) {
            double sceneAspectRatio = (raster.getRasterHeight() != 0) ? (double) raster.getRasterWidth() / (double) raster.getRasterHeight(): 1.0;
            // todo Preference on aspectRatio for best fit
//            if (raster.getRasterWidth() > raster.getRasterHeight()) {
            if (sceneAspectRatio > getSceneAspectBestFit()) {
                return true;
            } else {
                return false;
            }
        } else {
            if (ColorBarLayerType.OPTION_HORIZONTAL.equals(getOrientation())) {
                return true;
            } else {
                return false;
            }
        }
    };


    public String getDistributionType() {
        return distributionType;
    }

    public void setDistributionType(String distributionType) {
        this.distributionType = distributionType;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }


    public int getTickMarkCount() {
        return tickMarkCount;
    }

    public void setTickMarkCount(int tickMarkCount) {
        this.tickMarkCount = tickMarkCount;
    }

    public Color getBackdropColor() {
        return backdropColor;
    }

    public void setBackdropColor(Color backdropColor) {
        this.backdropColor = backdropColor;
    }


    public boolean isAntialiasing() {
        return antialiasing;
    }

    public void setAntialiasing(boolean antialiasing) {
        this.antialiasing = antialiasing;
    }

    public void setTransparent(boolean isTransparent) {
        if (isTransparent) {
            setBackdropTransparency(1.0f);
        } else {
            setBackdropTransparency(0.0f);
        }
        setTransparencyEnabled(isTransparent);
    }

    public boolean isTransparencyEnabled() {
        return transparencyEnabled;
    }

    public void setTransparencyEnabled(boolean transparencyEnabled) {
        this.transparencyEnabled = transparencyEnabled;
    }

    public float getBackdropTransparency() {
        return backdropTransparency;
    }

    public void setBackdropTransparency(float backdropTransparency) {
        this.backdropTransparency = backdropTransparency;
    }

    public boolean isAlphaUsed() {
        return transparencyEnabled;
//        return transparencyEnabled && (backdropTransparency > 0.0f && backdropTransparency <= 1.0f);
    }

    public int getBackgroundAlpha() {
        if (transparencyEnabled) {
            if (isBackdropShow()) {
                return Math.round(255f * (1f - backdropTransparency));
            } else {
                return Math.round(0f);
            }
        } else {
            return 255;
        }
    }


    public BufferedImage createImage(Dimension imageLayerDimension, boolean scaleToDimension) {

        double scalingFactor = 1.0;

        if (scaleToDimension) {

            createColorBarInfos();
            initDrawing();

            double oneHundredPercentScalingFactor;
            if (isHorizontalColorBar()) {
                oneHundredPercentScalingFactor = (double) imageLayerDimension.width / (double) legendSize.width;
            } else {
                oneHundredPercentScalingFactor = (double) imageLayerDimension.height / (double) legendSize.height;
            }

            scalingFactor = getLayerScaling() / 100.0;
            scalingFactor = scalingFactor * oneHundredPercentScalingFactor;

        }


        int tmpLabelsFontSize = getLabelsFontSize();
        int tmpTickmarkLength = getTickmarkLength();
        int tmpTickmarkWidth = getTickmarkWidth();
        int tmpTitleFontSize = getTitleFontSize();
        int tmpTitleUnitsFontSize = getUnitsFontSize();
        int tmpColorBarLength = getColorBarLength();
        int tmpColorBarThickness = getColorBarWidth();

        setLabelsFontSize((int) Math.round(scalingFactor * getLabelsFontSize()));
        setTickmarkLength((int) Math.round(scalingFactor * getTickmarkLength()));
        setTickmarkWidth((int) Math.round(scalingFactor * getTickmarkWidth()));
        setTitleFontSize((int) Math.round(scalingFactor * getTitleFontSize()));
        setUnitsFontSize((int) Math.round(scalingFactor * getUnitsFontSize()));
        setColorBarLength((int) Math.round(scalingFactor * getColorBarLength()));
        setColorBarWidth((int) Math.round(scalingFactor * getColorBarWidth()));

        BufferedImage bufferedImage = createImage();

        setLabelsFontSize(tmpLabelsFontSize);
        setTickmarkLength(tmpTickmarkLength);
        setTickmarkWidth(tmpTickmarkWidth);
        setTitleFontSize(tmpTitleFontSize);
        setUnitsFontSize(tmpTitleUnitsFontSize);
        setColorBarLength(tmpColorBarLength);
        setColorBarWidth(tmpColorBarThickness);

        return bufferedImage;
    }


    public BufferedImage createImage() {
        createColorBarInfos();
        initDrawing();
        final BufferedImage bi = createBufferedImage(legendSize.width, legendSize.height);
        final Graphics2D g2d = bi.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        if (isAntialiasing()) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }

        draw(g2d);
        return bi;
    }

    // todo Color Bar Scheme can possibly be added in the future
//    private File getColorPalettesAuxdataDir() {
//        return new File(SystemUtils.getApplicationDataDir(), "beam-ui/auxdata/color-palettes");
//    }

    public void createColorBarInfos() {

        final double min = getImageInfo().getColorPaletteDef().getMinDisplaySample();
        final double max = getImageInfo().getColorPaletteDef().getMaxDisplaySample();

        double value, weight;
        double roundedValue, adjustedWeight;
        colorBarInfos.clear();

        // todo Color Bar Scheme can possibly be added in the future
//
//        String schemeName = imageInfo.getColorPaletteSourcesInfo().getSchemeName();
//        boolean isDefault = false;


//        if (DISTRIB_DEFAULT_STR.equals(getDistributionType()) && schemeName != null && schemeName.equals("chlor_a")) {
//            String test = "0.01,0.03,0.1,0.3,1,3,10";
//    //        setHeaderText("Chlorophyll");
//            setFullCustomAddThesePoints(test);
//            isDefault = true;
//        }


//        if (getFullCustomAddThesePoints() == null || getFullCustomAddThesePoints().length() == 0) {
//            // this will initialize the points
//            distributeEvenly();
//            colorBarInfos.clear();
//        }


//        if (DISTRIB_EXACT_STR.equals(getDistributionType()) || imageInfo.getColorPaletteDef().isDiscrete()) {

//        if (DISTRIB_EVEN_STR.equals(getDistributionType()) && imageInfo.getColorPaletteDef().isDiscrete()) {
//            setDistributionType(DISTRIB_EXACT_STR);
//        }

        if (DISTRIB_EXACT_STR.equals(getDistributionType()) ||
                (DISTRIB_EVEN_STR.equals(getDistributionType()) && imageInfo.getColorPaletteDef().isDiscrete())
        ) {
            final int numPointsInCpdFile = getNumGradationCurvePoints();
            int stepSize = 1;
            //    int stepSize = numPointsInCpdFile / getNumberOfTicks();
            ArrayList<String> manualPointsArrayList = new ArrayList<>();

            for (int i = 0; i < numPointsInCpdFile; i = i + stepSize) {

                ColorPaletteDef.Point slider = getGradationCurvePointAt(i);
                value = slider.getSample();

                if (imageInfo.isLogScaled()) {
                    weight = getLinearWeightFromLogValue(value, min, max);
                } else {
                    weight = getLinearWeightFromLinearValue(value, min, max);
                }

                // Apply tolerance to potentially bring weight into valid state
                weight = getValidWeight(weight);

                if (weight >= 0 && weight <= 1) {
                    if (getScalingFactor() != 0) {
                        value = value * getScalingFactor();
                        ColorBarInfo colorBarInfo = new ColorBarInfo(value, weight, getDecimalPlaces(), isDecimalPlacesForce());

                        double valueScaledFormatted = Double.valueOf(colorBarInfo.getFormattedValue());
                        double minScaled = min * getScalingFactor();
                        double maxScaled = max * getScalingFactor();

                        double weightScaled;
                        if (imageInfo.isLogScaled()) {
                            weightScaled = getLinearWeightFromLogValue(valueScaledFormatted, minScaled, maxScaled);
                        } else {
                            weightScaled = getLinearWeightFromLinearValue(valueScaledFormatted, minScaled, maxScaled);
                        }

                        // Apply tolerance to potentially bring weight into valid state
                        weightScaled = getValidWeight(weightScaled);

                        if (weightScaled >= 0 && weightScaled <= 1) {
                            // adjust weight to match formatted string
                            colorBarInfo.setLocationWeight(weightScaled);

                            colorBarInfos.add(colorBarInfo);
                            manualPointsArrayList.add(colorBarInfo.getFormattedValue());
                        }
// This else block would put label in with no decimal restrictions for the case where the end point is not shown due to rounding
//                        } else {
//                            String newFormattedValue = Double.toString(colorBarInfo.getValue());
//                            colorBarInfo.setFormattedValue(newFormattedValue);
//                            colorBarInfos.add(colorBarInfo);
//                            manualPointsArrayList.add(newFormattedValue);
//                        }
                    }
                }
            }

            if (manualPointsArrayList.size() > 0) {
                String manualPoints = StringUtils.join(manualPointsArrayList, ", ");
                setCustomLabelValues(manualPoints);
            }

        } else if (DISTRIB_EVEN_STR.equals(getDistributionType())) {
            distributeEvenly();

        } else if (DISTRIB_MANUAL_STR.equals(getDistributionType())) {
            if (getCustomLabelValues() == null || getCustomLabelValues().length() == 0) {
                // this will initialize the points
                distributeEvenly();
                colorBarInfos.clear();
            }

            String addThese = getCustomLabelValues();

            if (addThese != null && addThese.length() > 0) {
                String[] formattedValues = addThese.split(",");
                int index = 0;
                for (String formattedValue : formattedValues) {
                    if (formattedValue != null) {
                        formattedValue.trim();
                        if (formattedValue.length() > 0 && scalingFactor != 0) {

                            String[] valueAndString = formattedValue.split(":");
                            if (valueAndString.length == 2) {
                                formattedValue = valueAndString[1];
                                try {
                                    value = Double.valueOf(valueAndString[0]) / getScalingFactor();
                                } catch (Exception e) {
                                    JOptionPane.showMessageDialog(new JOptionPane(), "<html>" + formattedValue + " is not a valid (numeric) Label Value <br> and the entry has been removed.</html>",
                                            "Color Bar Layer Editor Error",
                                            JOptionPane.WARNING_MESSAGE);
                                    String addTheseNew = addThese.replace(valueAndString[0] + ":" + formattedValue + ",", "");
                                    if (addTheseNew.endsWith(valueAndString[0] + ":" + formattedValue)) {
                                        addThese = addTheseNew.replace("," + valueAndString[0] + ":" + formattedValue, "");
                                    } else {
                                        addThese = addTheseNew;
                                    }
                                    formattedValues[index] = String.valueOf(min);
                                    setCustomLabelValues(addThese);
                                    index++;
                                    continue;
                                }
//                                    value = Double.valueOf(valueAndString[0]) / getScalingFactor();
                            } else {
                                try {
                                    value = Double.valueOf(formattedValue) / getScalingFactor();
                                } catch (Exception e) {
                                    JOptionPane.showMessageDialog(new JOptionPane(), "<html>" + formattedValue + " is not a valid (numeric) Label Value <br> and the entry has been removed.<html>",
                                            "Color Bar Layer Editor Error",
                                            JOptionPane.WARNING_MESSAGE);
                                    String addTheseNew = addThese.replace(formattedValue + ",", "");
                                    if (addTheseNew.endsWith(formattedValue)) {
                                        addThese = addTheseNew.replace("," + formattedValue, "");
                                    } else {
                                        addThese = addTheseNew;
                                    }
                                    formattedValues[index] = String.valueOf(min);
                                    setCustomLabelValues(addThese);
                                    index++;
                                    continue;
                                }
                            }
                            if (imageInfo.isLogScaled()) {
                                if (value == min) {
                                    weight = 0;
                                } else if (value == max) {
                                    weight = 1;
                                } else {
                                    weight = getLinearWeightFromLogValue(value, min, max);
                                }
                            } else {
                                weight = getLinearWeightFromLinearValue(value, min, max);
                            }

                            weight = getValidWeight(weight);
                            if (weight >= 0 && weight <= 1) {
                                ColorBarInfo colorBarInfo = new ColorBarInfo(value, weight, formattedValue);
                                colorBarInfos.add(colorBarInfo);
                            }
                        }
                    }
                    index++;
                }
            }
        }

    }


    private double getValidWeight(double weight) {
        // due to rounding issues we want to make sure the weight isn't just below 0 or just above 1
        // this would cause the tick mark to possibly be placed a very tiny amount outside of the colorbar if not corrected here

        boolean valid = (weight >= (0 - weightTolerance) && weight <= (1 + weightTolerance))
                ? true : false;

        if (valid) {
            if (weight > 1) {
                weight = 1;
            }
            if (weight < 0) {
                weight = 0;
            }

            return weight;
        } else {
            return -1;
        }

    }


    private void initDrawing() {

        final BufferedImage bufferedImage = createBufferedImage(100, 100);
        final Graphics2D g2dTmp = bufferedImage.createGraphics();

        initCoreGraphicSizes(g2dTmp);


        g2dTmp.setFont(getLabelsFont());

        Dimension headerRequiredDimension;

        if (isHorizontalColorBar()) {
            headerRequiredDimension = getTitleRequiredDimension(false, false);
        } else {
            if (ColorBarLayerType.VERTICAL_TITLE_TOP.equals(getTitleVerticalAnchor()) ||
                    ColorBarLayerType.VERTICAL_TITLE_BOTTOM.equals(getTitleVerticalAnchor())) {
                headerRequiredDimension = getTitleRequiredDimension(true, false);

            } else {
                headerRequiredDimension = getTitleRequiredDimension(false, true);
            }
        }


        double discreteBooster = 0;
        final int n = getNumGradationCurvePoints();

        double firstLabelWidth = getSingleLabelRequiredDimension(g2dTmp, 0).getWidth();
        int firstLabelOverhangWidth = (int) Math.ceil(firstLabelWidth / 2.0);

        double lastLabelWidth = getSingleLabelRequiredDimension(g2dTmp, colorBarInfos.size() - 1).getWidth();
        int lastLabelOverhangWidth = (int) Math.ceil(lastLabelWidth / 2.0);

        double firstLabelHeight = getSingleLabelRequiredDimension(g2dTmp, 0).getHeight();
        int labelOverhangHeight = (int) Math.ceil(firstLabelHeight / 2.0);

        int tickOffset = 0;

        if (getTickmarkLength() > 0) {
            tickOffset = getTickmarkLength();
        }

        if (isHorizontalColorBar()) {


            Dimension labelsRequiredDimension = getHorizontalLabelsRequiredDimension(g2dTmp);

            double colorBarWithLabelsRequiredLength = firstLabelOverhangWidth + getColorBarLength() + lastLabelOverhangWidth;

            double requiredWidth = Math.max(colorBarWithLabelsRequiredLength,
                    headerRequiredDimension.getWidth());

            requiredWidth = getLeftSideBorderGap() + requiredWidth + getRightSideBorderGap();


            if (n > 1 && imageInfo.getColorPaletteDef().isDiscrete()) {
                discreteBooster = labelsRequiredDimension.getWidth() / (n - 1);
                requiredWidth += discreteBooster;
            }


            int requiredHeaderHeight = (int) Math.ceil(headerRequiredDimension.getHeight());
            int requiredLabelsHeight = (int) Math.ceil(labelsRequiredDimension.getHeight());


            int requiredHeight = getTopBorderGap()
                    + requiredHeaderHeight
                    + getTitleGap()
                    + getColorBarWidth()
                    + getLabelGap()
                    + tickOffset
                    + requiredLabelsHeight
                    + getBottomBorderGap();


            legendSize = new Dimension((int) requiredWidth, requiredHeight);


            paletteRect = new Rectangle(getLeftSideBorderGap() + firstLabelOverhangWidth,
                    getTopBorderGap() + requiredHeaderHeight + getTitleGap(),
                    legendSize.width - getLeftSideBorderGap() - getRightSideBorderGap() - firstLabelOverhangWidth - lastLabelOverhangWidth,
                    getColorBarWidth());

            legendRect = new Rectangle(0, 0, legendSize.width - 1, legendSize.height - 1);


            int paletteGap = 0;
            palettePosStart = paletteRect.x + paletteGap;
            palettePosEnd = paletteRect.x + paletteRect.width - (int) discreteBooster;

        } else {


            Dimension labelsRequiredDimension = getVerticalLabelsRequiredDimension(g2dTmp);
            int requiredLabelsHeight = (int) Math.ceil(labelsRequiredDimension.getHeight());

            double requiredWidth;
            double requiredHeight;


            if (ColorBarLayerType.VERTICAL_TITLE_TOP.equals(getTitleVerticalAnchor()) ||
                    ColorBarLayerType.VERTICAL_TITLE_BOTTOM.equals(getTitleVerticalAnchor())) {


                double colorBarAndLabelsRequiredWidth = getColorBarWidth() + getLabelGap() + tickOffset + labelsRequiredDimension.getWidth();

                requiredWidth = Math.max(headerRequiredDimension.getWidth(), colorBarAndLabelsRequiredWidth);

                requiredWidth = getTopBorderGap() + requiredWidth + getBottomBorderGap();


                double colorBarWithLabelsRequiredHeight = labelOverhangHeight + getColorBarLength() + labelOverhangHeight;


                requiredHeight = getLeftSideBorderGap() +
                        colorBarWithLabelsRequiredHeight +
                        getTitleGap() +
                        headerRequiredDimension.getHeight()
                        + getRightSideBorderGap();


                if (n > 1 && imageInfo.getColorPaletteDef().isDiscrete()) {
                    discreteBooster = labelsRequiredDimension.getHeight() / (n - 1);
                    requiredWidth += discreteBooster;
                }


                legendSize = new Dimension((int) requiredWidth, (int) requiredHeight);


            } else {

                requiredWidth = getTopBorderGap()
                        + getTitleHeight()
                        + getColorBarWidth()
                        + tickOffset
                        + getLabelGap()
                        + getLongestLabelWidth(g2dTmp)
                        + getTitleGap()
                        + getBottomBorderGap();


                double colorBarWithLabelsRequiredHeight = labelOverhangHeight + getColorBarLength() + labelOverhangHeight;

                requiredHeight = Math.max(colorBarWithLabelsRequiredHeight, headerRequiredDimension.getHeight());
                requiredHeight = getLeftSideBorderGap() + requiredHeight + getRightSideBorderGap();

                if (n > 1 && imageInfo.getColorPaletteDef().isDiscrete()) {
                    discreteBooster = labelsRequiredDimension.getHeight() / (n - 1);
                    requiredWidth += discreteBooster;
                }

                legendSize = new Dimension((int) requiredWidth, (int) requiredHeight);
            }


            if (ColorBarLayerType.VERTICAL_TITLE_TOP.equals(getTitleVerticalAnchor())) {
                paletteRect = new Rectangle(getTopBorderGap(),
                        getRightSideBorderGap() + labelOverhangHeight + (int) headerRequiredDimension.getHeight() + getTitleGap(),
                        getColorBarWidth(),
                        getColorBarLength());
                legendRect = new Rectangle(0, 0, legendSize.width - 1, legendSize.height - 1);


            } else if (ColorBarLayerType.VERTICAL_TITLE_BOTTOM.equals(getTitleVerticalAnchor())) {
                paletteRect = new Rectangle(getTopBorderGap(),
                        getRightSideBorderGap() + labelOverhangHeight,
                        getColorBarWidth(),
                        getColorBarLength());

                legendRect = new Rectangle(0, 0, legendSize.width - 1, legendSize.height - 1);


            } else if (ColorBarLayerType.VERTICAL_TITLE_LEFT.equals(getTitleVerticalAnchor())) {

                paletteRect = new Rectangle(getTopBorderGap() + headerRequiredDimension.width + getTitleGap(),
                        getRightSideBorderGap() + labelOverhangHeight,
                        getColorBarWidth(),
                        legendSize.height - getLeftSideBorderGap() - getRightSideBorderGap() - labelOverhangHeight - labelOverhangHeight);
                legendRect = new Rectangle(0, 0, legendSize.width - 1, legendSize.height - 1);


            } else { // VERTICAL_TITLE_RIGHT

                paletteRect = new Rectangle(getTopBorderGap(),
                        getRightSideBorderGap() + labelOverhangHeight,
                        getColorBarWidth(),
                        legendSize.height - getLeftSideBorderGap() - getRightSideBorderGap() - labelOverhangHeight - labelOverhangHeight);

                legendRect = new Rectangle(0, 0, legendSize.width - 1, legendSize.height - 1);

//                paletteRect = new Rectangle(getLeftSideBorderGap() + firstLabelOverhangWidth,
//                        getTopBorderGap() + requiredHeaderHeight + getTitleGap(),
//                        legendSize.width - getLeftSideBorderGap() - getRightSideBorderGap() - firstLabelOverhangWidth - lastLabelOverhangWidth,
//                        getColorBarWidth());

            }

            palettePosStart = paletteRect.y + paletteRect.height;
            palettePosEnd = paletteRect.y + (int) discreteBooster;
        }


        tickMarkShape = createTickMarkShape();
    }


    private boolean hasTitleParameter() {
        return isShowTitle() && StringUtils.isNotNullAndNotEmpty(titleText);
    }

    private boolean hasTitleUnits() {
        return isShowUnits() && StringUtils.isNotNullAndNotEmpty(unitsText);
    }

    private void draw(Graphics2D g2d) {
        fillBackground(g2d);
        drawPalette(g2d);
        drawHeaderText(g2d);

        if (isLabelsShow()) {
            drawLabelsAndTickMarks(g2d);
        }
    }

    private void fillBackground(Graphics2D g2d) {
        Color color = backdropColor;
        if (isAlphaUsed()) {
            color = new Color(color.getRed(), color.getGreen(), color.getBlue(), getBackgroundAlpha());
        }

        g2d.setColor(color);
        g2d.fillRect(0, 0, legendSize.width + 1, legendSize.height + 1);
    }


    private void initCoreGraphicSizes(Graphics2D g2d) {

        double vertical_gap_factor = 0.3;
        double horizontal_gap_factor = HORIZONTAL_TITLE_PARAMETER_UNITS_GAP_FACTOR;

        Font originalFont = g2d.getFont();

        g2d.setFont(getTitleParameterFont());


        String titleString;
        if (isTitleAltUse()) {
            titleString = getTitleAlt();
        } else {
            titleString = getTitleText();
        }
        Rectangle2D titleRectangle = g2d.getFontMetrics().getStringBounds(titleString, g2d);
        Rectangle2D titleSingleLetterRectangle = g2d.getFontMetrics().getStringBounds("A", g2d);

        AffineTransform transformOrig = g2d.getTransform();
        double origTransX = g2d.getTransform().getTranslateX();
        double origTransY = g2d.getTransform().getTranslateY();
        drawTitle(g2d, false);
        double titleTransX = g2d.getTransform().getTranslateX();
        double titleTransY = g2d.getTransform().getTranslateY();
//        System.out.println("origTransX = " + origTransX);
//        System.out.println("origTransY = " + origTransY);
//        System.out.println("titleTransX = " + titleTransX);
//        System.out.println("titleTransY = " + titleTransY);
        g2d.setTransform(transformOrig);

//        System.out.println("titleRectangle.getHeight() = " + titleRectangle.getHeight());
//        System.out.println("titleRectangle.getWidth() = " + titleRectangle.getWidth());


        double titleParameterHeight = titleRectangle.getHeight();
//        double titleParameterWidth = titleRectangle.getWidth();
        double titleParameterWidth = titleTransX - origTransX;
        double titleParameterSingleLetterWidth = titleSingleLetterRectangle.getWidth();
        titleParameterWidth = titleParameterWidth + 2 * titleParameterSingleLetterWidth; // add a buffer


        g2d.setFont(getTitleUnitsFont());
        String unitsString;
        if (isUnitsAltUse()) {
            unitsString = getUnitsAlt();
        } else {
            unitsString = getUnitsText();
        }

        if (unitsString != null && unitsString.length() > 0) {
            if (isUnitsParenthesis()) {
                unitsString = "(" + unitsString + ")";
            }
        }

        Rectangle2D unitsRectangle = g2d.getFontMetrics().getStringBounds(unitsString, g2d);
        Rectangle2D unitsSingleLetterRectangle = g2d.getFontMetrics().getStringBounds("A", g2d);


        drawUnits(g2d, false);
        double unitsTransX = g2d.getTransform().getTranslateX();
        double unitsTransY = g2d.getTransform().getTranslateY();
        g2d.setTransform(transformOrig);


        double titleUnitsHeight = unitsRectangle.getHeight();
//        double titleUnitsWidth = unitsRectangle.getWidth();
        double titleUnitsWidth = unitsTransX - origTransX;
        double titleUnitsSingleLetterWidth = unitsSingleLetterRectangle.getWidth();
        titleUnitsWidth = titleUnitsWidth + 2 * titleUnitsSingleLetterWidth;  // add a buffer


        double titleToUnitsVerticalGap = 0.0;
        double titleToUnitsHorizontalGap = 0.0;

        if (hasTitleParameter() && hasTitleUnits()) {
            titleToUnitsVerticalGap = vertical_gap_factor * titleUnitsHeight;
            titleToUnitsHorizontalGap = horizontal_gap_factor * titleUnitsSingleLetterWidth;
        }

        g2d.setFont(getTitleUnitsFont());
        Rectangle2D labelSingleLetterRectangle = g2d.getFontMetrics().getStringBounds("A", g2d);

        int headerGap = (int) Math.round(getTitleGapFactor() * getTitleFontSize());


        int sideBorderGap = (int) Math.round(getLeftSideBorderGapFactor() * getTitleFontSize());
        int rightSideBorderGap = (int) Math.round(getRightSideBorderGapFactor() * getTitleFontSize());
        int topBorderGap = (int) Math.round(getTopBorderGapFactor() * getTitleFontSize());
        int bottomBorderGap = (int) Math.round(getBottomBorderGapFactor() * getTitleFontSize());


        setTitleToUnitsVerticalGap(titleToUnitsVerticalGap);
        setTitleToUnitsHorizontalGap(titleToUnitsHorizontalGap);

        setTitleHeight(titleParameterHeight);
        setTitleWidth(titleParameterWidth);
        setTitleSingleLetterWidth(titleParameterSingleLetterWidth);

        setUnitsHeight(titleUnitsHeight);
        setUnitsWidth(titleUnitsWidth);
        setUnitsSingleLetterWidth(titleUnitsSingleLetterWidth);

        setLabelLongestWidth(getLongestLabelWidth(g2d));
        setLabelHeight(labelSingleLetterRectangle.getHeight());
        setLabelSingleLetterWidth(labelSingleLetterRectangle.getWidth());

        setTitleGap(headerGap);
        setLeftSideBorderGap(sideBorderGap);
        setRightSideBorderGap(rightSideBorderGap);
        setTopBorderGap(topBorderGap);
        setBottomBorderGap(bottomBorderGap);


        g2d.setFont(originalFont);
    }


    private Dimension getTitleRequiredDimension(boolean lineBreak, boolean rotate) {

        double width = 0;
        double height = 0;

        if (hasTitleParameter() && hasTitleUnits()) {
            if (lineBreak) {
                height = getTitleHeight() + getTitleToUnitsVerticalGap() + getUnitsHeight();
                width = Math.max(getTitleWidth(), getUnitsWidth());
            } else {
                height = Math.max(getTitleHeight(), getUnitsHeight());
                width = getTitleWidth() + getTitleToUnitsHorizontalGap() + getUnitsWidth();
            }
        } else if (hasTitleUnits()) {
            // hasTitleUnits only
            height = getUnitsHeight();
            width = getUnitsWidth();
        } else {
            // hasTitleParameter only
            height = getTitleHeight();
            width = getTitleWidth();
        }

        if (rotate) {
            return new Dimension((int) Math.ceil(height), (int) Math.ceil(width));
        } else {
            return new Dimension((int) Math.ceil(width), (int) Math.ceil(height));
        }
    }


    private Dimension getHorizontalLabelsRequiredDimension(Graphics2D g2d) {

        double width = 0;
        double height = 0;

        if (isLabelsShow() && colorBarInfos.size() > 0) {


            Font originalFont = g2d.getFont();
            g2d.setFont(getLabelsFont());

            double totalLabelsNoGapsWidth = 0;

            for (ColorBarInfo colorBarInfo : colorBarInfos) {
                Rectangle2D labelRectangle = g2d.getFontMetrics().getStringBounds(colorBarInfo.getFormattedValue(), g2d);
                totalLabelsNoGapsWidth += labelRectangle.getWidth();
            }

            Rectangle2D singleLetter = g2d.getFontMetrics().getStringBounds("A", g2d);
            double interLabelGap = (HORIZONTAL_INTER_LABEL_GAP_FACTOR * singleLetter.getWidth());

            double totalLabelGapsWidth = (colorBarInfos.size() - 1) * interLabelGap;

            width = totalLabelsNoGapsWidth + totalLabelGapsWidth;

            height = singleLetter.getHeight();

            g2d.setFont(originalFont);
        }

        return new Dimension((int) Math.ceil(width), (int) Math.ceil(height));
    }


    private Dimension getVerticalLabelsRequiredDimension(Graphics2D g2d) {

        double width = 0;
        double height = 0;

        if (isLabelsShow() && colorBarInfos.size() > 0) {

            Font originalFont = g2d.getFont();
            g2d.setFont(getLabelsFont());

            double totalLabelsNoGapHeight = 0;

            for (ColorBarInfo colorBarInfo : colorBarInfos) {
                Rectangle2D labelRectangle = g2d.getFontMetrics().getStringBounds(colorBarInfo.getFormattedValue(), g2d);
                totalLabelsNoGapHeight += labelRectangle.getHeight();
                width = Math.max(width, labelRectangle.getWidth());
            }

            Rectangle2D singleLetter = g2d.getFontMetrics().getStringBounds("A", g2d);
            double interLabelGap = (VERTICAL_INTER_LABEL_GAP_FACTOR * singleLetter.getHeight());

            double totalLabelGapsHeight = (colorBarInfos.size() - 1) * interLabelGap;

            height = totalLabelsNoGapHeight + totalLabelGapsHeight;

            g2d.setFont(originalFont);
        }

        return new Dimension((int) Math.ceil(width), (int) Math.ceil(height));
    }


    private double getLongestLabelWidth(Graphics2D g2d) {

        double width = 0;

        if (isLabelsShow() && colorBarInfos.size() > 0) {

            Font originalFont = g2d.getFont();
            g2d.setFont(getLabelsFont());

            for (ColorBarInfo colorBarInfo : colorBarInfos) {
                Rectangle2D labelRectangle = g2d.getFontMetrics().getStringBounds(colorBarInfo.getFormattedValue(), g2d);
                width = Math.max(width, labelRectangle.getWidth());
            }

            g2d.setFont(originalFont);
        }

        return width;
    }


    private Dimension getSingleLabelRequiredDimension(Graphics2D g2d, int colorBarInfoIndex) {

        double width = 0;
        double height = 0;

        if (colorBarInfos.size() > 0 && colorBarInfos.size() > colorBarInfoIndex) {
            Font originalFont = g2d.getFont();
            g2d.setFont(getLabelsFont());

            ColorBarInfo colorBarInfo = colorBarInfos.get(colorBarInfoIndex);
            Rectangle2D labelRectangle = g2d.getFontMetrics().getStringBounds(colorBarInfo.getFormattedValue(), g2d);
            width = labelRectangle.getWidth();
            height = labelRectangle.getHeight();


            g2d.setFont(originalFont);
        }

        return new Dimension((int) Math.ceil(width), (int) Math.ceil(height));
    }


    private void drawHeaderText(Graphics2D g2d) {
        if (hasTitleParameter() || hasTitleUnits()) {

            AffineTransform transformOrig = g2d.getTransform();

            Font origFont = g2d.getFont();
            Paint origPaint = g2d.getPaint();

            int x0 = paletteRect.x;
            int y0 = paletteRect.y;


            if (isHorizontalColorBar()) {
                double translateTitleX = x0;
                double translateTitleY = y0 - getTitleGap();

                g2d.translate(translateTitleX, translateTitleY);

                if (hasTitleParameter()) {
                    drawTitle(g2d, true);
                    g2d.translate(getTitleToUnitsHorizontalGap(), 0);
                }

                if (hasTitleUnits()) {
                    drawUnits(g2d, true);
                }
            } else {

                g2d.setFont(getTitleUnitsFont());
                Rectangle2D singleLetter = g2d.getFontMetrics().getStringBounds("A", g2d);
                int labelOverhangHeight = (int) Math.ceil(singleLetter.getHeight() / 2.0);

                double translateX;

                if (ColorBarLayerType.VERTICAL_TITLE_TOP.equals(getTitleVerticalAnchor())) {
                    translateX = x0;

                } else if (ColorBarLayerType.VERTICAL_TITLE_BOTTOM.equals(getTitleVerticalAnchor())) {
                    translateX = x0;

                } else if (ColorBarLayerType.VERTICAL_TITLE_LEFT.equals(getTitleVerticalAnchor())) {
                    translateX = x0 - getTopBorderGap() - 0.5 * getTitleHeight();
                    translateX = x0 - getTitleGap();


                } else { // VERTICAL_TITLE_RIGHT
//                    translateX = x0
//                            + getTopBorderGap()
//                            + 0.5 * getTitleHeight()
//                            + getColorBarWidth()
//                            + getTickmarkLength()
//                            + getLabelGap()
//                            + getLongestLabelWidth(g2d)
//                            + getTitleGap();

                    translateX = x0
                            + getTitleHeight()
                            + getTitleGap()
                            + getColorBarWidth()
                            + getTickmarkLength()
                            + getLabelGap()
                            + getLongestLabelWidth(g2d);

                }


                double translateY;


                if (ColorBarLayerType.VERTICAL_TITLE_TOP.equals(getTitleVerticalAnchor()) ||
                        ColorBarLayerType.VERTICAL_TITLE_BOTTOM.equals(getTitleVerticalAnchor())) {

                    if (ColorBarLayerType.VERTICAL_TITLE_TOP.equals(getTitleVerticalAnchor())) {
                        if (hasTitleParameter() && hasTitleUnits()) {
                            translateY = y0 - getTitleGap() - labelOverhangHeight - getUnitsHeight() - 0.5 * getTitleHeight() - getTitleToUnitsVerticalGap();
                        } else {
                            translateY = y0 - getTitleGap() - labelOverhangHeight - 0.5 * getTitleHeight();
                        }
                    } else {
                        translateY = y0 + getColorBarLength() + labelOverhangHeight + getTitleGap() + 0.5 * getTitleHeight();
                    }

                    g2d.translate(translateX, translateY);

                    if (hasTitleParameter()) {
                        drawTitle(g2d, true);
                        g2d.translate(0, getTitleToUnitsVerticalGap());
                    }

                    if (hasTitleUnits()) {
                        drawUnits(g2d, true);
                    }



                } else if (ColorBarLayerType.VERTICAL_TITLE_BOTTOM.equals(getTitleVerticalAnchor())) {
                    translateY = y0;

                } else { // VERTICAL_TITLE_RIGHT || VERTICAL_TITLE_LEFT
                    translateY = y0 + getColorBarLength();
//                    translateY = y0 + paletteRect.height - getBorderGap() - labelOverhangHeight - 500;
                    double width1 = getColorBarLength();
                    double width2 = getTitleWidth() + getTitleToUnitsHorizontalGap() + getUnitsWidth() - getRightSideBorderGap();
                    translateY = y0 + Math.max(width1, width2);

                    // todo Danny   fixing offset






                    double rotate = -Math.PI / 2.0;
                    g2d.translate(translateX, translateY);

                    if (hasTitleParameter()) {
                        g2d.rotate(rotate);
                        drawTitle(g2d, true);

                        g2d.translate(getTitleToUnitsHorizontalGap(), 0);
                        g2d.rotate(-rotate);
                    }

                    if (hasTitleUnits()) {
                        g2d.rotate(rotate);
                        drawUnits(g2d, true);
                        g2d.rotate(-rotate);
                    }
                }
            }


            // Restore font graphics
            g2d.setPaint(origPaint);
            g2d.setFont(origFont);

            g2d.setTransform(transformOrig);
        }
    }



    private void drawTitle(Graphics2D g2d, boolean draw) {
        Font origFont = g2d.getFont();
        Paint origPaint = g2d.getPaint();

        g2d.setFont(getTitleParameterFont());
        g2d.setPaint(getTitleColor());

        String titleString;
        if (isTitleAltUse()) {
            titleString = getTitleAlt();
        } else {
            titleString = getTitleText();
        }

        String description = raster.getDescription();
        String bandname = raster.getName();
        String units = raster.getUnit();
        float wavelength = raster.getProduct().getBand(raster.getName()).getSpectralWavelength();
        boolean allowWavelengthZero = true;

        titleString = ColorSchemeInfo.getColorBarTitle(titleString, bandname, description, wavelength, units, allowWavelengthZero);

        drawHeaderSubMethod(g2d, titleString, draw, isConvertCaret());

        g2d.setFont(origFont);
        g2d.setPaint(origPaint);
    }


    private void drawUnits(Graphics2D g2d, boolean draw) {
        Font origFont = g2d.getFont();
        Paint origPaint = g2d.getPaint();

        g2d.setFont(getTitleUnitsFont());
        g2d.setPaint(getUnitsColor());

        g2d.setFont(getTitleUnitsFont());
        String unitsString;
        if (isUnitsAltUse()) {
            unitsString = getUnitsAlt();
        } else {
            unitsString = getUnitsText();
        }

        if (unitsString != null && unitsString.length() > 0) {
            if (isUnitsParenthesis()) {
                unitsString = "(" + unitsString + ")";
            }
        }

        String description = raster.getDescription();
        String bandname = raster.getName();
        String units = raster.getUnit();
        float wavelength = raster.getProduct().getBand(raster.getName()).getSpectralWavelength();
        boolean allowWavelengthZero = true;

        unitsString = ColorSchemeInfo.getColorBarTitle(unitsString, bandname, description, wavelength, units, allowWavelengthZero);


        if (isUnitsParenthesis() && unitsString != null) {
            if (unitsString.trim().startsWith("(") && unitsString.trim().endsWith(")")) {
                // it already has parenthesis so leave it alone
            } else {
                unitsString = "("+ unitsString + ")";
            }
        }

        drawHeaderSubMethod(g2d, unitsString, draw, isConvertCaret());

        g2d.setFont(origFont);
        g2d.setPaint(origPaint);
    }




    private void drawHeaderSubMethod(Graphics2D g2d, String headerString, boolean draw, boolean convertCaret) {

//        double wave = getRaster().getProduct().getBand(getRaster().getName()).getSpectralWavelength();
//        String waveString = Double.toString(wave);
//        if (wave > 0) {
//            unitsString = getUnitsText() + " wave=" + waveString;
//        }

        Font origFont = g2d.getFont();

        int openParenthesisStartedSuper = 0;
        boolean currentIdxIsSuperScript = false;  // indicates whether current idx is a superscript
        boolean currentIdxIsSubScript = false;  // indicates whether current idx is a superscript
        boolean containsSuperScript = false;
        boolean italicsOverride = false;
        boolean boldOverride = false;
        boolean prevIdxNormal = true; // used to determine if subscript or superscript immediately follow normal
        boolean caratAwaitingEntry = false;

        if ((headerString.contains("^") && convertCaret) || headerString.contains("[sup]") || headerString.contains("[sub]")) {
            containsSuperScript = true;
        }

        for (int idx = 0; idx < headerString.length(); idx++) {
            boolean ignoreThisIdx = false;

            String charStringCurrent = headerString.substring(idx, idx + 1);
            char charCurrent = headerString.charAt(idx);

            if (charStringCurrent.equals("^") && convertCaret) {
                currentIdxIsSuperScript = true;
                caratAwaitingEntry = true;
                ignoreThisIdx = true;
            }

            if (isStartSuperScript(headerString, idx)) {
                ignoreThisIdx = true;
                currentIdxIsSuperScript = true;
            }

            if (isEndSuperScript(headerString, idx)) {
                ignoreThisIdx = true;
                currentIdxIsSuperScript = false;
            }

            if (isStartSubScript(headerString, idx)) {
                ignoreThisIdx = true;
                currentIdxIsSubScript = true;
            }

            if (isEndSubScript(headerString, idx)) {
                ignoreThisIdx = true;
                currentIdxIsSubScript = false;
            }


            if (isStartItalics(headerString, idx)) {
                ignoreThisIdx = true;
                italicsOverride = true;
            }

            if (isEndItalics(headerString, idx)) {
                ignoreThisIdx = true;
                italicsOverride = false;
            }

            if (isStartBold(headerString, idx)) {
                ignoreThisIdx = true;
                boldOverride = true;
            }

            if (isEndBold(headerString, idx)) {
                ignoreThisIdx = true;
                boldOverride = false;
            }


            if (!ignoreThisIdx) {
                if (Character.isWhitespace(charCurrent)) {
                    if (openParenthesisStartedSuper <= 0 && !caratAwaitingEntry) {
                        currentIdxIsSuperScript = false;
                    }
                } else {
                    if (caratAwaitingEntry) {
                        caratAwaitingEntry = false;
                    }
                }

                if (charStringCurrent.equals("(")) {
                    if (currentIdxIsSuperScript) {
                        openParenthesisStartedSuper++;
                    }
                }

                if (charStringCurrent.equals(")")) {
                    if (currentIdxIsSuperScript) {
                        if (openParenthesisStartedSuper > 0) {
                            openParenthesisStartedSuper--;
                        } else {
                            currentIdxIsSuperScript = false;
                        }
                    }
                }

                if (charStringCurrent.equals("(") || charStringCurrent.equals(")")) {
                    if (!currentIdxIsSuperScript && containsSuperScript) {
                        int parenthesisFontSize = (int) Math.ceil(g2d.getFont().getSize() * 1.2);
                        Font parenthesisFont = new Font(g2d.getFont().getName(), g2d.getFont().getStyle(), parenthesisFontSize);
                        g2d.setFont(parenthesisFont);
                    }
                }


                if (italicsOverride) {
                    int fontType = ColorBarLayer.getFontType(true, g2d.getFont().isBold());
                    Font italicsFont = new Font(g2d.getFont().getName(), fontType, g2d.getFont().getSize());
                    g2d.setFont(italicsFont);
                }

                if (boldOverride) {
                    int fontType = ColorBarLayer.getFontType(g2d.getFont().isItalic(), true);
                    Font boldFont = new Font(g2d.getFont().getName(), fontType, g2d.getFont().getSize());
                    g2d.setFont(boldFont);
                }

                FONT_SCRIPT font_script;
                if (currentIdxIsSuperScript) {
                    font_script = FONT_SCRIPT.SUPER_SCRIPT;
                } else if (currentIdxIsSubScript) {
                    font_script = FONT_SCRIPT.SUBSCRIPT;
                } else {
                    font_script = FONT_SCRIPT.NORMAL;
                }

                // give a little space in front of subscript or superscript
                if ((currentIdxIsSuperScript || currentIdxIsSubScript) && prevIdxNormal) {
                        Rectangle2D singleLetter = g2d.getFontMetrics().getStringBounds("A", g2d);
                        double translateUnitsX = singleLetter.getWidth() * (0.1);
                        g2d.translate(translateUnitsX, 0);
                }

                drawHeaderSingleChar(g2d, charStringCurrent, font_script, true);

                g2d.setFont(origFont);

                if ((currentIdxIsSuperScript || currentIdxIsSubScript)) {
                    prevIdxNormal = false;
                } else {
                    prevIdxNormal = true;
                }

            }
        }
    }


    private boolean isStartSubScript(String text, int idx) {
        return isStringOnIndex(text, idx, "[sub]") || isStringOnIndex(text, idx, "<sub>");
    }
    private boolean isEndSubScript(String text, int idx) {
        return isStringOnIndex(text, idx, "[/sub]") || isStringOnIndex(text, idx, "</sub>");
    }

    private boolean isStartSuperScript(String text, int idx) {
        return isStringOnIndex(text, idx, "[sup]") || isStringOnIndex(text, idx, "<sup>") ||
                isStringOnIndex(text, idx, "[super]") || isStringOnIndex(text, idx, "<super>");
    }
    private boolean isEndSuperScript(String text, int idx) {
        return isStringOnIndex(text, idx, "[/sup]") || isStringOnIndex(text, idx, "</sup>") ||
                isStringOnIndex(text, idx, "[/super]") || isStringOnIndex(text, idx, "</super>");
    }

    private boolean isStartItalics(String text, int idx) {
        return isStringOnIndex(text, idx, "[i]") || isStringOnIndex(text, idx, "<i>");
    }
    private boolean isEndItalics(String text, int idx) {
       return isStringOnIndex(text, idx, "[/i]") || isStringOnIndex(text, idx, "</i>");
    }

    private boolean isStartBold(String text, int idx) {
        return isStringOnIndex(text, idx, "[b]") || isStringOnIndex(text, idx, "<b>");
    }
    private boolean isEndBold(String text, int idx) {
        return isStringOnIndex(text, idx, "[/b]") || isStringOnIndex(text, idx, "</b>");
    }




    private boolean isStringOnIndex(String text, int idx, String subtext) {

        if (text == null || subtext == null) {
            return false;
        }

        int offset = 0;

        for (int i=subtext.length(); i > 0; i--) {
            if (text.length() >= idx + i && idx >= offset) {
                String charStringCurrent = text.substring(idx - offset, idx + i);
                if (charStringCurrent.equals(subtext)) {
                    return true;
                }
            }

            offset++;
        }

        return false;
    }




    private void drawHeaderSingleChar(Graphics2D g2d, String text, FONT_SCRIPT fontScript, boolean draw) {

        double translateX = 0;
        double translateY = 0;

        if (fontScript == FONT_SCRIPT.NORMAL) {
            if (draw) {
                g2d.drawString(text, 0, 0);
            }

            Rectangle2D textRectangle = g2d.getFontMetrics().getStringBounds(text, g2d);
            translateX = textRectangle.getWidth();
            g2d.translate(translateX, 0);
            return;
        }

        Font fontOrig = g2d.getFont();

        int fontSize;
        if (fontScript == FONT_SCRIPT.SUPER_SCRIPT) {
//            int superScriptHeight = (int) Math.ceil(singleLetter.getHeight() * 0.3);
            int superScriptHeight = (int) Math.ceil(g2d.getFont().getSize() * 0.3);

            translateY = -superScriptHeight;
            fontSize = (int) Math.ceil(g2d.getFont().getSize() * 0.75);
        } else { // it is subscript
//            int subScriptHeight = (int) Math.ceil(singleLetter.getHeight() * 0.1);
            int subScriptHeight = (int) Math.ceil(g2d.getFont().getSize() * 0.2);
            translateY = subScriptHeight;
            fontSize = (int) Math.ceil(g2d.getFont().getSize() * 0.75);
        }

        g2d.translate(0, translateY);

        Font superScriptFont = new Font(g2d.getFont().getName(), g2d.getFont().getStyle(), fontSize);
        g2d.setFont(superScriptFont);

        if (draw) {
            g2d.drawString(text, 0, 0);
        }

        Rectangle2D textRectangle = g2d.getFontMetrics().getStringBounds(text, g2d);
        translateX = textRectangle.getWidth();
        translateY = -translateY;

        g2d.translate(translateX, translateY);

        g2d.setFont(fontOrig);
    }


    private void drawPalette(Graphics2D g2d) {

        final Color[] palette = ImageManager.createColorPalette(getRaster().getImageInfo());

        Stroke originalStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(1));


        if (isHorizontalColorBar()) {
            int xStart = paletteRect.x;
            int xEnd = paletteRect.x + paletteRect.width;
            int y1 = paletteRect.y;
            int y2 = paletteRect.y + paletteRect.height;

            int divisor = palettePosEnd - palettePosStart;
            int palIndex;

            if (isReversePalette()) {
                for (int x = xEnd; x > xStart; x--) {

                    if (divisor == 0) {
                        palIndex = x < palettePosStart ? 0 : palette.length - 1;
                    } else {
                        palIndex = Math.round((palette.length * (palettePosEnd - x)) / divisor);
                    }

                    drawLineInHorizontalPalette(g2d, palette, palIndex, x, y1, y2);
                }
            } else {
                for (int x = xStart; x < xEnd; x++) {
                    if (divisor == 0) {
                        palIndex = x < palettePosStart ? 0 : palette.length - 1;
                    } else {
                        palIndex = Math.round((palette.length * (x - palettePosStart)) / divisor);
                    }

                    drawLineInHorizontalPalette(g2d, palette, palIndex, x, y1, y2);
                }
            }
        } else {
            int x1 = paletteRect.x;
            int x2 = paletteRect.x + paletteRect.width;
            int yStart = paletteRect.y + paletteRect.height;
            int yEnd = paletteRect.y;

            int divisor = Math.abs(palettePosEnd - palettePosStart);
            int palIndex;

            if (isReversePalette()) {
                for (int y = yEnd; y < yStart; y++) {

                    if (divisor == 0) {
                        palIndex = y < palettePosStart ? 0 : palette.length - 1;
                    } else {
                        palIndex = Math.round((palette.length * (y - palettePosEnd)) / divisor);
                    }

                    drawLineInVerticalPalette(g2d, palette, palIndex, x1, x2, y);
                }
            } else {
                for (int y = yStart; y > yEnd; y--) {

                    if (divisor == 0) {
                        palIndex = y < palettePosStart ? 0 : palette.length - 1;
                    } else {
                        palIndex = Math.round((palette.length * (palettePosStart - y)) / divisor);
                    }

                    drawLineInVerticalPalette(g2d, palette, palIndex, x1, x2, y);
                }
            }
        }


        if (isBorderShow()) {
            g2d.setColor(getBorderColor());
            g2d.setStroke(new BasicStroke(getBorderWidth()));
            g2d.draw(paletteRect);
        }

        if (isBackdropBorderShow()) {
            g2d.setColor(getBackdropBorderColor());
            g2d.setStroke(new BasicStroke(getBackdropBorderWidth()));
            g2d.draw(legendRect);
        }

        g2d.setStroke(originalStroke);
    }


    private void drawLineInHorizontalPalette(Graphics2D g2d, Color[] palette, int index, int x, int y1, int y2) {
        index = forceIndexInBounds(palette, index);

        g2d.setColor(palette[index]);
        g2d.drawLine(x, y1, x, y2);
    }


    private void drawLineInVerticalPalette(Graphics2D g2d, Color[] palette, int index, int x1, int x2, int y) {
        index = forceIndexInBounds(palette, index);

        g2d.setColor(palette[index]);
        g2d.drawLine(x1, y, x2, y);
    }


    private int forceIndexInBounds(Color[] palette, int index) {
        if (index < 0) {
            index = 0;
        }
        if (index > palette.length - 1) {
            index = palette.length - 1;
        }

        return index;
    }


    private void drawLabelsAndTickMarks(Graphics2D g2d) {

        Color originalColor = g2d.getColor();
        Stroke originalStroke = g2d.getStroke();
        Color originalPaint = (Color) g2d.getPaint();
        Font originalFont = g2d.getFont();

        g2d.setFont(getLabelsFont());
        g2d.setPaint(getLabelsColor());

        Stroke tickMarkStroke = new BasicStroke(getTickmarkWidth());
        g2d.setStroke(tickMarkStroke);

        double translateX, translateY;

        int tickMarkOverHang;
        if (Math.floor((getTickmarkWidth()) / 2) == (getTickmarkWidth()) / 2) {
            // even
            tickMarkOverHang = (int) Math.floor((getTickmarkWidth()) / 2);
        } else {
            // odd
            tickMarkOverHang = (int) Math.floor((getTickmarkWidth() - 1) / 2);
        }

        for (ColorBarInfo colorBarInfo : colorBarInfos) {
            String formattedValue = colorBarInfo.getFormattedValue();
            double weight = colorBarInfo.getLocationWeight();

            double tickMarkRelativePosition = weight * (palettePosEnd - palettePosStart);

            if (isHorizontalColorBar()) {
                translateY = paletteRect.y + paletteRect.height;

                if (isReversePalette()) {
                    translateX = palettePosEnd - tickMarkRelativePosition;
                    translateX = keepTickMarkInBoundsHorizontal(translateX, tickMarkOverHang);
                } else {
                    translateX = palettePosStart + tickMarkRelativePosition;
                    translateX = keepTickMarkInBoundsHorizontal(translateX, tickMarkOverHang);
                }
            } else {
                translateX = paletteRect.x + paletteRect.width;

                if (isReversePalette()) {
                    translateY = palettePosEnd - tickMarkRelativePosition;
                    translateY = keepTickMarkInBoundsVertical(translateY, tickMarkOverHang);
                } else {
                    translateY = palettePosStart + tickMarkRelativePosition;
                    translateY = keepTickMarkInBoundsVertical(translateY, tickMarkOverHang);
                }
            }


            g2d.translate(translateX, translateY);

            if (isTickmarkShow()) {
                g2d.setPaint(getTickmarkColor());
                g2d.draw(tickMarkShape);
            }

            final FontMetrics fontMetrics = g2d.getFontMetrics();
            int labelWidth = fontMetrics.stringWidth(formattedValue);
            int labelHeight = fontMetrics.getHeight();

            float x0, y0;
            int tickOffset = 0;

            if (getTickmarkLength() > 0) {
                tickOffset = getTickmarkLength();
            }


            if (isHorizontalColorBar()) {
                x0 = -0.5f * labelWidth;
                y0 = getLabelGap() + tickOffset + fontMetrics.getMaxAscent();
            } else {
                x0 = getLabelGap() + tickOffset;
                y0 = -0.5f * labelHeight + fontMetrics.getMaxAscent();
            }

            g2d.setPaint(getLabelsColor());
            g2d.drawString(formattedValue, x0, y0);
            g2d.translate(-translateX, -translateY);
        }

        g2d.setFont(originalFont);
        g2d.setColor(originalColor);
        g2d.setStroke(originalStroke);
        g2d.setPaint(originalPaint);
    }


    private double keepTickMarkInBoundsHorizontal(double translate, int tickMarkOverHang) {
        // make sure end tickmarks are placed within palette
        // tickmark hardcoded at 3 width will have 1 tickMarkOverHang

        if (translate <= (palettePosStart + tickMarkOverHang)) {
            translate = (palettePosStart + tickMarkOverHang);
        }

        if (translate >= (palettePosEnd - tickMarkOverHang)) {
            translate = (palettePosEnd - tickMarkOverHang);
        }

        return translate;
    }


    private double keepTickMarkInBoundsVertical(double translate, int tickMarkOverHang) {
        // make sure end tickmarks are placed within palette
        // tickmark hardcoded at 3 width will have 1 tickMarkOverHang

        if (translate >= (palettePosStart - tickMarkOverHang)) {
            translate = (palettePosStart - tickMarkOverHang);
        }

        if (translate <= (palettePosEnd + tickMarkOverHang)) {
            translate = (palettePosEnd + tickMarkOverHang);
        }

        return translate;
    }


    public static double getLinearWeightFromLinearValue(double linearValue, double min, double max) {

        // Prevent extrapolation which could occur due to machine roundoffs in the calculations
        if (linearValue == min) {
            return 0;
        }
        if (linearValue == max) {
            return 1;
        }

        double linearWeight = (linearValue - min) / (max - min);

        // Prevent UNEXPECTED interpolation/extrapolation which could occur due to machine roundoffs in the calculations
        if (linearValue > min && linearWeight < 0) {
            return 0;
        }
        if (linearValue < max && linearWeight > 1) {
            return 1;
        }
        if (linearValue < min && linearWeight >= 0) {
            return 0 - FORCED_CHANGE_FACTOR;
        }
        if (linearValue > max && linearWeight <= 1) {
            return 1 + FORCED_CHANGE_FACTOR;
        }
        return linearWeight;
    }


    public static double getLinearWeightFromLogValue(double logValue, double min, double max) {

        // Prevent extrapolation which could occur due to machine roundoffs in the calculations
        if (logValue == min) {
            return 0;
        }
        if (logValue == max) {
            return 1;
        }

        double b = Math.log(max / min) / (max - min);
        double a = min / (Math.exp(b * min));
        double linearValue = Math.log(logValue / a) / b;
        double linearWeight = (linearValue - min) / (max - min);

        // Prevent UNEXPECTED interpolation/extrapolation which could occur due to machine roundoffs in the calculations
        if (logValue > min && linearWeight < 0) {
            return 0;
        }
        if (logValue < max && linearWeight > 1) {
            return 1;
        }
        if (logValue < min && linearWeight >= 0) {
            return 0 - FORCED_CHANGE_FACTOR;
        }
        if (logValue > max && linearWeight <= 1.0) {
            return 1 + FORCED_CHANGE_FACTOR;
        }

        return linearWeight;
    }


    public static double getLinearValueUsingLinearWeight(double linearWeight, double min, double max) {

        // Prevent extrapolation which could occur due to machine roundoffs in the calculations
        if (linearWeight == 0) {
            return min;
        }
        if (linearWeight == 1) {
            return max;
        }

        double deltaNormalized = (max - min);
        double linearValue = min + linearWeight * (deltaNormalized);

        // Prevent UNEXPECTED interpolation/extrapolation which could occur due to machine roundoffs in the calculations
        if (linearWeight > 0 && linearValue < min) {
            return min;
        }
        if (linearWeight < 1 && linearValue > max) {
            return max;
        }
        if (linearWeight < 0 && linearValue >= min) {
            return min - (max - min) * FORCED_CHANGE_FACTOR;
        }
        if (linearWeight > 1 && linearValue <= max) {
            return max + (max - min) * FORCED_CHANGE_FACTOR;
        }

        return linearValue;
    }


    public static double getLogarithmicValueUsingLinearWeight(double weight, double min, double max) {

        double linearValue = getLinearValueUsingLinearWeight(weight, min, max);

        // Prevent extrapolation which could occur due to machine roundoffs in the calculations
        if (linearValue == min) {
            return min;
        }
        if (linearValue == max) {
            return max;
        }

        double b = Math.log(max / min) / (max - min);
        double a = min / (Math.exp(b * min));
        double logValue = a * Math.exp(b * linearValue);

        // Prevent UNEXPECTED interpolation/extrapolation which could occur due to machine roundoffs in the calculations
        if (linearValue > min && logValue < min) {
            return min;
        }
        if (linearValue < max && logValue > max) {
            return max;
        }
        if (linearValue < min && logValue >= min) {
            return min - (max - min) * FORCED_CHANGE_FACTOR;
        }
        if (linearValue > max && logValue <= max) {
            return max + (max - min) * FORCED_CHANGE_FACTOR;
        }

        return logValue;
    }


    private double normalizeSample(double sample) {
        final double minDisplaySample = getRaster().scaleInverse(getImageInfo().getColorPaletteDef().getMinDisplaySample());
        final double maxDisplaySample = getRaster().scaleInverse(getImageInfo().getColorPaletteDef().getMaxDisplaySample());
        sample = getRaster().scaleInverse(sample);
        double delta = maxDisplaySample - minDisplaySample;
        if (delta == 0 || Double.isNaN(delta)) {
            delta = 1;
        }
        return (sample - minDisplaySample) / delta;
    }

    private Shape createTickMarkShape() {
        GeneralPath path = new GeneralPath();
        if (isHorizontalColorBar()) {
            path.moveTo(0.0F, 1.0F * getTickmarkLength());
            path.lineTo(0.0F, 0.0F);
        } else {
            path.moveTo(0.0F, 0.0F);
            path.lineTo(1.0F * getTickmarkLength(), 0.0F);
        }
        path.closePath();
        return path;
    }

    private int getNumGradationCurvePoints() {
        return getImageInfo().getColorPaletteDef().getNumPoints();
    }

    private ColorPaletteDef.Point getGradationCurvePointAt(int index) {
        return getImageInfo().getColorPaletteDef().getPointAt(index);
    }

    private static int adjust(int size, final int blockSize) {
        return blockSize * (size / blockSize) + (size % blockSize == 0 ? 0 : blockSize);
    }

    private FontMetrics createFontMetrics() {
        BufferedImage bi = createBufferedImage(32, 32);
        final Graphics2D g2d = bi.createGraphics();
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        g2d.dispose();
        return fontMetrics;
    }

    private BufferedImage createBufferedImage(final int width, final int height) {
        return new BufferedImage(width, height,
                isAlphaUsed() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
    }


    public String getCustomLabelValues() {
        return customLabelValues;
    }

    public void setCustomLabelValues(String customLabelValues) {
        this.customLabelValues = customLabelValues;
    }


    public Font getTitleParameterFont() {
        return new Font(getTitleFontName(), getTitleFontType(), getTitleFontSize());
    }

    public Font getTitleUnitsFont() {
        return new Font(getUnitsFontName(), getUnitsFontType(), getUnitsFontSize());
    }


    public Font getLabelsFont() {
        return new Font(getLabelsFontName(), getLabelsFontType(), getLabelsFontSize());
    }


    public int getTitleFontSize() {
        return titleFontSize;
    }

    public void setTitleFontSize(int titleFontSize) {
        this.titleFontSize = titleFontSize;
    }

    public int getUnitsFontSize() {
        return unitsFontSize;
    }

    public void setUnitsFontSize(int unitsFontSize) {
        this.unitsFontSize = unitsFontSize;
    }

    public int getLabelsFontSize() {
        return labelsFontSize;
    }

    public void setLabelsFontSize(int labelsFontSize) {
        this.labelsFontSize = labelsFontSize;
    }

    public double getScalingFactor() {
        return scalingFactor;
    }

    public void setScalingFactor(double scalingFactor) {
        this.scalingFactor = scalingFactor;
    }

    public int getColorBarLength() {
        return colorBarLength;
    }

    public void setColorBarLength(int colorBarLength) {
        this.colorBarLength = colorBarLength;
    }

    public int getColorBarWidth() {
        return colorBarWidth;
    }

    public void setColorBarWidth(int colorBarThickness) {
        this.colorBarWidth = colorBarThickness;
    }

    public double getLayerScaling() {
        return layerScaling;
    }

    public void setLayerScaling(double layerScaling) {
        this.layerScaling = layerScaling;
    }



    public double getLeftSideBorderGapFactor() {
            return leftSideBorderGapFactor;
    }

    public void setLeftSideBorderGapFactor(double leftSideBorderGapFactor) {
        this.leftSideBorderGapFactor = leftSideBorderGapFactor;
    }


    public double getRightSideBorderGapFactor() {
        return rightSideBorderGapFactor;
    }

    public void setRightSideBorderGapFactor(double rightSideBorderGapFactor) {
        this.rightSideBorderGapFactor = rightSideBorderGapFactor;
    }

    public double getTopBorderGapFactor() {
        return topBorderGapFactor;
    }

    public void setTopBorderGapFactor(double topBorderGapFactor) {
        this.topBorderGapFactor = topBorderGapFactor;
    }

    public double getBottomBorderGapFactor() {
        return bottomBorderGapFactor;
    }
    public void setBottomBorderGapFactor(double bottomBorderGapFactor) {
        this.bottomBorderGapFactor = bottomBorderGapFactor;
    }

    public double getTitleGapFactor() {
        return titleGapFactor;
    }
    public void setTitleGapFactor(double titleGapFactor) {
        this.titleGapFactor = titleGapFactor;
    }


    public double getLabelGapFactor() {
        return labelGapFactor;
    }
    public void setLabelGapFactor(double labelGapFactor) {
        this.labelGapFactor = labelGapFactor;
    }


    public int getLeftSideBorderGap() {
//        if (leftSideBorderGap != NULL_INT) {
//            return leftSideBorderGap;
//        } else {
            return (int) Math.round(getLeftSideBorderGapFactor() * getTitleFontSize());
//        }
    }

    public void setLeftSideBorderGap(int leftSideBorderGap) {
        this.leftSideBorderGap = leftSideBorderGap;
    }

    public int getRightSideBorderGap() {
//        if (rightSideBorderGap != NULL_INT) {
//            return rightSideBorderGap;
//        } else {
            return (int) Math.round(getRightSideBorderGapFactor() * getTitleFontSize());
//        }
    }

    public void setRightSideBorderGap(int rightSideBorderGap) {
        this.rightSideBorderGap = rightSideBorderGap;
    }

    public int getTopBorderGap() {
//        if (topBorderGap != NULL_INT) {
//            return topBorderGap;
//        } else {
            return (int) Math.round(getTopBorderGapFactor() * getTitleFontSize());
//        }
    }

    public void setTopBorderGap(int topBorderGap) {
        this.topBorderGap = topBorderGap;
    }

    public int getBottomBorderGap() {
//        if (bottomBorderGap != NULL_INT) {
//            return bottomBorderGap;
//        } else {
            return (int) Math.round(getBottomBorderGapFactor() * getTitleFontSize());
//        }
    }

    public void setBottomBorderGap(int bottomeBorderGap) {
        this.bottomBorderGap = bottomeBorderGap;
    }



    public int getLabelGap() {
//        if (labelGap != NULL_INT) {
//            return labelGap;
//        } else {
//            return (int) Math.round(getLabelGapFactor() * getLabelsFontSize());
            return (int) Math.round(getLabelGapFactor() * getTitleFontSize());
//        }
    }


    public void setLabelGap(int labelGap) {
        this.labelGap = labelGap;
    }

    public int getTitleGap() {
//        if (titleGap != NULL_INT) {
//            return titleGap;
//        } else {
            return (int) Math.round(getTitleGapFactor() * getTitleFontSize());
//        }
    }

    public void setTitleGap(int titleGap) {
        this.titleGap = titleGap;
    }


    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public boolean isDecimalPlacesForce() {
        return decimalPlacesForce;
    }

    public void setDecimalPlacesForce(boolean decimalPlacesForce) {
        this.decimalPlacesForce = decimalPlacesForce;
    }

    public void distributeEvenly() {

        final double min = getImageInfo().getColorPaletteDef().getMinDisplaySample();
        final double max = getImageInfo().getColorPaletteDef().getMaxDisplaySample();

        double value;
        double weight;

        colorBarInfos.clear();

        if (getTickMarkCount() >= 2) {
            ArrayList<String> manualPointsArrayList = new ArrayList<>();
            double normalizedDelta = (1.0 / (getTickMarkCount() - 1.0));

            for (int i = 0; i < getTickMarkCount(); i++) {

                weight = i * normalizedDelta;
                if (imageInfo.isLogScaled()) {
                    value = getLogarithmicValueUsingLinearWeight(weight, min, max);
                } else {
                    value = getLinearValueUsingLinearWeight(weight, min, max);
                }


                // Apply tolerance to potentially bring weight into valid state
                weight = getValidWeight(weight);

                if (weight >= 0 && weight <= 1) {
                    if (getScalingFactor() != 0) {
                        value = value * getScalingFactor();
                        ColorBarInfo colorBarInfo = new ColorBarInfo(value, weight, getDecimalPlaces(), isDecimalPlacesForce());

                        double valueScaledFormatted = Double.valueOf(colorBarInfo.getFormattedValue());
                        double minScaled = min * getScalingFactor();
                        double maxScaled = max * getScalingFactor();

                        double weightScaled;
                        if (imageInfo.isLogScaled()) {
                            weightScaled = getLinearWeightFromLogValue(valueScaledFormatted, minScaled, maxScaled);
                        } else {
                            weightScaled = getLinearWeightFromLinearValue(valueScaledFormatted, minScaled, maxScaled);
                        }

                        // Apply tolerance to potentially bring weight into valid state
                        weightScaled = getValidWeight(weightScaled);

                        if (weightScaled >= 0 && weightScaled <= 1) {
                            // adjust weight to match formatted string
                            colorBarInfo.setLocationWeight(weightScaled);

                            colorBarInfos.add(colorBarInfo);
                            manualPointsArrayList.add(colorBarInfo.getFormattedValue());
                        }

                    }
                }
            }
            if (manualPointsArrayList.size() > 0) {
                String manualPoints = StringUtils.join(manualPointsArrayList, ", ");
                setCustomLabelValues(manualPoints);
            }
        }
    }

    public String getTitleOverRide() {
        return titleOverRide;
    }

    public void setTitleOverRide(String titleOverRide) {
        this.titleOverRide = titleOverRide;
    }


    public boolean allowTitleOverride(PropertyMap configuration) {
        if (configuration != null) {
            return configuration.getPropertyBool(PROPERTY_NAME_COLORBAR_TITLE_OVERRIDE, DEFAULT_COLORBAR_TITLE_OVERRIDE);
        } else {
            return DEFAULT_COLORBAR_TITLE_OVERRIDE;
        }
    }

    public boolean allowLabelsOverride(PropertyMap configuration) {
        if (configuration != null) {
            return configuration.getPropertyBool(PROPERTY_NAME_COLORBAR_LABELS_OVERRIDE, DEFAULT_COLORBAR_LABELS_OVERRIDE);
        } else {
            return DEFAULT_COLORBAR_LABELS_OVERRIDE;
        }
    }

    public static boolean allowColorbarAutoReset(PropertyMap configuration) {
        if (configuration != null) {
            return configuration.getPropertyBool(PROPERTY_NAME_COLORBAR_ALLOW_RESET, DEFAULT_COLORBAR_ALLOW_RESET);
        } else {
            return DEFAULT_COLORBAR_ALLOW_RESET;
        }
    }


    public Color getTickmarkColor() {
        return tickmarkColor;
    }

    public void setTickmarkColor(Color tickmarkColor) {
        this.tickmarkColor = tickmarkColor;
    }

    public Color getLabelsColor() {
        return labelsColor;
    }

    public void setLabelsColor(Color labelsColor) {
        this.labelsColor = labelsColor;
    }

    public Color getTitleColor() {
        return titleColor;
    }

    public void setTitleColor(Color color) {
        this.titleColor = color;
    }

    public int getTickmarkLength() {
        if (tickmarkLength != NULL_INT) {
            return tickmarkLength;
        } else {
            return (int) Math.round(0.8 * getLabelGap());
        }
    }

    public void setTickmarkLength(int tickmarkLength) {
        this.tickmarkLength = tickmarkLength;
    }

    public int getTickmarkWidth() {
        if (tickmarkWidth != NULL_INT) {
            return tickmarkWidth;
        } else {
            return (int) Math.round(0.8 * getLabelGap());
        }
    }

    public void setTickmarkWidth(int tickmarkWidth) {
        this.tickmarkWidth = tickmarkWidth;
    }


    public boolean isTickmarkShow() {
        return tickmarkShow;
    }

    public void setTickmarkShow(boolean tickmarkShow) {
        this.tickmarkShow = tickmarkShow;
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    public boolean isBorderShow() {
        return borderShow;
    }

    public void setBorderShow(boolean borderShow) {
        this.borderShow = borderShow;
    }

    public boolean isBackdropShow() {
        return backdropShow;
    }

    public void setBackdropShow(boolean backdropShow) {
        this.backdropShow = backdropShow;
    }

    public String getLabelsFontName() {
        return labelsFontName;
    }

    public void setLabelsFontName(String labelsFontName) {
        this.labelsFontName = labelsFontName;
    }

    public int getLabelsFontType() {
        return labelsFontType;
    }

    public void setLabelsFontType(int labelsFontType) {
        this.labelsFontType = labelsFontType;
    }

    public Color getUnitsColor() {
        return unitsColor;
    }

    public void setUnitsColor(Color unitsColor) {
        this.unitsColor = unitsColor;
    }

    public boolean isShowUnits() {
        return showUnits;
    }

    public void setShowUnits(boolean showUnits) {
        this.showUnits = showUnits;
    }

    public String getTitleFontName() {
        return titleFontName;
    }

    public void setTitleFontName(String titleFontName) {
        this.titleFontName = titleFontName;
    }

    public int getTitleFontType() {
        return titleFontType;
    }

    public void setTitleFontType(int titleFontType) {
        this.titleFontType = titleFontType;
    }

    public String getUnitsFontName() {
        return unitsFontName;
    }

    public void setUnitsFontName(String unitsFontName) {
        this.unitsFontName = unitsFontName;
    }

    public int getUnitsFontType() {
        return unitsFontType;
    }

    public void setUnitsFontType(int unitsFontType) {
        this.unitsFontType = unitsFontType;
    }

    public boolean isLabelsShow() {
        return labelsShow;
    }

    public void setLabelsShow(boolean labelsShow) {
        this.labelsShow = labelsShow;
    }


    public double getSceneAspectBestFit() {
        return sceneAspectBestFit;
    }

    public void setSceneAspectBestFit(double sceneAspectBestFit) {
        this.sceneAspectBestFit = sceneAspectBestFit;
    }

    public boolean isReversePalette() {
        return reversePalette;
    }

    public void setReversePalette(boolean reversePalette) {
        this.reversePalette = reversePalette;
    }

    public String getTitleVerticalAnchor() {
        return titleVerticalAnchor;
    }

    public void setTitleVerticalAnchor(String titleVerticalAnchor) {
        this.titleVerticalAnchor = titleVerticalAnchor;
    }


    private double getTitleToUnitsVerticalGap() {
        return titleToUnitsVerticalGap;
    }

    private void setTitleToUnitsVerticalGap(double titleToUnitsVerticalGap) {
        this.titleToUnitsVerticalGap = titleToUnitsVerticalGap;
    }


    public double getTitleToUnitsHorizontalGap() {
        return titleToUnitsHorizontalGap;
    }

    public void setTitleToUnitsHorizontalGap(double titleToUnitsHorizontalGap) {
        this.titleToUnitsHorizontalGap = titleToUnitsHorizontalGap;
    }

    public double getTitleHeight() {
        return titleHeight;
    }

    public void setTitleHeight(double titleHeight) {
        this.titleHeight = titleHeight;
    }

    public double getTitleWidth() {
        return titleWidth;
    }

    public void setTitleWidth(double titleWidth) {
        this.titleWidth = titleWidth;
    }

    public double getTitleSingleLetterWidth() {
        return titleSingleLetterWidth;
    }

    public void setTitleSingleLetterWidth(double titleSingleLetterWidth) {
        this.titleSingleLetterWidth = titleSingleLetterWidth;
    }

    public double getUnitsHeight() {
        return unitsHeight;
    }

    public void setUnitsHeight(double unitsHeight) {
        this.unitsHeight = unitsHeight;
    }

    public double getUnitsWidth() {
        return unitsWidth;
    }

    public void setUnitsWidth(double unitsWidth) {
        this.unitsWidth = unitsWidth;
    }

    public double getUnitsSingleLetterWidth() {
        return unitsSingleLetterWidth;
    }

    public void setUnitsSingleLetterWidth(double unitsSingleLetterWidth) {
        this.unitsSingleLetterWidth = unitsSingleLetterWidth;
    }


    public double getLabelHeight() {
        return labelHeight;
    }

    public void setLabelHeight(double labelHeight) {
        this.labelHeight = labelHeight;
    }

    public double getLabelLongestWidth() {
        return labelLongestWidth;
    }

    public void setLabelLongestWidth(double labelLongestWidth) {
        this.labelLongestWidth = labelLongestWidth;
    }

    public double getLabelSingleLetterWidth() {
        return labelSingleLetterWidth;
    }

    public void setLabelSingleLetterWidth(double labelSingleLetterWidth) {
        this.labelSingleLetterWidth = labelSingleLetterWidth;
    }

    public Color getBackdropBorderColor() {
        return backdropBorderColor;
    }

    public void setBackdropBorderColor(Color backdropBorderColor) {
        this.backdropBorderColor = backdropBorderColor;
    }

    public int getBackdropBorderWidth() {
        return backdropBorderWidth;
    }

    public void setBackdropBorderWidth(int backdropBorderWidth) {
        this.backdropBorderWidth = backdropBorderWidth;
    }

    public boolean isBackdropBorderShow() {
        return backdropBorderShow;
    }

    public void setBackdropBorderShow(boolean backdropBorderShow) {
        this.backdropBorderShow = backdropBorderShow;
    }

    public double getWeightTolerance() {
        return weightTolerance;
    }

    public void setWeightTolerance(double weightTolerance) {
        this.weightTolerance = weightTolerance;
    }
}
