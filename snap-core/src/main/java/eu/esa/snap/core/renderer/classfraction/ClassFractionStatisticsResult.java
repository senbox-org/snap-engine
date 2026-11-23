package eu.esa.snap.core.renderer.classfraction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * Diagnostic result for statistics calculated from original class-band samples.
 */
public final class ClassFractionStatisticsResult {


    private final String roiName;
    private final long selectedPixelCount;
    private final long validPixelCount;
    private final long invalidPixelCount;
    private final String validityRule;
    private final List<BandStatistics> bandStatistics;


    ClassFractionStatisticsResult(String roiName, long selectedPixelCount, long validPixelCount,
                                  long invalidPixelCount, String validityRule,
                                  List<BandStatistics> bandStatistics) {
        this.roiName = roiName;
        this.selectedPixelCount = selectedPixelCount;
        this.validPixelCount = validPixelCount;
        this.invalidPixelCount = invalidPixelCount;
        this.validityRule = Objects.requireNonNull(validityRule, "validityRule");
        this.bandStatistics = Collections.unmodifiableList(new ArrayList<>(bandStatistics));
    }


    public String getRoiName() {
        return roiName;
    }

    public long getSelectedPixelCount() {
        return selectedPixelCount;
    }

    public long getValidPixelCount() {
        return validPixelCount;
    }

    public long getInvalidPixelCount() {
        return invalidPixelCount;
    }

    public String getValidityRule() {
        return validityRule;
    }

    public List<BandStatistics> getBandStatistics() {
        return bandStatistics;
    }


    public static final class BandStatistics {

        private final ClassBandConfig classBand;
        private final long validPixelCount;
        private final double minimum;
        private final double maximum;
        private final double mean;
        private final double standardDeviation;
        private final double percentile05;
        private final double percentile25;
        private final double median;
        private final double percentile75;
        private final double percentile95;
        private final ValueDistribution distribution;

        BandStatistics(ClassBandConfig classBand, long validPixelCount, double minimum, double maximum,
                       double mean, double standardDeviation, double percentile05, double percentile25,
                       double median, double percentile75, double percentile95, ValueDistribution distribution) {
            this.classBand = Objects.requireNonNull(classBand, "classBand");
            this.validPixelCount = validPixelCount;
            this.minimum = minimum;
            this.maximum = maximum;
            this.mean = mean;
            this.standardDeviation = standardDeviation;
            this.percentile05 = percentile05;
            this.percentile25 = percentile25;
            this.median = median;
            this.percentile75 = percentile75;
            this.percentile95 = percentile95;
            this.distribution = Objects.requireNonNull(distribution, "distribution");
        }

        public ClassBandConfig getClassBand() {
            return classBand;
        }

        public long getValidPixelCount() {
            return validPixelCount;
        }

        public double getMinimum() {
            return minimum;
        }

        public double getMaximum() {
            return maximum;
        }

        public double getMean() {
            return mean;
        }

        public double getStandardDeviation() {
            return standardDeviation;
        }

        public double getPercentile05() {
            return percentile05;
        }

        public double getPercentile25() {
            return percentile25;
        }

        public double getMedian() {
            return median;
        }

        public double getPercentile75() {
            return percentile75;
        }

        public double getPercentile95() {
            return percentile95;
        }

        public ValueDistribution getDistribution() {
            return distribution;
        }
    }

    /**
     * Fixed-width histogram of the unmodified original samples.
     */
    public static final class ValueDistribution {
        private final double minimum;
        private final double maximum;
        private final long[] binCounts;

        ValueDistribution(double minimum, double maximum, long[] binCounts) {
            this.minimum = minimum;
            this.maximum = maximum;
            this.binCounts = binCounts.clone();
        }

        public double getMinimum() {
            return minimum;
        }

        public double getMaximum() {
            return maximum;
        }

        public int getBinCount() {
            return binCounts.length;
        }

        public long getBinCount(int index) {
            return binCounts[index];
        }

        public long[] getBinCounts() {
            return binCounts.clone();
        }

        public double getBinMinimum(int index) {
            return minimum + (maximum - minimum) * index / binCounts.length;
        }

        public double getBinMaximum(int index) {
            return index == binCounts.length - 1 ? maximum : getBinMinimum(index + 1);
        }
    }
}
