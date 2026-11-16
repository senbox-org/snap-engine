package org.esa.snap.core.util.io;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class FileUtilsTest_RelativeUri {

    private Path root;
    private URI rootURI;

    @Before
    public void setUp() throws Exception {
        root = Path.of(".").toAbsolutePath().normalize();
        rootURI = root.toUri();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testRelativeInTheRootDir() {
        final Path relativeFile = root.resolve("lsmf");

        final URI uri = FileUtils.getRelativeUri(rootURI, relativeFile);

        assertEquals("lsmf", uri.toString());
    }

    @Test
    public void testRelativeOneDirDeeper() {
        final Path relativeDir = root.resolve("oneDirDeeper");
        final Path relativeFile = relativeDir.resolve("lsmf");

        final URI uri = FileUtils.getRelativeUri(rootURI, relativeFile);

        assertEquals("oneDirDeeper/lsmf", uri.toString());
    }

    @Test
    public void testRelativeOneDirHigher() throws Exception {
        final Path deeperRootDir = root.resolve("oneDirDeeper");
        final Path twoDeeperRootDir = deeperRootDir.resolve("secondDirDeeper");
        final Path relativeFileTwoDirsHigher = twoDeeperRootDir.resolve("../../lsmf").toAbsolutePath().normalize();

        final URI uri = FileUtils.getRelativeUri(twoDeeperRootDir.toUri(), relativeFileTwoDirsHigher);

        final String path = relativeFileTwoDirsHigher.toAbsolutePath().normalize().toString().replace("\\", "/");
        String expected = "file:" + (path.startsWith("/") ? path : "/" + path);

        //Fix when space on path
        expected = expected.replaceAll(" ","%20");

        assertEquals(expected, uri.toURL().toURI().toString());//from Path to URI returns a URI with file:/// (on Windows OS), so the chain .toURL().toURI() fix it
    }
}
