package org.esa.stac;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.stac.internal.StacItem;
import org.esa.stac.reader.STACMetadataFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestMetadataFactory {

    private String catalogURL = "https://planetarycomputer.microsoft.com/api/stac/v1";
    StacClient client;

    @Before
    public void setup() throws Exception {
        client = new StacClient(catalogURL);
    }

    @Test
    public void testFactoryFromItem() throws Exception {
        StacItem item = new StacItem("https://planetarycomputer.microsoft.com/api/stac/v1/collections/landsat-c2-l2/items/LC09_L2SP_047028_20221031_02_T2", client );
        STACMetadataFactory factory = new STACMetadataFactory(item);
        MetadataElement origRoot = factory.generate();
        Assert.assertTrue(origRoot.containsAttribute("id"));
        Assert.assertEquals("LC09_L2SP_047028_20221031_02_T2", origRoot.getAttributeString("id"));
        Assert.assertTrue(origRoot.containsAttribute("collection"));
        Assert.assertEquals("landsat-c2-l2", origRoot.getAttributeString("collection"));
        Assert.assertTrue(origRoot.containsElement("assets"));
        Assert.assertEquals(25, origRoot.getElement("assets").getElements().length);
    }

    @Test
    public void testFactoryFromJSON() throws ParseException {

        String jsonString = "{\"a\": 3, \"b\":{\"c\":false, \"d\":[\"e\",\"f\",\"g\"], \"h\":[{\"i\":34, \"j\":45}, {\"l\":0}], \"m\":{\"n\":true, \"o\": false}}}";
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(jsonString);
        STACMetadataFactory factory = new STACMetadataFactory(json);
        MetadataElement origRoot = factory.generate();
        Assert.assertTrue(origRoot.containsAttribute("a"));
        Assert.assertTrue(origRoot.containsElement("b"));

    }

}
