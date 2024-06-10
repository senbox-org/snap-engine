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
// The StacClient class acts as a way to search for STAC assets, and download these
// STAC items and assets.

import org.esa.snap.stac.internal.DownloadModifier;
import org.esa.snap.stac.internal.EstablishedModifiers;
import org.esa.snap.stac.internal.StacComponent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;


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

    public StacCatalog getCatalog() {
        return this.catalog;
    }

    // Performs a search and returns an array of STAC Items from the server that match the search
    public StacItem[] search(final String[] collections, final double[] bbox, String datetime) throws Exception {
        String searchEndpoint = stacURL + "/search?";
        String bboxStr = bbox[0] + "," + bbox[1] + "," + bbox[2] + "," + bbox[3];
        String validCollections = "";
        for (String collectionName : collections) {
            if (this.catalog.containsCollection(collectionName)) {
                validCollections = validCollections + collectionName + ",";
            }
        }
        if (Objects.equals(validCollections, "")) {
            return new StacItem[0];
        }
        validCollections = validCollections.substring(0, validCollections.length() - 1);
        String query = searchEndpoint + "collections=" + validCollections +
                "&bbox=" + bboxStr;
        if(datetime != null) query = query + "&datetime=" + datetime;

        JSONObject queryResults = StacComponent.getJSONFromURLStatic(query);
        JSONArray jsonFeatures = getAllFeatures(queryResults);
        StacItem[] items = new StacItem[jsonFeatures.size()];
        for (int x = 0; x < jsonFeatures.size(); x++) {
            items[x] = new StacItem((JSONObject) jsonFeatures.get(x));
        }
        return items;
    }

    // Search using a defined GeoJSON polygon.
    // TODO implement.
    //public void search(String [] collections, JSONObject intersects, String datetime){
    //
    //}

    public String signURL(StacItem.StacAsset asset) {
        if (signDownloads) {
            return downloadModifier.signURL(asset.getURL());
        } else {
            return asset.getURL();
        }
    }

    public InputStream streamAsset(StacItem.StacAsset asset) throws IOException {
        String downloadURL = signURL(asset);
        InputStream inputStream = new URL(downloadURL).openStream();
        return inputStream;
    }

    public File downloadAsset(StacItem.StacAsset asset, File targetFolder) throws IOException {
        targetFolder.mkdirs();
        String outputFilename = asset.getFileName();

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
            StacItem.StacAsset curAsset = item.getAsset(s);
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
