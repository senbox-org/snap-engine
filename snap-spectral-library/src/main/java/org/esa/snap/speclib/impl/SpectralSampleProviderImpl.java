package org.esa.snap.speclib.impl;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.speclib.api.SpectralSampleProvider;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.image.Raster;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


public class SpectralSampleProviderImpl implements SpectralSampleProvider {


    private record BBox(int minX, int minY, int w, int h, Rectangle maskRect) {}


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

    @Override
    public double[][] readSamples(List<Band> bands, int[] xs, int[] ys, int level) {
        validateBulkArgs(bands, xs, ys);
        final int nPix = xs.length;
        final int nBands = bands.size();
        final double[][] out = createNaNMatrix(nPix, nBands);

        if (nPix == 0 || nBands == 0) {
            return out;
        }

        Band ref = firstNonNullBand(bands);
        if (ref == null) {
            return out;
        }
        Optional<BBox> bboxOpt = computeBBox(ref, xs, ys, level);
        if (bboxOpt.isEmpty()) {
            return out;
        }

        BBox bbox = bboxOpt.get();
        fillFromBBoxPerBand(bands, xs, ys, level, bbox, out);
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


    private static void validateBulkArgs(List<Band> bands, int[] xs, int[] ys) {
        Objects.requireNonNull(bands, "bands must not be null");
        Objects.requireNonNull(xs, "xs must not be null");
        Objects.requireNonNull(ys, "ys must not be null");

        if (xs.length != ys.length) {
            throw new IllegalArgumentException("xs/ys length mismatch");
        }
    }

    private static double[][] createNaNMatrix(int nPix, int nBands) {
        double[][] out = new double[nPix][nBands];
        for (int i = 0; i < nPix; i++) {
            Arrays.fill(out[i], Double.NaN);
        }
        return out;
    }

    private static Band firstNonNullBand(List<Band> bands) {
        for (Band b : bands) {
            if (b != null) {
                return b;
            }
        }
        return null;
    }

    private Optional<BBox> computeBBox(Band ref, int[] xs, int[] ys, int level) {
        int minLx = Integer.MAX_VALUE, minLy = Integer.MAX_VALUE, maxLx = Integer.MIN_VALUE, maxLy = Integer.MIN_VALUE;
        int minX  = Integer.MAX_VALUE, minY  = Integer.MAX_VALUE, maxX  = Integer.MIN_VALUE, maxY  = Integer.MIN_VALUE;

        for (int i = 0; i < xs.length; i++) {
            int x = xs[i], y = ys[i];
            if (x < 0 || y < 0) {
                continue;
            }
            if (!isWithinLevelBounds(ref, x, y, level)) {
                continue;
            }

            minLx = Math.min(minLx, x); minLy = Math.min(minLy, y);
            maxLx = Math.max(maxLx, x); maxLy = Math.max(maxLy, y);

            int bx = (level != 0) ? (x << level) : x;
            int by = (level != 0) ? (y << level) : y;

            minX = Math.min(minX, bx); minY = Math.min(minY, by);
            maxX = Math.max(maxX, bx); maxY = Math.max(maxY, by);
        }

        if (minX == Integer.MAX_VALUE) {
            return Optional.empty();
        }

        int w  = (maxX - minX) + 1;
        int h  = (maxY - minY) + 1;
        int wL = (maxLx - minLx) + 1;
        int hL = (maxLy - minLy) + 1;

        Rectangle maskRect = new Rectangle(minLx, minLy, Math.max(1, wL), Math.max(1, hL));
        return Optional.of(new BBox(minX, minY, w, h, maskRect));
    }

    private void fillFromBBoxPerBand(List<Band> bands, int[] xs, int[] ys, int level, BBox bbox, double[][] out) {
        final int nPix = xs.length;

        for (int bi = 0; bi < bands.size(); bi++) {
            Band band = bands.get(bi);
            if (band == null) {
                continue;
            }

            double[] buf = readBandBlockOrNull(band, bbox);
            if (buf == null) {
                continue;
            }

            double noDataVal = band.getGeophysicalNoDataValue();

            for (int i = 0; i < nPix; i++) {
                if (!isWithinLevelBounds(band, xs[i], ys[i], level)) {
                    continue;
                }

                int bx = (level != 0) ? (xs[i] << level) : xs[i];
                int by = (level != 0) ? (ys[i] << level) : ys[i];

                int ix = bx - bbox.minX();
                int iy = by - bbox.minY();
                if (ix < 0 || iy < 0 || ix >= bbox.w() || iy >= bbox.h()) {
                    continue;
                }

                double v = buf[iy * bbox.w() + ix];
                if (isRejectedValue(v, noDataVal)) {
                    continue;
                }

                out[i][bi] = v;
            }
        }
    }

    private static double[] readBandBlockOrNull(Band band, BBox bbox) {
        try {
            double[] buf = new double[bbox.w() * bbox.h()];
            band.readPixels(bbox.minX(), bbox.minY(), bbox.w(), bbox.h(), buf);
            return buf;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isRejectedValue(double v, double noDataVal) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return true;
        }
        return !Double.isNaN(noDataVal) && Double.compare(v, noDataVal) == 0;
    }
}
