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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public interface StacComponent {

    String STAC_VERSION = "stac_version";

    String ID = "id";
    String DESCRIPTION = "description";
    String TITLE = "title";
    String TYPE = "type";
    String FEATURE = "Feature";

    String STAC_EXTENSIONS = "stac_extensions";
    String COLLECTION = "collection";
    String KEYWORDS = "keywords";

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

    int HTTP_MAX_ATTEMPTS = 3;
    int HTTP_CONNECT_TIMEOUT_MS = 15_000;
    int HTTP_READ_TIMEOUT_MS = 90_000;

    static JSONObject getJSONFromURLStatic(final String jsonURL) {
        final URL url;
        try {
            url = new URL(jsonURL);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        IOException lastError = null;
        for (int attempt = 1; attempt <= HTTP_MAX_ATTEMPTS; attempt++) {
            HttpURLConnection conn = null;
            try {
                URLConnection raw = url.openConnection();
                raw.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS);
                raw.setReadTimeout(HTTP_READ_TIMEOUT_MS);
                raw.setRequestProperty("Accept", "application/json");
                if (raw instanceof HttpURLConnection) {
                    conn = (HttpURLConnection) raw;
                    int code = conn.getResponseCode();
                    if (code == 408 || code == 429 || code >= 500) {
                        lastError = new IOException("HTTP " + code + " for URL: " + jsonURL);
                        sleepBeforeRetry(attempt);
                        continue;
                    }
                }
                try (InputStream in = (InputStream) raw.getContent()) {
                    String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    return (JSONObject) new JSONParser().parse(content);
                }
            } catch (SocketTimeoutException e) {
                // Server is genuinely slow, not flaky - retrying just multiplies the wait.
                throw new RuntimeException(e);
            } catch (IOException e) {
                lastError = e;
                sleepBeforeRetry(attempt);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        throw new RuntimeException(lastError != null
                ? lastError
                : new IOException("Failed to fetch " + jsonURL));
    }

    static void sleepBeforeRetry(final int attempt) {
        if (attempt >= HTTP_MAX_ATTEMPTS) {
            return;
        }
        try {
            Thread.sleep(1000L * (1L << (attempt - 1))); // 1s, 2s, 4s
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    default JSONObject getJSONFromURL(final String jsonURL) {
        return getJSONFromURLStatic(jsonURL);
    }
}
