package org.esa.s3tbx.idepix.algorithms.probav;

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
import org.esa.snap.core.util.io.FileUtils;

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

    @Parameter(defaultValue = "true",
            label = " Write TOA Reflectances to the target product",
            description = " Write TOA Reflectances to the target product")
    private boolean copyToaReflectances = true;

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

    @Parameter(defaultValue = "false",
            label = " Compute cloud shadow",
            description = " Compute cloud shadow with the algorithm from 'Fronts' project")
    private boolean computeCloudShadow;

    @Parameter(defaultValue = "true", label = " Compute a cloud buffer")
    private boolean computeCloudBuffer;

    @Parameter(defaultValue = "2", interval = "[0,100]",
            label = " Width of cloud buffer (# of pixels)",
            description = " The width of the 'safety buffer' around a pixel identified as cloudy.")
    private int cloudBufferWidth;

    @Parameter(defaultValue = "false", label = " Use the LandCover advanced cloud buffer algorithm")
    private boolean useLcCloudBuffer;

    @Parameter(defaultValue = "false",
            label = " Use land-water flag from L1b product instead",
            description = "Use land-water flag from L1b product instead of SRTM mask")
    private boolean useL1bLandWaterFlag;


    @SourceProduct(alias = "l1bProduct",
            label = "OLCI L1b product",
            description = "The OLCI L1b source product.")
    private Product sourceProduct;

    @TargetProduct(description = "The target product.")
    private Product targetProduct;

    private Map<String, Object> cloudClassificationParameters;

    private Product cloudProduct;
    private Product waterMaskProduct;
    private Product postProcessingProduct;

    @Override
    public void initialize() throws OperatorException {
        final boolean inputProductIsValid = IdepixUtils.validateInputProduct(sourceProduct, AlgorithmSelector.PROBAV);
        sourceProduct.setPreferredTileSize(sourceProduct.getSceneRasterWidth(), 16); // test
        if (!inputProductIsValid) {
            throw new OperatorException(IdepixConstants.INPUT_INCONSISTENCY_ERROR_MESSAGE);
        }

        preProcess();
        processGlobAlbedoProbav();
    }

    private void preProcess() {
        HashMap<String, Object> waterMaskParameters = new HashMap<>();
        waterMaskParameters.put("resolution", ProbaVConstants.LAND_WATER_MASK_RESOLUTION);
        waterMaskParameters.put("subSamplingFactorX", ProbaVConstants.OVERSAMPLING_FACTOR_X);
        waterMaskParameters.put("subSamplingFactorY", ProbaVConstants.OVERSAMPLING_FACTOR_Y);
        waterMaskProduct = GPF.createProduct("LandWaterMask", waterMaskParameters, sourceProduct);
    }

    private void processGlobAlbedoProbav() {
        // Cloud Classification
        Map<String, Product> gaCloudInput = new HashMap<>(4);
        gaCloudInput.put("l1b", sourceProduct);
        gaCloudInput.put("waterMask", waterMaskProduct);

        cloudClassificationParameters = createCloudClassificationParameters();

        cloudProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ProbaVClassificationOp.class),
                                         cloudClassificationParameters, gaCloudInput);

        if (computeCloudBuffer || computeCloudShadow) {
            // Post Cloud Classification: coastline refinement, cloud shadow, cloud buffer
            computePostProcessProduct();

            targetProduct = IdepixUtils.cloneProduct(cloudProduct, true);

            Band cloudFlagBand = targetProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS);
            cloudFlagBand.setSourceImage(postProcessingProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS).getSourceImage());
        } else {
            targetProduct = cloudProduct;
        }
    }

    private void computePostProcessProduct() {
        HashMap<String, Product> input = new HashMap<>();
        input.put("l1b", sourceProduct);
        input.put("probavCloud", cloudProduct);

        Map<String, Object> params = new HashMap<>();
        final Product classifiedProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ProbaVPostProcessOp.class),
                                                            params, input);

        if (computeCloudBuffer) {
            input = new HashMap<>();
            input.put("classifiedProduct", classifiedProduct);
            params = new HashMap<>();
            params.put("cloudBufferWidth", cloudBufferWidth);
            params.put("useLcCloudBuffer", useLcCloudBuffer);
            postProcessingProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(CloudBufferOp.class),
                                                        params, input);
        } else {
            postProcessingProduct = classifiedProduct;
        }
    }

    private Map<String, Object> createCloudClassificationParameters() {
        Map<String, Object> gaCloudClassificationParameters = new HashMap<>(1);
        gaCloudClassificationParameters.put("gaCopyToaReflectances", copyToaReflectances);
        gaCloudClassificationParameters.put("gaCopyFeatureValues", copyFeatureValues);
        gaCloudClassificationParameters.put("gaUseL1bLandWaterFlag", useL1bLandWaterFlag);
        gaCloudClassificationParameters.put("gaCopyAnnotations", copyAnnotations);
        gaCloudClassificationParameters.put("gaApplyVGTSchillerNN", applySchillerNN);
        gaCloudClassificationParameters.put("gaSchillerNNCloudAmbiguousLowerBoundaryValue",
                                            schillerNNCloudAmbiguousLowerBoundaryValue);
        gaCloudClassificationParameters.put("gaSchillerNNCloudAmbiguousSureSeparationValue",
                                            schillerNNCloudAmbiguousSureSeparationValue);
        gaCloudClassificationParameters.put("gaSchillerNNCloudSureSnowSeparationValue",
                                            schillerNNCloudSureSnowSeparationValue);
        gaCloudClassificationParameters.put("gaCloudBufferWidth", cloudBufferWidth);

        return gaCloudClassificationParameters;
    }

    // package local for testing
    static boolean isProbavUrbanProductValid(Product sourceProduct, Product urbanProduct) {
        if (urbanProduct == null) {
            return false;
        }

        // e.g. urban_mask_X00Y01.nc
        final String name = sourceProduct.getName();
        final int startIndex = name.indexOf("TOA_X") + 4;
        final String tileString = name.substring(startIndex, startIndex+6);

        final String productName = FileUtils.getFilenameWithoutExtension(urbanProduct.getName());
        final boolean valid = productName.matches("urban_mask_" + tileString);
        if (!valid) {
            System.out.println("WARNING: invalid urbanProduct '" + urbanProduct.getName() + "'");
        }

        return valid;
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
