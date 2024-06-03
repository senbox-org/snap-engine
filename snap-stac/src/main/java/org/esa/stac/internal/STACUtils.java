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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public interface STACUtils {

    static JSONObject getJSONFromURLStatic(String jsonURL){
        try {
            URL url = new URL(jsonURL);
            URLConnection request = url.openConnection();
            request.connect();
            String content = new String(
                    ((InputStream) request.getContent()).readAllBytes(),
                    StandardCharsets.UTF_8
            );
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(content);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    default JSONObject getJSONFromURL(String jsonURL){
        return getJSONFromURLStatic(jsonURL);
    }

    default JSONArray getAllFeaturesRecursive(JSONObject rootObject, JSONArray returnArray){
        JSONArray features = (JSONArray) rootObject.get("features");
        JSONArray links = (JSONArray) rootObject.get("links");
        returnArray.addAll(features);
        for (Object o : links){
            if (Objects.equals("next", ((JSONObject) o).get("rel"))){
                getAllFeaturesRecursive(
                        getJSONFromURL((String) ((JSONObject) o).get("href")),
                        returnArray);
            }
        }
        return returnArray;
    }

    default JSONArray getAllFeatures(JSONObject rootObject){
        return getAllFeaturesRecursive(rootObject, new JSONArray());
    }

}
