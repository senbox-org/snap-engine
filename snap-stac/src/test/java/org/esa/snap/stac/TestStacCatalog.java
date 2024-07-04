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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestStacCatalog {
    private final String catalogURL = "https://planetarycomputer.microsoft.com/api/stac/v1";
    StacCatalog catalog;

    @Before
    public void setup() {
        catalog = new StacCatalog(catalogURL);
    }

    @Test
    public void testName() {

        Assert.assertEquals("Microsoft Planetary Computer STAC API", catalog.getTitle());
    }

    @Test
    public void testListCollections() {
        String[] collections = catalog.listCollections();

        // Using a greater than or equal check here as MSFT could add more collections, or remove some.
        // At the time of writing these tests, 122 collections were hosted.
        Assert.assertTrue(collections.length >= 100);
    }

    @Test
    public void testContainsCollection() {
        Assert.assertTrue(catalog.containsCollection("landsat-c2-l2"));
        Assert.assertTrue(catalog.containsCollection("sentinel-2-l2a"));
        Assert.assertFalse(catalog.containsCollection("a non existent collection name"));
    }

    @Test
    public void testGetCollection() {
        StacCollection sentinel2Collection = catalog.getCollection("sentinel-2-l2a");
        Assert.assertNotNull(sentinel2Collection);
        Assert.assertEquals(
                "https://planetarycomputer.microsoft.com/api/stac/v1/collections/sentinel-2-l2a",
                sentinel2Collection.getCollectionURL());
    }

}
