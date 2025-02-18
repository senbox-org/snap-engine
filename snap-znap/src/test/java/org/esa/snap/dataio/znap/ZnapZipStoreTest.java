package org.esa.snap.dataio.znap;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import static org.junit.Assert.*;

public class ZnapZipStoreTest {

    @Test
    @STTM("SNAP-3872")
    public void testZnapZipStoreHandlesSpacesInPath() throws IOException {

        Path tempDir = Files.createTempDirectory("znap_test");
        Path zipFilePath = tempDir.resolve("test file with spaces.znap.zip");

        HashMap<String, String> env = new HashMap<>();
        env.put("create", "true");
        try (FileSystem zipFs = FileSystems.newFileSystem(URI.create("jar:" + zipFilePath.toUri()), env)) {
            Path dummyFile = zipFs.getPath("dummy.txt");
            Files.write(dummyFile, "Dummy Text!".getBytes());
        }

        try {
            new ZnapZipStore(zipFilePath);
        } catch (IOException e) {
            fail("ZnapZipStore constructor threw an exception for a path with spaces: " + e.getMessage());
        } finally {
            Files.deleteIfExists(zipFilePath);
            Files.deleteIfExists(tempDir);
        }

    }
}