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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AggregatorSumTest {

    private BinContext ctx;

    @Before
    public void setUp() {
        ctx = createCtx();
    }

    @Test
    public void testRequiresGrowableSpatialData() {
        final AggregatorSum agg = new AggregatorSum(new MyVariableContext("var"), "var");

        assertFalse(agg.requiresGrowableSpatialData());
    }

    @Test
    public void testMetadata() {
        final AggregatorSum agg = new AggregatorSum(new MyVariableContext("var"), "var");

        assertEquals("SUM", agg.getName());

        assertEquals(2, agg.getSpatialFeatureNames().length);
        assertEquals("var_sum", agg.getSpatialFeatureNames()[0]);
        assertEquals("var_counts", agg.getSpatialFeatureNames()[1]);

        assertEquals(2, agg.getTemporalFeatureNames().length);
        assertEquals("var_sum", agg.getTemporalFeatureNames()[0]);
        assertEquals("var_counts", agg.getTemporalFeatureNames()[1]);

        assertEquals(2, agg.getOutputFeatureNames().length);
        assertEquals("var_sum", agg.getOutputFeatureNames()[0]);
        assertEquals("var_counts", agg.getOutputFeatureNames()[1]);
    }

    @Test
    public void testAggregateSpatial_noMeasurements() {
        final AggregatorSum agg = new AggregatorSum(new MyVariableContext("the_var"), "the_var");

        final VectorImpl svec = vec(NaN);
        agg.initSpatial(ctx, svec);

        assertEquals(0.f, svec.get(0), 1e-8);

        agg.completeSpatial(ctx, 0, svec);
        assertEquals(0.f, svec.get(0), 1e-8);
    }

    @Test
    public void testAggregateSpatial_oneMeasurement() {
        final AggregatorSum agg = new AggregatorSum(new MyVariableContext("the_var"), "the_var");

        final VectorImpl svec = vec(NaN);
        agg.initSpatial(ctx, svec);

        agg.aggregateSpatial(ctx, obsNT(0.32f), svec);
        assertEquals(1, svec.size());
        assertEquals(0.32f, svec.get(0), 1e-8);

        agg.completeSpatial(ctx, 1, svec);
        assertEquals(1, svec.size());
        assertEquals(0.32f, svec.get(0), 1e-8);
    }

}
