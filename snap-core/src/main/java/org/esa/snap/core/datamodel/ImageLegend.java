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

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;


/**
 * The <code>ImageLegend</code> class is used to generate an image legend from a <code>{@link
 * ImageInfo}</code> instance.
 *
 * @author Norman Fomferra
 * @author Daniel Knowles
 * @version $Revision$ $Date$
 */
// MAY2021 - Daniel Knowles - Major revisions to Color Bar

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

    public static final int DEFAULT_COLOR_BAR_LENGTH = 1200;
    public static final int DEFAULT_PREVIEW_LENGTH_PIXELS = 750;

    public static final double HORIZONTAL_TITLE_PARAMETER_UNITS_GAP_FACTOR = 2;
    public static final double HORIZONTAL_INTER_LABEL_GAP_FACTOR = 3;
    public static final double VERTICAL_INTER_LABEL_GAP_FACTOR = 0.75;

    public static final double WEIGHT_TOLERANCE = 0.0001;  // machine error can make calculated weight slightly outside of range 0-1
    public static final double FORCED_CHANGE_FACTOR = 0.0001;
    public static final double INVALID_WEIGHT = -1.0;

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
    private String titleText;
    private String unitsText;
    private int orientation;
    private String distributionType;
    private int tickMarkCount;


    private String titleVerticalAnchor;
    private boolean reversePalette = true;

    private Color tickmarkColor;
    private int tickmarkLength = NULL_INT;
    private int tickmarkWidth = NULL_INT;
    private boolean tickmarkShow;

    private int borderWidth = 5;
    private Color borderColor = Color.RED;
    private boolean borderShow = true;

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
    private int colorBarThickness;
    private double layerScaling;

    private String titleOverRide = null;

    private int borderGap = NULL_INT;   // TITLE_TO_PALETTE_GAP
    private int labelGap = NULL_INT;      // LABEL_TO_COLORBAR BORDER_GAP9
    private int titleGap = NULL_INT;      // HEADER_TO_COLORBAR BORDER_GAP

    // Dependent, internal attributes
    private Rectangle paletteRect;
    private Rectangle legendRect;
    private Dimension legendSize;
    private Shape tickMarkShape;
    private int palettePosStart;
    private int palettePosEnd;
    private ArrayList<ColorBarInfo> colorBarInfos = new ArrayList<ColorBarInfo>();


    public ImageLegend(ImageInfo imageInfo, RasterDataNode raster) {
        this.imageInfo = imageInfo;
        this.raster = raster;
        showTitle = true;
        titleText = "";

        orientation = HORIZONTAL;
        backdropColor = Color.white;
        tickmarkColor = ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT;
        labelsColor = ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_DEFAULT;
        titleColor = ColorBarLayerType.PROPERTY_TITLE_COLOR_DEFAULT;
        unitsColor = ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_DEFAULT;

        setTickmarkLength(ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_DEFAULT);
        setTickmarkWidth(ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_DEFAULT);
        setTickmarkShow(ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_DEFAULT);

        setLabelsFontSize((Integer) 35);
        setLabelsFontName(ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_DEFAULT);

        backdropTransparency = 1.0f;
        antialiasing = true;
        setDecimalPlaces(2);
        scalingFactor = 1;

        setDecimalPlacesForce(false);
        setCustomLabelValues("");
    }


    public ImageLegend getCopyOfImageLegend() {

        ImageLegend imageLegendCopy = new ImageLegend(raster.getImageInfo(), raster);

        imageLegendCopy.setOrientation(getOrientation());
        imageLegendCopy.setReversePalette(isReversePalette());

        imageLegendCopy.setShowTitle(isShowTitle());
        imageLegendCopy.setTitleText(getTitleText());
        imageLegendCopy.setTitleFontSize(getTitleFontSize());
        imageLegendCopy.setTitleColor(getTitleColor());
        imageLegendCopy.setTitleFontName(getTitleFontName());
        imageLegendCopy.setTitleFontType(getTitleFontType());

        imageLegendCopy.setShowUnits(isShowUnits());
        imageLegendCopy.setUnitsText(getUnitsText());
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
        imageLegendCopy.setColorBarThickness(getColorBarThickness());

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

        return imageLegendCopy;
    }


    public void initLegendWithPreferences(PropertyMap configuration, RasterDataNode raster) {


        // Orientation Parameters

        String orientationString = configuration.getPropertyString(ColorBarLayerType.PROPERTY_ORIENTATION_KEY,
                ColorBarLayerType.PROPERTY_ORIENTATION_DEFAULT);

        if (ColorBarLayerType.OPTION_VERTICAL.equals(orientationString)) {
            setOrientation(ImageLegend.VERTICAL);
        } else {
            setOrientation(ImageLegend.HORIZONTAL);
        }

        setReversePalette(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_KEY,
                ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_DEFAULT));


        // Label Distribution and Values

        setTickMarkCount(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_DEFAULT));

        if (imageInfo.getColorPaletteDef().isDiscrete()) {
            setDistributionType(configuration.getPropertyString(ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_KEY,
                    ColorBarLayerType.DISTRIB_EXACT_STR));
        } else {
            setDistributionType(configuration.getPropertyString(ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_KEY,
                    ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_DEFAULT));
        }

        setCustomLabelValues(configuration.getPropertyString(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_DEFAULT));

        setScalingFactor(configuration.getPropertyDouble(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_DEFAULT));

        setDecimalPlaces(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_DEFAULT));

        setDecimalPlacesForce(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_DEFAULT));


        // Sizing and Location
        setColorBarLength(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_LEGEND_LENGTH_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_LENGTH_DEFAULT));

        setColorBarThickness(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_LEGEND_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_WIDTH_DEFAULT));

        setTitleVerticalAnchor(configuration.getPropertyString(ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_DEFAULT));


        // Title parameters

        setShowTitle(
                configuration.getPropertyBool(ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY,
                        ColorBarLayerType.PROPERTY_TITLE_SHOW_DEFAULT));


        String titleTextDefault = configuration.getPropertyString(ColorBarLayerType.PROPERTY_TITLE_TEXT_KEY,
                ColorBarLayerType.PROPERTY_TITLE_TEXT_DEFAULT);

        String titleText = (ColorBarLayerType.NULL_SPECIAL.equals(titleTextDefault)) ? raster.getName() : titleTextDefault;

        setTitleText(titleText);


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


        String unitsTextDefault = configuration.getPropertyString(ColorBarLayerType.PROPERTY_UNITS_TEXT_KEY,
                ColorBarLayerType.PROPERTY_UNITS_TEXT_DEFAULT);


        String unitsText = "";
        if (ColorBarLayerType.NULL_SPECIAL.equals(unitsTextDefault)) {
            String unit = raster.getUnit();
            if (unit != null && unit.length() > 0) {
                unitsText = "(" + raster.getUnit() + ")";
            }
        } else {
            unitsText = unitsTextDefault;
        }


        setUnitsText(unitsText);


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
                ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_DEFAULT));

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

    public String getTitleText() {
        return (titleText == null) ? "null-test" : titleText;
        // todo possible Color Bar Scheme could go here in the future
//        if (!isInitialized() && titleOverRide != null && titleOverRide.length() > 0) {
//            return titleOverRide;
//        } else {
//            return headerText;
//        }
//        return headerText;
    }

    public void setTitleText(String titleText) {
        this.titleText = titleText;
    }

    public String getUnitsText() {
        if (unitsText == null || unitsText.length() == 0) {
            return "";
        } else {
            return unitsText;
        }
    }

    public void setUnitsText(String unitsText) {
        this.unitsText = unitsText;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

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
            if (orientation == HORIZONTAL) {
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
        int tmpColorBarThickness = getColorBarThickness();

        setLabelsFontSize((int) Math.round(scalingFactor * getLabelsFontSize()));
        setTickmarkLength((int) Math.round(scalingFactor * getTickmarkLength()));
        setTickmarkWidth((int) Math.round(scalingFactor * getTickmarkWidth()));
        setTitleFontSize((int) Math.round(scalingFactor * getTitleFontSize()));
        setUnitsFontSize((int) Math.round(scalingFactor * getUnitsFontSize()));
        setColorBarLength((int) Math.round(scalingFactor * getColorBarLength()));
        setColorBarThickness((int) Math.round(scalingFactor * getColorBarThickness()));

        BufferedImage bufferedImage = createImage();

        setLabelsFontSize(tmpLabelsFontSize);
        setTickmarkLength(tmpTickmarkLength);
        setTickmarkWidth(tmpTickmarkWidth);
        setTitleFontSize(tmpTitleFontSize);
        setUnitsFontSize(tmpTitleUnitsFontSize);
        setColorBarLength(tmpColorBarLength);
        setColorBarThickness(tmpColorBarThickness);

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


        if (DISTRIB_EXACT_STR.equals(getDistributionType()) || imageInfo.getColorPaletteDef().isDiscrete()) {
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
                weight = getValidWeight(weight);
                if (weight != INVALID_WEIGHT) {
                    if (getScalingFactor() != 0) {
                        value = value * getScalingFactor();
                        ColorBarInfo colorBarInfo = new ColorBarInfo(value, weight, getDecimalPlaces(), isDecimalPlacesForce());
                        colorBarInfos.add(colorBarInfo);
                        manualPointsArrayList.add(colorBarInfo.getFormattedValue());
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


                    for (String formattedValue : formattedValues) {
                        if (formattedValue != null) {
                            formattedValue.trim();
                            if (formattedValue.length() > 0 && scalingFactor != 0) {

                                String[] valueAndString = formattedValue.split(":");
                                if (valueAndString.length == 2) {
                                    value = Double.valueOf(valueAndString[0]) / getScalingFactor();
                                    formattedValue = valueAndString[1];
                                } else {
                                    value = Double.valueOf(formattedValue) / getScalingFactor();
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
                                if (weight != INVALID_WEIGHT) {
//                                System.out.println("TEST formattedValue=" + formattedValue);
//                                System.out.println("TEST weight=" + weight);
                                    ColorBarInfo colorBarInfo = new ColorBarInfo(value, weight, formattedValue);
                                    colorBarInfos.add(colorBarInfo);
                                }
                            }
                        }
                    }
                }
            }

    }

    private double getValidWeight(double weight) {
        // due to rounding issues we want to make sure the weight isn't just below 0 or just above 1
        // this would cause the tick mark to possibly be placed a very tiny amount outside of the colorbar if not corrected here

        boolean valid = (weight >= (0 - WEIGHT_TOLERANCE) && weight <= (1 + WEIGHT_TOLERANCE))
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
            return INVALID_WEIGHT;
        }

    }


    private void initDrawing() {

        final BufferedImage bufferedImage = createBufferedImage(100, 100);
        final Graphics2D g2dTmp = bufferedImage.createGraphics();

        initCoreGraphicSizes(g2dTmp);


        g2dTmp.setFont(getLabelsFont());

        Dimension headerRequiredDimension;

        if (orientation == HORIZONTAL) {
            headerRequiredDimension = getTitleRequiredDimension(false, false);
        } else {
            if (ColorBarLayerType.VERTICAL_TITLE_TOP.equals(getTitleVerticalAnchor()) ||
                    ColorBarLayerType.VERTICAL_TITLE_BOTTOM.equals(getTitleVerticalAnchor())) {
                headerRequiredDimension = getTitleRequiredDimension(true, false);

            } else {
                headerRequiredDimension = getTitleRequiredDimension(false, true);
            }
        }

//        System.out.println("Title required width =" + headerRequiredDimension.width);
//        System.out.println("Title required height =" + headerRequiredDimension.height);
//        System.out.println("Title =" + getTitleText());
//        System.out.println("Title units =" + getUnitsText());

        double discreteBooster = 0;
        final int n = getNumGradationCurvePoints();

        double firstLabelWidth = getSingleLabelRequiredDimension(g2dTmp, 0).getWidth();
        int firstLabelOverhangWidth = (int) Math.ceil(firstLabelWidth / 2.0);

        double lastLabelWidth = getSingleLabelRequiredDimension(g2dTmp, colorBarInfos.size() - 1).getWidth();
        int lastLabelOverhangWidth = (int) Math.ceil(lastLabelWidth / 2.0);

        double firstLabelHeight = getSingleLabelRequiredDimension(g2dTmp, 0).getHeight();
        int labelOverhangHeight = (int) Math.ceil(firstLabelHeight / 2.0);


        if (orientation == HORIZONTAL) {


            Dimension labelsRequiredDimension = getHorizontalLabelsRequiredDimension(g2dTmp);

            double colorBarWithLabelsRequiredLength = firstLabelOverhangWidth + getColorBarLength() + lastLabelOverhangWidth;

            double requiredWidth = Math.max(colorBarWithLabelsRequiredLength,
                    headerRequiredDimension.getWidth());

            requiredWidth = getBorderGap() + requiredWidth + getBorderGap();


            if (n > 1 && imageInfo.getColorPaletteDef().isDiscrete()) {
                discreteBooster = labelsRequiredDimension.getWidth() / (n - 1);
                requiredWidth += discreteBooster;
            }


            int requiredHeaderHeight = (int) Math.ceil(headerRequiredDimension.getHeight());
            int requiredLabelsHeight = (int) Math.ceil(labelsRequiredDimension.getHeight());


            int requiredHeight = getBorderGap()
                    + requiredHeaderHeight
                    + getTitleGap()
                    + getColorBarThickness()
                    + getLabelGap()
                    + requiredLabelsHeight
                    + getBorderGap();


            legendSize = new Dimension((int) requiredWidth, requiredHeight);


            paletteRect = new Rectangle(getBorderGap() + firstLabelOverhangWidth,
                    getBorderGap() + requiredHeaderHeight + getTitleGap(),
                    legendSize.width - getBorderGap() - getBorderGap() - firstLabelOverhangWidth - lastLabelOverhangWidth,
                    getColorBarThickness());

            legendRect = new Rectangle(0, 0, legendSize.width - 1, legendSize.height - 1);


            int paletteGap = 0;
            palettePosStart = paletteRect.x + paletteGap;
            palettePosEnd = paletteRect.x + paletteRect.width - (int) discreteBooster;

            // todo a piece of the old beam code, see what adjust does
            //   Math.max(_MIN_LEGEND_WIDTH, adjust(legendWidth, 16));

        } else {


            Dimension labelsRequiredDimension = getVerticalLabelsRequiredDimension(g2dTmp);
            int requiredLabelsHeight = (int) Math.ceil(labelsRequiredDimension.getHeight());

            double requiredWidth;
            double requiredHeight;


            if (ColorBarLayerType.VERTICAL_TITLE_TOP.equals(getTitleVerticalAnchor()) ||
                    ColorBarLayerType.VERTICAL_TITLE_BOTTOM.equals(getTitleVerticalAnchor())) {


                double colorBarAndLabelsRequiredWidth = getColorBarThickness() + getLabelGap() + labelsRequiredDimension.getWidth();

                requiredWidth = Math.max(headerRequiredDimension.getWidth(), colorBarAndLabelsRequiredWidth);

                requiredWidth = getBorderGap() + requiredWidth + getBorderGap();


                double colorBarWithLabelsRequiredHeight = labelOverhangHeight + getColorBarLength() + labelOverhangHeight;


                requiredHeight = getBorderGap() +
                        colorBarWithLabelsRequiredHeight +
                        getTitleGap() +
                        headerRequiredDimension.getHeight()
                        + getBorderGap();


                if (n > 1 && imageInfo.getColorPaletteDef().isDiscrete()) {
                    discreteBooster = labelsRequiredDimension.getHeight() / (n - 1);
                    requiredWidth += discreteBooster;
                }


                legendSize = new Dimension((int) requiredWidth, (int) requiredHeight);


            } else {

                requiredWidth = getBorderGap()
                        + getTitleHeight()
                        + getColorBarThickness()
                        + getTickmarkLength()
                        + getLabelGap()
                        + getLongestLabelWidth(g2dTmp)
                        + getTitleGap()
                        + getBorderGap();


                double colorBarWithLabelsRequiredHeight = labelOverhangHeight + getColorBarLength() + labelOverhangHeight;

                requiredHeight = Math.max(colorBarWithLabelsRequiredHeight, headerRequiredDimension.getHeight());
//                requiredHeight = Math.max(requiredHeight, getColorBarLength());
//            requiredHeight = Math.max(requiredHeight, MIN_VERTICAL_COLORBAR_HEIGHT);
                requiredHeight = getBorderGap() + requiredHeight + getBorderGap();

                //todo Danny changed this to make legend size stable

                if (n > 1 && imageInfo.getColorPaletteDef().isDiscrete()) {
                    discreteBooster = labelsRequiredDimension.getHeight() / (n - 1);
                    requiredWidth += discreteBooster;
                }

//                requiredHeight = getColorBarLength();


                legendSize = new Dimension((int) requiredWidth, (int) requiredHeight);
            }


            if (ColorBarLayerType.VERTICAL_TITLE_TOP.equals(getTitleVerticalAnchor())) {
                paletteRect = new Rectangle(getBorderGap(),
                        getBorderGap() + labelOverhangHeight + (int) headerRequiredDimension.getHeight() + getTitleGap(),
                        getColorBarThickness(),
                        getColorBarLength());
                legendRect = new Rectangle(0, 0, legendSize.width - 1, legendSize.height - 1);


            } else if (ColorBarLayerType.VERTICAL_TITLE_BOTTOM.equals(getTitleVerticalAnchor())) {
                paletteRect = new Rectangle(getBorderGap(),
                        getBorderGap() + labelOverhangHeight,
                        getColorBarThickness(),
                        getColorBarLength());
                legendRect = new Rectangle(0, 0, legendSize.width - 1, legendSize.height - 1);


            } else if (ColorBarLayerType.VERTICAL_TITLE_LEFT.equals(getTitleVerticalAnchor())) {
                paletteRect = new Rectangle(getBorderGap() + headerRequiredDimension.width + getTitleGap(),
                        getBorderGap() + labelOverhangHeight,
                        getColorBarThickness(),
                        getColorBarLength());
                legendRect = new Rectangle(0, 0, legendSize.width - 1, legendSize.height - 1);


            } else { // VERTICAL_TITLE_RIGHT
                paletteRect = new Rectangle(getBorderGap(),
                        getBorderGap() + labelOverhangHeight,
                        getColorBarThickness(),
                        getColorBarLength());
                legendRect = new Rectangle(0, 0, legendSize.width - 1, legendSize.height - 1);


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
//        if (isBackdropShow()) {
        fillBackground(g2d);
//        }


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
//        if (getBackgroundTransparency() == 1.0) {
//             color = UIManager.getColor("Panel.background");
//        }
        g2d.setColor(color);
        g2d.fillRect(0, 0, legendSize.width + 1, legendSize.height + 1);
    }


    private void initCoreGraphicSizes(Graphics2D g2d) {

        double vertical_gap_factor = 0.3;
        double horizontal_gap_factor = HORIZONTAL_TITLE_PARAMETER_UNITS_GAP_FACTOR;

        Font originalFont = g2d.getFont();

        g2d.setFont(getTitleParameterFont());
        Rectangle2D titleRectangle = g2d.getFontMetrics().getStringBounds(getTitleText(), g2d);
        Rectangle2D titleSingleLetterRectangle = g2d.getFontMetrics().getStringBounds("A", g2d);

        double titleParameterHeight = titleRectangle.getHeight();
        double titleParameterWidth = titleRectangle.getWidth();
        double titleParameterSingleLetterWidth = titleSingleLetterRectangle.getWidth();

        g2d.setFont(getTitleUnitsFont());
        Rectangle2D unitsRectangle = g2d.getFontMetrics().getStringBounds(getUnitsText(), g2d);
        Rectangle2D unitsSingleLetterRectangle = g2d.getFontMetrics().getStringBounds("A", g2d);

        double titleUnitsHeight = unitsRectangle.getHeight();
        double titleUnitsWidth = unitsRectangle.getWidth();
        double titleUnitsSingleLetterWidth = unitsSingleLetterRectangle.getWidth();

        double titleToUnitsVerticalGap = 0.0;
        double titleToUnitsHorizontalGap = 0.0;

        if (hasTitleParameter() && hasTitleUnits()) {
            titleToUnitsVerticalGap = vertical_gap_factor * titleUnitsHeight;
            titleToUnitsHorizontalGap = horizontal_gap_factor * titleUnitsSingleLetterWidth;
        }

        g2d.setFont(getTitleUnitsFont());
        Rectangle2D labelSingleLetterRectangle = g2d.getFontMetrics().getStringBounds("A", g2d);

        int headerGap = (int) Math.round(0.5 * getTitleFontSize());
        int borderGap = (int) Math.round(0.3 * getTitleFontSize());


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
        setBorderGap(borderGap);


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
            Font origFont = g2d.getFont();
            Paint origPaint = g2d.getPaint();

            int x0 = paletteRect.x;
            int y0 = paletteRect.y;


            if (orientation == HORIZONTAL) {
                double translateTitleX = x0;
                double translateTitleY = y0 - getTitleGap();

                double translateUnitsX = 0;
                double translateUnitsY = 0;

                g2d.translate(translateTitleX, translateTitleY);


                if (hasTitleParameter()) {
                    g2d.setFont(getTitleParameterFont());
                    g2d.setPaint(getTitleColor());
                    g2d.drawString(titleText, 0, 0);

                    translateUnitsX = getTitleWidth() + getTitleToUnitsHorizontalGap();
                }


                g2d.translate(translateUnitsX, translateUnitsY);

                if (hasTitleUnits()) {
                    g2d.setFont(getTitleUnitsFont());
                    g2d.setPaint(getUnitsColor());
                    g2d.drawString(getUnitsText(), 0, 0);
                }


                g2d.translate(-translateUnitsX, -translateUnitsY);
                g2d.translate(-translateTitleX, -translateTitleY);


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
                    translateX = x0 - getTitleGap() - 0.5 * getTitleHeight();


                } else { // VERTICAL_TITLE_RIGHT
                    translateX = x0
                            + 0.5 * getTitleHeight()
                            + getColorBarThickness()
                            + getTickmarkLength()
                            + getLabelGap()
                            + getLongestLabelWidth(g2d)
                            + getTitleGap();

                }


                double translateY;


                if (ColorBarLayerType.VERTICAL_TITLE_TOP.equals(getTitleVerticalAnchor()) ||
                        ColorBarLayerType.VERTICAL_TITLE_BOTTOM.equals(getTitleVerticalAnchor())) {

                    double translateY2 = 0;


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
                        g2d.setFont(getTitleParameterFont());
                        g2d.setPaint(getTitleColor());
                        g2d.drawString(titleText, 0, 0);
                        translateY2 = getTitleHeight() + getTitleToUnitsVerticalGap();
                    }


                    g2d.translate(0, translateY2);

                    if (hasTitleUnits()) {
                        g2d.setFont(getTitleUnitsFont());
                        g2d.setPaint(getUnitsColor());
                        g2d.drawString(getUnitsText(), 0, 0);
                    }

                    g2d.translate(0, -translateY2);
                    g2d.translate(-translateX, -translateY);


                } else if (ColorBarLayerType.VERTICAL_TITLE_BOTTOM.equals(getTitleVerticalAnchor())) {
                    translateY = y0;

                } else { // VERTICAL_TITLE_RIGHT || VERTICAL_TITLE_LEFT
                    translateY = y0 + getColorBarLength();
                    double translateY2 = 0;


                    double rotate = -Math.PI / 2.0;
                    g2d.translate(translateX, translateY);


                    if (hasTitleParameter()) {
                        g2d.rotate(rotate);
                        g2d.setFont(getTitleParameterFont());
                        g2d.setPaint(getTitleColor());
                        g2d.drawString(titleText, 0, 0);
                        g2d.rotate(-rotate);

                        translateY2 = -getTitleWidth() - getTitleToUnitsHorizontalGap();
                    }


                    g2d.translate(0, translateY2);

                    if (hasTitleUnits()) {
                        g2d.rotate(rotate);
                        g2d.setFont(getTitleUnitsFont());
                        g2d.setPaint(getUnitsColor());
                        g2d.drawString(getUnitsText(), 0, 0);
                        g2d.rotate(-rotate);
                    }

                    g2d.translate(0, -translateY2);
                    g2d.translate(-translateX, -translateY);

                }


            }


            // Restore font graphics
            g2d.setPaint(origPaint);
            g2d.setFont(origFont);
        }
    }


    private void drawPalette(Graphics2D g2d) {

        final Color[] palette = ImageManager.createColorPalette(getRaster().getImageInfo());

        Stroke originalStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(1));


        if (orientation == HORIZONTAL) {
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

            if (orientation == HORIZONTAL) {
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
            if (orientation == HORIZONTAL) {
                x0 = -0.5f * labelWidth;
                y0 = getLabelGap() + fontMetrics.getMaxAscent();
            } else {
                x0 = getLabelGap();
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
        if (orientation == HORIZONTAL) {
            path.moveTo(0.0F, 0.7F * getTickmarkLength());
            path.lineTo(0.0F, 0.0F);
        } else {
            path.moveTo(0.0F, 0.0F);
            path.lineTo(0.7F * getTickmarkLength(), 0.0F);
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
//        if (font != null) {
//            g2d.setFont(font);
//        }
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

    public int getColorBarThickness() {
        return colorBarThickness;
    }

    public void setColorBarThickness(int colorBarThickness) {
        this.colorBarThickness = colorBarThickness;
    }

    public double getLayerScaling() {
        return layerScaling;
    }

    public void setLayerScaling(double layerScaling) {
        this.layerScaling = layerScaling;
    }


    public int getBorderGap() {
        if (borderGap != NULL_INT) {
            return borderGap;
        } else {
            return (int) Math.round(0.3 * getLabelsFontSize());
        }
    }

    public void setBorderGap(int borderGap) {
        this.borderGap = borderGap;
    }

    public int getLabelGap() {
        if (labelGap != NULL_INT) {
            return labelGap;
        } else {
            return (int) Math.round(0.3 * getLabelsFontSize());
        }
    }

    public void setLabelGap(int labelGap) {
        this.labelGap = labelGap;
    }

    public int getTitleGap() {
        if (titleGap != NULL_INT) {
            return titleGap;
        } else {
            return (int) Math.round(0.5 * getTitleFontSize());
        }
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

        double value, weight;
        double roundedValue, adjustedWeight;
        colorBarInfos.clear();

        if (getTickMarkCount() >= 2) {
            ArrayList<String> manualPointsArrayList = new ArrayList<>();
            double normalizedDelta = (1.0 / (getTickMarkCount() - 1.0));

            for (int i = 0; i < getTickMarkCount(); i++) {

                weight = i * normalizedDelta;
                double linearValue = getLinearValueUsingLinearWeight(weight, min, max);
                if (imageInfo.isLogScaled()) {
                    value = getLogarithmicValueUsingLinearWeight(weight, min, max);
//                        roundedValue = round(value,decimalPlaces-1);
//                        adjustedWeight = getLinearWeightFromLogValue(roundedValue, min, max);
                } else {
                    value = getLinearValueUsingLinearWeight(weight, min, max);
//                        roundedValue = round(linearValue,decimalPlaces);
//                        adjustedWeight = getLinearWeightFromLinearValue(roundedValue, min, max);
                }


                // todo try to make some kind of rounding thing work
                roundedValue = value;
                adjustedWeight = weight;

                adjustedWeight = getValidWeight(adjustedWeight);
                if (adjustedWeight != INVALID_WEIGHT) {
                    if (getScalingFactor() != 0) {
                        roundedValue = roundedValue * getScalingFactor();
                        ColorBarInfo colorBarInfo = new ColorBarInfo(roundedValue, adjustedWeight, getDecimalPlaces(), isDecimalPlacesForce());

                        double newValue = Double.valueOf(colorBarInfo.getFormattedValue()) / getScalingFactor();
                        double newWeight;
                        if (imageInfo.isLogScaled()) {
                            newWeight = getLinearWeightFromLogValue(newValue, min, max);
                        } else {
                            newWeight = getLinearWeightFromLinearValue(newValue, min, max);
                        }

                        // adjust weight to match formatted string
                        colorBarInfo.setLocationWeight(newWeight);


                        colorBarInfos.add(colorBarInfo);
                        manualPointsArrayList.add(colorBarInfo.getFormattedValue());
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

    // todo Danny tmp edit
//    public boolean isInitialized() {
//        ColorPaletteSourcesInfo colorPaletteSourcesInfo = raster.getImageInfo().getColorPaletteSourcesInfo();
//        return colorPaletteSourcesInfo.isColorBarInitialized();
//    }
//
//    public void setInitialized(boolean initialized) {
//        ColorPaletteSourcesInfo colorPaletteSourcesInfo = raster.getImageInfo().getColorPaletteSourcesInfo();
//        colorPaletteSourcesInfo.setColorBarInitialized(initialized);
//    }

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
}
