package org.esa.snap.binning;

import org.esa.snap.binning.aggregators.AggregatorAverage;
import org.esa.snap.binning.aggregators.AggregatorAverageOutlierAware;
import org.esa.snap.binning.aggregators.AggregatorMeanObs;
import org.esa.snap.binning.aggregators.AggregatorMinMax;
import org.esa.snap.binning.aggregators.AggregatorOnMaxSet;
import org.esa.snap.binning.aggregators.AggregatorPercentile;
import org.esa.snap.binning.aggregators.AggregatorSum;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AggregatorDescriptorRegistryTest {

    private MyVariableContext ctx = new MyVariableContext("x", "y", "z");

    @Test
    public void testDefaultAggregatorIsRegistered_Average() {
        AggregatorDescriptor descriptor = assertRegistered("AVG");
        Aggregator aggregator = descriptor.createAggregator(ctx, new AggregatorAverage.Config("x", "target", 0.2, false, false));
        assertNotNull(aggregator);
        assertEquals(AggregatorAverage.class, aggregator.getClass());
    }

    @Test
    public void testDefaultAggregatorIsRegistered_MinMax() {
        AggregatorDescriptor descriptor = assertRegistered("MIN_MAX");
        Aggregator aggregator = descriptor.createAggregator(ctx, new AggregatorMinMax.Config("x", "y"));
        assertNotNull(aggregator);
        assertEquals(AggregatorMinMax.class, aggregator.getClass());
    }

    @Test
    public void testDefaultAggregatorIsRegistered_Percentile() {
        AggregatorDescriptor descriptor = assertRegistered("PERCENTILE");
        Aggregator aggregator = descriptor.createAggregator(ctx, new AggregatorPercentile.Config("x", "y", 75));
        assertNotNull(aggregator);
        assertEquals(AggregatorPercentile.class, aggregator.getClass());
    }

    @Test
    public void testDefaultAggregatorIsRegistered_OnMaxSet() {
        AggregatorDescriptor descriptor = assertRegistered("ON_MAX_SET");
        AggregatorOnMaxSet.Config config = new AggregatorOnMaxSet.Config("target", "x", "y", "z");
        Aggregator aggregator = descriptor.createAggregator(ctx, config);
        assertNotNull(aggregator);
        assertEquals(AggregatorOnMaxSet.class, aggregator.getClass());
    }

    @Test
    public void testDefaultAggregatorIsRegistered_AverageOutlierAware() {
        AggregatorDescriptor descriptor = assertRegistered("AVG_OUTLIER");
        Aggregator aggregator = descriptor.createAggregator(ctx, new AggregatorAverageOutlierAware.Config("x", "target", 1.4));
        assertNotNull(aggregator);
        assertEquals(AggregatorAverageOutlierAware.class, aggregator.getClass());
    }

    @Test
    public void testDefaultAggregatorIsRegistered_Sum() {
        AggregatorDescriptor descriptor = assertRegistered("SUM");
        Aggregator aggregator = descriptor.createAggregator(ctx, new AggregatorSum.Config("x", "target"));
        assertNotNull(aggregator);
        assertEquals(AggregatorSum.class, aggregator.getClass());
    }

    @Test
    public void testDefaultAggregatorIsRegistered_MeanObs() {
        AggregatorDescriptor descriptor = assertRegistered("MEAN_OBS");
        Aggregator aggregator = descriptor.createAggregator(ctx, new AggregatorMeanObs.Config("x", "target"));
        assertNotNull(aggregator);
        assertEquals(AggregatorMeanObs.class, aggregator.getClass());
    }

    @Test
    public void testGetAllRegisteredAggregatorDescriptors() {
        TypedDescriptorsRegistry registry = TypedDescriptorsRegistry.getInstance();
        List<AggregatorDescriptor> aggregatorDescriptors = registry.getDescriptors(AggregatorDescriptor.class);
        assertEquals(7, aggregatorDescriptors.size());
    }

    private AggregatorDescriptor assertRegistered(String name) {
        TypedDescriptorsRegistry registry = TypedDescriptorsRegistry.getInstance();
        AggregatorDescriptor descriptor = registry.getDescriptor(AggregatorDescriptor.class, name);
        assertNotNull(descriptor);
        assertEquals(name, descriptor.getName());
        return descriptor;
    }
}
