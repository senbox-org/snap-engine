/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.datamodel;

import org.esa.snap.core.util.GeoUtils;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.Range;

import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * A geometric representation of a geographical grid measured in longitudes and latitudes.
 *
 * @author Brockmann Consult
 * @author Daniel Knowles
 */
//JAN2018 - Daniel Knowles - updated with SeaDAS gridline revisions


public class Graticule {

    private static final double ONE_MINUTE = 1.0 / 60.0;
    private static final double TEN_MINUTES = 10.0 / 60.0;

    private final GeneralPath[] _linePaths;
    private final TextGlyph[] _textGlyphsNorth;
    private final TextGlyph[] _textGlyphsSouth;
    private final TextGlyph[] _textGlyphsWest;
    private final TextGlyph[] _textGlyphsEast;
    private final TextGlyph[] _textGlyphsLatCorners;
    private final TextGlyph[] _textGlyphsLonCorners;
    private final PixelPos[] _tickPointsNorth;
    private final PixelPos[] _tickPointsSouth;
    private final PixelPos[] _tickPointsWest;
    private final PixelPos[] _tickPointsEast;

    public enum TextLocation {
        NORTH,
        SOUTH,
        WEST,
        EAST,
        TOP,
        BOTTOM,
        LEFT,
        RIGHT
    }

    public static int TOP_LEFT_CORNER_INDEX = 0;
    public static int TOP_RIGHT_CORNER_INDEX = 1;
    public static int BOTTOM_RIGHT_CORNER_INDEX = 2;
    public static int BOTTOM_LEFT_CORNER_INDEX = 3;

    private Graticule(GeneralPath[] paths,
                      TextGlyph[] textGlyphsNorth,
                      TextGlyph[] textGlyphsSouth,
                      TextGlyph[] textGlyphsWest,
                      TextGlyph[] textGlyphsEast,
                      TextGlyph[] textGlyphsLatCorners,
                      TextGlyph[] textGlyphsLonCorners,
                      PixelPos[] tickPointsNorth,
                      PixelPos[] tickPointsSouth,
                      PixelPos[] tickPointsWest,
                      PixelPos[] tickPointsEast
    ) {
        _linePaths = paths;
        _textGlyphsNorth = textGlyphsNorth;
        _textGlyphsSouth = textGlyphsSouth;
        _textGlyphsWest = textGlyphsWest;
        _textGlyphsEast = textGlyphsEast;
        _textGlyphsLatCorners = textGlyphsLatCorners;
        _textGlyphsLonCorners = textGlyphsLonCorners;
        _tickPointsNorth = tickPointsNorth;
        _tickPointsSouth = tickPointsSouth;
        _tickPointsWest = tickPointsWest;
        _tickPointsEast = tickPointsEast;
    }


    public GeneralPath[] getLinePaths() {
        return _linePaths;
    }

    public TextGlyph[] getTextGlyphsNorth() {
        return _textGlyphsNorth;
    }

    public TextGlyph[] getTextGlyphsSouth() {
        return _textGlyphsSouth;
    }

    public TextGlyph[] getTextGlyphsWest() {
        return _textGlyphsWest;
    }

    public TextGlyph[] getTextGlyphsEast() {
        return _textGlyphsEast;
    }

    public TextGlyph[] getTextGlyphsLatCorners() {
        return _textGlyphsLatCorners;
    }

    public TextGlyph[] getTextGlyphsLonCorners() {
        return _textGlyphsLonCorners;
    }

    public PixelPos[] getTickPointsNorth() {
        return _tickPointsNorth;
    }

    public PixelPos[] getTickPointsSouth() {
        return _tickPointsSouth;
    }

    public PixelPos[] getTickPointsWest() {
        return _tickPointsWest;
    }

    public PixelPos[] getTickPointsEast() {
        return _tickPointsEast;
    }

    /**
     * Creates a graticule for the given product.
     *
     * @param raster              the product
     * @param desiredNumGridLines the grid cell size in pixels
     * @param latMajorStep        the grid cell size in meridional direction
     * @param lonMajorStep        the grid cell size in parallel direction
     * @return the graticule or null, if it could not be created
     */
    public static Graticule create(RasterDataNode raster,
                                   int desiredNumGridLines,
                                   double latMajorStep,
                                   double lonMajorStep,
                                   boolean formatCompass,
                                   boolean decimalFormat) {
        Guardian.assertNotNull("product", raster);
        final GeoCoding geoCoding = raster.getGeoCoding();
        if (geoCoding == null || raster.getRasterWidth() < 16 || raster.getRasterHeight() < 16) {
            return null;
        }

        if (desiredNumGridLines <= 1) {
            desiredNumGridLines = 2;
        }

        final GeoPos geoDelta = getGeoDelta(geoCoding, raster);

        if (latMajorStep == 0) {
            int height = raster.getRasterHeight();
            double ratio = height / (desiredNumGridLines - 1);

            double tmpLatMajorStep = ratio * geoDelta.lat;

            latMajorStep = getSensibleDegreeIncrement(tmpLatMajorStep);
        }

        if (lonMajorStep == 0) {
            int width = raster.getRasterWidth();
            double ratio = width / (desiredNumGridLines - 1);

            double tmpLonMajorStep = ratio * geoDelta.lon;

            lonMajorStep = getSensibleDegreeIncrement(tmpLonMajorStep);
        }

        final int desiredMinorSteps = getDesiredMinorSteps(raster);
        final double ratioLatMinor = raster.getRasterHeight() / (desiredMinorSteps - 1);
        final double latMinorStep = ratioLatMinor * geoDelta.lat;
        final double ratioLonMinor = raster.getRasterHeight() / (desiredMinorSteps - 1);
        final double lonMinorStep = ratioLonMinor * geoDelta.lon;

        int geoBoundaryStep = getGeoBoundaryStep(geoCoding, raster);
        GeoPos[] geoBoundary = createGeoBoundary(raster, geoBoundaryStep);

// nf Debugging, don't delete!
//        GeneralPath generalPath = createPixelBoundaryPath(geoCoding, geoBoundary);
//        if (generalPath != null) {
//            return new Graticule(new GeneralPath[]{generalPath}, null);
//        }

        final Range[] ranges = getRanges(geoBoundary);
        final Range lonRange = ranges[0];
        final Range latRange = ranges[1];

        final List<List<Coord>> meridianList = computeMeridianList(raster.getGeoCoding(), geoBoundary, lonMajorStep, latMinorStep,
                                                                   lonRange.getMin(), lonRange.getMax());
        final List<List<Coord>> parallelList = computeParallelList(raster.getGeoCoding(), geoBoundary, latMajorStep, lonMinorStep,
                                                                   latRange.getMin(), latRange.getMax());

        if (parallelList.size() > 0 && meridianList.size() > 0) {
            final GeneralPath[] paths = createPaths(parallelList, meridianList);

            final TextGlyph[] textGlyphsNorth = createTextGlyphs(parallelList, meridianList, TextLocation.NORTH, formatCompass, decimalFormat);
            final TextGlyph[] textGlyphsSouth = createTextGlyphs(parallelList, meridianList, TextLocation.SOUTH, formatCompass, decimalFormat);
            final TextGlyph[] textGlyphsWest = createTextGlyphs(parallelList, meridianList, TextLocation.WEST, formatCompass, decimalFormat);
            final TextGlyph[] textGlyphsEast = createTextGlyphs(parallelList, meridianList, TextLocation.EAST, formatCompass, decimalFormat);

            final TextGlyph[] textGlyphsLatCorners = createLatCornerTextGlyphs(raster, formatCompass, decimalFormat);
            final TextGlyph[] textGlyphsLonCorners = createLonCornerTextGlyphs(raster, formatCompass, decimalFormat);

            final PixelPos[] tickPointsNorth = createTickPoints(parallelList, meridianList, TextLocation.NORTH);
            final PixelPos[] tickPointsSouth = createTickPoints(parallelList, meridianList, TextLocation.SOUTH);
            final PixelPos[] tickPointsWest = createTickPoints(parallelList, meridianList, TextLocation.WEST);
            final PixelPos[] tickPointsEast = createTickPoints(parallelList, meridianList, TextLocation.EAST);

            return new Graticule(paths,
                                 textGlyphsNorth,
                                 textGlyphsSouth,
                                 textGlyphsWest,
                                 textGlyphsEast,
                                 textGlyphsLatCorners,
                                 textGlyphsLonCorners,
                                 tickPointsNorth,
                                 tickPointsSouth,
                                 tickPointsWest,
                                 tickPointsEast);
        } else {
            return new Graticule(null,
                                 null,
                                 null,
                                 null,
                                 null,
                                 null,
                                 null,
                                 null,
                                 null,
                                 null,
                                 null);
        }
    }

    private static GeoPos[] createGeoBoundary(RasterDataNode raster, int geoBoundaryStep) {
        final GeoPos[] geoBoundary = GeoUtils.createGeoBoundary(raster, null, geoBoundaryStep);

        // add first point as last so that we catch all possible intersections tb 2020-03-31
        final GeoPos[] fullGeoBoundary = new GeoPos[geoBoundary.length + 1];
        System.arraycopy(geoBoundary, 0, fullGeoBoundary, 0, geoBoundary.length);
        fullGeoBoundary[fullGeoBoundary.length - 1] = geoBoundary[0];

        ProductUtils.normalizeGeoPolygon(fullGeoBoundary);
        return fullGeoBoundary;
    }

    static int getDesiredMinorSteps(RasterDataNode raster) {
        int desiredMinorSteps = (int) Math.min((raster.getRasterHeight() / 4.0), (raster.getRasterWidth() / 4.0));
        if (desiredMinorSteps > 200) {
            desiredMinorSteps = 200;
        } else if (desiredMinorSteps < 3) {
            desiredMinorSteps = 3;
        }
        return desiredMinorSteps;
    }

    static double getSensibleDegreeIncrement(double degreeIncrement) {
        if (degreeIncrement > 30.0) {
            return 30.0;
        } else if (degreeIncrement >= 5.0) {
            // if each division is greater than 5 degrees then round to nearest 5 degrees
            return 5.0 * Math.round((degreeIncrement / 5.0));
        } else if (degreeIncrement >= 1.0) {
            // if each division is greater than 1 degrees then round to nearest degree
            return Math.round(degreeIncrement);
        } else if (degreeIncrement >= TEN_MINUTES) {
            // round to nearest ten minutes of a degree
            return Math.round((6.0 * degreeIncrement)) / 6.0;
        } else if (degreeIncrement >= ONE_MINUTE) {
            // round to nearest minute of a degree
            return Math.round((60.0 * degreeIncrement)) / 60.0;
        } else {
            return ONE_MINUTE;
        }
    }

    static int getGeoBoundaryStep(final GeoCoding geoCoding, RasterDataNode raster) {
        double minDimensionLength = Math.min(raster.getRasterHeight(), raster.getRasterWidth());
        int step = (int) Math.floor(minDimensionLength / 50.0);

        if (step < 1) {
            step = 1;
        }

        if (geoCoding instanceof TiePointGeoCoding) {
            final TiePointGeoCoding tiePointGeoCoding = (TiePointGeoCoding) geoCoding;
            step = Math.round((int) Math.min(tiePointGeoCoding.getLonGrid().getSubSamplingX(), tiePointGeoCoding.getLonGrid().getSubSamplingY()));
        }
        return step;
    }

    private static List<List<Coord>> computeParallelList(final GeoCoding geoCoding,
                                                         final GeoPos[] geoBoundary,
                                                         final double latMajorStep,
                                                         final double lonMinorStep,
                                                         final double yMin,
                                                         final double yMax) {
        List<List<Coord>> parallelList = new ArrayList<>();
        ArrayList<GeoPos> intersectionList = new ArrayList<>();
        GeoPos geoPos, int1, int2;
        PixelPos pixelPos;
        double lat, lon;
        double my = latMajorStep * Math.floor(yMin / latMajorStep);
        for (; my <= yMax; my += latMajorStep) {
            intersectionList.clear();
            computeParallelIntersections(geoBoundary, my, intersectionList);
            if (intersectionList.size() > 0 && intersectionList.size() % 2 == 0) {
                final GeoPos[] intersections = intersectionList.toArray(new GeoPos[0]);
                Arrays.sort(intersections, new GeoPosLonComparator());
                List<Coord> parallel = new ArrayList<>();
                // loop forward order
                for (int i = 0; i < intersections.length; i += 2) {
                    int1 = intersections[i];
                    int2 = intersections[i + 1];
                    lat = int1.lat;
                    lon = int1.lon;
                    for (int k = 0; k <= 1; ) {
                        geoPos = new GeoPos(lat, limitLon(lon));
                        pixelPos = geoCoding.getPixelPos(geoPos, null);
//                        DANNY added this to avoid adding in null pixels
                        if (pixelPos.isValid()) {
                            parallel.add(new Coord(geoPos, pixelPos));
                        }
                        lon += lonMinorStep;
                        if (lon >= int2.lon) {
                            lon = int2.lon;
                            k++;
                        }
                    }
                }
                parallelList.add(parallel);
            }
        }
        return parallelList;
    }

    private static List<List<Coord>> computeMeridianList(final GeoCoding geoCoding,
                                                         final GeoPos[] geoBoundary,
                                                         final double lonMajorStep,
                                                         final double latMinorStep,
                                                         final double xMin,
                                                         final double xMax) {
        List<List<Coord>> meridianList = new ArrayList<>();
        List<GeoPos> intersectionList = new ArrayList<>();
        GeoPos geoPos, int1, int2;
        PixelPos pixelPos;
        double lat, lon;
        double mx = lonMajorStep * Math.floor(xMin / lonMajorStep);
        for (; mx <= xMax; mx += lonMajorStep) {
            intersectionList.clear();
            computeMeridianIntersections(geoBoundary, mx, intersectionList);
            if (intersectionList.size() > 0 && intersectionList.size() % 2 == 0) {
                final GeoPos[] intersections = intersectionList.toArray(new GeoPos[0]);
                Arrays.sort(intersections, new GeoPosLatComparator());
                List<Coord> meridian = new ArrayList<>();
                // loop reverse order
                for (int i = intersections.length - 2; i >= 0; i -= 2) {
                    int1 = intersections[i + 1];
                    int2 = intersections[i];
                    lat = int1.lat;
                    lon = int1.lon;
                    for (int k = 0; k <= 1; ) {
                        geoPos = new GeoPos(lat, limitLon(lon));
                        pixelPos = geoCoding.getPixelPos(geoPos, null);

                        // DANNY added this to avoid adding in null pixels
                        if (pixelPos.isValid()) {
                            meridian.add(new Coord(geoPos, pixelPos));
                        }
                        lat -= latMinorStep;
                        if (lat <= int2.lat) {
                            lat = int2.lat;
                            k++;
                        }
                    }
                }
                meridianList.add(meridian);
            }
        }
        return meridianList;
    }


    static void computeParallelIntersections(final GeoPos[] geoBoundary,
                                             final double lat,
                                             final List<GeoPos> intersectionList) {
        GeoPos geoPos = geoBoundary[0];
        double lonBoundaryPrev = geoPos.lon;
        double latBoundaryPrev = geoPos.lat;
        double lonBoundaryCurr;
        double latBoundaryCurr;

        for (int i = 1; i < geoBoundary.length; i++) {
            geoPos = geoBoundary[i];
            lonBoundaryCurr = geoPos.lon;
            latBoundaryCurr = geoPos.lat;

            // only examine steps around geoBoundary where lat is changing
            if (latBoundaryCurr != latBoundaryPrev) {
                // find the step which crosses over desired lat
                if (((lat >= latBoundaryPrev && lat <= latBoundaryCurr) ||
                        (lat >= latBoundaryCurr && lat <= latBoundaryPrev))) {

                    // compute lon based on interpolation and add geoPos to intersectionList
                    final double interpolationWeight = (lat - latBoundaryPrev) / (latBoundaryCurr - latBoundaryPrev);
                    if (interpolationWeight >= 0.0 && interpolationWeight < 1.0) {
                        final double lon = lonBoundaryPrev + interpolationWeight * (lonBoundaryCurr - lonBoundaryPrev);
                        intersectionList.add(new GeoPos(lat, lon));
                    }
                }
            }

            lonBoundaryPrev = lonBoundaryCurr;
            latBoundaryPrev = latBoundaryCurr;
        }
    }


    static void computeMeridianIntersections(final GeoPos[] geoBoundary,
                                             final double lon,
                                             final List<GeoPos> intersectionList) {
        GeoPos geoPos = geoBoundary[0];
        double lonBoundaryPrev = geoPos.lon;
        double latBoundaryPrev = geoPos.lat;
        double lonBoundaryCurr;
        double latBoundaryCurr;

        for (int i = 1; i < geoBoundary.length; i++) {
            geoPos = geoBoundary[i];
            lonBoundaryCurr = geoPos.lon;
            latBoundaryCurr = geoPos.lat;

            // only examine steps around geoBoundary where lon is changing
            if (lonBoundaryCurr != lonBoundaryPrev) {
                // find the step which crosses over desired lon
                if (((lon >= lonBoundaryPrev && lon <= lonBoundaryCurr) ||
                        (lon >= lonBoundaryCurr && lon <= lonBoundaryPrev))) {

                    // compute lon based on interpolation and add geoPos to intersectionList
                    final double interpolationWeight = (lon - lonBoundaryPrev) / (lonBoundaryCurr - lonBoundaryPrev);
                    if (interpolationWeight >= 0.0 && interpolationWeight < 1.0) {
                        final double lat = latBoundaryPrev + interpolationWeight * (latBoundaryCurr - latBoundaryPrev);
                        intersectionList.add(new GeoPos(lat, lon));
                    }
                }
            }

            lonBoundaryPrev = lonBoundaryCurr;
            latBoundaryPrev = latBoundaryCurr;
        }
    }


    private static GeneralPath[] createPaths(List<List<Coord>> parallelList, List<List<Coord>> meridianList) {
        final ArrayList<GeneralPath> generalPathList = new ArrayList<GeneralPath>();
        addToPath(parallelList, generalPathList);
        addToPath(meridianList, generalPathList);
        return generalPathList.toArray(new GeneralPath[0]);
    }

    private static void addToPath(List<List<Coord>> lineList, List<GeneralPath> generalPathList) {
        for (final List<Coord> coordList : lineList) {
            if (coordList.size() >= 2) {
                final GeneralPath generalPath = new GeneralPath();
                boolean restart = true;
                for (Coord coord : coordList) {
                    PixelPos pixelPos = coord.pixelPos;
                    if (pixelPos.isValid()) {
                        if (restart) {
                            generalPath.moveTo(pixelPos.x, pixelPos.y);
                        } else {
                            generalPath.lineTo(pixelPos.x, pixelPos.y);
                        }
                        restart = false;
                    } else {
                        restart = true;
                    }
                }
                generalPathList.add(generalPath);
            }
        }
    }

    private static TextGlyph[] createLonCornerTextGlyphs(RasterDataNode raster, boolean formatCompass, boolean formatDecimal) {
        final TextGlyph[] textGlyphs;
        textGlyphs = new TextGlyph[4];

        GeoCoding geoCoding = raster.getGeoCoding();

        PixelPos pixelPos1;
        PixelPos pixelPos2;

        if (geoCoding != null && raster.getRasterHeight() >= 2 && raster.getRasterWidth() >= 2) {
            pixelPos1 = new PixelPos(0, 0);
            pixelPos2 = new PixelPos(0, 1);
            textGlyphs[TOP_LEFT_CORNER_INDEX] = getLonCornerTextGlyph(geoCoding, pixelPos1, pixelPos2, formatCompass, formatDecimal);

            pixelPos1 = new PixelPos(raster.getRasterWidth(), 0);
            pixelPos2 = new PixelPos(raster.getRasterWidth(), 1);
            textGlyphs[TOP_RIGHT_CORNER_INDEX] = getLonCornerTextGlyph(geoCoding, pixelPos1, pixelPos2, formatCompass, formatDecimal);

            pixelPos1 = new PixelPos(raster.getRasterWidth(), raster.getRasterHeight());
            pixelPos2 = new PixelPos(raster.getRasterWidth(), raster.getRasterHeight() - 1);
            textGlyphs[BOTTOM_RIGHT_CORNER_INDEX] = getLonCornerTextGlyph(geoCoding, pixelPos1, pixelPos2, formatCompass, formatDecimal);

            pixelPos1 = new PixelPos(0, raster.getRasterHeight());
            pixelPos2 = new PixelPos(0, raster.getRasterHeight() - 1);
            textGlyphs[BOTTOM_LEFT_CORNER_INDEX] = getLonCornerTextGlyph(geoCoding, pixelPos1, pixelPos2, formatCompass, formatDecimal);
        }

        return textGlyphs;
    }

    private static TextGlyph[] createLatCornerTextGlyphs(RasterDataNode raster, boolean formatCompass, boolean formatDecimal) {
        final TextGlyph[] textGlyphs;
        textGlyphs = new TextGlyph[4];

        GeoCoding geoCoding = raster.getGeoCoding();

        PixelPos pixelPos1;
        PixelPos pixelPos2;

        if (geoCoding != null && raster.getRasterHeight() >= 2 && raster.getRasterWidth() >= 2) {
            pixelPos1 = new PixelPos(0, 0);
            pixelPos2 = new PixelPos(1, 0);
            textGlyphs[TOP_LEFT_CORNER_INDEX] = getLatCornerTextGlyph(geoCoding, pixelPos1, pixelPos2, formatCompass, formatDecimal);

            pixelPos1 = new PixelPos(raster.getRasterWidth(), 0);
            pixelPos2 = new PixelPos(raster.getRasterWidth() - 1, 0);
            textGlyphs[TOP_RIGHT_CORNER_INDEX] = getLatCornerTextGlyph(geoCoding, pixelPos1, pixelPos2, formatCompass, formatDecimal);

            pixelPos1 = new PixelPos(raster.getRasterWidth(), raster.getRasterHeight());
            pixelPos2 = new PixelPos(raster.getRasterWidth() - 1, raster.getRasterHeight());
            textGlyphs[BOTTOM_RIGHT_CORNER_INDEX] = getLatCornerTextGlyph(geoCoding, pixelPos1, pixelPos2, formatCompass, formatDecimal);

            pixelPos1 = new PixelPos(0, raster.getRasterHeight());
            pixelPos2 = new PixelPos(1, raster.getRasterHeight());
            textGlyphs[BOTTOM_LEFT_CORNER_INDEX] = getLatCornerTextGlyph(geoCoding, pixelPos1, pixelPos2, formatCompass, formatDecimal);
        }

        return textGlyphs;
    }

    private static PixelPos[] createTickPoints(List<List<Coord>> latitudeGridLinePoints,
                                               List<List<Coord>> longitudeGridLinePoints,
                                               TextLocation textLocation) {
        final List<PixelPos> pixelPoses = new ArrayList<>();

        switch (textLocation) {
            case NORTH:
                createNorthernLongitudeTickPoints(longitudeGridLinePoints, pixelPoses);
                break;
            case SOUTH:
                createSouthernLongitudeTickPoints(longitudeGridLinePoints, pixelPoses);
                break;
            case WEST:
                createWesternLatitudeTickPoints(latitudeGridLinePoints, pixelPoses);
                break;
            case EAST:
                createEasternLatitudeTickPoints(latitudeGridLinePoints, pixelPoses);
                break;
        }

        return pixelPoses.toArray(new PixelPos[0]);
    }


    private static TextGlyph[] createTextGlyphs(List<List<Coord>> latitudeGridLinePoints,
                                                List<List<Coord>> longitudeGridLinePoints,
                                                TextLocation textLocation, boolean formatCompass, boolean formatDecimal) {
        final List<TextGlyph> textGlyphs = new ArrayList<TextGlyph>();

        switch (textLocation) {
            case NORTH:
                createNorthernLongitudeTextGlyphs(longitudeGridLinePoints, textGlyphs, formatCompass, formatDecimal);
                break;
            case SOUTH:
                createSouthernLongitudeTextGlyphs(longitudeGridLinePoints, textGlyphs, formatCompass, formatDecimal);
                break;
            case WEST:
                createWesternLatitudeTextGlyphs(latitudeGridLinePoints, textGlyphs, formatCompass, formatDecimal);
                break;
            case EAST:
                createEasternLatitudeTextGlyphs(latitudeGridLinePoints, textGlyphs, formatCompass, formatDecimal);
                break;
        }

        return textGlyphs.toArray(new TextGlyph[0]);
    }

    private static void createWesternLatitudeTickPoints(List<List<Coord>> latitudeGridLinePoints,
                                                        List<PixelPos> pixelPoses) {

        for (final List<Coord> latitudeGridLinePoint : latitudeGridLinePoints) {

            if (latitudeGridLinePoint.size() >= 2) {
                int first = 0;
                Coord coord = latitudeGridLinePoint.get(first);

                if (coord.pixelPos.isValid()) {
                    pixelPoses.add(coord.pixelPos);
                }
            }
        }
    }

    private static void createWesternLatitudeTextGlyphs(List<List<Coord>> latitudeGridLinePoints,
                                                        List<TextGlyph> textGlyphs, boolean formatCompass, boolean formatDecimal) {

        // Assumes that the line was drawn from west to east
        // coord1 set to first point in order to anchor the text to the edge of the line
        for (final List<Coord> latitudeGridLinePoint : latitudeGridLinePoints) {

            if (latitudeGridLinePoint.size() >= 2) {

                int first = 0;
                int second = 1;

                Coord coord1 = latitudeGridLinePoint.get(first);
                Coord coord2 = latitudeGridLinePoint.get(second);

                PixelPos pixelPos2 = new PixelPos((float) (coord1.pixelPos.getX() + 1), (float) coord1.pixelPos.getY());
                coord2 = new Coord(coord1.geoPos, pixelPos2);

                if (isCoordPairValid(coord1, coord2)) {
                    TextGlyph textGlyph = createTextGlyph(coord1.geoPos.getLatString(formatCompass, formatDecimal), coord1, coord2);
                    textGlyphs.add(textGlyph);
                }
            }
        }
    }


    private static void createEasternLatitudeTickPoints(List<List<Coord>> latitudeGridLinePoints,
                                                        List<PixelPos> pixelPoses) {

        for (final List<Coord> latitudeGridLinePoint : latitudeGridLinePoints) {

            if (latitudeGridLinePoint.size() >= 2) {
                int last = latitudeGridLinePoint.size() - 1;
                Coord coord = latitudeGridLinePoint.get(last);

                if (coord.pixelPos.isValid()) {
                    pixelPoses.add(coord.pixelPos);
                }
            }
        }
    }

    private static void createEasternLatitudeTextGlyphs(List<List<Coord>> latitudeGridLinePoints,
                                                        List<TextGlyph> textGlyphs, boolean formatCompass, boolean formatDecimal) {

        // Assumes that the line was drawn from west to east
        // coord1 set to last point in order to anchor the text to the edge of the line
        // text will point backwards due to this so it will subsequently need to be rotated
        for (final List<Coord> latitudeGridLinePoint : latitudeGridLinePoints) {
            if (latitudeGridLinePoint.size() >= 2) {

                int last = latitudeGridLinePoint.size() - 1;
                int nextToLast = last - 1;

                Coord coord1 = latitudeGridLinePoint.get(last);
                Coord coord2 = latitudeGridLinePoint.get(nextToLast);

                PixelPos pixelPos2 = new PixelPos((float) (coord1.pixelPos.getX() - 1), (float) coord1.pixelPos.getY());
                coord2 = new Coord(coord1.geoPos, pixelPos2);

                if (isCoordPairValid(coord1, coord2)) {
                    TextGlyph textGlyph = createTextGlyph(coord1.geoPos.getLatString(formatCompass, formatDecimal), coord1, coord2);
                    textGlyphs.add(textGlyph);
                }
            }
        }
    }

    private static void createNorthernLongitudeTickPoints(List<List<Coord>> longitudeGridLinePoints,
                                                          List<PixelPos> pixelPoses) {

        for (final List<Coord> longitudeGridLinePoint : longitudeGridLinePoints) {

            if (longitudeGridLinePoint.size() >= 2) {
                int first = 0;
                Coord coord = longitudeGridLinePoint.get(first);

                if (coord.pixelPos.isValid()) {
                    pixelPoses.add(coord.pixelPos);
                }
            }
        }
    }

    private static void createNorthernLongitudeTextGlyphs(List<List<Coord>> longitudeGridLinePoints,
                                                          List<TextGlyph> textGlyphs, boolean formatCompass, boolean formatDecimal) {

        // Assumes that the line was drawn from north to south
        // coord1 set to first point in order to anchor the text to the edge of the line
        for (List<Coord> longitudeGridLinePoint : longitudeGridLinePoints) {

            if (longitudeGridLinePoint.size() >= 2) {
                int first = 0;
                int second = 1;

                Coord coord1 = longitudeGridLinePoint.get(first);
                Coord coord2 = longitudeGridLinePoint.get(second);

                PixelPos pixelPos2 = new PixelPos((float) (coord1.pixelPos.getX()), (float) (coord1.pixelPos.getY() + 1));
                coord2 = new Coord(coord1.geoPos, pixelPos2);


                if (isCoordPairValid(coord1, coord2)) {
                    TextGlyph textGlyph = createTextGlyph(coord1.geoPos.getLonString(formatCompass, formatDecimal), coord1, coord2);
                    textGlyphs.add(textGlyph);
                }
            }
        }
    }

    static void createSouthernLongitudeTickPoints(List<List<Coord>> longitudeGridLinePoints,
                                                  List<PixelPos> pixelPoses) {

        for (final List<Coord> longitudeGridLinePoint : longitudeGridLinePoints) {

            if (longitudeGridLinePoint.size() >= 2) {
                int last = longitudeGridLinePoint.size() - 1;
                Coord coord = longitudeGridLinePoint.get(last);

                if (coord.pixelPos.isValid()) {
                    pixelPoses.add(coord.pixelPos);
                }
            }
        }
    }


    static void createSouthernLongitudeTextGlyphs(List<List<Coord>> longitudeGridLinePoints,
                                                  List<TextGlyph> textGlyphs, boolean formatCompass, boolean formatDecimal) {

        // Assumes that the line was drawn from north to south
        // coord1 set to last point in order to anchor the text to the edge of the line
        // text will point upwards due to this so it may be subsequently rotated if desired
        for (List<Coord> longitudeGridLinePoint : longitudeGridLinePoints) {
            if (longitudeGridLinePoint.size() >= 2) {
                final int last = longitudeGridLinePoint.size() - 1;
                final Coord coord1 = longitudeGridLinePoint.get(last);

                final PixelPos pixelPos2 = new PixelPos((float) (coord1.pixelPos.getX()), (float) (coord1.pixelPos.getY() - 1));
                final Coord coord2 = new Coord(coord1.geoPos, pixelPos2);

                if (isCoordPairValid(coord1, coord2)) {
                    TextGlyph textGlyph = createTextGlyph(coord1.geoPos.getLonString(formatCompass, formatDecimal), coord1, coord2);
                    textGlyphs.add(textGlyph);
                }
            }
        }
    }

    static TextGlyph getLonCornerTextGlyph(GeoCoding geoCoding, PixelPos pixelPos1, PixelPos pixelPos2, boolean formatCompass, boolean formatDecimal) {
        if (geoCoding == null) {
            return null;
        }

        final GeoPos geoPos1 = geoCoding.getGeoPos(pixelPos1, null);
        final Coord coord1 = new Coord(geoPos1, pixelPos1);

        final GeoPos geoPos2 = geoCoding.getGeoPos(pixelPos2, null);
        final Coord coord2 = new Coord(geoPos2, pixelPos2);

        if (isCoordPairValid(coord1, coord2)) {
            return createTextGlyph(coord1.geoPos.getLonString(formatCompass, formatDecimal), coord1, coord2);
        }

        return null;
    }

    static TextGlyph getLatCornerTextGlyph(GeoCoding geoCoding, PixelPos pixelPos1, PixelPos pixelPos2, boolean formatCompass, boolean formatDecimal) {
        if (geoCoding == null) {
            return null;
        }

        final GeoPos geoPos1 = geoCoding.getGeoPos(pixelPos1, null);
        final Coord coord1 = new Coord(geoPos1, pixelPos1);

        final GeoPos geoPos2 = geoCoding.getGeoPos(pixelPos2, null);
        final Coord coord2 = new Coord(geoPos2, pixelPos2);

        if (isCoordPairValid(coord1, coord2)) {
            return createTextGlyph(coord1.geoPos.getLatString(formatCompass, formatDecimal), coord1, coord2);
        }

        return null;
    }

    static Range[] getRanges(GeoPos[] geoPositions) {
        double xMin = +1.0e10;
        double yMin = +1.0e10;
        double xMax = -1.0e10;
        double yMax = -1.0e10;
        for (GeoPos geoPos : geoPositions) {
            xMin = Math.min(xMin, geoPos.lon);
            yMin = Math.min(yMin, geoPos.lat);
            xMax = Math.max(xMax, geoPos.lon);
            yMax = Math.max(yMax, geoPos.lat);
        }

        final Range[] ranges = new Range[2];
        ranges[0] = new Range(xMin, xMax);
        ranges[1] = new Range(yMin, yMax);
        return ranges;
    }

    static GeoPos getGeoDelta(GeoCoding geoCoding, RasterDataNode dataNode) {
        final double posX = 0.5 * dataNode.getRasterWidth();
        final double posy = 0.5 * dataNode.getRasterHeight();

        final PixelPos pixelPos_1 = new PixelPos(posX, posy);
        final PixelPos pixelPos_2 = new PixelPos(posX + 1.0, posy + 1.0);

        final GeoPos geoPos_1 = geoCoding.getGeoPos(pixelPos_1, null);
        final GeoPos geoPos_2 = geoCoding.getGeoPos(pixelPos_2, null);

        final double deltaLat = Math.abs(geoPos_2.lat - geoPos_1.lat);
        double deltaLon = Math.abs(geoPos_2.lon - geoPos_1.lon);
        if (deltaLon > 180) {
            deltaLon = Math.abs(deltaLon - 360.0);
        }

        return new GeoPos(deltaLat, deltaLon);
    }

    static boolean isCoordPairValid(Coord coord1, Coord coord2) {
        // @todo 1 tb/tb check if it may also be necessary to verify the geo-locations 2020-03-26
        return coord1.pixelPos.isValid() && coord2.pixelPos.isValid();
    }

    static TextGlyph createTextGlyph(String text, Coord coord1, Coord coord2) {
        final double angle = Math.atan2(coord2.pixelPos.y - coord1.pixelPos.y,
                                        coord2.pixelPos.x - coord1.pixelPos.x);
        return new TextGlyph(text, coord1.pixelPos.x, coord1.pixelPos.y, angle);
    }

    static double limitLon(double lon) {
        while (lon < -180f) {
            lon += 360f;
        }
        while (lon > 180f) {
            lon -= 360f;
        }
        return lon;
    }

    public static class TextGlyph {

        private final String text;
        private final double x;
        private final double y;
        private final double angle;

        TextGlyph(String text, double x, double y, double angle) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.angle = angle;
        }

        public String getText() {
            return text;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getAngle() {
            return angle;
        }
    }

    static class Coord {
        GeoPos geoPos;
        PixelPos pixelPos;

        public Coord(GeoPos geoPos, PixelPos pixelPos) {
            this.geoPos = geoPos;
            this.pixelPos = pixelPos;
        }
    }

    static class GeoPosLatComparator extends GeoPosComparator {
        @Override
        public int compare(GeoPos geoPos1, GeoPos geoPos2) {
            return getCompare(geoPos1.lat - geoPos2.lat);
        }
    }

    static class GeoPosLonComparator extends GeoPosComparator {
        @Override
        public int compare(GeoPos geoPos1, GeoPos geoPos2) {
            return getCompare(geoPos1.lon - geoPos2.lon);
        }
    }

    abstract static class GeoPosComparator implements Comparator<GeoPos> {

        int getCompare(double delta) {
            if (delta < 0f) {
                return -1;
            } else if (delta > 0f) {
                return 1;
            } else {
                return 0;
            }
        }
    }

//    private static double[] normalize(double x, double[] result) {
//        final double exponent = (x == 0.0) ? 0.0 : Math.ceil(Math.log(Math.abs(x)) / Math.log(10.0));
//        final double mantissa = (x == 0.0) ? 0.0 : x / Math.pow(10.0, exponent);
//        if (result == null) {
//            result = new double[2];
//        }
//        result[0] = mantissa;
//        result[1] = exponent;
//        return result;
//    }
//
//    private static double compose(final double[] components) {
//        final double mantissa = components[0];
//        final double exponent = components[1];
//        final double mantissaRounded;
//        if (mantissa < 0.15) {
//            mantissaRounded = 0.1;
//        } else if (mantissa < 0.225) {
//            mantissaRounded = 0.2;
//        } else if (mantissa < 0.375) {
//            mantissaRounded = 0.25;
//        } else if (mantissa < 0.75) {
//            mantissaRounded = 0.5;
//        } else {
//            mantissaRounded = 1.0;
//        }
//        return mantissaRounded * Math.pow(10.0, exponent);
//    }

//    /**
//     * Not used, but useful for debugging: DON'T delete this method!
//     *
//     * @param geoCoding   The geo-coding
//     * @param geoBoundary The geo-boundary
//     * @return the geo-boundary
//     */
//    @SuppressWarnings({"UnusedDeclaration"})
//    private static GeneralPath createPixelBoundaryPath(final GeoCoding geoCoding, final GeoPos[] geoBoundary) {
//        final GeneralPath generalPath = new GeneralPath();
//        boolean restart = true;
//        for (final GeoPos geoPos : geoBoundary) {
//            geoPos.lon = limitLon(geoPos.lon);
//            final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, null);
//            if (pixelPos.isValid()) {
//                if (restart) {
//                    generalPath.moveTo(pixelPos.x, pixelPos.y);
//                } else {
//                    generalPath.lineTo(pixelPos.x, pixelPos.y);
//                }
//                restart = false;
//            } else {
//                restart = true;
//            }
//        }
//        return generalPath;
//    }

    //    // please see the human readable version: computeParallelIntersections
//    private static void computeParallelIntersections(final GeoPos[] geoBoundary,
//                                                     final double my,
//                                                     final List<GeoPos> intersectionList) {
//        double p0x = 0, p0y = 0;
//        double p1x, p1y;
//        double pa;
//        double mx;
//        for (int i = 0; i < geoBoundary.length; i++) {
//            GeoPos geoPos = geoBoundary[i];
//            p1x = geoPos.lon;
//            p1y = geoPos.lat;
//            if (i > 0) {
//                if (((my >= p0y && my <= p1y) || (my >= p1y && my <= p0y)) &&
//                        (p1y - p0y != 0.0)) {
//                    pa = (my - p0y) / (p1y - p0y);
//                    if (pa >= 0.0 && pa < 1.0) {
//                        mx = p0x + pa * (p1x - p0x);
//                        intersectionList.add(new GeoPos(my, mx));
//                    }
//                }
//            }
//            p0x = p1x;
//            p0y = p1y;
//        }
//    }
    /**
     * Creates a graticule for the given product.
     *
     * @param product              the product
     * @param autoDeterminingSteps if true, <code>gridCellSize</code> is used to compute <code>latMajorStep</code>, <code>lonMajorStep</code> for the given product
     * @param gridCellSize         the grid cell size in pixels, ignored if <code>autoDeterminingSteps</code> if false
     * @param latMajorStep         the grid cell size in meridional direction, ignored if <code>autoDeterminingSteps</code> if true
     * @param lonMajorStep         the grid cell size in parallel direction, ignored if <code>autoDeterminingSteps</code> if true
     * @return the graticule or null, if it could not be created
     */
//    public static Graticule create(Product product,
//                                   boolean autoDeterminingSteps,
//                                   int gridCellSize,
//                                   float latMajorStep,
//                                   float lonMajorStep) {
//        Guardian.assertNotNull("product", product);
//        final GeoCoding geoCoding = product.getGeoCoding();
//        if (geoCoding == null || product.getSceneRasterWidth() < 16 || product.getSceneRasterHeight() < 16) {
//            return null;
//        }
//
//        if (autoDeterminingSteps) {
//            final PixelPos pixelPos1 = new PixelPos(0.5f * product.getSceneRasterWidth(), 0.5f * product.getSceneRasterHeight());
//            final PixelPos pixelPos2 = new PixelPos(pixelPos1.x + 1f, pixelPos1.y + 1f);
//            final GeoPos geoPos1 = geoCoding.getGeoPos(pixelPos1, null);
//            final GeoPos geoPos2 = geoCoding.getGeoPos(pixelPos2, null);
//            double deltaLat = Math.abs(geoPos2.lat - geoPos1.lat);
//            double deltaLon = Math.abs(geoPos2.lon - geoPos1.lon);
//            if (deltaLon > 180) {
//                deltaLon += 360;
//            }
//// todo Danny adding new code for the raster version of this below but only in part here
//            // is this code being used?
//            //
//
//            int height = product.getSceneRasterHeight();
//            int width = product.getSceneRasterWidth();
//            int min = width;
//
//            if (height < min) {
//                min = height;
//            }
//
//            double ratio = min / 4.0;
//            gridCellSize = (int) Math.floor(ratio);
//            Debug.trace("Graticule.create: deltaLat=" + deltaLat + ", deltaLon=" + deltaLon);
//            latMajorStep = (float) compose(normalize(gridCellSize * 0.5 * (deltaLon + deltaLat), null));
//            lonMajorStep = latMajorStep;
//        }
//        Debug.trace("Graticule.create: latMajorStep=" + latMajorStep + ", lonMajorStep=" + lonMajorStep);
//
//        float latMinorStep = latMajorStep / 4.0f;
//        float lonMinorStep = lonMajorStep / 4.0f;
//
//        int geoBoundaryStep = getGeoBoundaryStep(geoCoding);
//        Debug.trace("Graticule.create: geoBoundaryStep=" + geoBoundaryStep);
//        final GeoPos[] geoBoundary = ProductUtils.createGeoBoundary(product, null, geoBoundaryStep);
//        ProductUtils.normalizeGeoPolygon(geoBoundary);
//
//// nf Debugging, don't delete!
////        GeneralPath generalPath = createPixelBoundaryPath(geoCoding, geoBoundary);
////        if (generalPath != null) {
////            return new Graticule(new GeneralPath[]{generalPath}, null);
////        }
//
//        double xMin = +1.0e10;
//        double yMin = +1.0e10;
//        double xMax = -1.0e10;
//        double yMax = -1.0e10;
//        for (GeoPos geoPos : geoBoundary) {
//            xMin = Math.min(xMin, geoPos.lon);
//            yMin = Math.min(yMin, geoPos.lat);
//            xMax = Math.max(xMax, geoPos.lon);
//            yMax = Math.max(yMax, geoPos.lat);
//        }
//
//
//        final List<List<Coord>> parallelList = computeParallelList(product.getGeoCoding(), geoBoundary, latMajorStep, lonMinorStep, yMin, yMax);
//        final List<List<Coord>> meridianList = computeMeridianList(product.getGeoCoding(), geoBoundary, lonMajorStep, latMinorStep, xMin, xMax);
//        final GeneralPath[] paths = createPaths(parallelList, meridianList);
//
//
//        final TextGlyph[] textGlyphsNorth = createTextGlyphs(parallelList, meridianList, TextLocation.NORTH, null, false, false);
//        final TextGlyph[] textGlyphsSouth = createTextGlyphs(parallelList, meridianList, TextLocation.SOUTH, null, false, false);
//        final TextGlyph[] textGlyphsWest = createTextGlyphs(parallelList, meridianList, TextLocation.WEST, null, false, false);
//        final TextGlyph[] textGlyphsEast = createTextGlyphs(parallelList, meridianList, TextLocation.EAST, null, false, false);
//
//
//        return new Graticule(paths, textGlyphsNorth, textGlyphsSouth, textGlyphsWest, textGlyphsEast, textGlyphsLatCorners, textGlyphsLonCorners);
//
//    }

    //    // please see the human readable version: computeMeridianIntersections
//    private static void computeMeridianIntersections(final GeoPos[] geoBoundary,
//                                                     final double mx,
//                                                     final List<GeoPos> intersectionList) {
//        double p0x = 0, p0y = 0;
//        double p1x, p1y;
//        double pa;
//        double my;
//        for (int i = 0; i < geoBoundary.length; i++) {
//            GeoPos geoPos = geoBoundary[i];
//            p1x = geoPos.lon;
//            p1y = geoPos.lat;
//            if (i > 0) {
//                if (((mx >= p0x && mx <= p1x) || (mx >= p1x && mx <= p0x)) &&
//                        (p1x - p0x != 0.0)) {
//                    pa = (mx - p0x) / (p1x - p0x);
//                    if (pa >= 0.0 && pa < 1.0) {
//                        my = p0y + pa * (p1y - p0y);
//                        intersectionList.add(new GeoPos(my, mx));
//                    }
//                }
//            }
//            p0x = p1x;
//            p0y = p1y;
//        }
//    }

}

