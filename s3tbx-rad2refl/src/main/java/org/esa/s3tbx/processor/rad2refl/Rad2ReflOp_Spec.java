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
package org.esa.s3tbx.processor.rad2refl;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;

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
public class Rad2ReflOp_Spec extends Operator {

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

    String[] spectralInputBandNames;
    String[] spectralOutputBandNames;
    private String spectralInputBandPrefix;
    private Product targetProduct;


    @Override
    public void initialize() throws OperatorException {
        spectralInputBandPrefix = isRadToReflMode() ? "radiance" : "reflectance";
        spectralInputBandNames = isRadToReflMode() ? sensor.getRadBandNames() : sensor.getReflBandNames();
        spectralOutputBandNames = isRadToReflMode() ? sensor.getReflBandNames() : sensor.getRadBandNames();


        Product targetProduct = createTargetProduct();
        setTargetProduct(targetProduct);

        if (sensor == Sensor.MERIS) {
            converter = new MerisRadReflConverter(sourceProduct, conversionMode);
        } else if (sensor == Sensor.OLCI) {
            converter = new OlciRadReflConverter(conversionMode);
        } else if (sensor == Sensor.SLSTR_500m) {
            converter = new SlstrRadReflConverter(conversionMode);
        } else {
            throw new OperatorException("Sensor '" + sensor.getName() + "' not supported.");
        }
    }

    private boolean isRadToReflMode() {
        return conversionMode.equals("RAD_TO_REFL");
    }

    private Product createTargetProduct() {
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        for (int i = 0; i < sensor.getNumSpectralBands(); i++) {
            Band band = ProductUtils.copyBand(sensor.getRadBandNames()[i], sourceProduct, sensor.getReflBandNames()[i], targetProduct, false);
            if (band != null) {
                band.setNoDataValue(Float.NaN);
                band.setNoDataValueUsed(true);
            }

        }

        if (sensor != Sensor.MERIS) {
            for (int i = 0; i < sensor.getSolarFluxBandNames().length; i++) {
                ProductUtils.copyBand(sensor.getSolarFluxBandNames()[i], sourceProduct, targetProduct, true);
            }
        }

        if (sensor == Sensor.MERIS || sensor == Sensor.OLCI) {
            ProductUtils.copyBand(MERIS_DETECTOR_INDEX_DS_NAME, sourceProduct, targetProduct, true);
        }

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        targetProduct.setAutoGrouping(isRadToReflMode() ? sensor.getReflAutogroupingString() : sensor.getRadAutogroupingString());

        return targetProduct;
    }


    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        checkCancellation();
        int bandIndex = -1;
        for (int i = 0; i < spectralOutputBandNames.length; i++) {
            if (spectralOutputBandNames[i].equals(targetBand.getName())) {
                bandIndex = i;
            }
        }

        final Rectangle rectangle = targetTile.getRectangle();

        final float[] samplesFlux = getSampleFlux(bandIndex, rectangle);
        final float[] samplesSZA = getSampleSZA(rectangle);

        final Tile sourceTile = getSourceTile(sourceProduct.getBand(spectralInputBandNames[bandIndex]), rectangle);
        final float[] radiances = sourceTile.getSamplesFloat();

        final float[] reflectances = converter.convert(radiances, samplesSZA, samplesFlux);
        targetTile.setSamples(reflectances);
    }


    private float[] getSampleSZA(Rectangle rectangle) {
        final Tile sourceTileSza = getSourceTile(sourceProduct.getTiePointGrid("SZA"), rectangle);
        return sourceTileSza.getSamplesFloat();
    }

    private float[] getSampleFlux(int bandIndex, Rectangle rectangle) {
        float[] samplesFlux = new float[0];
        if (sensor.equals(Sensor.OLCI)) {
            samplesFlux = getSourceTile(sourceProduct.getBand(sensor.getSolarFluxBandNames()[bandIndex]), rectangle).getSamplesFloat();
        } else {
//            final int detectorIndex = getSourceTile(sourceProduct.getBand(sensor.)).getInt();
//            if (detectorIndex >= 0) {
//                double solarFlux = rad2ReflAuxdata.getDetectorSunSpectralFluxes()[detectorIndex][bandIndex];
        }
        return samplesFlux;
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
            super(Rad2ReflOp_Spec.class);
        }
    }
}
