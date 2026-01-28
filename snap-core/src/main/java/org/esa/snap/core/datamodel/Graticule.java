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


    // todo
    // Support level-2 files regarding bow-tie effect
    // todo items
    // support level2 MODIS files by having a bowTieSecondSample line (similar and in addition to the farther off second line 
    //      but is distinguished by nearby proximity and conjunction with being in the same direction as the nearby line.
    //      only test for this if GeoSceneInfo indicates presence of nearby repeated lat/lon pixels
    
    // add checkbox for whether to use auto-labels or to use a custom textfield label list
    //       populate the list with the defaults when the checkbox is toggled "ON"
    // write a method to determine precise pixel of north/south poles
    // write a method to determine if projection is equidistant cylindrical  (check if edge and center lines have same geocding,
    //      both in vertical and horizonal direction)
    //      for equidistant cylidrical the smoothingCount can be reduced

    // AQUA_MODIS.2002070420231001.L3m.CU_9km.wink2.nc has meridian issues with smoothing steps low

    // AQUA_MODIS.2002070420231001.L3m.CU_9km.wink2.nc has issues with tolerance high (for example tolarance=100)

    // set north, south, east, west tolerance individually
    //      Edge Tolerance (North)
    //      Edge Tolerance (South)
    //      Edge Tolerance (West)
    //      Edge Tolerance (East)

    // enable meridian line and parallel lines individually
    //      Show Gridlines (Meridians)
    //      Show Gridlines (Parallels)

    // set smoothing steps for parallel and meridian lines individually
    //      Smoothing Steps (Meridians)
    //      Smoothing Steps (Parallels)

    // user configured lat and lon label list
    //      Meridian Spacing (Longitudes)
    //      Parallel Spacing (Latitudes)
    //      0 = auto
    //      number = spacing
    //      list = specific locations

    // lon lines may not reach edge for certain scenes when smoothing set low.

    // pole crossing determination needs possible improvements

    // variable name such PROPERTY_NAME change to PROPERTY_KEY


public class Graticule {

    private static final double ONE_MINUTE = 1.0 / 60.0;
    private static final double TEN_MINUTES = 10.0 / 60.0;

    private final GeneralPath[] _meridiansLinePaths;
    private final GeneralPath[] _parallelsLinePaths;
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
    private final boolean _flippedLons;
    private final boolean _flippedLats;

    private static final double NULL_LON = -99999;
    private static final double NULL_LAT = -99999;

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

    public static enum DIRECTION {
        NOT_SET,
        ASCENDING,
        DESCENDING
    }

    public static int TOP_LEFT_CORNER_INDEX = 0;
    public static int TOP_RIGHT_CORNER_INDEX = 1;
    public static int BOTTOM_RIGHT_CORNER_INDEX = 2;
    public static int BOTTOM_LEFT_CORNER_INDEX = 3;

    private Graticule(GeneralPath[] meridiansLinePaths,
                      GeneralPath[] parallelsLinePaths,
                      TextGlyph[] textGlyphsNorth,
                      TextGlyph[] textGlyphsSouth,
                      TextGlyph[] textGlyphsWest,
                      TextGlyph[] textGlyphsEast,
                      TextGlyph[] textGlyphsLatCorners,
                      TextGlyph[] textGlyphsLonCorners,
                      PixelPos[] tickPointsNorth,
                      PixelPos[] tickPointsSouth,
                      PixelPos[] tickPointsWest,
                      PixelPos[] tickPointsEast,
                      boolean flippedLats,
                      boolean flippedLons
    ) {
        _meridiansLinePaths = meridiansLinePaths;
        _parallelsLinePaths = parallelsLinePaths;
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
        _flippedLats = flippedLats;
        _flippedLons = flippedLons;
    }


    public GeneralPath[] getMeridiansLinePaths() {
        return _meridiansLinePaths;
    }

    public GeneralPath[] getParallelsLinePaths() {
        return _parallelsLinePaths;
    }

    public boolean isFlippedLats() {
        return _flippedLats;
    }

    public boolean isFlippedLons() {
        return _flippedLons;
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
                                   int desiredMinorSteps,
                                   double latMajorStep,
                                   double lonMajorStep,
                                   boolean interpolate,
                                   double tolerance,
                                   boolean formatCompass,
                                   boolean decimalFormat,
                                   int spacer) {
        Guardian.assertNotNull("product", raster);
        final GeoCoding geoCoding = raster.getGeoCoding();
        if (geoCoding == null || raster.getRasterWidth() < 16 || raster.getRasterHeight() < 16) {
            return null;
        }

        if (desiredNumGridLines <= 1) {
            desiredNumGridLines = 2;
        }


//        final GeoPos geoDelta = getGeoDelta(geoCoding, raster);
        final GeoSpan geoSpan = getGeoSpanScene(geoCoding, raster);

        boolean autoBoth = (latMajorStep == 0 && lonMajorStep == 0) ? true : false;
        if (latMajorStep == 0) {
            double tmpLatMajorStep =  geoSpan.latSpan / desiredNumGridLines;

            latMajorStep = getSensibleDegreeIncrement(tmpLatMajorStep);
        }

        if (lonMajorStep == 0) {
            double tmpLonMajorStep = geoSpan.lonSpan / desiredNumGridLines;

            lonMajorStep = getSensibleDegreeIncrement(tmpLonMajorStep);
        }

        if (autoBoth) {
            latMajorStep = Math.min(latMajorStep, lonMajorStep);
            lonMajorStep = latMajorStep;
        }

//        final int desiredMinorSteps = getDesiredMinorSteps(raster);
//        final double ratioLatMinor = raster.getRasterHeight() / (desiredMinorSteps - 1);
//        double latMinorStep = ratioLatMinor * geoDelta.lat;
//        final double ratioLonMinor = raster.getRasterHeight() / (desiredMinorSteps - 1);
//        double lonMinorStep = ratioLonMinor * geoDelta.lon;

        double latMinorStep = latMajorStep / desiredMinorSteps;
        double lonMinorStep = lonMajorStep / desiredMinorSteps;

//        int geoBoundaryStep = getGeoBoundaryStep(geoCoding, raster);
//        GeoPos[] geoBoundary = createGeoBoundary(raster, geoBoundaryStep);

// nf Debugging, don't delete!
//        GeneralPath generalPath = createPixelBoundaryPath(geoCoding, geoBoundary);
//        if (generalPath != null) {
//            return new Graticule(new GeneralPath[]{generalPath}, null);
//        }

        // todo fill in min and max based on geoSpan
//        final Range[] ranges = getRanges(geoBoundary);
//        final Range lonRange = ranges[0];
//        final Range latRange = ranges[1];

//        System.out.println("MinX=" + lonRange.getMin());
//        System.out.println("MaxX=" + lonRange.getMax());
//        System.out.println("MinY=" + latRange.getMin());
//        System.out.println("MaxY=" + latRange.getMax());



        if (geoSpan.southPoleCrossed || geoSpan.northPoleCrossed) {
            if (desiredMinorSteps < 256) {
                desiredMinorSteps = 256;
            }
            tolerance = 0;
        }

        if (geoSpan.equidistantCylindrical) {
            // todo should this be 1?
            // todo also add option in GUI to enforce/disable this?
            desiredMinorSteps = 4;
        }

        final List<List<Coord>> meridiansList = computeMeridiansList(lonMajorStep, desiredMinorSteps, raster, geoSpan, tolerance, interpolate);
        final List<List<Coord>> parallelsList = computeParallelsList(latMajorStep, desiredMinorSteps, raster, geoSpan, tolerance, interpolate);



//        final List<List<Coord>> meridianList = computeMeridianList(raster.getGeoCoding(), null, lonMajorStep, latMinorStep,
//                0.0, 0.0, raster);
//        final List<List<Coord>> parallelList = computeParallelList(raster.getGeoCoding(), null, latMajorStep, lonMinorStep,
//                0.0, 0.0, raster);


        // todo maybe make this ||
        if (parallelsList.size() > 0 || meridiansList.size() > 0) {
            final GeneralPath[] meridiansPaths = createPathsNew(meridiansList);
            final GeneralPath[] parallelsPaths = createPathsNew(parallelsList);

            final TextGlyph[] textGlyphsNorth = createTextGlyphs(parallelsList, meridiansList, TextLocation.NORTH, formatCompass, decimalFormat, lonMajorStep, raster, spacer, geoSpan);
            final TextGlyph[] textGlyphsSouth = createTextGlyphs(parallelsList, meridiansList, TextLocation.SOUTH, formatCompass, decimalFormat, lonMajorStep, raster, spacer, geoSpan);
            final TextGlyph[] textGlyphsWest = createTextGlyphs(parallelsList, meridiansList, TextLocation.WEST, formatCompass, decimalFormat, latMajorStep, raster, spacer, geoSpan);
            final TextGlyph[] textGlyphsEast = createTextGlyphs(parallelsList, meridiansList, TextLocation.EAST, formatCompass, decimalFormat,latMajorStep, raster, spacer, geoSpan);

            final TextGlyph[] textGlyphsLatCorners = createLatCornerTextGlyphs(raster, formatCompass, decimalFormat);
            final TextGlyph[] textGlyphsLonCorners = createLonCornerTextGlyphs(raster, formatCompass, decimalFormat);

            final PixelPos[] tickPointsNorth = createTickPoints(parallelsList, meridiansList, TextLocation.NORTH);
            final PixelPos[] tickPointsSouth = createTickPoints(parallelsList, meridiansList, TextLocation.SOUTH);
            final PixelPos[] tickPointsWest = createTickPoints(parallelsList, meridiansList, TextLocation.WEST);
            final PixelPos[] tickPointsEast = createTickPoints(parallelsList, meridiansList, TextLocation.EAST);

            final boolean flippedLats = geoSpan.latDescending && !geoSpan.latAscending;
            final boolean flippedLons = geoSpan.lonDescending && !geoSpan.lonAscending;

            return new Graticule(meridiansPaths,
                    parallelsPaths,
                    textGlyphsNorth,
                    textGlyphsSouth,
                    textGlyphsWest,
                    textGlyphsEast,
                    textGlyphsLatCorners,
                    textGlyphsLonCorners,
                    tickPointsNorth,
                    tickPointsSouth,
                    tickPointsWest,
                    tickPointsEast,
                    flippedLats,
                    flippedLons);
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
                    null,
                    null,
                    false,
                    false);
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

        if (step > 1) {
            step = 1;
        }
        return step;
    }

    private static List<List<Coord>> computeParallelListOld(final GeoCoding geoCoding,
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


    private static List<List<Coord>> computeParallelsList(final double latMajorStep,
                                                          double minorSteps,
                                                          RasterDataNode raster,
                                                          GeoSpan geoSpan,
                                                          double tolerance,
                                                          boolean interpolate) {

        List<List<Coord>> parallelsList = new ArrayList<>();

        double pixelX;
        double prevPixelX = -1;


        // todo decide whether this should be restricted
        double maxSteps = Math.floor((raster.getRasterWidth() - 1) / 4.0);
        if (minorSteps > maxSteps) {
            minorSteps = maxSteps;
        }


        int PARALLELS_COUNT_MAX = 200;   // just in case default is bad or too tight spacing

        // Get a list of latitudes to attempt to use as the gridlines.
        ArrayList<Double> parellelsLatsArrayList = getParallelsLatsArrayList(geoSpan, latMajorStep, PARALLELS_COUNT_MAX);

        int parallelsCount = 0;


        for (double parallelLat : parellelsLatsArrayList) {

            List<Coord> parallel1 = new ArrayList<>();
            List<Coord> parallel2 = new ArrayList<>();

            Coord[] coords;

            boolean found = false;
            boolean finished = false;
            double prevPixel = -1;

            for (double step = 0; step <= minorSteps; step += 1.0) {
                pixelX = (int) Math.floor((raster.getRasterWidth() - 1) * step / minorSteps);
                if (pixelX != prevPixelX) {

                    coords = getCoordParallel(parallelLat, pixelX, raster, tolerance, interpolate);
                    if (coords != null) {
                        if (coords[0] != null) {

                            if (!found) {
                                // need to look back for actual intersection
                                if (pixelX > 0) {
                                    for (double innerPixel = prevPixel; innerPixel < pixelX; innerPixel += 1.0) {
                                        Coord[] coordsInner = getCoordParallel(parallelLat, innerPixel, raster, tolerance, interpolate);
                                        if (coordsInner != null) {
                                            if (coordsInner[0] != null) {
                                                parallel1.add(coordsInner[0]);
                                                break;
                                            }
                                        }
                                    }
                                }
                                found = true;
                            }
                            parallel1.add(coords[0]);
                        } else {
                            if (found && !finished) {
                                // need to look back for actual intersection
                                if (pixelX <= (raster.getRasterWidth() - 1)) {
                                    for (double innerPixel = pixelX; innerPixel > prevPixelX; innerPixel -= 1.0) {
                                        Coord[] coordsInner = getCoordParallel(parallelLat, innerPixel, raster, tolerance, interpolate);
                                        if (coordsInner != null) {
                                            if (coordsInner[0] != null) {
                                                parallel1.add(coordsInner[0]);
                                                break;
                                            }
                                        }
                                    }
                                }

                                finished = true;
                            }
                        }
                        if (coords[1] != null) {
                            parallel2.add(coords[1]);
                        }
                    }
                }

                prevPixelX = pixelX;
            }

            if (parallel1.size() > 0) {
                parallelsList.add(parallel1);
            }

            if (parallel2.size() > 0) {
                parallelsList.add(parallel2);
            }

            parallelsCount++;
            if (parallelsCount > PARALLELS_COUNT_MAX) {
                break;
            }
        }


        return parallelsList;
    }




    /**
     * Creates a list of longitudes to attempt to use as the gridlines.
     *
     * @param geoSpan geoSpan of the scene
     * @param lonMajorStep the grid cell size in longitudinal direction
     * @param PARALLELS_COUNT_MAX maximum size of list
     * @return list of latitudes to attempt to use as the gridlines.
     */
    private  static ArrayList<Double> getMeridiansLonsArrayList(GeoSpan geoSpan,
                                                                double lonMajorStep,
                                                                int PARALLELS_COUNT_MAX) {

        ArrayList<Double> meridiansLonsArrayList = new ArrayList<Double>();


        boolean forceFullEarth = false;

        double min;
        double max;

        if (geoSpan.lonSpan >= 90) {
            forceFullEarth = true;
            min = -180;
            max = 180;
        } else {
            if (geoSpan.lonAscending && !geoSpan.lonDescending) {
                min = Math.floor(geoSpan.firstLon);
                max = Math.ceil(geoSpan.lastLon);
            } else if (geoSpan.lonDescending && !geoSpan.lonAscending) {
                min = Math.floor(geoSpan.lastLon);
                max = Math.ceil(geoSpan.firstLon);
            } else {
                forceFullEarth = true;
                min = -180;
                max = 180;
            }
        }




        // increase min and max to add a slight buffer to be able to potentially add lines just outside the actual scene area.
        min = min - lonMajorStep;
        max = max + lonMajorStep;

        // restrict min to a valid value
        if (min < -180) {
            min = -180;
        }

        // restrict min to a valid value
        if (max > 180) {
            max = 180;
        }



        int parallelsAddedCount = 0;

        if (geoSpan.datelineCrossed && !forceFullEarth) {

            // East of crossing
            for (double meridianLon = -180 + lonMajorStep; meridianLon <= max; meridianLon += lonMajorStep) {
                if (parallelsAddedCount <= PARALLELS_COUNT_MAX) {
                    meridiansLonsArrayList.add(meridianLon);
                    parallelsAddedCount++;
                }
            }


            // West of crossing
            for (double meridianLon = 180; meridianLon >= min; meridianLon -= lonMajorStep) {
                if (parallelsAddedCount <= PARALLELS_COUNT_MAX) {
                    meridiansLonsArrayList.add(meridianLon);
                    parallelsAddedCount++;
                }
            }


        } else {
            // Eastern hemisphere
            if (max >= 0) {
                for (double meridianLon = 0; meridianLon <= max; meridianLon += lonMajorStep) {
                    if (meridianLon >= min) {
                        if (parallelsAddedCount <= PARALLELS_COUNT_MAX) {
                            meridiansLonsArrayList.add(meridianLon);
                            parallelsAddedCount++;
                        }
                    }
                }
            }


            // Western hemisphere
            if (min < 0) {
                for (double meridianLon = 0 - lonMajorStep; meridianLon >= min; meridianLon -= lonMajorStep) {
                    if (meridianLon <= max) {
                        if (parallelsAddedCount <= PARALLELS_COUNT_MAX) {
                            meridiansLonsArrayList.add(meridianLon);
                            parallelsAddedCount++;
                        }
                    }
                }
            }


        }






//        ArrayList<Double> meridianLonsArrayList = new ArrayList<Double>();
//        if (geoSpan.datelineCrossed) {
//
//            for (double meridianLon = 180; meridianLon >= min; meridianLon -= lonMajorStep) {
//                meridianLonsArrayList.add(meridianLon);
//            }
//
//
//            for (double meridianLon = -180 + lonMajorStep; meridianLon <= max; meridianLon += lonMajorStep) {
//                meridianLonsArrayList.add(meridianLon);
//            }
//        } else {
//            for (double meridianLon = min; meridianLon <= max; meridianLon += lonMajorStep) {
//                meridianLonsArrayList.add(meridianLon);
//            }
//        }
//
//        // todo temp testing
//        meridianLonsArrayList.add(-70.0);
//        meridianLonsArrayList.add(-75.0);
//        meridianLonsArrayList.add(-80.0);
//        meridianLonsArrayList.add(-85.0);
//        meridianLonsArrayList.add(-90.0);
//        meridianLonsArrayList.add(-95.0);



        return meridiansLonsArrayList;

    }
    /**
     * Creates a list of latitudes to attempt to use as the gridlines.
     *
     * @param geoSpan geoSpan of the scene
     * @param latMajorStep the grid cell size in latitudinal direction
     * @param PARALLELS_COUNT_MAX maximum size of list
     * @return list of latitudes to attempt to use as the gridlines.
     */
    private  static ArrayList<Double> getParallelsLatsArrayList(GeoSpan geoSpan,
                                                                double latMajorStep,
                                                                int PARALLELS_COUNT_MAX) {

        ArrayList<Double> parellelsLatsArrayList = new ArrayList<Double>();

        boolean lat90Found = false;
        boolean latNeg90Found = false;
        double min;
        double max;
        int parallelsAddedCount = 0;

        boolean forceFullEarth = false;

        if (geoSpan.latSpan >= 90 && forceFullEarth) {
            min = -90;
            max = 90;
        } else {
            // increase min and max to add a slight buffer to be able to potentially add lines just outside the actual scene area.
            min = geoSpan.minLat - latMajorStep;
            max = geoSpan.maxLat + latMajorStep;

            // restrict min to a valid value
            if (min < -90) {
                min = -90;
            }

            // restrict max to a valid value
            if (max > 90) {
                max = 90;
            }
        }


        if (geoSpan.northPoleCrossed) {
            // force the polar lats to be included in case the actual data slightly missed hitting the pole
            // if close to north pole then
            if (max > 80) {
                max = 90;
            }
        } else if (geoSpan.southPoleCrossed) {
            // force the polar lats to be included in case the actual data slightly missed hitting the pole
            // if close to south pole then
            if (min < -80) {
                min = -90;
            }
        }




        // northern hemisphere
        if (max >= 0) {
            for (double parallelLat = 0; parallelLat <= max; parallelLat += latMajorStep) {
                if (parallelLat >= min) {
                    if (parallelsAddedCount <= PARALLELS_COUNT_MAX) {
                        parellelsLatsArrayList.add(parallelLat);
                        parallelsAddedCount++;
                    }

                    if (parallelLat == 90) {
                        lat90Found = true;
                    }
                }
            }
        }

        // todo this may be optional
        if (max == 90 && !lat90Found) {
            parellelsLatsArrayList.add(90.0);
        }

        // southern hemisphere
        if (min < 0) {
            for (double parallelLat = 0 - latMajorStep; parallelLat >= min; parallelLat -= latMajorStep) {
                if (parallelLat <= max) {
                    if (parallelsAddedCount <= PARALLELS_COUNT_MAX) {
                        parellelsLatsArrayList.add(parallelLat);
                        parallelsAddedCount++;
                    }

                    if (parallelLat == -90) {
                        latNeg90Found = true;
                    }
                }
            }
        }

        // todo this may be optional
        if (min == -90 && !latNeg90Found) {
            parellelsLatsArrayList.add(-90.0);
        }


        return parellelsLatsArrayList;

    }




    private static List<List<Coord>> computeMeridiansList(final double lonMajorStep,
                                                          double minorSteps,
                                                          RasterDataNode raster,
                                                          GeoSpan geoSpan,
                                                          double tolerance,
                                                          boolean interpolate) {

        List<List<Coord>> meridiansList = new ArrayList<>();

        double pixelY;
        double prevPixelY = -1;

        double maxSteps = Math.floor((raster.getRasterHeight() - 1) / 5.0);
        if (minorSteps > maxSteps) {
            minorSteps = maxSteps;
        }


        int MERIDIANS_COUNT_MAX = 200;  // just in case default is bad or too tight spacing
        // Get a list of longitudes to attempt to use as the gridlines.
        ArrayList<Double> meridianLonsArrayList = getMeridiansLonsArrayList(geoSpan, lonMajorStep, MERIDIANS_COUNT_MAX);


//
//        double min = Math.floor(geoSpan.firstLon);
//        double max = Math.ceil(geoSpan.lastLon);
//
//
//        boolean forceFullEarth = false;
//
//        if (geoSpan.lonSpan >= 90 && forceFullEarth) {
//            // big enough so use whole earth
//            min = -180;
//            max = 180;
//        } else if (geoSpan.lonSpan >= 1) {
//
//            double minTmp;
//            double maxTmp;
//
//            if (lonMajorStep >= 1) {
//                // in this case: check for every lon across the full earth
//                minTmp = -180;
//                maxTmp = 180;
//            } else {
//                //  in this case: limit check to the lon range of the scene
//                minTmp = Math.floor(geoSpan.firstLon);
//                maxTmp = Math.ceil(geoSpan.lastLon);
//            }

//
//            for (double i = minTmp; i <= maxTmp; i += lonMajorStep) {
//                if (i >= (geoSpan.firstLon)) {
//                    min = i - lonMajorStep;
//                    if (min < -180) {
//                        min = -180;
//                    }
//                    break;
//                }
//            }
//
//            for (double i = minTmp; i <= maxTmp; i += lonMajorStep) {
//                if (i >= (geoSpan.lastLon)) {
//                    max = i + lonMajorStep;
//                    if (max > 180) {
//                        max = 180;
//                    }
//                    break;
//                }
//            }
//
//        }
//
//
//        ArrayList<Double> meridianLonsArrayList = new ArrayList<Double>();
//        if (geoSpan.datelineCrossed) {
//
//            for (double meridianLon = 180; meridianLon >= min; meridianLon -= lonMajorStep) {
//                meridianLonsArrayList.add(meridianLon);
//            }
//
////
////
////            boolean mx180Found = false;
////            for (double mx = min; mx <= 180; mx += lonMajorStep) {
////                meridianLonsArrayList.add(mx);
////
////                if (mx == 180) {
////                    mx180Found = true;
////                }
////            }
////
////            if (!mx180Found) {
////                meridianLonsArrayList.add(180.0);
////            }
//
//            for (double meridianLon = -180 + lonMajorStep; meridianLon <= max; meridianLon += lonMajorStep) {
//                meridianLonsArrayList.add(meridianLon);
//            }
//        } else {
//            for (double meridianLon = min; meridianLon <= max; meridianLon += lonMajorStep) {
//                meridianLonsArrayList.add(meridianLon);
//            }

//        }
//
//        // todo temp testing
//        meridianLonsArrayList.add(-70.0);
//        meridianLonsArrayList.add(-75.0);
//        meridianLonsArrayList.add(-80.0);
//        meridianLonsArrayList.add(-85.0);
//        meridianLonsArrayList.add(-90.0);
//        meridianLonsArrayList.add(-95.0);


        // loop through each desired meridian lon
        int meridiansCount = 0;
        for (double meridianLon : meridianLonsArrayList) {

            List<Coord> meridian1 = new ArrayList<>();
            List<Coord> meridian2 = new ArrayList<>();

            Coord[] coords;

            for (double step = 0; step <= minorSteps; step += 1.0) {

                pixelY = (int) Math.floor((raster.getRasterHeight() - 1) * step / minorSteps);
                if (pixelY != prevPixelY) {
                    coords = getCoordMeridian(meridianLon, pixelY, raster, tolerance, interpolate);
                    if (coords != null) {
                        if (coords[0] != null) {
                            meridian1.add(coords[0]);
                        }
                        if (coords[1] != null) {
                            meridian2.add(coords[1]);
                        }
                    }
                }

                prevPixelY = pixelY;
            }

            if (meridian1.size() > 0) {
                meridiansList.add(meridian1);
            }

            if (meridian2.size() > 0) {
                meridiansList.add(meridian2);
            }

            meridiansCount++;
            if (meridiansCount > MERIDIANS_COUNT_MAX) {
                break;
            }

        }


        return meridiansList;
    }


    static Coord[] getCoordMeridian(double meridianLon,
                                    double pixelY,
                                    RasterDataNode raster,
                                    double tolerance,
                                    boolean interpolate) {

        enum DIRECTION {
            NOT_SET,
            ASCENDING,
            DESCENDING
        }
        Coord coord1 = null;
        Coord coord2 = null; // this would be a second occurrence, for instance a 90 rotated global map with -90 at far left and -90 at far right

        PixelPos pixelPosCurr;
        GeoPos geoPosCurr;

        int increment = 1;

        double NAN_PIXEL = 9991234;  // just needs to be unique (note 999 is used for no data)
//        double NAN_PIXEL = Double.NaN;  // just needs to be unique (note 999 is used for no data)
        double lonPrev = NAN_PIXEL;
        double lonCurr = NAN_PIXEL;
        PixelPos pixelPosPrev = null;
        GeoPos geoPosPrev = null;

        DIRECTION direction = DIRECTION.NOT_SET;
        boolean directionChange = false;
        boolean directionChangeMaintained = false;
        boolean datelineJustCrossed = false;
        double deltaPixel = 0;
        double toleranceDegrees = 0;

        double shiftPixelX = 0.5;  // used to shift pixel location to center of pixel (by default pixel location is left edge in x direction

        for (double pixelX = 0.0; pixelX <= (raster.getRasterWidth() - 1); pixelX += increment) {
            pixelPosCurr = new PixelPos(pixelX, pixelY);
            geoPosCurr = raster.getGeoCoding().getGeoPos(pixelPosCurr, null);
            boolean matchFound = false;
//            lonPixel = validAdjust(lonCurr);

            lonCurr = adjustLon(geoPosCurr.lon);
//            lonPrev = adjustLon(geoPosPrev.lon);


            if (validLon(lonCurr)) {
                if (geoPosPrev != null) {
                    if (validLon(lonPrev)) {
                        deltaPixel = lonCurr - lonPrev;
                    }
                } else {
                    PixelPos pixPosNext = new PixelPos(pixelX + 1, pixelY);
                    GeoPos geoPosNext = raster.getGeoCoding().getGeoPos(pixPosNext, null);
                    double lonNext = adjustLon(geoPosNext.lon);
                    if (validLon(lonNext)) {
                        deltaPixel = lonNext - lonCurr;
                    }
                }


                // establish direction and whether direction has changed this time

                // todo maybe add a 2nd check for maintained direction change?
                if (Math.abs(deltaPixel) < 180) {
                    if (deltaPixel >= 0) {
                        if (direction != DIRECTION.NOT_SET && direction == DIRECTION.DESCENDING) {
                            if (directionChange == true) {
                                directionChangeMaintained = true;
                                directionChange = false;
                            } else {
                                directionChange = true;
                            }
                        }
                        // todo maybe not maintained?
                        direction = DIRECTION.ASCENDING;
                    } else {
                        if (direction != DIRECTION.NOT_SET && direction == DIRECTION.ASCENDING) {
                            if (directionChange == true) {
                                directionChangeMaintained = true;
                                directionChange = false;
                            } else {
                                directionChange = true;
                            }
                        }
                        // todo maybe not maintained?
                        direction = DIRECTION.DESCENDING;
                    }

                    datelineJustCrossed = false;
                } else {
                    datelineJustCrossed = true;
                }

                if (lonPrev == NAN_PIXEL) {
                    // this is first valid pixel

                    toleranceDegrees = Math.abs(tolerance * deltaPixel);

                    if (direction == DIRECTION.ASCENDING) {
                    if (meridianLon >= (lonCurr - toleranceDegrees) && meridianLon <= (lonCurr)) {
                            matchFound = true;
                        }
                    } else {
                        if (meridianLon <= (lonCurr + toleranceDegrees) && meridianLon >= (lonCurr)) {
                            matchFound = true;
                        }
                    }


                    if (matchFound) {
                            PixelPos pixelPosNext = new PixelPos(pixelX + 1, pixelY);
                            GeoPos geoPosNext = raster.getGeoCoding().getGeoPos(pixelPosNext, null);

                            Coord coordCurr = new Coord(geoPosCurr, pixelPosCurr);
                            Coord coordNext = new Coord(geoPosNext, pixelPosNext);

                            Coord coordInterp;
                            if (interpolate) {
                                coordInterp = getCoordInterpolateToFixedLon(coordCurr, coordNext, meridianLon, false);
                            } else {
                                coordInterp = coordCurr;
                            }
                            coordInterp = getCoordShiftPixelX(coordInterp, shiftPixelX);

                        if (coord1 == null) {
                            coord1 = coordInterp;
                        } else if (coord2 == null) {
                            // avoid adding a duplicate gridlines caused by a nearby pixel geo fluctuation such a might occur in unmapped level-2 data
                            if (Math.abs(pixelPosCurr.x - coord1.pixelPos.x) > (raster.getRasterWidth() / 4.0)) {
                                coord2 = coordInterp;
                            }
                        }
                    }
                } else {
                    // this is not the first geo pixel and probably not the last (but could be the last)

                    if (pixelPosCurr.x < raster.getRasterWidth() - 1) {

                    if ((direction == DIRECTION.ASCENDING && lonCurr > lonPrev)
                    || (direction == DIRECTION.DESCENDING && lonCurr < lonPrev)) {
                        // dateline not crossed
                        if ((direction == DIRECTION.ASCENDING && meridianLon > lonPrev && meridianLon <= lonCurr) ||
                                (direction == DIRECTION.DESCENDING && meridianLon < lonPrev && meridianLon >= lonCurr)) {
                            Coord coordCurr = new Coord(geoPosCurr, pixelPosCurr);
                            Coord coordPrev = new Coord(geoPosPrev, pixelPosPrev);

                            Coord coordInterp;
                            if (interpolate) {
                                coordInterp = getCoordInterpolateToFixedLon(coordPrev, coordCurr, meridianLon, true);
                            } else {
                                coordInterp = coordCurr;
                            }
                            coordInterp = getCoordShiftPixelX(coordInterp, shiftPixelX);


                            if (coord1 == null) {
                                coord1 = coordInterp;
                            } else if (coord2 == null) {
                                // avoid adding a duplicate gridlines caused by a nearby pixel geo fluctuation such a might occur in unmapped level-2 data
                                if (Math.abs(pixelPosCurr.x - coord1.pixelPos.x) > (raster.getRasterWidth() / 4.0)) {
                                    coord2 = coordInterp;
                                }
                            }
                        }

                    } else if (datelineJustCrossed) {
                        // dateline just crossed

                        if (meridianLon == 180) {
                            PixelPos pixelPosPrev2Back;
                            if (direction == DIRECTION.ASCENDING) {
                                pixelPosPrev2Back = new PixelPos(pixelPosPrev.x - 1, pixelPosPrev.y);
                            } else {
                                pixelPosPrev2Back = new PixelPos(pixelPosPrev.x + 1, pixelPosPrev.y);
                            }
                            GeoPos geoPosPrev2Back = raster.getGeoCoding().getGeoPos(pixelPosPrev2Back, null);

                            Coord coordPrev2Back = new Coord(geoPosPrev2Back, pixelPosPrev2Back);
                            Coord coordPrev = new Coord(geoPosPrev, pixelPosPrev);

                            Coord coordInterp;
                            if (interpolate) {
                                coordInterp = getCoordInterpolateToFixedLon(coordPrev2Back, coordPrev, meridianLon, false);
                            } else {
                                coordInterp = coordPrev;
                            }
                            coordInterp = getCoordShiftPixelX(coordInterp, shiftPixelX);

                            if (coord1 == null) {
                                coord1 = coordInterp;
                            } else if (coord2 == null) {
                                // avoid adding a duplicate gridlines caused by a nearby pixel geo fluctuation such a might occur in unmapped level-2 data
                                if (Math.abs(pixelPosCurr.x - coord1.pixelPos.x) > (raster.getRasterWidth() / 4.0)) {
                                    coord2 = coordInterp;
                                }
                            }
                        }
                    } else {
                        // ignore
                    }

                    } else {
                        // this is last pixel
                        toleranceDegrees = Math.abs(tolerance * deltaPixel);

                        if (direction == DIRECTION.ASCENDING) {
                        if (meridianLon >= (lonCurr) && meridianLon <= (lonCurr + toleranceDegrees)) {
                                matchFound = true;
                            }
                        } else {
                            if (meridianLon <= (lonCurr) && meridianLon >= (lonCurr - toleranceDegrees)) {
                                matchFound = true;
                            }
                        }

                        if (matchFound) {
                            Coord coordPrev = new Coord(geoPosPrev, pixelPosPrev);
                            Coord coordCurr = new Coord(geoPosCurr, pixelPosCurr);


                            Coord coordInterp;
                            if (interpolate) {
                                coordInterp = getCoordInterpolateToFixedLon(coordPrev, coordCurr, meridianLon, false);
                            } else {
                                coordInterp = coordCurr;
                            }
                            coordInterp = getCoordShiftPixelX(coordInterp, shiftPixelX);


                            if (coord1 == null) {
                                coord1 = coordInterp;
                            } else if (coord2 == null) {
                                // avoid adding a duplicate gridlines caused by a nearby pixel geo fluctuation such a might occur in unmapped level-2 data
                                if (Math.abs(pixelPosCurr.x - coord1.pixelPos.x) > (raster.getRasterWidth() / 4.0)) {
                                    coord2 = coordInterp;
                                }
                            }
                        }
                    }

                }

                lonPrev = lonCurr;
                pixelPosPrev = pixelPosCurr;
                geoPosPrev = geoPosCurr;
            } else {
                if (validLon(lonPrev)) {
                    // this is first pixel after last valid pixel
                    toleranceDegrees = Math.abs(tolerance * deltaPixel);

                    if (direction == DIRECTION.ASCENDING) {
                    if (meridianLon >= (lonPrev) && meridianLon <= (lonPrev + toleranceDegrees)) {
                            matchFound = true;
                        }
                    } else {
                        if (meridianLon <= (lonPrev) && meridianLon >= (lonPrev - toleranceDegrees)) {
                            matchFound = true;
                        }
                    }


                    if (matchFound) {

                        if (pixelPosPrev != null) {
                            PixelPos pixelPosPrev2Back = new PixelPos(pixelPosPrev.x - 1, pixelPosPrev.y);
                            GeoPos geoPosPrev2Back = raster.getGeoCoding().getGeoPos(pixelPosPrev2Back, null);

                            Coord coordPrev2Back = new Coord(geoPosPrev2Back, pixelPosPrev2Back);
                            Coord coordPrev = new Coord(geoPosPrev, pixelPosPrev);

                            Coord coordInterp;
                            if (interpolate) {
                                coordInterp = getCoordInterpolateToFixedLon(coordPrev2Back, coordPrev, meridianLon, false);
                            } else {
                                coordInterp = coordPrev;
                            }
                            coordInterp = getCoordShiftPixelX(coordInterp, shiftPixelX);


                            if (coord1 == null) {
                                coord1 = coordInterp;
                            } else if (coord2 == null) {
                                // avoid adding a duplicate gridlines caused by a nearby pixel geo fluctuation such a might occur in unmapped level-2 data
                                if (Math.abs(pixelPosCurr.x - coord1.pixelPos.x) > (raster.getRasterWidth() / 4.0)) {
                                    coord2 = coordInterp;
                                }
                            }
                        }
                    }

                    break;
                }
            }
        }

        Coord[] coords = {coord1, coord2};


        return coords;
    }


    static double adjustLon(double lon) {
        // adapt lon that is out of range
        if (lon > 180) {
            lon -= 360;
        } else if (lon < -180) {
            lon += 360;
        }

        return lon;
    }


    static Coord[] getCoordParallel(double parallelLat, double pixelX, RasterDataNode raster, double tolerance, boolean interpolate) {


        enum DIRECTION {
            NOT_SET,
            ASCENDING,
            DESCENDING
        }

        Coord coord1 = null;
        Coord coord2 = null; // this would be a second ocurance, for instance a 90 rotated global map with -90 at far left and -90 at far right

        PixelPos pixPosCurr;
        GeoPos geoPoxCurr;

        int increment = 1;


        double NULL_LAT = 9991234;  // just needs to be unique (note 999 is used for no data)
//        double NAN_PIXEL = Double.NaN;  // just needs to be unique (note 999 is used for no data)
        double latPrev = NULL_LAT;
        PixelPos pixPosPrev = null;
        GeoPos geoPosPrev = null;

        boolean firstValidFound = false;
        DIRECTION direction = DIRECTION.NOT_SET;
        DIRECTION directionFirst = DIRECTION.NOT_SET;
        boolean directionChange = false;


        double deltaLat = 0;
        double toleranceDegrees = 0;

        double offset = 0.5;

        boolean matchFound = false;
        for (double pixelY =  (raster.getRasterHeight() - 1); pixelY >= 0;  pixelY -= increment) {
            pixPosCurr = new PixelPos(pixelX, pixelY);
            geoPoxCurr = raster.getGeoCoding().getGeoPos(pixPosCurr, null);
            double latCurr = geoPoxCurr.lat;
            matchFound = false;
//            lonPixel = validAdjust(lonPixel);

            if (validLat(latCurr)) {
                if (firstValidFound) {
                    if (validLat(latPrev)) {
                        deltaLat = latCurr - latPrev;
                    }
                } else {
                    PixelPos nextPixelPos = new PixelPos(pixelX, pixelY - 1);
                    GeoPos nextGeoPos = raster.getGeoCoding().getGeoPos(nextPixelPos, null);
                    if (validLat(nextGeoPos.lat)) {
                        deltaLat = nextGeoPos.lat - geoPoxCurr.lat;
                    }
                }

                // establish direction and whether direction has changed this time
                directionChange = false;
                if (deltaLat >= 0) {
                    if (direction != DIRECTION.NOT_SET && direction == DIRECTION.DESCENDING) {
                        directionChange = true;
                    }
                    direction = DIRECTION.ASCENDING;
                } else {
                    if (direction != DIRECTION.NOT_SET && direction == DIRECTION.ASCENDING) {
                        directionChange = true;
                    }
                    direction = DIRECTION.DESCENDING;
                }


                if (latPrev == NULL_LAT) {
                    // this is first valid pixel
                    firstValidFound = true;

                    toleranceDegrees = Math.abs(tolerance * deltaLat);

                    if (direction == DIRECTION.ASCENDING) {

                    if (parallelLat >= (latCurr - toleranceDegrees) && parallelLat <= (latCurr)) {
                            matchFound = true;
                        }
                    } else {
                        if (parallelLat <= (latCurr + toleranceDegrees) && parallelLat >= (latCurr)) {
                            matchFound = true;
                        }
                    }

                    if (matchFound) {
                            PixelPos pixPosNext = new PixelPos(pixelX, pixelY - 1);
                            GeoPos geoPosNext = raster.getGeoCoding().getGeoPos(pixPosNext, null);

                            Coord coordCurr = new Coord(geoPoxCurr, pixPosCurr);
                            Coord coordNext = new Coord(geoPosNext, pixPosNext);

                            Coord coordInterp;
                            if (interpolate) {
                                coordInterp = getCoordInterpolateToFixedLat(coordCurr, coordNext, parallelLat, false);
                            } else {
                                coordInterp = coordCurr;
                            }
                            coordInterp = getCoordShiftPixelY(coordInterp, offset);


                        if (coord1 == null) {
                            coord1 = coordInterp;
                            directionFirst = direction;
                        } else if (coord2 == null) {
                            // avoid adding a duplicate gridlines caused by a nearby pixel geo fluctuation such a might occur in unmapped level-2 data
//                            if (!restrictNearbyDuplicate ||  (Math.abs(pixPosCurr.y - coord1.pixelPos.y) > (raster.getRasterHeight() / 4.0))) {
                            if (direction != directionFirst) {
                                coord2 = coordInterp;
                            }
                        }
                    }
                } else {

                    if (pixPosCurr.y > 0 ) {
                    // this is not the first geo pixel and probably not the last (but could be the last)
                    if (!directionChange) {
                        if (direction == DIRECTION.ASCENDING) {
                            if (parallelLat > latPrev && parallelLat <= latCurr) {
                                matchFound = true;
                            }
                        } else {
                            if (parallelLat < latPrev && parallelLat >= latCurr) {
                                matchFound = true;
                            }
                        }
                    } else {
                        // skipping initial direction change in case bow-tie effect or some other artifact
                    }

                    if (matchFound) {
                        Coord coordCurr = new Coord(geoPoxCurr, pixPosCurr);
                        Coord coordPrev = new Coord(geoPosPrev, pixPosPrev);

                        Coord coordInterp;
                        if (interpolate) {
                            coordInterp = getCoordInterpolateToFixedLat(coordPrev, coordCurr, parallelLat, true);
                        } else {
                            coordInterp = coordCurr;
                        }
                        coordInterp = getCoordShiftPixelY(coordInterp, offset);

                        if (coord1 == null) {
                            coord1 = coordInterp;
                                directionFirst = direction;
                        } else if (coord2 == null) {
                            // avoid adding a duplicate gridlines caused by a nearby pixel geo fluctuation such a might occur in unmapped level-2 data
//                            if (!restrictNearbyDuplicate ||  (Math.abs(pixPosCurr.y - coord1.pixelPos.y) > (raster.getRasterHeight() / 4.0))) {
                                if (direction != directionFirst) {
                                coord2 = coordInterp;
                            }
                        }
                    }


                    } else {
                        // this is last pixel
                        toleranceDegrees = Math.abs(tolerance * deltaLat);

                        if (direction == DIRECTION.ASCENDING) {
                        if (parallelLat >= (latCurr) && parallelLat <= (latCurr + toleranceDegrees)) {
                                matchFound = true;
                            }
                        } else {
                            if (parallelLat <= (latCurr) && parallelLat >= (latCurr - toleranceDegrees)) {
                                matchFound = true;
                            }
                        }

                        if (matchFound) {
                            Coord coordPrev = new Coord(geoPosPrev, pixPosPrev);
                            Coord coordCurr = new Coord(geoPoxCurr, pixPosCurr);


                            Coord coordInterp;
                            if (interpolate) {
                                coordInterp = getCoordInterpolateToFixedLat(coordPrev, coordCurr, parallelLat, false);
                            } else {
                                coordInterp = coordCurr;
                            }
                            coordInterp = getCoordShiftPixelY(coordInterp, offset);


                            if (coord1 == null) {
                                coord1 = coordInterp;
                                directionFirst = direction;
                            } else if (coord2 == null) {
                                // avoid adding a duplicate gridlines caused by a nearby pixel geo fluctuation such a might occur in unmapped level-2 data
//                                if (!restrictNearbyDuplicate ||  (Math.abs(pixPosCurr.y - coord1.pixelPos.y) > (raster.getRasterHeight() / 4.0))) {
                                if (direction != directionFirst) {
                                    coord2 = coordInterp;
                                }
                            }
                        }
                    }

                }

                latPrev = latCurr;
                pixPosPrev = pixPosCurr;
                geoPosPrev = geoPoxCurr;
            } else {
                if (validLat(latPrev)) {
                    // this is first pixel after last valid pixel
                    toleranceDegrees = Math.abs(tolerance * deltaLat);

                    if (direction == DIRECTION.ASCENDING) {
                    if (parallelLat >= (latPrev) && parallelLat <= (latPrev + toleranceDegrees)) {
                            matchFound = true;
                        }
                    } else {
                        if (parallelLat <= (latPrev) && parallelLat >= (latPrev - toleranceDegrees)) {
                            matchFound = true;
                        }
                    }


                    if (matchFound) {
                        PixelPos prevPrevPoint = new PixelPos(pixPosPrev.x, pixPosPrev.y + 1);
                        GeoPos prevPrevCoordAtPoint = raster.getGeoCoding().getGeoPos(prevPrevPoint, null);

                        Coord coordPrevPrev = new Coord(prevPrevCoordAtPoint, prevPrevPoint);
                        Coord coordPrev = new Coord(geoPosPrev, pixPosPrev);

                        Coord coordInterp;
                        if (interpolate) {
                            coordInterp = getCoordInterpolateToFixedLat(coordPrevPrev, coordPrev, parallelLat, false);
                        } else {
                            coordInterp = coordPrev;
                        }
                        coordInterp = getCoordShiftPixelY(coordInterp, offset);

                        if (coord1 == null) {
                            coord1 = coordInterp;
                            directionFirst = direction;
                        } else if (coord2 == null) {
                            // avoid adding a duplicate gridlines caused by a nearby pixel geo fluctuation such a might occur in unmapped level-2 data
//                            if (!restrictNearbyDuplicate ||  (Math.abs(pixPosCurr.y - coord1.pixelPos.y) > (raster.getRasterHeight() / 4.0))) {
                            if (direction != directionFirst) {
                                coord2 = coordInterp;
                            }
                        }
                    }

                    break;
                }
            }


        }

        Coord[] coords = {coord1, coord2};
        return coords;
    }








    static Coord getCoordInterpolateToFixedLon(Coord prevCoord, Coord currCoord, double lonDesired, boolean interpolate) {

        double lonCurr = adjustLon(currCoord.geoPos.lon);
        double lonPrev = adjustLon(prevCoord.geoPos.lon);
        double latPrev = prevCoord.geoPos.lat;
        double latCurr = currCoord.geoPos.lat;

        double xPrev = prevCoord.pixelPos.x;
        double xCurr = currCoord.pixelPos.x;
        double yPrev = prevCoord.pixelPos.y;
        double yCurr = currCoord.pixelPos.y;

        final double interpolationWeight = (lonDesired - lonPrev) / (lonCurr - lonPrev);
        if (interpolate && (interpolationWeight < 0.0 || interpolationWeight > 1.0)) {
            // this is extrapolation and interpolation was requested
            return currCoord;
        }


        final double latDesired = latPrev + interpolationWeight * (latCurr - latPrev);
        final double x = xPrev + interpolationWeight * (xCurr - xPrev);
        final double y = yPrev + interpolationWeight * (yCurr - yPrev);

        GeoPos desiredGeoPos = new GeoPos(latDesired, lonDesired);
        PixelPos desiredPixelPos = new PixelPos(x,y);
        Coord desiredCoord = new Coord(desiredGeoPos, desiredPixelPos);

        return desiredCoord;
    }



    static Coord getCoordInterpolateToFixedLat(Coord prevCoord, Coord currCoord, double latDesired, boolean interpolate) {

        double lonCurr = currCoord.geoPos.lon;
        double lonPrev = prevCoord.geoPos.lon;
        double latPrev = prevCoord.geoPos.lat;
        double latCurr = currCoord.geoPos.lat;

        double xPrev = prevCoord.pixelPos.x;
        double xCurr = currCoord.pixelPos.x;
        double yPrev = prevCoord.pixelPos.y;
        double yCurr = currCoord.pixelPos.y;

        final double interpolationWeight = (latDesired - latPrev) / (latCurr - latPrev);
        if (interpolate && (interpolationWeight < 0.0 || interpolationWeight > 1.0)) {
            // this is extrapolation and interpolation was requested
            return currCoord;
        }


        final double lonDesired = lonPrev + interpolationWeight * (lonCurr - lonPrev);
        final double x = xPrev + interpolationWeight * (xCurr - xPrev);
        final double y = yPrev + interpolationWeight * (yCurr - yPrev);

        GeoPos desiredGeoPos = new GeoPos(latDesired, lonDesired);
        PixelPos desiredPixelPos = new PixelPos(x,y);
        Coord desiredCoord = new Coord(desiredGeoPos, desiredPixelPos);

        return desiredCoord;
    }



    static Coord getCoordShiftPixelX(Coord coord, double shift) {

        PixelPos pixelPosShifted = new PixelPos(coord.pixelPos.x + shift, coord.pixelPos.y);

        Coord coordShifted = new Coord(coord.geoPos, pixelPosShifted);

        return coordShifted;

    }

    static Coord getCoordShiftPixelY(Coord coord, double shift) {

        PixelPos pixelPosShifted = new PixelPos(coord.pixelPos.x, coord.pixelPos.y + shift);

        Coord coordShifted = new Coord(coord.geoPos, pixelPosShifted);

        return coordShifted;

    }




    static boolean validLon(double lon) {
        if (lon >= -180 && lon <= 180) {
            return true;
        } else {
            return false;
        }
    }

    static boolean validLat(double lat) {
        if (lat >= -90 && lat <= 90) {
            return true;
        } else {
            return false;
        }
    }

    static double validAdjust(double lon) {
        // Allows data to contain lon values exceeding 180 by 360, but adjusts to normal
        // Conforms all invalid lon values to Double.NaN
        if (lon >= -180 && lon <= 180) {
            return lon;
        } else if (lon > 180 && lon <= (180 + 360)) {
            return lon - 360;
        } else {
            return Double.NaN;
        }
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

        boolean lonBoundaryIncreasingFound = false;
        boolean lonBoundaryDecreasingFound = false;

        for (int i = 1; i < geoBoundary.length; i++) {
            geoPos = geoBoundary[i];
            lonBoundaryCurr = geoPos.lon;
            latBoundaryCurr = geoPos.lat;

            // todo test

            if (lonBoundaryCurr > 117.99 & lonBoundaryCurr < 118.01) {
                int r=0;
                int t = r;
            }
            if (lonBoundaryCurr > 119.99 & lonBoundaryCurr < 120.01) {
                int r=0;
                int t = r;
            }


//            boolean onMeridian = false;
//            // only examine steps around geoBoundary where lon is changing
//            if (lonBoundaryCurr != lonBoundaryPrev) {
//                double deltaLon = Math.abs(lonBoundaryCurr - lonBoundaryPrev);
//                double deltaLat = Math.abs(latBoundaryCurr - latBoundaryPrev);
//                if (deltaLon > deltaLat) {
//                    onMeridian = true;
//                }
//            }

            // only examine steps around geoBoundary where lon is changing
//            if (onMeridian) {
            if (lonBoundaryCurr != lonBoundaryPrev) {
                boolean lonBoundaryIsIncreasing = lonBoundaryCurr > lonBoundaryPrev;

                boolean useThis = false;
                if (lonBoundaryIsIncreasing) {
                    if (!lonBoundaryIncreasingFound && lonBoundaryCurr >= lon) {
                        useThis = true;
                        lonBoundaryIncreasingFound =true;
                    }
                } else {
                    if (!lonBoundaryDecreasingFound && lonBoundaryCurr <= lon) {
                        useThis = true;
                        lonBoundaryDecreasingFound = true;
                    }
                }

                if (useThis) {
                    // compute lat based on interpolation and add geoPos to intersectionList
                    final double interpolationWeight = (lon - lonBoundaryPrev) / (lonBoundaryCurr - lonBoundaryPrev);
                    if (interpolationWeight >= 0.0 && interpolationWeight < 1.0) {
                        final double lat = latBoundaryPrev + interpolationWeight * (latBoundaryCurr - latBoundaryPrev);
                        intersectionList.add(new GeoPos(lat, lon));
                    }
                }

                // find the step which crosses over desired lon
//                if (((lon >= lonBoundaryPrev && lon <= lonBoundaryCurr) ||
//                        (lon >= lonBoundaryCurr && lon <= lonBoundaryPrev))) {
//
//                    // compute lon based on interpolation and add geoPos to intersectionList
//                    final double interpolationWeight = (lon - lonBoundaryPrev) / (lonBoundaryCurr - lonBoundaryPrev);
//                    if (interpolationWeight >= 0.0 && interpolationWeight < 1.0) {
//                        final double lat = latBoundaryPrev + interpolationWeight * (latBoundaryCurr - latBoundaryPrev);
//                        intersectionList.add(new GeoPos(lat, lon));
//                    }
//                }

            }

            lonBoundaryPrev = lonBoundaryCurr;
            latBoundaryPrev = latBoundaryCurr;
        }
    }

    private static GeneralPath[] createPathsNew(List<List<Coord>> pathsList) {
        final ArrayList<GeneralPath> generalPathList = new ArrayList<GeneralPath>();
        addToPath(pathsList, generalPathList);
        return generalPathList.toArray(new GeneralPath[0]);
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
                                                TextLocation textLocation,
                                                boolean formatCompass,
                                                boolean formatDecimal,
                                                double majorStep,
                                                RasterDataNode raster,
                                                int spacer,
                                                GeoSpan geoSpan) {
        final List<TextGlyph> textGlyphs = new ArrayList<TextGlyph>();

        switch (textLocation) {
            case NORTH:
                createNorthernLongitudeTextGlyphs(longitudeGridLinePoints, textGlyphs, formatCompass, formatDecimal, majorStep, raster, geoSpan);
                break;
            case SOUTH:
                createSouthernLongitudeTextGlyphs(longitudeGridLinePoints, textGlyphs, formatCompass, formatDecimal, majorStep, raster, geoSpan);
                break;
            case WEST:
                createWesternLatitudeTextGlyphs(latitudeGridLinePoints, textGlyphs, formatCompass, formatDecimal, majorStep, raster, spacer, geoSpan);
                break;
            case EAST:
                createEasternLatitudeTextGlyphs(latitudeGridLinePoints, textGlyphs, formatCompass, formatDecimal, majorStep, raster, spacer, geoSpan);
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
                                                        List<TextGlyph> textGlyphs,
                                                        boolean formatCompass,
                                                        boolean formatDecimal,
                                                        double majorStep,
                                                        RasterDataNode raster,
                                                        int spacer,
                                                        GeoSpan geoSpan) {

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

                double yPos = (coord1.pixelPos.getY());
                double xPos = (coord1.pixelPos.getX());
                int height = raster.getRasterHeight();
                int width = raster.getRasterWidth();

                double offsetX = 0.0;


                boolean allowLabel = true;
                boolean onlyAllowLeftEdgeLabels = false;
                // todo improve logic or add uers option to control labels
                if (geoSpan.lonSpan < 180 && geoSpan.latSpan < 90) {
                    onlyAllowLeftEdgeLabels = true;
                }

                if (onlyAllowLeftEdgeLabels) {
                    int buffer = 3;
                    if (xPos > (0 + buffer)) {
                        allowLabel = false;
                    }
                } else {
                    if (yPos < 1 || yPos > (height - 1)) {
                        if (xPos > 1) {
                            if (spacer >= 0) {
                                offsetX = -spacer;
                            }
                        }
                    }
                }




                if (isCoordPairValid(coord1, coord2) && allowLabel) {

                    double lat = coord1.geoPos.lat;
                    double latToUse = lat;
                    double currDiff;
                    double lowestDiff = 100000;

//                    if (majorStep != Double.NaN && majorStep >= 1) {
//                        for (double curr = -90; curr <= 90; curr += majorStep) {
//                            // todo
//                            if ((lat < (curr - majorStep)) || (lat > (curr + majorStep))) {
//                                continue;
//                            }
//                            currDiff = Math.abs(lat - curr);
//                            if (currDiff < lowestDiff) {
//                                lowestDiff = currDiff;
//                                latToUse = curr;
//                            }
//                        }
//                    }

                    TextGlyph textGlyph = createTextGlyph(coord1.geoPos.getLatString(latToUse,formatCompass, formatDecimal), coord1, coord2, offsetX, 0);
                    textGlyphs.add(textGlyph);



//                    TextGlyph textGlyph = createTextGlyph(coord1.geoPos.getLatString(formatCompass, formatDecimal), coord1, coord2);
//                    textGlyphs.add(textGlyph);
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
                                                        List<TextGlyph> textGlyphs,
                                                        boolean formatCompass,
                                                        boolean formatDecimal,
                                                        double majorStep,
                                                        RasterDataNode raster,
                                                        int spacer,
                                                        GeoSpan geoSpan) {

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


                double yPos = (coord1.pixelPos.getY());
                double xPos = (coord1.pixelPos.getX());
                int height = raster.getRasterHeight();
                int width = raster.getRasterWidth();

                double offsetX = 0.0;


                boolean allowLabel = true;
                boolean onlyAllowRightEdgeLabels = false;
                // todo improve logic or add uers option to control labels
                if (geoSpan.lonSpan < 180 && geoSpan.latSpan < 90) {
                    onlyAllowRightEdgeLabels = true;
                }

                if (onlyAllowRightEdgeLabels) {
                    int buffer = 3;
                    if (xPos < (width - buffer)) {
                        allowLabel = false;
                    }
                } else {
                    if (yPos < 1 || yPos > (height - 1)) {
                        if (xPos < (width -1)) {
                            if (spacer >= 0) {
                                offsetX = spacer;
                            }
                        }
                    }
                }



                if (isCoordPairValid(coord1, coord2) && allowLabel) {

                    double lat = coord1.geoPos.lat;
                    double latToUse = lat;
                    double currDiff;
                    double lowestDiff = 100000;

//                    if (majorStep != Double.NaN && majorStep >= 1) {
//                        for (int curr = -90; curr <= 90; curr += majorStep) {
//                            // todo
//                            if ((lat < (curr - majorStep)) || (lat > (curr + majorStep))) {
//                                continue;
//                            }
//                            double currDouble = (double) curr;
//                            currDiff = Math.abs(lat - currDouble);
//                            if (currDiff < lowestDiff) {
//                                lowestDiff = currDiff;
//                                latToUse = currDouble;
//                            }
//                        }
//                    }

                    TextGlyph textGlyph = createTextGlyph(coord1.geoPos.getLatString(latToUse, formatCompass, formatDecimal), coord1, coord2, offsetX, 0);
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
                                                          List<TextGlyph> textGlyphs, boolean formatCompass,
                                                          boolean formatDecimal,
                                                          double majorStep,
                                                          RasterDataNode raster,
                                                          GeoSpan geoSpan) {

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

                double yPos = (coord1.pixelPos.getY());
                double xPos = (coord1.pixelPos.getX());
                int height = raster.getRasterHeight();
                int width = raster.getRasterWidth();

                boolean allowLabel = true;
                boolean onlyAllowNorthEdgeLabels = false;
                // todo improve logic or add uers option to control labels
                if (geoSpan.lonSpan < 180 && geoSpan.latSpan < 90) {
                    onlyAllowNorthEdgeLabels = true;
                }

                if (onlyAllowNorthEdgeLabels) {
                    int buffer = 3;
                    if (yPos > (0 + buffer)) {
                        allowLabel = false;
                    }
                }

//                System.out.println("Northern:  xPos=" + xPos + "  yPos=" + yPos + " width=" + width + " height=" + height);

                if (isCoordPairValid(coord1, coord2) && allowLabel) {
                    double lon = coord1.geoPos.lon;
                    double lonToUse = lon;
                    double currDiff;
                    double lowestDiff = 100000;

//                    if (majorStep != Double.NaN && majorStep >= 1) {
//                        for (double curr = -180; curr <= 180; curr += majorStep) {
//                            // todo
//                            if ((lon < (curr - majorStep)) || (lon > (curr + majorStep))) {
//                                continue;
//                            }
//                            currDiff = Math.abs(lon - curr);
//                            if (currDiff < lowestDiff) {
//                                lowestDiff = currDiff;
//                                lonToUse = curr;
//                            }
//                        }
//                    }

                    TextGlyph textGlyph = createTextGlyph(coord1.geoPos.getLonString(lonToUse,formatCompass, formatDecimal), coord1, coord2, 0, 0);
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
                                                  List<TextGlyph> textGlyphs, boolean formatCompass, boolean formatDecimal,
                                                  double majorStep,
                                                  RasterDataNode raster,
                                                  GeoSpan geoSpan) {

        // Assumes that the line was drawn from north to south
        // coord1 set to last point in order to anchor the text to the edge of the line
        // text will point upwards due to this so it may be subsequently rotated if desired
        for (List<Coord> longitudeGridLinePoint : longitudeGridLinePoints) {
            if (longitudeGridLinePoint.size() >= 2) {
                final int last = longitudeGridLinePoint.size() - 1;
                final Coord coord1 = longitudeGridLinePoint.get(last);

                final PixelPos pixelPos2 = new PixelPos((float) (coord1.pixelPos.getX()), (float) (coord1.pixelPos.getY() - 1));
                final Coord coord2 = new Coord(coord1.geoPos, pixelPos2);

                double yPos = (coord1.pixelPos.getY());
                double xPos = (coord1.pixelPos.getX());
                int height = raster.getRasterHeight();
                int width = raster.getRasterWidth();


                boolean allowLabel = true;
                boolean onlyAllowSouthEdgeLabels = false;
                // todo improve logic or add uers option to control labels
                if (geoSpan.lonSpan < 180 && geoSpan.latSpan < 90) {
                    onlyAllowSouthEdgeLabels = true;
                }


                if (onlyAllowSouthEdgeLabels) {
                    int buffer = 3;
                    if (yPos < (height - buffer)) {
                        allowLabel = false;
                    }
                }




//                System.out.println("Southern:  xPos=" + xPos + "  yPos=" + yPos + " width=" + width + " height=" + height);

                if (isCoordPairValid(coord1, coord2) && allowLabel) {
                    double lon = coord1.geoPos.lon;
                    double lonToUse = lon;
                    double currDiff;
                    double lowestDiff = 100000;

//                    if (majorStep != Double.NaN && majorStep >= 1) {
//                        for (double curr = -180; curr <= 180; curr += majorStep) {
//                            // todo
//                            if ((lon < (curr - majorStep)) || (lon > (curr + majorStep))) {
//                                continue;
//                            }
//                            currDiff = Math.abs(lon - curr);
//                            if (currDiff < lowestDiff) {
//                                lowestDiff = currDiff;
//                                lonToUse = curr;
//                            }
//                        }
//                    }

                    TextGlyph textGlyph = createTextGlyph(coord1.geoPos.getLonString(lonToUse,formatCompass, formatDecimal), coord1, coord2, 0, 0);
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
            return createTextGlyph(coord1.geoPos.getLonString(formatCompass, formatDecimal), coord1, coord2, 0, 0);
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
            return createTextGlyph(coord1.geoPos.getLatString(formatCompass, formatDecimal), coord1, coord2, 0, 0);
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



    public static boolean containsValidGeoCorners(RasterDataNode dataNode) {

        final GeoCoding geoCoding = dataNode.getGeoCoding();
        PixelPos pixelPosCurr = new PixelPos(0, 0);
        GeoPos geoPosCurr = geoCoding.getGeoPos(pixelPosCurr, null);

        if (validLon(geoPosCurr.lon) && validLat(geoPosCurr.lat)) {
            return true;
        } else {
            return false;
        }
    }


    static boolean isEquidistantCylindrical(GeoCoding geoCoding, RasterDataNode dataNode) {

       boolean equidistantCylindrical = true;
       boolean test;

        int centerParallel = (int) Math.floor(dataNode.getRasterHeight() / 2.0);
        test = isParallelLineEquidistantCylindrical(geoCoding, dataNode, centerParallel);
        if (!test) {
            return false;
        }

        int nearTopParallel = 5;
        test = isParallelLineEquidistantCylindrical(geoCoding, dataNode, nearTopParallel);
        if (!test) {
            return false;
        }

        int nearBottomParallel = dataNode.getRasterHeight() - 5;
        test = isParallelLineEquidistantCylindrical(geoCoding, dataNode, nearBottomParallel);
        if (!test) {
            return false;
        }



        int centerMeridian = (int) Math.floor(dataNode.getRasterWidth() / 2.0);
        test = isMeridianLineEquidistantCylindrical(geoCoding, dataNode, centerMeridian);
        if (!test) {
            return false;
        }

        int nearLeftMeridian = 5;
        test = isMeridianLineEquidistantCylindrical(geoCoding, dataNode, nearLeftMeridian);
        if (!test) {
            return false;
        }

        int nearRightMeridian = dataNode.getRasterHeight() - 5;
        test = isMeridianLineEquidistantCylindrical(geoCoding, dataNode, nearRightMeridian);
        if (!test) {
            return false;
        }



        return equidistantCylindrical;
    }



    static boolean isParallelLineEquidistantCylindrical(GeoCoding geoCoding, RasterDataNode dataNode, int pixelYCurr) {

        boolean equidistantCylindrical = true;

        if (pixelYCurr < 0 || pixelYCurr >= dataNode.getRasterHeight()) {
            //ensure valid index
            return true;
        }

        double NULL_LAT = -99999;
        double validLatValue = NULL_LAT;
        boolean initialLatValueFound = false;

        for (double pixelXCurr=0.0 ; pixelXCurr <= (dataNode.getRasterWidth()-1) ; pixelXCurr++ ) {

            PixelPos pixelPosCurr = new PixelPos(pixelXCurr, pixelYCurr);
            GeoPos geoPosCurr = geoCoding.getGeoPos(pixelPosCurr, null);

            if (validLat(geoPosCurr.lat)) {
                if (initialLatValueFound) {
                    if (geoPosCurr.lat != validLatValue) {
                        return false;
                    }
                } else {
                    validLatValue = geoPosCurr.lat;
                    initialLatValueFound = true;
                }
            }
        }

        return equidistantCylindrical;
    }



    static boolean isMeridianLineEquidistantCylindrical(GeoCoding geoCoding, RasterDataNode dataNode, int pixelXCurr) {

        boolean equidistantCylindrical = true;

        if (pixelXCurr < 0 || pixelXCurr >= dataNode.getRasterWidth()) {
            //ensure valid index
            return true;
        }

        double NULL_LON = -99999;
        double validLonValue = NULL_LON;
        boolean initialLonValueFound = false;

        for (double pixelYCurr=0.0 ; pixelYCurr <= (dataNode.getRasterHeight()-1) ; pixelYCurr++ ) {

            PixelPos pixelPosCurr = new PixelPos(pixelXCurr, pixelYCurr);
            GeoPos geoPosCurr = geoCoding.getGeoPos(pixelPosCurr, null);

            if (validLat(geoPosCurr.lon)) {
                if (initialLonValueFound) {
                    if (geoPosCurr.lon != validLonValue) {
                        return false;
                    }
                } else {
                    validLonValue = geoPosCurr.lon;
                    initialLonValueFound = true;
                }
            }
        }

        return equidistantCylindrical;
    }






    static CoordsPolar getCoordsPolar(GeoCoding geoCoding, RasterDataNode dataNode) {

        boolean initialLatValueFound = false;

        GeoPos northernmostGeoPos = null;
        PixelPos northernmostPixelPos = null;
        GeoPos southernmostGeoPos = null;
        PixelPos southernmostPixelPos = null;

        boolean northernmostPixelOnRasterEdge = true;
        boolean southernmostPixelOnRasterEdge = true;

        boolean northPoleCrossingDetected = false;
        boolean southPoleCrossingDetected = false;


        for (double pixelXCurr=0.0 ; pixelXCurr <= (dataNode.getRasterWidth()-1) ; pixelXCurr++ ) {
            for (double pixelYCurr=0.0 ; pixelYCurr <= (dataNode.getRasterHeight()-1) ; pixelYCurr++ ) {

                PixelPos pixelPosCurr = new PixelPos(pixelXCurr, pixelYCurr);
                GeoPos geoPosCurr = geoCoding.getGeoPos(pixelPosCurr, null);


                if (validLat(geoPosCurr.lat) && validLon(geoPosCurr.lon)) {
                    if (initialLatValueFound) {
                        if (geoPosCurr.lat >  northernmostGeoPos.lat) {
                            northernmostGeoPos = geoPosCurr;
                            northernmostPixelPos = pixelPosCurr;
                        }
                        if (geoPosCurr.lat < southernmostGeoPos.lat) {
                            southernmostGeoPos = geoPosCurr;
                            southernmostPixelPos = pixelPosCurr;
                        }
                    } else {
                        northernmostGeoPos = geoPosCurr;
                        northernmostPixelPos = pixelPosCurr;
                        southernmostGeoPos = geoPosCurr;
                        southernmostPixelPos = pixelPosCurr;
                        initialLatValueFound = true;
                    }
                }
            }
        }




        // Determine whether scene crosses North Pole
        // Note that likely no pixel in the scene will actually equal 90 degrees lat
        // So this is done by comparing pixels which surround the northernmost pixel and then extrapolating to determine whether it is close to 90 degrees lat
        // Sets boolean northPoleCrossingDetected

        if (northernmostPixelPos != null) {
            if (northernmostPixelPos.x > 0 && northernmostPixelPos.x < (dataNode.getRasterWidth() - 1) &&
                    northernmostPixelPos.y > 0 && northernmostPixelPos.y < (dataNode.getRasterHeight() - 1)) {

                northernmostPixelOnRasterEdge = false;

                double largestDeltaLat = 0;
                int validPixFound = 0;

                for (double pixelXCurr = northernmostPixelPos.x - 1; pixelXCurr <= northernmostPixelPos.x - 1; pixelXCurr++) {
                    for (double pixelYCurr = northernmostPixelPos.y - 1; pixelYCurr <= northernmostPixelPos.y - 1; pixelYCurr++) {

                        PixelPos pixelPosCurr = new PixelPos(pixelXCurr, pixelYCurr);
                        GeoPos geoPosCurr = geoCoding.getGeoPos(pixelPosCurr, null);

                        if (validLat(geoPosCurr.lat) && validLon(geoPosCurr.lon)) {
                            double deltaLat = Math.abs(northernmostGeoPos.lat - geoPosCurr.lat);

                            if (deltaLat > largestDeltaLat) {
                                largestDeltaLat = deltaLat;
                                validPixFound++;
                            }
                        }
                    }
                }


                if (validPixFound > 0) {
                    // increase the delta with a an extra buffer
                    double buffer = 2;
                    largestDeltaLat = buffer * largestDeltaLat;
                } else {
                    // force a delta as 10% of scene lat expanse
                    largestDeltaLat = Math.abs(northernmostGeoPos.lat - southernmostGeoPos.lat) * 0.1;
                }

                if ((northernmostGeoPos.lat + largestDeltaLat) >= 90) {
                    northPoleCrossingDetected = true;
                }
            }
        }



        // Determine whether scene crosses South Pole
        // Note that likely no pixel in the scene will actually equal -90 degrees lat
        // So this is done by comparing pixels which surround the southernmost pixel and then extrapolating to determine whether it is close to -90 degrees lat
        // Sets boolean southPoleCrossingDetected

        if (southernmostPixelPos != null) {
            if (southernmostPixelPos.x > 0 && southernmostPixelPos.x < (dataNode.getRasterWidth() - 1) &&
                    southernmostPixelPos.y > 0 && southernmostPixelPos.y < (dataNode.getRasterHeight() - 1)) {

                southernmostPixelOnRasterEdge = false;

                double largestDeltaLat = 0;
                int validPixFound = 0;

                for (double pixelXCurr = southernmostPixelPos.x - 1; pixelXCurr <= southernmostPixelPos.x - 1; pixelXCurr++) {
                    for (double pixelYCurr = southernmostPixelPos.y - 1; pixelYCurr <= southernmostPixelPos.y - 1; pixelYCurr++) {

                        PixelPos pixelPosCurr = new PixelPos(pixelXCurr, pixelYCurr);
                        GeoPos geoPosCurr = geoCoding.getGeoPos(pixelPosCurr, null);

                        if (validLat(geoPosCurr.lat) && validLon(geoPosCurr.lon)) {
                            double deltaLat = Math.abs(southernmostGeoPos.lat - geoPosCurr.lat);

                            if (deltaLat > largestDeltaLat) {
                                largestDeltaLat = deltaLat;
                                validPixFound++;
                            }
                        }
                    }
                }


                if (validPixFound > 0) {
                    // increase the delta with a an extra buffer
                    double buffer = 2;
                    largestDeltaLat = buffer * largestDeltaLat;
                } else {
                    // force a delta as 10% of scene lat expanse
                    largestDeltaLat = Math.abs(northernmostGeoPos.lat - southernmostGeoPos.lat) * 0.1;
                }

                if ((southernmostGeoPos.lat - largestDeltaLat) <= -90) {
                    southPoleCrossingDetected = true;
                }
            }
        }
        


        if (northernmostGeoPos != null && northernmostPixelPos != null && southernmostGeoPos != null && southernmostPixelPos != null) {
            Coord northernmostCoord = new Coord(northernmostGeoPos, northernmostPixelPos);
            Coord southernmostCoord = new Coord(southernmostGeoPos, southernmostPixelPos);

            return  new CoordsPolar(northernmostCoord, southernmostCoord, northernmostPixelOnRasterEdge, southernmostPixelOnRasterEdge, northPoleCrossingDetected, southPoleCrossingDetected);

        } else {
            return null;
        }

    }






    static boolean validLonSpan(GeoSpanLon geoSpanLon) {

        boolean valid = (geoSpanLon.lonSpan > 0 && validLon(geoSpanLon.firstLon) && validLon(geoSpanLon.lastLon));

        return valid;
    }



    static GeoSpanLon combineLonSpans(GeoSpanLon lonSpanCombined, GeoSpanLon lonSpanNewRow) {


        boolean newRowValid = validLonSpan(lonSpanNewRow);
        if (!newRowValid) {
            return lonSpanCombined;
        }


        boolean combinedValid = validLonSpan(lonSpanCombined);
        if (!combinedValid) {
            GeoSpanLon.GeoSpanLonUpdate(lonSpanCombined, lonSpanNewRow);
            return lonSpanCombined;
        }


        // At this point both entries are valid so now combine then


        if (lonSpanCombined.datelineCrossed == lonSpanNewRow.datelineCrossed) {

            if (lonSpanCombined.ascending && !lonSpanCombined.descending) {
                lonSpanCombined.firstLon = Math.min(lonSpanNewRow.firstLon, lonSpanCombined.firstLon);
                lonSpanCombined.lastLon = Math.max(lonSpanNewRow.lastLon, lonSpanCombined.lastLon);
            } else if (!lonSpanCombined.ascending && lonSpanCombined.descending) {
                lonSpanCombined.firstLon = Math.max(lonSpanNewRow.firstLon, lonSpanCombined.firstLon);
                lonSpanCombined.lastLon = Math.min(lonSpanNewRow.lastLon, lonSpanCombined.lastLon);
            } else {
                // handle this tougher case or leave with just center values
            }

        } else {
            // handle this tougher case or leave with just center values
        }

        lonSpanCombined.lonSpan = Math.max(lonSpanNewRow.lonSpan, lonSpanCombined.lonSpan);




        if (lonSpanCombined.datelineCrossed || lonSpanNewRow.datelineCrossed) {
            lonSpanCombined.datelineCrossed = true;
        }

        if (lonSpanCombined.ascending || lonSpanNewRow.ascending) {
            lonSpanCombined.ascending = true;
        }

        if (lonSpanCombined.descending || lonSpanNewRow.descending) {
            lonSpanCombined.descending = true;
        }

        if (lonSpanCombined.northPoleCrossed || lonSpanNewRow.northPoleCrossed) {
            lonSpanCombined.northPoleCrossed = true;
        }

        if (lonSpanCombined.southPoleCrossed || lonSpanNewRow.southPoleCrossed) {
            lonSpanCombined.southPoleCrossed = true;
        }


        return lonSpanCombined;
    }





    static GeoSpanLon getLonSpanForRowAtPixelY(GeoCoding geoCoding, RasterDataNode dataNode, int pixelYCurr) {

        PixelPos pixelPosPrev = null;
        GeoPos geoPosPrev = null;
        double degreesSpanTotal = 0.0;

        double NULL_LON = -99999;
        double firstLon = NULL_LON;
        double lastLon = NULL_LON;
        boolean datelineCrossed = false;
        boolean datelineJustCrossedAscending = false;
        boolean datelineJustCrossedDescending = false;

        boolean northPoleCrossed = false;
        boolean southPoleCrossed = false;


        int directionCountMicro = 0;
        DIRECTION directionMicro = DIRECTION.NOT_SET;
        DIRECTION currDirection = DIRECTION.NOT_SET;
        int NUM_CONSECUTIVE_PIXELS_TO_TRIGGER_DIRECTION = 5;


        // if 5 consecutive pixel ascending then trigger ascending
        // if 5 consecutive pixel descending then trigger descending
        boolean ascending = false;
        boolean descending = false;


        double DATELINE_DEGREES_BUFFER = 30;

        for (double pixelXCurr=0.0 ; pixelXCurr <= (dataNode.getRasterWidth()-1) ; pixelXCurr++ ) {

            PixelPos pixelPosCurr = new PixelPos(pixelXCurr, pixelYCurr);
            GeoPos geoPosCurr = geoCoding.getGeoPos(pixelPosCurr, null);

            if (validLon(geoPosCurr.lon)) {
                if (firstLon == NULL_LON) {
                    firstLon = geoPosCurr.lon;
                    lastLon = firstLon;
                } else {
                    if (pixelPosPrev != null && geoPosPrev != null && validLon(geoPosPrev.lon)) {
                        lastLon = geoPosCurr.lon;

                        datelineJustCrossedAscending = false;
                        datelineJustCrossedDescending = false;


                        if (Math.abs(geoPosCurr.lon - geoPosPrev.lon) >= (360 -  DATELINE_DEGREES_BUFFER)) {
                            // dateline likely crossed
                            if (geoPosCurr.lon <= (-180 + DATELINE_DEGREES_BUFFER) && geoPosPrev.lon >= (180 - DATELINE_DEGREES_BUFFER)) {
                                // dateline crossed in ascending direction
                                datelineJustCrossedAscending = true;
                            } else {
                                // dateline crossed in descending direction
                                datelineJustCrossedDescending = true;
                            }

                            datelineCrossed = true;
                        }


                        if (geoPosCurr.lon > geoPosPrev.lon || datelineJustCrossedAscending) {
                            if (directionMicro != DIRECTION.ASCENDING) {
                                directionMicro = DIRECTION.ASCENDING;
                                directionCountMicro = 0;
                            }
                            directionCountMicro++;

                            if (directionCountMicro >= NUM_CONSECUTIVE_PIXELS_TO_TRIGGER_DIRECTION) {
                                if (currDirection != DIRECTION.ASCENDING) {
                                    if (currDirection == DIRECTION.DESCENDING) {
                                        if (geoPosCurr.lat > 70) {
                                            northPoleCrossed = true;  // North Pole
                                        } else if (geoPosCurr.lat < -70) {
                                            southPoleCrossed = true;  // South Pole
                                        }
                                    }

                                    currDirection = DIRECTION.ASCENDING;
                                    ascending = true;
                                }
                            }

                        } else if (geoPosCurr.lon < geoPosPrev.lon || datelineJustCrossedDescending) {
                            if (directionMicro != DIRECTION.DESCENDING) {
                                directionMicro = DIRECTION.DESCENDING;
                                directionCountMicro = 0;
                            }
                            directionCountMicro++;


                            if (directionCountMicro >= NUM_CONSECUTIVE_PIXELS_TO_TRIGGER_DIRECTION) {
                                if (currDirection != DIRECTION.DESCENDING) {
                                    if (currDirection == DIRECTION.ASCENDING) {
                                        if (geoPosCurr.lat > 70) {
                                            northPoleCrossed = true;  // north pole
                                        } else if (geoPosCurr.lat < -70) {
                                            southPoleCrossed = true;  // south pole
                                        }
                                    }

                                    currDirection = DIRECTION.DESCENDING;
                                    descending = true;
                                }
                            }

                        }
                    }
                }

                pixelPosPrev = pixelPosCurr;
                geoPosPrev = geoPosCurr;
            }


        }

        if (ascending && !descending) {
            if (!datelineCrossed) {
                degreesSpanTotal = lastLon - firstLon;
            } else {
                // dateline crossed
                degreesSpanTotal = lastLon - firstLon + 360;
            }
        } else if (!ascending && descending) {
            if (!datelineCrossed) {
                degreesSpanTotal = firstLon - lastLon;
            } else {
                // dateline crossed
                degreesSpanTotal = firstLon - lastLon + 360;
            }
        } else {
            // likely pole crossing
            degreesSpanTotal = 360;
        }


        degreesSpanTotal =  Math.abs(degreesSpanTotal);

        if (firstLon != NULL_LON && lastLon != NULL_LON) {
            GeoSpanLon geoSpanLon = new GeoSpanLon(firstLon, lastLon, degreesSpanTotal, datelineCrossed, northPoleCrossed, southPoleCrossed, ascending, descending);
            return geoSpanLon;
        } else {
            return null;
        }
    }




    static GeoSpanLat getLatSpanForColumnAtPixelX(GeoCoding geoCoding, RasterDataNode dataNode, int pixelXCurr) {
        PixelPos pixelPosPrev = null;
        GeoPos geoPosPrev = null;
        double degreesSpanTotal = 0.0;

        double NULL_LAT = -99999;
        double firstLat = NULL_LAT;
        double lastLat = NULL_LAT;
        boolean northPoleCrossed = false;
        boolean southPoleCrossed = false;

        double minLat = NULL_LAT;
        double maxLat = NULL_LAT;



        int directionCountMicro = 0;
        DIRECTION directionMicro = DIRECTION.NOT_SET;
        DIRECTION currDirection = DIRECTION.NOT_SET;
        int NUM_CONSECUTIVE_PIXELS_TO_TRIGGER_DIRECTION = 5;


        // if 5 consecutive pixel ascending then trigger ascending
        // if 5 consecutive pixel descending then trigger descending
        boolean ascending = false;
        boolean descending = false;


        double height = dataNode.getRasterHeight()-1;
        for (double pixelYCurr=height ; pixelYCurr >= 0.0 ; pixelYCurr-- ) {

            PixelPos pixelPosCurr = new PixelPos(pixelXCurr, pixelYCurr);
            GeoPos geoPosCurr = geoCoding.getGeoPos(pixelPosCurr, null);

            if (validLat(geoPosCurr.lat)) {
                if (minLat == NULL_LAT || minLat > geoPosCurr.lat ) {
                    minLat = geoPosCurr.lat;
                }

                if (maxLat == NULL_LAT || maxLat < geoPosCurr.lat ) {
                    maxLat = geoPosCurr.lat;
                }

                if (firstLat == NULL_LAT) {
                    firstLat = geoPosCurr.lat;
                    lastLat = firstLat;
                } else {
                    if (pixelPosPrev != null && geoPosPrev != null) {
                        lastLat = geoPosCurr.lat;

                        if (geoPosCurr.lat > geoPosPrev.lat) {
                            if (directionMicro != DIRECTION.ASCENDING) {
                                directionMicro = DIRECTION.ASCENDING;
                                directionCountMicro = 0;
                            }
                            directionCountMicro++;

                            if (directionCountMicro >= NUM_CONSECUTIVE_PIXELS_TO_TRIGGER_DIRECTION) {
                                if (currDirection != DIRECTION.ASCENDING) {
                                    if (currDirection == DIRECTION.DESCENDING && geoPosCurr.lat < -60) {
                                        southPoleCrossed = true;  // south pole
                                    }

                                    currDirection = DIRECTION.ASCENDING;
                                    ascending = true;
                                }
                            }

                        } else if (geoPosCurr.lat < geoPosPrev.lat) {
                            if (directionMicro != DIRECTION.DESCENDING) {
                                directionMicro = DIRECTION.DESCENDING;
                                directionCountMicro = 0;
                            }
                            directionCountMicro++;

                            if (directionCountMicro >= NUM_CONSECUTIVE_PIXELS_TO_TRIGGER_DIRECTION) {
                                if (currDirection != DIRECTION.DESCENDING) {
                                    if (currDirection == DIRECTION.ASCENDING && geoPosCurr.lat > 70) {
                                        northPoleCrossed = true;  // north pole
                                    }

                                    currDirection = DIRECTION.DESCENDING;
                                    descending = true;
                                }
                            }

                        }

                    }
                }

                pixelPosPrev = pixelPosCurr;
                geoPosPrev = geoPosCurr;
            }

        }

        if (northPoleCrossed) {
            degreesSpanTotal = Math.abs(90 - lastLat) + Math.abs(90 - firstLat);
        } else if (southPoleCrossed) {
            degreesSpanTotal = Math.abs(-90 - lastLat) + Math.abs(-90 - firstLat);
        } else {
            degreesSpanTotal = Math.abs(lastLat - firstLat);
        }


        if (firstLat != NULL_LAT && lastLat != NULL_LAT) {
            GeoSpanLat geoSpanLat = new GeoSpanLat(firstLat, lastLat, minLat, maxLat, degreesSpanTotal, northPoleCrossed, southPoleCrossed, ascending, descending);
            return geoSpanLat;
        } else {
            return null;
        }
    }











    static GeoSpan getGeoSpanScene(GeoCoding geoCoding, RasterDataNode dataNode) {


        GeoSpan geoSpan = new GeoSpan();
        

        boolean forceCheckForPolar = false;

        GeoSpanLon lonSpanScene = getLonSpan(geoCoding, dataNode);

        if (lonSpanScene != null && lonSpanScene.lonSpan > 0) {
            GeoSpanLat latSpanScene = getLatSpan(geoCoding, dataNode);

            if (latSpanScene != null && latSpanScene.latSpan > 0) {
                // todo add in a field to geospan regarding whether corner pixels have valid geo (this could be used for auto-border since if corner pixels are invalid a border might not be desired)

                geoSpan.lonSpan = lonSpanScene.lonSpan;
                geoSpan.firstLon = lonSpanScene.firstLon;
                geoSpan.lastLon = lonSpanScene.lastLon;
                geoSpan.lonAscending = lonSpanScene.ascending;
                geoSpan.lonDescending = lonSpanScene.descending;

                geoSpan.datelineCrossed = lonSpanScene.datelineCrossed;

                geoSpan.latSpan = latSpanScene.latSpan;
                geoSpan.bottomLat = latSpanScene.firstLat;
                geoSpan.topLat = latSpanScene.lastLat;
                geoSpan.minLat = latSpanScene.minLat;
                geoSpan.maxLat = latSpanScene.maxLat;
                geoSpan.latAscending = latSpanScene.ascending;
                geoSpan.latDescending = latSpanScene.descending;

                geoSpan.northPoleCrossed = lonSpanScene.northPoleCrossed || latSpanScene.northPoleCrossed;
                geoSpan.southPoleCrossed = lonSpanScene.southPoleCrossed || latSpanScene.southPoleCrossed;


                geoSpan.equidistantCylindrical = isEquidistantCylindrical(geoCoding,  dataNode);

                if (geoSpan.northPoleCrossed || geoSpan.southPoleCrossed || forceCheckForPolar) {
                    geoSpan.coordsPolar = getCoordsPolar(geoCoding, dataNode);

                    if (geoSpan.coordsPolar != null) {
                        if (geoSpan.coordsPolar.coordNorthernmost != null) {

                            if (geoSpan.coordsPolar.coordNorthernmost.geoPos != null) {
                                geoSpan.maxLat = geoSpan.coordsPolar.coordNorthernmost.geoPos.lat;
                            }

                            if (geoSpan.coordsPolar.northPoleCrossingDetected) {
                                geoSpan.northPoleCrossed = true;
                            }
                        }

                        if (geoSpan.coordsPolar.coordSouthernmost != null) {

                            if (geoSpan.coordsPolar.coordSouthernmost.geoPos != null) {
                                geoSpan.minLat = geoSpan.coordsPolar.coordSouthernmost.geoPos.lat;
                            }

                            if (geoSpan.coordsPolar.southPoleCrossingDetected) {
                                geoSpan.southPoleCrossed = true;
                            }
                        }

                    }
                }

            }
        }



        return geoSpan;
    }




    static boolean validLatSpan(GeoSpanLat geoSpanLat) {

        boolean valid = (geoSpanLat.latSpan > 0 &&
                validLat(geoSpanLat.firstLat) &&
                validLat(geoSpanLat.lastLat) &&
                validLat(geoSpanLat.minLat) &&
                validLat(geoSpanLat.maxLat));

        return valid;

    }



    static GeoSpanLat combineLatSpans(GeoSpanLat latSpanCombined, GeoSpanLat latSpanNewColumn) {


        boolean newRowValid =  validLatSpan(latSpanNewColumn);
        if (!newRowValid) {
            return latSpanCombined;
        }


        boolean combinedValid = validLatSpan(latSpanCombined);
        if (!combinedValid) {
            GeoSpanLat.GeoSpanLatUpdate(latSpanCombined, latSpanNewColumn);
            return latSpanCombined;
        }



        // At this point both entries are valid



        if ((latSpanCombined.northPoleCrossed == latSpanNewColumn.northPoleCrossed) &&
                (latSpanCombined.southPoleCrossed == latSpanNewColumn.southPoleCrossed)) {

            latSpanCombined.firstLat = Math.min(latSpanNewColumn.firstLat, latSpanCombined.firstLat);

            // todo add into GeoSceneInfo directions of "ASCENDING, DESCENDING, ASCENDING_THEN_DESCENDING, DESCENDING_THEN_ASCENDING"
            if (latSpanCombined.ascending && !latSpanCombined.descending) {
                latSpanCombined.firstLat = Math.min(latSpanNewColumn.firstLat, latSpanCombined.firstLat);
                latSpanCombined.lastLat = Math.max(latSpanNewColumn.lastLat, latSpanCombined.lastLat);
            } else if (!latSpanCombined.ascending && latSpanCombined.descending) {
                latSpanCombined.firstLat = Math.max(latSpanNewColumn.firstLat, latSpanCombined.firstLat);
                latSpanCombined.lastLat = Math.min(latSpanNewColumn.lastLat, latSpanCombined.lastLat);
            } else {
                // handle this tougher case or leave with just center values
                latSpanCombined.firstLat = Math.min(latSpanNewColumn.firstLat, latSpanCombined.firstLat);
                latSpanCombined.lastLat = Math.min(latSpanNewColumn.lastLat, latSpanCombined.lastLat);
            }

            latSpanCombined.latSpan = Math.max(latSpanNewColumn.latSpan, latSpanCombined.latSpan);
        } else {
            // handle this tougher case or leave with just center values
        }

        latSpanCombined.latSpan = Math.max(latSpanNewColumn.latSpan, latSpanCombined.latSpan);



        latSpanCombined.minLat = Math.min(latSpanNewColumn.minLat, latSpanCombined.minLat);
        if (latSpanCombined.minLat < -90) {
            latSpanCombined.minLat = -90;
        }


        latSpanCombined.maxLat = Math.max(latSpanNewColumn.maxLat, latSpanCombined.maxLat);
        if (latSpanCombined.maxLat > 90) {
            latSpanCombined.maxLat = 90;
        }


        if (latSpanCombined.ascending || latSpanNewColumn.ascending) {
            latSpanCombined.ascending = true;
        }

        if (latSpanCombined.descending || latSpanNewColumn.descending) {
            latSpanCombined.descending = true;
        }


        if (latSpanCombined.northPoleCrossed || latSpanNewColumn.northPoleCrossed) {
            latSpanCombined.northPoleCrossed = true;
        }

        if (latSpanCombined.southPoleCrossed || latSpanNewColumn.southPoleCrossed) {
            latSpanCombined.southPoleCrossed = true;
        }


        return latSpanCombined;
    }




    static GeoSpanLat getLatSpan(GeoCoding geoCoding, RasterDataNode dataNode) {

        GeoSpanLat latSpanCombined = new GeoSpanLat();


        int pixelXCenterColumn = (int) Math.floor ((dataNode.getRasterWidth()-1) * 0.5);
//        int centerWidth = (int) Math.floor(dataNode.getRasterWidth() / 2.0);
        GeoSpanLat latSpanCenterColumn = getLatSpanForColumnAtPixelX(geoCoding,  dataNode,  pixelXCenterColumn);
        if (latSpanCenterColumn != null) {
            combineLatSpans(latSpanCombined, latSpanCenterColumn);
        }



        // todo testing new
        int pixelXNearLeftColumnPixelX = (int) Math.floor ((dataNode.getRasterWidth()-1) * 0.25);
        GeoSpanLat latSpanNearLeftColumn = getLatSpanForColumnAtPixelX(geoCoding,  dataNode,  pixelXNearLeftColumnPixelX);
        if (latSpanNearLeftColumn != null) {
            combineLatSpans(latSpanCombined, latSpanNearLeftColumn);
        }

        // todo testing new
        int pixelXNearRightColumnPixelX = (int) Math.floor ((dataNode.getRasterWidth()-1) * 0.75);
        GeoSpanLat latSpanNearRightColumn = getLatSpanForColumnAtPixelX(geoCoding,  dataNode,  pixelXNearRightColumnPixelX);
        if (latSpanNearLeftColumn != null) {
            combineLatSpans(latSpanCombined, latSpanNearRightColumn);
        }



        int pixelXLeftColumn = 0;
        GeoSpanLat latSpanLeftColumn = getLatSpanForColumnAtPixelX(geoCoding,  dataNode,  pixelXLeftColumn);
        if (latSpanLeftColumn != null) {
            combineLatSpans(latSpanCombined, latSpanLeftColumn);
        }


        int pixelXRightColumn = dataNode.getRasterWidth() - 1;
        GeoSpanLat latSpanRightColumn = getLatSpanForColumnAtPixelX(geoCoding,  dataNode,  pixelXRightColumn);
        if (latSpanRightColumn != null) {
            combineLatSpans(latSpanCombined, latSpanRightColumn);
        }



        if (validLatSpan(latSpanCombined)) {

            // Calculate latSpan and compare against largest individual column latSpan

            double latSpanCalculated = 0;

            if (latSpanCombined.northPoleCrossed) {
                latSpanCalculated = Math.abs(90 - latSpanCombined.lastLat) + Math.abs(90 - latSpanCombined.firstLat);
            } else if (latSpanCombined.southPoleCrossed) {
                // todo fix
                latSpanCalculated = Math.abs(-90 - latSpanCombined.lastLat) + Math.abs(-90 - latSpanCombined.firstLat);

            } else {
                if (latSpanCombined.ascending) {
                    latSpanCalculated = latSpanCombined.lastLat - latSpanCombined.firstLat;
                } else {
                    latSpanCalculated = latSpanCombined.firstLat - latSpanCombined.lastLat;
                }
            }


            latSpanCombined.latSpan = Math.max(latSpanCombined.latSpan, latSpanCalculated);

        }



        if (validLatSpan(latSpanCombined)) {
            return latSpanCombined;
        } else {
            return null;
        }

    }




    static GeoSpanLon getLonSpan(GeoCoding geoCoding, RasterDataNode dataNode) {

        GeoSpanLon lonSpanCombined = new GeoSpanLon();


        int pixelYCenterRow = (int) Math.floor ((dataNode.getRasterHeight()-1) * 0.5);
//        int centerHeight = (int) Math.floor(dataNode.getRasterHeight() * 0.5);
        GeoSpanLon lonSpanCenterRow = getLonSpanForRowAtPixelY(geoCoding,  dataNode,  pixelYCenterRow);
        if (lonSpanCenterRow != null) {
            combineLonSpans(lonSpanCombined, lonSpanCenterRow);
        }


        // todo testing new
        int pixelYNearTopRow = (int) Math.floor ((dataNode.getRasterHeight()-1) * 0.25);
        GeoSpanLon lonSpanNearTopRow = getLonSpanForRowAtPixelY(geoCoding,  dataNode,  pixelYNearTopRow);
        if (lonSpanNearTopRow != null) {
            combineLonSpans(lonSpanCombined, lonSpanNearTopRow);
        }

        // todo testing new
        int pixelYNearBottomRow = (int) Math.floor ((dataNode.getRasterHeight()-1) * 0.75);
        GeoSpanLon lonSpanNearBottomRow = getLonSpanForRowAtPixelY(geoCoding,  dataNode,  pixelYNearBottomRow);
        if (lonSpanNearTopRow != null) {
            combineLonSpans(lonSpanCombined, lonSpanNearBottomRow);
        }



        int pixelYTopRow = 0;
        GeoSpanLon lonSpanTopRow = getLonSpanForRowAtPixelY(geoCoding,  dataNode,  pixelYTopRow);
        if (lonSpanTopRow != null) {
            combineLonSpans(lonSpanCombined, lonSpanTopRow);
        }


        int pixelYBottomRow = dataNode.getRasterHeight()-1;
        GeoSpanLon lonSpanBottomRow = getLonSpanForRowAtPixelY(geoCoding,  dataNode,  pixelYBottomRow);
        if (lonSpanBottomRow != null) {
            combineLonSpans(lonSpanCombined, lonSpanBottomRow);
        }



        if (validLonSpan(lonSpanCombined)) {

            // Calculate lonSpan and compare against largest individual row lonSpan

            double lonSpanCalculated = 0;

            if (lonSpanCombined.ascending && !lonSpanCombined.descending) {
                if (!lonSpanCombined.datelineCrossed) {
                    lonSpanCalculated = lonSpanCombined.lastLon - lonSpanCombined.firstLon;
                } else {
                    // dateline crossed
                    lonSpanCalculated = lonSpanCombined.lastLon - lonSpanCombined.firstLon + 360;
                }
            } else if (!lonSpanCombined.ascending && lonSpanCombined.descending) {
                if (!lonSpanCombined.datelineCrossed) {
                    lonSpanCalculated = lonSpanCombined.firstLon - lonSpanCombined.lastLon;
                } else {
                    // dateline crossed
                    lonSpanCalculated = lonSpanCombined.firstLon - lonSpanCombined.lastLon + 360;
                }
            } else {
                // likely pole crossing
                lonSpanCalculated = 360;
            }

            lonSpanCalculated =  Math.abs(lonSpanCalculated);


            lonSpanCombined.lonSpan = Math.max(lonSpanCalculated, lonSpanCombined.lonSpan);

        }



        if (validLonSpan(lonSpanCombined)) {
            return lonSpanCombined;
        } else {
            return null;
        }

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

    static TextGlyph createTextGlyph(String text, Coord coord1, Coord coord2, double offsetX, double offsetY) {
        final double angle = Math.atan2(coord2.pixelPos.y - coord1.pixelPos.y,
                coord2.pixelPos.x - coord1.pixelPos.x);
        return new TextGlyph(text, coord1.pixelPos.x + offsetX, coord1.pixelPos.y + offsetY, angle);
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



    static class CoordsPolar {

        Coord coordNorthernmost;
        Coord coordSouthernmost;
        boolean northernmostPixelOnRasterEdge;
        boolean southernmostPixelOnRasterEdge;
        boolean northPoleCrossingDetected;
        boolean southPoleCrossingDetected;

        public CoordsPolar(Coord coordNorthernmost,
                           Coord coordSouthernmost,
                           boolean northernmostPixelOnRasterEdge,
                           boolean southernmostPixelOnRasterEdge,
                           boolean northPoleCrossingDetected,
                           boolean southPoleCrossingDetected) {
            this.coordNorthernmost = coordNorthernmost;
            this.coordSouthernmost = coordSouthernmost;
            this.northernmostPixelOnRasterEdge = northernmostPixelOnRasterEdge;
            this.southernmostPixelOnRasterEdge = southernmostPixelOnRasterEdge;
            this.northPoleCrossingDetected = northPoleCrossingDetected;
            this.southPoleCrossingDetected = southPoleCrossingDetected;
        }
    }




    static class GeoSpanLon {
        double firstLon;
        double lastLon;
        double lonSpan;
        boolean datelineCrossed;
        boolean northPoleCrossed;
        boolean southPoleCrossed;
        boolean ascending;
        boolean descending;


        public GeoSpanLon() {
            this.firstLon = NULL_LON;
            this.lastLon = NULL_LON;
            this.lonSpan = 0;
            this.datelineCrossed = false;
            this.northPoleCrossed = false;
            this.southPoleCrossed = false;
            this.ascending = false;
            this.descending = false;
        }

        public GeoSpanLon(double firstLon,
                          double lastLon,
                          double lonSpan,
                          boolean datelineCrossed,
                          boolean northPoleCrossed,
                          boolean southPoleCrossed,
                          boolean ascending,
                          boolean descending) {
            this.firstLon = firstLon;
            this.lastLon = lastLon;
            this.lonSpan = lonSpan;
            this.datelineCrossed = datelineCrossed;
            this.northPoleCrossed = northPoleCrossed;
            this.southPoleCrossed = southPoleCrossed;
            this.ascending = ascending;
            this.descending = descending;
        }


        public static GeoSpanLon GeoSpanLonCopy(GeoSpanLon geoSpanLonOriginal) {

            if (geoSpanLonOriginal == null) {
                return null;
            }

            GeoSpanLon geoSpanLonCopy = new GeoSpanLon();

            geoSpanLonCopy.firstLon = geoSpanLonOriginal.firstLon;
            geoSpanLonCopy.lastLon = geoSpanLonOriginal.lastLon;
            geoSpanLonCopy.lonSpan = geoSpanLonOriginal.lonSpan;
            geoSpanLonCopy.datelineCrossed = geoSpanLonOriginal.datelineCrossed;
            geoSpanLonCopy.northPoleCrossed = geoSpanLonOriginal.northPoleCrossed;
            geoSpanLonCopy.southPoleCrossed = geoSpanLonOriginal.southPoleCrossed;
            geoSpanLonCopy.ascending = geoSpanLonOriginal.ascending;
            geoSpanLonCopy.descending = geoSpanLonOriginal.descending;

            return geoSpanLonCopy;
        }

        public static void GeoSpanLonUpdate(GeoSpanLon geoSpanLonTarget,  GeoSpanLon geoSpanLonOriginal) {

            if (geoSpanLonOriginal == null) {
                return;
            }

            geoSpanLonTarget.firstLon = geoSpanLonOriginal.firstLon;
            geoSpanLonTarget.lastLon = geoSpanLonOriginal.lastLon;
            geoSpanLonTarget.lonSpan = geoSpanLonOriginal.lonSpan;
            geoSpanLonTarget.datelineCrossed = geoSpanLonOriginal.datelineCrossed;
            geoSpanLonTarget.northPoleCrossed = geoSpanLonOriginal.northPoleCrossed;
            geoSpanLonTarget.southPoleCrossed = geoSpanLonOriginal.southPoleCrossed;
            geoSpanLonTarget.ascending = geoSpanLonOriginal.ascending;
            geoSpanLonTarget.descending = geoSpanLonOriginal.descending;
        }
    }



    static class GeoSpanLat {
        double firstLat;
        double lastLat;
        double minLat;
        double maxLat;
        double latSpan;
        boolean northPoleCrossed;
        boolean southPoleCrossed;
        boolean ascending;
        boolean descending;

        public GeoSpanLat() {
            this.firstLat = NULL_LAT;
            this.lastLat = NULL_LAT;
            this.minLat = NULL_LAT;
            this.maxLat = NULL_LAT;
            this.latSpan = 0;
            this.northPoleCrossed = false;
            this.southPoleCrossed = false;
            this.ascending = false;
            this.descending = false;
        }

        public GeoSpanLat(double firstLat,
                          double lastLat,
                          double minLat,
                          double maxLat,
                          double latSpan,
                          boolean northPoleCrossed,
                          boolean southPoleCrossed,
                          boolean ascending,
                          boolean descending) {
            this.firstLat = firstLat;
            this.lastLat = lastLat;
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.latSpan = latSpan;
            this.northPoleCrossed = northPoleCrossed;
            this.southPoleCrossed = southPoleCrossed;
            this.ascending = ascending;
            this.descending = descending;
        }

        public static GeoSpanLat GeoSpanLatCopy(GeoSpanLat geoSpanLatOriginal) {

            if (geoSpanLatOriginal == null) {
                return null;
            }

            GeoSpanLat geoSpanLatCopy = new GeoSpanLat();

            geoSpanLatCopy.firstLat = geoSpanLatOriginal.firstLat;
            geoSpanLatCopy.lastLat = geoSpanLatOriginal.lastLat;
            geoSpanLatCopy.minLat = geoSpanLatOriginal.minLat;
            geoSpanLatCopy.maxLat = geoSpanLatOriginal.maxLat;
            geoSpanLatCopy.latSpan = geoSpanLatOriginal.latSpan;
            geoSpanLatCopy.northPoleCrossed = geoSpanLatOriginal.northPoleCrossed;
            geoSpanLatCopy.southPoleCrossed = geoSpanLatOriginal.southPoleCrossed;
            geoSpanLatCopy.ascending = geoSpanLatOriginal.ascending;
            geoSpanLatCopy.descending = geoSpanLatOriginal.descending;

            return geoSpanLatCopy;
        }


        public static void GeoSpanLatUpdate(GeoSpanLat geoSpanLatTarget,  GeoSpanLat geoSpanLatOriginal) {

            if (geoSpanLatOriginal == null) {
                return;
            }

            geoSpanLatTarget.firstLat = geoSpanLatOriginal.firstLat;
            geoSpanLatTarget.lastLat = geoSpanLatOriginal.lastLat;
            geoSpanLatTarget.minLat = geoSpanLatOriginal.minLat;
            geoSpanLatTarget.maxLat = geoSpanLatOriginal.maxLat;
            geoSpanLatTarget.latSpan = geoSpanLatOriginal.latSpan;
            geoSpanLatTarget.northPoleCrossed = geoSpanLatOriginal.northPoleCrossed;
            geoSpanLatTarget.southPoleCrossed = geoSpanLatOriginal.southPoleCrossed;
            geoSpanLatTarget.ascending = geoSpanLatOriginal.ascending;
            geoSpanLatTarget.descending = geoSpanLatOriginal.descending;
        }


    }






    static class GeoSpan {
        double firstLon;
        double lastLon;
        double lonSpan;

        double bottomLat;
        double topLat;
        double minLat;
        double maxLat;
        double latSpan;

        boolean datelineCrossed;
        boolean northPoleCrossed;
        boolean southPoleCrossed;

        boolean lonAscending;
        boolean lonDescending;
        boolean latAscending;
        boolean latDescending;

        boolean equidistantCylindrical;
        CoordsPolar coordsPolar;


        public GeoSpan() {
            firstLon = NULL_LON;
            lastLon = NULL_LON;
            lonSpan = 0;

            bottomLat = NULL_LAT;
            topLat = NULL_LAT;
            minLat = NULL_LAT;
            maxLat = NULL_LAT;
            latSpan = 0;

            datelineCrossed = false;
            northPoleCrossed = false;
            southPoleCrossed = false;

            lonAscending = false;
            lonDescending = false;
            latAscending = false;
            latDescending = false;

            equidistantCylindrical = false;
            coordsPolar = null;
        }


        public GeoSpan(double firstLon,
                       double lastLon,
                       double lonSpan,
                       boolean datelineCrossed,
                       double bottomLat,
                       double topLat,
                       double minLat,
                       double maxLat,
                       double latSpan,
                       boolean northPoleCrossed,
                       boolean southPoleCrossed,
                       boolean lonAscending,
                       boolean lonDescending,
                       boolean latAscending,
                       boolean latDescending,
                       boolean equidistantCylindrical,
                       CoordsPolar coordsPolar) {
            this.firstLon = firstLon;
            this.lastLon = lastLon;
            this.lonSpan = lonSpan;
            this.bottomLat = bottomLat;
            this.topLat = topLat;
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.latSpan = latSpan;
            this.datelineCrossed = datelineCrossed;
            this.northPoleCrossed = northPoleCrossed;
            this.southPoleCrossed = southPoleCrossed;
            this.lonAscending = lonAscending;
            this.lonDescending = lonDescending;
            this.latAscending = latAscending;
            this.latDescending = latDescending;
            this.equidistantCylindrical = equidistantCylindrical;
            this.coordsPolar = coordsPolar;

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

