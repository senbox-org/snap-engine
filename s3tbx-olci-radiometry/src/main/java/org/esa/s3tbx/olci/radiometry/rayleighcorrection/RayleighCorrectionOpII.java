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

package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

import com.bc.ceres.core.ProgressMonitor;
import com.google.common.primitives.Doubles;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.esa.s3tbx.olci.radiometry.gaseousabsorption.GaseousAbsorptionAuxII;
import org.esa.s3tbx.olci.radiometry.smilecorr.SmileUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;

/**
 * @author muhammad.bc.
 */
@OperatorMetadata(alias = "Olci.RayleighCorrectionII",
        description = "Performs radiometric corrections on OLCI L1b data products.",
        authors = " Marco Peters, Muhammad Bala (Brockmann Consult)",
        copyright = "(c) 2015 by Brockmann Consult",
        category = "Optical/Pre-Processing",
        version = "1.2")
public class RayleighCorrectionOpII extends Operator {
    private static final String[] BAND_CATEGORIES = new String[]{
            "taur_%02d",
            "transSRay_%02d",
            "transVRay_%02d",
            "sARay_%02d",
            "rtoaRay_%02d",
            "rBRR_%02d",
            "sphericalAlbedoFactor_%02d",
            "RayleighSimple_%02d",
            "rtoa_ng_%02d",
            "taurS_%02d",
            "rtoa_%02d"
    };


    private final String OLCI = "OLCI";
    private final String MERIS_DEM_ALT = "dem_alt";
    private final String MERIS_SUN_AZIMUTH = "sun_azimuth";
    private final String MERIS_SUN_ZENITH = "sun_zenith";
    private final String MERIS_VIEW_ZENITH = "view_zenith";
    private final String MERIS_VIEW_AZIMUTH = "view_azimuth";
    private final String MERIS_ATM_PRESS = "atm_press";
    private final String MERIS_OZONE = "ozone";
    private final String MERIS_LATITUDE = "latitude";
    private final String MERIS_LONGITUDE = "longitude";
    private final String SZA = "SZA";
    private final String OZA = "OZA";
    private final String SAA = "SAA";
    private final String OAA = "OAA";
    private final String SEA_LEVEL_PRESSURE = "sea_level_pressure";
    private final String TOTAL_OZONE = "total_ozone";
    private final String TP_LATITUDE = "TP_latitude";
    private final String TP_LONGITUDE = "TP_longitude";

    @SourceProduct
    Product sourceProduct;

    private RayleighCorrAlgorithm algorithm;
    GaseousAbsorptionAuxII gaseousAbsorptionAuxII;
    private Sensor sensor;
    private double[] absorpOzone;
    private double[] crossSectionSigma;
    private AuxiliaryValues auxiliaryValues = new AuxiliaryValues(AuxiliaryValues.GETASSE_30);


    @Override
    public void initialize() throws OperatorException {

        Product targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        sensor = getSensorPattern();
        algorithm = new RayleighCorrAlgorithm();
        gaseousAbsorptionAuxII = new GaseousAbsorptionAuxII();
        absorpOzone = gaseousAbsorptionAuxII.absorptionOzone(sensor.toString());
        targetProduct.addBand("airmass", ProductData.TYPE_FLOAT32);
        targetProduct.addBand("azidiff", ProductData.TYPE_FLOAT32);

        for (String bandCategory : BAND_CATEGORIES) {
            for (int i = 1; i <= sensor.getNumBands(); i++) {
                Band sourceBand = sourceProduct.getBand(String.format(sensor.getPattern, i));
                Band targetBand = targetProduct.addBand(String.format(bandCategory, i), ProductData.TYPE_FLOAT32);
                ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
            }
        }


        ProductUtils.copyBand("altitude", sourceProduct, targetProduct, true);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
        targetProduct.setAutoGrouping("RayleighSimple:taurS:rtoa:taur:transSRay:rtoa_ng:transVRay:sARay:rtoaRay:rBRR:sphericalAlbedoFactor");
        setTargetProduct(targetProduct);

        String getPattern = sensor.getGetPattern();
        List<Double> waveLenght = new ArrayList<>();
        for (int i = 1; i <= sensor.getNumBands(); i++) {
            waveLenght.add((double) getSourceProduct().getBand(String.format(getPattern, i)).getSpectralWavelength());
        }
        crossSectionSigma = algorithm.getCrossSectionSigma(Doubles.toArray(waveLenght));
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        Iterator<Band> iterator = targetTiles.keySet().iterator();
        HashMap<String, double[]> rhoBrrHashMap = new HashMap<>();
        HashMap<String, double[]> correctHashMap = new HashMap<>();

        while (iterator.hasNext()) {
            Band targetBand = iterator.next();
            Tile targetTile = targetTiles.get(targetBand);

            String targetBandName = targetBand.getName();
            float spectralWavelength = targetBand.getSpectralWavelength();
            String sourceBandName = null;
            if (spectralWavelength > 0) {
                sourceBandName = getSourceBand(spectralWavelength);
            }


            AuxiliaryValues auxiliaryValues = getRayleighValues(sensor, sourceBandName, targetRectangle);
            int sourceBandIndex = auxiliaryValues.getSourceBandIndex();

            if (targetBandName.equals("airmass")) {
                double[] massAirs = SmileUtils.getAirMass(auxiliaryValues);
                targetTile.setSamples(massAirs);
                continue;
            } else if (targetBandName.equals("azidiff")) {
                double[] aziDifferences = SmileUtils.getAziDiff(auxiliaryValues);
                targetTile.setSamples(aziDifferences);
                continue;
            }

            double[] reflectance = algorithm.convertRadsToRefls(auxiliaryValues);
            double[] rayleighOpticalThickness = algorithm.getRayleighOpticalThicknessII(auxiliaryValues, crossSectionSigma[sourceBandIndex - 1]);

            boolean isRayleighSample = targetBandName.matches("RayleighSimple_\\d{2}");
            boolean isTaurS = targetBandName.matches("taurS_\\d{2}");

            if (isRayleighSample || isTaurS) {
                if (!correctHashMap.containsKey(targetBandName)) {
                    double[] phaseRaylMin = algorithm.getPhaseRaylMin(auxiliaryValues);
                    correctHashMap = algorithm.correct(auxiliaryValues, phaseRaylMin, sourceBandIndex);
                }
                if (isRayleighSample) {
                    setSamples(correctHashMap, targetTile);
                    continue;
                }
                if (isTaurS) {
                    setSamples(correctHashMap, targetTile);
                }

            } else if (targetBandName.matches("rtoa_\\d{2}")) {
                targetTile.setSamples(reflectance);
            } else if (targetBandName.matches("taur_\\d{2}")) {
                targetTile.setSamples(rayleighOpticalThickness);
            } else {
                if (Math.ceil(spectralWavelength) == 709) { // band 709
                    String bandNamePattern = sensor.getGetPattern();
                    int[] upperLowerBounds = sensor.getUpperLowerBounds();
                    double[] bWVRefTile = getSourceTile(sourceProduct.getBand(String.format(bandNamePattern, upperLowerBounds[0])), targetRectangle).getSamplesDouble();
                    double[] bWVTile = getSourceTile(sourceProduct.getBand(String.format(bandNamePattern, upperLowerBounds[1])), targetRectangle).getSamplesDouble();
                    waterVaporCorrection709(reflectance, bWVRefTile, bWVTile);
                }
                double[] corrOzoneRefl = algorithm.getCorrOzone(auxiliaryValues, reflectance, absorpOzone[sourceBandIndex - 1]);
                if (targetBandName.matches("rtoa_ng_\\d{2}")) {
                    targetTile.setSamples(corrOzoneRefl);
                    continue;
                }

                if (!rhoBrrHashMap.containsKey(targetBandName)) {
                    rhoBrrHashMap = algorithm.getRhoBrr(auxiliaryValues, rayleighOpticalThickness, corrOzoneRefl, sourceBandIndex);
                }
                setSamples(rhoBrrHashMap, targetTile);
            }
        }
    }


    private void setSamples(HashMap<String, double[]> rhoBrrHashMap, Tile targetTile) {
        double[] transSRays = rhoBrrHashMap.get(targetTile.getRasterDataNode().getName());
        targetTile.setSamples(transSRays);
    }

    private AuxiliaryValues getRayleighValues(Sensor sensor, String sourceBandName, Rectangle rectangle) {
        if (sensor.equals(Sensor.MERIS)) {
            auxiliaryValues.setSunAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(MERIS_SUN_AZIMUTH), rectangle).getSamplesDouble());
            auxiliaryValues.setSunZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(MERIS_SUN_ZENITH), rectangle).getSamplesDouble());
            auxiliaryValues.setViewZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(MERIS_VIEW_ZENITH), rectangle).getSamplesDouble());
            auxiliaryValues.setViewAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(MERIS_VIEW_AZIMUTH), rectangle).getSamplesDouble());
            auxiliaryValues.setSeaLevels(getSourceTile(sourceProduct.getTiePointGrid(MERIS_ATM_PRESS), rectangle).getSamplesDouble());
            auxiliaryValues.setTotalOzones(getSourceTile(sourceProduct.getTiePointGrid(MERIS_OZONE), rectangle).getSamplesDouble());

            double[] latitude = getSourceTile(sourceProduct.getTiePointGrid(MERIS_LATITUDE), rectangle).getSamplesDouble();
            double[] longitude = getSourceTile(sourceProduct.getTiePointGrid(MERIS_LONGITUDE), rectangle).getSamplesDouble();
            auxiliaryValues.setLatitudes(latitude);
            auxiliaryValues.setAltitudes(longitude, latitude);


            if (sourceBandName != null) {
                Band sourceBand = sourceProduct.getBand(sourceBandName);
                int size = rectangle.width * rectangle.height;
                double[] solarFluxs = new double[size];
                double[] lambdaSource = new double[size];
                Arrays.fill(solarFluxs, (double) sourceBand.getSolarFlux());
                Arrays.fill(lambdaSource, (double) sourceBand.getSpectralWavelength());

                auxiliaryValues.setSolarFluxs(solarFluxs);
                auxiliaryValues.setLambdaSource(lambdaSource);
                auxiliaryValues.setSourceBandIndex(Integer.parseInt(sourceBandName.substring(9, sourceBandName.length())));
                auxiliaryValues.setSourceSampleRad(getSourceTile(sourceBand, rectangle).getSamplesDouble());
            }
            return auxiliaryValues;

        } else if (sensor.equals(Sensor.OLCI)) {
            auxiliaryValues.setSunZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(SZA), rectangle).getSamplesDouble());
            auxiliaryValues.setViewZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(OZA), rectangle).getSamplesDouble());
            auxiliaryValues.setSunAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(SAA), rectangle).getSamplesDouble());
            auxiliaryValues.setViewAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(OAA), rectangle).getSamplesDouble());
            auxiliaryValues.setSeaLevels(getSourceTile(sourceProduct.getTiePointGrid(SEA_LEVEL_PRESSURE), rectangle).getSamplesDouble());
            auxiliaryValues.setTotalOzones(getSourceTile(sourceProduct.getTiePointGrid(TOTAL_OZONE), rectangle).getSamplesDouble());

            double[] tp_latitudes = getSourceTile(sourceProduct.getTiePointGrid(TP_LATITUDE), rectangle).getSamplesDouble();
            double[] tp_longitude = getSourceTile(sourceProduct.getTiePointGrid(TP_LONGITUDE), rectangle).getSamplesDouble();
            auxiliaryValues.setLatitudes(tp_latitudes);
            auxiliaryValues.setAltitudes(tp_longitude, tp_latitudes);

            if (sourceBandName != null) {
                String extractBandIndex = sourceBandName.substring(2, 4);
                int sourceBandIndex = Integer.parseInt(extractBandIndex);
                auxiliaryValues.setSourceBandIndex(sourceBandIndex);
                auxiliaryValues.setSolarFluxs(getSourceTile(sourceProduct.getBand(String.format("solar_flux_band_%d", sourceBandIndex)), rectangle).getSamplesDouble());
                auxiliaryValues.setLambdaSource(getSourceTile(sourceProduct.getBand(String.format("lambda0_band_%d", sourceBandIndex)), rectangle).getSamplesDouble());
                auxiliaryValues.setSourceSampleRad(getSourceTile(sourceProduct.getBand(sourceBandName), rectangle).getSamplesDouble());
            }
            return auxiliaryValues;
        }
        throw new IllegalArgumentException("Sensor is not supported");
    }

    private void waterVaporCorrection709(double[] reflectances, double[] bWVRefTile, double[] bWVTile) {
        double[] H2O_COR_POLY = new double[]{0.3832989, 1.6527957, -1.5635101, 0.5311913};  // Polynomial coefficients for WV transmission @ 709nm
        // in order to optimise performance we do:
        // trans709 = H2O_COR_POLY[0] + (H2O_COR_POLY[1] + (H2O_COR_POLY[2] + H2O_COR_POLY[3] * X2) * X2) * X2
        // when X2 = 1
        // trans709 = 0.3832989 + ( 1.6527957+ (-1.5635101+ 0.5311913*1)*1)*1
        double trans709 = 1.0037757999999999;
        for (int i = 0; i < bWVTile.length; i++) {
            if (bWVRefTile[i] > 0) {
                double X2 = bWVTile[i] / bWVRefTile[i];
                trans709 = H2O_COR_POLY[0] + (H2O_COR_POLY[1] + (H2O_COR_POLY[2] + H2O_COR_POLY[3] * X2) * X2) * X2;
            }
            reflectances[i] = reflectances[i] / trans709;
        }
    }

    //todo mba/** write a test
    private String getSourceBand(float spectralWavelength) {
        Band[] bands = sourceProduct.getBands();
        List<Band> collectBand = Arrays.stream(bands).filter(p -> p.getSpectralWavelength() == spectralWavelength &&
                !p.getName().contains("err")).collect(Collectors.toList());
        return collectBand.get(0).getName();
    }

    private Sensor getSensorPattern() {
        String[] bandNames = getSourceProduct().getBandNames();
        boolean isSensor = Stream.of(bandNames).anyMatch(p -> p.matches("Oa\\d+_radiance"));
        if (isSensor) {
            return Sensor.OLCI;
        }
        isSensor = Stream.of(bandNames).anyMatch(p -> p.matches("radiance_\\d+"));

        if (isSensor) {
            return Sensor.MERIS;
        }
        throw new OperatorException("The operator can't be applied on the sensor");
    }

    private enum Sensor {
        MERIS("radiance_%d", 15, new int[]{13, 14}),
        OLCI("Oa%02d_radiance", 21, new int[]{17, 18});

        public int[] getUpperLowerBounds() {
            return side;
        }

        private final int[] side;
        final int numBands;
        final String getPattern;

        public int getNumBands() {
            return numBands;
        }

        public String getGetPattern() {
            return getPattern;
        }

        Sensor(String getPattern, int numBands, int[] side) {
            this.numBands = numBands;
            this.getPattern = getPattern;
            this.side = side;
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(RayleighCorrectionOpII.class);
        }
    }
}
