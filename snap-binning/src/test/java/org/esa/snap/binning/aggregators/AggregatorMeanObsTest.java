/*
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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

public class AggregatorMeanObsTest {

    private BinContext ctx;

    @Before
    public void setUp() {
        ctx = createCtx();
    }

    @Test
    public void testRequiresGrowableSpatialData() {
        final AggregatorMeanObs agg = new AggregatorMeanObs(new MyVariableContext("var"), "var");

        assertFalse(agg.requiresGrowableSpatialData());
    }

    @Test
    public void testMetadata() {
        final AggregatorMeanObs agg = new AggregatorMeanObs(new MyVariableContext("Hans"), "Hans");

        assertEquals("MEAN_OBS", agg.getName());

        assertEquals(3, agg.getSpatialFeatureNames().length);
        assertEquals("Hans_mean", agg.getTemporalFeatureNames()[0]);
        assertEquals("Hans_sigma", agg.getTemporalFeatureNames()[1]);
        assertEquals("Hans_counts", agg.getTemporalFeatureNames()[2]);

        assertEquals(3, agg.getTemporalFeatureNames().length);
        assertEquals("Hans_mean", agg.getTemporalFeatureNames()[0]);
        assertEquals("Hans_sigma", agg.getTemporalFeatureNames()[1]);
        assertEquals("Hans_counts", agg.getTemporalFeatureNames()[2]);

        assertEquals(3, agg.getOutputFeatureNames().length);
        assertEquals("Hans_mean", agg.getOutputFeatureNames()[0]);
        assertEquals("Hans_sigma", agg.getOutputFeatureNames()[1]);
        assertEquals("Hans_counts", agg.getOutputFeatureNames()[2]);
    }

    @Test
    public void testAggregateSpatial_noMeasurements() {
        final AggregatorMeanObs agg = new AggregatorMeanObs(new MyVariableContext("Hans"), "Hans");

        VectorImpl vector = vec(NaN, NaN, NaN);
        agg.initSpatial(ctx, vector);

        assertEquals(0.f, vector.get(0), 1e-8);
        assertEquals(0.f, vector.get(1), 1e-8);
        assertEquals(0.f, vector.get(2), 1e-8);

        vector = vec(NaN, NaN, NaN);
        agg.completeSpatial(ctx, 0, vector);

        assertEquals(NaN, vector.get(0), 1e-8);
        assertEquals(NaN, vector.get(1), 1e-8);
        assertEquals(NaN, vector.get(2), 1e-8);
    }

    @Test
    public void testAggregateSpatial_oneMeasurement() {
        final AggregatorMeanObs agg = new AggregatorMeanObs(new MyVariableContext("Hans"), "Hans");

        VectorImpl vector = vec(NaN, NaN, NaN);

        agg.initSpatial(ctx, vector);

        agg.aggregateSpatial(ctx, obsNT(0.32f), vector);
        assertEquals(0.32f, vector.get(0), 1e-8);
        assertEquals(0.1024f, vector.get(1), 1e-8);
        assertEquals(1.f, vector.get(2), 1e-8);

        agg.completeSpatial(ctx, 1, vector);
        assertEquals(0.32f, vector.get(0), 1e-8);
        assertEquals(0.1024f, vector.get(1), 1e-8);
        assertEquals(1.f, vector.get(2), 1e-8);
    }

    @Test
    public void testAggregateSpatial_threeMeasurement_withInvalid() {
        final AggregatorMeanObs agg = new AggregatorMeanObs(new MyVariableContext("Hans"), "Hans");

        VectorImpl vector = vec(NaN, NaN, NaN);

        agg.initSpatial(ctx, vector);

        agg.aggregateSpatial(ctx, obsNT(NaN), vector);
        agg.aggregateSpatial(ctx, obsNT(2.f), vector);
        agg.aggregateSpatial(ctx, obsNT(3.f), vector);
        assertEquals(5.f, vector.get(0), 1e-8);
        assertEquals(13.f, vector.get(1), 1e-8);
        assertEquals(2.f, vector.get(2), 1e-8);

        agg.completeSpatial(ctx, 1, vector);
        assertEquals(5.f, vector.get(0), 1e-8);
        assertEquals(13.f, vector.get(1), 1e-8);
        assertEquals(2.f, vector.get(2), 1e-8);
    }

    @Test
    public void testAggregateTemporal_noMeasurements() {
        final AggregatorMeanObs agg = new AggregatorMeanObs(new MyVariableContext("Hans"), "Hans");

        final VectorImpl tvec = vec(NaN, NaN, NaN);
        final VectorImpl out = vec(NaN, NaN, NaN);

        agg.initTemporal(ctx, tvec);
        assertEquals(0.0f, tvec.get(0), 1e-8);
        assertEquals(0.0f, tvec.get(1), 1e-8);
        assertEquals(0.0f, tvec.get(2), 1e-8);

        agg.completeTemporal(ctx, 0, tvec);
        assertEquals(0.f, tvec.get(0), 1e-8);
        assertEquals(0.f, tvec.get(1), 1e-8);
        assertEquals(0, tvec.get(2), 1e-8);

        agg.computeOutput(tvec, out);
        assertEquals(NaN, out.get(0), 1e-8);
        assertEquals(NaN, out.get(1), 1e-8);
        assertEquals(0, out.get(2), 1e-8);
    }

    @Test
    public void testAggregateTemporal_oneMeasurement() {
        final AggregatorMeanObs agg = new AggregatorMeanObs(new MyVariableContext("Hans"), "Hans");

        final VectorImpl tvec = vec(NaN, NaN, NaN);
        final VectorImpl out = vec(NaN, NaN, NaN);

        agg.initTemporal(ctx, tvec);
        assertEquals(0.0f, tvec.get(0), 1e-8);
        assertEquals(0.0f, tvec.get(1), 1e-8);
        assertEquals(0.0f, tvec.get(2), 1e-8);

        agg.aggregateTemporal(ctx, vec(2.f, 4.f, 1.f), 1, tvec);
        assertEquals(2.0f, tvec.get(0), 1e-8);
        assertEquals(4.0f, tvec.get(1), 1e-8);
        assertEquals(1.0f, tvec.get(2), 1e-8);

        agg.completeTemporal(ctx, 0, tvec);
        assertEquals(2.0f, tvec.get(0), 1e-8);
        assertEquals(4.0f, tvec.get(1), 1e-8);
        assertEquals(1.0f, tvec.get(2), 1e-8);

        agg.computeOutput(tvec, out);
        assertEquals(2.f, out.get(0), 1e-8);
        assertEquals(0.f, out.get(1), 1e-8);
        assertEquals(1.f, out.get(2), 1e-8);
    }

    @Test
    public void testAggregateTemporal_threeMeasurements() {
        final AggregatorMeanObs agg = new AggregatorMeanObs(new MyVariableContext("Hans"), "Hans");

        final VectorImpl tvec = vec(NaN, NaN, NaN);
        final VectorImpl out = vec(NaN, NaN, NaN);

        agg.initTemporal(ctx, tvec);
        assertEquals(0.0f, tvec.get(0), 1e-8);
        assertEquals(0.0f, tvec.get(1), 1e-8);
        assertEquals(0.0f, tvec.get(2), 1e-8);

        agg.aggregateTemporal(ctx, vec(2.f, 4.f, 1.f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(3.f, 5.f, 2.f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(4.f, 6.f, 3.f), 1, tvec);
        assertEquals(9.0f, tvec.get(0), 1e-8);
        assertEquals(15.0f, tvec.get(1), 1e-8);
        assertEquals(6.0f, tvec.get(2), 1e-8);

        agg.completeTemporal(ctx, 0, tvec);
        assertEquals(9.0f, tvec.get(0), 1e-8);
        assertEquals(15.0f, tvec.get(1), 1e-8);
        assertEquals(6.0f, tvec.get(2), 1e-8);

        agg.computeOutput(tvec, out);
        assertEquals(1.5f, out.get(0), 1e-8);
        assertEquals(0.5f, out.get(1), 1e-8);
        assertEquals(6.f, out.get(2), 1e-8);
    }

    @Test
    public void testAggregateTemporal_threeMeasurements_withInvalid() {
        final AggregatorMeanObs agg = new AggregatorMeanObs(new MyVariableContext("Hans"), "Hans");

        final VectorImpl tvec = vec(NaN, NaN, NaN);
        final VectorImpl out = vec(NaN, NaN, NaN);

        agg.initTemporal(ctx, tvec);
        assertEquals(0.0f, tvec.get(0), 1e-8);
        assertEquals(0.0f, tvec.get(1), 1e-8);
        assertEquals(0.0f, tvec.get(2), 1e-8);

        agg.aggregateTemporal(ctx, vec(2.f, 4.f, 1.f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(3.f, 9.f, 1.f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(NaN, NaN, 0.f), 1, tvec);
        assertEquals(5.0f, tvec.get(0), 1e-8);
        assertEquals(13.0f, tvec.get(1), 1e-8);
        assertEquals(2.0f, tvec.get(2), 1e-8);

        agg.completeTemporal(ctx, 0, tvec);
        assertEquals(5.0f, tvec.get(0), 1e-8);
        assertEquals(13.0f, tvec.get(1), 1e-8);
        assertEquals(2.0f, tvec.get(2), 1e-8);

        agg.computeOutput(tvec, out);
        assertEquals(2.5f, out.get(0), 1e-8);
        assertEquals(0.5f, out.get(1), 1e-8);
        assertEquals(2.f, out.get(2), 1e-8);
    }

    @Test
    public void testDescriptor_createAggregator_standard() {
        final AggregatorMeanObs.Descriptor descriptor = new AggregatorMeanObs.Descriptor();

        final Aggregator aggregator = descriptor.createAggregator(new MyVariableContext(), new AggregatorMeanObs.Config("variable", null));
        assertNotNull(aggregator);

        assertEquals("variable_mean", aggregator.getTemporalFeatureNames()[0]);
        assertEquals("variable_mean", aggregator.getOutputFeatureNames()[0]);
    }

    @Test
    public void testDescriptor_createAggregator_withOutputNamed() {
        final AggregatorMeanObs.Descriptor descriptor = new AggregatorMeanObs.Descriptor();

        final Aggregator aggregator = descriptor.createAggregator(new MyVariableContext(), new AggregatorMeanObs.Config("variable", "Lena"));
        assertNotNull(aggregator);

        assertEquals("Lena_mean", aggregator.getTemporalFeatureNames()[0]);
        assertEquals("Lena_mean", aggregator.getOutputFeatureNames()[0]);
    }

    @Test
    public void testDescriptor_createConfig() {
        final AggregatorMeanObs.Descriptor descriptor = new AggregatorMeanObs.Descriptor();

        final AggregatorConfig aggregatorConfig = descriptor.createConfig();
        assertNotNull(aggregatorConfig);

        final AggregatorMeanObs.Config config = (AggregatorMeanObs.Config) aggregatorConfig;
        assertNull(config.varName);
        assertNull(config.targetName);
        assertEquals("MEAN_OBS", config.getName());
    }

    @Test
    public void testDescriptor_getSourceVarNames() {
        final AggregatorMeanObs.Descriptor descriptor = new AggregatorMeanObs.Descriptor();

        final String[] sourceVarNames = descriptor.getSourceVarNames(new AggregatorMeanObs.Config("source", "target"));
        assertEquals(1, sourceVarNames.length);
        assertEquals("source", sourceVarNames[0]);
    }

    @Test
    public void testDescriptor_getTargetVarNames_standard() {
        final AggregatorMeanObs.Descriptor descriptor = new AggregatorMeanObs.Descriptor();

        final String[] sourceVarNames = descriptor.getTargetVarNames(new AggregatorMeanObs.Config("source", null));
        assertEquals(3, sourceVarNames.length);
        assertEquals("source_mean", sourceVarNames[0]);
        assertEquals("source_sigma", sourceVarNames[1]);
        assertEquals("source_counts", sourceVarNames[2]);
    }

    @Test
    public void testDescriptor_getTargetVarNames_withOutputName() {
        final AggregatorMeanObs.Descriptor descriptor = new AggregatorMeanObs.Descriptor();

        final String[] sourceVarNames = descriptor.getTargetVarNames(new AggregatorMeanObs.Config("source", "Klaus"));
        assertEquals(3, sourceVarNames.length);
        assertEquals("Klaus_mean", sourceVarNames[0]);
        assertEquals("Klaus_sigma", sourceVarNames[1]);
        assertEquals("Klaus_counts", sourceVarNames[2]);
    }

    @Test
    public void testDescriptor_getName() {
        final AggregatorMeanObs.Descriptor descriptor = new AggregatorMeanObs.Descriptor();

        assertEquals("MEAN_OBS", descriptor.getName());
    }
}
