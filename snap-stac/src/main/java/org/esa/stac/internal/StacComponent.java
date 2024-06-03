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

public interface StacComponent {

    String stac_version = "stac_version";

    String id = "id";
    String description = "description";
    String title = "title";
    String type = "type";
    String license = "license";
    String providers = "providers";

    String assets = "assets";

    String links = "links";
    String rel = "rel";
    String href = "href";
    String self = "self";
    String root = "root";
    String parent = "parent";
    String child = "child";
    String item = "item";

    JSONObject getJSON();


    String getId();

    String getSelfURL();

    String getRootURL();

    default String getURL(final JSONObject json, final String relType) {
        if (json.containsKey(links)) {
            final JSONArray linksArray = (JSONArray) json.get(links);
            for (Object o : linksArray) {
                final JSONObject link = (JSONObject) o;
                String relStr = (String) link.get(rel);
                if (relStr.equals(relType)) {
                    return (String) link.get(href);
                }
            }
        }
        return null;
    }
}
