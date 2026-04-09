package org.esa.snap.speclib.io.geojson.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Converts between WKT geometry strings and GeoJSON geometry objects.
 */
public final class GeoJsonGeometryConverter {


    private static final String TYPE = "type";
    private static final String COORDINATES = "coordinates";


    private GeoJsonGeometryConverter() {}


    /**
     * Converts a GeoJSON geometry node to a WKT string.
     * Returns {@code null} for null, empty, or unsupported geometry.
     */
    public static String toWkt(JsonNode geomNode) {
        if (geomNode == null || geomNode.isNull()) {
            return null;
        }

        JsonNode typeNode = geomNode.get(TYPE);
        if (typeNode == null) {
            return null;
        }

        JsonNode coords = geomNode.get(COORDINATES);
        if (coords == null) {
            return null;
        }

        return switch (typeNode.asText()) {
            case "Point" -> "POINT" + pointCoords(coords);
            case "LineString" -> "LINESTRING" + lineCoords(coords);
            case "Polygon" -> "POLYGON" + polygonCoords(coords);
            case "MultiPoint" -> "MULTIPOINT" + multiPointCoords(coords);
            case "MultiLineString" -> "MULTILINESTRING" + multiLineCoords(coords);
            case "MultiPolygon" -> "MULTIPOLYGON" + multiPolygonCoords(coords);
            default -> null;
        };
    }

    /**
     * Parses a WKT string into a GeoJSON geometry {@link ObjectNode}.
     * Returns {@code null} for blank, unrecognised, or malformed WKT.
     */
    public static ObjectNode toGeoJson(String wkt, ObjectMapper mapper) {
        if (wkt == null || wkt.isBlank()) {
            return null;
        }

        try {
            String upper = wkt.trim().toUpperCase();
            if (upper.startsWith("MULTIPOLYGON"))    {
                return buildMultiPolygon(wkt, mapper);
            }
            if (upper.startsWith("MULTILINESTRING")) {
                return buildMultiLinestring(wkt, mapper);
            }
            if (upper.startsWith("MULTIPOINT"))      {
                return buildMultiPoint(wkt, mapper);
            }
            if (upper.startsWith("POLYGON"))         {
                return buildPolygon(wkt, mapper);
            }
            if (upper.startsWith("LINESTRING"))      {
                return buildLinestring(wkt, mapper);
            }
            if (upper.startsWith("POINT"))           {
                return buildPoint(wkt, mapper);
            }
        } catch (Exception ignore) {}

        return null;
    }


    private static String pointCoords(JsonNode coords) {
        if (!coords.isArray() || coords.size() < 2) {
            return "(0 0)";
        }

        StringBuilder sb = new StringBuilder("(");
        for (int ii = 0; ii < coords.size(); ii++) {
            if (ii > 0) {
                sb.append(' ');
            }
            sb.append(coords.get(ii).doubleValue());
        }
        return sb.append(')').toString();
    }

    private static String lineCoords(JsonNode coords) {
        StringBuilder sb = new StringBuilder("(");
        for (int ii = 0; ii < coords.size(); ii++) {
            if (ii > 0) {
                sb.append(", ");
            }
            appendPoint(sb, coords.get(ii));
        }
        return sb.append(')').toString();
    }

    private static String polygonCoords(JsonNode coords) {
        StringBuilder sb = new StringBuilder("(");
        for (int ii = 0; ii < coords.size(); ii++) {
            if (ii > 0) {
                sb.append(", ");
            }
            sb.append(lineCoords(coords.get(ii)));
        }
        return sb.append(')').toString();
    }

    private static String multiPointCoords(JsonNode coords) {
        StringBuilder sb = new StringBuilder("(");
        for (int ii = 0; ii < coords.size(); ii++) {
            if (ii > 0) {
                sb.append(", ");
            }
            sb.append(pointCoords(coords.get(ii)));
        }
        return sb.append(')').toString();
    }

    private static String multiLineCoords(JsonNode coords) {
        StringBuilder sb = new StringBuilder("(");
        for (int ii = 0; ii < coords.size(); ii++) {
            if (ii > 0) {
                sb.append(", ");
            }
            sb.append(lineCoords(coords.get(ii)));
        }
        return sb.append(')').toString();
    }

    private static String multiPolygonCoords(JsonNode coords) {
        StringBuilder sb = new StringBuilder("(");
        for (int ii = 0; ii < coords.size(); ii++) {
            if (ii > 0) {
                sb.append(", ");
            }
            sb.append(polygonCoords(coords.get(ii)));
        }
        return sb.append(')').toString();
    }

    private static void appendPoint(StringBuilder sb, JsonNode pt) {
        sb.append(pt.get(0).doubleValue()).append(' ').append(pt.get(1).doubleValue());
        if (pt.size() > 2) {
            sb.append(' ').append(pt.get(2).doubleValue());
        }
    }


    private static ObjectNode buildPoint(String wkt, ObjectMapper mapper) {
        double[] coords = parseFlatCoords(extractContent(wkt));
        if (coords.length < 2) {
            return null;
        }

        ObjectNode node = geomNode("Point", mapper);
        ArrayNode arr = node.putArray(COORDINATES);

        for (double cc : coords) {
            arr.add(cc);
        }
        return node;
    }

    private static ObjectNode buildLinestring(String wkt, ObjectMapper mapper) {
        ObjectNode node = geomNode("LineString", mapper);
        addPointList(node.putArray(COORDINATES), extractContent(wkt));
        return node;
    }

    private static ObjectNode buildPolygon(String wkt, ObjectMapper mapper) {
        ObjectNode node = geomNode("Polygon", mapper);
        ArrayNode rings = node.putArray(COORDINATES);

        for (String ring : splitRings(extractContent(wkt))) {
            addPointList(rings.addArray(), ring);
        }
        return node;
    }

    private static ObjectNode buildMultiPoint(String wkt, ObjectMapper mapper) {
        ObjectNode node = geomNode("MultiPoint", mapper);
        ArrayNode arr = node.putArray(COORDINATES);

        for (String ring : splitRings(extractContent(wkt))) {
            double[] coords = parseFlatCoords(ring);
            ArrayNode ptArr = arr.addArray();
            for (double cc : coords) {
                ptArr.add(cc);
            }
        }
        return node;
    }

    private static ObjectNode buildMultiLinestring(String wkt, ObjectMapper mapper) {
        ObjectNode node = geomNode("MultiLineString", mapper);
        ArrayNode arr = node.putArray(COORDINATES);

        for (String ring : splitRings(extractContent(wkt))) {
            addPointList(arr.addArray(), ring);
        }
        return node;
    }

    private static ObjectNode buildMultiPolygon(String wkt, ObjectMapper mapper) {
        ObjectNode node = geomNode("MultiPolygon", mapper);
        ArrayNode arr = node.putArray(COORDINATES);

        for (String poly : splitPolygons(extractContent(wkt))) {
            ArrayNode polyArr = arr.addArray();
            for (String ring : splitRings(poly)) {
                addPointList(polyArr.addArray(), ring);
            }
        }
        return node;
    }

    private static ObjectNode geomNode(String type, ObjectMapper mapper) {
        ObjectNode node = mapper.createObjectNode();
        node.put(TYPE, type);
        return node;
    }

    private static void addPointList(ArrayNode target, String content) {
        for (double[] pt : parsePointList(content)) {
            ArrayNode ptArr = target.addArray();
            for (double cc : pt) {
                ptArr.add(cc);
            }
        }
    }


    private static String extractContent(String wkt) {
        int open  = wkt.indexOf('(');
        int close = wkt.lastIndexOf(')');

        if (open < 0 || close < 0) {
            return wkt;
        }
        return wkt.substring(open + 1, close).trim();
    }

    private static List<double[]> parsePointList(String content) {
        List<double[]> points = new ArrayList<>();

        for (String token : content.split(",")) {
            double[] coords = parseFlatCoords(token.trim());
            if (coords.length >= 2) {
                points.add(coords);
            }
        }
        return points;
    }

    private static double[] parseFlatCoords(String s) {
        String[] parts = s.trim().split("\\s+");
        double[] result = new double[parts.length];
        int count = 0;

        for (String p : parts) {
            try {
                result[count++] = Double.parseDouble(p);
            } catch (NumberFormatException ignored) {}
        }
        return count == parts.length ? result : Arrays.copyOf(result, count);
    }

    private static List<String> splitRings(String content) {
        List<String> rings = extractParenthesisedSegments(content);
        if (rings.isEmpty() && !content.isBlank()) {
            rings.add(content.trim());
        }
        return rings;
    }

    private static List<String> splitPolygons(String content) {
        return extractParenthesisedSegments(content);
    }

    private static List<String> extractParenthesisedSegments(String content) {
        List<String> segments = new ArrayList<>();
        int depth = 0;
        int start = -1;

        for (int ii = 0; ii < content.length(); ii++) {
            char c = content.charAt(ii);
            if (c == '(') {
                if (depth == 0) start = ii + 1;
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0 && start >= 0) {
                    segments.add(content.substring(start, ii).trim());
                    start = -1;
                }
            }
        }
        return segments;
    }
}
