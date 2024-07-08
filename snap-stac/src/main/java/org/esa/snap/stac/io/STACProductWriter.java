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
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.geotiff.GeoTiffProductWriterPlugIn;
import org.esa.snap.stac.StacItem;
import org.esa.snap.stac.extensions.Assets;
import org.esa.snap.stac.internal.JsonUtils;
import org.esa.snap.stac.internal.SnapStacProduct;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class STACProductWriter extends AbstractProductWriter {

    private static final GeoTiffProductWriterPlugIn geoTiffProductWriterPlugIn = new GeoTiffProductWriterPlugIn();

    private final Map<Band, ProductWriter> bandWriterMap = new HashMap<>();
    private final Map<Band, Band> bandMap = new HashMap<>();
    private final boolean singleBand;

    public STACProductWriter(final ProductWriterPlugIn writerPlugIn) {
        this(writerPlugIn, false);
    }

    public STACProductWriter(final ProductWriterPlugIn writerPlugIn, final boolean singleBand) {
        super(writerPlugIn);
        this.singleBand = singleBand;
    }

    @Override
    protected void writeProductNodesImpl() throws IOException {

        final Product srcProduct = getSourceProduct();

        File imageFile;
        if (getOutput() instanceof String) {
            imageFile = new File((String) getOutput());
        } else {
            imageFile = (File) getOutput();
        }
        imageFile.getParentFile().mkdirs();

        final StacItem stacItem = new StacItem(srcProduct.getName());
        SnapStacProduct snapStacProduct = new SnapStacProduct(stacItem);
        snapStacProduct.writeProductProperties(srcProduct);

        String baseName = imageFile.getName();
        if (baseName.endsWith(STACProductReaderPlugIn.IMAGE_GEOTIFF_EXT)) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }

        if (singleBand) {
            for (Band srcBand : srcProduct.getBands()) {
                ProductWriter bandWriter = geoTiffProductWriterPlugIn.createWriterInstance();
                imageFile = new File(imageFile.getParentFile(), baseName + "_" + srcBand.getName() + STACProductReaderPlugIn.IMAGE_GEOTIFF_EXT);

                Product trgProduct = new Product(srcProduct.getName(), srcProduct.getProductType(), srcProduct.getSceneRasterWidth(), srcProduct.getSceneRasterHeight());
                ProductUtils.copyMetadata(srcProduct, trgProduct);
                ProductUtils.copyTiePointGrids(srcProduct, trgProduct);
                ProductUtils.copyFlagCodings(srcProduct, trgProduct);
                //ProductUtils.copyFlagBands(srcProduct, trgProduct, true);
                ProductUtils.copyGeoCoding(srcProduct, trgProduct);
                ProductUtils.copyMasks(srcProduct, trgProduct);
                ProductUtils.copyVectorData(srcProduct, trgProduct);
                ProductUtils.copyIndexCodings(srcProduct, trgProduct);
                ProductUtils.copyQuicklookBandName(srcProduct, trgProduct);
                trgProduct.setStartTime(srcProduct.getStartTime());
                trgProduct.setEndTime(srcProduct.getEndTime());
                trgProduct.setDescription(srcProduct.getDescription());
                trgProduct.setAutoGrouping(srcProduct.getAutoGrouping());

                Band trgBand = ProductUtils.copyBand(srcBand.getName(), srcProduct, trgProduct, true);

                bandWriter.writeProductNodes(trgProduct, imageFile);

                bandWriterMap.put(srcBand, bandWriter);
                bandMap.put(srcBand, trgBand);
            }
        } else {
            ProductWriter bandWriter = geoTiffProductWriterPlugIn.createWriterInstance();
            bandWriter.writeProductNodes(srcProduct, imageFile);

            stacItem.addAsset(Assets.raster, "Raster image", null,
                    FileUtils.getFilenameWithoutExtension(imageFile.getName()), Assets.type_image_tiff, null);

            for (Band srcBand : srcProduct.getBands()) {
                bandWriterMap.put(srcBand, bandWriter);
                bandMap.put(srcBand, srcBand);
            }
        }

        writeProductMetadata(imageFile, stacItem);
    }

    private void writeProductMetadata(final File imageFile, final StacItem stacItem) throws IOException {

        final File metadataFile = FileUtils.exchangeExtension(imageFile, STACProductReaderPlugIn.METADATA_EXT);
        final FileWriter metaStringWriter = new FileWriter(metadataFile);

        stacItem.addAsset(Assets.metadata, metadataFile.getName(), null,
                FileUtils.getFilenameWithoutExtension(metadataFile.getName()), Assets.type_json, null);

        metaStringWriter.write(JsonUtils.prettyPrint(stacItem.getJSON()));
        metaStringWriter.close();
    }

    @Override
    public void writeBandRasterData(final Band sourceBand,
                                    final int sourceOffsetX,
                                    final int sourceOffsetY,
                                    final int sourceWidth,
                                    final int sourceHeight,
                                    final ProductData sourceBuffer,
                                    ProgressMonitor pm) throws IOException {
        if (sourceBand instanceof VirtualBand) {

        } else {
            ProductWriter bandWriter = bandWriterMap.get(sourceBand);
            bandWriter.writeBandRasterData(bandMap.get(sourceBand),
                    sourceOffsetX, sourceOffsetY,
                    sourceWidth, sourceHeight,
                    sourceBuffer, pm);
        }
    }

    @Override
    public boolean shouldWrite(ProductNode node) {
        return !(node instanceof VirtualBand) && !(node instanceof FilterBand);
    }

    @Override
    public void flush() throws IOException {
        for (ProductWriter bandWriter : bandWriterMap.values()) {
            bandWriter.flush();
        }
    }

    @Override
    public void close() throws IOException {
        for (ProductWriter bandWriter : bandWriterMap.values()) {
            bandWriter.close();
        }
    }

    @Override
    public void deleteOutput() throws IOException {
        for (ProductWriter bandWriter : bandWriterMap.values()) {
            bandWriter.deleteOutput();
        }
    }
}
