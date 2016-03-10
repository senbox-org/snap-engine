package org.esa.s3tbx.idepix.algorithms.avhrr;

import org.esa.s3tbx.idepix.core.AlgorithmSelector;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixUtils;
import org.esa.s3tbx.idepix.operators.BasisOp;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;

import java.util.HashMap;
import java.util.Map;

/**
 * The Idepix pixel classification for AVHRR products
 *
 * @author olafd
 */
@SuppressWarnings({"FieldCanBeLocal"})
@OperatorMetadata(alias = "Idepix.Avhrr",
        internal = true, // todo: remove when activated
        category = "Optical/Pre-Processing",
        version = "1.0",
        authors = "Olaf Danne, Grit Kirches",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Pixel identification and classification for AVHRR.")
public class AvhrrOp extends BasisOp {

    private static final int LAND_WATER_MASK_RESOLUTION = 50;
    private static final int OVERSAMPLING_FACTOR_X = 3;
    private static final int OVERSAMPLING_FACTOR_Y = 3;

    @SourceProduct(alias = "sourceProduct",
            label = "Landsat 8 product",
            description = "The Landsat 8 source product.")
    private Product sourceProduct;

    @TargetProduct(description = "The target product.")
    private Product targetProduct;

    private Product classificationProduct;
    private Product postProcessingProduct;
    private Product waterMaskProduct;


    @Parameter(defaultValue = "false", label = " Copy input radiance/reflectance bands")
    private boolean aacCopyRadiances = false;

    @Parameter(defaultValue = "true",
            label = " Compute a cloud buffer")
    private boolean computeCloudBuffer;

    @Parameter(defaultValue = "2", label = " Width of cloud buffer (# of pixels)")
    private int cloudBufferWidth;

    @Parameter(defaultValue = "true",
            label = " Refine pixel classification near coastlines",
            description = "Refine pixel classification near coastlines. ")
    private boolean refineClassificationNearCoastlines;

    @Parameter(defaultValue = "2.15",
            label = " Schiller NN cloud ambiguous lower boundary ",
            description = " Schiller NN cloud ambiguous lower boundary ")
    double avhrracSchillerNNCloudAmbiguousLowerBoundaryValue;

    @Parameter(defaultValue = "3.45",
            label = " Schiller NN cloud ambiguous/sure separation value ",
            description = " Schiller NN cloud ambiguous cloud ambiguous/sure separation value ")
    double avhrracSchillerNNCloudAmbiguousSureSeparationValue;

    @Parameter(defaultValue = "4.45",
            label = " Schiller NN cloud sure/snow separation value ",
            description = " Schiller NN cloud ambiguous cloud sure/snow separation value ")
    double avhrracSchillerNNCloudSureSnowSeparationValue;

    private Map<String, Object> aacCloudClassificationParameters;

    @Override
    public void initialize() throws OperatorException {
        final boolean inputProductIsValid = IdepixUtils.validateInputProduct(sourceProduct, AlgorithmSelector.AVHRR);
        if (!inputProductIsValid) {
            throw new OperatorException(IdepixConstants.INPUT_INCONSISTENCY_ERROR_MESSAGE);
        }

        aacCloudClassificationParameters = createAacCloudClassificationParameters();
        processAvhrrAc();
    }

    private Map<String, Object> createAacCloudClassificationParameters() {
        Map<String, Object> aacCloudClassificationParameters = new HashMap<>(1);
        aacCloudClassificationParameters.put("aacCopyRadiances", aacCopyRadiances);
        aacCloudClassificationParameters.put("aacCloudBufferWidth", cloudBufferWidth);
//        aacCloudClassificationParameters.put("wmResolution", 50);
//        aacCloudClassificationParameters.put("aacUseWaterMaskFraction", true);
        aacCloudClassificationParameters.put("avhrracSchillerNNCloudAmbiguousLowerBoundaryValue",
                                             avhrracSchillerNNCloudAmbiguousLowerBoundaryValue);
        aacCloudClassificationParameters.put("avhrracSchillerNNCloudAmbiguousSureSeparationValue",
                                             avhrracSchillerNNCloudAmbiguousSureSeparationValue);
        aacCloudClassificationParameters.put("avhrracSchillerNNCloudSureSnowSeparationValue",
                                             avhrracSchillerNNCloudSureSnowSeparationValue);

        return aacCloudClassificationParameters;
    }


    private void processAvhrrAc() {
        AbstractAvhrrClassificationOp acClassificationOp = new AvhrrUSGSClassificationOp();

        acClassificationOp.setParameterDefaultValues();
        for (String key : aacCloudClassificationParameters.keySet()) {
            acClassificationOp.setParameter(key, aacCloudClassificationParameters.get(key));
        }
        acClassificationOp.setSourceProduct("aacl1b", sourceProduct);
        createWaterMaskProduct();
        acClassificationOp.setSourceProduct("waterMask", waterMaskProduct);

        classificationProduct = acClassificationOp.getTargetProduct();
        postProcess();

        targetProduct = IdepixUtils.cloneProduct(classificationProduct, true);
        targetProduct.setName(sourceProduct.getName() + ".idepix");

        Band cloudFlagBand;
        cloudFlagBand = targetProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS);
        cloudFlagBand.setSourceImage(postProcessingProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS).getSourceImage());

    }

    private void postProcess() {
        HashMap<String, Product> input = new HashMap<>();
        input.put("l1b", sourceProduct);
        input.put("avhrrCloud", classificationProduct);
        input.put("waterMask", waterMaskProduct);
        Map<String, Object> params = new HashMap<>();
        params.put("cloudBufferWidth", cloudBufferWidth);
        params.put("computeCloudBuffer", computeCloudBuffer);
        params.put("computeCloudShadow", false);     // todo: we need algo
        params.put("refineClassificationNearCoastlines", refineClassificationNearCoastlines);  // always an improvement, but time consuming
        postProcessingProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(AvhrrPostProcessOp.class), params, input);
    }

    private void createWaterMaskProduct() {
//        HashMap<String, Object> waterParameters = new HashMap<>();
//        waterParameters.put("resolution", 50);
//        waterParameters.put("subSamplingFactorX", 3);
//        waterParameters.put("subSamplingFactorY", 3);
//        waterMaskProduct = GPF.createProduct("LandWaterMask", waterParameters, sourceProduct);
        HashMap<String, Object> waterMaskParameters = new HashMap<>();
        final String[] sourceBandNames = {AvhrrConstants.AVHRR_AC_ALBEDO_1_BAND_NAME};
        waterMaskParameters.put("sourceBandNames", sourceBandNames);
        waterMaskParameters.put("landMask", false);
        waterMaskProduct = GPF.createProduct("LandWaterMask", waterMaskParameters, sourceProduct);
    }


    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AvhrrOp.class);
        }
    }
}
