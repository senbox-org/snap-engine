/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.s3tbx.processor.rad2refl;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.*;
import org.esa.snap.core.util.ProductUtils;

import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME;

/**
 * An operator to provide conversion from radiances to reflectances or backwards.
 * Currently supports MERIS, OLCI and SLSTR 500m L1 products.
 *
 * @author Olaf Danne
 * @author Marco Peters
 */
@OperatorMetadata(alias = "Rad2Refl",
        authors = "Olaf Danne, Marco Peters",
        copyright = "Brockmann Consult GmbH",
        category = "Optical/Pre-Processing",
        version = "2.0",
        description = "Provides conversion from radiances to reflectances or backwards.")
public class Rad2ReflOp extends SampleOperator {

    @Parameter(defaultValue = "OLCI",
            description = "The sensor", valueSet = {"MERIS", "OLCI", "SLSTR_500m"})
    private Sensor sensor;

    @Parameter(description = "Conversion mode: from rad to refl, or backwards", valueSet = {"RAD_TO_REFL", "REFL_TO_RAD"},
            defaultValue = "RAD_TO_REFL")
    private String conversionMode;

    @SourceProduct(alias = "source", label = "Name", description = "The source product.")
    private Product sourceProduct;

    @Parameter(defaultValue = "false",
            description = "If set, non-spectral bands from source product are written to target product")
    private boolean copyNonSpectralBands;

    private RadReflConverter converter;
    private transient int currentPixel = 0;

    Band[] spectralInputBands;
    String[] spectralInputBandNames;
    String[] spectralOutputBandNames;
    private String spectralInputBandPrefix;
    private String autogroupingString;


    @Override
    protected void computeSample(int x, int y, Sample[] sourceSamples, WritableSample targetSample) {
        checkCancellation();
        final int bandIndex = targetSample.getIndex();
        final float convertedValue = converter.convert(sourceProduct, sourceSamples, bandIndex);
        final float result = convertedValue > 0.0f ? convertedValue :
                (float) spectralInputBands[bandIndex].getNoDataValue();
        targetSample.set(result);
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sc) throws OperatorException {
        int index = 0;
        for (int i = 0; i < sensor.getNumSpectralBands(); i++) {
            spectralInputBands[i] = sourceProduct.getBand(spectralInputBandNames[i]);
            if (spectralInputBands[i] != null) {
                sc.defineSample(index++, spectralInputBandNames[i]);
            }
        }

        sc.defineSample(index++, sensor.getSzaBandNames()[0]);
        if (sensor == Sensor.SLSTR_500m) {
            sc.defineSample(index++, sensor.getSzaBandNames()[1]);  // nadir and oblique
        }

        if (sensor.getSolarFluxBandNames() != null) {
            // all besides MERIS
            for (int i = 0; i < sensor.getSolarFluxBandNames().length; i++) {
                sc.defineSample(index++, sensor.getSolarFluxBandNames()[i]);
            }
        }
        if (sensor == Sensor.MERIS || sensor == Sensor.OLCI) {
            // only OLCI, MERIS
            sc.defineSample(index, MERIS_DETECTOR_INDEX_DS_NAME);
        }
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sc) throws OperatorException {
        for (int i = 0; i < sensor.getNumSpectralBands(); i++) {
            sc.defineSample(i, spectralOutputBandNames[i]);
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        final Product targetProduct = productConfigurer.getTargetProduct();

        for (int i = 0; i < sensor.getNumSpectralBands(); i++) {
            spectralInputBands[i] = sourceProduct.getBand(spectralInputBandNames[i]);
            if (spectralInputBands[i] != null) {
                final Band bandToConvert = targetProduct.addBand(spectralOutputBandNames[i], ProductData.TYPE_FLOAT32);
                String descr = spectralInputBands[i].getDescription();
                String descrToConvert = descr;
                if (conversionMode.equals("RAD_TO_REFL") && descr != null && descr.contains("adiance")) {
                    descrToConvert = descr.replace("adiance", "eflectance");
                } else if (conversionMode.equals("REFL_TO_RAD") && descr != null && descr.contains("eflectance")) {
                    descrToConvert = descr.replace("eflectance", "adiance");
                }
                bandToConvert.setDescription(descrToConvert);
                final String unit = conversionMode.equals("RAD_TO_REFL") ? Rad2ReflConstants.REFL_UNIT :
                        Rad2ReflConstants.RAD_UNIT;
                bandToConvert.setUnit(unit);
                bandToConvert.setValidPixelExpression(spectralInputBands[i].getValidPixelExpression());
                bandToConvert.setNoDataValue(spectralInputBands[i].getNoDataValue());
                bandToConvert.setNoDataValueUsed(spectralInputBands[i].isNoDataValueUsed());
                bandToConvert.setSpectralBandIndex(spectralInputBands[i].getSpectralBandIndex());
                bandToConvert.setSpectralWavelength(spectralInputBands[i].getSpectralWavelength());
                bandToConvert.setSpectralBandwidth(spectralInputBands[i].getSpectralBandwidth());
            }
        }

        // copy non-spectral bands
        if (copyNonSpectralBands) {
            for (Band b : sourceProduct.getBands()) {
                if (!b.getName().contains(spectralInputBandPrefix)) {
                    ProductUtils.copyBand(b.getName(), sourceProduct, targetProduct, true);
                    targetProduct.getBand(b.getName()).setGeoCoding(b.getGeoCoding());
                }
            }
            targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());

        }
        addPatternToAutoGrouping(targetProduct, autogroupingString);

        targetProduct.setProductType(sourceProduct.getProductType());
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);


    }

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        spectralInputBands = new Band[sensor.getNumSpectralBands()];
        spectralInputBandPrefix = conversionMode.equals("RAD_TO_REFL") ? "radiance" : "reflectance";
        spectralInputBandNames = conversionMode.equals("RAD_TO_REFL") ? sensor.getRadBandNames() : sensor.getReflBandNames();
        spectralOutputBandNames = conversionMode.equals("RAD_TO_REFL") ? sensor.getReflBandNames() : sensor.getRadBandNames();

        if (sensor == Sensor.MERIS) {
            converter = new MerisRadReflConverter(sourceProduct, conversionMode);
            autogroupingString = ((MerisRadReflConverter) converter).getSpectralBandAutogroupingString();
        } else if (sensor == Sensor.OLCI) {
            converter = new OlciRadReflConverter(conversionMode);
            autogroupingString = ((OlciRadReflConverter) converter).getSpectralBandAutogroupingString();
        } else if (sensor == Sensor.SLSTR_500m) {
            converter = new SlstrRadReflConverter(conversionMode);
            autogroupingString = ((SlstrRadReflConverter) converter).getSpectralBandAutogroupingString();
        } else {
            throw new OperatorException("Sensor '" + sensor.getName() + "' not supported.");
        }

    }

    private void addPatternToAutoGrouping(Product product, String groupPattern) {
        Product.AutoGrouping autoGrouping = product.getAutoGrouping();
        String stringPattern = autoGrouping != null ? autoGrouping.toString() + ":" + groupPattern : groupPattern;
        product.setAutoGrouping(stringPattern);
    }

    private void checkCancellation() {
        if (currentPixel % 1000 == 0) {
            checkForCancellation();
            currentPixel = 0;
        }
        currentPixel++;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(Rad2ReflOp.class);
        }
    }
}
