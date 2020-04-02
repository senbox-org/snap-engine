package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.geocoding.forward.PixelForward;
import org.esa.snap.core.dataio.geocoding.forward.PixelInterpolatingForward;
import org.esa.snap.core.dataio.geocoding.forward.TiePointBilinearForward;
import org.esa.snap.core.dataio.geocoding.forward.TiePointSplineForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelGeoIndexInverse;
import org.esa.snap.core.dataio.geocoding.inverse.PixelQuadTreeInverse;
import org.esa.snap.core.dataio.geocoding.inverse.TiePointInverse;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;

public class ComponentGeoCodingTestUtils {
    final static int SCENE_WIDTH = 40;
    final static int SCENE_HEIGHT = 50;
    final static int TP_WIDTH = 8;
    final static int TP_HEIGHT = 10;

    public static Product createProduct() {
        final Product product = new Product("P", "T", SCENE_WIDTH, SCENE_HEIGHT);
        product.addTiePointGrid(new TiePointGrid("tpLon", TP_WIDTH, TP_HEIGHT, 0.5, 0.5, 5, 5));
        product.addTiePointGrid(new TiePointGrid("tpLat", TP_WIDTH, TP_HEIGHT, 0.5, 0.5, 5, 5));
        product.addBand("Lon", ProductData.TYPE_FLOAT64);
        product.addBand("Lat", ProductData.TYPE_FLOAT64);
        product.addBand("dummy", ProductData.TYPE_FLOAT64);
        return product;
    }

    public static ComponentGeoCoding initializeWithBands(Product srcProduct, boolean interpolating, boolean quadTree, boolean antimeridian) {
        double[][] sceneDoubles = createSceneDoubles(antimeridian);
        double[] lons = sceneDoubles[0];
        double[] lats = sceneDoubles[1];
        srcProduct.getBand("Lon").setData(ProductData.createInstance(lons));
        srcProduct.getBand("Lat").setData(ProductData.createInstance(lats));
        final GeoRaster geoRaster = new GeoRaster(lons, lats, "Lon", "Lat",
                                                  SCENE_WIDTH, SCENE_HEIGHT,
                                                  300.0);
        final ForwardCoding forwardCoding;
        final InverseCoding inverseCoding;
        if (interpolating) {
            forwardCoding = ComponentFactory.getForward(PixelInterpolatingForward.KEY);
            if (quadTree) {
                inverseCoding = ComponentFactory.getInverse(PixelQuadTreeInverse.KEY_INTERPOLATING);
            } else {
                inverseCoding = ComponentFactory.getInverse(PixelGeoIndexInverse.KEY_INTERPOLATING);
            }
        } else {
            forwardCoding = ComponentFactory.getForward(PixelForward.KEY);
            if (quadTree) {
                inverseCoding = ComponentFactory.getInverse(PixelQuadTreeInverse.KEY);
            } else {
                inverseCoding = ComponentFactory.getInverse(PixelGeoIndexInverse.KEY);
            }
        }
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding, GeoChecks.ANTIMERIDIAN);
        geoCoding.initialize();
        srcProduct.setSceneGeoCoding(geoCoding);
        return geoCoding;
    }

    public static ComponentGeoCoding initializeWithTiePoints(Product srcProduct, boolean bilinear, boolean antimeridian) {
        float[][] tiePointFloats = createTiePointFloats(antimeridian);
        float[] lons = tiePointFloats[0];
        float[] lats = tiePointFloats[1];
        srcProduct.getTiePointGrid("tpLon").setData(ProductData.createInstance(lons));
        srcProduct.getTiePointGrid("tpLat").setData(ProductData.createInstance(lats));
        final GeoRaster geoRaster = new GeoRaster(toD(lons), toD(lats), "tpLon", "tpLat",
                                                  TP_WIDTH, TP_HEIGHT, SCENE_WIDTH, SCENE_HEIGHT,
                                                  300.0, 0.5, 0.5, 5, 5);
        final ForwardCoding forwardCoding;
        if (bilinear) {
            forwardCoding = ComponentFactory.getForward(TiePointBilinearForward.KEY);
        } else {
            forwardCoding = ComponentFactory.getForward(TiePointSplineForward.KEY);
        }
        final InverseCoding inverseCoding = ComponentFactory.getInverse(TiePointInverse.KEY);
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding, GeoChecks.ANTIMERIDIAN);
        geoCoding.initialize();
        srcProduct.setSceneGeoCoding(geoCoding);
        return geoCoding;
    }

    public static double[][] createSceneDoubles(boolean withAntimeridian) {

        final double[] lon = new double[SCENE_WIDTH * SCENE_HEIGHT];
        final double[] lat = new double[SCENE_WIDTH * SCENE_HEIGHT];

        final double lonStart = withAntimeridian ? 160 : -20;
        final double lonStepY = -0.4;
        final double lonStepX = 1;

        final double latStart = 50;
        final double latStepY = -1;
        final double latStepX = -0.4;

        for (int y = 0; y < SCENE_HEIGHT; y++) {
            final double lonOffY = y * lonStepY;
            final double latOffY = y * latStepY;
            for (int x = 0; x < SCENE_WIDTH; x++) {
                final int idx = y * SCENE_WIDTH + x;
                lon[idx] = lonStart + lonOffY + x * lonStepX;
                lat[idx] = latStart + latOffY + x * latStepX;
            }
        }

        for (int i = 0; i < lon.length; i++) {
            double v = lon[i];
            lon[i] = v > 180 ? v - 360 : v;
        }

        return new double[][]{lon, lat};
    }

    public static float[][] createTiePointFloats(boolean withAntimeridian) {

        final float[] lon = new float[TP_WIDTH * TP_HEIGHT];
        final float[] lat = new float[TP_WIDTH * TP_HEIGHT];

        final float lonStart = withAntimeridian ? 160 : -20;
        final float lonStepY = -2;
        final float lonStepX = 5;

        final float latStart = 50;
        final float latStepY = -5;
        final float latStepX = -2;

        for (int y = 0; y < TP_HEIGHT; y++) {
            final float lonOffY = y * lonStepY;
            final float latOffY = y * latStepY;
            for (int x = 0; x < TP_WIDTH; x++) {
                final int idx = y * TP_WIDTH + x;
                lon[idx] = lonStart + lonOffY + x * lonStepX;
                lat[idx] = latStart + latOffY + x * latStepX;
            }
        }

        for (int i = 0; i < lon.length; i++) {
            float v = lon[i];
            lon[i] = v > 180 ? v - 360 : v;
        }

        return new float[][]{lon, lat};
    }

    public static double[] toD(float[] lons) {
        final double[] doubles = new double[lons.length];
        for (int i = 0; i < lons.length; i++) {
            doubles[i] = lons[i];
        }
        return doubles;
    }
}
