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
package org.esa.snap.core.layer;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.esa.snap.core.datamodel.*;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;


/**
 * @author Daniel Knowles
 */

public class ColorBarLayer extends Layer {

    private static final ColorBarLayerType LAYER_TYPE = LayerTypeRegistry.getLayerType(ColorBarLayerType.class);

    private RasterDataNode raster;

    private ProductNodeHandler productNodeHandler;
    private ImageLegend imageLegend;
    BufferedImage bufferedImage = null;

    private double NULL_DOUBLE = -1.0;
    private double ptsToPixelsMultiplier = NULL_DOUBLE;

    private boolean allowImageLegendReset = true;
    private boolean imageLegendInitialized = false;

    boolean autoApplyPrevious;
    boolean schemeLabelsApplyPrevious;
    boolean schemeLabelsRestrictPrevious;
    String orientationPrevious;
    double sceneAspectBestFitPrevious;
    String locationPrevious;
    String locationVerticalPrevious;
    double locationGapFactorPrevious;
    double paletteMinPrevious;
    double paletteMaxPrevious;
    boolean paletteLogPrevious;
    boolean schemeMatchedPaletteOriginally;
    boolean isSchemeLabelsApplyPreference;


    String titlePreferences;
    String titleAltPreferences;
    String unitsPreferences;
    String unitsAltPreferences;
    String labelValuesActualPreferences;
    String labelValuesModePreferences;
    boolean populateLabelsTextfieldPreferences;
    int colorBarLengthPreferences;
    double labelValuesScalingFactorPreferences;
    boolean colorBarLocationInsidePrevious;
    boolean colorBarLocationInsidePreference;
    double locationOffsetPreference;
    double locationShiftPreference;

    boolean schemeOverRidden = false;


    public ColorBarLayer(RasterDataNode raster) {
        this(LAYER_TYPE, raster, initConfiguration(LAYER_TYPE.createLayerConfig(null), raster));
    }

    public ColorBarLayer(ColorBarLayerType type, RasterDataNode raster, PropertySet configuration) {
        super(type, configuration);
        setName(ColorBarLayerType.COLOR_BAR_LAYER_NAME);
        this.raster = raster;

        productNodeHandler = new ProductNodeHandler();
        raster.getProduct().addProductNodeListener(productNodeHandler);

        setTransparency(0.0);
    }


    private static PropertySet initConfiguration(PropertySet configurationTemplate, RasterDataNode raster) {
        configurationTemplate.setValue(ColorBarLayerType.PROPERTY_NAME_RASTER, raster);
        return configurationTemplate;
    }

    private Product getProduct() {
        return getRaster().getProduct();
    }

    RasterDataNode getRaster() {
        return raster;
    }

    ColorSchemeInfo schemeInfo = null;


    @Override
    public void renderLayer(Rendering rendering) {
//        System.out.println("Rendering Layer");

        String description = raster.getDescription();
        String bandname = raster.getName();
        String units = raster.getUnit();
        float wavelength = raster.getProduct().getBand(raster.getName()).getSpectralWavelength();
        boolean allowWavelengthZero = true;  //todo Consider adding this in preferences


        if (allowImageLegendReset == true) {
            allowImageLegendReset = false;

            imageLegend = new ImageLegend(raster.getImageInfo(), raster);

            if (!imageLegendInitialized) {
                String convertedTitle = ColorSchemeInfo.getColorBarTitle(getTitle(), bandname, description, wavelength, units, allowWavelengthZero);
                setTitle(convertedTitle);
                String convertedTitleAlt = ColorSchemeInfo.getColorBarTitle(getTitleAlt(), bandname, description, wavelength, units, allowWavelengthZero);
                setTitleAlt(convertedTitleAlt);
                String convertedUnits = ColorSchemeInfo.getColorBarTitle(getUnits(), bandname, description, wavelength, units, allowWavelengthZero);
                setUnits(convertedUnits);
                String convertedUnitsAlt = ColorSchemeInfo.getColorBarTitle(getUnitsAlt(), bandname, description, wavelength, units, allowWavelengthZero);
                setUnitsAlt(convertedUnitsAlt);

                paletteMinPrevious = raster.getImageInfo().getColorPaletteDef().getMinDisplaySample();
                paletteMaxPrevious = raster.getImageInfo().getColorPaletteDef().getMaxDisplaySample();
                paletteLogPrevious = raster.getImageInfo().isLogScaled();


                autoApplyPrevious = isAutoApplySchemes();
                schemeLabelsApplyPrevious = isSchemeLabelsApply();
                schemeLabelsRestrictPrevious = isSchemeLabelsRestrict();
                orientationPrevious = getOrientation();
                sceneAspectBestFitPrevious = getSceneAspectBestFit();
                locationPrevious = getColorBarLocationHorizontalPlacement();
                locationVerticalPrevious = getColorBarLocationVerticalPlacement();
                locationGapFactorPrevious = getLocationOffsetOutside();
                titlePreferences = getTitle();
                titleAltPreferences = getTitleAlt();
                unitsPreferences = getUnits();
                unitsAltPreferences = getUnitsAlt();
                labelValuesActualPreferences = getLabelValuesActual();
                labelValuesModePreferences = getLabelValuesMode();
                populateLabelsTextfieldPreferences = getPopulateLabelsTextfield();
                colorBarLengthPreferences = getColorBarLength();
                labelValuesScalingFactorPreferences = getLabelValuesScalingFactor();
                colorBarLocationInsidePrevious = isColorBarLocationInside();
                colorBarLocationInsidePreference = isColorBarLocationInside();
                locationOffsetPreference = getLocationOffsetInside();
                locationShiftPreference = getLocationShift();
                isSchemeLabelsApplyPreference = isSchemeLabelsApply();
            }


            if (!imageLegendInitialized || (isAutoApplySchemes() || isSchemeLabelsApply())) {
                schemeInfo = ColorSchemeInfo.getColorPaletteInfoByBandNameLookup(raster.getName());
            }


            if (!imageLegendInitialized || (isAutoApplySchemes() != autoApplyPrevious)) {
                setTitle(titlePreferences);
                setTitleAlt(titleAltPreferences);
                setUnits(unitsPreferences);
                setUnitsAlt(unitsAltPreferences);
                setColorBarLength(colorBarLengthPreferences);

                if (isAutoApplySchemes()) {
                    if (schemeInfo != null) {
                        if (schemeInfo.getColorBarTitle() != null && schemeInfo.getColorBarTitle().trim().length() > 0) {
                            setTitle(schemeInfo.getColorBarTitle());
                        }

                        if (schemeInfo.getColorBarTitleAlt() != null && schemeInfo.getColorBarTitleAlt().trim().length() > 0) {
                            setTitleAlt(schemeInfo.getColorBarTitleAlt());
                        }

                        if (schemeInfo.getColorBarUnits() != null && schemeInfo.getColorBarUnits().trim().length() > 0) {
                            setUnits(schemeInfo.getColorBarUnits());
                        }

                        if (schemeInfo.getColorBarUnitsAlt() != null && schemeInfo.getColorBarUnitsAlt().trim().length() > 0) {
                            setUnitsAlt(schemeInfo.getColorBarUnitsAlt());
                        }

                        if (schemeInfo.getColorBarLengthStr() != null && schemeInfo.getColorBarLengthStr().trim().length() > 0) {
                            setColorBarLength(Integer.parseInt(schemeInfo.getColorBarLengthStr()));
                        }
                    }
                }
            }



            if (!imageLegendInitialized || (isSchemeLabelsApply() != schemeLabelsApplyPrevious)) {
                if (isSchemeLabelsApply()
                        && schemeInfo != null
                        && schemeInfo.getColorBarLabels() != null
                        && schemeInfo.getColorBarLabels().trim().length() > 0) {
                    if (!isSchemeLabelsRestrict() || (isSchemeLabelsRestrict() && isSchemeMatchesPalette())) {
                        setLabelValuesActual(schemeInfo.getColorBarLabels());
                        setLabelValuesMode(ColorBarLayerType.DISTRIB_MANUAL_STR);
                        setPopulateLabelsTextfield(true);
                        schemeMatchedPaletteOriginally = isSchemeMatchesPalette();

                        if (schemeInfo.getColorBarLabelScalingStr() != null && schemeInfo.getColorBarLabelScalingStr().trim().length() > 0) {
                            setLabelValuesScalingFactor(Double.parseDouble(schemeInfo.getColorBarLabelScalingStr()));
                        }

                        schemeOverRidden = false;
                    } else {
                        setLabelValuesActual(labelValuesActualPreferences);
//                    setLabelValuesMode(labelValuesModePreferences);
                        setLabelValuesMode(ColorBarLayerType.DISTRIB_EVEN_STR);
                        setPopulateLabelsTextfield(populateLabelsTextfieldPreferences);
                        setLabelValuesScalingFactor(labelValuesScalingFactorPreferences);
                        schemeOverRidden = true;
                    }
                } else {
                    setLabelValuesActual(labelValuesActualPreferences);
//                    setLabelValuesMode(labelValuesModePreferences);
                    setLabelValuesMode(ColorBarLayerType.DISTRIB_EVEN_STR);
                    setPopulateLabelsTextfield(populateLabelsTextfieldPreferences);
                    setLabelValuesScalingFactor(labelValuesScalingFactorPreferences);
                    schemeOverRidden = false;
                }
            }




//
//            if (imageLegendInitialized &&  (schemeLabelsRestrictPrevious != isSchemeLabelsRestrict())) {
//                if (isSchemeLabelsApply()
//                        && schemeInfo != null
//                        && schemeInfo.getColorBarLabels() != null
//                        && schemeInfo.getColorBarLabels().trim().length() > 0) {
//                    if (!isSchemeLabelsRestrict() || (isSchemeLabelsRestrict() && isSchemeMatchesPalette())) {
//                        setLabelValuesActual(schemeInfo.getColorBarLabels());
//                        setLabelValuesMode(ColorBarLayerType.DISTRIB_MANUAL_STR);
//                        setPopulateLabelsTextfield(true);
//                        schemeMatchedPaletteOriginally = isSchemeMatchesPalette();
//
//                        if (schemeInfo.getColorBarLabelScalingStr() != null && schemeInfo.getColorBarLabelScalingStr().trim().length() > 0) {
//                            setLabelValuesScalingFactor(Double.parseDouble(schemeInfo.getColorBarLabelScalingStr()));
//                        }
//
//                        schemeOverRidden = false;
//                    } else {
//                        setLabelValuesActual(labelValuesActualPreferences);
//                        setLabelValuesMode(labelValuesModePreferences);
//                        setPopulateLabelsTextfield(populateLabelsTextfieldPreferences);
//                        setLabelValuesScalingFactor(labelValuesScalingFactorPreferences);
//                        schemeOverRidden = true;
//                    }
//                }
//            }



            if (imageLegendInitialized && isPaletteChanged() && isSchemeLabelsRestrict() && isSchemeLabelsApply()) {

                if (!schemeOverRidden ) {

                    // reset to even distribution if the palette gets altered

//            if (isSchemeLabelsApply() && schemeLabelsApplyPrevious != false){  // user just click on schemes  //todo maybe change this to remove autoApplyPrevious != false
//                    if (schemeInfo != null && schemeMatchedPaletteOriginally) {
                    if (schemeInfo != null) {
                        if (!isSchemeMatchesPalette()) {
                            System.out.println("raster.getImageInfo().getColorPaletteDef().getMinDisplaySample()=" + raster.getImageInfo().getColorPaletteDef().getMinDisplaySample());
                            System.out.println("schemeInfo.getMinValue()=" + schemeInfo.getMinValue());
                            System.out.println("raster.getImageInfo().getColorPaletteDef().getMaxDisplaySample()=" + raster.getImageInfo().getColorPaletteDef().getMaxDisplaySample());
                            System.out.println("schemeInfo.getMaxValue()=" + schemeInfo.getMaxValue());

                            System.out.println("raster.getImageInfo().getColorPaletteDef().isLogScaled()=" + raster.getImageInfo().getColorPaletteDef().isLogScaled());
                            System.out.println("schemeInfo.isLogScaled()=" + schemeInfo.isLogScaled());

                            if (ColorBarLayerType.DISTRIB_MANUAL_STR.equals(getLabelValuesMode())) {
                                setLabelValuesActual(labelValuesActualPreferences);
//                                setLabelValuesMode(labelValuesModePreferences);
                                setLabelValuesMode(ColorBarLayerType.DISTRIB_EVEN_STR);
                                setPopulateLabelsTextfield(populateLabelsTextfieldPreferences);
                                setLabelValuesScalingFactor(labelValuesScalingFactorPreferences);
                                schemeOverRidden = true;
//                                setSchemeLabelsApply(false);
//                                schemeLabelsApplyPrevious = false;
                            }
                        }
                    }
                }

                if (schemeOverRidden) {
                    if (isSchemeMatchesPalette()
                            && schemeInfo != null
                            && schemeInfo.getColorBarLabels() != null
                            && schemeInfo.getColorBarLabels().trim().length() > 0) {

//                        schemeInfo = ColorSchemeInfo.getColorPaletteInfoByBandNameLookup(raster.getName());

                        setLabelValuesActual(schemeInfo.getColorBarLabels());
                        setLabelValuesMode(ColorBarLayerType.DISTRIB_MANUAL_STR);
                        setPopulateLabelsTextfield(true);
                        schemeMatchedPaletteOriginally = isSchemeMatchesPalette();

                        if (schemeInfo.getColorBarLabelScalingStr() != null && schemeInfo.getColorBarLabelScalingStr().trim().length() > 0) {
                            setLabelValuesScalingFactor(Double.parseDouble(schemeInfo.getColorBarLabelScalingStr()));
                        }

                        schemeOverRidden = false;
                    }
                }

            }




            imageLegendInitialized = true;
            autoApplyPrevious = isAutoApplySchemes();
            schemeLabelsApplyPrevious = isSchemeLabelsApply();
            schemeLabelsRestrictPrevious = isSchemeLabelsRestrict();

            paletteMinPrevious = raster.getImageInfo().getColorPaletteDef().getMinDisplaySample();
            paletteMaxPrevious = raster.getImageInfo().getColorPaletteDef().getMaxDisplaySample();
            paletteLogPrevious = raster.getImageInfo().isLogScaled();

            String convertedTitle = ColorSchemeInfo.getColorBarTitle(getTitle(), bandname, description, wavelength, units, allowWavelengthZero);
            setTitle(convertedTitle);

            if (convertedTitle == null || convertedTitle.length() == 0) {
                String convertedTitlePreferences = ColorSchemeInfo.getColorBarTitle(titlePreferences, bandname, description, wavelength, units, allowWavelengthZero);

                setTitle(convertedTitlePreferences);

                if (convertedTitlePreferences == null || convertedTitlePreferences.length() == 0) {
                    setTitle(raster.getName());
                }
            }


            String convertedTitleAlt = ColorSchemeInfo.getColorBarTitle(getTitleAlt(), bandname, description, wavelength, units, allowWavelengthZero);
            setTitleAlt(convertedTitleAlt);
            String convertedUnits = ColorSchemeInfo.getColorBarTitle(getUnits(), bandname, description, wavelength, units, allowWavelengthZero);
            setUnits(convertedUnits);

            if (convertedUnits == null || convertedUnits.length() == 0) {
                String convertedUnitsPreferences = ColorSchemeInfo.getColorBarTitle(unitsPreferences, bandname, description, wavelength, units, allowWavelengthZero);

                setUnits(convertedUnitsPreferences);

                if (convertedUnitsPreferences == null || convertedUnitsPreferences.length() == 0) {
                    setUnits(raster.getUnit());
                }
            }

            if (convertedUnits == null || convertedUnits.length() == 0) {
                setUnits(raster.getUnit());
            }
            String convertedUnitsAlt = ColorSchemeInfo.getColorBarTitle(getUnitsAlt(), bandname, description, wavelength, units, allowWavelengthZero);
            setUnitsAlt(convertedUnitsAlt);


            // Title & Units Text
            // todo Danny
//            imageLegend.setTitleAltUse(isTitleAltUse());
            imageLegend.setTitleAltUse(false);
            imageLegend.setTitle(getTitle());
            imageLegend.setTitleAlt(getTitleAlt());
            imageLegend.setUnitsAltUse(isUnitsAltUse());
            imageLegend.setUnitsAlt(getUnitsAlt());
            imageLegend.setUnits(getUnits());
            imageLegend.setUnitsNull(getUnitsNull());
            imageLegend.setConvertCaret(isConvertCaret());
            imageLegend.setUnitsParenthesis(isUnitsParenthesis());


            // Orientation
            imageLegend.setOrientation(getOrientation());
            imageLegend.setSceneAspectBestFit(getSceneAspectBestFit());
            imageLegend.setTitleVerticalAnchor(getTitleVerticalAnchor());
            imageLegend.setReversePalette(isReversePalette());


            // Tick Label Values
            imageLegend.setDistributionType(getLabelValuesMode());
            if (getLabelValuesMode().equals(ColorBarLayerType.DISTRIB_MANUAL_STR)) {
                setPopulateLabelsTextfield(true);
            }
            imageLegend.setTickMarkCount(getLabelValuesCount());
            imageLegend.setCustomLabelValues(getLabelValuesActual());

            imageLegend.setScalingFactor(getLabelValuesScalingFactor());
            imageLegend.setDecimalPlaces(getDecimalPlaces());
            imageLegend.setDecimalPlacesForce(getDecimalPlacesForce());
            imageLegend.setWeightTolerance(getWeightTolerance());


            // Placement Location


            // Size & Scaling
            imageLegend.setLayerScaling(getLayerScaling());
            imageLegend.setColorBarLength(getColorBarLength());
            imageLegend.setColorBarWidth(getColorBarWidth());


            // Title Format
            imageLegend.setShowTitle(isShowTitle());
            imageLegend.setTitleFontSize(getTitleFontSize());
            imageLegend.setTitleColor(getTitleColor());
            imageLegend.setTitleFontName(getTitleFontName());
            imageLegend.setTitleFontType(getTitleFontType());


            // Units Format
            imageLegend.setShowUnits(isShowTitleUnits());
            imageLegend.setUnitsFontSize(getUnitsFontSize());
            imageLegend.setUnitsColor(getUnitsColor());
            imageLegend.setUnitsFontName(getUnitsFontName());
            imageLegend.setUnitsFontType(getUnitsFontType());


            // Tick Label Format
            imageLegend.setLabelsShow(isLabelsShow());
            imageLegend.setLabelsFontName(getLabelsFontName());
            imageLegend.setLabelsFontType(getLabelsFontType());
            imageLegend.setLabelsFontSize(getLabelsFontSize());
            imageLegend.setLabelsColor(getLabelsColor());


            // Tickmarks
            imageLegend.setTickmarkShow(isTickmarksShow());
            imageLegend.setTickmarkLength(getTickmarksLength());
            imageLegend.setTickmarkWidth(getTickmarksWidth());
            imageLegend.setTickmarkColor(getTickmarksColor());


            // Palette Border
            imageLegend.setBorderShow(isBorderShow());
            imageLegend.setBorderWidth(getBorderWidth());
            imageLegend.setBorderColor(getBorderColor());


            // Legend Border
            imageLegend.setBackdropBorderShow(isBackdropBorderShow());
            imageLegend.setBackdropBorderWidth(getBackdropBorderWidth());
            imageLegend.setBackdropBorderColor(getBackdropBorderColor());


            // Legend Backdrop
            imageLegend.setBackdropShow(isBackdropShow());
            imageLegend.setBackdropTransparency(((Number) getBackdropTransparency()).floatValue());
            imageLegend.setBackdropColor(getBackdropColor());


            // Legend Margins
            imageLegend.setTopBorderGapFactor(getBorderGapFactorTop());
            imageLegend.setBottomBorderGapFactor(getBorderGapFactorBottom());
            imageLegend.setLeftSideBorderGapFactor(getBorderGapFactorLeftside());
            imageLegend.setRightSideBorderGapFactor(getBorderGapFactorRightside());
            imageLegend.setTitleGapFactor(getTitleGapFactor());
            imageLegend.setLabelGapFactor(getLabelGapFactor());


            imageLegend.setTransparencyEnabled(true);
            imageLegend.setAntialiasing((Boolean) true);


            int imageHeight = raster.getRasterHeight();
            int imageWidth = raster.getRasterWidth();


            if (ColorBarLayerType.SCENE_SCALING_LENGTH.equals(applySizeScaling())) {
                bufferedImage = imageLegend.createImage(new Dimension(imageWidth, imageHeight), true, true, false);
            } else if (ColorBarLayerType.SCENE_SCALING_LENGTH.equals(applySizeScaling())) {
                bufferedImage = imageLegend.createImage(new Dimension(imageWidth, imageHeight), true, false, true);
            } else {
                bufferedImage = imageLegend.createImage();
            }


            // Update the properties with some calculated/looked-up values

            if (getPopulateLabelsTextfield()) {
                setLabelValuesActual(imageLegend.getCustomLabelValues());
            }

            if (imageLegend != null && bufferedImage != null) {

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
                    g2d.setTransform(transform);
                    drawImage(g2d, raster, bufferedImage);

                } finally {
                    g2d.setTransform(transformSave);
                }

            }

            allowImageLegendReset = true;
        }
    }


    private boolean isPaletteChanged() {
        if (raster.getImageInfo().getColorPaletteDef().getMinDisplaySample() != paletteMinPrevious) {
            return true;
        }

        if (raster.getImageInfo().getColorPaletteDef().getMaxDisplaySample() != paletteMaxPrevious) {
            return true;
        }

        if (raster.getImageInfo().isLogScaled() != paletteLogPrevious) {
            return true;
        }

        return false;
    }


    private boolean isSchemeMatchesPalette() {
        if (raster.getImageInfo().getColorPaletteDef().getMinDisplaySample() == schemeInfo.getMinValue() &&
                raster.getImageInfo().getColorPaletteDef().getMaxDisplaySample() == schemeInfo.getMaxValue() &&
                raster.getImageInfo().getColorPaletteDef().isLogScaled() == schemeInfo.isLogScaled()) {

            return true;
        } else {
            return false;
        }
    }





    private void drawImage(Graphics2D g2d, RasterDataNode raster, BufferedImage bufferedImage) {

        AffineTransform transform = createTransform(bufferedImage);
        g2d.drawRenderedImage(bufferedImage, transform);

    }

    private AffineTransform createTransform(BufferedImage image) {

        AffineTransform transform = raster.getSourceImage().getModel().getImageToModelTransform(0);
        transform.concatenate(createTransform(raster, image));
        return transform;
    }

    private AffineTransform createTransform(RasterDataNode raster, RenderedImage colorBarImage) {

        int colorBarImageWidth = colorBarImage.getWidth();
        int colorBarImageHeight = colorBarImage.getHeight();

        int rasterWidth = raster.getRasterWidth();
        int rasterHeight = raster.getRasterHeight();

        double offset;
        if (isColorBarLocationInside()) {
            if (isHorizontalColorBar()) {
                offset = (getLocationOffsetInside() / 100) *  ((raster.getRasterWidth() + raster.getRasterHeight()) / 2.0);
            } else {
                offset = (getLocationOffsetInside() / 100) *  ((raster.getRasterWidth() + raster.getRasterHeight()) / 2.0);
            }
        } else {
            if (isHorizontalColorBar()) {
                offset = (getLocationOffsetOutside() / 100) *  ((raster.getRasterWidth() + raster.getRasterHeight()) / 2.0);
            } else {
                offset = (getLocationOffsetOutside() / 100) *  ((raster.getRasterWidth() + raster.getRasterHeight()) / 2.0);
            }
        }


        double shift;

        if (isHorizontalColorBar()) {
            shift = getLocationShift() * raster.getRasterWidth() / 100;
        } else {
            shift = - getLocationShift() * raster.getRasterHeight() / 100;
        }

        double offsetAdjust = 0;
        double shiftAdjust = 0;


        if (isHorizontalColorBar()) {
            if (isColorBarLocationInside()) {
                switch (getColorBarLocationHorizontalPlacement()) {

                    case ColorBarLayerType.LOCATION_LOWER_LEFT:
                        offsetAdjust = -colorBarImageHeight;
                        shiftAdjust = 0;
                        offset = -offset;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_CENTER:
                        offsetAdjust = -colorBarImageHeight;
                        shiftAdjust = (rasterWidth - colorBarImageWidth) / 2;
                        offset = -offset;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_RIGHT:
                        offsetAdjust = -colorBarImageHeight;
                        shiftAdjust = rasterWidth - colorBarImageWidth;
                        offset = -offset;
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_LEFT:
                        offsetAdjust = -rasterHeight;
                        shiftAdjust = 0;
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_CENTER:
                        offsetAdjust = -rasterHeight;
                        shiftAdjust = (rasterWidth - colorBarImageWidth) / 2;
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_RIGHT:
                        offsetAdjust = -rasterHeight;
                        shiftAdjust = rasterWidth - colorBarImageWidth;
                        break;
//                    case ColorBarLayerType.LOCATION_LEFT_CENTER:
//                        offsetAdjust = -(rasterHeight + colorBarImageHeight) / 2;
//                        shiftAdjust = 0;
//                        break;
//                    case ColorBarLayerType.LOCATION_RIGHT_CENTER:
//                        offsetAdjust = -(rasterHeight + colorBarImageHeight) / 2;
//                        shiftAdjust = rasterWidth - colorBarImageWidth;
//                        break;
                    default:
                        offsetAdjust = -colorBarImageHeight;
                        shiftAdjust = (rasterWidth - colorBarImageWidth) / 2;
                }


            } else {
                switch (getColorBarLocationHorizontalPlacement()) {

                    case ColorBarLayerType.LOCATION_LOWER_LEFT:
                        offsetAdjust = 0;
                        shiftAdjust = 0;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_CENTER:
                        offsetAdjust = 0;
                        shiftAdjust = (rasterWidth - colorBarImageWidth) / 2;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_RIGHT:
                        offsetAdjust = 0;
                        shiftAdjust = rasterWidth - colorBarImageWidth;
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_LEFT:
                        offsetAdjust = -rasterHeight - colorBarImageHeight;
                        shiftAdjust = 0;
                        offset = -offset;
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_CENTER:
                        offsetAdjust = -rasterHeight - colorBarImageHeight;
                        shiftAdjust = (rasterWidth - colorBarImageWidth) / 2;
                        offset = -offset;
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_RIGHT:
                        offsetAdjust = -rasterHeight - colorBarImageHeight;
                        shiftAdjust = rasterWidth - colorBarImageWidth;
                        offset = -offset;
                        break;
//                    case ColorBarLayerType.LOCATION_LEFT_CENTER:
//                        offsetAdjust = -(rasterHeight + colorBarImageHeight) / 2;
//                        shiftAdjust = -colorBarImageWidth;
//                        break;
//                    case ColorBarLayerType.LOCATION_RIGHT_CENTER:
//                        offsetAdjust = -(rasterHeight + colorBarImageHeight) / 2;
//                        shiftAdjust = rasterWidth;
//                        break;
                    default:
                        offsetAdjust = 0;
                        shiftAdjust = (rasterWidth - colorBarImageWidth) / 2;
                }
            }

        } else {  // vertical
            if (isColorBarLocationInside()) {

                switch (getColorBarLocationVerticalPlacement()) {
                    case ColorBarLayerType.LOCATION_UPPER_LEFT:
                        offsetAdjust = -rasterWidth;
                        shiftAdjust = 0;
                        break;
                    case ColorBarLayerType.LOCATION_LEFT_CENTER:
                        offsetAdjust = -rasterWidth;
                        shiftAdjust = (rasterHeight - colorBarImageHeight) / 2;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_LEFT:
                        offsetAdjust = -rasterWidth;
                        shiftAdjust = rasterHeight - colorBarImageHeight;
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_RIGHT:
                        offsetAdjust = -colorBarImageWidth;
                        shiftAdjust = 0;
                        offset = -offset;
                        break;
                    case ColorBarLayerType.LOCATION_RIGHT_CENTER:
                        offsetAdjust = -colorBarImageWidth;
                        shiftAdjust = (rasterHeight - colorBarImageHeight) / 2;
                        offset = -offset;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_RIGHT:
                        offsetAdjust = -colorBarImageWidth;
                        shiftAdjust = rasterHeight - colorBarImageHeight;
                        offset = -offset;
                        break;
//                    case ColorBarLayerType.LOCATION_UPPER_CENTER:
//                        offsetAdjust = -rasterWidth / 2.0 - colorBarImageWidth / 2.0;
//                        shiftAdjust = 0;
//                        break;
//                    case ColorBarLayerType.LOCATION_LOWER_CENTER:
//                        offsetAdjust = -rasterWidth / 2.0 - colorBarImageWidth / 2.0;
//                        shiftAdjust = rasterHeight - colorBarImageHeight;
//                        break;
                    default:
                        offsetAdjust = -colorBarImageWidth;
                        shiftAdjust = rasterHeight - colorBarImageHeight;
                }
            } else {
                switch (getColorBarLocationVerticalPlacement()) {
                    case ColorBarLayerType.LOCATION_UPPER_LEFT:
                        offsetAdjust = -rasterWidth - colorBarImageWidth;
                        shiftAdjust = 0;
                        offset = -offset;
                        break;
                    case ColorBarLayerType.LOCATION_LEFT_CENTER:
                        offsetAdjust = -rasterWidth - colorBarImageWidth;
                        shiftAdjust = (rasterHeight - colorBarImageHeight) / 2;
                        offset = -offset;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_LEFT:
                        offsetAdjust = -rasterWidth - colorBarImageWidth;
                        shiftAdjust = rasterHeight - colorBarImageHeight;
                        offset = -offset;
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_RIGHT:
                        offsetAdjust = 0;
                        shiftAdjust = 0;
                        break;
                    case ColorBarLayerType.LOCATION_RIGHT_CENTER:
                        offsetAdjust = 0;
                        shiftAdjust = (rasterHeight - colorBarImageHeight) / 2;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_RIGHT:
                        offsetAdjust = 0;
                        shiftAdjust = rasterHeight - colorBarImageHeight;
                        break;
//                    case ColorBarLayerType.LOCATION_UPPER_CENTER:
//                        offsetAdjust = -rasterWidth / 2.0 - colorBarImageWidth / 2.0;
//                        shiftAdjust = -colorBarImageHeight;
//                        break;
//                    case ColorBarLayerType.LOCATION_LOWER_CENTER:
//                        offsetAdjust = -rasterWidth / 2.0 - colorBarImageWidth / 2.0;
//                        shiftAdjust = rasterHeight;
//                        break;
                    default:
                        offsetAdjust = 0;
                        shiftAdjust = rasterHeight - colorBarImageHeight;
                }
            }
        }

        double y_axis_translation = (isHorizontalColorBar()) ? rasterHeight + offset + offsetAdjust : shift + shiftAdjust;
        double x_axis_translation = (isHorizontalColorBar()) ? shift + shiftAdjust : rasterWidth + offset + offsetAdjust;

        double[] flatmatrix = {1, 0.0, 0.0, 1, x_axis_translation, y_axis_translation};


        AffineTransform i2mTransform = new AffineTransform(flatmatrix);
        return i2mTransform;
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
            imageLegend = null;
            raster = null;
        }
    }

    @Override
    protected void fireLayerPropertyChanged(PropertyChangeEvent event) {
        String propertyName = event.getPropertyName();


        if (allowImageLegendReset) {
            imageLegend = null;
        }


        if (getConfiguration().getProperty(propertyName) != null) {
            getConfiguration().setValue(propertyName, event.getNewValue());
        }

        super.fireLayerPropertyChanged(event);
    }


    private boolean isAutoApplySchemes() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_SCHEME_AUTO_APPLY_KEY,
                ColorBarLayerType.PROPERTY_SCHEME_AUTO_APPLY_DEFAULT);
    }

    private void setAutoApplySchemes(boolean value) {
        try {
            boolean valueCurrent = isAutoApplySchemes();

            if (valueCurrent != value) {
//                System.out.println("Inside and setting title to " + value);
                getConfiguration().getProperty(ColorBarLayerType.PROPERTY_SCHEME_AUTO_APPLY_KEY).setValue((Object) value);
            }
        } catch (ValidationException v) {
        }
    }


    private boolean isSchemeLabelsApply() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_SCHEME_LABELS_APPLY_KEY,
                ColorBarLayerType.PROPERTY_SCHEME_LABELS_APPLY_DEFAULT);
    }


    private void setSchemeLabelsApply(boolean value) {
        try {
            boolean valueCurrent = isSchemeLabelsApply();

            if (valueCurrent != value) {
//                System.out.println("Inside and setting title to " + value);
                getConfiguration().getProperty(ColorBarLayerType.PROPERTY_SCHEME_LABELS_APPLY_KEY).setValue((Object) value);
            }
        } catch (ValidationException v) {
        }
    }


    private boolean isSchemeLabelsRestrict() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_SCHEME_LABELS_RESTRICT_KEY,
                ColorBarLayerType.PROPERTY_SCHEME_LABELS_RESTRICT_DEFAULT);
    }


    // Title & Units Text

    private boolean isTitleAltUse() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_ALT_USE_KEY,
                ColorBarLayerType.PROPERTY_TITLE_ALT_USE_DEFAULT);
    }

    private String getTitle() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_KEY,
                ColorBarLayerType.PROPERTY_TITLE_DEFAULT);
    }

    private void setTitle(String value) {
        try {
            String valueCurrent = getTitle();
//            System.out.println("Current title = " + valueCurrent);
            if (valueCurrent == null || (valueCurrent != null && !valueCurrent.equals(value))) {
//                System.out.println("Inside and setting title to " + value);
                getConfiguration().getProperty(ColorBarLayerType.PROPERTY_TITLE_KEY).setValue((Object) value);
            }
        } catch (ValidationException v) {
        }
    }


    private String getTitleAlt() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_ALT_KEY,
                ColorBarLayerType.PROPERTY_TITLE_ALT_DEFAULT);
    }

    private void setTitleAlt(String value) {
        try {
            String valueCurrent = getUnits();

            if (valueCurrent == null || (valueCurrent != null && !valueCurrent.equals(value))) {
                getConfiguration().getProperty(ColorBarLayerType.PROPERTY_TITLE_ALT_KEY).setValue((Object) value);
            }
        } catch (ValidationException v) {
        }
    }


    private boolean isUnitsAltUse() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_UNITS_ALT_USE_KEY,
                ColorBarLayerType.PROPERTY_UNITS_ALT_USE_DEFAULT);
    }

    private String getUnits() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_UNITS_KEY,
                ColorBarLayerType.PROPERTY_UNITS_DEFAULT);
    }

    private void setUnits(String value) {
        try {
            String valueCurrent = getUnits();

            if (valueCurrent == null || (valueCurrent != null && !valueCurrent.equals(value))) {
                getConfiguration().getProperty(ColorBarLayerType.PROPERTY_UNITS_KEY).setValue((Object) value);
            }
        } catch (ValidationException v) {
        }
    }


    private String getUnitsAlt() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_UNITS_ALT_KEY,
                ColorBarLayerType.PROPERTY_UNITS_ALT_DEFAULT);
    }

    private void setUnitsAlt(String value) {
        try {
            String valueCurrent = getUnits();

            if (valueCurrent == null || (valueCurrent != null && !valueCurrent.equals(value))) {
                getConfiguration().getProperty(ColorBarLayerType.PROPERTY_UNITS_ALT_KEY).setValue((Object) value);
            }
        } catch (ValidationException v) {
        }
    }


    private String getUnitsNull() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_UNITS_NULL_KEY,
                ColorBarLayerType.PROPERTY_UNITS_NULL_DEFAULT);
    }

    private void setUnitsNull(String value) {
        try {
            String valueCurrent = getUnits();

            if (valueCurrent == null || (valueCurrent != null && !valueCurrent.equals(value))) {
                getConfiguration().getProperty(ColorBarLayerType.PROPERTY_UNITS_NULL_KEY).setValue((Object) value);
            }
        } catch (ValidationException v) {
        }
    }

    private boolean isConvertCaret() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_CONVERT_CARET_KEY,
                ColorBarLayerType.PROPERTY_CONVERT_CARET_DEFAULT);
    }

    private boolean isUnitsParenthesis() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_UNITS_PARENTHESIS_KEY,
                ColorBarLayerType.PROPERTY_UNITS_PARENTHESIS_DEFAULT);
    }


    // Orientation

    private String getOrientation() {
        String orientation = getConfigurationProperty(ColorBarLayerType.PROPERTY_ORIENTATION_KEY,
                ColorBarLayerType.PROPERTY_ORIENTATION_DEFAULT);

        return orientation;

//        if (ColorBarLayerType.OPTION_VERTICAL.equals(orientation)) {
//            return ImageLegend.VERTICAL;
//        } else {
//            return ImageLegend.HORIZONTAL;
//        }
    }

    // todo Danny


    private void setOrientation(String value) {
        try {
            String valueCurrent = getOrientation();

            if (valueCurrent == null || (valueCurrent != null && !valueCurrent.equals(value))) {
                getConfiguration().getProperty(ColorBarLayerType.PROPERTY_ORIENTATION_KEY).setValue((Object) value);
            }
        } catch (ValidationException v) {
        }
    }


    public boolean isHorizontalColorBar() {
        if (ColorBarLayerType.OPTION_BEST_FIT.equals(getOrientation())) {
            double sceneAspectRatio = (raster.getRasterHeight() != 0) ? (double) raster.getRasterWidth() / (double) raster.getRasterHeight() : 1.0;
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
    }

    ;


    private double getSceneAspectBestFit() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_SCENE_ASPECT_BEST_FIT_KEY,
                ColorBarLayerType.PROPERTY_SCENE_ASPECT_BEST_FIT_DEFAULT);
    }

    private String getTitleVerticalAnchor() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_DEFAULT);
    }


    private boolean isReversePalette() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_KEY,
                ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_DEFAULT);
    }


    // Tick Label Values


    private String getLabelValuesMode() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_DEFAULT);
    }

    private void setLabelValuesMode(String value) {
        try {
            getConfiguration().getProperty(ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_KEY).setValue((Object) value);
        } catch (ValidationException v) {
        }
    }

    private int getLabelValuesCount() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_DEFAULT);
    }

    private String getLabelValuesActual() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_DEFAULT);
    }

    private void setLabelValuesActual(String value) {
        try {
            getConfiguration().getProperty(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_KEY).setValue((Object) value);
        } catch (ValidationException v) {
        }
    }

    private boolean getPopulateLabelsTextfield() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_POPULATE_VALUES_TEXTFIELD_KEY,
                ColorBarLayerType.PROPERTY_POPULATE_VALUES_TEXTFIELD_DEFAULT);
    }

    private void setPopulateLabelsTextfield(boolean value) {
        try {
            getConfiguration().getProperty(ColorBarLayerType.PROPERTY_POPULATE_VALUES_TEXTFIELD_KEY).setValue((Object) value);
        } catch (ValidationException v) {
        }
    }


    private double getLabelValuesScalingFactor() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_DEFAULT);
    }

    private void setLabelValuesScalingFactor(double value) {
        try {
            getConfiguration().getProperty(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_KEY).setValue((Object) value);
        } catch (ValidationException v) {
        }
    }

    private int getDecimalPlaces() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_DEFAULT);
    }

    private boolean getDecimalPlacesForce() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_DEFAULT);
    }


    private Double getWeightTolerance() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_WEIGHT_TOLERANCE_KEY,
                ColorBarLayerType.PROPERTY_WEIGHT_TOLERANCE_DEFAULT);
    }


    // Placement Location

    private boolean isColorBarLocationInside() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LOCATION_INSIDE_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_INSIDE_DEFAULT);
    }


    private String getColorBarLocationHorizontalPlacement() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_DEFAULT);
    }


    private void setColorBarLocationHorizontalPlacement(String value) {
        try {
            getConfiguration().getProperty(ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_KEY).setValue((Object) value);
        } catch (ValidationException v) {
        }
    }

    private String getColorBarLocationVerticalPlacement() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_VERTICAL_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_VERTICAL_DEFAULT);
    }


    private void setColorBarLocationVerticalPlacement(String value) {
        try {
            getConfiguration().getProperty(ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_VERTICAL_KEY).setValue((Object) value);
        } catch (ValidationException v) {
        }
    }


    private Double getLocationOffsetOutside() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LOCATION_GAP_FACTOR_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_GAP_FACTOR_DEFAULT);
    }

    private Double getLocationOffsetInside() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LOCATION_OFFSET_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_OFFSET_DEFAULT);
    }

    private void setLocationOffset(double value) {
        System.out.println("INSIDE setLocationOffset");

        try {
            getConfiguration().getProperty(ColorBarLayerType.PROPERTY_LOCATION_OFFSET_KEY).setValue((Object) value);
        } catch (ValidationException v) {
        }
    }

    private Double getLocationShift() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LOCATION_SHIFT_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_SHIFT_DEFAULT);
    }

    private void setLocationShift(double value) {
        try {
            getConfiguration().getProperty(ColorBarLayerType.PROPERTY_LOCATION_SHIFT_KEY).setValue((Object) value);
        } catch (ValidationException v) {
        }
    }


    // Size & Scaling

    private String applySizeScaling() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_KEY,
                ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_DEFAULT);
    }


    private Double getLayerScaling() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_KEY,
                ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_DEFAULT);
    }


    private int getColorBarLength() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_KEY,
                ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_DEFAULT);
    }

    private void setColorBarLength(int value) {
        try {
            getConfiguration().getProperty(ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_KEY).setValue((Object) value);
        } catch (ValidationException v) {
        }
    }

    private int getColorBarWidth() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_DEFAULT);
    }


    // Title Format

    private boolean isShowTitle() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY,
                ColorBarLayerType.PROPERTY_TITLE_SHOW_DEFAULT);
    }


    private int getTitleFontSize() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_KEY,
                ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_DEFAULT);
    }

    private Boolean isTitleFontBold() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_KEY,
                ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_DEFAULT);
    }

    private Boolean isTitleFontItalic() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_KEY,
                ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_DEFAULT);
    }


    private int getTitleFontType() {
        return getFontType(isTitleFontItalic(), isTitleFontBold());
    }

    private String getTitleFontName() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_KEY,
                ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_DEFAULT);
    }

    private Color getTitleColor() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_COLOR_KEY,
                ColorBarLayerType.PROPERTY_TITLE_COLOR_DEFAULT);
    }


    // Units Format

    private boolean isShowTitleUnits() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_UNITS_SHOW_KEY,
                ColorBarLayerType.PROPERTY_UNITS_SHOW_DEFAULT);
    }

    private int getUnitsFontSize() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_KEY,
                ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_DEFAULT);
    }

    private Boolean isTitleUnitsFontBold() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_UNITS_FONT_BOLD_KEY,
                ColorBarLayerType.PROPERTY_UNITS_FONT_BOLD_DEFAULT);
    }

    private Boolean isTitleUnitsFontItalic() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_UNITS_FONT_ITALIC_KEY,
                ColorBarLayerType.PROPERTY_UNITS_FONT_ITALIC_DEFAULT);
    }

    private int getUnitsFontType() {
        return getFontType(isTitleUnitsFontItalic(), isTitleUnitsFontBold());
    }

    private String getUnitsFontName() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_UNITS_FONT_NAME_KEY,
                ColorBarLayerType.PROPERTY_UNITS_FONT_NAME_DEFAULT);
    }

    private Color getUnitsColor() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_KEY,
                ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_DEFAULT);
    }


    // Tick Label Format

    private boolean isLabelsShow() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY,
                ColorBarLayerType.PROPERTY_LABELS_SHOW_DEFAULT);
    }

    private int getLabelsFontSize() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_KEY,
                ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_DEFAULT);
    }

    private Boolean isLabelsBold() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_KEY,
                ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_DEFAULT);
    }

    private Boolean isLabelsItalic() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_KEY,
                ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_DEFAULT);
    }

    private String getLabelsFontName() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_KEY,
                ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_DEFAULT);
    }

    private int getLabelsFontType() {
        return getFontType(isLabelsItalic(), isLabelsBold());
    }


    private Color getLabelsColor() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_KEY,
                ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_DEFAULT);
    }


    // Tickmarks

    private boolean isTickmarksShow() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY,
                ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_DEFAULT);
    }


    private int getTickmarksLength() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_KEY,
                ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_DEFAULT);
    }

    private int getTickmarksWidth() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_DEFAULT);
    }

    private Color getTickmarksColor() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY,
                ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT);
    }


    // Palette Border

    private boolean isBorderShow() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_KEY,
                ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_DEFAULT);
    }

    private Color getBorderColor() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_KEY,
                ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_DEFAULT);
    }

    private int getBorderWidth() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_DEFAULT);
    }


    // Legend Border

    private boolean isBackdropBorderShow() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_DEFAULT);
    }

    private int getBackdropBorderWidth() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_DEFAULT);
    }

    private Color getBackdropBorderColor() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_DEFAULT);
    }


    // Legend Backdrop

    private boolean isBackdropShow() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_BACKDROP_SHOW_KEY,
                ColorBarLayerType.PROPERTY_BACKDROP_SHOW_DEFAULT);
    }

    private double getBackdropTransparency() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_KEY,
                ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_DEFAULT);
    }

    private Color getBackdropColor() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_BACKDROP_COLOR_KEY,
                ColorBarLayerType.PROPERTY_BACKDROP_COLOR_DEFAULT);
    }


    // Legend Border Gap

    private double getBorderGapFactorTop() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_DEFAULT);
    }

    private double getBorderGapFactorBottom() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_DEFAULT);
    }

    private double getBorderGapFactorLeftside() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_DEFAULT);
    }

    private double getBorderGapFactorRightside() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_DEFAULT);
    }

    private double getTitleGapFactor() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LEGEND_TITLE_GAP_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_TITLE_GAP_DEFAULT);
    }

    private double getLabelGapFactor() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LEGEND_LABEL_GAP_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_LABEL_GAP_DEFAULT);
    }

    // Some general font methods

    public static int getFontType(boolean italic, boolean bold) {
        if (italic && bold) {
            return Font.ITALIC | Font.BOLD;
        } else if (italic) {
            return Font.ITALIC;
        } else if (bold) {
            return Font.BOLD;
        } else {
            return Font.PLAIN;
        }
    }

    public static boolean isFontTypeBold(int fontType) {
        if (fontType == (Font.ITALIC | Font.BOLD) || fontType == Font.BOLD) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isFontTypeItalic(int fontType) {
        if (fontType == (Font.ITALIC | Font.BOLD) || fontType == Font.ITALIC) {
            return true;
        } else {
            return false;
        }
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
                imageLegend = null;
                fireLayerDataChanged(getModelBounds());
            }
        }
    }


    public ImageLegend getImageLegend() {
        return imageLegend;
    }
}
