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
package org.esa.stac.extensions;

import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
public class Provider implements StacExtension {

    private Provider() {}

    public static final String schema = "https://stac-extensions.github.io/provider/v1.0.0/schema.json";

    public static final String name = "name";
    public static final String role = "role";
    public static final String url = "url";

    public static final String LICENSOR = "licensor";
    public static final String PRODUCER = "producer";
    public static final String PROCESSOR = "processor";
    public static final String HOST = "host";

    public static JSONObject create(final String nameVal, final String roleVal, final String urlVal) {
        final JSONObject provider = new JSONObject();
        provider.put(name, nameVal);
        provider.put(role, roleVal);
        provider.put(url, urlVal);
        return provider;
    }
}
