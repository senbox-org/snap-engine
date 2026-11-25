package org.esa.snap.dataio.geotiff;

import com.bc.ceres.annotation.STTM;
import it.geosolutions.imageio.plugins.tiff.TIFFDirectory;
import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageio.plugins.tiff.TIFFTag;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriterSpi;
import org.junit.Test;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
    @STTM("SNAP-4221")
    public void testReadsNegativeInfinityNoData() throws IOException {
        assertReadsNoData("-inf", Double.NEGATIVE_INFINITY);
    }

    @Test
    public void testReadsPositiveInfinityNoData() throws IOException {
        assertReadsNoData("+inf", Double.POSITIVE_INFINITY);
    }

    @Test
    public void testReadsFiniteNoData() throws IOException {
        assertReadsNoData("255", 255.0);
    }

    private static void assertReadsNoData(String tagValue, double expected) throws IOException {
        File file = File.createTempFile("geotiff-nodata-", ".tif");
        try {
            writeTiffWithNoData(file, tagValue);
            try (GeoTiffImageReader reader = new GeoTiffImageReader(file)) {
                assertEquals(Double.valueOf(expected), GeoTiffProductReader.getFillValue(reader));
                assertEquals(42, reader.readRect(false, 0, 0, 1, 1, 0, 0, 1, 1)
                        .getSample(0, 0, 0));
            }
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    private static void writeTiffWithNoData(File file, String tagValue) throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
        image.getRaster().setSample(0, 0, 0, 42);

        ImageWriter writer = new TIFFImageWriterSpi().createWriterInstance();
        try (var output = ImageIO.createImageOutputStream(file)) {
            writer.setOutput(output);
            var param = writer.getDefaultWriteParam();
            TIFFDirectory directory = TIFFDirectory.createFromMetadata(
                    writer.getDefaultImageMetadata(new ImageTypeSpecifier(image), param));
            TIFFTag tag = new TIFFTag("GDALNoData", 42113, 1 << TIFFTag.TIFF_ASCII);
            directory.addTIFFField(new TIFFField(tag, TIFFTag.TIFF_ASCII, 1, new String[]{tagValue}));
            writer.write(null, new IIOImage(image, null, directory.getAsMetadata()), param);
        } finally {
            writer.dispose();
        }
    }

    private static File createTempTiffFile() throws IOException {
        File file = File.createTempFile("geotiff-image-reader-", ".tif");
        BufferedImage image = new BufferedImage(4, 3, BufferedImage.TYPE_BYTE_GRAY);
        boolean written = ImageIO.write(image, "TIFF", file);
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