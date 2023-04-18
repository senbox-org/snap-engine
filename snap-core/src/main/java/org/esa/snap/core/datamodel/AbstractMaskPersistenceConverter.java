/*
 *
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 */

package org.esa.snap.core.datamodel;

import com.bc.ceres.binding.PropertyContainer;
import org.esa.snap.core.dataio.persistence.Container;
import org.esa.snap.core.dataio.persistence.HistoricalDecoder;
import org.esa.snap.core.dataio.persistence.Item;
import org.esa.snap.core.dataio.persistence.PersistenceConverter;
import org.esa.snap.core.dataio.persistence.Property;

import java.awt.Color;

import static org.esa.snap.core.datamodel.RasterDataNodePersistenceHelper.addAncillaryElements;
import static org.esa.snap.core.datamodel.RasterDataNodePersistenceHelper.addImageToModelTransformElement;
import static org.esa.snap.core.datamodel.RasterDataNodePersistenceHelper.setAncillaryRelations;
import static org.esa.snap.core.datamodel.RasterDataNodePersistenceHelper.setAncillaryVariables;
import static org.esa.snap.core.datamodel.RasterDataNodePersistenceHelper.setImageToModelTransform;

/**
 * @author Marco Peters
 * @author Sabine Embacher
 * @since SNAP 9.0
 */
public abstract class AbstractMaskPersistenceConverter extends PersistenceConverter<Mask> {

    private static final String NAME_MASK = "Mask";
    private static final String NAME_NAME = "NAME";
    private static final String NAME_MASK_RASTER_WIDTH = "MASK_RASTER_WIDTH";
    private static final String NAME_MASK_RASTER_HEIGHT = "MASK_RASTER_HEIGHT";
    private static final String NAME_DESCRIPTION = "DESCRIPTION";
    private static final String NAME_TRANSPARENCY = "TRANSPARENCY";
    private static final String NAME_TYPE = "type";
    private static final String NAME_COLOR = "COLOR";
    private static final String NAME_RED = "red";
    private static final String NAME_GREEN = "green";
    private static final String NAME_BLUE = "blue";
    private static final String NAME_ALPHA = "alpha";

    @Override
    protected Mask decodeImpl(Item item, Product product) {
        final Container root = item.asContainer();
        final String name = root.getProperty(NAME_NAME).getValueString();
        final int width;
        final int height;
        if (root.getProperty(NAME_MASK_RASTER_WIDTH) != null && root.getProperty(NAME_MASK_RASTER_HEIGHT) != null) {
            width = root.getProperty(NAME_MASK_RASTER_WIDTH).getValueInt();
            height = root.getProperty(NAME_MASK_RASTER_HEIGHT).getValueInt();
        } else {
            width = product.getSceneRasterWidth();
            height = product.getSceneRasterHeight();
        }

        final Mask mask = new Mask(name, width, height, createImageType());
        mask.setDescription(root.getProperty(NAME_DESCRIPTION).getValueString());
        mask.setImageTransparency(root.getProperty(NAME_TRANSPARENCY).getValueDouble());
        setImageColor(root, mask);
        setImageToModelTransform(root, mask);
        setAncillaryRelations(root, mask);
        setAncillaryVariables(root, mask, product);
        configureMask(mask, root, product);
        return mask;
    }

    @Override
    public Item encode(Mask mask) {
        final Container root = createRootContainer(NAME_MASK);
        root.add(new Property<>(NAME_TYPE, mask.getImageType().getName()));
        root.add(new Property<>(NAME_NAME, mask.getName()));
        root.add(new Property<>(NAME_MASK_RASTER_WIDTH, mask.getRasterWidth()));
        root.add(new Property<>(NAME_MASK_RASTER_HEIGHT, mask.getRasterHeight()));
        root.add(new Property<>(NAME_DESCRIPTION, mask.getDescription()));
        addAncillaryElements(root, mask);
        addImageConfigElements(root, mask);
        addImageToModelTransformElement(root, mask);
        configureContainer(root, mask);
        return root;
    }

    @Override
    public HistoricalDecoder[] getHistoricalDecoders() {
        return new HistoricalDecoder[0];
    }

    private void setImageColor(Container container, Mask mask) {
        final Container colorContainer = container.getContainer(NAME_COLOR);
        final int r = colorContainer.getProperty(NAME_RED).getValueInt();
        final int g = colorContainer.getProperty(NAME_GREEN).getValueInt();
        final int b = colorContainer.getProperty(NAME_BLUE).getValueInt();
        final int a = colorContainer.getProperty(NAME_ALPHA).getValueInt();
        mask.setImageColor(new Color(r, g, b, a));
    }

    private void addImageConfigElements(Container root, Mask mask) {
        final Container colorContainer = new Container(NAME_COLOR);
        final PropertyContainer config = mask.getImageConfig();
        final Color color = config.getValue(Mask.ImageType.PROPERTY_NAME_COLOR);
        colorContainer.add(new Property<>(NAME_RED, color.getRed()));
        colorContainer.add(new Property<>(NAME_GREEN, color.getGreen()));
        colorContainer.add(new Property<>(NAME_BLUE, color.getBlue()));
        colorContainer.add(new Property<>(NAME_ALPHA, color.getAlpha()));
        root.add(colorContainer);
        root.add(new Property<>(NAME_TRANSPARENCY, mask.getImageTransparency()));
    }

    protected abstract Mask.ImageType createImageType();

    protected abstract void configureMask(Mask mask, Container root, Product product);

    protected abstract void configureContainer(Container root, Mask mask);
}
