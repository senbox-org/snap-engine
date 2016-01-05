//package org.esa.s3tbx.idepix.algorithms.seawifs;
//
//import org.esa.beam.framework.datamodel.Product;
//import org.esa.beam.framework.gpf.GPF;
//import org.esa.beam.framework.gpf.OperatorException;
//import org.esa.beam.framework.gpf.OperatorSpi;
//import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
//import org.esa.beam.framework.gpf.annotations.Parameter;
//import org.esa.beam.framework.gpf.annotations.SourceProduct;
//import org.esa.beam.idepix.AlgorithmSelector;
//import org.esa.beam.idepix.IdepixConstants;
//import org.esa.beam.idepix.IdepixProducts;
//import org.esa.beam.idepix.operators.BasisOp;
//import org.esa.beam.idepix.util.IdepixUtils;
//import org.esa.beam.util.ProductUtils;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * Idepix operator for pixel identification and classification with OC-CCI algorithm.
// *
// * @author olafd
// */
//@SuppressWarnings({"FieldCanBeLocal"})
//@OperatorMetadata(alias = "idepix.occci",
//        version = "2.2",
//        copyright = "(c) 2014 by Brockmann Consult",
//        description = "Pixel identification and classification with OC-CCI algorithm.")
//public class OccciOp extends BasisOp {
//
//    @SourceProduct(alias = "source", label = "Name (MODIS/SeaWiFS L1b product)", description = "The source product.")
//    private Product sourceProduct;
//
//    private Product rad2reflProduct;
//    private Product pressureLiseProduct;
//    private Product ctpProduct;
//    private Product pbaroProduct;
//
//    @Parameter(defaultValue = "false",
//            label = " Process MERIS for Sea Ice CCN ",
//            description = " For Sea Ice CCN, use Schiller 'MERIS/AATSR' NN (instead of standard CC 'WATER' NN) ")
//    private boolean processMerisSeaIce = false;
//
//    @Parameter(defaultValue = "5.0",
//            label = " Schiller 'MERIS1600' threshold (MERIS Sea Ice) ",
//            description = " Schiller 'MERIS1600' threshold value ")
//    double schillerMeris1600Threshold;
//
//    @Parameter(defaultValue = "0.5",
//            label = " Schiller 'MERIS/AATSR' cloud/ice separation value (MERIS Sea Ice) ",
//            description = " Schiller 'MERIS/AATSR' cloud/ice separation value ")
//    double schillerMerisAatsrCloudIceSeparationValue;
//
//    @Parameter(defaultValue = "true",
//            label = " Radiance bands (MERIS)",
//            description = "Write TOA radiance bands to target product (MERIS).")
//    private boolean ocOutputMerisRadiance = true;
//
//    @Parameter(defaultValue = "false",
//            label = " Write Schiller NN value to the target product (MERIS).",
//            description = " If applied, write Schiller NN value to the target product (MERIS)")
//    private boolean outputSchillerMerisNNValue;
//
////    @Parameter(defaultValue = "2.0",
////            label = " Schiller NN cloud ambiguous lower boundary (MERIS)",
////            description = " Schiller NN cloud ambiguous lower boundary (MERIS)")
////    double schillerMerisNNCloudAmbiguousLowerBoundaryValue;
//    double schillerMerisNNCloudAmbiguousLowerBoundaryValue = 2.0;
//
////    @Parameter(defaultValue = "3.7",
////            label = " Schiller NN cloud ambiguous/sure separation value (MERIS)",
////            description = " Schiller NN cloud ambiguous cloud ambiguous/sure separation value (MERIS)")
////    double schillerMerisNNCloudAmbiguousSureSeparationValue;
//    double schillerMerisNNCloudAmbiguousSureSeparationValue = 3.7;
//
////    @Parameter(defaultValue = "4.05",
////            label = " Schiller NN cloud sure/snow separation value (MERIS)",
////            description = " Schiller NN cloud ambiguous cloud sure/snow separation value (MERIS)")
////    double schillerMerisNNCloudSureSnowSeparationValue;
//    double schillerMerisNNCloudSureSnowSeparationValue = 4.05;
//
//
//    @Parameter(defaultValue = "true",
//            label = " Reflective solar bands (MODIS)",
//            description = "Write TOA reflective solar bands (RefSB) to target product (MODIS).")
//    private boolean ocOutputRad2Refl = true;
//
//    @Parameter(defaultValue = "false",
//            label = " Emissive bands (MODIS)",
//            description = "Write 'Emissive' bands to target product (MODIS).")
//    private boolean ocOutputEmissive = false;
//
//    //    @Parameter(defaultValue = "0.15",
////               label = " Brightness test threshold (MODIS)",
////               description = "Brightness test threshold: EV_250_Aggr1km_RefSB_1 > THRESH (MODIS).")
//    private double ocModisBrightnessThreshCloudSure = 0.15;
//
//    @Parameter(defaultValue = "0.15",
//            label = " 'Dark glint' threshold at 859nm (MODIS)",
//            description = "'Dark glint' threshold: Cloud possible only if EV_250_Aggr1km_RefSB_2 > THRESH.")
//    private double ocModisGlintThresh859 = 0.15;
//
//    @Parameter(defaultValue = "true",
//            label = " Apply brightness test (MODIS)",
//            description = "Apply brightness test: EV_250_Aggr1km_RefSB_1 > THRESH (MODIS).")
//    private boolean ocModisApplyBrightnessTest = true;
//
//    @Parameter(defaultValue = "true",
//            label = " Apply 'OR' logic in cloud test (MODIS)",
//            description = "Apply 'OR' logic instead of 'AND' logic in cloud test (MODIS).")
//    private boolean ocModisApplyOrLogicInCloudTest = true;
//
//    //    @Parameter(defaultValue = "0.07",
////               label = " Brightness test 'cloud ambiguous' threshold (MODIS)",
////               description = "Brightness test 'cloud ambiguous' threshold: EV_250_Aggr1km_RefSB_1 > THRESH (MODIS).")
//    private double ocModisBrightnessThreshCloudAmbiguous = 0.125;
//
//    @Parameter(defaultValue = "false",
//            label = " Radiance bands (SeaWiFS)",
//            description = "Write TOA radiance bands to target product (SeaWiFS).")
//    private boolean ocOutputSeawifsRadiance = false;
//
//    @Parameter(defaultValue = "true",
//            label = " Reflectance bands (SeaWiFS)",
//            description = "Write TOA reflectance bands to target product (SeaWiFS).")
//    private boolean ocOutputSeawifsRefl = true;
//
//    @Parameter(defaultValue = "true",
//            label = " Geometry bands (SeaWiFS)",
//            description = "Write geometry bands to target product (SeaWiFS).")
//    private boolean ocOutputGeometry = true;
//
//    @Parameter(defaultValue = "L_", valueSet = {"L_", "Lt_", "rhot_"}, label = " Prefix of input spectral bands (SeaWiFS).",
//            description = "Prefix of input radiance or reflectance bands (SeaWiFS)")
//    private String ocSeawifsRadianceBandPrefix;
//
//    @Parameter(defaultValue = "false",
//            label = " Debug bands",
//            description = "Write further useful bands to target product.")
//    private boolean ocOutputDebug = false;
//
//    @Parameter(label = " Product type",
//            description = "Defines the product type to use. If the parameter is not set, the product type defined by the input file is used.")
//    String productTypeString;
//
//    @Parameter(defaultValue = "1", label = " Width of cloud buffer (# of pixels)")
//    private int cloudBufferWidth;
//
//    @Parameter(defaultValue = "50", valueSet = {"50", "150"}, label = " Resolution of used land-water mask in m/pixel",
//            description = "Resolution in m/pixel")
//    private int ocWaterMaskResolution;
//
//    private Product classifProduct;
//    private Product waterMaskProduct;
//    private Map<String, Object> waterClassificationParameters;
//
//
//    @Override
//    public void initialize() throws OperatorException {
//        final boolean inputProductIsValid = IdepixUtils.validateInputProduct(sourceProduct, AlgorithmSelector.Occci);
//        if (!inputProductIsValid) {
//            throw new OperatorException(IdepixConstants.INPUT_INCONSISTENCY_ERROR_MESSAGE);
//        }
//
//        processOccci(createOccciCloudClassificationParameters());
//    }
//
//    private void processOccci(Map<String, Object> occciCloudClassificationParameters) {
//        Map<String, Product> occciClassifInput = new HashMap<>(4);
//        computeAlgorithmInputProducts(occciClassifInput);
//
//        // post processing input:
//        // - cloud buffer
//        // - cloud shadow todo (currently only for Meris)
//        Map<String, Object> postProcessParameters = new HashMap<>();
//        postProcessParameters.put("cloudBufferWidth", cloudBufferWidth);
//        Map<String, Product> postProcessInput = new HashMap<>();
//        postProcessInput.put("waterMask", waterMaskProduct);
//
//        if (IdepixUtils.isValidMerisProduct(sourceProduct)) {
//            classifProduct = computeMerisClassificationProduct();
//            postProcessInput.put("refl", sourceProduct);
//            postProcessInput.put("ctp", ctpProduct);
//            postProcessParameters.put("computeCloudShadow", true);
//        } else {
//            postProcessInput.put("refl", rad2reflProduct);
//            classifProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(OccciClassificationOp.class),
//                                               occciCloudClassificationParameters, occciClassifInput);
//        }
//
//        postProcessInput.put("classif", classifProduct);
//
//        Product postProcessProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(OccciPostProcessingOp.class),
//                                                       postProcessParameters, postProcessInput);
//        setTargetProduct(postProcessProduct);
//        addBandsToTargetProduct(postProcessProduct);
//    }
//
//    private void computeAlgorithmInputProducts(Map<String, Product> occciClassifInput) {
//        createWaterMaskProduct();
//        occciClassifInput.put("waterMask", waterMaskProduct);
//
//        if (IdepixUtils.isValidMerisProduct(sourceProduct)) {
//            // MERIS:
//            rad2reflProduct = IdepixProducts.computeRadiance2ReflectanceProduct(sourceProduct);
//            ctpProduct = IdepixProducts.computeCloudTopPressureProduct(sourceProduct);
//            pressureLiseProduct = IdepixProducts.computePressureLiseProduct(sourceProduct, rad2reflProduct,
//                                                                            false, false, true, false, false, true);
//            pbaroProduct = IdepixProducts.computeBarometricPressureProduct(sourceProduct, false);
//        } else {
//            // MODIS, SeaWIFS: we will convert pixelwise later, for MODIS inputs are TOA reflectances anyway
//            rad2reflProduct = sourceProduct;
//        }
//        occciClassifInput.put("refl", rad2reflProduct);
//    }
//
//    private void createWaterMaskProduct() {
//        HashMap<String, Object> waterParameters = new HashMap<>();
//        waterParameters.put("resolution", ocWaterMaskResolution);
//        waterParameters.put("subSamplingFactorX", 3);
//        waterParameters.put("subSamplingFactorY", 3);
//        waterMaskProduct = GPF.createProduct("LandWaterMask", waterParameters, sourceProduct);
//    }
//
//    private Map<String, Object> createOccciCloudClassificationParameters() {
//        Map<String, Object> occciCloudClassificationParameters = new HashMap<>(1);
//        occciCloudClassificationParameters.put("productTypeString", productTypeString);
//        occciCloudClassificationParameters.put("cloudBufferWidth", cloudBufferWidth);
//        occciCloudClassificationParameters.put("wmResolution", ocWaterMaskResolution);
//        occciCloudClassificationParameters.put("ocOutputDebug", ocOutputDebug);
//        occciCloudClassificationParameters.put("ocOutputSeawifsRadiance", ocOutputSeawifsRadiance);
//        occciCloudClassificationParameters.put("ocSeawifsRadianceBandPrefix", ocSeawifsRadianceBandPrefix);
//        occciCloudClassificationParameters.put("ocModisApplyBrightnessTest", ocModisApplyBrightnessTest);
//        occciCloudClassificationParameters.put("ocModisBrightnessThreshCloudSure", ocModisBrightnessThreshCloudSure);
//        occciCloudClassificationParameters.put("ocModisBrightnessThreshCloudAmbiguous", ocModisBrightnessThreshCloudAmbiguous);
//        occciCloudClassificationParameters.put("ocModisGlintThresh859", ocModisGlintThresh859);
//        occciCloudClassificationParameters.put("ocModisApplyOrLogicInCloudTest", ocModisApplyOrLogicInCloudTest);
//
//        return occciCloudClassificationParameters;
//    }
//
//    private Product computeMerisClassificationProduct() {
//        Map<String, Product> classificationInputProducts = new HashMap<>();
//        classificationInputProducts.put("l1b", sourceProduct);
//        classificationInputProducts.put("rhotoa", rad2reflProduct);
//        classificationInputProducts.put("pressure", ctpProduct);
//        classificationInputProducts.put("pressureLise", pressureLiseProduct);
//        classificationInputProducts.put("waterMask", waterMaskProduct);
//        if (processMerisSeaIce) {
//            setMerisSeaIceClassificationParameters();
//            return GPF.createProduct(OperatorSpi.getOperatorAlias(OccciMerisSeaiceClassificationOp.class),
//                                     waterClassificationParameters, classificationInputProducts);
//        } else {
//            setMerisStandardClassificationParameters();
//            return GPF.createProduct(OperatorSpi.getOperatorAlias(OccciMerisClassificationOp.class),
//                                     waterClassificationParameters, classificationInputProducts);
//        }
//    }
//
//    private void setMerisStandardClassificationParameters() {
//        waterClassificationParameters = new HashMap<>();
//        waterClassificationParameters.put("copyAllTiePoints", true);
//        waterClassificationParameters.put("outputSchillerNNValue", outputSchillerMerisNNValue);
//        waterClassificationParameters.put("ccSchillerNNCloudAmbiguousLowerBoundaryValue",
//                                          schillerMerisNNCloudAmbiguousLowerBoundaryValue);
//        waterClassificationParameters.put("ccSchillerNNCloudAmbiguousSureSeparationValue",
//                                          schillerMerisNNCloudAmbiguousSureSeparationValue);
//        waterClassificationParameters.put("ccSchillerNNCloudSureSnowSeparationValue",
//                                          schillerMerisNNCloudSureSnowSeparationValue);
//    }
//
//    private void setMerisSeaIceClassificationParameters() {
//        waterClassificationParameters = new HashMap<>();
//        waterClassificationParameters.put("copyAllTiePoints", true);
//        waterClassificationParameters.put("schillerMeris1600Threshold", schillerMeris1600Threshold);
//        waterClassificationParameters.put("schillerMerisAatsrCloudIceSeparationValue", schillerMerisAatsrCloudIceSeparationValue);
//    }
//
//
//    private void addBandsToTargetProduct(Product targetProduct) {
////        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
//
//        if (ocOutputMerisRadiance) {
//            copySourceBands(sourceProduct, targetProduct, "radiance_");
//        }
//        if (ocOutputRad2Refl) {
//            copySourceBands(rad2reflProduct, targetProduct, "RefSB");
//        }
//        if (ocOutputEmissive) {
//            copySourceBands(rad2reflProduct, targetProduct, "Emissive");
//        }
//        if (ocOutputSeawifsRadiance) {
////            copySourceBands(rad2reflProduct, targetProduct, "L_");
//            copySourceBands(rad2reflProduct, targetProduct, "Lt_");
//        }
//
//        if (ocOutputDebug) {
//            copySourceBands(classifProduct, targetProduct, "_value");
//        }
//        if (outputSchillerMerisNNValue) {
//            copySourceBands(classifProduct, targetProduct, OccciConstants.SCHILLER_NN_OUTPUT_BAND_NAME);
//        }
//
//        if (ocOutputSeawifsRefl) {
//            copySourceBands(classifProduct, targetProduct, "_refl");
//        }
//
//        if (ocOutputGeometry) {
//            copySourceBands(rad2reflProduct, targetProduct, "sol");       // SeaWiFS
//            copySourceBands(rad2reflProduct, targetProduct, "sen");       // SeaWiFS
//        }
//    }
//
//    private static void copySourceBands(Product rad2reflProduct, Product targetProduct, String bandNameSubstring) {
//        for (String bandname : rad2reflProduct.getBandNames()) {
//            if (bandname.contains(bandNameSubstring) && !targetProduct.containsBand(bandname)) {
//                System.out.println("copy band: " + bandname);
//                ProductUtils.copyBand(bandname, rad2reflProduct, targetProduct, true);
//            }
//        }
//    }
//
//
//    /**
//     * The Service Provider Interface (SPI) for the operator.
//     * It provides operator meta-data and is a factory for new operator instances.
//     */
//    public static class Spi extends OperatorSpi {
//
//        public Spi() {
//            super(OccciOp.class);
//        }
//    }
//}
