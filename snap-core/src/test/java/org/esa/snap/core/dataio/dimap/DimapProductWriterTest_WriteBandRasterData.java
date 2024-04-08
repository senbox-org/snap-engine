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
package org.esa.snap.core.dataio.dimap;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.GlobalTestConfig;
import org.esa.snap.GlobalTestTools;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.stream.FileImageInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class DimapProductWriterTest_WriteBandRasterData {

    private final DimapProductWriterPlugIn _writerPlugIn = new DimapProductWriterPlugIn();
    private DimapProductWriter _productWriter;
    private File _outputDir;
    private File _outputFile;

    @Before
    public void setUp() {
        GlobalTestTools.deleteTestDataOutputDirectory();
        _productWriter = (DimapProductWriter) _writerPlugIn.createWriterInstance();
        _outputDir = new File(GlobalTestConfig.getSnapTestDataOutputDirectory(), "testproduct");
        _outputFile = new File(_outputDir, "testproduct" + DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION);
    }

    @After
    public void tearDown() {
        GlobalTestTools.deleteTestDataOutputDirectory();
    }

    @Test
    public void testWriteBandRasterData() throws IOException {
        int sceneWidth = 16;
        int sceneHeight = 12;
        int offsetX = 1;
        int offsetY = 1;
        int sourceWidth = sceneWidth - 2;
        int sourceHeight = sceneHeight - 2;
        Product product = new Product("name", "MER_FR__1P",
                sceneWidth, sceneHeight);
        Band band = new Band("band", ProductData.TYPE_INT8, sceneWidth, sceneHeight);
        product.addBand(band);

        _productWriter.setUseCache(false);
        _productWriter.writeProductNodes(product, _outputFile);
        ProductData sourceBuffer = getFilledSourceData(sourceWidth * sourceHeight);
        _productWriter.writeBandRasterData(band, offsetX, offsetY, sourceWidth, sourceHeight, sourceBuffer,
                ProgressMonitor.NULL);
        _productWriter.close();

        byte[] expectedArray = prepareExpectedArrayInt8(sceneWidth, sceneHeight);
        byte[] currentArray = getCurrentByteArray(band);
        assertEquals(expectedArray.length, currentArray.length);

        for (int i = 0; i < expectedArray.length; i++) {
            assertEquals(expectedArray[i], currentArray[i]);
        }
    }

    @Test
    public void testWriteBandRasterData_SourceBuffer_toSmall() {
        int sceneWidth = 16;
        int sceneHeight = 12;
        Product product = new Product("name", "MER_FR__1P",
                sceneWidth, sceneHeight);
        Band band = new Band("band", ProductData.TYPE_INT8, sceneWidth, sceneHeight);
        product.addBand(band);

        try {
            _productWriter.writeProductNodes(product, _outputFile);
            //Make the sourceBuffer to small
            ProductData sourceBuffer = getSourceData(sceneWidth * sceneHeight - 1);
            _productWriter.writeBandRasterData(band, 0, 0, sceneWidth, sceneHeight, sourceBuffer, ProgressMonitor.NULL);
            fail("IllegalArgumentException expected because sourceBuffer is to small");
        } catch (IOException e) {
            fail("IOException not expected: " + e.getMessage());
        } catch (IllegalArgumentException e) {
        } finally {
            try {
                _productWriter.close();
            } catch (IOException e) {
            }
        }
    }

    @Test
    public void testWriteBandRasterData_SourceBuffer_toBig() {
        int sceneWidth = 16;
        int sceneHeight = 12;
        Product product = new Product("name", "MER_FR__1P",
                sceneWidth, sceneHeight);
        Band band = new Band("band", ProductData.TYPE_INT8, sceneWidth, sceneHeight);
        product.addBand(band);

        try {
            _productWriter.writeProductNodes(product, _outputFile);
            //Make the sourceBuffer to big
            ProductData sourceBuffer = getSourceData(sceneWidth * sceneHeight + 1);
            _productWriter.writeBandRasterData(band, 0, 0, sceneWidth, sceneHeight, sourceBuffer, ProgressMonitor.NULL);
            fail("IllegalArgumentException expected because sourceBuffer is to big");
        } catch (IOException e) {
            fail("IOException not expected: " + e.getMessage());
        } catch (IllegalArgumentException e) {
        } finally {
            try {
                _productWriter.close();
            } catch (IOException e) {
            }
        }
    }

    @Test
    @STTM("SNAP-3508")
    public void testWriteBandRasterData_SourceRegionIsOutOfBandsRaster() {
        int sceneWidth = 16;
        int sceneHeight = 12;
        Product product = new Product("name", "MER_FR__1P",
                sceneWidth, sceneHeight);
        Band band = new Band("band", ProductData.TYPE_INT8, sceneWidth, sceneHeight);
        product.addBand(band);

        try {
            _productWriter.writeProductNodes(product, _outputFile);
        } catch (IOException e) {
            fail("IOException not expected");
        }

        //buffer is outside at the left side
        try {
            ProductData sourceBuffer = getSourceData(sceneWidth * sceneHeight);
            int makeOutside = -1;
            _productWriter.writeBandRasterData(band,
                    makeOutside, 0,
                    sceneWidth, sceneHeight,
                    sourceBuffer, ProgressMonitor.NULL);
            fail("IllegalArgumentException expected because region is ot of band's region");
        } catch (IOException e) {
            fail("IOException not expected");
        } catch (IllegalArgumentException e) {
        }

        //buffer is outside at the top side
        try {
            ProductData sourceBuffer = getSourceData(sceneWidth * sceneHeight);
            int makeOutside = -1;
            _productWriter.writeBandRasterData(band,
                    0, makeOutside,
                    sceneWidth, sceneHeight,
                    sourceBuffer, ProgressMonitor.NULL);
            fail("IllegalArgumentException expected because region is ot of band's region");
        } catch (IOException e) {
            fail("IOException not expected");
        } catch (IllegalArgumentException e) {
        }

        //buffer is outside at the right side
        try {
            ProductData sourceBuffer = getSourceData(sceneWidth * sceneHeight);
            int makeOutside = 1;
            _productWriter.writeBandRasterData(band,
                    makeOutside, 0,
                    sceneWidth, sceneHeight,
                    sourceBuffer, ProgressMonitor.NULL);
            fail("IllegalArgumentException expected because region is ot of band's region");
        } catch (IOException e) {
            fail("IOException not expected");
        } catch (IllegalArgumentException e) {
        }

        //buffer is outside at the botom side
        try {
            ProductData sourceBuffer = getSourceData(sceneWidth * sceneHeight);
            int makeOutside = 1;
            _productWriter.writeBandRasterData(band,
                    0, makeOutside,
                    sceneWidth, sceneHeight,
                    sourceBuffer, ProgressMonitor.NULL);
            fail("IllegalArgumentException expected because region is ot of band's region");
        } catch (IOException e) {
            fail("IOException not expected");
        } catch (IllegalArgumentException e) {
        }

        try {
            _productWriter.close();
        } catch (IOException e) {
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    ///   End Of Public
    ///////////////////////////////////////////////////////////////////////////

    private ProductData getFilledSourceData(int size) {
        ProductData data = getSourceData(size);
        byte[] bytes = new byte[data.getNumElems()];
        Arrays.fill(bytes, (byte) 85);
        data.setElems(bytes);
        return data;
    }

    private ProductData getSourceData(int size) {
        return ProductData.createInstance(ProductData.TYPE_INT8, (size));
    }

    private byte[] prepareExpectedArrayInt8(int width, int height) {
        byte[] bytes = new byte[width * height];
        byte zero = 0;
        byte fill = 85;

//      This loop fills the array like these scheme
//        00000000   # -> fill
//        0######0   . -> variable size filled with #
//        0#....#0
//        0######0
//        00000000
        for (int y = 0; y < height; y++) {
            byte filler = (y == 0 || y == height - 1) ? zero : fill;
            for (int x = 0; x < width; x++) {
                bytes[y * width + x] = x == 0 || x == width - 1 ? zero : filler;
            }
        }
        return bytes;
    }

    private byte[] getCurrentByteArray(Band band) {
        FileImageInputStream inputStream = createInputStream(band);
        int fileLength = Long.valueOf(inputStream.length()).intValue();
        byte[] currentBytes = new byte[fileLength];
        try {
            inputStream.readFully(currentBytes);
            inputStream.close();
        } catch (FileNotFoundException e) {
            fail("FileNotFoundException not expected");
        } catch (IOException e) {
            fail("IOException not expected");
        }
        return currentBytes;
    }

    private FileImageInputStream createInputStream(Band band) {
        final String nameWithoutExtension = FileUtils.getFilenameWithoutExtension(_outputFile);
        File dataDir = new File(_outputDir, nameWithoutExtension + ".data");
        File file = new File(dataDir, band.getName() + DimapProductConstants.IMAGE_FILE_EXTENSION);
        assertTrue(file.exists());
        FileImageInputStream inputStream = null;
        try {
            inputStream = new FileImageInputStream(file);
        } catch (FileNotFoundException e) {
            fail("FileNotFoundException not expected");
        } catch (IOException e) {
            fail("IOException not expected");
        }
        return inputStream;
    }
}
