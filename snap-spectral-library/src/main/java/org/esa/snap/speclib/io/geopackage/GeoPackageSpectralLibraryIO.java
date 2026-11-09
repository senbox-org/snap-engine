package org.esa.snap.speclib.io.geopackage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.esa.snap.speclib.io.SpectralLibraryIODelegate;
import org.esa.snap.speclib.io.geojson.util.GeoJsonAttributeConverter;
import org.esa.snap.speclib.io.util.IOUtils;
import org.esa.snap.speclib.model.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;


/**
 * Reads EnMAP-Box spectral libraries stored as GeoPackage feature layers.
 * A SNAP library is created for every {@code layer + profile field + axis}
 * combination.  This mirrors EnMAP-Box's vector-layer/profile-field model
 * while retaining SNAP's invariant of one common axis per library.
 */
public class GeoPackageSpectralLibraryIO implements SpectralLibraryIODelegate {


    private static final List<String> EXTENSIONS = List.of("gpkg");
    private static final String PROFILE_FIELD = "profiles";
    private static final String GEOMETRY_FIELD = "geom";
    private static final String NAME_FIELD = "name";
    private static final ObjectMapper MAPPER = new ObjectMapper();


    @Override
    public SpectralLibrary read(Path path) throws IOException {
        List<SpectralLibrary> libraries = readAll(path);
        if (libraries.isEmpty()) {
            throw new IOException("No EnMAP-Box spectral profiles found in GeoPackage: " + path);
        }
        return libraries.getFirst();
    }

    @Override
    public List<SpectralLibrary> readAll(Path path) throws IOException {
        Objects.requireNonNull(path, "path must not be null");
        if (!Files.isRegularFile(path)) {
            throw new IOException("GeoPackage does not exist: " + path);
        }

        loadDriver();
        List<SpectralLibrary> result = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(url(path))) {
            for (String table : featureTables(connection)) {
                result.addAll(readTable(connection, table));
            }
        } catch (SQLException e) {
            throw new IOException("Unable to read GeoPackage " + path + ": " + e.getMessage(), e);
        }

        result.sort(Comparator.comparing(SpectralLibrary::getName));
        return List.copyOf(result);
    }

    @Override
    public void write(SpectralLibrary library, Path path) throws IOException {
        Objects.requireNonNull(library, "library must not be null");
        Objects.requireNonNull(path, "path must not be null");

        loadDriver();
        Files.createDirectories(path.toAbsolutePath().getParent());
        Files.deleteIfExists(path);

        try (Connection connection = DriverManager.getConnection(url(path))) {
            connection.setAutoCommit(false);
            createGeoPackageSchema(connection);
            writeLibrary(connection, library);
            connection.commit();
        } catch (SQLException e) {
            throw new IOException("Unable to write GeoPackage " + path + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean canRead(Path path) {
        return IOUtils.hasExtension(path, EXTENSIONS.getFirst());
    }

    @Override
    public boolean canWrite(Path path) {
        return IOUtils.hasExtension(path, EXTENSIONS.getFirst());
    }

    @Override
    public List<String> getFileExtensions() {
        return EXTENSIONS;
    }

    private static List<String> featureTables(Connection c) throws SQLException {
        List<String> tables = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement("SELECT table_name FROM gpkg_contents WHERE data_type = 'features' ORDER BY table_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        return tables;
    }

    private static List<SpectralLibrary> readTable(Connection c, String table) throws SQLException, IOException {
        List<String> columns = columns(c, table);
        String geometryColumn = geometryColumn(c, table).orElse(null);
        List<String> profileColumns = new ArrayList<>();

        for (String column : columns) {
            if (!column.equals(geometryColumn) && containsProfile(c, table, column)) profileColumns.add(column);
        }

        List<SpectralLibrary> output = new ArrayList<>();
        for (String profileColumn : profileColumns) {
            Map<SpectralAxis, List<SpectralProfile>> groups = new LinkedHashMap<>();
            Map<SpectralAxis, AttributeSchema> schemas = new LinkedHashMap<>();

            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM " + quote(table))) {
                ResultSetMetaData md = rs.getMetaData();

                while (rs.next()) {
                    String profileJson = rs.getString(profileColumn);

                    if (profileJson == null || profileJson.isBlank()) {
                        continue;
                    }

                    ParsedProfile parsed = parseProfile(profileJson, table + "." + profileColumn);
                    Map<String, AttributeValue> attrs = readAttributes(rs, md, profileColumn, geometryColumn);
                    if (geometryColumn != null) {
                        String wkt = geometryAsWkt(rs.getBytes(geometryColumn));
                        if (wkt != null) {
                            attrs.put("wkt", AttributeValue.ofString(wkt));
                        }
                    }

                    AttributeSchema schema = schemas.computeIfAbsent(parsed.axis(), ignored -> new AttributeSchema());
                    schema.inferFromAttributes(attrs);
                    String name = profileName(rs, table, profileColumn);
                    groups.computeIfAbsent(parsed.axis(), ignored -> new ArrayList<>())
                        .add(new SpectralProfile(UUID.randomUUID(), name, SpectralSignature.of(parsed.values()), attrs, null));
                }
            }

            List<SpectralAxis> axes = new ArrayList<>(groups.keySet());
            axes.sort(Comparator.comparing(GeoPackageSpectralLibraryIO::axisLabel));
            boolean multipleAxes = axes.size() > 1;

            for (SpectralAxis axis : axes) {
                String name = table + "." + profileColumn + (multipleAxes ? " [" + axisLabel(axis) + "]" : "");
                output.add(new SpectralLibrary(UUID.randomUUID(), name, axis, null, groups.get(axis), schemas.get(axis)));
            }
        }
        return output;
    }

    private static boolean containsProfile(Connection c, String table, String column) throws SQLException {
        String sql = "SELECT " + quote(column) + " FROM " + quote(table) + " WHERE " + quote(column) + " IS NOT NULL LIMIT 20";

        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String text = rs.getString(1);
                if (text != null && isProfileJson(text)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isProfileJson(String text) {
        try {
            return MAPPER.readTree(text).path("y").isArray();
        }
        catch (Exception ignored) {
            return false;
        }
    }

    private static ParsedProfile parseProfile(String text, String source) throws IOException {
        JsonNode root = MAPPER.readTree(text);
        JsonNode y = root.path("y");
        if (!y.isArray() || y.isEmpty()) {
            throw new IOException("Profile has no non-empty y array: " + source);
        }

        JsonNode x = root.path("x");
        if (x.isArray() && x.size() != y.size()) {
            throw new IOException("Profile x array differs from y length: " + source);
        }

        double[] wavelengths = new double[y.size()];
        double[] values = new double[y.size()];
        JsonNode bbl = root.path("bbl");

        for (int i = 0; i < y.size(); i++) {
            if (x.isArray()) {
                if (!x.get(i).isNumber()) {
                    throw new IOException("Profile x contains a non-number: " + source);
                }
                wavelengths[i] = x.get(i).doubleValue();
            } else {
                // EnMAP profiles may legally contain only y.  Preserve them
                // as an index-based axis because SNAP requires an explicit axis.
                wavelengths[i] = i;
            }
            values[i] = y.get(i).isNumber() && !(bbl.isArray() && i < bbl.size() && bbl.get(i).asInt(1) == 0)
                    ? y.get(i).doubleValue() : Double.NaN;
        }

        return new ParsedProfile(new SpectralAxis(wavelengths, root.path("xUnit").asText("")), values);
    }

    private static Map<String, AttributeValue> readAttributes(ResultSet rs, ResultSetMetaData md, String profileColumn, String geometryColumn) throws SQLException {
        Map<String, AttributeValue> attrs = new LinkedHashMap<>();

        for (int i = 1; i <= md.getColumnCount(); i++) {
            String key = md.getColumnLabel(i);
            if (key.equals(profileColumn) || key.equals(geometryColumn) || "id".equalsIgnoreCase(key) || NAME_FIELD.equalsIgnoreCase(key)) {
                continue;
            }
            Object value = rs.getObject(i);
            AttributeValue attribute = toAttribute(value);

            if (attribute != null) {
                attrs.put(key, attribute);
            }
        }
        return attrs;
    }

    private static AttributeValue toAttribute(Object value) throws SQLException {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer i) {
            return AttributeValue.ofInt(i);
        }
        if (value instanceof Long l) {
            return AttributeValue.ofLong(l);
        }
        if (value instanceof Number n) {
            return AttributeValue.ofDouble(n.doubleValue());
        }
        if (value instanceof Boolean b) {
            return AttributeValue.ofBoolean(b);
        }

        String text = value.toString();
        if ((text.startsWith("[") || text.startsWith("{"))) {
            try {
                return GeoJsonAttributeConverter.fromJsonNode(MAPPER.readTree(text));
            } catch (Exception ignored) {
                // ignore
            }
        }
        return GeoJsonAttributeConverter.fromJsonNode(MAPPER.getNodeFactory().textNode(text));
    }

    private static String profileName(ResultSet rs, String table, String field) throws SQLException {
        try {
            String name = rs.getString(NAME_FIELD);
            if (name != null && !name.isBlank()) {
                return name;
            }
        } catch (SQLException ignored) {
            // EnMAP allows a name expression instead of a dedicated name column.
        }
        return table + "." + field + " #" + rs.getRow();
    }

    private static List<String> columns(Connection c, String table) throws SQLException {
        List<String> result = new ArrayList<>();

        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("PRAGMA table_info(" + quote(table) + ")")) {
            while (rs.next()) {
                result.add(rs.getString("name"));
            }
        }
        return result;
    }

    private static Optional<String> geometryColumn(Connection c, String table) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT column_name FROM gpkg_geometry_columns WHERE table_name = ?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        }
    }

    private static void createGeoPackageSchema(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA application_id=1196437808");
            st.execute("PRAGMA user_version=10300");
            st.execute("CREATE TABLE gpkg_spatial_ref_sys (srs_name TEXT NOT NULL, srs_id INTEGER NOT NULL PRIMARY KEY, organization TEXT NOT NULL, organization_coordsys_id INTEGER NOT NULL, definition TEXT NOT NULL, description TEXT)");
            st.execute("INSERT INTO gpkg_spatial_ref_sys VALUES ('Undefined Cartesian', -1, 'NONE', -1, 'undefined', 'undefined Cartesian coordinate reference system')");
            st.execute("INSERT INTO gpkg_spatial_ref_sys VALUES ('Undefined geographic', 0, 'NONE', 0, 'undefined', 'undefined geographic coordinate reference system')");
            st.execute("INSERT INTO gpkg_spatial_ref_sys VALUES ('WGS 84 geodetic', 4326, 'EPSG', 4326, 'GEOGCS[\"WGS 84\",DATUM[\"World Geodetic System 1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]', 'longitude/latitude coordinates in decimal degrees on the WGS 84 spheroid')");
            st.execute("CREATE TABLE gpkg_contents (table_name TEXT NOT NULL PRIMARY KEY, data_type TEXT NOT NULL, identifier TEXT UNIQUE, description TEXT DEFAULT '', last_change DATETIME NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')), min_x DOUBLE, min_y DOUBLE, max_x DOUBLE, max_y DOUBLE, srs_id INTEGER)");
            st.execute("CREATE TABLE gpkg_geometry_columns (table_name TEXT NOT NULL, column_name TEXT NOT NULL, geometry_type_name TEXT NOT NULL, srs_id INTEGER NOT NULL, z TINYINT NOT NULL, m TINYINT NOT NULL, PRIMARY KEY (table_name, column_name))");
        }
    }

    private static void writeLibrary(Connection c, SpectralLibrary library) throws SQLException, IOException {
        String table = "spectral_library";
        Map<String, AttributeType> fields = attributeFields(library);
        StringBuilder create = new StringBuilder("CREATE TABLE ")
                .append(quote(table))
                .append(" (id INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(quote(GEOMETRY_FIELD))
                .append(" BLOB, ")
                .append(quote(NAME_FIELD))
                .append(" TEXT NOT NULL, ")
                .append(quote(PROFILE_FIELD))
                .append(" TEXT NOT NULL");

        for (Map.Entry<String, AttributeType> e : fields.entrySet()) {
            create.append(", ").append(quote(e.getKey())).append(' ').append(sqlType(e.getValue()));
        }
        create.append(')');

        try (Statement st = c.createStatement()) {
            st.execute(create.toString());

            try (PreparedStatement ps = c.prepareStatement("INSERT INTO gpkg_contents (table_name,data_type,identifier,description,srs_id) VALUES (?,?,?,?,?)")) {
                ps.setString(1, table); ps.setString(2, "features"); ps.setString(3, library.getName()); ps.setString(4, "SNAP spectral library"); ps.setInt(5, 4326); ps.executeUpdate();
            }
            st.execute("INSERT INTO gpkg_geometry_columns VALUES ('" + table + "','" + GEOMETRY_FIELD + "','GEOMETRY',4326,0,0)");
        }

        List<String> allColumns = new ArrayList<>(List.of(GEOMETRY_FIELD, NAME_FIELD, PROFILE_FIELD)); allColumns.addAll(fields.keySet());
        String marks = String.join(",", Collections.nCopies(allColumns.size(), "?"));

        try (PreparedStatement ps = c.prepareStatement("INSERT INTO " + quote(table) + " (" + allColumns.stream().map(GeoPackageSpectralLibraryIO::quote).reduce((a,b) -> a + "," + b).orElseThrow() + ") VALUES (" + marks + ")")) {
            for (SpectralProfile profile : library.getProfiles()) {
                ps.setBytes(1, geometryBytes(profile));
                ps.setString(2, profile.getName());
                ps.setString(3, profileJson(profile, library.getAxis()));

                for (int i = 0; i < fields.size(); i++) {
                    setAttribute(ps, i + 4, profile.getAttributes().get(allColumns.get(i + 3)));
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static Map<String, AttributeType> attributeFields(SpectralLibrary library) {
        Map<String, AttributeType> fields = new TreeMap<>();
        for (SpectralProfile p : library.getProfiles()) for (Map.Entry<String, AttributeValue> e : p.getAttributes().entrySet()) {
            if (!"wkt".equals(e.getKey()) && !Set.of("id", GEOMETRY_FIELD, NAME_FIELD, PROFILE_FIELD).contains(e.getKey())) {
                fields.putIfAbsent(e.getKey(), e.getValue().getType());
            }
        }
        return fields;
    }

    private static String profileJson(SpectralProfile profile, SpectralAxis axis) throws IOException {
        ObjectNode root = MAPPER.createObjectNode(); ArrayNode y = root.putArray("y"); ArrayNode x = root.putArray("x"); ArrayNode bbl = root.putArray("bbl");
        for (double v : profile.getSignature().getValues()) {
            if (Double.isFinite(v)) {
                y.add(v); bbl.add(1);
            } else {
                y.addNull(); bbl.add(0);
            }
        }

        for (double v : axis.getWavelengths()) {
            x.add(v);
        }
        root.put("xUnit", axis.getXUnit());

        return MAPPER.writeValueAsString(root);
    }

    private static byte[] geometryBytes(SpectralProfile profile) {
        AttributeValue value = profile.getAttributes().get("wkt");
        if (value == null || value.getType() != AttributeType.STRING) {
            return null;
        }

        try {
            Geometry geometry = new org.locationtech.jts.io.WKTReader().read(value.asString());
            byte[] wkb = new WKBWriter(2, false).write(geometry);
            return ByteBuffer.allocate(8 + wkb.length).order(ByteOrder.BIG_ENDIAN).put((byte)'G').put((byte)'P').put((byte)0).put((byte)0).putInt(4326).put(wkb).array();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String geometryAsWkt(byte[] bytes) {
        if (bytes == null || bytes.length < 9 || bytes[0] != 'G' || bytes[1] != 'P') {
            return null;
        }

        int flags = bytes[3] & 0xff, envelope = (flags >> 1) & 7;
        int header = 8 + switch (envelope) { case 0 -> 0; case 1 -> 32; case 2, 3 -> 48; case 4 -> 64; default -> 0; };
        if (header >= bytes.length) {
            return null;
        }

        try {
            return new WKTWriter().write(new WKBReader().read(Arrays.copyOfRange(bytes, header, bytes.length)));
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private static void setAttribute(PreparedStatement ps, int index, AttributeValue value) throws SQLException, IOException {
        if (value == null) {
            ps.setNull(index, Types.NULL);
            return;
        }

        switch (value.getType()) {
            case INT -> ps.setInt(index, value.asInt());
            case LONG -> ps.setLong(index, value.asLong());
            case DOUBLE -> ps.setDouble(index, value.asDouble());
            case BOOLEAN -> ps.setBoolean(index, value.asBoolean());
            case STRING -> ps.setString(index, value.asString());
            case INSTANT -> ps.setString(index, value.asInstant().toString());
            default -> {
                ObjectNode n = MAPPER.createObjectNode();
                GeoJsonAttributeConverter.toJsonNode(n, "v", value);
                ps.setString(index, n.get("v").toString());
            }
        }
    }

    private static String sqlType(AttributeType type) {
        return switch (type) {
            case INT, LONG, BOOLEAN -> "INTEGER";
            case DOUBLE -> "REAL";
            default -> "TEXT";
        };
    }

    private static String axisLabel(SpectralAxis axis) {
        double[] x = axis.getWavelengths();
        return x.length + " bands, " + x[0] + "–" + x[x.length - 1] + " " + axis.getXUnit();
    }

    private static String quote(String name) {
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    private static String url(Path path) {
        return "jdbc:sqlite:" + path.toAbsolutePath();
    }

    private static void loadDriver() throws IOException {
        try {
            Class.forName("org.sqlite.JDBC"); }
        catch (ClassNotFoundException e) {
            throw new IOException("SQLite JDBC driver is unavailable", e);
        }
    }

    private record ParsedProfile(SpectralAxis axis, double[] values) { }
}
