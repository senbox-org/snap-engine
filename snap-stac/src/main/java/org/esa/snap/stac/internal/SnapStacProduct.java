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
package org.esa.snap.stac.internal;

import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.stac.StacItem;
import org.esa.snap.stac.extensions.Assets;
import org.esa.snap.stac.extensions.Basics;
import org.esa.snap.stac.extensions.DateTime;
import org.esa.snap.stac.extensions.EO;
import org.esa.snap.stac.extensions.Instrument;
import org.esa.snap.stac.extensions.Proj;
import org.esa.snap.stac.extensions.Raster;
import org.esa.snap.stac.extensions.SNAP;
import org.esa.snap.stac.extensions.Sat;
import org.esa.snap.stac.extensions.View;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.media.jai.ImageLayout;
import java.awt.*;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class SnapStacProduct implements Closeable {

    private final StacItem stacItem;
    private boolean includeMetadata = true;

    private final static GeoTiffProductReaderPlugIn geoTiffReaderPlugin = new GeoTiffProductReaderPlugIn();
    private final List<ProductReader> imageReaderList = new ArrayList<>();
    private final Map<Assets.Asset, Product> bandProductMap = new HashMap<>();

    public SnapStacProduct(final StacItem stacItem) {
        this.stacItem = stacItem;
    }

    @Override
    public void close() throws IOException {
        for (Product b : bandProductMap.values()) {
            if (b != null) {
                b.dispose();
            }
        }
        for (ProductReader r : imageReaderList) {
            r.close();
        }
    }

    public void includeMetadata(final boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }

    public Product createProduct() throws Exception {
        return createProduct(false);
    }

    public Product createProduct(final boolean reverseHeightWidth) throws Exception {
        final JSONObject propertiesJSON = stacItem.getProperties();
        final Dimension sceneDim = BandSupport.getMaxDimension(propertiesJSON);
        final Product product;
        if (sceneDim != null) {
            if(reverseHeightWidth) {
                product = new Product(stacItem.getId(), stacItem.getProductType(), sceneDim.height, sceneDim.width);
            } else {
                product = new Product(stacItem.getId(), stacItem.getProductType(), sceneDim.width, sceneDim.height);
            }
        } else {
            product = new Product(stacItem.getId(), stacItem.getProductType());
        }

        final ProductData.UTC startTime = DateTime.getStartTime(propertiesJSON);
        if (startTime != null) {
            product.setStartTime(startTime);
        }
        final ProductData.UTC endTime = DateTime.getEndTime(propertiesJSON);
        if (endTime != null) {
            product.setEndTime(endTime);
        }
        product.setDescription(stacItem.getDescription());

        final StacToSnap stacToSnap = new StacToSnap(stacItem, product);

        addBands(product);

        stacToSnap.addMasks();
        stacToSnap.addFlagsCoding();
        stacToSnap.addIndexCoding();
        stacToSnap.addTiePointGrids();
        stacToSnap.addGeoCoding(product);

        if(product.getSceneGeoCoding() == null) {
            for(Product bandProduct : bandProductMap.values()) {
                if (bandProduct.getSceneGeoCoding() != null &&
                        product.getSceneRasterWidth() == bandProduct.getSceneRasterWidth() &&
                        product.getSceneRasterHeight() == bandProduct.getSceneRasterHeight()) {
                    product.setSceneGeoCoding(bandProduct.getSceneGeoCoding());
                    break;
                }
            }
        }

        updateMetadata(product);

        return product;
    }

    private void addBands(final Product product) throws Exception {
        final Map<String, Assets.Asset> imageAssets = stacItem.getImageAssets();
        final List<JSONObject> addedBandPropertiesList = new ArrayList<>();

        if (!imageAssets.isEmpty()) {
            final File metadataFile = stacItem.getJSONFile();
            if (metadataFile != null) {

                for (Object bandObj : stacItem.getBands()) {
                    final JSONObject bandProperties = (JSONObject) bandObj;
                    String name = (String) bandProperties.get(EO.name);

                    if (addedBandPropertiesList.contains(bandProperties)) {
                        continue;
                    }

                    if (imageAssets.containsKey(name)) {
                        final Assets.Asset imageAsset = imageAssets.get(name);

                        final File imageFile = new File(metadataFile.getParentFile(), imageAsset.href);
                        bandProperties.put("file", imageFile.getName());
                        if (imageFile.exists() && imageFile.length() > 0) {

                            final ProductReader imageReader = geoTiffReaderPlugin.createReaderInstance();
                            imageReaderList.add(imageReader);

                            Product bandProduct = imageReader.readProductNodes(imageFile, null);
                            bandProductMap.put(imageAsset, bandProduct);
                            addedBandPropertiesList.add(bandProperties);

                            addBandAsset(product, bandProduct, bandProperties, 0);
                        }

                    } else {
                        for (Assets.Asset imageAsset : imageAssets.values()) {
                            final File imageFile = new File(metadataFile.getParentFile(), imageAsset.href);
                            if (imageAsset.name.toLowerCase().contains(name.toLowerCase())) {
                                bandProperties.put("file", imageFile.getName());
                            }
                        }
                        processAssetBands(product, imageAssets, metadataFile, addedBandPropertiesList);
                    }
                }

                if (product.getNumBands() == 0) {
                    processAssetBands(product, imageAssets, metadataFile, addedBandPropertiesList);
                }
            }
        }

        if (product.getNumBands() == 0) {
            for (Object bandObj : stacItem.getBands()) {
                addBandAsset(product, (JSONObject) bandObj);
            }
        }
    }

    private void processAssetBands(final Product product, final Map<String, Assets.Asset> imageAssets,
                                   final File metadataFile, final List<JSONObject> addedBandPropertiesList) throws Exception {
        JSONArray eoBands = stacItem.getBands();
        for (Assets.Asset imageAsset : imageAssets.values()) {
            if (!bandProductMap.containsKey(imageAsset)) {
                JSONArray assetBandArray = imageAsset.getBands();
                if(eoBands.isEmpty()) {
                    eoBands.addAll(assetBandArray);
                    if(!stacItem.getProperties().containsKey(EO.bands)) {
                        stacItem.getProperties().put(EO.bands, eoBands);
                    }
                }

                final File imageFile = new File(metadataFile.getParentFile(), imageAsset.href);
                Product bandProduct = null;
                if (imageFile.exists() && imageFile.length() > 0) {

                    final ProductReader imageReader = geoTiffReaderPlugin.createReaderInstance();
                    imageReaderList.add(imageReader);

                    bandProduct = imageReader.readProductNodes(imageFile, null);
                    bandProductMap.put(imageAsset, bandProduct);
                }

                int bandCnt = 0;
                for (Object bObj : eoBands) {
                    final JSONObject bProperties = (JSONObject) bObj;

                    if (addedBandPropertiesList.contains(bProperties)) {
                        continue;
                    }
                    addedBandPropertiesList.add(bProperties);
                    addBandAsset(product, bandProduct, bProperties, bandCnt);
                    ++bandCnt;
                }
            }
        }
    }

    private void addBandAsset(final Product product, final Product bandProduct,
                              final JSONObject bandProperties, final int bandCnt) throws Exception {

        String bandName = (String) bandProperties.get(EO.common_name);
        if (bandName == null) {
            bandName = (String) bandProperties.get(EO.name);
        }

        final Band trgBand;
        if(bandProduct != null) {
            Band band = bandProduct.containsBand(bandName) ? bandProduct.getBand(bandName) : bandProduct.getBandAt(bandCnt);

            if (product.containsBand(bandName)) {
                trgBand = product.getBand(bandName);
                trgBand.setSourceImage(band.getSourceImage());
            } else {
                trgBand = ProductUtils.copyBand(band.getName(), bandProduct, bandName, product, true);
            }
        } else {
            trgBand = new Band(bandName, ProductData.TYPE_INT16, product.getSceneRasterWidth(), product.getSceneRasterHeight());
            trgBand.setNoDataValue(0.0);
            trgBand.setNoDataValueUsed(true);
            product.addBand(trgBand);
        }

        trgBand.setDescription(bandProperties.containsKey("title") ? (String)bandProperties.get("title") : (String) bandProperties.get("file"));
        setBandProperties(trgBand, stacItem, bandProperties);

        if (product.getSceneGeoCoding() == null && bandProduct != null &&
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

    private void addBandAsset(final Product product, final JSONObject bandProperties) throws Exception {

        String bandName = (String) bandProperties.get(EO.common_name);
        if (bandName == null) {
            bandName = (String) bandProperties.get(EO.name);
        }
        int dataType = ProductData.TYPE_INT8;
        if (bandProperties.containsKey(Raster.data_type)) {
            dataType = ProductData.getType((String) bandProperties.get(Raster.data_type));
        }

        Band trgBand = new Band(bandName, dataType, product.getSceneRasterWidth(), product.getSceneRasterHeight());
        if (bandProperties.containsKey("file")) {
            trgBand.setDescription((String) bandProperties.get("file"));
        } else {
            trgBand.setDescription((String) bandProperties.get(EO.description));
        }
        setBandProperties(trgBand, stacItem, bandProperties);

        product.addBand(trgBand);
    }

    private static void setBandProperties(final Band trgBand, final StacItem stacItem,
                                          final JSONObject bandProperties) throws IOException {
        trgBand.setUnit("dn");
        if (bandProperties.containsKey(Raster.unit)) {
            trgBand.setUnit((String) bandProperties.get(Raster.unit));
        }
        if (bandProperties.containsKey(EO.full_width_half_max)) {
            float fwhm = (float) JsonUtils.getDouble(bandProperties.get(EO.full_width_half_max));
            trgBand.setSpectralBandwidth(fwhm * 1000); // micrometer to nanometers
        }
        if (bandProperties.containsKey(EO.center_wavelength)) {
            float center = (float) JsonUtils.getDouble(bandProperties.get(EO.center_wavelength));
            trgBand.setSpectralWavelength(center * 1000);
            //trgBand.setSpectralBandIndex(cnt);
        }

        stacItem.setBandProperties(trgBand);
    }

    private void updateMetadata(final Product product) {
        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(product.getMetadataRoot());

        absRoot.setAttributeString(AbstractMetadata.PRODUCT, stacItem.getId());
        absRoot.setAttributeString(AbstractMetadata.PRODUCT_TYPE, stacItem.getProductType());

        final JSONObject propertiesJSON = stacItem.getProperties();

        if (stacItem.getJSON().containsKey(StacItem.COLLECTION)) {
            absRoot.setAttributeString(AbstractMetadata.SPH_DESCRIPTOR, (String) stacItem.getJSON().get(StacItem.COLLECTION));
        } else {
            absRoot.setAttributeString(AbstractMetadata.SPH_DESCRIPTOR, stacItem.getId());
        }
        if (propertiesJSON.containsKey(Instrument.mission)) {
            absRoot.setAttributeString(AbstractMetadata.MISSION, (String) propertiesJSON.get(Instrument.mission));
        } else if (propertiesJSON.containsKey(Instrument.constellation)) {
            absRoot.setAttributeString(AbstractMetadata.MISSION, (String) propertiesJSON.get(Instrument.constellation));
        }
        if (propertiesJSON.containsKey(Instrument.instrument_mode)) {
            absRoot.setAttributeString(AbstractMetadata.ACQUISITION_MODE, (String) propertiesJSON.get(Instrument.instrument_mode));
        } else if (propertiesJSON.containsKey(Instrument.sensor_mode)) {
            absRoot.setAttributeString(AbstractMetadata.ACQUISITION_MODE, (String) propertiesJSON.get(Instrument.sensor_mode));
        }
        if (propertiesJSON.containsKey(Instrument.gsd)) {
            try {
                absRoot.setAttributeDouble(AbstractMetadata.range_spacing, JsonUtils.getDouble(propertiesJSON.get(Instrument.gsd)));
                absRoot.setAttributeDouble(AbstractMetadata.azimuth_spacing, JsonUtils.getDouble(propertiesJSON.get(Instrument.gsd)));
            } catch(NumberFormatException e) {
                // ignore
            }
        }
        if (propertiesJSON.containsKey(Sat.relative_orbit)) {
            absRoot.setAttributeInt(AbstractMetadata.REL_ORBIT, JsonUtils.getInt(propertiesJSON.get(Sat.relative_orbit)));
        }
        if (propertiesJSON.containsKey(Sat.absolute_orbit)) {
            absRoot.setAttributeInt(AbstractMetadata.ABS_ORBIT, JsonUtils.getInt(propertiesJSON.get(Sat.absolute_orbit)));
        }
        if (propertiesJSON.containsKey(Sat.orbit_state)) {
            absRoot.setAttributeString(AbstractMetadata.PASS, (String) propertiesJSON.get(Sat.orbit_state));
        }
    }

    public void writeProductProperties(final Product product) throws IOException {

        final JSONObject propertiesJSON = stacItem.getProperties();

        propertiesJSON.put(Basics.title, product.getName());
        propertiesJSON.put(Basics.description, product.getDescription());
        propertiesJSON.put(SNAP.product_type, product.getProductType());

        stacItem.addExtension(EO.schema, View.schema, Sat.schema, Proj.schema);
        stacItem.addKeywords(EO.KeyWords.earth_observation, EO.KeyWords.satellite);

        final SnapToStac snapToStac = new SnapToStac(stacItem, product);

        snapToStac.writeProjection();
        snapToStac.writeBoundingBox();
        snapToStac.writeTimes();
        snapToStac.writeCoordinates();
        snapToStac.writeBands();
        snapToStac.writeTiePointGrids();
        snapToStac.writeFlagCoding();
        snapToStac.writeIndexCoding();
        snapToStac.writeMasks();

        if(includeMetadata) {
            snapToStac.writeMetadata();
        }
    }
}
