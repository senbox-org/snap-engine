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

import org.esa.snap.core.dataio.persistence.Container;
import org.esa.snap.core.dataio.persistence.Property;

import java.awt.geom.AffineTransform;
import java.util.TreeSet;


public class RasterDataNodePersistenceHelper {
    public static final String NAME_ANCILLARY_RELATIONS = "ANCILLARY_RELATIONS";
    public static final String NAME_ANCILLARY_VARIABLES = "ANCILLARY_VARIABLES";
    public static final String NAME_IMAGE_TO_MODEL_TRANSFORM = "IMAGE_TO_MODEL_TRANSFORM";

    public static void addAncillaryElements(Container root, RasterDataNode rasterDataNode) {
        String[] ancillaryRelations = rasterDataNode.getAncillaryRelations();
        root.add(new Property<>(NAME_ANCILLARY_RELATIONS, ancillaryRelations));
        RasterDataNode[] ancillaryVariables = rasterDataNode.getAncillaryVariables();
        final String[] variableNames = new String[ancillaryVariables.length];
        for (int i = 0; i < ancillaryVariables.length; i++) {
            variableNames[i] = ancillaryVariables[i].getName();
        }
        root.add(new Property(NAME_ANCILLARY_VARIABLES, variableNames));
    }

    public static void setAncillaryVariables(Container container, RasterDataNode rasterDataNode, Product product) {
        final Property<?> ancVars = container.getProperty(NAME_ANCILLARY_VARIABLES);
        final String[] variableNames = ancVars.getValueStrings();
        for (String variableName : variableNames) {
            final RasterDataNode variable = product.getRasterDataNode(variableName);
            if (variable != null) {
                rasterDataNode.addAncillaryVariable(variable);
            } else {
                product.addProductNodeListener(new ProductNodeListenerAdapter() {
                    @Override
                    public void nodeAdded(ProductNodeEvent event) {
                        final ProductNode sourceNode = event.getSourceNode();
                        final String sourceNodeName = sourceNode.getName();
                        if (!variableName.equals(sourceNodeName)) {
                            return;
                        }
                        if (!(sourceNode instanceof RasterDataNode)) {
                            return;
                        }
                        product.removeProductNodeListener(this);
                        rasterDataNode.addAncillaryVariable((RasterDataNode) sourceNode);
                    }
                });
            }
        }
    }

    public static void setAncillaryRelations(Container element, RasterDataNode rasterDataNode) {
        final String[] relations = element.getProperty(NAME_ANCILLARY_RELATIONS).getValueStrings();
        if (relations.length > 0) {
            rasterDataNode.setAncillaryRelations(relations);
        }
    }

    public static void addImageToModelTransformElement(Container root, RasterDataNode rasterDataNode) {
        final AffineTransform imageToModelTransform = rasterDataNode.getImageToModelTransform();
        if (!imageToModelTransform.isIdentity()) {
            final double[] matrix = new double[6];
            imageToModelTransform.getMatrix(matrix);
            root.add(new Property(NAME_IMAGE_TO_MODEL_TRANSFORM, matrix));
        }
    }

    public static void setImageToModelTransform(Container container, RasterDataNode rasterDataNode) {
        Property<?> transformProp = container.getProperty(NAME_IMAGE_TO_MODEL_TRANSFORM);
        if (transformProp == null) {
            return;
        }
        final Double[] matrix = transformProp.getValueDoubles();
        if (matrix != null && matrix.length > 0) {
            final double[] doubles = new double[matrix.length];
            for (int i = 0; i < matrix.length; i++) {
                doubles[i] = matrix[i];
            }
            final AffineTransform transform = new AffineTransform(doubles);
            rasterDataNode.setImageToModelTransform(transform);
        }
    }
}
