/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s3tbx.arc;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.PixelOperator;
import org.esa.snap.core.gpf.pointop.ProductConfigurer;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.converters.BooleanExpressionConverter;
import org.esa.snap.core.util.converters.GeneralExpressionConverter;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import javax.media.jai.OpImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An operator for computing sea surface temperature from (A)ATSR products.
 *
 * @author Owen Embury
 */
@OperatorMetadata(alias = "Arc.SST",
                  category = "Optical/Thematic Water Processing",
                  authors = "Owen Embury, Marco Peters",
                  copyright = "University of Reading, Brockmann Consult GmbH",
                  version = "0.1",
                  description = "Computes sea surface temperature (SST) from (A)ATSR products.")
public class ArcSstOp extends PixelOperator {

    private Sensor sensor;

    @SuppressWarnings("unused")

    @SourceProduct(alias = "source",
                   description = "The path of the (A)ATSR source product",
                   label = "(A)ATSR source product")
    private Product sourceProduct;

    @Parameter(defaultValue = "30.0", label = "Total Column Water Vapour",
               description = "TCWV value to use in SST retrieval",
               converter = GeneralExpressionConverter.class)
    private String tcwvExpression;

    @Parameter(defaultValue = "true",
               label = ArcConstants.PROCESS_ASDI_LABELTEXT,
               description = ArcConstants.PROCESS_ASDI_DESCRIPTION)
    private boolean asdi;

    @Parameter(defaultValue = "ASDI_AATSR", label = "ASDI coefficient file",
               description = ArcConstants.ASDI_COEFF_FILE_DESCRIPTION,
               valueSet = {
                       "ASDI_ATSR1", "ASDI_ATSR2", "ASDI_AATSR",
               })
    private ArcFiles asdiCoefficientsFile;

    @Parameter(defaultValue = ArcConstants.DEFAULT_ASDI_BITMASK, label = "ASDI mask",
               description = "ROI-mask used for the ASDI",
               converter = BooleanExpressionConverter.class)
    private String asdiMaskExpression;

    @Parameter(defaultValue = "true",
               label = ArcConstants.PROCESS_DUAL_VIEW_SST_LABELTEXT,
               description = ArcConstants.PROCESS_DUAL_VIEW_SST_DESCRIPTION)
    private boolean dual;

    @Parameter(defaultValue = "ARC_D2_AATSR", label = "Dual-view coefficient file",
               description = ArcConstants.DUAL_VIEW_COEFF_FILE_DESCRIPTION,
               valueSet = {
                       "ARC_D2_ATSR1", "ARC_D2_ATSR2", "ARC_D2_AATSR", "ARC_D2_SLSTR",
                       "ARC_D3_ATSR1", "ARC_D3_ATSR2", "ARC_D3_AATSR", "ARC_D3_SLSTR"
               })
    private ArcFiles dualCoefficientsFile;

    @Parameter(defaultValue = ArcConstants.DEFAULT_DUAL_VIEW_BITMASK, label = "Dual-view mask",
               description = "ROI-mask used for the dual-view SST",
               converter = BooleanExpressionConverter.class)
    private String dualMaskExpression;

    @Parameter(defaultValue = "true", label = ArcConstants.PROCESS_NADIR_VIEW_SST_LABELTEXT,
               description = ArcConstants.PROCESS_NADIR_VIEW_SST_DESCRIPTION)
    private boolean nadir;

    @Parameter(defaultValue = "ARC_N2_AATSR", label = "Nadir-view coefficient file",
               description = ArcConstants.NADIR_VIEW_COEFF_FILE_DESCRIPTION,
               valueSet = {
                       "ARC_N2_ATSR1", "ARC_N2_ATSR2", "ARC_N2_AATSR", "ARC_N2_SLSTR",
                       "ARC_N3_ATSR1", "ARC_N3_ATSR2", "ARC_N3_AATSR", "ARC_N3_SLSTR"
               })
    private ArcFiles nadirCoefficientsFile;

    @Parameter(defaultValue = ArcConstants.DEFAULT_NADIR_VIEW_BITMASK, label = "Nadir-view mask",
               description = "ROI-mask used for the nadir-view SST",
               converter = BooleanExpressionConverter.class)
    private String nadirMaskExpression;

    @Parameter(defaultValue = "-999.0f", label = "Invalid SST value",
               description = "Value used to fill invalid SST pixels")
    private float invalidSstValue;

    private transient ArcCoefficients coeff1;
    private transient ArcCoefficients coeff2;
    private transient ArcCoefficients coeff3;

    private transient int nadirMaskIndex;
    private transient int dualMaskIndex;
    private transient int asdiMaskIndex;

    private transient int currentPixel = 0;

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        checkCancellation();
        double secnad, secfwd;
        if (sensor.isAtsr()) {
            secnad = 1.0 / Math.cos(Math.toRadians(90 - sourceSamples[6].getFloat()));
            secfwd = 1.0 / Math.cos(Math.toRadians(90 - sourceSamples[7].getFloat()));
        } else {
            secnad = 1.0 / Math.cos(Math.toRadians(sourceSamples[6].getFloat()));
            secfwd = 1.0 / Math.cos(Math.toRadians(sourceSamples[7].getFloat()));
        }

        final float ir37N = sourceSamples[0].getFloat();
        final float ir11N = sourceSamples[1].getFloat();
        final float ir12N = sourceSamples[2].getFloat();
        final float ir37F = sourceSamples[3].getFloat();
        final float ir11F = sourceSamples[4].getFloat();
        final float ir12F = sourceSamples[5].getFloat();
        final float ntcwv = sourceSamples[8].getFloat();

        if (nadir) {
            if (nadirMaskIndex >= 0 && !sourceSamples[nadirMaskIndex].getBoolean()) {
                targetSamples[0].set(invalidSstValue);
            } else if (ir11N < 260.0 || ir12N < 260.0) {
                targetSamples[0].set(invalidSstValue);
            } else {
                final double coeff[] = coeff1.get_Coeffs().getValues(ntcwv, 1.75, secnad);
                final double nadirSst = coeff[0]*ir37N + coeff[1]*ir11N + coeff[2]*ir12N +
                                        coeff[6];

                targetSamples[0].set(nadirSst);
            }
        }
        if (dual) {
            if (dualMaskIndex >= 0 && !sourceSamples[dualMaskIndex].getBoolean()) {
                targetSamples[1].set(invalidSstValue);
            } else if (ir11N < 260.0 || ir12N < 260.0 || ir11F < 260.0 || ir12F < 260.0) {
                targetSamples[1].set(invalidSstValue);
            } else {
                final double coeff[] = coeff2.get_Coeffs().getValues(ntcwv, secfwd, secnad);
                final double dualSst = coeff[0]*ir37N + coeff[1]*ir11N + coeff[2]*ir12N +
                                       coeff[3]*ir37F + coeff[4]*ir11F + coeff[5]*ir12F +
                                       coeff[6];

                targetSamples[1].set(dualSst);
            }
        }
        if (asdi) {
            if (asdiMaskIndex >= 0 && !sourceSamples[asdiMaskIndex].getBoolean()) {
                targetSamples[2].set(invalidSstValue);
            } else if (ir11N < 100.0 || ir12N < 100.0 || ir11F < 100.0 || ir12F < 100.0) {
                targetSamples[2].set(invalidSstValue);
            } else {
                final double coeff[] = coeff3.get_Coeffs().getValues(ntcwv, secfwd, secnad);
                final double asdi = coeff[0] * ir37N + coeff[1] * ir11N + coeff[2] * ir12N +
                                    coeff[3] * ir37F + coeff[4] * ir11F + coeff[5] * ir12F +
                                    coeff[6];
                targetSamples[2].set(asdi);
            }
        }
    }

    private void checkCancellation() {
        if (currentPixel % 1000 == 0) {
            checkForCancellation();
            currentPixel = 0;
        }
        currentPixel++;
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sc) throws OperatorException {
        String[] sourceRasterNames = sensor.getRasterNames();
        for (int i = 0; i < sourceRasterNames.length; i++) {
            sc.defineSample(i, sourceRasterNames[i]);
        }
        if (sensor.isAtsr()) {
            sc.defineComputedSample(sourceRasterNames.length, ProductData.TYPE_FLOAT32, tcwvExpression);
        }

        nadirMaskIndex = -1;
        if (nadirMaskExpression != null && !nadirMaskExpression.trim().isEmpty()) {
            nadirMaskIndex = 10;
            sc.defineComputedSample(nadirMaskIndex, ProductData.TYPE_UINT8, nadirMaskExpression);
        }

        dualMaskIndex = -1;
        if (dualMaskExpression != null && !dualMaskExpression.trim().isEmpty()) {
            dualMaskIndex = 11;
            sc.defineComputedSample(dualMaskIndex, ProductData.TYPE_UINT8, dualMaskExpression);
        }
        asdiMaskIndex = -1;
        if (asdiMaskExpression != null && !asdiMaskExpression.trim().isEmpty()) {
            asdiMaskIndex = 12;
            sc.defineComputedSample(asdiMaskIndex, ProductData.TYPE_UINT8, asdiMaskExpression);
        }
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sc) throws OperatorException {
        if (nadir) {
            sc.defineSample(0, coeff1.getName());
        }
        if (dual) {
            sc.defineSample(1, coeff2.getName());
        }
        if (asdi) {
            sc.defineSample(2, coeff3.getName());
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        if (nadir) {
            createBand(productConfigurer, coeff1);
        }
        if (dual) {
            createBand(productConfigurer, coeff2);
        }
        if (asdi) {
            createBand(productConfigurer, coeff3);
        }
   }

    private void createBand(ProductConfigurer productConfigurer, ArcCoefficients coeff) {
        final Band nadirSstBand = productConfigurer.addBand(coeff.getName(), ProductData.TYPE_FLOAT32);
        nadirSstBand.setUnit(ArcConstants.OUT_BAND_UNIT);
        nadirSstBand.setDescription(coeff.getDescription());
        nadirSstBand.setGeophysicalNoDataValue(invalidSstValue);
        nadirSstBand.setNoDataValueUsed(true);
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
        sensor = getSensor();

        final File auxdataDir = installAuxiliaryData();
        initNadirCoefficients(auxdataDir);
    }


    private void initNadirCoefficients(File auxdataDir) throws OperatorException {
        final ArcCoefficientLoader loader = new ArcCoefficientLoader();
        try {
            if (nadir) coeff1 = loader.load(new File(auxdataDir, nadirCoefficientsFile.getFilename()).toURI().toURL());
            if (dual) coeff2 = loader.load(new File(auxdataDir, dualCoefficientsFile.getFilename()).toURI().toURL());
            if (asdi) coeff3 = loader.load(new File(auxdataDir, asdiCoefficientsFile.getFilename()).toURI().toURL());
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    private File installAuxiliaryData() {
        Path auxDataDir = SystemUtils.getAuxDataPath().resolve("arc").toAbsolutePath();

        Path sourcePath = ResourceInstaller.findModuleCodeBasePath(getClass()).resolve("auxdata/arc");
        ResourceInstaller installer = new ResourceInstaller(sourcePath, auxDataDir);
        try {
            installer.install(".*", ProgressMonitor.NULL);
        } catch (IOException e) {
            throw new OperatorException(e);
        }

        return auxDataDir.toFile();
    }

    private static boolean isMasked(final OpImage maskOpImage, final int x, final int y) {
        if (maskOpImage == null) {
            return true;
        }
        final int tileX = maskOpImage.XToTileX(x);
        final int tileY = maskOpImage.YToTileY(y);
        final Raster tile = maskOpImage.getTile(tileX, tileY);

        return tile.getSample(x, y, 0) != 0;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ArcSstOp.class);
        }
    }

    private Sensor getSensor() {
        Stream<RasterDataNode> nodeStream = getSourceProduct().getRasterDataNodes().stream();
        List<String> rasterNames = nodeStream.map(ProductNode::getName).collect(Collectors.toList());

        if (rasterNames.containsAll(Arrays.asList(Sensor.AATSR.getRasterNames()))) {
            return Sensor.AATSR;
        }

        if (rasterNames.containsAll(Arrays.asList(Sensor.SLSTR.getRasterNames()))) {
            return Sensor.SLSTR;
        }
        throw new OperatorException("The operator can't be applied on the sensor");
    }


    private enum Sensor {
        AATSR(ArcConstants.SOURCE_RASTER_NAMES_AATSR, true),
        SLSTR(ArcConstants.SOURCE_RASTER_NAMES_SLSTR, false);

        private final String[] bandNames;
        private final boolean atsr;

        public String[] getRasterNames() {
            return bandNames;
        }

        public boolean isAtsr() { return atsr;}

        Sensor(String[] bandNames, boolean atsr) {
            this.bandNames = bandNames;
            this.atsr = atsr;
        }
    }
}
