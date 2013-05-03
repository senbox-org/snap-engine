package org.esa.beam.binning.cellprocessor;

import com.bc.ceres.binding.PropertySet;
import org.esa.beam.binning.*;
import org.esa.beam.framework.gpf.annotations.Parameter;

/**
 * A cell processor that select a number of features from the available ones.
 */
public class FeatureSelection extends CellProcessor {

    private final int[] varIndexes;

    public FeatureSelection(VariableContext varCtx, String... outputFeatureNames) {
        super(outputFeatureNames);
        varIndexes = new int[outputFeatureNames.length];
        for (int i = 0; i < outputFeatureNames.length; i++) {
            String name = outputFeatureNames[i];
            int variableIndex = varCtx.getVariableIndex(name);
            if (variableIndex == -1) {
                throw new IllegalArgumentException("unknown feature name: " + name);
            }
            varIndexes[i] = variableIndex;
        }
    }

    @Override
    public void compute(Vector inputVector, WritableVector outputVector) {
        for (int i = 0; i < varIndexes.length; i++) {
            outputVector.set(i, inputVector.get(varIndexes[i]));
        }
    }

    public static class Config extends CellProcessorConfig {
        @Parameter(notEmpty = true, notNull = true)
        private String[] varNames;

        public Config(String...varNames) {
            super(Descriptor.NAME);
            this.varNames = varNames;
        }

    }

    public static class Descriptor implements CellProcessorDescriptor {

        public static final String NAME = "Selection";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public CellProcessor createCellProcessor(VariableContext varCtx, CellProcessorConfig cellProcessorConfig) {
            PropertySet propertySet = cellProcessorConfig.asPropertySet();
            return new FeatureSelection(varCtx, (String[]) propertySet.getValue("varNames"));
        }

        @Override
        public CellProcessorConfig createConfig() {
            return new Config();
        }
    }
}
