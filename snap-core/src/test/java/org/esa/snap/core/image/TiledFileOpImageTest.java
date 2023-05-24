package org.esa.snap.core.image;

import com.bc.ceres.annotation.STTM;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@STTM("SNAP-3446")
public class TiledFileOpImageTest {
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
    public void testCreate() throws Exception {
        Path outputPath = testDir.newFolder("test-dir").toPath();
        Files.createDirectories(outputPath);
        final Path imageFile = Path.of(getClass().getResource("PyramidBuilderTestInput.jpg").toURI());
        PyramidBuilder builder = new PyramidBuilder();
        builder.doit(imageFile, outputPath, "jpeg", 4, 100, 100);

        Properties properties = new Properties();
        properties.put("tileFormat", "jpeg");
        TiledFileOpImage opImage = TiledFileOpImage.create(outputPath.resolve("2"), properties);
        assertEquals(375, opImage.getWidth());
        assertEquals(250, opImage.getHeight());
        Raster data = opImage.getData(new Rectangle(50, 50, 50, 50));
        assertEquals(3, opImage.getSampleModel().getNumBands());
        assertArrayEquals(new int[]{16, 62, 88}, data.getPixel(55, 55, new int[3]));
    }

}