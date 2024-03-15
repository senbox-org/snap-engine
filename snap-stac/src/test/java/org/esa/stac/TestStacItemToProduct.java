package org.esa.stac;

import com.bc.ceres.test.LongTestRunner;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.stac.internal.StacCatalog;
import org.esa.stac.internal.StacItem;
import org.esa.stac.reader.STACMetadataFactory;
import org.esa.stac.reader.StacItemToProduct;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LongTestRunner.class)
public class TestStacItemToProduct {

    private static final String catalogURL = "https://planetarycomputer.microsoft.com/api/stac/v1";
    StacClient client;

    @Before
    public void setup() throws Exception {
        client = new StacClient(catalogURL);
    }

    @Test
    public void testCreateProduct() throws Exception {
        String itemURL = "https://planetarycomputer.microsoft.com/api/stac/v1/collections/landsat-c2-l2/items/LC09_L2SP_047028_20221031_02_T2";
        StacItem item = new StacItem(itemURL);
        StacItemToProduct converter = new StacItemToProduct(item);
        Product productNoStreaming = converter.createProduct(false, false);
        Assert.assertEquals("LC09_L2SP_047028_20221031_02_T2", productNoStreaming.getName());
        Assert.assertNotNull(productNoStreaming.getSceneGeoCoding());
        Assert.assertEquals(16, productNoStreaming.getNumBands());
    }
    


}
