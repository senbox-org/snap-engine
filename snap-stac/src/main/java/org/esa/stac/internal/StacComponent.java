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

public interface StacComponent {

    String STAC_VERSION = "stac_version";

    String ID = "id";
    String DESCRIPTION = "description";
    String TITLE = "title";
    String TYPE = "type";
    String FEATURE = "feature";

    String BBOX = "bbox";
    String GEOMETRY = "geometry";
    String COORDINATES = "coordinates";

    String PROPERTIES = "properties";
    String ASSETS = "assets";
    String LICENSE = "license";
    String PROVIDERS = "providers";

    String LINKS = "links";
    String REL = "rel";
    String HREF = "href";
    String SELF = "self";
    String ROOT = "root";
    String PARENT = "parent";
    String CHILD = "child";
    String ITEM = "item";

    JSONObject getJSON();

    String getId();

    String getSelfURL();

    String getRootURL();

    default String getURL(final JSONObject json, final String relType) {
        if (json.containsKey(LINKS)) {
            final JSONArray linksArray = (JSONArray) json.get(LINKS);
            for (Object o : linksArray) {
                final JSONObject link = (JSONObject) o;
                String relStr = (String) link.get(REL);
                if (relStr.equals(relType)) {
                    return (String) link.get(HREF);
                }
            }
        }
        return null;
    }

    static JSONObject getJSONFromURLStatic(String jsonURL) {
        try {
            URL url = new URL(jsonURL);
            URLConnection request = url.openConnection();
            request.connect();
            String content = new String(
                    ((InputStream) request.getContent()).readAllBytes(), StandardCharsets.UTF_8
            );
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(content);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    default JSONObject getJSONFromURL(String jsonURL) {
        return getJSONFromURLStatic(jsonURL);
    }
}
