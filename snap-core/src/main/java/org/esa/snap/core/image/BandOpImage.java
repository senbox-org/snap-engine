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
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.math.MathUtils;

import java.awt.Point;
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
    protected void computeProductData(ProductData destData, Rectangle destRect) throws IOException {
        Band band = getBand();
        if (band.getProductReader() == null) {
            throw new IllegalStateException("no product reader for band '" + band.getDisplayName() + "'");
        }
        if (getLevel() == 0) {
            band.getProductReader().readBandRasterData(band, destRect.x, destRect.y,
                                                       destRect.width, destRect.height,
                                                       destData, ProgressMonitor.NULL);
        } else {
            readHigherLevelData(band, destData, destRect, (int) getLevelImageSupport().getScale());
        }
    }

    private void readHigherLevelData(Band band, ProductData destTileData, Rectangle destTileRect, int scale) throws IOException {
        int l0DestTileWidth = destTileRect.width * scale;
        int l0DestTileHeight = destTileRect.height * scale;
        int l0DestTileX = destTileRect.x * scale;
        int l0DestTileY = destTileRect.y * scale;
        Rectangle l0destTileRect = new Rectangle(l0DestTileX, l0DestTileY, l0DestTileWidth, l0DestTileHeight);

        ProductReader productReader = band.getProductReader();

        MultiLevelImage l0Image = band.getSourceImage();
        Point[] tileIndices = l0Image.getTileIndices(l0destTileRect);
        for (Point l0TileIndex : tileIndices) {
            Rectangle l0TileRect = l0Image.getTileRect(l0TileIndex.x, l0TileIndex.y);

            ProductData l0TileBuffer = ProductData.createInstance(destTileData.getType(), l0TileRect.width * l0TileRect.height);
            productReader.readBandRasterData(band, l0TileRect.x, l0TileRect.y, l0TileRect.width, l0TileRect.height, l0TileBuffer, ProgressMonitor.NULL);

            double invScale = 1.0 / scale;
            final Rectangle destLevelTileRect = new Rectangle(
                    MathUtils.floorInt(l0TileRect.x * invScale),
                    MathUtils.floorInt(l0TileRect.y * invScale),
                    MathUtils.ceilInt(l0TileRect.width * invScale),
                    MathUtils.ceilInt(l0TileRect.height * invScale)
            );
            for (int destTileY = 0; destTileY < destLevelTileRect.height; destTileY++) {
                if (destTileY <= destTileRect.height) {
                    int subTileYOffset = destLevelTileRect.y - destTileRect.y;
                    int subTileXOffset = destLevelTileRect.x - destTileRect.x;
                    int destDataYOffset = (destTileY + subTileYOffset) * destTileRect.width + subTileXOffset;
                    int l0TileBufferYOffset = destTileY * scale * l0TileRect.width;
                    for (int destTileX = 0; destTileX < destLevelTileRect.width; destTileX++) {
                        if (destTileX <= destTileRect.width) {
                            int l0tileBufferIndex = l0TileBufferYOffset + destTileX * scale;
                            double value = l0TileBuffer.getElemDoubleAt(l0tileBufferIndex);

                            int destDataIndex = destDataYOffset + destTileX;
                            destTileData.setElemDoubleAt(destDataIndex, value);
                        }
                    }
                }
            }
            l0TileBuffer.dispose();
        }
    }

}
