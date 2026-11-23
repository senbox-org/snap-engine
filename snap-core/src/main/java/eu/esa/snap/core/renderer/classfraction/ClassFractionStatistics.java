package eu.esa.snap.core.renderer.classfraction;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.jai.NoDataRaster;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ImageManager;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Common-validity statistics for a class-fraction group.
 *
 * <p>{@code StxFactory.withRoiMask} remains the compatible single-band ROI mechanism, but it
 * always intersects each band's own valid mask. That cannot express this group's common validity
 * policy, notably {@link ClassBandGroupConfig.ValidityPolicy#FINITE_AND_DECLARED_NODATA}; this
 * class therefore traverses the original geophysical source images directly. It never reads the
 * rendered ARGB image and never creates a product band.</p>
 */
public final class ClassFractionStatistics {


    public static final int DEFAULT_HISTOGRAM_BIN_COUNT = 64;
    private static final int QUANTILE_BIN_COUNT = 2048;

    private ClassFractionStatistics() {}


    public static ClassFractionStatisticsResult compute(ClassFractionRendererConfig config,
                                                        List<RasterDataNode> sources,
                                                        Mask roiMask,
                                                        int histogramBinCount,
                                                        ProgressMonitor progressMonitor) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(sources, "sources");
        if (histogramBinCount < 2) {
            throw new IllegalArgumentException("histogramBinCount must be at least two");
        }

        ClassFractionRendererValidator.validate(config, sources);
        validateRoi(sources.get(0), roiMask);

        ProgressMonitor pm = progressMonitor != null ? progressMonitor : ProgressMonitor.NULL;
        List<PlanarImage> images = new ArrayList<>();
        List<PlanarImage> validMasks = new ArrayList<>();
        double[] noDataValues = new double[sources.size()];

        for (RasterDataNode source : sources) {
            images.add(ImageManager.getInstance().getGeophysicalImage(source, 0));
            validMasks.add(ImageManager.getInstance().getValidMaskImage(source, 0));
            noDataValues[images.size() - 1] = source.isNoDataValueUsed() ? source.getGeophysicalNoDataValue() : Double.NaN;
        }

        PlanarImage roiImage = roiMask != null ? PlanarImage.wrapRenderedImage(roiMask.getSourceImage()) : null;
        Accumulator[] accumulators = new Accumulator[sources.size()];

        for (int i = 0; i < accumulators.length; i++) {
            accumulators[i] = new Accumulator();
        }

        Counts counts = new Counts();
        pm.beginTask("Computing class-fraction statistics", 2);
        try {
            visit(images, validMasks, roiImage, noDataValues, config.getClassBandGroup().getValidityPolicy(),
                  (values) -> {
                      counts.validPixelCount++;
                      for (int i = 0; i < values.length; i++) {
                          accumulators[i].add(values[i]);
                      }
                  }, counts);
            pm.worked(1);

            long[][] histograms = new long[sources.size()][histogramBinCount];
            long[][] quantileHistograms = new long[sources.size()][QUANTILE_BIN_COUNT];
            visit(images, validMasks, roiImage, noDataValues, config.getClassBandGroup().getValidityPolicy(),
                  (values) -> {
                      for (int i = 0; i < values.length; i++) {
                          histograms[i][bin(values[i], accumulators[i].minimum, accumulators[i].maximum, histogramBinCount)]++;
                          quantileHistograms[i][bin(values[i], accumulators[i].minimum, accumulators[i].maximum, QUANTILE_BIN_COUNT)]++;
                      }
                  }, null);
            pm.worked(1);

            List<ClassFractionStatisticsResult.BandStatistics> resultBands = new ArrayList<>();
            for (int i = 0; i < accumulators.length; i++) {
                Accumulator accumulator = accumulators[i];
                double[] percentiles = percentiles(accumulator.minimum, accumulator.maximum, quantileHistograms[i], accumulator.count);
                resultBands.add(new ClassFractionStatisticsResult.BandStatistics(
                        config.getClassBandGroup().getBands().get(i), accumulator.count, accumulator.minimum,
                        accumulator.maximum, accumulator.mean, accumulator.standardDeviation(), percentiles[0], percentiles[1],
                        percentiles[2], percentiles[3], percentiles[4],
                        new ClassFractionStatisticsResult.ValueDistribution(accumulator.minimum, accumulator.maximum, histograms[i])));
            }

            return new ClassFractionStatisticsResult(roiMask != null ? roiMask.getName() : null,
                    counts.selectedPixelCount, counts.validPixelCount, counts.invalidPixelCount,
                    validityDescription(config.getClassBandGroup().getValidityPolicy(), roiMask), resultBands);
        } finally {
            pm.done();
        }
    }

    private static void validateRoi(RasterDataNode anchor, Mask roiMask) {
        if (roiMask == null) {
            return;
        }
        if (roiMask.getProduct() != anchor.getProduct()
                || roiMask.getRasterWidth() != anchor.getRasterWidth()
                || roiMask.getRasterHeight() != anchor.getRasterHeight()
                || !roiMask.getImageToModelTransform().equals(anchor.getImageToModelTransform())) {
            throw new IllegalArgumentException("ROI mask must belong to the class product and share its raster grid");
        }
    }

    private static String validityDescription(ClassBandGroupConfig.ValidityPolicy policy, Mask roiMask) {
        String roi = roiMask == null ? "Whole scene" : "ROI '" + roiMask.getName() + "'";
        if (policy == ClassBandGroupConfig.ValidityPolicy.FINITE_AND_DECLARED_NODATA) {
            return roi + "; every band must be finite and not declared NoData; source valid-pixel masks are intentionally ignored.";
        }
        return roi + "; every band must be finite, not declared NoData, and valid according to its source valid-pixel mask.";
    }

    private static int bin(double value, double minimum, double maximum, int binCount) {
        if (minimum == maximum) {
            return 0;
        }
        int index = (int) ((value - minimum) / (maximum - minimum) * binCount);
        return Math.max(0, Math.min(binCount - 1, index));
    }

    private static double[] percentiles(double minimum, double maximum, long[] histogram, long count) {
        return new double[]{
                percentile(minimum, maximum, histogram, count, 0.05),
                percentile(minimum, maximum, histogram, count, 0.25),
                percentile(minimum, maximum, histogram, count, 0.50),
                percentile(minimum, maximum, histogram, count, 0.75),
                percentile(minimum, maximum, histogram, count, 0.95)
        };
    }

    /**
     * Estimates a conventional interpolated sample percentile from a deliberately much finer
     * histogram than the one exposed by the UI. This bounds memory independently of scene size.
     */
    private static double percentile(double minimum, double maximum, long[] histogram, long count, double fraction) {
        if (count == 0 || !Double.isFinite(minimum) || !Double.isFinite(maximum)) {
            return Double.NaN;
        }
        if (minimum == maximum || count == 1) {
            return minimum;
        }
        double rank = fraction * (count - 1);
        long lowerRank = (long) Math.floor(rank);
        long upperRank = (long) Math.ceil(rank);
        double lowerValue = valueAtRank(minimum, maximum, histogram, count, lowerRank);
        double upperValue = valueAtRank(minimum, maximum, histogram, count, upperRank);
        return lowerValue + (upperValue - lowerValue) * (rank - lowerRank);
    }

    private static double valueAtRank(double minimum, double maximum, long[] histogram, long count, long rank) {
        if (rank <= 0) {
            return minimum;
        }
        if (rank >= count - 1) {
            return maximum;
        }
        long cumulative = histogram[0];
        int i = 0;
        while (cumulative <= rank) {
            i++;
            cumulative += histogram[i];
        }
        return minimum + (maximum - minimum) * (i + 0.5) / histogram.length;
    }

    private static void visit(List<PlanarImage> images, List<PlanarImage> validMasks, PlanarImage roiImage,
                              double[] noDataValues, ClassBandGroupConfig.ValidityPolicy policy,
                              ValuesConsumer consumer, Counts counts) {
        PlanarImage anchor = images.get(0);
        Rectangle imageBounds = anchor.getBounds();

        for (int tileY = anchor.getMinTileY(); tileY <= anchor.getMaxTileY(); tileY++) {
            for (int tileX = anchor.getMinTileX(); tileX <= anchor.getMaxTileX(); tileX++) {
                Raster anchorTile = anchor.getTile(tileX, tileY);

                if (anchorTile instanceof NoDataRaster) {
                    continue;
                }
                Rectangle rectangle = imageBounds.intersection(anchorTile.getBounds());
                Raster[] data = new Raster[images.size()];
                Raster[] masks = new Raster[images.size()];

                for (int i = 0; i < images.size(); i++) {
                    data[i] = images.get(i).getData(rectangle);
                    masks[i] = validMasks.get(i) != null ? validMasks.get(i).getData(rectangle) : null;
                }
                Raster roi = roiImage != null ? roiImage.getData(rectangle) : null;
                double[] values = new double[images.size()];

                for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                    for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                        if (roi != null && roi.getSample(x, y, 0) == 0) {
                            continue;
                        }
                        if (counts != null) {
                            counts.selectedPixelCount++;
                        }
                        boolean valid = true;

                        for (int i = 0; i < values.length; i++) {
                            values[i] = data[i].getSampleDouble(x, y, 0);
                            if (!Double.isFinite(values[i]) || (Double.isFinite(noDataValues[i])
                                    && Double.compare(values[i], noDataValues[i]) == 0)
                                    || (policy == ClassBandGroupConfig.ValidityPolicy.STRICT_SOURCE_VALIDITY
                                    && masks[i] != null && masks[i].getSample(x, y, 0) == 0)) {
                                valid = false;
                                break;
                            }
                        }
                        if (valid) {
                            consumer.accept(values);
                        } else if (counts != null) {
                            counts.invalidPixelCount++;
                        }
                    }
                }
            }
        }
    }

    private interface ValuesConsumer {
        void accept(double[] values);
    }

    private static final class Counts {
        private long selectedPixelCount;
        private long validPixelCount;
        private long invalidPixelCount;
    }

    private static final class Accumulator {
        private long count;
        private double minimum = Double.NaN;
        private double maximum = Double.NaN;
        private double mean;
        private double m2;

        private void add(double value) {
            if (count == 0) {
                minimum = value;
                maximum = value;
            } else {
                minimum = Math.min(minimum, value);
                maximum = Math.max(maximum, value);
            }
            count++;
            double delta = value - mean;
            mean += delta / count;
            m2 += delta * (value - mean);
        }

        private double standardDeviation() {
            return count == 0 ? Double.NaN : count == 1 ? 0.0 : Math.sqrt(m2 / (count - 1));
        }
    }
}
