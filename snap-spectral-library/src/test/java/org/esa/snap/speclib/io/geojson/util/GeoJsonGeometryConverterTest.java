package org.esa.snap.speclib.io.geojson.util;

import com.bc.ceres.annotation.STTM;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import static org.junit.Assert.*;


public class GeoJsonGeometryConverterTest {


    private final ObjectMapper mapper = new ObjectMapper();


    @Test
    @STTM("SNAP-4177")
    public void test_toWkt_nullNode_returnsNull() {
        assertNull(GeoJsonGeometryConverter.toWkt(null));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toWkt_jsonNullNode_returnsNull() {
        assertNull(GeoJsonGeometryConverter.toWkt(mapper.nullNode()));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toWkt_missingTypeField_returnsNull() {
        ObjectNode node = mapper.createObjectNode();
        node.putArray("coordinates").add(1.0).add(2.0);

        assertNull(GeoJsonGeometryConverter.toWkt(node));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toWkt_missingCoordinatesField_returnsNull() {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "Point");

        assertNull(GeoJsonGeometryConverter.toWkt(node));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toWkt_unknownType_returnsNull() {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "GeometryCollection");
        node.putArray("coordinates");

        assertNull(GeoJsonGeometryConverter.toWkt(node));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_toWkt_point2D() {
        assertEquals("POINT(13.5 52.1)", GeoJsonGeometryConverter.toWkt(point(13.5, 52.1)));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toWkt_point3D() {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "Point");

        ArrayNode coords = node.putArray("coordinates");
        coords.add(10.0); coords.add(20.0); coords.add(100.0);

        String wkt = GeoJsonGeometryConverter.toWkt(node);
        assertEquals("POINT(10.0 20.0 100.0)", wkt);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toWkt_point_tooFewCoords_fallback() {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "Point");
        node.putArray("coordinates").add(1.0);

        String wkt = GeoJsonGeometryConverter.toWkt(node);
        assertEquals("POINT(0 0)", wkt);
    }


    @Test
    @STTM("SNAP-4177")
    public void test_toWkt_lineString() {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "LineString");
        ArrayNode coords = node.putArray("coordinates");

        addPt(coords, 0.0, 0.0);
        addPt(coords, 1.0, 1.0);

        assertEquals("LINESTRING(0.0 0.0, 1.0 1.0)", GeoJsonGeometryConverter.toWkt(node));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toWkt_lineString3D() {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "LineString");
        ArrayNode coords = node.putArray("coordinates");

        addPt(coords, 0.0, 0.0, 5.0);
        addPt(coords, 1.0, 1.0, 10.0);

        String wkt = GeoJsonGeometryConverter.toWkt(node);
        assertEquals("LINESTRING(0.0 0.0 5.0, 1.0 1.0 10.0)", wkt);
    }


    @Test
    @STTM("SNAP-4177")
    public void test_toWkt_polygonSingleRing() {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "Polygon");
        ArrayNode rings = node.putArray("coordinates");
        ArrayNode ring = rings.addArray();

        addPt(ring, 0.0, 0.0); addPt(ring, 1.0, 0.0);
        addPt(ring, 1.0, 1.0); addPt(ring, 0.0, 0.0);

        String wkt = GeoJsonGeometryConverter.toWkt(node);
        assertEquals("POLYGON((0.0 0.0, 1.0 0.0, 1.0 1.0, 0.0 0.0))", wkt);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toWkt_polygonWithHole() {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "Polygon");
        ArrayNode rings = node.putArray("coordinates");

        ArrayNode outer = rings.addArray();
        addPt(outer, 0.0, 0.0); addPt(outer, 4.0, 0.0);
        addPt(outer, 4.0, 4.0); addPt(outer, 0.0, 0.0);

        ArrayNode hole = rings.addArray();
        addPt(hole, 1.0, 1.0); addPt(hole, 2.0, 1.0);
        addPt(hole, 2.0, 2.0); addPt(hole, 1.0, 1.0);

        String wkt = GeoJsonGeometryConverter.toWkt(node);
        assertTrue(wkt.startsWith("POLYGON("));
        assertTrue(wkt.contains(", ("));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_toWkt_multiPoint() {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "MultiPoint");
        ArrayNode coords = node.putArray("coordinates");

        addPt(coords, 1.0, 2.0);
        addPt(coords, 3.0, 4.0);

        String wkt = GeoJsonGeometryConverter.toWkt(node);
        assertEquals("MULTIPOINT((1.0 2.0), (3.0 4.0))", wkt);
    }


    @Test
    @STTM("SNAP-4177")
    public void test_toWkt_multiLineString() {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "MultiLineString");
        ArrayNode lines = node.putArray("coordinates");

        ArrayNode line1 = lines.addArray();
        addPt(line1, 0.0, 0.0); addPt(line1, 1.0, 1.0);

        ArrayNode line2 = lines.addArray();
        addPt(line2, 2.0, 2.0); addPt(line2, 3.0, 3.0);

        String wkt = GeoJsonGeometryConverter.toWkt(node);
        assertEquals("MULTILINESTRING((0.0 0.0, 1.0 1.0), (2.0 2.0, 3.0 3.0))", wkt);
    }


    @Test
    @STTM("SNAP-4177")
    public void test_toWkt_multiPolygon() {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "MultiPolygon");
        ArrayNode polys = node.putArray("coordinates");

        ArrayNode poly1 = polys.addArray();
        ArrayNode ring1 = poly1.addArray();
        addPt(ring1, 0.0, 0.0); addPt(ring1, 1.0, 0.0);
        addPt(ring1, 1.0, 1.0); addPt(ring1, 0.0, 0.0);

        ArrayNode poly2 = polys.addArray();
        ArrayNode ring2 = poly2.addArray();
        addPt(ring2, 2.0, 2.0); addPt(ring2, 3.0, 2.0);
        addPt(ring2, 3.0, 3.0); addPt(ring2, 2.0, 2.0);

        String wkt = GeoJsonGeometryConverter.toWkt(node);
        assertTrue(wkt.startsWith("MULTIPOLYGON("));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_toGeoJson_null_returnsNull() {
        assertNull(GeoJsonGeometryConverter.toGeoJson(null, mapper));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toGeoJson_blank_returnsNull() {
        assertNull(GeoJsonGeometryConverter.toGeoJson("   ", mapper));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toGeoJson_unknownType_returnsNull() {
        assertNull(GeoJsonGeometryConverter.toGeoJson("CIRCLE(0 0, 1)", mapper));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toGeoJson_malformed_returnsNull() {
        assertNull(GeoJsonGeometryConverter.toGeoJson("POINT", mapper));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toGeoJson_pointTooFewCoords_returnsNull() {
        assertNull(GeoJsonGeometryConverter.toGeoJson("POINT(1.0)", mapper));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_toGeoJson_point2D() {
        ObjectNode geom = GeoJsonGeometryConverter.toGeoJson("POINT(13.5 52.1)", mapper);

        assertNotNull(geom);
        assertEquals("Point", geom.get("type").asText());

        ArrayNode coords = (ArrayNode) geom.get("coordinates");
        assertEquals(2, coords.size());
        assertEquals(13.5, coords.get(0).doubleValue(), 1e-9);
        assertEquals(52.1, coords.get(1).doubleValue(), 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toGeoJson_point3D() {
        ObjectNode geom = GeoJsonGeometryConverter.toGeoJson("POINT(1.0 2.0 3.0)", mapper);
        assertNotNull(geom);

        ArrayNode coords = (ArrayNode) geom.get("coordinates");
        assertEquals(3, coords.size());
        assertEquals(3.0, coords.get(2).doubleValue(), 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toGeoJson_point_caseInsensitive() {
        assertNotNull(GeoJsonGeometryConverter.toGeoJson("point(1.0 2.0)", mapper));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_toGeoJson_lineString() {
        ObjectNode geom = GeoJsonGeometryConverter.toGeoJson("LINESTRING(0 0, 1 1, 2 2)", mapper);
        assertNotNull(geom);
        assertEquals("LineString", geom.get("type").asText());

        ArrayNode coords = (ArrayNode) geom.get("coordinates");
        assertEquals(3, coords.size());
        assertEquals(1.0, coords.get(1).get(0).doubleValue(), 1e-9);
    }


    @Test
    @STTM("SNAP-4177")
    public void test_toGeoJson_polygon() {
        ObjectNode geom = GeoJsonGeometryConverter.toGeoJson("POLYGON((0 0, 1 0, 1 1, 0 0))", mapper);
        assertNotNull(geom);
        assertEquals("Polygon", geom.get("type").asText());

        ArrayNode rings = (ArrayNode) geom.get("coordinates");
        assertEquals(1, rings.size());
        assertEquals(4, rings.get(0).size());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toGeoJson_polygonWithHole() {
        ObjectNode geom = GeoJsonGeometryConverter.toGeoJson("POLYGON((0 0, 4 0, 4 4, 0 0), (1 1, 2 1, 2 2, 1 1))", mapper);
        assertNotNull(geom);

        ArrayNode rings = (ArrayNode) geom.get("coordinates");
        assertEquals(2, rings.size());
    }


    @Test
    @STTM("SNAP-4177")
    public void test_toGeoJson_multiPoint() {
        ObjectNode geom = GeoJsonGeometryConverter.toGeoJson("MULTIPOINT((0 0), (1 1))", mapper);
        assertNotNull(geom);
        assertEquals("MultiPoint", geom.get("type").asText());

        ArrayNode pts = (ArrayNode) geom.get("coordinates");
        assertEquals(2, pts.size());
    }


    @Test
    @STTM("SNAP-4177")
    public void test_toGeoJson_multiLineString() {
        ObjectNode geom = GeoJsonGeometryConverter.toGeoJson("MULTILINESTRING((0 0, 1 1), (2 2, 3 3))", mapper);
        assertNotNull(geom);
        assertEquals("MultiLineString", geom.get("type").asText());
        assertEquals(2, geom.get("coordinates").size());
    }


    @Test
    @STTM("SNAP-4177")
    public void test_toGeoJson_multiPolygon() {
        ObjectNode geom = GeoJsonGeometryConverter.toGeoJson("MULTIPOLYGON(((0 0, 1 0, 1 1, 0 0)), ((2 2, 3 2, 3 3, 2 2)))", mapper);
        assertNotNull(geom);
        assertEquals("MultiPolygon", geom.get("type").asText());
        assertEquals(2, geom.get("coordinates").size());
    }


    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_point() {
        assertWktRoundtrip("POINT(10.0 20.0)");
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_lineString() {
        assertWktRoundtrip("LINESTRING(0.0 0.0, 1.0 1.0, 2.0 2.0)");
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_polygon() {
        assertWktRoundtrip("POLYGON((0.0 0.0, 1.0 0.0, 1.0 1.0, 0.0 0.0))");
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_multiPoint() {
        String original = "MULTIPOINT((1.0 2.0), (3.0 4.0))";
        ObjectNode geom = GeoJsonGeometryConverter.toGeoJson(original, mapper);
        String back = GeoJsonGeometryConverter.toWkt(geom);

        assertNotNull(back);
        assertTrue(back.startsWith("MULTIPOINT"));
        assertTrue(back.contains("1.0 2.0"));
        assertTrue(back.contains("3.0 4.0"));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_multiLineString() {
        String original = "MULTILINESTRING((0.0 0.0, 1.0 1.0), (2.0 2.0, 3.0 3.0))";
        ObjectNode geom = GeoJsonGeometryConverter.toGeoJson(original, mapper);
        String back = GeoJsonGeometryConverter.toWkt(geom);

        assertNotNull(back);
        assertTrue(back.startsWith("MULTILINESTRING"));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_multiPolygon() {
        String original = "MULTIPOLYGON(((0.0 0.0, 1.0 0.0, 1.0 1.0, 0.0 0.0)), ((2.0 2.0, 3.0 2.0, 3.0 3.0, 2.0 2.0)))";
        ObjectNode geom = GeoJsonGeometryConverter.toGeoJson(original, mapper);
        String back = GeoJsonGeometryConverter.toWkt(geom);

        assertNotNull(back);
        assertTrue(back.startsWith("MULTIPOLYGON"));
    }



    private ObjectNode point(double lon, double lat) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "Point");
        node.putArray("coordinates").add(lon).add(lat);
        return node;
    }

    private void addPt(ArrayNode parent, double x, double y) {
        parent.addArray().add(x).add(y);
    }

    private void addPt(ArrayNode parent, double x, double y, double z) {
        parent.addArray().add(x).add(y).add(z);
    }

    private void assertWktRoundtrip(String wkt) {
        ObjectNode geom = GeoJsonGeometryConverter.toGeoJson(wkt, mapper);
        assertNotNull("toGeoJson returned null for: " + wkt, geom);
        String back = GeoJsonGeometryConverter.toWkt(geom);
        assertEquals(wkt, back);
    }
}
