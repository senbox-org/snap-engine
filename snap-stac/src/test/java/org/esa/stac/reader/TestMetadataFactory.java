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
package org.esa.stac.reader;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.stac.StacClient;
import org.esa.stac.StacItem;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestMetadataFactory {

    private final String catalogURL = "https://planetarycomputer.microsoft.com/api/stac/v1";
    StacClient client;

    @Before
    public void setup() throws Exception {
        client = new StacClient(catalogURL);
    }

    @Test
    public void testFactoryFromItem() throws Exception {
        StacItem item = new StacItem("https://planetarycomputer.microsoft.com/api/stac/v1/collections/landsat-c2-l2/items/LC09_L2SP_047028_20221031_02_T2");
        StacMetadataFactory factory = new StacMetadataFactory(item);
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
        StacMetadataFactory factory = new StacMetadataFactory(json);
        MetadataElement origRoot = factory.generate();
        Assert.assertTrue(origRoot.containsAttribute("a"));
        Assert.assertTrue(origRoot.containsElement("b"));

    }

}
