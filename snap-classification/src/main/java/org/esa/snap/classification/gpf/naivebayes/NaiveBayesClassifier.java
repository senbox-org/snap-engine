package org.esa.snap.classification.gpf.naivebayes;

import com.bc.ceres.core.ProgressMonitor;
import com.thoughtworks.xstream.XStream;
import org.esa.snap.classification.gpf.*;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.util.*;
import org.esa.snap.core.util.Debug;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;
import org.esa.snap.engine_utilities.util.VectorUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import weka.core.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class NaiveBayesClassifier {

    private final NBClassifierParams params;

    private Product targetProduct = null;

    private Product trainingProduct;
    private Band trainingSetBand;

    private double trainingBandNoDataValue = Double.NaN;

    private FeatureInfo[] featureInfoList;

    private Map<String, Product> classifProductsMap;

    private List<PolygonWithClass> trainingPolygonVectorDataNodes;

    private Map<Double, String> classesMap = new HashMap<>(); //hash map containing (classValue, classLabel)pairs

    private Band classificationBand;
    private Band confidenceBand;

    private NaiveBayesWrapper mlClassifier;
    private double maxClassValue;

    private int sourceImageWidth;
    private int sourceImageHeight;
    private boolean classifierTrained = false;
    private boolean classifierFailed = false;

    private ClassifierDescriptor classifierDescriptor = null; // only for when doLoadClassifier is true
    private Double noDataVal;

    private static final String CLASSIFICATION_BAND_NAME = "nb_class";
    private static final String CONFIDENCE_BAND_NAME = "nb_confidence";
    public final static String CLASSIFIER_FILE_EXTENSION = ".model";
    public final static String CLASSIFIER_USER_INFO_FILE_EXTENSION = ".xml";
    public final static String CLASSIFIER_ROOT_FOLDER = "classifiers";

    public final static String TRAIN_ON_VECTOR_CLASSNAME = "???";

    public NaiveBayesClassifier(final NBClassifierParams params) {
        this.params = params;
    }

    public void initialize(){
        classifierFailed = false;
        classifierTrained = false;
        // Force Weka to use pure Java math backend to avoid native ARPACK issues
        System.setProperty("com.github.fommil.netlib.BLAS", "com.github.fommil.netlib.F2jBLAS");
        System.setProperty("com.github.fommil.netlib.LAPACK", "com.github.fommil.netlib.F2jLAPACK");
        System.setProperty("com.github.fommil.netlib.ARPACK", "com.github.fommil.netlib.F2jARPACK");

        checkSourceProductsValidity();

        // training product is always the 1st product
        trainingProduct = params.getTrainingProduct();

        classifProductsMap = new HashMap<>();
        if(params.getSourceProducts() != null) {
            for (Product product : params.getSourceProducts()) {
                classifProductsMap.put(product.getName(), product);
            }
        }

        if (params.getClassValStepSize() < 0.0) {
            throw new OperatorException("Invalid class value step size = " + params.getClassValStepSize());
        }

        if (params.getClassLevels() < 2) {
            throw new OperatorException("Invalid class levels = " + params.getClassLevels() + "; it must be at least 2");
        }
        maxClassValue = getMaxValue(params.getMinClassValue(), params.getClassValStepSize(), params.getClassLevels());

        if (!params.isDoLoadClassifier()) {
            if (params.isTrainOnRaster()) {
                String bandName = "";
                // UI should not allow user to choose more than one training band
                if (StringUtils.isNullOrEmpty(params.getTrainingBandName())) {
                    // user did not choose any, just take the first one
                    trainingSetBand = trainingProduct.getBandAt(0);
                } else {
                    bandName = params.getTrainingBandName();
                    if (bandName.contains("::")) {
                        bandName = bandName.substring(0, bandName.indexOf("::"));
                    }
                    trainingSetBand = trainingProduct.getBand(bandName);
                }
                if (trainingSetBand == null) {
                    throw new OperatorException("Fail to find training band in 1st source product: " + bandName);
                }
            }
        }

        if (trainingSetBand != null && trainingSetBand.isNoDataValueSet()) {
            trainingBandNoDataValue = trainingSetBand.getNoDataValue();
        }
    }

    public void executePreparation() {
        if (!params.isDoLoadClassifier()) {
            if (params.getFeatureBandNames() == null || params.getFeatureBandNames().length == 0) {
                loadFeatureBands();
            }
            validateFeatureBands();
        }

        // trainingPolygonVectorDataNodes contains all the polygons selected by user. They will be used to define the class labels
        // The polygons must be in the first product
        ProductUtils.copyVectorData(trainingProduct, targetProduct);
        if (!params.isDoLoadClassifier() && (trainingProduct != null && !params.isTrainOnRaster())) {
            // train on vectors/polygons
            //if no training vector selected, get all vectors from the training product
            loadTrainingVectors();
        }

        setIndexCoding();

       classesMap = collectClassLabels();
    }

    public void computeTileStack(final Operator operator, final Map<Band, Tile> targetTileMap,
                                 final Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException, IOException {

        if (classifierFailed) return;

        if (!classifierTrained) {
            if (params.isDoLoadClassifier()) {
                loadClassifier();
            } else {
                trainClassifier(operator, pm);
            }
        }

        if (classifierFailed) return;

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int xMax = x0 + targetRectangle.width;
        final int yMax = y0 + targetRectangle.height;

        final Tile labelTile = targetTileMap.get(classificationBand);
        final Tile confidenceTile = targetTileMap.get(confidenceBand);
        final ProductData labelBuffer = labelTile.getDataBuffer();
        final ProductData confidenceBuffer = confidenceTile.getDataBuffer();
        final TileIndex tgtIndex = new TileIndex(labelTile);

        final Tile[] featureTiles = new Tile[featureInfoList.length];
        int i = 0;
        for (FeatureInfo feature : featureInfoList) {
            featureTiles[i++] = operator.getSourceTile(feature.getFeatureBand(), targetRectangle);
        }

        for (int y = y0; y < yMax; ++y) {
            tgtIndex.calculateStride(y);
            for (int x = x0; x < xMax; ++x) {
                final int tgtIdx = tgtIndex.getIndex(x);
                final double[] features = ClassificationUtils.getFeatures(featureTiles, featureInfoList, x, y);
                if (features == null || ClassificationUtils.hasInvalidFeature(features, featureInfoList)) {
                    labelBuffer.setElemDoubleAt(tgtIdx, noDataVal);
                    confidenceBuffer.setElemDoubleAt(tgtIdx, FeatureInfo.DOUBLE_NO_DATA_VALUE);
                    continue;
                }
                double[] vals = new double[features.length + 1];

                System.arraycopy(features, 0, vals, 0, features.length);
                // Mark class value as missing
                vals[features.length] = Utils.missingValue();
                final Instance instance = new DenseInstance(1.0, vals);
                instance.setDataset(mlClassifier.getWekaHeader());

                double confidence = FeatureInfo.DOUBLE_NO_DATA_VALUE;
                double[] arrInstanceDistr = null;

                Double classIdx;
                String classLabel;
                Double classValue = noDataVal;
                try {
                    classIdx = (Double)mlClassifier.classifyInstance(instance);
                    arrInstanceDistr = mlClassifier.distributionForInstance(instance);
                } catch (Exception e) {
                    classIdx = null;
                }
                if (classIdx != null) {
                    if(arrInstanceDistr != null) {
                        try {
                            confidence = arrInstanceDistr[((Double) classIdx).intValue()];
                        }catch(Exception ex){
                            Debug.trace("Get confidence value - cannot covert class value to int " + classIdx + " : " + ex.getMessage());
                        }
                    }

                    classLabel = mlClassifier.getWekaHeader().classAttribute().value(classIdx.intValue());
                    classValue = getClassValueFromLabel(classLabel);
                }
                labelBuffer.setElemDoubleAt(tgtIdx,  classValue);
                confidenceBuffer.setElemDoubleAt(tgtIdx, confidence);
            }
        }
    }

    public Product createTargetProduct() {

        targetProduct = new Product(
                trainingProduct.getName() + params.getProductSuffix(),
                trainingProduct.getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        ProductUtils.copyProductNodes(trainingProduct, targetProduct);

        int dataType;
        String unit;
        String bandName = CLASSIFICATION_BAND_NAME;
        if (params.isDoLoadClassifier()) {
            ClassifierUserInfo info = readXMLParameters();
            dataType = info.datatype;
            unit = info.unit;
            if (!info.className.contains(TRAIN_ON_VECTOR_CLASSNAME)) {
                bandName = "predicted" + info.className;
            }
        } else {
            dataType = (params.isTrainOnRaster() && trainingSetBand != null) ? trainingSetBand.getDataType() : ProductData.TYPE_INT16;
            unit = (params.isTrainOnRaster() && trainingSetBand != null ? trainingSetBand.getUnit() : "discrete classes");
            bandName = (params.isTrainOnRaster() && trainingSetBand != null) ? "predicted" + trainingSetBand.getName() : CLASSIFICATION_BAND_NAME;
        }

        classificationBand = new Band(
                bandName,
                dataType,
                sourceImageWidth,
                sourceImageHeight);

        classificationBand.setUnit(unit);
        noDataVal = (dataType == ProductData.TYPE_INT16) ? FeatureInfo.INT_NO_DATA_VALUE : FeatureInfo.DOUBLE_NO_DATA_VALUE;
        classificationBand.setNoDataValue(noDataVal);
        classificationBand.setNoDataValueUsed(true);
        classificationBand.setValidPixelExpression(CONFIDENCE_BAND_NAME + " >= 0.5"); // Can change this in properties of band
        targetProduct.addBand(classificationBand);

        confidenceBand = new Band(
                CONFIDENCE_BAND_NAME,
                ProductData.TYPE_FLOAT32,
                sourceImageWidth,
                sourceImageHeight);

        confidenceBand.setUnit("(0, 1]");
        confidenceBand.setNoDataValue(FeatureInfo.DOUBLE_NO_DATA_VALUE);
        confidenceBand.setNoDataValueUsed(true);
        targetProduct.addBand(confidenceBand);

        return targetProduct;
    }

    private synchronized void trainClassifier(final Operator operator, final ProgressMonitor pm)  {

        if (classifierTrained || classifierFailed) return;

        featureInfoList = getFeatureInfos();

        final List<Instance> instanceList = new ArrayList<>();

        Set<String> classLabels = new HashSet<>(classesMap.values());

        mlClassifier = createMLClassifier(featureInfoList, classLabels, params.getNumTrainSamples());

        if (params.isTrainOnRaster()){
            instanceList.addAll(getInstanceListFromTrainingBand(operator, params.getNumTrainSamples(), featureInfoList));
        }else {
            instanceList.addAll(getInstanceListFromPolygons(operator, params.getNumTrainSamples(), featureInfoList));
        }

        try {
            Instances trainingSet = new Instances(mlClassifier.getWekaHeader(), params.getNumTrainSamples());
            trainingSet.addAll(instanceList);
            mlClassifier.buildClassifier(trainingSet);

            saveClassifier(mlClassifier);
        } catch (OperatorException e){
            classifierFailed = true;
            throw  e;
        } catch (Exception e) {
            classifierFailed = true;
            throw new OperatorException("Failed to train classifier.", e);
        }finally {
            classifierTrained = true;
        }
    }

    private List<Instance> getInstanceListFromTrainingBand(final Operator operator, final int numInstances,
                                                       final FeatureInfo[] featureInfos) {
        final List<Instance> instanceList = new ArrayList<>();

        final Dimension tileSize = new Dimension(20, 10);
        final Rectangle[] tileRectangles = OperatorUtils.getAllTileRectangles(trainingProduct, tileSize, 0);
        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Getting training data... ", tileRectangles.length);

        try {
            final ThreadExecutor executor = new ThreadExecutor();

            for (final Rectangle rectangle : tileRectangles) {
                final ThreadRunnable worker = new ThreadRunnable() {

                    final int xMin = rectangle.x;
                    final int xMax = rectangle.x + rectangle.width;
                    final int yMin = rectangle.y;
                    final int yMax = rectangle.y + rectangle.height;

                    final Tile trainingBandTile = operator.getSourceTile(trainingSetBand, rectangle);
                    final Tile[] featureTiles = new Tile[featureInfos.length];

                    @Override
                    public void process() {
                        int i = 0;
                        for (FeatureInfo featureInfo : featureInfos) {
                            featureTiles[i++] = operator.getSourceTile(featureInfo.getFeatureBand(), rectangle);
                        }

                        for (int y = yMin; y < yMax; ++y) {
                            for (int x = xMin; x < xMax; ++x) {
                                final double classVal = trainingBandTile.getDataBuffer().getElemDoubleAt(trainingBandTile.getDataBufferIndex(x, y));
                                if (Double.isNaN(classVal) || classVal == trainingBandNoDataValue) {
                                    continue;
                                }
                                final double[] features = ClassificationUtils.getFeatures(featureTiles, featureInfoList, x, y);
                                if (features == null) {
                                    continue;
                                }

                                double[] arrClassValues = new double[features.length + 1];
                                System.arraycopy(features, 0, arrClassValues, 0, features.length);
                                Double qClassValue = quantize(classVal);
                                arrClassValues[features.length] = mlClassifier.getWekaHeader().classAttribute().indexOfValue(classesMap.get(qClassValue));
                                final Instance instance = new DenseInstance(1.0, arrClassValues);

                                synchronized (instanceList) {
                                    if (instanceList.size() < numInstances) {
                                        instanceList.add(instance);
                                        if (instanceList.size() >= numInstances) {
                                            return;
                                        }
                                    } else {
                                        return;
                                    }
                                }
                            }
                        }
                    }
                };

                executor.execute(worker);

                status.worked(1);
            }

            executor.complete();


        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(params.getClassifierType() + " getTrainingData ", e);
        } finally {
            status.done();
        }
        return instanceList;
    }

    private List<Instance> getInstanceListFromPolygons(final Operator operator, final int numInstances,
                                                                        final FeatureInfo[] featureInfos) throws OperatorException {
        final String tmpVirtualBandPrefix = "tmpVirtualBand_";
        final Dimension tileSize = new Dimension(512, 512);
        final Rectangle[] tileRectangles = OperatorUtils.getAllTileRectangles(trainingProduct, tileSize, 0);

        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Extracting data... ", tileRectangles.length);

        final List<Instance> instanceList = new ArrayList<>();
        final ThreadExecutor executor = new ThreadExecutor();

        final int numClasses = mlClassifier.getWekaHeader().numClasses();

        final int[] instancesCnt = new int[numClasses];

        final int maxCnt = (int) Math.ceil(numInstances / (double) numClasses);

        try {
            // Loop through each rectangle to see if it intersects with any of the class polygons, if it does, then
            // the intersecting pixel is added to instanceList
            for (int i = 0; i < tileRectangles.length; i++) {

                final Rectangle rectangle = tileRectangles[i];

                VectorDataNode[] polygonVectorDataNodes = trainingPolygonVectorDataNodes.stream()
                        .map(PolygonWithClass::getPolygon)
                        .toArray(VectorDataNode[]::new);

                // Get the class polygons that intersect this rectangle
                final VectorDataNode[] polygons = VectorUtils.getPolygonsForOneRectangle(rectangle,
                        trainingProduct.getSceneGeoCoding(),polygonVectorDataNodes);

                if (polygons.length == 0) {
                    status.worked(1);
                    continue;
                }
                Map<VectorDataNode, Integer> polygonVectorDataNodeToVectorIndex =  trainingPolygonVectorDataNodes.stream()
                        .collect(Collectors.toMap(PolygonWithClass::getPolygon, PolygonWithClass::getPolygonIndex));

                final String expression = ClassificationUtils.getExpression(polygons, polygonVectorDataNodeToVectorIndex);

                final String virtualBandName = tmpVirtualBandPrefix + i;

                // The virtual band will contain the class values
                final Band virtualBand = new VirtualBand(
                        virtualBandName,
                        ProductData.TYPE_INT16,
                        sourceImageWidth,
                        sourceImageHeight,
                        expression);
                trainingProduct.addBand(virtualBand);

                final ThreadRunnable worker = new ThreadRunnable() {

                    @Override
                    public void process() {
                        try {
                            final int xMin = rectangle.x, yMin = rectangle.y;
                            final int w = rectangle.width, h = rectangle.height;
                            final int xMax = xMin + w, yMax = yMin + h;

                            final Tile virtualBandTile = operator.getSourceTile(virtualBand, rectangle);
                            final ProductData virtualBandData = virtualBandTile.getDataBuffer();

                            final Tile[] featureTiles = new Tile[featureInfos.length];

                            for (int j = 0; j < featureInfos.length; j++) {
                                featureTiles[j] = operator.getSourceTile(featureInfos[j].getFeatureBand(), rectangle);
                            }

                            for (int y = yMin; y < yMax; ++y) {
                                for (int x = xMin; x < xMax; ++x) {

                                    int classVal = virtualBandData.getElemIntAt(virtualBandTile.getDataBufferIndex(x, y));
                                    if (classVal < 0) {
                                        // This pixel is not inside a class polygon
                                        continue;
                                    }

                                    // Get the features values for this pixel
                                    final double[] features = ClassificationUtils.getFeatures(featureTiles, featureInfos, x, y);
                                    if (features == null) {
                                        continue;
                                    }

                                    double[] arrClassValues = new double[features.length + 1];
                                    System.arraycopy(features, 0, arrClassValues, 0, features.length);
                                    arrClassValues[features.length] = mlClassifier.getWekaHeader().classAttribute().indexOfValue(classesMap.get(classVal *1.0));
                                    final Instance instance = new DenseInstance(1.0, arrClassValues);

                                    synchronized (instanceList) {
                                        if (instanceList.size() < numInstances) {
                                            if (instancesCnt[classVal] < maxCnt) {
                                                instanceList.add(instance);
                                                instancesCnt[classVal]++;
                                                if (instanceList.size() >= numInstances) {
                                                    return;
                                                }
                                            }
                                        } else {
                                            return;
                                        }
                                    }
                                }
                            }

                        } catch (Exception e) {
                            SystemUtils.LOG.severe("Error retrieving features from polygons " + e.getMessage());
                        }
                    }
                };

                executor.execute(worker);
                status.worked(1);
            }

            executor.complete();

            for (int i = 0; i < tileRectangles.length; i++) {
                Band band = trainingProduct.getBand(tmpVirtualBandPrefix + i);
                if (band != null) {
                    trainingProduct.removeBand(band);
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(params.getClassifierType() + " getTrainingData from polygons ", e);
        } finally {
            status.done();
        }

        return instanceList;
    }

    private double quantize(double val) {
        if (!params.isDoClassValQuantization()) {
            return val;
        }
        return VectorUtils.quantize(val, params.getMinClassValue(), maxClassValue, params.getClassValStepSize());
    }

    public static double getMaxValue(final double minVal, final double stepSize, final int levels) {
        return minVal + stepSize * (levels - 1);
    }

    private FeatureInfo[] getFeatureInfos() throws OperatorException {
        int bandId = 0;

        final List<FeatureInfo> featureInfos = new ArrayList<>(params.getFeatureBandNames().length);
        for (String featureBandName : params.getFeatureBandNames()) {
            final int multiProductIndex = featureBandName.indexOf("::");
            String bandName = featureBandName;
            String productName = trainingProduct.getName();
            if (multiProductIndex > 0) {
                bandName = featureBandName.substring(0, multiProductIndex);
                productName = featureBandName.substring(featureBandName.indexOf("::") + 2);
            }

            final Product product = classifProductsMap.get(productName);
            if (product != null) {
                final Band featureBand = product.getBand(bandName);
                if (featureBand == null) {
                    classifierFailed = true;
                    throw new OperatorException("Failed to find feature band " + featureBandName);
                }

                FeatureInfo featureInfo;
                if(featureBand.isScalingApplied()) {
                    featureInfo = new FeatureInfo(featureBand, bandId, featureBand.getNoDataValue(), featureBand.getScalingOffset(), featureBand.getScalingFactor());
                } else {
                    featureInfo = new FeatureInfo(featureBand, bandId);
                }
                featureInfos.add(featureInfo);
                bandId++;
            } else {
                throw new OperatorException("Failed to find feature product " + bandId);
            }
        }
        return featureInfos.toArray(new FeatureInfo[0]);
    }

    private void checkSourceProductsValidity() {

        // All the source products must have the same raster dimensions.
        // All bands in a product must have the same raster dimensions.
        sourceImageHeight = params.getSourceProduct(0).getSceneRasterHeight();
        sourceImageWidth = params.getSourceProduct(0).getSceneRasterWidth();

        for (int i = 0; i < params.getSourceProducts().length; i++) {
            Product currentProduct = params.getSourceProduct(i);
            if (sourceImageHeight != currentProduct.getSceneRasterHeight() ||
                    sourceImageWidth != currentProduct.getSceneRasterWidth()) {
                throw new OperatorException("Source products are of different dimensions");
            }

            for (Band band : currentProduct.getBands()) {
                if (band.getRasterWidth() != sourceImageWidth || band.getRasterHeight() != sourceImageHeight) {
                    throw new OperatorException("Bands in source product " + currentProduct.getName() +
                            " are of different dimensions");
                }
            }
        }
    }

    private void setIndexCoding() {
        if (!params.isTrainOnRaster()) {
            if (!params.isDoLoadClassifier()) {
                // train on vectors/polygons
                final IndexCoding indexCoding = new IndexCoding("Classes");
                indexCoding.addIndex("no data", FeatureInfo.INT_NO_DATA_VALUE, "no data");
                for (int i = 0; i< trainingPolygonVectorDataNodes.size(); i++) {
                    PolygonWithClass polygon =trainingPolygonVectorDataNodes.get(i);
                    if (StringUtils.isNullOrEmpty(polygon.classLabel)){
                        polygon.classLabel = "null";
                    }
                    indexCoding.addIndex(polygon.classLabel, i, "");
                }
                targetProduct.getIndexCodingGroup().add(indexCoding);
                classificationBand.setSampleCoding(indexCoding);

                // remove training vectors
                final ProductNodeGroup<VectorDataNode> vectorDataGroup = targetProduct.getVectorDataGroup();
                for (String vector : params.getTrainingVectors()) {
                    vectorDataGroup.remove(vectorDataGroup.get(getVectorClassLabel(vector)));
                }
            }
        } else if (!params.isDoLoadClassifier()) {
            // train on raster
            IndexCoding indexCoding = trainingSetBand.getIndexCoding();
            if (indexCoding != null) {
                IndexCoding icCopy = ProductUtils.copyIndexCoding(indexCoding, targetProduct);
                classificationBand.setSampleCoding(icCopy);
            }
        }
    }

    private Map<Double, String> collectClassLabels() {

        Map<Double, String> classesMap = new HashMap<>();
        if (params.isTrainOnRaster()) {
            if (trainingSetBand != null){
                int w = trainingSetBand.getRasterWidth();
                int h = trainingSetBand.getRasterHeight();
                float[] classes = new float[w * h];
                try {
                    trainingSetBand.readPixels(0, 0, w, h, classes);
                } catch (Exception e) {
                    throw new OperatorException("Failed to read label band for class collection", e);
                }
                for (float classVal : classes) {
                    if (Float.isNaN(classVal)) continue;
                    Double qClassValue = quantize(classVal);
                    if (!classesMap.containsKey(qClassValue)){
                        classesMap.put(qClassValue, qClassValue.toString());
                    }
                }
            }
        }else {
            if (trainingPolygonVectorDataNodes != null && trainingPolygonVectorDataNodes.size() > 0) {
                for (PolygonWithClass polygon : trainingPolygonVectorDataNodes) {
                    classesMap.put(polygon.getPolygonIndex() *1.0, getVectorClassLabel(polygon.getClassLabel()));
                }
            }
        }
        if (classesMap.isEmpty()) classesMap.put(0.0,"0");

        return classesMap;
    }

    private double getClassValueFromLabel(String classLabel){
        for (Double classValue: classesMap.keySet()){
            if (classesMap.get(classValue).compareTo(classLabel) ==0 ){
                return classValue;
            }
        }
        return -1;
    }

    private static String getVectorClassLabel(String vectorName) {
        String label = vectorName;
        if (vectorName.contains("::")) {
            label = vectorName.substring(0, vectorName.indexOf("::"));
        }
        return label;
    }

    private double[] getSortedClassValues(final NaiveBayesWrapper classifier){
        String[] classLabels = classifier.getClassLabels();
        double[] sortedClassValues = new double[classLabels.length];

        for (int idx = 0; idx < classLabels.length; idx++){
            sortedClassValues[idx] = getClassValueFromLabel(classLabels[idx]);
        }
        return sortedClassValues;
    }

    private void loadFeatureBands() throws OperatorException {
        if (params.getFeatureBandNames() == null || params.getFeatureBandNames().length == 0) {
            final List<String> featureBandNames = new ArrayList<>();
            final List<String> distinctBandNames = new ArrayList<>();
            for (Product p : params.getSourceProducts()) {
                for (Band b : p.getBands()) {
                    if (b == trainingSetBand) continue;
                    String bandName = b.getName();
                    if (distinctBandNames.contains(bandName)) {
                        throw new OperatorException("Cannot have same feature band " + bandName + " in more than one product");
                    }
                    if (trainingSetBand != null && bandName.equals(trainingSetBand.getName())) {
                        throw new OperatorException("Training band cannot be feature band - " + bandName);
                    }
                    distinctBandNames.add(bandName);
                    featureBandNames.add(bandName + "::" + p.getName());
                }
            }
            params.setFeatureBandNames(featureBandNames.toArray(new String[0]));
        }
    }

    private void loadTrainingVectors(){
        if (params.getTrainingVectors() == null || params.getTrainingVectors().length == 0) {
            final List<String> vectorNames = new ArrayList<>();
            final ProductNodeGroup<VectorDataNode> vectorDataNodes = trainingProduct.getVectorDataGroup();
            for (int i = 0; i < vectorDataNodes.getNodeCount(); ++i) {
                VectorDataNode node = vectorDataNodes.get(i);
                if (!node.getFeatureCollection().isEmpty()) {
                    vectorNames.add(node.getName() + "::" + trainingProduct.getName());
                }
            }

            if (vectorNames.size() < 2) {
                throw new OperatorException("Cannot train on vectors because first source product has less than 2 vectors");
            }
            params.setTrainingVectors(vectorNames.toArray(new String[0]));
        }

        trainingPolygonVectorDataNodes = new ArrayList<>();
        final ProductNodeGroup<VectorDataNode> vectorGroup = trainingProduct.getVectorDataGroup();
        for (int i = 0; i < params.getTrainingVectors().length; ++i) {
            final String strTrainingVector = params.getTrainingVector(i);
            int multiProductIndex = strTrainingVector.indexOf("::");
            String name = strTrainingVector;
            if (multiProductIndex > 0) {
                name = strTrainingVector.substring(0, multiProductIndex);
            }
            VectorDataNode currentVectorNode = vectorGroup.get(name);
            if (currentVectorNode == null) {
                throw new OperatorException("Cannot find vector " + strTrainingVector);
            }
            trainingPolygonVectorDataNodes.add(new PolygonWithClass(currentVectorNode, i, currentVectorNode.getName()));
        }

        if (trainingPolygonVectorDataNodes.stream().map(PolygonWithClass::getClassLabel).toList().size() < 2) {
            throw new OperatorException("Cannot train on vectors because the user selected less than 2 class vectors");
        }
    }

    private void validateFeatureBands() {
        if (params.getFeatureBandNames() == null || params.getFeatureBandNames().length == 0){
            classifierFailed = true;
            throw new OperatorException("No feature band has been selected.");
        }
        List<String> bandNames = new ArrayList<>();
        for (String s : params.getFeatureBandNames()) {
            final int multiProductIndex = s.indexOf("::");
            String bandName = s;
            String productName = trainingProduct.getName();
            if (multiProductIndex > 0) {
                bandName = s.substring(0, multiProductIndex);
                productName = s.substring(s.indexOf("::") + 2);
                if (bandNames.contains(bandName)) {
                    classifierFailed = true;
                    throw new OperatorException("Cannot select feature band " + bandName + " in more than one product");
                } else {
                    bandNames.add(bandName);
                }
            }
            final Product product = classifProductsMap.get(productName);
            if (product == null) {
                classifierFailed = true;
                throw new OperatorException("Failed to find feature product " + s);
            }else {
                final Band featureBand = product.getBand(bandName);
                if (featureBand == null) {
                    classifierFailed = true;
                    throw new OperatorException("Failed to find feature band " + s);
                } else if (trainingSetBand != null && bandName.equals(trainingSetBand.getName())) {
                    classifierFailed = true;
                    throw new OperatorException("Cannot select training band as feature band");
                }
            }
        }
    }

    private NaiveBayesWrapper createMLClassifier(FeatureInfo[] featureInfos, Set<String> classLabels, int capacity) {
        String[] featureNames = new String[featureInfos.length];
        for (int i = 0; i< featureInfos.length; i++) {
            featureNames[i] = featureInfos[i].getFeatureBand().getName();
        }
        return new NaiveBayesWrapper(featureNames, classLabels, capacity);
    }

    private void saveClassifier(final NaiveBayesWrapper classifier) throws IOException {
        //save the model file
        saveClassifierModel(classifier);

        // Now save in a xml file what the user needs to know to executePreparation the source products
        saveParametersAsXml(classifier);
    }

    private void saveClassifierModel(final NaiveBayesWrapper classifier) throws IOException {

        final String className = trainingSetBand == null ? TRAIN_ON_VECTOR_CLASSNAME : StackUtils.getBandNameWithoutDate(trainingSetBand.getName());
        double[] sortedClassValues = getSortedClassValues(classifier);
        final String classUnit = classificationBand.getUnit();
        final String[] featureNames = new String[featureInfoList.length];
        final double[] featureMinValues = new double[featureInfoList.length];
        final double[] featureMaxValues = new double[featureInfoList.length];
        for (int i = 0; i < featureInfoList.length; i++) {
            final Band featureBand = featureInfoList[i].getFeatureBand();
            featureNames[i] = featureBand.getName();
            if (featureNames[i].contains(StackUtils.MST) || featureNames[i].contains(StackUtils.SLV))
                featureNames[i] = StackUtils.getBandNameWithoutDate(featureNames[i]);
            featureMinValues[i] = featureBand.getStx().getMinimum();
            featureMaxValues[i] = featureBand.getStx().getMaximum();
        }

        ClassifierDescriptor classifierDescriptor =
                new ClassifierDescriptor(params.getClassifierType(), params.getSavedClassifierName(),
                        classifier, sortedClassValues,
                        className, classUnit, featureNames, featureMinValues, featureMaxValues,
                        params.isDoClassValQuantization(), params.getMinClassValue(),
                        params.getClassValStepSize(), params.getClassLevels(), params.getTrainingVectors());

        final Path modelFilePath = getClassifierFilePath(CLASSIFIER_FILE_EXTENSION);

        FileOutputStream fos;
        ObjectOutputStream out;
        try {
            fos = new FileOutputStream(modelFilePath.toString());
            out = new ObjectOutputStream(fos);
            out.writeObject(classifierDescriptor);
            out.close();
        } catch (Exception ex) {
            throw new OperatorException("Failed to save classifier " + ex.getMessage());
        }
    }

    private void saveParametersAsXml(NaiveBayesWrapper classifier) throws IOException {

        final String className = trainingSetBand == null ? TRAIN_ON_VECTOR_CLASSNAME : StackUtils.getBandNameWithoutDate(trainingSetBand.getName());

        double[] sortedClassValues = getSortedClassValues(classifier);

        final String[] featureNames = classifier.getFeatureAttributes();

        ClassifierUserInfo classifierUserInfo =
                new ClassifierUserInfo(params.getSavedClassifierName(), params.getClassifierType(),
                        className, params.getNumTrainSamples(), sortedClassValues, featureInfoList.length,
                        new String[] { params.getTrainingBandName() }, params.getTrainingVectors(), featureNames,
                        (params.isTrainOnRaster() && params.isDoClassValQuantization() ? params.getMinClassValue() : 0.0),
                        (params.isTrainOnRaster() && params.isDoClassValQuantization()  ? params.getClassValStepSize() : 0.0),
                        (params.isTrainOnRaster() && params.isDoClassValQuantization()  ? params.getClassLevels() : -1),
                        (params.isTrainOnRaster() && params.isDoClassValQuantization()  ? maxClassValue : 0.0),
                        classificationBand.getDataType(), classificationBand.getUnit());

        final XStream xstream = new XStream();
        xstream.processAnnotations(classifierUserInfo.getClass());
        final String xmlContent = xstream.toXML(classifierUserInfo);

        final Path modelFilePath = getClassifierFilePath(CLASSIFIER_USER_INFO_FILE_EXTENSION);
        File infoFile = new File(modelFilePath.toUri());

        FileWriter fileWriter = new FileWriter(infoFile);
        fileWriter.write(xmlContent);
        fileWriter.flush();
        fileWriter.close();
    }

    private ClassifierUserInfo readXMLParameters() {
        ClassifierUserInfo info = new ClassifierUserInfo();

        try {
            final Path modelFilePath = getClassifierFilePath(CLASSIFIER_USER_INFO_FILE_EXTENSION);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(modelFilePath.toString());
            doc.getDocumentElement().normalize();

            NodeList datatype = doc.getElementsByTagName("datatype");
            if (datatype!= null && datatype.getLength() > 0){
                info.datatype = Integer.parseInt(datatype.item(0).getTextContent());
            }

            NodeList unit = doc.getElementsByTagName("unit");
            if (unit!= null && unit.getLength() > 0){
                info.unit = unit.item(0).getTextContent();
            }

            NodeList className = doc.getElementsByTagName("className");
            if (className!= null && className.getLength() > 0){
                info.className = className.item(0).getTextContent();
            }

        } catch (Exception ex) {
            throw new OperatorException("Failed to read XML parameters of the classifier " + ex.getMessage());
        }


        return info;
    }

    private Path getClassifierFilePath(final String fileExtension) throws IOException {

        final Path classifierDir = SystemUtils.getAuxDataPath().
                resolve(CLASSIFIER_ROOT_FOLDER).resolve(params.getClassifierType());

        if (Files.notExists(classifierDir)) {
            Files.createDirectories(classifierDir);
        }
        return classifierDir.resolve(params.getSavedClassifierName() + fileExtension);
    }

    private synchronized void loadClassifier()  {

        if (classifierTrained || classifierFailed) return;

        try {
            loadClassifierDescriptor();

            mlClassifier = (NaiveBayesWrapper) classifierDescriptor.getObject();

            final double[] featureMinValues = classifierDescriptor.getFeatureMinValues();
            final double[] featureMaxValues = classifierDescriptor.getFeatureMaxValues();
            double[] sortedClasses = classifierDescriptor.getSortedClassValues();

            classesMap = new HashMap<>();

            //load vectors if trained on vectors
            if (classifierDescriptor.getClassName().compareTo(TRAIN_ON_VECTOR_CLASSNAME) == 0) {
                String[] labels = classifierDescriptor.getPolygonsAsClasses();
                if (labels != null && labels.length != 0) {
                    final IndexCoding indexCoding = new IndexCoding("Classes");
                    indexCoding.addIndex("no data", FeatureInfo.INT_NO_DATA_VALUE, "no data");
                    for (int i = 0; i < sortedClasses.length; i++) {
                        final int idx = labels[i].indexOf("::");
                        if (idx < 0) {
                            indexCoding.addIndex(labels[i], (int) sortedClasses[i], "");
                            classesMap.put(sortedClasses[i], labels[i]);
                        } else {
                            indexCoding.addIndex(labels[i].substring(0, idx), (int) sortedClasses[i], "");
                            classesMap.put(sortedClasses[i], labels[i].substring(0, idx));
                        }
                    }
                    targetProduct.getIndexCodingGroup().add(indexCoding);
                    classificationBand.setSampleCoding(indexCoding);
                }
            }else{
                for (Double classValue: sortedClasses){
                    classesMap.put(classValue, classValue.toString());
                }
            }

            //load features
            final String[] featureNames = classifierDescriptor.getFeatureNames();

            final int totalAvailableFeatures = classifProductsMap.values().stream().mapToInt(p -> p.getNumBands()).sum();

            if (featureNames.length > totalAvailableFeatures) {
                classifierFailed = true;
                throw new OperatorException("Classifier expects " + featureNames.length
                        + " features; source product(s) only have " + totalAvailableFeatures);
            }

            final List<FeatureInfo> featureInfos = new ArrayList<>(featureNames.length);
            Product[] classifProducts = classifProductsMap.values().toArray(new Product[0]);

            for (int i = 0; i < featureNames.length; i++) {
                Set<Product> parentProducts = new HashSet<>();
                Band featureBand = null;
                for (Product classifProduct: classifProducts){
                    featureBand = getBandByName(featureNames[i], classifProduct);
                    if (featureBand == null){
                        throw new OperatorException("Failed to find feature band " + featureNames[i] + " in source product");
                    }else if (parentProducts.contains(classifProduct)){
                        classifierFailed = true;
                        throw new OperatorException(featureBand.getName() + " for " + featureNames[i] + " has already appeared as an earlier feature");
                    } else {
                        parentProducts.add(classifProduct);
                        break;
                    }
                }

                double noDataValue = FeatureInfo.DOUBLE_NO_DATA_VALUE;
                if (featureBand != null && featureBand.isNoDataValueSet()) {
                    noDataValue = featureBand.getNoDataValue();
                }

                double offset = featureMinValues[i];
                double scale = 1.0 / (featureMaxValues[i] - offset);

                if (featureBand.isScalingApplied()) {
                    offset = featureBand.getScalingOffset();
                    scale = featureBand.getScalingFactor();
                }

                featureInfos.add(new FeatureInfo(featureBand, i, noDataValue, offset, scale));
            }

            SystemUtils.LOG.info("*** Loaded " + params.getClassifierType() + " classifier (filename = " + params.getSavedClassifierName()
                    + ") to predict " + classifierDescriptor.getClassName());

            featureInfoList = featureInfos.toArray(new FeatureInfo[0]);

        } catch (Exception ex) {
            classifierFailed = true;
            throw new OperatorException("Error loading or using loaded classifier (" + ex.getMessage() + ')');
        }

        classifierTrained = true;
    }

    private Band getBandByName(String bandName, Product product){
        if (product == null || StringUtils.isNullOrEmpty(bandName)){
            return null;
        }
        for (int idx = 0; idx < product.getNumBands(); idx++) {
            String currentBandName= product.getBandAt(idx).getName();
            if (currentBandName.compareTo(bandName) == 0 || currentBandName.startsWith(bandName + "::")){
                return product.getBandAt(idx);
            }
        }
        return null;
    }

    private void loadClassifierDescriptor() {

        try {
            final Path filePath = getClassifierFilePath(CLASSIFIER_FILE_EXTENSION);

            final FileInputStream fis = new FileInputStream(filePath.toFile());
            try (final ObjectInputStream in = new ObjectInputStream(fis)) {

                classifierDescriptor = (ClassifierDescriptor) in.readObject();

                final String cType = classifierDescriptor.getClassifierType();
                if (!cType.equals(params.getClassifierType())) {
                    throw new OperatorException("Loaded classifier is " + cType + " NOT " + params.getClassifierType());
                }

                params.setDoClassValQuantization(classifierDescriptor.getDoClassValQuantization());
                params.setMinClassValue(classifierDescriptor.getMinClassValue());
                params.setClassValStepSize( classifierDescriptor.getClassValStepSize());
                params.setClassLevels(classifierDescriptor.getClassLevels());
                params.setTrainingVectors( classifierDescriptor.getPolygonsAsClasses());
            }
        } catch (Exception ex) {
            throw new OperatorException("Failed to load classifier " + ex.getMessage());
        }
    }

    private static class PolygonWithClass{
        private final VectorDataNode polygon;
        private final int polygonIndex;
        private String classLabel;

        public PolygonWithClass (VectorDataNode polygon, int polygonIndex, String classLabel){
            this.polygon = polygon;
            this.polygonIndex = polygonIndex;
            this.classLabel = classLabel;
        }

        public VectorDataNode getPolygon(){
            return polygon;
        }
        public int getPolygonIndex(){
            return polygonIndex;
        }

        public String getClassLabel(){
            return classLabel;
        }
    }
}
