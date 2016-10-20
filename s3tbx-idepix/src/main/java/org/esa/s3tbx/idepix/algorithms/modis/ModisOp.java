package org.esa.s3tbx.idepix.algorithms.modis;

import org.esa.s3tbx.idepix.core.AlgorithmSelector;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixIO;
import org.esa.s3tbx.idepix.operators.BasisOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Idepix operator for pixel identification and classification for MODIS
 *
 * @author olafd
 */
@SuppressWarnings({"FieldCanBeLocal"})
@OperatorMetadata(alias = "Idepix.Modis",
        category = "Optical/Pre-Processing",
        version = "2.2",
        authors = "Olaf Danne, Marco Zuehlke",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Pixel identification and classification for MODIS.")
public class ModisOp extends BasisOp {

    @Parameter(defaultValue = "CLEAR_SKY_CONSERVATIVE",
            valueSet = {"CLEAR_SKY_CONSERVATIVE", "CLOUD_CONSERVATIVE"},
            label = " Strength of cloud flagging",
            description = "Strength of cloud flagging. In case of 'CLOUD_CONSERVATIVE', more pixels might be flagged as cloud.")
    private String cloudFlaggingStrength;

    @Parameter(defaultValue = "1", label = " Width of cloud buffer (# of pixels)")
    private int cloudBufferWidth;

    @Parameter(defaultValue = "150", valueSet = {"1000", "150", "50"},
            label = " Resolution of land-water mask (m/pixel)",
            description = "Resolution of used land-water mask in meters per pixel")
    private int waterMaskResolution;

    @Parameter(defaultValue = "true",
            label = " Write reflective solar bands",
            description = "Write TOA reflective solar bands (RefSB) to target product.")
    private boolean outputRad2Refl = true;

    @Parameter(defaultValue = "false",
            label = " Write emissive bands",
            description = "Write 'Emissive' bands to target product.")
    private boolean outputEmissive = false;


    @SourceProduct(alias = "source", label = "Name (MODIS L1b product)", description = "The source product.")
    private Product sourceProduct;

    private Product waterMaskProduct;
    private Product classifProduct;
    private Map<String, Object> waterClassificationParameters;

    private boolean applyOrLogicInCloudTest;

    // former user options, now fix
    private boolean applyBrightnessTest = true;
    private final double glintThresh859 = 0.15;


    @Override
    public void initialize() throws OperatorException {
        applyOrLogicInCloudTest = cloudFlaggingStrength.equals("CLOUD_CONSERVATIVE");

        final boolean inputProductIsValid = IdepixIO.validateInputProduct(sourceProduct, AlgorithmSelector.MODIS);
        if (!inputProductIsValid) {
            throw new OperatorException(IdepixConstants.INPUT_INCONSISTENCY_ERROR_MESSAGE);
        }

        processModis(createModisPixelClassificationParameters());
    }

    private void processModis(Map<String, Object> occciCloudClassificationParameters) {
        Map<String, Product> occciClassifInput = new HashMap<>(4);
        computeAlgorithmInputProducts(occciClassifInput);

        // post processing input:
        // - cloud buffer
        // - cloud shadow todo (currently exisis only for Meris)
        Map<String, Object> postProcessParameters = new HashMap<>();
        postProcessParameters.put("cloudBufferWidth", cloudBufferWidth);
        Map<String, Product> postProcessInput = new HashMap<>();
        postProcessInput.put("waterMask", waterMaskProduct);

        postProcessInput.put("refl", sourceProduct);
        classifProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ModisClassificationOp.class),
                                           occciCloudClassificationParameters, occciClassifInput);

        postProcessInput.put("classif", classifProduct);

        Product postProcessProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ModisPostProcessingOp.class),
                                                       postProcessParameters, postProcessInput);

        ProductUtils.copyMetadata(sourceProduct,postProcessProduct);
        setTargetProduct(postProcessProduct);
        addBandsToTargetProduct(postProcessProduct);
    }

    private void computeAlgorithmInputProducts(Map<String, Product> occciClassifInput) {
        createWaterMaskProduct();
        occciClassifInput.put("waterMask", waterMaskProduct);
        occciClassifInput.put("refl", sourceProduct);
    }


    private void createWaterMaskProduct() {
        HashMap<String, Object> waterParameters = new HashMap<>();
        waterParameters.put("resolution", waterMaskResolution);
        waterParameters.put("subSamplingFactorX", 3);
        waterParameters.put("subSamplingFactorY", 3);
        waterMaskProduct = GPF.createProduct("LandWaterMask", waterParameters, sourceProduct);
    }

    private Map<String, Object> createModisPixelClassificationParameters() {
        Map<String, Object> occciCloudClassificationParameters = new HashMap<>(1);
        occciCloudClassificationParameters.put("cloudBufferWidth", cloudBufferWidth);
        occciCloudClassificationParameters.put("wmResolution", waterMaskResolution);
        occciCloudClassificationParameters.put("applyBrightnessTest", applyBrightnessTest);
        occciCloudClassificationParameters.put("glintThresh859", glintThresh859);
        occciCloudClassificationParameters.put("applyOrLogicInCloudTest", applyOrLogicInCloudTest);

        return occciCloudClassificationParameters;
    }

    private void addBandsToTargetProduct(Product targetProduct) {
        if (outputRad2Refl) {
            copySourceBands(sourceProduct, targetProduct, "RefSB");
        }
        if (outputEmissive) {
            copySourceBands(sourceProduct, targetProduct, "Emissive");
        }
    }

    private static void copySourceBands(Product rad2reflProduct, Product targetProduct, String bandNameSubstring) {
        for (String bandname : rad2reflProduct.getBandNames()) {
            if (bandname.contains(bandNameSubstring) && !targetProduct.containsBand(bandname)) {
                ProductUtils.copyBand(bandname, rad2reflProduct, targetProduct, true);
            }
        }
    }


    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ModisOp.class);
        }
    }
}
