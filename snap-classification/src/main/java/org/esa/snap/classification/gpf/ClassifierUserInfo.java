package org.esa.snap.classification.gpf;

import org.esa.snap.core.datamodel.ProductData;

public class ClassifierUserInfo {
    private String classifierFilename;
    private String classifierType;
    public String className; // E.g., biomass or landcover classes
    private int numSamples;
    private double[] sortedClasses;
    private int numFeatures;
    private String[] trainingBands; // can be null
    private String[] trainingVectors; // can be null
    private String[] featureNames;
    public int datatype = ProductData.TYPE_INT16;
    public String unit = "discrete classes";

    // If quantization is not done, then classLevels is set to -1
    private double minClassValue;
    private double classValStepSize;
    private int classLevels;
    private double maxClassValue;

    public ClassifierUserInfo() {

    }

    public ClassifierUserInfo(final String classifierFilename, final String classifierType,
                              final String className, final int numSamples, final double[] sortedClasses,
                              final int numFeatures,
                              final String[] trainingBands,
                              final String[] trainingVectors,
                              final String[] featureNames,
                              final double minClassValue, final double classValStepSize, final int classLevels,
                              final double maxClassValue,
                              final int datatype,
                              final String unit) {
        this.classifierFilename = classifierFilename;
        this.classifierType = classifierType;
        this.className = className;
        this.numSamples = numSamples;
        this.sortedClasses = sortedClasses;
        this.numFeatures = numFeatures;
        this.trainingBands = trainingBands;
        this.trainingVectors = trainingVectors;
        this.featureNames = featureNames;
        this.minClassValue = minClassValue;
        this.classValStepSize = classValStepSize;
        this.classLevels = classLevels;
        this.maxClassValue = maxClassValue;
        this.datatype = datatype;
        this.unit = unit;
    }
}
