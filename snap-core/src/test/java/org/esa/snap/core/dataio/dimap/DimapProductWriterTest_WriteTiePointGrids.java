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
import org.esa.snap.GlobalTestConfig;
import org.esa.snap.GlobalTestTools;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.stream.ImageInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DimapProductWriterTest_WriteTiePointGrids {

    private final DimapProductWriterPlugIn _writerPlugIn = new DimapProductWriterPlugIn();
    private DimapProductWriter _productWriter;
    private Path _outputDir;
    private Path _outputFile;

    @Before
    public void setUp() {
        GlobalTestTools.deleteTestDataOutputDirectory();
        _productWriter = new DimapProductWriter(_writerPlugIn);
        _outputDir = GlobalTestConfig.getSnapTestDataOutputDirectory().resolve("testproduct");
        _outputFile = _outputDir.resolve("testproduct" + DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION);
    }

    @After
    public void tearDown() {
        if (_productWriter != null) {
            try {
                _productWriter.close();
            } catch (IOException e) {
            }
        }
        GlobalTestTools.deleteTestDataOutputDirectory();
    }

    @Test
    @STTM("SNAP-3508")
    public void testWriteProductNodes_TiePointGrid() {
        Product product = new Product("name", "MER_FR__1P",
                50, 25);
        float[] expectedArray = getTiePointData(10, 5);
        TiePointGrid tiePointGrid = new TiePointGrid("name", 10, 5, 0, 0, 5, 5, expectedArray);
        product.addTiePointGrid(tiePointGrid);

        try {
            _productWriter.writeProductNodes(product, _outputFile);
            _productWriter.close();
        } catch (IOException e) {
            fail("IOException not expected");
        }

        float[] currentArray = getCurrentByteArray(tiePointGrid);
        assertEquals(expectedArray.length, currentArray.length);

        for (int i = 0; i < expectedArray.length; i++) {
            assertEquals(expectedArray[i], currentArray[i], 1e-8f);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    ///   End Of Public
    ///////////////////////////////////////////////////////////////////////////

    private float[] getTiePointData(int width, int height) {
        float[] tiePoints = new float[width * height];
        for (int x = 0; x < tiePoints.length; x++) {
            tiePoints[x] = x * 1000;
        }
        return tiePoints;
    }

    private float[] getCurrentByteArray(TiePointGrid grid) {
        int fileLength = Long.valueOf(getSize(grid)).intValue();
        int arrayLength = fileLength / ProductData.getElemSize(ProductData.TYPE_FLOAT32);
        float[] currentFloats = new float[arrayLength];
        try (ImageInputStream inputStream = createInputStream(grid)){
            inputStream.readFully(currentFloats, 0, arrayLength);
        } catch (FileNotFoundException e) {
            fail("FileNotFoundException not expected");
        } catch (IOException e) {
            fail("IOException not expected");
        }
        return currentFloats;
    }

    private ImageInputStream createInputStream(TiePointGrid grid) {
        Path file = getFile(grid);
        assertTrue(Files.exists(file));
        ImageInputStream inputStream = null;
        try {
            inputStream = ImageUtils.getImageInputStream(file);
        } catch (FileNotFoundException e) {
            fail("FileNotFoundException not expected");
        } catch (IOException e) {
            fail("IOException not expected");
        }
        return inputStream;
    }

    private Path createDataDir() {
        final String nameWithoutExtension = FileUtils.getFilenameWithoutExtension(_outputFile);
        return _outputDir.resolve(nameWithoutExtension + ".data");
    }

    private long getSize(TiePointGrid grid){
        final Path file = getFile(grid);
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path getFile(TiePointGrid grid){
        final Path tiePointGridDir = createDataDir().resolve(DimapProductConstants.TIE_POINT_GRID_DIR_NAME);
        return tiePointGridDir.resolve(grid.getName() + DimapProductConstants.IMAGE_FILE_EXTENSION);
    }

}
