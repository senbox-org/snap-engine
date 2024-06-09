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
package org.esa.snap.stac.reader;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.stac.StacClient;
import org.esa.snap.stac.StacItem;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
