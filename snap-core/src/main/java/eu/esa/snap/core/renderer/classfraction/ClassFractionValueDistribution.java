package eu.esa.snap.core.renderer.classfraction;


/**
 * Fixed-width histogram of the unmodified original samples.
 */
public final class ClassFractionValueDistribution {


    private final double minimum;
    private final double maximum;
    private final long[] binCounts;


    ClassFractionValueDistribution(double minimum, double maximum, long[] binCounts) {
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
