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
import org.esa.snap.core.util.SystemUtils;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;


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

    @Override
    public void renderLayer(Rendering rendering) {
//        System.out.println("Rendering Layer");

        if (allowImageLegendReset == true) {
            allowImageLegendReset = false;


            imageLegend = new ImageLegend(raster.getImageInfo(), raster);

            String title = (ColorBarLayerType.NULL_SPECIAL.equals(getTitle())) ? raster.getName() : getTitle();


            String unitsText = "";
            if (ColorBarLayerType.NULL_SPECIAL.equals(getUnits())) {
                String unit = raster.getUnit();
                if (unit != null && unit.length() > 0) {
                    unitsText = "(" + raster.getUnit() + ")";
                }
            } else {
                unitsText = getUnits();
            }


            // Title & Units Text
            imageLegend.setTitleText(title);
            imageLegend.setUnitsText(unitsText);


            // Orientation
            imageLegend.setOrientation(getOrientation());
            imageLegend.setTitleVerticalAnchor(getTitleVerticalAnchor());
            imageLegend.setReversePalette(isReversePalette());


            // Tick Label Values

//             todo Color Schemes - this would call methods in snap-desktop so some of those methods would need to be either moved or replicated in snap-engine
            if (!imageLegendInitialized) {
                if (isAutoApplySchmes()) {//auto-apply
                    ColorSchemeInfo schemeInfo = getColorPaletteInfoByBandNameLookup(raster.getName());
                    if (schemeInfo.getColorBarLabels() != null && schemeInfo.getColorBarLabels().trim().length() > 1) {
                        setLabelValuesActual(schemeInfo.getColorBarLabels());
                        setLabelValuesMode(ColorBarLayerType.DISTRIB_MANUAL_STR);
                        setPopulateLabelsTextfield(true);
                    }
                    // color bar title
                    String colorBarTitle = schemeInfo.getColorBarTitle();
                    boolean colorBarTitleReplaceFailed = false;
                    String description = schemeInfo.getDescription();
                    String bandname = raster.getName();
                    float wavelength = raster.getProduct().getBand(raster.getName()).getSpectralWavelength();
                    String wvlStr = "";
                    if (wavelength > 0.0) {
                        if (Math.ceil(wavelength) == Math.round(wavelength)) {
                            wvlStr = String.valueOf(Math.round(wavelength));
                        } else {
                            wvlStr = String.valueOf(wavelength);
                        }
                    }
                    if (colorBarTitle != null && colorBarTitle.trim().length() > 0) {
                        if (colorBarTitle.contains("[WAVELENGTH]")) {
                            if (wavelength > 0.0) {
                                while (colorBarTitle.contains("[WAVELENGTH]")) {
                                    colorBarTitle = colorBarTitle.replace("[WAVELENGTH]", wvlStr);
                                }
                            } else {
                                colorBarTitleReplaceFailed = true;
                            }
                        }
                        if (!colorBarTitleReplaceFailed) {
                            while(colorBarTitle.contains("[DESCRIPTION]")) {
                                colorBarTitle = colorBarTitle.replace("[DESCRIPTION]", description);
                            }
                            while(colorBarTitle.contains("[BANDNAME]")) {
                                colorBarTitle = colorBarTitle.replace("[BANDNAME]", bandname);
                            }
                            if (colorBarTitle.length() == 0) {
                                colorBarTitleReplaceFailed = true;
                            }
                        }
                    }  else {
                        colorBarTitleReplaceFailed = true;
                    }
                    if (colorBarTitleReplaceFailed) {
                        colorBarTitle = schemeInfo.getColorBarTitleAlt();
                        colorBarTitleReplaceFailed = false;
                        if (colorBarTitle != null && colorBarTitle.trim().length() > 0) {
                            if (colorBarTitle.contains("[WAVELENGTH]")) {
                                if (wavelength > 0.0) {
                                    while (colorBarTitle.contains("[WAVELENGTH]")) {
                                        colorBarTitle = colorBarTitle.replace("[WAVELENGTH]", wvlStr);
                                    }
                                } else {
                                    colorBarTitleReplaceFailed = true;
                                }
                            }
                            if (!colorBarTitleReplaceFailed) {
                                while(colorBarTitle.contains("[DESCRIPTION]")) {
                                    colorBarTitle = colorBarTitle.replace("[DESCRIPTION]", description);
                                }
                                while(colorBarTitle.contains("[BANDNAME]")) {
                                    colorBarTitle = colorBarTitle.replace("[BANDNAME]", bandname);
                                }
                                if (colorBarTitle.length() == 0) {
                                    colorBarTitleReplaceFailed = true;
                                }
                            }
                        } else {
                            colorBarTitleReplaceFailed = true;
                        }
                    }
                    if (colorBarTitleReplaceFailed) {
                        colorBarTitle = raster.getName();
                    }
                    setTitle(colorBarTitle);
                    imageLegend.setTitleText(colorBarTitle);
                    if (schemeInfo.getColorBarUnits() != null && schemeInfo.getColorBarUnits().trim().length() > 0) {
                        setUnits(schemeInfo.getColorBarUnits());
                        imageLegend.setUnitsText(schemeInfo.getColorBarUnits());
                    } else {
                        String unit = raster.getUnit();
                        if (unit != null && unit.length() > 0) {
                            unitsText = "(" + raster.getUnit() + ")";
                        } else {
                            unitsText = "";
                        }
                        setUnits(unitsText);
                        imageLegend.setUnitsText(unitsText);
                    }
                    if (schemeInfo.getColorBarLengthStr() != null && schemeInfo.getColorBarLengthStr().trim().length() > 0) {
                        setColorBarLength(Integer.valueOf(schemeInfo.getColorBarLengthStr()));
                    }
                    if (schemeInfo.getColorBarLabelScalingStr() != null && schemeInfo.getColorBarLabelScalingStr().trim().length() > 0) {
                        setLabelValuesScalingFactor(Double.valueOf(schemeInfo.getColorBarLabelScalingStr()));
                    }
                    imageLegendInitialized = true;
                } else {
//                    setLabelValuesActual(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_DEFAULT);
//                    setLabelValuesMode(ColorBarLayerType.DISTRIB_EVEN_STR);
//                    setPopulateLabelsTextfield(ColorBarLayerType.PROPERTY_POPULATE_VALUES_TEXTFIELD_DEFAULT);
//                    setColorBarLength(ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_DEFAULT);
//                    setLabelValuesScalingFactor(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_DEFAULT);
                    setTitle(raster.getName());
                    imageLegend.setTitleText(raster.getName());
                    String unit = raster.getUnit();
                    if (unit != null && unit.length() > 0) {
                        unitsText = "(" + raster.getUnit() + ")";
                    } else {
                        unitsText = "";
                    }
                    setUnits(unitsText);
                    imageLegend.setUnitsText(unitsText);
                    imageLegendInitialized = true;
                }
            }

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



            imageLegend.setTransparencyEnabled(true);
            imageLegend.setAntialiasing((Boolean) true);



            int imageHeight = raster.getRasterHeight();
            int imageWidth = raster.getRasterWidth();


            if (applySizeScaling()) {
                bufferedImage = imageLegend.createImage(new Dimension(imageWidth, imageHeight), true);
            } else {
                bufferedImage = imageLegend.createImage();
            }


            // Update the properties with some calculated/looked-up values

            if (getPopulateLabelsTextfield()) {
                setLabelValuesActual(imageLegend.getCustomLabelValues());
            }

            setTitle(imageLegend.getTitleText());

//            setUnits(unitsText);
            setUnits(imageLegend.getUnitsText());


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


    public static ColorSchemeInfo getColorPaletteInfoByBandNameLookup(String bandName) {

        // todo

        ColorBarSchemeManager colorBarSchemeManager = ColorBarSchemeManager.getDefault();
        if (colorBarSchemeManager != null) {

            bandName = bandName.trim();
//            bandName = bandName.substring(bandName.indexOf(" ")).trim();

            ArrayList<ColorSchemeLookupInfo> colorSchemeLookupInfos = colorBarSchemeManager.getColorSchemeLookupInfos();
            for (ColorSchemeLookupInfo colorSchemeLookupInfo : colorSchemeLookupInfos) {
                if (colorSchemeLookupInfo.isMatch(bandName)) {
                    return colorBarSchemeManager.getColorSchemeInfoBySchemeId(colorSchemeLookupInfo.getScheme_id());
                }
            }
        }

        return null;
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


        double offset = (getOrientation() == ImageLegend.HORIZONTAL) ? -(colorBarImageHeight * getLocationOffset() / 100) : (colorBarImageWidth * getLocationOffset() / 100);
        double shift = (getOrientation() == ImageLegend.HORIZONTAL) ? (colorBarImageWidth * getLocationShift() / 100) : -(colorBarImageHeight * getLocationShift() / 100);

        double offsetAdjust = 0;
        double shiftAdjust = 0;


        if (getOrientation() == ImageLegend.HORIZONTAL) {
            if (isColorBarLocationInside()) {
                switch (getColorBarLocationPlacement()) {

                    case ColorBarLayerType.LOCATION_LOWER_LEFT:
                        offsetAdjust = -colorBarImageHeight;
                        shiftAdjust = 0;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_CENTER:
                        offsetAdjust = -colorBarImageHeight;
                        shiftAdjust = (rasterWidth - colorBarImageWidth) / 2;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_RIGHT:
                        offsetAdjust = -colorBarImageHeight;
                        shiftAdjust = rasterWidth - colorBarImageWidth;
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
                    case ColorBarLayerType.LOCATION_LEFT_CENTER:
                        offsetAdjust = -(rasterHeight + colorBarImageHeight) / 2;
                        ;
                        shiftAdjust = 0;
                        break;
                    case ColorBarLayerType.LOCATION_RIGHT_CENTER:
                        offsetAdjust = -(rasterHeight + colorBarImageHeight) / 2;
                        ;
                        shiftAdjust = rasterWidth - colorBarImageWidth;
                        break;
                    default:
                        offsetAdjust = -colorBarImageHeight;
                        shiftAdjust = (rasterWidth - colorBarImageWidth) / 2;
                }


            } else {
                switch (getColorBarLocationPlacement()) {

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
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_CENTER:
                        offsetAdjust = -rasterHeight - colorBarImageHeight;
                        shiftAdjust = (rasterWidth - colorBarImageWidth) / 2;
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_RIGHT:
                        offsetAdjust = -rasterHeight - colorBarImageHeight;
                        shiftAdjust = rasterWidth - colorBarImageWidth;
                        break;
                    case ColorBarLayerType.LOCATION_LEFT_CENTER:
                        offsetAdjust = -(rasterHeight + colorBarImageHeight) / 2;
                        shiftAdjust = -colorBarImageWidth;
                        break;
                    case ColorBarLayerType.LOCATION_RIGHT_CENTER:
                        offsetAdjust = -(rasterHeight + colorBarImageHeight) / 2;
                        shiftAdjust = rasterWidth;
                        break;
                    default:
                        offsetAdjust = 0;
                        shiftAdjust = (rasterWidth - colorBarImageWidth) / 2;
                }
            }

        } else {
            if (isColorBarLocationInside()) {
                offset = -offset;

                switch (getColorBarLocationPlacement()) {
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
                        break;
                    case ColorBarLayerType.LOCATION_RIGHT_CENTER:
                        offsetAdjust = -colorBarImageWidth;
                        shiftAdjust = (rasterHeight - colorBarImageHeight) / 2;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_RIGHT:
                        offsetAdjust = -colorBarImageWidth;
                        shiftAdjust = rasterHeight - colorBarImageHeight;
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_CENTER:
                        offsetAdjust = -rasterWidth / 2.0 - colorBarImageWidth / 2.0;
                        shiftAdjust = 0;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_CENTER:
                        offsetAdjust = -rasterWidth / 2.0 - colorBarImageWidth / 2.0;
                        shiftAdjust = rasterHeight - colorBarImageHeight;
                        break;
                    default:
                        offsetAdjust = -colorBarImageWidth;
                        shiftAdjust = rasterHeight - colorBarImageHeight;
                }
            } else {
                switch (getColorBarLocationPlacement()) {
                    case ColorBarLayerType.LOCATION_UPPER_LEFT:
                        offsetAdjust = -rasterWidth - colorBarImageWidth;
                        shiftAdjust = 0;
                        break;
                    case ColorBarLayerType.LOCATION_LEFT_CENTER:
                        offsetAdjust = -rasterWidth - colorBarImageWidth;
                        shiftAdjust = (rasterHeight - colorBarImageHeight) / 2;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_LEFT:
                        offsetAdjust = -rasterWidth - colorBarImageWidth;
                        shiftAdjust = rasterHeight - colorBarImageHeight;
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
                    case ColorBarLayerType.LOCATION_UPPER_CENTER:
                        offsetAdjust = -rasterWidth / 2.0 - colorBarImageWidth / 2.0;
                        shiftAdjust = -colorBarImageHeight;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_CENTER:
                        offsetAdjust = -rasterWidth / 2.0 - colorBarImageWidth / 2.0;
                        shiftAdjust = rasterHeight;
                        break;
                    default:
                        offsetAdjust = 0;
                        shiftAdjust = rasterHeight - colorBarImageHeight;
                }
            }
        }

        double y_axis_translation = (getOrientation() == ImageLegend.HORIZONTAL) ? rasterHeight + offset + offsetAdjust : shift + shiftAdjust;
        double x_axis_translation = (getOrientation() == ImageLegend.HORIZONTAL) ? shift + shiftAdjust : rasterWidth + offset + offsetAdjust;

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




    // Title & Units Text

    private String getTitle() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_TEXT_KEY,
                ColorBarLayerType.PROPERTY_TITLE_TEXT_DEFAULT);
    }

    private void setTitle(String value) {
        try {
            String valueCurrent = getTitle();
            System.out.println("Current title = " + valueCurrent);
            if (valueCurrent == null || (valueCurrent != null && !valueCurrent.equals(value))) {
                System.out.println("Inside and setting title to " + value);
                getConfiguration().getProperty(ColorBarLayerType.PROPERTY_TITLE_TEXT_KEY).setValue((Object) value);
            }
        } catch (ValidationException v) {
        }
    }



    private String getUnits() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_UNITS_TEXT_KEY,
                ColorBarLayerType.PROPERTY_UNITS_TEXT_DEFAULT);
    }

    private void setUnits(String value) {
        try {
            String valueCurrent = getUnits();

            if (valueCurrent == null || (valueCurrent != null && !valueCurrent.equals(value))) {
                getConfiguration().getProperty(ColorBarLayerType.PROPERTY_UNITS_TEXT_KEY).setValue((Object) value);
            }
        } catch (ValidationException v) {
        }
    }





    // Orientation

    private int getOrientation() {
        String orientation = getConfigurationProperty(ColorBarLayerType.PROPERTY_ORIENTATION_KEY,
                ColorBarLayerType.PROPERTY_ORIENTATION_DEFAULT);

        if (ColorBarLayerType.OPTION_VERTICAL.equals(orientation)) {
            return ImageLegend.VERTICAL;
        } else {
            return ImageLegend.HORIZONTAL;
        }
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

    private boolean isAutoApplySchmes() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_SCHEME_AUTO_APPLY_KEY,
                ColorBarLayerType.PROPERTY_SCHEME_AUTO_APPLY_DEFAULT);
    }

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


    private String getColorBarLocationPlacement() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_DEFAULT);
    }

    private Double getLocationOffset() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LOCATION_OFFSET_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_OFFSET_DEFAULT);
    }

    private Double getLocationShift() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LOCATION_SHIFT_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_SHIFT_DEFAULT);
    }



    // Size & Scaling

    private boolean applySizeScaling() {
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
        if (fontType  == (Font.ITALIC | Font.BOLD) || fontType == Font.BOLD) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isFontTypeItalic(int fontType) {
        if (fontType  == (Font.ITALIC | Font.BOLD) || fontType == Font.ITALIC) {
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
