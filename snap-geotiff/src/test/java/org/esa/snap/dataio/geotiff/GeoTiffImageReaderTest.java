package org.esa.snap.dataio.geotiff;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;


public class GeoTiffImageReaderTest {


    @Test
    @STTM("SNAP-4105")
    public void test_close_closesProvidedCloseable_onlyOnce() throws Exception {
        File tiffFile = createTempTiffFile();
        CountingCloseable countingCloseable = new CountingCloseable();

        try (java.io.InputStream inputStream = Files.newInputStream(tiffFile.toPath())) {
            GeoTiffImageReader reader = new GeoTiffImageReader(inputStream, countingCloseable);

            assertTrue(reader.getImageWidth() > 0);
            assertTrue(reader.getImageHeight() > 0);

            reader.close();
            reader.close();

            assertEquals(1, countingCloseable.getCloseCount());
        } finally {
            Files.deleteIfExists(tiffFile.toPath());
        }
    }

    @Test
    @STTM("SNAP-4105")
    public void test_fileConstructor_close_isIdempotent() throws Exception {
        File tiffFile = createTempTiffFile();

        try {
            GeoTiffImageReader reader = new GeoTiffImageReader(tiffFile);

            assertTrue(reader.getImageWidth() > 0);
            assertTrue(reader.getImageHeight() > 0);

            reader.close();
            reader.close();
        } finally {
            Files.deleteIfExists(tiffFile.toPath());
        }
    }

    @Test
    @STTM("SNAP-4105")
    public void test_buildGeoTiffImageReaderObject_closesProvidedCloseable_onlyOnce() throws Exception {
        File tiffFile = createTempTiffFile();
        CountingCloseable countingCloseable = new CountingCloseable();

        try {
            GeoTiffImageReader reader = GeoTiffImageReader.buildGeoTiffImageReaderObject(tiffFile.toPath(), countingCloseable);

            assertTrue(reader.getImageWidth() > 0);
            assertTrue(reader.getImageHeight() > 0);

            reader.close();
            reader.close();

            assertEquals(1, countingCloseable.getCloseCount());
        } finally {
            Files.deleteIfExists(tiffFile.toPath());
        }
    }

    @Test
    @STTM("SNAP-4213")
    public void test_fileConstructor_canReadSelectedImageIndex() throws Exception {
        File tiffFile = createTempMultiImageTiffFile();

        try {
            GeoTiffImageReader firstImageReader = new GeoTiffImageReader(tiffFile);
            GeoTiffImageReader secondImageReader = new GeoTiffImageReader(tiffFile, 1);

            assertEquals(2, firstImageReader.getImageCount());
            assertEquals(4, firstImageReader.getImageWidth());
            assertEquals(3, firstImageReader.getImageHeight());
            assertEquals(2, secondImageReader.getImageWidth());
            assertEquals(2, secondImageReader.getImageHeight());

            firstImageReader.close();
            secondImageReader.close();
        } finally {
            Files.deleteIfExists(tiffFile.toPath());
        }
    }

    private static File createTempTiffFile() throws IOException {
        File file = File.createTempFile("geotiff-image-reader-", ".tif");
        BufferedImage image = new BufferedImage(4, 3, BufferedImage.TYPE_BYTE_GRAY);
        boolean written = ImageIO.write(image, "TIFF", file);
        assertTrue("TIFF test image could not be written", written);
        return file;
    }

    private static File createTempMultiImageTiffFile() throws IOException {
        File file = File.createTempFile("geotiff-image-reader-multi-", ".tif");
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("TIFF");
        assertTrue("TIFF image writer is not available", writers.hasNext());
        ImageWriter writer = writers.next();
        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(file)) {
            writer.setOutput(outputStream);
            writer.prepareWriteSequence(null);
            writer.writeToSequence(new IIOImage(new BufferedImage(4, 3, BufferedImage.TYPE_BYTE_GRAY), null, null), null);
            writer.writeToSequence(new IIOImage(new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_GRAY), null, null), null);
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
        return file;
    }

    private static final class CountingCloseable implements Closeable {
        private final AtomicInteger closeCount = new AtomicInteger();

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }

        int getCloseCount() {
            return closeCount.get();
        }
    }
}
