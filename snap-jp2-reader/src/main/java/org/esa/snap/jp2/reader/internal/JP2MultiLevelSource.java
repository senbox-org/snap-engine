/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2014-2015 CS-Romania (office@c-s.ro)
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

package org.esa.snap.jp2.reader.internal;

import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.esa.snap.jp2.reader.JP2ImageFile;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.image.TileImageDisposer;
import org.esa.snap.lib.openjpeg.jp2.TileLayout;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.BorderDescriptor;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * A single banded multi-level image source for JP2 files.
 *
 * @author Cosmin Cara
 * modified 20191108 to read a specific area from the input product by Denisa Stefanescu
 */
public class JP2MultiLevelSource extends AbstractMultiLevelSource {

    private final TileLayout tileLayout;
    private final int dataType;
    private final Logger logger;
    private final int bandIndex;
    private final TileImageDisposer tileManager;
    private final Rectangle subsetRegion;
    private final JP2ImageFile jp2ImageFile;
    private final Path localCacheFolder;
    private final int tileStartX;
    private final int tileStartY;
    private int numTilesX;
    private int numTilesY;

    /**
     * Constructs an instance of a single band multi-level image source
     *
     * @param bandIndex   The destination Product band for which the image source is created
     * @param imageWidth  The width of the scene image
     * @param imageHeight The height of the scene image
     * @param tileWidth   The width of a JP2 tile composing the scene image
     * @param tileHeight  The height of a JP2 tile composing the scene image
     * @param numTilesX   The number of JP2 tiles in a row
     * @param numTilesY   The number of JP2 tiles in a column
     * @param levels      The number of resolutions found in the JP2 file
     * @param dataType    The pixel data type
     * @param geoCoding   (optional) The geocoding found (if any) in the JP2 header
     */
    public JP2MultiLevelSource(Path localCacheFolder, JP2ImageFile jp2ImageFile, int bandIndex, int numBands,
                               int imageWidth, int imageHeight, int tileWidth, int tileHeight,
                               int numTilesX, int numTilesY, int levels, int dataType, GeoCoding geoCoding, Rectangle subsetRegion) {

        super(new DefaultMultiLevelModel(levels, Product.findImageToModelTransform(geoCoding), imageWidth, imageHeight));

        this.jp2ImageFile = jp2ImageFile;
        this.localCacheFolder = localCacheFolder;
        this.dataType = dataType;
        this.logger = Logger.getLogger(JP2MultiLevelSource.class.getName());
        this.tileLayout = new TileLayout(imageWidth, imageHeight, tileWidth, tileHeight, numTilesX, numTilesY, levels);
        this.tileLayout.numBands = numBands;
        this.bandIndex = bandIndex;
        this.tileManager = new TileImageDisposer();
        this.subsetRegion = subsetRegion;
        if (subsetRegion != null) {
            //image width and height are already the one from the subset region (were retrieved when the product vas instantiated)
            this.numTilesX = (imageWidth + subsetRegion.x) / tileWidth;
            if ((imageWidth + subsetRegion.x) % tileWidth != 0) {
                this.numTilesX++;
            }
            this.numTilesY = (imageHeight + subsetRegion.y) / tileHeight;
            if ((imageHeight + subsetRegion.y) % tileHeight != 0) {
                this.numTilesY++;
            }
            //we need to compute the tiles on row and column from where the selected subset starts
            this.tileStartY = subsetRegion.y / tileLayout.tileHeight;
            this.tileStartX = subsetRegion.x / tileLayout.tileWidth;
        } else {
            this.numTilesX = numTilesX;
            this.numTilesY = numTilesY;
            this.tileStartX = 0;
            this.tileStartY = 0;
        }

    }

    /**
     * Creates a planar image corresponding of a tile identified by row and column, at the specified resolution.
     *
     * @param row   The row of the tile (0-based)
     * @param col   The column of the tile (0-based)
     * @param level The resolution level (0 = highest)
     */
    private PlanarImage createTileImage(JP2ImageFile jp2ImageFile, Path localCacheFolder, int row, int col, int level, Point tileOffset) throws IOException {
        TileLayout currentLayout = tileLayout;
        // the edge tiles dimensions may be less than the dimensions from JP2 header
        if (subsetRegion != null) {
            //when a subset region is set by the user, the width and height for the middle tiles are the same as the tile width and height
            int tileSubsetWidth = tileLayout.tileWidth;
            int tileSubsetHeight = tileLayout.tileHeight;
            if (row == tileStartY && row == this.numTilesY - 1) {
                //when the subset have only one row tile, the height will be the same as the one selected by the user
                tileSubsetHeight = subsetRegion.height;
            } else if (row == tileStartY) {
                //when the subset have more row tiles and the current tile is the first row
                //if the default tile height is greater than the subset start point on row, the height is the difference between default tile height and subset start point on row
                //otherwise the height is the difference between row number (+1 because the count starts from 0) multiplied by tile default height and subset start point on row
                //the height will be the minimum value between the above conditions and the height selected by the user
                tileSubsetHeight = Math.min(tileSubsetHeight > subsetRegion.y ? tileSubsetHeight - subsetRegion.y : (row + 1) * tileSubsetHeight - subsetRegion.y, subsetRegion.height);
            } else if (row == this.numTilesY - 1) {
                //when the subset have more row tiles and the current tile is the last row
                //the height will be the the difference between:
                // the sum of the height selected by the user and the subset start point on row and the multiplication between row number
                // and the default tile height
                tileSubsetHeight = subsetRegion.height + subsetRegion.y - row * tileLayout.tileHeight;
            }
            if (col == tileStartX && col == this.numTilesX - 1) {
                //when the subset have only one column tile, the width will be the same as the one selected by the user
                tileSubsetWidth = subsetRegion.width;
            } else if (col == tileStartX) {
                //when the subset have more column tiles and the current tile is the first column
                //if the default tile width is greater than the subset start point on column, the width is the difference between default tile width and subset start point on column
                //otherwise the width is the difference between column number (+1 because the count starts from 0) multiplied by tile default width and subset start point on column
                //the width will be the minimum value between the above conditions and the width selected by the user
                tileSubsetWidth = Math.min(tileSubsetWidth > subsetRegion.x ? tileSubsetWidth - subsetRegion.x : (col + 1) * tileSubsetWidth - subsetRegion.x, subsetRegion.width);
            } else if (col == this.numTilesX - 1) {
                //when the subset have more column tiles and the current tile is the last column
                //the width will be the the difference between:
                // the sum of the width selected by the user and the subset start point on column and the multiplication between column number
                // and the default tile width
                tileSubsetWidth = subsetRegion.width + subsetRegion.x - col * tileLayout.tileWidth;
            }
            currentLayout = new TileLayout(tileLayout.width, tileLayout.height, tileSubsetWidth, tileSubsetHeight,
                                           tileLayout.numXTiles, tileLayout.numYTiles, tileLayout.numResolutions);
            currentLayout.numBands = tileLayout.numBands;
        } else {
            if (row == tileLayout.numYTiles - 1 || col == tileLayout.numXTiles - 1) {
                currentLayout = new TileLayout(tileLayout.width, tileLayout.height,
                                               Math.min(tileLayout.width - col * tileLayout.tileWidth, tileLayout.tileWidth),
                                               Math.min(tileLayout.height - row * tileLayout.tileHeight, tileLayout.tileHeight),
                                               tileLayout.numXTiles, tileLayout.numYTiles, tileLayout.numResolutions);
                currentLayout.numBands = tileLayout.numBands;
            }
        }
        return JP2TileOpImage.create(jp2ImageFile, localCacheFolder, bandIndex, row, col, currentLayout, getModel(), dataType, level, tileOffset);
    }

    @Override
    protected RenderedImage createImage(int level) {
        List<RenderedImage> tileImages = Collections.synchronizedList(new ArrayList<>(tileLayout.numXTiles * tileLayout.numYTiles));
        TileLayout layout = tileLayout;
        double factorX = 1.0 / Math.pow(2, level);
        double factorY = 1.0 / Math.pow(2, level);
        int offsetX = layout.tileWidth;
        int offsetY = layout.tileHeight;
        int distX = 0;
        int distY = 0;
        //when a subset region is set by the user, we need to compute an offset from the initial product
        if (subsetRegion != null) {
            offsetX = tileStartX > 0 ? (tileStartX + 1) * tileLayout.tileWidth - subsetRegion.x - 1 : tileLayout.tileWidth - subsetRegion.x;
            offsetY = tileStartY > 0 ? (tileStartY + 1) * tileLayout.tileHeight - subsetRegion.y : tileLayout.tileHeight - subsetRegion.y;
            distX = tileStartX > 0 ? tileLayout.tileWidth - offsetX : subsetRegion.x;
            distY = tileStartY > 0 ? tileLayout.tileHeight - offsetY : subsetRegion.y;
        }
        Point tileOffset = new Point(0, 0);
        //variables needed to compute the tile offset and xTrans/yTrans variables (used when a sunset is set by the user)
        int tileColCount = 0;
        int tileRowCount = 0;
        for (int x = tileStartY; x < this.numTilesY; x++) {
            for (int y = tileStartX; y < this.numTilesX; y++) {
                int xTrans = y * tileLayout.tileWidth;
                int yTrans = x * tileLayout.tileHeight;
                //when a subset region is set by the user, the tile offset and xTrans/yTrans variables need to be computed
                if (subsetRegion != null) {
                    if (tileColCount == 0) { //first column of the selected subset
                        yTrans = tileColCount * offsetY;
                    } else {
                        yTrans = tileColCount * offsetY + (tileColCount - 1) * distY;
                    }
                    if (tileRowCount == 0) { //first row of the selected subset
                        xTrans = tileRowCount * offsetX;
                    } else {
                        xTrans = tileRowCount * offsetX + (tileRowCount - 1) * distX;
                    }
                    if (x == 0) { //the first tile of the subset, also the first tile of the initial the product (columns)
                        tileOffset.y = subsetRegion.y;
                    } else if (x == tileStartY) { //the first tile of the subset, not the first tile of the initial product (columns)
                        tileOffset.y = subsetRegion.y - tileStartY * tileLayout.tileHeight;
                    } else {
                        tileOffset.y = 0;
                    }
                    if (y == 0) { //the first tile of the subset, also the first tile of the initial the product (rows)
                        tileOffset.x = subsetRegion.x;
                    } else if (tileStartX == y) { //the first tile of the subset, not the first tile of the initial product (rows)
                        tileOffset.x = subsetRegion.x - tileStartX * tileLayout.tileWidth;
                    } else {
                        tileOffset.x = 0;
                    }
                }
                PlanarImage opImage;
                try {
                    opImage = createTileImage(jp2ImageFile, this.localCacheFolder, x, y, level, tileOffset);
                    if (opImage != null) {
                        tileManager.registerForDisposal(opImage);
                        opImage = TranslateDescriptor.create(opImage,
                                                             (float) (xTrans * factorX),
                                                             (float) (yTrans * factorY),
                                                             Interpolation.getInstance(Interpolation.INTERP_NEAREST),
                                                             null);
                    }
                } catch (IOException ex) {
                    opImage = ConstantDescriptor.create((float) layout.tileWidth, (float) layout.tileHeight, new Number[]{0}, null);
                }
                tileImages.add(opImage);
                tileRowCount++; //increment the row
            }
            tileColCount++; //increment the column and reset the row
            tileRowCount = 0;
        }
        if (tileImages.isEmpty()) {
            logger.warning("No tile images for mosaic");
            return null;
        }

        ImageLayout imageLayout = new ImageLayout();
        imageLayout.setMinX(0);
        imageLayout.setMinY(0);
        imageLayout.setTileWidth(JAI.getDefaultTileSize().width);
        imageLayout.setTileHeight(JAI.getDefaultTileSize().height);
        imageLayout.setTileGridXOffset(0);
        imageLayout.setTileGridYOffset(0);

        // It must be specified which values shall be mosaicked. The default settings don't work
        // We want all values to be considered
        ROI[] sourceRois = new ROI[tileImages.size()];
        for (int i = 0; i < sourceRois.length; i++) {
            RenderedImage image = tileImages.get(i);
            ImageLayout roiLayout = new ImageLayout(image);
            ROI roi = new ROI(ConstantDescriptor.create((float) image.getWidth(), (float) image.getHeight(), new Byte[]{Byte.MAX_VALUE}, new RenderingHints(JAI.KEY_IMAGE_LAYOUT, roiLayout)), Byte.MAX_VALUE);
            sourceRois[i] = roi;
        }

        RenderedOp mosaicOp = MosaicDescriptor.create(tileImages.toArray(new RenderedImage[tileImages.size()]),
                                                      MosaicDescriptor.MOSAIC_TYPE_OVERLAY,
                                                      null, sourceRois, null, null,
                                                      new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout));

        int fittingRectWidth = JP2TileOpImage.scaleValue(tileLayout.width, level);
        int fittingRectHeight = JP2TileOpImage.scaleValue(tileLayout.height, level);

        Rectangle fitRect = new Rectangle(0, 0, fittingRectWidth, fittingRectHeight);
        final Rectangle destBounds = DefaultMultiLevelSource.getLevelImageBounds(fitRect, Math.pow(2.0, level));

        BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);

        if (mosaicOp.getWidth() < destBounds.width || mosaicOp.getHeight() < destBounds.height) {
            int rightPad = destBounds.width - mosaicOp.getWidth();
            int bottomPad = destBounds.height - mosaicOp.getHeight();

            mosaicOp = BorderDescriptor.create(mosaicOp, 0, rightPad, 0, bottomPad, borderExtender, null);
        }
        //sometimes the when the values are scaled a pixel is loosed or added
        if (mosaicOp.getWidth() != fitRect.width || mosaicOp.getHeight() != fitRect.height) {
            int rightPad = fitRect.width - mosaicOp.getWidth();
            int bottomPad = fitRect.height - mosaicOp.getHeight();
            mosaicOp = BorderDescriptor.create(mosaicOp, 0, rightPad, 0, bottomPad, borderExtender, null);
        }

        return mosaicOp;
    }

    @Override
    public synchronized void reset() {
        super.reset();
        tileManager.disposeAll();
        System.gc();
    }
}
