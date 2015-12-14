package org.esa.s3tbx.meris.brr.operator;


import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;

class MerisL1BProduct {

    // due to the 4x4 constraint in the algorithm, the two pixels we'd like to test are distributed
    // [0,0]                         -> px1
    // all other 15 raster positions -> px2
    // tb 2014-01-22
    static Product create() {
        final Product merisL1BProduct = new Product("Meris L1B", "MER_FR__1P", 4, 4);

        addRadiance_01(merisL1BProduct);
        addRadiance_02(merisL1BProduct);
        addRadiance_03(merisL1BProduct);
        addRadiance_04(merisL1BProduct);
        addRadiance_05(merisL1BProduct);
        addRadiance_06(merisL1BProduct);
        addRadiance_07(merisL1BProduct);
        addRadiance_08(merisL1BProduct);
        addRadiance_09(merisL1BProduct);
        addRadiance_10(merisL1BProduct);
        addRadiance_11(merisL1BProduct);
        addRadiance_12(merisL1BProduct);
        addRadiance_13(merisL1BProduct);
        addRadiance_14(merisL1BProduct);
        addRadiance_15(merisL1BProduct);

        addDetectorIndex(merisL1BProduct);
        addFlagBand(merisL1BProduct);

        addLatitude(merisL1BProduct);
        addLongitude(merisL1BProduct);
        addDemAlt(merisL1BProduct);
        addDemRough(merisL1BProduct);
        addLatCorr(merisL1BProduct);
        addLonCorr(merisL1BProduct);
        addSunZenith(merisL1BProduct);
        addSunAzimuth(merisL1BProduct);
        addViewZenith(merisL1BProduct);
        addViewAzimuth(merisL1BProduct);
        addZonalWind(merisL1BProduct);
        addMeridWind(merisL1BProduct);
        addAtmPress(merisL1BProduct);
        addOzone(merisL1BProduct);
        addRelHum(merisL1BProduct);

        return merisL1BProduct;
    }

    private static void addRadiance_01(Product merisL1BProduct) {
        addFloatBand(merisL1BProduct, "radiance_1", new float[]{82.19423f, 82.21318f});
    }

    private static void addRadiance_02(Product merisL1BProduct) {
        addFloatBand(merisL1BProduct, "radiance_2", new float[]{71.743126f, 71.9658f});
    }

    private static void addRadiance_03(Product merisL1BProduct) {
        addFloatBand(merisL1BProduct, "radiance_3", new float[]{54.295757f, 54.214664f});
    }

    private static void addRadiance_04(Product merisL1BProduct) {
        addFloatBand(merisL1BProduct, "radiance_4", new float[]{47.23346f, 47.05146f});
    }

    private static void addRadiance_05(Product merisL1BProduct) {
        addFloatBand(merisL1BProduct, "radiance_5", new float[]{30.590908f, 30.525602f});
    }

    private static void addRadiance_06(Product merisL1BProduct) {
        addFloatBand(merisL1BProduct, "radiance_6", new float[]{18.275892f, 18.218576f});
    }

    private static void addRadiance_07(Product merisL1BProduct) {
        addFloatBand(merisL1BProduct, "radiance_7", new float[]{14.202907f, 14.15497f});
    }

    private static void addRadiance_08(Product merisL1BProduct) {
        addFloatBand(merisL1BProduct, "radiance_8", new float[]{12.717301f, 12.856061f});
    }

    private static void addRadiance_09(Product merisL1BProduct) {
        addFloatBand(merisL1BProduct, "radiance_9", new float[]{10.618f, 10.630618f});
    }

    private static void addRadiance_10(Product merisL1BProduct) {
        addFloatBand(merisL1BProduct, "radiance_10", new float[]{8.136091f, 8.0754385f});
    }

    private static void addRadiance_11(Product merisL1BProduct) {
        addFloatBand(merisL1BProduct, "radiance_11", new float[]{3.6290364f, 3.4870691f});
    }

    private static void addRadiance_12(Product merisL1BProduct) {
        addFloatBand(merisL1BProduct, "radiance_12", new float[]{6.9346585f, 6.949189f});
    }

    private static void addRadiance_13(Product merisL1BProduct) {
        addFloatBand(merisL1BProduct, "radiance_13", new float[]{4.322079f, 4.2545466f});
    }

    private static void addRadiance_14(Product merisL1BProduct) {
        addFloatBand(merisL1BProduct, "radiance_14", new float[]{3.8636525f, 3.7782004f});
    }

    private static void addRadiance_15(Product merisL1BProduct) {
        addFloatBand(merisL1BProduct, "radiance_15", new float[]{2.9162102f, 2.943363f});
    }

    private static void addFloatBand(Product merisL1BProduct, String bandName, float[] data) {
        final Band band = merisL1BProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemFloatAt(0, data[0]);
        for (int i = 1; i < 16; i++) {
            rasterData.setElemFloatAt(i, data[1]);
        }
        band.setData(rasterData);
    }

    private static void addDetectorIndex(Product merisL1BProduct) {
        final Band band = merisL1BProduct.addBand("detector_index", ProductData.TYPE_INT16);
        final ProductData rasterData = band.createCompatibleRasterData();
        for (int i = 0; i < 16; i++) {
            rasterData.setElemIntAt(i, 3258);
        }
        band.setData(rasterData);
    }

    private static void addFlagBand(Product merisL1BProduct) {
        final Band band = merisL1BProduct.addBand("l1_flags", ProductData.TYPE_UINT8);
        final ProductData rasterData = band.createCompatibleRasterData();
        for (int i = 0; i < 16; i++) {
            rasterData.setElemIntAt(i, 0);
        }
        band.setData(rasterData);
    }

    private static void addLatitude(Product merisL1BProduct) {
        addTiePointRaster(merisL1BProduct, "latitude", new float[]{44.932995f, 44.940742f});
    }

    private static void addLongitude(Product merisL1BProduct) {
        addTiePointRaster(merisL1BProduct, "longitude", new float[]{-86.573494f, -86.5713f});
    }

    private static void addDemAlt(Product merisL1BProduct) {
        addTiePointRaster(merisL1BProduct, "dem_alt", new float[]{176.f, 176.f});
    }

    private static void addDemRough(Product merisL1BProduct) {
        addTiePointRaster(merisL1BProduct, "dem_rough", new float[]{0.f, 0.f});
    }

    private static void addLatCorr(Product merisL1BProduct) {
        addTiePointRaster(merisL1BProduct, "lat_corr", new float[]{-1.7532325E-4f, -1.7534228E-4f});
    }

    private static void addLonCorr(Product merisL1BProduct) {
        addTiePointRaster(merisL1BProduct, "lon_corr", new float[]{0.0012349063f, 0.0012350469f});
    }

    private static void addSunZenith(Product merisL1BProduct) {
        addTiePointRaster(merisL1BProduct, "sun_zenith", new float[]{30.868954f, 30.872784f});
    }

    private static void addSunAzimuth(Product merisL1BProduct) {
        addTiePointRaster(merisL1BProduct, "sun_azimuth", new float[]{127.82522f, 127.837845f});
    }

    private static void addViewZenith(Product merisL1BProduct) {
        addTiePointRaster(merisL1BProduct, "view_zenith", new float[]{29.379765f, 29.379715f});
    }

    private static void addViewAzimuth(Product merisL1BProduct) {
        addTiePointRaster(merisL1BProduct, "view_azimuth", new float[]{101.36201f, 101.362305f});
    }

    private static void addZonalWind(Product merisL1BProduct) {
        addTiePointRaster(merisL1BProduct, "zonal_wind", new float[]{-0.6052734f, -0.618457f});
    }

    private static void addMeridWind(Product merisL1BProduct) {
        addTiePointRaster(merisL1BProduct, "merid_wind", new float[]{0.46982425f, 0.46235356f});
    }

    private static void addAtmPress(Product merisL1BProduct) {
        addTiePointRaster(merisL1BProduct, "atm_press", new float[]{1013.0114f, 1013.0095f});
    }

    private static void addOzone(Product merisL1BProduct) {
        addTiePointRaster(merisL1BProduct, "ozone", new float[]{370.99918f, 370.97424f});
    }

    private static void addRelHum(Product merisL1BProduct) {
        addTiePointRaster(merisL1BProduct, "rel_hum", new float[]{85.31759f, 85.427155f});
    }

    private static void addTiePointRaster(Product merisL1BProduct, String name, float[] data) {
        float[] tiePointData = new float[16];
        tiePointData[0] = data[0];
        for (int i = 1; i < 16; i++) {
            tiePointData[i] = data[1];
        }

        merisL1BProduct.addTiePointGrid(new TiePointGrid(name, 4, 4, 0.5f, 0.5f, 1, 1, tiePointData));
    }
}
