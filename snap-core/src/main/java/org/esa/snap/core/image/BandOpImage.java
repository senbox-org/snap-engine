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
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.runtime.Config;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * An {@code OpImage} which retrieves its data from the product reader associated with the
 * given {@code RasterDataNode} at a given pyramid level.
 */
public class BandOpImage extends RasterDataNodeOpImage {

    public static boolean prefetchTiles;

    public BandOpImage(Band band) {
        this(band, ResolutionLevel.MAXRES);
    }

    public BandOpImage(Band band, ResolutionLevel level) {
        super(band, level);
        if (Boolean.getBoolean("snap.imageManager.disableSourceTileCaching")) {
            setTileCache(null);
        }
        prefetchTiles = Config.instance("snap").preferences().getBoolean("snap.jai.prefetchTiles", true);
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
            readHigherLevelData(band, destData, destRect, getLevelImageSupport());
        }
    }

    private static void readHigherLevelData(Band band, ProductData destData, Rectangle destRect, LevelImageSupport lvlSupport) throws IOException {

        if (band.isProductReaderDirectlyUsable() && band.getProductReader() instanceof AbstractProductReader) {
            AbstractProductReader reader = (AbstractProductReader) band.getProductReader();
            if (reader.isSubsetReadingFullySupported()) {
                ProductIO.readLevelBandRasterData(reader, band, lvlSupport, destRect, destData);
                return;
            }
        }
        final int sourceWidth = lvlSupport.getSourceWidth(destRect.width);
        final int sourceHeight = lvlSupport.getSourceHeight(destRect.height);
        final int srcX = lvlSupport.getSourceX(destRect.x);
        final int srcY = lvlSupport.getSourceY(destRect.y);

        final MultiLevelImage img = band.getSourceImage();
        final int tileWidth = img.getTileWidth();
        final int tileHeight = img.getTileHeight();

        Map<Integer, List<PositionCouple>> xSrcTiled = computeTiledL0AxisIdx(destRect.x, destRect.width, tileWidth, lvlSupport::getSourceX);
        Map<Integer, List<PositionCouple>> ySrcTiled = computeTiledL0AxisIdx(destRect.y, destRect.height, tileHeight, lvlSupport::getSourceY);

        Point[] tileIndices = img.getTileIndices(new Rectangle(srcX, srcY, sourceWidth, sourceHeight));
        if (prefetchTiles) {
            img.prefetchTiles(tileIndices);
        }
        for (Point tileIndex : tileIndices) {
            final int xTileIdx = tileIndex.x;
            final int yTileIdx = tileIndex.y;
            final List<PositionCouple> yPositions = ySrcTiled.get(yTileIdx);
            final List<PositionCouple> xPositions = xSrcTiled.get(xTileIdx);
            if (yPositions == null || xPositions == null) {
                continue;
            }
            Rectangle tileRect = img.getTileRect(xTileIdx, yTileIdx);
            if (tileRect.isEmpty()) {
                continue;
            }
            final ProductData tileData = ProductData.createInstance(band.getDataType(), tileRect.width * tileRect.height);
            band.readRasterData(tileRect.x, tileRect.y, tileRect.width, tileRect.height, tileData, ProgressMonitor.NULL);

            for (PositionCouple yPos : yPositions) {
                final int ySrc = yPos.srcPos;
                final int yPosInTile = ySrc % tileHeight;
                final int yOffsetInTile = yPosInTile * tileRect.width;
                final int yDest = yPos.destPos;
                final int yDestOffset = (yDest - destRect.y) * destRect.width;
                for (PositionCouple xPos : xPositions) {
                    final int xSrc = xPos.srcPos;
                    final int xPosInTile = xSrc % tileWidth;
                    final int xDest = xPos.destPos;
                    final double v = tileData.getElemDoubleAt(yOffsetInTile + xPosInTile);
                    destData.setElemDoubleAt(yDestOffset + xDest - destRect.x, v);
                }
            }
        }
    }

    static Map<Integer, List<PositionCouple>> computeTiledL0AxisIdx(int destStart, int destAxislength, int tileAxisLengthL0, SourceConverter lvlSupport) {
        final Map<Integer, List<PositionCouple>> map = new HashMap<>();
        for (int i = destStart; i < destStart + destAxislength; i++) {
            int srcIdx = lvlSupport.getSource(i);
            final int tileIdx = srcIdx / tileAxisLengthL0;
            final List<PositionCouple> xSrc;
            if (map.containsKey(tileIdx)) {
                xSrc = map.get(tileIdx);
            } else {
                xSrc = new ArrayList<>();
                map.put(tileIdx, xSrc);
            }
            xSrc.add(new PositionCouple(srcIdx, i));
        }
        return map;
    }

    interface SourceConverter {
        int getSource(int idx);
    }

    static class PositionCouple {
        public final int srcPos;
        public final int destPos;

        PositionCouple(int srcPos, int destPos) {
            this.srcPos = srcPos;
            this.destPos = destPos;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PositionCouple that = (PositionCouple) o;
            return srcPos == that.srcPos &&
                   destPos == that.destPos;
        }

        @Override
        public int hashCode() {
            return Objects.hash(srcPos, destPos);
        }

        @Override
        public String toString() {
            return "PositionCouple{" +
                   "srcPos=" + srcPos +
                   ", destPos=" + destPos +
                   '}';
        }
    }
}
