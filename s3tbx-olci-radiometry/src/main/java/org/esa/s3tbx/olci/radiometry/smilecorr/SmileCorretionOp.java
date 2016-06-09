/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.olci.radiometry.smilecorr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.RsMathUtils;

import java.awt.*;


/**
 * @author muhammad.bc
 */
@OperatorMetadata(alias = "Olci.SmileCorrection",
        description = "Performs radiometric corrections on OLCI L1b data products.",
        authors = " Marco Peters ,Muhammad Bala (Brockmann Consult)",
        copyright = "(c) 2015 by Brockmann Consult",
        category = "Optical/Pre-Processing",
        version = "1.2")
public class SmileCorretionOp extends Operator {


    public static final String WATER_EXPRESSION = "not quality_flags_land";
    public static final String SZA = "SZA";
    private static final String LAMBDA0_BAND_NAME_PATTERN = "lambda0_band_%d";
    private static final String SOLAR_FLUX_BAND_NAME_PATTERN = "solar_flux_band_%d";
    private static final String OA_RADIANCE_BAND_NAME_PATTERN = "Oa%02d_radiance";
    private static final String OA_RADIANCE_ERR_BAND_NAME_PATTERN = "Oa%02d_radiance_err";
    public static final String FWHM_BAND_PATTERN = "FWHM_band_%d";
    public static final String ALTITUDE_BAND = "altitude";
    public static final String LATITUDE_BAND = "latitude";
    public static final String LONGITUDE_BAND = "longitude";
    public static final String DETECTOR_INDEX_BAND = "detector_index";
    public static final String FRAME_OFFSET_BAND = "frame_offset";
    public static final String BEFORE_BAND = "before";
    ;
    private static SmileCorrectionAuxdata smileAuxdata = new SmileCorrectionAuxdata();
    private Mask waterMask;

    @SourceProduct(alias = "source", label = "Name", description = "The source product.")
    private Product sourceProduct;

    @Override
    public void initialize() throws OperatorException {
        if (!sourceProduct.isCompatibleBandArithmeticExpression(WATER_EXPRESSION)) {
            throw new OperatorException("Can not evaluate expression'" + WATER_EXPRESSION + "' on source product");
        }

        // Configure the target
        Product targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        copyTargetBands(targetProduct, OA_RADIANCE_BAND_NAME_PATTERN);
        copyTargetBands(targetProduct, OA_RADIANCE_ERR_BAND_NAME_PATTERN);
        copyTargetBands(targetProduct, LAMBDA0_BAND_NAME_PATTERN);
        copyTargetBands(targetProduct, FWHM_BAND_PATTERN);
        copyTargetBands(targetProduct, SOLAR_FLUX_BAND_NAME_PATTERN);
        copyTargetBand(targetProduct, ALTITUDE_BAND);
        copyTargetBand(targetProduct, LATITUDE_BAND);
        copyTargetBand(targetProduct, LONGITUDE_BAND);
        copyTargetBand(targetProduct, DETECTOR_INDEX_BAND);
        copyTargetBand(targetProduct, FRAME_OFFSET_BAND);
        copyTargetBand(targetProduct, BEFORE_BAND);



        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());
        setTargetProduct(targetProduct);


        waterMask = Mask.BandMathsType.create("__water_mask", null,
                getSourceProduct().getSceneRasterWidth(),
                getSourceProduct().getSceneRasterHeight(),
                WATER_EXPRESSION,
                Color.GREEN, 0.0);
        waterMask.setOwner(getSourceProduct());
    }

    private void copyTargetBand(Product targetProduct, String bandName) {
        if (sourceProduct.containsBand(bandName)){
            ProductUtils.copyBand(bandName,sourceProduct,targetProduct,true);
        }
    }

    private void copyTargetBands(Product targetProduct, String bandNamePattern) {
        for (int i = 1; i <= 21; i++) {
            Band targetBand = targetProduct.addBand(String.format(bandNamePattern, i), ProductData.TYPE_FLOAT32);
            Band sourceBand = sourceProduct.getBand(String.format(bandNamePattern, i));
            targetBand.setSpectralWavelength(sourceBand.getSpectralWavelength());
            targetBand.setSpectralBandwidth(sourceBand.getSpectralBandwidth());
            targetBand.setSpectralBandIndex(sourceBand.getSpectralBandIndex());
            targetBand.setNoDataValueUsed(true);
            targetBand.setNoDataValue(Double.NaN);
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle rectangle = targetTile.getRectangle();
        String targetBandName = targetBand.getName();
        int targetBandIndex = Integer.parseInt(targetBandName.substring(2, 4)) - 1;


        Tile szaTile = getSourceTile(sourceProduct.getTiePointGrid(SZA), rectangle);
        Tile sourceRadianceTile = getSourceTile(sourceProduct.getBand(targetBandName), rectangle);
        Band effectiveSolarIrradianceBand = sourceProduct.getBand(String.format(SOLAR_FLUX_BAND_NAME_PATTERN, targetBandIndex + 1));
        Tile solarIrradianceTile = getSourceTile(effectiveSolarIrradianceBand, rectangle);

        int waterLowerBandIndex = smileAuxdata.getWaterLowerBands()[targetBandIndex];
        int waterUpperBandIndex = smileAuxdata.getWaterUpperBands()[targetBandIndex];
        int landLowerBandIndex = smileAuxdata.getLandLowerBands()[targetBandIndex];
        int landUpperBandIndex = smileAuxdata.getLandUpperBands()[targetBandIndex];

        Band lambdaWaterLowerBand = sourceProduct.getBand(String.format(LAMBDA0_BAND_NAME_PATTERN, waterLowerBandIndex));
        Band radianceWaterLowerBand = sourceProduct.getBand(String.format(OA_RADIANCE_BAND_NAME_PATTERN, waterLowerBandIndex));
        Band solarIrradianceWaterLowerBand = sourceProduct.getBand(String.format(SOLAR_FLUX_BAND_NAME_PATTERN, waterLowerBandIndex));
        Band lambdaWaterUpperBand = sourceProduct.getBand(String.format(LAMBDA0_BAND_NAME_PATTERN, waterUpperBandIndex));
        Band radianceWaterUpperBand = sourceProduct.getBand(String.format(OA_RADIANCE_BAND_NAME_PATTERN, waterUpperBandIndex));
        Band solarIrradianceWaterUpperBand = sourceProduct.getBand(String.format(SOLAR_FLUX_BAND_NAME_PATTERN, waterUpperBandIndex));
        Band lambdaLandLowerBand = sourceProduct.getBand(String.format(LAMBDA0_BAND_NAME_PATTERN, landLowerBandIndex));
        Band radianceLandLowerBand = sourceProduct.getBand(String.format(OA_RADIANCE_BAND_NAME_PATTERN, landLowerBandIndex));
        Band solarIrradianceLandLowerBand = sourceProduct.getBand(String.format(SOLAR_FLUX_BAND_NAME_PATTERN, landLowerBandIndex));
        Band lambdaLandUpperBand = sourceProduct.getBand(String.format(LAMBDA0_BAND_NAME_PATTERN, landUpperBandIndex));
        Band radianceLandUpperBand = sourceProduct.getBand(String.format(OA_RADIANCE_BAND_NAME_PATTERN, landUpperBandIndex));
        Band solarIrradianceLandUpperBand = sourceProduct.getBand(String.format(SOLAR_FLUX_BAND_NAME_PATTERN, landUpperBandIndex));


        Band lambdaSourceBand = sourceProduct.getBand(String.format(LAMBDA0_BAND_NAME_PATTERN, targetBandIndex + 1));

        boolean correctLand = smileAuxdata.getLandRefCorrectionSwitches()[targetBandIndex];
        boolean correctWater = smileAuxdata.getWaterRefCorrectionSwitches()[targetBandIndex];

        if (!correctLand && !correctWater) {
            float[] samplesFloat = sourceRadianceTile.getSamplesFloat();
            targetTile.setSamples(samplesFloat);
            return;
        }

        SmileTiles waterTiles = null;
        if (correctWater) {
            waterTiles = new SmileTiles(lambdaWaterLowerBand, radianceWaterLowerBand, solarIrradianceWaterLowerBand, lambdaWaterUpperBand,
                    radianceWaterUpperBand, solarIrradianceWaterUpperBand, rectangle);
        }
        SmileTiles landTiles = null;
        if (correctLand) {
            landTiles = new SmileTiles(lambdaLandLowerBand, radianceLandLowerBand, solarIrradianceLandLowerBand, lambdaLandUpperBand,
                    radianceLandUpperBand, solarIrradianceLandUpperBand, rectangle);
        }

        Tile lambdaSourceTile = getSourceTile(lambdaSourceBand, rectangle);

        final Tile waterMaskTile = getSourceTile(waterMask, rectangle);
        float refSolarIrradiance = smileAuxdata.getSolarIrradiances()[targetBandIndex];
        final float[] refCentralWaveLengths = smileAuxdata.getRefCentralWaveLengths();
        float refCentralWaveLength = refCentralWaveLengths[targetBandIndex];

        for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
            if (pm.isCanceled()) {
                return;
            }
            for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
                if (!sourceRadianceTile.isSampleValid(x, y)) {
                    continue;
                }
                float sourceRadiance = sourceRadianceTile.getSampleFloat(x, y);
                // For debug purpose
//                if (x == 600 && y == 10) {
//                    System.out.println("HELLO!");
//                }

                if (waterMaskTile.getSampleBoolean(x, y)) {
                    if (correctWater) {
                        float correctedRadiance = correctForSmileEffect(sourceRadiance, solarIrradianceTile, lambdaSourceTile,
                                refSolarIrradiance, refCentralWaveLength, waterTiles,
                                szaTile, x, y);
                        targetTile.setSample(x, y, correctedRadiance);
                    } else {
                        targetTile.setSample(x, y, sourceRadiance);
                    }
                } else {
                    if (correctLand) {
                        float correctedRadiance = correctForSmileEffect(sourceRadiance, solarIrradianceTile, lambdaSourceTile,
                                refSolarIrradiance, refCentralWaveLength, landTiles,
                                szaTile, x, y);
                        targetTile.setSample(x, y, correctedRadiance);
                    } else {
                        targetTile.setSample(x, y, sourceRadiance);
                    }
                }

            }
        }
    }

    private float correctForSmileEffect(float radiance, Tile solarIrradianceTile, Tile effectWavelengthTargetTile, float refSolarIrradiance,
                                        float refCentralWaveLength, SmileTiles smileTiles,
                                        Tile szaTile, int x, int y) {
        float sza = szaTile.getSampleFloat(x, y);
        float sourceLambda = effectWavelengthTargetTile.getSampleFloat(x, y);

        float sourceReflectance = convertRadToRefl(radiance, solarIrradianceTile.getSampleFloat(x, y), sza);
        float lowerReflectance = convertRadToRefl(smileTiles.getLowerRadianceTile().getSampleFloat(x, y),
                smileTiles.getLowerSolarIrradianceTile().getSampleFloat(x, y),
                sza);
        float upperReflectance = convertRadToRefl(smileTiles.getUpperRadianceTile().getSampleFloat(x, y),
                smileTiles.getUpperSolarIrradianceTile().getSampleFloat(x, y),
                sza);
        float lowerLambda = smileTiles.getLowerLambdaTile().getSampleFloat(x, y);
        float upperLambda = smileTiles.getUpperLambdaTile().getSampleFloat(x, y);
        float correctedReflectance = SmileCorrectionAlgorithm.correct(sourceReflectance, lowerReflectance, upperReflectance,
                sourceLambda, lowerLambda, upperLambda,
                refCentralWaveLength);
        return convertReflToRad(correctedReflectance, sza, refSolarIrradiance);
    }

    private float convertRadToRefl(float radiance, float solarIrradiance, float sza) {
        return RsMathUtils.radianceToReflectance(radiance, sza, solarIrradiance);
    }

    private float convertReflToRad(float refl, float sza, float solarIrradiance) {
        return RsMathUtils.reflectanceToRadiance(refl, sza, solarIrradiance);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SmileCorretionOp.class);
        }
    }

    private class SmileTiles {

        private Tile lowerLambdaTile;
        private Tile upperLambdaTile;
        private Tile lowerRadianceTile;
        private Tile upperRadianceTile;
        private Tile lowerSolarIrradianceTile;
        private Tile upperSolarIrradianceTile;

        public SmileTiles(Band lowerLambdaBand, Band lowerRadianceBand, Band lowerSolarIrradianceBand,
                          Band upperLambdaBand, Band upperRadianceBand, Band upperSolarIrradianceBand,
                          Rectangle rectangle) {
            lowerLambdaTile = getSourceTile(lowerLambdaBand, rectangle);
            upperLambdaTile = getSourceTile(upperLambdaBand, rectangle);
            lowerRadianceTile = getSourceTile(lowerRadianceBand, rectangle);
            upperRadianceTile = getSourceTile(upperRadianceBand, rectangle);
            lowerSolarIrradianceTile = getSourceTile(lowerSolarIrradianceBand, rectangle);
            upperSolarIrradianceTile = getSourceTile(upperSolarIrradianceBand, rectangle);
        }

        public Tile getLowerLambdaTile() {
            return lowerLambdaTile;
        }

        public Tile getUpperLambdaTile() {
            return upperLambdaTile;
        }

        public Tile getLowerRadianceTile() {
            return lowerRadianceTile;
        }

        public Tile getUpperRadianceTile() {
            return upperRadianceTile;
        }

        public Tile getLowerSolarIrradianceTile() {
            return lowerSolarIrradianceTile;
        }

        public Tile getUpperSolarIrradianceTile() {
            return upperSolarIrradianceTile;
        }

    }
}
