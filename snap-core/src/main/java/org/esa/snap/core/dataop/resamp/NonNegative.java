package org.esa.snap.core.dataop.resamp;

/**
 * Resampling decorator. Ensures that the decorated resampling yields non-negative values
 * by clipping any non-negative value to zero.
 *
 * @author Ralf Quast
 */
public class NonNegative implements Resampling {

    private final Resampling resampling;

    /**
     * Creates a new "non-negative" resampling from a given resampling type.
     *
     * @param resampling the resampling type.
     */
    public NonNegative(Resampling resampling) {
        this.resampling = resampling;
    }

    @Override
    public String getName() {
        return String.format("NON_NEGATIVE_%s", resampling.getName());
    }

    @Override
    public Index createIndex() {
        return resampling.createIndex();
    }

    @Override
    public void computeIndex(double x, double y, int width, int height, Index index) {
        resampling.computeIndex(x, y, width, height, index);
    }

    @Override
    public void computeCornerBasedIndex(double x, double y, int width, int height, Index index) {
        resampling.computeCornerBasedIndex(x, y, width, height, index);
    }

    @Override
    public double resample(Raster raster, Index index) throws Exception {
        return Math.max(0.0, resampling.resample(raster, index));
    }

    @Override
    public int getKernelSize() {
        return resampling.getKernelSize();
    }
}
