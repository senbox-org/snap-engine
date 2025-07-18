package org.esa.snap.core.datamodel;

import com.bc.ceres.annotation.STTM;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class WrappedHistogramTest {

    private static final int BIN_COUNT   = 5;
    private static final double LOW_VALUE  = 0.0;
    private static final double HIGH_VALUE = 1.0;

    private WrappedHistogram histogram;

    @Before
    public void setUp() {
        histogram = new WrappedHistogram(BIN_COUNT, LOW_VALUE, HIGH_VALUE, false, false);
    }

    @Test
    @STTM("SNAP-3907")
    public void testInitialBinsAreZero() {
        assertEquals(BIN_COUNT, histogram.getNumBins(0));
        long[] bins = histogram.getLongBins(0);
        for (long ii : bins) {
            assertEquals(0L, ii);
        }
        assertEquals(0L, histogram.getLongTotal(0));
    }

    @Test
    @STTM("SNAP-3907")
    public void testIncrementSingleBin() {
        histogram.incrementBin(0, 2);
        assertEquals(1L, histogram.getLongBins(0)[2]);
        assertEquals(1L, histogram.getLongTotal(0));
    }

    @Test
    @STTM("SNAP-3907")
    public void testIncrementMultipleTimesSameBin() {
        int times = 10;
        for (int ii = 0; ii < times; ii++) {
            histogram.incrementBin(0, 1);
        }
        assertEquals(times, histogram.getLongBins(0)[1]);
        assertEquals(times, histogram.getLongTotal(0));
    }

    @Test
    @STTM("SNAP-3907")
    public void testLongTotalAcrossBins() {
        histogram.incrementBin(0, 0);
        histogram.incrementBin(0, 3);
        histogram.incrementBin(0, 3);
        long[] bins = histogram.getLongBins(0);
        assertEquals(1L, bins[0]);
        assertEquals(2L, bins[3]);
        assertEquals(3L, histogram.getLongTotal(0));
    }

}