package org.esa.s3tbx.idepix.algorithms.meris;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixIO;
import org.esa.s3tbx.idepix.core.util.SchillerNeuralNetWrapper;
import org.esa.s3tbx.processor.rad2refl.Rad2ReflConstants;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.*;
import java.io.InputStream;
import java.util.Map;

/**
 * MERIS pixel classification operator.
 * Only land pixels are classified from NN approach (following BEAM Cawa algorithm).
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Meris.Land",
        version = "1.0",
        internal = true,
        authors = "Olaf Danne",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Idepix land pixel classification operator for MERIS.")
public class MerisLandClassificationOp extends Operator {

    @Parameter(defaultValue = "false",
            label = " Write NN value to the target product.",
            description = " If applied, write Schiller NN value to the target product ")
    private boolean outputSchillerNNValue;

    @Parameter(defaultValue = "2.0",
            label = " NN cloud ambiguous lower boundary",
            description = " NN cloud ambiguous lower boundary")
    double schillerNNCloudAmbiguousLowerBoundaryValue;

    @Parameter(defaultValue = "3.7",
            label = " NN cloud ambiguous/sure separation value",
            description = " NN cloud ambiguous cloud ambiguous/sure separation value")
    double schillerNNCloudAmbiguousSureSeparationValue;

    @Parameter(defaultValue = "4.05",
            label = " NN cloud sure/snow separation value (MERIS only)",
            description = " NN cloud ambiguous cloud sure/snow separation value")
    double schillerNNCloudSureSnowSeparationValue;

    @SourceProduct(alias = "l1b", description = "The source product.")
    Product sourceProduct;
    @SourceProduct(alias = "rhotoa")
    private Product rad2reflProduct;
    @SourceProduct(alias = "waterMask")
    private Product waterMaskProduct;

    @TargetProduct(description = "The target product.")
    Product targetProduct;

    Band cloudFlagBand;

    private Band[] merisReflBands;
    private Band landWaterBand;

    public static final String MERIS_LAND_NET_NAME = "11x8x5x3_1062.5_land.net";
    ThreadLocal<SchillerNeuralNetWrapper> merisLandNeuralNet;

//    static final int MERIS_L1B_F_LAND = 4;
//    static final int MERIS_L1B_F_INVALID = 7;

    @Override
    public void initialize() throws OperatorException {
        setBands();

        readSchillerNeuralNets();
        createTargetProduct();

        landWaterBand = waterMaskProduct.getBand("land_water_fraction");
    }

    private void readSchillerNeuralNets() {
        InputStream merisLandIS = getClass().getResourceAsStream(MERIS_LAND_NET_NAME);
        merisLandNeuralNet = SchillerNeuralNetWrapper.create(merisLandIS);
    }

    public void setBands() {
        merisReflBands = new Band[Rad2ReflConstants.MERIS_REFL_BAND_NAMES.length];
        for (int i = 0; i < EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
            final int suffixStart = Rad2ReflConstants.MERIS_REFL_BAND_NAMES[i].indexOf("_");
            final String reflBandname = Rad2ReflConstants.MERIS_REFL_BAND_NAMES[i].substring(0, suffixStart);
            merisReflBands[i] = rad2reflProduct.getBand(reflBandname + "_" + (i + 1));
        }
    }

    void createTargetProduct() throws OperatorException {
        int sceneWidth = sourceProduct.getSceneRasterWidth();
        int sceneHeight = sourceProduct.getSceneRasterHeight();

        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(), sceneWidth, sceneHeight);

        // shall be the only target band!!
        cloudFlagBand = targetProduct.addBand(IdepixConstants.CLASSIF_BAND_NAME, ProductData.TYPE_INT16);
//        cloudFlagBand = targetProduct.addBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS, ProductData.TYPE_INT8);
        FlagCoding flagCoding = MerisUtils.createMerisFlagCoding(IdepixConstants.CLASSIF_BAND_NAME);
        cloudFlagBand.setSampleCoding(flagCoding);
        targetProduct.getFlagCodingGroup().add(flagCoding);

        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);

        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        ProductUtils.copyMetadata(sourceProduct, targetProduct);

        if (outputSchillerNNValue) {
            targetProduct.addBand(IdepixConstants.NN_OUTPUT_BAND_NAME, ProductData.TYPE_FLOAT32);
        }
    }


    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle rectangle, ProgressMonitor pm) throws OperatorException {
        // MERIS variables
        final Tile waterFractionTile = getSourceTile(landWaterBand, rectangle);

        final Band merisL1bFlagBand = sourceProduct.getBand(EnvisatConstants.MERIS_L1B_FLAGS_DS_NAME);
        final Tile merisL1bFlagTile = getSourceTile(merisL1bFlagBand, rectangle);

        Tile[] merisReflectanceTiles = new Tile[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS];
        float[] merisReflectance = new float[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS];
        for (int i = 0; i < EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
            merisReflectanceTiles[i] = getSourceTile(merisReflBands[i], rectangle);
        }

        final Band cloudFlagTargetBand = targetProduct.getBand(IdepixConstants.CLASSIF_BAND_NAME);
        final Tile cloudFlagTargetTile = targetTiles.get(cloudFlagTargetBand);

        Band nnTargetBand;
        Tile nnTargetTile = null;
        if (outputSchillerNNValue) {
            nnTargetBand = targetProduct.getBand(IdepixConstants.NN_OUTPUT_BAND_NAME);
            nnTargetTile = targetTiles.get(nnTargetBand);
        }
        try {
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                checkForCancellation();
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    final int waterFraction = waterFractionTile.getSampleInt(x, y);
                    initCloudFlag(merisL1bFlagTile, targetTiles.get(cloudFlagTargetBand), merisReflectance, y, x);
                    if (!isLandPixel(x, y, merisL1bFlagTile, waterFraction)) {
                        cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_LAND, false);
                        cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD, false);
                        cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_SNOW_ICE, false);
                        if (nnTargetTile != null) {
                            nnTargetTile.setSample(x, y, Float.NaN);
                        }
                    } else {
                        // only use Schiller NN approach...
                        for (int i = 0; i < EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
                            merisReflectance[i] = merisReflectanceTiles[i].getSampleFloat(x, y);
                        }

                        SchillerNeuralNetWrapper nnWrapper = merisLandNeuralNet.get();
                        double[] inputVector = nnWrapper.getInputVector();
                        for (int i = 0; i < inputVector.length; i++) {
                            inputVector[i] = Math.sqrt(merisReflectance[i]);
                        }

                        final double[] nnOutput = nnWrapper.getNeuralNet().calc(inputVector);

                        if (!cloudFlagTargetTile.getSampleBit(x, y, IdepixConstants.IDEPIX_INVALID)) {
                            cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_AMBIGUOUS, false);
                            cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_SURE, false);
                            cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD, false);
                            cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_SNOW_ICE, false);
                            if (nnOutput[0] > schillerNNCloudAmbiguousLowerBoundaryValue &&
                                    nnOutput[0] <= schillerNNCloudAmbiguousSureSeparationValue) {
                                // this would be as 'CLOUD_AMBIGUOUS'...
                                cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_AMBIGUOUS, true);
                                cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD, true);
                            }
                            if (nnOutput[0] > schillerNNCloudAmbiguousSureSeparationValue &&
                                    nnOutput[0] <= schillerNNCloudSureSnowSeparationValue) {
                                // this would be as 'CLOUD_SURE'...
                                cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_SURE, true);
                                cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD, true);
                            }
                            if (nnOutput[0] > schillerNNCloudSureSnowSeparationValue) {
                                // this would be as 'SNOW/ICE'...
                                cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_SNOW_ICE, true);
                            }
                        }

                        if (nnTargetTile != null) {
                            nnTargetTile.setSample(x, y, nnOutput[0]);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new OperatorException("Failed to provide GA cloud screening:\n" + e.getMessage(), e);
        }
    }

    private boolean isLandPixel(int x, int y, Tile merisL1bFlagTile, int waterFraction) {
        // the water mask ends at 59 Degree south, stop earlier to avoid artefacts
        if (getGeoPos(x, y).lat > -58f) {
            // values bigger than 100 indicate no data
            if (waterFraction <= 100) {
                // todo: this does not work if we have a PixelGeocoding. In that case, waterFraction
                // is always 0 or 100!! (TS, OD, 20140502)
                return waterFraction == 0;
            } else {
                return merisL1bFlagTile.getSampleBit(x, y, MerisConstants.L1_F_LAND);
            }
        } else {
            return merisL1bFlagTile.getSampleBit(x, y, MerisConstants.L1_F_LAND);
        }
    }

    private GeoPos getGeoPos(int x, int y) {
        final GeoPos geoPos = new GeoPos();
        final GeoCoding geoCoding = getSourceProduct().getSceneGeoCoding();
        final PixelPos pixelPos = new PixelPos(x, y);
        geoCoding.getGeoPos(pixelPos, geoPos);
        return geoPos;
    }

    void initCloudFlag(Tile merisL1bFlagTile, Tile targetTile, float[] merisReflectances, int y, int x) {
        // for given instrument, compute boolean pixel properties and write to cloud flag band
        final boolean l1Invalid = merisL1bFlagTile.getSampleBit(x, y, MerisConstants.L1_F_INVALID);
        final boolean reflectancesValid = IdepixIO.areAllReflectancesValid(merisReflectances);

        targetTile.setSample(x, y, IdepixConstants.IDEPIX_INVALID, l1Invalid || !reflectancesValid);
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD, false);
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_SURE, false);
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_AMBIGUOUS, false);
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_SNOW_ICE, false);
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_BUFFER, false);
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_SHADOW, false);
        targetTile.setSample(x, y, MerisConstants.IDEPIX_GLINT_RISK, false);
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_COASTLINE, false);
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_LAND, true);   // already checked
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(MerisLandClassificationOp.class);
        }
    }
}
