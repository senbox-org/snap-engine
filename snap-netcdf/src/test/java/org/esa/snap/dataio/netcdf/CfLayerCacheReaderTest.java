package org.esa.snap.dataio.netcdf;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfNetCdfReaderPlugIn;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

public class CfLayerCacheReaderTest {

    private static final int WIDTH = 6;
    private static final int HEIGHT = 4;

    private static File tempFile;

    @BeforeClass
    public static void createTestFile() throws IOException {
        NetCdfActivator.activate();

        tempFile = File.createTempFile(CfLayerCacheReaderTest.class.getSimpleName(), ".nc");
        NetcdfFileWriter writer = NetcdfFileWriter.createNew(
                NetcdfFileWriter.Version.netcdf3, tempFile.getAbsolutePath());

        writer.addDimension("time", 2);
        writer.addDimension("lat", HEIGHT);
        writer.addDimension("lon", WIDTH);

        Variable lat = writer.addVariable("lat", DataType.DOUBLE, "lat");
        lat.addAttribute(new Attribute("units", "degrees_north"));
        lat.addAttribute(new Attribute("standard_name", "latitude"));
        Variable lon = writer.addVariable("lon", DataType.DOUBLE, "lon");
        lon.addAttribute(new Attribute("units", "degrees_east"));
        lon.addAttribute(new Attribute("standard_name", "longitude"));
        Variable data = writer.addVariable("data", DataType.INT, "time lat lon");

        writer.create();

        double[] latValues = new double[HEIGHT];
        for (int y = 0; y < HEIGHT; y++) {
            latValues[y] = 50.0 - y;
        }
        double[] lonValues = new double[WIDTH];
        for (int x = 0; x < WIDTH; x++) {
            lonValues[x] = 10.0 + x;
        }

        int[] dataValues = new int[2 * HEIGHT * WIDTH];
        int index = 0;
        for (int time = 0; time < 2; time++) {
            int layerValue = time * 100;
            for (int pixel = 0; pixel < HEIGHT * WIDTH; pixel++) {
                dataValues[index++] = layerValue + pixel;
            }
        }

        try {
            writer.write(lat, Array.factory(DataType.DOUBLE, new int[]{HEIGHT}, latValues));
            writer.write(lon, Array.factory(DataType.DOUBLE, new int[]{WIDTH}, lonValues));
            writer.write(data, Array.factory(DataType.INT, new int[]{2, HEIGHT, WIDTH}, dataValues));
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            writer.close();
        }
    }

    @AfterClass
    public static void deleteTestFile() {
        if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
            tempFile.deleteOnExit();
        }
        System.gc();
    }

    @Test
    @STTM("SNAP-4190")
    public void test_readCfLayerBand_usesLayerCache() throws IOException {
        ProductReader reader = new CfNetCdfReaderPlugIn().createReaderInstance();
        Product product = reader.readProductNodes(tempFile, null);
        try {
            Band band = product.getBand("data_time2");
            assertNotNull(band);

            int[] actual = new int[6];
            band.readPixels(0, 0, 3, 2, actual);

            assertArrayEquals(new int[]{100, 101, 102, 106, 107, 108}, actual);
        } finally {
            product.dispose();
            reader.close();
        }
    }
}
