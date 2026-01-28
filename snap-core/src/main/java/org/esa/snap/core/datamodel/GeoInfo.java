package org.esa.snap.core.datamodel;

public class GeoInfo {

    public static final double NULL_LON = -99999;
    public static final double NULL_LAT = -99999;

    public enum DIRECTION {
        NOT_SET,
        ASCENDING,
        DESCENDING
    }

    public double firstLon = NULL_LON;
    public double lastLon = NULL_LON;
    public double lonSpan = 0;

    public double bottomLat = NULL_LAT;
    public double topLat = NULL_LAT;
    public double minLat = NULL_LAT;
    public double maxLat = NULL_LAT;
    public double latSpan = 0;

    public boolean datelineCrossed = false;
    public boolean northPoleCrossed = false;
    public boolean southPoleCrossed = false;

    public boolean lonAscending = false;
    public boolean lonDescending = false;
    public boolean latAscending = false;
    public boolean latDescending = false;

    public boolean equidistantCylindrical = false;
    public CoordsPolar coordsPolar = null;

    private boolean forceCheckForPolar = false;


    public GeoInfo(GeoCoding geoCoding, RasterDataNode dataNode) {
        this(geoCoding, dataNode, false);
    }


    public GeoInfo(GeoCoding geoCoding, RasterDataNode dataNode, boolean forceCheckForPolar) {
        this.forceCheckForPolar = forceCheckForPolar;

        init_defaults();
        apply(geoCoding, dataNode);
    }


    private void apply(GeoCoding geoCoding, RasterDataNode dataNode) {

        GeoSpanLon lonSpanScene = getLonSpan(geoCoding, dataNode);

        if (lonSpanScene != null && lonSpanScene.lonSpan > 0) {
            GeoSpanLat latSpanScene = getLatSpan(geoCoding, dataNode);

            if (latSpanScene != null && latSpanScene.latSpan > 0) {
                // todo add in a field to geospan regarding whether corner pixels have valid geo (this could be used for auto-border since if corner pixels are invalid a border might not be desired)

                lonSpan = lonSpanScene.lonSpan;
                firstLon = lonSpanScene.firstLon;
                lastLon = lonSpanScene.lastLon;
                lonAscending = lonSpanScene.ascending;
                lonDescending = lonSpanScene.descending;

                datelineCrossed = lonSpanScene.datelineCrossed;

                latSpan = latSpanScene.latSpan;
                bottomLat = latSpanScene.firstLat;
                topLat = latSpanScene.lastLat;
                minLat = latSpanScene.minLat;
                maxLat = latSpanScene.maxLat;
                latAscending = latSpanScene.ascending;
                latDescending = latSpanScene.descending;

                northPoleCrossed = lonSpanScene.northPoleCrossed || latSpanScene.northPoleCrossed;
                southPoleCrossed = lonSpanScene.southPoleCrossed || latSpanScene.southPoleCrossed;


                equidistantCylindrical = isEquidistantCylindrical(geoCoding, dataNode);

                if (northPoleCrossed || southPoleCrossed || forceCheckForPolar) {
                    coordsPolar = getCoordsPolar(geoCoding, dataNode);

                    if (coordsPolar != null) {

                        if (coordsPolar.northernmostGeoPos != null) {
                            maxLat = coordsPolar.northernmostGeoPos.lat;
                        }

                        if (coordsPolar.northPoleCrossingDetected) {
                            northPoleCrossed = true;
                        }


                        if (coordsPolar.southernmostGeoPos != null) {
                            minLat = coordsPolar.southernmostGeoPos.lat;
                        }

                        if (coordsPolar.southPoleCrossingDetected) {
                            southPoleCrossed = true;
                        }


                    }
                }

            }
        }

    }


    private void init_defaults() {
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


    private static GeoSpanLon getLonSpan(GeoCoding geoCoding, RasterDataNode dataNode) {

        GeoSpanLon lonSpanCombined = new GeoSpanLon();


        int pixelYCenterRow = (int) Math.floor((dataNode.getRasterHeight() - 1) * 0.5);
//        int centerHeight = (int) Math.floor(dataNode.getRasterHeight() * 0.5);
        GeoSpanLon lonSpanCenterRow = getLonSpanForRowAtPixelY(geoCoding, dataNode, pixelYCenterRow);
        if (lonSpanCenterRow != null) {
            combineLonSpans(lonSpanCombined, lonSpanCenterRow);
        }


        // todo testing new
        int pixelYNearTopRow = (int) Math.floor((dataNode.getRasterHeight() - 1) * 0.25);
        GeoSpanLon lonSpanNearTopRow = getLonSpanForRowAtPixelY(geoCoding, dataNode, pixelYNearTopRow);
        if (lonSpanNearTopRow != null) {
            combineLonSpans(lonSpanCombined, lonSpanNearTopRow);
        }

        // todo testing new
        int pixelYNearBottomRow = (int) Math.floor((dataNode.getRasterHeight() - 1) * 0.75);
        GeoSpanLon lonSpanNearBottomRow = getLonSpanForRowAtPixelY(geoCoding, dataNode, pixelYNearBottomRow);
        if (lonSpanNearTopRow != null) {
            combineLonSpans(lonSpanCombined, lonSpanNearBottomRow);
        }


        int pixelYTopRow = 0;
        GeoSpanLon lonSpanTopRow = getLonSpanForRowAtPixelY(geoCoding, dataNode, pixelYTopRow);
        if (lonSpanTopRow != null) {
            combineLonSpans(lonSpanCombined, lonSpanTopRow);
        }


        int pixelYBottomRow = dataNode.getRasterHeight() - 1;
        GeoSpanLon lonSpanBottomRow = getLonSpanForRowAtPixelY(geoCoding, dataNode, pixelYBottomRow);
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

            lonSpanCalculated = Math.abs(lonSpanCalculated);


            lonSpanCombined.lonSpan = Math.max(lonSpanCalculated, lonSpanCombined.lonSpan);

        }


        if (validLonSpan(lonSpanCombined)) {
            return lonSpanCombined;
        } else {
            return null;
        }

    }


    private static GeoSpanLon getLonSpanForRowAtPixelY(GeoCoding geoCoding, RasterDataNode dataNode, int pixelYCurr) {

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

        for (double pixelXCurr = 0.0; pixelXCurr <= (dataNode.getRasterWidth() - 1); pixelXCurr++) {

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


                        if (Math.abs(geoPosCurr.lon - geoPosPrev.lon) >= (360 - DATELINE_DEGREES_BUFFER)) {
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


        degreesSpanTotal = Math.abs(degreesSpanTotal);

        if (firstLon != NULL_LON && lastLon != NULL_LON) {
            GeoSpanLon geoSpanLon = new GeoSpanLon(firstLon, lastLon, degreesSpanTotal, datelineCrossed, northPoleCrossed, southPoleCrossed, ascending, descending);
            return geoSpanLon;
        } else {
            return null;
        }
    }


    private static boolean validLonSpan(GeoSpanLon geoSpanLon) {

        boolean valid = (geoSpanLon.lonSpan > 0 && validLon(geoSpanLon.firstLon) && validLon(geoSpanLon.lastLon));

        return valid;
    }


    private static GeoSpanLon combineLonSpans(GeoSpanLon lonSpanCombined, GeoSpanLon lonSpanNewRow) {


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


    private static GeoSpanLat getLatSpan(GeoCoding geoCoding, RasterDataNode dataNode) {

        GeoSpanLat latSpanCombined = new GeoSpanLat();


        int pixelXCenterColumn = (int) Math.floor((dataNode.getRasterWidth() - 1) * 0.5);
//        int centerWidth = (int) Math.floor(dataNode.getRasterWidth() / 2.0);
        GeoSpanLat latSpanCenterColumn = getLatSpanForColumnAtPixelX(geoCoding, dataNode, pixelXCenterColumn);
        if (latSpanCenterColumn != null) {
            combineLatSpans(latSpanCombined, latSpanCenterColumn);
        }


        // todo testing new
        int pixelXNearLeftColumnPixelX = (int) Math.floor((dataNode.getRasterWidth() - 1) * 0.25);
        GeoSpanLat latSpanNearLeftColumn = getLatSpanForColumnAtPixelX(geoCoding, dataNode, pixelXNearLeftColumnPixelX);
        if (latSpanNearLeftColumn != null) {
            combineLatSpans(latSpanCombined, latSpanNearLeftColumn);
        }

        // todo testing new
        int pixelXNearRightColumnPixelX = (int) Math.floor((dataNode.getRasterWidth() - 1) * 0.75);
        GeoSpanLat latSpanNearRightColumn = getLatSpanForColumnAtPixelX(geoCoding, dataNode, pixelXNearRightColumnPixelX);
        if (latSpanNearLeftColumn != null) {
            combineLatSpans(latSpanCombined, latSpanNearRightColumn);
        }


        int pixelXLeftColumn = 0;
        GeoSpanLat latSpanLeftColumn = getLatSpanForColumnAtPixelX(geoCoding, dataNode, pixelXLeftColumn);
        if (latSpanLeftColumn != null) {
            combineLatSpans(latSpanCombined, latSpanLeftColumn);
        }


        int pixelXRightColumn = dataNode.getRasterWidth() - 1;
        GeoSpanLat latSpanRightColumn = getLatSpanForColumnAtPixelX(geoCoding, dataNode, pixelXRightColumn);
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


    private static boolean validLatSpan(GeoSpanLat geoSpanLat) {

        boolean valid = (geoSpanLat.latSpan > 0 &&
                validLat(geoSpanLat.firstLat) &&
                validLat(geoSpanLat.lastLat) &&
                validLat(geoSpanLat.minLat) &&
                validLat(geoSpanLat.maxLat));

        return valid;

    }


    private static GeoSpanLat combineLatSpans(GeoSpanLat latSpanCombined, GeoSpanLat latSpanNewColumn) {


        boolean newRowValid = validLatSpan(latSpanNewColumn);
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


    private static GeoSpanLat getLatSpanForColumnAtPixelX(GeoCoding geoCoding, RasterDataNode dataNode, int pixelXCurr) {
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


        double height = dataNode.getRasterHeight() - 1;
        for (double pixelYCurr = height; pixelYCurr >= 0.0; pixelYCurr--) {

            PixelPos pixelPosCurr = new PixelPos(pixelXCurr, pixelYCurr);
            GeoPos geoPosCurr = geoCoding.getGeoPos(pixelPosCurr, null);

            if (validLat(geoPosCurr.lat)) {
                if (minLat == NULL_LAT || minLat > geoPosCurr.lat) {
                    minLat = geoPosCurr.lat;
                }

                if (maxLat == NULL_LAT || maxLat < geoPosCurr.lat) {
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


    private static boolean validLon(double lon) {
        if (lon >= -180 && lon <= 180) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean validLat(double lat) {
        if (lat >= -90 && lat <= 90) {
            return true;
        } else {
            return false;
        }
    }


    private static CoordsPolar getCoordsPolar(GeoCoding geoCoding, RasterDataNode dataNode) {

        boolean initialLatValueFound = false;

        GeoPos northernmostGeoPos = null;
        PixelPos northernmostPixelPos = null;
        GeoPos southernmostGeoPos = null;
        PixelPos southernmostPixelPos = null;

        boolean northernmostPixelOnRasterEdge = true;
        boolean southernmostPixelOnRasterEdge = true;

        boolean northPoleCrossingDetected = false;
        boolean southPoleCrossingDetected = false;


        for (double pixelXCurr = 0.0; pixelXCurr <= (dataNode.getRasterWidth() - 1); pixelXCurr++) {
            for (double pixelYCurr = 0.0; pixelYCurr <= (dataNode.getRasterHeight() - 1); pixelYCurr++) {

                PixelPos pixelPosCurr = new PixelPos(pixelXCurr, pixelYCurr);
                GeoPos geoPosCurr = geoCoding.getGeoPos(pixelPosCurr, null);


                if (validLat(geoPosCurr.lat) && validLon(geoPosCurr.lon)) {
                    if (initialLatValueFound) {
                        if (geoPosCurr.lat > northernmostGeoPos.lat) {
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
//            Graticule.Coord northernmostCoord = new Graticule.Coord(northernmostGeoPos, northernmostPixelPos);
//            Graticule.Coord southernmostCoord = new Graticule.Coord(southernmostGeoPos, southernmostPixelPos);

            return new CoordsPolar(northernmostGeoPos, northernmostPixelPos, southernmostGeoPos, southernmostPixelPos, northernmostPixelOnRasterEdge, southernmostPixelOnRasterEdge, northPoleCrossingDetected, southPoleCrossingDetected);
//            return  new CoordsPolar(northernmostCoord, southernmostCoord, northernmostPixelOnRasterEdge, southernmostPixelOnRasterEdge, northPoleCrossingDetected, southPoleCrossingDetected);

        } else {
            return null;
        }

    }


    private static boolean isEquidistantCylindrical(GeoCoding geoCoding, RasterDataNode dataNode) {

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


    private static boolean isParallelLineEquidistantCylindrical(GeoCoding geoCoding, RasterDataNode dataNode, int pixelYCurr) {

        boolean equidistantCylindrical = true;

        if (pixelYCurr < 0 || pixelYCurr >= dataNode.getRasterHeight()) {
            //ensure valid index
            return true;
        }

        double NULL_LAT = -99999;
        double validLatValue = NULL_LAT;
        boolean initialLatValueFound = false;

        for (double pixelXCurr = 0.0; pixelXCurr <= (dataNode.getRasterWidth() - 1); pixelXCurr++) {

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


    private static boolean isMeridianLineEquidistantCylindrical(GeoCoding geoCoding, RasterDataNode dataNode, int pixelXCurr) {

        boolean equidistantCylindrical = true;

        if (pixelXCurr < 0 || pixelXCurr >= dataNode.getRasterWidth()) {
            //ensure valid index
            return true;
        }

        double NULL_LON = -99999;
        double validLonValue = NULL_LON;
        boolean initialLonValueFound = false;

        for (double pixelYCurr = 0.0; pixelYCurr <= (dataNode.getRasterHeight() - 1); pixelYCurr++) {

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


    // todo improve on this

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


    public static class GeoSpanLon {
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

        public static void GeoSpanLonUpdate(GeoSpanLon geoSpanLonTarget, GeoSpanLon geoSpanLonOriginal) {

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


    public static class GeoSpanLat {
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


        public static void GeoSpanLatUpdate(GeoSpanLat geoSpanLatTarget, GeoSpanLat geoSpanLatOriginal) {

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


    static public class CoordsPolar {

        GeoPos northernmostGeoPos = null;
        PixelPos northernmostPixelPos = null;
        GeoPos southernmostGeoPos = null;
        PixelPos southernmostPixelPos = null;

        //        Graticule.Coord coordNorthernmost;
//        Graticule.Coord coordSouthernmost;
        boolean northernmostPixelOnRasterEdge;
        boolean southernmostPixelOnRasterEdge;
        boolean northPoleCrossingDetected;
        boolean southPoleCrossingDetected;

        public CoordsPolar(GeoPos northernmostGeoPos,
                           PixelPos northernmostPixelPos,
                           GeoPos southernmostGeoPos,
                           PixelPos southernmostPixelPos,
                           boolean northernmostPixelOnRasterEdge,
                           boolean southernmostPixelOnRasterEdge,
                           boolean northPoleCrossingDetected,
                           boolean southPoleCrossingDetected) {
            this.northernmostGeoPos = northernmostGeoPos;
            this.northernmostPixelPos = northernmostPixelPos;
            this.southernmostGeoPos = southernmostGeoPos;
            this.southernmostPixelPos = southernmostPixelPos;
            this.northernmostPixelOnRasterEdge = northernmostPixelOnRasterEdge;
            this.southernmostPixelOnRasterEdge = southernmostPixelOnRasterEdge;
            this.northPoleCrossingDetected = northPoleCrossingDetected;
            this.southPoleCrossingDetected = southPoleCrossingDetected;
        }


//        public CoordsPolar(Graticule.Coord coordNorthernmost,
//                           Graticule.Coord coordSouthernmost,
//                           boolean northernmostPixelOnRasterEdge,
//                           boolean southernmostPixelOnRasterEdge,
//                           boolean northPoleCrossingDetected,
//                           boolean southPoleCrossingDetected) {
//            this.coordNorthernmost = coordNorthernmost;
//            this.coordSouthernmost = coordSouthernmost;
//            this.northernmostPixelOnRasterEdge = northernmostPixelOnRasterEdge;
//            this.southernmostPixelOnRasterEdge = southernmostPixelOnRasterEdge;
//            this.northPoleCrossingDetected = northPoleCrossingDetected;
//            this.southPoleCrossingDetected = southPoleCrossingDetected;
//        }
    }


}
