package org.esa.snap.speclib.io.geojson.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.esa.snap.speclib.model.AttributeType;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.model.AttributeSchema;
import org.esa.snap.speclib.model.SpectralAxis;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;


/** Converts between GeoJSON Feature nodes and {@link SpectralProfile} objects. */
public final class GeoJsonProfileSerializer {


    private static final String NAME          = "name";
    private static final String PROFILES      = "profiles";
    private static final String Y             = "y";
    private static final String X             = "x";
    private static final String X_UNIT        = "xUnit";
    private static final String BBL           = "bbl";
    private static final String GEOMETRY      = "geometry";
    private static final String TYPE          = "type";
    private static final String FEATURE       = "Feature";
    private static final String SOURCE_PRODUCT = "sourceProduct";
    private static final String WKT_ATTR_KEY  = "wkt";

    private GeoJsonProfileSerializer() {}


    /**
     * Parses a GeoJSON Feature node into a {@link SpectralProfile}.
     *
     * @param featureNode the Feature JSON node
     * @param index       index within the FeatureCollection (for error messages)
     * @param schema      accumulates attribute definitions while reading
     * @return the parsed profile (axis embedded for caller to validate)
     */
    public static ParsedFeature read(JsonNode featureNode, int index, AttributeSchema schema) throws IOException {

        validateFeatureType(featureNode, index);

        JsonNode propsNode = featureNode.get("properties");
        JsonNode profilesNode = propsNode != null ? propsNode.get(PROFILES) : null;

        if (profilesNode == null || !profilesNode.isObject()) {
            throw new IOException("Feature[" + index + "] is missing a 'profiles' object");
        }

        SpectralAxis axis = readAxis(profilesNode, index);
        double[] yValues = readYValues(profilesNode, axis.size(), index);
        String profName = readProfileName(propsNode, index);

        Map<String, AttributeValue> attrs = new LinkedHashMap<>();
        readGeometryAsWkt(featureNode.get(GEOMETRY), attrs, schema);
        readCustomAttributes(propsNode, attrs, schema);

        SpectralProfile profile = new SpectralProfile(UUID.randomUUID(), profName, SpectralSignature.of(yValues), attrs, null);

        return new ParsedFeature(axis, profile);
    }


    /**
     * Serialises a {@link SpectralProfile} into a GeoJSON Feature {@link ObjectNode}.
     *
     * @param featuresArray the array to append the new Feature object to
     * @param profile       the profile to serialize
     * @param axis          the library's spectral axis, supplies x / xUnit
     * @param mapper        Jackson mapper used for node creation
     */
    public static void write(ArrayNode featuresArray, SpectralProfile profile, SpectralAxis axis, ObjectMapper mapper) {
        ObjectNode featureNode = featuresArray.addObject();
        featureNode.put(TYPE, FEATURE);

        ObjectNode propsNode = featureNode.putObject("properties");
        propsNode.put(NAME, profile.getName());

        writeProfilesObject(propsNode, profile.getSignature(), axis);
        writeCustomAttributes(propsNode, profile);

        ObjectNode geometry = buildGeometry(profile, mapper);
        if (geometry != null) {
            featureNode.set(GEOMETRY, geometry);
        }
        else {
            featureNode.putNull(GEOMETRY);
        }
    }


    private static void validateFeatureType(JsonNode featureNode, int index) throws IOException {
        if (featureNode == null || !featureNode.isObject()) {
            throw new IOException("Feature[" + index + "] is not a JSON object");
        }

        JsonNode typeNode = featureNode.get(TYPE);
        if (typeNode == null || !FEATURE.equals(typeNode.asText())) {
            throw new IOException("Feature[" + index + "] has unexpected type: " + (typeNode != null ? typeNode.asText() : "missing"));
        }
    }

    private static SpectralAxis readAxis(JsonNode profilesNode, int index) throws IOException {
        JsonNode xNode = profilesNode.get(X);
        if (xNode == null || !xNode.isArray() || xNode.isEmpty()) {
            throw new IOException("Feature[" + index + "].profiles is missing a non-empty 'x' array");
        }

        double[] wavelengths = new double[xNode.size()];

        for (int ii = 0; ii < xNode.size(); ii++) {
            JsonNode v = xNode.get(ii);
            if (v == null || v.isNull() || !v.isNumber()) {
                throw new IOException(
                        "Feature[" + index + "].profiles.x[" + ii + "] is not a number");
            }
            wavelengths[ii] = v.doubleValue();
        }

        String xUnit = readString(profilesNode, X_UNIT, "");
        return new SpectralAxis(wavelengths, xUnit);
    }

    private static double[] readYValues(JsonNode profilesNode, int expectedSize, int index) throws IOException {
        JsonNode yNode = profilesNode.get(Y);
        if (yNode == null || !yNode.isArray()) {
            throw new IOException("Feature[" + index + "].profiles is missing 'y' array");
        }
        if (yNode.size() != expectedSize) {
            throw new IOException("Feature[" + index + "].profiles.y length (" + yNode.size() + ") does not match x length (" + expectedSize + ")");
        }

        int[] bbl = readBbl(profilesNode, expectedSize);
        double[] values = new double[expectedSize];

        for (int ii = 0; ii < expectedSize; ii++) {
            JsonNode v = yNode.get(ii);
            boolean bad = (v == null || v.isNull()) || (bbl != null && bbl[ii] == 0);
            values[ii] = bad ? Double.NaN : v.doubleValue();
        }
        return values;
    }

    private static int[] readBbl(JsonNode profilesNode, int expectedSize) {
        JsonNode bblNode = profilesNode.get(BBL);
        if (bblNode == null || !bblNode.isArray() || bblNode.size() != expectedSize) {
            return null;
        }

        int[] bbl = new int[expectedSize];

        for (int ii = 0; ii < expectedSize; ii++) {
            bbl[ii] = bblNode.get(ii).asInt(1);
        }
        return bbl;
    }

    private static String readProfileName(JsonNode propsNode, int index) {
        final String defaultName = "Spectrum_" + (index + 1);

        if (propsNode == null) {
            return defaultName;
        }
        return readString(propsNode, NAME, defaultName);
    }

    private static void readGeometryAsWkt(JsonNode geometryNode, Map<String, AttributeValue> attrs, AttributeSchema schema) {
        if (geometryNode == null || geometryNode.isNull()) {
            return;
        }

        String wkt = GeoJsonGeometryConverter.toWkt(geometryNode);
        if (wkt != null) {
            attrs.put(WKT_ATTR_KEY, AttributeValue.ofString(wkt));
            schema.inferFromAttributes(attrs);
        }
    }

    private static void readCustomAttributes(JsonNode propsNode, Map<String, AttributeValue> attrs, AttributeSchema schema) {
        if (propsNode == null) {
            return;
        }

        propsNode.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            if (NAME.equals(key) || PROFILES.equals(key)) {
                return;
            }

            AttributeValue av = GeoJsonAttributeConverter.fromJsonNode(entry.getValue());
            if (av != null) {
                attrs.put(key, av);
                schema.inferFromAttributes(Map.of(key, av));
            }
        });
    }

    private static String readString(JsonNode node, String field, String defaultValue) {
        JsonNode n = node.get(field);
        return (n != null && n.isTextual()) ? n.textValue() : defaultValue;
    }

    private static void writeProfilesObject(ObjectNode propsNode, SpectralSignature signature, SpectralAxis axis) {
        ObjectNode profilesNode = propsNode.putObject(PROFILES);
        writeYValues(profilesNode, signature);
        writeDoubleArray(profilesNode, X, axis.getWavelengths());
        profilesNode.put(X_UNIT, axis.getXUnit());
        writeBbl(profilesNode, signature);
    }

    private static void writeYValues(ObjectNode profilesNode, SpectralSignature signature) {
        ArrayNode yArray = profilesNode.putArray(Y);
        for (double vv : signature.getValues()) {
            if (Double.isNaN(vv) || Double.isInfinite(vv)) {
                yArray.addNull();
            }
            else {
                yArray.add(vv);
            }
        }
    }

    private static void writeDoubleArray(ObjectNode node, String field, double[] values) {
        ArrayNode arr = node.putArray(field);
        for (double vv : values) {
            arr.add(vv);
        }
    }

    private static void writeBbl(ObjectNode profilesNode, SpectralSignature signature) {
        ArrayNode bblArray = profilesNode.putArray(BBL);
        for (double vv : signature.getValues()) {
            bblArray.add((Double.isNaN(vv) || Double.isInfinite(vv)) ? 0 : 1);
        }
    }

    private static void writeCustomAttributes(ObjectNode propsNode, SpectralProfile profile) {
        profile.getSourceRef()
                .flatMap(SpectralProfile.SourceRef::getProductId)
                .ifPresent(pid -> propsNode.put(SOURCE_PRODUCT, pid));

        for (Map.Entry<String, AttributeValue> e : profile.getAttributes().entrySet()) {
            String key = e.getKey();
            AttributeValue av = e.getValue();

            if (WKT_ATTR_KEY.equals(key)) {
                continue;
            }
            if (av.getType() == AttributeType.EMBEDDED_SPECTRUM) {
                continue;
            }
            GeoJsonAttributeConverter.toJsonNode(propsNode, key, av);
        }
    }

    private static ObjectNode buildGeometry(SpectralProfile profile, ObjectMapper mapper) {
        AttributeValue wktAttr = profile.getAttributes().get(WKT_ATTR_KEY);
        if (wktAttr != null && wktAttr.getType() == AttributeType.STRING) {
            return GeoJsonGeometryConverter.toGeoJson(wktAttr.asString(), mapper);
        }
        return null;
    }


    /** Carries the axis and profile parsed from a single Feature. */
    public record ParsedFeature(SpectralAxis axis, SpectralProfile profile) {}
}
