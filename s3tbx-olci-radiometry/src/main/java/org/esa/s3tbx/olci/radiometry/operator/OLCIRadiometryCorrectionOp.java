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

import org.esa.s3tbx.olci.radiometry.smilecorr.SmileCorrectionAuxdata;
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
    private Product radToReflectenceProduct;
    private SmileCorrectionAuxdata smileCorrectionAuxdata;


    @Override
    protected void prepareInputs() throws OperatorException {
        smileCorrectionAuxdata = new SmileCorrectionAuxdata();
        radToReflectenceProduct = GPF.createProduct("Rad2Refl", null, sourceProduct);
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) {

    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) {

    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {

    }

    @Override
    protected void computeSample(int x, int y, Sample[] sourceSamples, WritableSample targetSample) {

        float sampleFloatBand_1 = radToReflectenceProduct.getBandAt(1).getSampleFloat(x, y);
        float sampleFloatBand_2 = radToReflectenceProduct.getBandAt(2).getSampleFloat(x, y);
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OLCIRadiometryCorrectionOp.class);
        }
    }
}
