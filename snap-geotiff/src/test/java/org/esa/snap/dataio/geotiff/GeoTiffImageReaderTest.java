package org.esa.snap.dataio.geotiff;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.util.ImageUtils;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class GeoTiffImageReaderTest {


    @Test
    @STTM("SNAP-4105")
    public void test_close_closesProvidedCloseable_onlyOnce() throws Exception {
        Path tiffFile = createTempTiffFile();
        CountingCloseable countingCloseable = new CountingCloseable();

        try (java.io.InputStream inputStream = Files.newInputStream(tiffFile)) {
            GeoTiffImageReader reader = new GeoTiffImageReader(inputStream, countingCloseable);

            assertTrue(reader.getImageWidth() > 0);
            assertTrue(reader.getImageHeight() > 0);

            reader.close();
            reader.close();

            assertEquals(1, countingCloseable.getCloseCount());
        } finally {
            Files.deleteIfExists(tiffFile);
        }
    }

    @Test
    @STTM("SNAP-4105")
    public void test_fileConstructor_close_isIdempotent() throws Exception {
        Path tiffFile = createTempTiffFile();

        try {
            GeoTiffImageReader reader = new GeoTiffImageReader(tiffFile);

            assertTrue(reader.getImageWidth() > 0);
            assertTrue(reader.getImageHeight() > 0);

            reader.close();
            reader.close();
        } finally {
            Files.deleteIfExists(tiffFile);
        }
    }

    @Test
    @STTM("SNAP-4105")
    public void test_buildGeoTiffImageReaderObject_closesProvidedCloseable_onlyOnce() throws Exception {
        Path tiffFile = createTempTiffFile();
        CountingCloseable countingCloseable = new CountingCloseable();

        try {
            GeoTiffImageReader reader = GeoTiffImageReader.buildGeoTiffImageReaderObject(tiffFile, countingCloseable);

            assertTrue(reader.getImageWidth() > 0);
            assertTrue(reader.getImageHeight() > 0);

            reader.close();
            reader.close();

            assertEquals(1, countingCloseable.getCloseCount());
        } finally {
            Files.deleteIfExists(tiffFile);
        }
    }

    private static Path createTempTiffFile() throws IOException {
        Path file = IOHelper.createTempFile("geotiff-image-reader-", ".tif");
        BufferedImage image = new BufferedImage(4, 3, BufferedImage.TYPE_BYTE_GRAY);
        boolean written = ImageUtils.writeImage(image, "TIFF", file);
        assertTrue("TIFF test image could not be written", written);
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