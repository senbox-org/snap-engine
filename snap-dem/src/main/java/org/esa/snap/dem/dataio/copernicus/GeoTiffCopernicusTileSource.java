package org.esa.snap.dem.dataio.copernicus;

import org.esa.snap.dataio.geotiff.GeoTiffImageReader;

import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;


public final class GeoTiffCopernicusTileSource implements CopernicusTileSource {


    private final GeoTiffImageReader reader;
    private final int width;
    private final int height;
    private final int preferredBlockHeight;


    public GeoTiffCopernicusTileSource(final File file) throws IOException {
        reader = new GeoTiffImageReader(file);
        width = reader.getImageWidth();
        height = reader.getImageHeight();
        final int tileHeight = reader.getTileHeight();
        // A reader reporting 0 or 1 row is telling us it does not know the layout - a strip-per-row
        // file reports 1, and GeoTiffImageReader.computePreferredTiling treats tileHeight <= 1 as
        // bad tiling too. Passing that on would be worse than saying nothing: every block height is
        // a whole multiple of 1, so alignment silently becomes a no-op and unaligned reads get
        // through. Fall back to the contract's "report the full image height".
        preferredBlockHeight = tileHeight > 1 ? Math.min(tileHeight, height) : height;
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
    public int getPreferredBlockHeight() {
        return preferredBlockHeight;
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
