/*
 * Copyright (C) 2020 by SkyWatch Space Applications
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
package org.esa.snap.dem.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dem.dataio.DEMFactory;
import org.esa.snap.dem.dataio.FileElevationModel;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileGeoreferencing;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.Rectangle;
import java.io.File;
import java.util.Map;

/**
 * This operator computes slope and aspect from DEM for a given source product.
 */

@OperatorMetadata(alias = "Compute-Slope-Aspect",
        category = "Raster/DEM Tools",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C)  2020 by SkyWatch Space Applications",
        description = "Compute Slope and Aspect from DEM")
public final class ComputeSlopeAspectOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The digital elevation model.", defaultValue = "SRTM 1Sec HGT", label = "Digital Elevation Model")
    private String demName = "SRTM 1Sec HGT";

    @Parameter(defaultValue = ResamplingFactory.BILINEAR_INTERPOLATION_NAME, label = "DEM Resampling Method")
    private String demResamplingMethod = ResamplingFactory.BILINEAR_INTERPOLATION_NAME;

    @Parameter(label = "External DEM")
    private File externalDEMFile = null;

    @Parameter(label = "DEM No Data Value", defaultValue = "0")
    private double externalDEMNoDataValue = 0;

    @Parameter(label = "External DEM Apply EGM", defaultValue = "false")
    private Boolean externalDEMApplyEGM = false;


    private ElevationModel dem = null;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private boolean isElevationModelAvailable = false;
    private double demNoDataValue = 0.0; // no data value for DEM
    private double noDataValue = 0.0;
    private double rangeSpacing = 0.0;
    private double azimuthSpacing = 0.0;
    private Band slopeBand = null;
    private Band aspectBand = null;
    private Resampling selectedResampling = null;

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            getPixelSpacings();

            createTargetProduct();

            if (demName.equals("External DEM") && externalDEMFile == null) {
                throw new OperatorException("External DEM file is not found");
            }

            if (externalDEMApplyEGM == null) {
                externalDEMApplyEGM = false;
            }

            selectedResampling = ResamplingFactory.createResampling(demResamplingMethod);
            if(selectedResampling == null) {
                throw new OperatorException("Resampling method "+ demResamplingMethod + " is invalid");
            }

            if (externalDEMFile == null) {
                DEMFactory.checkIfDEMInstalled(demName);
            }

            DEMFactory.validateDEM(demName, sourceProduct);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    @Override
    public synchronized void dispose() {
        if (dem != null) {
            dem.dispose();
            dem = null;
        }
    }

    private void getPixelSpacings() throws Exception {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
        if (rangeSpacing <= 0.0) {
            throw new OperatorException("Invalid range pixel spacing: " + rangeSpacing);
        }

        azimuthSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.azimuth_spacing);
        if (azimuthSpacing <= 0.0) {
            throw new OperatorException("Invalid azimuth pixel spacing: " + azimuthSpacing);
        }
    }

    /**
     * Get elevation model.
     */
    private synchronized void getElevationModel() {

        if (isElevationModelAvailable) return;
        try {
            if (externalDEMFile != null) { // if external DEM file is specified by user

                dem = new FileElevationModel(externalDEMFile, demResamplingMethod, externalDEMNoDataValue);
                ((FileElevationModel) dem).applyEarthGravitionalModel(externalDEMApplyEGM);
                demNoDataValue = externalDEMNoDataValue;
                demName = externalDEMFile.getPath();

            } else {
                dem = DEMFactory.createElevationModel(demName, demResamplingMethod);
                demNoDataValue = dem.getDescriptor().getNoDataValue();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        isElevationModelAvailable = true;
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        addSelectedBands();

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);

        if (externalDEMFile != null) { // if external DEM file is specified by user
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.DEM, externalDEMFile.getPath());
        } else {
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.DEM, demName);
        }

        absTgt.setAttributeString("DEM resampling method", demResamplingMethod);

        if (externalDEMFile != null) {
            absTgt.setAttributeDouble("external DEM no data value", externalDEMNoDataValue);
        }
    }

    private void addSelectedBands() {

        for (Band band : sourceProduct.getBands()) {
            if (band instanceof VirtualBand) {
                ProductUtils.copyVirtualBand(targetProduct, (VirtualBand) band, band.getName());
            } else {
                final Band targetBand = ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, false);
                targetBand.setSourceImage(band.getSourceImage());
            }
        }

        slopeBand = new Band("Slope", ProductData.TYPE_FLOAT32, sourceImageWidth, sourceImageHeight);
        slopeBand.setUnit(Unit.DEGREES);
        targetProduct.addBand(slopeBand);

        aspectBand = new Band("Aspect", ProductData.TYPE_FLOAT32, sourceImageWidth, sourceImageHeight);
        aspectBand.setUnit(Unit.DEGREES);
        targetProduct.addBand(aspectBand);
    }


    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;

        try {
            if (!isElevationModelAvailable) {
                getElevationModel();
            }

            final int extTileHeight = h + 2;
            final int extTileWidth = w + 2;
            final double[][] localDEM = new double[extTileHeight][extTileWidth];

            final TileGeoreferencing tileGeoRef = new TileGeoreferencing(
                    targetProduct, x0 - 1, y0 - 1, extTileWidth, extTileHeight);

            final boolean valid = DEMFactory.getLocalDEM(dem, demNoDataValue, demResamplingMethod, tileGeoRef,
                    x0, y0, w, h, sourceProduct, true, localDEM);

            if (!valid) {
                return;
            }

            final Tile slopeTile = targetTiles.get(slopeBand);
            final Tile aspectTile = targetTiles.get(aspectBand);
            final ProductData slopeData = slopeTile.getDataBuffer();
            final ProductData aspectData = aspectTile.getDataBuffer();
            final TileIndex targetIndex = new TileIndex(slopeTile);

            final int ymax = y0 + h;
            final int xmax = x0 + w;
            for (int y = y0; y < ymax; ++y) {
                targetIndex.calculateStride(y);

                for (int x = x0; x < xmax; ++x) {
                    final int tgtIdx = targetIndex.getIndex(x);

                    final double[] slopeAspect = computeSlope(x, y, x0, y0, localDEM);
                    slopeData.setElemFloatAt(tgtIdx, (float) slopeAspect[0]);
                    aspectData.setElemFloatAt(tgtIdx, (float) slopeAspect[1]);
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private double[] computeSlope(final int x, final int y, final int x0, final int y0, final double[][] localDEM) {

        final Double[][] z = new Double[3][3];
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                z[i][j] = localDEM[y - y0 + i][x - x0 + j];
                if (z[i][j].equals(demNoDataValue)) {
                    return new double[]{noDataValue, noDataValue};
                }
            }
        }

        final double b = (z[0][2] + 2 * z[1][2] + z[2][2] - z[0][0] - 2 * z[1][0] - z[2][0]) / (8.0 * rangeSpacing);
        final double c = (z[0][0] + 2 * z[0][1] + z[0][2] - z[2][0] - 2 * z[2][1] - z[2][2]) / (8.0 * azimuthSpacing);
        final double slope = Math.atan(Math.sqrt(b * b + c * c)) * Constants.RTOD;

        double aspect = Math.atan2(-b, -c);
        if (aspect < 0.0) {
            aspect += 2.0 * Math.PI;
        }

        if (slope <= 0.0) {
            aspect = noDataValue;
        }

        return new double[]{slope, aspect};
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ComputeSlopeAspectOp.class);
        }
    }
}
