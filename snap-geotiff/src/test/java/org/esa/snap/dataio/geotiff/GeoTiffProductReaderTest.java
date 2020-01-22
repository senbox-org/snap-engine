/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.dataio.geotiff;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.Test;

import javax.imageio.stream.FileCacheImageInputStream;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.*;


/**
 * BigTiff reading
 *
 * @author Serge Stankovic
 */

public class GeoTiffProductReaderTest {

    public GeoTiffProductReaderTest() {
    }

    @Test
    public void testReadProduct() throws Exception {
        final URL resource = getClass().getResource("tiger-minisblack-strip-16.tif");
        assertNotNull(resource);

        FileCacheImageInputStream imageInputStream = new FileCacheImageInputStream(resource.openStream(), null);
        try (GeoTiffImageReader geoTiffImageReader = new GeoTiffImageReader(imageInputStream)) {

            final String filePath = resource.getFile();
            String defaultProductName = FileUtils.getFilenameWithoutExtension(new File(filePath).getName().toString());

            GeoTiffProductReaderPlugIn readerPlugIn = new GeoTiffProductReaderPlugIn();
            final GeoTiffProductReader productReader = new GeoTiffProductReader(readerPlugIn);
            final Product product = productReader.readProduct(geoTiffImageReader, defaultProductName);
            assertNotNull(product);
            assertNull(product.getFileLocation());
            assertNotNull(product.getMetadataRoot());
            assertNotNull(product.getName());
            assertNull(product.getSceneGeoCoding());
            assertNotNull(product.getProductReader());
            assertNotNull(product.getPreferredTileSize());
            assertEquals(product.getProductReader(), productReader);
            assertEquals(73, product.getSceneRasterWidth());
            assertEquals(76, product.getSceneRasterHeight());
            assertEquals(1, product.getNumBands());

            final Band band = product.getBandAt(0);
            assertNotNull(band);
            assertEquals("band_1", band.getName());
            assertEquals(73, band.getRasterWidth());
            assertEquals(76, band.getRasterHeight());

            final int[] pixels = new int[band.getRasterWidth() * band.getRasterHeight()];
            band.readPixels(0, 0, band.getRasterWidth(), band.getRasterHeight(), pixels, ProgressMonitor.NULL);

            assertEquals(52428, pixels[20]);
            assertEquals(18295, pixels[40]);
            assertEquals(52418, pixels[60]);
        }
    }

    @Test
    public void testReadProductSubset() throws Exception {
        final URL resource = getClass().getResource("tiger-minisblack-strip-16.tif");
        assertNotNull(resource);

        File productFile = new File(resource.toURI());

        GeoTiffProductReaderPlugIn readerPlugIn = new GeoTiffProductReaderPlugIn();
        final GeoTiffProductReader productReader = new GeoTiffProductReader(readerPlugIn);

        Rectangle subsetRegion = new Rectangle(23, 32, 41, 35);
        ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setNodeNames(new String[] { "band_1"} );
        subsetDef.setRegion(subsetRegion);
        subsetDef.setSubSampling(1, 1);

        final Product product = productReader.readProductNodes(productFile, subsetDef);
        assertNotNull(product);
        assertNotNull(product.getFileLocation());
        assertNull(product.getSceneGeoCoding());
        assertNotNull(product.getName());
        assertNotNull(product.getPreferredTileSize());
        assertNotNull(product.getProductReader());
        assertEquals(product.getProductReader(), productReader);
        assertEquals(41, product.getSceneRasterWidth());
        assertEquals(35, product.getSceneRasterHeight());
        assertEquals(1, product.getNumBands());

        final Band band = product.getBandAt(0);
        assertNotNull(band);
        assertEquals("band_1", band.getName());
        assertEquals(41, band.getRasterWidth());
        assertEquals(35, band.getRasterHeight());

        assertEquals(0, product.getMaskGroup().getNodeCount());

        final int[] pixels = new int[band.getRasterWidth() * band.getRasterHeight()];
        band.readPixels(0, 0, band.getRasterWidth(), band.getRasterHeight(), pixels, ProgressMonitor.NULL);

        assertEquals(33566, pixels[0]);
        assertEquals(2359, pixels[20]);
        assertEquals(43360, pixels[123]);
        assertEquals(49161, pixels[342]);
        assertEquals(31199, pixels[875]);
        assertEquals(52403, pixels[1342]);
        assertEquals(52239, pixels[1213]);
        assertEquals(52406, pixels[1431]);
        assertEquals(49988, pixels[555]);
        assertEquals(53921, pixels[765]);
        assertEquals(16508, pixels[999]);
        assertEquals(46053, pixels[434]);
    }
}
