package org.esa.snap.speclib.impl;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.speclib.api.SpectralSampleProvider;

import javax.media.jai.PlanarImage;
import java.awt.image.Raster;
import java.util.List;
import java.util.Objects;


public class SpectralSampleProviderImpl implements SpectralSampleProvider {


    @Override
    public double[] readSamples(List<Band> bands, int x, int y, int level) {
        Objects.requireNonNull(bands, "bands must not be null");

        double[] out = new double[bands.size()];
        for (int i = 0; i < bands.size(); i++) {
            Band b = bands.get(i);
            out[i] = readSampleOrNaN(b, x, y, level);
        }
        return out;
    }

    private double readSampleOrNaN(Band band, int x, int y, int level) {
        if (band == null) {
            return Double.NaN;
        }

        if (!isWithinLevelBounds(band, x, y, level)) {
            return Double.NaN;
        }

        if (!isPixelValid(band, x, y, level)) {
            return Double.NaN;
        }

        final double value;
        try {
            value = ProductUtils.getGeophysicalSampleAsDouble(band, x, y, level);
        } catch (Throwable t) {
            return Double.NaN;
        }

        final double noDataVal = band.getGeophysicalNoDataValue();

        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return Double.NaN;
        }
        if (!Double.isNaN(noDataVal) && Double.compare(value, noDataVal) == 0) {
            return Double.NaN;
        }
        return value;
    }

    private boolean isWithinLevelBounds(Band band, int x, int y, int level) {
        if (x < 0 || y < 0) {
            return false;
        }

        int w = band.getRasterWidth();
        int h = band.getRasterHeight();
        if (level > 0) {
            w = (w + (1 << level) - 1) >> level;
            h = (h + (1 << level) - 1) >> level;
        }
        return x < w && y < h;
    }

    private boolean isPixelValid(Band band, int x, int y, int level) {
        if (!band.isValidMaskUsed()) {
            return true;
        }

        PlanarImage image = ImageManager.getInstance().getValidMaskImage(band, level);
        if (image == null) {
            return true;
        }

        int tx = image.XToTileX(x);
        int ty = image.YToTileY(y);
        Raster tile = image.getTile(tx, ty);
        if (tile == null) {
            return true;
        }

        if (!tile.getBounds().contains(x, y)) {
            return true;
        }

        return tile.getSample(x, y, 0) != 0;
    }
}
