package org.esa.s3tbx.idepix.algorithms.probav;

import org.esa.s3tbx.idepix.core.AlgorithmSelector;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixIO;
import org.esa.s3tbx.idepix.operators.BasisOp;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;

import java.util.HashMap;
import java.util.Map;

/**
 * The Idepix pixel classification for PROBA-V Synthesis products
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Probav",
        category = "Optical/Pre-Processing",
        internal = true, // todo: remove when activated
        version = "2.2",
        authors = "Olaf Danne",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Pixel identification and classification for PROBA-V.")
public class ProbaVOp extends BasisOp {

    @Parameter(defaultValue = "false",
            label = " Write TOA Reflectances to the target product",
            description = " Write TOA Reflectances to the target product")
    private boolean copyToaReflectances = false;

    @Parameter(defaultValue = "false",
            label = " Write Feature Values to the target product",
            description = " Write all Feature Values to the target product")
    private boolean copyFeatureValues = false;

    @Parameter(defaultValue = "false",
            label = " Write input annotation bands to the target product",
            description = " Write input annotation bands to the target product")
    private boolean copyAnnotations;

    @Parameter(defaultValue = "false",
            label = " Apply NN for cloud classification",
            description = " Apply NN for cloud classification")
    private boolean applySchillerNN;

    @Parameter(defaultValue = "1.1",
            label = " NN cloud ambiguous lower boundary",
            description = " NN cloud ambiguous lower boundary")
    private double schillerNNCloudAmbiguousLowerBoundaryValue;

    @Parameter(defaultValue = "2.7",
            label = " NN cloud ambiguous/sure separation value",
            description = " NN cloud ambiguous cloud ambiguous/sure separation value")
    private double schillerNNCloudAmbiguousSureSeparationValue;

    @Parameter(defaultValue = "4.6",
            label = " NN cloud sure/snow separation value",
            description = " NN cloud ambiguous cloud sure/snow separation value")
    private double schillerNNCloudSureSnowSeparationValue;

    @Parameter(defaultValue = "true", label = " Compute a cloud buffer")
    private boolean computeCloudBuffer;

    @Parameter(defaultValue = "2", interval = "[0,100]",
            label = " Width of cloud buffer (# of pixels)",
            description = " The width of the 'safety buffer' around a pixel identified as cloudy.")
    private int cloudBufferWidth;

    @Parameter(defaultValue = "false",
            label = " Use land-water flag from L1b product instead",
            description = "Use land-water flag from L1b product instead of SRTM mask")
    private boolean useL1bLandWaterFlag;


    @SourceProduct(alias = "l1bProduct",
            label = "OLCI L1b product",
            description = "The OLCI L1b source product.")
    private Product sourceProduct;

    private Product cloudProduct;
    private Product postProcessingProduct;

    @Override
    public void initialize() throws OperatorException {
        final boolean inputProductIsValid = IdepixIO.validateInputProduct(sourceProduct, AlgorithmSelector.PROBAV);
        if (!inputProductIsValid) {
            throw new OperatorException(IdepixConstants.INPUT_INCONSISTENCY_ERROR_MESSAGE);
        }

        processProbav();
    }

    private void processProbav() {
        HashMap<String, Object> waterMaskParameters = new HashMap<>();
        waterMaskParameters.put("resolution", IdepixConstants.LAND_WATER_MASK_RESOLUTION);
        waterMaskParameters.put("subSamplingFactorX", IdepixConstants.OVERSAMPLING_FACTOR_X);
        waterMaskParameters.put("subSamplingFactorY", IdepixConstants.OVERSAMPLING_FACTOR_Y);
        Product waterMaskProduct = GPF.createProduct("LandWaterMask", waterMaskParameters, sourceProduct);

        // Cloud Classification
        Map<String, Product> cloudInput = new HashMap<>(4);
        cloudInput.put("l1b", sourceProduct);
        cloudInput.put("waterMask", waterMaskProduct);

        Map<String, Object> cloudClassificationParameters = createCloudClassificationParameters();

        cloudProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ProbaVClassificationOp.class),
                                         cloudClassificationParameters, cloudInput);

        computePostProcessProduct();

        Product targetProduct = IdepixIO.cloneProduct(cloudProduct, true);

        Band cloudFlagBand = targetProduct.getBand(IdepixIO.IDEPIX_CLASSIF_FLAGS);
        cloudFlagBand.setSourceImage(postProcessingProduct.getBand(IdepixIO.IDEPIX_CLASSIF_FLAGS).getSourceImage());
    }

    private void computePostProcessProduct() {
        // Post Cloud Classification: flag consolidation, cloud shadow, cloud buffer
        HashMap<String, Product> input = new HashMap<>();
        input.put("l1b", sourceProduct);
        input.put("probavCloud", cloudProduct);

        Map<String, Object> params = new HashMap<>();
        params.put("computeCloudBuffer", computeCloudBuffer);
        params.put("cloudBufferWidth", cloudBufferWidth);
        postProcessingProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ProbaVPostProcessOp.class),
                                                            params, input);
    }

    private Map<String, Object> createCloudClassificationParameters() {
        Map<String, Object> cloudClassificationParameters = new HashMap<>(1);
        cloudClassificationParameters.put("copyToaReflectances", copyToaReflectances);
        cloudClassificationParameters.put("copyFeatureValues", copyFeatureValues);
        cloudClassificationParameters.put("useL1bLandWaterFlag", useL1bLandWaterFlag);
        cloudClassificationParameters.put("copyAnnotations", copyAnnotations);
        cloudClassificationParameters.put("applySchillerNN", applySchillerNN);
        cloudClassificationParameters.put("schillerNNCloudAmbiguousLowerBoundaryValue",
                                            schillerNNCloudAmbiguousLowerBoundaryValue);
        cloudClassificationParameters.put("schillerNNCloudAmbiguousSureSeparationValue",
                                            schillerNNCloudAmbiguousSureSeparationValue);
        cloudClassificationParameters.put("schillerNNCloudSureSnowSeparationValue",
                                            schillerNNCloudSureSnowSeparationValue);

        return cloudClassificationParameters;
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ProbaVOp.class);
        }
    }
}
