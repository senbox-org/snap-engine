package org.esa.snap.speclib.io.geojson;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.speclib.model.AttributeType;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.model.SpectralAxis;
import org.esa.snap.speclib.model.SpectralLibrary;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;


public class GeoJsonSpectralLibraryIOTest {


    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final GeoJsonSpectralLibraryIO io = new GeoJsonSpectralLibraryIO();


    @Test(expected = NullPointerException.class)
    @STTM("SNAP-4177")
    public void test_read_nullPath_throws() throws IOException {
        io.read(null);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_notJsonObject_throws() throws IOException {
        thrown.expect(IOException.class);
        thrown.expectMessage("valid JSON");
        io.read(write("not_object.geojson", "[1, 2, 3]"));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_wrongType_throws() throws IOException {
        thrown.expect(IOException.class);
        thrown.expectMessage("FeatureCollection");
        io.read(write("wrong.geojson", """
                { "type": "Feature", "properties": {}, "geometry": null }
                """));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_missingTypeField_throws() throws IOException {
        thrown.expect(IOException.class);
        thrown.expectMessage("FeatureCollection");
        io.read(write("no_type.geojson", """
                { "name": "lib", "features": [] }
                """));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_emptyFeaturesArray_throws() throws IOException {
        thrown.expect(IOException.class);
        thrown.expectMessage("no features");
        io.read(write("empty.geojson", """
                { "type": "FeatureCollection", "name": "lib", "features": [] }
                """));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_missingFeaturesKey_throws() throws IOException {
        thrown.expect(IOException.class);
        thrown.expectMessage("no features");
        io.read(write("no_features.geojson", """
                { "type": "FeatureCollection", "name": "lib" }
                """));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_featuresIsNotArray_throws() throws IOException {
        thrown.expect(IOException.class);
        thrown.expectMessage("no features");
        io.read(write("features_not_array.geojson", """
                { "type": "FeatureCollection", "name": "lib", "features": "bad" }
                """));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_axisMismatchAcrossFeatures_throws() throws IOException {
        thrown.expect(IOException.class);
        thrown.expectMessage("different spectral axis");
        io.read(write("mismatch.geojson", """
                {
                  "type": "FeatureCollection", "name": "lib",
                  "features": [
                    {
                      "type": "Feature", "geometry": null,
                      "properties": { "name": "p1",
                        "profiles": { "y": [1.0], "x": [400.0], "xUnit": "nm" } }
                    },
                    {
                      "type": "Feature", "geometry": null,
                      "properties": { "name": "p2",
                        "profiles": { "y": [1.0, 2.0], "x": [400.0, 500.0], "xUnit": "nm" } }
                    }
                  ]
                }
                """));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_enmapBoxExample_libraryName() throws IOException {
        SpectralLibrary lib = io.read(write("enmap.geojson", ENMAP_EXAMPLE));
        assertEquals("libraryWithBadBands", lib.getName());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_enmapBoxExample_axis() throws IOException {
        SpectralAxis axis = io.read(write("enmap.geojson", ENMAP_EXAMPLE)).getAxis();
        assertEquals("nanometers", axis.getXUnit());
        assertArrayEquals(new double[]{490.0, 560.0, 665.0, 830.0}, axis.getWavelengths(), 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_enmapBoxExample_profileCount() throws IOException {
        assertEquals(2, io.read(write("enmap.geojson", ENMAP_EXAMPLE)).size());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_enmapBoxExample_firstProfileValues() throws IOException {
        double[] v = io.read(write("enmap.geojson", ENMAP_EXAMPLE)).getProfiles().getFirst().getSignature().getValues();

        assertEquals(95.0,  v[0], 1e-9);
        assertEquals(118.0, v[1], 1e-9);

        assertTrue("index 2: y=109 but bbl=0 → NaN", Double.isNaN(v[2]));
        assertTrue("index 3: y=null → NaN",           Double.isNaN(v[3]));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_enmapBoxExample_firstProfileNullY() throws IOException {
        double[] v = io.read(write("enmap.geojson", ENMAP_EXAMPLE)).getProfiles().getFirst().getSignature().getValues();
        assertTrue("index 3 y=null → NaN", Double.isNaN(v[3]));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_enmapBoxExample_firstProfileWkt() throws IOException {
        AttributeValue wkt = io.read(write("enmap.geojson", ENMAP_EXAMPLE)).getProfiles().getFirst().getAttributes().get("wkt");

        assertNotNull(wkt);
        assertEquals("POINT(13.012757 52.3944126)", wkt.asString());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_enmapBoxExample_secondProfileValues() throws IOException {
        double[] v = io.read(write("enmap.geojson", ENMAP_EXAMPLE)).getProfiles().get(1).getSignature().getValues();

        assertEquals(103.0, v[0], 1e-9);
        assertTrue(Double.isNaN(v[1]));

        assertTrue(Double.isNaN(v[2]));
        assertTrue(Double.isNaN(v[3]));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_noNameField_fallsBackToFilename() throws IOException {
        Path file = write("my_library.geojson", """
                {
                  "type": "FeatureCollection",
                  "features": [{
                    "type": "Feature", "geometry": null,
                    "properties": { "name": "p",
                      "profiles": { "y": [1.0], "x": [500.0], "xUnit": "nm" } }
                  }]
                }
                """);
        assertEquals("my_library", io.read(file).getName());
    }

    @Test(expected = NullPointerException.class)
    @STTM("SNAP-4177")
    public void test_write_nullLibrary_throws() throws IOException {
        io.write(null, tmp.getRoot().toPath().resolve("out.geojson"));
    }

    @Test(expected = NullPointerException.class)
    @STTM("SNAP-4177")
    public void test_write_nullPath_throws() throws IOException {
        io.write(SpectralLibrary.create("lib", axis(), null), null);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_write_topLevelFields() throws IOException {
        Path out = outPath("top.geojson");
        io.write(SpectralLibrary.create("MyLib", axis(), null), out);
        String json = Files.readString(out);

        assertTrue(json.contains("\"FeatureCollection\""));
        assertTrue(json.contains("\"MyLib\""));
        assertTrue(json.contains("\"description\""));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_write_emptyLibrary_emptyFeaturesArray() throws IOException {
        Path out = outPath("empty.geojson");
        io.write(SpectralLibrary.create("Empty", axis(), null), out);

        String json = Files.readString(out);
        assertTrue(json.contains("\"features\" : [ ]"));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_write_createsParentDirectories() throws IOException {
        Path deep = tmp.getRoot().toPath().resolve("a/b/c/out.geojson");
        io.write(SpectralLibrary.create("Lib", axis(), null), deep);

        assertTrue(Files.exists(deep));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_write_nanBecomesNullAndBblZero() throws IOException {
        Path out = outPath("bbl.geojson");
        SpectralLibrary lib = SpectralLibrary.create("Lib", axis(), null)
                .withProfileAdded(SpectralProfile.create("p", SpectralSignature.of(new double[]{1.0, Double.NaN})));

        io.write(lib, out);
        String json = Files.readString(out);

        assertTrue(json.contains("\"bbl\" : [ 1, 0 ]"));
        assertTrue(json.contains("null"));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_write_infiniteBecomesNullAndBblZero() throws IOException {
        Path out = outPath("inf.geojson");
        SpectralLibrary lib = SpectralLibrary.create("Lib", axis(), null)
                .withProfileAdded(SpectralProfile.create("p", SpectralSignature.of(new double[]{Double.POSITIVE_INFINITY, 2.0})));
        io.write(lib, out);

        String json = Files.readString(out);
        assertTrue(json.contains("\"bbl\" : [ 0, 1 ]"));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_libraryName() throws IOException {
        assertRoundtrip("NameLib", new double[]{400.0, 500.0}, "nm",
                profiles -> assertEquals("NameLib", profiles.getName()));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_axis() throws IOException {
        Path out = outPath("rt_axis.geojson");
        SpectralAxis originalAxis = new SpectralAxis(new double[]{450.0, 550.0, 650.0}, "nanometers");
        SpectralLibrary lib = SpectralLibrary.create("L", originalAxis, null)
                .withProfileAdded(SpectralProfile.create("p", SpectralSignature.of(new double[]{1.0, 2.0, 3.0})));

        io.write(lib, out);
        SpectralAxis readAxis = io.read(out).getAxis();

        assertEquals("nanometers", readAxis.getXUnit());
        assertArrayEquals(new double[]{450.0, 550.0, 650.0}, readAxis.getWavelengths(), 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_profileName() throws IOException {
        Path out = outPath("rt_name.geojson");
        io.write(libraryWith("ProfileAlpha", 1.0, 2.0), out);

        assertEquals("ProfileAlpha", io.read(out).getProfiles().getFirst().getName());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_goodValues() throws IOException {
        Path out = outPath("rt_values.geojson");
        io.write(libraryWith("p", 0.1, 0.2), out);

        double[] v = io.read(out).getProfiles().getFirst().getSignature().getValues();
        assertArrayEquals(new double[]{0.1, 0.2}, v, 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_nanPreserved() throws IOException {
        Path out = outPath("rt_nan.geojson");
        io.write(libraryWith("p", Double.NaN, 2.0), out);
        double[] v = io.read(out).getProfiles().getFirst().getSignature().getValues();

        assertTrue(Double.isNaN(v[0]));
        assertEquals(2.0, v[1], 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_wktGeometry() throws IOException {
        Path out = outPath("rt_wkt.geojson");
        SpectralProfile profile = SpectralProfile.create("p", SpectralSignature.of(new double[]{1.0, 2.0}))
                .withAttribute("wkt", AttributeValue.ofString("POINT(9.0 48.0)"));
        SpectralLibrary lib = SpectralLibrary.create("L", axis(), null).withProfileAdded(profile);

        io.write(lib, out);
        AttributeValue wkt = io.read(out).getProfiles().getFirst().getAttributes().get("wkt");

        assertNotNull(wkt);
        assertEquals("POINT(9.0 48.0)", wkt.asString());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_stringAttribute() throws IOException {
        Path out = outPath("rt_str.geojson");
        SpectralProfile profile = SpectralProfile.create("p", SpectralSignature.of(new double[]{1.0, 2.0}))
                .withAttribute("cls", AttributeValue.ofString("water"));
        io.write(SpectralLibrary.create("L", axis(), null).withProfileAdded(profile), out);

        assertEquals("water", io.read(out).getProfiles().getFirst().getAttributes().get("cls").asString());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_intAttribute() throws IOException {
        Path out = outPath("rt_int.geojson");
        SpectralProfile profile = SpectralProfile.create("p", SpectralSignature.of(new double[]{1.0, 2.0}))
                .withAttribute("count", AttributeValue.ofInt(42));
        io.write(SpectralLibrary.create("L", axis(), null).withProfileAdded(profile), out);

        assertEquals(42, io.read(out).getProfiles().getFirst().getAttributes().get("count").asInt());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_doubleAttribute() throws IOException {
        Path out = outPath("rt_dbl.geojson");
        SpectralProfile profile = SpectralProfile.create("p", SpectralSignature.of(new double[]{1.0, 2.0}))
                .withAttribute("score", AttributeValue.ofDouble(0.95));
        io.write(SpectralLibrary.create("L", axis(), null).withProfileAdded(profile), out);

        assertEquals(0.95, io.read(out).getProfiles().getFirst().getAttributes().get("score").asDouble(), 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_booleanAttribute() throws IOException {
        Path out = outPath("rt_bool.geojson");
        SpectralProfile profile = SpectralProfile.create("p", SpectralSignature.of(new double[]{1.0, 2.0}))
                .withAttribute("valid", AttributeValue.ofBoolean(true));
        io.write(SpectralLibrary.create("L", axis(), null).withProfileAdded(profile), out);

        assertTrue(io.read(out).getProfiles().getFirst().getAttributes().get("valid").asBoolean());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_stringListAttribute() throws IOException {
        Path out = outPath("rt_list.geojson");
        SpectralProfile profile = SpectralProfile.create("p", SpectralSignature.of(new double[]{1.0, 2.0}))
                .withAttribute("tags", AttributeValue.ofStringList(List.of("a", "b")));
        io.write(SpectralLibrary.create("L", axis(), null).withProfileAdded(profile), out);

        assertEquals(List.of("a", "b"), io.read(out).getProfiles().getFirst().getAttributes().get("tags").asStringList());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_sourceProductInProperties() throws IOException {
        Path out = outPath("rt_src.geojson");
        SpectralProfile.SourceRef ref = new SpectralProfile.SourceRef(10, 20, 0, "S2.tif");
        SpectralProfile profile = new SpectralProfile(UUID.randomUUID(), "p", SpectralSignature.of(new double[]{1.0, 2.0}), Map.of(), ref);
        io.write(SpectralLibrary.create("L", axis(), null).withProfileAdded(profile), out);

        String json = Files.readString(out);
        assertTrue(json.contains("\"sourceProduct\" : \"S2.tif\""));

        AttributeValue src = io.read(out).getProfiles().getFirst().getAttributes().get("sourceProduct");
        assertNotNull(src);
        assertEquals(AttributeType.STRING, src.getType());
        assertEquals("S2.tif", src.asString());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_multipleProfiles() throws IOException {
        Path out = outPath("rt_multi.geojson");
        SpectralLibrary lib = SpectralLibrary.create("Multi", axis(), null)
                .withProfileAdded(SpectralProfile.create("A", SpectralSignature.of(new double[]{1.0, 2.0})))
                .withProfileAdded(SpectralProfile.create("B", SpectralSignature.of(new double[]{3.0, 4.0})));
        io.write(lib, out);
        SpectralLibrary read = io.read(out);

        assertEquals(2, read.size());
        assertEquals("A", read.getProfiles().get(0).getName());
        assertEquals("B", read.getProfiles().get(1).getName());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_embeddedSpectrumSkipped() throws IOException {
        Path out = outPath("rt_es.geojson");
        var embAxis = new SpectralAxis(new double[]{400.0, 500.0}, "nm");
        var embSig  = SpectralSignature.of(new double[]{0.1, 0.2});

        SpectralProfile profile = SpectralProfile.create("p", SpectralSignature.of(new double[]{1.0, 2.0}))
                .withAttribute("ref", AttributeValue.ofEmbeddedSpectrum(new AttributeValue.EmbeddedSpectrum(embAxis, embSig)));
        io.write(SpectralLibrary.create("L", axis(), null).withProfileAdded(profile), out);

        String json = Files.readString(out);
        assertFalse(json.contains("\"ref\""));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_canRead() {
        assertTrue(io.canRead(Paths.get("library.geojson")));
        assertTrue(io.canRead(Paths.get("library.GEOJSON")));
        assertFalse(io.canRead(Paths.get("library.hdr")));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_canWrite() {
        assertTrue(io.canWrite(Paths.get("library.geojson")));
        assertTrue(io.canWrite(Paths.get("library.GEOJSON")));
        assertFalse(io.canWrite(Paths.get("library.sli")));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_getFileExtensions_containsGeojson() {
        List<String> exts = io.getFileExtensions();

        assertEquals(1, exts.size());
        assertTrue(exts.contains("geojson"));
    }



    private SpectralAxis axis() {
        return new SpectralAxis(new double[]{400.0, 500.0}, "nm");
    }

    private SpectralLibrary libraryWith(String profileName, double... yValues) {
        return SpectralLibrary.create("Lib", axis(), null)
                .withProfileAdded(SpectralProfile.create(profileName, SpectralSignature.of(yValues)));
    }

    private Path write(String filename, String content) throws IOException {
        Path path = tmp.newFile(filename).toPath();
        Files.writeString(path, content);
        return path;
    }

    private Path outPath(String filename) {
        return tmp.getRoot().toPath().resolve(filename);
    }

    @FunctionalInterface
    private interface LibraryAssertion {
        void check(SpectralLibrary lib);
    }

    private void assertRoundtrip(String name, double[] wavelengths, String xUnit,
                                 LibraryAssertion assertion) throws IOException {
        Path out = outPath(name + ".geojson");
        SpectralAxis ax = new SpectralAxis(wavelengths, xUnit);
        SpectralLibrary lib = SpectralLibrary.create(name, ax, null)
                .withProfileAdded(SpectralProfile.create("p", SpectralSignature.of(new double[wavelengths.length])));

        io.write(lib, out);
        assertion.check(io.read(out));
    }


    // EnMAP-Box example fixture
    private static final String ENMAP_EXAMPLE = """
            {
                "type": "FeatureCollection",
                "name": "libraryWithBadBands",
                "description": "SpectralLibrary #1",
                "features": [
                    {
                        "type": "Feature",
                        "properties": {
                            "name": "1",
                            "profiles": {
                                "y":    [ 95.0, 118.0, 109.0, null ],
                                "x":    [ 490.0, 560.0, 665.0, 830.0 ],
                                "xUnit": "nanometers",
                                "bbl":  [ 1, 1, 0, 1 ]
                            }
                        },
                        "geometry": { "type": "Point", "coordinates": [ 13.012757, 52.3944126 ] }
                    },
                    {
                        "type": "Feature",
                        "properties": {
                            "name": "2",
                            "profiles": {
                                "y":    [ 103.0, null, 93.0, null ],
                                "x":    [ 490.0, 560.0, 665.0, 830.0 ],
                                "xUnit": "nanometers",
                                "bbl":  [ 1, 1, 0, 1 ]
                            }
                        },
                        "geometry": { "type": "Point", "coordinates": [ 13.0168252, 52.3910204 ] }
                    }
                ]
            }
            """;
}
