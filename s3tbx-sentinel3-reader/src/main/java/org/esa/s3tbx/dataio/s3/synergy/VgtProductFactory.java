package org.esa.s3tbx.dataio.s3.synergy;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.s3tbx.dataio.s3.AbstractProductFactory;
import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.SourceImageScaler;

import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class VgtProductFactory extends AbstractProductFactory {

    public VgtProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        return manifest.getFileNames(new String[0]);
    }

    @Override
    protected void configureTargetNode(Band sourceBand, RasterDataNode targetNode) {
        if (targetNode instanceof Band) {
            final MetadataElement variableAttributes =
                    sourceBand.getProduct().getMetadataRoot().getElement("Variable_Attributes");
            if (variableAttributes != null) {
                final MetadataElement metadataElement = variableAttributes.getElement(sourceBand.getName());
                if (metadataElement != null) {
                    final MetadataAttribute bandwidthAttribute = metadataElement.getAttribute("bandwidth");
                    if (bandwidthAttribute != null) {
                        ((Band) targetNode).setSpectralBandwidth(bandwidthAttribute.getData().getElemFloat());
                    }
                    final MetadataAttribute wavelengthAttribute = metadataElement.getAttribute("wavelength");
                    if (wavelengthAttribute != null) {
                        ((Band) targetNode).setSpectralWavelength(wavelengthAttribute.getData().getElemFloat());
                    }
                }
            }
        }
    }

    @Override
    protected Band addSpecialNode(Product masterProduct, Band sourceBand, Product targetProduct) {
        final Band targetBand = copyBand(sourceBand, targetProduct, false);
        final RenderingHints renderingHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                                                 BorderExtender.createInstance(
                                                                         BorderExtender.BORDER_COPY)
        );
        final MultiLevelImage sourceImage = sourceBand.getSourceImage();
        float[] scalings = new float[]{((float) targetBand.getRasterWidth()) / sourceBand.getRasterWidth(),
                ((float) targetBand.getRasterHeight()) / sourceBand.getRasterHeight()};
        final MultiLevelImage scaledImage = SourceImageScaler.scaleMultiLevelImage(targetBand.getSourceImage(),
                                                                                   sourceImage, scalings, null,
                                                                                   renderingHints, sourceBand.getNoDataValue(),
                                                                                   Interpolation.getInstance(
                                                                                           Interpolation.INTERP_NEAREST)
        );
        targetBand.setSourceImage(scaledImage);
        return targetBand;
    }

    @Override
    protected Product readProduct(String fileName, Manifest manifest) throws IOException {
        final File file = new File(getInputFileParentDirectory(), fileName);
        if (!file.exists()) {
            return null;
        }
        return new VgtReader().readProductNodes(file, null);
    }

}
