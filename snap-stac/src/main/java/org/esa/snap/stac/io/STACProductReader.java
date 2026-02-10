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
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.stac.StacItem;
import org.esa.snap.stac.extensions.Assets;
import org.esa.snap.stac.internal.SnapStacProduct;

import javax.media.jai.ImageLayout;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The product reader for SkyWatch products.
 */
public class STACProductReader extends AbstractProductReader {

    private final List<ProductReader> imageReaderList = new ArrayList<>();
    private final List<Product> bandProductList = new ArrayList<>();

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
                final File imageFile = new File(metadataFile.getParentFile(), imageAsset.href.substring(imageAsset.href.lastIndexOf('/')));
                if (imageFile.exists() && imageFile.length() > 0) {

                    final ProductReader imageReader = createReaderInstance();
                    if (imageReader == null) {
                        throw new IOException("No reader found for " + imageFile);
                    }
                    imageReaderList.add(imageReader);

                    Product bandProduct = imageReader.readProductNodes(imageFile, null);
                    bandProductList.add(bandProduct);

                    final String bandName = imageAsset.name;
                    final String bandDescription = imageAsset.title;

                    for (Band band : bandProduct.getBands()) {
                        Band trgBand = null;
                        if (product.containsBand(bandName)) {
                            trgBand = product.getBand(bandName);
                            trgBand.setSourceImage(band.getSourceImage());
                        } else {
                            for(Band existingBand : product.getBands()) {
                                if(!existingBand.isSourceImageSet()) {
                                    existingBand.setSourceImage(band.getSourceImage());
                                    trgBand = existingBand;
                                    break;
                                }
                            }
                            if(trgBand == null) {
                                trgBand = ProductUtils.copyBand(band.getName(), bandProduct, bandName, product, true);
                            }
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

    private ProductReader createReaderInstance() {
        Iterator<ProductReaderPlugIn> it = ProductIOPlugInManager.getInstance().getReaderPlugIns("GDAL-GTiff-READER");
        if (it.hasNext()) {
            return it.next().createReaderInstance();
        }
        return null;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                          int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand,
                                          int destOffsetX, int destOffsetY,
                                          int destWidth, int destHeight,
                                          ProductData destBuffer,
                                          ProgressMonitor pm) {
    }
}
