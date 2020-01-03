package org.esa.snap.dataio.geotiff;

import com.bc.ceres.glevel.MultiLevelModel;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.image.AbstractSubsetTileOpImage;

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
    private final int bandIndex;

    public GeoTiffTileOpImage(GeoTiffImageReader geoTiffImageReader, MultiLevelModel imageMultiLevelModel, int dataBufferType, int bandIndex,
                              Rectangle imageReadBounds, Dimension tileSize, Point tileOffset, int level, boolean isGlobalShifted180) {

        super(imageMultiLevelModel, dataBufferType, imageReadBounds, tileSize, tileOffset, level);

        this.geoTiffImageReader = geoTiffImageReader;
        this.isGlobalShifted180 = isGlobalShifted180;
        this.bandIndex = bandIndex;
    }

    @Override
    protected void computeRect(PlanarImage[] sources, WritableRaster levelDestinationRaster, Rectangle levelDestinationRectangle) {
        Rectangle normalBoundsIntersection = computeIntersectionOnNormalBounds(levelDestinationRectangle);
        if (!normalBoundsIntersection.isEmpty()) {
            Raster normalRasterData;
            try {
                normalRasterData = readRasterData(normalBoundsIntersection.x, normalBoundsIntersection.y, normalBoundsIntersection.width, normalBoundsIntersection.height);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to read the data for level " + getLevel() + " and rectangle " + levelDestinationRectangle + ".", ex);
            }
            writeDataOnLevelRaster(normalRasterData, normalBoundsIntersection, levelDestinationRaster, levelDestinationRectangle, this.bandIndex);
        }
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
}
