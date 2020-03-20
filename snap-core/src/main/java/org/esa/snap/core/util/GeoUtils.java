package org.esa.snap.core.util;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.math.Range;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;

import java.awt.Rectangle;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.*;
import static org.esa.snap.core.util.ProductUtils.normalizeGeoPolygon;

public class GeoUtils {

    /**
     * Creates the geographical boundary of the given product and returns it as a list of geographical coordinates.
     *
     * @param product the input product, must not be null
     * @param step    the step given in pixels
     * @return an array of geographical coordinates
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     */
    public static GeoPos[] createGeoBoundary(Product product, int step) {
        return createGeoBoundary(product, null, step, true);
    }

    /**
     * Creates the geographical boundary of the given region within the given product and returns it as a list of
     * geographical coordinates.
     *
     * @param product        the input product, must not be null
     * @param region         the region rectangle in product pixel coordinates, can be null for entire product
     * @param step           the step given in pixels
     * @param usePixelCenter {@code true} if the pixel center should be used to create the boundary
     * @return an array of geographical coordinates
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     */
    public static GeoPos[] createGeoBoundary(Product product, Rectangle region, int step,
                                             final boolean usePixelCenter) {
        final GeoCoding gc = product.getSceneGeoCoding();
        if (gc == null) {
            throw new IllegalArgumentException(UtilConstants.MSG_NO_GEO_CODING);
        }

        if (region == null) {
            region = new Rectangle(0,
                    0,
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight());
        }

        return getGeoBoundary(gc, region, step, usePixelCenter);
    }

    /**
     * Creates the geographical boundary of the given region within the given raster and returns it as a list of
     * geographical coordinates.
     *
     * @param raster the input raster, must not be null
     * @param region the region rectangle in raster pixel coordinates, can be null for entire raster
     * @param step   the step given in pixels
     * @return an array of geographical coordinates
     * @throws IllegalArgumentException if raster is null or if the raster has no {@link GeoCoding} is null
     * @see #createPixelBoundary(RasterDataNode, java.awt.Rectangle, int)
     */
    public static GeoPos[] createGeoBoundary(RasterDataNode raster, Rectangle region, int step) {
        return createGeoBoundary(raster, region, step, true);
    }

    /**
     * Creates the geographical boundary of the given region within the given RasterDataNode and returns it as a list of
     * geographical coordinates.
     *
     * @param rasterDataNode the input rasterDataNode, must not be null
     * @param region         the region rectangle in product pixel coordinates, can be null for entire product
     * @param step           the step given in pixels
     * @param usePixelCenter {@code true} if the pixel center should be used to create the boundary
     * @return an array of geographical coordinates
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     */
    public static GeoPos[] createGeoBoundary(RasterDataNode rasterDataNode, Rectangle region, int step,
                                             final boolean usePixelCenter) {
        final GeoCoding gc = rasterDataNode.getGeoCoding();
        if (gc == null) {
            throw new IllegalArgumentException(UtilConstants.MSG_NO_GEO_CODING);
        }

        if (region == null) {
            region = new Rectangle(0,
                    0,
                    rasterDataNode.getRasterWidth(),
                    rasterDataNode.getRasterHeight());
        }

        return getGeoBoundary(gc, region, step, usePixelCenter);
    }

    // @todo 1 tb/tb implement this! 2020-01-31
//    public static ArrayList<GeoPos[]> createGeoBoundaries(Product product, Rectangle region, int step,
//                                                          final boolean usePixelCenter) {
//        throw new RuntimeException("not implemented");
//    }

    /**
     * Converts the geographic boundary entire product into one, two or three shape objects. If the product does not
     * intersect the 180 degree meridian, a single general path is returned. Otherwise two or three shapes are created
     * and returned in the order from west to east.
     * <p>
     * The geographic boundary of the given product are returned as shapes comprising (longitude,latitude) pairs.
     *
     * @param product the input product
     * @return an array of shape objects
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     * @see #createGeoBoundary(Product, int)
     */
    public static GeneralPath[] createGeoBoundaryPaths(Product product) {
        final Rectangle rect = new Rectangle(0, 0, product.getSceneRasterWidth(), product.getSceneRasterHeight());
        final int step = Math.min(rect.width, rect.height) / 8;
        return createGeoBoundaryPaths(product, rect, step > 0 ? step : 1);
    }

    /**
     * Converts the geographic boundary entire raster data node into one, two or three shape objects. If the data does not
     * intersect the 180 degree meridian, a single general path is returned. Otherwise two or three shapes are created
     * and returned in the order from west to east.
     * <p>
     * The geographic boundary of the given raster data node are returned as shapes comprising (longitude,latitude) pairs.
     *
     * @param rasterDataNode the input raster data node
     * @return an array of shape objects
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     * @see #createGeoBoundary(Product, int)
     */
    public static GeneralPath[] createGeoBoundaryPaths(RasterDataNode rasterDataNode) {
        final Rectangle rect = new Rectangle(0, 0, rasterDataNode.getRasterWidth(), rasterDataNode.getRasterHeight());
        final int step = Math.min(rect.width, rect.height) / 8;
        return createGeoBoundaryPaths(rasterDataNode, rect, step > 0 ? step : 1, false);
    }

    /**
     * Converts the geographic boundary of the region within the given product into one, two or three shape objects. If
     * the product does not intersect the 180 degree meridian, a single general path is returned. Otherwise two or three
     * shapes are created and returned in the order from west to east.
     * <p>
     * This method delegates to {@link #createGeoBoundaryPaths(Product, java.awt.Rectangle, int, boolean) createGeoBoundaryPaths(Product, Rectangle, int, boolean)}
     * and the additional parameter {@code usePixelCenter} is {@code true}.
     * <p>
     * The geographic boundary of the given product are returned as shapes comprising (longitude,latitude) pairs.
     *
     * @param product the input product
     * @param region  the region rectangle in product pixel coordinates, can be null for entire product
     * @param step    the step given in pixels
     * @return an array of shape objects
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     */
    public static GeneralPath[] createGeoBoundaryPaths(Product product, Rectangle region, int step) {
        final boolean usePixelCenter = true;
        return createGeoBoundaryPaths(product, region, step, usePixelCenter);
    }

    /**
     * Converts the geographic boundary of the region within the given product into one, two or three shape objects. If
     * the product does not intersect the 180 degree meridian, a single general path is returned. Otherwise two or three
     * shapes are created and returned in the order from west to east.
     * <p>
     * The geographic boundary of the given product are returned as shapes comprising (longitude,latitude) pairs.
     *
     * @param product        the input product
     * @param region         the region rectangle in product pixel coordinates, can be null for entire product
     * @param step           the step given in pixels
     * @param usePixelCenter {@code true} if the pixel center should be used to create the pathes
     * @return an array of shape objects
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     * @see #createGeoBoundary(Product, java.awt.Rectangle, int, boolean)
     */
    public static GeneralPath[] createGeoBoundaryPaths(Product product, Rectangle region, int step,
                                                       final boolean usePixelCenter) {
        final GeoPos[] geoPoints = createGeoBoundary(product, region, step, usePixelCenter);
        normalizeGeoPolygon(geoPoints);

        final ArrayList<GeneralPath> pathList = assemblePathList(geoPoints);

        return pathList.toArray(new GeneralPath[0]);
    }

    /**
     * Converts the geographic boundary of the region within the given rastrer data node into one, two or three shape objects. If
     * the data node does not intersect the 180 degree meridian, a single general path is returned. Otherwise two or three
     * shapes are created and returned in the order from west to east.
     * <p>
     * The geographic boundary of the given raster data node are returned as shapes comprising (longitude,latitude) pairs.
     *
     * @param rasterDataNode the input raster data node
     * @param region         the region rectangle in product pixel coordinates, can be null for entire product
     * @param step           the step given in pixels
     * @param usePixelCenter {@code true} if the pixel center should be used to create the pathes
     * @return an array of shape objects
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     * @see #createGeoBoundary(Product, java.awt.Rectangle, int, boolean)
     */
    public static GeneralPath[] createGeoBoundaryPaths(RasterDataNode rasterDataNode, Rectangle region, int step,
                                                       final boolean usePixelCenter) {
        final GeoPos[] geoPoints = createGeoBoundary(rasterDataNode, region, step, usePixelCenter);
        normalizeGeoPolygon(geoPoints);

        final ArrayList<GeneralPath> pathList = assemblePathList(geoPoints);

        return pathList.toArray(new GeneralPath[0]);
    }

    /**
     * Creates a rectangular boundary expressed in pixel positions for the given source rectangle. If the source
     * {@code rect} is 100 x 50 pixels and {@code step} is 10 the returned array will countain exactly 2 * 10
     * + 2 * (5 - 2) = 26 pixel positions.
     * <p>This method is used for an intermediate step when determining a raster boundary expressed in geographical
     * co-ordinates.
     *
     * @param raster the raster
     * @param rect   the source rectangle
     * @param step   the mean distance from one pixel position to the other in the returned array
     * @return the rectangular boundary
     */
    public static PixelPos[] createPixelBoundary(RasterDataNode raster, Rectangle rect, int step) {
        final int width = raster.getRasterWidth();
        final int height = raster.getRasterHeight();
        return createPixelBoundary(width, height, rect, step);
    }

    /**
     * Creates a rectangular boundary expressed in pixel positions for the given source rectangle. If the source
     * {@code rect} is 100 x 50 pixels and {@code step} is 10 the returned array will contain exactly 2 * 10
     * + 2 * (5 - 2) = 26 pixel positions.
     * <p>This method is used for an intermediate step when determining a raster boundary expressed in geographical
     * co-ordinates.
     *
     * @param rasterWidth  the raster width in pixels
     * @param rasterHeight the raster height in pixels
     * @param rect         the source rectangle
     * @param step         the mean distance from one pixel position to the other in the returned array
     * @return the rectangular boundary
     */
    public static PixelPos[] createPixelBoundary(int rasterWidth, int rasterHeight, Rectangle rect, int step) {
        if (rect == null) {
            rect = new Rectangle(0,
                    0,
                    rasterWidth,
                    rasterHeight);
        }
        return createPixelBoundaryFromRect(rect, step);
    }

    /**
     * Creates a rectangular boundary expressed in pixel positions for the given source rectangle. If the source
     * {@code rect} is 100 x 50 pixels and {@code step} is 10 the returned array will contain exactly 2 * 10
     * + 2 * (5 - 2) = 26 pixel positions.
     * <p>This method is used for an intermediate step when determining a product boundary expressed in geographical
     * co-ordinates.
     * <p> This method delegates to {@link #createPixelBoundaryFromRect(java.awt.Rectangle, int, boolean) createPixelBoundaryFromRect(Rectangle, int, boolean)}
     * and the additional boolean parameter {@code usePixelCenter} is {@code true}.
     *
     * @param rect the source rectangle
     * @param step the mean distance from one pixel position to the other in the returned array
     * @return the rectangular boundary
     */
    public static PixelPos[] createPixelBoundaryFromRect(Rectangle rect, int step) {
        final boolean usePixelCenter = true;
        return createPixelBoundaryFromRect(rect, step, usePixelCenter);
    }

    private static GeoPos[] getGeoBoundary(GeoCoding gc, Rectangle region, int step, boolean usePixelCenter) {
        final PixelPos[] points = createPixelBoundaryFromRect(region, step, usePixelCenter);
        ArrayList<GeoPos> geoPoints = new ArrayList<>(points.length);
        boolean calculateInsets = false;
        for (final PixelPos pixelPos : points) {
            final GeoPos gcGeoPos = gc.getGeoPos(pixelPos, null);
            if (!gcGeoPos.isValid()) {
                calculateInsets = true;
                break;
            }
            geoPoints.add(gcGeoPos);
        }

        if (calculateInsets) {
            geoPoints.clear();
            geoPoints = createInsetsGeoBoundary(gc, step, region, usePixelCenter);
        }

        return geoPoints.toArray(new GeoPos[0]);
    }

    private static ArrayList<GeoPos> createInsetsGeoBoundary(GeoCoding gc, int step, Rectangle rect, boolean usePixelCenter) {
        final double insetDistance = usePixelCenter ? 0.5 : 0.0;
        int xStart = rect.x;
        int yStart = rect.y;
        final int w = usePixelCenter ? rect.width - 1 : rect.width;
        final int h = usePixelCenter ? rect.height - 1 : rect.height;
        int xEnd = xStart + w;
        int yEnd = yStart + h;

        final ArrayList<GeoPos> geoPosList = new ArrayList<>();

        final int validYMin = getFirstValid_Y(gc, insetDistance, xStart, yStart, xEnd, yEnd);
        if (validYMin >= yEnd) {
            return geoPosList;  // empty list, we do not have valid geo positions within the rectangle tb 2020-01-31
        }
        final int validYMax = getLastValid_Y(gc, insetDistance, xStart, yStart, xEnd, yEnd);
        if (validYMax <= validYMin) {
            return geoPosList;  // empty list, we do not have valid geo positions within the rectangle tb 2020-01-31
        }

        yStart = validYMin;
        yEnd = validYMax;

        final int validXMin = getFirstValid_X(gc, insetDistance, xStart, yStart, xEnd, yEnd);
        if (validXMin >= xEnd) {
            return geoPosList; // empty list, we do not have valid geo positions within the rectangle tb 2020-01-31
        }
        final int validXMax = getLastValid_X(gc, insetDistance, xStart, yStart, xEnd, yEnd);
        if (validXMax <= validXMin) {
            return geoPosList; // empty list, we do not have valid geo positions within the rectangle tb 2020-01-31
        }

        xStart = validXMin;
        xEnd = validXMax;

        PixelPos pixelPos;
        GeoPos geoPos;

        int lastX = 0;
        for (int x = xStart; x < xEnd; x += step) {
            final double xPos = x + insetDistance;
            pixelPos = new PixelPos(xPos, yStart + insetDistance);
            geoPos = gc.getGeoPos(pixelPos, null);
            if (geoPos.isValid()) {
                geoPosList.add(geoPos);
            } else {
                // increase y until we have a valid pixel.
                for (int y = yStart + 1; y < yEnd; y++) {
                    pixelPos = new PixelPos(xPos, y + insetDistance);
                    geoPos = gc.getGeoPos(pixelPos, null);
                    if (geoPos.isValid()) {
                        geoPosList.add(geoPos);
                        break;
                    }
                }
            }
            lastX = x;
        }

        int lastY = 0;
        for (int y = yStart; y < yEnd; y += step) {
            final double yPos = y + insetDistance;
            pixelPos = new PixelPos(xEnd, yPos);
            geoPos = gc.getGeoPos(pixelPos, null);
            if (geoPos.isValid()) {
                geoPosList.add(geoPos);
            } else {
                // decrease x until we have a valid pixel
                for (int x = xEnd; x >= xStart; x--) {
                    pixelPos = new PixelPos(x, yPos);
                    geoPos = gc.getGeoPos(pixelPos, null);
                    if (geoPos.isValid()) {
                        geoPosList.add(geoPos);
                        break;
                    }
                }
            }
            lastY = y;
        }

        // add corner pixel, if it is a valid one
        pixelPos = new PixelPos(xEnd, yEnd);
        geoPos = gc.getGeoPos(pixelPos, null);
        if (geoPos.isValid()) {
            geoPosList.add(geoPos);
        }

        for (int x = lastX; x > xStart; x -= step) {
            final double xPos = x + insetDistance;
            pixelPos = new PixelPos(xPos, yEnd + insetDistance);
            geoPos = gc.getGeoPos(pixelPos, null);
            if (geoPos.isValid()) {
                geoPosList.add(geoPos);
            } else {
                // decrease y until we have a valid pixel.
                for (int y = yEnd - 1; y >= yStart; y--) {
                    pixelPos = new PixelPos(xPos, y + insetDistance);
                    geoPos = gc.getGeoPos(pixelPos, null);
                    if (geoPos.isValid()) {
                        geoPosList.add(geoPos);
                        break;
                    }
                }
            }
        }

        // add corner pixel, if it is a valid one
        pixelPos = new PixelPos(xStart, yEnd);
        geoPos = gc.getGeoPos(pixelPos, null);
        if (geoPos.isValid()) {
            geoPosList.add(geoPos);
        }

        for (int y = lastY; y > yStart; y -= step) {
            final double yPos = y + insetDistance;
            pixelPos = new PixelPos(xStart + insetDistance, yPos);
            geoPos = gc.getGeoPos(pixelPos, null);
            if (geoPos.isValid()) {
                geoPosList.add(geoPos);
            } else {
                // increase x until we have a valid pixel
                for (int x = xStart; x < xEnd; x++) {
                    pixelPos = new PixelPos(x + insetDistance, yPos);
                    geoPos = gc.getGeoPos(pixelPos, null);
                    if (geoPos.isValid()) {
                        geoPosList.add(geoPos);
                        break;
                    }
                }
            }
        }

        return geoPosList;
    }

    private static int getFirstValid_Y(GeoCoding gc, double insetDistance, int xStart, int yStart, int xEnd, int yEnd) {
        for (int y = yStart; y < yEnd; y++) {
            for (int x = xStart; x < xEnd; x++) {
                final PixelPos pixelPos = new PixelPos(x + insetDistance, y + insetDistance);
                final GeoPos geoPos = gc.getGeoPos(pixelPos, null);
                if (geoPos.isValid()) {
                    return y;
                }
            }
        }
        return yEnd;
    }

    private static int getLastValid_Y(GeoCoding gc, double insetDistance, int xStart, int yStart, int xEnd, int yEnd) {
        for (int y = yEnd; y >= yStart; y--) {
            for (int x = xStart; x < xEnd; x++) {
                final PixelPos pixelPos = new PixelPos(x + insetDistance, y + insetDistance);
                final GeoPos geoPos = gc.getGeoPos(pixelPos, null);
                if (geoPos.isValid()) {
                    return y;
                }
            }
        }
        return yStart;
    }

    private static int getFirstValid_X(GeoCoding gc, double insetDistance, int xStart, int yStart, int xEnd, int yEnd) {
        for (int x = xStart; x < xEnd; x++) {
            for (int y = yStart; y < yEnd; y++) {
                final PixelPos pixelPos = new PixelPos(x + insetDistance, y + insetDistance);
                final GeoPos geoPos = gc.getGeoPos(pixelPos, null);
                if (geoPos.isValid()) {
                    return x;
                }
            }
        }

        return xEnd;
    }

    private static int getLastValid_X(GeoCoding gc, double insetDistance, int xStart, int yStart, int xEnd, int yEnd) {
        for (int x = xEnd; x >= xStart; x--) {
            for (int y = yStart; y < yEnd; y++) {
                final PixelPos pixelPos = new PixelPos(x + insetDistance, y + insetDistance);
                final GeoPos geoPos = gc.getGeoPos(pixelPos, null);
                if (geoPos.isValid()) {
                    return x;
                }
            }
        }

        return xEnd;
    }

    /**
     * Creates a rectangular boundary expressed in pixel positions for the given source rectangle. If the source
     * {@code rect} is 100 x 50 pixels and {@code step} is 10 the returned array will countain exactly 2 * 10
     * + 2 * (5 - 2) = 26 pixel positions.
     * <p>
     * This method is used for an intermediate step when determining a product boundary expressed in geographical
     * co-ordinates.
     * <p>
     *
     * @param rect           the source rectangle
     * @param step           the mean distance from one pixel position to the other in the returned array
     * @param usePixelCenter {@code true} if the pixel center should be used
     * @return the rectangular boundary
     */
    static PixelPos[] createPixelBoundaryFromRect(final Rectangle rect, int step, final boolean usePixelCenter) {
        // package access for testing only tb 2020-01-30
        final double insetDistance = usePixelCenter ? 0.5 : 0.0;
        final int x1 = rect.x;
        final int y1 = rect.y;
        final int w = usePixelCenter ? rect.width - 1 : rect.width;
        final int h = usePixelCenter ? rect.height - 1 : rect.height;
        final int x2 = x1 + w;
        final int y2 = y1 + h;

        if (step <= 0) {
            step = 2 * Math.max(rect.width, rect.height); // don't step!
        }

        final ArrayList<PixelPos> pixelPosList = new ArrayList<>(2 * (rect.width + rect.height) / step + 10);

        int lastX = 0;
        for (int x = x1; x < x2; x += step) {
            pixelPosList.add(new PixelPos(x + insetDistance, y1 + insetDistance));
            lastX = x;
        }

        int lastY = 0;
        for (int y = y1; y < y2; y += step) {
            pixelPosList.add(new PixelPos(x2 + insetDistance, y + insetDistance));
            lastY = y;
        }

        pixelPosList.add(new PixelPos(x2 + insetDistance, y2 + insetDistance));

        for (int x = lastX; x > x1; x -= step) {
            pixelPosList.add(new PixelPos(x + insetDistance, y2 + insetDistance));
        }

        pixelPosList.add(new PixelPos(x1 + insetDistance, y2 + insetDistance));

        for (int y = lastY; y > y1; y -= step) {
            pixelPosList.add(new PixelPos(x1 + insetDistance, y + insetDistance));
        }

        return pixelPosList.toArray(new PixelPos[0]);
    }

    static ArrayList<GeneralPath> assemblePathList(GeoPos[] geoPoints) {
        final ArrayList<GeneralPath> pathList = new ArrayList<>(16);

        if (geoPoints.length > 1) {
            final GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, geoPoints.length + 8);
            Range range = fillPath(geoPoints, path);

            int runIndexMin = (int) Math.floor((range.getMin() + 180) / 360);
            int runIndexMax = (int) Math.floor((range.getMax() + 180) / 360);

            if (runIndexMin == 0 && runIndexMax == 0) {
                // the path is completely within [-180, 180] longitude
                pathList.add(path);
                return pathList;
            }

            final Area pathArea = new Area(path);
            for (int k = runIndexMin; k <= runIndexMax; k++) {
                final Area currentArea = new Area(new Rectangle2D.Double(k * 360.0 - 180.0, -90.0, 360.0, 180.0));
                currentArea.intersect(pathArea);
                if (!currentArea.isEmpty()) {
                    pathList.addAll(areaToSubPaths(currentArea, -k * 360.0));
                }
            }
        }
        return pathList;
    }

    /**
     * Fills the path with the given geo-points.
     *
     * @param geoPoints the points to add to the path
     * @param path      the path
     * @return the longitude value range
     */
    static Range fillPath(GeoPos[] geoPoints, GeneralPath path) {
        double lon = geoPoints[0].getLon();

        final Range range = new Range(lon, lon);
        path.moveTo(lon, geoPoints[0].getLat());

        for (int i = 1; i < geoPoints.length; i++) {
            if (!geoPoints[i].isValid()) {
                continue;
            }

            lon = geoPoints[i].getLon();
            final double lat = geoPoints[i].getLat();
            if (lon < range.getMin()) {
                range.setMin(lon);
            }
            if (lon > range.getMax()) {
                range.setMax(lon);
            }
            path.lineTo(lon, lat);
        }

        path.closePath();
        return range;
    }

    /**
     * Turns an area into one or multiple paths.
     *
     * @param area   the area to convert
     * @param deltaX the value is used to translate the x-cordinates
     * @return the list of paths
     */
    public static java.util.List<GeneralPath> areaToSubPaths(Area area, double deltaX) {
        final List<GeneralPath> subPaths = new ArrayList<>();

        final float[] floats = new float[6];

        // move to correct rectangle
        final AffineTransform transform = AffineTransform.getTranslateInstance(deltaX, 0.0);
        final PathIterator iterator = area.getPathIterator(transform);

        GeneralPath pixelPath = null;
        while (!iterator.isDone()) {
            if (pixelPath == null) {
                pixelPath = new GeneralPath(GeneralPath.WIND_NON_ZERO);
            }
            final int segmentType = iterator.currentSegment(floats);
            switch (segmentType) {
                case PathIterator.SEG_LINETO:
                    pixelPath.lineTo(floats[0], floats[1]);
                    break;
                case PathIterator.SEG_MOVETO:
                    pixelPath.moveTo(floats[0], floats[1]);
                    break;
                case PathIterator.SEG_CLOSE:
                    pixelPath.closePath();
                    subPaths.add(pixelPath);
                    pixelPath = null;
                    break;
                default:
                    throw new IllegalStateException("unhandled segment type in path iterator: " + segmentType);
            }
            iterator.next();
        }
        return subPaths;
    }

    private static org.locationtech.jts.geom.Polygon convertAwtPathToJtsPolygon(Path2D path, GeometryFactory factory) {
        final PathIterator pathIterator = path.getPathIterator(null);
        ArrayList<double[]> coordList = new ArrayList<>();
        int lastOpenIndex = 0;
        while (!pathIterator.isDone()) {
            final double[] coords = new double[6];
            final int segType = pathIterator.currentSegment(coords);
            if (segType == PathIterator.SEG_CLOSE) {
                // we should only detect a single SEG_CLOSE
                coordList.add(coordList.get(lastOpenIndex));
                lastOpenIndex = coordList.size();
            } else {
                coordList.add(coords);
            }
            pathIterator.next();
        }
        final Coordinate[] coordinates = new Coordinate[coordList.size()];
        for (int i1 = 0; i1 < coordinates.length; i1++) {
            final double[] coord = coordList.get(i1);
            coordinates[i1] = new Coordinate(coord[0], coord[1]);
        }

        return factory.createPolygon(factory.createLinearRing(coordinates), null);
    }

    public static Rectangle computePixelRegionUsingGeometry(GeoCoding rasterGeoCoding, int rasterWidth, int rasterHeight, Geometry geometryRegion,
                                                            int numBorderPixels, boolean roundPixelRegion) {

        final Geometry rasterGeometry = computeRasterGeometry(rasterGeoCoding, rasterWidth, rasterHeight);
        final Geometry regionIntersection = geometryRegion.intersection(rasterGeometry);
        if (regionIntersection.isEmpty()) {
            return new Rectangle(); // the intersection is empty
        }
        final GeoUtils.PixelRegionFinder pixelRegionFinder = new GeoUtils.PixelRegionFinder(rasterGeoCoding, roundPixelRegion);
        regionIntersection.apply(pixelRegionFinder);
        final Rectangle pixelRegion = pixelRegionFinder.getPixelRegion();
        pixelRegion.grow(numBorderPixels, numBorderPixels);
        return pixelRegion.intersection(new Rectangle(rasterWidth, rasterHeight));
    }

    public static Geometry computeGeometryUsingPixelRegion(GeoCoding rasterGeoCoding, Rectangle pixelRegion) {
        if (pixelRegion == null) {
            throw new NullPointerException("The pixel region is null.");
        }
        final int step = Math.min(pixelRegion.width, pixelRegion.height) / 8;
        GeneralPath[] paths = createGeoBoundaryPaths(rasterGeoCoding, pixelRegion, step, false);
        final org.locationtech.jts.geom.Polygon[] polygons = new org.locationtech.jts.geom.Polygon[paths.length];
        final GeometryFactory factory = new GeometryFactory();
        for (int i = 0; i < paths.length; i++) {
            polygons[i] = convertAwtPathToJtsPolygon(paths[i], factory);
        }
        if (polygons.length == 1) {
            return polygons[0];
        } else {
            return factory.createMultiPolygon(polygons);
        }
    }

    public static Geometry computeRasterGeometry(GeoCoding rasterGeoCoding, int rasterWidth, int rasterHeight) {
        final GeneralPath[] paths = createGeoBoundaryPaths(rasterGeoCoding, rasterWidth, rasterHeight);
        final org.locationtech.jts.geom.Polygon[] polygons = new org.locationtech.jts.geom.Polygon[paths.length];
        final GeometryFactory factory = new GeometryFactory();
        for (int i = 0; i < paths.length; i++) {
            polygons[i] = convertAwtPathToJtsPolygon(paths[i], factory);
        }
        final DouglasPeuckerSimplifier peuckerSimplifier = new DouglasPeuckerSimplifier(polygons.length == 1 ? polygons[0] : factory.createMultiPolygon(polygons));
        return peuckerSimplifier.getResultGeometry();
    }

    private static GeneralPath[] createGeoBoundaryPaths(GeoCoding productGeoCoding, int productWidth, int productHeight) {
        final Rectangle rect = new Rectangle(0, 0, productWidth, productHeight);
        final int step = Math.min(rect.width, rect.height) / 8;
        return createGeoBoundaryPaths(productGeoCoding, rect, step > 0 ? step : 1, true);
    }

    private static GeoPos[] createGeoBoundary(GeoCoding geoCoding, Rectangle region, int step, boolean usePixelCenter) {
        if (geoCoding == null) {
            throw new NullPointerException("The geo coding is null.");
        }
        if (region == null) {
            throw new NullPointerException("The region is null.");
        }
        final PixelPos[] points = GeoUtils.createPixelBoundaryFromRect(region, step, usePixelCenter);
        final ArrayList<GeoPos> geoPoints = new ArrayList<>(points.length);
        for (final PixelPos pixelPos : points) {
            final GeoPos gcGeoPos = geoCoding.getGeoPos(pixelPos, null);
            if (true) { // including valid positions only leads to unit test failures 'very elsewhere' rq-20140414
                geoPoints.add(gcGeoPos);
            }
        }
        return geoPoints.toArray(new GeoPos[geoPoints.size()]);
    }

    private static GeneralPath[] createGeoBoundaryPaths(GeoCoding geoCoding, Rectangle region, int step, boolean usePixelCenter) {
        if (geoCoding == null) {
            throw new NullPointerException("The geo coding is null.");
        }
        if (region == null) {
            throw new NullPointerException("The region is null.");
        }
        final GeoPos[] geoPoints = createGeoBoundary(geoCoding, region, step, usePixelCenter);
        normalizeGeoPolygon(geoPoints);
        final ArrayList<GeneralPath> pathList = assemblePathList(geoPoints);
        return pathList.toArray(new GeneralPath[pathList.size()]);
    }

    private static class PixelRegionFinder implements CoordinateFilter {

        private final GeoCoding geoCoding;

        private int x1;
        private int y1;
        private int x2;
        private int y2;
        private boolean round = false;

        private PixelRegionFinder(GeoCoding geoCoding, boolean round) {
            this.geoCoding = geoCoding;
            x1 = Integer.MAX_VALUE;
            x2 = Integer.MIN_VALUE;
            y1 = Integer.MAX_VALUE;
            y2 = Integer.MIN_VALUE;
            this.round = round;
        }

        @Override
        public void filter(Coordinate coordinate) {
            final GeoPos geoPos = new GeoPos(coordinate.y, coordinate.x);
            final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, null);
            if (pixelPos.isValid()) {
                if(round) {
                    x1 = min(x1, (int) round(pixelPos.x));
                    x2 = max(x2, (int) round(pixelPos.x));
                    y1 = min(y1, (int) round(pixelPos.y));
                    y2 = max(y2, (int) round(pixelPos.y));
                } else {
                    x1 = min(x1, (int) floor(pixelPos.x));
                    x2 = max(x2, (int) ceil(pixelPos.x));
                    y1 = min(y1, (int) floor(pixelPos.y));
                    y2 = max(y2, (int) ceil(pixelPos.y));
                }
            }
        }

        public Rectangle getPixelRegion() {
            return new Rectangle(x1, y1, x2 - x1, y2 - y1);
        }
    }
}
