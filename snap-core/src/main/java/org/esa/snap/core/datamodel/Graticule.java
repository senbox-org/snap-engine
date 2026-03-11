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

import org.esa.snap.core.layer.GraticuleLayerType;
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
    private final GeoInfo _geoInfo;
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
                      boolean flippedLons,
                      GeoInfo geoInfo
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
        _geoInfo = geoInfo;
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

    public GeoInfo getGeoInfo() {
        return _geoInfo;
    }

    /**
     * Creates a graticule for the given product.
     *
     * @param raster              the product
     * @param graticuleParameters input parameters
     * @return the graticule or null, if it could not be created
     */
    public static Graticule create(RasterDataNode raster,
                                   GraticuleParameters graticuleParameters) {

        if (graticuleParameters == null) {
            return null;
        }

        String mode = graticuleParameters.mode;

        int desiredNumGridLines = graticuleParameters.desiredNumGridLines;
        int desiredMinorSteps = graticuleParameters.desiredMinorSteps;
        int desiredMinorStepsCylindrical = graticuleParameters.desiredMinorStepsCylindrical;

        double latMajorStep = graticuleParameters.gridSpacingLat;
        double lonMajorStep = graticuleParameters.gridSpacingLon;

        boolean interpolate = graticuleParameters.interpolate;
        double toleranceParallels = graticuleParameters.toleranceParallels;
        double toleranceMeridians = graticuleParameters.toleranceMeridians;
        boolean formatCompass = graticuleParameters.formatCompass;
        boolean decimalFormat = graticuleParameters.decimalFormat;
        int spacer = graticuleParameters.spacer;


        Guardian.assertNotNull("product", raster);
        final GeoCoding geoCoding = raster.getGeoCoding();
        if (geoCoding == null || raster.getRasterWidth() < 16 || raster.getRasterHeight() < 16) {
            return null;
        }


        if (desiredNumGridLines <= 1) {
            desiredNumGridLines = 2;
        }


        boolean forceCheckForPolar = false;
        final GeoInfo geoInfo = new GeoInfo(geoCoding, raster, forceCheckForPolar);


        if (latMajorStep == 0 || lonMajorStep == 0) {

            int numGridlinesLat = desiredNumGridLines;
            int numGridlinesLon = desiredNumGridLines;

            boolean globalLat = false;
            boolean globalLon = false;
            boolean halfGlobalLon = false;


            // todo investigate mode

            if (GraticuleLayerType.MODE_GLOBAL.equals(mode)
                    || GraticuleLayerType.MODE_GLOBAL_CYLINDRICAL.equals(mode)) {
                globalLat = true;
                globalLon = true;
            } else if (GraticuleLayerType.MODE_HEMISPHERICAL.equals(mode)) {
                halfGlobalLon = true;
            } else if (GraticuleLayerType.MODE_REGIONAL.equals(mode)) {
                globalLat = false;
                globalLon = false;
                halfGlobalLon = false;
            } else {

                if (geoInfo.latSpan >= 75) {
                    globalLat = true;
                }

                if (geoInfo.lonSpan >= 200) {
                    globalLon = true;
                } else if (geoInfo.lonSpan >= 75) {
                    halfGlobalLon = true;
                }
            }

//            double MAX_LAT_MAJOR_STEP_DEFAULT = 30;
//            double MAX_LON_MAJOR_STEP_DEFAULT = 45;
            double MAX_LAT_MAJOR_STEP_DEFAULT = 90;
            double MAX_LON_MAJOR_STEP_DEFAULT = 180;

            double MAX_LAT_STEP_POLAR = 10;
            double MAX_LON_STEP_POLAR = 15;


            double LAT_STEP_GLOBAL = graticuleParameters.autoSpacingLatGlobal;
            double LON_STEP_GLOBAL = graticuleParameters.autoSpacingLonGlobal;
            double LON_STEP_HALF_GLOBAL = graticuleParameters.autoSpacingLonHemispherical;
            double LAT_STEP_HALF_GLOBAL = graticuleParameters.autoSpacingLatHemispherical;
            double LAT_STEP_GLOBAL_CYLINDRICAL = graticuleParameters.autoSpacingLatGlobalCylindrical;
            double LON_STEP_GLOBAL_CYLINDRICAL = graticuleParameters.autoSpacingLonGlobalCylindrical;


            double LON_STEP_HALF_GLOBAL_CYLINDRICAL = LON_STEP_GLOBAL_CYLINDRICAL;
            double LAT_STEP_HALF_GLOBAL_CYLINDRICAL = LAT_STEP_GLOBAL_CYLINDRICAL;


            if (latMajorStep == 0) {
                if (globalLat) {
                    if (geoInfo.cylindrical || GraticuleLayerType.MODE_GLOBAL_CYLINDRICAL.equals(mode)) {
                        latMajorStep = LAT_STEP_GLOBAL_CYLINDRICAL;
                    } else {
                        latMajorStep = LAT_STEP_GLOBAL;
                    }
                } else if (halfGlobalLon) {
                    if (geoInfo.cylindrical || GraticuleLayerType.MODE_GLOBAL_CYLINDRICAL.equals(mode)) {
                        latMajorStep = LAT_STEP_HALF_GLOBAL_CYLINDRICAL;
                    } else {
                        latMajorStep = LAT_STEP_HALF_GLOBAL;
                    }
                } else {
                    double tmpLatMajorStep = geoInfo.latSpan / numGridlinesLat;
                    latMajorStep = getSensibleDegreeIncrement(tmpLatMajorStep);

                    if (latMajorStep >= MAX_LAT_MAJOR_STEP_DEFAULT) {
                        latMajorStep = MAX_LAT_MAJOR_STEP_DEFAULT;
                    }
                }

                if (geoInfo.southPoleCrossed || geoInfo.northPoleCrossed) {
                    if (latMajorStep > MAX_LAT_STEP_POLAR) {
                        latMajorStep = MAX_LAT_STEP_POLAR;
                    }
                }

            }


            if (lonMajorStep == 0) {
                if (globalLon) {
                    if (geoInfo.cylindrical || GraticuleLayerType.MODE_GLOBAL_CYLINDRICAL.equals(mode)) {
                        lonMajorStep = LON_STEP_GLOBAL_CYLINDRICAL;
                    } else {
                        lonMajorStep = LON_STEP_GLOBAL;
                    }
                } else if (halfGlobalLon) {
                    if (geoInfo.cylindrical || GraticuleLayerType.MODE_GLOBAL_CYLINDRICAL.equals(mode)) {
                        lonMajorStep = LON_STEP_HALF_GLOBAL_CYLINDRICAL;
                    } else {
                        lonMajorStep = LON_STEP_HALF_GLOBAL;
                    }
                } else {
                    double tmpLonMajorStep = geoInfo.lonSpan / numGridlinesLon;
                    lonMajorStep = getSensibleDegreeIncrement(tmpLonMajorStep);

                    if (lonMajorStep >= MAX_LON_MAJOR_STEP_DEFAULT) {
                        lonMajorStep = MAX_LON_MAJOR_STEP_DEFAULT;
                    }

                }

                if (geoInfo.southPoleCrossed || geoInfo.northPoleCrossed) {
                    if (lonMajorStep > MAX_LON_STEP_POLAR) {
                        lonMajorStep = MAX_LON_STEP_POLAR;
                    }
                }


            }


            boolean matchLatLon = false;

            if (latMajorStep == 0 && lonMajorStep == 0) {
                if (!geoInfo.southPoleCrossed && !geoInfo.northPoleCrossed && !globalLon && !globalLat && !halfGlobalLon) {
                    matchLatLon = true;
                }
            }


            if (matchLatLon) {
                double minMajorStep = Math.min(latMajorStep, lonMajorStep);
                latMajorStep = minMajorStep;
                lonMajorStep = minMajorStep;

                if (latMajorStep > MAX_LAT_MAJOR_STEP_DEFAULT) {
                    latMajorStep = MAX_LAT_MAJOR_STEP_DEFAULT;
                }
            }

            // final sanity check because step sizes cannot be zero
            if (latMajorStep == 0) {
                latMajorStep = ONE_MINUTE;
            }
            if (lonMajorStep == 0) {
                lonMajorStep = ONE_MINUTE;
            }

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


//
//        if (geoInfo.cylindrical || GraticuleLayerType.MODE_GLOBAL_CYLINDRICAL.equals(mode)) {
////            tolerance = toleranceCylindrical;
//            desiredMinorSteps = desiredMinorStepsCylindrical;
//        } else if (geoInfo.southPoleCrossed || geoInfo.northPoleCrossed) {
//            // todo TESTING
////            if (desiredMinorSteps < 256) {
////                desiredMinorSteps = 256;
////            }
////            tolerance = 0;
//        }

        // todo TEMP look into this more   maybe added something in preferences
        if (geoInfo.southPoleCrossed || geoInfo.northPoleCrossed) {
            toleranceParallels = 0.0;
            toleranceMeridians = 0.0;
        }



        int meridianSmoothingSteps = desiredMinorSteps;
        int parallelSmoothingSteps = desiredMinorSteps;

        if (geoInfo.parallelConstant) {
            parallelSmoothingSteps = desiredMinorStepsCylindrical;
        }

        if (geoInfo.meridianConstant) {
            meridianSmoothingSteps = desiredMinorStepsCylindrical;
        }


        final List<List<Coord>> meridiansList = computeMeridiansList(lonMajorStep, meridianSmoothingSteps, raster, geoInfo, toleranceMeridians, interpolate);
        final List<List<Coord>> parallelsList = computeParallelsList(latMajorStep, parallelSmoothingSteps, raster, geoInfo, toleranceParallels, interpolate);


//        final List<List<Coord>> meridianList = computeMeridianList(raster.getGeoCoding(), null, lonMajorStep, latMinorStep,
//                0.0, 0.0, raster);
//        final List<List<Coord>> parallelList = computeParallelList(raster.getGeoCoding(), null, latMajorStep, lonMinorStep,
//                0.0, 0.0, raster);


        // todo maybe make this ||
        if (parallelsList.size() > 0 || meridiansList.size() > 0) {
            final GeneralPath[] meridiansPaths = createPathsNew(meridiansList);
            final GeneralPath[] parallelsPaths = createPathsNew(parallelsList);

            final TextGlyph[] textGlyphsNorth = createTextGlyphs(parallelsList, meridiansList, TextLocation.NORTH, formatCompass, decimalFormat, lonMajorStep, raster, spacer, geoInfo);
            final TextGlyph[] textGlyphsSouth = createTextGlyphs(parallelsList, meridiansList, TextLocation.SOUTH, formatCompass, decimalFormat, lonMajorStep, raster, spacer, geoInfo);
            final TextGlyph[] textGlyphsWest = createTextGlyphs(parallelsList, meridiansList, TextLocation.WEST, formatCompass, decimalFormat, latMajorStep, raster, spacer, geoInfo);
            final TextGlyph[] textGlyphsEast = createTextGlyphs(parallelsList, meridiansList, TextLocation.EAST, formatCompass, decimalFormat, latMajorStep, raster, spacer, geoInfo);

            final TextGlyph[] textGlyphsLatCorners = createLatCornerTextGlyphs(raster, formatCompass, decimalFormat);
            final TextGlyph[] textGlyphsLonCorners = createLonCornerTextGlyphs(raster, formatCompass, decimalFormat);


            final PixelPos[] tickPointsNorth = createTickPointsByGlyph(parallelsList, meridiansList, textGlyphsNorth, TextLocation.NORTH, geoInfo);
            final PixelPos[] tickPointsSouth = createTickPointsByGlyph(parallelsList, meridiansList, textGlyphsSouth, TextLocation.SOUTH, geoInfo);
            final PixelPos[] tickPointsWest = createTickPointsByGlyph(parallelsList, meridiansList, textGlyphsWest, TextLocation.WEST, geoInfo);
            final PixelPos[] tickPointsEast = createTickPointsByGlyph(parallelsList, meridiansList, textGlyphsEast, TextLocation.EAST, geoInfo);

            int buffer = 5;
            for (int i=0; i < textGlyphsWest.length; i++) {
                if (textGlyphsWest[i].getY() < (0 + buffer) || textGlyphsWest[i].getY() > (raster.getRasterHeight() - buffer)) {
                    if (!geoInfo.containsValidGeoCorners && !geoInfo.northPoleCrossed && !geoInfo.southPoleCrossed) {
                        if (textGlyphsWest[i].getX() > (0 + buffer)) {
                            textGlyphsWest[i].x = textGlyphsWest[i].x - spacer;  // todo TEMP testing
                        }

                        if (textGlyphsEast[i].getX() > (raster.getRasterWidth() - buffer)) {
                            textGlyphsEast[i].x = textGlyphsEast[i].x + spacer;  // todo TEMP testing
                        }
                    }

                }
            }


//            final PixelPos[] tickPointsNorth = createTickPoints(parallelsList, meridiansList, TextLocation.NORTH, geoInfo);
//            final PixelPos[] tickPointsSouth = createTickPoints(parallelsList, meridiansList, TextLocation.SOUTH, geoInfo);
//            final PixelPos[] tickPointsWest = createTickPoints(parallelsList, meridiansList, TextLocation.WEST, geoInfo);
//            final PixelPos[] tickPointsEast = createTickPoints(parallelsList, meridiansList, TextLocation.EAST, geoInfo);


            final boolean flippedLats = geoInfo.latDescending && !geoInfo.latAscending;
            final boolean flippedLons = geoInfo.lonDescending && !geoInfo.lonAscending;

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
                    flippedLons,
                    geoInfo);
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
                    false,
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
        if (degreeIncrement >= 90) {
            return 90;
        } else if (degreeIncrement >= 45) {
            return 45.0 * Math.round((degreeIncrement / 45.0));
        } else if (degreeIncrement >= 15.0) {
            // if each division is greater than 15 degrees then round to nearest 15 degrees
            return 15.0 * Math.round((degreeIncrement / 15.0));
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
                                                          GeoInfo geoInfo,
                                                          double tolerance,
                                                          boolean interpolate) {

        List<List<Coord>> parallelsList = new ArrayList<>();

        double pixelX;
        double prevPixelX = -1;
        boolean allowGap = false;  // todo Look more into this  level-2 files allow gaps the rest do not allow
        boolean allowSecondIdenticalParallel = true;
        int NUM_NULL_STEPS_TO_TRIGGER_A_GAP = 2;  // max steps tested before gap is affirmed


        // todo decide whether this should be restricted
        double maxSteps = Math.floor((raster.getRasterWidth() - 1) / 4.0);
        if (minorSteps > maxSteps) {
            minorSteps = maxSteps;
        }


        int PARALLELS_COUNT_MAX = 200;   // just in case default is bad or too tight spacing

        // Get a list of latitudes to attempt to use as the gridlines.
        ArrayList<Double> parellelsLatsArrayList = getParallelsLatsArrayList(geoInfo, latMajorStep, PARALLELS_COUNT_MAX);

        int parallelsCount = 0;


        for (double parallelLat : parellelsLatsArrayList) {

            List<Coord> parallel1a = new ArrayList<>();
            List<Coord> parallel1b = new ArrayList<>();
            List<Coord> parallel2a = new ArrayList<>();
            List<Coord> parallel2b = new ArrayList<>();

            int gap1StepsCount = 0;
            int gap2StepsCount = 0;

            Coord[] coords;

            boolean foundParallel1a = false;
            boolean finishedParallel1a = false;


            boolean foundParallel1b = false;
            boolean finishedParallel1b = false;

            boolean foundParallel2a = false;
            boolean finishedParallel2a = false;

            boolean foundParallel2b = false;
            boolean finishedParallel2b = false;


            prevPixelX = -1;


            for (double step = 0; step <= minorSteps; step += 1.0) {
                pixelX = (int) Math.floor((raster.getRasterWidth() - 1) * step / minorSteps);
                if (pixelX != prevPixelX) {

                    coords = getCoordParallel(parallelLat, pixelX, raster, tolerance, interpolate);

                    if (coords != null) {

                        if (coords[0] != null) {

                            gap1StepsCount = 0;

                            if (!foundParallel1a) {
                                Coord leftEdgeCoord = findLeftEdgeCoord(0, pixelX, prevPixelX, parallelLat, raster, tolerance, interpolate);
                                if (leftEdgeCoord != null) {
                                    parallel1a.add(leftEdgeCoord);
                                }
                                foundParallel1a = true;

                            } else if (finishedParallel1a && !foundParallel1b) {
                                Coord leftEdgeCoord = findLeftEdgeCoord(0, pixelX, prevPixelX, parallelLat, raster, tolerance, interpolate);
                                if (leftEdgeCoord != null) {
                                    parallel1b.add(leftEdgeCoord);
                                }
                                foundParallel1b = true;
                            }


                            if (!finishedParallel1a || allowGap) {
                                parallel1a.add(coords[0]);
                            } else if (!finishedParallel1b || allowGap) {
                                parallel1b.add(coords[0]);
                            }

                        } else {
                            if (foundParallel1a && !finishedParallel1a) {
                                Coord rightEdgeCoord = findRightEdgeCoord(0, pixelX, prevPixelX, parallelLat, raster, tolerance, interpolate);
                                if (rightEdgeCoord != null) {
                                    parallel1a.add(rightEdgeCoord);
                                }

                                if (!allowGap && gap1StepsCount >= NUM_NULL_STEPS_TO_TRIGGER_A_GAP) {
                                    finishedParallel1a = true;
                                } else {
                                    gap1StepsCount++;
                                }

                            } else if (foundParallel1b && !finishedParallel1b) {
                                Coord rightEdgeCoord = findRightEdgeCoord(0, pixelX, prevPixelX, parallelLat, raster, tolerance, interpolate);
                                if (rightEdgeCoord != null) {
                                    parallel1b.add(rightEdgeCoord);
                                }

                                if (!allowGap && gap1StepsCount >= NUM_NULL_STEPS_TO_TRIGGER_A_GAP) {
                                    finishedParallel1b = true;
                                } else {
                                    gap1StepsCount++;
                                }
                            }
                        }


                        if (allowSecondIdenticalParallel) {
                            if (coords[1] != null) {

                                gap2StepsCount = 0;

                                if (!foundParallel2a) {
                                    Coord leftEdgeCoord = findLeftEdgeCoord(1, pixelX, prevPixelX, parallelLat, raster, tolerance, interpolate);
                                    if (leftEdgeCoord != null) {
                                        parallel2a.add(leftEdgeCoord);
                                    }

                                    foundParallel2a = true;
                                } else if (finishedParallel2a && !foundParallel2b) {
                                    Coord leftEdgeCoord = findLeftEdgeCoord(1, pixelX, prevPixelX, parallelLat, raster, tolerance, interpolate);
                                    if (leftEdgeCoord != null) {
                                        parallel2b.add(leftEdgeCoord);
                                    }
                                    foundParallel2b = true;
                                }

                                if (!finishedParallel2a || allowGap) {
                                    parallel2a.add(coords[1]);
                                } else if (!finishedParallel2b || allowGap) {
                                    parallel2b.add(coords[1]);
                                }

                            } else {
                                if (foundParallel2a && !finishedParallel2a) {
                                    Coord rightEdgeCoord = findRightEdgeCoord(1, pixelX, prevPixelX, parallelLat, raster, tolerance, interpolate);
                                    if (rightEdgeCoord != null) {
                                        parallel2a.add(rightEdgeCoord);
                                    }

                                    if (!allowGap && gap2StepsCount >= NUM_NULL_STEPS_TO_TRIGGER_A_GAP) {
                                        finishedParallel2a = true;
                                    } else {
                                        gap2StepsCount++;
                                    }
                                } else if (foundParallel2b && !finishedParallel2b) {
                                    Coord rightEdgeCoord = findRightEdgeCoord(1, pixelX, prevPixelX, parallelLat, raster, tolerance, interpolate);
                                    if (rightEdgeCoord != null) {
                                        parallel2b.add(rightEdgeCoord);
                                    }

                                    if (!allowGap && gap2StepsCount >= NUM_NULL_STEPS_TO_TRIGGER_A_GAP) {
                                        finishedParallel2b = true;
                                    } else {
                                        gap2StepsCount++;
                                    }
                                }

                            }
                        }
                    }
                }

                prevPixelX = pixelX;
            }

            // todo Determine if needed or if needs users option
            double minPoints = Math.floor(0.1 * minorSteps);
            if (minPoints < 4) {
                minPoints = 4;
            }
            // todo TEMP
            minPoints = 1;


            if (!parallel1a.isEmpty() && parallel1a.size() > minPoints) {
                parallelsList.add(parallel1a);
            }

            if (!parallel1b.isEmpty() && parallel1b.size() > minPoints) {
                parallelsList.add(parallel1b);
            }

            if (!parallel2a.isEmpty() && parallel2a.size() > minPoints) {
                parallelsList.add(parallel2a);
            }

            if (!parallel2b.isEmpty() && parallel2b.size() > minPoints) {
                parallelsList.add(parallel2b);
            }

            parallelsCount++;
            if (parallelsCount > PARALLELS_COUNT_MAX) {
                break;
            }

        }


        return parallelsList;
    }


    static Coord findLeftEdgeCoord(int index, double pixelX, double prevPixelX, double parallelLat, RasterDataNode raster, double tolerance, boolean interpolate) {

        // need to look back for actual intersection
        // Go back to previous pixel and step forward until first valid geo pixel encountered.

        if (pixelX > 0) {
            for (double innerPixel = prevPixelX + 1; innerPixel < pixelX; innerPixel += 1.0) {
                Coord[] coordsInner = getCoordParallel(parallelLat, innerPixel, raster, tolerance, interpolate);
                if (coordsInner != null) {
                    if (coordsInner[index] != null) {
                        return coordsInner[index];
                    }
                }
            }
        }

        return null;

    }

    static Coord findRightEdgeCoord(int index, double pixelX, double prevPixelX, double parallelLat, RasterDataNode raster, double tolerance, boolean interpolate) {

        // need to look back for actual intersection
        // Start with current pixel and step backward until first valid geo pixel is found

        if (pixelX <= (raster.getRasterWidth() - 1)) {

            for (double innerPixel = pixelX; innerPixel > prevPixelX; innerPixel -= 1.0) {
                Coord[] coordsInner = getCoordParallel(parallelLat, innerPixel, raster, tolerance, interpolate);
                if (coordsInner != null) {
                    if (coordsInner[index] != null) {
                        return coordsInner[index];
                    }
                }
            }
        }

        return null;
    }


    /**
     * Creates a list of longitudes to attempt to use as the gridlines.
     *
     * @param geoSpan             geoSpan of the scene
     * @param lonMajorStep        the grid cell size in longitudinal direction
     * @param PARALLELS_COUNT_MAX maximum size of list
     * @return list of latitudes to attempt to use as the gridlines.
     */
    private static ArrayList<Double> getMeridiansLonsArrayList(GeoInfo geoSpan,
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
     * @param geoSpan             geoSpan of the scene
     * @param latMajorStep        the grid cell size in latitudinal direction
     * @param PARALLELS_COUNT_MAX maximum size of list
     * @return list of latitudes to attempt to use as the gridlines.
     */
    private static ArrayList<Double> getParallelsLatsArrayList(GeoInfo geoSpan,
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
//        if (max == 90 && !lat90Found) {
//            parellelsLatsArrayList.add(90.0);
//        }

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
//        if (min == -90 && !latNeg90Found) {
//            parellelsLatsArrayList.add(-90.0);
//        }


        return parellelsLatsArrayList;

    }


    private static List<List<Coord>> computeMeridiansList(final double lonMajorStep,
                                                          double minorSteps,
                                                          RasterDataNode raster,
                                                          GeoInfo geoInfo,
                                                          double tolerance,
                                                          boolean interpolate) {

        List<List<Coord>> meridiansList = new ArrayList<>();

        double pixelY;
        double prevPixelY = -1;

        double maxSteps = Math.floor((raster.getRasterHeight() - 1) / 5.0);
        if (minorSteps > maxSteps) {
            minorSteps = maxSteps;
        }

        boolean allowGap = false;  // todo Look more into this
        boolean allowSecondIdenticalMeridian = true;
        int NUM_NULL_STEPS_TO_TRIGGER_A_GAP = 2;  // max steps tested before gap is affirmed


        int MERIDIANS_COUNT_MAX = 200;  // just in case default is bad or too tight spacing
        // Get a list of longitudes to attempt to use as the gridlines.
        ArrayList<Double> meridianLonsArrayList = getMeridiansLonsArrayList(geoInfo, lonMajorStep, MERIDIANS_COUNT_MAX);


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

            List<Coord> meridian1a = new ArrayList<>();
            List<Coord> meridian1b = new ArrayList<>();
            List<Coord> meridian2a = new ArrayList<>();
            List<Coord> meridian2b = new ArrayList<>();

            int gap1StepsCount = 0;
            int gap2StepsCount = 0;

            Coord[] coords;

            boolean foundMeridian1a = false;
            boolean finishedMeridian1a = false;

            boolean foundMeridian1b = false;
            boolean finishedMeridian1b = false;

            boolean foundMeridian2a = false;
            boolean finishedMeridian2a = false;

            boolean foundMeridian2b = false;
            boolean finishedMeridian2b = false;

            prevPixelY = -1;


            for (double step = 0; step <= minorSteps; step += 1.0) {

                pixelY = (int) Math.floor((raster.getRasterHeight() - 1) * step / minorSteps);
                if (pixelY != prevPixelY) {
                    coords = getCoordMeridian(meridianLon, pixelY, raster, tolerance, interpolate);

                    if (coords != null) {
                        if (coords[0] != null) {

                            if (!foundMeridian1a) {
                                Coord bottomEdgeCoord = findBottomEdgeCoord(0, pixelY, prevPixelY, meridianLon, raster, tolerance, interpolate);
                                if (bottomEdgeCoord != null) {
                                    meridian1a.add(bottomEdgeCoord);
                                }

                                foundMeridian1a = true;
                            } else if (finishedMeridian1a && !foundMeridian1b) {
                                Coord bottomEdgeCoord = findBottomEdgeCoord(0, pixelY, prevPixelY, meridianLon, raster, tolerance, interpolate);
                                if (bottomEdgeCoord != null) {
                                    meridian1b.add(bottomEdgeCoord);
                                }

                                foundMeridian1b = true;
                            }


                            if (!finishedMeridian1a || allowGap) {
                                meridian1a.add(coords[0]);
                            } else if (!finishedMeridian1b || allowGap) {
                                meridian1b.add(coords[0]);
                            }


                        } else {
                            if (foundMeridian1a && !finishedMeridian1a) {
                                Coord topEdgeCoord = findTopEdgeCoord(0, pixelY, prevPixelY, meridianLon, raster, tolerance, interpolate);
                                if (topEdgeCoord != null) {
                                    meridian1a.add(topEdgeCoord);
                                }

                                if (!allowGap && gap1StepsCount >= NUM_NULL_STEPS_TO_TRIGGER_A_GAP) {
                                    finishedMeridian1a = true;
                                } else {
                                    gap1StepsCount++;
                                }

                            } else if (foundMeridian1b && !finishedMeridian1b) {
                                Coord topEdgeCoord = findTopEdgeCoord(0, pixelY, prevPixelY, meridianLon, raster, tolerance, interpolate);
                                if (topEdgeCoord != null) {
                                    meridian1b.add(topEdgeCoord);
                                }

                                if (!allowGap && gap1StepsCount >= NUM_NULL_STEPS_TO_TRIGGER_A_GAP) {
                                    finishedMeridian1b = true;
                                } else {
                                    gap1StepsCount++;
                                }

                            }
                        }


                        if (allowSecondIdenticalMeridian) {
                            if (coords[1] != null) {

                                gap2StepsCount = 0;

                                if (!foundMeridian2a) {
                                    Coord leftEdgeCoord = findBottomEdgeCoord(1, pixelY, prevPixelY, meridianLon, raster, tolerance, interpolate);
                                    if (leftEdgeCoord != null) {
                                        meridian2a.add(leftEdgeCoord);
                                    }

                                    foundMeridian2a = true;
                                } else if (finishedMeridian2a && !foundMeridian2b) {
                                    Coord leftEdgeCoord = findBottomEdgeCoord(1, pixelY, prevPixelY, meridianLon, raster, tolerance, interpolate);
                                    if (leftEdgeCoord != null) {
                                        meridian2b.add(leftEdgeCoord);
                                    }
                                    foundMeridian2b = true;
                                }

                                if (!finishedMeridian2a || allowGap) {
                                    meridian2a.add(coords[1]);
                                } else if (!finishedMeridian2b || allowGap) {
                                    meridian2b.add(coords[1]);
                                }

                            } else {
                                if (foundMeridian2a && !finishedMeridian2a) {
                                    Coord rightEdgeCoord = findTopEdgeCoord(1, pixelY, prevPixelY, meridianLon, raster, tolerance, interpolate);
                                    if (rightEdgeCoord != null) {
                                        meridian2a.add(rightEdgeCoord);
                                    }

                                    if (!allowGap && gap2StepsCount >= NUM_NULL_STEPS_TO_TRIGGER_A_GAP) {
                                        finishedMeridian2a = true;
                                    } else {
                                        gap2StepsCount++;
                                    }
                                } else if (foundMeridian2b && !finishedMeridian2b) {
                                    Coord rightEdgeCoord = findTopEdgeCoord(1, pixelY, prevPixelY, meridianLon, raster, tolerance, interpolate);
                                    if (rightEdgeCoord != null) {
                                        meridian2b.add(rightEdgeCoord);
                                    }

                                    if (!allowGap && gap2StepsCount >= NUM_NULL_STEPS_TO_TRIGGER_A_GAP) {
                                        finishedMeridian2b = true;
                                    } else {
                                        gap2StepsCount++;
                                    }
                                }

                            }
                        }

                    }
                }

                prevPixelY = pixelY;
            }


            // todo Determine if needed or if needs users option
            double minPoints = Math.floor(0.1 * minorSteps);
            if (minPoints < 4) {
                minPoints = 4;
            }


            if (!meridian1a.isEmpty() && meridian1a.size() > minPoints) {
                if (!containsExcessMeridianJumps(meridian1a, raster)) {
                    meridiansList.add(meridian1a);
                }
            }

            if (!meridian1b.isEmpty() && meridian1b.size() > minPoints) {
                if (!containsExcessMeridianJumps(meridian1b, raster)) {
                    meridiansList.add(meridian1b);
                }
            }

            if (!meridian2a.isEmpty() && meridian2a.size() > minPoints) {
                if (!containsExcessMeridianJumps(meridian2a, raster)) {
                    meridiansList.add(meridian2a);
                }
            }

            meridiansCount++;
            if (meridiansCount > MERIDIANS_COUNT_MAX) {
                break;
            }

        }


        return meridiansList;
    }


    static Coord findBottomEdgeCoord(int index, double pixelY, double prevPixelY, double meridianLon, RasterDataNode raster, double tolerance, boolean interpolate) {

        // need to look back for actual intersection
        // Go back to previous pixel and step forward until first valid geo pixel encountered.

        if (pixelY > 0) {
            for (double innerPixel = prevPixelY + 1; innerPixel < pixelY; innerPixel += 1.0) {
                Coord[] coordsInner = getCoordMeridian(meridianLon, innerPixel, raster, tolerance, interpolate);
                if (coordsInner != null) {
                    if (coordsInner[index] != null) {
                        return coordsInner[index];
                    }
                }
            }
        }

        return null;
    }


    static Coord findTopEdgeCoord(int index, double pixelY, double prevPixelY, double meridianLon, RasterDataNode raster, double tolerance, boolean interpolate) {

        // need to look back for actual intersection
        // Start with current pixel and step backward until first valid geo pixel is found

        if (pixelY <= (raster.getRasterHeight() - 1)) {
            for (double innerPixel = pixelY; innerPixel > prevPixelY; innerPixel -= 1.0) {
                Coord[] coordsInner = getCoordMeridian(meridianLon, innerPixel, raster, tolerance, interpolate);
                if (coordsInner != null) {
                    if (coordsInner[index] != null) {
                        return coordsInner[index];
                    }
                }
            }
        }

        return null;
    }


    static boolean containsExcessMeridianJumps(List<Coord> meridian1, RasterDataNode raster) {

        if (containsExcessMeridianJumps(meridian1, raster, 0, 20)) {
            return true;
        }

        if (containsExcessMeridianJumps(meridian1, raster, 2, 5)) {
            return true;
        }

        return false;
    }


    static boolean containsExcessMeridianJumps(List<Coord> meridian1, RasterDataNode raster, int maxJumps, double percentPixelsToSetJump) {

        double prevPixelX = -1;
        double currPixelX = -1;
        DIRECTION currDirection = DIRECTION.NOT_SET;
        DIRECTION prevDirection = DIRECTION.NOT_SET;
        int jumpChangeDirectionCount = 0;
        for (Coord coord : meridian1) {
            currPixelX = coord.pixelPos.x;

            if (currPixelX != -1 && prevPixelX != -1) {
                double deltaX = currPixelX - prevPixelX;
                double pixelJumpPercent = 100 * deltaX / (raster.getRasterWidth() - 1);

                if (Math.abs(pixelJumpPercent) > percentPixelsToSetJump) {
                    currDirection = (pixelJumpPercent > 0) ? DIRECTION.ASCENDING : DIRECTION.DESCENDING;

                    if (currDirection != prevDirection) {
                        jumpChangeDirectionCount++;

                    }
                    prevDirection = currDirection;
                }


            }
            prevPixelX = currPixelX;
        }

        if (jumpChangeDirectionCount > maxJumps) {
            return true;
        } else {
            return false;
        }
    }


    static Coord[] getCoordMeridian(double meridianLon,
                                    double pixelY,
                                    RasterDataNode raster,
                                    double tolerance,
                                    boolean interpolate) {


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
        for (double pixelY = (raster.getRasterHeight() - 1); pixelY >= 0; pixelY -= increment) {
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

                    if (pixPosCurr.y > 0) {
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
        PixelPos desiredPixelPos = new PixelPos(x, y);
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
        PixelPos desiredPixelPos = new PixelPos(x, y);
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
                int r = 0;
                int t = r;
            }
            if (lonBoundaryCurr > 119.99 & lonBoundaryCurr < 120.01) {
                int r = 0;
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
                        lonBoundaryIncreasingFound = true;
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
                                               TextLocation textLocation,
                                               GeoInfo geoInfo) {
        final List<PixelPos> pixelPoses = new ArrayList<>();

        if (geoInfo.southPoleCrossed || geoInfo.northPoleCrossed) {
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
        } else {
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
                                                GeoInfo geoSpan) {
        final List<TextGlyph> textGlyphs = new ArrayList<TextGlyph>();

        spacer = 0; //  todo TEMP

        if (geoSpan.southPoleCrossed) { // todo TMP
            switch (textLocation) {
                case NORTH:
                    createLongitudePolarEdgeTextGlyphs(longitudeGridLinePoints, textLocation, textGlyphs, formatCompass, formatDecimal, majorStep, raster, geoSpan);
                    createLatitudePolarEdgeTextGlyphs(latitudeGridLinePoints, textLocation, textGlyphs, formatCompass, formatDecimal, majorStep, raster, geoSpan);
                    break;
                case SOUTH:
                    createLongitudePolarEdgeTextGlyphs(longitudeGridLinePoints, textLocation, textGlyphs, formatCompass, formatDecimal, majorStep, raster, geoSpan);
                    break;
                case WEST:
                    createLongitudePolarEdgeTextGlyphs(longitudeGridLinePoints, textLocation, textGlyphs, formatCompass, formatDecimal, majorStep, raster, geoSpan);
                    break;
                case EAST:
                    createLongitudePolarEdgeTextGlyphs(longitudeGridLinePoints, textLocation, textGlyphs, formatCompass, formatDecimal, majorStep, raster, geoSpan);
                    break;
            }
        } else if (geoSpan.northPoleCrossed) {  // todo TMP
            switch (textLocation) {
                case NORTH:
                    createLongitudePolarEdgeTextGlyphs(longitudeGridLinePoints, textLocation, textGlyphs, formatCompass, formatDecimal, majorStep, raster, geoSpan);
                    createLatitudePolarEdgeTextGlyphs(latitudeGridLinePoints, textLocation, textGlyphs, formatCompass, formatDecimal, majorStep, raster, geoSpan);
                    break;
                case SOUTH:
                    createLongitudePolarEdgeTextGlyphs(longitudeGridLinePoints, textLocation, textGlyphs, formatCompass, formatDecimal, majorStep, raster, geoSpan);
                    break;
                case WEST:
                    createLongitudePolarEdgeTextGlyphs(longitudeGridLinePoints, textLocation, textGlyphs, formatCompass, formatDecimal, majorStep, raster, geoSpan);

                    break;
                case EAST:
                    createLongitudePolarEdgeTextGlyphs(longitudeGridLinePoints, textLocation, textGlyphs, formatCompass, formatDecimal, majorStep, raster, geoSpan);

                    break;
            }

        } else {
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
                                                        GeoInfo geoSpan) {

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
//                if (geoSpan.lonSpan < 180 && geoSpan.latSpan < 90) {
//                    onlyAllowLeftEdgeLabels = true;
//                }




                if (geoSpan.containsValidGeoCorners && !geoSpan.northPoleCrossed && !geoSpan.southPoleCrossed) {
                    onlyAllowLeftEdgeLabels = true;
                } else {
                    onlyAllowLeftEdgeLabels = false;
                }

                if (onlyAllowLeftEdgeLabels) {
                    int buffer = 5;
                    if (xPos > (0 + buffer)) {
                        allowLabel = false;
                    }
                }

                // older code
//
//
//                if (onlyAllowLeftEdgeLabels) {
//                    int buffer = 3;
//                    if (xPos > (0 + buffer)) {
//                        allowLabel = false;
//                    }
//                } else {
//                    if (yPos < 1 || yPos > (height - 1)) {
//                        if (xPos > 1) {
//                            if (spacer >= 0) {
//                                offsetX = -spacer;
//                            }
//                        }
//                    }
//                }




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


    private static void createWesternLatitudeTextGlyphsNew(List<List<Coord>> latitudeGridLinePoints,
                                                        List<TextGlyph> textGlyphs,
                                                        boolean formatCompass,
                                                        boolean formatDecimal,
                                                        double majorStep,
                                                        RasterDataNode raster,
                                                        int spacer,
                                                        GeoInfo geoSpan) {

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
//                if (geoSpan.lonSpan < 180 && geoSpan.latSpan < 90) {
//                    onlyAllowLeftEdgeLabels = true;
//                }


                if (geoSpan.containsValidGeoCorners && (geoSpan.southPoleCrossed || geoSpan.northPoleCrossed)) {
                    boolean edgeLabel = false;

                    int buffer = 3;

                    if (xPos <= (0 + buffer)) {
                        edgeLabel = true;
                    }

                    if (edgeLabel) {
                        allowLabel = false;
                    } else {
                        allowLabel = true;
                    }

                } else {

                    if (geoSpan.containsValidGeoCorners && !geoSpan.northPoleCrossed && !geoSpan.southPoleCrossed) {
                        onlyAllowLeftEdgeLabels = true;
                    } else {
                        onlyAllowLeftEdgeLabels = false;
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

                    TextGlyph textGlyph = createTextGlyph(coord1.geoPos.getLatString(latToUse, formatCompass, formatDecimal), coord1, coord2, offsetX, 0);
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

    private static void createEasternLatitudeTextGlyphsNew(List<List<Coord>> latitudeGridLinePoints,
                                                        List<TextGlyph> textGlyphs,
                                                        boolean formatCompass,
                                                        boolean formatDecimal,
                                                        double majorStep,
                                                        RasterDataNode raster,
                                                        int spacer,
                                                        GeoInfo geoSpan) {

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
//                if (geoSpan.lonSpan < 180 && geoSpan.latSpan < 90) {
//                    onlyAllowRightEdgeLabels = true;
//                }


                if (geoSpan.containsValidGeoCorners && (geoSpan.southPoleCrossed || geoSpan.northPoleCrossed)) {
                    boolean edgeLabel = false;

                    int buffer = 3;

                    if (xPos >= (width - buffer)) {
                        edgeLabel = true;
                    }

                    if (edgeLabel) {
                        allowLabel = false;
                    } else {
                        allowLabel = true;
                    }

                } else {


                    if (geoSpan.containsValidGeoCorners && !geoSpan.northPoleCrossed && !geoSpan.southPoleCrossed) {
                        onlyAllowRightEdgeLabels = true;
                    } else {
                        onlyAllowRightEdgeLabels = false;
                    }


                    if (onlyAllowRightEdgeLabels) {
                        int buffer = 3;
                        if (xPos < (width - buffer)) {
                            allowLabel = false;
                        }
                    } else {
                        if (yPos < 1 || yPos > (height - 1)) {
                            if (xPos < (width - 1)) {
                                if (spacer >= 0) {
                                    offsetX = spacer;
                                }
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


    private static void createEasternLatitudeTextGlyphs(List<List<Coord>> latitudeGridLinePoints,
                                                        List<TextGlyph> textGlyphs,
                                                        boolean formatCompass,
                                                        boolean formatDecimal,
                                                        double majorStep,
                                                        RasterDataNode raster,
                                                        int spacer,
                                                        GeoInfo geoSpan) {

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
//                if (geoSpan.lonSpan < 180 && geoSpan.latSpan < 90) {
//                    onlyAllowRightEdgeLabels = true;
//                }



                if (geoSpan.containsValidGeoCorners && !geoSpan.northPoleCrossed && !geoSpan.southPoleCrossed) {
                    onlyAllowRightEdgeLabels = true;
                } else {
                    onlyAllowRightEdgeLabels = false;
                }

                if (onlyAllowRightEdgeLabels) {
                    int buffer = 5;
                    if (xPos < (width - 1 - buffer)) {
                        allowLabel = false;
                    }
                }


                // older  code
//                if (geoSpan.containsValidGeoCorners && !geoSpan.northPoleCrossed && !geoSpan.southPoleCrossed) {
//                    onlyAllowRightEdgeLabels = true;
//                } else {
//                    onlyAllowRightEdgeLabels = false;
//                }
//
//
//                if (onlyAllowRightEdgeLabels) {
//                    int buffer = 3;
//                    if (xPos < (width - buffer)) {
//                        allowLabel = false;
//                    }
//                } else {
//                    if (yPos < 1 || yPos > (height - 1)) {
//                        if (xPos < (width -1)) {
//                            if (spacer >= 0) {
//                                offsetX = spacer;
//                            }
//                        }
//                    }
//                }



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



    private static void createTickPointsByGlyph(TextGlyph[] textGlyphArray,
                                                List<PixelPos> pixelPoses) {

        for (final TextGlyph textGlyph : textGlyphArray) {

            PixelPos pixelPos = new PixelPos(textGlyph.getX(), textGlyph.getY());

            if (pixelPos.isValid()) {
                pixelPoses.add(pixelPos);
            }
        }
    }




    private static PixelPos[] createTickPointsByGlyph(List<List<Coord>> latitudeGridLinePoints,
                                                      List<List<Coord>> longitudeGridLinePoints,
                                                      TextGlyph[] textGlyph,
                                               TextLocation textLocation,
                                               GeoInfo geoInfo) {
        final List<PixelPos> pixelPoses = new ArrayList<>();


        switch (textLocation) {
            case NORTH:
                createTickPointsByGlyph(textGlyph, pixelPoses);
                break;
            case SOUTH:
                createTickPointsByGlyph(textGlyph, pixelPoses);
                break;
            case WEST:
                createTickPointsByGlyph(textGlyph, pixelPoses);
                break;
            case EAST:
                createTickPointsByGlyph(textGlyph, pixelPoses);
                break;

        }

        // older code
//        if (geoInfo.southPoleCrossed || geoInfo.northPoleCrossed || 1 == 1) { // todo TEMP
//            switch (textLocation) {
//                case NORTH:
//                    createNorthernLongitudeTickPointsByGlyph(textGlyph, pixelPoses);
//                    break;
//                case SOUTH:
//                    createNorthernLongitudeTickPointsByGlyph(textGlyph, pixelPoses);
//                    break;
//                case WEST:
//                    createNorthernLongitudeTickPointsByGlyph(textGlyph, pixelPoses);
//                    break;
//                case EAST:
//                    createNorthernLongitudeTickPointsByGlyph(textGlyph, pixelPoses);
//                    break;
//            }
//        } else {
//            switch (textLocation) {
//                case NORTH:
//                    createNorthernLongitudeTickPoints(longitudeGridLinePoints, pixelPoses);
//                    break;
//                case SOUTH:
//                    createSouthernLongitudeTickPoints(longitudeGridLinePoints, pixelPoses);
//                    break;
//                case WEST:
//                    createWesternLatitudeTickPoints(latitudeGridLinePoints, pixelPoses);
//                    break;
//                case EAST:
//                    createEasternLatitudeTickPoints(latitudeGridLinePoints, pixelPoses);
//                    break;
//            }
//        }



        return pixelPoses.toArray(new PixelPos[0]);
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
                                                          GeoInfo geoSpan) {

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
//                if (geoSpan.lonSpan < 180 && geoSpan.latSpan < 90) {
//                    onlyAllowNorthEdgeLabels = true;
//                }

                if (geoSpan.containsValidGeoCorners && !geoSpan.northPoleCrossed && !geoSpan.southPoleCrossed) {
                    onlyAllowNorthEdgeLabels = true;
                } else {
                    onlyAllowNorthEdgeLabels = false;
                }

                if (onlyAllowNorthEdgeLabels) {
                    int buffer = 5;
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

                    TextGlyph textGlyph = createTextGlyph(coord1.geoPos.getLonString(lonToUse, formatCompass, formatDecimal), coord1, coord2, 0, 0);
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
                                                  GeoInfo geoSpan) {

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
//                if (geoSpan.lonSpan < 180 && geoSpan.latSpan < 90) {
//                    onlyAllowSouthEdgeLabels = true;
//                }

                if (geoSpan.containsValidGeoCorners && !geoSpan.northPoleCrossed && !geoSpan.southPoleCrossed) {
                    onlyAllowSouthEdgeLabels = true;
                } else {
                    onlyAllowSouthEdgeLabels = false;
                }

                if (onlyAllowSouthEdgeLabels) {
                    int buffer = 5;
                    if (yPos < (height - 1 - buffer)) {
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

                    TextGlyph textGlyph = createTextGlyph(coord1.geoPos.getLonString(lonToUse, formatCompass, formatDecimal), coord1, coord2, 0, 0);
                    textGlyphs.add(textGlyph);
                }
            }
        }
    }


    private static void createLongitudePolarEdgeTextGlyphs(List<List<Coord>> longitudeGridLinePoints,
                                                           TextLocation textLocation,
                                                           List<TextGlyph> textGlyphs, boolean formatCompass,
                                                           boolean formatDecimal,
                                                           double majorStep,
                                                           RasterDataNode raster,
                                                           GeoInfo geoSpan) {


        int height = raster.getRasterHeight();
        int width = raster.getRasterWidth();

        for (List<Coord> longitudeGridLinePoint : longitudeGridLinePoints) {

            if (longitudeGridLinePoint.size() >= 2) {

                int indexFirst = 0;
                Coord coordFirst = longitudeGridLinePoint.get(indexFirst);
                double yPosFirst = (coordFirst.pixelPos.getY());
                double xPosFirst = (coordFirst.pixelPos.getX());
                Coord coordFirstInner = null;

                int indexSecond = 1;
                Coord coordSecond = longitudeGridLinePoint.get(indexSecond);
                double yPosSecond = (coordSecond.pixelPos.getY());
                double xPosSecond = (coordSecond.pixelPos.getX());


                int indexLast = longitudeGridLinePoint.size() - 1;
                Coord coordLast = longitudeGridLinePoint.get(indexLast);
                double yPosLast = (coordLast.pixelPos.getY());
                double xPosLast = (coordLast.pixelPos.getX());
                Coord coordLastInner = null;

                int indexSecondToLast = longitudeGridLinePoint.size() - 2;
                Coord coordSecondToLast = longitudeGridLinePoint.get(indexSecondToLast);
                double yPosSecondToLast = (coordSecondToLast.pixelPos.getY());
                double xPosSecondToLast = (coordSecondToLast.pixelPos.getX());


                boolean allowLabel = false;
                boolean onlyAllowEdgeLabels = false;

                boolean allowLabelFirst = false;
                boolean allowLabelLast = false;


                if (geoSpan.containsValidGeoCorners) {
                    onlyAllowEdgeLabels = true;
                } else {
                    onlyAllowEdgeLabels = false;
                }

                boolean crossesTopEdge = false;
                boolean crossesBottomEdge = false;
                boolean crossesLeftEdge = false;
                boolean crossesRightEdge = false;

                boolean firstCrossesEdge = false;
                boolean lastCrossesEdge = false;

                int buffer = 5;


                if (onlyAllowEdgeLabels  && false) {  // todo TEMP


                    switch (textLocation) {

                        case NORTH: {
                            if (yPosFirst <= (0 + buffer)) {
                                crossesLeftEdge = true;
                                allowLabelFirst = true;
                                PixelPos pixelPosFirstInner = new PixelPos((float) (coordFirst.pixelPos.getX()), (float) (coordFirst.pixelPos.getY() + 1));
                                coordFirstInner = new Coord(coordFirst.geoPos, pixelPosFirstInner);
                            }

                            if (yPosLast <= (0 + buffer)) {
                                crossesLeftEdge = true;
                                allowLabelLast = true;
                                PixelPos pixelPosLastInner = new PixelPos((float) (coordLast.pixelPos.getX()), (float) (coordLast.pixelPos.getY() + 1));
                                coordLastInner = new Coord(coordLast.geoPos, pixelPosLastInner);
                            }
                        }
                        break;


                        case SOUTH: {
                            if (yPosFirst >= (height - buffer)) {
                                crossesLeftEdge = true;
                                allowLabelFirst = true;
                                PixelPos pixelPosFirstInner = new PixelPos((float) (coordFirst.pixelPos.getX()), (float) (coordFirst.pixelPos.getY() - 1));
                                coordFirstInner = new Coord(coordFirst.geoPos, pixelPosFirstInner);
                            }

                            if (yPosLast >= (height - buffer)) {
                                crossesLeftEdge = true;
                                allowLabelLast = true;
                                PixelPos pixelPosLastInner = new PixelPos((float) (coordLast.pixelPos.getX()), (float) (coordLast.pixelPos.getY() - 1));
                                coordLastInner = new Coord(coordLast.geoPos, pixelPosLastInner);
                            }
                        }
                        break;



                        case WEST: {
                            if (xPosFirst <= (0 + buffer)) {
                                crossesLeftEdge = true;
                                allowLabelFirst = true;
                                PixelPos pixelPosFirstInner = new PixelPos((float) (coordFirst.pixelPos.getX() + 1), (float) coordFirst.pixelPos.getY());
                                coordFirstInner = new Coord(coordFirst.geoPos, pixelPosFirstInner);
                            }

                            if (xPosLast <= (0 + buffer)) {
                                crossesLeftEdge = true;
                                allowLabelLast = true;
                                PixelPos pixelPosLastInner = new PixelPos((float) (coordLast.pixelPos.getX() + 1), (float) coordLast.pixelPos.getY());
                                coordLastInner = new Coord(coordLast.geoPos, pixelPosLastInner);
                            }
                        }
                        break;



                        case EAST: {
                            if (xPosFirst >= (width - buffer)) {
                                crossesLeftEdge = true;
                                allowLabelFirst = true;
                                PixelPos pixelPosFirstInner = new PixelPos((float) (coordFirst.pixelPos.getX() - 1), (float) coordFirst.pixelPos.getY());
                                coordFirstInner = new Coord(coordFirst.geoPos, pixelPosFirstInner);
                            }

                            if (xPosLast >= (width - buffer)) {
                                crossesLeftEdge = true;
                                allowLabelLast = true;
                                PixelPos pixelPosLastInner = new PixelPos((float) (coordLast.pixelPos.getX() - 1), (float) coordLast.pixelPos.getY());
                                coordLastInner = new Coord(coordLast.geoPos, pixelPosLastInner);
                            }
                        }
                        break;

                    }

                } else {

                    switch (textLocation) {

                        case NORTH: {
                            // the line starts at the top of the scene
                            if (yPosFirst < yPosSecond) {
                                PixelPos pixelPosAbove = new PixelPos((float) (coordFirst.pixelPos.getX()), (float) (coordFirst.pixelPos.getY() - (1 + buffer)));
                                GeoPos geoPosAbove = raster.getGeoCoding().getGeoPos(pixelPosAbove, null);
                                if (!geoPosAbove.isValid()) {
                                    PixelPos pixelPosFirstInner = new PixelPos((float) (coordFirst.pixelPos.getX()), (float) (coordFirst.pixelPos.getY() + 1));
                                    GeoPos geoPosFirstInner = raster.getGeoCoding().getGeoPos(pixelPosFirstInner, null);
                                    coordFirstInner = new Coord(geoPosFirstInner, pixelPosFirstInner);
                                    allowLabelFirst = true;
                                }
                            }


                            // the line ends at the top of the scene
                            if (yPosLast < yPosSecondToLast) {
                                PixelPos pixelPosAbove = new PixelPos((float) (coordLast.pixelPos.getX()), (float) (coordLast.pixelPos.getY() - (1 + buffer)));
                                GeoPos geoPosAbove = raster.getGeoCoding().getGeoPos(pixelPosAbove, null);
                                if (!geoPosAbove.isValid()) {
                                    PixelPos pixelPosLastInner = new PixelPos((float) (coordLast.pixelPos.getX()), (float) (coordLast.pixelPos.getY() + 1));
                                    GeoPos geoPosLastInner = raster.getGeoCoding().getGeoPos(pixelPosLastInner, null);
                                    coordLastInner = new Coord(geoPosLastInner, pixelPosLastInner);
                                    allowLabelLast = true;
                                }
                            }


//                            if (yPosFirst <= (0 + buffer)) {
//                                crossesLeftEdge = true;
//                                PixelPos pixelPosFirstInner = new PixelPos((float) (coordFirst.pixelPos.getX()), (float) (coordFirst.pixelPos.getY() + 1));
//                                coordFirstInner = new Coord(coordFirst.geoPos, pixelPosFirstInner);
//                            }
//
//                            if (yPosLast <= (0 + buffer)) {
//                                crossesLeftEdge = true;
//                                PixelPos pixelPosLastInner = new PixelPos((float) (coordLast.pixelPos.getX()), (float) (coordLast.pixelPos.getY() + 1));
//                                coordLastInner = new Coord(coordLast.geoPos, pixelPosLastInner);
//                            }
                        }
                        break;


                        case SOUTH: {
                            // the line starts at the bottom of the scene
                            if (yPosFirst > yPosSecond) {
                                PixelPos pixelPosBelow = new PixelPos((float) (coordFirst.pixelPos.getX()), (float) (coordFirst.pixelPos.getY() + (1 + buffer)));
                                GeoPos geoPosBelow = raster.getGeoCoding().getGeoPos(pixelPosBelow, null);
                                if (!geoPosBelow.isValid()) {
                                    PixelPos pixelPosFirstInner = new PixelPos((float) (coordFirst.pixelPos.getX()), (float) (coordFirst.pixelPos.getY() - 1));
                                    GeoPos geoPosFirstInner = raster.getGeoCoding().getGeoPos(pixelPosFirstInner, null);
                                    coordFirstInner = new Coord(geoPosFirstInner, pixelPosFirstInner);
                                    allowLabelFirst = true;
                                }
                            }


                            // the line ends at the bottom of the scene
                            if (yPosLast > yPosSecondToLast) {
                                PixelPos pixelPosBelow = new PixelPos((float) (coordLast.pixelPos.getX()), (float) (coordLast.pixelPos.getY() + (1 + buffer)));
                                GeoPos geoPosBelow = raster.getGeoCoding().getGeoPos(pixelPosBelow, null);
                                if (!geoPosBelow.isValid()) {
                                    PixelPos pixelPosLastInner = new PixelPos((float) (coordLast.pixelPos.getX()), (float) (coordLast.pixelPos.getY() - 1));
                                    GeoPos geoPosLastInner = raster.getGeoCoding().getGeoPos(pixelPosLastInner, null);
                                    coordLastInner = new Coord(geoPosLastInner, pixelPosLastInner);
                                    allowLabelLast = true;
                                }
                            }




//                            if (yPosFirst >= (height - buffer)) {
//                                crossesLeftEdge = true;
//                                PixelPos pixelPosFirstInner = new PixelPos((float) (coordFirst.pixelPos.getX()), (float) (coordFirst.pixelPos.getY() - 1));
//                                coordFirstInner = new Coord(coordFirst.geoPos, pixelPosFirstInner);
//                            }
//
//                            if (yPosLast >= (height - buffer)) {
//                                crossesLeftEdge = true;
//                                PixelPos pixelPosLastInner = new PixelPos((float) (coordLast.pixelPos.getX()), (float) (coordLast.pixelPos.getY() - 1));
//                                coordLastInner = new Coord(coordLast.geoPos, pixelPosLastInner);
//                            }
                        }
                        break;



                        case WEST: {
                            // the line starts at the left of the scene
                            if (xPosFirst < xPosSecond) {
                                PixelPos pixelPosToLeft = new PixelPos((float) (coordFirst.pixelPos.getX() - (1 + buffer)), (float) coordFirst.pixelPos.getY());
                                GeoPos geoPosToLeft = raster.getGeoCoding().getGeoPos(pixelPosToLeft, null);
                                if (!geoPosToLeft.isValid()) {
                                    PixelPos pixelPosFirstInner = new PixelPos((float) (coordFirst.pixelPos.getX() + 1), (float) coordFirst.pixelPos.getY());
                                    GeoPos geoPosFirstInner = raster.getGeoCoding().getGeoPos(pixelPosFirstInner, null);
                                    coordFirstInner = new Coord(geoPosFirstInner, pixelPosFirstInner);
                                    allowLabelFirst = true;
                                }
                            }

                            // the line ends at the left of the scene
                            if (xPosLast < xPosSecondToLast) {
                                PixelPos pixelPosToLeft = new PixelPos((float) (coordLast.pixelPos.getX() - (1 + buffer)), (float) coordLast.pixelPos.getY());
                                GeoPos geoPosToLeft = raster.getGeoCoding().getGeoPos(pixelPosToLeft, null);
                                if (!geoPosToLeft.isValid()) {
                                    PixelPos pixelPosLastInner = new PixelPos((float) (coordLast.pixelPos.getX() + 1), (float) coordLast.pixelPos.getY());
                                    GeoPos geoPosLastInner = raster.getGeoCoding().getGeoPos(pixelPosLastInner, null);
                                    coordLastInner = new Coord(geoPosLastInner, pixelPosLastInner);
                                    allowLabelLast = true;
                                }
                            }


                        }
                        break;



                        case EAST: {
                            // the line starts at the right of the scene
                            if (xPosFirst > xPosSecond) {
                                PixelPos pixelPosToRight = new PixelPos((float) (coordFirst.pixelPos.getX() + (1 + buffer)), (float) coordFirst.pixelPos.getY());
                                GeoPos geoPosToRight = raster.getGeoCoding().getGeoPos(pixelPosToRight, null);
                                if (!geoPosToRight.isValid()) {
                                    PixelPos pixelPosFirstInner = new PixelPos((float) (coordFirst.pixelPos.getX() - 1), (float) coordFirst.pixelPos.getY());
                                    GeoPos geoPosFirstInner = raster.getGeoCoding().getGeoPos(pixelPosFirstInner, null);
                                    coordFirstInner = new Coord(geoPosFirstInner, pixelPosFirstInner);
                                    allowLabelFirst = true;
                                }
                            }


                            // the line ends at the right of the scene
                            if (xPosLast > xPosSecondToLast) {
                                PixelPos pixelPosToRight = new PixelPos((float) (coordLast.pixelPos.getX() + (1 + buffer)), (float) coordLast.pixelPos.getY());
                                GeoPos geoPosToRight = raster.getGeoCoding().getGeoPos(pixelPosToRight, null);
                                if (!geoPosToRight.isValid()) {
                                    PixelPos pixelPosLastInner = new PixelPos((float) (coordLast.pixelPos.getX() - 1), (float) coordLast.pixelPos.getY());
                                    GeoPos geoPosLastInner = raster.getGeoCoding().getGeoPos(pixelPosLastInner, null);
                                    coordLastInner = new Coord(geoPosLastInner, pixelPosLastInner);
                                    allowLabelLast = true;
                                }
                            }

                        }
                        break;

                    }
                }


                // if onlyAllowEdgeLabels=true then restrict labels to only edge labels
                if (onlyAllowEdgeLabels) {
                    if (allowLabelFirst) {
                        if (!crossesEdge(raster, coordFirst, textLocation, buffer)) {
                            allowLabelFirst = false;
                        }
                    }

                    if (allowLabelLast) {
                        if (!crossesEdge(raster, coordLast, textLocation, buffer)) {
                            allowLabelLast = false;
                        }
                    }
                }


//                if (onlyAllowEdgeLabels) {
//                    if (crossesTopEdge || crossesBottomEdge || crossesLeftEdge || crossesRightEdge) {
//                        allowLabel = true;
//                    } else {
//                        allowLabel = false;
//                    }
//                } else {
//                    allowLabel = true;
//                }


                if (allowLabelFirst && coordFirst != null && coordFirstInner != null && isCoordPairValid(coordFirst, coordFirstInner)) {
                    double lon = coordFirst.geoPos.lon;
                    double lonToUse = lon;

                    TextGlyph textGlyph = createTextGlyph(coordFirst.geoPos.getLonString(lonToUse, formatCompass, formatDecimal), coordFirst, coordFirstInner, 0, 0);
                    textGlyphs.add(textGlyph);

                }

                if (allowLabelLast && coordLast != null && coordLastInner != null && isCoordPairValid(coordLast, coordLastInner)) {
                    double lon = coordLast.geoPos.lon;
                    double lonToUse = lon;

                    TextGlyph textGlyph = createTextGlyph(coordLast.geoPos.getLonString(lonToUse, formatCompass, formatDecimal), coordLast, coordLastInner, 0, 0);
                    textGlyphs.add(textGlyph);

                }


            }
        }
    }


    private static boolean crossesAnyEdge( RasterDataNode raster, Coord coord, int buffer) {

        boolean crossesEdge = false;

        if (raster == null) {
            return false;
        }

        if (crossesEdge(raster, coord, TextLocation.TOP, buffer)) {
            crossesEdge = true;
        }

        if (crossesEdge(raster, coord, TextLocation.BOTTOM, buffer)) {
            crossesEdge = true;
        }

        if (crossesEdge(raster, coord, TextLocation.LEFT, buffer)) {
            crossesEdge = true;
        }

        if (crossesEdge(raster, coord, TextLocation.RIGHT, buffer)) {
            crossesEdge = true;
        }

        return crossesEdge;

    }



    private static boolean crossesEdge( RasterDataNode raster, Coord coord, TextLocation textLocation, int buffer) {

        boolean crossesEdge = false;

        if (raster == null) {
            return false;
        }

        int height = raster.getRasterHeight();
        int width = raster.getRasterWidth();

        double yPos = (coord.pixelPos.getY());
        double xPos = (coord.pixelPos.getX());

        switch (textLocation) {

            case TOP, NORTH: {
                if (yPos <= (0 + buffer)) {
                    crossesEdge = true;
                }
            }
            break;


            case BOTTOM, SOUTH: {
                if (yPos >= (height - (1 + buffer))) {
                    crossesEdge = true;
                }
            }
            break;


            case LEFT, WEST: {
                if (xPos <= (0 + buffer)) {
                    crossesEdge = true;
                }
            }
            break;


            case RIGHT, EAST: {
                if (xPos >= (width - (1 + buffer))) {
                    crossesEdge = true;
                }
            }
            break;

        }


        return crossesEdge;

    }



    private static void createLatitudePolarEdgeTextGlyphs(List<List<Coord>> latitudeGridLinePoints,
                                                           TextLocation textLocation,
                                                           List<TextGlyph> textGlyphs, boolean formatCompass,
                                                           boolean formatDecimal,
                                                           double majorStep,
                                                           RasterDataNode raster,
                                                           GeoInfo geoSpan) {


        int height = raster.getRasterHeight();
        int width = raster.getRasterWidth();

        for (List<Coord> latitudeGridLinePoint : latitudeGridLinePoints) {

            if (latitudeGridLinePoint.size() >= 2) {

                int indexFirst = 0;
                Coord coordFirst = latitudeGridLinePoint.get(indexFirst);
                double yPosFirst = (coordFirst.pixelPos.getY());
                double xPosFirst = (coordFirst.pixelPos.getX());
                Coord coordFirstInner = null;

                int indexSecond = 1;
                Coord coordSecond = latitudeGridLinePoint.get(indexSecond);
                double yPosSecond = (coordSecond.pixelPos.getY());
                double xPosSecond = (coordSecond.pixelPos.getX());


                int indexLast = latitudeGridLinePoint.size() - 1;
                Coord coordLast = latitudeGridLinePoint.get(indexLast);
                double yPosLast = (coordLast.pixelPos.getY());
                double xPosLast = (coordLast.pixelPos.getX());
                Coord coordLastInner = null;

                int indexSecondToLast = latitudeGridLinePoint.size() - 2;
                Coord coordSecondToLast = latitudeGridLinePoint.get(indexSecondToLast);
                double yPosSecondToLast = (coordSecondToLast.pixelPos.getY());
                double xPosSecondToLast = (coordSecondToLast.pixelPos.getX());


                boolean allowLabelFirst = false;
                boolean allowLabelLast = false;


                int buffer = 5;


                switch (textLocation) {

                    case NORTH: {
                        // the line starts in a downward direction
                        if (yPosFirst < yPosSecond) {
                            PixelPos pixelPosFirstInner = new PixelPos((float) (coordFirst.pixelPos.getX()), (float) (coordFirst.pixelPos.getY() + 1));
                            GeoPos geoPosFirstInner = raster.getGeoCoding().getGeoPos(pixelPosFirstInner, null);
                            coordFirstInner = new Coord(geoPosFirstInner, pixelPosFirstInner);
                            allowLabelFirst = true;
                        }


                        // the line ends in an upward direction
                        if (yPosLast < yPosSecondToLast) {
                            PixelPos pixelPosLastInner = new PixelPos((float) (coordLast.pixelPos.getX()), (float) (coordLast.pixelPos.getY() + 1));
                            GeoPos geoPosLastInner = raster.getGeoCoding().getGeoPos(pixelPosLastInner, null);
                            coordLastInner = new Coord(geoPosLastInner, pixelPosLastInner);
                            allowLabelLast = true;
                        }
                    }
                    break;


                    case SOUTH: {
                    }
                    break;



                    case WEST: {
                    }
                    break;



                    case EAST: {
                    }
                    break;

                }



                boolean allowEdgeLabels = true;
                boolean alignWithPole = false;

                if (geoSpan.containsValidGeoCorners) {
                    allowEdgeLabels = false;
                    alignWithPole = true;
                }



                // if onlyAllowEdgeLabels=true then restrict labels to only edge labels
                if (!allowEdgeLabels) {  // todo TEMP
                    if (allowLabelFirst) {
                        if (crossesAnyEdge(raster, coordFirst, buffer)) {
                            allowLabelFirst = false;
                        }
                    }

                    if (allowLabelLast) {
                        if (crossesAnyEdge(raster, coordLast, buffer)) {
                            allowLabelLast = false;
                        }
                    }
                }




                PixelPos polarCoord = null;
                if (geoSpan.coordsPolar != null) {
                    if (geoSpan.southPoleCrossed) {
                        polarCoord = geoSpan.coordsPolar.southernmostPixelPos;
                    } else {
                        polarCoord = geoSpan.coordsPolar.northernmostPixelPos;
                    }
                }

                double centerShiftExtra = height * .01;  // todo Temp this kinda works but could be revised to be handled outside of this method

                if (allowLabelFirst && coordFirst != null && coordFirstInner != null && isCoordPairValid(coordFirst, coordFirstInner)) {

                    double lat = coordFirst.geoPos.lat;
                    double latToUse = lat;

                    if (alignWithPole && polarCoord != null) {
                        double deltaY = polarCoord.getY() - coordFirst.pixelPos.getY() + centerShiftExtra;

                        PixelPos pixelPosFirstAlignWithPole = new PixelPos((float) (coordFirst.pixelPos.getX()), (float) (coordFirst.pixelPos.getY() + deltaY));
                        GeoPos geoPosFirstAlignWithPole = raster.getGeoCoding().getGeoPos(pixelPosFirstAlignWithPole, null);
                        Coord coordFirstAlignWithPole = new Coord(geoPosFirstAlignWithPole, pixelPosFirstAlignWithPole);

                        PixelPos pixelPosFirstInnerAlignWithPole = new PixelPos((float) (coordFirstInner.pixelPos.getX()), (float) (coordFirstInner.pixelPos.getY() + deltaY));
                        GeoPos geoPosFirstInnerAlignWithPole = raster.getGeoCoding().getGeoPos(pixelPosFirstInnerAlignWithPole, null);
                        Coord coordFirstInnerAlignWithPole = new Coord(geoPosFirstInnerAlignWithPole, pixelPosFirstInnerAlignWithPole);

                        TextGlyph textGlyph = createTextGlyph(coordFirst.geoPos.getLatString(latToUse, formatCompass, formatDecimal), coordFirstAlignWithPole, coordFirstInnerAlignWithPole, 0, 0);
                        textGlyphs.add(textGlyph);
                    } else {
                        TextGlyph textGlyph = createTextGlyph(coordFirst.geoPos.getLatString(latToUse, formatCompass, formatDecimal), coordFirst, coordFirstInner, 0, 0);
                        textGlyphs.add(textGlyph);
                    }
                }




                if (allowLabelLast && coordLast != null && coordLastInner != null && isCoordPairValid(coordLast, coordLastInner)) {

                    double lat = coordLast.geoPos.lat;
                    double latToUse = lat;

                    if (alignWithPole && polarCoord != null) {
                        double deltaY = polarCoord.getY() - coordLast.pixelPos.getY() + centerShiftExtra;

                        PixelPos pixelPosLastAlignWithPole = new PixelPos((float) (coordLast.pixelPos.getX()), (float) (coordLast.pixelPos.getY() + deltaY));
                        GeoPos geoPosLastAlignWithPole = raster.getGeoCoding().getGeoPos(pixelPosLastAlignWithPole, null);
                        Coord coordLastAlignWithPole = new Coord(geoPosLastAlignWithPole, pixelPosLastAlignWithPole);

                        PixelPos pixelPosLastInnerAlignWithPole = new PixelPos((float) (coordLastInner.pixelPos.getX()), (float) (coordLastInner.pixelPos.getY() + deltaY));
                        GeoPos geoPosLastInnerAlignWithPole = raster.getGeoCoding().getGeoPos(pixelPosLastInnerAlignWithPole, null);
                        Coord coordLastInnerAlignWithPole = new Coord(geoPosLastInnerAlignWithPole, pixelPosLastInnerAlignWithPole);

                        TextGlyph textGlyph = createTextGlyph(coordLast.geoPos.getLatString(latToUse, formatCompass, formatDecimal), coordLastAlignWithPole, coordLastInnerAlignWithPole, 0, 0);
                        textGlyphs.add(textGlyph);
                    } else {
                        TextGlyph textGlyph = createTextGlyph(coordLast.geoPos.getLatString(latToUse, formatCompass, formatDecimal), coordLast, coordLastInner, 0, 0);
                        textGlyphs.add(textGlyph);
                    }

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
        if (coord1 == null || coord2 == null) {
            return false;
        }
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
        private double x;
        private double y;
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


    static public class GraticuleParameters {

        public String mode = GraticuleLayerType.PROPERTY_MODE_DEFAULT;

        public double autoSpacingLatGlobal = GraticuleLayerType.PROPERTY_AUTO_SPACING_LAT_GLOBAL_DEFAULT;
        public double autoSpacingLonGlobal = GraticuleLayerType.PROPERTY_AUTO_SPACING_LON_GLOBAL_DEFAULT;
        public double autoSpacingLatHemispherical = GraticuleLayerType.PROPERTY_AUTO_SPACING_LAT_HEMISPHERICAL_DEFAULT;
        public double autoSpacingLonHemispherical = GraticuleLayerType.PROPERTY_AUTO_SPACING_LON_HEMISPHERICAL_DEFAULT;
        public double autoSpacingLatGlobalCylindrical = GraticuleLayerType.PROPERTY_AUTO_SPACING_LAT_GLOBAL_CYLINDRICAL_DEFAULT;
        public double autoSpacingLonGlobalCylindrical = GraticuleLayerType.PROPERTY_AUTO_SPACING_LON_GLOBAL_CYLINDRICAL_DEFAULT;

        public double gridSpacingLat = GraticuleLayerType.PROPERTY_GRID_SPACING_LAT_DEFAULT;
        public double gridSpacingLon = GraticuleLayerType.PROPERTY_GRID_SPACING_LON_DEFAULT;


        public int desiredNumGridLines = GraticuleLayerType.PROPERTY_NUM_GRID_LINES_DEFAULT;
        public int desiredMinorSteps = GraticuleLayerType.PROPERTY_MINOR_STEPS_DEFAULT;
        public int desiredMinorStepsCylindrical = GraticuleLayerType.PROPERTY_MINOR_STEPS_CYLINDRICAL_DEFAULT;


        public boolean interpolate = GraticuleLayerType.PROPERTY_INTERPOLATE_DEFAULT;
        public double toleranceParallels = GraticuleLayerType.PROPERTY_TOLERANCE_PARALLELS_DEFAULT;
        public double toleranceMeridians = GraticuleLayerType.PROPERTY_TOLERANCE_MERIDIANS_DEFAULT;
        public boolean formatCompass = GraticuleLayerType.PROPERTY_LABELS_SUFFIX_NSWE_DEFAULT;
        public boolean decimalFormat = GraticuleLayerType.PROPERTY_LABELS_DECIMAL_VALUE_DEFAULT;
        public int spacer = GraticuleLayerType.PROPERTY_EDGE_LABELS_SPACER_DEFAULT;


        public GraticuleParameters() {
        }

        public void setAutoSpacingLatGlobal(double autoSpacingLatGlobal) {
            this.autoSpacingLatGlobal = autoSpacingLatGlobal;
        }

        public void setGridSpacingLat(double gridSpacingLat) {
            this.gridSpacingLat = gridSpacingLat;
        }

        public double getAutoSpacingLatGlobal() {
            return autoSpacingLatGlobal;
        }

        public double getGridSpacingLat() {
            return gridSpacingLat;
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

