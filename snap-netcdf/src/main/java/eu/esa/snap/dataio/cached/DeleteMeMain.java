package eu.esa.snap.dataio.cached;

import eu.esa.snap.core.dataio.cache.CacheManager;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.*;

import java.io.File;
import java.io.IOException;

public class DeleteMeMain {

    public static void main(String[] args) throws IOException {
        final PaceOCICachedProductReader deleteMeReader = new PaceOCICachedProductReader(null);
        CacheManager cacheManager = CacheManager.getInstance();
        final float MB = 1024*1024;

        final File input = new File("C:\\Satellite\\SNAP_test\\sensors_platforms\\PACE\\OCI\\PACE_OCI.20240514T094709.L1B.nc");
        try (Product product = deleteMeReader.readProductNodes(input, null)) {
            GeoCoding sceneGeoCoding = product.getSceneGeoCoding();
            GeoPos geoPos = sceneGeoCoding.getGeoPos(new PixelPos(0.5, 0.5), null);
            System.out.println("geoPos = " + geoPos.lon);

            System.out.println(cacheManager.getSizeInBytes()/MB);



            Band band = product.getBand("longitude");
            ProductData productData = ProductData.createInstance(band.getDataType(), 1202 * 512);
            ProductData productDataSmall = ProductData.createInstance(band.getDataType(), 1202 * 173);
            band.readRasterData(0, 0, 1202, 512, productData);
            System.out.println(productData.getElemFloatAt(0));

            band.readRasterData(0, 512, 1202, 512, productData);
            band.readRasterData(0, 1024, 1202, 512, productData);
            band.readRasterData(0, 1536, 1202, 173, productDataSmall);


            System.out.println(cacheManager.getSizeInBytes()/MB );
        }

        System.out.println(cacheManager.getSizeInBytes());
    }
}
