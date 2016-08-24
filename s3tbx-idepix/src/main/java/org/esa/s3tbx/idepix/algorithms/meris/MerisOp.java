package org.esa.s3tbx.idepix.algorithms.meris;

import org.esa.s3tbx.idepix.core.AlgorithmSelector;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixUtils;
import org.esa.s3tbx.idepix.operators.BasisOp;
import org.esa.s3tbx.idepix.operators.CloudBufferOp;
import org.esa.s3tbx.idepix.operators.IdepixProducts;
import org.esa.s3tbx.processor.rad2refl.Sensor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * The Idepix pixel classification operator for MERIS products.
 *
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Meris",
        category = "Optical/Pre-Processing",
        internal = true, // todo: remove when activated
        version = "1.0",
        authors = "Olaf Danne",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Pixel identification and classification for MERIS.")
public class MerisOp extends BasisOp {

    @SourceProduct(alias = "l1bProduct",
            label = "MERIS L1b product",
            description = "The MERIS L1b source product.")
    private Product sourceProduct;

    @TargetProduct(description = "The target product.")
    private Product targetProduct;

    // overall parameters

    @Parameter(defaultValue = "false",
            description = "Write TOA radiances to the target product.",
            label = " Write TOA radiances to the target product")
    private boolean outputRadiance = false;

    @Parameter(defaultValue = "false",
            description = "Write TOA reflectances to the target product.",
            label = " Write TOA Reflectances to the target product")
    private boolean outputRad2Refl = false;

    @Parameter(defaultValue = "false",
            label = " Write NN value to the target product.",
            description = " If applied, write NN value to the target product ")
    private boolean outputSchillerNNValue;

    @Parameter(defaultValue = "2.0",
            label = " NN cloud ambiguous lower boundary (applied on WATER)",
            description = " NN cloud ambiguous lower boundary (applied on WATER)")
    double schillerWaterNNCloudAmbiguousLowerBoundaryValue;

    @Parameter(defaultValue = "3.7",
            label = " NN cloud ambiguous/sure separation value (applied on WATER)",
            description = " NN cloud ambiguous cloud ambiguous/sure separation value (applied on WATER)")
    double schillerWaterNNCloudAmbiguousSureSeparationValue;

    @Parameter(defaultValue = "4.05",
            label = " NN cloud sure/snow separation value (applied on WATER)",
            description = " NN cloud ambiguous cloud sure/snow separation value (applied on WATER)")
    double schillerWaterNNCloudSureSnowSeparationValue;

    @Parameter(defaultValue = "1.1",
            label = " NN cloud ambiguous lower boundary (applied on LAND)",
            description = " NN cloud ambiguous lower boundary (applied on LAND)")
    double schillerLandNNCloudAmbiguousLowerBoundaryValue;

    @Parameter(defaultValue = "2.7",
            label = " NN cloud ambiguous/sure separation value (applied on LAND)",
            description = " NN cloud ambiguous cloud ambiguous/sure separation value")
    double schillerLandNNCloudAmbiguousSureSeparationValue;

    @Parameter(defaultValue = "4.6",
            label = " NN cloud sure/snow separation value (applied on LAND)",
            description = " NN cloud ambiguous cloud sure/snow separation value")
    double schillerLandNNCloudSureSnowSeparationValue;

    @Parameter(defaultValue = "true",
            label = " Compute cloud shadow",
            description = " Compute cloud shadow with the algorithm from 'Fronts' project")
    private boolean computeCloudShadow;

    @Parameter(defaultValue = "true", label = " Compute a cloud buffer")
    private boolean computeCloudBuffer;

    @Parameter(defaultValue = "2", interval = "[0,100]",
            description = "The width of a cloud 'safety buffer' around a pixel which was classified as cloudy.",
            label = "Width of cloud buffer (# of pixels)")
    private int cloudBufferWidth;

    private Product waterClassificationProduct;
    private Product landClassificationProduct;
    private Product mergedClassificationProduct;
    private Product postProcessingProduct;

    private Product rad2reflProduct;
    private Product ctpProduct;
    private Product waterMaskProduct;

    private Map<String, Product> classificationInputProducts;
    private Map<String, Object> waterClassificationParameters;
    private Map<String, Object> landClassificationParameters;

    @Override
    public void initialize() throws OperatorException {
        System.out.println("Running IDEPIX MERIS - source product: " + sourceProduct.getName());

        final boolean inputProductIsValid = IdepixUtils.validateInputProduct(sourceProduct, AlgorithmSelector.MERIS);
        if (!inputProductIsValid) {
            throw new OperatorException(IdepixConstants.INPUT_INCONSISTENCY_ERROR_MESSAGE);
        }

        preProcess();
        computeWaterCloudProduct();
        computeLandCloudProduct();
        mergeLandWater();
        postProcess();

        targetProduct = postProcessingProduct;

        targetProduct = IdepixUtils.cloneProduct(mergedClassificationProduct, true);
        targetProduct.setAutoGrouping("radiance:reflectance");

        Band cloudFlagBand = targetProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS);
        cloudFlagBand.setSourceImage(postProcessingProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS).getSourceImage());

        copyOutputBands();
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);   // we need the L1b flag!

//        targetProduct = waterClassificationProduct;
    }

    private void preProcess() {
        rad2reflProduct = IdepixProducts.computeRadiance2ReflectanceProduct(sourceProduct, Sensor.MERIS);
        ctpProduct = IdepixProducts.computeCloudTopPressureProduct(sourceProduct);

        HashMap<String, Object> waterMaskParameters = new HashMap<>();
        waterMaskParameters.put("resolution", MerisConstants.LAND_WATER_MASK_RESOLUTION);
        waterMaskParameters.put("subSamplingFactorX", MerisConstants.OVERSAMPLING_FACTOR_X);
        waterMaskParameters.put("subSamplingFactorY", MerisConstants.OVERSAMPLING_FACTOR_Y);
        waterMaskProduct = GPF.createProduct("LandWaterMask", waterMaskParameters, sourceProduct);
    }

    private void setLandClassificationParameters() {
        landClassificationParameters = new HashMap<>();
        landClassificationParameters.put("copyAllTiePoints", true);
        landClassificationParameters.put("outputSchillerNNValue",
                                         outputSchillerNNValue);
        landClassificationParameters.put("ccSchillerNNCloudAmbiguousLowerBoundaryValue",
                                         schillerLandNNCloudAmbiguousLowerBoundaryValue);
        landClassificationParameters.put("ccSchillerNNCloudAmbiguousSureSeparationValue",
                                         schillerLandNNCloudAmbiguousSureSeparationValue);
        landClassificationParameters.put("ccSchillerNNCloudSureSnowSeparationValue",
                                         schillerLandNNCloudSureSnowSeparationValue);
    }

    private void setWaterClassificationParameters() {
        waterClassificationParameters = new HashMap<>();
        waterClassificationParameters.put("copyAllTiePoints", true);
        waterClassificationParameters.put("outputSchillerNNValue",
                                          outputSchillerNNValue);
        waterClassificationParameters.put("ccSchillerNNCloudAmbiguousLowerBoundaryValue",
                                          schillerWaterNNCloudAmbiguousLowerBoundaryValue);
        waterClassificationParameters.put("ccSchillerNNCloudAmbiguousSureSeparationValue",
                                          schillerWaterNNCloudAmbiguousSureSeparationValue);
        waterClassificationParameters.put("ccSchillerNNCloudSureSnowSeparationValue",
                                          schillerWaterNNCloudSureSnowSeparationValue);
    }

    private void computeWaterCloudProduct() {
        setWaterClassificationParameters();
        classificationInputProducts = new HashMap<>();
        classificationInputProducts.put("l1b", sourceProduct);
        classificationInputProducts.put("rhotoa", rad2reflProduct);
        classificationInputProducts.put("waterMask", waterMaskProduct);

        waterClassificationProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(MerisWaterClassificationOp.class),
                                                       waterClassificationParameters, classificationInputProducts);
    }

    private void computeLandCloudProduct() {
        setLandClassificationParameters();
        landClassificationProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(MerisLandClassificationOp.class),
                                                      landClassificationParameters, classificationInputProducts);
    }

    private void mergeLandWater() {
        Map<String, Product> mergeInputProducts = new HashMap<>();
        mergeInputProducts.put("landClassif", landClassificationProduct);
        mergeInputProducts.put("waterClassif", waterClassificationProduct);

        Map<String, Object> mergeClassificationParameters = new HashMap<>();
        mergeClassificationParameters.put("copyAllTiePoints", true);
        mergedClassificationProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(MerisMergeLandWaterOp.class),
                                                        mergeClassificationParameters, mergeInputProducts);
    }

    private void postProcess() {
        HashMap<String, Product> input = new HashMap<>();
        input.put("l1b", sourceProduct);
        input.put("merisCloud", mergedClassificationProduct);
        input.put("ctp", ctpProduct);
        input.put("waterMask", waterMaskProduct);

        Map<String, Object> params = new HashMap<>();
        params.put("computeCloudShadow", computeCloudShadow);
        params.put("refineClassificationNearCoastlines", true);  // always an improvement

        final Product classifiedProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(MerisPostProcessOp.class),
                                                            params, input);

        if (computeCloudBuffer) {
            input = new HashMap<>();
            input.put("classifiedProduct", classifiedProduct);
            params = new HashMap<>();
            params.put("cloudBufferWidth", cloudBufferWidth);
            postProcessingProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(CloudBufferOp.class),
                                                      params, input);
        } else {
            postProcessingProduct = classifiedProduct;
        }
    }

    private void copyOutputBands() {
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        MerisUtils.setupMerisBitmasks(targetProduct);
        if (outputRadiance) {
            IdepixProducts.addRadianceBands(sourceProduct, targetProduct);
        }
        if (outputRad2Refl) {
            MerisUtils.addRadiance2ReflectanceBands(rad2reflProduct, targetProduct);
        }
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MerisOp.class);
        }
    }
}
