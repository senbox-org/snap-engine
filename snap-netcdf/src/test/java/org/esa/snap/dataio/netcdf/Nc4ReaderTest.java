/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.netcdf;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.util.DummyProductBuilder;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfNetCdfReaderPlugIn;
import org.esa.snap.runtime.Config;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * @author Ralf Quast
 * @version $Revision: 2182 $ $Date: 2008-06-12 11:09:11 +0200 (Do, 12 Jun 2008) $
 */
public class Nc4ReaderTest {

    @Test
    public void testGlobalAttributes() throws IOException {

        final URL url = Nc4ReaderTest.class.getResource("test.nc");
        assertNotNull(url);
        assertEquals("file", url.getProtocol());

        final String path = URLDecoder.decode(url.getPath(), "UTF-8");
        assertTrue(path.endsWith("test.nc"));

        final File file = new File(path);
        assertEquals(file.getName(), "test.nc");
        assertTrue(file.exists());
        assertTrue(file.canRead());

        final ProductReader reader = new CfNetCdfReaderPlugIn().createReaderInstance();
        final Product product = reader.readProductNodes(file.getPath(), null);
        assertNotNull(product);

        checkStartTime(product);
        checkEndTime(product);
    }

    @Test
    public void testVariableAttributes() throws Exception {
        final URL url = Nc4ReaderTest.class.getResource("test.nc");
        final String path = URLDecoder.decode(url.getPath(), "UTF-8");
        final File file = new File(path);
        final ProductReader reader = new CfNetCdfReaderPlugIn().createReaderInstance();

        // default case: up to 100 values are read
        final Product defaultProduct = reader.readProductNodes(file.getPath(), null);
        MetadataElement defaultLonElement = defaultProduct.getMetadataRoot().getElement("Variable_Attributes").getElement("lon");
        MetadataAttribute defaultLonValues = defaultLonElement.getElement("Values").getAttribute("data");
        long defaultLonValueCount = defaultLonValues.getNumDataElems();
        assertEquals(5, defaultLonValueCount);

        // constraining number of read values
        Config.instance().preferences().putInt("snap.dataio.netcdf.metadataElementLimit", 3);
        final Product constrainedProduct = reader.readProductNodes(file.getPath(), null);
        MetadataElement constrainedLonElement = constrainedProduct.getMetadataRoot().getElement("Variable_Attributes").getElement("lon");
        MetadataAttribute constrainedLonValues = constrainedLonElement.getElement("Values").getAttribute("data");
        long constrainedLonValueCount = constrainedLonValues.getNumDataElems();
        assertEquals(3, constrainedLonValueCount);

        // removing constrains of number of read values
        Config.instance().preferences().putInt("snap.dataio.netcdf.metadataElementLimit", -1);
        final Product unconstrainedProduct = reader.readProductNodes(file.getPath(), null);
        MetadataElement unconstrainedLonElement = unconstrainedProduct.getMetadataRoot().getElement("Variable_Attributes").getElement("lon");
        MetadataAttribute unconstrainedLonValues = unconstrainedLonElement.getElement("Values").getAttribute("data");
        long unconstrainedLonValueCount = unconstrainedLonValues.getNumDataElems();
        assertEquals(5, unconstrainedLonValueCount);
    }

    @Test
    public void testWithExistingLatLonBandsAndCrsGeoCoding() throws IOException {
        DummyProductBuilder pb = new DummyProductBuilder();
        pb.size(DummyProductBuilder.Size.SMALL);
        pb.gc(DummyProductBuilder.GC.PER_PIXEL);
        pb.gcOcc(DummyProductBuilder.GCOcc.UNIQUE);
        pb.sizeOcc(DummyProductBuilder.SizeOcc.SINGLE);
        Product product = pb.create();
        product.getBand("latitude").setName("lat");
        product.getBand("longitude").setName("lon");

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("crs", "EPSG:4326");
        Product reprojectProduct = GPF.createProduct("Reproject", parameters, product);

        File nc4testFile = File.createTempFile("nc4test", ".nc");
        ProductIO.writeProduct(reprojectProduct, nc4testFile.getAbsolutePath(), "NetCDF4-CF");
        reprojectProduct.dispose();
        product.dispose();

        Product readProduct = ProductIO.readProduct(nc4testFile.getAbsolutePath());
        assertNotNull(readProduct.getSceneGeoCoding().getGeoPos(new PixelPos(5.0, 5.0), null));

    }

    private void checkStartTime(final Product product) {
        final ProductData.UTC utc = product.getStartTime();
        assertNotNull(utc);

        final Calendar startTime = utc.getAsCalendar();
        assertEquals(2002, startTime.get(Calendar.YEAR));
        assertEquals(11, startTime.get(Calendar.MONTH));
        assertEquals(24, startTime.get(Calendar.DATE));
        assertEquals(11, startTime.get(Calendar.HOUR_OF_DAY));
        assertEquals(12, startTime.get(Calendar.MINUTE));
        assertEquals(13, startTime.get(Calendar.SECOND));
    }

    private void checkEndTime(final Product product) {
        final ProductData.UTC utc = product.getEndTime();
        assertNotNull(utc);

        final Calendar endTime = utc.getAsCalendar();
        assertEquals(2002, endTime.get(Calendar.YEAR));
        assertEquals(11, endTime.get(Calendar.MONTH));
        assertEquals(24, endTime.get(Calendar.DATE));
        assertEquals(11, endTime.get(Calendar.HOUR_OF_DAY));
        assertEquals(12, endTime.get(Calendar.MINUTE));
        assertEquals(14, endTime.get(Calendar.SECOND));
    }
}
