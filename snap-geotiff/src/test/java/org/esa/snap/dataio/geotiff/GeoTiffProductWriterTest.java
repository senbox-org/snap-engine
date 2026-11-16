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

package org.esa.snap.dataio.geotiff;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.io.EnhancedNIORandomAccessFile;
import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.geotiff.internal.TiffHeader;
import org.esa.snap.dataio.geotiff.internal.TiffIFD;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.file.StandardOpenOption;

import static org.junit.Assert.*;

public class GeoTiffProductWriterTest {

    private String FILENAME = "temp.tif";
    private GeoTiffProductWriter _productWriter;
    private Product _product;

    @Before
    public void setUp() throws Exception {
        FILENAME = IOHelper.createATempDir().resolve(FILENAME).toString();
        _productWriter = new GeoTiffProductWriter(new GeoTiffProductWriterPlugIn());

        _product = new Product("temp", "type", 10, 20);
        _product.addBand("b1", ProductData.TYPE_UINT32);
        fillBandWithData(_product.getBand("b1"), 1);
    }

    @After
    public void tearDown() throws Exception {
        _productWriter.close();
        Path testFile = Path.of(FILENAME);
        if (Files.isRegularFile(testFile)) {
            if (!IOHelper.delete(testFile)) {
                fail("unable to delete test file: " + testFile.getFileName());
            }
        }
    }

    @Test
    public void testGeoTIFFProductWriterCreation() {
        final GeoTiffProductWriter productWriter = new GeoTiffProductWriter(new GeoTiffProductWriterPlugIn());

        assertNotNull(productWriter.getWriterPlugIn());
    }

    @Test
    public void testThatStringIsAValidOutput() throws IOException {
        _productWriter.writeProductNodes(_product, FILENAME);
    }

    @Test
    public void testThatFileIsAValidOutput() throws IOException {
        _productWriter.writeProductNodes(_product, Path.of(FILENAME));
    }

    @Test
    public void testWriteProductNodesAreWritten() throws IOException {
        _productWriter.writeProductNodes(_product, FILENAME);
        _productWriter.close();

        assertTrue(Files.size(Path.of(FILENAME)) > 0);
    }

    @Test
    public void testWriteProductNodes_CropFileSize() throws IOException {
        final long expectedSize = computeExpectedSize(_product);
        createBiggerFile(expectedSize);

        _productWriter.writeProductNodes(_product, FILENAME);
        writeAllBands(_product);
        _productWriter.close();

        assertEquals(expectedSize, Files.size(Path.of(FILENAME)));
    }

    private void writeAllBands(final Product product) throws IOException {
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();
        final Band[] bands = product.getBands();
        for (Band band : bands) {
            _productWriter.writeBandRasterData(band, 0, 0, width, height, band.getData(), ProgressMonitor.NULL);
        }
    }

    private void createBiggerFile(final long expectedSize) throws IOException {
        final EnhancedNIORandomAccessFile raf = new EnhancedNIORandomAccessFile(Path.of(FILENAME), StandardOpenOption.READ, StandardOpenOption.WRITE);
        raf.setMinLength(expectedSize);
        raf.close();
    }

    private long computeExpectedSize(final Product product) {
        final TiffIFD tiffIFD = new TiffIFD(product);
        return tiffIFD.getRequiredEntireSize() + TiffHeader.FIRST_IFD_OFFSET.getValue();
    }

    private void fillBandWithData(final Band band, final int start) {
        final ProductData data = band.createCompatibleRasterData();
        for (int i = 0; i < band.getRasterWidth() * band.getRasterHeight(); i++) {
            data.setElemIntAt(i, start + i);
        }
        band.setData(data);
    }
}
