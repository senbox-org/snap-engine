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
package org.esa.snap.stac.io;

import com.bc.ceres.core.runtime.internal.IOHelper;
import com.bc.ceres.core.runtime.internal.UrlHelper;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.stac.StacItem;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestSTACProductReaderPlugIn {

    private STACProductReaderPlugIn plugIn;

    @Before
    public void setup() {
        plugIn = new STACProductReaderPlugIn();
    }

    @Test
    public void testGetFormatNames() {
        String[] names = plugIn.getFormatNames();
        assertEquals(1, names.length);
        assertEquals("STAC", names[0]);
    }

    @Test
    public void testGetDefaultFileExtensions() {
        String[] exts = plugIn.getDefaultFileExtensions();
        assertEquals(1, exts.length);
        assertEquals(".json", exts[0]);
    }

    @Test
    public void testGetDescription() {
        String desc = plugIn.getDescription(null);
        assertEquals("SpatioTemporal Asset Catalog", desc);
    }

    @Test
    public void testGetInputTypes() {
        Class[] types = plugIn.getInputTypes();
        assertEquals(4, types.length);
        boolean hasPath = false, hasFile = false, hasString = false, hasInputStream = false;
        for (Class c : types) {
            if (c == Path.class) hasPath = true;
            if (c == Path.class) hasFile = true;
            if (c == String.class) hasString = true;
            if (c == InputStream.class) hasInputStream = true;
        }
        assertTrue(hasPath);
        assertTrue(hasFile);
        assertTrue(hasString);
        assertTrue(hasInputStream);
    }

    @Test
    public void testCreateReaderInstance() {
        ProductReader reader = plugIn.createReaderInstance();
        assertNotNull(reader);
        assertTrue(reader instanceof STACProductReader);
    }

    @Test
    public void testGetProductFileFilter() {
        SnapFileFilter filter = plugIn.getProductFileFilter();
        assertNotNull(filter);
    }

    @Test
    public void testGetDecodeQualificationSuitableForStacJson() {
        URL resource = StacItem.class.getResource("capella/CAPELLA_ARL_SP_GEO_VV_20190927234024_20190927234124.json");
        assertNotNull(resource);
        Path path = UrlHelper.urlToFile(resource);

        DecodeQualification qual = plugIn.getDecodeQualification(path);
        assertEquals(DecodeQualification.SUITABLE, qual);
    }

    @Test
    public void testGetDecodeQualificationUnableForCatalog() {
        URL resource = StacItem.class.getResource("catalog/snapplanet.json");
        assertNotNull(resource);
        Path path = UrlHelper.urlToFile(resource);

        DecodeQualification qual = plugIn.getDecodeQualification(path);
        assertEquals(DecodeQualification.UNABLE, qual);
    }

    @Test
    public void testGetDecodeQualificationUnableForNonJson() {
        DecodeQualification qual = plugIn.getDecodeQualification(Path.of("test.tif"));
        assertEquals(DecodeQualification.UNABLE, qual);
    }

    @Test
    public void testGetDecodeQualificationUnableForNonExistent() {
        DecodeQualification qual = plugIn.getDecodeQualification(Path.of("nonexistent.json"));
        assertEquals(DecodeQualification.UNABLE, qual);
    }

    @Test
    public void testGetDecodeQualificationUnableForNull() {
        DecodeQualification qual = plugIn.getDecodeQualification(null);
        assertEquals(DecodeQualification.UNABLE, qual);
    }

    @Test
    public void testFileFilterAcceptsJson() throws Exception {
        STACProductReaderPlugIn.FileFilter filter = new STACProductReaderPlugIn.FileFilter();

        URL resource = StacItem.class.getResource("capella/CAPELLA_ARL_SP_GEO_VV_20190927234024_20190927234124.json");
        assertNotNull(resource);
        Path file = UrlHelper.urlToFile(resource);
        assertTrue(filter.accept(file));
    }

    @Test
    public void testFileFilterAcceptsDirectories() {
        STACProductReaderPlugIn.FileFilter filter = new STACProductReaderPlugIn.FileFilter();
        Path dir = IOHelper.createATempDir();
        assertTrue(filter.accept(dir));
    }
}
