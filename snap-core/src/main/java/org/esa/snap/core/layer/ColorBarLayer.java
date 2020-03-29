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



    public ColorBarLayer(RasterDataNode raster) {
        this(LAYER_TYPE, raster, initConfiguration(LAYER_TYPE.createLayerConfig(null), raster));
    }

    public ColorBarLayer(ColorBarLayerType type, RasterDataNode raster, PropertySet configuration) {
        super(type, configuration);
        setName("ColorBar Layer");
        this.raster = raster;

        productNodeHandler = new ProductNodeHandler();
        raster.getProduct().addProductNodeListener(productNodeHandler);

        setTransparency(0.0);

// todo Danny this doesn't work but would like to init the param in the Editor with the value
//        configuration.setValue(ColorBarLayerType.PROPERTY_COLORBAR_TITLE_TITLE_ALIAS, raster.getName());
//        configuration.setValue(ColorBarLayerType.PROPERTY_COLORBAR_TITLE_UNITS_ALIAS, raster.getUnit());


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

        getUserValues();

        if (imageLegend == null) {
            imageLegend = new ImageLegend(raster.getImageInfo(), raster);

            String title = (getTitle() != null && getTitle().trim().length() > 0) ? getTitle() : raster.getName();
            String units = (getUnits() != null && getUnits().trim().length() > 0) ? getUnits() : "(" + raster.getUnit() + ")";



            imageLegend.setShowTitle(isShowTitleParameter());
            imageLegend.setHeaderText(title);
            imageLegend.setTitleFontSize(getTitleParameterFontSize());
            imageLegend.setTitleParameterColor(getTitleParameterColor());
            imageLegend.setTitleParameterFontName(getTitleParameterFontName());
            imageLegend.setTitleParameterFontType(getTitleParameterFontType());

            imageLegend.setShowTitleUnits(isShowTitleUnits());
            imageLegend.setHeaderUnitsText(units);
            imageLegend.setTitleUnitsFontSize(getTitleUnitsFontSize());
            imageLegend.setTitleUnitsColor(getTitleUnitsColor());
            imageLegend.setTitleUnitsFontName(getTitleUnitsFontName());
            imageLegend.setTitleUnitsFontType(getTitleUnitsFontType());


            imageLegend.setNumberOfTicks(getLabelValuesCount());
            imageLegend.setDistributionType(getLabelValuesMode());
            imageLegend.setFullCustomAddThesePoints(getLabelValuesActual());


            imageLegend.setOrientation(getOrientation());
            imageLegend.setForegroundColor(getTitleParameterColor());
            imageLegend.setTickmarkColor(getTickmarksColor());
            imageLegend.setTickmarkLength(getTickmarksLength());
            imageLegend.setTickmarkWidth(getTickmarksWidth());
            imageLegend.setTickmarkShow(isTickmarksShow());


            imageLegend.setBorderShow(isBorderShow());
            imageLegend.setBorderWidth(getBorderWidth());
            imageLegend.setBorderColor(getBorderColor());


            imageLegend.setBackgroundColor(getBackdropColor());
            imageLegend.setBackgroundTransparency(((Number) getBackdropTransparency()).floatValue());
            imageLegend.setBackdropShow(isBackdropShow());



            imageLegend.setLabelsFontName(getLabelsFontName());
            imageLegend.setLabelsFontType(getLabelsFontType());
            imageLegend.setLabelsColor(getLabelsColor());





            imageLegend.setLabelsColor(getLabelsColor());
            imageLegend.setAntialiasing((Boolean) true);
            imageLegend.setColorBarLength((Integer) 1200);
            imageLegend.setColorBarThickness((Integer) 60);
            imageLegend.setLabelsFontSize((Integer) getFontSizePixels());
            imageLegend.setScalingFactor((Double) 1.0);
            imageLegend.setLayerScaling((Double) getSizeScaling());



            imageLegend.setBackgroundTransparencyEnabled(true);

            int imageHeight = raster.getRasterHeight();
            int imageWidth = raster.getRasterWidth();
            bufferedImage = imageLegend.createImage(new Dimension(imageWidth, imageHeight), true);


            // Update the properties with some calculated/looked-up values
            setLabelValuesActual(imageLegend.getFullCustomAddThesePoints());
            setTitle(imageLegend.getHeaderText());
            setUnits(imageLegend.getHeaderUnitsText());
        }


        if (imageLegend != null && bufferedImage != null) {


            int imageHeight = raster.getRasterHeight();
            int imageWidth = raster.getRasterWidth();

            if (applySizeScaling()) {
                bufferedImage = imageLegend.createImage(new Dimension(imageWidth, imageHeight), true);
            } else {
                bufferedImage = imageLegend.createImage();
            }



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
  //              transform.concatenate(raster.getSourceImage().getModel().getImageToModelTransform(0));
//
//                transform.concatenate(createTransform(raster, bufferedImage));
//                g2d.drawRenderedImage(bufferedImage, transform);


                g2d.setTransform(transform);
                drawImage(g2d, raster, bufferedImage);

            } finally {
                g2d.setTransform(transformSave);
            }
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
     //   return createTransform(raster, image);
    }

    private AffineTransform createTransform(RasterDataNode raster, RenderedImage colorBarImage) {


        int colorBarImageWidth = colorBarImage.getWidth();
        int colorBarImageHeight = colorBarImage.getHeight();

        int rasterWidth = raster.getRasterWidth();
        int rasterHeight = raster.getRasterHeight();


        double offset = (getOrientation() == ImageLegend.HORIZONTAL) ? (colorBarImageHeight * getLocationOffset() / 100) : (colorBarImageWidth * getLocationOffset() / 100);
        double shift = (getOrientation() == ImageLegend.HORIZONTAL) ? (colorBarImageWidth * getLocationShift() / 100) : -(colorBarImageHeight * getLocationShift() / 100);

        double defaultOffset = 0;
        double defaultShift;

        if (getOrientation() == ImageLegend.HORIZONTAL) {
            defaultShift = (rasterWidth - colorBarImageWidth) / 2;
        } else {
            defaultShift = (rasterHeight - colorBarImageHeight) / 2;
        }




        if (getOrientation() == ImageLegend.HORIZONTAL) {
            if (isColorBarLocationInside()) {
                switch (getColorBarLocationPlacement()) {

                    case ColorBarLayerType.LOCATION_LOWER_LEFT:
                        defaultOffset = -colorBarImageHeight;
                        defaultShift = 0;
                        offset = -offset;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_CENTER:
                        defaultOffset = -colorBarImageHeight;
                        defaultShift = (rasterWidth - colorBarImageWidth) / 2;
                        offset = -offset;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_RIGHT:
                        defaultOffset = -colorBarImageHeight;
                        defaultShift = rasterWidth - colorBarImageWidth;
                        offset = -offset;
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_LEFT:
                        defaultOffset = -rasterHeight;
                        defaultShift = 0;
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_CENTER:
                        defaultOffset = -rasterHeight;
                        defaultShift = (rasterWidth - colorBarImageWidth) / 2;
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_RIGHT:
                        defaultOffset = -rasterHeight;
                        defaultShift = rasterWidth - colorBarImageWidth;
                        break;
                    case ColorBarLayerType.LOCATION_LEFT_CENTER:
                        defaultOffset = -(rasterHeight + colorBarImageHeight) / 2;;
                        defaultShift = 0;
                        break;
                    case ColorBarLayerType.LOCATION_RIGHT_CENTER:
                        defaultOffset = -(rasterHeight + colorBarImageHeight) / 2;;
                        defaultShift = rasterWidth - colorBarImageWidth;
                        break;
                    default:
                        defaultOffset = -colorBarImageHeight;
                        defaultShift = (rasterWidth - colorBarImageWidth) / 2;
                }


            } else {
                switch (getColorBarLocationPlacement()) {

                    case ColorBarLayerType.LOCATION_LOWER_LEFT:
                        defaultOffset = 0;
                        defaultShift = 0;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_CENTER:
                        defaultOffset = 0;
                        defaultShift = (rasterWidth - colorBarImageWidth) / 2;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_RIGHT:
                        defaultOffset = 0;
                        defaultShift = rasterWidth - colorBarImageWidth;
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_LEFT:
                        defaultOffset = -rasterHeight - colorBarImageHeight;
                        defaultShift = 0;
                        offset = -offset;
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_CENTER:
                        defaultOffset = -rasterHeight - colorBarImageHeight;
                        defaultShift = (rasterWidth - colorBarImageWidth) / 2;
                        offset = -offset;
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_RIGHT:
                        defaultOffset = -rasterHeight - colorBarImageHeight;
                        defaultShift = rasterWidth - colorBarImageWidth;
                        offset = -offset;
                        break;
                    default:
                        defaultOffset = 0;
                        defaultShift = (rasterWidth - colorBarImageWidth) / 2;
                }
            }

        } else {
            if (isColorBarLocationInside()) {
                offset = -offset;

                switch (getColorBarLocationPlacement()) {
                    case ColorBarLayerType.LOCATION_UPPER_LEFT:
                        defaultOffset = -rasterWidth;
                        defaultShift = 0;
                        break;
                    case ColorBarLayerType.LOCATION_LEFT_CENTER:
                        defaultOffset = -rasterWidth;
                        defaultShift = (rasterHeight - colorBarImageHeight) / 2;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_LEFT:
                        defaultOffset = -rasterWidth;
                        defaultShift = rasterHeight - colorBarImageHeight;
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_RIGHT:
                        defaultOffset = -colorBarImageWidth;
                        defaultShift = 0;
                        break;
                    case ColorBarLayerType.LOCATION_RIGHT_CENTER:
                        defaultOffset = -colorBarImageWidth;
                        defaultShift = (rasterHeight - colorBarImageHeight) / 2;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_RIGHT:
                        defaultOffset = -colorBarImageWidth;
                        defaultShift = rasterHeight - colorBarImageHeight;
                        break;
                    default:
                        defaultOffset = -colorBarImageWidth;
                        defaultShift = (rasterHeight - colorBarImageHeight) / 2;
                }
            } else {
                switch (getColorBarLocationPlacement()) {
                    case ColorBarLayerType.LOCATION_UPPER_LEFT:
                        defaultOffset = -rasterWidth - colorBarImageWidth;
                        defaultShift = 0;
                        break;
                    case ColorBarLayerType.LOCATION_LEFT_CENTER:
                        defaultOffset = -rasterWidth - colorBarImageWidth;
                        defaultShift = (rasterHeight - colorBarImageHeight) / 2;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_LEFT:
                        defaultOffset = -rasterWidth - colorBarImageWidth;
                        defaultShift = rasterHeight - colorBarImageHeight;
                        break;
                    case ColorBarLayerType.LOCATION_UPPER_RIGHT:
                        defaultOffset = 0;
                        defaultShift = 0;
                        break;
                    case ColorBarLayerType.LOCATION_RIGHT_CENTER:
                        defaultOffset = 0;
                        defaultShift = (rasterHeight - colorBarImageHeight) / 2;
                        break;
                    case ColorBarLayerType.LOCATION_LOWER_RIGHT:
                        defaultOffset = 0;
                        defaultShift = rasterHeight - colorBarImageHeight;
                        break;
                    default:
                        defaultOffset = 0;
                        defaultShift = (rasterHeight - colorBarImageHeight) / 2;
                }
            }
        }

        double y_axis_translation = (getOrientation() == ImageLegend.HORIZONTAL) ? rasterHeight + offset + defaultOffset : shift + defaultShift;
        double x_axis_translation = (getOrientation() == ImageLegend.HORIZONTAL) ? shift + defaultShift : rasterWidth + offset + defaultOffset;
        //double[] flatmatrix = {scaleX, 0.0, 0.0, scaleY, x_axis_translation, y_axis_translation};


        double[] flatmatrix = {1, 0.0, 0.0, 1, x_axis_translation, y_axis_translation};


        AffineTransform i2mTransform = new AffineTransform(flatmatrix);
        return i2mTransform;
    }




    private String getInsideOutsideLocation() {
        return ColorBarParamInfo.LOCATION_INSIDE_STR;
    }


    private String getHorizontalLocation() {
        return ColorBarParamInfo.LOCATION_BOTTOM_RIGHT;
    }

    private String getVerticalLocation() {
        return ColorBarParamInfo.LOCATION_RIGHT_LOWER;
    }





    private void getUserValues() {


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

        if (
                propertyName.equals(ColorBarLayerType.PROPERTY_GRID_SPACING_LAT_NAME) ||
                        propertyName.equals(ColorBarLayerType.PROPERTY_GRID_SPACING_LON_NAME) ||
                        propertyName.equals(ColorBarLayerType.PROPERTY_NUM_GRID_LINES_NAME) ||
                        propertyName.equals(ColorBarLayerType.PROPERTY_LABELS_SIZE_NAME)
                ) {
            imageLegend = null;
        }
        imageLegend = null;

        if (getConfiguration().getProperty(propertyName) != null) {
            getConfiguration().setValue(propertyName, event.getNewValue());
        }


//        if (propertyName.equals(ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_KEY)) {
//            Property property = getConfiguration().getProperty(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY)
//            if (property != null) {
//                property.getContainer().getProperties().set
//            }
//        }

        super.fireLayerPropertyChanged(event);
    }






    private Color getLabelsColor() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LABELS_COLOR_NAME,
                ColorBarLayerType.PROPERTY_LABELS_COLOR_DEFAULT);
    }


    private String getLabelValuesMode() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_DEFAULT);
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
            String valueCurrent = getTitle();
            if (valueCurrent == null || (valueCurrent != null && !valueCurrent.equals(value))) {
                getConfiguration().getProperty(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_KEY).setValue((Object) value);
            }
        } catch (ValidationException v) {
        }
    }




    private int getFontSizePixels() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LABELS_SIZE_NAME,
                ColorBarLayerType.PROPERTY_LABELS_SIZE_DEFAULT);
    }


    private double getPtsToPixelsMultiplier() {

        if (ptsToPixelsMultiplier == NULL_DOUBLE) {
            final double PTS_PER_INCH = 72.0;
            final double PAPER_HEIGHT = 11.0;
            final double PAPER_WIDTH = 8.5;

            double heightToWidthRatioPaper = (PAPER_HEIGHT) / (PAPER_WIDTH);
            double heightToWidthRatioRaster = raster.getRasterHeight() / raster.getRasterWidth();

            if (heightToWidthRatioRaster > heightToWidthRatioPaper) {
                // use height
                ptsToPixelsMultiplier = (1 / PTS_PER_INCH) * (raster.getRasterHeight() / (PAPER_HEIGHT));
            } else {
                // use width
                ptsToPixelsMultiplier = (1 / PTS_PER_INCH) * (raster.getRasterWidth() / (PAPER_WIDTH));
            }
        }

        return ptsToPixelsMultiplier;
    }


    private int getOrientation() {
        String orientation = getConfigurationProperty(ColorBarLayerType.PROPERTY_FORMATTING_ORIENTATION_KEY,
                ColorBarLayerType.PROPERTY_FORMATTING_ORIENTATION_DEFAULT);

        if (ColorBarLayerType.OPTION_VERTICAL.equals(orientation)) {
            return ImageLegend.VERTICAL;
        } else {
            return ImageLegend.HORIZONTAL;
        }
    }






    // Tickmarks Section

    private Color getTickmarksColor() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY,
                ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT);
    }

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



    // Border Section

    private boolean isBorderShow() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_BORDER_SHOW_KEY,
                ColorBarLayerType.PROPERTY_BORDER_SHOW_DEFAULT);
    }

    private Color getBorderColor() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_BORDER_COLOR_KEY,
                ColorBarLayerType.PROPERTY_BORDER_COLOR_DEFAULT);
    }

    private int getBorderWidth() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_BORDER_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_BORDER_WIDTH_DEFAULT);
    }


    // Backdrop Section

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









    private String getLabelsFontName() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LABELS_FONT_NAME,
                ColorBarLayerType.PROPERTY_LABELS_FONT_DEFAULT);
    }

    private Boolean isLabelsItalic() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LABELS_ITALIC_NAME,
                ColorBarLayerType.PROPERTY_LABELS_ITALIC_DEFAULT);
    }

    private Boolean isLabelsBold() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_LABELS_BOLD_NAME,
                ColorBarLayerType.PROPERTY_LABELS_BOLD_DEFAULT);
    }

    private int getLabelsFontType() {
        return getFontType(isLabelsItalic(), isLabelsBold());
    }


    private int getFontType(boolean italic, boolean bold) {
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





    private boolean isColorBarLocationInside() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_INSIDE_NAME,
                ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_INSIDE_DEFAULT);
    }


    private String getColorBarLocationPlacement() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_PLACEMENT_NAME,
                ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_PLACEMENT_DEFAULT);
    }


    private Double getLocationOffset() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_OFFSET_NAME,
                ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_OFFSET_DEFAULT);
    }

    private Double getLocationShift() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_SHIFT_NAME,
                ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_SHIFT_DEFAULT);
    }





    private boolean isShowTitleParameter() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_PARAMETER_SHOW_KEY,
                ColorBarLayerType.PROPERTY_TITLE_PARAMETER_SHOW_DEFAULT);
    }

    private String getTitle() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_PARAMETER_TEXT_KEY,
                ColorBarLayerType.PROPERTY_TITLE_PARAMETER_TEXT_DEFAULT);
    }


    private void setTitle(String value) {
        try {
            String valueCurrent = getTitle();
            if (valueCurrent == null || (valueCurrent != null && !valueCurrent.equals(value))) {
                getConfiguration().getProperty(ColorBarLayerType.PROPERTY_TITLE_PARAMETER_TEXT_KEY).setValue((Object) value);
            }
        } catch (ValidationException v) {
        }
    }


    private Color getTitleParameterColor() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_PARAMETER_COLOR_KEY,
                ColorBarLayerType.PROPERTY_TITLE_PARAMETER_COLOR_DEFAULT);
    }


    private int getTitleParameterFontSize() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_PARAMETER_FONT_SIZE_KEY,
                ColorBarLayerType.PROPERTY_TITLE_PARAMETER_FONT_SIZE_DEFAULT);
    }




    private Boolean isTitleParameterFontItalic() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_PARAMETER_ITALIC_KEY,
                ColorBarLayerType.PROPERTY_TITLE_PARAMETER_ITALIC_DEFAULT);
    }

    private Boolean isTitleParameterFontBold() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_PARAMETER_BOLD_KEY,
                ColorBarLayerType.PROPERTY_TITLE_PARAMETER_BOLD_DEFAULT);
    }

    private int getTitleParameterFontType() {
        return getFontType(isTitleParameterFontItalic(), isTitleParameterFontBold());
    }

    private String getTitleParameterFontName() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_PARAMETER_FONT_KEY,
                ColorBarLayerType.PROPERTY_TITLE_PARAMETER_FONT_DEFAULT);
    }











    private boolean isShowTitleUnits() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_UNITS_SHOW_KEY,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_SHOW_DEFAULT);
    }


    private void setUnits(String value) {
        try {
            String valueCurrent = getUnits();
            if (valueCurrent == null || (valueCurrent != null && !valueCurrent.equals(value))) {
                getConfiguration().getProperty(ColorBarLayerType.PROPERTY_TITLE_UNITS_TEXT_KEY).setValue((Object) value);
            }
        } catch (ValidationException v) {
        }
    }

    private int getTitleUnitsFontSize() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_KEY,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_DEFAULT);
    }



    private Color getTitleUnitsColor() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_UNITS_COLOR_KEY,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_COLOR_DEFAULT);
    }




    private Boolean isTitleUnitsFontItalic() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_UNITS_ITALIC_KEY,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_ITALIC_DEFAULT);
    }

    private Boolean isTitleUnitsFontBold() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_UNITS_BOLD_KEY,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_BOLD_DEFAULT);
    }

    private int getTitleUnitsFontType() {
        return getFontType(isTitleUnitsFontItalic(), isTitleUnitsFontBold());
    }

    private String getTitleUnitsFontName() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_KEY,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_DEFAULT);
    }







    private String getUnits() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_TITLE_UNITS_TEXT_KEY,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_TEXT_DEFAULT);
    }



    private boolean applySizeScaling() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_COLORBAR_SCALING_APPLY_SIZE_SCALING_NAME,
                ColorBarLayerType.PROPERTY_COLORBAR_SCALING_APPLY_SIZE_SCALING_DEFAULT);
    }


    private Double getSizeScaling() {
        return getConfigurationProperty(ColorBarLayerType.PROPERTY_COLORBAR_SCALING_SIZE_SCALING_NAME,
                ColorBarLayerType.PROPERTY_COLORBAR_SCALING_SIZE_SCALING_DEFAULT);
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

}
