package org.esa.beam.binning.postprocessor;

import com.bc.ceres.binding.PropertySet;
import org.esa.beam.binning.*;
import org.esa.beam.binning.aggregators.AggregatorAverage;
import org.esa.beam.framework.gpf.annotations.Parameter;

/**
 * Created with IntelliJ IDEA.
 * User: marco
 * Date: 17.04.13
 * Time: 23:46
 * To change this template use File | Settings | File Templates.
 */
public class PPSelection extends PostProcessor{

    private final int[] varIndexes;

    public PPSelection(VariableContext varCtx, String...outputFeatureNames) {
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
    public void compute(Vector outputVector, WritableVector postVector) {
        for (int i = 0; i < varIndexes.length; i++) {
            postVector.set(i, outputVector.get(varIndexes[i]));
        }
    }

    public static class Config extends PostProcessorConfig {
        @Parameter(notEmpty = true, notNull = true)
        private String[] varNames;

        public Config(String...varNames) {
            super(Descriptor.NAME);
            this.varNames = varNames;
        }

    }

    public static class Descriptor implements PostProcessorDescriptor {

        public static final String NAME = "Selection";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public PostProcessor createPostProcessor(VariableContext varCtx, PostProcessorConfig postProcessorConfig) {
            PropertySet propertySet = postProcessorConfig.asPropertySet();
            return new PPSelection(varCtx, (String[]) propertySet.getValue("varNames"));
        }

        @Override
        public PostProcessorConfig createPostProcessorConfig() {
            return new Config();
        }
    }
}
