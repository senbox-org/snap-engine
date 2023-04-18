package org.esa.snap.binning.aggregators;

import org.esa.snap.binning.Aggregator;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.BinContext;
import org.esa.snap.binning.MyVariableContext;
import org.esa.snap.binning.support.VectorImpl;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Float.NaN;
import static org.esa.snap.binning.aggregators.AggregatorTestUtils.createCtx;
import static org.esa.snap.binning.aggregators.AggregatorTestUtils.obsNT;
import static org.esa.snap.binning.aggregators.AggregatorTestUtils.vec;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AggregatorSumTest {

    private BinContext ctx;

    @Before
    public void setUp() {
        ctx = createCtx();
    }

    @Test
    public void testRequiresGrowableSpatialData() {
        final AggregatorSum agg = new AggregatorSum(new MyVariableContext("var"), "var", "out");

        assertFalse(agg.requiresGrowableSpatialData());
    }

    @Test
    public void testMetadata() {
        final AggregatorSum agg = new AggregatorSum(new MyVariableContext("var"), "var", "out");

        assertEquals("SUM", agg.getName());

        assertEquals(1, agg.getSpatialFeatureNames().length);
        assertEquals("var_sum", agg.getSpatialFeatureNames()[0]);

        assertEquals(1, agg.getTemporalFeatureNames().length);
        assertEquals("var_sum", agg.getTemporalFeatureNames()[0]);

        assertEquals(1, agg.getOutputFeatureNames().length);
        assertEquals("out_sum", agg.getOutputFeatureNames()[0]);
    }

    @Test
    public void testAggregateSpatial_noMeasurements() {
        final AggregatorSum agg = new AggregatorSum(new MyVariableContext("the_var"), "the_var", "out");

        final VectorImpl svec = vec(NaN);
        agg.initSpatial(ctx, svec);

        assertEquals(0.f, svec.get(0), 1e-8);

        agg.completeSpatial(ctx, 0, svec);
        assertEquals(0.f, svec.get(0), 1e-8);
    }

    @Test
    public void testAggregateSpatial_oneMeasurement() {
        final AggregatorSum agg = new AggregatorSum(new MyVariableContext("the_var"), "the_var", "out");

        final VectorImpl svec = vec(NaN);
        agg.initSpatial(ctx, svec);

        agg.aggregateSpatial(ctx, obsNT(0.32f), svec);
        assertEquals(1, svec.size());
        assertEquals(0.32f, svec.get(0), 1e-8);

        agg.completeSpatial(ctx, 1, svec);
        assertEquals(1, svec.size());
        assertEquals(0.32f, svec.get(0), 1e-8);
    }

    @Test
    public void testAggregateSpatial_threeMeasurement() {
        final AggregatorSum agg = new AggregatorSum(new MyVariableContext("var"), "var", "out");

        final VectorImpl svec = vec(NaN);
        agg.initSpatial(ctx, svec);

        agg.aggregateSpatial(ctx, obsNT(0.32f), svec);
        agg.aggregateSpatial(ctx, obsNT(0.33f), svec);
        agg.aggregateSpatial(ctx, obsNT(0.34f), svec);
        assertEquals(1, svec.size());
        assertEquals(0.99f, svec.get(0), 1e-8);

        agg.completeSpatial(ctx, 1, svec);
        assertEquals(1, svec.size());
        assertEquals(0.99f, svec.get(0), 1e-8);
    }

    @Test
    public void testAggregateSpatial_threeMeasurement_withFillValue() {
        final AggregatorSum agg = new AggregatorSum(new MyVariableContext("var"), "var", "out");

        final VectorImpl svec = vec(NaN);
        agg.initSpatial(ctx, svec);

        agg.aggregateSpatial(ctx, obsNT(Float.NaN), svec);
        agg.aggregateSpatial(ctx, obsNT(0.33f), svec);
        agg.aggregateSpatial(ctx, obsNT(0.34f), svec);
        assertEquals(1, svec.size());
        assertEquals(0.67f, svec.get(0), 1e-8);

        agg.completeSpatial(ctx, 1, svec);
        assertEquals(1, svec.size());
        assertEquals(0.67f, svec.get(0), 1e-8);
    }

    @Test
    public void testAggregateTemporal_noMeasurements() {
        final AggregatorSum agg = new AggregatorSum(new MyVariableContext("var"), "var", "out");

        final VectorImpl tvec = vec(NaN);
        final VectorImpl out = vec(NaN);

        agg.initTemporal(ctx, tvec);
        assertEquals(0.0f, tvec.get(0), 1e-8);

        agg.completeTemporal(ctx, 0, tvec);
        assertEquals(0, tvec.get(0), 1e-8);

        agg.computeOutput(tvec, out);
        assertEquals(0, out.get(0), 1e-8);
    }

    @Test
    public void testAggregateTemporal_oneMeasurement() {
        final AggregatorSum agg = new AggregatorSum(new MyVariableContext("var"), "var", "out");

        final VectorImpl tvec = vec(NaN);
        final VectorImpl out = vec(NaN);

        agg.initTemporal(ctx, tvec);

        agg.aggregateTemporal(ctx, vec(0.69f), 1, tvec);

        agg.completeTemporal(ctx, 1, tvec);
        assertEquals(0.69f, tvec.get(0), 1e-8);

        agg.computeOutput(tvec, out);
        assertEquals(0.69f, out.get(0), 1e-8);
    }

    @Test
    public void testAggregateTemporal_threeMeasurements() {
        final AggregatorSum agg = new AggregatorSum(new MyVariableContext("var"), "var", "out");

        final VectorImpl tvec = vec(NaN);
        final VectorImpl out = vec(NaN);

        agg.initTemporal(ctx, tvec);

        agg.aggregateTemporal(ctx, vec(0.7f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.71f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.72f), 1, tvec);

        agg.completeTemporal(ctx, 1, tvec);
        assertEquals(2.13f, tvec.get(0), 1e-8);

        agg.computeOutput(tvec, out);
        assertEquals(2.13f, out.get(0), 1e-8);
    }

    @Test
    public void testConfig_construction() {
        AggregatorSum.Config config = new AggregatorSum.Config();
        assertNull(config.targetName);
        assertNull(config.varName);

        config = new AggregatorSum.Config("Hanz", "Franz");
        assertEquals("Franz", config.targetName);
        assertEquals("Hanz", config.varName);
    }

    @Test
    public void testDescriptor_getName() {
        final AggregatorSum.Descriptor descriptor = new AggregatorSum.Descriptor();

        assertEquals("SUM", descriptor.getName());
    }

    @Test
    public void testDescriptor_createAggregator() {
        final AggregatorSum.Descriptor descriptor = new AggregatorSum.Descriptor();

        final Aggregator aggregator = descriptor.createAggregator(new MyVariableContext("Yo"), new AggregatorSum.Config("Jenni", "Penni"));
        assertNotNull(aggregator);

        final String[] spatialFeatureNames = aggregator.getSpatialFeatureNames();
        assertEquals(1, spatialFeatureNames.length);
        assertEquals("Jenni_sum", spatialFeatureNames[0]);

        final String[] temporalFeatureNames = aggregator.getTemporalFeatureNames();
        assertEquals(1, temporalFeatureNames.length);
        assertEquals("Jenni_sum", temporalFeatureNames[0]);

        final String[] outputFeatureNames = aggregator.getOutputFeatureNames();
        assertEquals(1, outputFeatureNames.length);
        assertEquals("Penni_sum", outputFeatureNames[0]);
    }

    @Test
    public void testDescriptor_createAggregator_targetName_empty() {
        final AggregatorSum.Descriptor descriptor = new AggregatorSum.Descriptor();

        final Aggregator aggregator = descriptor.createAggregator(new MyVariableContext("Yo"), new AggregatorSum.Config("Jenni", null));
        assertNotNull(aggregator);

        final String[] getOutputFeatureNames = aggregator.getOutputFeatureNames();
        assertEquals(1, getOutputFeatureNames.length);
        assertEquals("Jenni_sum", getOutputFeatureNames[0]);
    }

    @Test
    public void testDescriptor_createConfig() {
        final AggregatorSum.Descriptor descriptor = new AggregatorSum.Descriptor();

        final AggregatorConfig config = descriptor.createConfig();
        assertNotNull(config);
        assertTrue(config instanceof AggregatorSum.Config);
    }

    @Test
    public void testDescriptor_getSourceVarNames() {
        final AggregatorSum.Descriptor descriptor = new AggregatorSum.Descriptor();

        final String[] sourceVarNames = descriptor.getSourceVarNames(new AggregatorSum.Config("Henni", "Lenni"));
        assertEquals(1, sourceVarNames.length);
        assertEquals("Henni", sourceVarNames[0]);
    }

    @Test
    public void testDescriptor_getTargetVarNames() {
        final AggregatorSum.Descriptor descriptor = new AggregatorSum.Descriptor();

        final String[] targetVarNames = descriptor.getTargetVarNames(new AggregatorSum.Config("Henni", "Lenni"));
        assertEquals(1, targetVarNames.length);
        assertEquals("Lenni_sum", targetVarNames[0]);
    }
}