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
package org.esa.snap.stac.extensions;

import com.bc.ceres.annotation.STTM;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unchecked")
public class TestAssets {

    @Test
    public void testAddAssetAndRetrieve() {
        final JSONObject assetsJSON = new JSONObject();

        Assets.addAsset(assetsJSON, "name", "title", "description",
                "href", "type", new String[]{"role"});
        JSONObject asset = (JSONObject) assetsJSON.get("name");
        assertEquals("title", asset.get(Assets.title));
        assertEquals("description", asset.get(Assets.description));
        assertEquals("href", asset.get(Assets.href));
        assertEquals("type", asset.get(Assets.type));

        JSONArray roles = (JSONArray) asset.get(Assets.roles);
        assertNotNull(roles);
        assertTrue(roles.contains("role"));
    }

    @Test
    public void testGetImageAssetsFiltersNonImageTypes() {
        final JSONObject assetsJSON = new JSONObject();
        Assets.addAsset(assetsJSON, "name", "title", "description",
                "href", "type", new String[]{"data"});

        Map<String, Assets.Asset> assetMap = Assets.getImageAssets(assetsJSON);
        assertEquals(0, assetMap.size());
    }

    @Test
    public void testGetImageAssetsIncludesImageTypes() {
        final JSONObject assetsJSON = new JSONObject();

        Assets.addAsset(assetsJSON, "tiff", "TIFF", null,
                "https://example.com/file.tif", Assets.type_image_tiff, new String[]{"data"});
        Assets.addAsset(assetsJSON, "geotiff", "GeoTIFF", null,
                "https://example.com/file.tif", Assets.type_image_geotiff, new String[]{"data"});
        Assets.addAsset(assetsJSON, "cog", "COG", null,
                "https://example.com/file.tif", Assets.type_image_cog, new String[]{"data"});
        Assets.addAsset(assetsJSON, "jp2", "JP2", null,
                "https://example.com/file.jp2", Assets.type_image_jp2, new String[]{"data"});
        Assets.addAsset(assetsJSON, "hdf5", "HDF5", null,
                "https://example.com/file.h5", Assets.type_hdf5, new String[]{"data"});

        Map<String, Assets.Asset> assetMap = Assets.getImageAssets(assetsJSON);
        assertEquals(5, assetMap.size());
    }

    @Test
    public void testGetImageAssetsFiltersPreviews() {
        final JSONObject assetsJSON = new JSONObject();
        Assets.addAsset(assetsJSON, "preview", "Preview", null,
                "https://example.com/preview.tif", Assets.type_image_geotiff, new String[]{"data"});
        Assets.addAsset(assetsJSON, "overview_band", "Overview", null,
                "https://example.com/overview.tif", Assets.type_image_geotiff, new String[]{"data"});
        Assets.addAsset(assetsJSON, "thumbnail_small", "Thumb", null,
                "https://example.com/thumb.tif", Assets.type_image_geotiff, new String[]{"data"});

        Map<String, Assets.Asset> assetMap = Assets.getImageAssets(assetsJSON);
        assertEquals(0, assetMap.size());
    }

    @Test
    @STTM("SNAP-4187")
    public void testGetImageAssetsFiltersAuxiliaryRoles() {
        final JSONObject assetsJSON = new JSONObject();
        Assets.addAsset(assetsJSON, "cloud_data", "Cloud", null,
                "https://example.com/cloud.tif", Assets.type_image_geotiff, new String[]{"cloud"});
        Assets.addAsset(assetsJSON, "mask_data", "Mask", null,
                "https://example.com/mask.tif", Assets.type_image_geotiff, new String[]{"mask"});
        Assets.addAsset(assetsJSON, "aux_data", "Aux", null,
                "https://example.com/aux.tif", Assets.type_image_geotiff, new String[]{"auxdata"});

        Map<String, Assets.Asset> assetMap = Assets.getImageAssets(assetsJSON);
        assertEquals(0, assetMap.size());
    }

    @Test
    public void testGetImageAssetsRetainsDataAssets() {
        final JSONObject assetsJSON = new JSONObject();
        Assets.addAsset(assetsJSON, "band1", "Band 1", null,
                "https://example.com/b1.tif", Assets.type_image_geotiff, new String[]{"data"});
        Assets.addAsset(assetsJSON, "metadata", "Meta", null,
                "https://example.com/meta.json", Assets.type_json, new String[]{"metadata"});

        Map<String, Assets.Asset> assetMap = Assets.getImageAssets(assetsJSON);
        assertEquals(1, assetMap.size());
        assertTrue(assetMap.containsKey("band1"));
    }

    @Test
    public void testAssetConstructorFromJSON() {
        JSONObject json = new JSONObject();
        json.put(Assets.href, "https://example.com/data/file.tif");
        json.put(Assets.title, "My Asset");
        json.put(Assets.description, "A test asset");
        json.put(Assets.type, Assets.type_image_geotiff);
        JSONArray roles = new JSONArray();
        roles.add("data");
        json.put(Assets.roles, roles);

        Assets.Asset asset = new Assets.Asset("my_asset", json);
        assertEquals("my_asset", asset.name);
        assertEquals("my_asset", asset.getId());
        assertEquals("https://example.com/data/file.tif", asset.href);
        assertEquals("https://example.com/data/file.tif", asset.getURL());
        assertEquals("My Asset", asset.title);
        assertEquals("A test asset", asset.description);
        assertEquals(Assets.type_image_geotiff, asset.type);
        assertNotNull(asset.roles);
        assertTrue(asset.roles.contains("data"));
        assertEquals("file.tif", asset.getFileName());
    }

    @Test
    public void testAssetConstructorFromJSONWithSingleRole() {
        JSONObject json = new JSONObject();
        json.put(Assets.href, "https://example.com/file.tif");
        json.put(Assets.role, "data");

        Assets.Asset asset = new Assets.Asset("band", json);
        assertNotNull(asset.roles);
        assertTrue(asset.roles.contains("data"));
    }

    @Test
    public void testAssetConstructorFromValues() {
        Assets.Asset asset = new Assets.Asset("band1", "Red Band", "Red reflectance",
                "https://example.com/red.tif", Assets.type_image_cog, new String[]{"data", "reflectance"});

        assertEquals("band1", asset.name);
        assertEquals("Red Band", asset.title);
        assertEquals("Red reflectance", asset.description);
        assertEquals("https://example.com/red.tif", asset.href);
        assertEquals(Assets.type_image_cog, asset.type);
        assertNotNull(asset.roles);
        assertTrue(asset.roles.contains("data"));
        assertTrue(asset.roles.contains("reflectance"));
        assertEquals("red.tif", asset.getFileName());
    }

    @Test
    public void testAssetAddRasterBand() {
        Assets.Asset asset = new Assets.Asset("band1", "Band 1", null,
                "https://example.com/band.tif", Assets.type_image_geotiff, new String[]{"data"});

        asset.addRasterBand("red", "uint16", "reflectance", 0, 1000, 2000);

        JSONArray bands = asset.getBands();
        assertNotNull(bands);
        assertEquals(1, bands.size());

        JSONObject band = (JSONObject) bands.get(0);
        assertEquals("red", band.get(Raster.name));
        assertEquals("uint16", band.get(Raster.data_type));
        assertEquals("reflectance", band.get(Raster.unit));
        assertEquals(0.0, (double) band.get(Raster.nodata), 1e-10);
    }

    @Test
    public void testAssetAddRasterBandDuplicateNameUpdates() {
        Assets.Asset asset = new Assets.Asset("band1", "Band 1", null,
                "https://example.com/band.tif", Assets.type_image_geotiff, new String[]{"data"});

        asset.addRasterBand("red", "uint16", "reflectance", 0, 1000, 2000);
        asset.addRasterBand("red", "float32", "sr", -9999, 1000, 2000);

        JSONArray bands = asset.getBands();
        assertEquals(1, bands.size());

        JSONObject band = (JSONObject) bands.get(0);
        assertEquals("float32", band.get(Raster.data_type));
        assertEquals("sr", band.get(Raster.unit));
    }

    @Test
    public void testAssetGetBandsEmpty() {
        Assets.Asset asset = new Assets.Asset("band1", "Band 1", null,
                "https://example.com/band.tif", Assets.type_image_geotiff, new String[]{"data"});
        JSONArray bands = asset.getBands();
        assertNotNull(bands);
        assertEquals(0, bands.size());
    }

    @Test
    public void testAssetGetBandsFromEO() {
        JSONObject json = new JSONObject();
        json.put(Assets.href, "https://example.com/file.tif");
        JSONArray eoBands = new JSONArray();
        JSONObject band = new JSONObject();
        band.put("name", "red");
        eoBands.add(band);
        json.put(EO.bands, eoBands);

        Assets.Asset asset = new Assets.Asset("test", json);
        JSONArray bands = asset.getBands();
        assertEquals(1, bands.size());
    }

    @Test
    public void testAssetConstructorMinimalJSON() {
        JSONObject json = new JSONObject();
        Assets.Asset asset = new Assets.Asset("minimal", json);
        assertEquals("minimal", asset.name);
        assertEquals("minimal", asset.getId());
    }

    @Test
    public void testAssetNullType() {
        Assets.Asset asset = new Assets.Asset("test", "Title", null,
                "https://example.com/file.tif", null, new String[]{});
        assertEquals("test", asset.name);
        assertEquals("Title", asset.title);
    }
}
