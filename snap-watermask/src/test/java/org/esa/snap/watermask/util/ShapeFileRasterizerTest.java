package org.esa.snap.watermask.util;

import com.bc.ceres.core.runtime.internal.IOHelper;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;
import org.esa.snap.watermask.operator.WatermaskUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ShapeFileRasterizerTest  {

    @Test
    public void testImageCreation() throws Exception {
        final Path targetDir = IOHelper.createATempDir();
        final ShapeFileRasterizer rasterizer = new ShapeFileRasterizer(targetDir);
        final URL shapeUrl = getClass().getResource("e000n05f.shp");
        final URI uri = new URI(shapeUrl.toString());
        final Path shapeFile = Path.of(uri);
        final int resolution = WatermaskUtils.computeSideLength(150);
        final BufferedImage image = rasterizer.createImage(shapeFile, resolution);

        // test some known values
        final Raster imageData = image.getData();
        assertEquals(0, imageData.getSample(526, 92, 0));
        assertEquals(1, imageData.getSample(312, 115, 0));
        assertEquals(1, imageData.getSample(141, 187, 0));
        assertEquals(0, imageData.getSample(197, 27, 0));
        assertEquals(1, imageData.getSample(308, 701, 0));
    }

     @Test
    public void testFromZip() throws Exception {
        final Path targetDir = IOHelper.createATempDir();
        final ShapeFileRasterizer rasterizer = new ShapeFileRasterizer(targetDir);
        final URL shapeUrl = getClass().getResource("e000n05f.zip");
         final URI uri = new URI(shapeUrl.toString());
         final int tileSize = WatermaskUtils.computeSideLength(150);
         final List<Path> tempFiles;
         final ZipFile zipFile = new ZipFile(uri.getPath());
         try {
             tempFiles = rasterizer.createTempFiles(zipFile);
         } finally {
             zipFile.close();
         }

         BufferedImage image = null;
         for (Path file : tempFiles) {
             if (file.getFileName().toString().endsWith("shp")) {
                 image = rasterizer.createImage(file, tileSize);
             }
         }
         assertNotNull(image);

//        // test some known values
        final Raster imageData = image.getData();
        assertEquals(0, imageData.getSample(526, 92, 0));
        assertEquals(1, imageData.getSample(312, 115, 0));
        assertEquals(1, imageData.getSample(141, 187, 0));
        assertEquals(0, imageData.getSample(197, 27, 0));
        assertEquals(1, imageData.getSample(308, 701, 0));

    }

}
