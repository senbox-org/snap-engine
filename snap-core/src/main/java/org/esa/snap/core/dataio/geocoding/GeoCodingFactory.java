package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataio.geocoding.forward.PixelForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelQuadTreeInverse;
import org.esa.snap.core.dataio.geocoding.util.EllipsoidDistance;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Scene;
import org.esa.snap.core.util.ProductUtils;
import org.geotools.referencing.datum.DefaultEllipsoid;

import javax.media.jai.Interpolation;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.IOException;

public class GeoCodingFactory {

    /**
     * Creates a ComponentGeoCoding using a PixelForward and a PixelQuadTreeInverse coding using the Bands passed in and the given ground resolution.
     * The ComponeneGeoDoding is returned is initialized and ready to use.
     *
     * @param latBand              the Band containing the latitude data
     * @param lonBand              the Band containing the longitude data
     * @param rasterResolutionInKm the ground resolution in km
     * @return a ready to use GeoCoding
     * @throws IOException on disk IO errors
     */
    public static ComponentGeoCoding createPixelGeoCoding(final Band latBand, final Band lonBand, double rasterResolutionInKm) throws IOException {
        final GeoRaster geoRaster = getGeoRaster(latBand, lonBand, rasterResolutionInKm);

        return createAndInitialize(geoRaster);
    }

    /**
     * Creates a ComponentGeoCoding using a PixelForward and a PixelQuadTreeInverse coding using the Bands passed in. The
     * ground resolution is estimatesd from the geolocation data.
     * The ComponeneGeoDoding is returned is initialized and ready to use.
     *
     * @param latBand              the Band containing the latitude data
     * @param lonBand              the Band containing the longitude data
     * @return a ready to use GeoCoding
     * @throws IOException on disk IO errors
     */
    public static ComponentGeoCoding createPixelGeoCoding(final Band latBand, final Band lonBand) throws IOException {
        final GeoRaster geoRaster = getGeoRaster(latBand, lonBand);

        return createAndInitialize(geoRaster);
    }

    public static Band createSubset(Band sourceBand, Scene targetScene, ProductSubsetDef subsetDef) {
        final Band targetBand = new Band(sourceBand.getName(),
                sourceBand.getDataType(),
                targetScene.getRasterWidth(),
                targetScene.getRasterHeight());
        ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
        targetBand.setSourceImage(getSourceImage(subsetDef, sourceBand));
        return targetBand;
    }

    private static RenderedImage getSourceImage(ProductSubsetDef subsetDef, Band band) {
        RenderedImage sourceImage = band.getSourceImage();
        if (subsetDef != null) {
            final Rectangle region = subsetDef.getRegion();
            if (region != null) {
                float x = region.x;
                float y = region.y;
                float width = region.width;
                float height = region.height;
                sourceImage = CropDescriptor.create(sourceImage, x, y, width, height, null);
            }
            final int subSamplingX = subsetDef.getSubSamplingX();
            final int subSamplingY = subsetDef.getSubSamplingY();
            if (mustSubSample(subSamplingX, subSamplingY) || mustTranslate(region)) {
                float scaleX = 1.0f / subSamplingX;
                float scaleY = 1.0f / subSamplingY;
                float transX = region != null ? -region.x : 0;
                float transY = region != null ? -region.y : 0;
                Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
                sourceImage = ScaleDescriptor.create(sourceImage, scaleX, scaleY, transX, transY, interpolation, null);
            }
        }
        return sourceImage;
    }

    // package access for testing only tb 2024-03-26
    static boolean mustTranslate(Rectangle region) {
        return (region != null && (region.x != 0 || region.y != 0));
    }

    // package access for testing only tb 2024-03-26
    static boolean mustSubSample(int subSamplingX, int subSamplingY) {
        return subSamplingX != 1 || subSamplingY != 1;
    }

    // package access for testing only tb 2024-03-27
    static double estimateGroundResolutionInKm(double[] latitudes, double[] longitudes, int width, int height) {
        final double[] measures = new double[3];
        // upper left, measure in y direction
        EllipsoidDistance distance = new EllipsoidDistance(longitudes[0], latitudes[0], DefaultEllipsoid.WGS84);
        measures[0] = distance.distance(longitudes[width], latitudes[width]);

        // center, measuring in x direction
        final int yOff = height/2;
        final int xOff = width/2 - 1;
        int pos = xOff + yOff * width;
        distance = new EllipsoidDistance(longitudes[pos], latitudes[pos], DefaultEllipsoid.WGS84);
        measures[1] = distance.distance(longitudes[pos + 1], latitudes[pos + 1]);

        // lower right, measuring in x direction
        pos = width * height - 1;
        distance = new EllipsoidDistance(longitudes[pos], latitudes[pos], DefaultEllipsoid.WGS84);
        measures[2] = distance.distance(longitudes[pos - 1], latitudes[pos - 1]);

        // return average and convert to Km
        return (measures[0] + measures[1] + measures[2]) / 3000.0;
    }

    private static GeoRaster getGeoRaster(Band latBand, Band lonBand) throws IOException {
        lonBand.loadRasterData();
        latBand.loadRasterData();

        final int rasterWidth = lonBand.getRasterWidth();
        final int rasterHeight = lonBand.getRasterHeight();
        final int size = rasterWidth * rasterHeight;

        final double[] longitudes = lonBand.getGeophysicalImage().getImage(0).getData()
                .getPixels(0, 0, rasterWidth, rasterHeight, new double[size]);
        final double[] latitudes = latBand.getGeophysicalImage().getImage(0).getData()
                .getPixels(0, 0, rasterWidth, rasterHeight, new double[size]);

        final double rasterResolutionInKm = estimateGroundResolutionInKm(latitudes, longitudes, rasterWidth, rasterHeight);

        return new GeoRaster(longitudes, latitudes, lonBand.getName(), latBand.getName(),
                rasterWidth, rasterHeight, rasterResolutionInKm);
    }

    private static GeoRaster getGeoRaster(Band latBand, Band lonBand, double rasterResolutionInKm) throws IOException {
        lonBand.loadRasterData();
        latBand.loadRasterData();

        final int rasterWidth = lonBand.getRasterWidth();
        final int rasterHeight = lonBand.getRasterHeight();
        final int size = rasterWidth * rasterHeight;

        final double[] longitudes = lonBand.getGeophysicalImage().getImage(0).getData()
                .getPixels(0, 0, rasterWidth, rasterHeight, new double[size]);
        final double[] latitudes = latBand.getGeophysicalImage().getImage(0).getData()
                .getPixels(0, 0, rasterWidth, rasterHeight, new double[size]);

        return new GeoRaster(longitudes, latitudes, lonBand.getName(), latBand.getName(),
                rasterWidth, rasterHeight, rasterResolutionInKm);
    }

    private static ComponentGeoCoding createAndInitialize(GeoRaster geoRaster) {
        final ForwardCoding forwardCoding = ComponentFactory.getForward(PixelForward.KEY);
        final InverseCoding inverseCoding = ComponentFactory.getInverse(PixelQuadTreeInverse.KEY);
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding);
        geoCoding.initialize();
        return geoCoding;
    }
}
