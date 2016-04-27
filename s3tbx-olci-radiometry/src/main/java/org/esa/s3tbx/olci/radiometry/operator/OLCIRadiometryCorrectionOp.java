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
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.ProductConfigurer;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SampleOperator;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.RsMathUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * @author muhammad.bc
 */
@OperatorMetadata(alias = "Olci.CorrectRadiometry",
        description = "Performs radiometric corrections on OLCI L1b data products.",
        authors = " Marco Peters ,Muhammad Bala (Brockmann Consult)",
        copyright = "(c) 2015 by Brockmann Consult",
        category = "Optical/Pre-Processing",
        version = "1.2")
public class OLCIRadiometryCorrectionOp extends Operator {


    public static final String LAND_EXPRESSION = "quality_flags_land";
    @Parameter(defaultValue = "false",
            label = "Perform radiance-to-reflectance conversion")
    private boolean doRadToRefl;

    @SourceProduct(alias = "source", label = "Name", description = "The source product.")
    private Product sourceProduct;

    private static final String[] SOURCE_RADIANCE_NAMES = new String[]{
            "Oa01_radiance", "Oa02_radiance", "Oa04_radiance", "Oa04_radiance", "Oa05_radiance", "Oa06_radiance", "Oa07_radiance",
            "Oa08_radiance", "Oa09_radiance", "Oa10_radiance", "Oa11_radiance", "Oa12_radiance", "Oa13_radiance", "Oa14_radiance",
            "Oa15_radiance", "Oa16_radiance", "Oa17_radiance", "Oa18_radiance", "Oa19_radiance", "Oa20_radiance", "Oa21_radiance",
    };
    private static final String[] SOURCE_SOLAR_FLUX_NAMES = new String[]{
            "solar_flux_band_1", "solar_flux_band_2", "solar_flux_band_3", "solar_flux_band_4", "solar_flux_band_5", "solar_flux_band_6", "solar_flux_band_7",
            "solar_flux_band_8", "solar_flux_band_9", "solar_flux_band_10", "solar_flux_band_11", "solar_flux_band_12", "solar_flux_band_13", "solar_flux_band_14",
            "solar_flux_band_15", "solar_flux_band_16", "solar_flux_band_17", "solar_flux_band_18", "solar_flux_band_19", "solar_flux_band_20", "solar_flux_band_21",
    };
    private static SmileCorrectionAuxdata auxdata = new SmileCorrectionAuxdata();
    private SmileCorrectionAlgorithm correctionAlgorithm;
    private Product radReflProduct;
    private Product targetProduct;
    private final List<Band> sourceBandList = new ArrayList<>();
    private final List<Band> sourceSolarFluxList = new ArrayList<>();

    @Override
    public void initialize() throws OperatorException {
        preparedInput();

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

    private void setSoureSolarFlux() {
        for (String sourceSolarFluxName : SOURCE_SOLAR_FLUX_NAMES) {
            sourceSolarFluxList.add(radReflProduct.getBand(sourceSolarFluxName));
        }
    }

    private void setSourceBands() {

        for (String sourceRadianceName : SOURCE_RADIANCE_NAMES) {
            sourceBandList.add(radReflProduct.getBand(sourceRadianceName));
        }
    }

    private void preparedInput() {
        correctionAlgorithm = new SmileCorrectionAlgorithm(auxdata);
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("sensor", "OLCI");
        radReflProduct = GPF.createProduct("Rad2Refl", parameters, sourceProduct);
        if (!radReflProduct.isCompatibleBandArithmeticExpression(LAND_EXPRESSION)) {
            throw new OperatorException("Expresssion '" + LAND_EXPRESSION + "'not compatible");
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle rectangle = targetTile.getRectangle();
        Band band = radReflProduct.getBand(targetBand.getName());
        Tile sourceTile = getSourceTile(band, rectangle);
        if (sourceTile != null) {
            for (int y = rectangle.y; y < sourceTile.getHeight(); y++) {
                for (int x = rectangle.x; x < sourceTile.getWidth(); x++) {
                    targetTile.setSample(x, y, sourceTile.getSampleDouble(x, y));
                }
            }
        }

    }

    private double rad2Refl(float rad, float sza, float solarFlux) {
        return RsMathUtils.radianceToReflectance(rad, sza, solarFlux);
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
            super(OLCIRadiometryCorrectionOp.class);
        }
    }
}
