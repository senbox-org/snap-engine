package org.esa.snap.engine_utilities.commons;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;


public class VirtualFileTest {


    @Test
    @STTM("SNAP-4105")
    public void test_close_deletesLocalTempFolder() throws IOException {
        Path input = Files.createTempFile("virtual-file", ".tmp");
        TestVirtualFile virtualFile = new TestVirtualFile(input);

        Path tempFolder = virtualFile.exposeLocalTempFolder();
        assertNotNull(tempFolder);
        assertTrue(Files.exists(tempFolder));
        assertTrue(Files.isDirectory(tempFolder));

        virtualFile.close();

        assertFalse(Files.exists(tempFolder));
        Files.deleteIfExists(input);
    }

    @Test
    @STTM("SNAP-4105")
    public void test_close_isIdempotent() throws IOException {
        Path input = Files.createTempFile("virtual-file", ".tmp");
        TestVirtualFile virtualFile = new TestVirtualFile(input);

        Path tempFolder = virtualFile.exposeLocalTempFolder();
        assertTrue(Files.exists(tempFolder));

        virtualFile.close();
        virtualFile.close();

        assertFalse(Files.exists(tempFolder));
        Files.deleteIfExists(input);
    }

    private static class TestVirtualFile extends VirtualFile {

        private TestVirtualFile(Path file) {
            super(file);
        }

        private Path exposeLocalTempFolder() throws IOException {
            return getLocalTempFolder();
        }
    }
}