package org.esa.snap.dataio.geotiff;

import com.bc.ceres.glevel.MultiLevelModel;
import org.esa.snap.core.image.AbstractSubsetTileOpImage;
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
public class GeoTiffTileOpImage extends AbstractSubsetTileOpImage {

    private final GeoTiffImageReader geoTiffImageReader;
    private final boolean isGlobalShifted180;

    public GeoTiffTileOpImage(GeoTiffImageReader geoTiffImageReader, MultiLevelModel imageMultiLevelModel, int dataBufferType, int bandIndex,
                              Rectangle imageBounds, Dimension tileSize, Point tileOffset, int level, boolean isGlobalShifted180) {

        super(imageMultiLevelModel, dataBufferType, bandIndex, imageBounds, tileSize, tileOffset, level);

        this.geoTiffImageReader = geoTiffImageReader;
        this.isGlobalShifted180 = isGlobalShifted180;
    }

    @Override
    protected void computeRect(PlanarImage[] sources, WritableRaster destinationRaster, Rectangle destinationRectangle) {
        try {
            Rectangle tileBoundsIntersection = computeIntersection(destinationRectangle);
            if (!tileBoundsIntersection.isEmpty()) {
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
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read the data for level " + getLevel()+" and rectangle " + destinationRectangle + ".", ex);
        }
    }

    private int computeSourceX(double x) {
        return this.levelImageSupport.getSourceCoord(x, 0, this.levelImageSupport.getSourceWidth()-1);
    }

    private int computeSourceY(double y) {
        return this.levelImageSupport.getSourceCoord(y, 0, this.levelImageSupport.getSourceHeight()-1);
    }

    private Raster readRasterData(int destOffsetX, int destOffsetY, int destWidth, int destHeight) throws IOException {
        int sourceStepX = 1;
        int sourceStepY = 1;
        int sourceOffsetX = sourceStepX * destOffsetX;
        int sourceOffsetY = sourceStepY * destOffsetY;
        synchronized (this.geoTiffImageReader) {
            return this.geoTiffImageReader.readRect(this.isGlobalShifted180, sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY, destOffsetX, destOffsetY, destWidth, destHeight);
        }
    }

    private static void validateCoordinate(int coordinateToCheck, int minimumCoordinate, int size) {
        if ((coordinateToCheck < minimumCoordinate) || (coordinateToCheck > (minimumCoordinate + size))) {
            throw new IllegalStateException("The coordinate " + coordinateToCheck + " is out of bounds. The minimum coordinate is " + minimumCoordinate+ " and the size is " + size + ".");
        }
    }
}
