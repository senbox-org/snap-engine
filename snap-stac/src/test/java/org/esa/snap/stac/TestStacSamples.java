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

import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestStacSamples {

    @Test
    public void testCapella() throws Exception {

        validateStac("capella/CAPELLA_ARL_SP_GEO_VV_20190927234024_20190927234124.json");
    }

    @Test
    public void testWyvern() throws Exception {

        validateStac("wyvern/wyvern_stac.json");
    }

    @Test
    public void testK3() throws Exception {

        validateStac("siis/k3.json");
    }

    private void validateStac(final String stacPath) throws Exception {

        URL resource = StacItem.class.getResource(stacPath);
        assertNotNull(resource);
        Path path = Paths.get(resource.toURI());
        assertTrue(StacItem.isStacItem(path));

        final StacItem stacItem = new StacItem(path.toFile());

        //StacItemValidator validator = new StacItemValidator();
        //validator.validate(stacItem);
    }
}
