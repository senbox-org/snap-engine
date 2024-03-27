package org.esa.stac;

import org.esa.stac.internal.StacCatalog;
import org.esa.stac.internal.StacCollection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestStacCatalog {
    private String catalogURL = "https://planetarycomputer.microsoft.com/api/stac/v1";
    StacCatalog catalog;

    @Before
    public void setup() throws Exception {
        catalog = new StacCatalog(catalogURL);
    }

    @Test
    public void testName() throws Exception {

        Assert.assertEquals("Microsoft Planetary Computer STAC API", catalog.getTitle());
    }
    @Test
    public void testListCollections() {
        String [] collections = catalog.listCollections();

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
    public void testGetCollection() throws Exception {
        StacCollection sentinel2Collection = catalog.getCollection("sentinel-2-l2a");
        Assert.assertNotNull(sentinel2Collection);
        Assert.assertEquals(
                "https://planetarycomputer.microsoft.com/api/stac/v1/collections/sentinel-2-l2a",
                sentinel2Collection.getCollectionURL());
    }

}
