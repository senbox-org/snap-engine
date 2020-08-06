package org.esa.snap.dataio.geotiff;

import org.esa.snap.core.image.MosaicMatrix;

import java.awt.image.Raster;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by jcoravu on 7/1/2020.
 */
public class GeoTiffMatrixCell extends GeoTiffFile implements MosaicMatrix.MatrixCell, GeoTiffRasterRegion, Closeable {

    private final int cellWidth;
    private final int cellHeight;
    private final int dataBufferType;

    private GeoTiffImageReader geoTiffImageReader;

    public GeoTiffMatrixCell(int cellWidth, int cellHeight, int dataBufferType, Path imageParentPath, String imageRelativeFilePath, Path localTempFolder) {
        super(imageParentPath, imageRelativeFilePath, true, localTempFolder);

        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.dataBufferType = dataBufferType;
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

        if (this.geoTiffImageReader == null) {
            this.geoTiffImageReader = buildImageReader();
        }
        return this.geoTiffImageReader.readRect(isGlobalShifted180, sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY, destOffsetX, destOffsetY, destWidth, destHeight);
    }

    @Override
    protected void cleanup() throws IOException {
        if (this.geoTiffImageReader != null) {
            this.geoTiffImageReader.close();
            this.geoTiffImageReader = null;
        }

        super.cleanup();
    }
}
