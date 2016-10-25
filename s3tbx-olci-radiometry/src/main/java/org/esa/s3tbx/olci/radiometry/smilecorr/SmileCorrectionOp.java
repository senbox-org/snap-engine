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
import org.esa.s3tbx.olci.radiometry.Sensor;
import org.esa.s3tbx.olci.radiometry.gasabsorption.GaseousAbsorptionAux;
import org.esa.s3tbx.olci.radiometry.rayleigh.RayleighAux;
import org.esa.s3tbx.olci.radiometry.rayleigh.RayleighCorrAlgorithm;
import org.esa.s3tbx.olci.radiometry.rayleigh.RayleighInput;
import org.esa.s3tbx.olci.radiometry.rayleigh.RayleighOutput;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.RsMathUtils;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.IOException;

import static org.esa.s3tbx.olci.radiometry.smilecorr.SmileCorrectionUtils.getSampleFloats;
import static org.esa.s3tbx.olci.radiometry.smilecorr.SmileCorrectionUtils.getSensorType;
import static org.esa.s3tbx.olci.radiometry.smilecorr.SmileCorrectionUtils.getSourceBandIndex;


/**
 * @author muhammad.bc
 */
@OperatorMetadata(alias = "SmileCorrection.Olci",
        description = "Performs radiometric corrections on OLCI L1b data products.",
        authors = " Marco Peters ,Muhammad Bala (Brockmann Consult)",
        copyright = "(c) 2015 by Brockmann Consult",
        category = "Optical/Pre-Processing",
        version = "1.2")
public class SmileCorrectionOp extends Operator {


    public static final String WATER_EXPRESSION = "not quality_flags_land";
    public static final String SZA = "SZA";
    public static final String OZA = "OZA";
    public static final String FWHM_BAND_PATTERN = "FWHM_band_%d";
    public static final String ALTITUDE_BAND = "altitude";
    public static final String LATITUDE_BAND = "latitude";
    public static final String LONGITUDE_BAND = "longitude";
    public static final String DETECTOR_INDEX_BAND = "detector_index";
    public static final String OLCI_SENSOR = "OLCI";
    public static final int DO_NOT_CORRECT_BAND = -1;
    private static final String LAMBDA0_BAND_NAME_PATTERN = "lambda0_band_%d";
    private static final String SOLAR_FLUX_BAND_NAME_PATTERN = "solar_flux_band_%d";
    private static final String OA_RADIANCE_BAND_NAME_PATTERN = "Oa%02d_radiance";
    private static final String OA_RADIANCE_ERR_BAND_NAME_PATTERN = "Oa%02d_radiance_err";

    private Mask waterMask;

    @SourceProduct(alias = "source", label = "Name", description = "The source product.")
    private Product sourceProduct;

    @Parameter(defaultValue = "false", description = "Execute Rayleigh Operator", label = "Execute Rayleigh Operator")
    private boolean isRalyeighOperator;

    private RayleighCorrAlgorithm rayleighCorrAlgorithm;
    private double[] absorpOzone;
    private Sensor sensor;
    private SmileCorrectionAuxdata smileAuxdata;

    @Override
    public void initialize() throws OperatorException {
        sensor = getSensorType(getSourceProduct());
        smileAuxdata = new SmileCorrectionAuxdata(sensor);
        if (Sensor.MERIS.equals(sensor)) {
            try {
                smileAuxdata.loadFluxWaven(sourceProduct.getProductType());
            } catch (IOException e) {
                throw new OperatorException(e);
            }
        }

        if (isRalyeighOperator) {
            RayleighAux.initDefaultAuxiliary();
            rayleighCorrAlgorithm = new RayleighCorrAlgorithm(sensor.getNamePattern(), sensor.getNumBands());
            absorpOzone = GaseousAbsorptionAux.getInstance().absorptionOzone(OLCI_SENSOR);
        }
        Product targetProduct = createTargetBands(sensor);
        setTargetProduct(targetProduct);
        waterMask = Mask.BandMathsType.create("__water_mask", null,
                                              getSourceProduct().getSceneRasterWidth(),
                                              getSourceProduct().getSceneRasterHeight(),
                                              WATER_EXPRESSION,
                                              Color.GREEN, 0.0);
        waterMask.setOwner(getSourceProduct());
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        checkForCancellation();
        String targetBandName = targetBand.getName();
        int targetBandIndex = getSourceBandIndex(targetBandName);

        if (Sensor.MERIS == sensor) {
            correctRadMeris(targetTile, targetBandName, targetBandIndex, smileAuxdata, pm);
        } else if (Sensor.OLCI == sensor) {
            if (targetBandName.matches("Oa\\d{2}_radiance")) {
                correctRad(targetTile, targetBandName, targetBandIndex, sensor, pm);
            }

            if (targetBandName.matches("lambda0_band_\\d+")) {
                correctLambda(targetTile, targetBandName, targetBandIndex, pm);
            }

            if (targetBandName.matches("solar_flux_band_\\d+")) {
                correctSolarFlux(targetTile, targetBandName, pm);
            }
        }

    }

    private Product createTargetBands(Sensor sensor) {
        // Configure the target
        Product targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                            sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());


        boolean[] landRefCorrectionSwitches = smileAuxdata.getLandRefCorrectionSwitches();
        boolean[] waterRefCorrectionSwitches = smileAuxdata.getWaterRefCorrectionSwitches();
        float[] refCentralWaveLengths = smileAuxdata.getRefCentralWaveLengths();


        if (Sensor.OLCI == sensor) {
            createTargetBands(targetProduct, sensor.getNamePattern(), landRefCorrectionSwitches, waterRefCorrectionSwitches, refCentralWaveLengths);
            createTargetLambda(targetProduct, LAMBDA0_BAND_NAME_PATTERN, landRefCorrectionSwitches, waterRefCorrectionSwitches, refCentralWaveLengths);
            createTargetBands(targetProduct, SOLAR_FLUX_BAND_NAME_PATTERN, landRefCorrectionSwitches, waterRefCorrectionSwitches, refCentralWaveLengths);
            copyTargetBandsImage(targetProduct, OA_RADIANCE_ERR_BAND_NAME_PATTERN, sensor.getNumBands());
            copyTargetBandsImage(targetProduct, FWHM_BAND_PATTERN, sensor.getNumBands());
            copyTargetBandImage(targetProduct, ALTITUDE_BAND);
            copyTargetBandImage(targetProduct, LATITUDE_BAND);
            copyTargetBandImage(targetProduct, LONGITUDE_BAND);
        } else if (Sensor.MERIS == sensor) {
            createTargetBands(targetProduct, sensor.getNamePattern(), landRefCorrectionSwitches, waterRefCorrectionSwitches, refCentralWaveLengths);
            copyTargetBandImage(targetProduct, DETECTOR_INDEX_BAND);
        }
        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
        return targetProduct;
    }

    private void createTargetLambda(Product targetProduct, String lambdaBandNamePattern, boolean[] landRefCorrectionSwitches, boolean[] waterRefCorrectionSwitches, float[] refCentralWaveLengths) {
        for (int i = 0; i < refCentralWaveLengths.length; i++) {
            String sourceBandName = String.format(lambdaBandNamePattern, i + 1);
            if (landRefCorrectionSwitches[i] && waterRefCorrectionSwitches[i]) {
                RenderedOp image = ConstantDescriptor.create((float) sourceProduct.getSceneRasterWidth(), (float) sourceProduct.getSceneRasterHeight(), new Float[]{refCentralWaveLengths[i]}, null);
                ProductUtils.copyBand(sourceBandName, sourceProduct, targetProduct, false);
                targetProduct.getBand(sourceBandName).setSourceImage(image);
            } else if (!landRefCorrectionSwitches[i] && !waterRefCorrectionSwitches[i]) {
                ProductUtils.copyBand(sourceBandName, sourceProduct, targetProduct, true);
            } else {
                createTargetBand(targetProduct, sourceBandName);
            }
        }
    }


    private void copyTargetBandsImage(Product targetProduct, String bandNamePattern, int numBand) {
        for (int i = 1; i <= numBand; i++) {
            String sourceBandName = String.format(bandNamePattern, i);
            copyTargetBandImage(targetProduct, sourceBandName);
        }
    }

    private void copyTargetBandImage(Product targetProduct, String bandName) {
        if (sourceProduct.containsBand(bandName)) {
            ProductUtils.copyBand(bandName, sourceProduct, targetProduct, true);
        }
    }

    private void createTargetBands(Product targetProduct, String bandNamePattern, boolean[] landRefCorrectionSwitches, boolean[] waterRefCorrectionSwitches, float[] refCentralWaveLengths) {
        for (int i = 0; i < refCentralWaveLengths.length; i++) {
            String sourceBandName = String.format(bandNamePattern, i + 1);
            if (landRefCorrectionSwitches[i] || waterRefCorrectionSwitches[i]) {
                createTargetBand(targetProduct, sourceBandName);
            } else if (!landRefCorrectionSwitches[i] && !waterRefCorrectionSwitches[i]) {
                ProductUtils.copyBand(sourceBandName, sourceProduct, targetProduct, true);
            }
        }
    }

    private void createTargetBand(Product targetProduct, String bandNamePattern) {
        Band targetBand = targetProduct.addBand(bandNamePattern, ProductData.TYPE_FLOAT32);
        Band sourceBand = sourceProduct.getBand(bandNamePattern);
        targetBand.setSpectralWavelength(sourceBand.getSpectralWavelength());
        targetBand.setSpectralBandwidth(sourceBand.getSpectralBandwidth());
        targetBand.setSpectralBandIndex(sourceBand.getSpectralBandIndex());
        targetBand.setNoDataValueUsed(true);
        targetBand.setNoDataValue(Double.NaN);
    }

    private void correctSolarFlux(Tile targetTile, String targetBandName, ProgressMonitor pm) {
        Rectangle rectangle = targetTile.getRectangle();

        String extractBandIndex = targetBandName.substring(16, targetBandName.length());
        int targetBandIndex = Integer.parseInt(extractBandIndex) - 1;

        boolean correctLand = smileAuxdata.getLandRefCorrectionSwitches()[targetBandIndex];
        boolean correctWater = smileAuxdata.getWaterRefCorrectionSwitches()[targetBandIndex];

        Band lambdaSourceBand = sourceProduct.getBand(String.format(LAMBDA0_BAND_NAME_PATTERN, targetBandIndex + 1));
        Band effectiveSolarIrradianceBand = sourceProduct.getBand(targetBandName);

        Tile sourceLambdaTile = getSourceTile(lambdaSourceBand, rectangle);
        Tile solarIrradianceTile = getSourceTile(effectiveSolarIrradianceBand, rectangle);
        Tile waterMaskTile = getSourceTile(waterMask, rectangle);

        float refCentralWaveLength = smileAuxdata.getRefCentralWaveLengths()[targetBandIndex];
        for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
            if (pm.isCanceled()) {
                return;
            }
            for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
                float solarIrradianceSample = solarIrradianceTile.getSampleFloat(x, y);
                float sourceTargetLambda = sourceLambdaTile.getSampleFloat(x, y);
                if (sourceTargetLambda == -1 || solarIrradianceSample == -1) {
                    continue;
                }
                if (waterMaskTile.getSampleBoolean(x, y)) {
                    if (correctWater) {
                        float shiftedSolarIrradiance = shiftSolarIrradiance(solarIrradianceSample, sourceTargetLambda, refCentralWaveLength);
                        targetTile.setSample(x, y, shiftedSolarIrradiance);
                    } else {
                        targetTile.setSample(x, y, solarIrradianceSample);
                    }
                } else {
                    if (correctLand) {
                        float shiftedSolarIrradiance = shiftSolarIrradiance(solarIrradianceSample, sourceTargetLambda, refCentralWaveLength);
                        targetTile.setSample(x, y, shiftedSolarIrradiance);
                    } else {
                        targetTile.setSample(x, y, solarIrradianceSample);
                    }
                }
            }
        }
    }

    private void correctLambda(Tile targetTile, String targetBandName, int targetBandIndex, ProgressMonitor pm) {
        Rectangle rectangle = targetTile.getRectangle();
        Tile sourceLambdaTile = getSourceTile(sourceProduct.getBand(targetBandName), rectangle);
        boolean correctLand = smileAuxdata.getLandRefCorrectionSwitches()[targetBandIndex];
        boolean correctWater = smileAuxdata.getWaterRefCorrectionSwitches()[targetBandIndex];

        float refCentralWaveLength = smileAuxdata.getRefCentralWaveLengths()[targetBandIndex];
        final Tile waterMaskTile = getSourceTile(waterMask, rectangle);
        for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
            if (pm.isCanceled()) {
                return;
            }
            for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
                if (waterMaskTile.getSampleBoolean(x, y)) {
                    if (correctWater) {
                        targetTile.setSample(x, y, refCentralWaveLength);
                    } else {
                        targetTile.setSample(x, y, sourceLambdaTile.getSampleFloat(x, y));
                    }
                } else {
                    if (correctLand) {
                        targetTile.setSample(x, y, refCentralWaveLength);
                    } else {
                        targetTile.setSample(x, y, sourceLambdaTile.getSampleFloat(x, y));
                    }
                }
            }
        }
    }

    private void correctRadMeris(Tile targetTile, String targetBandName, int targetBandIndex, SmileCorrectionAuxdata smileAuxdata, ProgressMonitor pm) {
        checkForCancellation();
        Rectangle rectangle = targetTile.getRectangle();

        boolean correctLand = smileAuxdata.getLandRefCorrectionSwitches()[targetBandIndex];
        boolean correctWater = this.smileAuxdata.getWaterRefCorrectionSwitches()[targetBandIndex];

        Tile sourceRadianceTile = getSourceTile(sourceProduct.getBand(targetBandName), rectangle);
        if (!correctLand && !correctWater) {
            float[] samplesFloat = sourceRadianceTile.getSamplesFloat();
            targetTile.setSamples(samplesFloat);
            return;
        }
        PrepareSmileCorrection waterTileValues = null;
        PrepareSmileCorrection landTileValues = null;
        if (correctWater) {
            waterTileValues = new MerisSmile(this.smileAuxdata, rectangle, targetBandIndex, SmileType.WATER);
        }
        if (correctLand) {
            landTileValues = new MerisSmile(this.smileAuxdata, rectangle, targetBandIndex, SmileType.LAND);
        }

        float refCentralWaveLength = this.smileAuxdata.getRefCentralWaveLengths()[targetBandIndex];
        Tile szaTile = getSourceTile(sourceProduct.getTiePointGrid("sun_zenith"), rectangle);
        if (correctWater) {
            float[] correctForSmileEffect = correctForSmileEffect(sourceRadianceTile, refCentralWaveLength, waterTileValues, szaTile, targetBandIndex);
            targetTile.setSamples(correctForSmileEffect);
            return;
        }
        if (correctLand) {
            float[] correctedRadiance = correctForSmileEffect(sourceRadianceTile, refCentralWaveLength, landTileValues, szaTile, targetBandIndex);
            targetTile.setSamples(correctedRadiance);
            return;
        }

        float[] sourceRadiance = sourceRadianceTile.getSamplesFloat();
        targetTile.setSamples(sourceRadiance);
    }


    private void correctRad(Tile targetTile, String targetBandName, int targetBandIndex, Sensor sensor, ProgressMonitor pm) {
        checkForCancellation();
        Rectangle rectangle = targetTile.getRectangle();
        Tile szaTile = getSourceTile(sourceProduct.getTiePointGrid(sensor.getSZA()), rectangle);
        Tile sourceRadianceTile = getSourceTile(sourceProduct.getBand(targetBandName), rectangle);

        Band sourceSolarIrradianceBand = sourceProduct.getBand(String.format(SOLAR_FLUX_BAND_NAME_PATTERN, targetBandIndex + 1));
        int waterLowerBandIndex = smileAuxdata.getWaterLowerBands()[targetBandIndex];
        int waterUpperBandIndex = smileAuxdata.getWaterUpperBands()[targetBandIndex];
        int landLowerBandIndex = smileAuxdata.getLandLowerBands()[targetBandIndex];
        int landUpperBandIndex = smileAuxdata.getLandUpperBands()[targetBandIndex];


        Band lambdaWaterLowerBand = sourceProduct.getBand(String.format(LAMBDA0_BAND_NAME_PATTERN, waterLowerBandIndex));
        Band lambdaWaterUpperBand = sourceProduct.getBand(String.format(LAMBDA0_BAND_NAME_PATTERN, waterUpperBandIndex));
        Band lambdaLandLowerBand = sourceProduct.getBand(String.format(LAMBDA0_BAND_NAME_PATTERN, landLowerBandIndex));
        Band lambdaLandUpperBand = sourceProduct.getBand(String.format(LAMBDA0_BAND_NAME_PATTERN, landUpperBandIndex));


        Band solarIrradianceWaterLowerBand = sourceProduct.getBand(String.format(SOLAR_FLUX_BAND_NAME_PATTERN, waterLowerBandIndex));
        Band solarIrradianceWaterUpperBand = sourceProduct.getBand(String.format(SOLAR_FLUX_BAND_NAME_PATTERN, waterUpperBandIndex));
        Band solarIrradianceLandLowerBand = sourceProduct.getBand(String.format(SOLAR_FLUX_BAND_NAME_PATTERN, landLowerBandIndex));
        Band solarIrradianceLandUpperBand = sourceProduct.getBand(String.format(SOLAR_FLUX_BAND_NAME_PATTERN, landUpperBandIndex));


        Band radianceWaterLowerBand = sourceProduct.getBand(String.format(OA_RADIANCE_BAND_NAME_PATTERN, waterLowerBandIndex));
        Band radianceWaterUpperBand = sourceProduct.getBand(String.format(OA_RADIANCE_BAND_NAME_PATTERN, waterUpperBandIndex));
        Band radianceLandLowerBand = sourceProduct.getBand(String.format(OA_RADIANCE_BAND_NAME_PATTERN, landLowerBandIndex));
        Band radianceLandUpperBand = sourceProduct.getBand(String.format(OA_RADIANCE_BAND_NAME_PATTERN, landUpperBandIndex));


        Band lambdaSourceBand = sourceProduct.getBand(String.format(LAMBDA0_BAND_NAME_PATTERN, targetBandIndex + 1));

        boolean correctLand = smileAuxdata.getLandRefCorrectionSwitches()[targetBandIndex];
        boolean correctWater = smileAuxdata.getWaterRefCorrectionSwitches()[targetBandIndex];


        if (!correctLand && !correctWater) {
            float[] samplesFloat = sourceRadianceTile.getSamplesFloat();
            targetTile.setSamples(samplesFloat);
            return;
        }

        PrepareSmileCorrection waterTiles = null;

        if (correctWater) {
            waterTiles = new SmileTiles(lambdaWaterLowerBand, lambdaSourceBand,
                                        lambdaWaterUpperBand, radianceWaterLowerBand, radianceWaterUpperBand,
                                        solarIrradianceWaterLowerBand, solarIrradianceWaterUpperBand,
                                        sourceSolarIrradianceBand, rectangle);
        }
        PrepareSmileCorrection landTiles = null;
        if (correctLand) {
            landTiles = new SmileTiles(lambdaLandLowerBand, lambdaSourceBand, lambdaLandUpperBand, radianceLandLowerBand,
                                       radianceLandUpperBand, solarIrradianceLandLowerBand, solarIrradianceLandUpperBand,
                                       solarIrradianceLandUpperBand, rectangle);
        }


        float refCentralWaveLength = smileAuxdata.getRefCentralWaveLengths()[targetBandIndex];
        float[] sourceRadiance = sourceRadianceTile.getSamplesFloat();
        if (correctWater) {
            float[] correctForSmileEffect = correctForSmileEffect(sourceRadianceTile, refCentralWaveLength, waterTiles,
                                                                  szaTile, targetBandIndex);
            targetTile.setSamples(correctForSmileEffect);
            return;
        }
        if (correctLand) {
            float[] correctedRadiance = correctForSmileEffect(sourceRadianceTile, refCentralWaveLength, landTiles,
                                                              szaTile, targetBandIndex);
            targetTile.setSamples(correctedRadiance);
            return;
        }
        targetTile.setSamples(sourceRadiance);
    }


    private float[] correctForSmileEffect(Tile sourceRadTile, float refCentralWaveLength, PrepareSmileCorrection smileTiles,
                                          Tile szaTile, int targetBandIndx) {

        float[] sourceTargetLambda = smileTiles.lambdaSourceBand();
        float[] solarIrradiance = smileTiles.solarIrradianceSourceBand();

        float[] lowerBandSolarIrrad = smileTiles.solarIrradianceLowerBand();
        float[] upperBandSolarIrrad = smileTiles.solarIrradianceUpperBand();

        float[] lowerBandRad = smileTiles.radianceLowerBand();
        float[] upperBandRad = smileTiles.radianceUpperBand();

        float[] lowerLambda = smileTiles.lambdaLowerBand();
        float[] upperLambda = smileTiles.lambdaUpperBand();

        float[] sza = getSampleFloats(szaTile);
        float[] radiance = getSampleFloats(sourceRadTile);
        float[] sourceRefl = convertRadToRefl(radiance, solarIrradiance, sza);
        float[] lowerRefl = convertRadToRefl(lowerBandRad, lowerBandSolarIrrad, sza);
        float[] upperRefl = convertRadToRefl(upperBandRad, upperBandSolarIrrad, sza);

        if (isRalyeighOperator) {
            int lowerWaterIndx = smileAuxdata.getWaterLowerBands()[targetBandIndx] - 1;
            int upperWaterIndx = smileAuxdata.getWaterUpperBands()[targetBandIndx] - 1;
            if (lowerWaterIndx != DO_NOT_CORRECT_BAND && upperWaterIndx != DO_NOT_CORRECT_BAND) {
                RayleighInput rayleighInputToCompute = new RayleighInput(sourceRefl, lowerRefl, upperRefl, targetBandIndx, lowerWaterIndx, upperWaterIndx);
                RayleighAux rayleighAux = prepareRayleighAux(sourceRadTile.getRectangle());
                RayleighOutput computedRayleighOutput = rayleighCorrAlgorithm.getRayleighReflectance(rayleighInputToCompute, rayleighAux, absorpOzone, getSourceProduct());
                sourceRefl = computedRayleighOutput.getSourceRayRefls();
                lowerRefl = computedRayleighOutput.getLowerRayRefls();
                upperRefl = computedRayleighOutput.getUpperRayRefls();
            }
        }


        float[] convertRefTo = new float[sourceRefl.length];
        for (int i = 0; i < sourceRefl.length; i++) {
            float correctedReflectance = SmileCorrectionAlgorithm.correctWithReflectance(sourceRefl[i], lowerRefl[i],
                                                                                         upperRefl[i], sourceTargetLambda[i], lowerLambda[i], upperLambda[i], refCentralWaveLength);

            float shiftedSolarIrradiance = shiftSolarIrradiance(solarIrradiance[i], sourceTargetLambda[i], refCentralWaveLength);
            convertRefTo[i] = convertReflToRad(correctedReflectance, sza[i], shiftedSolarIrradiance);
        }
        return convertRefTo;
    }


    private RayleighAux prepareRayleighAux(Rectangle rectangle) {
        RayleighAux rayleighAux = new RayleighAux();

        rayleighAux.setSunZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(sensor.getSZA()), rectangle));
        rayleighAux.setViewZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(sensor.getOZA()), rectangle));
        rayleighAux.setSunAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(sensor.getSAA()), rectangle));
        rayleighAux.setViewAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(sensor.getOAA()), rectangle));
        rayleighAux.setSeaLevels(getSourceTile(sourceProduct.getTiePointGrid(sensor.getSeaLevelPressure()), rectangle));
        rayleighAux.setTotalOzones(getSourceTile(sourceProduct.getTiePointGrid(sensor.getTotalOzone()), rectangle));
        if (Sensor.MERIS.equals(sensor)) {
            rayleighAux.setAltitudes(getSourceTile(sourceProduct.getTiePointGrid(sensor.getAltitude()), rectangle));
            rayleighAux.setLatitudes(getSourceTile(sourceProduct.getTiePointGrid(sensor.getLatitude()), rectangle));
            rayleighAux.setLongitude(getSourceTile(sourceProduct.getTiePointGrid(sensor.getLongitude()), rectangle));
        } else {
            rayleighAux.setAltitudes(getSourceTile(sourceProduct.getBand(sensor.getAltitude()), rectangle));
            rayleighAux.setLatitudes(getSourceTile(sourceProduct.getBand(sensor.getLatitude()), rectangle));
            rayleighAux.setLongitude(getSourceTile(sourceProduct.getBand(sensor.getLongitude()), rectangle));
        }
        return rayleighAux;
    }


    private float shiftSolarIrradiance(float solarIrradiance, float sourceTargetLambda, float refCentralWaveLength) {
//        poly =  2.329521314*10^(-10)* x^5 - 8.883158295*10^(-7)* x^4 + 1.341545977*10^(-3)*x^3 - 1.001512583* x^2 + 366.3249385* x - 50292.30277
//        dy/dx = 5 * 2.329521314*10^(-10)* x^4 - 4 * 8.883158295*10^(-7)* x^3 + 3 * 1.341545977*10^(-3)*x^2 - 2 * 1.001512583* x + 366.3249385
//        double forthDegree  = 5 * 2.329521314e-10 = 1.164760657E-9;
//        double thirdDegree  = 4 * 8.883158295e-7  = 3.553263318E-6;
//        double secondDegree = 3 * 1.341545977e-3 = 0.004024637931;
//        double firstDegree  = 2 * 1.001512583 = 2.003025166;
        double m = 1.164760657E-9 * Math.pow(sourceTargetLambda, 4) - 3.553263318E-6 * Math.pow(sourceTargetLambda, 3) + 0.004024637931 * Math.pow(sourceTargetLambda, 2) - 2.003025166 * Math.pow(sourceTargetLambda, 1) + 366.3249385;
        return (float) (solarIrradiance + m * (refCentralWaveLength - sourceTargetLambda));
    }


    private float[] convertRadToRefl(float[] radiance, float[] solarIrradiance, float[] sza) {
        float[] convertRadToRef = new float[radiance.length];
        for (int i = 0; i < radiance.length; i++) {
            convertRadToRef[i] = RsMathUtils.radianceToReflectance(radiance[i], sza[i], solarIrradiance[i]);
        }
        return convertRadToRef;
    }

    private float convertReflToRad(float refl, float sza, float solarIrradiance) {
        return RsMathUtils.reflectanceToRadiance(refl, sza, solarIrradiance);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SmileCorrectionOp.class);
        }
    }

    private class MerisSmile implements PrepareSmileCorrection {


        private final float[] samplesFloatRadianceLower;
        private final float[] samplesFloatRadianceUpper;
        private final float[] lambdaSourceBand;
        private final float[] lambdaLowerBand;
        private final float[] lambdaUpperBand;
        private final float[] solarIrradianceSource;
        private final float[] solarIrradianceLowerBand;
        private final float[] solarIrradianceUpperBand;


        public MerisSmile(SmileCorrectionAuxdata smileAuxdata, Rectangle rectangle, int targetBandIndex, SmileType smileType) {

            int lowerBandIndex = 0;
            int upperBandIndex = 0;

            if (SmileType.WATER.equals(smileType)) {
                lowerBandIndex = smileAuxdata.getWaterLowerBands()[targetBandIndex];
                upperBandIndex = smileAuxdata.getWaterUpperBands()[targetBandIndex];
            } else if (SmileType.LAND.equals(smileType)) {
                lowerBandIndex = smileAuxdata.getLandLowerBands()[targetBandIndex];
                upperBandIndex = smileAuxdata.getLandUpperBands()[targetBandIndex];
            }


            Tile sourceTile = getSourceTile(sourceProduct.getBand(DETECTOR_INDEX_BAND), rectangle);

            lambdaSourceBand = getLambdas(smileAuxdata.getDetectorWavelengths(), targetBandIndex, sourceTile);
            lambdaLowerBand = getLambdas(smileAuxdata.getDetectorWavelengths(), targetBandIndex, sourceTile);
            lambdaUpperBand = getLambdas(smileAuxdata.getDetectorWavelengths(), upperBandIndex, sourceTile);


            solarIrradianceSource = getLambdas(smileAuxdata.getDetectorSunSpectralFluxes(), targetBandIndex, sourceTile);
            solarIrradianceLowerBand = getLambdas(smileAuxdata.getDetectorSunSpectralFluxes(), lowerBandIndex, sourceTile);
            solarIrradianceUpperBand = getLambdas(smileAuxdata.getDetectorSunSpectralFluxes(), upperBandIndex, sourceTile);

            Band radianceWaterLowerBand = sourceProduct.getBand(String.format(sensor.getNamePattern(), lowerBandIndex));
            Band radianceWaterUpperBand = sourceProduct.getBand(String.format(sensor.getNamePattern(), upperBandIndex));

            samplesFloatRadianceLower = getSourceTile(radianceWaterLowerBand, rectangle).getSamplesFloat();
            samplesFloatRadianceUpper = getSourceTile(radianceWaterUpperBand, rectangle).getSamplesFloat();

        }

        private float[] getLambdas(double[][] detectorWavelengths, int index, Tile sourceTile) {
            int[] samplesFloat = sourceTile.getSamplesInt();
            float[] result = new float[samplesFloat.length];
            for (int i = 0; i < samplesFloat.length; i++) {
                int i1 = samplesFloat[i];
                if (i1 >= 0) {
                    result[i] = (float) detectorWavelengths[i1][index];
                }
            }
            return result;
        }

        @Override
        public float[] lambdaLowerBand() {
            return lambdaLowerBand;
        }

        @Override
        public float[] lambdaUpperBand() {
            return lambdaUpperBand;
        }

        @Override
        public float[] lambdaSourceBand() {
            return lambdaSourceBand;
        }

        @Override
        public float[] solarIrradianceLowerBand() {
            return solarIrradianceLowerBand;
        }

        @Override
        public float[] solarIrradianceSourceBand() {
            return solarIrradianceSource;
        }

        @Override
        public float[] solarIrradianceUpperBand() {
            return solarIrradianceUpperBand;
        }

        @Override
        public float[] radianceLowerBand() {
            return samplesFloatRadianceLower;
        }

        @Override
        public float[] radianceUpperBand() {
            return samplesFloatRadianceUpper;
        }
    }

    private class SmileTiles implements PrepareSmileCorrection {

        private final Tile sourceLambdaTile;
        private final Tile sourceSolarIrradianceTile;
        private Tile lowerLambdaTile;
        private Tile upperLambdaTile;
        private Tile lowerRadianceTile;
        private Tile upperRadianceTile;
        private Tile lowerSolarIrradianceTile;
        private Tile upperSolarIrradianceTile;

        SmileTiles(Band lowerLambdaBand, Band sourceLambdaBand, Band upperLambdaBand, Band lowerRadianceBand, Band upperRadianceBand, Band lowerSolarIrradianceBand,
                   Band upperSolarIrradianceBand, Band sourceSolarIrradianceBand,
                   Rectangle rectangle) {

            sourceLambdaTile = getSourceTile(sourceLambdaBand, rectangle);
            lowerLambdaTile = getSourceTile(lowerLambdaBand, rectangle);
            upperLambdaTile = getSourceTile(upperLambdaBand, rectangle);

            lowerRadianceTile = getSourceTile(lowerRadianceBand, rectangle);
            upperRadianceTile = getSourceTile(upperRadianceBand, rectangle);

            sourceSolarIrradianceTile = getSourceTile(sourceSolarIrradianceBand, rectangle);
            lowerSolarIrradianceTile = getSourceTile(lowerSolarIrradianceBand, rectangle);
            upperSolarIrradianceTile = getSourceTile(upperSolarIrradianceBand, rectangle);
        }

        @Override
        public float[] lambdaLowerBand() {
            return getSampleFloats(lowerLambdaTile);
        }

        @Override
        public float[] lambdaUpperBand() {
            return getSampleFloats(upperLambdaTile);
        }

        @Override
        public float[] lambdaSourceBand() {
            return getSampleFloats(sourceLambdaTile);
        }

        @Override
        public float[] solarIrradianceLowerBand() {
            return getSampleFloats(lowerSolarIrradianceTile);
        }

        @Override
        public float[] solarIrradianceSourceBand() {
            return getSampleFloats(sourceSolarIrradianceTile);
        }

        @Override
        public float[] solarIrradianceUpperBand() {
            return getSampleFloats(upperSolarIrradianceTile);
        }

        @Override
        public float[] radianceLowerBand() {
            return getSampleFloats(lowerRadianceTile);
        }

        @Override
        public float[] radianceUpperBand() {
            return getSampleFloats(upperRadianceTile);
        }
    }

    private enum SmileType {
        WATER, LAND
    }
}
