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
import org.esa.snap.core.util.math.RsMathUtils;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

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
    public static final String[] BAND_CATEGORIES = new String[]{"RayleighSimple_%02d", "rtoa_%02d", "taurS_%02d", "taur_%02d", "rtoa_ng_%02d",
            "transSRay_%02d", "transVRay_%02d", "sARay_%02d", "rtoaRay_%02d", "rBRR_%02d",
            "sphericalAlbedoFactor_%02d"};
    double[] H2O_COR_POLY = new double[]{0.3832989, 1.6527957, -1.5635101, 0.5311913};  // Polynomial coefficients for WV transmission @ 709nm

    public static final String OLCI = "OLCI";
    @SourceProduct
    Product sourceProduct;

    private Product targetProduct;
    private RayleighCorrAlgorithm algorithm;
    private double[] taur_std;
    private String[] bandAndTiepoint = new String[]{"SAA", "SZA", "OZA", "OAA", "altitude", "total_ozone",
            "TP_latitude", "TP_longitude", "sea_level_pressure"};
    private double[] absorpOzone;


    @Override
    public void initialize() throws OperatorException {
        final Band[] sourceBands = sourceProduct.getBands();
        algorithm = new RayleighCorrAlgorithm();
        GaseousAbsorptionAuxII gaseousAbsorptionAuxII = new GaseousAbsorptionAuxII();
        absorpOzone = gaseousAbsorptionAuxII.absorptionOzone(OLCI);

        taur_std = getRots(sourceBands);
        checkRequireBandTiePont(bandAndTiepoint);
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        for (String bandCategory : BAND_CATEGORIES) {
            for (int i = 1; i <= 21; i++) {
                Band sourceBand = sourceProduct.getBand(String.format("Oa%02d_radiance", i));
                Band targetBand = targetProduct.addBand(String.format(bandCategory, i), ProductData.TYPE_FLOAT32);
                ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
            }
        }

        targetProduct.addBand("airmass", ProductData.TYPE_FLOAT32);
        targetProduct.addBand("azidiff", ProductData.TYPE_FLOAT32);
        targetProduct.addBand("altitude", ProductData.TYPE_FLOAT32);

       /* Band raycorFlagBand = targetProduct.addBand("raycor_flags", ProductData.TYPE_FLOAT32);
        FlagCoding raycorFlags = new FlagCoding("raycor_flags");
        raycorFlags.addFlag("testflag_1", 1, "Flag 1 for Rayleigh Correction");
        raycorFlags.addFlag("testflag_2", 2, "Flag 2 for Rayleigh Correction");
        raycorFlagBand.setSampleCoding(raycorFlags);*/


        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
        targetProduct.setAutoGrouping("RayleighSimple:taurS:rtoa:taur:transSRay:rtoa_ng:transVRay:sARay:rtoaRay:rBRR:sphericalAlbedoFactor");
        setTargetProduct(targetProduct);
    }


    private void checkRequireBandTiePont(String[] bandTiepoints) {
        for (final String bandTiepoint : bandTiepoints) {
            if (!sourceProduct.containsRasterDataNode(bandTiepoint)) {
                throw new OperatorException("The required raster '" + bandTiepoint + "' is not in the product.");
            }
        }
    }

    private double[] getRots(Band[] sourceBands) {
        final double[] waveLenght = new double[sourceBands.length];
        for (int i = 0; i < sourceBands.length; i++) {
            waveLenght[i] = sourceBands[i].getSpectralWavelength();
        }
        return algorithm.getTaurStd(waveLenght);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Rectangle rectangle = targetTile.getRectangle();
        String targetBandName = targetBand.getName();

        String sourceProductBandName = getSourceBand(targetBand.getSpectralWavelength());
        String extractBandIndex = sourceProductBandName.substring(2, 4);
        int sourceBandIndex = Integer.parseInt(extractBandIndex) - 1;


        double[] sunZenithAngle = getSourceTile(sourceProduct.getTiePointGrid("SZA"), rectangle).getSamplesDouble();
        double[] viewZenithAngle = getSourceTile(sourceProduct.getTiePointGrid("OZA"), rectangle).getSamplesDouble();
        double[] sunAzimuthAngle = getSourceTile(sourceProduct.getTiePointGrid("SAA"), rectangle).getSamplesDouble();
        double[] viewAzimuthAngle = getSourceTile(sourceProduct.getTiePointGrid("OAA"), rectangle).getSamplesDouble();
        double[] altitude = getSourceTile(sourceProduct.getBand("altitude"), rectangle).getSamplesDouble();
        double[] seaLevel = getSourceTile(sourceProduct.getTiePointGrid("sea_level_pressure"), rectangle).getSamplesDouble();
        double[] totalOzones = getSourceTile(sourceProduct.getTiePointGrid("total_ozone"), rectangle).getSamplesDouble();
        double[] tpLatitudes = getSourceTile(sourceProduct.getTiePointGrid("TP_latitude"), rectangle).getSamplesDouble();

        Band solarFlux = sourceProduct.getBand(String.format("solar_flux_band_%d", Integer.parseInt(extractBandIndex)));
        double[] solarFluxs = getSourceTile(solarFlux, rectangle).getSamplesDouble();


        double[] sunAzimuthAngleRads = SmileUtils.convertDegreesToRadians(sunAzimuthAngle);
        double[] viewAzimuthAngleRads = SmileUtils.convertDegreesToRadians(viewAzimuthAngle);
        double[] sunZenithAngleRads = SmileUtils.convertDegreesToRadians(sunZenithAngle);
        double[] viewZenithAngleRads = SmileUtils.convertDegreesToRadians(viewZenithAngle);

        double[] sourceSampleRad = getSourceTile(sourceProduct.getBand(sourceProductBandName), rectangle).getSamplesDouble();

        double[] crossSectionSigma = algorithm.getCrossSectionSigma(sourceSampleRad);
        double[] rayleighOpticalThickness = algorithm.getRayleighOpticalThicknessII(seaLevel, altitude, tpLatitudes, crossSectionSigma);

        boolean isRayleighSample = targetBandName.matches("RayleighSimple_\\d{2}");
        boolean isTaurS = targetBandName.matches("taurS_\\d{2}");
        boolean isRtoa = targetBandName.matches("rtoa_\\d{2}");

        if (isRayleighSample || isTaurS || isRtoa) {
            double[] radsToRefls = convertRadsToRefls(sourceSampleRad, sunZenithAngle, solarFluxs);
            double[] phaseRaylMin = algorithm.getPhaseRaylMin(sunZenithAngleRads, sunAzimuthAngle, viewZenithAngleRads, viewAzimuthAngle);
            HashMap<String, double[]> correctHashMap = algorithm.correct(radsToRefls, seaLevel, altitude, sunZenithAngleRads, viewZenithAngleRads, phaseRaylMin);

            if (isRayleighSample) {
                double[] rRaySamples = correctHashMap.get("rRaySample");
                targetTile.setSamples(rRaySamples);
            }

            if (isTaurS) {
                double[] taurSes = correctHashMap.get("taurS");
                targetTile.setSamples(taurSes);
            }

            if (isRtoa) {
                targetTile.setSamples(radsToRefls);
            }

        } else if (targetBandName.matches("taur_\\d{2}")) {
            targetTile.setSamples(rayleighOpticalThickness);
        } else {
            double[] lineSpace = algorithm.getLineSpace(0, 1, 17);
            double[] corrOzoneRefl = getCorrOzone(totalOzones, sunZenithAngleRads, viewZenithAngleRads, sourceBandIndex);
            HashMap<String, double[]> rayHashMap = algorithm.getRHO_BRR(sunZenithAngleRads, viewZenithAngleRads, sunAzimuthAngleRads, viewAzimuthAngleRads,
                    rayleighOpticalThickness, corrOzoneRefl, lineSpace);

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
                throw new IllegalArgumentException("Not implement yet");

            } else if (targetBandName.matches("rtoaRay_\\d{2}")) {
                double[] rtoaRays = rayHashMap.get("rtoaRay");
                targetTile.setSamples(rtoaRays);

            } else if (targetBandName.matches("sphericalAlbedoFactor_\\d{2}")) {
                double[] sphericalAlbedoFactors = rayHashMap.get("sphericalAlbedoFactor");
                targetTile.setSamples(sphericalAlbedoFactors);

            } else if (targetBandName.matches("rtoa_ng_\\d{2}")) {
                targetTile.setSamples(corrOzoneRefl);
            }
        }
    }

    //todo mba/** write a test
    String getSourceBand(float spectralWavelength) {
        Band[] bands = sourceProduct.getBands();
        List<Band> collectBand = Arrays.stream(bands).filter(p -> p.getSpectralWavelength() == spectralWavelength &&
                !p.getName().contains("err")).collect(Collectors.toList());
        return collectBand.get(0).getName();
    }


    //todo mba/** write test
    private double[] convertRadsToRefls(double[] radiance, double[] solarIrradiance, double[] sza) {
        double[] ref = new double[radiance.length];
        for (int i = 0; i < ref.length; i++) {
            ref[i] = RsMathUtils.radianceToReflectance((float) radiance[i], (float) sza[i], (float) solarIrradiance[i]);
        }
        return ref;
    }

    public double[] getCorrOzone(double[] ozone, double[] szaRads, double[] ozaRads, int bandIndex1) {
//        float spectralWavelength = sourceBand.getSpectralWavelength();
//        int X2 = 1;
//        double trans709 = H2O_COR_POLY[0] + (H2O_COR_POLY[1] + (H2O_COR_POLY[2] + H2O_COR_POLY[3] * X2) * X2) * X2;
//        double[] radiances = getSourceTile(sourceBand, rectangle).getSamplesDouble();
//        float reflectance = RsMathUtils.radianceToReflectance((float) radiances[i], (float) szaRads[i], (float) solarIrradiance[i]);
//        if (reflectance > 0) {
//        }
        double absorpO = absorpOzone[bandIndex1];
        double[] rho_ng = new double[ozone.length];
        for (int i = 0; i < ozone.length; i++) {
            double model_ozone = 0;

            double cts = Math.cos(szaRads[i]); //#cosine of sun zenith angle
            double ctv = Math.cos(ozaRads[i]);//#cosine of view zenith angle
            double trans_ozoned12 = Math.exp(-(absorpO * ozone[i] / 1000.0 - model_ozone) / cts);
            double trans_ozoneu12 = Math.exp(-(absorpO * ozone[i] / 1000.0 - model_ozone) / ctv);
            double trans_ozone12 = trans_ozoned12 * trans_ozoneu12;
            rho_ng[i] = trans_ozone12;
//                rho_ng /= trans_ozone12;
        }
        return rho_ng;
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(RayleighCorrectionOpII.class);
        }
    }
}
