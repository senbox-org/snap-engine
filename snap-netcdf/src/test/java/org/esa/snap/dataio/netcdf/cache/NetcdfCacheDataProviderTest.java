package org.esa.snap.dataio.netcdf.cache;

import com.bc.ceres.annotation.STTM;
import eu.esa.snap.core.dataio.cache.DataBuffer;
import eu.esa.snap.core.dataio.cache.VariableDescriptor;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.util.ArrayConverter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class NetcdfCacheDataProviderTest {


    private static final int WIDTH = 6;
    private static final int HEIGHT = 4;

    private static File tempFile;
    private static NetcdfFile netcdfFile;


    @BeforeClass
    public static void createTestFile() throws IOException {
        tempFile = File.createTempFile("NetcdfCacheDataProviderTest", ".nc");
        NetcdfFileWriter writer = NetcdfFileWriter.createNew(
                NetcdfFileWriter.Version.netcdf3, tempFile.getAbsolutePath());

        writer.addDimension("lat", HEIGHT);
        writer.addDimension("lon", WIDTH);
        writer.addDimension("time", 2);

        Variable varData     = writer.addVariable("data", DataType.INT, "lat lon");
        Variable varShifted  = writer.addVariable("data_shifted", DataType.INT, "lat lon");
        varShifted.addAttribute(new Attribute("LONGITUDE_SHIFTED_180", 1));
        Variable varFlipped  = writer.addVariable("data_flipped", DataType.INT, "lat lon");
        Variable var3d       = writer.addVariable("data_3d", DataType.INT, "time lat lon");

        writer.create();

        int[] flat = new int[HEIGHT * WIDTH];
        for (int i = 0; i < flat.length; i++) {
            flat[i] = i;
        }
        Array arr2d = Array.factory(DataType.INT, new int[]{HEIGHT, WIDTH}, flat);

        int[] t1 = new int[HEIGHT * WIDTH];
        for (int i = 0; i < t1.length; i++) {
            t1[i] = flat[i] + HEIGHT * WIDTH;
        }

        try {
            writer.write(varData, arr2d);
            writer.write(varShifted, arr2d);
            writer.write(varFlipped, arr2d);
            writer.write(var3d, new int[]{0, 0, 0}, Array.factory(DataType.INT, new int[]{1, HEIGHT, WIDTH}, flat));
            writer.write(var3d, new int[]{1, 0, 0}, Array.factory(DataType.INT, new int[]{1, HEIGHT, WIDTH}, t1));
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
        writer.close();

        netcdfFile = NetcdfFile.open(tempFile.getAbsolutePath());
    }

    @AfterClass
    public static void deleteTestFile() throws IOException {
        if (netcdfFile != null) {
            netcdfFile.close();
        }
        if (tempFile != null) {
            tempFile.delete();
        }
    }

    @Test
    @STTM("SNAP-4190")
    public void test_getVariableDescriptor_returnsCorrectValues() throws IOException {
        NetcdfCacheDataProvider provider = new NetcdfCacheDataProvider(netcdfFile);
        provider.register("band", netcdfFile.findVariable("data"), new int[0], false,
                ProductData.TYPE_INT32, ArrayConverter.IDENTITY, new Dimension(2, 2));

        VariableDescriptor descriptor = provider.getVariableDescriptor("band");

        assertEquals(WIDTH, descriptor.width);
        assertEquals(HEIGHT, descriptor.height);
        assertEquals(ProductData.TYPE_INT32, descriptor.dataType);
        assertEquals(2, descriptor.tileWidth);
        assertEquals(2, descriptor.tileHeight);
    }

    @Test
    @STTM("SNAP-4190")
    public void test_readCacheBlock_simpleRead() throws IOException {
        NetcdfCacheDataProvider provider = new NetcdfCacheDataProvider(netcdfFile);
        provider.register("band", netcdfFile.findVariable("data"), new int[0], false,
                ProductData.TYPE_INT32, ArrayConverter.IDENTITY, new Dimension(WIDTH, HEIGHT));

        DataBuffer result = provider.readCacheBlock("band", new int[]{1, 2}, new int[]{2, 3}, null);

        assertArrayEquals(new int[]{8, 9, 10, 14, 15, 16}, (int[]) result.getData().getElems());
    }

    @Test
    @STTM("SNAP-4190")
    public void test_readCacheBlock_withFlipY() throws IOException {
        NetcdfCacheDataProvider provider = new NetcdfCacheDataProvider(netcdfFile);
        provider.register("band", netcdfFile.findVariable("data_flipped"), new int[0], true,
                ProductData.TYPE_INT32, ArrayConverter.IDENTITY, new Dimension(WIDTH, HEIGHT));

        DataBuffer result = provider.readCacheBlock("band", new int[]{0, 0}, new int[]{2, 3}, null);

        assertArrayEquals(new int[]{18, 19, 20, 12, 13, 14}, (int[]) result.getData().getElems());
    }

    @Test
    @STTM("SNAP-4190")
    public void test_readCacheBlock_shifted180_tileInLeftCanvasHalf() throws IOException {
        NetcdfCacheDataProvider provider = new NetcdfCacheDataProvider(netcdfFile);
        provider.register("band", netcdfFile.findVariable("data_shifted"), new int[0], false,
                ProductData.TYPE_INT32, ArrayConverter.IDENTITY, new Dimension(WIDTH, HEIGHT));

        DataBuffer result = provider.readCacheBlock("band", new int[]{0, 0}, new int[]{2, 2}, null);

        assertArrayEquals(new int[]{3, 4, 9, 10}, (int[]) result.getData().getElems());
    }

    @Test
    @STTM("SNAP-4190")
    public void test_readCacheBlock_shifted180_tileInRightCanvasHalf() throws IOException {
        NetcdfCacheDataProvider provider = new NetcdfCacheDataProvider(netcdfFile);
        provider.register("band", netcdfFile.findVariable("data_shifted"), new int[0], false,
                ProductData.TYPE_INT32, ArrayConverter.IDENTITY, new Dimension(WIDTH, HEIGHT));

        DataBuffer result = provider.readCacheBlock("band", new int[]{0, 3}, new int[]{2, 2}, null);

        assertArrayEquals(new int[]{0, 1, 6, 7}, (int[]) result.getData().getElems());
    }

    @Test
    @STTM("SNAP-4190")
    public void test_readCacheBlock_shifted180_tileCrossesBoundary() throws IOException {
        NetcdfCacheDataProvider provider = new NetcdfCacheDataProvider(netcdfFile);
        provider.register("band", netcdfFile.findVariable("data_shifted"), new int[0], false,
                ProductData.TYPE_INT32, ArrayConverter.IDENTITY, new Dimension(WIDTH, HEIGHT));

        DataBuffer result = provider.readCacheBlock("band", new int[]{0, 1}, new int[]{2, 4}, null);

        assertArrayEquals(new int[]{4, 5, 0, 1, 10, 11, 6, 7}, (int[]) result.getData().getElems());
    }

    @Test
    @STTM("SNAP-4190")
    public void test_readCacheBlock_3dVariable_withImageOrigin() throws IOException {
        NetcdfCacheDataProvider provider = new NetcdfCacheDataProvider(netcdfFile);
        provider.register("band", netcdfFile.findVariable("data_3d"), new int[]{1}, false,
                ProductData.TYPE_INT32, ArrayConverter.IDENTITY, new Dimension(WIDTH, HEIGHT));

        DataBuffer result = provider.readCacheBlock("band", new int[]{0, 0}, new int[]{2, 3}, null);

        assertArrayEquals(new int[]{24, 25, 26, 30, 31, 32}, (int[]) result.getData().getElems());
    }
}