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
package org.esa.snap.raster.gpf.masks;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.image.VirtualBandOpImage;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dem.dataio.DEMFactory;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileGeoreferencing;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The CreateLandMask operator.
 */
@OperatorMetadata(alias = "Land-Sea-Mask",
        category = "Raster/Masks",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2015 by Array Systems Computing Inc.",
        description = "Creates a bitmask defining land vs ocean.")
public class CreateLandMaskOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct = null;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames = null;

    @Parameter(label = "Mask the Land", defaultValue = "true")
    private Boolean landMask = true;

    @Parameter(label = "Use SRTM 3sec", defaultValue = "true")
    private Boolean useSRTM = true;

    @Parameter(label = "Vector")
    private String geometry = "";

    @Parameter(label = "Invert Vector", defaultValue = "false")
    private Boolean invertGeometry = false;

    @Parameter(label = "Extend shoreline by this many pixels", defaultValue = "0")
    private Integer shorelineExtension = 0;

    private ElevationModel dem = null;
    private final static int landThreshold = -10;
    private final static int seaThreshold = -10;

    @Override
    public void initialize() throws OperatorException {
        try {

            targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                        sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

            ProductUtils.copyProductNodes(sourceProduct, targetProduct);

            addSelectedBands();

            if (shorelineExtension == null) {
                shorelineExtension = 0;
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Add the user selected bands to target product.
     *
     * @throws OperatorException The exceptions.
     */
    private void addSelectedBands() throws OperatorException {

        boolean copyVirtualBands = false;

        if (sourceBandNames == null || sourceBandNames.length == 0) {
            copyVirtualBands = true;
            final Band[] bands = sourceProduct.getBands();
            final List<String> bandNameList = new ArrayList<>(sourceProduct.getNumBands());
            for (Band band : bands) {
                if(!targetProduct.containsBand(band.getName())) {
                    bandNameList.add(band.getName());
                }
            }
            sourceBandNames = bandNameList.toArray(new String[0]);
        }

        final List<Band> sourceBandList = new ArrayList<>(sourceBandNames.length);
        for (final String sourceBandName : sourceBandNames) {
            final Band sourceBand = sourceProduct.getBand(sourceBandName);
            if (sourceBand != null) {
                sourceBandList.add(sourceBand);
            }
        }
        final Band[] sourceBands = sourceBandList.toArray(new Band[0]);

        for (Band srcBand : sourceBands) {

            if (srcBand instanceof VirtualBand && copyVirtualBands) {
                Band trgBand = ProductUtils.copyVirtualBand(targetProduct, (VirtualBand) srcBand, srcBand.getName());
                if(srcBand.hasGeoCoding()) {
                    ProductUtils.copyGeoCoding(srcBand, trgBand);
                }
            } else if (geometry != null && !geometry.isEmpty()) {
                String expression = "'" + geometry + "' ? '" + srcBand.getName() + ".raw' : " + srcBand.getNoDataValue();
                if (invertGeometry) {
                    expression = '!' + expression;
                }
                Band trgBand = ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, false);

                VirtualBandOpImage.Builder builder = VirtualBandOpImage.builder(expression, sourceProduct)
                        .dataType(srcBand.getDataType())
                        .sourceSize(new Dimension(srcBand.getRasterWidth(), srcBand.getRasterHeight()));
                VirtualBandOpImage virtualBandImage = builder.create();

                trgBand.setSourceImage(virtualBandImage);
                if(srcBand.hasGeoCoding()) {
                    ProductUtils.copyGeoCoding(srcBand, trgBand);
                }
            } else {
                Band trgBand = ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, false);
                if(srcBand.hasGeoCoding()) {
                    ProductUtils.copyGeoCoding(srcBand, trgBand);
                }
            }
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {
            if (dem == null) {
                createDEM();
            }

            final Band sourceBand = sourceProduct.getBand(targetBand.getName());
            final Tile srcTile = getSourceTile(sourceBand, targetTile.getRectangle());

            final Rectangle targetRectangle = targetTile.getRectangle();
            final int minX = targetRectangle.x;
            final int minY = targetRectangle.y;
            final int maxX = targetRectangle.x + targetRectangle.width;
            final int maxY = targetRectangle.y + targetRectangle.height;
            boolean valid;

            final TileIndex srcTileIndex = new TileIndex(srcTile);
            final TileIndex trgTileIndex = new TileIndex(targetTile);
            final TileGeoreferencing tileGeoRef = new TileGeoreferencing(targetBand.getGeoCoding(),
                    minX, minY, maxX - minX, maxY - minY);

            ProductData tileDataBuffer = targetTile.getDataBuffer();
            ProductData srcDataBuffer = srcTile.getDataBuffer();
            double noDataValue = targetBand.getNoDataValue();
            boolean isInt = tileDataBuffer instanceof ProductData.Int;

            final double demNoDataValue = dem.getDescriptor().getNoDataValue();
            final double[][] localDEM = new double[maxY - minY + 2][maxX - minX + 2];
            DEMFactory.getLocalDEM(
                    dem, demNoDataValue, null, tileGeoRef, minX, minY, maxX - minX,
                    maxY - minY, null, true, localDEM);

            for (int y = minY; y < maxY; ++y) {
                srcTileIndex.calculateStride(y);
                trgTileIndex.calculateStride(y);
                final int yy = y - minY;
                final int eMinY = Math.max(minY, y - shorelineExtension);
                final int eMaxY = Math.min(maxY, y + shorelineExtension);
                for (int x = minX; x < maxX; ++x) {
                    final int trgIndex = trgTileIndex.getIndex(x);
                    final Double elev = localDEM[yy][x - minX];

                    if (landMask) {
                        if (useSRTM) {
                            valid = elev.equals(demNoDataValue);
                        } else {
                            valid = elev < seaThreshold;
                        }
                    } else {
                        if (useSRTM) {
                            valid = !elev.equals(demNoDataValue);
                        } else {
                            valid = elev > landThreshold;
                        }
                    }

                    if (valid) {
                        final int srcIndex = srcTileIndex.getIndex(x);
                        if (isInt) {
                            tileDataBuffer.setElemIntAt(trgIndex, srcDataBuffer.getElemIntAt(srcIndex));
                        } else {
                            tileDataBuffer.setElemDoubleAt(trgIndex, srcDataBuffer.getElemDoubleAt(srcIndex));
                        }
                    } else {
                        if (shorelineExtension > 0) {
                            final int eMinX = Math.max(minX, x - shorelineExtension);
                            final int eMaxX = Math.min(maxX, x + shorelineExtension);

                            for (int ey = eMinY; ey < eMaxY; ++ey) {
                                for (int ex = eMinX; ex < eMaxX; ++ex) {
                                    int eIndex = targetTile.getDataBufferIndex(ex, ey);
                                    if (tileDataBuffer.getElemDoubleAt(eIndex) != noDataValue) {
                                        if (isInt) {
                                            tileDataBuffer.setElemIntAt(eIndex, (int) noDataValue);
                                        } else {
                                            tileDataBuffer.setElemDoubleAt(eIndex, noDataValue);
                                        }
                                    }
                                }
                            }
                        } else {
                            tileDataBuffer.setElemDoubleAt(trgIndex, noDataValue);
                        }
                    }
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private synchronized void createDEM() throws IOException {
        if (dem != null) {
            return;
        }

        if (useSRTM) {
            dem = DEMFactory.createElevationModel("SRTM 3Sec", ResamplingFactory.NEAREST_NEIGHBOUR_NAME);
        } else {
            dem = DEMFactory.createElevationModel("ACE2_5Min", ResamplingFactory.NEAREST_NEIGHBOUR_NAME);
        }
    }

    /**
     * Operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(CreateLandMaskOp.class);
        }
    }
}
