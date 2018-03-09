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

import static java.lang.Float.NaN;

class AggregatorAverageOutlierAware extends AbstractAggregator {

    private final String vectorName;

    AggregatorAverageOutlierAware(VariableContext varCtx, String varName, double stdDevFactor) {
        super(Descriptor.NAME, new String[0],
                createFeatureNames(varName, "mean", "sigma", "counts"),
                createFeatureNames(varName, "mean", "sigma", "counts"));

        vectorName = "values." + varName;
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Observation observationVector, WritableVector spatialVector) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void completeSpatial(BinContext ctx, int numSpatialObs, WritableVector spatialVector) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void initTemporal(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.0f);
        vector.set(1, 0.0f);
        vector.set(2, 0.0f);
        ctx.put(vectorName, new GrowableVector(256));   // @todo 3 tb/tb is this a good default? 2018-03-09
    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs, WritableVector temporalVector) {
        final GrowableVector measurementsVec = ctx.get(vectorName);
        measurementsVec.add(spatialVector.get(0));
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {
        final GrowableVector measurementsVec = ctx.get(vectorName);
        if (measurementsVec.size() == 0) {
            temporalVector.set(0, NaN);
            temporalVector.set(1, NaN);
            temporalVector.set(2, 0);
        } else if(measurementsVec.size() == 1) {
            final float value = measurementsVec.get(0);
            temporalVector.set(0, value);
            temporalVector.set(1, 0.f);
            temporalVector.set(2, 1);
        }

        double sum = 0.0;
        for (int i = 0; i < measurementsVec.size(); i++) {
            sum += measurementsVec.get(i);
        }
    }

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        outputVector.set(0, temporalVector.get(0));
        outputVector.set(1, temporalVector.get(1));
        outputVector.set(2, temporalVector.get(2));
    }

    public static class Descriptor implements AggregatorDescriptor {

        public static final String NAME = "AVG_OUTLIER";

        @Override
        public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public String getName() {
            throw new RuntimeException("not implemented");
        }

        @Override
        public AggregatorConfig createConfig() {
            throw new RuntimeException("not implemented");
        }
    }
}
