package org.esa.snap.dem.dataio.copernicus;

import org.esa.snap.dataio.geotiff.GeoTiffImageReader;

import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;


public final class GeoTiffCopernicusTileSource implements CopernicusTileSource {


    private final GeoTiffImageReader reader;
    private final int width;
    private final int height;


    public GeoTiffCopernicusTileSource(final File file) throws IOException {
        reader = new GeoTiffImageReader(file);
        width = reader.getImageWidth();
        height = reader.getImageHeight();
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public float[] readRows(final int y, final int rowCount) throws IOException {
        final Raster raster = reader.readRect(false, 0, y, 1, 1, 0, y, width, rowCount);
        final float[] samples = new float[width * rowCount];
        final int minX = raster.getMinX();
        final int minY = raster.getMinY();
        int index = 0;
        for (int row = 0; row < rowCount; row++) {
            for (int x = 0; x < width; x++) {
                samples[index++] = raster.getSampleFloat(minX + x, minY + row, 0);
            }
        }
        return samples;
    }

    @Override
    public void close() {
        reader.close();
    }
}
