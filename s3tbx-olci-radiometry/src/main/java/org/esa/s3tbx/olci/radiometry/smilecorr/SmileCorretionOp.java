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
    private static SmileCorrectionAuxdata auxdata = new SmileCorrectionAuxdata();

    @SourceProduct(alias = "source", label = "Name", description = "The source product.")
    private Product sourceProduct;

    private SmileCorrectionAlgorithm correctionAlgorithm;
    private Product radReflProduct;
    private RayleighCorrAlgorithm algorithm;
    private double[] taur_std;
    private Mask waterMask;

    @Override
    public void initialize() throws OperatorException {
        convertRadtoReflectance();
        if (!sourceProduct.isCompatibleBandArithmeticExpression(WATER_EXPRESSION)) {
            throw new OperatorException("Can not evaluate expression'" + WATER_EXPRESSION + "' on source product");
        }
        final Band[] sourceBands = sourceProduct.getBands();
        algorithm = new RayleighCorrAlgorithm();
        taur_std = getTaurStd(sourceBands);
        correctionAlgorithm = new SmileCorrectionAlgorithm(auxdata);


        // Configure the target
        Product targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
        for (int i = 1; i <= 21; i++) {
            Band targetBand = targetProduct.addBand(String.format("Oa%02d_reflectance", i), ProductData.TYPE_FLOAT32);
            Band sourceBand = radReflProduct.getBand(String.format("Oa%02d_reflectance", i));
            targetBand.setSpectralWavelength(sourceBand.getSpectralWavelength());
            targetBand.setSpectralBandwidth(sourceBand.getSpectralBandwidth());
            targetBand.setSpectralBandIndex(sourceBand.getSpectralBandIndex());
            targetBand.setNoDataValueUsed(true);
            targetBand.setNoDataValue(Double.NaN);
//            map.put(targetBand.getName(), new int[]{getLowerBand(i-1), getUpperBand(i-1)})
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

        final int targetBandIndex = radReflProduct.getBandIndex(targetBand.getName());
        final int lowerBandIndex = getLowerBand(targetBandIndex);
        final int upperBandIndex = getUpperBand(targetBandIndex);

        Tile sourceLowerBandTile = null;
        Tile sourceUpperBandTile = null;

        if (lowerBandIndex != -1 && upperBandIndex != -1) {
            sourceLowerBandTile = getSourceTile(radReflProduct.getBandAt(lowerBandIndex), rectangle);
            sourceUpperBandTile = getSourceTile(radReflProduct.getBandAt(upperBandIndex), rectangle);
        }

        final Tile sourceTargetBandTile = getSourceTile(radReflProduct.getBand(targetBand.getName()), rectangle);
        final Tile sza = getSourceTile(sourceProduct.getTiePointGrid("SZA"), rectangle);
        final Tile saa = getSourceTile(sourceProduct.getTiePointGrid("SAA"), rectangle);
        final Tile oza = getSourceTile(sourceProduct.getTiePointGrid("OZA"), rectangle);
        final Tile oaa = getSourceTile(sourceProduct.getTiePointGrid("OAA"), rectangle);
        final Tile altitudeTile = getSourceTile(sourceProduct.getBand("altitude"), rectangle);
        final Tile sea_level_pressure = getSourceTile(sourceProduct.getTiePointGrid("sea_level_pressure"), rectangle);

        final Tile waterTile = getSourceTile(waterMask, rectangle);

        for (int y = waterTile.getMinY(); y <= waterTile.getMaxY(); y++) {
            for (int x = waterTile.getMinX(); x <= waterTile.getMaxX(); x++) {
                if (!sourceTargetBandTile.isSampleValid(x, y)) {
                    targetTile.setSample(x, y, Double.NaN);
                }
                if (waterTile.getSampleBoolean(x, y)) {
                    final double pressureAtSurface = algorithm.getPressureAtSurface(sea_level_pressure.getSampleDouble(x, y), altitudeTile.getSampleDouble(x, y));
                    final double taurPoZ = algorithm.getRayleighOpticalThickness(pressureAtSurface, taur_std[targetBand.getSpectralBandIndex()]);
                    final double reflRaly = algorithm.getRayleighReflectance(taurPoZ, sza.getSampleDouble(x, y), saa.getSampleDouble(x, y), oza.getSampleDouble(x, y), oaa.getSampleDouble(x, y));
                    targetTile.setSample(x, y, reflRaly);
                } else {
                    if (sourceUpperBandTile != null || sourceLowerBandTile != null) {
                        final float sampleFloatUpperBand = sourceUpperBandTile.getSampleFloat(x, y);
                        final float sampleFloatLowerBand = sourceLowerBandTile.getSampleFloat(x, y);
                        final double refCorrection = sourceTargetBandTile.getSampleFloat(x, y) + correctionAlgorithm.getFiniteDifference(sampleFloatUpperBand, sampleFloatLowerBand, upperBandIndex, lowerBandIndex);
                        targetTile.setSample(x, y, refCorrection);
                        continue;
                    }
                    targetTile.setSample(x, y, sourceTargetBandTile.getSampleDouble(x, y));
                }

            }
        }
    }

    private int getLowerBand(int index) {
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

    protected int getUpperBand(int index) {
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
