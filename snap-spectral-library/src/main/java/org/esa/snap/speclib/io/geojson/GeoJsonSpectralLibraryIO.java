package org.esa.snap.speclib.io.geojson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.esa.snap.speclib.io.SpectralLibraryIODelegate;
import org.esa.snap.speclib.io.geojson.util.GeoJsonProfileSerializer;
import org.esa.snap.speclib.io.util.IOUtils;
import org.esa.snap.speclib.model.AttributeSchema;
import org.esa.snap.speclib.model.SpectralAxis;
import org.esa.snap.speclib.model.SpectralLibrary;
import org.esa.snap.speclib.model.SpectralProfile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


/**
 * GeoJSON implementation of {@link SpectralLibraryIODelegate}.
 *
 * <p>Reads and writes spectral libraries as GeoJSON FeatureCollections,
 * compatible with the EnMAP-Box spectral library format.
 *
 * <p>Geometry is stored as a {@code "wkt"} attribute on each profile and
 * mapped to/from the GeoJSON {@code geometry} field on read/write.
 * Bad bands are represented as {@code NaN} internally and as {@code null}
 * in the {@code y} array plus {@code 0} in the {@code bbl} array on disk.
 */
public class GeoJsonSpectralLibraryIO implements SpectralLibraryIODelegate {


    private static final List<String> EXTENSIONS    = List.of("geojson");
    private static final String TYPE               = "type";
    private static final String FEATURE_COLLECTION = "FeatureCollection";
    private static final String NAME               = "name";
    private static final String DESCRIPTION        = "description";
    private static final String FEATURES           = "features";


    @Override
    public SpectralLibrary read(Path path) throws IOException {
        Objects.requireNonNull(path, "path must not be null");

        JsonNode root = parseJson(path);
        validateFeatureCollection(root, path);

        JsonNode featuresNode = root.get(FEATURES);
        if (featuresNode == null || !featuresNode.isArray() || featuresNode.isEmpty()) {
            throw new IOException("GeoJSON FeatureCollection has no features: " + path);
        }

        String libraryName = readString(root, NAME, deriveNameFromPath(path));

        SpectralAxis axis = null;
        AttributeSchema schema = new AttributeSchema();
        List<SpectralProfile> profiles = new ArrayList<>(featuresNode.size());

        for (int ii = 0; ii < featuresNode.size(); ii++) {
            GeoJsonProfileSerializer.ParsedFeature parsed = GeoJsonProfileSerializer.read(featuresNode.get(ii), ii, schema);

            axis = validateAxis(axis, parsed.axis(), ii);
            profiles.add(parsed.profile());
        }

        return new SpectralLibrary(UUID.randomUUID(), libraryName, axis, null, profiles, schema);
    }

    @Override
    public void write(SpectralLibrary library, Path path) throws IOException {
        Objects.requireNonNull(library, "library must not be null");
        Objects.requireNonNull(path, "path must not be null");

        Files.createDirectories(path.toAbsolutePath().getParent());

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = buildFeatureCollection(library, mapper);

        try (OutputStream out = Files.newOutputStream(path)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(out, root);
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


    private static JsonNode parseJson(Path path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = Files.newInputStream(path)) {
            return mapper.readTree(in);
        }
    }

    private static void validateFeatureCollection(JsonNode root, Path path) throws IOException {
        if (root == null || !root.isObject()) {
            throw new IOException("Not a valid JSON object: " + path);
        }

        JsonNode typeNode = root.get(TYPE);
        if (typeNode == null || !FEATURE_COLLECTION.equals(typeNode.asText())) {
            throw new IOException("Not a GeoJSON FeatureCollection (type=" + (typeNode != null ? typeNode.asText() : "missing") + "): " + path);
        }
    }

    private static SpectralAxis validateAxis(SpectralAxis current, SpectralAxis candidate, int featureIndex) throws IOException {
        if (current == null) {
            return candidate;
        }
        if (!current.equals(candidate)) {
            throw new IOException("Feature[" + featureIndex + "] has a different spectral axis than feature[0]. " +
                    "All profiles in a single library must share the same axis.");
        }
        return current;
    }

    private static ObjectNode buildFeatureCollection(SpectralLibrary library, ObjectMapper mapper) {
        ObjectNode root = mapper.createObjectNode();
        root.put(TYPE, FEATURE_COLLECTION);
        root.put(NAME, library.getName());
        root.put(DESCRIPTION, "SpectralLibrary exported by SNAP");

        ArrayNode featuresArray = root.putArray(FEATURES);

        for (SpectralProfile profile : library.getProfiles()) {
            GeoJsonProfileSerializer.write(featuresArray, profile, library.getAxis(), mapper);
        }
        return root;
    }

    private static String readString(JsonNode node, String field, String defaultValue) {
        JsonNode n = node.get(field);
        return (n != null && n.isTextual()) ? n.textValue() : defaultValue;
    }

    private static String deriveNameFromPath(Path path) {
        String fname = path.getFileName().toString();
        int dot = fname.lastIndexOf('.');
        return dot > 0 ? fname.substring(0, dot) : fname;
    }
}
