package org.esa.snap.core.dataio.geocoding.inverse;

import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.InverseCoding;
import org.esa.snap.core.dataio.geocoding.util.InterpolationContext;
import org.esa.snap.core.dataio.geocoding.util.InterpolatorFactory;
import org.esa.snap.core.dataio.geocoding.util.XYInterpolator;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.util.PreferencesPropertyMap;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.core.util.math.Range;
import org.esa.snap.core.util.math.RsMathUtils;
import org.esa.snap.runtime.Config;

import java.util.ArrayList;
import java.util.Properties;

import static org.esa.snap.core.dataio.geocoding.inverse.Segment.MIN_DIMENSION;
import static org.esa.snap.core.dataio.geocoding.inverse.SegmentCoverage.*;

public class PixelQuadTreeInverse implements InverseCoding, GeoPosCalculator {

    public static final String KEY = "INV_PIXEL_QUAD_TREE";
    public static final String KEY_INTERPOLATING = KEY + KEY_SUFFIX_INTERPOLATING;

    private static final double TO_DEG = 180.0 / Math.PI;
    private static final double ANGLE_THRESHOLD = 270.0;

    private final boolean fractionalAccuracy;
    private final XYInterpolator interpolator;

    private int rasterWidth;
    private int rasterHeight;
    private double epsilon;
    private double[] epsilonLon;
    private double offsetX;
    private double offsetY;
    private boolean isCrossingMeridian;
    private double[] longitudes;
    private double[] latitudes;
    private Range lonRange;
    private Range latRange;
    private ArrayList<Segment> segmentList;

    PixelQuadTreeInverse() {
        this(false);
    }

    PixelQuadTreeInverse(boolean fractionalAccuracy) {
        this(fractionalAccuracy, new PreferencesPropertyMap(Config.instance("snap").preferences()).getProperties());
    }

    PixelQuadTreeInverse(boolean fractionalAccuracy, Properties properties) {
        this(fractionalAccuracy, InterpolatorFactory.create(properties));
    }

    private PixelQuadTreeInverse(boolean fractionalAccuracy, XYInterpolator interpolator) {
        this.fractionalAccuracy = fractionalAccuracy;
        this.interpolator = interpolator;
        segmentList = new ArrayList<>();
    }

    static double getPositiveLonMin(double[] longitudes) {
        double lonMin = Double.MAX_VALUE;
        for (final double lon : longitudes) {
            if (lon >= 0.0 && lon < lonMin) {
                lonMin = lon;
            }
        }
        return lonMin;
    }

    static double getNegativeLonMax(double[] longitudes) {
        double lonMax = -Double.MAX_VALUE;
        for (final double lon : longitudes) {
            if (lon < 0 && lon > lonMax) {
                lonMax = lon;
            }
        }

        return lonMax;
    }

    static boolean isCrossingAntiMeridianInsideQuad(double[] longitudes) {
        final int numLons = longitudes.length;

        double maxDelta = -1.0;
        double delta;
        for (int i = 1; i < numLons; i++) {
            delta = Math.abs(longitudes[i] - longitudes[i - 1]);
            if (delta > maxDelta) {
                maxDelta = delta;
            }
        }

        delta = Math.abs(longitudes[numLons - 1] - longitudes[0]);
        if (delta > maxDelta) {
            maxDelta = delta;
        }

        return maxDelta > ANGLE_THRESHOLD;
    }

    // package access for testing only tb 2019-12-16
    static double sq(final double dx, final double dy) {
        return dx * dx + dy * dy;
    }

    static double[] createEpsilonLongitude(double epsilon) {
        // @todo 1 tb/tb rethink this algo 2021-03-16
        final double[] d = new double[901];
        for (int i = 0; i < d.length; i++) {
            final double ang = i * 0.1;
            final double rad = Math.toRadians(ang);
            final double multiplier = 1 / Math.cos(rad);
            d[i] = epsilon * multiplier;
        }
        return d;
    }

    static Segment getPoleSegment(PixelPos[] poleLocations, int rasterWidth, int rasterHeight) {
        int x_min = Integer.MAX_VALUE;
        int x_max = Integer.MIN_VALUE;
        int y_min = Integer.MAX_VALUE;
        int y_max = Integer.MIN_VALUE;
        for (PixelPos location : poleLocations) {
            if (location.x > x_max) {
                x_max = (int) location.x;
            }
            if (location.x < x_min) {
                x_min = (int) location.x;
            }
            if (location.y > y_max) {
                y_max = (int) location.y;
            }
            if (location.y < y_min) {
                y_min = (int) location.y;
            }
        }

        // ensure that we're inside the product and if pole is closer to border than MIN_DIMENSION -> extend to border
        // this avoids too-small segments
        int minDimHalf = MIN_DIMENSION / 2;
        x_min = x_min - minDimHalf;
        if (x_min <= MIN_DIMENSION) {
            x_min = 0;
        }

        x_max = x_max + minDimHalf;
        if (x_max > rasterWidth - 1 - MIN_DIMENSION) {
            x_max = rasterWidth - 1;
        }

        y_min = y_min - minDimHalf;
        if (y_min < MIN_DIMENSION) {
            y_min = 0;
        }

        y_max = y_max + minDimHalf;
        if (y_max > rasterHeight - 1 - MIN_DIMENSION) {
            y_max = rasterHeight - 1;
        }

        return new Segment(x_min, x_max, y_min, y_max);
    }

    static Segment[] removeSegment(Segment toRemove, Segment origin, int rasterWidth, int rasterHeight) {
        final ArrayList<Segment> segmentList = new ArrayList<>();

        Segment remaining;
        if (toRemove.y_min > 0) {
            // cut out segment that is above the pole (upper split[0])
            // the remainder contains the pole and a lot of data
            final Segment[] splits = origin.split_y(toRemove.y_min);
            segmentList.add(splits[0]);
            remaining = splits[1];
        } else {
            // pole region touches top of product - nothing to cut out
            remaining = origin;
        }

        if (toRemove.y_max < rasterHeight - 1) {
            final Segment[] splits = remaining.split_y(toRemove.y_max + 1);
            remaining = splits[0];
            segmentList.add(splits[1]);
        }

        if (toRemove.x_min > 0) {
            final Segment[] splits = remaining.split_x(toRemove.x_min);
            segmentList.add(splits[0]);
            remaining = splits[1];
        }

        if (toRemove.x_max < rasterWidth - 1) {
            final Segment[] splits = remaining.split_x(toRemove.x_max + 1);
            // splits[0] must be the pole segment at this point
            segmentList.add(splits[1]);
        }

        return segmentList.toArray(new Segment[0]);
    }

    static double getMin(double[] values, double epsilon) {
        double min = Double.MAX_VALUE;
        for (double value : values) {
            if (value < min) {
                min = value;
            }
        }

        return min - epsilon;
    }

    static double getMax(double[] values, double epsilon) {
        double max = -Double.MAX_VALUE;
        for (double value : values) {
            if (value > max) {
                max = value;
            }
        }

        return max + epsilon;
    }

    private ArrayList<Segment> calculateSegmentation(int rasterWidth, int rasterHeight, PixelPos[] poleLocations) {
        final ArrayList<Segment> segmentList = new ArrayList<>();

        final Segment fullProductSegment = new Segment(0, rasterWidth - 1, 0, rasterHeight - 1);

        if (poleLocations.length == 0) {
            // start segmentation at full product
            calculateSegmentation(fullProductSegment, segmentList);
        } else {
            // create segment containing the pole
            final Segment poleSegment = getPoleSegment(poleLocations, rasterWidth, rasterHeight);

            // cut it from full orbit raster and calculate segmentation
            final Segment[] segments = removeSegment(poleSegment, fullProductSegment, rasterWidth, rasterHeight);
            for (final Segment segment : segments) {
                calculateSegmentation(segment, segmentList);
            }
        }

        return segmentList;
    }

    private void calculateSegmentation(Segment segment, ArrayList<Segment> segmentList) {
        segment.calculateGeoPoints(this);
        final SegmentCoverage segmentCoverage = hasGeoCoverage(segment);

        if (segmentCoverage == INSIDE) {
            // all test-points are inside the lon/lat segment
            segmentList.add(segment);
        } else {
            // we need to divide further down
            Segment[] splits = new Segment[0];
            if (segment.containsAntiMeridian) {
                splits = splitAtAntiMeridian(segment, segmentCoverage, this);
            }

            if (splits.length == 0) {
                splits = splitAtOutsidePoint(segment, segmentCoverage, this);
            }

            if (splits.length == 0) {
                // cannot divide further
                segmentList.add(segment);
            } else if (splits.length == 1) {
                // cannot divide further
                segmentList.add(splits[0]);
            } else {
                calculateSegmentation(splits[0], segmentList);
                calculateSegmentation(splits[1], segmentList);
            }
        }
    }

    static Segment[] splitAtOutsidePoint(Segment segment, SegmentCoverage segmentCoverage, GeoPosCalculator calculator) {
        final GeoPos geoPos = new GeoPos();
        if (segmentCoverage == ACROSS) {
            // check left
            int y_l = -1;
            for (int y = segment.y_min + 1; y < segment.y_max; y++) {
                calculator.getGeoPos(segment.x_min, y, geoPos);
                if (!segment.isInside(geoPos.lon, geoPos.lat)) {
                    y_l = y;
                    break;
                }
            }

            // check right
            int y_r = -1;
            for (int y = segment.y_min + 1; y < segment.y_max; y++) {
                calculator.getGeoPos(segment.x_max, y, geoPos);
                if (!segment.isInside(geoPos.lon, geoPos.lat)) {
                    y_r = y;
                    break;
                }
            }
            if ((y_l - segment.y_min) < MIN_DIMENSION && (y_r - segment.y_min) < MIN_DIMENSION) {
                // no suitable split-points found - as a last idea, we split at half
                return segment.split(true);
            }
            final int center = segment.y_min + segment.getHeight() / 2;
            final int delta_l = Math.abs(center - y_l);
            final int delta_r = Math.abs(center - y_r);
            if (delta_l < delta_r) {
                return segment.split_y(y_l);
            } else {
                return segment.split_y(y_r);
            }
        } else if (segmentCoverage == ALONG) {
            // check top
            int x_t = -1;
            for (int x = segment.x_min + 1; x < segment.x_max; x++) {
                calculator.getGeoPos(x, segment.y_min, geoPos);
                if (!segment.isInside(geoPos.lon, geoPos.lat)) {
                    x_t = x;
                    break;
                }
            }
            // check bottom
            int x_b = -1;
            for (int x = segment.x_min + 1; x < segment.x_max; x++) {
                calculator.getGeoPos(x, segment.y_max, geoPos);
                if (!segment.isInside(geoPos.lon, geoPos.lat)) {
                    x_b = x;
                    break;
                }
            }
            if ((x_t - segment.x_min) < MIN_DIMENSION && (x_b - segment.x_min) < MIN_DIMENSION) {
                // no suitable split-points found - as a last idea, we split at half
                return segment.split(false);
            }
            final int center = segment.x_min + segment.getWidth() / 2;
            final int delta_t = Math.abs(center - x_t);
            final int delta_b = Math.abs(center - x_b);
            if (delta_t < delta_b) {
                return segment.split_x(x_t);
            } else {
                return segment.split_x(x_b);
            }
        } else {
            throw new IllegalStateException("should not come here");
        }
    }

    static Segment[] splitAtAntiMeridian(Segment segment, SegmentCoverage segmentCoverage, GeoPosCalculator calculator) {
        final GeoPos geoPos = new GeoPos();
        if (segmentCoverage == ACROSS) {
            // find y with anti-meridian jump
            calculator.getGeoPos(segment.x_min, segment.y_min, geoPos);
            double lon_l = geoPos.lon;

            calculator.getGeoPos(segment.x_max, segment.y_min, geoPos);
            double lon_r = geoPos.lon;

            int y_l = Integer.MIN_VALUE;
            int y_r = Integer.MIN_VALUE;
            for (int y = segment.y_min + 1; y <= segment.y_max; y++) {
                calculator.getGeoPos(segment.x_min, y, geoPos);
                double delta = Math.abs(lon_l - geoPos.lon);
                if (delta > ANGLE_THRESHOLD) {
                    y_l = y;
                }
                lon_l = geoPos.lon;

                calculator.getGeoPos(segment.x_max, y, geoPos);
                delta = Math.abs(lon_r - geoPos.lon);
                if (delta > ANGLE_THRESHOLD) {
                    y_r = y;
                }
                lon_r = geoPos.lon;
            }

            if (y_l < MIN_DIMENSION && y_r < MIN_DIMENSION) {
                // antimeridian not passing through segment in a way that enables across-swath splitting
                return new Segment[0];
            }

            final int center = segment.y_min + segment.getHeight() / 2;
            final int delta_l = Math.abs(center - y_l);
            final int delta_r = Math.abs(center - y_r);
            if (delta_l < delta_r) {
                return segment.split_y(y_l);
            } else {
                return segment.split_y(y_r);
            }
        } else if (segmentCoverage == ALONG) {
            // find x with anti-meridian jump
            calculator.getGeoPos(segment.x_min, segment.y_min, geoPos);
            double lon_t = geoPos.lon;

            calculator.getGeoPos(segment.x_min, segment.y_max, geoPos);
            double lon_b = geoPos.lon;

            int x_t = Integer.MIN_VALUE;
            int x_b = Integer.MIN_VALUE;
            for (int x = segment.x_min + 1; x <= segment.x_max; x++) {
                calculator.getGeoPos(x, segment.y_min, geoPos);
                double delta = Math.abs(lon_t - geoPos.lon);
                if (delta > ANGLE_THRESHOLD) {
                    x_t = x;
                }
                lon_t = geoPos.lon;

                calculator.getGeoPos(x, segment.y_max, geoPos);
                delta = Math.abs(lon_b - geoPos.lon);
                if (delta > ANGLE_THRESHOLD) {
                    x_b = x;
                }
                lon_b = geoPos.lon;
            }
            if (x_t < MIN_DIMENSION && x_b < MIN_DIMENSION) {
                // antimeridian not passing through segment in a way that enables across-swath splitting
                return new Segment[0];
            }
            final int center = segment.x_min + segment.getWidth() / 2;
            final int delta_t = Math.abs(center - x_t);
            final int delta_b = Math.abs(center - x_b);
            if (delta_t < delta_b) {
                return segment.split_x(x_t);
            } else {
                return segment.split_x(x_b);
            }
        } else {
            // split at x_antimeridian
            throw new IllegalStateException("not implemented");
        }
    }

    private SegmentCoverage hasGeoCoverage(Segment segment) {
        final GeoPos geoPos = new GeoPos();
        final int xOffset = segment.getWidth() / 2;
        final int yOffset = segment.getHeight() / 2;

        getGeoPos(segment.x_min + xOffset, segment.y_min, geoPos);
        final boolean b1 = segment.isInside(geoPos.lon, geoPos.lat);

        getGeoPos(segment.x_max, segment.y_min + yOffset, geoPos);
        final boolean b2 = segment.isInside(geoPos.lon, geoPos.lat);

        getGeoPos(segment.x_min + xOffset, segment.y_max, geoPos);
        final boolean b3 = segment.isInside(geoPos.lon, geoPos.lat);

        getGeoPos(segment.x_min, segment.y_min + yOffset, geoPos);
        final boolean b4 = segment.isInside(geoPos.lon, geoPos.lat);

        if (!(b2 && b4)) {
            return ACROSS;
        }

        if (!(b1 && b3)) {
            return ALONG;
        }

        return INSIDE;
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        pixelPos.setInvalid();

        final ArrayList<Result> results = new ArrayList<>();
        for (final Segment segment : segmentList) {
            if (segment.isInside(geoPos.lon, geoPos.lat)) {
                final Result result = new Result();
                final boolean found = segmentSearch(geoPos.lon, geoPos.lat, segment, result);
                if (found) {
                    results.add(result);
                }
            }
        }

        if (results.size() > 0) {
            Result minDeltaResult = null;
            double minDelta = Double.MAX_VALUE;
            for (Result result : results) {
                final GeoPos resultGeoPos = new GeoPos();
                getGeoPos(result.x, result.y, resultGeoPos);
                final double absLonDist = Math.abs(resultGeoPos.lon - geoPos.lon);
                final double absLatDist = Math.abs(resultGeoPos.lat - geoPos.lat);
                final double delta = absLonDist * absLonDist + absLatDist * absLatDist;
                if (delta < minDelta) {
                    minDelta = delta;
                    minDeltaResult = result;
                }
            }

            if (minDelta < epsilon && minDeltaResult != null) {
                if (fractionalAccuracy) {
                    final InterpolationContext context = InterpolationContext.extract(minDeltaResult.x, minDeltaResult.y, longitudes, latitudes, rasterWidth, rasterHeight);
                    final PixelPos interpolated = interpolator.interpolate(geoPos, pixelPos, context);
                    pixelPos.setLocation(interpolated.x + offsetX, interpolated.y + offsetY);
                } else {
                    pixelPos.setLocation(minDeltaResult.x + offsetX, minDeltaResult.y + offsetY);
                }
            }
        }

        return pixelPos;
    }

    @Override
    public void initialize(GeoRaster geoRaster, boolean containsAntiMeridian, PixelPos[] poleLocations) {
        this.rasterWidth = geoRaster.getSceneWidth();
        this.rasterHeight = geoRaster.getSceneHeight();

        this.longitudes = geoRaster.getLongitudes();
        this.latitudes = geoRaster.getLatitudes();

        epsilon = getEpsilon(geoRaster.getRasterResolutionInKm());
        epsilonLon = createEpsilonLongitude(epsilon);

        isCrossingMeridian = containsAntiMeridian;

        offsetX = geoRaster.getOffsetX();
        offsetY = geoRaster.getOffsetY();

        lonRange = Range.computeRangeDouble(longitudes, null);
        latRange = Range.computeRangeDouble(latitudes, null);

        segmentList = calculateSegmentation(rasterWidth, rasterHeight, poleLocations);
    }

    @Override
    public String getKey() {
        if (fractionalAccuracy) {
            return KEY_INTERPOLATING;
        } else {
            return KEY;
        }
    }

    @Override
    public void dispose() {
        longitudes = null;
        latitudes = null;
        segmentList.clear();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public InverseCoding clone() {
        final PixelQuadTreeInverse clone = new PixelQuadTreeInverse(fractionalAccuracy, interpolator);

        clone.rasterWidth = rasterWidth;
        clone.rasterHeight = rasterHeight;

        clone.longitudes = longitudes;
        clone.latitudes = latitudes;

        clone.lonRange = new Range(lonRange.getMin(), lonRange.getMax());
        clone.latRange = new Range(latRange.getMin(), latRange.getMax());

        clone.epsilon = epsilon;
        clone.epsilonLon = epsilonLon;
        clone.isCrossingMeridian = isCrossingMeridian;

        clone.offsetX = offsetX;
        clone.offsetY = offsetY;

        for (Segment segment : segmentList) {
            clone.segmentList.add(segment.clone());
        }

        return clone;
    }

    // package access for testing only tb 2019-12-16
    public void getGeoPos(int pixelX, int pixelY, GeoPos geoPos) {
        final int index = pixelY * rasterWidth + pixelX;

        geoPos.setLocation(latitudes[index], longitudes[index]);
    }

    // package access for testing only tb 2019-12-16
    double getEpsilon(double resolutionInKm) {
        final double angle = 2.0 * Math.asin((resolutionInKm * 1000.0) / (2 * RsMathUtils.MEAN_EARTH_RADIUS));
        return TO_DEG * angle * 2.0;
    }

    private boolean segmentSearch(final double lon,
                                  final double lat,
                                  Segment segment,
                                  final Result result) {

        return quadTreeSearch(0, lat, lon, segment.x_min, segment.y_min, segment.getWidth(), segment.getHeight(), result);
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private boolean quadTreeSearch(final int depth,
                                   final double lat,
                                   final double lon,
                                   final int x, final int y,
                                   final int w, final int h,
                                   final Result result) {

        if (w < 2 || h < 2) {
            return false;
        }

        final int x_1 = x;
        final int x_2 = x_1 + w - 1;

        final int y_1 = y;
        final int y_2 = y_1 + h - 1;

        final boolean closeToPole = Math.abs(lon) > 70.0;

        final double[] lonArray;
        final double[] latArray;
        if (closeToPole) {
            lonArray = new double[8];
            latArray = new double[8];
        } else {
            lonArray = new double[4];
            latArray = new double[4];
        }

        final GeoPos geoPos = new GeoPos();
        getGeoPos(x_1, y_1, geoPos);
        lonArray[0] = geoPos.lon;
        latArray[0] = geoPos.lat;

        getGeoPos(x_1, y_2, geoPos);
        lonArray[1] = geoPos.lon;
        latArray[1] = geoPos.lat;

        getGeoPos(x_2, y_1, geoPos);
        lonArray[2] = geoPos.lon;
        latArray[2] = geoPos.lat;

        getGeoPos(x_2, y_2, geoPos);
        lonArray[3] = geoPos.lon;
        latArray[3] = geoPos.lat;

        if (closeToPole) {
            // add more points to compensate extreme curvature close to pole tb 2021-03-29
            final int xOffset = w / 2;
            final int yOffset = h / 2;

            getGeoPos(x_1 + xOffset, y_1, geoPos);
            lonArray[4] = geoPos.lon;
            latArray[4] = geoPos.lat;

            getGeoPos(x_2, y_1 + yOffset, geoPos);
            lonArray[5] = geoPos.lon;
            latArray[5] = geoPos.lat;

            getGeoPos(x_1 + xOffset, y_2, geoPos);
            lonArray[6] = geoPos.lon;
            latArray[6] = geoPos.lat;

            getGeoPos(x_1, y_1 + yOffset, geoPos);
            lonArray[7] = geoPos.lon;
            latArray[7] = geoPos.lat;
        }

        final double latMin = getMin(latArray, epsilon);
        final double latMax = getMax(latArray, epsilon);

        if (lat < latMin || lat > latMax) {
            return false;
        }

        final int idx = (int) Math.floor((Math.abs(latMin) + Math.abs(latMax)) / 2 * 10);
        final double epsLon = epsilonLon[idx];
        if (isCrossingMeridian && isCrossingAntiMeridianInsideQuad(lonArray)) {
            boolean lonOutside = false;
            if (lon > 0f) {
                // position is in a region with positive longitudes, so cut negative longitudes from quad area tb 2021-03-29
                final double lonMin = getPositiveLonMin(lonArray);
                if (lon + epsLon < lonMin) {
                    lonOutside = true;
                }
            } else {
                // position is in a region with negative longitudes, so cut positive longitudes from quad area tb 2021-03-29
                final double lonMax = getNegativeLonMax(lonArray);
                if (lon - epsLon > lonMax) {
                    lonOutside = true;
                }
            }
            if (lonOutside) {
                return false;
            }
        } else {
            final double lonMin = getMin(lonArray, epsLon);
            final double lonMax = getMax(lonArray, epsLon);

            if (lon < lonMin || lon > lonMax) {
                return false;
            }
        }

        boolean pixelFound = false;
        if (w == 2 && h == 2) {
            final double f = Math.cos(lat * MathUtils.DTOR);
            if (result.update(x_1, y_1, sq(lat - latArray[0], f * (lon - lonArray[0])))) {
                pixelFound = true;
            }
            if (result.update(x_1, y_2, sq(lat - latArray[1], f * (lon - lonArray[1])))) {
                pixelFound = true;
            }
            if (result.update(x_2, y_1, sq(lat - latArray[2], f * (lon - lonArray[2])))) {
                pixelFound = true;
            }
            if (result.update(x_2, y_2, sq(lat - latArray[3], f * (lon - lonArray[3])))) {
                pixelFound = true;
            }
        } else {
            pixelFound = quadTreeRecursion(depth, lat, lon, x_1, y_1, w, h, result);
        }

        return pixelFound;
    }

    private boolean quadTreeRecursion(final int depth,
                                      final double lat, final double lon,
                                      final int i, final int j,
                                      final int w, final int h,
                                      final Result result) {

        int w2 = w >> 1;
        int h2 = h >> 1;
        final int i2 = i + w2;
        final int j2 = j + h2;
        final int w2r = w - w2;
        final int h2r = h - h2;

        if (w2 < 2) {
            w2 = 2;
        }

        if (h2 < 2) {
            h2 = 2;
        }

        final int increasedDepth = depth + 1;
        final boolean b1 = quadTreeSearch(increasedDepth, lat, lon, i, j, w2, h2, result);
        final boolean b2 = quadTreeSearch(increasedDepth, lat, lon, i, j2, w2, h2r, result);
        final boolean b3 = quadTreeSearch(increasedDepth, lat, lon, i2, j, w2r, h2, result);
        final boolean b4 = quadTreeSearch(increasedDepth, lat, lon, i2, j2, w2r, h2r, result);

        return b1 || b2 || b3 || b4;
    }

    public static class Plugin implements InversePlugin {

        private final boolean fractionalAccuracy;

        public Plugin(boolean fractionalAccuracy) {
            this.fractionalAccuracy = fractionalAccuracy;
        }

        @Override
        public InverseCoding create() {
            return new PixelQuadTreeInverse(fractionalAccuracy);
        }
    }
}
