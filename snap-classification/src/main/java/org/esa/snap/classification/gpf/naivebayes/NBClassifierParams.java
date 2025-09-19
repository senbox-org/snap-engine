package org.esa.snap.classification.gpf.naivebayes;

import org.esa.snap.core.datamodel.Product;

public class NBClassifierParams {
    private final String classifierType;
    private final String productSuffix;
    private final Product[] sourceProducts;

    private final int numTrainSamples;
    private double minClassValue;
    private double classValStepSize;
    private int classLevels;
    private String savedClassifierName;
    private boolean doLoadClassifier;

    private boolean doClassValQuantization;
    private final boolean trainOnRaster;
    private final String trainingBandName;
    private String[] trainingVectors;
    private String labelSource;         // vector node name or attribute name
    private String[] featureBandNames;

    public NBClassifierParams(final String classifierType, final String productSuffix, final Product[] sourceProducts,
                            final int numTrainSamples, final double minClassValue,
                            final double classValStepSize, final int classLevels,
                            final String savedClassifierName, final boolean doLoadClassifier, final boolean doClassValQuantization,
                            final boolean trainOnRaster,
                            final String trainingBandName,
                            final String[] trainingVectors,
                            final String[] featureBandNames,
                            final String labelSource) {
        this.classifierType = classifierType;
        this.productSuffix = productSuffix;
        this.sourceProducts = sourceProducts;
        this.numTrainSamples = numTrainSamples;
        this.minClassValue = minClassValue;
        this.classValStepSize = classValStepSize;
        this.classLevels = classLevels;
        this.savedClassifierName = savedClassifierName;
        this.doLoadClassifier = doLoadClassifier;
        this.doClassValQuantization = doClassValQuantization;
        this.trainOnRaster = trainOnRaster;
        this.trainingBandName = trainingBandName;
        this.trainingVectors = trainingVectors;
        this.featureBandNames = featureBandNames;
        this.labelSource = labelSource;
    }

    public String getClassifierType(){
        return this.classifierType;
    }

    public String getSavedClassifierName(){
        return this.savedClassifierName;
    }

    public String getProductSuffix(){
        return this.productSuffix;
    }

    /**
     * Get the training product
     * The training product is the 1st product of the sourceProducts list
     * @return Product
     */
    public Product getTrainingProduct(){
        return (this.sourceProducts == null || this.sourceProducts.length < 1)? null : sourceProducts[0];
    }

    public Product[] getSourceProducts(){
        return sourceProducts;
    }
    public Product getSourceProduct(int productIdx) {
        if (productIdx < 0 ||  sourceProducts == null || sourceProducts.length <= productIdx)
        {
            return null;
        } else {
            return sourceProducts[productIdx];
        }
    }

    public int getNumTrainSamples() {
        return numTrainSamples;
    }

    public double getMinClassValue() {
        return minClassValue;
    }

    public void setMinClassValue(double value){
        minClassValue = value;
    }

    public double getClassValStepSize() {
        return classValStepSize;
    }

    public void setClassValStepSize(double value){
        classValStepSize = value;
    }

    public int getClassLevels() {
        return classLevels;
    }

    public void setClassLevels (int value){
        classLevels = value;
    }

    public boolean isDoLoadClassifier() {
        return doLoadClassifier;
    }

    public void setDoClassValQuantization(boolean value){
        doClassValQuantization = value;
    }

    public boolean isDoClassValQuantization() {
        return doClassValQuantization;
    }

    public boolean isTrainOnRaster() {
        return trainOnRaster;
    }

    public String getTrainingBandName() {
        return trainingBandName;
    }

    public String[] getTrainingVectors() {
        return trainingVectors;
    }

    public void setTrainingVectors(String[] trainingVectors) {
        this.trainingVectors = trainingVectors;
    }

    public String getTrainingVector(int vectorIdx) {
        if (vectorIdx < 0 ||  trainingVectors == null || trainingVectors.length <= vectorIdx)
        {
            return null;
        } else {
            return trainingVectors[vectorIdx];
        }
    }

    public String[] getFeatureBandNames() {
        return featureBandNames;
    }

    public void setFeatureBandNames(String[] featureBandNames) {
        this.featureBandNames = featureBandNames;
    }

    public String getLabelSource() {
        return labelSource;
    }
}
