package org.esa.snap.dataio.geotiff;

import org.esa.snap.core.image.AbstractSubsetTileOpImage;
import org.esa.snap.core.image.ImageReadBoundsSupport;
import org.esa.snap.core.util.ImageUtils;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * Created by jcoravu on 22/11/2019.
 */
public class GeoTiffTileOpImage extends AbstractSubsetTileOpImage {

    private final GeoTiffRasterRegion geoTiffRasterRegion;
    private final GeoTiffBandSource geoTiffBandSource;

    public GeoTiffTileOpImage(GeoTiffRasterRegion geoTiffRasterRegion, GeoTiffBandSource geoTiffBandSource, int dataBufferType, int tileWidth, int tileHeight,
                              int tileOffsetFromReadBoundsX, int tileOffsetFromReadBoundsY, ImageReadBoundsSupport levelImageBoundsSupport) {

        super(dataBufferType, tileWidth, tileHeight, tileOffsetFromReadBoundsX, tileOffsetFromReadBoundsY, levelImageBoundsSupport, geoTiffBandSource.getDefaultJAIReadTileSize());

        this.geoTiffRasterRegion = geoTiffRasterRegion;
        this.geoTiffBandSource = geoTiffBandSource;
    }

    @Override
    protected void computeRect(PlanarImage[] sources, WritableRaster levelDestinationRaster, Rectangle levelDestinationRectangle) {
        Rectangle normalBoundsIntersection = computeIntersectionOnNormalBounds(levelDestinationRectangle);
        if (!normalBoundsIntersection.isEmpty()) {
            if (getLevel() == 0) { // the first image level
                Raster normalRasterData = readRasterData(normalBoundsIntersection.x, normalBoundsIntersection.y, normalBoundsIntersection.width, normalBoundsIntersection.height);
                writeDataOnLevelRaster(normalRasterData, normalBoundsIntersection, levelDestinationRaster, levelDestinationRectangle, this.geoTiffBandSource.getBandIndex());
            } else if (this.geoTiffBandSource.canDivideTileRegionToRead(getLevel())) {
                readHigherLevelData(normalBoundsIntersection, levelDestinationRaster, levelDestinationRectangle);
            } else {
                Raster normalRasterData = readRasterData(normalBoundsIntersection.x, normalBoundsIntersection.y, normalBoundsIntersection.width, normalBoundsIntersection.height);
                writeDataOnLevelRaster(normalRasterData, normalBoundsIntersection, levelDestinationRaster, levelDestinationRectangle, this.geoTiffBandSource.getBandIndex());
            }
        }
    }

    private void readHigherLevelData(Rectangle normalBoundsIntersection, WritableRaster levelDestinationRaster, Rectangle levelDestinationRectangle) {
        int multiplyFactor = 2; // read twice the default JAI tile size
        Dimension defaultJAIReadTileSize = this.geoTiffBandSource.getDefaultJAIReadTileSize();
        int defaultTileWidthToRead = computeTileSizeToRead(getTileWidth(), defaultJAIReadTileSize.width * multiplyFactor, normalBoundsIntersection.width);
        int defaultTileHeightToRead = computeTileSizeToRead(getTileHeight(), defaultJAIReadTileSize.height * multiplyFactor, normalBoundsIntersection.height);

        int columnTileCount = ImageUtils.computeTileCount(normalBoundsIntersection.width, defaultTileWidthToRead);
        int rowTileCount = ImageUtils.computeTileCount(normalBoundsIntersection.height, defaultTileHeightToRead);

        int bandIndex = this.geoTiffBandSource.getBandIndex();
        int levelOffsetY = this.levelTileOffsetFromReadBoundsY + levelDestinationRectangle.y;
        int levelOffsetX = this.levelTileOffsetFromReadBoundsX + levelDestinationRectangle.x;

        int levelColumnReadOffsetX = 0;

        // iterate horizontally the tiles to read
        for (int columnIndex = 0; columnIndex < columnTileCount; columnIndex++) {
            int tileLeftXToRead = (columnIndex * defaultTileWidthToRead);
            int tileWidthToRead = (columnIndex < columnTileCount - 1) ? defaultTileWidthToRead : (normalBoundsIntersection.width - tileLeftXToRead);
            tileLeftXToRead += normalBoundsIntersection.x;
            int tileRightXToRead = tileLeftXToRead + tileWidthToRead - 1;

            int levelColumnReadOffsetY = 0;

            // iterate vertically the tiles to read
            for (int rowIndex = 0; rowIndex < rowTileCount; rowIndex++) {
                int tileTopYToRead = rowIndex * defaultTileHeightToRead;
                int tileHeightToRead = (rowIndex < rowTileCount - 1) ? defaultTileHeightToRead : (normalBoundsIntersection.height - tileTopYToRead);
                tileTopYToRead += normalBoundsIntersection.y;
                int tileBottomYToRead = tileTopYToRead + tileHeightToRead - 1;

                Raster normalRasterData = readRasterData(tileLeftXToRead, tileTopYToRead, tileWidthToRead, tileHeightToRead);

                int x = levelColumnReadOffsetX;
                int y = levelColumnReadOffsetY;
                int currentSrcXOffset;
                while ((currentSrcXOffset = this.imageBoundsSupport.getSourceX() + computeSourceX(levelOffsetX + x)) <= tileRightXToRead) {
                    if (currentSrcXOffset >= tileLeftXToRead) {
                        y = levelColumnReadOffsetY; // initialize again the 'y' variable
                        int currentSrcYOffset;
                        while ((currentSrcYOffset = this.imageBoundsSupport.getSourceY() + computeSourceY(levelOffsetY + y)) <= tileBottomYToRead) {
                            if (currentSrcYOffset >= tileTopYToRead) {
                                double value = normalRasterData.getSampleDouble(currentSrcXOffset, currentSrcYOffset, bandIndex);
                                levelDestinationRaster.setSample(levelDestinationRectangle.x + x, levelDestinationRectangle.y + y, bandIndex, value);
                            } else {
                                throw new IllegalStateException("Invalid values when iterate on the Y axis: levelColumnReadOffsetY=" + levelColumnReadOffsetY + ", y=" + y + ", currentSrcYOffset=" + currentSrcYOffset + ", tileTopYToRead=" + tileTopYToRead + ".");
                            }
                            y++;
                            if (y >= levelDestinationRectangle.height) {
                                break;
                            }
                        }
                    } else {
                        throw new IllegalStateException("Invalid values when iterate on the X axis: levelColumnReadOffsetX=" + levelColumnReadOffsetX + ", x=" + x + ", currentSrcXOffset=" + currentSrcXOffset + ".");
                    }
                    x++;
                    if (x >= levelDestinationRectangle.width) {
                        break;
                    }
                }
                levelColumnReadOffsetY = y;
                if (rowIndex == rowTileCount - 1) {
                    levelColumnReadOffsetX = x; // the last cell in the vertical column
                }
            }
        }
    }

    private Raster readRasterData(int destOffsetX, int destOffsetY, int destWidth, int destHeight) {
        try {
            int sourceStepX = 1;
            int sourceStepY = 1;
            int sourceOffsetX = sourceStepX * destOffsetX;
            int sourceOffsetY = sourceStepY * destOffsetY;
            synchronized (this.geoTiffRasterRegion) {
                return this.geoTiffRasterRegion.readRect(this.geoTiffBandSource.isGlobalShifted180(), sourceOffsetX, sourceOffsetY,
                                                        sourceStepX, sourceStepY, destOffsetX, destOffsetY, destWidth, destHeight);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read the data: level=" + getLevel() + ", region=[x="+ destOffsetX + ", y="+destOffsetY+ ", width="+destWidth+", height="+destHeight+"].", ex);
        }
    }

    private static int computeTileSizeToRead(int actualTileSize, int defaultJAIReadTileSize, int normalBoundsSize) {
        int defaultTileSizeToRead = Math.max(actualTileSize, defaultJAIReadTileSize);
        if (defaultTileSizeToRead > normalBoundsSize) {
            defaultTileSizeToRead = normalBoundsSize;
        }
        return defaultTileSizeToRead;
    }
}
