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
import org.esa.snap.core.dataio.persistence.Property;

/**
 * @author Sabine Embacher
 * @since SNAP 9.0
 */
public class VectorDataMaskPersistenceConverter extends AbstractMaskPersistenceConverter {

    // Never change this constant! Instead, create a new one with the
    // name ID_VERSION_2, as ID_VERSION_1 will be used in HistoricalDecoder0.
    // And so on ...
    public static final String ID_VERSION_1 = "VD_MASK:1";

    @Override
    public String getID() {
        return ID_VERSION_1;
    }

    @Override
    protected Mask.ImageType createImageType() {
        return Mask.VectorDataType.INSTANCE;
    }

    @Override
    protected void configureMask(Mask mask, Container root, Product product) {
        final PropertyContainer imageConfig = mask.getImageConfig();
        final String propertyName = Mask.VectorDataType.PROPERTY_NAME_VECTOR_DATA;
        final String vectorDataNodeName = root.getProperty(propertyName).getValueString();
        final VectorDataNode vectorDataNode = product.getVectorDataGroup().get(vectorDataNodeName);
        if (vectorDataNode == null) {
            product.addProductNodeListener(new ProductNodeListenerAdapter() {
                @Override
                public void nodeAdded(ProductNodeEvent event) {
                    final ProductNode sourceNode = event.getSourceNode();
                    final String sourceNodeName = sourceNode.getName();
                    if (!vectorDataNodeName.equals(sourceNodeName)) return;
                    if (!(sourceNode instanceof VectorDataNode)) return;
                    product.removeProductNodeListener(this);
                    imageConfig.setValue(propertyName, sourceNode);
                }
            });
        } else {
            imageConfig.setValue(propertyName, vectorDataNode);
        }
    }

    @Override
    protected void configureContainer(Container root, Mask mask) {
        final String propertyName = Mask.VectorDataType.PROPERTY_NAME_VECTOR_DATA;
        final VectorDataNode vectorDataNode = mask.getImageConfig().getValue(propertyName);

        root.add(new Property<>(propertyName, vectorDataNode.getName()));
    }
}
