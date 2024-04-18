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

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.annotations.LayerTypeMetadata;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ColoredMaskImageMultiLevelSource;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;

/**
 * A layer used to display the no-data mask of a raster data node.
 *
 * @author Marco Peters
 * @version $ Revision: $ Date: $
 * @since BEAM 4.6
 */
@LayerTypeMetadata(name = "NoDataLayerType", aliasNames = {"org.esa.snap.core.layer.NoDataLayerType"})
public class NoDataLayerType extends ImageLayer.Type {

    public static final String NO_DATA_LAYER_ID = "org.esa.snap.layers.noData";
    public static final String PROPERTY_NAME_COLOR = "color";
    public static final String PROPERTY_NAME_VALID_GEO = "valid.geo";
    public static final String PROPERTY_NAME_RASTER = "raster";
    public static final Color DEFAULT_COLOR = new Color(128, 128, 128);
    public static final boolean DEFAULT_VALID_GEO = true;

    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        final Color color = (Color) configuration.getValue(PROPERTY_NAME_COLOR);
        final boolean validGeo = (boolean) configuration.getValue(PROPERTY_NAME_VALID_GEO);
        Assert.notNull(color, PROPERTY_NAME_COLOR);
        final RasterDataNode raster = (RasterDataNode) configuration.getValue(PROPERTY_NAME_RASTER);

        MultiLevelSource multiLevelSource = (MultiLevelSource) configuration.getValue(
                ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE);
        if (multiLevelSource == null) {
            multiLevelSource = createMultiLevelSource(color, raster, validGeo);
            configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
        }

        final ImageLayer noDataLayer = new ImageLayer(this, multiLevelSource, configuration);
        noDataLayer.addListener(new AbstractLayerListener() {
            @Override
            public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
                if (event.getPropertyName().equals(PROPERTY_NAME_COLOR) || event.getPropertyName().equals(PROPERTY_NAME_VALID_GEO)) {
                    renewMultiLevelSource(noDataLayer, (Color) event.getNewValue());
                }
            }
        });
        noDataLayer.setName("No-Data Layer");
        noDataLayer.setId(NO_DATA_LAYER_ID);
        noDataLayer.setVisible(false);
        return noDataLayer;
    }

    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertySet prototype = super.createLayerConfig(ctx);

        prototype.addProperty(Property.create(PROPERTY_NAME_RASTER, RasterDataNode.class));
        prototype.getDescriptor(PROPERTY_NAME_RASTER).setNotNull(true);

        prototype.addProperty(Property.create(PROPERTY_NAME_COLOR, Color.class, DEFAULT_COLOR, true));
        prototype.addProperty(Property.create(PROPERTY_NAME_VALID_GEO, Boolean.class, DEFAULT_VALID_GEO, true));

        return prototype;

    }

    public static void renewMultiLevelSource(ImageLayer layer, Color newColor) {
        final PropertySet configuration = layer.getConfiguration();
        final RasterDataNode raster = (RasterDataNode) configuration.getValue(PROPERTY_NAME_RASTER);
        final boolean validGeo = configuration.getValue(PROPERTY_NAME_VALID_GEO);
        final MultiLevelSource source = createMultiLevelSource(newColor, raster, validGeo);
        configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, source);
        layer.setMultiLevelSource(source);
    }

    private static MultiLevelSource createMultiLevelSource(Color newColor, RasterDataNode raster, boolean validGeo) {
        MultiLevelSource source;
        if (raster.getValidMaskExpression() != null) {
            String expression = raster.getValidMaskExpression();
            if (validGeo) {
                    if (raster.getValidMaskExpression().trim().length() > 0) {
                        expression = raster.getValidMaskExpression().trim() + " or LAT < -90 or LAT > 90 or nan(LAT) or nan(LON)";
                    } else {
                        expression = "LAT < -90 or LAT > 90 or nan(LAT) or nan(LON)";
                    }
            }


            final AffineTransform transform = raster.getSourceImage().getModel().getImageToModelTransform(0);
            source = ColoredMaskImageMultiLevelSource.create(raster.getProduct(),
                                                             newColor,
                                                             expression,
                                                             true,
                                                             transform);
        } else {
            source = MultiLevelSource.NULL;
        }
        return source;
    }
}


