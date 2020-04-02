package org.esa.snap.csv.dataio;

import org.esa.snap.core.dataio.geocoding.*;
import org.esa.snap.core.dataio.geocoding.forward.PixelForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelGeoIndexInverse;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

public class CsvTestUtils {

    public static Product createProductWithoutGeoCoding(int startValue, int width, int height) {
        final Product product = new Product("testProduct", "testType", width, height);
        fillBandDataFloat(product.addBand("radiance_1", ProductData.TYPE_FLOAT32), startValue);
        fillBandDataFloat(product.addBand("radiance_2", ProductData.TYPE_FLOAT64), 10 + startValue);
        fillBandDataInt(product.addBand("radiance_3", ProductData.TYPE_INT32), 100 + startValue);
        return product;
    }

    public static Product createProductWithPixelGeoCoding(int startValue, int width, int height) {
        final Product product = createProductWithoutGeoCoding(startValue, width, height);

        fillBandDataFloat(product.addBand("longitude", ProductData.TYPE_FLOAT32), -120 + startValue);
        fillBandDataFloat(product.addBand("latitude", ProductData.TYPE_FLOAT32), 20 + startValue);

        final GeoRaster geoRaster = new GeoRaster(null, null, "longitude", "latitude",
                2, 3, 1.3);

        final ForwardCoding forward = ComponentFactory.getForward(PixelForward.KEY);
        final InverseCoding inverse = ComponentFactory.getInverse(PixelGeoIndexInverse.KEY_INTERPOLATING);

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forward, inverse);
        product.setSceneGeoCoding(geoCoding);

        return product;
    }

    public static void fillBandDataFloat(Band band, int startValue) {
        final int rasterWidth = band.getRasterWidth();
        final int rasterHeight = band.getRasterHeight();

        final ProductData data = band.createCompatibleProductData(rasterWidth * rasterHeight);
        int value = startValue;
        int dataIndex = 0;
        for (int i = 0; i < rasterWidth; i++) {
            for (int j = 0; j < rasterHeight; j++) {
                data.setElemFloatAt(dataIndex++, value++);
            }
        }
        band.setData(data);
    }

    public static void fillBandDataInt(Band band, int startValue) {
        final int rasterWidth = band.getRasterWidth();
        final int rasterHeight = band.getRasterHeight();

        final ProductData data = band.createCompatibleProductData(rasterWidth * rasterHeight);
        int value = startValue;
        int dataIndex = 0;
        for (int i = 0; i < rasterWidth; i++) {
            for (int j = 0; j < rasterHeight; j++) {
                data.setElemIntAt(dataIndex++, value++);
            }
        }
        band.setData(data);
    }
}
