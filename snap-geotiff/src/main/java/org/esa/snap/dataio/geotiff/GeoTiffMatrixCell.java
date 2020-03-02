package org.esa.snap.dataio.geotiff;

import org.esa.snap.core.image.MosaicMatrix;

/**
 * Created by jcoravu on 7/1/2020.
 */
public class GeoTiffMatrixCell implements MosaicMatrix.MatrixCell {

    private final int cellWidth;
    private final int cellHeight;
    private final GeoTiffImageReader geoTiffImageReader;
    private final int dataBufferType;

    public GeoTiffMatrixCell(int cellWidth, int cellHeight, GeoTiffImageReader geoTiffImageReader, int dataBufferType) {
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.geoTiffImageReader = geoTiffImageReader;
        this.dataBufferType = dataBufferType;
    }

    @Override
    public int getCellWidth() {
        return cellWidth;
    }

    @Override
    public int getCellHeight() {
        return cellHeight;
    }

    public int getDataBufferType() {
        return dataBufferType;
    }

    public GeoTiffImageReader getGeoTiffImageReader() {
        return geoTiffImageReader;
    }
}
