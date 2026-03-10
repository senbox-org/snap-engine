package org.esa.snap.engine_utilities.commons;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;


public class AbstractVirtualPathTest {


    @Test
    @STTM("SNAP-4105")
    public void test_close_deletesLocalTempFolder() throws IOException {
        TestVirtualPath virtualPath = new TestVirtualPath(true);

        Path tempFolder = virtualPath.makeLocalTempFolder();
        assertNotNull(tempFolder);
        assertTrue(Files.exists(tempFolder));
        assertTrue(Files.isDirectory(tempFolder));
        assertEquals(tempFolder.toFile(), virtualPath.getTempDir());

        virtualPath.close();

        assertFalse(Files.exists(tempFolder));
        assertNull(virtualPath.getTempDir());
    }

    @Test
    @STTM("SNAP-4105")
    public void test_close_isIdempotent() throws IOException {
        TestVirtualPath virtualPath = new TestVirtualPath(true);

        Path tempFolder = virtualPath.makeLocalTempFolder();
        assertTrue(Files.exists(tempFolder));

        virtualPath.close();
        virtualPath.close();

        assertFalse(Files.exists(tempFolder));
        assertNull(virtualPath.getTempDir());
    }

    private static final class TestVirtualPath extends AbstractVirtualPath {

        private TestVirtualPath(boolean copyFilesOnLocalDisk) {
            super(copyFilesOnLocalDisk);
        }

        @Override
        public Path buildPath(String first, String... more) {
            return Path.of(first, more);
        }

        @Override
        public String getFileSystemSeparator() {
            return File.separator;
        }

        @Override
        public Path getFileIgnoreCaseIfExists(String relativePath) {
            return null;
        }

        @Override
        public FilePathInputStream getInputStreamIgnoreCaseIfExists(String relativePath) {
            return null;
        }

        @Override
        public FilePathInputStream getInputStream(String path) {
            return null;
        }

        @Override
        public FilePath getFilePath(String childRelativePath) {
            return null;
        }

        @Override
        public String getBasePath() {
            return "test";
        }

        @Override
        public File getBaseFile() {
            return new File(".");
        }

        @Override
        public File getFile(String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] list(String path) {
            return new String[0];
        }

        @Override
        public boolean exists(String path) {
            return false;
        }

        @Override
        public String[] listAllFiles() {
            return new String[0];
        }

        @Override
        public boolean isCompressed() {
            return false;
        }

        @Override
        public boolean isArchive() {
            return false;
        }
    }
}