package org.esa.snap.dataio.geotiff;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;


public class GeoTiffFileTest {


    @Test
    @STTM("SNAP-4105")
    public void test_close_deletesLocalCopiedFile() throws Exception {
        Path zipFile = createZipWithTiff("geotiff-file-close", "image/test.tif");
        Path localTempFolder = Files.createTempDirectory("geotiff-local-copy");

        GeoTiffFile geoTiffFile = new GeoTiffFile(zipFile, "image/test.tif", true, localTempFolder);
        GeoTiffImageReader reader = null;
        Path localCopy = localTempFolder.resolve("image/test.tif");

        try {
            reader = geoTiffFile.buildImageReader();

            assertNotNull(reader);
            assertTrue(Files.exists(localCopy));
            assertTrue(Files.isRegularFile(localCopy));

            reader.close();
            geoTiffFile.close();

            assertFalse(Files.exists(localCopy));
        } finally {
            if (reader != null) {
                reader.close();
            }
            geoTiffFile.close();
            deleteTree(localTempFolder);
            Files.deleteIfExists(zipFile);
        }
    }

    @Test
    @STTM("SNAP-4105")
    public void test_close_isIdempotent_afterLocalCopyWasCreated() throws Exception {
        Path zipFile = createZipWithTiff("geotiff-file-idempotent", "scene/test.tif");
        Path localTempFolder = Files.createTempDirectory("geotiff-local-copy");

        GeoTiffFile geoTiffFile = new GeoTiffFile(zipFile, "scene/test.tif", true, localTempFolder);
        GeoTiffImageReader reader = null;
        Path localCopy = localTempFolder.resolve("scene/test.tif");

        try {
            reader = geoTiffFile.buildImageReader();

            assertTrue(Files.exists(localCopy));

            reader.close();
            geoTiffFile.close();
            geoTiffFile.close();

            assertFalse(Files.exists(localCopy));
        } finally {
            if (reader != null) {
                reader.close();
            }
            geoTiffFile.close();
            deleteTree(localTempFolder);
            Files.deleteIfExists(zipFile);
        }
    }

    private static Path createZipWithTiff(String prefix, String entryName) throws IOException {
        Path zipFile = Files.createTempFile(prefix, ".zip");
        Path tempTiff = Files.createTempFile(prefix, ".tif");

        try {
            BufferedImage image = new BufferedImage(4, 3, BufferedImage.TYPE_BYTE_GRAY);
            boolean written = ImageIO.write(image, "TIFF", tempTiff.toFile());
            assertTrue("TIFF test image could not be written", written);

            try (OutputStream fileOut = Files.newOutputStream(zipFile);
                 ZipOutputStream zipOut = new ZipOutputStream(fileOut)) {
                zipOut.putNextEntry(new ZipEntry(entryName));
                Files.copy(tempTiff, zipOut);
                zipOut.closeEntry();
            }
        } finally {
            Files.deleteIfExists(tempTiff);
        }

        return zipFile;
    }

    private static void deleteTree(Path root) throws IOException {
        if (root == null || Files.notExists(root)) {
            return;
        }
        Files.walk(root)
                .sorted((a, b) -> Integer.compare(b.getNameCount(), a.getNameCount()))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}