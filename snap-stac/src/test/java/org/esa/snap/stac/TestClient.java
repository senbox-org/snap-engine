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

import com.bc.ceres.test.LongTestRunner;
import org.esa.snap.stac.extensions.Assets;
import org.esa.snap.stac.internal.EstablishedModifiers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(LongTestRunner.class)
public class TestClient {
    private static final String catalogURL = "https://planetarycomputer.microsoft.com/api/stac/v1";

    StacClient client;

    @Before
    public void setup() {
        client = new StacClient(catalogURL, EstablishedModifiers.planetaryComputer());
    }

    @Test
    public void testCatalog() {
        Assert.assertNotNull(client.getCatalog());
    }

    @Test
    public void testSearch() throws Exception {
        StacItem[] results = client.search(
                new String[]{"sentinel-2-l2a", "landsat-c2-l2"},
                new double[]{-124.2751, 45.5469, -123.9613, 45.7458},
                "2020-01-01/2022-11-05");
        Assert.assertEquals(670, results.length);
    }

    @Test
    public void testSearchWorldCover() throws Exception {
        StacItem[] results = client.search(
                new String[]{"esa-worldcover"},
                new double[]{-124.2751, 45.5469, -123.9613, 45.7458},
                null);
        Assert.assertEquals(2, results.length);

        File tmpDir = new File("/tmp");
        if (!tmpDir.exists()) {
            return;
        }

        File folder = client.downloadItem(results[0], tmpDir);
        Assert.assertTrue(folder.exists());
        Assert.assertEquals(4, folder.listFiles().length);
    }

    @Test
    public void testSearchESRILULC() throws Exception {
        StacItem[] results = client.search(
                new String[]{"io-lulc"},
                new double[]{-124.2751, 45.5469, -123.9613, 45.7458},
                null);
        Assert.assertEquals(1, results.length);

        File tmpDir = new File("/tmp");
        if (!tmpDir.exists()) {
            return;
        }

        File folder = client.downloadItem(results[0], tmpDir);
        Assert.assertTrue(folder.exists());
        Assert.assertEquals(3, folder.listFiles().length);
    }

    @Test
    public void testStream() throws Exception {
        String itemURL = "https://planetarycomputer.microsoft.com/api/stac/v1/collections/landsat-c2-l2/items/LC09_L2SP_047028_20221031_02_T2";
        StacItem item = new StacItem(itemURL);
        InputStream is = client.streamAsset(item.getAsset(item.listAssetIds()[0]));
        Assert.assertNotNull(is);
        Assert.assertTrue(is.available() > 0);
    }

    @Test
    public void testDownloadAsset() throws Exception {
        File tmpDir = new File("/tmp");
        if (!tmpDir.exists()) {
            return;
        }

        String itemURL = "https://planetarycomputer.microsoft.com/api/stac/v1/collections/landsat-c2-l2/items/LC09_L2SP_047028_20221031_02_T2";
        StacItem item = new StacItem(itemURL);
        Assets.Asset asset = item.getAsset(item.listAssetIds()[0]);
        File outputFile = client.downloadAsset(asset, tmpDir);
        Assert.assertTrue(outputFile.exists());
        Assert.assertEquals(117392, outputFile.length());
        outputFile.delete();

    }

    //Very long running test.
//    @Test
//    public void testDownloadItem() throws Exception {
//        File tmpDir = new File("/tmp");
//        if(!tmpDir.exists()){
//            return;
//        }
//        String itemURL = "https://planetarycomputer.microsoft.com/api/stac/v1/collections/landsat-c2-l2/items/LC09_L2SP_047028_20221031_02_T2";
//        StacItem item = new StacItem(itemURL);
//        File folder = client.downloadItem(item, tmpDir);
//        Assert.assertTrue(folder.exists());
//    }


    @Test
    public void testCollectionList() {
        String[] collectionList = client.getCatalog().listCollections();
        assert (collectionList != null);
    }

    @Test
    public void testGetCollection() {
        String[] collectionList = client.getCatalog().listCollections();
        assert (collectionList != null);

        StacCollection collection = client.getCatalog().getCollection(collectionList[0]);
        assert (collection != null);
    }

    @Test
    public void testSearchParams() throws Exception {
        final String collectionName = "sentinel-2-l2a";
        Map<String, Object> params = new HashMap<>();
        params.put("bbox", "20.2201924985,43.6884447292,29.62654341,48.2208812526");
        params.put("datetime", "2022-05-01T00:00:00Z/2022-05-02T23:59:59Z");
        StacItem[] results = client.search(new String[]{collectionName}, params);
        assert (results != null);
    }

    @Test
    public void testPlanetaryComputer_Catalog() {
        final StacClient client = new StacClient(catalogURL);
        StacCatalog catalog = client.getCatalog();

        assertEquals("getId", "microsoft-pc", catalog.getId());
        assertEquals("getVersion", "1.0.0", catalog.getVersion());

        String[] collections = catalog.listCollections();
        for(String collection : collections) {
            System.out.println(collection);
        }

        StacCollection asterCollection = catalog.getCollection("aster-l1t");

        System.out.println(asterCollection.getJSON());

    }

}
