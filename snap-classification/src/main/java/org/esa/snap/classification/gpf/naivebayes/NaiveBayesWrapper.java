package org.esa.snap.classification.gpf.naivebayes;

import net.sf.javaml.classification.AbstractClassifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Set;

public class NaiveBayesWrapper extends AbstractClassifier {
    private final NaiveBayes nbClassifier;
    private final Instances wekaHeader;

    public NaiveBayesWrapper(String[] featureNames, Set<String> classLabels, Integer capacity){
        nbClassifier = new NaiveBayes();
        wekaHeader = buildWekaHeader (featureNames, classLabels, capacity);
    }

    private Instances buildWekaHeader(String[] featureNames, Set<String> classLabels, Integer capacity) {
        ArrayList<Attribute> attributes = new ArrayList<>();
        for (String name : featureNames) {
            attributes.add(new Attribute(name));
        }
        attributes.add(new Attribute("class", new ArrayList<>(classLabels)));
        Instances header = new Instances("TrainingData", attributes, capacity);
        header.setClassIndex(header.numAttributes() - 1);
        return header;
    }

    public void buildClassifier(Instances trainingSet) throws Exception {
        nbClassifier.buildClassifier(trainingSet);
    }

    public Object classifyInstance(Instance instance) throws Exception {
        return nbClassifier.classifyInstance(instance);
    }

    public double[] distributionForInstance(Instance instance) throws Exception {
        return nbClassifier.distributionForInstance(instance);
    }

    public Instances getWekaHeader(){
        return wekaHeader;
    }

    public String[] getClassLabels(){
        Attribute classAttr = wekaHeader.classAttribute();
        String[] sortedClassLabels = new String[classAttr.numValues()];
        for (int idx = 0; idx < classAttr.numValues(); idx++){
            sortedClassLabels[idx] = classAttr.value(idx);
        }
        return sortedClassLabels;
    }

    public String[] getFeatureAttributes(){
        String[] arrFeatures = new String[wekaHeader.numAttributes()];
        for (int idx = 0; idx < wekaHeader.classIndex(); idx++){
            arrFeatures[idx] = wekaHeader.attribute(idx).name();
        }
        return arrFeatures;
    }
}
