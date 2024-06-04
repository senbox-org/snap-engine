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
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TestAssets {

    @Test
    public void testAssets() {
        final JSONObject assetsJSON = new JSONObject();

        Assets.addAsset(assetsJSON, "name", "title", "description",
                "href", "type", "role");
        JSONObject asset = (JSONObject) assetsJSON.get("name");
        assertEquals("title", asset.get(Assets.title));
        assertEquals("description", asset.get(Assets.description));
        assertEquals("href", asset.get(Assets.href));
        assertEquals("role", asset.get(Assets.role));
        assertEquals("type", asset.get(Assets.type));

        Map<String, Assets.Asset> assetMap = Assets.getImageAssets(assetsJSON);
        assertEquals(0, assetMap.size());

        Assets.addAsset(assetsJSON, "name2", "title2", "description2",
                "href2", Assets.type_image_tiff, "role2");

        assetMap = Assets.getImageAssets(assetsJSON);
        assertEquals(1, assetMap.size());

        Assets.Asset asset2 = assetMap.get("name2");
        assertEquals("name2", asset2.name);
        assertEquals("title2", asset2.title);
        assertEquals("description2", asset2.description);
        assertEquals("href2", asset2.href);
        assertEquals("role2", asset2.role);
        assertEquals(Assets.type_image_tiff, asset2.type);
    }
}
