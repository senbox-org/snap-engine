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
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.VirtualBandOpImage;
import org.esa.snap.core.util.ProductUtils;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.esa.snap.dataio.envisat.EnvisatConstants.*;

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
public class Rad2ReflOp extends Operator {

    //    @Parameter(defaultValue = "OLCI", description = "The sensor", valueSet = {"MERIS", "OLCI", "SLSTR_500m"})
    // hide SLSTR for the moment, needs to be tested in more detail (20160818)
    @Parameter(defaultValue = "OLCI", description = "The sensor", valueSet = {"MERIS", "OLCI"})
    private Sensor sensor;

    @Parameter(description = "Conversion mode: from rad to refl, or backwards", valueSet = {"RAD_TO_REFL", "REFL_TO_RAD"},
            defaultValue = "RAD_TO_REFL")
    private String conversionMode;

    @SourceProduct(alias = "source", label = "Name", description = "The source product.")
    private Product sourceProduct;

    @Parameter(defaultValue = "false", description = "If set, non-spectral bands from source product are written to target product")
    private boolean copyNonSpectralBands;

    private RadReflConverter converter;

    private transient int currentPixel = 0;
    private String spectralInputBandPrefix;
    private Product targetProduct;
    private Rad2ReflAuxdata rad2ReflAuxdata;

    private String[] spectralInputBandNames;
    private String[] spectralOutputBandNames;

    private VirtualBandOpImage invalidImage;


    @Override
    public void initialize() throws OperatorException {
        spectralInputBandPrefix = isRadToReflMode() ? "radiance" : "reflectance";
        spectralInputBandNames = isRadToReflMode() ? sensor.getRadBandNames() : sensor.getReflBandNames();
        spectralOutputBandNames = isRadToReflMode() ? sensor.getReflBandNames() : sensor.getRadBandNames();


        Product targetProduct = createTargetProduct();
        setTargetProduct(targetProduct);

        setupInvalidImage(sensor.getInvalidPixelExpression());

        boolean checkSensor = checkSensor(spectralInputBandNames);
        if (sensor == Sensor.MERIS && checkSensor) {
            converter = new MerisRadReflConverter(conversionMode);
            try {
                rad2ReflAuxdata = Rad2ReflAuxdata.loadMERISAuxdata(sourceProduct.getProductType());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (sensor == Sensor.OLCI && checkSensor) {
            converter = new OlciRadReflConverter(conversionMode);
        } else if (sensor == Sensor.SLSTR_500m && checkSensor) {
            converter = new SlstrRadReflConverter(conversionMode);
        } else {
            throw new OperatorException("Sensor '" + sensor.getName() + "' not supported. Please check the sensor selection.");
        }

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

        if (bandIndex >= 0) {
            final Rectangle rectangle = targetTile.getRectangle();

            Raster isInvalid = invalidImage.getData(rectangle);
            // NOTE: in this case we use a single invalid expression for ALL target bands (e.g. l1_flags.INVALID for MERIS).
            // In general, we may have different invalid expressions for different bands (e.g. containing thresholds etc.)
            // In that case, we would have to use rasterDataNode.isPixelValid(x, y), which is rather slow...

            final Tile szaTile = getSzaSourceTile(rectangle);
            final Band radianceBand = sourceProduct.getBand(spectralInputBandNames[bandIndex]);
            final Tile radianceTile = getSourceTile(radianceBand, rectangle);

            Tile detectorIndexTile = null;
            if (sensor == Sensor.MERIS) {
                detectorIndexTile = getSourceTile(sourceProduct.getBand(MERIS_DETECTOR_INDEX_DS_NAME), rectangle);
            }
            Tile solarFluxTile = null;
            if (sensor == Sensor.OLCI) {
                solarFluxTile = getSourceTile(sourceProduct.getBand(sensor.getSolarFluxBandNames()[bandIndex]), rectangle);
            }

            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                checkForCancellation();
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    if (isInvalid.getSample(x, y, 0) != 0) {
                        targetTile.setSample(x, y, Float.NaN);
                    } else {
                        float solarFlux = Float.NaN;
                        if (solarFluxTile != null && sensor == Sensor.OLCI) {
                            solarFlux = solarFluxTile.getSampleFloat(x, y);
                        } else if (detectorIndexTile != null && sensor == Sensor.MERIS) {
                            final int detectorIndex = detectorIndexTile.getSampleInt(x, y);
                            if (detectorIndex >= 0) {
                                solarFlux = (float) rad2ReflAuxdata.getDetectorSunSpectralFluxes()[detectorIndex][bandIndex];
                            } else {
                                solarFlux = radianceBand.getSolarFlux();
                            }
                        } else if (sensor == Sensor.SLSTR_500m) {
                            final int channel = Integer.parseInt(Sensor.SLSTR_500m.getRadBandNames()[bandIndex].substring(1, 2));
                            solarFlux = Sensor.SLSTR_500m.getSolarFluxesDefault()[channel - 1];
                        }

                        final float sza = szaTile.getSampleFloat(x, y);
                        final float radiance = radianceTile.getSampleFloat(x, y);
                        final float reflectance = converter.convert(radiance, sza, solarFlux);
                        targetTile.setSample(x, y, reflectance);
                    }
                }
            }
        }
    }

    //todo 2 mba/** write a testcase 06/05/2016
    private boolean checkSensor(String[] firstBandName) {
        List<String> allBandsName = Arrays.asList(sourceProduct.getBandNames());
        boolean checker = false;
        for (String bandName : firstBandName) {
            checker = allBandsName.contains(bandName);
        }

        return checker;
    }

    private Tile getSzaSourceTile(Rectangle rectangle) {
        Tile sourceTileSza;
        try {
            if (sensor.equals(Sensor.MERIS) || sensor.equals(Sensor.OLCI)) {
                TiePointGrid tiePointGrid = sourceProduct.getTiePointGrid(sensor.getSzaBandNames()[0]);
                if (tiePointGrid == null) {
                    throw new OperatorException("SZA is null ");
                }
                sourceTileSza = getSourceTile(tiePointGrid, rectangle);
            } else {
                if (sourceProduct.getBandAt(0).getName().endsWith("_o")) {
                    sourceTileSza = getSourceTile(sourceProduct.getTiePointGrid(sensor.getSzaBandNames()[0]), rectangle);
                } else {
                    sourceTileSza = getSourceTile(sourceProduct.getTiePointGrid(sensor.getSzaBandNames()[1]), rectangle);
                }
            }
        } catch (OperatorException e) {
            throw new OperatorException("SZA is null ");
        }
        return sourceTileSza;
    }

    private void setupInvalidImage(String expression) {
        invalidImage = VirtualBandOpImage.builder(expression, sourceProduct)
                .dataType(ProductData.TYPE_FLOAT32)
                .fillValue(0.0f)
                .tileSize(sourceProduct.getPreferredTileSize())
                .mask(false)
                .level(ResolutionLevel.MAXRES)
                .create();
    }

    private Product createTargetProduct() {
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        for (int i = 0; i < sensor.getNumSpectralBands(); i++) {
            if (isRadToReflMode()) {
                Band sourceBand = sourceProduct.getBand(sensor.getRadBandNames()[i]);
                createSpectralBand(sourceBand, sensor.getReflBandNames()[i], Rad2ReflConstants.REFL_UNIT,"Reflectance converted from radiance");
            } else {
                Band sourceBand = sourceProduct.getBand(sensor.getReflBandNames()[i]);
                createSpectralBand(sourceBand, sensor.getRadBandNames()[i], Rad2ReflConstants.RAD_UNIT, "Radiance converted from reflectance");
            }
        }

        if (sensor == Sensor.MERIS || sensor == Sensor.OLCI) {
            ProductUtils.copyBand(MERIS_DETECTOR_INDEX_DS_NAME, sourceProduct, targetProduct, true);
        }

        if (sensor != Sensor.MERIS) {
            for (int i = 0; i < sensor.getSolarFluxBandNames().length; i++) {
                ProductUtils.copyBand(sensor.getSolarFluxBandNames()[i], sourceProduct, targetProduct, true);
            }
        }

        if (copyNonSpectralBands) {
            for (Band b : sourceProduct.getBands()) {
                if (!b.getName().contains(spectralInputBandPrefix) && !targetProduct.containsBand(b.getName())) {
                    ProductUtils.copyBand(b.getName(), sourceProduct, targetProduct, true);
                }
            }
//            targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());
        }

        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        final String autogroupingExt = isRadToReflMode() ? sensor.getReflAutogroupingString() : sensor.getRadAutogroupingString();
//        targetProduct.setAutoGrouping(isRadToReflMode() ? sensor.getReflAutogroupingString() : sensor.getRadAutogroupingString());
        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping() + ":" + autogroupingExt);

        return targetProduct;
    }

    private Band createSpectralBand(Band sourceBand, String targetBandName, String unit, String description) {
        Band targetBand = new Band(targetBandName, ProductData.TYPE_FLOAT32,
                                   sourceBand.getRasterWidth(), sourceBand.getRasterHeight());
        targetProduct.addBand(targetBand);
        ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
        targetBand.setUnit(description);
        targetBand.setUnit(unit);
        targetBand.setScalingFactor(1);
        targetBand.setNoDataValue(Float.NaN);
        targetBand.setNoDataValueUsed(true);
        targetBand.setSpectralBandIndex(sourceBand.getSpectralBandIndex());
        targetBand.setSpectralWavelength(sourceBand.getSpectralWavelength());
        targetBand.setSpectralBandwidth(sourceBand.getSpectralBandwidth());

        return targetBand;
    }

    private boolean isRadToReflMode() {
        return conversionMode.equals("RAD_TO_REFL");
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
