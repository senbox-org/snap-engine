/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning;

import org.esa.snap.binning.aggregators.AggregatorAverage;
import org.esa.snap.binning.aggregators.AggregatorAverageML;
import org.esa.snap.binning.aggregators.AggregatorMinMax;
import org.esa.snap.binning.aggregators.AggregatorOnMaxSet;
import org.esa.snap.binning.cellprocessor.FeatureSelection;
import org.esa.snap.binning.support.ObservationImpl;
import org.esa.snap.binning.support.VectorImpl;
import org.junit.Test;

import static org.junit.Assert.*;

public class BinManagerTest {

    @Test
    public void testBinCreation() {
        VariableContext variableContext = createVariableContext();
        BinManager binManager = new BinManager(variableContext,
                new AggregatorAverage(variableContext, "c", 0.0),
                new AggregatorAverageML(variableContext, "b", 0.5),
                new AggregatorMinMax(variableContext, "a", "a"),
                new AggregatorOnMaxSet(variableContext, "c", "c", "a", "b"));

        assertEquals(4, binManager.getAggregatorCount());

        SpatialBin sbin = binManager.createSpatialBin(42);
        assertEquals(42, sbin.getIndex());
        assertEquals(2 + 2 + 2 + 4, sbin.getFeatureValues().length);

        TemporalBin tbin = binManager.createTemporalBin(42);
        assertEquals(42, tbin.getIndex());
        assertEquals(3 + 3 + 2 + 4, tbin.getFeatureValues().length);
    }

    @Test
    public void testNameUnifying() {
        BinManager.NameUnifier nameUnifier = new BinManager.NameUnifier();
        assertEquals("expression_p90", nameUnifier.unifyName("expression_p90"));
        assertEquals("expression_p90_1", nameUnifier.unifyName("expression_p90"));
        assertEquals("expression_p90_2", nameUnifier.unifyName("expression_p90"));
        assertEquals("expression_p50", nameUnifier.unifyName("expression_p50"));
        assertEquals("expression_p50_1", nameUnifier.unifyName("expression_p50"));
    }

    @Test
    public void testGetResultFeatureNames_noPostProcessor() {
        final VariableContext variableContext = createVariableContext();
        final BinManager binManager = new BinManager(variableContext,
                new AggregatorAverage(variableContext, "d", 0.0));

        final String[] resultFeatureNames = binManager.getResultFeatureNames();
        assertEquals(2, resultFeatureNames.length);
        assertEquals("d_mean", resultFeatureNames[0]);
        assertEquals("d_sigma", resultFeatureNames[1]);
    }

    @Test
    public void testGetResultFeatureCount_noPostProcessor() {
        final VariableContext variableContext = createVariableContext();
        final BinManager binManager = new BinManager(variableContext,
                new AggregatorAverageML(variableContext, "e", 0.5));

        final int featureCount = binManager.getResultFeatureCount();
        assertEquals(4, featureCount);
    }

    @Test
    public void testGetResultFeatureNames_withPostProcessor() {
        final VariableContext variableContext = createVariableContext();
        final FeatureSelection.Config ppSelection = new FeatureSelection.Config("out_min");

        final BinManager binManager = new BinManager(variableContext,
                ppSelection,
                new AggregatorMinMax(variableContext, "e", "out"));

        final String[] resultFeatureNames = binManager.getResultFeatureNames();
        assertEquals(1, resultFeatureNames.length);
        assertEquals("out_min", resultFeatureNames[0]);
    }

    @Test
    public void testGetResultFeatureCount_withPostProcessor_targetName() {
        final VariableContext variableContext = createVariableContext();
        final FeatureSelection.Config ppSelection = new FeatureSelection.Config("out_max");

        final BinManager binManager = new BinManager(variableContext,
                ppSelection,
                new AggregatorMinMax(variableContext, "f", "out"));

        final int featureCount = binManager.getResultFeatureCount();
        assertEquals(1, featureCount);
    }

    @Test
    public void testGetResultFeatureCount_withPostProcessor() {
        final VariableContext variableContext = createVariableContext();
        final FeatureSelection.Config ppSelection = new FeatureSelection.Config("f_max");

        final BinManager binManager = new BinManager(variableContext,
                ppSelection,
                new AggregatorMinMax(variableContext, "f", "f"));

        final int featureCount = binManager.getResultFeatureCount();
        assertEquals(1, featureCount);
    }

    @Test
    public void testAggregationWithThreeAVGs(){
        final VariableContext ctx = new MyVariableContext("tcwv", "tcwv_uncertainty");
        final AggregatorAverage agg_tcwv = new AggregatorAverage(ctx, "tcwv", null, 1.0, true, false);
        final AggregatorAverage agg_tcwv_unc = new AggregatorAverage(ctx, "tcwv_uncertainty", "tcwv_unc", 1.0, false, false);
        final AggregatorAverage agg_tcwv_sums = new AggregatorAverage(ctx, "tcwv_uncertainty", "tcwv_unc_sum", 1.0, false, true);

        final BinManager binManager = new BinManager(ctx,
                agg_tcwv,
                agg_tcwv_unc,
                agg_tcwv_sums);

        final int binIndex = 20;
        final SpatialBin spatialBin = binManager.createSpatialBin(binIndex);
        assertEquals(7, spatialBin.getFeatureValues().length);

        // ------------------------------------------------------------------------------------------------------------tcwv tcwv_unc
        ObservationImpl observation = new ObservationImpl(23.8, 19.09, 78687443, 35.4f, 1.76f);
        binManager.aggregateSpatialBin(observation, spatialBin);

        observation = new ObservationImpl(23.8, 19.09, 78687443, 35.4f, 1.76f);
        binManager.aggregateSpatialBin(observation, spatialBin);

        observation = new ObservationImpl(23.8, 19.09, 78687443, Float.NaN, Float.NaN);
        binManager.aggregateSpatialBin(observation, spatialBin);

        observation = new ObservationImpl(23.8, 19.09, 78687443, Float.NaN, Float.NaN);
        binManager.aggregateSpatialBin(observation, spatialBin);

        observation = new ObservationImpl(23.8, 19.09, 78687443, 35.4f, 1.76f);
        binManager.aggregateSpatialBin(observation, spatialBin);

        binManager.completeSpatialBin(spatialBin);
        final float[] featureValues = spatialBin.getFeatureValues();
        // agg tvwv
        assertEquals(35.400001525878906, featureValues[0], 1e-8);    // sum_tcwv/count
        assertEquals(1253.16015625, featureValues[1], 1e-8);    // sum_tcwv²/count
        assertEquals(3, featureValues[2], 1e-8);    // tcwv counts
        // agg tcwv_unc
        assertEquals(1.7599998712539673, featureValues[3], 1e-8);    // tcwv_unc/counts
        assertEquals(3.097599983215332, featureValues[4], 1e-8);    // tcwv_unc²/counts
        // agg tcwv_unc - sum+sums_quares
        assertEquals(1.7599998712539673, featureValues[5], 1e-8);    // tcwv_unc/counts
        assertEquals(3.097599983215332, featureValues[6], 1e-8);    // tcwv_unc²/counts

        final TemporalBin temporalBin = binManager.createTemporalBin(binIndex);
        binManager.aggregateTemporalBin(spatialBin, temporalBin);

        binManager.completeTemporalBin(temporalBin);

        final VectorImpl vector = new VectorImpl(new float[8]);
        binManager.computeOutput(temporalBin, vector);
        assertEquals(35.400001525878906, vector.get(0), 1e-8);
        assertEquals(0.012500000186264515, vector.get(1), 1e-8);
        assertEquals(3.0, vector.get(2), 1e-8);
        assertEquals(1.7599998712539673, vector.get(3), 1e-8);
        assertEquals(7.213353528641164E-4, vector.get(4), 1e-8);
        assertEquals(8.799999237060547, vector.get(5), 1e-8);
        assertEquals(15.48799991607666, vector.get(6), 1e-8);
        assertEquals(5.0, vector.get(7), 1e-8);
    }

    private VariableContext createVariableContext() {
        return new MyVariableContext("a", "b", "c");
    }
}
