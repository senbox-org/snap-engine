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
import org.esa.s3tbx.olci.radiometry.rayleighcorrection.RayleighCorrAlgorithm;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.RsMathUtils;

import java.awt.*;
import java.util.HashMap;


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
    public static final String SAA = "SAA";
    public static final String OZA = "OZA";
    public static final String OAA = "OAA";
    public static final String ALTITUDE = "altitude";
    public static final String SEA_LEVEL_PRESSURE = "sea_level_pressure";
    private static SmileCorrectionAuxdata auxdata = new SmileCorrectionAuxdata();
    public final static String[] OLCI_SOLAR_FLUX_BAND_NAMES = new String[]{
            "solar_flux_band_1", "solar_flux_band_2", "solar_flux_band_3", "solar_flux_band_4", "solar_flux_band_5",
            "solar_flux_band_6", "solar_flux_band_7", "solar_flux_band_8", "solar_flux_band_9", "solar_flux_band_10",
            "solar_flux_band_11", "solar_flux_band_12", "solar_flux_band_13", "solar_flux_band_14", "solar_flux_band_15",
            "solar_flux_band_16", "solar_flux_band_17", "solar_flux_band_18", "solar_flux_band_19", "solar_flux_band_20",
            "solar_flux_band_21"
    };

    private SmileCorrectionAlgorithm correctionAlgorithm;

    private Product radReflProduct;
    @SourceProduct(alias = "source", label = "Name", description = "The source product.")
    private Product sourceProduct;
    private double[] taur_std;
    private RayleighCorrAlgorithm algorithm;
    private Mask waterMask;
    private HashMap<String, int[]> bandPositionLand;
    private HashMap<String, int[]> bandPositionWater;
    private HashMap<String, String> lambdaSearch;
    private HashMap<String, String> fluxSearch;

    @Override
    public void initialize() throws OperatorException {
        if (!sourceProduct.isCompatibleBandArithmeticExpression(WATER_EXPRESSION)) {
            throw new OperatorException("Can not evaluate expression'" + WATER_EXPRESSION + "' on source product");
        }
        final Band[] sourceBands = sourceProduct.getBands();
        algorithm = new RayleighCorrAlgorithm();
        taur_std = getTaurStd(sourceBands);
        correctionAlgorithm = new SmileCorrectionAlgorithm(auxdata);
        bandPositionLand = new HashMap<>();
        bandPositionWater = new HashMap<>();
        lambdaSearch = new HashMap<>();
        fluxSearch = new HashMap<>();


        // Configure the target
        Product targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
        for (int i = 1; i <= 21; i++) {
            Band targetBand = targetProduct.addBand(String.format("Oa%02d_radiance", i), ProductData.TYPE_FLOAT32);
            Band sourceBand = sourceProduct.getBand(String.format("Oa%02d_radiance", i));
            targetBand.setSpectralWavelength(sourceBand.getSpectralWavelength());
            targetBand.setSpectralBandwidth(sourceBand.getSpectralBandwidth());
            targetBand.setSpectralBandIndex(sourceBand.getSpectralBandIndex());
            targetBand.setNoDataValueUsed(true);
            targetBand.setNoDataValue(Double.NaN);
            bandPositionLand.put(targetBand.getName(), new int[]{getLandLowerBand(i - 1), getLandUpperBand(i - 1)});
            bandPositionWater.put(targetBand.getName(), new int[]{getWaterLowerBand(i - 1), getWaterUpperBand(i - 1)});
            lambdaSearch.put(targetBand.getName(), String.format("lambda0_band_%d", i));
            fluxSearch.put(targetBand.getName(), String.format("solar_flux_band_%d", i));
        }

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
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

    private double[] getTaurStd(Band[] sourceBands) {
        final double[] waveLenght = new double[sourceBands.length];
        for (int i = 0; i < sourceBands.length; i++) {
            waveLenght[i] = sourceBands[i].getSpectralWavelength();
        }
        return algorithm.getTaurStd(waveLenght);
    }


    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle rectangle = targetTile.getRectangle();
        String targetBandName = targetBand.getName();

        Tile szaTile = getSourceTile(sourceProduct.getTiePointGrid(SZA), rectangle);
        int targetBandIndex = Integer.parseInt(targetBandName.substring(2, 4)) - 1;
        Tile sourceTile = getSourceTile(sourceProduct.getBand(targetBandName), rectangle);

        int[] landPositionIndex = bandPositionLand.get(targetBandName);
        int[] waterPositionIndex = bandPositionWater.get(targetBandName);

        final int landLowerBandIndex = landPositionIndex[0];
        final int landUpperBandIndex = landPositionIndex[1];

        final int waterLowerBandIndex = waterPositionIndex[0];
        final int waterUpperBandIndex = waterPositionIndex[1];

        boolean correctLand = true;
        boolean correctWater = true;
        Tile lamdaLandLowerTile = null;
        Tile lamdaLandUpperTile = null;

        Tile lamdaWaterLowerTile = null;
        Tile lamdaWaterUpperTile = null;


        if (landLowerBandIndex == -1 && landUpperBandIndex == -1 && waterLowerBandIndex == -1 && waterUpperBandIndex == -1) {
            float[] samplesFloat = getSourceTile(sourceProduct.getBand(targetBandName), rectangle).getSamplesFloat();
            targetTile.setSamples(samplesFloat);
            return;
        }

        if (landLowerBandIndex == -1 && landUpperBandIndex == -1) {
            correctLand = false;
        }

        if (waterLowerBandIndex == -1 && waterUpperBandIndex == -1) {
            correctWater = false;
        }


        if (correctWater) {
            lamdaWaterLowerTile = getLambda(rectangle, waterLowerBandIndex);
            lamdaWaterUpperTile = getLambda(rectangle, waterUpperBandIndex);
        }

        if (correctLand) {
            lamdaLandLowerTile = getLambda(rectangle, landLowerBandIndex);
            lamdaLandUpperTile = getLambda(rectangle, landUpperBandIndex);
        }

        Tile lamdaTarget = getLambda(rectangle, targetBandIndex);

        final Tile waterTile = getSourceTile(waterMask, rectangle);
        float refLowerBand = 0;
        float refUpperBand = 0;
        float lambdaLowerBand = 0;
        float lambdaUpperBand = 0;

        for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
            for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
                float sza = szaTile.getSampleFloat(x, y);
                float radSample = sourceTile.getSampleFloat(x, y);
                if (x == 600 && y == 10) {
                    System.out.println("radSample = " + radSample);
                }
                float sourceRef = convertRadToRefl(rectangle, targetBandIndex, x, y, sza, radSample);
                float lambdaActualBand = lamdaTarget.getSampleFloat(x, y);


                if (waterTile.getSampleBoolean(x, y) && correctWater) {
                    refLowerBand = convertRadToRefl(rectangle, waterLowerBandIndex, x, y, sza, radSample);
                    refUpperBand = convertRadToRefl(rectangle, waterUpperBandIndex, x, y, sza, radSample);
                    lambdaLowerBand = lamdaWaterLowerTile.getSampleFloat(x, y);
                    lambdaUpperBand = lamdaWaterUpperTile.getSampleFloat(x, y);
                } else if (correctLand) {
                    refLowerBand = convertRadToRefl(rectangle, landLowerBandIndex, x, y, sza, radSample);
                    refUpperBand = convertRadToRefl(rectangle, landUpperBandIndex, x, y, sza, radSample);
                    lambdaLowerBand = lamdaLandLowerTile.getSampleFloat(x, y);
                    lambdaUpperBand = lamdaLandUpperTile.getSampleFloat(x, y);

                }
                float correct = correctionAlgorithm.correct(sourceRef, refUpperBand, refLowerBand, lambdaLowerBand, lambdaUpperBand, lambdaActualBand, targetBandIndex);
                float corrToRad = convertReflToRad(rectangle, correct, sza, targetBandIndex, x, y);
                targetTile.setSample(x, y, corrToRad);
            }
        }
    }

    private Tile getLambda(Rectangle rectangle, int landLowerBandIndex) {
        Band band = sourceProduct.getBand(String.format("lambda0_band_%d", landLowerBandIndex + 1));
        if (band == null) {
            String searchedBand = lambdaSearch.get(sourceProduct.getBandAt(landLowerBandIndex).getName());
            band = sourceProduct.getBand(searchedBand);
        }
        return getSourceTile(band, rectangle);
    }

    private float convertRadToRefl(Rectangle rectangle, int bandIndex, int x, int y, float sza, float radSample) {
        Band band = null;
        if (bandIndex < OLCI_SOLAR_FLUX_BAND_NAMES.length) {
            band = sourceProduct.getBand(OLCI_SOLAR_FLUX_BAND_NAMES[bandIndex]);
        } else {
            String sbandFluxName = fluxSearch.get(sourceProduct.getBandAt(bandIndex).getName());
            band = sourceProduct.getBand(sbandFluxName);
        }
        float sampleFlux = getSourceTile(band, rectangle).getSampleFloat(x, y);
        return RsMathUtils.radianceToReflectance(radSample, sza, sampleFlux);
    }

    private float convertReflToRad(Rectangle rectangle, Float refl, float sza, int bandIndex, int x, int y) {
//        Band band = sourceProduct.getBand(OLCI_SOLAR_FLUX_BAND_NAMES[bandIndex]);
//        if (band == null) {
//            String sbandFluxName = fluxSearch.get(sourceProduct.getBandAt(bandIndex).getName());
//            band = sourceProduct.getBand(sbandFluxName);
//        }
//        Band band = null;
//        if (bandIndex < OLCI_SOLAR_FLUX_BAND_NAMES.length) {
//            band = sourceProduct.getBand(OLCI_SOLAR_FLUX_BAND_NAMES[bandIndex]);
//        } else {
//            String sbandFluxName = fluxSearch.get(sourceProduct.getBandAt(bandIndex).getName());
//            band = sourceProduct.getBand(sbandFluxName);
//        }
//        float sampleFlux = getSourceTile(band, rectangle).getSampleFloat(x, y);
        float sampleFlux = (float) auxdata.getSolarIrradiances()[bandIndex];
        return RsMathUtils.reflectanceToRadiance(refl, sza, sampleFlux);
    }

    private int getLandLowerBand(int index) {
        if (index > auxdata.getBands().length) {
            throw new OperatorException("The band does not exist");
        }
        boolean mustCorrect = auxdata.getLandRefCorrectionSwitchs()[index];
        int lowerBandIndex = -1;
        if (mustCorrect) {
            lowerBandIndex = (int) auxdata.getLandLowerBands()[index] - 1;
        }
        return lowerBandIndex;
    }

    protected int getLandUpperBand(int index) {
        boolean toCorrectBand = auxdata.getLandRefCorrectionSwitchs()[index];
        int upperBandIndex = -1;
        if (toCorrectBand) {
            upperBandIndex = (int) auxdata.getLandUpperBands()[index] - 1;
        }
        return upperBandIndex;
    }

    private int getWaterLowerBand(int i) {
        if (i > auxdata.getBands().length) {
            throw new OperatorException("The band does not exist");
        }
        boolean mustCorrect = auxdata.getWaterRefCorrectionSwitchs()[i];
        int lowerBandIndex = -1;
        if (mustCorrect) {
            lowerBandIndex = (int) auxdata.getWater_LowerBands()[i] - 1;
        }
        return lowerBandIndex;
    }

    private int getWaterUpperBand(int index) {
        boolean toCorrectBand = auxdata.getWaterRefCorrectionSwitchs()[index];
        int upperBandIndex = -1;
        if (toCorrectBand) {
            upperBandIndex = (int) auxdata.getWaterUpperBands()[index] - 1;
        }
        return upperBandIndex;
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SmileCorretionOp.class);
        }
    }
}
