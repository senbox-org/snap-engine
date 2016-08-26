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
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.esa.snap.core.gpf.annotations.Parameter;
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
            "rBRR_%02d",
            "rtoa_ng_%02d",
            "rtoa_%02d",
    };
    private static final String AIRMASS = "airmass";
    private static final String ALTITUDE = "altitude";
    private static final String RTOA_PATTERN = "rtoa_\\d{2}";
    private static final String TAUR_D_PATTERN = "taur_\\d{2}";
    private static final String RTOA_NG_D_PATTERN = "rtoa_ng_\\d{2}";
    public static final String R_BRR_D_PATTERN = "rBRR_\\d{2}";
    public static final String AUTO_GROUPING = "rtoa:taur:rtoa_ng:rtoaRay:rBRR";


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

    @Parameter(defaultValue = "false", label = "Add Rayleig optical thickness bands")
    private boolean isTaurSelected;


    @Parameter(defaultValue = "false", label = "Add bottom of Rayleigh reflectance bands")
    private boolean isRBrr;

    @Parameter(defaultValue = "false", label = "Add gaseous absorption corrected TOA reflectances bands")
    private boolean isRtoa_ngSelected;

    @Parameter(defaultValue = "false", label = "Add TOA reflectances bands")
    private boolean isRtoaSelected;

    @Parameter(defaultValue = "false", label = "Add air mass")
    private boolean isAirMass;


    private RayleighCorrAlgorithm algorithm;
    private Sensor sensor;
    private double[] absorpOzone;
    private double[] crossSectionSigma;

    @Override
    public void initialize() throws OperatorException {
        Product targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        sensor = getSensorPattern();
        algorithm = new RayleighCorrAlgorithm();
        GaseousAbsorptionAuxII gaseousAbsorptionAuxII = new GaseousAbsorptionAuxII();
        absorpOzone = gaseousAbsorptionAuxII.absorptionOzone(sensor.toString());
        AuxiliaryValues.initDefaultAuxiliary();

        double[] waveLength = getAllWavelengths(getSourceProduct(), sensor.getNumBands(), sensor.getGetPattern());
        crossSectionSigma = algorithm.getCrossSectionSigma(waveLength);

        initBandGroup(targetProduct);
        ProductUtils.copyBand(ALTITUDE, sourceProduct, targetProduct, true);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
        targetProduct.setAutoGrouping(AUTO_GROUPING);
        setTargetProduct(targetProduct);
    }

    public double[] getAllWavelengths(Product sourceProduct, int numBands, String getPattern) {
        double[] waveLenght = new double[numBands];
        for (int i = 0; i < numBands; i++) {
            waveLenght[i] = sourceProduct.getBand(String.format(getPattern, i+1)).getSpectralWavelength();
        }
        return waveLenght;
    }

    private void initBandGroup(Product targetProduct) {
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
        createBandGroup(targetProduct, bandsToInit);
    }

    private void createBandGroup(Product targetProduct, List<String> bandCategoryList) {
        for (String bandCategory : bandCategoryList) {
            for (int i = 1; i <= sensor.getNumBands(); i++) {
                Band sourceBand = sourceProduct.getBand(String.format(sensor.getPattern, i));
                Band targetBand = targetProduct.addBand(String.format(bandCategory, i), ProductData.TYPE_FLOAT32);
                ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
            }
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        AuxiliaryValues auxiliaryValues = createAuxiliary(sensor, targetRectangle);
        Iterator<Band> iteratorBand = targetTiles.keySet().iterator();
        while (iteratorBand.hasNext()) {
            Band targetBand = iteratorBand.next();
            Tile targetTile = targetTiles.get(targetBand);
            String targetBandName = targetBand.getName();

            if (targetBandName.equals(AIRMASS) && isAirMass) {
                double[] massAirs = auxiliaryValues.getAirMass();
                targetTile.setSamples(massAirs);
                continue;
            }
            int sourceBandIndex = getSourceBandIndexAndinitAuxBand(auxiliaryValues, targetBand, targetRectangle);
            if (sourceBandIndex == -1) {
                continue;
            }
            double[] reflectance = algorithm.convertRadsToRefls(auxiliaryValues);
            double[] rayleighOpticalThickness = algorithm.getRayleighOpticalThicknessII(auxiliaryValues, crossSectionSigma[sourceBandIndex - 1]);

            if (targetBandName.matches(RTOA_PATTERN) && isRtoaSelected) {
                targetTile.setSamples(reflectance);
            } else if (targetBandName.matches(TAUR_D_PATTERN) && isTaurSelected) {
                targetTile.setSamples(rayleighOpticalThickness);
            } else if (isRBrr || isRtoa_ngSelected) {
                if (Math.ceil(auxiliaryValues.getWaveLenght()) == 709) { // band 709
                    reflectance = waterVaporCorrection709(reflectance, targetRectangle);
                }
                double[] corrOzoneRefl = algorithm.getCorrOzone(auxiliaryValues, reflectance, absorpOzone[sourceBandIndex - 1]);
                if (targetBandName.matches(RTOA_NG_D_PATTERN) && isRtoa_ngSelected) {
                    targetTile.setSamples(corrOzoneRefl);
                    continue;
                }

                if (targetBandName.matches(R_BRR_D_PATTERN) && isRBrr) {
                    double[] rhoBrr = algorithm.getRhoBrr(auxiliaryValues, rayleighOpticalThickness, corrOzoneRefl, sourceBandIndex);
                    targetTile.setSamples(rhoBrr);
                }
            }
        }
    }

    private int getSourceBandIndexAndinitAuxBand(AuxiliaryValues auxiliaryValues, Band targetBandName, Rectangle rectangle) {
        int sourceBandRefIndex = getSourceBandIndex(targetBandName.getName());
        if (sourceBandRefIndex == -1) {
            return -1;
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
        return sourceBandRefIndex;
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
        AuxiliaryValues auxiliaryValues = new AuxiliaryValues();
        if (sensor.equals(Sensor.MERIS)) {
            auxiliaryValues.setSunAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(MERIS_SUN_AZIMUTH), rectangle));
            auxiliaryValues.setSunZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(MERIS_SUN_ZENITH), rectangle));
            auxiliaryValues.setViewZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(MERIS_VIEW_ZENITH), rectangle));
            auxiliaryValues.setViewAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(MERIS_VIEW_AZIMUTH), rectangle));
            auxiliaryValues.setSeaLevels(getSourceTile(sourceProduct.getTiePointGrid(MERIS_ATM_PRESS), rectangle));
            auxiliaryValues.setTotalOzones(getSourceTile(sourceProduct.getTiePointGrid(MERIS_OZONE), rectangle));
            auxiliaryValues.setLatitudes(getSourceTile(sourceProduct.getTiePointGrid(MERIS_LATITUDE), rectangle));
            auxiliaryValues.setLongitude(getSourceTile(sourceProduct.getTiePointGrid(MERIS_LONGITUDE), rectangle));
            auxiliaryValues.setAltitudes();


        } else if (sensor.equals(Sensor.OLCI)) {
            auxiliaryValues.setSunZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(SZA), rectangle));
            auxiliaryValues.setViewZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(OZA), rectangle));
            auxiliaryValues.setSunAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(SAA), rectangle));
            auxiliaryValues.setViewAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(OAA), rectangle));
            auxiliaryValues.setSeaLevels(getSourceTile(sourceProduct.getTiePointGrid(SEA_LEVEL_PRESSURE), rectangle));
            auxiliaryValues.setTotalOzones(getSourceTile(sourceProduct.getTiePointGrid(TOTAL_OZONE), rectangle));
            auxiliaryValues.setLatitudes(getSourceTile(sourceProduct.getTiePointGrid(TP_LATITUDE), rectangle));
            auxiliaryValues.setLongitude(getSourceTile(sourceProduct.getTiePointGrid(TP_LONGITUDE), rectangle));
            auxiliaryValues.setAltitudes(getSourceTile(sourceProduct.getBand(ALTITUDE), rectangle));
        }

        if (isAirMass || isRBrr) {
            auxiliaryValues.setAirMass();
        }

        if (isRBrr) {
            auxiliaryValues.setAziDifferent();
            auxiliaryValues.setFourier();
            auxiliaryValues.setInterpolation();
        }
        return auxiliaryValues;
    }

    private double[] waterVaporCorrection709(double[] reflectances, Rectangle targetRectangle) {
        String bandNamePattern = sensor.getGetPattern();
        int[] upperLowerBounds = sensor.getUpperLowerBounds();
        double[] bWVRefTile = AuxiliaryValues.getSampleDoubles(getSourceTile(sourceProduct.getBand(String.format(bandNamePattern, upperLowerBounds[0])), targetRectangle));
        double[] bWVTile = AuxiliaryValues.getSampleDoubles(getSourceTile(sourceProduct.getBand(String.format(bandNamePattern, upperLowerBounds[1])), targetRectangle));
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
        return reflectances;
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
