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

package org.esa.snap.core.util;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

import static org.junit.Assert.*;


public class ImageUtilsTest {

    @Test
    @STTM("SNAP-4240")
    public void testGetImageInputStream_forFile_isRandomAccess() throws Exception {
        // Regression: getImageInputStream wrapped a plain InputStream, yielding a
        // forward-caching ImageInputStream whose seek() re-streams from position 0.
        // Callers that open a fresh stream per tile and seek to increasing offsets
        // (DimapProductReader) then become O(n^2). A File input must yield a
        // random-access FileImageInputStream so seek() is O(1).
        final File tmp = File.createTempFile("imageutils-randomaccess", ".img");
        try {
            try (DataOutputStream out = new DataOutputStream(new FileOutputStream(tmp))) {
                for (int i = 0; i < 1000; i++) {
                    out.writeShort(i);
                }
            }

            try (ImageInputStream iis = ImageUtils.getImageInputStream(tmp)) {
                assertTrue("expected random-access FileImageInputStream, got " + iis.getClass().getName(),
                        iis instanceof FileImageInputStream);

                // seek far forward then read - must return the right sample
                iis.seek(900L * 2);
                assertEquals(900, iis.readShort());
                // seek backward - random access must still work
                iis.seek(10L * 2);
                assertEquals(10, iis.readShort());
            }
        } finally {
            Files.deleteIfExists(tmp.toPath());
        }
    }

    @Test
    public void testCreateRenderedImage() {
        testByte();
        testUShort();
        testShort();
        testInt();
        testFloat();
       testDouble();
    }

    @Test
    public void testComputeTileCount() {
        assertEquals(ImageUtils.computeTileCount(1021,256),computeTileCountPrevImpl(1021,256));
        assertEquals(ImageUtils.computeTileCount(1024,256),computeTileCountPrevImpl(1024,256));
        assertEquals(ImageUtils.computeTileCount(1027,256),computeTileCountPrevImpl(1027,256));
    }

    @Test
    public void testComputeLevelSizeAsDouble() {
        assertEquals(ImageUtils.computeLevelSizeAsDouble(1021,2),computeLevelSizeAsDoublePrevImpl(1021,2), 0.01);
        assertEquals(ImageUtils.computeLevelSizeAsDouble(1024,2),computeLevelSizeAsDoublePrevImpl(1024,2),0.01);
        assertEquals(ImageUtils.computeLevelSizeAsDouble(1027,2),computeLevelSizeAsDoublePrevImpl(1027,2),0.01);
    }

    @Test
    public void testComputeLevelSize() {
        assertEquals(ImageUtils.computeLevelSize(1021,2),computeLevelSizePrevImpl(1021,2));
        assertEquals(ImageUtils.computeLevelSize(1024,2),computeLevelSizePrevImpl(1024,2));
        assertEquals(ImageUtils.computeLevelSize(1027,2),computeLevelSizePrevImpl(1027,2));
    }

    public static int computeTileCountPrevImpl(int imageSize, int tileSize) {
        int tileCount = imageSize / tileSize;
        if (imageSize % tileSize != 0) {
            tileCount++;
        }
        return tileCount;
    }

    public static double computeLevelSizeAsDoublePrevImpl(int sourceSize, int level) {
        return sourceSize / Math.pow(2, level);
    }

    public static int computeLevelSizePrevImpl(int sourceSize, int level) {
        return (int) Math.ceil(computeLevelSizeAsDoublePrevImpl(sourceSize, level));
    }

    private void testByte() {
        final RenderedImage image = createImage(ProductData.TYPE_UINT8, DataBuffer.TYPE_BYTE,
                                                new byte[]{12, 23, 34, 45});
        assertEquals(12, image.getData().getSample(0, 0, 0));
        assertEquals(23, image.getData().getSample(1, 0, 0));
        assertEquals(34, image.getData().getSample(0, 1, 0));
        assertEquals(45, image.getData().getSample(1, 1, 0));
    }


    private void testUShort() {
        final RenderedImage image = createImage(ProductData.TYPE_UINT16, DataBuffer.TYPE_USHORT,
                                                new short[]{12, 23, 34, 45});
        assertEquals(12, image.getData().getSample(0, 0, 0));
        assertEquals(23, image.getData().getSample(1, 0, 0));
        assertEquals(34, image.getData().getSample(0, 1, 0));
        assertEquals(45, image.getData().getSample(1, 1, 0));
    }

    private void testShort() {
        final RenderedImage image = createImage(ProductData.TYPE_INT16, DataBuffer.TYPE_SHORT,
                                                new short[]{-12, 23, 34, 45});
        assertEquals(-12, image.getData().getSample(0, 0, 0));
        assertEquals(23, image.getData().getSample(1, 0, 0));
        assertEquals(34, image.getData().getSample(0, 1, 0));
        assertEquals(45, image.getData().getSample(1, 1, 0));
    }

    private void testInt() {
        final RenderedImage image = createImage(ProductData.TYPE_INT32, DataBuffer.TYPE_INT,
                                                new int[]{-12, 23, 34, 45});
        assertEquals(-12, image.getData().getSample(0, 0, 0));
        assertEquals(23, image.getData().getSample(1, 0, 0));
        assertEquals(34, image.getData().getSample(0, 1, 0));
        assertEquals(45, image.getData().getSample(1, 1, 0));
    }


    private void testFloat() {
        final RenderedImage image = createImage(ProductData.TYPE_FLOAT32, DataBuffer.TYPE_FLOAT,
                                                new float[]{1.2f, 5.6f, -12.6f, -12345.6f});
        assertEquals(1.2f, image.getData().getSampleFloat(0, 0, 0), 1e-6);
        assertEquals(5.6f, image.getData().getSampleFloat(1, 0, 0), 1e-6);
        assertEquals(-12.6f, image.getData().getSampleFloat(0, 1, 0), 1e-6);
        assertEquals(-12345.6f, image.getData().getSampleFloat(1, 1, 0), 1e-6);
    }

    private void testDouble() {
        final RenderedImage image = createImage(ProductData.TYPE_FLOAT64, DataBuffer.TYPE_DOUBLE,
                                                new double []{1.2, 5.6, -12.6, -12345.6});
        assertEquals(1.2, image.getData().getSampleDouble(0, 0, 0), 1e-6);
        assertEquals(5.6, image.getData().getSampleDouble(1, 0, 0), 1e-6);
        assertEquals(-12.6, image.getData().getSampleDouble(0, 1, 0), 1e-6);
        assertEquals(-12345.6, image.getData().getSampleDouble(1, 1, 0), 1e-6);
    }

    private RenderedImage createImage(int pdType, int dbType, Object data) {
        final ProductData floatData = ProductData.createInstance(pdType, data);
        final RenderedImage image = ImageUtils.createRenderedImage(2, 2, floatData);
        assertEquals(dbType, image.getSampleModel().getDataType());
        return image;
    }
}
