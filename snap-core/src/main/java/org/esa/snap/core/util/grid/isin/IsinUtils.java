package org.esa.snap.core.util.grid.isin;

import org.esa.snap.core.datamodel.*;

import java.text.SimpleDateFormat;
import java.util.Date;

public class IsinUtils {

    private static final SimpleDateFormat YYYYMMDD_FORMAT = new SimpleDateFormat("yyyyMMdd");

    private static final String VERSION = "v1";
    private static final String CENTRE = "MPC";
    private static final String STATUS = "O";

    public static String createFileName(String platform, Date startDate, Date endDate, Date processingDate, String tileIndex) {
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

    public static Product createMpcVegetationPrototype(int tileH, int tileV, IsinAPI.Raster raster) {
        final IsinAPI isinAPI = new IsinAPI(raster);
        final IsinPoint tileDimensions = isinAPI.getTileDimensions();
        final int rasterWidth = (int) tileDimensions.getX();
        final int rasterHeight = (int) tileDimensions.getY();

        final Product product = new Product("MPC_VEG_OL_L3", "Level3", rasterWidth, rasterHeight);

        final GeoLocations geoLocations = getGeoLocations(tileH, tileV, raster);
        final Band lonBand = new Band("lon", ProductData.TYPE_FLOAT32, rasterWidth, rasterHeight);
        lonBand.setRasterData(geoLocations.getLongitudes());
        lonBand.setNoDataValue(Double.NaN);
        lonBand.setUnit("degrees_east");
        product.addBand(lonBand);

        final Band latBand = new Band("lat", ProductData.TYPE_FLOAT32, rasterWidth, rasterHeight);
        latBand.setRasterData(geoLocations.getLatitudes());
        latBand.setNoDataValue(Double.NaN);
        latBand.setUnit("degrees_north");
        product.addBand(latBand);

        addVariables(rasterWidth, rasterHeight, product);

        final MetadataElement metadataRoot = product.getMetadataRoot();
        metadataRoot.addAttribute(new MetadataAttribute("title", ProductData.createInstance("OLCI Level 3 vegetation data"), true));
        metadataRoot.addAttribute(new MetadataAttribute("institution", ProductData.createInstance("Brockmann Consult GmbH"), true));
        metadataRoot.addAttribute(new MetadataAttribute("source", ProductData.createInstance("OLCI Level 2 Land data (OLCI L2 L)"), true));
        // @todo 1 tb/tb add references section, ask Carsten about content
        metadataRoot.addAttribute(new MetadataAttribute("comment", ProductData.createInstance("This dataset was produced at Brockmann Consult GmbH for the Sentinel-3 Mission Performance Centre under ESA contract no. TODO"), true));

        // @todo 1 tb/tb add standard names
        // standard name: normalized_difference_vegetation_index
        // long name: add mean value, std dev and counts
        return product;
    }

    private static void addVariables(int rasterWidth, int rasterHeight, Product product) {
        final int rasterSize = rasterHeight * rasterWidth;
        final Band ogvi_mean = new Band("OGVI_mean", ProductData.TYPE_FLOAT32, rasterWidth, rasterHeight);
        ogvi_mean.setNoDataValue(Double.NaN);
        ogvi_mean.setNoDataValueUsed(true);
        ogvi_mean.setRasterData(createFloat(rasterSize));
        product.addBand(ogvi_mean);

        final Band ogvi_sigma = new Band("OGVI_sigma", ProductData.TYPE_FLOAT32, rasterWidth, rasterHeight);
        ogvi_sigma.setNoDataValue(Double.NaN);
        ogvi_sigma.setNoDataValueUsed(true);
        ogvi_sigma.setRasterData(createFloat(rasterSize));
        product.addBand(ogvi_sigma);

        final Band ogvi_count = new Band("OGVI_count", ProductData.TYPE_INT32, rasterWidth, rasterHeight);
        ogvi_count.setNoDataValue(Integer.MIN_VALUE);
        ogvi_count.setNoDataValueUsed(true);
        ogvi_count.setRasterData(createInt(rasterSize));
        product.addBand(ogvi_count);

        final Band otci_mean = new Band("OTCI_mean", ProductData.TYPE_FLOAT32, rasterWidth, rasterHeight);
        otci_mean.setNoDataValue(Double.NaN);
        otci_mean.setNoDataValueUsed(true);
        otci_mean.setRasterData(createFloat(rasterSize));
        product.addBand(otci_mean);

        final Band otci_sigma = new Band("OTCI_sigma", ProductData.TYPE_FLOAT32, rasterWidth, rasterHeight);
        otci_sigma.setNoDataValue(Double.NaN);
        otci_sigma.setNoDataValueUsed(true);
        otci_sigma.setRasterData(createFloat(rasterSize));
        product.addBand(otci_sigma);

        final Band otci_count = new Band("OTCI_count", ProductData.TYPE_INT32, rasterWidth, rasterHeight);
        otci_count.setNoDataValue(Integer.MIN_VALUE);
        otci_count.setNoDataValueUsed(true);
        otci_count.setRasterData(createInt(rasterSize));
        product.addBand(otci_count);
    }

    private static ProductData createFloat(int size) {
        final float[] data = new float[size];
        for (int i = 0; i < size; i++) {
            data[i] = Float.NaN;
        }

        return ProductData.createInstance(data);
    }

    private static ProductData createInt(int size) {
        final int[] data = new int[size];
        for (int i = 0; i < size; i++) {
            data[i] = Integer.MIN_VALUE;
        }

        return ProductData.createInstance(data);
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
