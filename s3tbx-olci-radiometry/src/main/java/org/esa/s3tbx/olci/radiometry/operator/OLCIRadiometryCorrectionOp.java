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

import org.esa.s3tbx.olci.radiometry.smilecorr.SmileCorrectionAlgorithm;
import org.esa.s3tbx.olci.radiometry.smilecorr.SmileCorrectionAuxdata;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
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

import java.util.HashMap;


/**
 * @author muhammad.bc
 */
@OperatorMetadata(alias = "Olci.CorrectRadiometry",
        description = "Performs radiometric corrections on OLCI L1b data products.",
        authors = " Marco Peters ,Muhammad(Brockmann Consult)",
        copyright = "(c) 2015 by Brockmann Consult",
        category = "Optical/Pre-Processing",
        version = "1.2")
public class OLCIRadiometryCorrectionOp extends SampleOperator {


    @Parameter(defaultValue = "false",
            label = "Perform radiance-to-reflectance conversion",
            description = "Whether to perform radiance-to-reflectance conversion. " +
                    "When selecting ENVISAT as target format, the radiance to reflectance conversion can not be performed.")
    private boolean doRadToRefl;

    @SourceProduct(alias = "source", label = "Name", description = "The source product.")
    private Product sourceProduct;

    private SmileCorrectionAuxdata smileCorrectionAuxdata;
    private static String[] expressions = {"WQSF_lsb.WATER and WQSF_lsb.LAND", "LQSF_lsb.WATER  and LQSF_lsb.LAND"};
    private static SmileCorrectionAuxdata auxdata = new SmileCorrectionAuxdata();
    private SmileCorrectionAlgorithm correctionAlgorithm;


    @Override
    protected void prepareInputs() throws OperatorException {
        smileCorrectionAuxdata = new SmileCorrectionAuxdata();
        correctionAlgorithm = new SmileCorrectionAlgorithm(auxdata);
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("sensor", "OLCI");
        sourceProduct = GPF.createProduct("Rad2Refl", parameters, sourceProduct);


        setSourceProduct(sourceProduct);
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) {
        String landExpression = null;
        if (sourceProduct.isCompatibleBandArithmeticExpression("WQSF_lsb.LAND") || sourceProduct.isCompatibleBandArithmeticExpression("WQSF_lsb.LAND")) {
            landExpression = "WQSF_lsb.LAND";
        } else if (sourceProduct.isCompatibleBandArithmeticExpression("LQSF_lsb.LAND")) {
            landExpression = "LQSF_lsb.LAND";
        }
        if (landExpression != null) {
            sampleConfigurer.defineComputedSample(30, ProductData.TYPE_INT8, landExpression);

        }


        Band[] bands = sourceProduct.getBands();
        for (int i = 0; i < bands.length; i++) {
            sampleConfigurer.defineSample(i, bands[i].getName());
        }
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) {
        for (final Band band : getTargetProduct().getBands()) { // pitfall: using targetProduct field here throws NPE
            final int spectralBandIndex = band.getSpectralBandIndex();
            if (spectralBandIndex != -1) {
                sampleConfigurer.defineSample(spectralBandIndex, band.getName());
            }
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        Product targetProduct = productConfigurer.getTargetProduct();
        for (Band band : sourceProduct.getBands()) {
            final Band targetBand = targetProduct.addBand(band.getName(), band.getDataType());
            targetBand.setUnit(band.getUnit());
            ProductUtils.copySpectralBandProperties(band, targetBand);
        }

        for (final Band sourceBand : getSourceProduct().getBands()) {
            if (sourceBand.getSpectralBandIndex() == -1 && !targetProduct.containsBand(sourceBand.getName())) {
                productConfigurer.copyBands(sourceBand.getName());
            }
        }

//        ProductUtils.copyMetadata(sourceProduct, targetProduct);
//        ProductUtils.copyVectorData(sourceProduct, targetProduct);
//        ProductUtils.copyMasks(sourceProduct, targetProduct);
//        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());

    }

    @Override
    protected void computeSample(int x, int y, Sample[] sourceSamples, WritableSample targetSample) {
        double correctValue = 0;
        boolean aBoolean = sourceSamples[1].getBoolean();
        if (aBoolean) {
            int index = targetSample.getIndex();
            correctValue = correctionAlgorithm.getReflectanceCorrection(index);
        }
        targetSample.set(correctValue);
    }


    public static class Spi extends OperatorSpi {
        public Spi() {
            super(OLCIRadiometryCorrectionOp.class);
        }
    }
}
