package org.esa.snap.binning.aggregators;

import org.esa.snap.binning.BinContext;
import org.esa.snap.binning.MyVariableContext;
import org.esa.snap.binning.support.GrowableVector;
import org.esa.snap.binning.support.VectorImpl;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Float.NaN;
import static org.esa.snap.binning.aggregators.AggregatorTestUtils.createCtx;
import static org.esa.snap.binning.aggregators.AggregatorTestUtils.obsNT;
import static org.esa.snap.binning.aggregators.AggregatorTestUtils.vec;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AggregatorAverageOutlierAwareTest {

    private BinContext ctx;

    @Before
    public void setUp() {
        ctx = createCtx();
    }

    @Test
    public void testRequiresGrowableSpatialData() {
        final AggregatorAverageOutlierAware agg = new AggregatorAverageOutlierAware(new MyVariableContext("var"), "var", 1.3);

        assertTrue(agg.requiresGrowableSpatialData());
    }

    @Test
    public void testMetadata() {
        final AggregatorAverageOutlierAware agg = new AggregatorAverageOutlierAware(new MyVariableContext("var"), "var", 1.4);

        assertEquals("AVG_OUTLIER", agg.getName());

        // @todo 1 tb/tb add checks for spatial features

        assertEquals(3, agg.getTemporalFeatureNames().length);
        assertEquals("var_mean", agg.getTemporalFeatureNames()[0]);
        assertEquals("var_sigma", agg.getTemporalFeatureNames()[1]);
        assertEquals("var_counts", agg.getTemporalFeatureNames()[2]);

        assertEquals(3, agg.getOutputFeatureNames().length);
        assertEquals("var_mean", agg.getOutputFeatureNames()[0]);
        assertEquals("var_sigma", agg.getOutputFeatureNames()[1]);
        assertEquals("var_counts", agg.getOutputFeatureNames()[2]);
    }

    @Test
    public void testAggregateSpatial_noMeasurements() {
        final AggregatorAverageOutlierAware agg = new AggregatorAverageOutlierAware(new MyVariableContext("var"), "var", 1.3);

        agg.initSpatial(ctx, new GrowableVector(12));

        agg.completeSpatial(ctx, 0, new GrowableVector(12));
    }

    @Test
    public void testAggregateSpatial_oneMeasurement() {
        final AggregatorAverageOutlierAware agg = new AggregatorAverageOutlierAware(new MyVariableContext("var"), "var", 1.3);

        final GrowableVector vector = new GrowableVector(12);

        agg.initSpatial(ctx, vector);

        agg.aggregateSpatial(ctx, obsNT(0.21f), vector);
        assertEquals(1, vector.size());
        assertEquals(0.21, vector.get(0), 1e-8);

        agg.completeSpatial(ctx, 0, vector);
    }

    @Test
    public void testAggregateSpatial_threeMeasurements() {
        final AggregatorAverageOutlierAware agg = new AggregatorAverageOutlierAware(new MyVariableContext("var"), "var", 1.2);

        final GrowableVector vector = new GrowableVector(12);

        agg.initSpatial(ctx, vector);

        agg.aggregateSpatial(ctx, obsNT(0.31f), vector);
        agg.aggregateSpatial(ctx, obsNT(0.32f), vector);
        agg.aggregateSpatial(ctx, obsNT(0.33f), vector);

        assertEquals(3, vector.size());
        assertEquals(0.31f, vector.get(0), 1e-8);
        assertEquals(0.32f, vector.get(1), 1e-8);
        assertEquals(0.33f, vector.get(2), 1e-8);

        agg.completeSpatial(ctx, 0, vector);
    }

    @Test
    public void testAggregateTemporal_noMeasurements() {
        final AggregatorAverageOutlierAware agg = new AggregatorAverageOutlierAware(new MyVariableContext("var"), "var", 1.5);

        final VectorImpl tvec = vec(NaN, NaN, NaN);
        final VectorImpl out = vec(NaN, NaN, NaN);

        agg.initTemporal(ctx, tvec);
        assertEquals(0.0f, tvec.get(0), 1e-8);
        assertEquals(0.0f, tvec.get(1), 1e-8);
        assertEquals(0.0f, tvec.get(2), 1e-8);

        agg.completeTemporal(ctx, 0, tvec);
        assertEquals(NaN, tvec.get(0), 1e-8);
        assertEquals(NaN, tvec.get(1), 1e-8);
        assertEquals(0, tvec.get(2), 1e-8);

        agg.computeOutput(tvec, out);
        assertEquals(NaN, out.get(0), 1e-8);
        assertEquals(NaN, out.get(1), 1e-8);
        assertEquals(0, out.get(2), 1e-8);
    }

    @Test
    public void testAggregateTemporal_oneMeasurement() {
        final AggregatorAverageOutlierAware agg = new AggregatorAverageOutlierAware(new MyVariableContext("var"), "var", 1.6);

        final VectorImpl tvec = vec(NaN, NaN, NaN);
        final VectorImpl out = vec(NaN, NaN, NaN);

        agg.initTemporal(ctx, tvec);

        agg.aggregateTemporal(ctx, vec(0.68f), 1, tvec);

        agg.completeTemporal(ctx, 0, tvec);
        assertEquals(0.68, tvec.get(0), 1e-8);
        assertEquals(0.0, tvec.get(1), 1e-8);
        assertEquals(1, tvec.get(2), 1e-8);

        agg.computeOutput(tvec, out);
        assertEquals(0.68, out.get(0), 1e-8);
        assertEquals(0.0, out.get(1), 1e-8);
        assertEquals(1, out.get(2), 1e-8);
    }

    @Test
    public void testAggregateTemporal_twoMeasurements() {
        final AggregatorAverageOutlierAware agg = new AggregatorAverageOutlierAware(new MyVariableContext("var"), "var", 1.7);

        final VectorImpl tvec = vec(NaN, NaN, NaN);
        final VectorImpl out = vec(NaN, NaN, NaN);

        agg.initTemporal(ctx, tvec);

        agg.aggregateTemporal(ctx, vec(0.68f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.72f), 1, tvec);

        agg.completeTemporal(ctx, 0, tvec);
        assertEquals(0.7000000476837158, tvec.get(0), 1e-8);
        assertEquals(0.02000001072883606, tvec.get(1), 1e-8);
        assertEquals(2, tvec.get(2), 1e-8);

        agg.computeOutput(tvec, out);
        assertEquals(0.7000000476837158, out.get(0), 1e-8);
        assertEquals(0.02000001072883606, out.get(1), 1e-8);
        assertEquals(2, out.get(2), 1e-8);
    }

    @Test
    public void testAggregateTemporal_fiveMeasurements_withOutlier() {
        final AggregatorAverageOutlierAware agg = new AggregatorAverageOutlierAware(new MyVariableContext("var"), "var", 1.9);

        final VectorImpl tvec = vec(NaN, NaN, NaN);
        final VectorImpl out = vec(NaN, NaN, NaN);

        agg.initTemporal(ctx, tvec);

        agg.aggregateTemporal(ctx, vec(0.68f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.72f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(1.87f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.64f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.7f), 1, tvec);

        agg.completeTemporal(ctx, 0, tvec);
        assertEquals(0.6850000023841858, tvec.get(0), 1e-8);
        assertEquals(0.029580410569906235, tvec.get(1), 1e-8);
        assertEquals(4, tvec.get(2), 1e-8);

        agg.computeOutput(tvec, out);
        assertEquals(0.6850000023841858, out.get(0), 1e-8);
        assertEquals(0.029580410569906235, out.get(1), 1e-8);
        assertEquals(4, out.get(2), 1e-8);
    }
}
