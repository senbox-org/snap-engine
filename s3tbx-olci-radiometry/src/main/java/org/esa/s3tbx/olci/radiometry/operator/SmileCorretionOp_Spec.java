/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s3tbx.olci.radiometry.operator;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.olci.radiometry.smilecorr.SmileCorrectionAlgorithm;
import org.esa.s3tbx.olci.radiometry.smilecorr.SmileCorrectionAuxdata;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;
import java.util.HashMap;


/**
 * @author muhammad.bc
 */
@OperatorMetadata(alias = "Olci.CorrectRadiometry",
        description = "Performs radiometric corrections on OLCI L1b data products.",
        authors = " Marco Peters ,Muhammad Bala (Brockmann Consult)",
        copyright = "(c) 2015 by Brockmann Consult",
        category = "Optical/Pre-Processing",
        version = "1.2")
public class SmileCorretionOp_Spec extends Operator {


    public static final String LAND_EXPRESSION = "quality_flags_land";
    @Parameter(defaultValue = "false",
            label = "Perform radiance-to-reflectance conversion")
    private boolean doRadToRefl;

    @SourceProduct(alias = "source", label = "Name", description = "The source product.")
    private Product sourceProduct;

    private static SmileCorrectionAuxdata auxdata = new SmileCorrectionAuxdata();
    private SmileCorrectionAlgorithm correctionAlgorithm;
    private Product radReflProduct;
    private Product targetProduct;

    @Override
    public void initialize() throws OperatorException {
        convertRadtoReflectance();
        correctionAlgorithm = new SmileCorrectionAlgorithm(auxdata);
        // Configure the target
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
        for (Band band : radReflProduct.getBands()) {
            final Band targetBand = targetProduct.addBand(band.getName(), band.getDataType());
            ProductUtils.copyRasterDataNodeProperties(band, targetBand);
        }

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());
        setTargetProduct(targetProduct);


    }

    private void convertRadtoReflectance() {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("sensor", "OLCI");
        radReflProduct = GPF.createProduct("Rad2Refl", parameters, sourceProduct);
        if (!radReflProduct.isCompatibleBandArithmeticExpression(LAND_EXPRESSION)) {
            throw new OperatorException("Expresssion '" + LAND_EXPRESSION + "'not compatible");
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final int targetBandIndex = radReflProduct.getBandIndex(targetBand.getName());
        final int lowerBandIndex = getLowerBand(targetBandIndex);
        final int upperBandIndex = getUpperBand(targetBandIndex);

        final Rectangle rectangle = targetTile.getRectangle();
        final Tile sourceTargetBandTile = getSourceTile(radReflProduct.getBand(targetBand.getName()), rectangle);
        final Tile sourceLowerBandTile = getSourceTile(radReflProduct.getBandAt(lowerBandIndex), rectangle);
        final Tile sourceUpperBandTile = getSourceTile(radReflProduct.getBandAt(upperBandIndex), rectangle);

        for (int y = targetTile.getMinY(); y < targetTile.getMaxY(); y++) {
            for (int x = targetTile.getMinX(); x < targetTile.getMaxX(); x++) {
                if (lowerBandIndex != -1 && upperBandIndex != -1) {
                    final float sampleFloatUpperBand = sourceUpperBandTile.getSampleFloat(x, y);
                    final float sampleFloatLowerBand = sourceLowerBandTile.getSampleFloat(x, y);

                    final double reflectanceCorrection = correctionAlgorithm.getReflectanceCorrection(sampleFloatUpperBand, sampleFloatLowerBand, upperBandIndex, lowerBandIndex);
                    targetTile.setSample(x, y, reflectanceCorrection);
                }
            }
        }
    }

    private int getLowerBand(int index) {
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
            super(SmileCorretionOp_Spec.class);
        }
    }
}
