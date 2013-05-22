package org.esa.beam.binning.support;

import static org.junit.Assert.*;

import org.junit.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Marco Peters
 */
public class RegularGaussianGridTest {

    @Test
    public void testRegularGird() throws Exception {
        RegularGaussianGrid gaussianGrid = new RegularGaussianGrid(64);
        assertEquals(64, gaussianGrid.getNumRows());
        assertEquals(128, gaussianGrid.getNumCols(0));
        assertEquals(128, gaussianGrid.getNumCols(32));
        assertEquals(128, gaussianGrid.getNumCols(63));
        assertEquals(2, gaussianGrid.getRowIndex(270));
        assertEquals(64 * 128, gaussianGrid.getNumBins());
        assertEquals(0, gaussianGrid.getFirstBinIndex(0));
        assertEquals(10 * 128, gaussianGrid.getFirstBinIndex(10));
        assertEquals(50 * 128, gaussianGrid.getFirstBinIndex(50));

        assertEquals(-51.62573, gaussianGrid.getCenterLat(50), 1.0e-6);
        assertEquals(-87.86379, gaussianGrid.getCenterLat(63), 1.0e-6);

        // binIndex=300 -> row=2,col=44
        assertEquals(82.31291, gaussianGrid.getCenterLatLon(300)[0], 1.0e-6);
        assertEquals(-54.84375, gaussianGrid.getCenterLatLon(300)[1], 1.0e-6);

        // lat=45.0,lon=90.0 -> row=15,col=96 -> 2016
        assertEquals(2016, gaussianGrid.getBinIndex(45.0, 90.0));
    }

    @Test
    public void testFindClosestLat() throws Exception {
        double[] values = createValues(89.5, -89.5, -1);
        assertEquals(0, RegularGaussianGrid.findClosestLat(values, 89.5));
        assertEquals(179, RegularGaussianGrid.findClosestLat(values, -89.5));
        assertEquals(90, RegularGaussianGrid.findClosestLat(values, 0.0));
        assertEquals(90, RegularGaussianGrid.findClosestLat(values, -0.0));
        assertEquals(89, RegularGaussianGrid.findClosestLat(values, 0.0000000000001));

        assertEquals(0, RegularGaussianGrid.findClosestLat(values, 89.0000000000001));
        assertEquals(1, RegularGaussianGrid.findClosestLat(values, 89.0));
        assertEquals(178, RegularGaussianGrid.findClosestLat(values, -88.9999999999999));
        assertEquals(179, RegularGaussianGrid.findClosestLat(values, -89.0));

        assertEquals(44, RegularGaussianGrid.findClosestLat(values, 45.00000000001));
        assertEquals(45, RegularGaussianGrid.findClosestLat(values, 45.0));
        assertEquals(134, RegularGaussianGrid.findClosestLat(values, -44.9999999999999));
        assertEquals(135, RegularGaussianGrid.findClosestLat(values, -45.0));

        assertEquals(0, RegularGaussianGrid.findClosestLat(values, 90.0));
        assertEquals(179, RegularGaussianGrid.findClosestLat(values, -90.0));
    }

    @Test
    public void testFindClosestLon() throws Exception {
        double[] values = createValues(-179.5, 179.5, 1);
        assertEquals(0, RegularGaussianGrid.findClosestLon(values, -179.5));
        assertEquals(359, RegularGaussianGrid.findClosestLon(values, 179.5));
        assertEquals(180, RegularGaussianGrid.findClosestLon(values, 0.0));
        assertEquals(180, RegularGaussianGrid.findClosestLon(values, -0.0));
        assertEquals(179, RegularGaussianGrid.findClosestLon(values, -0.0000000000001));

        assertEquals(0, RegularGaussianGrid.findClosestLon(values, -179.0000000000001));
        assertEquals(1, RegularGaussianGrid.findClosestLon(values, -179.0));
        assertEquals(358, RegularGaussianGrid.findClosestLon(values, 178.9999999999999));
        assertEquals(359, RegularGaussianGrid.findClosestLon(values, 179.0));

        assertEquals(89, RegularGaussianGrid.findClosestLon(values, -90.00000000001));
        assertEquals(90, RegularGaussianGrid.findClosestLon(values, -90.0));
        assertEquals(269, RegularGaussianGrid.findClosestLon(values, 89.9999999999999));
        assertEquals(270, RegularGaussianGrid.findClosestLon(values, 90.0));

        assertEquals(0, RegularGaussianGrid.findClosestLon(values, -180.0));
        assertEquals(359, RegularGaussianGrid.findClosestLon(values, 180.0));
    }

    private double[] createValues(double min, double max, double step) {
        final List<Double> values = new ArrayList<Double>();
        double value = min;
        if (min<max) {
            while (value <= max) {
                values.add(value);
                value += step;
            }
        } else {
            while (value >= max) {
                values.add(value);
                value += step;
            }
        }
        final double[] doubles = new double[values.size()];
        for (int i = 0; i < doubles.length; i++) {
            doubles[i] = values.get(i);
        }
        return doubles;
    }
}
