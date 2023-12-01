package org.esa.stac;

import org.esa.stac.internal.EstablishedModifiers;
import org.esa.stac.internal.StacCatalog;
import org.esa.stac.internal.StacItem;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;

public class TestStac {
    @Test
    public void testStac() throws MalformedURLException {
        StacClient client = new StacClient(
                "https://planetarycomputer.microsoft.com/api/stac/v1",
                EstablishedModifiers.planetaryComputer());

        StacCatalog catalog = client.getCatalog();
        System.out.println(catalog.getTitle());

        StacItem[] results = client.search(
                new String[]{"sentinel-2-l2a", "landsat-c2-l2"},
                new double[]{-124.2751,45.5469,-123.9613,45.7458},
                "2020-01-01/2022-11-05" );
        System.out.println(results.length);
        System.out.println(results[35].getId());
        System.out.println(results[35].getAsset("B08").getURL());

        client.downloadItem(results[35], new File("/tmp"));
    }
}
