package org.esa.snap.dataio.netcdf;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.io.File;
import java.io.IOException;

public class DeleteMeMain {

    public static void main(String[] args) throws IOException {
        final DeleteMeReader deleteMeReader = new DeleteMeReader(null);

        final File input = new File("C:\\Satellite\\SNAP_test\\sensors_platforms\\PACE\\OCI\\PACE_OCI.20240514T094709.L1B.nc");
        try (Product product = deleteMeReader.readProductNodes(input, null)) {
            final Band band = product.getBand("height");
            ProductData productData = ProductData.createInstance(band.getDataType(), 512 * 512);
            band.readRasterData(128, 200, 512, 512, productData);

            band.readRasterData(128, 200, 512, 512, productData);
            band.readRasterData(126, 198, 512, 512, productData);

        }
    }
}
