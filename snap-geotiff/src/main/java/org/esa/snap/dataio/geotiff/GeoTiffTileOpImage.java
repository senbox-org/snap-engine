package org.esa.snap.dataio.geotiff;

import com.bc.ceres.glevel.MultiLevelModel;
import org.esa.snap.core.image.LevelImageSupport;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.SingleBandedOpImage;
import org.esa.snap.core.util.ImageUtils;

import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;

/**
 * Created by jcoravu on 22/11/2019.
 */
public class GeoTiffTileOpImage extends SingleBandedOpImage {

    private final GeoTiffImageReader geoTiffImageReader;
    private final LevelImageSupport levelImageSupport;
    private final Point imageOffset;
    private final Point levelTileOffset;
    private final int bandIndex;
    private final boolean isGlobalShifted180;

    public GeoTiffTileOpImage(GeoTiffImageReader geoTiffImageReader, MultiLevelModel imageMultiLevelModel, int dataBufferType, int bandIndex,
                              Rectangle imageBounds, Dimension tileSize, Point tileOffset, int level, boolean isGlobalShifted180) {

        this(geoTiffImageReader, dataBufferType, bandIndex, imageBounds, tileSize, ImageUtils.computeTileDimensionAtResolutionLevel(tileSize, level),
                tileOffset, ResolutionLevel.create(imageMultiLevelModel, level), isGlobalShifted180);
    }

    private GeoTiffTileOpImage(GeoTiffImageReader geoTiffImageReader, int dataBufferType, int bandIndex, Rectangle imageBounds, Dimension tileSize,
                               Dimension subTileSize, Point tileOffset, ResolutionLevel resolutionLevel, boolean isGlobalShifted180) {

        super(dataBufferType, null, tileSize.width, tileSize.height, subTileSize, null, resolutionLevel);

        this.geoTiffImageReader = geoTiffImageReader;
        this.bandIndex = bandIndex;
        this.isGlobalShifted180 = isGlobalShifted180;
        this.levelTileOffset = new Point(ImageUtils.computeLevelSize(tileOffset.x, resolutionLevel.getIndex()), ImageUtils.computeLevelSize(tileOffset.y, resolutionLevel.getIndex()));
        this.imageOffset = new Point(imageBounds.x, imageBounds.y);
        this.levelImageSupport = new LevelImageSupport(imageBounds.width, imageBounds.height, resolutionLevel);
    }

    @Override
    protected void computeRect(PlanarImage[] sources, WritableRaster destinationRaster, Rectangle destinationRectangle) {
        try {
            Rectangle tileBoundsIntersection = computeIntersection(destinationRectangle);
            if (!tileBoundsIntersection.isEmpty()) {
                if (this.isGlobalShifted180) {
                    readGlobalShifted180Rectangle(destinationRaster, destinationRectangle, tileBoundsIntersection);
                } else {
                    readNormalRectangle(destinationRaster, destinationRectangle, tileBoundsIntersection);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read the data for level " + getLevel()+" and rectangle " + destinationRectangle + ".", ex);
        }
    }

    private void readNormalRectangle(WritableRaster destinationRaster, Rectangle destinationRectangle, Rectangle tileBoundsIntersection) throws IOException {
        Raster rasterData = readRasterData(tileBoundsIntersection.x, tileBoundsIntersection.y, tileBoundsIntersection.width, tileBoundsIntersection.height);

        int offsetY = this.levelTileOffset.y + destinationRectangle.y;
        int offsetX = this.levelTileOffset.x + destinationRectangle.x;
        for (int y = 0; y < destinationRectangle.height; y++) {
            int currentSrcYOffset = this.imageOffset.y + computeSourceY(offsetY + y);
            validateCoordinate(currentSrcYOffset, tileBoundsIntersection.y, tileBoundsIntersection.height);
            for (int x = 0; x < destinationRectangle.width; x++) {
                int currentSrcXOffset = this.imageOffset.x + computeSourceX(offsetX + x);
                validateCoordinate(currentSrcXOffset, tileBoundsIntersection.x, tileBoundsIntersection.width);
                double value = rasterData.getSampleDouble(currentSrcXOffset, currentSrcYOffset, this.bandIndex);
                destinationRaster.setSample(destinationRectangle.x + x, destinationRectangle.y + y, this.bandIndex, value);
            }
        }
    }

    private void readGlobalShifted180Rectangle(WritableRaster destinationRaster, Rectangle destinationRectangle, Rectangle tileBoundsIntersection) throws IOException {
        int leftRasterWidth = tileBoundsIntersection.width / 2;
        int rightRasterWidth = tileBoundsIntersection.width - leftRasterWidth;
        int rightRasterX = tileBoundsIntersection.x + tileBoundsIntersection.width - rightRasterWidth;
        Raster leftRasterData = readRasterData(tileBoundsIntersection.x, tileBoundsIntersection.y, leftRasterWidth, tileBoundsIntersection.height);
        Raster rightRasterData = readRasterData(rightRasterX, tileBoundsIntersection.y, rightRasterWidth, tileBoundsIntersection.height);

        int offsetY = this.levelTileOffset.y + destinationRectangle.y;
        int offsetX = this.levelTileOffset.x + destinationRectangle.x;
        double halfDestinationWidth = destinationRectangle.width / 2.0d;
        for (int y = 0; y < destinationRectangle.height; y++) {
            int currentSrcYOffset = this.imageOffset.y + computeSourceY(offsetY + y);
            validateCoordinate(currentSrcYOffset, tileBoundsIntersection.y, tileBoundsIntersection.height);
            for (int x = 0; x < destinationRectangle.width; x++) {
                double value;
                if (x < ((int) halfDestinationWidth)) {
                    int currentSrcXOffset = this.imageOffset.x + computeSourceX(offsetX + x + halfDestinationWidth);
                    validateCoordinate(currentSrcXOffset, rightRasterX, rightRasterWidth);
                    value = rightRasterData.getSampleDouble(currentSrcXOffset, currentSrcYOffset, this.bandIndex);
                } else {
                    int currentSrcXOffset = this.imageOffset.x + computeSourceX(offsetX + x - halfDestinationWidth);
                    validateCoordinate(currentSrcXOffset, tileBoundsIntersection.x, leftRasterWidth);
                    value = leftRasterData.getSampleDouble(currentSrcXOffset, currentSrcYOffset, this.bandIndex);
                }
                destinationRaster.setSample(destinationRectangle.x + x, destinationRectangle.y + y, this.bandIndex, value);
            }
        }
    }

    private Rectangle computeIntersection(Rectangle destinationRectangle) {
        int sourceImageWidth = getImageWidth();
        int sourceImageHeight = getImageHeight();
        Rectangle sourceImageBounds = new Rectangle(this.imageOffset.x, this.imageOffset.y, sourceImageWidth, sourceImageHeight);

        int destinationSourceWidth = this.levelImageSupport.getSourceWidth(destinationRectangle.width);
        int destinationSourceHeight = this.levelImageSupport.getSourceHeight(destinationRectangle.height);
        int destinationSourceX = this.levelImageSupport.getSourceX(this.levelTileOffset.x + destinationRectangle.x);
        int destinationSourceY = this.levelImageSupport.getSourceY(this.levelTileOffset.y + destinationRectangle.y);
        if (destinationSourceX + destinationSourceWidth > sourceImageWidth) {
            destinationSourceWidth = sourceImageWidth - destinationSourceX;
        }
        if (destinationSourceY + destinationSourceHeight > sourceImageHeight) {
            destinationSourceHeight = sourceImageHeight - destinationSourceY;
        }
        destinationSourceX += this.imageOffset.x;
        destinationSourceY += this.imageOffset.y;

        Rectangle tileBoundsInSourceImage = new Rectangle(destinationSourceX, destinationSourceY, destinationSourceWidth, destinationSourceHeight);
        return sourceImageBounds.intersection(tileBoundsInSourceImage);
    }

    private int computeSourceX(double x) {
        return this.levelImageSupport.getSourceCoord(x, 0, this.levelImageSupport.getSourceWidth()-1);
    }

    private int computeSourceY(double y) {
        return this.levelImageSupport.getSourceCoord(y, 0, this.levelImageSupport.getSourceHeight()-1);
    }

    private int getImageWidth() {
        return this.levelImageSupport.getSourceWidth();
    }

    private int getImageHeight() {
        return this.levelImageSupport.getSourceHeight();
    }

    private Raster readRasterData(int destOffsetX, int destOffsetY, int destWidth, int destHeight) throws IOException {
        int sourceStepX = 1;
        int sourceStepY = 1;
        int sourceOffsetX = sourceStepX * destOffsetX;
        int sourceOffsetY = sourceStepY * destOffsetY;
        synchronized (this.geoTiffImageReader) {
            return this.geoTiffImageReader.readRect(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY, destOffsetX, destOffsetY, destWidth, destHeight);
        }
    }

    private static void validateCoordinate(int coordinateToCheck, int minimumCoordinate, int size) {
        if ((coordinateToCheck < minimumCoordinate) || (coordinateToCheck > (minimumCoordinate + size))) {
            throw new IllegalStateException("The coordinate " + coordinateToCheck + " is out of bounds. The minimum coordinate is " + minimumCoordinate+ " and the size is " + size + ".");
        }
    }
}
