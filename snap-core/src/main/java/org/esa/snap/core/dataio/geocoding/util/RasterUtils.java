package org.esa.snap.core.dataio.geocoding.util;

import org.esa.snap.core.dataio.geocoding.Discontinuity;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.math.RsMathUtils;
import org.esa.snap.core.util.math.SphericalDistance;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.datum.Ellipsoid;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

public class RasterUtils {

    private static final double MEAN_EARTH_RADIUS_KM = RsMathUtils.MEAN_EARTH_RADIUS * 0.001;
    private static final double STEP_THRESH = 180.0;

    // this one is for backward compatibility - I guess it can be removed during the merge operation with SNAP core tb 2019-10-24
    // @todo discuss with Marco and Norman
    static Discontinuity calculateDiscontinuity(float[] longitudes) {
        float max = Float.MIN_VALUE;

        for (float value : longitudes) {
            if (value > max) {
                max = value;
            }
        }

        if (max > 180.0) {
            return Discontinuity.AT_360;
        } else {
            return Discontinuity.AT_180;
        }
    }

    /**
     * Checks if the longitude data raster contains the anti-meridian. Runs along the raster borders and if a
     * longitude-delta larger than 180 deg is detected, an anti-meridian crossing is contained in the data.
     *
     * @param longitudes the longitude raster
     * @param width      the data width in pixels
     * @return whether the data contains the anti-meridian or not
     */
    public static boolean containsAntiMeridian(double[] longitudes, int width) {
        final int height = longitudes.length / width;

        // top
        for (int x = 1; x < width; x++) {
            final double step = Math.abs(longitudes[x] - longitudes[x - 1]);
            if (step > STEP_THRESH) {
                return true;
            }
        }

        // left
        int lineOffset;
        for (int y = 1; y < height; y++) {
            final double step = Math.abs(longitudes[y * width] - longitudes[(y - 1) * width]);
            if (step > STEP_THRESH) {
                return true;
            }
        }

        // bottom
        lineOffset = (height - 1) * width;
        for (int x = 1; x < width; x++) {
            final double step = Math.abs(longitudes[lineOffset + x] - longitudes[lineOffset + x - 1]);
            if (step > STEP_THRESH) {
                return true;
            }
        }

        // right
        for (int y = 1; y < height; y++) {
            lineOffset = width - 1;
            final double step = Math.abs(longitudes[y * width + lineOffset] - longitudes[(y - 1) * width + lineOffset]);
            if (step > STEP_THRESH) {
                return true;
            }
        }

        return false;
    }

    public static PixelPos[] getPoleLocations(GeoRaster geoRaster) {
        final double deltaToPole = getLatDeltaToPole(geoRaster.getRasterResolutionInKm());
        double maxLat = 90.0 - deltaToPole;
        double minLat = -90.0 + deltaToPole;

        final ArrayList<PixelPos> poleCandidates = findPoleCandidates(geoRaster, maxLat, minLat);
        if (poleCandidates.size() == 0) {
            return new PixelPos[0];
        }

        final ArrayList<PixelPos> consolidatedPoleLocations = new ArrayList<>();
        final double[] lons = geoRaster.getLongitudes();
        final int width = geoRaster.getRasterWidth();
        for (final PixelPos candidate : poleCandidates) {
            final double[] deltas = new double[8];
            final int x = (int) candidate.x;
            final int y = (int) candidate.y;

            // walk clockwise around the pole and calculate the 8 longitude deltas
            deltas[0] = Math.abs(lons[(y) * width + (x - 1)] - lons[(y - 1) * width + (x - 1)]);
            deltas[1] = Math.abs(lons[(y + 1) * width + (x - 1)] - lons[(y) * width + (x - 1)]);
            deltas[2] = Math.abs(lons[(y + 1) * width + (x)] - lons[(y + 1) * width + (x - 1)]);
            deltas[3] = Math.abs(lons[(y + 1) * width + (x + 1)] - lons[(y + 1) * width + (x)]);
            deltas[4] = Math.abs(lons[(y) * width + (x + 1)] - lons[(y + 1) * width + (x + 1)]);
            deltas[5] = Math.abs(lons[(y - 1) * width + (x + 1)] - lons[(y) * width + (x + 1)]);
            deltas[6] = Math.abs(lons[(y - 1) * width + (x)] - lons[(y - 1) * width + (x + 1)]);
            deltas[7] = Math.abs(lons[(y - 1) * width + (x - 1)] - lons[(y - 1) * width + (x)]);

            int numMeridianCrossings = 0;
            for (final double delta : deltas) {
                if (delta > STEP_THRESH) {
                    ++numMeridianCrossings;
                }
            }
            if (numMeridianCrossings % 2 == 1) {
                consolidatedPoleLocations.add(candidate);
            }
        }

        return consolidatedPoleLocations.toArray(new PixelPos[0]);
    }

    /**
     * Returns the latitude difference to the poles (i.e. the lenght of the circle segment) given the desired distance
     * in kilometers.
     *
     * @param distanceInKm the distance in kilometers
     * @return the latitude delta in degrees
     */
    static double getLatDeltaToPole(double distanceInKm) {
        return ((distanceInKm * 180.0) / (Math.PI * MEAN_EARTH_RADIUS_KM));
    }

    public static float[] toFloat(double[] doubles) {
        final float[] floats = new float[doubles.length];

        for (int i = 0; i < doubles.length; i++) {
            floats[i] = (float) doubles[i];
        }

        return floats;
    }

    static double[] toDouble(float[] floats) {
        final double[] doubles = new double[floats.length];

        for (int i = 0; i < floats.length; i++) {
            doubles[i] = floats[i];
        }

        return doubles;
    }

    public static double[] loadDataScaled(RasterDataNode dataNode) throws IOException {
        dataNode.loadRasterData();
        final Dimension rasterSize = dataNode.getRasterSize();
        final double[] values = new double[rasterSize.width * rasterSize.height];
        dataNode.readPixels(0, 0, rasterSize.width, rasterSize.height, values);
        return values;
    }

    public static double[] loadData(RasterDataNode dataNode) throws IOException {
        dataNode.loadRasterData();
        final ProductData data = dataNode.getData();
        final double[] values = new double[data.getNumElems()];
        for (int i = 0; i < data.getNumElems(); i++) {
            values[i] = data.getElemDoubleAt(i);
        }
        return values;
    }

    // returns al (x/y) positions that have a latitude above the defined pole-angle threshold
    private static ArrayList<PixelPos> findPoleCandidates(GeoRaster geoRaster, double maxLat, double minLat) {
        final ArrayList<PixelPos> poleCandidates = new ArrayList<>();

        final double[] latitudes = geoRaster.getLatitudes();
        final int rasterWidth = geoRaster.getRasterWidth();
        for (int y = 0; y < geoRaster.getRasterHeight(); y++) {
            final int lineOffset = y * rasterWidth;
            for (int x = 0; x < rasterWidth; x++) {
                final double lat = latitudes[lineOffset + x];
                if ((lat >= maxLat) || (lat <= minLat)) {
                    poleCandidates.add(new PixelPos(x, y));
                }
            }
        }
        return poleCandidates;
    }

    public static double computeResolutionInKm(Band lonBand, Band latBand) {
        final Product product = lonBand.getProduct();
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();

        final Rectangle R = new Rectangle(0, 0, 10, 10);
        R.width = Math.min(R.width, width);
        R.height = Math.min(R.height, height);
        if (width > R.width) {
            R.x = (width - R.width) / 2;
        }
        if (height > R.height) {
            R.y = (height - R.height) / 2;
        }

        final int resPixelsSize = R.width * R.height;
        double[] resLons = lonBand.getSourceImage().getData().getSamples(R.x, R.y, R.width, R.height, 0, new double[resPixelsSize]);
        double[] resLats = latBand.getSourceImage().getData().getSamples(R.x, R.y, R.width, R.height, 0, new double[resPixelsSize]);

        return computeResolutionInKm(resLons, resLats, R.width, R.height);
    }

    public static double computeResolutionInKm(double[] lonData, double[] latData, int width, int height) {
        int count = 0;
        double distanceSum = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int idx = y * width + x;
                final double resLon = lonData[idx];
                final double resLat = latData[idx];
                final SphericalDistance spherDist = new SphericalDistance(resLon, resLat);
                if (x < width - 1) {
                    final int idxRight = idx + 1;
                    final double distance = spherDist.distance(lonData[idxRight], latData[idxRight]);
                    distanceSum += distance;
                    count++;
                }
                if (y < height - 1) {
                    final int idxBottom = idx + width;
                    final double distance = spherDist.distance(lonData[idxBottom], latData[idxBottom]);
                    distanceSum += distance;
                    count++;
                }
            }
        }

        final double distanceMeanRadian = distanceSum / count;

        final DefaultGeographicCRS wgs84 = DefaultGeographicCRS.WGS84;
        final Ellipsoid ellipsoid = wgs84.getDatum().getEllipsoid();
        final double meanEarthRadiusM = (ellipsoid.getSemiMajorAxis() + ellipsoid.getSemiMinorAxis()) / 2;
        final double meanEarthRadiusKm = meanEarthRadiusM / 1000.0;

        return distanceMeanRadian * meanEarthRadiusKm;
    }
}
