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
package org.esa.stac.internal;

// Author Alex McVittie, SkyWatch Space Applications Inc. January 2024
// The StacCatalog class allows you to interact with a specific catalog


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class StacCatalog implements StacComponent, STACUtils {
    private String rootURL;

    private JSONObject catalogJSON;

    private JSONObject collectionJSON;

    private String [] allCollections;

    private HashMap<String, String> collectionsWithURLs;

    private String title;
    public StacCatalog(String catalogURL) throws Exception {
        rootURL = catalogURL;

        catalogJSON = getJSONFromURL(catalogURL);
        collectionJSON = getJSONFromURL(catalogURL + "/collections");

        title = (String) catalogJSON.get("title");

        collectionsWithURLs = new HashMap<>();

        // Store list of collections
        allCollections = new String[((JSONArray) collectionJSON.get("collections")).size()];
        for (int x = 0; x < ((JSONArray) collectionJSON.get("collections")).size(); x++){
            JSONObject curCollection = (JSONObject) ((JSONArray) collectionJSON.get("collections")).get(x);
            allCollections[x] = (String) curCollection.get("id");
            for (Object o: (JSONArray) curCollection.get("links")){
                if (Objects.equals("self", ((JSONObject) o).get("rel"))){
                    collectionsWithURLs.put((String) curCollection.get("id"), (String) ((JSONObject) o).get("href"));
                }
            }
        }
        Arrays.sort(allCollections);
    }


    @Override
    public JSONObject getJSON() {
        return catalogJSON;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getSelfURL() {
        return null;
    }

    @Override
    public String getRootURL() {
        return rootURL;
    }

    public String getTitle() {
        return this.title;
    }

    public String [] listCollections(){
        return this.allCollections;
    }
    public int getNumCollections(){
        return this.allCollections.length;
    }

    public StacCollection getCollection(String collectionName) throws Exception {
        if (collectionsWithURLs.containsKey(collectionName)){
            return new StacCollection(collectionsWithURLs.get(collectionName));
        }
        return null;
    }

    public boolean containsCollection(String collectionName){
        for (String collection : allCollections){
            if (Objects.equals(collectionName, collection)){
                return true;
            }
        }
        return false;
    }


}
