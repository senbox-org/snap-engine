package org.esa.s3tbx.idepix.algorithms.modis;

import org.esa.s3tbx.idepix.core.AlgorithmSelector;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixUtils;
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
 * The Idepix pixel classification for MODIS products
 *
 * @author olafd
 */
@SuppressWarnings({"FieldCanBeLocal"})
@OperatorMetadata(alias = "idepix.modis",
        category = "Optical/Pre-Processing",
        version = "1.0",
        authors = "Olaf Danne, Marco Zuehlke",
        copyright = "(c) 2015 by Brockmann Consult",
        description = "Pixel identification and classification for MODIS.")
public class ModisOp extends BasisOp {

    @Parameter(defaultValue = "true",
            label = " Reflective solar bands (MODIS)",
            description = "Write TOA reflective solar bands (RefSB) to target product (MODIS).")
    private boolean outputRad2Refl = true;

    @Parameter(defaultValue = "false",
            label = " Emissive bands (MODIS)",
            description = "Write 'Emissive' bands to target product (MODIS).")
    private boolean outputEmissive = false;

    //    @Parameter(defaultValue = "0.15",
//               label = " Brightness test threshold (MODIS)",
//               description = "Brightness test threshold: EV_250_Aggr1km_RefSB_1 > THRESH (MODIS).")
    private double brightnessThreshCloudSure = 0.15;

    @Parameter(defaultValue = "0.15",
            label = " 'Dark glint' threshold at 859nm (MODIS)",
            description = "'Dark glint' threshold: Cloud possible only if EV_250_Aggr1km_RefSB_2 > THRESH.")
    private double glintThresh859 = 0.15;

    @Parameter(defaultValue = "true",
            label = " Apply brightness test (MODIS)",
            description = "Apply brightness test: EV_250_Aggr1km_RefSB_1 > THRESH (MODIS).")
    private boolean applyBrightnessTest = true;

    @Parameter(defaultValue = "true",
            label = " Apply 'OR' logic in cloud test (MODIS)",
            description = "Apply 'OR' logic instead of 'AND' logic in cloud test (MODIS).")
    private boolean applyOrLogicInCloudTest = true;

    //    @Parameter(defaultValue = "0.07",
//               label = " Brightness test 'cloud ambiguous' threshold (MODIS)",
//               description = "Brightness test 'cloud ambiguous' threshold: EV_250_Aggr1km_RefSB_1 > THRESH (MODIS).")
    private double brightnessThreshCloudAmbiguous = 0.125;

    @Parameter(defaultValue = "1", label = " Width of cloud buffer (# of pixels)")
    private int cloudBufferWidth;

    @Parameter(defaultValue = "50", valueSet = {"50", "150"}, label = " Resolution of used land-water mask in m/pixel",
            description = "Resolution in m/pixel")
    private int waterMaskResolution;


    @SourceProduct(alias = "source", label = "Name (MODIS L1b product)", description = "The source product.")
    private Product sourceProduct;

    private Product waterMaskProduct;
    private Product classifProduct;
    private Map<String, Object> waterClassificationParameters;


    @Override
    public void initialize() throws OperatorException {
        // todo - take from OccciOp in BEAM Idepix
        final boolean inputProductIsValid = IdepixUtils.validateInputProduct(sourceProduct, AlgorithmSelector.MODIS);
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
        occciCloudClassificationParameters.put("brightnessThreshCloudSure", brightnessThreshCloudSure);
        occciCloudClassificationParameters.put("brightnessThreshCloudAmbiguous", brightnessThreshCloudAmbiguous);
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
                System.out.println("copy band: " + bandname);
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
