package eu.esa.snap.dataio.cached;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.io.File;
import java.io.IOException;

public class DeleteMeMain {

    public static void main(String[] args) throws IOException {
        final PaceOCICachedProductReader deleteMeReader = new PaceOCICachedProductReader(null);

        final File input = new File("C:\\Satellite\\SNAP_test\\sensors_platforms\\PACE\\OCI\\PACE_OCI.20240514T094709.L1B.nc");
        try (Product product = deleteMeReader.readProductNodes(input, null)) {

            Band band = product.getBand("height");
            ProductData productData = ProductData.createInstance(band.getDataType(), 512 * 512);
            band.readRasterData(128, 200, 512, 512, productData);

            band.readRasterData(128, 200, 512, 512, productData);
            band.readRasterData(126, 198, 512, 512, productData);

            band = product.getBand("sensor_azimuth");
            productData = ProductData.createInstance(band.getDataType(), 512 * 512);
            band.readRasterData(300, 0, 512, 512, productData);
            band.readRasterData(400, 0, 512, 512, productData);
            band.readRasterData(500, 0, 512, 512, productData);
        }
    }
}
