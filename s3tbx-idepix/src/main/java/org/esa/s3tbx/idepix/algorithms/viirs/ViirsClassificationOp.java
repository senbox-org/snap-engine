package org.esa.s3tbx.idepix.algorithms.viirs;

import org.esa.s3tbx.idepix.core.util.SchillerNeuralNetWrapper;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.*;

import java.io.IOException;
import java.io.InputStream;

/**
 * VIIRS pixel classification operator.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Viirs.Classification",
        version = "2.2",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "VIIRS pixel classification operator.",
        internal = true)
public class ViirsClassificationOp extends PixelOperator {

    @Parameter(defaultValue = "true",
            label = " RhoTOA bands (VIIRS)",
            description = "Write RhoTOA bands to target product (VIIRS).")
    private boolean outputViirsRhoToa = true;

    @Parameter(defaultValue = "true",
            label = " Debug bands",
            description = "Write further useful bands to target product.")
    private boolean outputDebug = true;

    @Parameter(defaultValue = "1", label = " Width of cloud buffer (# of pixels)")
    private int cloudBufferWidth;

    @Parameter(defaultValue = "50", valueSet = {"50", "150"}, label = " Resolution of used land-water mask in m/pixel",
            description = "Resolution in m/pixel")
    private int waterMaskResolution;


    @SourceProduct(alias = "refl", description = "MODIS L1b reflectance product")
    private Product reflProduct;

    @SourceProduct(alias = "waterMask")
    private Product waterMaskProduct;

    public static final String SCHILLER_VIIRS_NET_NAME = "6x5x4x3x2_204.8.net";
    ThreadLocal<SchillerNeuralNetWrapper> viirsNeuralNet;


    @Override
    public Product getSourceProduct() {
        // this is the source product for the ProductConfigurer
        return reflProduct;
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        readSchillerNet();
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        ViirsAlgorithm algorithm = createViirsAlgorithm(x, y, sourceSamples, targetSamples);
        setClassifFlag(targetSamples, algorithm);
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
        for (int i = 0; i < ViirsConstants.VIIRS_L1B_NUM_SPECTRAL_BANDS; i++) {
            if (getSourceProduct().containsBand(ViirsConstants.VIIRS_SPECTRAL_BAND_NAMES[i])) {
                sampleConfigurer.defineSample(i, ViirsConstants.VIIRS_SPECTRAL_BAND_NAMES[i], getSourceProduct());
            } else {
                sampleConfigurer.defineSample(i, ViirsConstants.VIIRS_SPECTRAL_BAND_NAMES[i].replace(".", "_"),
                                              getSourceProduct());
            }
        }

        sampleConfigurer.defineSample(ViirsConstants.VIIRS_L1B_NUM_SPECTRAL_BANDS+1,
                                      ViirsConstants.LAND_WATER_FRACTION_BAND_NAME, waterMaskProduct);
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
        // the only standard band:
        sampleConfigurer.defineSample(0, ViirsConstants.CLASSIF_BAND_NAME);

        // debug bands:
        if (outputDebug) {
            sampleConfigurer.defineSample(1, ViirsConstants.BRIGHTNESS_BAND_NAME);
            sampleConfigurer.defineSample(2, ViirsConstants.NDSI_BAND_NAME);
        }
        sampleConfigurer.defineSample(3, ViirsConstants.SCHILLER_NN_OUTPUT_BAND_NAME);
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        productConfigurer.copyTimeCoding();
        productConfigurer.copyTiePointGrids();
        Band classifFlagBand = productConfigurer.addBand(ViirsConstants.CLASSIF_BAND_NAME, ProductData.TYPE_INT16);

        classifFlagBand.setDescription("Pixel classification flag");
        classifFlagBand.setUnit("dl");
        FlagCoding flagCoding = ViirsUtils.createViirsFlagCoding(ViirsConstants.CLASSIF_BAND_NAME);
        classifFlagBand.setSampleCoding(flagCoding);
        getTargetProduct().getFlagCodingGroup().add(flagCoding);

        productConfigurer.copyGeoCoding();
        ViirsUtils.setupOccciClassifBitmask(getTargetProduct());

        // debug bands:
        if (outputDebug) {
            Band brightnessValueBand = productConfigurer.addBand(ViirsConstants.BRIGHTNESS_BAND_NAME, ProductData.TYPE_FLOAT32);
            brightnessValueBand.setDescription("Brightness value (uses EV_250_Aggr1km_RefSB_1) ");
            brightnessValueBand.setUnit("dl");

            Band ndsiValueBand = productConfigurer.addBand(ViirsConstants.NDSI_BAND_NAME, ProductData.TYPE_FLOAT32);
            ndsiValueBand.setDescription("NDSI value (uses EV_250_Aggr1km_RefSB_1, EV_500_Aggr1km_RefSB_7)");
            ndsiValueBand.setUnit("dl");

        }
        Band nnValueBand = productConfigurer.addBand(ViirsConstants.SCHILLER_NN_OUTPUT_BAND_NAME, ProductData.TYPE_FLOAT32);
        nnValueBand.setDescription("Schiller NN output value");
        nnValueBand.setUnit("dl");
    }

    private void readSchillerNet() {
        try (InputStream isV = getClass().getResourceAsStream(SCHILLER_VIIRS_NET_NAME)) {
            viirsNeuralNet = SchillerNeuralNetWrapper.create(isV);
        } catch (IOException e) {
            throw new OperatorException("Cannot read Neural Nets: " + e.getMessage());
        }
    }

    private void setClassifFlag(WritableSample[] targetSamples, ViirsAlgorithm algorithm) {
        targetSamples[0].set(ViirsConstants.F_INVALID, algorithm.isInvalid());
        targetSamples[0].set(ViirsConstants.F_CLOUD, algorithm.isCloud());
        targetSamples[0].set(ViirsConstants.F_CLOUD_AMBIGUOUS, algorithm.isCloudAmbiguous());
        targetSamples[0].set(ViirsConstants.F_CLOUD_SURE, algorithm.isCloudSure());
        targetSamples[0].set(ViirsConstants.F_CLOUD_BUFFER, algorithm.isCloudBuffer());
        targetSamples[0].set(ViirsConstants.F_CLOUD_SHADOW, algorithm.isCloudShadow());
        targetSamples[0].set(ViirsConstants.F_SNOW_ICE, algorithm.isSnowIce());
        targetSamples[0].set(ViirsConstants.F_MIXED_PIXEL, algorithm.isMixedPixel());
        targetSamples[0].set(ViirsConstants.F_GLINT_RISK, algorithm.isGlintRisk());
        targetSamples[0].set(ViirsConstants.F_LAND, algorithm.isLand());
        targetSamples[0].set(ViirsConstants.F_BRIGHT, algorithm.isBright());

        if (outputDebug) {
            targetSamples[1].set(algorithm.brightValue());
            targetSamples[2].set(algorithm.ndsiValue());
        }
    }

    private ViirsAlgorithm createViirsAlgorithm(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final double[] reflectance = new double[ViirsConstants.VIIRS_L1B_NUM_SPECTRAL_BANDS];
        double[] neuralNetOutput;

        float waterFraction = Float.NaN;

        ViirsAlgorithm viirsAlgorithm = new ViirsAlgorithm();
        for (int i = 0; i < ViirsConstants.VIIRS_L1B_NUM_SPECTRAL_BANDS; i++) {
            reflectance[i] = sourceSamples[i].getFloat();
        }
        viirsAlgorithm.setRefl(reflectance);
        // the water mask ends at 59 Degree south, stop earlier to avoid artefacts
        if (getGeoPos(x, y).lat > -58f) {
            waterFraction =
                    sourceSamples[ViirsConstants.VIIRS_L1B_NUM_SPECTRAL_BANDS + 1].getFloat();
        }
        viirsAlgorithm.setWaterFraction(waterFraction);

        double[] viirsNeuralNetInput = viirsNeuralNet.get().getInputVector();
        for (int i = 0; i < viirsNeuralNetInput.length; i++) {
            viirsNeuralNetInput[i] = Math.sqrt(sourceSamples[i].getFloat());
        }

        neuralNetOutput = viirsNeuralNet.get().getNeuralNet().calc(viirsNeuralNetInput);

        viirsAlgorithm.setNnOutput(neuralNetOutput);

        final int targetOffset = outputDebug ? 3 : 1;
        targetSamples[targetOffset].set(neuralNetOutput[0]);

        return viirsAlgorithm;
    }

    private GeoPos getGeoPos(int x, int y) {
        final GeoPos geoPos = new GeoPos();
        final GeoCoding geoCoding = reflProduct.getSceneGeoCoding();
        final PixelPos pixelPos = new PixelPos(x, y);
        geoCoding.getGeoPos(pixelPos, geoPos);
        return geoPos;
    }


    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ViirsClassificationOp.class);
        }
    }

}
