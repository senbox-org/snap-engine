/*
 * Copyright (C) 2024 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.snap.stac.io;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.stac.StacItem;
import org.esa.snap.stac.extensions.Assets;
import org.esa.snap.stac.internal.SnapStacProduct;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.ImageLayout;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * The product reader for SkyWatch products.
 */
public class STACProductReader extends AbstractProductReader {

    private final static GeoTiffProductReaderPlugIn geoTiffReaderPlugin = new GeoTiffProductReaderPlugIn();

    private final List<ProductReader> imageReaderList = new ArrayList<>();
    private final List<Product> bandProductList = new ArrayList<>();
    private final Map<Band, ImageInputStream> bandInputStreams = new Hashtable<>();
    private final Map<Band, File> bandDataFiles = new HashMap<>();

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public STACProductReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    public void close() throws IOException {
        for (Product b : bandProductList) {
            b.dispose();
        }
        for (ProductReader r : imageReaderList) {
            r.close();
        }
        super.close();
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        try {
            Object input = getInput();
            if (input instanceof InputStream) {
                throw new IOException("InputStream not supported");
            }

            final Path path = ReaderUtils.getPathFromInput(input);
            if(path == null) {
                throw new IOException("Unable to get Path from " + input);
            }
            File metadataFile = path.toFile();
            if (metadataFile.isDirectory()) {
                metadataFile = null;//STACProductReaderPlugIn.findMetadataFile(path);
            } else if (metadataFile.getName().toLowerCase().endsWith(".zip")) {
                throw new IOException("Zipped STAC not supported");
            }

            final StacItem stacItem = new StacItem(metadataFile);
            final SnapStacProduct snapStacProduct = new SnapStacProduct(stacItem);
            Product product = snapStacProduct.createProduct();

            final Map<String, Assets.Asset> imageAssets = stacItem.getImageAssets();

            for (Assets.Asset imageAsset : imageAssets.values()) {
                final File imageFile = new File(metadataFile.getParentFile(), imageAsset.href);
                if (imageFile.exists() && imageFile.length() > 0) {

                    final ProductReader imageReader = geoTiffReaderPlugin.createReaderInstance();
                    imageReaderList.add(imageReader);

                    Product bandProduct = imageReader.readProductNodes(imageFile, null);
                    bandProductList.add(bandProduct);

                    final String bandName = imageAsset.name;
                    final String bandDescription = imageAsset.title;

                    for (Band band : bandProduct.getBands()) {
                        final Band trgBand;
                        if (product.containsBand(bandName)) {
                            trgBand = product.getBand(bandName);
                            trgBand.setSourceImage(band.getSourceImage());
                        } else {
                            trgBand = ProductUtils.copyBand(band.getName(), bandProduct, bandName, product, true);
                        }
                        trgBand.setDescription(bandDescription);

                        stacItem.setBandProperties(trgBand);
                    }

                    if(product.getSceneGeoCoding() == null &&
                            product.getSceneRasterWidth() == bandProduct.getSceneRasterWidth() &&
                            product.getSceneRasterHeight() == bandProduct.getSceneRasterHeight()) {
                        // get geocoding from band geotiffs
                        final ImageLayout imageLayout = new ImageLayout();
                        product.setSceneGeoCoding(bandProduct.getSceneGeoCoding());
                        Dimension tileSize = bandProduct.getPreferredTileSize();
                        if (tileSize == null) {
                            tileSize = ImageManager.getPreferredTileSize(bandProduct);
                        }
                        product.setPreferredTileSize(tileSize);
                        imageLayout.setTileWidth(tileSize.width);
                        imageLayout.setTileHeight(tileSize.height);
                    }
                }
            }

            if(product.getSceneGeoCoding() == null) {
                // get geocoding from geometry

            }

            product.setFileLocation(metadataFile);
            product.setProductReader(this);

            return product;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                          int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand,
                                          int destOffsetX, int destOffsetY,
                                          int destWidth, int destHeight,
                                          ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        final int sourceMinX = sourceOffsetX;
        final int sourceMinY = sourceOffsetY;
        final int sourceMaxX = sourceOffsetX + sourceWidth - 1;
        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;

        final File dataFile = bandDataFiles.get(destBand);
        final ImageInputStream inputStream = getOrCreateImageInputStream(destBand, dataFile);
        if (inputStream == null) {
            return;
        }

        int destPos = 0;

        pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceMaxY - sourceMinY);
        // For each scan in the data source
        try {
            synchronized (inputStream) {
                for (int sourceY = sourceMinY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    final long sourcePosY = (long) sourceY * destBand.getRasterWidth();
                    if (sourceStepX == 1) {
                        long inputPos = sourcePosY + sourceMinX;
                        destBuffer.readFrom(destPos, destWidth, inputStream, inputPos);
                        destPos += destWidth;
                    } else {
                        for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                            long inputPos = sourcePosY + sourceX;
                            destBuffer.readFrom(destPos, 1, inputStream, inputPos);
                            destPos++;
                        }
                    }
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private ImageInputStream getOrCreateImageInputStream(Band band, File file) {
        ImageInputStream inputStream = getImageInputStream(band);
        if (inputStream == null) {
            try {
                inputStream = new FileImageInputStream(file);
            } catch (IOException e) {
                SystemUtils.LOG.log(Level.WARNING,
                        "DimapProductReader: Unable to read file '" + file + "' referenced by '" + band.getName() + "'.",
                        e);
            }
            if (inputStream == null) {
                return null;
            }
            bandInputStreams.put(band, inputStream);
        }
        return inputStream;
    }

    private ImageInputStream getImageInputStream(Band band) {
        if (bandInputStreams != null) {
            return bandInputStreams.get(band);
        }
        return null;
    }
}
