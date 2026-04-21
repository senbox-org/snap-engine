package org.esa.snap.speclib.io.geojson.util;

import com.bc.ceres.annotation.STTM;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.esa.snap.speclib.model.AttributeSchema;
import org.esa.snap.speclib.model.AttributeType;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.model.SpectralAxis;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;


public class GeoJsonProfileSerializerTest {


    private final ObjectMapper mapper = new ObjectMapper();


    @Test(expected = IOException.class)
    @STTM("SNAP-4177")
    public void test_read_nullNode_throws() throws IOException {
        GeoJsonProfileSerializer.read(null, 0, new AttributeSchema());
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4177")
    public void test_read_notAnObject_throws() throws IOException {
        GeoJsonProfileSerializer.read(mapper.getNodeFactory().textNode("bad"), 0, new AttributeSchema());
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4177")
    public void test_read_missingType_throws() throws IOException {
        ObjectNode node = mapper.createObjectNode();
        node.putObject("properties");
        GeoJsonProfileSerializer.read(node, 0, new AttributeSchema());
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4177")
    public void test_read_wrongType_throws() throws IOException {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "FeatureCollection");
        node.putObject("properties");

        GeoJsonProfileSerializer.read(node, 0, new AttributeSchema());
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4177")
    public void test_read_missingProfilesObject_throws() throws IOException {
        ObjectNode node = featureNode();
        node.putObject("properties").put("name", "p");
        GeoJsonProfileSerializer.read(node, 0, new AttributeSchema());
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4177")
    public void test_read_profilesIsNotObject_throws() throws IOException {
        ObjectNode node = featureNode();
        ObjectNode props = node.putObject("properties");

        props.put("profiles", "not-an-object");
        GeoJsonProfileSerializer.read(node, 0, new AttributeSchema());
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4177")
    public void test_read_missingXArray_throws() throws IOException {
        ObjectNode node = featureNode();
        ObjectNode profiles = node.putObject("properties").putObject("profiles");

        profiles.putArray("y").add(1.0);
        GeoJsonProfileSerializer.read(node, 0, new AttributeSchema());
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4177")
    public void test_read_emptyXArray_throws() throws IOException {
        ObjectNode node = featureNode();
        ObjectNode profiles = node.putObject("properties").putObject("profiles");

        profiles.putArray("x");
        profiles.putArray("y");
        GeoJsonProfileSerializer.read(node, 0, new AttributeSchema());
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4177")
    public void test_read_xContainsNull_throws() throws IOException {
        ObjectNode node = featureNode();
        ObjectNode profiles = node.putObject("properties").putObject("profiles");

        profiles.putArray("x").addNull();
        profiles.putArray("y").add(1.0);
        GeoJsonProfileSerializer.read(node, 0, new AttributeSchema());
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4177")
    public void test_read_xContainsString_throws() throws IOException {
        ObjectNode node = featureNode();
        ObjectNode profiles = node.putObject("properties").putObject("profiles");

        profiles.putArray("x").add("bad");
        profiles.putArray("y").add(1.0);
        GeoJsonProfileSerializer.read(node, 0, new AttributeSchema());
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4177")
    public void test_read_missingYArray_throws() throws IOException {
        ObjectNode node = featureNode();
        ObjectNode profiles = node.putObject("properties").putObject("profiles");

        profiles.putArray("x").add(500.0);
        GeoJsonProfileSerializer.read(node, 0, new AttributeSchema());
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4177")
    public void test_read_yLengthMismatch_throws() throws IOException {
        ObjectNode node = featureNode();
        ObjectNode profiles = node.putObject("properties").putObject("profiles");

        profiles.putArray("x").add(500.0).add(600.0);
        profiles.putArray("y").add(1.0);
        GeoJsonProfileSerializer.read(node, 0, new AttributeSchema());
    }


    @Test
    @STTM("SNAP-4177")
    public void test_read_minimalFeature_parsedCorrectly() throws IOException {
        ObjectNode node = minimalFeature("MyProfile", new double[]{400.0, 500.0}, new double[]{1.0, 2.0}, "nm");
        GeoJsonProfileSerializer.ParsedFeature pf = GeoJsonProfileSerializer.read(node, 0, new AttributeSchema());

        assertEquals("nm", pf.axis().getXUnit());
        assertArrayEquals(new double[]{400.0, 500.0}, pf.axis().getWavelengths(), 1e-9);

        SpectralProfile p = pf.profile();
        assertEquals("MyProfile", p.getName());
        assertArrayEquals(new double[]{1.0, 2.0}, p.getSignature().getValues(), 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_noName_defaultsToSpectrumIndex() throws IOException {
        ObjectNode node = featureNode();
        ObjectNode profiles = node.putObject("properties").putObject("profiles");

        profiles.putArray("x").add(500.0);
        profiles.putArray("y").add(1.0);
        profiles.put("xUnit", "nm");

        GeoJsonProfileSerializer.ParsedFeature pf =
                GeoJsonProfileSerializer.read(node, 3, new AttributeSchema());
        assertEquals("Spectrum_4", pf.profile().getName());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_noProperties_defaultsToSpectrumIndex() throws IOException {
        ObjectNode node = featureNode();
        ObjectNode profiles = node.putObject("properties").putObject("profiles");

        profiles.putArray("x").add(500.0);
        profiles.putArray("y").add(1.0);
        profiles.put("xUnit", "nm");

        GeoJsonProfileSerializer.ParsedFeature pf =
                GeoJsonProfileSerializer.read(node, 1, new AttributeSchema());
        assertEquals("Spectrum_2", pf.profile().getName());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_xUnitMissing_defaultsToEmpty() throws IOException {
        ObjectNode node = featureNode();
        ObjectNode profiles = node.putObject("properties").putObject("profiles");

        profiles.putArray("x").add(500.0);
        profiles.putArray("y").add(1.0);

        GeoJsonProfileSerializer.ParsedFeature pf =
                GeoJsonProfileSerializer.read(node, 0, new AttributeSchema());
        assertEquals("", pf.axis().getXUnit());
    }


    @Test
    @STTM("SNAP-4177")
    public void test_read_nullInY_becomesNaN() throws IOException {
        ObjectNode node = featureNode();
        ObjectNode profiles = node.putObject("properties").putObject("profiles");

        profiles.putArray("x").add(400.0).add(500.0);
        profiles.putArray("y").addNull().add(2.0);
        profiles.put("xUnit", "nm");

        double[] values = GeoJsonProfileSerializer.read(node, 0, new AttributeSchema())
                .profile().getSignature().getValues();
        assertTrue(Double.isNaN(values[0]));
        assertEquals(2.0, values[1], 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_bblZeroOverridesRealY() throws IOException {
        ObjectNode node = featureNode();
        ObjectNode profiles = node.putObject("properties").putObject("profiles");

        profiles.putArray("x").add(400.0).add(500.0);
        profiles.putArray("y").add(99.0).add(2.0);  // 99.0 is a real value but bbl says bad
        profiles.putArray("bbl").add(0).add(1);
        profiles.put("xUnit", "nm");

        double[] values = GeoJsonProfileSerializer.read(node, 0, new AttributeSchema())
                .profile().getSignature().getValues();
        assertTrue("bbl=0 must override y=99", Double.isNaN(values[0]));
        assertEquals(2.0, values[1], 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_bblWrongSize_ignored() throws IOException {
        ObjectNode node = featureNode();
        ObjectNode profiles = node.putObject("properties").putObject("profiles");

        profiles.putArray("x").add(400.0).add(500.0);
        profiles.putArray("y").add(1.0).add(2.0);
        profiles.putArray("bbl").add(0);  // only 1 element for 2 bands
        profiles.put("xUnit", "nm");

        double[] values = GeoJsonProfileSerializer.read(node, 0, new AttributeSchema())
                .profile().getSignature().getValues();
        assertEquals(1.0, values[0], 1e-9);
        assertEquals(2.0, values[1], 1e-9);
    }


    @Test
    @STTM("SNAP-4177")
    public void test_read_pointGeometry_storedAsWktAttribute() throws IOException {
        ObjectNode node = minimalFeature("p", new double[]{500.0}, new double[]{1.0}, "nm");
        ObjectNode geom = node.putObject("geometry");
        geom.put("type", "Point");
        geom.putArray("coordinates").add(13.0).add(52.0);

        SpectralProfile profile = GeoJsonProfileSerializer.read(node, 0, new AttributeSchema()).profile();
        AttributeValue wkt = profile.getAttributes().get("wkt");

        assertNotNull(wkt);
        assertEquals(AttributeType.STRING, wkt.getType());
        assertEquals("POINT(13.0 52.0)", wkt.asString());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_nullGeometry_noWktAttribute() throws IOException {
        ObjectNode node = minimalFeature("p", new double[]{500.0}, new double[]{1.0}, "nm");
        node.putNull("geometry");

        SpectralProfile profile = GeoJsonProfileSerializer.read(node, 0, new AttributeSchema()).profile();
        assertNull(profile.getAttributes().get("wkt"));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_geometryWithoutType_noWktAttribute() throws IOException {
        ObjectNode node = minimalFeature("p", new double[]{500.0}, new double[]{1.0}, "nm");
        node.putObject("geometry").putArray("coordinates").add(1.0).add(2.0);

        SpectralProfile profile = GeoJsonProfileSerializer.read(node, 0, new AttributeSchema()).profile();
        assertNull(profile.getAttributes().get("wkt"));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_read_customStringAttribute_preserved() throws IOException {
        ObjectNode node = minimalFeature("p", new double[]{500.0}, new double[]{1.0}, "nm");
        ((ObjectNode) node.get("properties")).put("label", "vegetation");

        SpectralProfile profile = GeoJsonProfileSerializer.read(node, 0, new AttributeSchema()).profile();
        assertEquals("vegetation", profile.getAttributes().get("label").asString());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_nameAndProfilesKeysSkipped() throws IOException {
        ObjectNode node = minimalFeature("p", new double[]{500.0}, new double[]{1.0}, "nm");
        SpectralProfile profile = GeoJsonProfileSerializer.read(node, 0, new AttributeSchema()).profile();

        assertFalse(profile.getAttributes().containsKey("name"));
        assertFalse(profile.getAttributes().containsKey("profiles"));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_nullCustomAttribute_skipped() throws IOException {
        ObjectNode node = minimalFeature("p", new double[]{500.0}, new double[]{1.0}, "nm");
        ((ObjectNode) node.get("properties")).putNull("nullField");

        SpectralProfile profile = GeoJsonProfileSerializer.read(node, 0, new AttributeSchema()).profile();
        assertFalse(profile.getAttributes().containsKey("nullField"));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_write_producesFeatureWithCorrectType() {
        ArrayNode arr = mapper.createArrayNode();
        GeoJsonProfileSerializer.write(arr, simpleProfile("P", 1.0, 2.0), axis(), mapper);

        ObjectNode feature = (ObjectNode) arr.get(0);
        assertEquals("Feature", feature.get("type").asText());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_write_profilesObjectContainsXYBbl() {
        ArrayNode arr = mapper.createArrayNode();
        GeoJsonProfileSerializer.write(arr, simpleProfile("P", 1.0, Double.NaN), axis(), mapper);

        ObjectNode profiles = (ObjectNode) arr.get(0).get("properties").get("profiles");

        ArrayNode y = (ArrayNode) profiles.get("y");
        assertEquals(1.0, y.get(0).doubleValue(), 1e-9);
        assertTrue(y.get(1).isNull());

        ArrayNode x = (ArrayNode) profiles.get("x");
        assertEquals(400.0, x.get(0).doubleValue(), 1e-9);
        assertEquals(500.0, x.get(1).doubleValue(), 1e-9);

        assertEquals("nm", profiles.get("xUnit").asText());

        ArrayNode bbl = (ArrayNode) profiles.get("bbl");
        assertEquals(1, bbl.get(0).intValue());
        assertEquals(0, bbl.get(1).intValue());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_write_infiniteValue_writtenAsNull() {
        ArrayNode arr = mapper.createArrayNode();
        GeoJsonProfileSerializer.write(arr, simpleProfile("P", Double.POSITIVE_INFINITY, 1.0), axis(), mapper);

        ArrayNode y = (ArrayNode) arr.get(0).get("properties").get("profiles").get("y");
        assertTrue(y.get(0).isNull());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_write_wktAttribute_appearsAsGeometry() {
        SpectralProfile profile = SpectralProfile.create("P", SpectralSignature.of(new double[]{1.0, 2.0}))
                .withAttribute("wkt", AttributeValue.ofString("POINT(10.0 50.0)"));

        ArrayNode arr = mapper.createArrayNode();
        GeoJsonProfileSerializer.write(arr, profile, axis(), mapper);

        ObjectNode geom = (ObjectNode) arr.get(0).get("geometry");
        assertNotNull(geom);
        assertEquals("Point", geom.get("type").asText());
        assertEquals(10.0, geom.get("coordinates").get(0).doubleValue(), 1e-9);
        assertEquals(50.0, geom.get("coordinates").get(1).doubleValue(), 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_write_wktNotWrittenAsProperty() {
        SpectralProfile profile = SpectralProfile.create("P", SpectralSignature.of(new double[]{1.0, 2.0}))
                .withAttribute("wkt", AttributeValue.ofString("POINT(10.0 50.0)"));

        ArrayNode arr = mapper.createArrayNode();
        GeoJsonProfileSerializer.write(arr, profile, axis(), mapper);

        ObjectNode props = (ObjectNode) arr.get(0).get("properties");
        assertNull(props.get("wkt"));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_write_noWktAndNoSourceRef_geometryIsNull() {
        ArrayNode arr = mapper.createArrayNode();
        GeoJsonProfileSerializer.write(arr, simpleProfile("P", 1.0, 2.0), axis(), mapper);

        assertTrue(arr.get(0).get("geometry").isNull());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_write_embeddedSpectrumAttributeSkipped() {
        var embAxis = new SpectralAxis(new double[]{400.0, 500.0}, "nm");
        var embSig  = SpectralSignature.of(new double[]{0.1, 0.2});
        var es = new AttributeValue.EmbeddedSpectrum(embAxis, embSig);

        SpectralProfile profile = SpectralProfile.create("P", SpectralSignature.of(new double[]{1.0, 2.0}))
                .withAttribute("embedded", AttributeValue.ofEmbeddedSpectrum(es));

        ArrayNode arr = mapper.createArrayNode();
        GeoJsonProfileSerializer.write(arr, profile, axis(), mapper);

        ObjectNode props = (ObjectNode) arr.get(0).get("properties");
        assertNull(props.get("embedded"));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_write_sourceRefProductId_appearsAsProperty() {
        SpectralProfile.SourceRef ref = new SpectralProfile.SourceRef(0, 0, 0, "S2.tif");
        SpectralProfile profile = new SpectralProfile(
                UUID.randomUUID(), "P",
                SpectralSignature.of(new double[]{1.0, 2.0}),
                Map.of(), ref
        );

        ArrayNode arr = mapper.createArrayNode();
        GeoJsonProfileSerializer.write(arr, profile, axis(), mapper);

        ObjectNode props = (ObjectNode) arr.get(0).get("properties");
        assertEquals("S2.tif", props.get("sourceProduct").asText());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_write_sourceRefWithoutProductId_noSourceProductProperty() {
        SpectralProfile.SourceRef ref = new SpectralProfile.SourceRef(0, 0, 0, null);
        SpectralProfile profile = new SpectralProfile(
                UUID.randomUUID(), "P",
                SpectralSignature.of(new double[]{1.0, 2.0}),
                Map.of(), ref
        );

        ArrayNode arr = mapper.createObjectNode().putArray("f");
        GeoJsonProfileSerializer.write(arr, profile, axis(), mapper);

        ObjectNode props = (ObjectNode) arr.get(0).get("properties");
        assertNull(props.get("sourceProduct"));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_write_customAttributesSerialisedIntoProperties() {
        SpectralProfile profile = SpectralProfile.create("P",
                        SpectralSignature.of(new double[]{1.0, 2.0}))
                .withAttribute("label", AttributeValue.ofString("forest"))
                .withAttribute("score", AttributeValue.ofDouble(0.9));

        ArrayNode arr = mapper.createArrayNode();
        GeoJsonProfileSerializer.write(arr, profile, axis(), mapper);

        ObjectNode props = (ObjectNode) arr.get(0).get("properties");
        assertEquals("forest", props.get("label").asText());
        assertEquals(0.9, props.get("score").doubleValue(), 1e-9);
    }



    private ObjectNode featureNode() {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "Feature");
        return node;
    }

    private ObjectNode minimalFeature(String name, double[] wavelengths,
                                      double[] yValues, String xUnit) {
        ObjectNode node = featureNode();
        ObjectNode props = node.putObject("properties");
        props.put("name", name);
        ObjectNode profiles = props.putObject("profiles");

        ArrayNode x = profiles.putArray("x");
        for (double w : wavelengths) {
            x.add(w);
        }

        ArrayNode y = profiles.putArray("y");
        for (double v : yValues) {
            y.add(v);
        }

        profiles.put("xUnit", xUnit);
        return node;
    }

    private SpectralAxis axis() {
        return new SpectralAxis(new double[]{400.0, 500.0}, "nm");
    }

    private SpectralProfile simpleProfile(String name, double... yValues) {
        return SpectralProfile.create(name, SpectralSignature.of(yValues));
    }
}
