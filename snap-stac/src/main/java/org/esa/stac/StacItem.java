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
package org.esa.stac;

// Author Alex McVittie, SkyWatch Space Applications Inc. December 2023
// The StacItem class allows you to interact with specific Items retrieved
// from a StacCatalog.


import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.util.SystemUtils;
import org.esa.stac.extensions.Basics;
import org.esa.stac.extensions.DateTime;
import org.esa.stac.extensions.ExtensionFactory;
import org.esa.stac.extensions.Provider;
import org.esa.stac.extensions.SAR;
import org.esa.stac.extensions.SNAP;
import org.esa.stac.internal.GeoCodingSupport;
import org.esa.stac.internal.StacComponent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

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
    private final HashMap<String, StacAsset> assetsById = new HashMap<>();
    private final List<StacAsset> dataAssets = new ArrayList<>();
    private final List<StacAsset> metadataAssets = new ArrayList<>();

    private JSONObject propertiesJSON;
    private JSONObject assetsJSON;
    private JSONArray stacExtensionsArray;
    JSONArray linksArray;
    private JSONArray providersArray;
    private JSONArray keywordsArray;

    public StacItem(String inputStr) throws Exception {
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
            throw new ParseException("Unable to parse JSON from given path");
        }
        initStacItem(json);
    }

    public StacItem(File productInputFile) throws Exception {
        try {
            initStacItem((JSONObject) new JSONParser().parse(new FileReader(productInputFile)));
        } catch (Exception e) {
            throw new ParseException("Unable to parse JSON from given local file.");
        }
    }

    public StacItem(JSONObject json) throws Exception {
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

    private void initStacItem(final JSONObject json) throws Exception {
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
            StacAsset curAsset = new StacAsset((JSONObject) assetsJSON.get(o), (String) o);
            assetsById.put((String) o, curAsset);

            if (curAsset.getRole() != null && Objects.equals(METADATAROLE, curAsset.getRole())) {
                metadataAssets.add(curAsset);
            } else {
                dataAssets.add(curAsset);
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

    private static void validate(final JSONObject stacItemJSON) throws ParseException {
        if (Objects.isNull(stacItemJSON)) {
            throw new ParseException("Null JSON object passed in");
        }
        if (!stacItemJSON.containsKey(ID)
                || !stacItemJSON.containsKey(LINKS)
                || !stacItemJSON.containsKey(ASSETS)) {
            throw new ParseException("Invalid STAC JSON");
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

    public StacAsset getAsset(String assetID) {
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

    public static class StacAsset {
        private final String href;
        private final String title;
        private final String id;
        private final JSONObject assetJSON;
        public EOBandData bandData;
        private final List<String> roles;
        private final String fileName;

        private int width = 0;
        private int height = 0;

        StacAsset(JSONObject asset, String id) {
            this.id = id;
            this.title = (String) asset.get("title");
            this.href = (String) asset.get("href");
            this.assetJSON = asset;
            if (asset.containsKey("eo:bands")) {
                bandData = new EOBandData(
                        (JSONObject) ((JSONArray) asset.get("eo:bands")).get(0));
            }
            if (asset.containsKey("proj:shape")) {
                this.width = (int) (long) ((JSONArray) asset.get("proj:shape")).get(0);
                this.height = (int) (long) ((JSONArray) asset.get("proj:shape")).get(1);

            }
            roles = (List<String>) asset.get("roles");
            fileName = this.href.split("/")[this.href.split("/").length - 1];
        }

        public String getTitle() {
            return this.title;
        }

        public String getFileName() {
            return fileName;
        }

        public String getId() {
            return this.id;
        }

        public String getURL() {
            return this.href;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }

        public JSONObject getJSON() {
            return this.assetJSON;
        }

        public String getRole() {
            if(roles == null)
                return null;
            return roles.get(0);
        }

    }

    public static class EOBandData {
        public double centerWavelength;
        public double fullWidthHalfMax;
        public String name;
        public String description;
        public String commonName;

        public EOBandData(JSONObject eoBandData) {
            commonName = "";

            this.centerWavelength = (double) eoBandData.get("center_wavelength");
            this.fullWidthHalfMax = (double) eoBandData.get("full_width_half_max");
            this.name = (String) eoBandData.get("name");
            this.description = (String) eoBandData.get("description");
            this.commonName = (String) eoBandData.get("common_name");
        }
    }
}
