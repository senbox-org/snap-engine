/*
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

package org.esa.snap.core.image;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;

import java.awt.Rectangle;
import java.io.IOException;


/**
 * An {@code OpImage} which retrieves its data from the product reader associated with the
 * given {@code RasterDataNode} at a given pyramid level.
 */
public class BandOpImage extends RasterDataNodeOpImage {

    public BandOpImage(Band band) {
        this(band, ResolutionLevel.MAXRES);
    }

    public BandOpImage(Band band, ResolutionLevel level) {
        super(band, level);
        if (Boolean.getBoolean("snap.imageManager.disableSourceTileCaching")) {
            setTileCache(null);
        }
    }

    public Band getBand() {
        return (Band) getRasterDataNode();
    }

    @Override
    protected void computeProductData(ProductData productData, Rectangle destRect) throws IOException {
        Band band = getBand();
        final ProductReader productReader = band.getProductReader();
        if (productReader == null) {
            throw new IllegalStateException("no product reader for band '" + band.getDisplayName() + "'");
        }
        if (getLevel() == 0) {
            productReader.readBandRasterData(band, destRect.x, destRect.y,
                                             destRect.width, destRect.height,
                                             productData,
                                             ProgressMonitor.NULL);
        } else {
            final int sourceWidth = getSourceWidth(destRect.width);
            final int sourceHeight = getSourceHeight(destRect.height);

            final ProductData regionData = ProductData.createInstance(band.getDataType(), sourceWidth * sourceHeight);
            final int srcX = getSourceX(destRect.x);
            final int srcY = getSourceY(destRect.y);
            productReader.readBandRasterData(band, srcX, srcY,
                                             sourceWidth, sourceHeight,
                                             regionData, ProgressMonitor.NULL);
            for (int y = 0; y < destRect.height; y++) {
                int sourceY = getSourceY(y);
                int sourceOffsetY = sourceY * sourceWidth;
                int destOffsetY = y * destRect.width;
                for (int x = 0; x < destRect.width; x++) {
                    int sourceX = getSourceX(x);
                    switch (regionData.getType()) {
                        case ProductData.TYPE_INT8:
                        case ProductData.TYPE_INT16:
                        case ProductData.TYPE_INT32:
                            productData.setElemIntAt(destOffsetY + x, regionData.getElemIntAt(sourceOffsetY + sourceX));
                            break;
                        case ProductData.TYPE_UINT8:
                        case ProductData.TYPE_UINT16:
                        case ProductData.TYPE_UINT32:
                            productData.setElemUIntAt(destOffsetY + x, regionData.getElemUIntAt(sourceOffsetY + sourceX));
                            break;
                        case ProductData.TYPE_FLOAT32:
                            productData.setElemFloatAt(destOffsetY + x, regionData.getElemFloatAt(sourceOffsetY + sourceX));
                            break;
                        case ProductData.TYPE_FLOAT64:
                            productData.setElemDoubleAt(destOffsetY + x, regionData.getElemDoubleAt(sourceOffsetY + sourceX));
                            break;
                        case ProductData.TYPE_ASCII:
                        case ProductData.TYPE_UTC:
                        default:
                            throw new IllegalArgumentException("wrong product data type: " + regionData.getType());
                    }

                }
            }
            regionData.dispose();
        }
    }
}
