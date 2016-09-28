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


package org.esa.s3tbx.fu;


import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.SampleCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.pointop.PixelOperator;
import org.esa.snap.core.gpf.pointop.ProductConfigurer;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.converters.BooleanExpressionConverter;

import java.awt.Color;
import java.util.ArrayList;

/**
 * The {@code FuOp} performs a MERIS, MODIS, OLCI and SeaWiFS based ocean colour classification
 * with the discrete Forel-Ule scale.
 *
 * @author Muhammad Bala
 * @author Marco Peters
 */
@OperatorMetadata(
        alias = "FuClassification",
        version = "1.0",
        category = "Optical/Thematic Water Processing",
        description = "Colour classification based on the discrete Forel-Ule scale.",
        authors = " H.J van der Woerd (IVM), M.R. Wernand (NIOZ), Muhammad Bala (BC), Marco Peters (BC)",
        copyright = "(c) 2016 by Brockmann Consult GmbH")
public class FuOp extends PixelOperator {

    private static final int MAX_DELTA_WAVELENGTH = 3;



    static Color[] FU_COLORS = new Color[]{
            new Color(0, 0, 0),
            new Color(33, 88, 188),
            new Color(49, 109, 197),
            new Color(50, 124, 187),
            new Color(75, 128, 160),
            new Color(86, 143, 150),
            new Color(109, 146, 152),
            new Color(105, 140, 134),
            new Color(117, 158, 114),
            new Color(123, 166, 84),
            new Color(125, 174, 56),
            new Color(149, 182, 69),
            new Color(148, 182, 96),
            new Color(165, 188, 118),
            new Color(170, 184, 109),
            new Color(173, 181, 95),
            new Color(168, 169, 101),
            new Color(174, 159, 92),
            new Color(179, 160, 83),
            new Color(175, 138, 68),
            new Color(164, 105, 5),
            new Color(161, 77, 4)
    };

    private String[] reflecBandNames;

    @SourceProduct(alias = "source", label = "Reflectance", description = "The source product providing reflectances.")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "false",
            description = "Weather or not to copy all the bands to the target product from the source product.")
    private boolean copyAllSourceBands;

    @Parameter(label = "Input is irradiance reflectance", defaultValue = "false",
            description = "If enabled, the source reflectances will be converted to radiance reflectances by dividing it by PI before passing to the algorithm.")
    private boolean inputIsIrradianceReflectance;

    @Parameter(label = "Valid pixel expression", description = "An expression to filter which pixel are considered.",
            converter = BooleanExpressionConverter.class)
    private String validExpression;

    @Parameter(defaultValue = "AUTO_DETECT", description = "The instrument to compute FU for.")
    private Instrument instrument;

    @Parameter(label = "Include intermediate results in output", defaultValue = "true",
            description = "Whether or not the intermediate results shall be written to the target output")
    private boolean includeIntermediateResults;

    private FuAlgo fuAlgo;
    private boolean autoDetectedInstrument = false;
    private BandDefinition[] targetBandDefs;


    /**
     * Computes the target samples from the given source samples.
     * <p/>
     * The number of source/target samples is the maximum defined sample index plus one. Source/target samples are defined
     * by using the respective sample configurer in the
     * {@link #configureSourceSamples(SourceSampleConfigurer) configureSourceSamples} and
     * {@link #configureTargetSamples(TargetSampleConfigurer) configureTargetSamples} methods.
     * Attempts to read from source samples or write to target samples at undefined sample indices will
     * cause undefined behaviour.
     *
     * @param x             The current pixel's X coordinate.
     * @param y             The current pixel's Y coordinate.
     * @param sourceSamples The source samples (= source pixel).
     * @param targetSamples The target samples (= target pixel).
     */
    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        boolean isValid = true;
        for (Sample sourceSample : sourceSamples) {
            if (!sourceSample.getNode().isPixelValid(x, y)) {
                isValid = false;
                break;
            }
        }

        if (isValid) {
            final double spectrum[] = getInputSpectrum(sourceSamples);
            FuResult result = fuAlgo.compute(spectrum);
            for (int i = 0; i < targetBandDefs.length; i++) {
                BandDefinition targetBandDef = targetBandDefs[i];
                targetBandDef.setTargetSample(result, targetSamples[i]);
            }
        } else {
            for (int i = 0; i < targetSamples.length; i++) {
                targetBandDefs[i].setNoDataValue(targetSamples[i]);
            }
        }
    }

    private double[] getInputSpectrum(Sample[] sourceSamples) {
        double[] spectrum = new double[sourceSamples.length];
        if ((autoDetectedInstrument && instrument.isIrradiance()) || inputIsIrradianceReflectance) {
            for (int i = 0; i < sourceSamples.length; i++) {
                spectrum[i] = sourceSamples[i].getDouble() / Math.PI;
            }
        } else {
            for (int i = 0; i < sourceSamples.length; i++) {
                spectrum[i] = sourceSamples[i].getDouble();
            }
        }

        return spectrum;
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
        final Product sourceProduct = getSourceProduct();
        if (instrument == Instrument.AUTO_DETECT) {
            instrument = DetectInstrument.getInstrument(sourceProduct);

            if (instrument == null) {
                throw new OperatorException("The instrument can not be automatically detected, " +
                                                    "please select the instrument in the processing parameter.");
            }

            autoDetectedInstrument = true;
        }
        fuAlgo = new FuAlgoFactory(instrument).create();
        reflecBandNames = findWaveBand(sourceProduct, this.instrument.getWavelengths(), MAX_DELTA_WAVELENGTH);
        final int bandNum = reflecBandNames.length;
        if (bandNum != instrument.getWavelengths().length) {
            throw new OperatorException("Could not find all necessary wavelengths for processing the instrument " + instrument.name() + ".");
        }

        targetBandDefs = BandDefinition.create(includeIntermediateResults, instrument);
    }

    /**
     * Configures the target product via the given {@link ProductConfigurer}. Called by {@link #initialize()}.
     * <p/>
     * Client implementations of this method usually add product components to the given target product, such as
     * {@link Band bands} to be computed by this operator,
     * {@link VirtualBand virtual bands},
     * {@link Mask masks}
     * or {@link SampleCoding sample codings}.
     * <p/>
     * The default implementation retrieves the (first) source product and copies to the target product
     * <ul>
     * <li>the start and stop time by calling {@link ProductConfigurer#copyTimeCoding()},</li>
     * <li>all tie-point grids by calling {@link ProductConfigurer#copyTiePointGrids(String...)},</li>
     * <li>the geo-coding by calling {@link ProductConfigurer#copyGeoCoding()}.</li>
     * </ul>
     * <p/>
     * Clients that require a similar behaviour in their operator shall first call the {@code super} method
     * in their implementation.
     *
     * @param productConfigurer The target product configurer.
     * @throws OperatorException If the target product cannot be configured.
     * @see Product#addBand(Band)
     * @see Product#addBand(String, String)
     * @see Product#addTiePointGrid(TiePointGrid)
     * @see Product#getMaskGroup()
     */
    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        targetProduct = productConfigurer.getTargetProduct();
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyVectorData(sourceProduct, targetProduct);
        if (copyAllSourceBands) {
            for (Band band : sourceProduct.getBands()) {
                if (!targetProduct.containsBand(band.getName())) {
                    ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, true);
                }
            }
            targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());
        }
        ProductUtils.copyMasks(sourceProduct, targetProduct);

        for (BandDefinition targetBandDef : targetBandDefs) {
            targetBandDef.addToProduct(targetProduct);
        }

        Band fuBand = targetProduct.getBand(targetBandDefs[targetBandDefs.length - 1].name);
        attachIndexCoding(fuBand);
    }

    /**
     * Configures all source samples that this operator requires for the computation of target samples.
     * Source sample are defined by using the provided {@link SourceSampleConfigurer}.
     * <p/>
     * <p/> The method is called by {@link #initialize()}.
     *
     * @param sourceSampleConfigurer The configurer that defines the layout of a pixel.
     * @throws OperatorException If the source samples cannot be configured.
     */
    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sourceSampleConfigurer) throws OperatorException {
        for (int i = 0; i < reflecBandNames.length; i++) {
            sourceSampleConfigurer.defineSample(i, reflecBandNames[i]);
        }
        if (StringUtils.isNotNullAndNotEmpty(validExpression)) {
            sourceSampleConfigurer.setValidPixelMask(validExpression);
        } else {
            final String[] validExpressions = instrument.getValidExpression();
            for (String expression : validExpressions) {
                boolean isCompatible = sourceProduct.isCompatibleBandArithmeticExpression(expression);
                if (isCompatible) {
                    sourceSampleConfigurer.setValidPixelMask(expression);
                }
            }
        }
    }

    /**
     * Configures all target samples computed by this operator.
     * Target samples are defined by using the provided {@link TargetSampleConfigurer}.
     * <p/>
     * <p/> The method is called by {@link #initialize()}.
     *
     * @param sampleConfigurer The configurer that defines the layout of a pixel.
     * @throws OperatorException If the target samples cannot be configured.
     */
    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
        for (int i = 0; i < targetBandDefs.length; i++) {
            BandDefinition targetBandDef = targetBandDefs[i];
            sampleConfigurer.defineSample(i, targetBandDef.name);
        }
    }

    static String[] findWaveBand(Product product, double centralWavelengths[], double maxDeltaWavelength) {
        final Band[] bands = product.getBands();
        final ArrayList<String> band_Names = new ArrayList<>();
        for (double centralWl : centralWavelengths) {
            String name = null;
            double minDelta = Double.MAX_VALUE;
            for (Band band : bands) {
                double bandWavelength = band.getSpectralWavelength();
                if (bandWavelength > 0.0) {
                    double delta = Math.abs(bandWavelength - centralWl);
                    if (delta < minDelta && delta <= maxDeltaWavelength) {
                        name = band.getName();
                        minDelta = delta;
                    }
                }
            }
            if (name != null) {
                band_Names.add(name);
            }
        }
        return band_Names.toArray(new String[0]);
    }

    static void attachIndexCoding(Band fuBand) {
        IndexCoding indexCoding = new IndexCoding("Forel-Ule Scale");
        ImageInfo imageInfo = createImageInfo(indexCoding);
        fuBand.setImageInfo(imageInfo);
        fuBand.getProduct().getIndexCodingGroup().add(indexCoding);
        fuBand.setSampleCoding(indexCoding);
    }

    static ImageInfo createImageInfo(IndexCoding indexCoding) {
        ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[FuAlgo.MAX_FU_VALUE + 1];
        for (int i = 0; i < points.length; i++) {
            String name = i != 0 ? String.format("FU_%2d", i) : "undefined";
            indexCoding.addIndex(name, i, "");
            points[i] = new ColorPaletteDef.Point(i, FU_COLORS[i], name);
        }
        return new ImageInfo(new ColorPaletteDef(points, points.length));
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(FuOp.class);
        }
    }
}
