/*
 * Copyright (C) 2024 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.snap.stac.internal;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opengis.geometry.BoundingBox;

public class GeoCodingSupport {

    private static final String POLYGON = "POLYGON";
    private static final String COORDINATES = "coordinates";

    private GeoCodingSupport() {
    }

    public static BoundingBox getBoundingBox(final Product product) {
        final GeoCoding geoCoding = product.getSceneGeoCoding();
        final PixelPos upperLeftPP = new PixelPos(0, 0);
        final PixelPos lowerRightPP = new PixelPos(product.getSceneRasterWidth(), product.getSceneRasterHeight());
        final GeoPos upperLeftGP = geoCoding.getGeoPos(upperLeftPP, null);
        final GeoPos lowerRightGP = geoCoding.getGeoPos(lowerRightPP, null);
        final double north = upperLeftGP.getLat();
        final double south = lowerRightGP.getLat();
        double east = lowerRightGP.getLon();
        final double west = upperLeftGP.getLon();
        if (geoCoding.isCrossingMeridianAt180()) {
            east += 360;
        }

        return new ReferencedEnvelope(west, east, north, south, DefaultGeographicCRS.WGS84);
    }

    public static void addGeoCoding(final Product product, final JSONArray boundingBox) {

        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null.");
        }
        if (boundingBox == null || boundingBox.isEmpty()) {
            // Or log a warning and return, depending on desired behavior
            throw new IllegalArgumentException("Bounding box JSON array cannot be null or empty.");
        }

        double lonUL, latUL, lonUR, latUR, lonLL, latLL, lonLR, latLR;
        if (boundingBox.get(0) instanceof JSONArray) {
            // Handles format: [[lonUL, latUL], [lonLR, latLR]]
            if (boundingBox.size() < 2) {
                throw new IllegalArgumentException("Two-point bounding box requires 2 coordinate pairs.");
            }
            final JSONArray ul = (JSONArray) boundingBox.get(0);
            final JSONArray lr = (JSONArray) boundingBox.get(1);
            if (ul.size() < 2 || lr.size() < 2) {
                throw new IllegalArgumentException("Bounding box coordinate pairs must have 2 values (lon, lat).");
            }
            lonUL = ((Number) ul.get(0)).doubleValue();
            latUL = ((Number) ul.get(1)).doubleValue();
            lonLR = ((Number) lr.get(0)).doubleValue();
            latLR = ((Number) lr.get(1)).doubleValue();

            lonUR = lonLR;
            latUR = latUL;
            lonLL = lonUL;
            latLL = latLR;
        } else {
            // Handles STAC format: [west, south, east, north]
            if (boundingBox.size() < 4) {
                throw new IllegalArgumentException("Flat bounding box requires at least 4 values.");
            }
            final double west = ((Number) boundingBox.get(0)).doubleValue();
            final double south = ((Number) boundingBox.get(1)).doubleValue();
            final double east = ((Number) boundingBox.get(2)).doubleValue();
            final double north = ((Number) boundingBox.get(3)).doubleValue();

            lonUL = west;
            latUL = north;
            lonUR = east;
            latUR = north;
            lonLL = west;
            latLL = south;
            lonLR = east;
            latLR = south;
        }

        final double[] latCorners = new double[]{latUL, latUR, latLL, latLR};
        final double[] lonCorners = new double[]{lonUL, lonUR, lonLL, lonLR};

        ReaderUtils.addGeoCoding(product, latCorners, lonCorners);
    }

    public static String toWKT(final JSONObject geometry) {
        final JSONArray coords = (JSONArray)geometry.get(COORDINATES);

        final StringBuilder wkt = new StringBuilder();
        wkt.append(POLYGON).append("((");

        for(Object o : coords) {
            final JSONArray latlons = (JSONArray)o;
            for(Object ll : latlons) {
                final JSONArray latlon = (JSONArray) ll;
                wkt.append(latlon.get(0))
                        .append(" ")
                        .append(latlon.get(1))
                        .append(",");
            }
        }

        wkt.setLength(wkt.length() - 1);
        wkt.append("))");
        return wkt.toString();
    }
}
