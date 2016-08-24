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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.esa.s3tbx.olci.radiometry.gaseousabsorption.GaseousAbsorptionAuxII;
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
            "rtoa_%02d",
            "rRay_ % 02d",
            "rRayF1_%02d",
            "rRayF2_%02d",
            "rRayF3__%02d"
    };
    private static final String AIRMASS = "airmass";
    private static final String AZIDIFF = "azidiff";
    private static final String ALTITUDE = "altitude";
    private static final String RAYLEIGH_SIMPLE_PATTERN = "RayleighSimple_\\d{2}";
    private static final String TAUR_S_PATTERN = "taurS_\\d{2}";
    private static final String RTOA_PATTERN = "rtoa_\\d{2}";
    private static final String TAUR_D_PATTERN = "taur_\\d{2}";
    private static final String RTOA_NG_D_PATTERN = "rtoa_ng_\\d{2}";


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
    private GaseousAbsorptionAuxII gaseousAbsorptionAuxII;
    private Sensor sensor;
    private double[] absorpOzone;
    private double[] crossSectionSigma;
    private HashMap<String, double[]> cache = new HashMap<>();

    @Override
    public void initialize() throws OperatorException {
        Product targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());


        sensor = getSensorPattern();
        algorithm = new RayleighCorrAlgorithm();
        gaseousAbsorptionAuxII = new GaseousAbsorptionAuxII();
        absorpOzone = gaseousAbsorptionAuxII.absorptionOzone(sensor.toString());
        targetProduct.addBand(AIRMASS, ProductData.TYPE_FLOAT32);
        targetProduct.addBand(AZIDIFF, ProductData.TYPE_FLOAT32);

        for (String bandCategory : BAND_CATEGORIES) {
            for (int i = 1; i <= sensor.getNumBands(); i++) {
                Band sourceBand = sourceProduct.getBand(String.format(sensor.getPattern, i));
                Band targetBand = targetProduct.addBand(String.format(bandCategory, i), ProductData.TYPE_FLOAT32);
                ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
            }
        }


        ProductUtils.copyBand(ALTITUDE, sourceProduct, targetProduct, true);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
        targetProduct.setAutoGrouping("RayleighSimple:taurS:rtoa:taur:transSRay:transVRay:rtoa_ng:sARay:rtoaRay:rBRR:sphericalAlbedoFactor:rRay:rRayF1:rRayF2:rRayF3");
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
        AuxiliaryValues auxiliaryValues = createAuxiliary(sensor, targetRectangle);
        Iterator<Band> iteratorBand = targetTiles.keySet().iterator();

        while (iteratorBand.hasNext()) {
            Band targetBand = iteratorBand.next();
            initWithBandValues(auxiliaryValues, targetBand, targetRectangle);

            Tile targetTile = targetTiles.get(targetBand);
            String targetBandName = targetBand.getName();

            if (targetBandName.equals(AIRMASS)) {
                double[] massAirs = auxiliaryValues.getAirMass();
                targetTile.setSamples(massAirs);
                continue;
            }

            if (targetBandName.equals(AZIDIFF)) {
                double[] aziDifferences = auxiliaryValues.getAziDifferent();
                targetTile.setSamples(aziDifferences);
                continue;
            }

            int sourceBandIndex = auxiliaryValues.getSourceBandIndex();
            double[] reflectance = algorithm.convertRadsToRefls(auxiliaryValues);
            double[] rayleighOpticalThickness = algorithm.getRayleighOpticalThicknessII(auxiliaryValues, crossSectionSigma[sourceBandIndex - 1]);

            boolean isRayleighSample = targetBandName.matches(RAYLEIGH_SIMPLE_PATTERN);
            boolean isTaurS = targetBandName.matches(TAUR_S_PATTERN);
            if (isRayleighSample || isTaurS) {
                if (!cache.containsKey(targetBandName)) {
                    double[] phaseRaylMin = algorithm.getPhaseRaylMin(auxiliaryValues);
                    HashMap<String, double[]> correct = algorithm.correct(auxiliaryValues, phaseRaylMin, sourceBandIndex);
                    addToCache(correct);
                }
                setTargetSamples(cache, targetTile);
            }

            if (targetBandName.matches(RTOA_PATTERN)) {
                targetTile.setSamples(reflectance);
            } else if (targetBandName.matches(TAUR_D_PATTERN)) {
                targetTile.setSamples(rayleighOpticalThickness);
            } else {
                if (Math.ceil(auxiliaryValues.getWaveLenght()) == 709) { // band 709
                    String bandNamePattern = sensor.getGetPattern();
                    int[] upperLowerBounds = sensor.getUpperLowerBounds();
                    double[] bWVRefTile = getSourceTile(sourceProduct.getBand(String.format(bandNamePattern, upperLowerBounds[0])), targetRectangle).getSamplesDouble();
                    double[] bWVTile = getSourceTile(sourceProduct.getBand(String.format(bandNamePattern, upperLowerBounds[1])), targetRectangle).getSamplesDouble();
                    waterVaporCorrection709(reflectance, bWVRefTile, bWVTile);
                }
                double[] corrOzoneRefl = algorithm.getCorrOzone(auxiliaryValues, reflectance, absorpOzone[sourceBandIndex - 1]);
                if (targetBandName.matches(RTOA_NG_D_PATTERN)) {
                    targetTile.setSamples(corrOzoneRefl);
                    continue;
                }

                if (!cache.containsKey(targetBandName) || cache.size() <= 0) {
                    HashMap<String, double[]> rhoBrr = algorithm.getRhoBrr(auxiliaryValues, rayleighOpticalThickness, corrOzoneRefl, sourceBandIndex);
                    addToCache(rhoBrr);
                }
                setTargetSamples(cache, targetTile);
            }
        }
        clearCache();
    }


    private void addToCache(HashMap<String, double[]> correct) {
        correct.keySet().stream().forEach(p -> {
            double[] doubles = correct.get(p);
            cache.put(p, doubles);
        });
    }

    @Override
    public void dispose() {
        super.dispose();
        clearCache();
    }

    private void clearCache() {
        cache.clear();
    }

    private void setTargetSamples(HashMap<String, double[]> valHashMap, Tile targetTile) {
        String targetBandNameKey = targetTile.getRasterDataNode().getName();
        double[] transSRays = valHashMap.get(targetBandNameKey);
        if (Objects.nonNull(transSRays)) {
            targetTile.setSamples(transSRays);
            valHashMap.remove(targetBandNameKey);
        }
    }

    private void initWithBandValues(AuxiliaryValues auxiliaryValues, Band targetBandName, Rectangle rectangle) {
        int sourceBandRefIndex = getSourceBandIndex(targetBandName.getName());
        if (sourceBandRefIndex == -1) {
            return;
        }
        String format = String.format(sensor.getGetPattern(), sourceBandRefIndex);
        Band band = getSourceProduct().getBand(format);
        String sourceBandName = band.getName();
        auxiliaryValues.setWavelength(band.getSpectralWavelength());
        auxiliaryValues.setSourceBandIndex(sourceBandRefIndex);
        auxiliaryValues.setSourceBandName(sourceBandName);

        if (sensor.equals(Sensor.OLCI)) {
            auxiliaryValues.setSolarFluxs(getSourceTile(sourceProduct.getBand(String.format("solar_flux_band_%d", sourceBandRefIndex)), rectangle));
            auxiliaryValues.setLambdaSource(getSourceTile(sourceProduct.getBand(String.format("lambda0_band_%d", sourceBandRefIndex)), rectangle));
            auxiliaryValues.setSourceSampleRad(getSourceTile(sourceProduct.getBand(sourceBandName), rectangle));
        } else if (sensor.equals(Sensor.MERIS)) {
            Band sourceBand = sourceProduct.getBand(sourceBandName);
            auxiliaryValues.setSourceSampleRad(getSourceTile(sourceBand, rectangle));
            int length = (int) (rectangle.getWidth() * rectangle.getHeight());

            double[] solarFluxs = new double[length];
            double[] lambdaSource = new double[length];
            Arrays.fill(solarFluxs, (double) sourceBand.getSolarFlux());
            Arrays.fill(lambdaSource, (double) sourceBand.getSpectralWavelength());
            auxiliaryValues.setSolarFluxs(solarFluxs);
            auxiliaryValues.setLambdaSource(lambdaSource);
        }
    }

    int getSourceBandIndex(String name) {
        Matcher matcher = Pattern.compile("(\\d+)").matcher(name);
        if (!matcher.find()) {
            return -1;
        }
        String group = matcher.group(0);
        return Integer.parseInt(group);
    }

    private AuxiliaryValues createAuxiliary(Sensor sensor, Rectangle rectangle) {
        AuxiliaryValues auxiliaryValues = AuxiliaryValues.getInstance();
        if (sensor.equals(Sensor.MERIS)) {
            auxiliaryValues.setSunAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(MERIS_SUN_AZIMUTH), rectangle));
            auxiliaryValues.setSunZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(MERIS_SUN_ZENITH), rectangle));
            auxiliaryValues.setViewZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(MERIS_VIEW_ZENITH), rectangle));
            auxiliaryValues.setViewAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(MERIS_VIEW_AZIMUTH), rectangle));
            auxiliaryValues.setSeaLevels(getSourceTile(sourceProduct.getTiePointGrid(MERIS_ATM_PRESS), rectangle));
            auxiliaryValues.setTotalOzones(getSourceTile(sourceProduct.getTiePointGrid(MERIS_OZONE), rectangle));
            auxiliaryValues.setLatitudes(getSourceTile(sourceProduct.getTiePointGrid(MERIS_LATITUDE), rectangle));
            auxiliaryValues.setLongitude(getSourceTile(sourceProduct.getTiePointGrid(MERIS_LONGITUDE), rectangle));

            auxiliaryValues.setSunAzimuthAnglesRad();
            auxiliaryValues.setViewAzimuthAnglesRad();
            auxiliaryValues.setSunZenithAnglesRad();
            auxiliaryValues.setViewZenithAnglesRad();
            auxiliaryValues.setCosOZARads();
            auxiliaryValues.setCosSZARads();
            auxiliaryValues.setSinOZARads();
            auxiliaryValues.setSinSZARads();
            auxiliaryValues.setAirMass();
            auxiliaryValues.setAziDifferent();

            auxiliaryValues.setAltitudes();
            auxiliaryValues.setFourier();
            auxiliaryValues.setInterpolation();
//            auxiliaryValues.setInterpolationSpike();


            return auxiliaryValues;

        } else if (sensor.equals(Sensor.OLCI)) {
            auxiliaryValues.setSunZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(SZA), rectangle));
            auxiliaryValues.setViewZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(OZA), rectangle));
            auxiliaryValues.setSunAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(SAA), rectangle));
            auxiliaryValues.setViewAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(OAA), rectangle));
            auxiliaryValues.setSeaLevels(getSourceTile(sourceProduct.getTiePointGrid(SEA_LEVEL_PRESSURE), rectangle));
            auxiliaryValues.setTotalOzones(getSourceTile(sourceProduct.getTiePointGrid(TOTAL_OZONE), rectangle));
            auxiliaryValues.setLatitudes(getSourceTile(sourceProduct.getTiePointGrid(TP_LATITUDE), rectangle));
            auxiliaryValues.setLongitude(getSourceTile(sourceProduct.getTiePointGrid(TP_LONGITUDE), rectangle));

            auxiliaryValues.setSunAzimuthAnglesRad();
            auxiliaryValues.setViewAzimuthAnglesRad();
            auxiliaryValues.setSunZenithAnglesRad();
            auxiliaryValues.setViewZenithAnglesRad();
            auxiliaryValues.setCosOZARads();
            auxiliaryValues.setCosSZARads();
            auxiliaryValues.setSinOZARads();
            auxiliaryValues.setSinSZARads();
            auxiliaryValues.setAirMass();
            auxiliaryValues.setAziDifferent();


            auxiliaryValues.setAltitudes();
            auxiliaryValues.setFourier();
            auxiliaryValues.setInterpolation();


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
