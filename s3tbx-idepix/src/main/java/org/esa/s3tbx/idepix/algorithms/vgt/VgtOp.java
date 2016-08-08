package org.esa.s3tbx.idepix.algorithms.vgt;

import org.esa.s3tbx.idepix.core.AlgorithmSelector;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixUtils;
import org.esa.s3tbx.idepix.operators.BasisOp;
import org.esa.s3tbx.idepix.operators.CloudBufferOp;
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
            label = " Write TOA Reflectances to the target product",
            description = " Write TOA Reflectances to the target product")
    boolean copyToaReflectances = true;

    @Parameter(defaultValue = "false",
            label = " Write input annotation bands to the target product (VGT only)",
            description = " Write input annotation bands to the target product (has only effect for VGT L1b products)")
    boolean copyAnnotations;

    @Parameter(defaultValue = "false",
            label = " Write NN value to the target product.",
            description = " If applied, write NN value to the target product ")
    private boolean outputSchillerNNValue;

    @Parameter(defaultValue = "1.1",
            label = " NN cloud ambiguous lower boundary (VGT only)",
            description = " NN cloud ambiguous lower boundary (has only effect for VGT L1b products)")
    private double nnCloudAmbiguousLowerBoundaryValue;

    @Parameter(defaultValue = "2.7",
            label = " NN cloud ambiguous/sure separation value (VGT only)",
            description = " NN cloud ambiguous cloud ambiguous/sure separation value (has only effect for VGT L1b products)")
    private double nnCloudAmbiguousSureSeparationValue;

    @Parameter(defaultValue = "4.6",
            label = " NN cloud sure/snow separation value (VGT only)",
            description = " NN cloud ambiguous cloud sure/snow separation value (has only effect for VGT L1b products)")
    private double nnCloudSureSnowSeparationValue;

    @Parameter(defaultValue = "true", label = " Compute a cloud buffer")
    private boolean computeCloudBuffer;

    @Parameter(defaultValue = "2", interval = "[0,100]",
            description = "The width of a cloud 'safety buffer' around a pixel which was classified as cloudy.",
            label = "Width of cloud buffer (# of pixels)")
    private int cloudBufferWidth;

    @Parameter(defaultValue = "false",
            label = " Compute cloud shadow",
            description = " Compute cloud shadow with the algorithm from 'Fronts' project")
    private boolean computeCloudShadow;

    @Parameter(defaultValue = "true",
            label = " Refine pixel classification near coastlines",
            description = "Refine pixel classification near coastlines. ")
    private boolean refineClassificationNearCoastlines;

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
            description = "The MERIS or SPOT-VGT L1b product.")
    private Product sourceProduct;

    // skip for the moment, clarify if needed
//    @SourceProduct(alias = "urbanProduct", optional = true,
//            label = "ProbaV or VGT urban product",
//            description = "Urban product (only considered for Proba-V and VGT classification, otherwise ignored).")
//    private Product urbanProduct;

    @TargetProduct(description = "The target product.")
    private Product targetProduct;

    private Map<String, Object> cloudClassificationParameters;
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
    }

    private void processGlobAlbedoVgt() {
        // Cloud Classification
        Map<String, Product> gaCloudInput = new HashMap<>(4);
        gaCloudInput.put("gal1b", sourceProduct);

        cloudClassificationParameters = createVgtCloudClassificationParameters();

        cloudProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(VgtClassificationOp.class),
                                           cloudClassificationParameters, gaCloudInput);

//        targetProduct = gaCloudProduct;
        // introduce post-processing as for Proba-V (request GK/JM 20160416)
        if (computeCloudBuffer || computeCloudShadow || refineClassificationNearCoastlines) {
            // Post Cloud Classification: coastline refinement, cloud shadow, cloud buffer
            computeVgtPostProcessProduct();

            targetProduct = IdepixUtils.cloneProduct(cloudProduct, true);

            Band cloudFlagBand = targetProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS);
            cloudFlagBand.setSourceImage(postProcessingProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS).getSourceImage());
        } else {
            targetProduct = cloudProduct;
        }
    }

    private Map<String, Object> createVgtCloudClassificationParameters() {
        Map<String, Object> gaCloudClassificationParameters = new HashMap<>(1);
        gaCloudClassificationParameters.put("copyToaReflectances", copyToaReflectances);
        gaCloudClassificationParameters.put("copyAnnotations", copyAnnotations);
        gaCloudClassificationParameters.put("outputSchillerNNValue", outputSchillerNNValue);
        gaCloudClassificationParameters.put("useL1bLandWaterFlag", useL1bLandWaterFlag);
        gaCloudClassificationParameters.put("nnCloudAmbiguousLowerBoundaryValue", nnCloudAmbiguousLowerBoundaryValue);
        gaCloudClassificationParameters.put("nnCloudAmbiguousSureSeparationValue", nnCloudAmbiguousSureSeparationValue);
        gaCloudClassificationParameters.put("nnCloudSureSnowSeparationValue", nnCloudSureSnowSeparationValue);

        return gaCloudClassificationParameters;
    }

    private void computeVgtPostProcessProduct() {
        HashMap<String, Product> input = new HashMap<>();
        input.put("l1b", sourceProduct);
        input.put("vgtCloud", cloudProduct);

        // skip for the moment, clarify if needed
//        final boolean isUrbanProductValid = isVgtUrbanProductValid(sourceProduct, urbanProduct);
//        final Product validUrbanProduct = isUrbanProductValid ? urbanProduct : null;
//        input.put("urban", validUrbanProduct);

        Map<String, Object> params = new HashMap<>();
        final Product classifiedProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(VgtPostProcessOp.class),
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
