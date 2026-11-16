package org.esa.snap.engine_utilities.commons;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.runtime.internal.IOHelper;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class AbstractVirtualPathTest {


    @Test
    @STTM("SNAP-4105")
    public void test_close_deletesLocalTempFolder() throws IOException {
        TestVirtualPath virtualPath = new TestVirtualPath(true);

        Path tempFolder = virtualPath.makeLocalTempFolder();
        assertNotNull(tempFolder);
        assertTrue(Files.exists(tempFolder));
        assertTrue(Files.isDirectory(tempFolder));
        assertEquals(tempFolder, virtualPath.getTempDir());

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

        final Path baseDir = IOHelper.createATempDir();

        private TestVirtualPath(boolean copyFilesOnLocalDisk) {
            super(copyFilesOnLocalDisk);
        }

        @Override
        public Path buildPath(String first, String... more) {
            return Path.of(first, more);
        }

        @Override
        public String getFileSystemSeparator() {
            return IOHelper.SEPARATOR;
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
        public Path getBaseFile() {
            return baseDir;
        }

        @Override
        public Path getFile(String path) {
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