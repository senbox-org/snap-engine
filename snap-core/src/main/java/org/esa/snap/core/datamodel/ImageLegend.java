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
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.math.MathUtils;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

// @todo 2 nf/** - if orientation is vertical, sample values should increase from bottom to top
// @todo 1 nf/** - make PALETTE_HEIGHT a fixed value, fill space into gaps instead
// @todo 2 nf/** - draw header text vertically for vertical orientations
// @todo 3 nf/** - also draw legend into product scene view
//                 make "color legend properties" dialog a preferences page


/**
 * The <code>ImageLegend</code> class is used to generate an image legend from a <code>{@link
 * ImageInfo}</code> instance.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class ImageLegend {

    public static final String PROPERTY_NAME_COLORBAR_TITLE_OVERRIDE = "palettes.colorbar.Title.Override";
    public static final boolean DEFAULT_COLORBAR_TITLE_OVERRIDE = false;
    public static final String PROPERTY_NAME_COLORBAR_LABELS_OVERRIDE = "palettes.colorbar.Labels.Override";
    public static final boolean DEFAULT_COLORBAR_LABELS_OVERRIDE = false;
    public static final String PROPERTY_NAME_COLORBAR_ALLOW_RESET = "palettes.colorbar.Allow.Reset";
    public static final boolean DEFAULT_COLORBAR_ALLOW_RESET = false;


    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    public static final int INSIDE = 0;
    public static final int OUTSIDE = 1;

    public static final int NULL_INT = -999;

    public static final String DISTRIB_EVEN_STR = "Use Even Distribution";
    public static final String DISTRIB_EXACT_STR = "Use Palette Distribution";
    public static final String DISTRIB_MANUAL_STR = "Use Manually Entered Points";


    public static final int DEFAULT_COLOR_BAR_LENGTH = 1200;
    public static final double DEFAULT_LAYER_OFFSET = 0.0;
    public static final double DEFAULT_LAYER_SHIFT = 0.0;
    public static final int DEFAULT_COLOR_BAR_THICKNESS = 48;
    public static final double DEFAULT_LAYER_SCALING = 75;
    public static final double DEFAULT_SCALING_FACTOR = 1;
    public static final int DEFAULT_TITLE_FONT_SIZE = 36;
    public static final int DEFAULT_TITLE_UNITS_FONT_SIZE = 28;
    public static final int DEFAULT_LABELS_FONT_SIZE = 28;
    public static final int DEFAULT_PREVIEW_LENGTH_PIXELS = 750;
    public static final int DEFAULT_FILE_LENGTH_PIXELS = DEFAULT_COLOR_BAR_LENGTH; // we could set this differently in the future if we wish to scale to a different size for the file export mode
    public static final boolean DEFAULT_CENTER_ON_LAYER = Boolean.TRUE;

    public static final double HORIZONTAL_INTER_LABEL_GAP_FACTOR = 3;
    public static final double VERTICAL_INTER_LABEL_GAP_FACTOR = 0.75;
    public static final int DEFAULT_TICKMARK_WIDTH = 3;


    public static final double WEIGHT_TOLERANCE = 0.00;
    public static final double FORCED_CHANGE_FACTOR = 0.0001;
    public static final double INVALID_WEIGHT = -1.0;

    // Independent attributes (Properties)
    private final ImageInfo imageInfo;
    private boolean initialized = false;
    private final RasterDataNode raster;
    private boolean showTitle;
    private String headerText;
    private String headerUnitsText;
    private int orientation;
    private String distributionType;
    private int numberOfTicks;
    private Color foregroundColor;
    private Color backgroundColor;
    private boolean backgroundTransparencyEnabled;
    private float backgroundTransparency;
    private boolean antialiasing;
    private int decimalPlaces;
    private boolean decimalPlacesForce;
    private String fullCustomAddThesePoints;

    private double scalingFactor;
    private int titleFontSize;
    private int titleUnitsFontSize;
    private int labelsFontSize;
    private int colorBarLength;
    private int colorBarThickness;
    private double layerScaling;
    private double layerOffset;
    private double layerShift;
    private boolean centerOnLayer;
    private String horizontalLocation;
    private String verticalLocation;
    private String insideOutsideLocation;
    private String titleOverRide = null;


    private int tickMarkLength = NULL_INT;
    private int borderGap = NULL_INT;   // TITLE_TO_PALETTE_GAP
    private int labelGap = NULL_INT;      // LABEL_TO_COLORBAR BORDER_GAP9
    private int headerGap = NULL_INT;      // HEADER_TO_COLORBAR BORDER_GAP


    // Dependent, internal attributes
    private Rectangle paletteRect;
    private Dimension legendSize;
    private Shape tickMarkShape;
    private int palettePosStart;
    private int palettePosEnd;
    private ArrayList<ColorBarInfo> colorBarInfos = new ArrayList<ColorBarInfo>();
    private int tickWidth;
    private int borderLineWidth;


    public ImageLegend(ImageInfo imageInfo, RasterDataNode raster) {
        this.imageInfo = imageInfo;
        this.raster = raster;
        showTitle = true;
        headerText = "";

        orientation = HORIZONTAL;
        backgroundColor = Color.white;
        foregroundColor = Color.black;
        backgroundTransparency = 1.0f;
        antialiasing = true;
        decimalPlaces = 2;
        scalingFactor = 1;
        decimalPlacesForce = false;
        setFullCustomAddThesePoints("");
        tickWidth = DEFAULT_TICKMARK_WIDTH;


    }

    // todo Danny tmp edit
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
//            // todo Danny changed this to get it to work in SNAP
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

    public String getHeaderText() {
//        if (!isInitialized() && titleOverRide != null && titleOverRide.length() > 0) {
//            return titleOverRide;
//        } else {
//            return headerText;
//        }
        return headerText;
    }

    public void setHeaderText(String headerText) {
        this.headerText = headerText;
    }

    public String getHeaderUnitsText() {
        return headerUnitsText;
    }

    public void setHeaderUnitsText(String headerUnitsText) {
        this.headerUnitsText = headerUnitsText;
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


    public int getNumberOfTicks() {
        return numberOfTicks;
    }

    public void setNumberOfTicks(int numberOfTicks) {
        this.numberOfTicks = numberOfTicks;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public Color getForegroundColor() {
        return foregroundColor;
    }

    public void setForegroundColor(Color foregroundColor) {
        this.foregroundColor = foregroundColor;
    }

    public boolean isAntialiasing() {
        return antialiasing;
    }

    public void setAntialiasing(boolean antialiasing) {
        this.antialiasing = antialiasing;
    }

    public void setTransparent(boolean isTransparent) {
        if (isTransparent) {
            setBackgroundTransparency(1.0f);
        } else {
            setBackgroundTransparency(0.0f);
        }
        setBackgroundTransparencyEnabled(isTransparent);
    }

    public boolean isBackgroundTransparencyEnabled() {
        return backgroundTransparencyEnabled;
    }

    public void setBackgroundTransparencyEnabled(boolean backgroundTransparencyEnabled) {
        this.backgroundTransparencyEnabled = backgroundTransparencyEnabled;
    }

    public float getBackgroundTransparency() {
        return backgroundTransparency;
    }

    public void setBackgroundTransparency(float backgroundTransparency) {
        this.backgroundTransparency = backgroundTransparency;
    }

    public boolean isAlphaUsed() {
        return backgroundTransparencyEnabled && backgroundTransparency > 0.0f && backgroundTransparency <= 1.0f;
    }

    public int getBackgroundAlpha() {
        return isAlphaUsed() ? Math.round(255f * (1f - backgroundTransparency)) : 255;
    }


    public BufferedImage createImage(Dimension imageLayerDimension, boolean colorBarLayer) {

        double scalingFactor = 1;

        if (colorBarLayer) {

            double oneHundredPercentScalingFactor;
            if (orientation == HORIZONTAL) {
                oneHundredPercentScalingFactor = (double) imageLayerDimension.width / (double) getColorBarLength();
            } else {
                oneHundredPercentScalingFactor = (double) imageLayerDimension.height / (double) getColorBarLength();
            }

            scalingFactor = getLayerScaling() / 100.0;
            scalingFactor = scalingFactor * oneHundredPercentScalingFactor;

        } else {

            scalingFactor = DEFAULT_FILE_LENGTH_PIXELS / (double) getColorBarLength();
            // todo DANNY
        }

        setLabelsFontSize((int) Math.round(scalingFactor * getLabelsFontSize()));
        setTitleFontSize((int) Math.round(scalingFactor * getTitleFontSize()));
        setTitleUnitsFontSize((int) Math.round(scalingFactor * getTitleUnitsFontSize()));
        setColorBarLength((int) Math.round(scalingFactor * getColorBarLength()));
        setColorBarThickness((int) Math.round(scalingFactor * getColorBarThickness()));

        return createImage();
    }


    public BufferedImage createPreviewImage() {

        double scalingFactor = DEFAULT_PREVIEW_LENGTH_PIXELS / (double) getColorBarLength();

        int originalLabelsFontSize = getLabelsFontSize();
        int originalTitleFontSize = getTitleFontSize();
        int originalTitleUnitsFontSize = getTitleUnitsFontSize();
        int originalColorBarLength = getColorBarLength();
        int originalColorBarThickness = getColorBarThickness();

        setLabelsFontSize((int) Math.round(scalingFactor * getLabelsFontSize()));
        setTitleFontSize((int) Math.round(scalingFactor * getTitleFontSize()));
        setTitleUnitsFontSize((int) Math.round(scalingFactor * getTitleUnitsFontSize()));
        setColorBarLength((int) Math.round(scalingFactor * getColorBarLength()));
        setColorBarThickness((int) Math.round(scalingFactor * getColorBarThickness()));


        BufferedImage bufferedImage = createImage();

        setLabelsFontSize(originalLabelsFontSize);
        setTitleFontSize(originalTitleFontSize);
        setTitleUnitsFontSize(originalTitleUnitsFontSize);
        setColorBarLength(originalColorBarLength);
        setColorBarThickness(originalColorBarThickness);

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
//        if (font != null) {
//            g2d.setFont(font);
//        }
        draw(g2d);
        return bi;
    }

//    private File getColorPalettesAuxdataDir() {
//        return new File(SystemUtils.getApplicationDataDir(), "beam-ui/auxdata/color-palettes");
//    }

    private void createColorBarInfos() {

        final double min = getImageInfo().getColorPaletteDef().getMinDisplaySample();
        final double max = getImageInfo().getColorPaletteDef().getMaxDisplaySample();

        double value, weight;
        double roundedValue, adjustedWeight;
        colorBarInfos.clear();
//
//        String schemeName = imageInfo.getColorPaletteSourcesInfo().getSchemeName();
//        boolean isDefault = false;


//        if (DISTRIB_DEFAULT_STR.equals(getDistributionType()) && schemeName != null && schemeName.equals("chlor_a")) {
//            String test = "0.01,0.03,0.1,0.3,1,3,10";
//    //        setHeaderText("Chlorophyll");
//            setFullCustomAddThesePoints(test);
//            isDefault = true;
//        }


        if (DISTRIB_EVEN_STR.equals(getDistributionType())) {
            distributeEvenly();
        } else if (DISTRIB_EXACT_STR.equals(getDistributionType())) {
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
                setFullCustomAddThesePoints(manualPoints);
            }
        } else if (DISTRIB_MANUAL_STR.equals(getDistributionType())) {
            String addThese = getFullCustomAddThesePoints();

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

        g2dTmp.setFont(getLabelsFont());

        Dimension headerRequiredDimension = getHeaderTextRequiredDimension(g2dTmp);

        double discreteBooster = 0;
        final int n = getNumGradationCurvePoints();

        if (orientation == HORIZONTAL) {


            Dimension labelsRequiredDimension = getHorizontalLabelsRequiredDimension(g2dTmp);


            double requiredWidth = Math.max(labelsRequiredDimension.getWidth(),
                    headerRequiredDimension.getWidth());

            requiredWidth = Math.max(requiredWidth, getColorBarLength());

            requiredWidth = getBorderGap() + requiredWidth + getBorderGap();

            // todo isDiscrete goes here

            if (n > 1 && imageInfo.getColorPaletteDef().isDiscrete()) {
                discreteBooster = labelsRequiredDimension.getWidth() / (n - 1);
                requiredWidth += discreteBooster;
            }

            //todo Danny changed this to make legend size stable
            requiredWidth = getColorBarLength();


            int requiredHeaderHeight = (int) Math.ceil(headerRequiredDimension.getHeight());
            int requiredLabelsHeight = (int) Math.ceil(labelsRequiredDimension.getHeight());


            int requiredHeight = getBorderGap()
                    + requiredHeaderHeight
                    + getHeaderGap()
                    + getColorBarThickness()
                    + getLabelGap()
                    + requiredLabelsHeight
                    + getBorderGap();


            legendSize = new Dimension((int) requiredWidth, requiredHeight);

            double firstLabelWidth = getSingleLabelRequiredDimension(g2dTmp, 0).getWidth();
            int firstLabelOverhangWidth = (int) Math.ceil(firstLabelWidth / 2.0);

            double lastLabelWidth = getSingleLabelRequiredDimension(g2dTmp, colorBarInfos.size() - 1).getWidth();
            int lastLabelOverhangWidth = (int) Math.ceil(lastLabelWidth / 2.0);


            paletteRect = new Rectangle(getBorderGap() + firstLabelOverhangWidth,
                    getBorderGap() + requiredHeaderHeight + getHeaderGap(),
                    legendSize.width - getBorderGap() - getBorderGap() - firstLabelOverhangWidth - lastLabelOverhangWidth,
                    getColorBarThickness());


            int paletteGap = 0;
            palettePosStart = paletteRect.x + paletteGap;
            palettePosEnd = paletteRect.x + paletteRect.width - (int) discreteBooster;

            // todo a piece of the old beam code, see what adjust does
            //   Math.max(_MIN_LEGEND_WIDTH, adjust(legendWidth, 16));

        } else {


            Dimension labelsRequiredDimension = getVerticalLabelsRequiredDimension(g2dTmp);

            double requiredWidth = getBorderGap()
                    + getColorBarThickness()
                    + getLabelGap()
                    + labelsRequiredDimension.getWidth()
                    + getHeaderGap()
                    + headerRequiredDimension.getHeight()
                    + getBorderGap();


            int requiredLabelsHeight = (int) Math.ceil(labelsRequiredDimension.getHeight());

            int requiredHeight = (int) Math.max(requiredLabelsHeight, headerRequiredDimension.getWidth());
            requiredHeight = Math.max(requiredHeight, getColorBarLength());
//            requiredHeight = Math.max(requiredHeight, MIN_VERTICAL_COLORBAR_HEIGHT);
            requiredHeight = getBorderGap() + requiredHeight + getBorderGap();

            //todo Danny changed this to make legend size stable

            if (n > 1 && imageInfo.getColorPaletteDef().isDiscrete()) {
                discreteBooster = labelsRequiredDimension.getHeight() / (n - 1);
                requiredWidth += discreteBooster;
            }

            requiredHeight = getColorBarLength();


            legendSize = new Dimension((int) requiredWidth, requiredHeight);


            double firstLabelHeight = getSingleLabelRequiredDimension(g2dTmp, 0).getHeight();
            int labelOverhangHeight = (int) Math.ceil(firstLabelHeight / 2.0);


            paletteRect = new Rectangle(getBorderGap(),
                    getBorderGap() + labelOverhangHeight,
                    getColorBarThickness(),
//                    MIN_VERTICAL_COLORBAR_WIDTH,
                    legendSize.height - getBorderGap() - getBorderGap() - labelOverhangHeight - labelOverhangHeight);


//            int paletteGap = 0;
//            palettePosStart = paletteRect.y + paletteGap;
//            palettePosEnd = paletteRect.y + paletteRect.height - paletteGap;

            palettePosStart = paletteRect.y + paletteRect.height;
            palettePosEnd = paletteRect.y + (int) discreteBooster;

        }


        tickMarkShape = createTickMarkShape();
    }


    private boolean hasHeaderText() {
        return showTitle && StringUtils.isNotNullAndNotEmpty(headerText);
    }

    private boolean hasUnitsText() {
        return StringUtils.isNotNullAndNotEmpty(headerUnitsText);
    }

    private void draw(Graphics2D g2d) {
        fillBackground(g2d);
        drawHeaderText(g2d);
        drawPalette(g2d);
        drawLabels(g2d);
    }

    private void fillBackground(Graphics2D g2d) {
        Color color = backgroundColor;
        if (isAlphaUsed()) {
            color = new Color(color.getRed(), color.getGreen(), color.getBlue(), getBackgroundAlpha());
        }
//        if (getBackgroundTransparency() == 1.0) {
//             color = UIManager.getColor("Panel.background");
//        }
        g2d.setColor(color);
        g2d.fillRect(0, 0, legendSize.width + 1, legendSize.height + 1);
    }


    private Dimension getHeaderTextRequiredDimension(Graphics2D g2d) {

        double width = 0;
        double height = 0;


        int UNITS_GAP_FACTOR = 3;

        if (hasHeaderText()) {

            Font originalFont = g2d.getFont();
            Rectangle2D singleLetter = g2d.getFontMetrics().getStringBounds("A", g2d);

            if (hasHeaderText()) {
                g2d.setFont(getTitleFont());
                Rectangle2D headerTextRectangle = g2d.getFontMetrics().getStringBounds(headerText, g2d);
                width += headerTextRectangle.getWidth();


                if (hasUnitsText()) {
                    width += (UNITS_GAP_FACTOR * singleLetter.getWidth());
                }
            }

            if (hasUnitsText()) {
                g2d.setFont(getTitleUnitsFont());
                Rectangle2D unitsTextRectangle = g2d.getFontMetrics().getStringBounds(getHeaderUnitsText(), g2d);
                width += unitsTextRectangle.getWidth();
            }

            height = singleLetter.getHeight();

            g2d.setFont(originalFont);
        }

        return new Dimension((int) Math.ceil(width), (int) Math.ceil(height));
    }


    private Dimension getHorizontalLabelsRequiredDimension(Graphics2D g2d) {

        double width = 0;
        double height = 0;

        if (colorBarInfos.size() > 0) {


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

        if (colorBarInfos.size() > 0) {

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
        if (hasHeaderText()) {
            Font origFont = g2d.getFont();

            final FontMetrics fontMetrics = g2d.getFontMetrics();
            g2d.setPaint(foregroundColor);

            int x0 = paletteRect.x;
            int y0 = paletteRect.y - getHeaderGap();

            g2d.setFont(getTitleFont());

            if (orientation == HORIZONTAL) {
                Rectangle2D headerTextRectangle = g2d.getFontMetrics().getStringBounds(headerText, g2d);
                g2d.drawString(headerText, x0, y0);

                Rectangle2D singleLetter = g2d.getFontMetrics().getStringBounds("A", g2d);
                int gap = (int) (2 * singleLetter.getWidth());

                if (hasUnitsText()) {
                    g2d.setFont(getTitleUnitsFont());

                    g2d.drawString(getHeaderUnitsText(), (int) (x0 + headerTextRectangle.getWidth() + gap), y0);
                }
            } else {
                Rectangle2D headerTextRectangle = g2d.getFontMetrics().getStringBounds(headerText, g2d);

                Rectangle2D singleLetter = g2d.getFontMetrics().getStringBounds("A", g2d);
                int gap = (int) (2 * singleLetter.getWidth());

                int labelOverhangHeight = (int) Math.ceil(singleLetter.getHeight() / 2.0);
                double translateX = x0
                        + paletteRect.width
                        + getLabelGap()
                        + getVerticalLabelsRequiredDimension(g2d).width
                        + getHeaderGap()
                        + singleLetter.getHeight();




                double translateY = y0 + paletteRect.height + labelOverhangHeight;

                double rotate = -Math.PI / 2.0;
                g2d.translate(translateX, translateY);
                g2d.rotate(rotate);


                g2d.drawString(headerText, 0, 0);

                g2d.rotate(-rotate);

                double translateY2 = -headerTextRectangle.getWidth() - gap;
                g2d.translate(0, translateY2);
                g2d.rotate(rotate);

                if (hasUnitsText()) {
                    g2d.setFont(getTitleUnitsFont());
                    g2d.drawString(getHeaderUnitsText(), 0, 0);
                }

                g2d.rotate(-rotate);
                g2d.translate(0, -translateY2);
                g2d.translate(-translateX, -translateY);
            }


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

            for (int x = xStart; x < xEnd; x++) {
                int divisor = palettePosEnd - palettePosStart;
                int palIndex;
                if (divisor == 0) {
                    palIndex = x < palettePosStart ? 0 : palette.length - 1;
                } else {
                    palIndex = Math.round((palette.length * (x - palettePosStart)) / divisor);
                }
                if (palIndex < 0) {
                    palIndex = 0;
                }
                if (palIndex > palette.length - 1) {
                    palIndex = palette.length - 1;
                }

                g2d.setColor(palette[palIndex]);
                g2d.drawLine(x, y1, x, y2);
            }
        } else {
            int x1 = paletteRect.x;
            int x2 = paletteRect.x + paletteRect.width;
            int yStart = paletteRect.y + paletteRect.height;
            int yEnd = paletteRect.y;

            for (int y = yStart; y > yEnd; y--) {
                int divisor = Math.abs(palettePosEnd - palettePosStart);

                int palIndex;
                if (divisor == 0) {
                    palIndex = y < palettePosStart ? 0 : palette.length - 1;
                } else {
                    palIndex = Math.round((palette.length * (palettePosStart - y)) / divisor);
                }
                if (palIndex < 0) {
                    palIndex = 0;
                }
                if (palIndex > palette.length - 1) {
                    palIndex = palette.length - 1;
                }

                g2d.setColor(palette[palIndex]);
                g2d.drawLine(x1, y, x2, y);
            }
        }


        g2d.setColor(Color.black);
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(paletteRect);
        g2d.setStroke(originalStroke);
    }


    private void drawLabels(Graphics2D g2d) {

        Color originalColor = g2d.getColor();
        Stroke originalStroke = g2d.getStroke();
        Color originalPaint = (Color) g2d.getPaint();
        Font originalFont = g2d.getFont();

        g2d.setFont(getLabelsFont());
        g2d.setPaint(foregroundColor);

        //   Color tickMarkColor = new Color(0, 0, 0);
        Color tickMarkColor = foregroundColor;

        Stroke tickMarkStroke = new BasicStroke(tickWidth);
        g2d.setStroke(tickMarkStroke);

        double translateX, translateY;

        int tickMarkOverHang;
        if (Math.floor((tickWidth) / 2) == (tickWidth) / 2) {
            // even
            tickMarkOverHang = (int) Math.floor((tickWidth) / 2);
        } else {
            // odd
            tickMarkOverHang = (int) Math.floor((tickWidth - 1) / 2);
        }

        for (ColorBarInfo colorBarInfo : colorBarInfos) {
            String formattedValue = colorBarInfo.getFormattedValue();
            double weight = colorBarInfo.getLocationWeight();

            double tickMarkRelativePosition = weight * (palettePosEnd - palettePosStart);
            if (orientation == HORIZONTAL) {
                translateX = palettePosStart + tickMarkRelativePosition;
                translateY = paletteRect.y + paletteRect.height;

                // make sure end tickmarks are placed within palette
                // tickmark hardcoded at 3 width will have 1 tickMarkOverHang
                if (translateX <= (palettePosStart + tickMarkOverHang)) {
                    translateX = (palettePosStart + tickMarkOverHang);
                }

                if (translateX >= (palettePosEnd - tickMarkOverHang)) {
                    translateX = (palettePosEnd - tickMarkOverHang);
                }

            } else {
                translateX = paletteRect.x + paletteRect.width;
                translateY = palettePosStart + tickMarkRelativePosition;

                if (translateY >= (palettePosStart - tickMarkOverHang)) {
                    translateY = (palettePosStart - tickMarkOverHang);
                }

                if (translateY <= (palettePosEnd + tickMarkOverHang)) {
                    translateY = (palettePosEnd + tickMarkOverHang);
                }

            }
            g2d.translate(translateX, translateY);
            g2d.setPaint(foregroundColor);
            g2d.draw(tickMarkShape);

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

            g2d.setColor(tickMarkColor);
            g2d.drawString(formattedValue, x0, y0);
            g2d.translate(-translateX, -translateY);
        }

        g2d.setFont(originalFont);
        g2d.setColor(originalColor);
        g2d.setStroke(originalStroke);
        g2d.setPaint(originalPaint);
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
            path.moveTo(0.0F, 0.7F * getTickMarkLength());
            path.lineTo(0.0F, 0.0F);
        } else {
            path.moveTo(0.0F, 0.0F);
            path.lineTo(0.7F * getTickMarkLength(), 0.0F);
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


    public String getFullCustomAddThesePoints() {
        return fullCustomAddThesePoints;
    }

    public void setFullCustomAddThesePoints(String fullCustomAddThesePoints) {
        this.fullCustomAddThesePoints = fullCustomAddThesePoints;
    }


    public Font getTitleFont() {
        return new Font("SansSerif", Font.BOLD, getTitleFontSize());
    }

    public Font getTitleUnitsFont() {
        return new Font("SansSerif", Font.ITALIC, getTitleUnitsFontSize());
    }


    public Font getLabelsFont() {
        return new Font("SansSerif", Font.TRUETYPE_FONT, getLabelsFontSize());
    }


    public int getTitleFontSize() {
        return titleFontSize;
    }

    public void setTitleFontSize(int titleFontSize) {
        this.titleFontSize = titleFontSize;
    }

    public int getTitleUnitsFontSize() {
        return titleUnitsFontSize;
    }

    public void setTitleUnitsFontSize(int titleUnitsFontSize) {
        this.titleUnitsFontSize = titleUnitsFontSize;
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

    public int getTickMarkLength() {
        if (tickMarkLength != NULL_INT) {
            return tickMarkLength;
        } else {
            return (int) Math.round(0.8 * getLabelGap());
        }
    }

    public void setTickMarkLength(int tickMarkLength) {
        this.tickMarkLength = tickMarkLength;
    }

    public int getBorderGap() {
        if (borderGap != NULL_INT) {
            return borderGap;
        } else {
            return (int) Math.round(0.5 * getLabelsFontSize());
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

    public int getHeaderGap() {
        if (headerGap != NULL_INT) {
            return headerGap;
        } else {
            return (int) Math.round(0.5 * getTitleFontSize());
        }
    }

    public void setHeaderGap(int headerGap) {
        this.headerGap = headerGap;
    }


    public boolean isCenterOnLayer() {
        return centerOnLayer;
    }

    public void setCenterOnLayer(boolean centerOnLayer) {
        this.centerOnLayer = centerOnLayer;
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

        if (getNumberOfTicks() >= 2) {
            ArrayList<String> manualPointsArrayList = new ArrayList<>();
            double normalizedDelta = (1.0 / (getNumberOfTicks() - 1.0));

            for (int i = 0; i < getNumberOfTicks(); i++) {

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
                setFullCustomAddThesePoints(manualPoints);
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

    public double getLayerOffset() {
        return layerOffset;
    }

    public void setLayerOffset(double layerOffset) {
        this.layerOffset = layerOffset;
    }


    public double getLayerShift() {
        return layerShift;
    }

    public void setLayerShift(double layerShift) {
        this.layerShift = layerShift;
    }



    public String getHorizontalLocation() {
        return horizontalLocation;
    }

    public void setHorizontalLocation(String horizontalLocation) {
        this.horizontalLocation = horizontalLocation;
    }

    public String getVerticalLocation() {
        return verticalLocation;
    }

    public void setVerticalLocation(String verticalLocation) {
        this.verticalLocation = verticalLocation;
    }

    public String getInsideOutsideLocation() {
        return insideOutsideLocation;
    }

    public void setInsideOutsideLocation(String insideOutsideLocation) {
        this.insideOutsideLocation = insideOutsideLocation;
    }
}
