package org.esa.snap.core.datamodel;

import javax.media.jai.Histogram;

/**
 * Wrapper around javax.media.jai.Histogram with overflow‑safe 64‑bit bins.
 */
public class WrappedHistogram {

    private final Histogram delegate;
    private final long[][] binsLong;


    public WrappedHistogram(int binCount, double lowValue, double highValue,
                            boolean intHistogram, boolean logHistogram) {
        this.delegate = StxFactory.createHistogram(binCount, lowValue, highValue,
                logHistogram, intHistogram);
        int bands = delegate.getNumBands();
        this.binsLong = new long[bands][];
        for (int b = 0; b < bands; b++) {
            this.binsLong[b] = new long[delegate.getNumBins(b)];
        }
    }

    public WrappedHistogram(Histogram delegate) {
        if (delegate == null) throw new NullPointerException("Histogram");
        this.delegate = delegate;
        int bands = delegate.getNumBands();
        this.binsLong = new long[bands][];

        for (int b = 0; b < bands; b++) {
            int n = delegate.getNumBins(b);
            this.binsLong[b] = new long[n];
            int[] db = delegate.getBins(b);
            for (int i = 0; i < n; i++) {
                this.binsLong[b][i] = db[i];
            }
        }
    }

    public void incrementBin(int band, int index) {
        delegate.getBins(band)[index]++;
        binsLong[band][index]++;
    }

    // delegate methods
    public int getNumBins(int band) {
        return delegate.getNumBins(band);
    }
    public double getLowValue(int band) {
        return delegate.getLowValue(band);
    }
    public double getHighValue(int band) {
        return delegate.getHighValue(band);
    }

    public long[] getLongBins(int band) {
        return binsLong[band];
    }

    public long getLongTotal(int band) {
        long sum = 0;
        for (long v : binsLong[band]) {
            sum += v;
        }
        return sum;
    }

    public Histogram getDelegateHistogram() {
        return delegate;
    }
}
