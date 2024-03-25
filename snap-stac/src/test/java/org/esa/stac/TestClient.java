package org.esa.stac;

import com.bc.ceres.test.LongTestRunner;
import org.esa.stac.internal.EstablishedModifiers;
import org.esa.stac.internal.StacCatalog;
import org.esa.stac.internal.StacItem;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.InputStream;

@RunWith(LongTestRunner.class)
public class TestClient {
    private static final String catalogURL = "https://planetarycomputer.microsoft.com/api/stac/v1";

    StacClient client;

    @Before
    public void setup() throws Exception {
        client = new StacClient(catalogURL, EstablishedModifiers.planetaryComputer());
    }

    @Test
    public void testCatalog(){
        Assert.assertNotNull(client.getCatalog());
    }

    @Test
    public void testSearch() throws Exception {
        StacItem[] results = client.search(
            new String[]{"sentinel-2-l2a", "landsat-c2-l2"},
            new double[]{-124.2751,45.5469,-123.9613,45.7458},
            "2020-01-01/2022-11-05" );
        Assert.assertEquals(619, results.length);
    }

    @Test
    public void testStream() throws Exception {
        String itemURL = "https://planetarycomputer.microsoft.com/api/stac/v1/collections/landsat-c2-l2/items/LC09_L2SP_047028_20221031_02_T2";
        StacItem item = new StacItem(itemURL, client);
        InputStream is = client.streamAsset(item.getAsset(item.listAssetIds()[0]));
        Assert.assertNotNull(is);
        Assert.assertEquals(15557, is.available());
    }

    @Test
    public void testDownloadAsset() throws Exception {
        File tmpDir = new File("/tmp");
        if(!tmpDir.exists()){
            return;
        }

        String itemURL = "https://planetarycomputer.microsoft.com/api/stac/v1/collections/landsat-c2-l2/items/LC09_L2SP_047028_20221031_02_T2";
        StacItem item = new StacItem(itemURL);
        StacItem.StacAsset asset = item.getAsset(item.listAssetIds()[0]);
        File outputFile = client.downloadAsset(asset, tmpDir);
        Assert.assertTrue(outputFile.exists());
        Assert.assertEquals(117392, outputFile.length());
        outputFile.delete();

    }
/*
    Very long running test. 
    @Test @Ignore
    public void testDownloadItem() throws Exception {
        File tmpDir = new File("/tmp");
        if(!tmpDir.exists()){
            return;
        }
        String itemURL = "https://planetarycomputer.microsoft.com/api/stac/v1/collections/landsat-c2-l2/items/LC09_L2SP_047028_20221031_02_T2";
        StacItem item = new StacItem(itemURL);
        client.downloadItem(item, tmpDir);
    }
*/
}
