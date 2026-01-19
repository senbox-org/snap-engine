package eu.esa.snap.dataio.cached;

import eu.esa.snap.core.dataio.cache.CacheManager;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.io.File;
import java.io.IOException;

public class DeleteMeMain {

    public static void main(String[] args) throws IOException {
        final PaceOCICachedProductReader deleteMeReader = new PaceOCICachedProductReader(null);
        CacheManager cacheManager = CacheManager.getInstance();

        final File input = new File("C:\\Satellite\\SNAP_test\\sensors_platforms\\PACE\\OCI\\PACE_OCI.20240514T094709.L1B.nc");
        try (Product product = deleteMeReader.readProductNodes(input, null)) {
            System.out.println(cacheManager.getSizeInBytes());

            Band band = product.getBand("height");
            ProductData productData = ProductData.createInstance(band.getDataType(), 512 * 512);
            /*band.readRasterData(128, 200, 512, 512, productData);

            band.readRasterData(128, 200, 512, 512, productData);
            band.readRasterData(126, 198, 512, 512, productData);

            System.out.println(cacheManager.getSizeInBytes());

            band = product.getBand("sensor_azimuth");
            productData = ProductData.createInstance(band.getDataType(), 512 * 512);
            band.readRasterData(300, 0, 512, 512, productData);
            band.readRasterData(400, 0, 512, 512, productData);
            band.readRasterData(500, 0, 512, 512, productData);

            System.out.println(cacheManager.getSizeInBytes());


            band = product.getBand("rhot_blue_001");
            productData = ProductData.createInstance(band.getDataType(), 512 * 512);
            band.readRasterData(400, 0, 512, 512, productData);

            System.out.println(cacheManager.getSizeInBytes());
*/
            band = product.getBand("rhot_blue_002");
            productData = ProductData.createInstance(band.getDataType(), 16 * 16);
            band.readRasterData(0, 0, 16, 16, productData);

            //ProductData productData_2 = ProductData.createInstance(band.getDataType(), 512 * 512);
            //band.readRasterData(0, 1, 512, 512, productData_2);

            //System.out.println("data: " + productData.getElemFloatAt(512) + "   " + productData_2.getElemFloatAt(0));
            System.out.println("data: " + productData.getElemFloatAt(0));

            //ProductIO.writeProduct(product, "C:\\Satellite\\DELETE\\pace_copy.nc", "NetCDF4-CF");
        }

        System.out.println(cacheManager.getSizeInBytes());
    }
}
