package org.esa.snap.dataio.geotiff;

import org.esa.snap.core.image.MosaicMatrix;

import java.awt.image.Raster;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by jcoravu on 7/1/2020.
 */
public class GeoTiffMatrixCell implements MosaicMatrix.MatrixCell, GeoTiffRasterRegion {

    private final int cellWidth;
    private final int cellHeight;
    private final int dataBufferType;
    private final Path imageParentPath;
    private final String imageRelativeFilePath;

    public GeoTiffMatrixCell(int cellWidth, int cellHeight, int dataBufferType, Path imageParentPath, String imageRelativeFilePath) {
        if (imageParentPath == null) {
            throw new NullPointerException("The image path is null.");
        }
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.dataBufferType = dataBufferType;
        this.imageParentPath = imageParentPath;
        this.imageRelativeFilePath = imageRelativeFilePath; // the relative path may be null
    }

    @Override
    public int getCellWidth() {
        return this.cellWidth;
    }

    @Override
    public int getCellHeight() {
        return this.cellHeight;
    }

    public int getDataBufferType() {
        return this.dataBufferType;
    }

    @Override
    public Raster readRect(boolean isGlobalShifted180, int sourceOffsetX, int sourceOffsetY, int sourceStepX, int sourceStepY,
                           int destOffsetX, int destOffsetY, int destWidth, int destHeight)
                           throws Exception {

        try (GeoTiffImageReader geoTiffImageReader = GeoTiffImageReader.buildGeoTiffImageReader(this.imageParentPath, this.imageRelativeFilePath)) {
            return geoTiffImageReader.readRect(isGlobalShifted180, sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY, destOffsetX, destOffsetY, destWidth, destHeight);
        }
    }
}
