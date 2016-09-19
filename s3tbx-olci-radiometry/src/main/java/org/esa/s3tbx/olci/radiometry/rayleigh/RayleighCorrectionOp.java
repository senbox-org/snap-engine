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

package org.esa.s3tbx.olci.radiometry.rayleigh;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.olci.radiometry.gasabsorption.GaseousAbsorptionAux;
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
import org.esa.snap.core.util.ProductUtils;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author muhammad.bc.
 */
@OperatorMetadata(alias = "RayleighCorrection",
        description = "Performs radiometric corrections on OLCI and MERIS L1b data products.",
        authors = " Marco Peters, Muhammad Bala (Brockmann Consult)",
        copyright = "(c) 2015 by Brockmann Consult",
        category = "Optical/Pre-Processing",
        version = "1.2")
public class RayleighCorrectionOp extends Operator {
    private static final String[] BAND_CATEGORIES = new String[]{
            "taur_%02d",
            "rBRR_%02d",
            "rtoa_ng_%02d",
            "rtoa_%02d",
    };
    private static final String AIRMASS = "airmass";
    public static final String ALTITUDE = "altitude";
    private static final String RTOA_PATTERN = "rtoa_\\d{2}";
    private static final String TAUR_PATTERN = "taur_\\d{2}";
    private static final String RTOA_NG_PATTERN = "rtoa_ng_\\d{2}";
    public static final String R_BRR_PATTERN = "rBRR_\\d{2}";
    public static final String AUTO_GROUPING = "rtoa:taur:rtoa_ng:rtoaRay:rBRR";
    public static final int WV_709_FOR_GASEOUS_ABSORPTION_CALCULATION = 709;
    public static final String SOLAR_FLUX_BAND_PATTERN = "solar_flux_band_%d";
    public static final String LAMBDA0_BAND_PATTERN = "lambda0_band_%d";


    public static final String MERIS_SUN_AZIMUTH = "sun_azimuth";
    public static final String MERIS_SUN_ZENITH = "sun_zenith";
    public static final String MERIS_VIEW_ZENITH = "view_zenith";
    public static final String MERIS_VIEW_AZIMUTH = "view_azimuth";
    public static final String MERIS_ATM_PRESS = "atm_press";
    public static final String MERIS_OZONE = "ozone";
    public static final String MERIS_LATITUDE = "latitude";
    public static final String MERIS_LONGITUDE = "longitude";
    public static final String SZA = "SZA";
    public static final String OZA = "OZA";
    public static final String SAA = "SAA";
    public static final String OAA = "OAA";
    public static final String SEA_LEVEL_PRESSURE = "sea_level_pressure";
    public static final String TOTAL_OZONE = "total_ozone";
    public static final String TP_LATITUDE = "TP_latitude";
    public static final String TP_LONGITUDE = "TP_longitude";

    @SourceProduct
    Product sourceProduct;

    @Parameter(defaultValue = "false", label = "Add Rayleig optical thickness bands")
    private boolean isTaurSelected;


    @Parameter(defaultValue = "false", label = "Add bottom of Rayleigh reflectance bands")
    private boolean isRBrr;

    @Parameter(defaultValue = "false", label = "Add gaseous absorption corrected TOA reflectance bands")
    private boolean isRtoa_ngSelected;

    @Parameter(defaultValue = "false", label = "Add TOA reflectance bands")
    private boolean isRtoaSelected;

    @Parameter(defaultValue = "false", label = "Add air mass")
    private boolean isAirMass;


    private RayleighCorrAlgorithm algorithm;
    private Sensor sensor;
    private double[] absorpOzone;
    private double[] crossSectionSigma;

    @Override
    public void initialize() throws OperatorException {
        sensor = getSensorPattern();
        algorithm = new RayleighCorrAlgorithm();
        absorpOzone = GaseousAbsorptionAux.getInstance().absorptionOzone(sensor.toString());
        crossSectionSigma = getCrossSectionSigma(sourceProduct, sensor.getNumBands(), sensor.getGetPattern());

        Product targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                            sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        RayleighAux.initDefaultAuxiliary();
        initTargetBand(targetProduct);
        ProductUtils.copyBand(ALTITUDE, sourceProduct, targetProduct, true);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
        targetProduct.setAutoGrouping(AUTO_GROUPING);
        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        checkForCancellation();
        RayleighAux rayleighAux = createAuxiliary(sensor, targetRectangle);
        for (Band targetBand : targetTiles.keySet()) {
            Tile targetTile = targetTiles.get(targetBand);
            String targetBandName = targetBand.getName();
            int sourceBandIndex = getSourceBandIndex(targetBand.getName());

            if (targetBandName.equals(AIRMASS) && isAirMass) {
                double[] massAirs = rayleighAux.getAirMass();
                targetTile.setSamples(massAirs);
                continue;
            }
            if (sourceBandIndex == -1) {
                continue;
            }
            initAuxBand(rayleighAux, targetRectangle, sourceBandIndex);

            if (targetBandName.matches(RTOA_PATTERN) && isRtoaSelected) {
                targetTile.setSamples(getReflectance(rayleighAux));
            } else if (targetBandName.matches(TAUR_PATTERN) && isTaurSelected) {
                double[] rayleighOpticalThickness = getRayleighThickness(rayleighAux, crossSectionSigma, sourceBandIndex);
                targetTile.setSamples(rayleighOpticalThickness);
            } else if (isRBrr || isRtoa_ngSelected) {
                double[] reflectance = getReflectance(rayleighAux);
                if (Math.ceil(rayleighAux.getWaveLenght()) == WV_709_FOR_GASEOUS_ABSORPTION_CALCULATION) {
                    reflectance = waterVaporCorrection709(reflectance, targetRectangle, sensor);
                }
                double[] corrOzoneRefl = getCorrectOzone(rayleighAux, reflectance, sourceBandIndex);
                if (targetBandName.matches(RTOA_NG_PATTERN) && isRtoa_ngSelected) {
                    targetTile.setSamples(corrOzoneRefl);
                    continue;
                }
                if (targetBandName.matches(R_BRR_PATTERN) && isRBrr) {
                    double[] rayleighOpticalThickness = getRayleighThickness(rayleighAux, crossSectionSigma, sourceBandIndex);
                    double[] rhoBrr = getRhoBrr(rayleighAux, rayleighOpticalThickness, corrOzoneRefl);
                    targetTile.setSamples(rhoBrr);
                }
            }
        }
    }

    private double[] waterVaporCorrection709(double[] reflectances, Rectangle targetRectangle, Sensor sensor) {
        String bandNamePattern = sensor.getGetPattern();
        int[] upperLowerBounds = sensor.getBounds();
        double[] bWVRefTile = RayleighAux.getSampleDoubles(getSourceTile(sourceProduct.getBand(String.format(bandNamePattern, upperLowerBounds[1])), targetRectangle));
        double[] bWVTile = RayleighAux.getSampleDoubles(getSourceTile(sourceProduct.getBand(String.format(bandNamePattern, upperLowerBounds[0])), targetRectangle));
        return algorithm.waterVaporCorrection709(reflectances, bWVRefTile, bWVTile);
    }

    private double[] getRhoBrr(RayleighAux rayleighAux, double[] rayleighOpticalThickness, double[] corrOzoneRefl) {
        return algorithm.getRhoBrr(rayleighAux, rayleighOpticalThickness, corrOzoneRefl);
    }


    private double[] getCorrectOzone(RayleighAux rayleighAux, double[] reflectance, int sourceBandIndex) {
        double absorpO = absorpOzone[sourceBandIndex - 1];
        double[] totalOzones = rayleighAux.getTotalOzones();
        double[] cosOZARads = rayleighAux.getCosOZARads();
        double[] cosSZARads = rayleighAux.getCosSZARads();

        return algorithm.getCorrOzone(reflectance, absorpO, totalOzones, cosOZARads, cosSZARads);
    }

    double[] getCrossSectionSigma(Product sourceProduct, int numBands, String getBandNamePattern) {
        return algorithm.getCrossSectionSigma(sourceProduct, numBands, getBandNamePattern);
    }


    private double[] getRayleighThickness(RayleighAux rayleighAux, double[] crossSectionSigma, int sourceBandIndex) {
        double[] seaLevels = rayleighAux.getSeaLevels();
        double[] altitudes = rayleighAux.getAltitudes();
        double[] latitudes = rayleighAux.getLatitudes();
        double sigma = crossSectionSigma[sourceBandIndex - 1];

        double rayleighOpticalThickness[] = new double[altitudes.length];
        for (int i = 0; i < altitudes.length; i++) {
            rayleighOpticalThickness[i] = algorithm.getRayleighOpticalThickness(sigma, seaLevels[i], altitudes[i], latitudes[i]);
        }

        return rayleighOpticalThickness;
    }

    private double[] getReflectance(RayleighAux rayleighAux) {
        double[] sourceSampleRad = rayleighAux.getSourceSampleRad();
        double[] solarFluxs = rayleighAux.getSolarFluxs();
        double[] sunZenithAngles = rayleighAux.getSunZenithAngles();

        return algorithm.convertRadsToRefls(sourceSampleRad, solarFluxs, sunZenithAngles);
    }

    private void initTargetBand(Product targetProduct) {
        List<String> bandsToInit = new ArrayList<>();
        if (isTaurSelected) {
            bandsToInit.add(BAND_CATEGORIES[0]);
        }
        if (isRBrr) {
            bandsToInit.add(BAND_CATEGORIES[1]);
        }
        if (isRtoa_ngSelected) {
            bandsToInit.add(BAND_CATEGORIES[2]);
        }
        if (isRtoaSelected) {
            bandsToInit.add(BAND_CATEGORIES[3]);
        }
        if (isAirMass) {
            targetProduct.addBand(AIRMASS, ProductData.TYPE_FLOAT32);
        }
        createTargetBand(targetProduct, bandsToInit);
    }


    private void createTargetBand(Product targetProduct, List<String> bandCategoryList) {
        for (String bandCategory : bandCategoryList) {
            for (int i = 1; i <= sensor.getNumBands(); i++) {
                Band sourceBand = sourceProduct.getBand(String.format(sensor.getPattern, i));
                Band targetBand = targetProduct.addBand(String.format(bandCategory, i), ProductData.TYPE_FLOAT32);
                ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
            }
        }
    }

    private int initAuxBand(RayleighAux rayleighAux, Rectangle rectangle, int sourceBandRefIndex) {
        String format = String.format(sensor.getGetPattern(), sourceBandRefIndex);
        Band band = getSourceProduct().getBand(format);
        String sourceBandName = band.getName();
        rayleighAux.setWavelength(band.getSpectralWavelength());
        rayleighAux.setSourceBandIndex(sourceBandRefIndex);
        rayleighAux.setSourceBandName(sourceBandName);

        if (sensor.equals(Sensor.OLCI)) {
            rayleighAux.setSolarFluxs(getSourceTile(sourceProduct.getBand(String.format(SOLAR_FLUX_BAND_PATTERN, sourceBandRefIndex)), rectangle));
            rayleighAux.setLambdaSource(getSourceTile(sourceProduct.getBand(String.format(LAMBDA0_BAND_PATTERN, sourceBandRefIndex)), rectangle));
            rayleighAux.setSourceSampleRad(getSourceTile(sourceProduct.getBand(sourceBandName), rectangle));
        } else if (sensor.equals(Sensor.MERIS)) {
            Band sourceBand = sourceProduct.getBand(sourceBandName);
            rayleighAux.setSourceSampleRad(getSourceTile(sourceBand, rectangle));
            int length = (int) (rectangle.getWidth() * rectangle.getHeight());

            double[] solarFlux = fillDefaultArray(length, sourceBand.getSolarFlux());
            double[] lambdaSource = fillDefaultArray(length, sourceBand.getSpectralWavelength());

            rayleighAux.setSolarFluxs(solarFlux);
            rayleighAux.setLambdaSource(lambdaSource);
        }
        return sourceBandRefIndex;
    }

    private double[] fillDefaultArray(int length, double value) {
        double[] createArray = new double[length];
        Arrays.fill(createArray, value);
        return createArray;
    }

    int getSourceBandIndex(String name) {
        Matcher matcher = Pattern.compile("(\\d+)").matcher(name);
        if (!matcher.find()) {
            return -1;
        }
        String group = matcher.group(0);
        return Integer.parseInt(group);
    }

    private RayleighAux createAuxiliary(Sensor sensor, Rectangle rectangle) {
        RayleighAux rayleighAux = new RayleighAux();
        if (sensor.equals(Sensor.MERIS)) {
            rayleighAux.setSunAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(MERIS_SUN_AZIMUTH), rectangle));
            rayleighAux.setSunZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(MERIS_SUN_ZENITH), rectangle));
            rayleighAux.setViewZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(MERIS_VIEW_ZENITH), rectangle));
            rayleighAux.setViewAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(MERIS_VIEW_AZIMUTH), rectangle));
            rayleighAux.setSeaLevels(getSourceTile(sourceProduct.getTiePointGrid(MERIS_ATM_PRESS), rectangle));
            rayleighAux.setTotalOzones(getSourceTile(sourceProduct.getTiePointGrid(MERIS_OZONE), rectangle));
            rayleighAux.setLatitudes(getSourceTile(sourceProduct.getTiePointGrid(MERIS_LATITUDE), rectangle));
            rayleighAux.setLongitude(getSourceTile(sourceProduct.getTiePointGrid(MERIS_LONGITUDE), rectangle));
            rayleighAux.setAltitudes();


        } else if (sensor.equals(Sensor.OLCI)) {
            rayleighAux.setSunZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(SZA), rectangle));
            rayleighAux.setViewZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(OZA), rectangle));
            rayleighAux.setSunAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(SAA), rectangle));
            rayleighAux.setViewAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(OAA), rectangle));
            rayleighAux.setSeaLevels(getSourceTile(sourceProduct.getTiePointGrid(SEA_LEVEL_PRESSURE), rectangle));
            rayleighAux.setTotalOzones(getSourceTile(sourceProduct.getTiePointGrid(TOTAL_OZONE), rectangle));
            rayleighAux.setLatitudes(getSourceTile(sourceProduct.getTiePointGrid(TP_LATITUDE), rectangle));
            rayleighAux.setLongitude(getSourceTile(sourceProduct.getTiePointGrid(TP_LONGITUDE), rectangle));
            rayleighAux.setAltitudes(getSourceTile(sourceProduct.getBand(ALTITUDE), rectangle));
//            auxiliaryValues.setAltitudes();
        }

        if (isAirMass || isRBrr) {
            rayleighAux.setAirMass();
        }

        if (isRBrr) {
            rayleighAux.setAziDifferent();
            rayleighAux.setFourier();
//            auxiliaryValues.setInterpolation();
            rayleighAux.setSpikeInterpolation();
        }
        return rayleighAux;
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
        throw new OperatorException("The operator can't be applied on this sensor.\n" +
                                    "Only OLCI and MERIS are supported");
    }

    private enum Sensor {
        MERIS("radiance_%d", 15, new int[]{13, 14}),
        OLCI("Oa%02d_radiance", 21, new int[]{17, 18});

        public int[] getBounds() {
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
            super(RayleighCorrectionOp.class);
        }
    }
}
