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

package org.esa.s3tbx.olci.radiometry.operator;

import org.esa.s3tbx.olci.radiometry.smilecorr.SmileCorrectionAlgorithm;
import org.esa.s3tbx.olci.radiometry.smilecorr.SmileCorrectionAuxdata;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
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
public class SmileCorrectionOp extends SampleOperator {


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


    @Override
    protected void prepareInputs() throws OperatorException {
        correctionAlgorithm = new SmileCorrectionAlgorithm(auxdata);
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("sensor", "OLCI");
        radReflProduct = GPF.createProduct("Rad2Refl", parameters, sourceProduct);
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) {
        String landExpression = null;
        if (sourceProduct.isCompatibleBandArithmeticExpression("quality_flags_land")) {
            landExpression = "quality_flags_land";
        }
        if (landExpression != null) {
            int countIndex = 0;
            for (; countIndex < SOURCE_RADIANCE_NAMES.length; countIndex++) {
                sampleConfigurer.defineSample(countIndex, SOURCE_RADIANCE_NAMES[countIndex]);
            }
            for (String SOURCE_SOLAR_FLUX_NAME : SOURCE_SOLAR_FLUX_NAMES) {
                sampleConfigurer.defineSample(countIndex++, SOURCE_SOLAR_FLUX_NAME);
            }
            sampleConfigurer.defineSample(countIndex, "SZA");
            sampleConfigurer.setValidPixelMask(landExpression);
        }

    }

    private double rad2Refl(float rad, float sza, float solarFlux) {
        return RsMathUtils.radianceToReflectance(rad, sza, solarFlux);
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) {
        for (final Band band : getTargetProduct().getBands()) {
            final int spectralBandIndex = band.getSpectralBandIndex();
            if (spectralBandIndex != -1) {
                sampleConfigurer.defineSample(spectralBandIndex, band.getName()); // name open
            }
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        Product targetProduct = productConfigurer.getTargetProduct();

        for (Band band : radReflProduct.getBands()) {
            final Band targetBand = targetProduct.addBand(band.getName(), band.getDataType());
            ProductUtils.copyRasterDataNodeProperties(band, targetBand);
        }

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());

    }

    @Override
    protected void computeSample(int x, int y, Sample[] sourceSamples, WritableSample targetSample) {
        final int targetSampleIndex = targetSample.getIndex();
        final int lowerBandIndex = getLowerBand(targetSampleIndex);
        final int upperBandIndex = getUpperBand(targetSampleIndex);

        if (lowerBandIndex != -1 && upperBandIndex != -1) {
            Sample radiance = sourceSamples[targetSampleIndex];
            Sample sza = sourceSamples[42];
            Sample solarFlux = sourceSamples[targetSampleIndex+21];
            double reflectance = rad2Refl(radiance.getFloat(), sza.getFloat(), solarFlux.getFloat());
            final Sample sourceSampleUpper = sourceSamples[upperBandIndex];
            final Sample sourceSampleLower = sourceSamples[lowerBandIndex];
            final double reflectanceCorrection = correctionAlgorithm.getFiniteDifference(sourceSampleUpper, sourceSampleLower, upperBandIndex, lowerBandIndex);
            targetSample.set(reflectance + reflectanceCorrection);
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
            super(SmileCorrectionOp.class);
        }
    }
}
