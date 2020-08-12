package org.esa.snap.binning.aggregators;

import org.esa.snap.binning.AbstractAggregator;
import org.esa.snap.binning.Aggregator;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.AggregatorDescriptor;
import org.esa.snap.binning.BinContext;
import org.esa.snap.binning.Observation;
import org.esa.snap.binning.VariableContext;
import org.esa.snap.binning.Vector;
import org.esa.snap.binning.WritableVector;
import org.esa.snap.binning.support.GrowableVector;

public class AggregatorSum extends AbstractAggregator {

    private final int varIndex;

    public AggregatorSum(VariableContext varCtx, String varName) {
        super(Descriptor.NAME,
              createFeatureNames(varName, "sum", "counts"),
              createFeatureNames(varName, "sum", "counts"),
              createFeatureNames(varName, "sum", "counts"));

        varIndex = varCtx.getVariableIndex(varName);
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.f);
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Observation observationVector, WritableVector spatialVector) {
        final float value = observationVector.get(varIndex);
        if (!Float.isNaN(value)) {
            float sum = spatialVector.get(0);
            sum += value;
            spatialVector.set(0, sum);
        }
    }

    @Override
    public void completeSpatial(BinContext ctx, int numSpatialObs, WritableVector spatialVector) {
        // nothing to do here tb 2020-08-11
    }

    @Override
    public void initTemporal(BinContext ctx, WritableVector vector) {

    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs, WritableVector temporalVector) {

    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {

    }

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {

    }

    public static class Descriptor implements AggregatorDescriptor {

        public static final String NAME = "SUM";

        @Override
        public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
            return null;
        }

        @Override
        public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
            return new String[0];
        }

        @Override
        public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
            return new String[0];
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public AggregatorConfig createConfig() {
            return null;
        }
    }
}
