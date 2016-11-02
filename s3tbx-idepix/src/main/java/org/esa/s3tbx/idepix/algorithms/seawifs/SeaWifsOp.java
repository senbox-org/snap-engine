package org.esa.s3tbx.idepix.algorithms.seawifs;

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
 * The Idepix pixel classification for SeaWiFS products
 *
 * @author olafd
 */
@SuppressWarnings({"FieldCanBeLocal"})
@OperatorMetadata(alias = "Idepix.Seawifs",
        internal = true, // todo: remove when activated
        category = "Optical/Pre-Processing",
        version = "2.2",
        authors = "Olaf Danne, Marco Zuehlke",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Pixel identification and classification for SeaWiFS.")
public class SeaWifsOp extends BasisOp {

    @Parameter(defaultValue = "false",
            label = " Radiance bands",
            description = "Write TOA radiance bands to target product.")
    private boolean outputRadiance = false;

    @Parameter(defaultValue = "true",
            label = " Reflectance bands",
            description = "Write TOA reflectance bands to target product.")
    private boolean outputReflectance = true;

    @Parameter(defaultValue = "true",
            label = " Geometry bands",
            description = "Write geometry bands to target product.")
    private boolean outputGeometry = true;

    @Parameter(defaultValue = "true",
            label = " Debug bands",
            description = "Write further useful bands to target product.")
    private boolean outputDebug = true;

    @Parameter(defaultValue = "L_", valueSet = {"L_", "Lt_", "rhot_"}, label = " Prefix of input spectral bands.",
            description = "Prefix of input radiance or reflectance bands")
    private String radianceBandPrefix;

    @Parameter(defaultValue = "1", label = " Width of cloud buffer (# of pixels)")
    private int cloudBufferWidth;

    @Parameter(defaultValue = "50", valueSet = {"50", "150"}, label = " Resolution of used land-water mask in m/pixel",
            description = "Resolution in m/pixel")
    private int waterMaskResolution;


    @SourceProduct(alias = "source", label = "Name (SeaWiFS L1b product)", description = "The source product.")
    private Product sourceProduct;


    private Product waterMaskProduct;
    private Product classifProduct;
    private Map<String, Object> waterClassificationParameters;

    @Override
    public void initialize() throws OperatorException {

        final boolean inputProductIsValid = IdepixIO.validateInputProduct(sourceProduct, AlgorithmSelector.SEAWIFS);
        if (!inputProductIsValid) {
            throw new OperatorException(IdepixConstants.INPUT_INCONSISTENCY_ERROR_MESSAGE);
        }

        processSeawifs(createSeawifsClassificationParameters());
    }

    private void processSeawifs(Map<String, Object> seawifsClassificationParameters) {
        Map<String, Product> seawifsClassifInput = new HashMap<>(4);
        computeAlgorithmInputProducts(seawifsClassifInput);

        // post processing input:
        // - cloud buffer
        // - cloud shadow todo (currently exisis only for Meris)
        Map<String, Object> postProcessParameters = new HashMap<>();
        postProcessParameters.put("cloudBufferWidth", cloudBufferWidth);
        Map<String, Product> postProcessInput = new HashMap<>();
        postProcessInput.put("waterMask", waterMaskProduct);

        postProcessInput.put("refl", sourceProduct);
        classifProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(SeaWifsClassificationOp.class),
                                           seawifsClassificationParameters, seawifsClassifInput);

        postProcessInput.put("classif", classifProduct);

        Product postProcessProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(SeaWifsPostProcessOp.class),
                                                       postProcessParameters, postProcessInput);

        ProductUtils.copyMetadata(sourceProduct, postProcessProduct);
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

    private Map<String, Object> createSeawifsClassificationParameters() {
        Map<String, Object> occciCloudClassificationParameters = new HashMap<>(1);
        occciCloudClassificationParameters.put("cloudBufferWidth", cloudBufferWidth);
        occciCloudClassificationParameters.put("wmResolution", waterMaskResolution);

        return occciCloudClassificationParameters;
    }

    private void addBandsToTargetProduct(Product targetProduct) {
        if (outputRadiance) {
            IdepixIO.copySourceBands(sourceProduct, targetProduct, "Lt_");
        }
        if (outputReflectance) {
            IdepixIO.copySourceBands(classifProduct, targetProduct, "_refl");
        }
        if (outputDebug) {
            IdepixIO.copySourceBands(classifProduct, targetProduct, "_value");
        }
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SeaWifsOp.class);
        }
    }
}
