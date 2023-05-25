package org.esa.snap.dataio.gdal.writer;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.dataio.gdal.reader.plugins.AbstractDriverProductReaderPlugIn;
import org.esa.snap.dataio.gdal.writer.plugins.AbstractDriverProductWriterPlugIn;
import org.esa.snap.engine_utilities.utils.TestUtil;
import org.esa.lib.gdal.activator.GDALDriverInfo;
import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.gdal.GDALLoader;
import org.esa.snap.dataio.gdal.drivers.GDAL;
import org.esa.snap.dataio.gdal.drivers.GDALConstConstants;
import org.geotools.referencing.CRS;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.JAI;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * @author Jean Coravu
 */
public abstract class AbstractTestDriverProductWriter {
    private static Path testsFolderPath;

    private final String driverName;
    private final String fileExtension;
    private final String driverCreationTypes;
    private final AbstractDriverProductReaderPlugIn readerPlugIn;
    private final AbstractDriverProductWriterPlugIn writerPlugIn;

    protected AbstractTestDriverProductWriter(String driverName, String fileExtension, String driverCreationTypes,
                                              AbstractDriverProductReaderPlugIn readerPlugIn, AbstractDriverProductWriterPlugIn writerPlugIn) {

        this.driverName = driverName;
        this.fileExtension = fileExtension;
        this.driverCreationTypes = driverCreationTypes;
        this.readerPlugIn = readerPlugIn;
        this.writerPlugIn = writerPlugIn;
    }

    @BeforeClass
    public static void oneTimeSetUp() throws IOException {
        testsFolderPath = Files.createTempDirectory("_temp");
        if (!Files.exists(testsFolderPath)) {
            fail("The test directory path '"+testsFolderPath+"' is not valid.");
        }
    }

    @AfterClass
    public static void oneTimeTearDown() {
        if (!FileUtils.deleteTree(testsFolderPath.toFile())) {
            fail("Unable to delete test directory.");
        }
    }

    @Before
    public final void setUp() {
        assumeTrue(TestUtil.testdataAvailable());
    }

    @Test
    public final void testWriteFileOnDisk() throws IOException, FactoryException, TransformException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            int sceneRasterWidth = 20;
            int sceneRasterHeight = 30;
            double originX = 0.0d;
            double originY = 0.0d;
            double pixelSizeX = 1.234d;
            double pixelSizeY = 5.678d;
            String wellKnownText = "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]";
            CoordinateReferenceSystem crsToSave = CRS.parseWKT(wellKnownText);
            GeoCoding geoCodingToSave = new CrsGeoCoding(crsToSave, sceneRasterWidth, sceneRasterHeight, originX, originY, pixelSizeX, pixelSizeY);

            Set<String> driverNamesToIgnoreGeoCoding = new HashSet<>();
            driverNamesToIgnoreGeoCoding.add("netCDF");
            driverNamesToIgnoreGeoCoding.add("NITF");
            driverNamesToIgnoreGeoCoding.add("ILWIS");
            driverNamesToIgnoreGeoCoding.add("RMF");
            driverNamesToIgnoreGeoCoding.add("MFF");

            GDALDriverInfo driverInfo = this.writerPlugIn.getWriterDriver();

            assertEquals(this.driverName, driverInfo.getDriverName());
            assertEquals(this.driverName, this.readerPlugIn.getDriverName());

            assertEquals(this.fileExtension, driverInfo.getExtensionName());

            if (this.driverCreationTypes != null && driverInfo.getCreationDataTypes() != null) {
                StringTokenizer str = new StringTokenizer(this.driverCreationTypes, " ");
                while (str.hasMoreTokens()) {
                    String gdalDataTypeName = str.nextToken();
                    int gdalDataType = GDAL.getDataTypeByName(gdalDataTypeName);
                    boolean result = driverInfo.canExportProduct(gdalDataType);
                    assertTrue(result);
                }
            } else if (this.driverCreationTypes != null) {
                fail("The driver creation types does not match.");
            } else if (driverInfo.getCreationDataTypes() != null) {
                fail("The driver creation types does not match.");
            }

            int gdalDataType = getBandDataTypeToSave(driverInfo);
            boolean canIgnore = driverNamesToIgnoreGeoCoding.contains(driverInfo.getDriverName());
            int bandDataType = GDALLoader.getInstance().getBandDataType(gdalDataType);
            File file = new File(testsFolderPath.toFile(), this.driverName + driverInfo.getExtensionName());
            try {
                Product product = buildProductToSave(driverInfo, canIgnore, sceneRasterWidth, sceneRasterHeight, geoCodingToSave, bandDataType);

                checkSaveProductToFile(file, this.writerPlugIn, product);

                checkReadProductFromFile(file, this.readerPlugIn, product, canIgnore);
            } finally {
                try {
                    Thread.sleep(100);//pause the thread for waiting resources releases
                } catch (Exception ignored) {
                    //nothing to do
                }
                file.delete();
            }
        }
    }

    private static int getBandDataTypeToSave(GDALDriverInfo driverInfo) {
        int gdalDataType = GDALConstConstants.gdtByte();
        String creationDataTypes = driverInfo.getCreationDataTypes();
        if (driverInfo.getCreationDataTypes() != null) {
            // get the first data type
            int index = creationDataTypes.indexOf(" ");
            if (index < 0) {
                index = creationDataTypes.length();
            }
            String gdalDataTypeName = creationDataTypes.substring(0, index).trim();
            gdalDataType = GDAL.getDataTypeByName(gdalDataTypeName);
        }
        return gdalDataType;
    }

    private static Product buildProductToSave(GDALDriverInfo driverInfo, boolean canIgnore, int sceneRasterWidth, int sceneRasterHeight, GeoCoding geoCodingToSave, int bandDataType) {
        Product product = new Product("tempProduct", driverInfo.getWriterPluginFormatName(), sceneRasterWidth, sceneRasterHeight);
        if (!canIgnore) {
            product.setSceneGeoCoding(geoCodingToSave);
        }

        product.setPreferredTileSize(JAI.getDefaultTileSize());
        Band firstBand = product.addBand("band_1", bandDataType);

        ProductData data = firstBand.createCompatibleRasterData();
        for (int i = 0; i < firstBand.getRasterWidth() * firstBand.getRasterHeight(); i++) {
            int value = i + 1;
            if (bandDataType == ProductData.TYPE_UINT8) {
                data.setElemIntAt(i, value);
            } else if (bandDataType == ProductData.TYPE_INT16) {
                data.setElemIntAt(i, value);
            } else if (bandDataType == ProductData.TYPE_UINT16) {
                data.setElemUIntAt(i, value);
            } else if (bandDataType == ProductData.TYPE_INT32) {
                data.setElemLongAt(i, value);
            } else if (bandDataType == ProductData.TYPE_UINT32) {
                data.setElemLongAt(i, value);
            } else if (bandDataType == ProductData.TYPE_FLOAT32) {
                data.setElemFloatAt(i, value);
            } else if (bandDataType == ProductData.TYPE_FLOAT64) {
                data.setElemDoubleAt(i, value);
            }
        }
        firstBand.setData(data);

        return product;
    }

    private static void checkSaveProductToFile(File file, AbstractDriverProductWriterPlugIn writerPlugIn, Product productToSave) throws IOException {
        ProductWriter productWriter = writerPlugIn.createWriterInstance();
        try {
            productWriter.writeProductNodes(productToSave, file);

            int width = productToSave.getSceneRasterWidth();
            int height = productToSave.getSceneRasterHeight();
            int bandCount = productToSave.getNumBands();
            for (int i = 0; i < bandCount; i++) {
                Band band = productToSave.getBandAt(i);
                productWriter.writeBandRasterData(band, 0, 0, width, height, band.getData(), ProgressMonitor.NULL);
            }
            productWriter.flush();
        } finally {
            productWriter.close();
        }
        assertTrue(file.length() > 0);
    }

    private static void checkReadProductFromFile(File file, AbstractDriverProductReaderPlugIn readerPlugIn, Product savedProduct, boolean canIgnoreGeoCoding) throws IOException {
        GDALProductReader reader = (GDALProductReader)readerPlugIn.createReaderInstance();
        Product finalProduct = reader.readProductNodes(file, null);
        assertNotNull(finalProduct);

        if (!canIgnoreGeoCoding) {
            GeoCoding loadedGeoCoding = finalProduct.getSceneGeoCoding();
            assertNotNull(loadedGeoCoding);

            GeoCoding geoCodingToSave = savedProduct.getSceneGeoCoding();
            CoordinateReferenceSystem crsToSave = geoCodingToSave.getGeoCRS();

            CoordinateReferenceSystem loadedGeoCodingGeoCRS = loadedGeoCoding.getGeoCRS();
            assertEquals(crsToSave.getCoordinateSystem().getDimension(), loadedGeoCodingGeoCRS.getCoordinateSystem().getDimension());
            assertEquals(crsToSave.getCoordinateSystem().getName().getVersion(), loadedGeoCodingGeoCRS.getCoordinateSystem().getName().getVersion());
            assertNull(loadedGeoCodingGeoCRS.getCoordinateSystem().getRemarks());
            assertNull(loadedGeoCodingGeoCRS.getCoordinateSystem().getName().getAuthority());
        }

        assertEquals(savedProduct.getSceneRasterWidth(), finalProduct.getSceneRasterWidth());
        assertEquals(savedProduct.getSceneRasterHeight(), finalProduct.getSceneRasterHeight());
        assertEquals(savedProduct.getNumBands(), finalProduct.getNumBands());
        finalProduct.dispose();
    }
}
