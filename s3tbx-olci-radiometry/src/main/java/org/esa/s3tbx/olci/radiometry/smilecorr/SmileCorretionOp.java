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

    @SourceProduct(alias = "source", label = "Name", description = "The source product.")
    private Product sourceProduct;

    private SmileCorrectionAlgorithm correctionAlgorithm;
    private Product radReflProduct;
    private RayleighCorrAlgorithm algorithm;
    private double[] taur_std;
    private Mask waterMask;
    private HashMap<String, int[]> hashMapBandPosition;

    @Override
    public void initialize() throws OperatorException {
        if (!sourceProduct.isCompatibleBandArithmeticExpression(WATER_EXPRESSION)) {
            throw new OperatorException("Can not evaluate expression'" + WATER_EXPRESSION + "' on source product");
        }
        final Band[] sourceBands = sourceProduct.getBands();
        algorithm = new RayleighCorrAlgorithm();
        taur_std = getTaurStd(sourceBands);
        correctionAlgorithm = new SmileCorrectionAlgorithm(auxdata);
        hashMapBandPosition = new HashMap<>();


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
            hashMapBandPosition.put(targetBand.getName(), new int[]{getLandLowerBand(i - 1), getLandUpperBand(i - 1)});
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

    private void convertRadtoReflectance() {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("sensor", "OLCI");
        parameters.put("copyNonSpectralBands", "false");
        radReflProduct = GPF.createProduct("Rad2Refl", parameters, sourceProduct);


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
        final Rectangle rectangle = targetTile.getRectangle();

        final String targetBandName = targetBand.getName();
        final int[] landPositionIndex = hashMapBandPosition.get(targetBandName);

        final int landLowerBandIndex = landPositionIndex[0];
        final int landUpperBandIndex = landPositionIndex[1];


        Tile sza = getSourceTile(sourceProduct.getTiePointGrid(SZA), rectangle);
        Tile saa = getSourceTile(sourceProduct.getTiePointGrid(SAA), rectangle);
        Tile oza = getSourceTile(sourceProduct.getTiePointGrid(OZA), rectangle);
        Tile oaa = getSourceTile(sourceProduct.getTiePointGrid(OAA), rectangle);
        Tile altitudeTile = getSourceTile(sourceProduct.getBand(ALTITUDE), rectangle);
        Tile sea_level_pressure = getSourceTile(sourceProduct.getTiePointGrid(SEA_LEVEL_PRESSURE), rectangle);
        int targetBandIndex = sourceProduct.getBandIndex(targetBandName);

        final Tile waterTile = getSourceTile(waterMask, rectangle);

        for (int y = waterTile.getMinY(); y <= waterTile.getMaxY(); y++) {
            for (int x = waterTile.getMinX(); x <= waterTile.getMaxX(); x++) {
//                if (!sourceTargetBandTile.isSampleValid(x, y)) {
//                    targetTile.setSample(x, y, Double.NaN);
//                }
                if (waterTile.getSampleBoolean(x, y)) {
                    final double pressureAtSurface = algorithm.getPressureAtSurface(sea_level_pressure.getSampleDouble(x, y), altitudeTile.getSampleDouble(x, y));
                    final double taurPoZ = algorithm.getRayleighOpticalThickness(pressureAtSurface, taur_std[targetBand.getSpectralBandIndex()]);
                    final double reflRaly = algorithm.getRayleighReflectance(taurPoZ, sza.getSampleDouble(x, y), saa.getSampleDouble(x, y), oza.getSampleDouble(x, y), oaa.getSampleDouble(x, y));
                    targetTile.setSample(x, y, reflRaly);
                } else {
                    float radToReflSource = convertRadToRefl(rectangle, targetBandIndex, x, y, sza.getSampleFloat(x, y));
                    if (landLowerBandIndex != -1 && landUpperBandIndex != -1) {
                        float radToReflLower = convertRadToRefl(rectangle, landLowerBandIndex, x, y, sza.getSampleFloat(x, y));
                        float radToReflUpper = convertRadToRefl(rectangle, landUpperBandIndex, x, y, sza.getSampleFloat(x, y));
                        double refCorrection = radToReflSource + correctionAlgorithm.getFiniteDifference(radToReflUpper, radToReflLower, landUpperBandIndex, landLowerBandIndex);
                        targetTile.setSample(x, y, refCorrection);
                        continue;
                    }
                    targetTile.setSample(x, y, radToReflSource);
                }

            }
        }
    }

    private float convertRadToRefl(Rectangle rectangle, int bandIndex, int x, int y, float sza) {
        float sampleFloat = getSourceTile(sourceProduct.getBandAt(bandIndex), rectangle).getSampleFloat(x, y);
        float sampleFlux = getSourceTile(sourceProduct.getBand(OLCI_SOLAR_FLUX_BAND_NAMES[bandIndex]), rectangle).getSampleFloat(x, y);
        return RsMathUtils.radianceToReflectance(sampleFloat, sza, sampleFlux);
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

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SmileCorretionOp.class);
        }
    }
}
