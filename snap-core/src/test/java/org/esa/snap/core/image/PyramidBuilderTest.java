package org.esa.snap.core.image;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.runtime.internal.IOHelper;
import com.bc.ceres.core.runtime.internal.UrlHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PyramidBuilderTest {
    private static String mlibStatus;
    public Path testDir = IOHelper.createATempDir();

    @BeforeClass
    public static void beforeClass() throws Exception {
        mlibStatus = System.getProperty("com.sun.media.jai.disableMediaLib", "false");
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
    }

    @AfterClass
    public static void afterClass() {
        System.setProperty("com.sun.media.jai.disableMediaLib", mlibStatus);
    }

    @Test
    @STTM("SNAP-3446")
    public void testDoit() throws Exception {
        Path outputPath = testDir.resolve("test-dir");

        Files.createDirectories(outputPath);

        final URL fileURL = getClass().getResource("PyramidBuilderTestInput.jpg");
        assertNotNull(fileURL);
        final Path imageFile = UrlHelper.urlToFile(fileURL);
        int levelCount = 4;
        PyramidBuilder builder = new PyramidBuilder();
        builder.doit(imageFile, outputPath, "jpeg", levelCount, 100, 100);

        assertEquals(levelCount, IOHelper.listFiles(outputPath).size());
        Path lev0Dir = outputPath.resolve("0");
        Path lev1Dir = outputPath.resolve("1");
        Path lev2Dir = outputPath.resolve("2");
        Path lev3Dir = outputPath.resolve("3");
        assertEquals(151, IOHelper.listFiles(lev0Dir).size());
        assertTrue(Files.exists(lev0Dir.resolve("image.properties")));
        assertEquals(76, IOHelper.listFiles(lev1Dir).size());
        assertTrue(Files.exists(lev1Dir.resolve("image.properties")));
        assertEquals(2, IOHelper.listFiles(outputPath.resolve("2")).size());
        assertTrue(Files.exists(lev2Dir.resolve("image.properties")));
        assertEquals(95, IOHelper.listFiles(outputPath.resolve("3")).size());
        assertTrue(Files.exists(lev3Dir.resolve("image.properties")));

    }
}