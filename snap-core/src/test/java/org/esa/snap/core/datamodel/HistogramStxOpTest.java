package org.esa.snap.core.datamodel;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import javax.media.jai.UnpackedImageData;

import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;

import static org.junit.Assert.*;

public class HistogramStxOpTest {

    @Test
    @STTM("SNAP-3907")
    public void testAccumulateData_noOverflow_longBins() {
        HistogramStxOp op = new HistogramStxOp(1, 0.0, 1.0, false, false);
        WrappedHistogram wh = op.getWrappedHistogram();

        wh.getDelegateHistogram().getBins(0)[0] = Integer.MAX_VALUE;
        wh.getLongBins(0)[0] = Integer.MAX_VALUE;

        UnpackedImageData data = new UnpackedImageData(
                Raster.createBandedRaster(DataBuffer.TYPE_INT, 1, 1, 1, null),
                new Rectangle(0, 0, 1, 1),
                DataBuffer.TYPE_INT,
                new int[][]{{0}},
                1, 1,
                new int[]{0},
                false
        );

        op.accumulateData(data, null);

        long[] longBins = wh.getLongBins(0);
        assertEquals((long)Integer.MAX_VALUE + 1, longBins[0]);
        assertEquals((long)Integer.MAX_VALUE + 1, wh.getLongTotal(0));
    }
}