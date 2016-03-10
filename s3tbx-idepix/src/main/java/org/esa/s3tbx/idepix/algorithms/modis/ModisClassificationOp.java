package org.esa.s3tbx.idepix.algorithms.modis;

import org.esa.s3tbx.idepix.core.util.SchillerNeuralNetWrapper;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.*;
import org.esa.snap.core.util.ProductUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * MODIS pixel classification operator.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Modis.Classification",
        version = "2.2",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "MODIS pixel classification operator.",
        internal = true)
public class ModisClassificationOp extends PixelOperator {

    @Parameter(defaultValue = "true",
            label = " Apply brightness test",
            description = "Apply brightness test: EV_250_Aggr1km_RefSB_1 > THRESH.")
    private boolean applyBrightnessTest;

    @Parameter(defaultValue = "true",
            label = " Apply 'OR' logic in cloud test",
            description = "Apply 'OR' logic instead of 'AND' logic in cloud test.")
    private boolean applyOrLogicInCloudTest;

    @Parameter(defaultValue = "0.15",
            label = " 'Dark glint' threshold at 859nm",
            description = "'Dark glint' threshold: Cloud possible only if EV_250_Aggr1km_RefSB_2 > THRESH.")
    private double ocModisGlintThresh859 = 0.15;

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




    @SourceProduct(alias = "refl", description = "MODIS L1b reflectance product")
    private Product reflProduct;

    @SourceProduct(alias = "waterMask")
    private Product waterMaskProduct;

    public static final String SCHILLER_MODIS_WATER_NET_NAME = "9x7x5x3_130.3_water.net";
    public static final String SCHILLER_MODIS_LAND_NET_NAME = "8x6x4x2_290.4_land.net";
    public static final String SCHILLER_MODIS_ALL_NET_NAME = "9x7x5x3_319.7_all.net";

    ThreadLocal<SchillerNeuralNetWrapper> modisWaterNeuralNet;
    ThreadLocal<SchillerNeuralNetWrapper> modisLandNeuralNet;
    ThreadLocal<SchillerNeuralNetWrapper> modisAllNeuralNet;

    @Override
    public Product getSourceProduct() {
        // this is the source product for the ProductConfigurer
        return reflProduct;
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        readSchillerNets();
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final ModisAlgorithm algorithm = createModisAlgorithm(x, y, sourceSamples, targetSamples);
        setClassifFlag(targetSamples, algorithm);
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
        for (int i = 0; i < ModisConstants.MODIS_L1B_NUM_SPECTRAL_BANDS; i++) {
            if (reflProduct.containsBand(ModisConstants.MODIS_L1B_SPECTRAL_BAND_NAMES[i])) {
                sampleConfigurer.defineSample(i, ModisConstants.MODIS_L1B_SPECTRAL_BAND_NAMES[i], reflProduct);
            } else {
                sampleConfigurer.defineSample(i, ModisConstants.MODIS_L1B_SPECTRAL_BAND_NAMES[i].replace(".", "_"),
                                              reflProduct);
            }
        }
        for (int i = 0; i < ModisConstants.MODIS_L1B_NUM_EMISSIVE_BANDS; i++) {
            if (reflProduct.containsBand(ModisConstants.MODIS_L1B_EMISSIVE_BAND_NAMES[i])) {
                sampleConfigurer.defineSample(ModisConstants.MODIS_SRC_RAD_OFFSET + i,
                                              ModisConstants.MODIS_L1B_EMISSIVE_BAND_NAMES[i], reflProduct);
            } else {
                final String newEmissiveBandName = ModisConstants.MODIS_L1B_EMISSIVE_BAND_NAMES[i].replace(".", "_");
                final Band emissiveBand = reflProduct.getBand(newEmissiveBandName);
                emissiveBand.setScalingFactor(1.0);      // todo: we do this to come back to counts with SeaDAS reader,
                // as the NN was also trained with counts
                emissiveBand.setScalingOffset(0.0);
                sampleConfigurer.defineSample(ModisConstants.MODIS_SRC_RAD_OFFSET + i, newEmissiveBandName, reflProduct);
            }
        }

        int index = ModisConstants.MODIS_SRC_RAD_OFFSET + ModisConstants.MODIS_L1B_NUM_SPECTRAL_BANDS + 1;
        sampleConfigurer.defineSample(index, ModisConstants.LAND_WATER_FRACTION_BAND_NAME, waterMaskProduct);
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
        // the only standard band:
        sampleConfigurer.defineSample(0, ModisConstants.CLASSIF_BAND_NAME);
        sampleConfigurer.defineSample(1, ModisConstants.SCHILLER_NN_OUTPUT_BAND_NAME);
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        productConfigurer.copyTimeCoding();
        productConfigurer.copyTiePointGrids();
        Band classifFlagBand = productConfigurer.addBand(ModisConstants.CLASSIF_BAND_NAME, ProductData.TYPE_INT16);

        classifFlagBand.setDescription("Pixel classification flag");
        classifFlagBand.setUnit("dl");
        FlagCoding flagCoding = ModisUtils.createOccciFlagCoding(ModisConstants.CLASSIF_BAND_NAME);
        classifFlagBand.setSampleCoding(flagCoding);
        getTargetProduct().getFlagCodingGroup().add(flagCoding);

        getTargetProduct().setSceneGeoCoding(reflProduct.getSceneGeoCoding());
        ModisUtils.setupClassifBitmask(getTargetProduct());

        Band nnValueBand = productConfigurer.addBand(ModisConstants.SCHILLER_NN_OUTPUT_BAND_NAME, ProductData.TYPE_FLOAT32);
        nnValueBand.setDescription("Schiller NN output value");
        nnValueBand.setUnit("dl");
    }

    private void readSchillerNets() {
        try (
                InputStream isMW = getClass().getResourceAsStream(SCHILLER_MODIS_WATER_NET_NAME);
                InputStream isML = getClass().getResourceAsStream(SCHILLER_MODIS_LAND_NET_NAME);
                InputStream isMA = getClass().getResourceAsStream(SCHILLER_MODIS_ALL_NET_NAME)
        ) {
            modisWaterNeuralNet = SchillerNeuralNetWrapper.create(isMW);
            modisLandNeuralNet = SchillerNeuralNetWrapper.create(isML);
            modisAllNeuralNet = SchillerNeuralNetWrapper.create(isMA);
        } catch (IOException e) {
            throw new OperatorException("Cannot read Schiller neural nets: " + e.getMessage());
        }
    }

    private void setClassifFlag(WritableSample[] targetSamples, ModisAlgorithm algorithm) {
        targetSamples[0].set(ModisConstants.F_INVALID, algorithm.isInvalid());
        targetSamples[0].set(ModisConstants.F_CLOUD, algorithm.isCloud());
        targetSamples[0].set(ModisConstants.F_CLOUD_AMBIGUOUS, algorithm.isCloudAmbiguous());
        targetSamples[0].set(ModisConstants.F_CLOUD_SURE, algorithm.isCloudSure());
        targetSamples[0].set(ModisConstants.F_CLOUD_BUFFER, algorithm.isCloudBuffer());
        targetSamples[0].set(ModisConstants.F_CLOUD_SHADOW, algorithm.isCloudShadow());
        targetSamples[0].set(ModisConstants.F_SNOW_ICE, algorithm.isSnowIce());
        targetSamples[0].set(ModisConstants.F_MIXED_PIXEL, algorithm.isMixedPixel());
        targetSamples[0].set(ModisConstants.F_GLINT_RISK, algorithm.isGlintRisk());
        targetSamples[0].set(ModisConstants.F_COASTLINE, algorithm.isCoastline());
        targetSamples[0].set(ModisConstants.F_LAND, algorithm.isLand());
        targetSamples[0].set(ModisConstants.F_BRIGHT, algorithm.isBright());
    }

    private ModisAlgorithm createModisAlgorithm(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        ModisAlgorithm modisAlgorithm = new ModisAlgorithm();

        final double[] reflectance = new double[ModisConstants.MODIS_L1B_NUM_SPECTRAL_BANDS];
        double[] neuralNetOutput;

        float waterFraction = Float.NaN;

        for (int i = 0; i < ModisConstants.MODIS_L1B_NUM_SPECTRAL_BANDS; i++) {
            reflectance[i] = sourceSamples[i].getFloat();
        }
        modisAlgorithm.setRefl(reflectance);
        // the water mask ends at 59 Degree south, stop earlier to avoid artefacts
        if (getGeoPos(x, y).lat > -58f) {
            waterFraction =
                    sourceSamples[ModisConstants.MODIS_SRC_RAD_OFFSET + ModisConstants.MODIS_L1B_NUM_SPECTRAL_BANDS + 1].getFloat();
        }
        modisAlgorithm.setWaterFraction(waterFraction);

        modisAlgorithm.setModisApplyBrightnessTest(applyBrightnessTest);
        final double ocModisBrightnessThreshCloudSure = 0.15;
        modisAlgorithm.setModisBrightnessThreshCloudSure(ocModisBrightnessThreshCloudSure);
        final double ocModisBrightnessThreshCloudAmbiguous = 0.125;
        modisAlgorithm.
                setModisBrightnessThreshCloudAmbiguous(ocModisBrightnessThreshCloudAmbiguous);
        double ocModisGlintThresh859 = 0.15;
        modisAlgorithm.setModisGlintThresh859(ocModisGlintThresh859);
        modisAlgorithm.setModisApplyOrLogicInCloudTest(applyOrLogicInCloudTest);

        double[] modisNeuralNetInput = modisAllNeuralNet.get().getInputVector();
        modisNeuralNetInput[0] = Math.sqrt(sourceSamples[0].getFloat());    // EV_250_Aggr1km_RefSB.1 (645nm)
        modisNeuralNetInput[1] = Math.sqrt(sourceSamples[2].getFloat());    // EV_250_Aggr1km_RefSB.3 (469nm)
        modisNeuralNetInput[2] = Math.sqrt(sourceSamples[3].getFloat());    // EV_500_Aggr1km_RefSB.4 (555nm)
        modisNeuralNetInput[3] = Math.sqrt(sourceSamples[4].getFloat());    // EV_500_Aggr1km_RefSB.5 (1240nm)
        modisNeuralNetInput[4] = Math.sqrt(sourceSamples[6].getFloat());    // EV_500_Aggr1km_RefSB.7 (2130nm)
        final float emissive23Rad = sourceSamples[ModisConstants.MODIS_SRC_RAD_OFFSET + 3].getFloat();
        modisNeuralNetInput[5] = Math.sqrt(emissive23Rad);                  // EV_1KM_Emissive.23   (4050nm)
        final float emissive25Rad = sourceSamples[ModisConstants.MODIS_SRC_RAD_OFFSET + 5].getFloat();
        modisNeuralNetInput[6] = Math.sqrt(emissive25Rad);                  // EV_1KM_Emissive.25   (4515nm)
        modisNeuralNetInput[7] = Math.sqrt(sourceSamples[21].getFloat());   // EV_1KM_RefSB.26    (1375nm)
        final float emissive31Rad = sourceSamples[ModisConstants.MODIS_SRC_RAD_OFFSET + 10].getFloat();
        modisNeuralNetInput[8] = Math.sqrt(emissive31Rad);                  // EV_1KM_Emissive.31   (11030nm)
        final float emissive32Rad = sourceSamples[ModisConstants.MODIS_SRC_RAD_OFFSET + 11].getFloat();
        modisNeuralNetInput[9] = Math.sqrt(emissive32Rad);                  // EV_1KM_Emissive.32   (12020nm)

        neuralNetOutput = modisAllNeuralNet.get().getNeuralNet().calc(modisNeuralNetInput);

        modisAlgorithm.setNnOutput(neuralNetOutput);

        targetSamples[1].set(neuralNetOutput[0]);

        return modisAlgorithm;
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
            super(ModisClassificationOp.class);
        }
    }

}
