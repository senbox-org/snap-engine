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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
        authors = " Marco Peters ,Muhammad Bala (Brockmann Consult)",
        copyright = "(c) 2015 by Brockmann Consult",
        category = "Optical/Pre-Processing",
        version = "1.2")
public class RayleighCorrectionOpII extends Operator {
//    public static final String[] BAND_CATEGORIES = new String[]{"RayleighSimple_%02d", "rtoa_%02d", "taurS_%02d", "taur_%02d", "rtoa_ng_%02d",
//            "transSRay_%02d", "transVRay_%02d", "sARay_%02d", "rtoaRay_%02d", "rBRR_%02d","sphericalAlbedoFactor_%02d"};

    public static final String[] BAND_CATEGORIES = new String[]{
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


    public static final String OLCI = "OLCI";
    @SourceProduct
    Product sourceProduct;

    private RayleighCorrAlgorithm algorithm;
    GaseousAbsorptionAuxII gaseousAbsorptionAuxII;
    private String[] bandNotToCompute = new String[]{"SAA", "SZA", "OZA", "OAA", "altitude", "total_ozone", "TP_latitude", "TP_longitude", "latitude", "longitude", "sea_level_pressure"};
    private Sensor sensor;
    private double[] absorpOzone;

    @Override
    public void initialize() throws OperatorException {

        Product targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        sensor = getSensorPattern();
        algorithm = new RayleighCorrAlgorithm();
        gaseousAbsorptionAuxII = new GaseousAbsorptionAuxII();
        absorpOzone = gaseousAbsorptionAuxII.absorptionOzone(sensor.toString());

        for (String bandCategory : BAND_CATEGORIES) {
            for (int i = 1; i <= sensor.getNumBands(); i++) {
                Band sourceBand = sourceProduct.getBand(String.format(sensor.getPattern, i));
                Band targetBand = targetProduct.addBand(String.format(bandCategory, i), ProductData.TYPE_FLOAT32);
                ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
            }
        }

        targetProduct.addBand("airmass", ProductData.TYPE_FLOAT32);
        targetProduct.addBand("azidiff", ProductData.TYPE_FLOAT32);

        ProductUtils.copyBand("altitude", sourceProduct, targetProduct, true);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
        targetProduct.setAutoGrouping("RayleighSimple:taurS:rtoa:taur:transSRay:rtoa_ng:transVRay:sARay:rtoaRay:rBRR:sphericalAlbedoFactor");
        setTargetProduct(targetProduct);
    }


    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Rectangle rectangle = targetTile.getRectangle();
        String targetBandName = targetBand.getName();

        //todo mba/*** ask Marco Peter if this is necessary
        if (Arrays.asList(bandNotToCompute).contains(targetBandName)) {
            double[] samplesDouble = getSourceTile(sourceProduct.getBand(targetBandName), rectangle).getSamplesDouble();
            targetTile.setSamples(samplesDouble);
            return;
        }

        double[] sunZenithAngles = getSourceTile(sourceProduct.getTiePointGrid("SZA"), rectangle).getSamplesDouble();
        double[] viewZenithAngles = getSourceTile(sourceProduct.getTiePointGrid("OZA"), rectangle).getSamplesDouble();
        double[] sunAzimuthAngles = getSourceTile(sourceProduct.getTiePointGrid("SAA"), rectangle).getSamplesDouble();
        double[] viewAzimuthAngles = getSourceTile(sourceProduct.getTiePointGrid("OAA"), rectangle).getSamplesDouble();
        double[] aziDifferences = SmileUtils.getAziDiff(sunAzimuthAngles, viewAzimuthAngles);
        double[] massAirs = SmileUtils.getAirMass(sunZenithAngles, viewZenithAngles);

        if (targetBandName.equals("airmass")) {
            targetTile.setSamples(massAirs);
            return;
        } else if (targetBandName.equals("azidiff")) {
            targetTile.setSamples(aziDifferences);
            return;
        }

        float spectralWavelength = targetBand.getSpectralWavelength();
        //todo mba/**** ask Marco Peter if is necessary.
        if (spectralWavelength <= 0) {
            throw new IllegalArgumentException("The band must have wavelength");
        }
        String sourceProductBandName = getSourceBand(spectralWavelength);
        String extractBandIndex = sourceProductBandName.substring(2, 4);
        int sourceBandIndex = Integer.parseInt(extractBandIndex);
        double absorpO = absorpOzone[sourceBandIndex - 1];


        double[] altitudes = getSourceTile(sourceProduct.getBand("altitude"), rectangle).getSamplesDouble();
        double[] seaLevels = getSourceTile(sourceProduct.getTiePointGrid("sea_level_pressure"), rectangle).getSamplesDouble();
        double[] totalOzones = getSourceTile(sourceProduct.getTiePointGrid("total_ozone"), rectangle).getSamplesDouble();
        double[] tpLatitudes = getSourceTile(sourceProduct.getTiePointGrid("TP_latitude"), rectangle).getSamplesDouble();
        Band solarFlux = sourceProduct.getBand(String.format("solar_flux_band_%d", sourceBandIndex));
        Band lambdaBand = sourceProduct.getBand(String.format("lambda0_band_%d", sourceBandIndex));
        double[] solarFluxs = getSourceTile(solarFlux, rectangle).getSamplesDouble();

        double[] sunAzimuthAngleRads = SmileUtils.convertDegreesToRadians(sunAzimuthAngles);
        double[] viewAzimuthAngleRads = SmileUtils.convertDegreesToRadians(viewAzimuthAngles);
        double[] sunZenithAngleRads = SmileUtils.convertDegreesToRadians(sunZenithAngles);
        double[] viewZenithAngleRads = SmileUtils.convertDegreesToRadians(viewZenithAngles);

        double[] lambdaSource = getSourceTile(lambdaBand, rectangle).getSamplesDouble();
        double[] sourceSampleRad = getSourceTile(sourceProduct.getBand(sourceProductBandName), rectangle).getSamplesDouble();
        double[] reflectances = algorithm.convertRadsToRefls(sourceSampleRad, solarFluxs, sunZenithAngles);

        double[] crossSectionSigma = algorithm.getCrossSectionSigma(lambdaSource);
        double[] rayleighOpticalThickness = algorithm.getRayleighOpticalThicknessII(seaLevels, altitudes, tpLatitudes, crossSectionSigma);

        boolean isRayleighSample = targetBandName.matches("RayleighSimple_\\d{2}");
        boolean isTaurS = targetBandName.matches("taurS_\\d{2}");

        if (isRayleighSample || isTaurS) {
            double[] phaseRaylMin = algorithm.getPhaseRaylMin(sunZenithAngleRads, sunAzimuthAngles, viewZenithAngleRads, viewAzimuthAngles);
            HashMap<String, double[]> correctHashMap = algorithm.correct(lambdaSource, seaLevels, altitudes, sunZenithAngleRads, viewZenithAngleRads, phaseRaylMin);
            if (isRayleighSample) {
                double[] rRaySamples = correctHashMap.get("rRaySample");
                targetTile.setSamples(rRaySamples);
            }

            if (isTaurS) {
                double[] taurSes = correctHashMap.get("taurS");
                targetTile.setSamples(taurSes);
            }
        } else if (targetBandName.matches("rtoa_\\d{2}")) {
            targetTile.setSamples(reflectances);
        } else if (targetBandName.matches("taur_\\d{2}")) {
            targetTile.setSamples(rayleighOpticalThickness);
        } else {
            if (Math.ceil(spectralWavelength) == 709) { // band 709
                String bandNamePattern = sensor.getGetPattern();
                int[] outSide = sensor.getOutSide();
                double[] bWVRefTile = getSourceTile(sourceProduct.getBand(String.format(bandNamePattern, outSide[0])), rectangle).getSamplesDouble();
                double[] bWVTile = getSourceTile(sourceProduct.getBand(String.format(bandNamePattern, outSide[1])), rectangle).getSamplesDouble();
                waterVaporCorrection709(reflectances, bWVRefTile, bWVTile);
            }

            double[] corrOzoneRefl = algorithm.getCorrOzone(reflectances, totalOzones, sunZenithAngleRads, viewZenithAngleRads, absorpO);
            if (targetBandName.matches("rtoa_ng_\\d{2}")) {
                targetTile.setSamples(corrOzoneRefl);
                return;
            }

            HashMap<String, double[]> rayHashMap = algorithm.getRhoBrr(sunZenithAngles, viewZenithAngles, sunZenithAngleRads, viewZenithAngleRads, sunAzimuthAngleRads, viewAzimuthAngleRads, rayleighOpticalThickness, corrOzoneRefl);
            if (targetBandName.matches("rBRR_\\d{2}")) {
                double[] rBRRs = rayHashMap.get("rBRR");
                targetTile.setSamples(rBRRs);

            } else if (targetBandName.matches("transSRay_\\d{2}")) {
                double[] transSRays = rayHashMap.get("transSRay");
                targetTile.setSamples(transSRays);

            } else if (targetBandName.matches("transVRay_\\d{2}")) {
                double[] transVRays = rayHashMap.get("transVRay");
                targetTile.setSamples(transVRays);

            } else if (targetBandName.matches("sARay_\\d{2}")) {
                double[] sARays = rayHashMap.get("sARay");
                targetTile.setSamples(sARays);

            } else if (targetBandName.matches("rtoaRay_\\d{2}")) {
                double[] rtoaRays = rayHashMap.get("rtoaRay");
                targetTile.setSamples(rtoaRays);

            } else if (targetBandName.matches("sphericalAlbedoFactor_\\d{2}")) {
                double[] sphericalAlbedoFactors = rayHashMap.get("sphericalAlbedoFactor");
                targetTile.setSamples(sphericalAlbedoFactors);
            }

/*
            List<RayleighBands> rayleighBandsList = new ArrayList<>();
            for (int y = (int) rectangle.getMinY(); y < rectangle.getMaxY(); y++) {
                for (int x = (int) rectangle.getMinX(); x < rectangle.getMaxX(); x++) {
                    double width = rectangle.getWidth();
                    int index = (int) ((width * y) + x);
                    double oza = viewZenithAngles[index];
                    double ozaRad = viewZenithAngleRads[index];
                    double sza = sunZenithAngles[index];
                    double szaRad = sunZenithAngleRads[index];
                    double correctRefl = corrOzoneRefl[index];
                    double aziDifference = aziDifferences[index];
                    double massAir = massAirs[index];
                    double rayleighOpticalThicknes = rayleighOpticalThickness[index];
                    rayleighBandsList.add(algorithm.getRhoBrr(sza, oza, szaRad, ozaRad, rayleighOpticalThicknes, correctRefl, aziDifference, massAir));
                }
            }
            writeToTarget(rayleighBandsList, targetBandName, targetTile);*/
//            System.out.println("Stop");

        }
    }

    private void writeToTarget(List<RayleighBands> rayleighBandsList, String targetBandName, Tile targetTile) {
        RayleighBandsSearch rayleighBandsSearch = new RayleighBandsSearch(rayleighBandsList);
        double[] bandSamples = rayleighBandsSearch.getBandSamples(targetBandName);
        targetTile.setSamples(bandSamples);
    }

    private void waterVaporCorrection709(double[] reflectances, double[] bWVRefTile, double[] bWVTile) {
        double[] H2O_COR_POLY = new double[]{0.3832989, 1.6527957, -1.5635101, 0.5311913};  // Polynomial coefficients for WV transmission @ 709nm
        // 0.3832989 + ( 1.6527957+ (-1.5635101+ 0.5311913*1)*1)*1
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
    String getSourceBand(float spectralWavelength) {
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
        MERIS("radiance_%02d", 15, new int[]{13, 14}),
        OLCI("Oa%02d_radiance", 21, new int[]{17, 18});

        public int[] getOutSide() {
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
