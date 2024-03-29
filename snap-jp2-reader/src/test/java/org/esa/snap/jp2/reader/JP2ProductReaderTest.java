package org.esa.snap.jp2.reader;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.test.LongTestRunner;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.runtime.LogUtils4Tests;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by jcoravu on 5/11/2019.
 */
@RunWith(LongTestRunner.class)
public class JP2ProductReaderTest {

    @BeforeClass
    public static void initialize() throws Exception {
        LogUtils4Tests.initLogger();
        OpenJPEGLibraryInstaller.install();
    }

    @Test
    public void testReadProduct() throws Exception {
        URL resource = getClass().getResource("S2_subset_sample.jp2");
        assertNotNull(resource);

        File productFile = new File(resource.toURI());

        ProductReader productReader = buildProductReader();
        Product product = productReader.readProductNodes(productFile, null);

        assertNotNull(product);
        assertNotNull(product.getFileLocation());
        assertNotNull(product.getSceneGeoCoding());
        assertNotNull(product.getName());
        assertNotNull(product.getPreferredTileSize());
        assertNotNull(product.getProductReader());
        assertEquals(product.getProductReader(), productReader);
        assertEquals(400, product.getSceneRasterWidth());
        assertEquals(300, product.getSceneRasterHeight());
        assertEquals(3, product.getNumBands());

        assertEquals(0, product.getMaskGroup().getNodeCount());

        Band band = product.getBandAt(0);
        assertNotNull(band);
        assertEquals("band_1", band.getName());
        assertEquals(400, band.getRasterWidth());
        assertEquals(300, band.getRasterHeight());
        assertEquals(21, band.getDataType());

        MultiLevelImage multiLevelImage = band.getSourceImage();
        assertNotNull(multiLevelImage);
        assertNotNull(multiLevelImage.getColorModel());
        assertNotNull(multiLevelImage.getData());
        assertNotNull(multiLevelImage.getBounds());

        Rectangle part = new Rectangle(0, 0, 10, 10);
        BufferedImage bufferedImage = multiLevelImage.getAsBufferedImage(part, multiLevelImage.getColorModel());
        assertNotNull(bufferedImage);

        int level = 0;
        int x = 60;
        int y = 60;
        PlanarImage image = (PlanarImage)band.getGeophysicalImage().getImage(level);
        assertNotNull(image);
        int tx = image.XToTileX(x);
        int ty = image.YToTileY(y);
        Raster tile = image.getTile(tx, ty);
        assertNotNull(tile);
        int pixelValue = tile.getSample(x, y, 0);
        assertEquals(2922, pixelValue);
    }

    @Test
    public void testReadProductSubset() throws Exception {
        URL resource = getClass().getResource("S2_subset_sample.jp2");
        assertNotNull(resource);

        File productFile = new File(resource.toURI());

        Rectangle subsetRegion = new Rectangle(100, 100, 278, 189);
        ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setNodeNames(new String[] { "band_2", "band_3"} );
        subsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRegion, 0));
        subsetDef.setSubSampling(1, 1);

        ProductReader productReader = buildProductReader();
        Product product = productReader.readProductNodes(productFile, subsetDef);

        assertNotNull(product);
        assertNotNull(product.getFileLocation());
        assertNotNull(product.getSceneGeoCoding());
        assertNotNull(product.getName());
        assertNotNull(product.getPreferredTileSize());
        assertNotNull(product.getProductReader());
        assertEquals(product.getProductReader(), productReader);
        assertEquals(278, product.getSceneRasterWidth());
        assertEquals(189, product.getSceneRasterHeight());
        assertEquals(2, product.getNumBands());

        assertEquals(0, product.getMaskGroup().getNodeCount());

        Band band = product.getBandAt(1);
        assertNotNull(band);
        assertEquals("band_3", band.getName());
        assertEquals(278, band.getRasterWidth());
        assertEquals(189, band.getRasterHeight());

        int[] pixels = new int[band.getRasterWidth() * band.getRasterHeight()];
        band.readPixels(0, 0, band.getRasterWidth(), band.getRasterHeight(), pixels, ProgressMonitor.NULL);
        assertEquals(3959, pixels[10]);
        assertEquals(3730, pixels[40]);
        assertEquals(4003, pixels[13]);
        assertEquals(3718, pixels[142]);
        assertEquals(3910, pixels[475]);
        assertEquals(3786, pixels[1012]);
        assertEquals(3783, pixels[1313]);
        assertEquals(3803, pixels[1231]);
        assertEquals(3793, pixels[555]);
        assertEquals(3919, pixels[365]);
        assertEquals(3862, pixels[678]);
        assertEquals(3813, pixels[931]);

        band = product.getBandAt(0);
        assertNotNull(band);
        assertEquals("band_2", band.getName());
        assertEquals(278, band.getRasterWidth());
        assertEquals(189, band.getRasterHeight());

        pixels = new int[band.getRasterWidth() * band.getRasterHeight()];
        band.readPixels(0, 0, band.getRasterWidth(), band.getRasterHeight(), pixels, ProgressMonitor.NULL);
        assertEquals(3651, pixels[0]);
        assertEquals(3416, pixels[20]);
        assertEquals(3410, pixels[123]);
        assertEquals(3546, pixels[342]);
        assertEquals(3407, pixels[875]);
        assertEquals(3492, pixels[1142]);
        assertEquals(3459, pixels[1213]);
        assertEquals(3414, pixels[1431]);
        assertEquals(3394, pixels[555]);
        assertEquals(3375, pixels[765]);
        assertEquals(3355, pixels[999]);
        assertEquals(3429, pixels[665]);
    }

    private static ProductReader buildProductReader() {
        JP2ProductReaderPlugin plugin = new JP2ProductReaderPlugin();
        return plugin.createReaderInstance();
    }
}
