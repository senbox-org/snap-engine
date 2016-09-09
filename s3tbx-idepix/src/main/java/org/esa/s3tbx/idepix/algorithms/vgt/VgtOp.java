package org.esa.s3tbx.idepix.algorithms.vgt;

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
import org.esa.snap.core.util.ProductUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * The Idepix pixel classification for SPOT-VGT products
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Vgt",
        category = "Optical/Pre-Processing",
        internal = true, // todo: remove when activated
        version = "1.0",
        authors = "Olaf Danne",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Pixel identification and classification for SPOT-VGT.")
public class VgtOp extends BasisOp {

    @Parameter(defaultValue = "true",
            label = " Write TOA reflectances to the target product",
            description = " Write TOA reflectances to the target product")
    boolean copyToaReflectances = true;

    @Parameter(defaultValue = "false",
            label = " Write input annotation bands to the target product",
            description = " Write input annotation bands to the target product")
    boolean copyAnnotations;

    @Parameter(defaultValue = "false",
            label = " Write feature values to the target product",
            description = " Write all feature values to the target product")
    boolean copyFeatureValues = false;

    @Parameter(defaultValue = "false",
            label = " Write NN value to the target product.",
            description = " If applied, write NN value to the target product ")
    private boolean outputSchillerNNValue;

    @Parameter(defaultValue = "1.1",
            label = " NN cloud ambiguous lower boundary",
            description = " NN cloud ambiguous lower boundary")
    private double nnCloudAmbiguousLowerBoundaryValue;

    @Parameter(defaultValue = "2.7",
            label = " NN cloud ambiguous/sure separation value",
            description = " NN cloud ambiguous cloud ambiguous/sure separation value")
    private double nnCloudAmbiguousSureSeparationValue;

    @Parameter(defaultValue = "4.6",
            label = " NN cloud sure/snow separation value",
            description = " NN cloud ambiguous cloud sure/snow separation value")
    private double nnCloudSureSnowSeparationValue;

    @Parameter(defaultValue = "true", label = " Compute a cloud buffer")
    private boolean computeCloudBuffer;

    @Parameter(defaultValue = "2", interval = "[0,100]",
            description = "The width of a cloud 'safety buffer' around a pixel which was classified as cloudy.",
            label = "Width of cloud buffer (# of pixels)")
    private int cloudBufferWidth;

    @Parameter(defaultValue = "50", valueSet = {"50", "150"},
            label = " Resolution of used land-water mask in m/pixel",
            description = "Resolution of the used SRTM land-water mask in m/pixel")
    private int wmResolution;

    @Parameter(defaultValue = "false",
            label = " Use land-water flag from L1b product instead",
            description = "Use land-water flag from L1b product instead of SRTM mask")
    private boolean useL1bLandWaterFlag;


    @SourceProduct(alias = "l1bProduct",
            label = "L1b product",
            description = "The SPOT-VGT L1b product.")
    private Product sourceProduct;

    // skip for the moment, clarify if needed
//    @SourceProduct(alias = "urbanProduct", optional = true,
//            label = "ProbaV or VGT urban product",
//            description = "Urban product (only considered for Proba-V and VGT classification, otherwise ignored).")
//    private Product urbanProduct;

    @TargetProduct(description = "The target product.")
    private Product targetProduct;

    private Product cloudProduct;
    private Product postProcessingProduct;

    @Override
    public void initialize() throws OperatorException {
        final boolean inputProductIsValid = IdepixUtils.validateInputProduct(sourceProduct, AlgorithmSelector.VGT);
//        sourceProduct.setPreferredTileSize(sourceProduct.getSceneRasterWidth(), 16); // test
        if (!inputProductIsValid) {
            throw new OperatorException(IdepixConstants.INPUT_INCONSISTENCY_ERROR_MESSAGE);
        }
        processGlobAlbedoVgt();

        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);   // we need the L1b flag!
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        VgtUtils.setupVgtBitmasks(targetProduct);
    }

    private void processGlobAlbedoVgt() {
        // Water mask
        HashMap<String, Object> waterMaskParameters = new HashMap<>();
        waterMaskParameters.put("resolution", VgtConstants.LAND_WATER_MASK_RESOLUTION);
        waterMaskParameters.put("subSamplingFactorX", VgtConstants.OVERSAMPLING_FACTOR_X);
        waterMaskParameters.put("subSamplingFactorY", VgtConstants.OVERSAMPLING_FACTOR_Y);
        Product waterMaskProduct = GPF.createProduct("LandWaterMask", waterMaskParameters, sourceProduct);

        // Cloud Classification
        Map<String, Product> classificationInputProducts = new HashMap<>(4);
        classificationInputProducts.put("l1b", sourceProduct);
        classificationInputProducts.put("waterMask", waterMaskProduct);

        Map<String, Object> cloudClassificationParameters = createVgtCloudClassificationParameters();

        cloudProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(VgtClassificationOp.class),
                                         cloudClassificationParameters, classificationInputProducts);

        computeVgtPostProcessProduct();

        targetProduct = IdepixUtils.cloneProduct(cloudProduct, true);

        Band cloudFlagBand = targetProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS);
        cloudFlagBand.setSourceImage(postProcessingProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS).getSourceImage());
    }

    private Map<String, Object> createVgtCloudClassificationParameters() {
        Map<String, Object> cloudClassificationParameters = new HashMap<>(1);
        cloudClassificationParameters.put("copyToaReflectances", copyToaReflectances);
        cloudClassificationParameters.put("copyAnnotations", copyAnnotations);
        cloudClassificationParameters.put("copyFeatureValues", copyFeatureValues);
        cloudClassificationParameters.put("outputSchillerNNValue", outputSchillerNNValue);
        cloudClassificationParameters.put("useL1bLandWaterFlag", useL1bLandWaterFlag);
        cloudClassificationParameters.put("nnCloudAmbiguousLowerBoundaryValue", nnCloudAmbiguousLowerBoundaryValue);
        cloudClassificationParameters.put("nnCloudAmbiguousSureSeparationValue", nnCloudAmbiguousSureSeparationValue);
        cloudClassificationParameters.put("nnCloudSureSnowSeparationValue", nnCloudSureSnowSeparationValue);

        return cloudClassificationParameters;
    }

    private void computeVgtPostProcessProduct() {
        HashMap<String, Product> input = new HashMap<>();
        input.put("l1b", sourceProduct);
        input.put("vgtCloud", cloudProduct);

        Map<String, Object> params = new HashMap<>();
        params.put("computeCloudBuffer", computeCloudBuffer);
        params.put("cloudBufferWidth", cloudBufferWidth);
        postProcessingProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(VgtPostProcessOp.class),
                                                            params, input);
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(VgtOp.class);
        }
    }
}
