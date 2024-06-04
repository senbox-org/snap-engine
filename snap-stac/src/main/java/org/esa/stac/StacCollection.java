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

// TODO fully implement.

import org.esa.stac.internal.StacComponent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Objects;

public class StacCollection implements StacComponent {

    private final JSONObject collectionJSON;

    private final String collectionURL;

    private final String collectionItemURL;

    public StacCollection(final String collectionURL) {
        this.collectionURL = collectionURL;
        collectionJSON = getJSONFromURL(collectionURL);
        collectionItemURL = getCollectionItemURL();
    }

    public String getCollectionURL() {
        return collectionURL;
    }

    private String getCollectionItemURL() {
        JSONArray links = (JSONArray) collectionJSON.get(LINKS);
        for (Object o : links) {
            JSONObject j = ((JSONObject) o);
            if (j.containsKey(REL) && Objects.equals(j.get(REL), "items")) {
                return (String) j.get(HREF);
            }
        }
        return null;
    }

    @Override
    public JSONObject getJSON() {
        return collectionJSON;
    }

    @Override
    public String getId() {
        return (String) collectionJSON.get(ID);
    }

    @Override
    public String getSelfURL() {
        return getURL(collectionJSON, SELF);
    }

    @Override
    public String getRootURL() {
        return getURL(collectionJSON, ROOT);
    }

/*
    public StacItem[] getAllItems() throws Exception {
        // Get the initial items
        JSONObject items = getJSONFromURL(collectionItemURL);
        JSONArray features = getAllFeatures(items);



        return null;
    }

    public JSONObject getAssets(){
        return null;
    }

 */


}
