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
import org.esa.snap.core.dataio.AbstractProductReader;
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

            final int srcX = getSourceX(destRect.x);
            final int srcY = getSourceY(destRect.y);
            if (productReader instanceof AbstractProductReader) {
                AbstractProductReader reader = (AbstractProductReader) productReader;
                int scale = (int) getLevelImageSupport().getScale();
                reader.readBandRasterDataSubsampled(srcX, srcY, sourceWidth, sourceHeight, scale, scale, band, destRect.x, destRect.y,
                                                    destRect.width, destRect.height,
                                                    productData, ProgressMonitor.NULL);
                return;
            }

            // here it is probably better to read the whole tile row at once and then read from this the necessary data
            final ProductData lineData = ProductData.createInstance(getBand().getDataType(), sourceWidth);
            final int[] sourceCoords = getSourceCoords(sourceWidth, destRect.width);
            for (int y = 0; y < destRect.height; y++) {
                productReader.readBandRasterData(band,
                                                 srcX,
                                                 getSourceY(destRect.y + y),
                                                 lineData.getNumElems(), 1,
                                                 lineData,
                                                 ProgressMonitor.NULL);
                copyLine(y, destRect.width, lineData, productData, sourceCoords);
            }
        }
    }
}
