package org.esa.snap.dataio.netcdf;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.ProgressMonitor;
import eu.esa.snap.core.dataio.cache.CacheManager;
import eu.esa.snap.core.dataio.cache.ProductCache;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.cache.NetcdfCacheDataProvider;
import org.esa.snap.dataio.netcdf.cache.NetcdfCacheLayerMapper;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfNetCdfReaderPlugIn;
import org.esa.snap.dataio.netcdf.util.ArrayConverter;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;

public class DefaultNetCdfReaderLayerCacheTest {

    private static final int WIDTH = 6;
    private static final int HEIGHT = 4;

    @Test
    @STTM("SNAP-4190")
    public void testReadBandRasterData_singleLayer3DMapping_uses2DCacheRead() throws Exception {
        File tempFile = createSingleLayer3DFile();
        NetcdfFile netcdfFile = null;
        ProductCache productCache = null;
        DefaultNetCdfReader reader = null;
        try {
            netcdfFile = NetcdfFile.open(tempFile.getAbsolutePath());
            NetcdfCacheDataProvider provider = new NetcdfCacheDataProvider();
            provider.register("data_3d", netcdfFile.findVariable("data_3d"), new int[0], false,
                    ProductData.TYPE_INT32, ArrayConverter.IDENTITY, new Dimension(2, 2));

            productCache = new ProductCache(provider);
            CacheManager.getInstance().register(productCache);

            Product product = new Product("single-layer-3d", "test", WIDTH, HEIGHT);
            Band band = product.addBand("band", ProductData.TYPE_INT32);
            ProductData destBuffer = ProductData.createInstance(ProductData.TYPE_INT32, 3);

            reader = new DefaultNetCdfReader(new CfNetCdfReaderPlugIn());
            setField(reader, "cacheDataProvider", provider);
            setField(reader, "productCache", productCache);
            setField(reader, "cacheLayerMappings", createLayerMapping("band", "data_3d", 0));

            reader.readBandRasterDataImpl(1, 2, 3, 1,
                    1, 1, band, 0, 0, 3, 1, destBuffer, ProgressMonitor.NULL);

            assertArrayEquals(new int[]{13, 14, 15}, (int[]) destBuffer.getElems());
        } finally {
            if (reader != null) {
                reader.close();
            } else if (productCache != null) {
                CacheManager.getInstance().remove(productCache);
            }
            if (netcdfFile != null) {
                netcdfFile.close();
            }
            tempFile.delete();
        }
    }

    private static File createSingleLayer3DFile() throws IOException {
        File tempFile = File.createTempFile("DefaultNetCdfReaderLayerCacheTest", ".nc");
        NetcdfFileWriter writer = NetcdfFileWriter.createNew(
                NetcdfFileWriter.Version.netcdf3, tempFile.getAbsolutePath());

        writer.addDimension("time", 1);
        writer.addDimension("lat", HEIGHT);
        writer.addDimension("lon", WIDTH);
        Variable variable = writer.addVariable("data_3d", DataType.INT, "time lat lon");
        writer.create();

        int[] data = new int[WIDTH * HEIGHT];
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
        }

        try {
            writer.write(variable, new int[]{0, 0, 0},
                    Array.factory(DataType.INT, new int[]{1, HEIGHT, WIDTH}, data));
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            writer.close();
        }
        return tempFile;
    }

    private static Map<String, NetcdfCacheLayerMapper.LayerReference> createLayerMapping(String bandName,
                                                                                        String cacheKey,
                                                                                        int layer) {
        ProfileReadContextImpl context = new ProfileReadContextImpl(null);
        NetcdfCacheLayerMapper.mapBand(context, bandName, cacheKey, layer);
        return NetcdfCacheLayerMapper.getMappings(context);
    }

    private static void setField(DefaultNetCdfReader reader, String name, Object value) throws Exception {
        Field field = DefaultNetCdfReader.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(reader, value);
    }
}
