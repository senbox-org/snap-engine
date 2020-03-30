/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.landcover.gpf;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.image.RasterDataNodeSampleOpImage;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.landcover.dataio.FileLandCoverModelDescriptor;
import org.esa.snap.landcover.dataio.LandCoverFactory;
import org.esa.snap.landcover.dataio.LandCoverModel;
import org.esa.snap.landcover.dataio.LandCoverModelDescriptor;
import org.esa.snap.landcover.dataio.LandCoverModelRegistry;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * AddLandCover adds a land cover band to a product
 */

@OperatorMetadata(alias = "AddLandCover",
        category = "Raster/Masks",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2015 by Array Systems Computing Inc.",
        description = "Creates a land cover band")
public final class AddLandCoverOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The land cover model.", defaultValue = "AAFC Canada Sand Pct", label = "Land Cover Model")
    private String[] landCoverNames = {"AAFC Canada Sand Pct"};

    @Parameter(description = "The external landcover files.", defaultValue = "", label = "External Files")
    private File[] externalFiles = null;

    @Parameter(defaultValue = ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
            label = "Resampling Method")
    private String resamplingMethod = ResamplingFactory.NEAREST_NEIGHBOUR_NAME;

    private static Map<LandCoverParameters, LandCoverModelDescriptor> paramsToDescriptor;
    private static Map<LandCoverParameters, Band> paramsToBand;

    public static final String DEFAULT_BAND_NAME = "land_cover";

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.snap.core.datamodel.Product} annotated with the
     * {@link org.esa.snap.core.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.snap.core.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {
        try {
            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {
        ensureSingleRasterSize(sourceProduct);

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());
        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        for (Band srcBand : sourceProduct.getBands()) {
            if (!targetProduct.containsBand(srcBand.getName())) {
                ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, true);
            }
        }

        if(landCoverNames != null) {
            for (String landCoverName : landCoverNames) {
                final LandCoverParameters param = new LandCoverParameters(
                        landCoverName, resamplingMethod);

                addLandCover(targetProduct, param);

            }
        }
        if (externalFiles != null) {
            for(File externalFile : externalFiles) {
                if (externalFile.exists()) {

                    final LandCoverParameters param = new LandCoverParameters(
                            externalFile.getName(), externalFile, resamplingMethod);

                    addLandCover(targetProduct, param);
                }
            }
        }
    }

    public static void AddLandCover(final Product product, final AddLandCoverOp.LandCoverParameters param) throws IOException {
        addLandCover(product, param);
        initializeDescriptors();
        setLandCoverBandImages();
    }

    private static void addLandCover(final Product product, final AddLandCoverOp.LandCoverParameters param) {
        paramsToDescriptor = new HashMap<>();
        paramsToBand = new HashMap<>();
        String name = LandCoverFactory.getProperName(param.name);

        LandCoverModelDescriptor descriptor;
        if (param.externalFile != null) {
            descriptor = new FileLandCoverModelDescriptor(param.externalFile);
            paramsToDescriptor.put(param, descriptor);
        } else {
            final LandCoverModelRegistry modelRegistry = LandCoverModelRegistry.getInstance();
            descriptor = modelRegistry.getDescriptor(name);
            paramsToDescriptor.put(param, descriptor);
        }
        addLandCoverBand(product, descriptor, param);
    }

    @Override
    public void doExecute(ProgressMonitor pm) throws OperatorException {
        pm.beginTask("", 2);
        try {
            pm.setTaskName("Initializing Descriptors");
            initializeDescriptors();
            pm.worked(1);
            pm.setTaskName("Setting Land Cover Band Images");
            setLandCoverBandImages();
            pm.worked(1);
        } catch (IOException e) {
            throw new OperatorException(e.getMessage());
        } finally {
            pm.done();
        }
    }

    @Override
    public void dispose() {
        paramsToDescriptor = null;
        paramsToBand = null;
    }

    private static void initializeDescriptors() {
        for (Map.Entry<LandCoverParameters, LandCoverModelDescriptor> entry : paramsToDescriptor.entrySet()) {
            LandCoverParameters param = entry.getKey();
            if (param.externalFile != null) {
                try {
                    Product extProduct = ProductIO.readProduct(param.externalFile);

                    // integer data should only use nearest neighbour
                    if (extProduct.getBandAt(0).getDataType() < ProductData.TYPE_FLOAT32) {
                        param.resamplingMethod = ResamplingFactory.NEAREST_NEIGHBOUR_NAME;
                    }
                } catch (Exception e) {
                    SystemUtils.LOG.warning("Unable to read external file " + param.externalFile);
                }
            } else {
                LandCoverModelDescriptor descriptor = entry.getValue();
                if (descriptor == null) {
                    String name = LandCoverFactory.getProperName(param.name);
                    throw new OperatorException("The Land Cover '" + name + "' is not supported.");
                }
                if (!descriptor.isInstalled()) {
                    descriptor.installFiles();
                }
                // integer data should only use nearest neighbour
                if (descriptor.getDataType() < ProductData.TYPE_FLOAT32) {
                    param.resamplingMethod = ResamplingFactory.NEAREST_NEIGHBOUR_NAME;
                }
            }
        }
    }

    private static void setLandCoverBandImages() throws IOException {
        for (Map.Entry<LandCoverParameters, LandCoverModelDescriptor> entry : paramsToDescriptor.entrySet()) {
            LandCoverParameters param = entry.getKey();
            Resampling resampling = Resampling.NEAREST_NEIGHBOUR;
            if (param.resamplingMethod != null) {
                resampling = ResamplingFactory.createResampling(param.resamplingMethod);
                if (resampling == null) {
                    throw new OperatorException("Resampling method " + param.resamplingMethod + " is invalid");
                }
            }
            LandCoverModelDescriptor descriptor = entry.getValue();
            final LandCoverModel landcover = descriptor.createLandCoverModel(resampling);
            Band band = paramsToBand.get(param);
            band.setSourceImage(createLandCoverSourceImage(landcover, band));
        }
    }

    private static void addLandCoverBand(Product product, LandCoverModelDescriptor descriptor, LandCoverParameters param) {
        String bandName = getValidBandName(param.bandName, product);
        final Band band = product.addBand(bandName, descriptor.getDataType());
        band.setNoDataValueUsed(true);
        band.setNoDataValue(descriptor.getNoDataValue());
        band.setUnit(descriptor.getUnit());
        band.setDescription(descriptor.getName());

        final IndexCoding indexCoding = descriptor.getIndexCoding();
        if (indexCoding != null) {
            product.getIndexCodingGroup().add(indexCoding);
            band.setSampleCoding(indexCoding);
            band.setImageInfo(descriptor.getImageInfo());
        }
        paramsToBand.put(param, band);
    }

    private static RenderedImage createLandCoverSourceImage(final LandCoverModel landcover, final Band band) {
        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(band.createMultiLevelModel()) {
            @Override
            protected RenderedImage createImage(final int level) {
                return new LandCoverSourceImage(landcover, band.getGeoCoding(),
                        band, ResolutionLevel.create(getModel(), level));
            }
        });
    }

    public static String getValidBandName(String bandName, Product product) {
        String newBandName = bandName;
        if (!bandName.startsWith(DEFAULT_BAND_NAME)) {
            newBandName = DEFAULT_BAND_NAME + '_' + bandName;
        }
        for (int i = 2; product.containsBand(newBandName); i++) {
            newBandName = String.format("%s_%d", newBandName, i);
        }
        return newBandName;
    }

    private static class LandCoverSourceImage extends RasterDataNodeSampleOpImage {
        private final LandCoverModel landcover;
        private final GeoCoding geoCoding;
        private double noDataValue;

        public LandCoverSourceImage(final LandCoverModel landcover, final GeoCoding geoCoding,
                                    final Band band, final ResolutionLevel level) {
            super(band, level);
            this.landcover = landcover;
            this.geoCoding = geoCoding;
            noDataValue = landcover.getDescriptor().getNoDataValue();
        }

        @Override
        protected double computeSample(int sourceX, int sourceY) {
            try {
                return landcover.getLandCover(geoCoding.getGeoPos(new PixelPos(sourceX + 0.5f, sourceY + 0.5f), null));
            } catch (Exception e) {
                return noDataValue;
            }
        }
    }

    public static class LandCoverParameters {
        public String name;
        public String resamplingMethod;
        public String bandName = DEFAULT_BAND_NAME;
        public File externalFile = null;

        public LandCoverParameters(final String name, final String resamplingMethod) {
            this.name = name;
            this.resamplingMethod = resamplingMethod;
            this.bandName = name;
        }

        public LandCoverParameters(final String name, final File externalFile, final String resamplingMethod) {
            this(name, resamplingMethod);
            this.name = externalFile.getName();
            this.externalFile = externalFile;
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.snap.core.gpf.OperatorSpi#createOperator()
     * @see org.esa.snap.core.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(AddLandCoverOp.class);
        }
    }
}