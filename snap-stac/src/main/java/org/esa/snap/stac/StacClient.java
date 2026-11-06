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

// Author Alex McVittie, SkyWatch Space Applications Inc. January 2024

import org.esa.snap.stac.extensions.Assets;
import org.esa.snap.stac.internal.DownloadModifier;
import org.esa.snap.stac.internal.EstablishedModifiers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *  The StacClient class acts as a way to search for STAC assets, and download these
 *  STAC items and assets.
 */
public class StacClient {

    private DownloadModifier downloadModifier;

    private boolean signDownloads;

    public String title;

    private final StacCatalog catalog;

    private final String stacURL;

    public StacClient(final String catalogURL) {
        this(catalogURL, null);
    }

    public StacClient(final String catalogURL, final DownloadModifier modifierFunction) {
        this.stacURL = catalogURL;
        this.catalog = new StacCatalog(catalogURL);
        this.signDownloads = false;
        if(modifierFunction != null) {
            this.downloadModifier = modifierFunction;
            this.signDownloads = true;
        } else {
            if (catalogURL.contains("planetarycomputer.microsoft.com")) {
                this.downloadModifier = EstablishedModifiers.planetaryComputer();
                this.signDownloads = true;
            }
        }
    }

    public static StacClient createClient(final StacItem stacItem) throws IOException {
        for (Object o : stacItem.linksArray) {
            JSONObject curLinkObject = (JSONObject) o;
            if (curLinkObject.get(StacComponent.REL).equals(StacComponent.ROOT)) {
                String mainURL = (String) curLinkObject.get(StacComponent.HREF);
                try {
                    return new StacClient(mainURL);
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
        }
        throw new IOException("No root link found in STAC item");
    }

    /**
     * Retrieves the catalog description from the remote STAC service
     */
    public StacCatalog getCatalog() {
        return this.catalog;
    }

    /**
     * Performs a search and returns an array of STAC Items from the server that match the search
     * @param collections
     * @param bbox
     * @param datetime
     * @return
     * @throws Exception
     */
    public StacItem[] search(final String[] collections, final double[] bbox, String datetime) throws Exception {
        String bboxStr = bbox[0] + "," + bbox[1] + "," + bbox[2] + "," + bbox[3];
        String queryParams = "&bbox=" + bboxStr;
        if(datetime != null) queryParams = queryParams + "&datetime=" + datetime;

        return searchEachCollection(collections, queryParams);
    }

    // Performs a search and returns an array of STAC Items from the server that match the search
    public StacItem[] search(final String[] collections, final JSONObject geometry, String datetime) throws Exception {
        String queryParams = "&intersects=" + geometry;
        if(datetime != null) queryParams = queryParams + "&datetime=" + datetime;

        return searchEachCollection(collections, queryParams);
    }

    /**
     * Returns the first page of items that match the given parameters from a collection
     * @param collections    The collection name
     * @param parameters     The search criteria
     */
    public StacItem[] search(String[] collections, Map<String, Object> parameters) throws Exception {
        return search(collections, parameters, 0, 0);
    }

    /**
     * Returns a page of items that match the given parameters from a collection
     * @param collections    The collection name
     * @param parameters        The search criteria
     * @param pageNumber        The page number (1-based)
     * @param pageSize          The page size
     */
    public StacItem[] search(String[] collections, Map<String, Object> parameters, int pageNumber, int pageSize) throws Exception {

        String queryParams = "&";
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            queryParams += entry.getKey()+"="+entry.getValue()+"&";
        }
        if (pageNumber > 0 & pageSize > 0) {
            queryParams +="page="+pageSize+"&limit="+pageSize;
        }
        if (queryParams.charAt(queryParams.length() - 1) == '&') {
            queryParams = queryParams.substring(0, queryParams.length() - 1);
        }

        return searchEachCollection(collections, queryParams);
    }

    /**
     * Runs one search request per collection and concatenates the results. Some servers (e.g. Planetary
     * Computer) reject a search spanning more than one collection with HTTP 400, so collections are never
     * combined into a single request. Collections not in the catalog are skipped.
     * @param collections    The collection names
     * @param queryParams    The query string appended after the collections parameter, starting with '&'
     */
    private StacItem[] searchEachCollection(final String[] collections, final String queryParams) throws Exception {
        String searchEndpoint = stacURL + "/search?";
        List<StacItem> items = new ArrayList<>();
        for (String collectionName : collections) {
            if (!this.catalog.containsCollection(collectionName)) {
                continue;
            }
            String query = searchEndpoint + "collections=" + collectionName + queryParams;

            JSONObject queryResults = StacComponent.getJSONFromURLStatic(query);
            JSONArray jsonFeatures = getAllFeatures(queryResults);
            for (Object feature : jsonFeatures) {
                items.add(new StacItem((JSONObject) feature));
            }
        }
        return items.toArray(new StacItem[0]);
    }

    public String signURL(Assets.Asset asset) {
        if (signDownloads) {
            return downloadModifier.signURL(asset.getURL());
        } else {
            return asset.getURL();
        }
    }

    public InputStream streamAsset(Assets.Asset asset) throws IOException {
        String downloadURL = signURL(asset);
        InputStream inputStream = new URL(downloadURL).openStream();
        return inputStream;
    }

    public File downloadAsset(Assets.Asset asset, File targetFolder) throws IOException {
        targetFolder.mkdirs();
        String outputFilename = asset.getFileName();
        if(outputFilename.contains("?")) {
            outputFilename = outputFilename.substring(0, outputFilename.indexOf("?"));
        }

        System.out.println("Downloading asset " + asset.getId());
        String downloadURL = signURL(asset);
        try (BufferedInputStream in = new BufferedInputStream(new URL(downloadURL).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(new File(targetFolder.getAbsolutePath(), outputFilename))) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
            return new File(targetFolder.getAbsolutePath(), outputFilename);
        }
    }

    public File downloadItem(StacItem item, File targetFolder) throws IOException {
        File outputFolder = new File(targetFolder, item.getId());
        outputFolder.mkdirs();
        System.out.println("Downloading STAC item " + item.getId() + " to directory " + outputFolder.getAbsolutePath());
        for (String s : item.listAssetIds()) {
            Assets.Asset curAsset = item.getAsset(s);
            downloadAsset(curAsset, outputFolder);
        }
        return outputFolder;
    }

    private JSONArray getAllFeaturesRecursive(JSONObject rootObject, JSONArray returnArray) {
        JSONArray features = (JSONArray) rootObject.get("features");
        JSONArray links = (JSONArray) rootObject.get(StacComponent.LINKS);
        returnArray.addAll(features);
        for (Object o : links) {
            if (Objects.equals("next", ((JSONObject) o).get(StacComponent.REL))) {
                getAllFeaturesRecursive(
                        StacComponent.getJSONFromURLStatic((String) ((JSONObject) o).get(StacComponent.HREF)),
                        returnArray);
            }
        }
        return returnArray;
    }

    private JSONArray getAllFeatures(JSONObject rootObject) {
        return getAllFeaturesRecursive(rootObject, new JSONArray());
    }
}
