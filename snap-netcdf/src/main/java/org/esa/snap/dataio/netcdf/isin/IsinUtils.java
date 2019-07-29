package org.esa.snap.dataio.netcdf.isin;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.grid.isin.IsinAPI;
import org.esa.snap.core.util.grid.isin.IsinPoint;

import java.text.SimpleDateFormat;
import java.util.Date;

class IsinUtils {

    private static final SimpleDateFormat YYYYMMDD_FORMAT = new SimpleDateFormat("yyyyMMdd");

    private static final String VERSION = "v1";
    private static final String CENTRE = "MPC";
    private static final String STATUS = "O";

    static String createFileName(String platform, Date startDate, Date endDate, Date processingDate, String tileIndex) {
        return platform +
                "_OL_3_VEG_" +
                YYYYMMDD_FORMAT.format(startDate) +
                "_" +
                YYYYMMDD_FORMAT.format(endDate) +
                "_" +
                YYYYMMDD_FORMAT.format(processingDate) +
                "_" +
                tileIndex +
                "_" +
                VERSION +
                "_" +
                CENTRE +
                "_" +
                STATUS +
                ".nc";
    }

    static GeoLocations getGeoLocations(int tileH, int tileV, IsinAPI.Raster rasterSize) {
        final IsinAPI isinAPI = new IsinAPI(rasterSize);

        final IsinPoint tileDimensions = isinAPI.getTileDimensions();
        final int width = (int) tileDimensions.getX();
        final int height = (int) tileDimensions.getY();

        final float[] longitudes = new float[width * height];
        final float[] latitudes = new float[width * height];

        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final IsinPoint isinPoint = isinAPI.tileImageCoordinatesToGeo(x, y, tileH, tileV);

                longitudes[index] = (float) isinPoint.getX();
                latitudes[index] = (float) isinPoint.getY();
                ++index;
            }
        }

        final ProductData lonData = ProductData.createInstance(longitudes);
        final ProductData latData = ProductData.createInstance(latitudes);

        return new GeoLocations(lonData, latData);
    }

    static class GeoLocations {
        private final ProductData longitudes;
        private final ProductData latitudes;

        GeoLocations(ProductData longitudes, ProductData latitudes) {
            this.longitudes = longitudes;
            this.latitudes = latitudes;
        }

        ProductData getLongitudes() {
            return longitudes;
        }

        ProductData getLatitudes() {
            return latitudes;
        }
    }
}
