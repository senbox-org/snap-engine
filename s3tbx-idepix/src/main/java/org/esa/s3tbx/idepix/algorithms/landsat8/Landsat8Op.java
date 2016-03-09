package org.esa.s3tbx.idepix.algorithms.landsat8;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.tools.Tools;
import org.esa.s3tbx.idepix.core.AlgorithmSelector;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixUtils;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.media.jai.RenderedOp;
import java.util.HashMap;
import java.util.Map;

/**
 * Idepix operator for pixel identification and classification for Landsat 8
 *
 * @author olafd
 */
@SuppressWarnings({"FieldCanBeLocal"})
@OperatorMetadata(alias = "Idepix.Landsat8",
        category = "Optical/Pre-Processing",
        version = "2.2",
        authors = "Olaf Danne",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Pixel identification and classification for Landsat 8.")
public class Landsat8Op extends Operator {

    @SourceProduct(alias = "sourceProduct",
            label = "Landsat 8 product",
            description = "The Landsat 8 source product.")
    private Product sourceProduct;

    @TargetProduct(description = "The target product.")
    private Product targetProduct;

    // overall parameters

    //    @Parameter(defaultValue = "false",
//            label = " Compute cloud shadow",
//            description = " Compute cloud shadow with the algorithm from 'Fronts' project")
//    private boolean computeCloudShadow;  // todo: later if we find a way how to compute
    private boolean computeCloudShadow = false;  // todo: later if we find a way how to compute
    private boolean computeCloudBuffer = true;

    @Parameter(defaultValue = "2",
            interval = "[0,100]",
            description = "The width of a cloud 'safety buffer' around a pixel which was classified as cloudy.",
            label = "Width of cloud buffer (# of pixels)")
    private int cloudBufferWidth;

    @Parameter(defaultValue = "false",
            label = " Refine pixel classification near coastlines",
            description = "Refine pixel classification near coastlines (time consuming operation!). Improves distinction of clouds and bright beaches. ")
    private boolean refineClassificationNearCoastlines;

    @Parameter(defaultValue = "ALL", valueSet = {"ALL", "LAND", "LAND_USE_THERMAL",
            "WATER", "WATER_NOTIDAL", "WATER_USE_THERMAL", "WATER_NOTIDAL_USE_THERMAL"},
            label = "Neural Net to be applied",
            description = "The Neural Net which will be applied.")
    private NNSelector nnSelector;

    @Parameter(defaultValue = "1.95",
            label = "NN cloud ambiguous lower boundary ")
    private double nnCloudAmbiguousLowerBoundaryValue;
    @Parameter(defaultValue = "3.45",
            label = "NN cloud ambiguous/sure separation value ")
    private double nnCloudAmbiguousSureSeparationValue;
    @Parameter(defaultValue = "4.3",
            label = "NN cloud sure / snow separation value ")
    private double nnCloudSureSnowSeparationValue;

    // SHIMEZ parameters:
    @Parameter(defaultValue = "true",
            label = " Apply SHIMEZ cloud test")
    private boolean applyShimezCloudTest;

    @Parameter(defaultValue = "0.1",
            description = "Threshold A for SHIMEZ cloud test: cloud if mean > B AND diff < A.",
            label = "Threshold A for SHIMEZ cloud test")
    private float shimezDiffThresh;

    @Parameter(defaultValue = "0.25",
            description = "Threshold B for SHIMEZ cloud test: cloud if mean > B AND diff < A.",
            label = "Threshold B for SHIMEZ cloud test")
    private float shimezMeanThresh;

    // HOT parameters:
    @Parameter(defaultValue = "false",
            label = " Apply HOT cloud test")
    private boolean applyHotCloudTest;

    @Parameter(defaultValue = "0.1",
            description = "Threshold A for HOT cloud test: cloud if blue - 0.5*red > A.",
            label = "Threshold A for HOT cloud test")
    private float hotThresh;

    // CLOST parameters:
    @Parameter(defaultValue = "true",
            label = " Apply CLOST cloud test")
    private boolean applyClostCloudTest;

    @Parameter(defaultValue = "0.001",
            description = "Threshold A for CLOST cloud test: cloud if coastal_aerosol*blue*panchromatic*cirrus > A.",
            label = "Threshold A for CLOST cloud test")
    private double clostThresh;

    // OTSU parameters:
    @Parameter(defaultValue = "true",
            label = " Apply OTSU cloud test")
    private boolean applyOtsuCloudTest;

    // todo: maybe activate
//    @Parameter(defaultValue = "GREY",
//               valueSet = {"GREY", "BINARY"},
//               description = "OTSU processing mode (grey or binary target image)",
//               label = "OTSU processing mode (grey or binary target image)")
    private String otsuMode = "BINARY";

    //    @Parameter(defaultValue = "false",
//            description = "If computed, write OTSU bands (Clost and binary) to the target product.",
//            label = " Write OTSU bands (Clost and binary) to the target product")
//    private boolean outputOtsuBands;
    private boolean outputOtsuBands = false;  // todo: discuss if needed

    @Parameter(defaultValue = "true",
            description = "Write source bands to the target product.",
            label = " Write source bands to the target product")
    private boolean outputSourceBands;


    private static final int LAND_WATER_MASK_RESOLUTION = 50;
    private static final int OVERSAMPLING_FACTOR_X = 3;
    private static final int OVERSAMPLING_FACTOR_Y = 3;

    private Product classificationProduct;
    private Product postProcessingProduct;

    private Product waterMaskProduct;

    private Map<String, Product> classificationInputProducts;
    private Map<String, Object> classificationParameters;
    private Product otsuProduct;
    private Product clostProduct;

    // now fix values, no longer user options (20160302):
    private final String brightnessBandLandString = "480";
    private final float brightnessThreshLand = 0.5f;
    private final String brightnessBand1WaterString = "655";
    private final float brightnessWeightBand1Water = 1.0f;
    private final String brightnessBand2WaterString = "865";
    private final float brightnessWeightBand2Water = 1.0f;
    private final float brightnessThreshWater = 0.5f;
    private final String whitenessBand1LandString = "655";
    private final String whitenessBand2LandString = "865";
    private final float whitenessThreshLand = 2.0f;
    private final String whitenessBand1WaterString = "655";
    private final String whitenessBand2WaterString = "865";
    private final float whitenessThreshWater = 2.0f;
    private final double darkGlintThreshTest1 = 0.15;
    private final String darkGlintThreshTest1WavelengthString = "865";
    private final double darkGlintThreshTest2 = 0.15;
    private String darkGlintThreshTest2WavelengthString = "1610";

    private int standardBandWidth;
    private int standardBandHeight;

    @Override
    public void initialize() throws OperatorException {
        System.out.println("Running IDEPIX Landsat 8 - source product: " + sourceProduct.getName());


        final boolean inputProductIsValid = IdepixUtils.validateInputProduct(sourceProduct, AlgorithmSelector.LANDSAT8);
        if (!inputProductIsValid) {
            throw new OperatorException(IdepixConstants.INPUT_INCONSISTENCY_ERROR_MESSAGE);
        }
        standardBandWidth =
                sourceProduct.getBand(Landsat8Constants.LANDSAT8_BLUE_BAND_NAME).getSourceImage().getWidth();
        standardBandHeight =
                sourceProduct.getBand(Landsat8Constants.LANDSAT8_BLUE_BAND_NAME).getSourceImage().getHeight();

        checkIfLandsatIsReadAsReflectance();

        if (applyClostCloudTest || applyOtsuCloudTest) {
            rescalePanchromaticBand();

            HashMap<String, Product> clostInput = new HashMap<>();
            clostInput.put("l8source", sourceProduct);
            clostProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ClostOp.class), GPF.NO_PARAMS, clostInput);
            final Band clostBand = clostProduct.getBand(ClostOp.CLOST_BAND_NAME);

            clostThresh = computeClostHistogram3PercentOfMaximum(clostBand);
            System.out.println("clostThresh = " + clostThresh);
        }

        if (applyOtsuCloudTest) {
            HashMap<String, Product> otsuInput = new HashMap<>();
            otsuInput.put("l8source", sourceProduct);
            otsuInput.put("clost", clostProduct);
            HashMap<String, Object> otsuParameters = new HashMap<>();
            otsuParameters.put("otsuMode", otsuMode);
            otsuProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(OtsuBinarizeOp.class), otsuParameters, otsuInput);
        }

        preProcess();
        computeCloudProduct();
        postProcess();

        targetProduct = IdepixUtils.cloneProduct(classificationProduct, standardBandWidth, standardBandHeight, false);

        Band cloudFlagBand = targetProduct.getBand(IdepixUtils.IDEPIX_CLOUD_FLAGS);
        cloudFlagBand.setSourceImage(postProcessingProduct.getBand(IdepixUtils.IDEPIX_CLOUD_FLAGS).getSourceImage());

        copyOutputBands();
    }

    private void rescalePanchromaticBand() {
        final Band panBand = sourceProduct.getBand(Landsat8Constants.LANDSAT8_PANCHROMATIC_BAND_NAME);

        final int panBandImageWidth = panBand.getSourceImage().getWidth();
        final int panBandImageHeight = panBand.getSourceImage().getHeight();
        if (standardBandWidth != panBandImageWidth || standardBandHeight != panBandImageHeight) {
            final float scaleFactorW = (float) (standardBandWidth * 1.0 / panBandImageWidth);
            final RenderedOp scaledPanImage = Tools.scaleImage(panBand.getSourceImage(), scaleFactorW);
            if (scaledPanImage.getWidth() == standardBandWidth && scaledPanImage.getHeight() == standardBandHeight) {
                panBand.setSourceImage(scaledPanImage);
            } else {
                System.out.println
                        ("WARNING: cannot rescale panchromatic band properly - will skip CLOST and OTSU cloud tests.");
                applyClostCloudTest = false;
                applyOtsuCloudTest = false;
            }
        }
    }

    private void checkIfLandsatIsReadAsReflectance() {
        if (!sourceProduct.getBandAt(0).getDescription().toLowerCase().contains("reflectance")) {
            throw new OperatorException("The landsat source product must provide reflectances. " +
                                                "Consider setting system property landsat.reader.readAs=reflectance.");
        }
    }

    private void preProcess() {
        HashMap<String, Object> waterMaskParameters = new HashMap<>();
        waterMaskParameters.put("resolution", LAND_WATER_MASK_RESOLUTION);
        waterMaskParameters.put("subSamplingFactorX", OVERSAMPLING_FACTOR_X);
        waterMaskParameters.put("subSamplingFactorY", OVERSAMPLING_FACTOR_Y);
        waterMaskProduct = GPF.createProduct("LandWaterMask", waterMaskParameters, sourceProduct);
    }

    private void setClassificationParameters() {
        classificationParameters = new HashMap<>();
        classificationParameters.put("brightnessThreshLand", brightnessThreshLand);
        classificationParameters.put("brightnessBandLand", Landsat8Utils.getWavelengthFromString(brightnessBandLandString));
        classificationParameters.put("brightnessThreshWater", brightnessThreshWater);
        classificationParameters.put("brightnessBand1Water",
                                     Landsat8Utils.getWavelengthFromString(brightnessBand1WaterString));
        classificationParameters.put("brightnessBand2Water",
                                     Landsat8Utils.getWavelengthFromString(brightnessBand2WaterString));
        classificationParameters.put("brightnessWeightBand1Water", brightnessWeightBand1Water);
        classificationParameters.put("brightnessWeightBand2Water", brightnessWeightBand2Water);

        classificationParameters.put("whitenessThreshLand", whitenessThreshLand);
        classificationParameters.put("whitenessBand1Land",
                                     Landsat8Utils.getWavelengthFromString(whitenessBand1LandString));
        classificationParameters.put("whitenessBand2Land",
                                     Landsat8Utils.getWavelengthFromString(whitenessBand2LandString));
        classificationParameters.put("whitenessThreshWater", whitenessThreshWater);
        classificationParameters.put("whitenessBand1Water",
                                     Landsat8Utils.getWavelengthFromString(whitenessBand1WaterString));
        classificationParameters.put("whitenessBand2Water",
                                     Landsat8Utils.getWavelengthFromString(whitenessBand2WaterString));

        classificationParameters.put("applyShimezCloudTest", applyShimezCloudTest);
        classificationParameters.put("shimezDiffThresh", shimezDiffThresh);
        classificationParameters.put("shimezMeanThresh", shimezMeanThresh);
        classificationParameters.put("applyHotCloudTest", applyHotCloudTest);
        classificationParameters.put("hotThresh", hotThresh);
        classificationParameters.put("applyClostCloudTest", applyClostCloudTest);
        classificationParameters.put("clostThresh", clostThresh);
        classificationParameters.put("applyOtsuCloudTest", applyOtsuCloudTest);

        classificationParameters.put("nnSelector", nnSelector);
        classificationParameters.put("nnCloudAmbiguousLowerBoundaryValue", nnCloudAmbiguousLowerBoundaryValue);
        classificationParameters.put("nnCloudAmbiguousLowerBoundaryValue", nnCloudAmbiguousLowerBoundaryValue);
        classificationParameters.put("nnCloudSureSnowSeparationValue", nnCloudSureSnowSeparationValue);

        classificationParameters.put("darkGlintThreshTest1", darkGlintThreshTest1);
        classificationParameters.put("darkGlintThreshTest1Wavelength",
                                     Landsat8Utils.getWavelengthFromString(darkGlintThreshTest1WavelengthString));
        classificationParameters.put("darkGlintThreshTest2", darkGlintThreshTest2);
        classificationParameters.put("darkGlintThreshTest2Wavelength",
                                     Landsat8Utils.getWavelengthFromString(darkGlintThreshTest2WavelengthString));
    }

    private void computeCloudProduct() {
        setClassificationParameters();
        classificationInputProducts = new HashMap<>();
        classificationInputProducts.put("l8source", sourceProduct);
        classificationInputProducts.put("otsu", otsuProduct);
        classificationInputProducts.put("waterMask", waterMaskProduct);
        classificationProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(Landsat8ClassificationOp.class),
                                                  classificationParameters, classificationInputProducts);
    }

    private void postProcess() {
        HashMap<String, Product> input = new HashMap<>();
        input.put("landsatCloud", classificationProduct);
        input.put("waterMask", waterMaskProduct);

        Map<String, Object> params = new HashMap<>();
        params.put("cloudBufferWidth", cloudBufferWidth);
        params.put("computeCloudBuffer", computeCloudBuffer);
        params.put("computeCloudShadow", false);     // todo: we need algo
        params.put("refineClassificationNearCoastlines", refineClassificationNearCoastlines);  // always an improvement, but time consuming
        postProcessingProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(Landsat8PostProcessOp.class), params, input);
    }

    private void copyOutputBands() {
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        Landsat8Utils.setupLandsat8Bitmasks(targetProduct);
        if (outputSourceBands) {
            ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
            for (Band sourceBand : sourceProduct.getBands()) {
                if (!sourceBand.getName().equals(Landsat8Constants.LANDSAT8_PANCHROMATIC_BAND_NAME)) {
                    ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct, true);
                }
            }
        }

        if (outputOtsuBands) {
            for (Band otsuBands : otsuProduct.getBands()) {
                ProductUtils.copyBand(otsuBands.getName(), otsuProduct, targetProduct, true);
            }
        }
    }

    private double computeClostHistogram3PercentOfMaximum(Band band) {
        final Stx stx = new StxFactory().create(band, ProgressMonitor.NULL);
        return Landsat8Utils.getHistogramBinAtNPercentOfMaximum(stx, 3.0);
    }


    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(Landsat8Op.class);
        }
    }
}
