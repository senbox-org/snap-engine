package eu.esa.snap.core.renderer.classfraction;


/**
 * Statistics calculated from the original samples of one configured class band.
 */
public final class ClassFractionBandStatistics {


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
    private final ClassFractionValueDistribution distribution;


    ClassFractionBandStatistics(ClassBandConfig classBand, long validPixelCount, double minimum, double maximum,
                                double mean, double standardDeviation, double percentile05, double percentile25,
                                double median, double percentile75, double percentile95,
                                ClassFractionValueDistribution distribution) {
        this.classBand = classBand;
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
        this.distribution = distribution;
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

    public ClassFractionValueDistribution getDistribution() {
        return distribution;
    }
}
