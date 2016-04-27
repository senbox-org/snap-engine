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
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.ProductConfigurer;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
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

    Band[] spectralInputBands;
    String[] spectralInputBandNames;
    String[] spectralOutputBandNames;
    private String spectralInputBandPrefix;
    private Product targetProduct;


    @Override
    public void initialize() throws OperatorException {
        spectralInputBandPrefix = conversionMode.equals("RAD_TO_REFL") ? "radiance" : "reflectance";
        spectralInputBandNames = conversionMode.equals("RAD_TO_REFL") ? sensor.getRadBandNames() : sensor.getReflBandNames();
        spectralOutputBandNames = conversionMode.equals("RAD_TO_REFL") ? sensor.getReflBandNames() : sensor.getRadBandNames();


        Product targetProduct = configureSourceSamples();
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

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        checkCancellation();
        int bandIndex = -1;
        float sza = 0.0f;
        for (int i = 0; i < spectralOutputBandNames.length; i++) {
            if (spectralOutputBandNames[i].equals(targetBand.getName())) {
                bandIndex = i;
            }
        }

        Rectangle rectangle = targetTile.getRectangle();
        int y = rectangle.y;
        int x = rectangle.x;


        sza = sourceProduct.getRasterDataNode("SZA").getPixelFloat(x, y);
        float spectralInputValue = sourceProduct.getRasterDataNode(spectralInputBandNames[bandIndex]).getSampleFloat(x, y);
        float solarFlux = sourceProduct.getRasterDataNode(sensor.getSolarFluxBandNames()[bandIndex]).getSampleFloat(x, y);

        final float convertedValue = converter.convert(spectralInputValue, sza, solarFlux);
        final float result = convertedValue > 0.0f ? convertedValue :
                (float) sourceProduct.getBandAt(bandIndex).getNoDataValue();
        targetBand.setPixelFloat(x, y, result);
    }


    protected Product configureSourceSamples() {
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        for (int i = 0; i < sensor.getNumSpectralBands(); i++) {
            final String spectralOutputBandName = spectralOutputBandNames[i];
            final Band bandAt = sourceProduct.getBandAt(i);
            final Band targetBand = targetProduct.addBand(spectralOutputBandName, bandAt.getDataType());
            ProductUtils.copyRasterDataNodeProperties(bandAt, targetBand);
        }

        if (sensor.getSolarFluxBandNames() != null) {
            // all besides MERIS
            for (int i = 0; i < sensor.getSolarFluxBandNames().length; i++) {
                final Band bandAt = sourceProduct.getBand(sensor.getSolarFluxBandNames()[i]);
                final Band targetBand = targetProduct.addBand(bandAt.getName(), bandAt.getDataType());
                ProductUtils.copyRasterDataNodeProperties(bandAt, targetBand);
            }
        }

        if (sensor == Sensor.MERIS || sensor == Sensor.OLCI) {
            // only OLCI, MERIS
            final Band bandAt = sourceProduct.getBand(MERIS_DETECTOR_INDEX_DS_NAME);
            Band targetBand = targetProduct.addBand(MERIS_DETECTOR_INDEX_DS_NAME, bandAt.getDataType());
            ProductUtils.copyRasterDataNodeProperties(bandAt, targetBand);
        }

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());

        return targetProduct;
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
