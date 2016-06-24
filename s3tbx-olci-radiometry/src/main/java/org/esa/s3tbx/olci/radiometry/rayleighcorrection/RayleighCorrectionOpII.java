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
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;

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
    //rtoa:taur:rRay:transSRay:transVRay:sARay:rtoaRay:rBRR:sphericalAlbedoFactor:rtoa_ng
    public static final String[] BAND_CATEGORIES = new String[]{"refl_ray_%02d", "rtoa_%02d", "taur_%02d",
            "transSRay_%02d", "transVRay_%02d", "sARay_%02d", "rtoaRay_%02d", "rBRR_%02d",
            "sphericalAlbedoFactor_%02d"};
    @SourceProduct
    Product sourceProduct;

    private Product targetProduct;
    private RayleighCorrAlgorithm algorithm;
    private double[] taur_std;
    private String[] bandAndTiepoint = new String[]{"SAA", "SZA", "OZA", "OAA", "altitude", "total_ozone",
            "TP_latitude", "TP_longitude", "sea_level_pressure"};


    @Override
    public void initialize() throws OperatorException {
        final Band[] sourceBands = sourceProduct.getBands();
        algorithm = new RayleighCorrAlgorithm();
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
        targetProduct.setAutoGrouping("refl_ray:rtoa:taur:transSRay:transVRay:sARay:rtoaRay:rBRR:sphericalAlbedoFactor");
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

        float[] sunZenithAngle = getSourceTile(sourceProduct.getTiePointGrid("SZA"), rectangle).getSamplesFloat();
        float[] viewZenithAngle = getSourceTile(sourceProduct.getTiePointGrid("OZA"), rectangle).getSamplesFloat();
        float[] sunAzimuthAngle = getSourceTile(sourceProduct.getTiePointGrid("SAA"), rectangle).getSamplesFloat();
        float[] viewAzimuthAngle = getSourceTile(sourceProduct.getTiePointGrid("OAA"), rectangle).getSamplesFloat();
        float[] altitude = getSourceTile(sourceProduct.getBand("altitude"), rectangle).getSamplesFloat();
        float[] seaLevel = getSourceTile(sourceProduct.getTiePointGrid("sea_level_pressure"), rectangle).getSamplesFloat();
        float[] totalOzones = getSourceTile(sourceProduct.getTiePointGrid("total_ozone"), rectangle).getSamplesFloat();
        float[] tpLatitudes = getSourceTile(sourceProduct.getTiePointGrid("TP_latitude"), rectangle).getSamplesFloat();
        float[] tpLongitudes = getSourceTile(sourceProduct.getTiePointGrid("TP_longitude"), rectangle).getSamplesFloat();

        float[] sourceSampleFloat = getSourceTile(sourceProduct.getBand(targetBand.getName()), rectangle).getSamplesFloat();

        float[] crossSectionSigma = algorithm.getCrossSectionSigma(sourceSampleFloat);
        float[] corrOzone = algorithm.getCorrOzone(totalOzones);
        float[] rayleighOpticalThickness = algorithm.getRayleighOpticalThickness(seaLevel, altitude, tpLatitudes, crossSectionSigma);
        float[] rayleighPhaseMin = algorithm.getRayleighPhaseMin(sunZenithAngle, viewZenithAngle, sunAzimuthAngle, viewAzimuthAngle, rayleighOpticalThickness);

        float[] correct = algorithm.correct(sourceSampleFloat, seaLevel, altitude, sunZenithAngle, viewZenithAngle, rayleighPhaseMin);
        targetTile.setSamples(correct);
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(RayleighCorrectionOpII.class);
        }
    }
}
