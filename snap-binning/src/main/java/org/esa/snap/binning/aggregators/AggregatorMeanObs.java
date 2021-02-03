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
import org.esa.snap.core.util.StringUtils;

import static java.lang.Float.NaN;

public class AggregatorMeanObs extends AbstractAggregator {

    private final int varIndex;

    public AggregatorMeanObs(VariableContext varCtx, String varName) {
        super("MEAN_OBS",
              createFeatureNames(varName, "mean", "sigma", "counts"),
              createFeatureNames(varName, "mean", "sigma", "counts"),
              createFeatureNames(varName, "mean", "sigma", "counts"));

        this.varIndex = varCtx.getVariableIndex(varName);
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.f);
        vector.set(1, 0.f);
        vector.set(2, 0.f);
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Observation observationVector, WritableVector spatialVector) {
        final float value = observationVector.get(varIndex);
        if (!Float.isNaN(value)) {
            spatialVector.set(0, spatialVector.get(0) + value);
            spatialVector.set(1, spatialVector.get(1) + value * value);
            spatialVector.set(2, spatialVector.get(2) + 1);
        }
    }

    @Override
    public void completeSpatial(BinContext ctx, int numSpatialObs, WritableVector spatialVector) {
        // nothing to do here tb 2021-02-02
    }

    @Override
    public void initTemporal(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.f);
        vector.set(1, 0.f);
        vector.set(2, 0.f);
    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs, WritableVector temporalVector) {
        final float sum = spatialVector.get(0);

        if (!Float.isNaN(sum)) {
            final float sumSqr = spatialVector.get(1);
            final float counts = spatialVector.get(2);

            temporalVector.set(0, temporalVector.get(0) + sum);
            temporalVector.set(1, temporalVector.get(1) + sumSqr);
            temporalVector.set(2, temporalVector.get(2) + counts);
        }
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {
        // nothing to do here tb 2021-02-02
    }

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        final double sum = temporalVector.get(0);
        final double sumSqr = temporalVector.get(1);
        final double counts = temporalVector.get(2);

        if (counts > 0) {
            final double mean = sum / counts;
            final double sigmaSqr = sumSqr / counts - mean * mean;
            final double sigma = sigmaSqr > 0.0 ? Math.sqrt(sigmaSqr) : 0.0;

            outputVector.set(0, (float) mean);
            outputVector.set(1, (float) sigma);
            outputVector.set(2, (float) counts);
        } else {
            outputVector.set(0, NaN);
            outputVector.set(1, NaN);
            outputVector.set(2, 0);
        }
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
            super(Descriptor.NAME);
            this.varName = varName;
            this.targetName = targetName;
        }
    }

    public static class Descriptor implements AggregatorDescriptor {

        public static final String NAME = "MEAN_OBS";

        @Override
        public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
            final AggregatorMeanObs.Config config = (AggregatorMeanObs.Config) aggregatorConfig;
            final String targetName = StringUtils.isNotNullAndNotEmpty(config.targetName) ? config.targetName : config.varName;
            return new AggregatorMeanObs(varCtx, targetName);
        }

        @Override
        public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
            final Config config = (Config) aggregatorConfig;

            return new String[]{config.varName};
        }

        @Override
        public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
            final Config config = (Config) aggregatorConfig;
            if (StringUtils.isNullOrEmpty(config.targetName)) {
                return createFeatureNames(config.varName, "mean", "sigma", "counts");
            } else {
                return createFeatureNames(config.targetName, "mean", "sigma", "counts");
            }
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public AggregatorConfig createConfig() {
            return new Config();
        }
    }
}
