package org.esa.s3tbx.idepix.algorithms.seawifs;

import org.esa.s3tbx.idepix.core.IdepixConstants;
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
 * SeaWiFS pixel classification operator.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.SeaWifs.Classification",
        version = "2.2",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "SeaWiFS pixel classification operator.",
        internal = true)
public class SeaWifsClassificationOp extends PixelOperator {

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


    @SourceProduct(alias = "refl", description = "MODIS L1b reflectance product")
    private Product reflProduct;

    @SourceProduct(alias = "waterMask")
    private Product waterMaskProduct;

    public static final String SEAWIFS_NET_NAME = "6x3_166.0.net";

    private static final int earthSunDistance = 1;

    // derived from cahalan table from Kerstin tb 2013-11-22
    private static final double[] nasaSolarFluxes =
            {1735.518167, 1858.404314, 1981.076667, 1881.566829, 1874.005, 1537.254783, 1230.04, 957.6122143};

    ThreadLocal<SchillerNeuralNetWrapper> seawifsNeuralNet;

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
        final SeaWifsAlgorithm algorithm = createSeawifsAlgorithm(x, y, sourceSamples, targetSamples);
        setClassifFlag(targetSamples, algorithm);
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(SeaWifsConstants.SRC_SZA, "solz", getSourceProduct());
        sampleConfigurer.defineSample(SeaWifsConstants.SRC_SAA, "sola", getSourceProduct());
        sampleConfigurer.defineSample(SeaWifsConstants.SRC_VZA, "senz", getSourceProduct());
        sampleConfigurer.defineSample(SeaWifsConstants.SRC_VAA, "sena", getSourceProduct());
        for (int i = 0; i < SeaWifsConstants.SEAWIFS_L1B_NUM_SPECTRAL_BANDS; i++) {
            sampleConfigurer.defineSample(SeaWifsConstants.SEAWIFS_SRC_RAD_OFFSET+ i,
                                          radianceBandPrefix + SeaWifsConstants.SEAWIFS_L1B_SPECTRAL_BAND_NAMES[i],
                                          getSourceProduct());
        }
        int index = SeaWifsConstants.SEAWIFS_SRC_RAD_OFFSET + SeaWifsConstants.SEAWIFS_L1B_NUM_SPECTRAL_BANDS + 1;
        sampleConfigurer.defineSample(index, IdepixConstants.LAND_WATER_FRACTION_BAND_NAME, waterMaskProduct);
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
        // the only standard band:
        sampleConfigurer.defineSample(0, IdepixConstants.CLASSIF_BAND_NAME);
        sampleConfigurer.defineSample(1, IdepixConstants.NN_OUTPUT_BAND_NAME);
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        productConfigurer.copyTimeCoding();
        productConfigurer.copyTiePointGrids();
        Band classifFlagBand = productConfigurer.addBand(IdepixConstants.CLASSIF_BAND_NAME, ProductData.TYPE_INT16);

        classifFlagBand.setDescription("Pixel classification flag");
        classifFlagBand.setUnit("dl");
        FlagCoding flagCoding = SeaWifsUtils.createSeawifsFlagCoding(IdepixConstants.CLASSIF_BAND_NAME);
        classifFlagBand.setSampleCoding(flagCoding);
        getTargetProduct().getFlagCodingGroup().add(flagCoding);

        getTargetProduct().setSceneGeoCoding(reflProduct.getSceneGeoCoding());
        SeaWifsUtils.setupSeawifsClassifBitmask(getTargetProduct());

        Band nnValueBand = productConfigurer.addBand(IdepixConstants.NN_OUTPUT_BAND_NAME, ProductData.TYPE_FLOAT32);
        nnValueBand.setDescription("NN output value");
        nnValueBand.setUnit("dl");
    }

    private void readSchillerNets() {
        try (InputStream isSW = getClass().getResourceAsStream(SEAWIFS_NET_NAME)) {
            seawifsNeuralNet = SchillerNeuralNetWrapper.create(isSW);
        } catch (IOException e) {
            throw new OperatorException("Cannot read Schiller neural nets: " + e.getMessage());
        }
    }

    private void setClassifFlag(WritableSample[] targetSamples, SeaWifsAlgorithm algorithm) {
        targetSamples[0].set(IdepixConstants.IDEPIX_INVALID, algorithm.isInvalid());
        targetSamples[0].set(IdepixConstants.IDEPIX_CLOUD, algorithm.isCloud());
        targetSamples[0].set(IdepixConstants.IDEPIX_CLOUD_AMBIGUOUS, algorithm.isCloudAmbiguous());
        targetSamples[0].set(IdepixConstants.IDEPIX_CLOUD_SURE, algorithm.isCloudSure());
        targetSamples[0].set(IdepixConstants.IDEPIX_CLOUD_BUFFER, algorithm.isCloudBuffer());
        targetSamples[0].set(IdepixConstants.IDEPIX_CLOUD_SHADOW, algorithm.isCloudShadow());
        targetSamples[0].set(IdepixConstants.IDEPIX_SNOW_ICE, algorithm.isSnowIce());
        targetSamples[0].set(IdepixConstants.IDEPIX_MIXED_PIXEL, algorithm.isMixedPixel());
        targetSamples[0].set(IdepixConstants.IDEPIX_GLINT_RISK, algorithm.isGlintRisk());
        targetSamples[0].set(IdepixConstants.IDEPIX_COASTLINE, algorithm.isCoastline());
        targetSamples[0].set(IdepixConstants.IDEPIX_LAND, algorithm.isLand());
        targetSamples[0].set(IdepixConstants.IDEPIX_BRIGHT, algorithm.isBright());
    }

    private SeaWifsAlgorithm createSeawifsAlgorithm(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final double[] reflectance = new double[SeaWifsConstants.SEAWIFS_L1B_NUM_SPECTRAL_BANDS];
        double[] neuralNetOutput;

        float waterFraction = Float.NaN;

        SeaWifsAlgorithm occciAlgorithm = new SeaWifsAlgorithm();
        double[] seawifsNeuralNetInput = seawifsNeuralNet.get().getInputVector();
        for (int i = 0; i < SeaWifsConstants.SEAWIFS_L1B_NUM_SPECTRAL_BANDS; i++) {
            reflectance[i] = sourceSamples[SeaWifsConstants.SEAWIFS_SRC_RAD_OFFSET+ i].getFloat();
            if (!radianceBandPrefix.equals("rhot_")) {  // L1C are already reflectances
                scaleInputSpectralDataToReflectance(reflectance, 0);
            }
            seawifsNeuralNetInput[i] = Math.sqrt(reflectance[i]);
        }
        occciAlgorithm.setRefl(reflectance);
        // the water mask ends at 59 Degree south, stop earlier to avoid artefacts
        if (getGeoPos(x, y).lat > -58f) {
            waterFraction = sourceSamples[SeaWifsConstants.SEAWIFS_SRC_RAD_OFFSET +
                            SeaWifsConstants.SEAWIFS_L1B_NUM_SPECTRAL_BANDS + 1].getFloat();
        }
        occciAlgorithm.setWaterFraction(waterFraction);

        neuralNetOutput = seawifsNeuralNet.get().getNeuralNet().calc(seawifsNeuralNetInput);

        occciAlgorithm.setNnOutput(neuralNetOutput);
        targetSamples[3].set(neuralNetOutput[0]);

        // SeaWiFS reflectances output:
        if (outputReflectance) {
            for (int i = 0; i < SeaWifsConstants.SEAWIFS_L1B_NUM_SPECTRAL_BANDS; i++) {
                targetSamples[4 + i].set(reflectance[i]);
            }
        }

        return occciAlgorithm;
    }

    private void scaleInputSpectralDataToReflectance(double[] inputs, int offset) {
        // first scale to consistent radiances:
        scaleInputSpectralDataToRadiance(inputs, offset);
        final double oneDivEarthSunDistanceSquare = 1.0 / (earthSunDistance * earthSunDistance);
        for (int i = 0; i < SeaWifsConstants.SEAWIFS_L1B_NUM_SPECTRAL_BANDS; i++) {
            final int index = offset + i;
            // this is rad2refl:
            inputs[index] = inputs[index] * Math.PI  / (nasaSolarFluxes[i] * oneDivEarthSunDistanceSquare);
        }
    }

    /**
     * Scales the input spectral data to be consistent with the MERIS case. Resulting data should be TOA radiance in
     * [mW/(m^2 * sr * nm)] or [LU], i.e. Luminance Unit
     * Scaling is performed "in place", if necessary
     *
     * @param inputs input data vector
     */
    public void scaleInputSpectralDataToRadiance(double[] inputs, int offset) {
        for (int i = 0; i < SeaWifsConstants.SEAWIFS_L1B_NUM_SPECTRAL_BANDS; i++) {
            final int index = offset + i;
            inputs[index] *= 10.0;
        }
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
            super(SeaWifsClassificationOp.class);
        }
    }

}
