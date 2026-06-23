package org.esa.snap.dataio.gdal.gdal.reader;

import com.bc.ceres.annotation.STTM;
import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.dataio.gdal.reader.plugins.GTiffDriverProductReaderPlugIn;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Adrian Drăghici
 */
public class GTiffDriverProductReaderTest extends AbstractTestDriverProductReader {

    public GTiffDriverProductReaderTest() {
    }

    @Test
    @STTM("SNAP-3567")
    public void testReadProduct() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            final File productFile = this.gdalTestsFolderPath.resolve("s1a-1-cog.tiff").toFile();

            final GDALProductReader reader = buildProductReader();
            final Product product = reader.readProductNodes(productFile, null);
            assertNotNull(product.getFileLocation());
            assertNotNull(product.getName());
            assertNotNull(product.getPreferredTileSize());
            assertNotNull(product.getProductReader());
            assertEquals(product.getProductReader(), reader);
            assertEquals("GeoTIFF (GDAL)", product.getProductType());
            assertEquals(25862, product.getSceneRasterWidth());
            assertEquals(16721, product.getSceneRasterHeight());

            final GeoCoding geoCoding = product.getSceneGeoCoding();
            assertNotNull(geoCoding);
            final CoordinateReferenceSystem coordinateReferenceSystem = geoCoding.getGeoCRS();
            assertNotNull(coordinateReferenceSystem);
            assertNotNull(coordinateReferenceSystem.getName());
            assertEquals("WGS84(DD)", coordinateReferenceSystem.getName().getCode());

            assertEquals(1, product.getMaskGroup().getNodeCount());

            assertEquals(1, product.getBands().length);

            final Band band = product.getBand("band_1");
            assertEquals(21, band.getDataType());
            assertEquals(432438502, band.getNumDataElems());
            assertEquals("band_1", band.getName());
            assertEquals(25862, band.getRasterWidth());
            assertEquals(16721, band.getRasterHeight());

            assertEquals(65, band.getSampleInt(8755, 14123));
            assertEquals(82, band.getSampleInt(3919, 6185));
            assertEquals(61, band.getSampleInt(8352, 9997));
            assertEquals(109, band.getSampleInt(16991, 3182));
            assertEquals(78, band.getSampleInt(9333, 10759));
            assertEquals(137, band.getSampleInt(3513, 16410));
            assertEquals(0, band.getSampleInt(25738, 8239));
            assertEquals(107, band.getSampleInt(14099, 9866));
            assertEquals(103, band.getSampleInt(4935, 7929));
            assertEquals(83, band.getSampleInt(8522, 12413));
        }
    }

    @Test
    @STTM("SNAP-3567")
    public void testReadProductPixelSubset() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            final File productFile = this.gdalTestsFolderPath.resolve("s1a-1-cog.tiff").toFile();

            final ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.setNodeNames(new String[]{"band_1"});
            subsetDef.setSubsetRegion(new PixelSubsetRegion(new Rectangle(21410, 4530, 210, 200), 0));
            subsetDef.setSubSampling(1, 1);

            final GDALProductReader reader = buildProductReader();
            final Product product = reader.readProductNodes(productFile, subsetDef);
            assertNotNull(product.getFileLocation());
            assertNotNull(product.getName());
            assertNotNull(product.getPreferredTileSize());
            assertNotNull(product.getProductReader());
            assertEquals(product.getProductReader(), reader);
            assertEquals("GeoTIFF (GDAL)", product.getProductType());
            assertEquals(210, product.getSceneRasterWidth());
            assertEquals(200, product.getSceneRasterHeight());

            final GeoCoding geoCoding = product.getSceneGeoCoding();
            assertNotNull(geoCoding);
            final CoordinateReferenceSystem coordinateReferenceSystem = geoCoding.getGeoCRS();
            assertNotNull(coordinateReferenceSystem);
            assertNotNull(coordinateReferenceSystem.getName());
            assertEquals("WGS84(DD)", coordinateReferenceSystem.getName().getCode());

            assertEquals(0, product.getMaskGroup().getNodeCount());

            assertEquals(1, product.getBands().length);

            final Band band = product.getBand("band_1");
            assertEquals(21, band.getDataType());
            assertEquals(42000, band.getNumDataElems());
            assertEquals("band_1", band.getName());
            assertEquals(210, band.getRasterWidth());
            assertEquals(200, band.getRasterHeight());

            assertEquals(62, band.getSampleInt(0, 0));
            assertEquals(104, band.getSampleInt(110, 110));
            assertEquals(97, band.getSampleInt(199, 199));
            assertEquals(110, band.getSampleInt(198, 165));
            assertEquals(75, band.getSampleInt(120, 198));
            assertEquals(103, band.getSampleInt(50, 50));
            assertEquals(55, band.getSampleInt(100, 100));
            assertEquals(103, band.getSampleInt(200, 169));
            assertEquals(52, band.getSampleInt(156, 187));
            assertEquals(154, band.getSampleInt(209, 199));
        }
    }

    private static GDALProductReader buildProductReader() {
        final GTiffDriverProductReaderPlugIn readerPlugin = new GTiffDriverProductReaderPlugIn();
        return (GDALProductReader) readerPlugin.createReaderInstance();
    }
}
