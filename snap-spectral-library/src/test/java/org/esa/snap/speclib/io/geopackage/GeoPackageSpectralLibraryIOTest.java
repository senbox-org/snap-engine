package org.esa.snap.speclib.io.geopackage;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.speclib.model.*;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;


public class GeoPackageSpectralLibraryIOTest {


    @Test
    @STTM("SNAP-4336")
    public void writesAndReadsEnmapCompatibleFeatureLayer() throws Exception {
        Path path = Files.createTempFile("speclib-", ".gpkg");
        GeoPackageSpectralLibraryIO io = new GeoPackageSpectralLibraryIO();

        SpectralAxis axis = new SpectralAxis(new double[]{400, 500, 600}, "nm");
        SpectralProfile profile = new SpectralProfile(
                UUID.randomUUID(),
                "grass",
                SpectralSignature.of(new double[]{0.1, Double.NaN, 0.3}),
                Map.of("class", AttributeValue.ofString("vegetation"), "wkt", AttributeValue.ofString("POINT (13.4 52.5)")),
                null
        );

        io.write(new SpectralLibrary(UUID.randomUUID(), "field samples", axis, null, List.of(profile), new AttributeSchema()), path);
        List<SpectralLibrary> libraries = io.readAll(path);

        assertEquals(1, libraries.size());
        SpectralLibrary library = libraries.getFirst();

        assertArrayEquals(axis.getWavelengths(), library.getAxis().getWavelengths(), 0.0);
        assertEquals("nm", library.getAxis().getXUnit());
        assertEquals("grass", library.getProfiles().getFirst().getName());
        assertTrue(Double.isNaN(library.getProfiles().getFirst().getSignature().getValues()[1]));
        assertEquals("vegetation", library.getProfiles().getFirst().getAttribute("class").orElseThrow().asString());
        assertEquals("POINT (13.4 52.5)", library.getProfiles().getFirst().getAttribute("wkt").orElseThrow().asString());
    }

    @Test
    @STTM("SNAP-4336")
    public void readUsesFirstLibrary_andWriterPreservesIntAndJsonAttributes() throws Exception {
        Path path = Files.createTempFile("speclib-", ".gpkg");
        GeoPackageSpectralLibraryIO io = new GeoPackageSpectralLibraryIO();

        SpectralProfile profile = new SpectralProfile(
                UUID.randomUUID(),
                "invalid geometry",
                SpectralSignature.of(new double[]{0.1, 0.2}),
                Map.of("count", AttributeValue.ofInt(7), "labels", AttributeValue.ofStringList(List.of("field", "test")), "wkt", AttributeValue.ofString("not valid WKT")),
                null
        );

        io.write(new SpectralLibrary(
                UUID.randomUUID(),
                "test",
                new SpectralAxis(new double[]{1, 2}, "nm"),
                null,
                List.of(profile),
                new AttributeSchema()
            ),
            path
        );

        assertTrue(io.canRead(path));
        assertTrue(io.canWrite(path));
        assertFalse(io.canRead(Path.of("library.geojson")));
        assertFalse(io.canWrite(Path.of("library.geojson")));

        SpectralProfile imported = io.read(path).getProfiles().getFirst();
        assertEquals(7, imported.getAttribute("count").orElseThrow().asInt());
        assertEquals(List.of("field", "test"), imported.getAttribute("labels").orElseThrow().asStringList());
        assertTrue(imported.getAttribute("wkt").isEmpty());
    }

    @Test
    @STTM("SNAP-4336")
    public void supportsProfilesWithoutX_andIgnoresInvalidGeometry() throws Exception {
        Path path = libraryWithOneProfile();
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
             PreparedStatement ps = c.prepareStatement("INSERT INTO spectral_library (geom, name, profiles) VALUES (?, ?, ?)")) {

            ps.setBytes(1, new byte[]{'G', 'P', 0, 0, 0, 0, 16, -26, 0});
            ps.setString(2, "index based");
            ps.setString(3, "{\"y\":[0.3,0.4]}");
            ps.executeUpdate();
        }

        List<SpectralLibrary> libraries = new GeoPackageSpectralLibraryIO().readAll(path);
        SpectralLibrary indexBased = libraries.stream().filter(l -> l.getAxis().getXUnit().isEmpty()).findFirst().orElseThrow();

        assertArrayEquals(new double[]{0, 1}, indexBased.getAxis().getWavelengths(), 0.0);
        assertTrue(indexBased.getProfiles().getFirst().getAttribute("wkt").isEmpty());
    }

    @Test
    @STTM("SNAP-4336")
    public void readRejectsValidGeoPackageWithoutSpectralProfiles() throws Exception {
        Path path = libraryWithOneProfile();

        try (var c = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath()); Statement st = c.createStatement()) {
            st.execute("DELETE FROM spectral_library");
        }

        try {
            new GeoPackageSpectralLibraryIO().read(path);
            fail("expected IOException");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("No spectral profiles found"));
        }
    }

    @Test
    @STTM("SNAP-4336")
    public void usesFallbackNameWhenLayerHasNoNameColumn() throws Exception {
        Path path = libraryWithOneProfile();

        try (var c = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath()); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE nameless (id INTEGER PRIMARY KEY, geom BLOB, profiles TEXT)");
            st.execute("INSERT INTO gpkg_contents (table_name,data_type,identifier,description,srs_id) VALUES ('nameless','features','nameless','',4326)");
            st.execute("INSERT INTO gpkg_geometry_columns VALUES ('nameless','geom','POINT',4326,0,0)");
            st.execute("INSERT INTO nameless (profiles) VALUES ('{\"x\":[1,2],\"xUnit\":\"nm\",\"y\":[0.1,0.2]}')");
        }

        SpectralLibrary library = new GeoPackageSpectralLibraryIO().readAll(path).stream()
                .filter(l -> l.getName().startsWith("nameless.profiles")).findFirst().orElseThrow();
        assertEquals("nameless.profiles #1", library.getProfiles().getFirst().getName());
    }

    @Test
    @STTM("SNAP-4336")
    public void reportsInvalidAndMissingGeoPackages() throws Exception {
        GeoPackageSpectralLibraryIO io = new GeoPackageSpectralLibraryIO();

        try {
            io.readAll(Path.of("does-not-exist.gpkg"));
            fail("expected IOException");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("does not exist"));
        }
        Path invalid = Files.createTempFile("invalid-", ".gpkg");
        Files.writeString(invalid, "not a SQLite database");

        try {
            io.readAll(invalid);
            fail("expected IOException");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Unable to read GeoPackage"));
        }
    }

    @Test
    @STTM("SNAP-4336")
    public void reportsWriteFailureForInvalidSqlIdentifier() throws Exception {
        GeoPackageSpectralLibraryIO io = new GeoPackageSpectralLibraryIO();
        Path target = Files.createTempFile("invalid-write-", ".gpkg");
        Files.writeString(target, "original GeoPackage content");
        SpectralProfile profile = new SpectralProfile(
                UUID.randomUUID(),
                "p",
                SpectralSignature.of(new double[]{1}),
                Map.of("bad\u0000column", AttributeValue.ofString("value")),
                null
        );

        try {
            io.write(new SpectralLibrary(
                    UUID.randomUUID(),
                    "test",
                    new SpectralAxis(new double[]{1}, ""),
                    null,
                    List.of(profile),
                    new AttributeSchema()
                ),
                target
            );

            fail("expected IOException");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Unable to write GeoPackage"));
        }
        assertEquals("original GeoPackage content", Files.readString(target));
    }

    @Test
    @STTM("SNAP-4336")
    public void reportsProfilesWithInconsistentXAndYLengths() throws Exception {
        Path path = libraryWithOneProfile();

        try (var c = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
             PreparedStatement ps = c.prepareStatement("INSERT INTO spectral_library (name, profiles) VALUES (?, ?)")) {
            ps.setString(1, "bad profile");
            ps.setString(2, "{\"x\":[1],\"y\":[0.1,0.2]}");
            ps.executeUpdate();
        }
        try {
            new GeoPackageSpectralLibraryIO().readAll(path);
            fail("expected IOException");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("differs from y length"));
        }
    }

    @Test
    @STTM("SNAP-4336")
    public void identifiesProfileJsonAndConvertsDatabaseValues() {
        assertTrue(GeoPackageSpectralLibraryIO.isProfileJson("{\"y\":[0.1]}"));
        assertFalse(GeoPackageSpectralLibraryIO.isProfileJson("{\"y\":0.1}"));
        assertFalse(GeoPackageSpectralLibraryIO.isProfileJson("not JSON"));

        assertNull(GeoPackageSpectralLibraryIO.toAttribute(null));
        assertEquals(7, GeoPackageSpectralLibraryIO.toAttribute(7).asInt());
        assertEquals(8L, GeoPackageSpectralLibraryIO.toAttribute(8L).asLong());
        assertEquals(0.5, GeoPackageSpectralLibraryIO.toAttribute(0.5f).asDouble(), 0.0);
        assertTrue(GeoPackageSpectralLibraryIO.toAttribute(true).asBoolean());
        assertEquals("text", GeoPackageSpectralLibraryIO.toAttribute("text").asString());
        assertEquals(List.of("field", "test"), GeoPackageSpectralLibraryIO.toAttribute("[\"field\",\"test\"]").asStringList());
        assertEquals("[invalid", GeoPackageSpectralLibraryIO.toAttribute("[invalid").asString());
    }


    @Test
    @STTM("SNAP-4336")
    public void splitsOneProfileFieldIntoLibrariesByAxis() throws Exception {
        Path path = Files.createTempFile("speclib-", ".gpkg");
        GeoPackageSpectralLibraryIO io = new GeoPackageSpectralLibraryIO();
        SpectralAxis axis = new SpectralAxis(new double[]{400, 500}, "nm");
        SpectralProfile profile = SpectralProfile.create("first", SpectralSignature.of(new double[]{0.1, 0.2}));
        io.write(new SpectralLibrary(UUID.randomUUID(), "test", axis, null, List.of(profile), new AttributeSchema()), path);

        try (var c = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
             PreparedStatement ps = c.prepareStatement("INSERT INTO spectral_library (name, profiles) VALUES (?, ?)") ) {
            ps.setString(1, "second");
            ps.setString(2, "{\"y\":[0.3,0.4,0.5],\"x\":[450,550,650],\"xUnit\":\"nm\",\"bbl\":[1,1,1]}");
            ps.executeUpdate();
        }

        List<SpectralLibrary> libraries = io.readAll(path);

        assertEquals(2, libraries.size());
        assertEquals(1, libraries.get(0).getProfiles().size());
        assertEquals(1, libraries.get(1).getProfiles().size());
        assertNotEquals(libraries.get(0).getAxis(), libraries.get(1).getAxis());
    }


    private static Path libraryWithOneProfile() throws Exception {
        Path path = Files.createTempFile("speclib-", ".gpkg");
        SpectralAxis axis = new SpectralAxis(new double[]{400, 500}, "nm");

        new GeoPackageSpectralLibraryIO().write(new SpectralLibrary(
                UUID.randomUUID(),
                "test",
                axis,
                null,
                List.of(SpectralProfile.create("first", SpectralSignature.of(new double[]{0.1, 0.2}))),
                new AttributeSchema()),
                path
        );
        return path;
    }
}
