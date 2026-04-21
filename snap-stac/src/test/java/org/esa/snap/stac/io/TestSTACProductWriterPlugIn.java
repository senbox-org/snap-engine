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

import org.esa.snap.core.dataio.EncodeQualification;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestSTACProductWriterPlugIn {

    private STACProductWriterPlugIn plugIn;

    @Before
    public void setup() {
        plugIn = new STACProductWriterPlugIn();
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
    public void testGetOutputTypes() {
        Class[] types = plugIn.getOutputTypes();
        assertEquals(2, types.length);
        boolean hasString = false, hasFile = false;
        for (Class c : types) {
            if (c == String.class) hasString = true;
            if (c == File.class) hasFile = true;
        }
        assertTrue(hasString);
        assertTrue(hasFile);
    }

    @Test
    public void testCreateWriterInstance() {
        ProductWriter writer = plugIn.createWriterInstance();
        assertNotNull(writer);
        assertTrue(writer instanceof STACProductWriter);
    }

    @Test
    public void testGetProductFileFilter() {
        SnapFileFilter filter = plugIn.getProductFileFilter();
        assertNotNull(filter);
    }

    @Test
    public void testGetEncodeQualificationNoGeoCoding() {
        Product product = new Product("test", "test", 100, 100);
        EncodeQualification qual = plugIn.getEncodeQualification(product);
        assertEquals(EncodeQualification.Preservation.PARTIAL, qual.getPreservation());
    }

    @Test
    public void testGetEncodeQualificationWithCrsGeoCoding() throws Exception {
        Product product = new Product("test", "test", 100, 100);
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 100, 100, 0, 0, 0.01, 0.01));

        EncodeQualification qual = plugIn.getEncodeQualification(product);
        assertEquals(EncodeQualification.Preservation.FULL, qual.getPreservation());
    }
}
