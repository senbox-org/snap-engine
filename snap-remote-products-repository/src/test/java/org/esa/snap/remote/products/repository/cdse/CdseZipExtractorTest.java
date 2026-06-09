package org.esa.snap.remote.products.repository.cdse;

import org.esa.snap.remote.products.repository.listener.ProgressListener;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CdseZipExtractorTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void extractsProductFolderFromZipFile() throws Exception {
        Path zipFile = temporaryFolder.getRoot().toPath().resolve("product.zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zipOutputStream.putNextEntry(new ZipEntry("S3A_TEST.SEN3/xfdumanifest.xml"));
            zipOutputStream.write("<manifest/>".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }

        Path extracted = new CdseZipExtractor().extract(zipFile, temporaryFolder.getRoot().toPath(), "S3A_TEST.SEN3", new NoOpProgressListener());

        assertEquals(temporaryFolder.getRoot().toPath().resolve("S3A_TEST.SEN3"), extracted);
        assertTrue(Files.exists(extracted.resolve("xfdumanifest.xml")));
    }

    @Test
    public void rejectsZipSlipEntries() throws Exception {
        Path zipFile = temporaryFolder.getRoot().toPath().resolve("product.zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zipOutputStream.putNextEntry(new ZipEntry("../evil.txt"));
            zipOutputStream.write("evil".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }

        try {
            new CdseZipExtractor().extract(zipFile, temporaryFolder.getRoot().toPath(), "product", new NoOpProgressListener());
            fail("Expected IOException");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("outside target directory"));
        }
        assertFalse(Files.exists(temporaryFolder.getRoot().toPath().getParent().resolve("evil.txt")));
    }

    private static class NoOpProgressListener implements ProgressListener {
        @Override
        public void notifyProgress(short progressPercent) {
        }

        @Override
        public void notifyApproximateSize(long approximateSize) {
        }

        @Override
        public void notifyProductStatus(String productStatus) {
        }
    }
}
