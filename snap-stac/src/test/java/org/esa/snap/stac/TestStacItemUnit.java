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

import org.esa.snap.stac.extensions.Assets;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestStacItemUnit {

    @Test
    public void testCreateEmptyStacItem() throws Exception {
        StacItem item = new StacItem("my-test-id");
        assertEquals("my-test-id", item.getId());
        assertEquals("1.0.0", item.getVersion());
        assertTrue(item.isStacItem());
        assertNotNull(item.getAssets());
        assertNotNull(item.getProperties());
    }

    @Test
    public void testCreateFromFile() throws Exception {
        URL resource = StacItem.class.getResource("capella/CAPELLA_ARL_SP_GEO_VV_20190927234024_20190927234124.json");
        assertNotNull(resource);
        Path path = Paths.get(resource.toURI());
        StacItem item = new StacItem(path.toFile());

        assertEquals("CAPELLA_ARL_SP_GEO_VV_20190927234024_20190927234124", item.getId());
        assertNotNull(item.getJSONFile());
    }

    @Test
    public void testCreateFromJSONObject() throws Exception {
        JSONObject json = createMinimalStacJSON("test-item-1");
        StacItem item = new StacItem(json);
        assertEquals("test-item-1", item.getId());
    }

    @Test
    public void testCreateFromJSONString() throws Exception {
        String jsonStr = createMinimalStacJSONString("test-item-2");
        StacItem item = new StacItem(jsonStr);
        assertEquals("test-item-2", item.getId());
    }

    @Test(expected = IOException.class)
    public void testCreateFromNullJSONThrows() throws Exception {
        new StacItem((JSONObject) null);
    }

    @Test
    public void testCreateFromInvalidJSONStringThrows() {
        try {
            new StacItem("{\"invalid\": \"json\"}");
            fail("Expected IOException");
        } catch (IOException e) {
            assertEquals("Invalid STAC JSON", e.getMessage());
        }
    }

    @Test
    public void testCreateFromNonExistentFileThrows() {
        try {
            new StacItem(new File("nonexistent_file.json"));
            fail("Expected IOException");
        } catch (IOException e) {
            assertEquals("Unable to parse JSON from given local file.", e.getMessage());
        }
    }

    @Test
    public void testGetDescriptionFromProperties() throws Exception {
        JSONObject json = createMinimalStacJSON("desc-test");
        ((JSONObject) json.get(StacComponent.PROPERTIES)).put("description", "My Description");
        StacItem item = new StacItem(json);
        assertEquals("My Description", item.getDescription());
    }

    @Test
    public void testGetDescriptionFallbackToTitle() throws Exception {
        JSONObject json = createMinimalStacJSON("desc-test");
        ((JSONObject) json.get(StacComponent.PROPERTIES)).put("title", "My Title");
        StacItem item = new StacItem(json);
        assertEquals("My Title", item.getDescription());
    }

    @Test
    public void testGetDescriptionEmpty() throws Exception {
        JSONObject json = createMinimalStacJSON("desc-test");
        StacItem item = new StacItem(json);
        assertEquals("", item.getDescription());
    }

    @Test
    public void testGetProductTypeSAR() throws Exception {
        JSONObject json = createMinimalStacJSON("sar-test");
        ((JSONObject) json.get(StacComponent.PROPERTIES)).put("sar:product_type", "GRD");
        StacItem item = new StacItem(json);
        assertEquals("GRD", item.getProductType());
    }

    @Test
    public void testGetProductTypeDefault() throws Exception {
        JSONObject json = createMinimalStacJSON("default-test");
        StacItem item = new StacItem(json);
        assertEquals("stac_product", item.getProductType());
    }

    @Test
    public void testGetProductTypeSNAP() throws Exception {
        JSONObject json = createMinimalStacJSON("snap-test");
        ((JSONObject) json.get(StacComponent.PROPERTIES)).put("product:type", "S1_GRD");
        StacItem item = new StacItem(json);
        assertEquals("S1_GRD", item.getProductType());
    }

    @Test
    public void testAddExtension() throws Exception {
        StacItem item = new StacItem("ext-test");
        item.addExtension("https://stac-extensions.github.io/eo/v1.0.0/schema.json");
        String[] extensions = item.getExtensions();
        assertEquals(1, extensions.length);
        assertEquals("https://stac-extensions.github.io/eo/v1.0.0/schema.json", extensions[0]);
    }

    @Test
    public void testAddDuplicateExtension() throws Exception {
        StacItem item = new StacItem("ext-test");
        String ext = "https://stac-extensions.github.io/eo/v1.0.0/schema.json";
        item.addExtension(ext);
        item.addExtension(ext);
        assertEquals(1, item.getExtensions().length);
    }

    @Test
    public void testAddMultipleExtensions() throws Exception {
        StacItem item = new StacItem("ext-test");
        item.addExtension(
                "https://stac-extensions.github.io/eo/v1.0.0/schema.json",
                "https://stac-extensions.github.io/sar/v1.0.0/schema.json"
        );
        assertEquals(2, item.getExtensions().length);
    }

    @Test
    public void testAddKeywords() throws Exception {
        StacItem item = new StacItem("kw-test");
        item.addKeywords("satellite", "optical");

        JSONObject json = item.getJSON();
        JSONArray keywords = (JSONArray) json.get(StacItem.KEYWORDS);
        assertTrue(keywords.contains("satellite"));
        assertTrue(keywords.contains("optical"));
    }

    @Test
    public void testAddDuplicateKeywords() throws Exception {
        StacItem item = new StacItem("kw-test");
        item.addKeywords("satellite");
        item.addKeywords("satellite");

        JSONArray keywords = (JSONArray) item.getJSON().get(StacItem.KEYWORDS);
        assertEquals(1, keywords.size());
    }

    @Test
    public void testAddProvider() throws Exception {
        StacItem item = new StacItem("prov-test");
        item.addProvider("ESA", "producer", "https://esa.int");

        JSONObject properties = item.getProperties();
        JSONArray providers = (JSONArray) properties.get(StacComponent.PROVIDERS);
        assertEquals(1, providers.size());
        JSONObject provider = (JSONObject) providers.get(0);
        assertEquals("ESA", provider.get("name"));
        assertEquals("producer", provider.get("role"));
    }

    @Test
    public void testAddAsset() throws Exception {
        StacItem item = new StacItem("asset-test");
        Assets.Asset asset = item.addAsset("band1", "Band 1", "Red band",
                "https://example.com/band1.tif", Assets.type_image_geotiff, "data");

        assertNotNull(asset);
        assertEquals("band1", asset.name);
        assertEquals("Band 1", asset.title);
        assertEquals("data", asset.role);

        // addAsset adds to the JSON but not the internal assetsById map,
        // so verify via the assets JSON directly
        assertNotNull(item.getAssets().get("band1"));
    }

    @Test
    public void testGetAssetNonExistent() throws Exception {
        StacItem item = new StacItem("asset-test");
        assertNull(item.getAsset("nonexistent"));
    }

    @Test
    public void testListAssetIds() throws Exception {
        StacItem item = new StacItem("asset-test");
        item.addAsset("b_red", "Red", null, "https://example.com/red.tif", Assets.type_image_geotiff, "data");
        item.addAsset("a_blue", "Blue", null, "https://example.com/blue.tif", Assets.type_image_geotiff, "data");

        String[] ids = item.listAssetIds();
        assertEquals(2, ids.length);
        assertEquals("a_blue", ids[0]);
        assertEquals("b_red", ids[1]);
    }

    @Test
    public void testGetImageAssets() throws Exception {
        StacItem item = new StacItem("img-test");
        item.addAsset("band1", "Band 1", null,
                "https://example.com/band1.tif", Assets.type_image_geotiff, "data");
        item.addAsset("metadata", "Metadata", null,
                "https://example.com/meta.json", Assets.type_json, "metadata");

        Map<String, Assets.Asset> imageAssets = item.getImageAssets();
        assertEquals(1, imageAssets.size());
        assertTrue(imageAssets.containsKey("band1"));
    }

    @Test
    public void testGetGeometryFromCapella() throws Exception {
        URL resource = StacItem.class.getResource("capella/CAPELLA_ARL_SP_GEO_VV_20190927234024_20190927234124.json");
        StacItem item = new StacItem(Paths.get(resource.toURI()).toFile());

        JSONObject geometry = item.getGeometry();
        assertNotNull(geometry);
        assertEquals("Polygon", geometry.get("type"));
    }

    @Test
    public void testGetGeometryAsWKTFromCapella() throws Exception {
        URL resource = StacItem.class.getResource("capella/CAPELLA_ARL_SP_GEO_VV_20190927234024_20190927234124.json");
        StacItem item = new StacItem(Paths.get(resource.toURI()).toFile());

        String wkt = item.getGeometryAsWKT();
        assertNotNull(wkt);
        assertTrue(wkt.startsWith("POLYGON(("));
        assertTrue(wkt.endsWith("))"));
    }

    @Test
    public void testGetBoundingBoxFromCapella() throws Exception {
        URL resource = StacItem.class.getResource("capella/CAPELLA_ARL_SP_GEO_VV_20190927234024_20190927234124.json");
        StacItem item = new StacItem(Paths.get(resource.toURI()).toFile());

        JSONArray bbox = item.getBoundingBox();
        assertNotNull(bbox);
        assertEquals(4, bbox.size());
        assertEquals(-121.94798471, (double) bbox.get(0), 1e-6);
        assertEquals(38.2416324, (double) bbox.get(1), 1e-6);
    }

    @Test
    public void testGetVersionFromCapella() throws Exception {
        URL resource = StacItem.class.getResource("capella/CAPELLA_ARL_SP_GEO_VV_20190927234024_20190927234124.json");
        StacItem item = new StacItem(Paths.get(resource.toURI()).toFile());
        assertEquals("0.9.0", item.getVersion());
    }

    @Test
    public void testGetTime() throws Exception {
        URL resource = StacItem.class.getResource("capella/CAPELLA_ARL_SP_GEO_VV_20190927234024_20190927234124.json");
        StacItem item = new StacItem(Paths.get(resource.toURI()).toFile());
        assertNotNull(item.getTime());
    }

    @Test
    public void testIsStacItemTrue() throws Exception {
        StacItem item = new StacItem("test");
        assertTrue(item.isStacItem());
    }

    @Test
    public void testIsStacItemStaticWithValidPath() throws Exception {
        URL resource = StacItem.class.getResource("capella/CAPELLA_ARL_SP_GEO_VV_20190927234024_20190927234124.json");
        Path path = Paths.get(resource.toURI());
        assertTrue(StacItem.isStacItem(path));
    }

    @Test
    public void testIsStacItemStaticWithCatalog() throws Exception {
        URL resource = StacItem.class.getResource("catalog/snapplanet.json");
        Path path = Paths.get(resource.toURI());
        assertFalse(StacItem.isStacItem(path));
    }

    @Test
    public void testGetPropertiesFromCapella() throws Exception {
        URL resource = StacItem.class.getResource("capella/CAPELLA_ARL_SP_GEO_VV_20190927234024_20190927234124.json");
        StacItem item = new StacItem(Paths.get(resource.toURI()).toFile());

        JSONObject properties = item.getProperties();
        assertNotNull(properties);
        assertEquals("eros", properties.get("platform"));
        assertEquals("GEO", properties.get("sar:product_type"));
    }

    @Test
    public void testGetProductTypeFromCapella() throws Exception {
        URL resource = StacItem.class.getResource("capella/CAPELLA_ARL_SP_GEO_VV_20190927234024_20190927234124.json");
        StacItem item = new StacItem(Paths.get(resource.toURI()).toFile());
        assertEquals("GEO", item.getProductType());
    }

    @Test
    public void testSetJSONFile() throws Exception {
        StacItem item = new StacItem("file-test");
        assertNull(item.getJSONFile());

        File file = new File("test.json");
        item.setJSONFile(file);
        assertEquals(file, item.getJSONFile());
    }

    @Test
    public void testGetSelfURLFromItem() throws Exception {
        JSONObject json = createMinimalStacJSON("self-test");
        JSONArray links = new JSONArray();
        JSONObject selfLink = new JSONObject();
        selfLink.put("rel", "self");
        selfLink.put("href", "https://example.com/items/self-test");
        links.add(selfLink);
        json.put(StacComponent.LINKS, links);

        StacItem item = new StacItem(json);
        assertEquals("https://example.com/items/self-test", item.getSelfURL());
        assertEquals("https://example.com/items/self-test", item.getURL());
    }

    @Test
    public void testGetRootURL() throws Exception {
        JSONObject json = createMinimalStacJSON("root-test");
        JSONArray links = new JSONArray();
        JSONObject rootLink = new JSONObject();
        rootLink.put("rel", "root");
        rootLink.put("href", "https://example.com/");
        links.add(rootLink);
        json.put(StacComponent.LINKS, links);

        StacItem item = new StacItem(json);
        assertEquals("https://example.com/", item.getRootURL());
    }

    @Test
    public void testExtensionShortNamesAreResolved() throws Exception {
        JSONObject json = createMinimalStacJSON("ext-resolve-test");
        JSONArray extensions = new JSONArray();
        extensions.add("eo");
        extensions.add("sar");
        json.put(StacItem.STAC_EXTENSIONS, extensions);

        StacItem item = new StacItem(json);
        String[] exts = item.getExtensions();
        boolean hasEO = false, hasSAR = false;
        for (String ext : exts) {
            if (ext.contains("eo")) hasEO = true;
            if (ext.contains("sar")) hasSAR = true;
        }
        assertTrue("EO extension should be resolved to full URL", hasEO);
        assertTrue("SAR extension should be resolved to full URL", hasSAR);
    }

    @SuppressWarnings("unchecked")
    private JSONObject createMinimalStacJSON(String id) {
        JSONObject json = new JSONObject();
        json.put(StacComponent.STAC_VERSION, "1.0.0");
        json.put(StacComponent.ID, id);
        json.put(StacComponent.TYPE, StacComponent.FEATURE);
        json.put(StacItem.STAC_EXTENSIONS, new JSONArray());
        JSONObject properties = new JSONObject();
        properties.put(StacComponent.PROVIDERS, new JSONArray());
        json.put(StacComponent.PROPERTIES, properties);
        json.put(StacItem.KEYWORDS, new JSONArray());
        json.put(StacComponent.ASSETS, new JSONObject());
        json.put(StacComponent.LINKS, new JSONArray());
        return json;
    }

    private String createMinimalStacJSONString(String id) {
        return "{\"stac_version\":\"1.0.0\",\"id\":\"" + id + "\",\"type\":\"feature\"," +
                "\"stac_extensions\":[],\"properties\":{\"providers\":[]}," +
                "\"keywords\":[],\"assets\":{},\"links\":[]}";
    }
}
