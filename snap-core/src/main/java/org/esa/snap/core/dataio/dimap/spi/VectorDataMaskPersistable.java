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

package org.esa.snap.core.dataio.dimap.spi;

import com.bc.ceres.binding.PropertyContainer;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListenerAdapter;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.jdom.Element;

import static org.esa.snap.core.dataio.dimap.DimapProductConstants.ATTRIB_VALUE;
import static org.esa.snap.core.dataio.dimap.DimapProductConstants.TAG_VECTOR_DATA_NODE;

/**
 * @author Sabine Embacher
 * @since SNAP 9.0
 */
class VectorDataMaskPersistable extends MaskPersistable {

    @Override
    protected Mask.ImageType createImageType() {
        return Mask.VectorDataType.INSTANCE;
    }

    @Override
    protected void configureMask(Mask mask, Element element, Product product) {
        final PropertyContainer imageConfig = mask.getImageConfig();
        final String nodeName = getChildAttributeValue(element, TAG_VECTOR_DATA_NODE, ATTRIB_VALUE);
        final VectorDataNode vectorDataNode = product.getVectorDataGroup().get(nodeName);
        final String propertyName = Mask.VectorDataType.PROPERTY_NAME_VECTOR_DATA;
        if (vectorDataNode != null) {
            imageConfig.setValue(propertyName, vectorDataNode);
        } else {
            product.addProductNodeListener(new ProductNodeListenerAdapter(){
                @Override
                public void nodeAdded(ProductNodeEvent event) {
                    final ProductNode sourceNode = event.getSourceNode();
                    final String sourceNodeName = sourceNode.getName();
                    if (!nodeName.equals(sourceNodeName)) return;
                    if (!(sourceNode instanceof VectorDataNode)) return;
                    product.removeProductNodeListener(this);
                    imageConfig.setValue(propertyName, sourceNode);
                }
            });
        }
    }

    @Override
    protected void configureElement(Element root, Mask mask) {
        final VectorDataNode vectorDataNode = mask.getImageConfig()
                .getValue(Mask.VectorDataType.PROPERTY_NAME_VECTOR_DATA);
        root.addContent(createValueAttributeElement(TAG_VECTOR_DATA_NODE, vectorDataNode.getName()));
    }
}
