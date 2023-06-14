package org.esa.snap.core.image;

import com.bc.ceres.annotation.STTM;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PyramidBuilderTest {
    private static String mlibStatus;
    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

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
        Path outputPath = testDir.newFolder("test-dir").toPath();

        Files.createDirectories(outputPath);

        final Path imageFile = Path.of(getClass().getResource("PyramidBuilderTestInput.jpg").toURI());
        int levelCount = 4;
        PyramidBuilder builder = new PyramidBuilder();
        builder.doit(imageFile, outputPath, "jpeg", levelCount, 100, 100);

        assertEquals(levelCount, Files.list(outputPath).collect(Collectors.toList()).size());
        Path lev0Dir = outputPath.resolve("0");
        Path lev1Dir = outputPath.resolve("1");
        Path lev2Dir = outputPath.resolve("2");
        Path lev3Dir = outputPath.resolve("3");
        assertEquals(151, Files.list(lev0Dir).collect(Collectors.toList()).size());
        assertTrue(Files.exists(lev0Dir.resolve("image.properties")));
        assertEquals(76, Files.list(lev1Dir).collect(Collectors.toList()).size());
        assertTrue(Files.exists(lev1Dir.resolve("image.properties")));
        assertEquals(2, Files.list(outputPath.resolve("2")).collect(Collectors.toList()).size());
        assertTrue(Files.exists(lev2Dir.resolve("image.properties")));
        assertEquals(95, Files.list(outputPath.resolve("3")).collect(Collectors.toList()).size());
        assertTrue(Files.exists(lev3Dir.resolve("image.properties")));

    }
}