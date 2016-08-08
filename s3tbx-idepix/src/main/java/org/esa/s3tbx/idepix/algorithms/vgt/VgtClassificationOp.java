package org.esa.s3tbx.idepix.algorithms.vgt;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.idepix.algorithms.CloudBuffer;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.pixel.AbstractPixelProperties;
import org.esa.s3tbx.idepix.core.util.IdepixUtils;
import org.esa.s3tbx.idepix.core.util.SchillerNeuralNetWrapper;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.watermask.operator.WatermaskClassifier;

import java.awt.*;
import java.util.Map;

/**
 * VGT pixel classification operator.
 * Pixels are classified from NN approach (following BEAM Idepix algorithm).
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Vgt",
        version = "1.0",
        internal = true,
        authors = "Olaf Danne",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Idepix land pixel classification operator for VGT.")
public class VgtClassificationOp extends Operator {

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

    @Parameter(defaultValue = "false",
            label = " Use land-water flag from L1b product instead",
            description = "Use land-water flag from L1b product instead of SRTM mask")
    private boolean useL1bLandWaterFlag;

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


    // VGT bands:
    private Band[] vgtReflectanceBands;

    Band cloudFlagBand;
    Band temperatureBand;
    Band brightBand;
    Band whiteBand;
    Band brightWhiteBand;
    Band spectralFlatnessBand;
    Band ndviBand;
    Band ndsiBand;
    Band glintRiskBand;
    Band radioLandBand;

    Band radioWaterBand;

    public static final int SM_F_CLOUD_1 = 0;
    public static final int SM_F_CLOUD_2 = 1;
    public static final int SM_F_ICE_SNOW = 2;
    public static final int SM_F_LAND = 3;
    public static final int SM_F_MIR_GOOD = 4;
    public static final int SM_F_B3_GOOD = 5;
    public static final int SM_F_B2_GOOD = 6;
    public static final int SM_F_B0_GOOD = 7;

    @SourceProduct(alias = "gal1b", description = "The source product.")
    Product sourceProduct;

    @TargetProduct(description = "The target product.")
    Product targetProduct;

    @SourceProduct(alias = "waterMask")
    private Product waterMaskProduct;


    public static final String SCHILLER_VGT_NET_NAME = "3x2x2_341.8.net";
    ThreadLocal<SchillerNeuralNetWrapper> vgtNeuralNet;

    private Band landWaterBand;

    static final byte WATERMASK_FRACTION_THRESH = 23;   // for 3x3 subsampling, this means 2 subpixels water

    @Override
    public void initialize() throws OperatorException {
        landWaterBand = waterMaskProduct.getBand("land_water_fraction");
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle rectangle, ProgressMonitor pm) throws OperatorException {
        // VGT variables
        final Band smFlagBand = sourceProduct.getBand("SM");
        final Tile smFlagTile = getSourceTile(smFlagBand, rectangle);

        Tile waterFractionTile = getSourceTile(landWaterBand, rectangle);

        Tile[] vgtReflectanceTiles = new Tile[IdepixConstants.VGT_REFLECTANCE_BAND_NAMES.length];
        float[] vgtReflectance = new float[IdepixConstants.VGT_REFLECTANCE_BAND_NAMES.length];
        for (int i = 0; i < IdepixConstants.VGT_REFLECTANCE_BAND_NAMES.length; i++) {
            vgtReflectanceTiles[i] = getSourceTile(vgtReflectanceBands[i], rectangle);
        }

        GeoPos geoPos = null;
        final Band cloudFlagTargetBand = targetProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS);
        final Tile cloudFlagTargetTile = targetTiles.get(cloudFlagTargetBand);

        final Band nnTargetBand = targetProduct.getBand("vgt_nn_value");
        final Tile nnTargetTile = targetTiles.get(nnTargetBand);

        try {
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                checkForCancellation();
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {

                    byte waterMaskFraction = WatermaskClassifier.INVALID_VALUE;
                    if (!useL1bLandWaterFlag) {
                        waterMaskFraction = (byte) waterFractionTile.getSampleInt(x, y);
                    }

                    // set up pixel properties for given instruments...
                    VgtAlgorithm vgtAlgorithm = createVgtAlgorithm(smFlagTile, vgtReflectanceTiles,
                                                                   vgtReflectance,
                                                                   waterMaskFraction,
                                                                   y, x);

                    setCloudFlag(cloudFlagTargetTile, y, x, vgtAlgorithm);

                    // apply improvement from NN approach...
                    final double[] nnOutput = vgtAlgorithm.getNnOutput();
                    if (!cloudFlagTargetTile.getSampleBit(x, y, IdepixConstants.F_INVALID)) {
                        cloudFlagTargetTile.setSample(x, y, IdepixConstants.F_CLOUD_AMBIGUOUS, false);
                        cloudFlagTargetTile.setSample(x, y, IdepixConstants.F_CLOUD_SURE, false);
                        cloudFlagTargetTile.setSample(x, y, IdepixConstants.F_CLOUD, false);
                        cloudFlagTargetTile.setSample(x, y, IdepixConstants.F_CLEAR_SNOW, false);
                        if (nnOutput[0] > nnCloudAmbiguousLowerBoundaryValue &&
                                nnOutput[0] <= nnCloudAmbiguousSureSeparationValue) {
                            // this would be as 'CLOUD_AMBIGUOUS'...
                            cloudFlagTargetTile.setSample(x, y, IdepixConstants.F_CLOUD_AMBIGUOUS, true);
                            cloudFlagTargetTile.setSample(x, y, IdepixConstants.F_CLOUD, true);
                        }
                        if (nnOutput[0] > nnCloudAmbiguousSureSeparationValue &&
                                nnOutput[0] <= nnCloudSureSnowSeparationValue) {
                            // this would be as 'CLOUD_SURE'...
                            cloudFlagTargetTile.setSample(x, y, IdepixConstants.F_CLOUD_SURE, true);
                            cloudFlagTargetTile.setSample(x, y, IdepixConstants.F_CLOUD, true);
                        }
                        if (nnOutput[0] > nnCloudSureSnowSeparationValue) {
                            // this would be as 'SNOW/ICE'...
                            cloudFlagTargetTile.setSample(x, y, IdepixConstants.F_CLEAR_SNOW, true);
                        }
                    }
                    nnTargetTile.setSample(x, y, nnOutput[0]);

                    for (Band band : targetProduct.getBands()) {
                        final Tile targetTile = targetTiles.get(band);
                        setPixelSamples(band, targetTile, y, x, vgtAlgorithm);
                    }
                }
            }
        } catch (Exception e) {
            throw new OperatorException("Failed to provide GA cloud screening:\n" + e.getMessage(), e);
        }
    }

    public void setBands() {
        vgtReflectanceBands = new Band[IdepixConstants.VGT_REFLECTANCE_BAND_NAMES.length];
        for (int i = 0; i < IdepixConstants.VGT_REFLECTANCE_BAND_NAMES.length; i++) {
            vgtReflectanceBands[i] = sourceProduct.getBand(IdepixConstants.VGT_REFLECTANCE_BAND_NAMES[i]);
        }
    }

    void setCloudFlag(Tile targetTile, int y, int x, VgtAlgorithm vgtAlgorithm) {
        // for given instrument, compute boolean pixel properties and write to cloud flag band
        targetTile.setSample(x, y, IdepixConstants.F_INVALID, vgtAlgorithm.isInvalid());
        targetTile.setSample(x, y, IdepixConstants.F_CLOUD, vgtAlgorithm.isCloud());
        targetTile.setSample(x, y, IdepixConstants.F_CLOUD_SURE, vgtAlgorithm.isCloud());
        targetTile.setSample(x, y, IdepixConstants.F_CLOUD_SHADOW, false); // not computed here
        targetTile.setSample(x, y, IdepixConstants.F_CLEAR_LAND, vgtAlgorithm.isClearLand());
        targetTile.setSample(x, y, IdepixConstants.F_CLEAR_WATER, vgtAlgorithm.isClearWater());
        targetTile.setSample(x, y, IdepixConstants.F_CLEAR_SNOW, vgtAlgorithm.isClearSnow());
        targetTile.setSample(x, y, IdepixConstants.F_LAND, vgtAlgorithm.isLand());
        targetTile.setSample(x, y, IdepixConstants.F_WATER, vgtAlgorithm.isWater());
        targetTile.setSample(x, y, IdepixConstants.F_BRIGHT, vgtAlgorithm.isBright());
        targetTile.setSample(x, y, IdepixConstants.F_WHITE, vgtAlgorithm.isWhite());
        targetTile.setSample(x, y, IdepixConstants.F_BRIGHTWHITE, vgtAlgorithm.isBrightWhite());
        targetTile.setSample(x, y, IdepixConstants.F_HIGH, vgtAlgorithm.isHigh());
        targetTile.setSample(x, y, IdepixConstants.F_VEG_RISK, vgtAlgorithm.isVegRisk());
        targetTile.setSample(x, y, IdepixConstants.F_SEAICE, vgtAlgorithm.isSeaIce());
    }

    void setPixelSamples(Band band, Tile targetTile, int y, int x,
                         VgtAlgorithm vgtAlgorithm) {
        // for given instrument, compute more pixel properties and write to distinct band
        if (band == brightBand) {
            targetTile.setSample(x, y, vgtAlgorithm.brightValue());
        } else if (band == whiteBand) {
            targetTile.setSample(x, y, vgtAlgorithm.whiteValue());
        } else if (band == brightWhiteBand) {
            targetTile.setSample(x, y, vgtAlgorithm.brightValue() + vgtAlgorithm.whiteValue());
        } else if (band == temperatureBand) {
            targetTile.setSample(x, y, vgtAlgorithm.temperatureValue());
        } else if (band == spectralFlatnessBand) {
            targetTile.setSample(x, y, vgtAlgorithm.spectralFlatnessValue());
        } else if (band == ndviBand) {
            targetTile.setSample(x, y, vgtAlgorithm.ndviValue());
        } else if (band == ndsiBand) {
            targetTile.setSample(x, y, vgtAlgorithm.ndsiValue());
        } else if (band == glintRiskBand) {
            targetTile.setSample(x, y, vgtAlgorithm.glintRiskValue());
        } else if (band == radioLandBand) {
            targetTile.setSample(x, y, vgtAlgorithm.radiometricLandValue());
        } else if (band == radioWaterBand) {
            targetTile.setSample(x, y, vgtAlgorithm.radiometricWaterValue());
        }
    }


    public void extendTargetProduct() throws OperatorException {
        if (copyToaReflectances) {
            copyVgtReflectances();
            ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        }

        if (copyAnnotations) {
            copyVgtAnnotations();
        }

        if (outputSchillerNNValue) {
            targetProduct.addBand("vgt_nn_value", ProductData.TYPE_FLOAT32);
        }
    }

    private void copyVgtAnnotations() {
        for (String bandName : IdepixConstants.VGT_ANNOTATION_BAND_NAMES) {
            ProductUtils.copyBand(bandName, sourceProduct, targetProduct, true);
        }
    }

    private void copyVgtReflectances() {
        for (int i = 0; i < IdepixConstants.VGT_REFLECTANCE_BAND_NAMES.length; i++) {
            // write the original reflectance bands:
            ProductUtils.copyBand(IdepixConstants.VGT_REFLECTANCE_BAND_NAMES[i], sourceProduct,
                                  targetProduct, true);
        }
    }

    private VgtAlgorithm createVgtAlgorithm(Tile smFlagTile, Tile[] vgtReflectanceTiles,
                                            float[] vgtReflectance,
                                            byte watermaskFraction,
                                            int y, int x) {

        VgtAlgorithm gaAlgorithm = new VgtAlgorithm();

        for (int i = 0; i < IdepixConstants.VGT_REFLECTANCE_BAND_NAMES.length; i++) {
            vgtReflectance[i] = vgtReflectanceTiles[i].getSampleFloat(x, y);
        }

        checkVgtReflectanceQuality(vgtReflectance, smFlagTile, x, y);
        float[] vgtReflectanceSaturationCorrected = IdepixUtils.correctSaturatedReflectances(vgtReflectance);
        gaAlgorithm.setRefl(vgtReflectanceSaturationCorrected);

        SchillerNeuralNetWrapper nnWrapper = vgtNeuralNet.get();
        double[] inputVector = nnWrapper.getInputVector();
        for (int i = 0; i < inputVector.length; i++) {
            inputVector[i] = Math.sqrt(vgtReflectanceSaturationCorrected[i]);
        }
        gaAlgorithm.setNnOutput(nnWrapper.getNeuralNet().calc(inputVector));

        if (useL1bLandWaterFlag) {
            final boolean isLand = smFlagTile.getSampleBit(x, y, SM_F_LAND);
            gaAlgorithm.setSmLand(isLand);
            gaAlgorithm.setIsWater(!isLand);
        } else {
            final boolean isLand = smFlagTile.getSampleBit(x, y, SM_F_LAND) &&
                    watermaskFraction < WATERMASK_FRACTION_THRESH;
            gaAlgorithm.setSmLand(isLand);
            setIsWaterByFraction(watermaskFraction, gaAlgorithm);
        }

        return gaAlgorithm;
    }

    void setIsWaterByFraction(byte watermaskFraction, AbstractPixelProperties pixelProperties) {
        boolean isWater;
        if (watermaskFraction == WatermaskClassifier.INVALID_VALUE) {
            // fallback
            isWater = pixelProperties.isL1Water();
        } else {
            isWater = watermaskFraction >= WATERMASK_FRACTION_THRESH;
        }
        pixelProperties.setIsWater(isWater);
    }


    private void checkVgtReflectanceQuality(float[] vgtReflectance, Tile smFlagTile, int x, int y) {
        final boolean isB0Good = smFlagTile.getSampleBit(x, y, SM_F_B0_GOOD);
        final boolean isB2Good = smFlagTile.getSampleBit(x, y, SM_F_B2_GOOD);
        final boolean isB3Good = smFlagTile.getSampleBit(x, y, SM_F_B3_GOOD);
        final boolean isMirGood = smFlagTile.getSampleBit(x, y, SM_F_MIR_GOOD) || vgtReflectance[3] <= 0.65; // MIR_refl
        if (!isB0Good || !isB2Good || !isB3Good || !isMirGood) {
            for (int i = 0; i < vgtReflectance.length; i++) {
                vgtReflectance[i] = Float.NaN;
            }
        }
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(VgtClassificationOp.class);
        }
    }
}
