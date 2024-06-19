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
package org.esa.snap.stac;

// Author Alex McVittie, SkyWatch Space Applications Inc. December 2023


import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.stac.extensions.Assets;
import org.esa.snap.stac.extensions.Basics;
import org.esa.snap.stac.extensions.DateTime;
import org.esa.snap.stac.extensions.EO;
import org.esa.snap.stac.extensions.ExtensionFactory;
import org.esa.snap.stac.extensions.Provider;
import org.esa.snap.stac.extensions.Raster;
import org.esa.snap.stac.extensions.SAR;
import org.esa.snap.stac.extensions.SNAP;
import org.esa.snap.stac.internal.GeoCodingSupport;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The StacItem class allows you to interact with specific Items retrieved
 * from a StacCatalog.
 */
@SuppressWarnings("unchecked")
public class StacItem implements StacComponent {

    private final static String LATEST_VERSION = "1.0.0";

    public final static String STAC_EXTENSIONS = "stac_extensions";

    public final static String COLLECTION = "collection";
    public final static String KEYWORDS = "keywords";

    private final String DATAROLE = "data";
    private final String PREVIEWROLE = "thumbnail";
    private final String OVERVIEWROLE = "overview";
    private final String METADATAROLE = "metadata";
    private final String TILESROLE = "tiles";


    private JSONObject stacItemJSON;

    private String itemURL;
    private String version;
    private File jsonFile;
    private final HashMap<String, Assets.Asset> assetsById = new HashMap<>();
    private final List<Assets.Asset> dataAssets = new ArrayList<>();
    private final List<Assets.Asset> metadataAssets = new ArrayList<>();

    private JSONObject propertiesJSON;
    private JSONObject assetsJSON;
    private JSONArray stacExtensionsArray;
    JSONArray linksArray;
    private JSONArray providersArray;
    private JSONArray keywordsArray;

    /**
     * Creates a new StacItem object from a given URL
     *
     * @param inputStr The URL of the StacItem
     * @throws Exception
     */
    public StacItem(String inputStr) throws IOException {
        JSONObject json;
        try {
            if (inputStr.startsWith("http")) {
                json = getJSONFromURL(inputStr);
            } else if(inputStr.startsWith("{")) {
                json = (JSONObject) new JSONParser().parse(inputStr);
            } else {
                json = createEmptyStacItem(inputStr);
            }
        } catch (Exception e) {
            throw new IOException("Unable to parse JSON from given path");
        }
        initStacItem(json);
    }

    /**
     * Creates a new StacItem object from a given File
     *
     * @param productInputFile The file of the StacItem
     * @throws Exception
     */
    public StacItem(File productInputFile) throws IOException {
        try {
            jsonFile = productInputFile;
            initStacItem((JSONObject) new JSONParser().parse(new FileReader(productInputFile)));
        } catch (Exception e) {
            throw new IOException("Unable to parse JSON from given local file.");
        }
    }

    /**
     * Creates a new StacItem object from a given JSONObject
     *
     * @param json The path of the StacItem
     * @throws Exception
     */
    public StacItem(JSONObject json) throws IOException {
        initStacItem(json);
    }

    private static JSONObject createEmptyStacItem(final String identifier) {
        final JSONObject json = new JSONObject();

        json.put(STAC_VERSION, LATEST_VERSION);
        json.put(ID, identifier);
        json.put(TYPE, FEATURE);

        json.put(STAC_EXTENSIONS, new JSONArray());

        JSONObject propertiesJSON = new JSONObject();
        json.put(PROPERTIES, propertiesJSON);
        propertiesJSON.put(PROVIDERS, new JSONArray());

        json.put(KEYWORDS, new JSONArray());
        json.put(ASSETS, new JSONObject());
        json.put(LINKS, new JSONArray());

        return json;
    }

    private void initStacItem(final JSONObject json) throws IOException {
        validate(json);

        this.stacItemJSON = json;
        if (json.containsKey(STAC_VERSION)) {
            version = (String) json.get(STAC_VERSION);
        }

        if (json.containsKey(STAC_EXTENSIONS)) {
            stacExtensionsArray = (JSONArray) json.get(STAC_EXTENSIONS);
            Object[] exts = stacExtensionsArray.toArray();
            for (Object ext : exts) {
                if(!((String)ext).startsWith("http")) {
                    stacExtensionsArray.remove(ext);
                    stacExtensionsArray.add(ExtensionFactory.getSchema((String) ext));
                }
            }
        }

        if (json.containsKey(PROPERTIES)) {
            propertiesJSON = (JSONObject) json.get(PROPERTIES);

            if (propertiesJSON.containsKey(PROVIDERS)) {
                providersArray = (JSONArray) propertiesJSON.get(PROVIDERS);
            }
        }

        this.assetsJSON = (JSONObject) stacItemJSON.get(ASSETS);
        for (Object o : assetsJSON.keySet()) {
            final Assets.Asset asset = new Assets.Asset((String)o, (JSONObject)assetsJSON.get(o));
            assetsById.put((String) o, asset);

            if (asset.role != null && Objects.equals(METADATAROLE, asset.role)) {
                metadataAssets.add(asset);
            } else {
                dataAssets.add(asset);
            }
        }

        if (json.containsKey(KEYWORDS)) {
            keywordsArray = (JSONArray) json.get(KEYWORDS);
        }

        if (json.containsKey(ASSETS)) {
            assetsJSON = (JSONObject) json.get(ASSETS);
        }

        if (json.containsKey(LINKS)) {
            linksArray = (JSONArray) json.get(LINKS);
            for (Object o : linksArray) {
                String rel = (String) ((JSONObject) o).get(REL);
                if (Objects.equals(rel, SELF)) {
                    itemURL = (String) ((JSONObject) o).get(HREF);
                }
            }
        }
    }

    private static void validate(final JSONObject stacItemJSON) throws IOException {
        if (Objects.isNull(stacItemJSON)) {
            throw new IOException("Null JSON object passed in");
        }
        if (!stacItemJSON.containsKey(ID)
                || !stacItemJSON.containsKey(ASSETS)) {
            throw new IOException("Invalid STAC JSON");
        }
    }

    @Override
    public JSONObject getJSON() {
        return stacItemJSON;
    }

    @Override
    public String getId() {
        return (String) stacItemJSON.get(ID);
    }

    @Override
    public String getSelfURL() {
        return getURL(stacItemJSON, SELF);
    }

    @Override
    public String getRootURL() {
        return getURL(stacItemJSON, ROOT);
    }

    public String getURL() {
        return this.itemURL;
    }

    public String getDescription() {
        if (propertiesJSON.containsKey(Basics.description)) {
            return (String) propertiesJSON.get(Basics.description);
        }
        if (propertiesJSON.containsKey(Basics.title)) {
            return (String) propertiesJSON.get(Basics.title);
        }
        if (stacItemJSON.containsKey(Basics.title)) {
            return (String) stacItemJSON.get(Basics.title);
        }
        return "";
    }

    public String getProductType() {
        if (propertiesJSON.containsKey(SAR.product_type)) {
            return (String) propertiesJSON.get(SAR.product_type);
        } else if (propertiesJSON.containsKey(SNAP.product_type)) {
            return (String) propertiesJSON.get(SNAP.product_type);
        } else if (propertiesJSON.containsKey("product_type")) {
            return (String) propertiesJSON.get("product_type");
        }
        return "stac_product";
    }

    public ProductData.UTC getTime() throws Exception {
        return DateTime.getStartTime(propertiesJSON);
    }

    public void addExtension(final String... extensions) {
        for(String ext : extensions) {
            if(!stacExtensionsArray.contains(ext)) {
                stacExtensionsArray.add(ext);
            }
        }
    }

    public String[] getExtensions() {
        final List<String> extList = new ArrayList<>();
        for (Object ext : stacExtensionsArray) {
            extList.add((String) ext);
        }
        return extList.toArray(new String[0]);
    }

    public void addKeywords(final String... keywords) {
        for(String keyword : keywords) {
            if(!keywordsArray.contains(keyword)) {
                keywordsArray.add(keyword);
            }
        }
    }

    public void addProvider(final String name, final String role, final String url) {
        providersArray.add(Provider.create(name, role, url));
    }

    public JSONObject getGeometry() {
        return (JSONObject) stacItemJSON.get(GEOMETRY);
    }

    public String getGeometryAsWKT() {
        return GeoCodingSupport.toWKT(getGeometry());
    }

    public JSONArray getBoundingBox() {
        return (JSONArray) stacItemJSON.get(BBOX);
    }

    public JSONObject getProperties() {
        return propertiesJSON;
    }

    public JSONObject getAssets() {
        return assetsJSON;
    }

    public Assets.Asset getAsset(String assetID) {
        if (assetsById.containsKey(assetID)) {
            return assetsById.get(assetID);
        }
        return null;
    }

    public String[] listAssetIds() {
        String[] assetArray = new String[assetsJSON.size()];
        assetArray = (String[]) assetsJSON.keySet().toArray(assetArray);
        Arrays.sort(assetArray);
        return assetArray;
    }

    public JSONArray getBands() throws IOException {
        JSONArray bandArray = new JSONArray();
        if (propertiesJSON.containsKey(EO.bands)) {
            bandArray = (JSONArray) propertiesJSON.get(EO.bands);
        }
        if (propertiesJSON.containsKey(Raster.bands)) {
            if (bandArray.isEmpty()) {
                bandArray = (JSONArray) propertiesJSON.get(Raster.bands);
            } else {
                JSONArray rasterArray = (JSONArray) propertiesJSON.get(Raster.bands);
                if(bandArray.size() != rasterArray.size()) {
                    throw new IOException("EO bands and raster bands are not the same size");
                }
                for (int i = 0; i < bandArray.size(); ++i) {
                    JSONObject band = (JSONObject)bandArray.get(i);
                    JSONObject raster = (JSONObject)rasterArray.get(i);
                    for(Object key : raster.keySet()) {
                        band.put(key, raster.get(key));
                    }
                }
            }
        }
        return bandArray;
    }

    public void setBandProperties(final Band band) throws IOException {
        final JSONArray bandsArray = getEOBands();
        for (Object o : bandsArray) {
            final JSONObject bandProperties = (JSONObject) o;
            if (band.getName().equalsIgnoreCase((String) bandProperties.get(EO.name)) ||
                    band.getName().equalsIgnoreCase((String) bandProperties.get(EO.common_name))) {
                EO.getBandProperties(band, bandProperties);
                return;
            }
        }
    }

    public JSONArray getEOBands() throws IOException {
        if (propertiesJSON.containsKey(EO.bands)) {
            return (JSONArray) propertiesJSON.get(EO.bands);
        }
        throw new IOException("EO band properties not found");
    }

    public File getJSONFile() {
        return jsonFile;
    }

    public String getVersion() {
        return version;
    }

    public boolean isStacItem() {
        return getVersion() != null && assetsJSON != null;
    }

    public static boolean isStacItem(final Path path) {
        try {
            final JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(path.toFile())) {
                final JSONObject json = (JSONObject) parser.parse(reader);
                final StacItem stacItem = new StacItem(json);
                return stacItem.isStacItem();
            }
        } catch (Exception e) {
            SystemUtils.LOG.severe("Unable to parse " + path);
            return false;
        }
    }

    public static boolean isStacItem(final URL url) {
        try (InputStream inStream = url.openStream()) {
            final JSONParser parser = new JSONParser();
            try (InputStreamReader inputStreamReader = new InputStreamReader(inStream, StandardCharsets.UTF_8)) {
                try (BufferedReader streamReader = new BufferedReader(inputStreamReader)) {
                    final JSONObject json = (JSONObject) parser.parse(streamReader);
                    final StacItem stacItem = new StacItem(json);
                    return stacItem.isStacItem();
                }
            }
        } catch (Exception e) {
            SystemUtils.LOG.severe("Unable to parse " + url);
            return false;
        }
    }

    public Assets.Asset addAsset(final String name, final String titleValue, final String descriptionValue,
                                 final String hrefValue, final String typeValue, final String roleValue) {
        return Assets.addAsset(assetsJSON, name, titleValue, descriptionValue, hrefValue, typeValue, roleValue);
    }

    public Map<String, Assets.Asset> getImageAssets() {
        return Assets.getImageAssets(assetsJSON);
    }
}
