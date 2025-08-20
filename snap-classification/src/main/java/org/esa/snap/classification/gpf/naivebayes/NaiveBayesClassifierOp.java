package org.esa.snap.classification.gpf.naivebayes;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.*;

import java.awt.*;
import java.util.*;

@OperatorMetadata(alias = "Naive-Bayes-Classifier",
        category = "Raster/Classification/Supervised Classification",
        authors = "CSG RO",
        copyright = "Copyright (C) 2025 by CS Group Romania.",
        description = "Naive Bayes based classifier (weka)")
public class NaiveBayesClassifierOp extends Operator {

    public final static String CLASSIFIER_TYPE = "NaiveBayes";
    private final static String PRODUCT_SUFFIX = "_NB";

    @SourceProducts
    private Product[] sourceProducts;

    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "false", description = "Choose to save or load classifier")
    private Boolean doLoadClassifier = false;

    @Parameter(description = "The saved classifier name", label = "Classifier name")
    private String savedClassifierName = null;

    @Parameter(defaultValue = "false", description = "Train on raster or vector data")
    private Boolean trainOnRaster;

    // Size of training dataset
    @Parameter(description = "The number of training samples", interval = "(1,*]", defaultValue = "50000",
            label = "Number of training samples")
    private int numTrainSamples = 50000;

    @Parameter(label = "Vector Classes", description = "Vectors to train on", itemAlias = "trainingVectors")
    private String[] trainingVectors;

    @Parameter(label = "Raster Classes", description = "Raster band to train on", itemAlias = "trainingBands")
    private String[] trainingBands;

    @Parameter(defaultValue = "true", description = "Quantization for raster training")
    private Boolean doClassValQuantization = true;

    @Parameter(defaultValue = "0.0", description = "Quantization min class value for raster training")
    private Double minClassValue = 0.0;

    @Parameter(defaultValue = "5.0", description = "Quantization step size for raster training")
    private Double classValStepSize = 5.0;

    @Parameter(defaultValue = "101", description = "Quantization class levels for raster training")
    private int classLevels = 101;

    @Parameter(label = "Feature Band Names", description = "Names of bands used as features", itemAlias = "featureBands")
    private String[] featureBands;

    private NaiveBayesClassifier classifier;


    @Override
    public void initialize() throws OperatorException {
        String trainingBandName = (trainingBands == null || trainingBands.length < 1) ? null : trainingBands[0];
        classifier = new NaiveBayesClassifier(
                new NBClassifierParams(CLASSIFIER_TYPE, PRODUCT_SUFFIX,
                        sourceProducts, numTrainSamples,
                        minClassValue, classValStepSize, classLevels,
                        savedClassifierName, doLoadClassifier, doClassValQuantization,
                        trainOnRaster,
                        trainingBandName, trainingVectors, featureBands, null));

        classifier.initialize();

        targetProduct = classifier.createTargetProduct();
    }

    @Override
    public void doExecute(ProgressMonitor pm) throws OperatorException {
        pm.beginTask("Preparing", 1);
        try {
            classifier.executePreparation();
            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {
        try {
            classifier.computeTileStack(this, targetTileMap, targetRectangle, pm);
        } catch (Exception e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(NaiveBayesClassifierOp.class);
        }
    }
}