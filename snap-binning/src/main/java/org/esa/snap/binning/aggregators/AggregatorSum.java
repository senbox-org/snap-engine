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
import org.esa.snap.core.gpf.annotations.Parameter;

public class AggregatorSum extends AbstractAggregator {

    private final int varIndex;

    AggregatorSum(VariableContext varCtx, String varName, String targetName) {
        super(Descriptor.NAME,
              createFeatureNames(varName, "sum", "counts"),
              createFeatureNames(varName, "sum", "counts"),
              createFeatureNames(targetName, "sum", "counts"));

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
        // nothing to do here tb 2020-08-12
    }

    @Override
    public void initTemporal(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.0f);
    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs, WritableVector temporalVector) {
        float accum = temporalVector.get(0);
        accum += spatialVector.get(0);
        temporalVector.set(0, accum);
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {
        // nothing to do here tb 2020-08-13
    }

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        outputVector.set(0, temporalVector.get(0));
    }

    public static class Config extends AggregatorConfig {
        @Parameter(label = "Source band name", notEmpty = true, notNull = true, description = "The source band used for aggregation.")
        String varName;

        @Parameter(label = "Target band name prefix (optional)", description = "The name prefix for the resulting bands. If empty, the source band name is used.")
        String targetName;

        public Config() {
            this(null, null);
        }

        public Config(String varName, String targetName) {
            super(AggregatorAverageOutlierAware.Descriptor.NAME);
            this.varName = varName;
            this.targetName = targetName;
        }
    }

    public static class Descriptor implements AggregatorDescriptor {

        public static final String NAME = "SUM";

        @Override
        public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
            AggregatorSum.Config config = (AggregatorSum.Config) aggregatorConfig;
            return new AggregatorSum(varCtx, config.varName, config.targetName);
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
            return NAME;
        }

        @Override
        public AggregatorConfig createConfig() {
            return null;
        }
    }
}
