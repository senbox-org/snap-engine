package org.esa.snap.dataio.geotiff;

import org.esa.snap.core.image.AbstractSubsetTileOpImage;
import org.esa.snap.core.image.ImageReadBoundsSupport;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.lang.ref.WeakReference;

/**
 * Created by jcoravu on 22/11/2019.
 */
public class GeoTiffTileOpImage extends AbstractSubsetTileOpImage {

    private final GeoTiffRasterRegion geoTiffImageReader;
    private final GeoTiffBandSource geoTiffBandSource;
    private final boolean synchronizeReadRegion;

    public GeoTiffTileOpImage(GeoTiffRasterRegion geoTiffImageReader, GeoTiffBandSource geoTiffBandSource, int dataBufferType, int tileWidth, int tileHeight,
                              int tileOffsetFromReadBoundsX, int tileOffsetFromReadBoundsY, ImageReadBoundsSupport levelImageBoundsSupport,
                              Dimension defaultJAIReadTileSize, boolean synchronizeReadRegion) {

        super(dataBufferType, tileWidth, tileHeight, tileOffsetFromReadBoundsX, tileOffsetFromReadBoundsY, levelImageBoundsSupport, defaultJAIReadTileSize);

        this.geoTiffImageReader = geoTiffImageReader;
        this.geoTiffBandSource = geoTiffBandSource;
        this.synchronizeReadRegion = synchronizeReadRegion;
    }

    @Override
    protected void computeRect(PlanarImage[] sources, WritableRaster levelDestinationRaster, Rectangle levelDestinationRectangle) {
        Rectangle normalBoundsIntersection = computeIntersectionOnNormalBounds(levelDestinationRectangle);
        if (!normalBoundsIntersection.isEmpty()) {
            Raster normalRasterData;
            try {
                normalRasterData = readRasterData(normalBoundsIntersection.x, normalBoundsIntersection.y, normalBoundsIntersection.width, normalBoundsIntersection.height);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to read the data for level " + getLevel() + " and rectangle " + levelDestinationRectangle + ".", ex);
            }
            writeDataOnLevelRaster(normalRasterData, normalBoundsIntersection, levelDestinationRaster, levelDestinationRectangle, this.geoTiffBandSource.getBandIndex());
        }
    }

    private Raster readRasterData(int destOffsetX, int destOffsetY, int destWidth, int destHeight) throws Exception {
        int sourceStepX = 1;
        int sourceStepY = 1;
        int sourceOffsetX = sourceStepX * destOffsetX;
        int sourceOffsetY = sourceStepY * destOffsetY;
        if (this.synchronizeReadRegion) {
            synchronized (this.geoTiffImageReader) {
                return this.geoTiffImageReader.readRect(this.geoTiffBandSource.isGlobalShifted180(), sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY, destOffsetX, destOffsetY, destWidth, destHeight);
            }
        } else {
            return this.geoTiffImageReader.readRect(this.geoTiffBandSource.isGlobalShifted180(), sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY, destOffsetX, destOffsetY, destWidth, destHeight);
        }
    }
}
