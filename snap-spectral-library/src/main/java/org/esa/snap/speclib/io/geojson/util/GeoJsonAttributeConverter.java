package org.esa.snap.speclib.io.geojson.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.esa.snap.speclib.model.AttributeValue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Converts between {@link AttributeValue} and Jackson {@link JsonNode}.
 */
public final class GeoJsonAttributeConverter {


    private GeoJsonAttributeConverter() {}


    /**
     * Infers the best-matching {@link AttributeValue} from a JSON node.
     * Returns {@code null} for null/missing nodes.
     */
    public static AttributeValue fromJsonNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isBoolean())             {
            return AttributeValue.ofBoolean(node.booleanValue());
        }
        if (node.isIntegralNumber())      {
            return fromIntegralNode(node);
        }
        if (node.isFloatingPointNumber()) {
            return AttributeValue.ofDouble(node.doubleValue());
        }
        if (node.isTextual())             {
            return fromTextNode(node);
        }
        if (node.isArray())               {
            return fromArrayNode(node);
        }
        if (node.isObject())              {
            return fromObjectNode(node);
        }
        return null;
    }

    /**
     * Writes an {@link AttributeValue} into {@code parent} under {@code key}.
     */
    public static void toJsonNode(ObjectNode parent, String key, AttributeValue av) {
        switch (av.getType()) {
            case STRING -> parent.put(key, av.asString());
            case INT -> parent.put(key, av.asInt());
            case LONG -> parent.put(key, av.asLong());
            case DOUBLE -> parent.put(key, av.asDouble());
            case BOOLEAN -> parent.put(key, av.asBoolean());
            case INSTANT -> parent.put(key, av.asInstant().toString());
            case STRING_LIST -> writeStringList(parent.putArray(key), av.asStringList());
            case DOUBLE_ARRAY -> writeDoubleArray(parent.putArray(key), av.asDoubleArray());
            case INT_ARRAY -> writeIntArray(parent.putArray(key), av.asIntArray());
            case STRING_MAP -> writeStringMap(parent.putObject(key), av.asStringMap());
            case EMBEDDED_SPECTRUM -> { /* not yet supported */ }
        }
    }


    private static AttributeValue fromIntegralNode(JsonNode node) {
        long lv = node.longValue();

        return (lv >= Integer.MIN_VALUE && lv <= Integer.MAX_VALUE)
                ? AttributeValue.ofInt((int) lv)
                : AttributeValue.ofLong(lv);
    }

    private static AttributeValue fromTextNode(JsonNode node) {
        String text = node.textValue();

        try {
            return AttributeValue.ofInstant(Instant.parse(text));
        } catch (Exception ignored) {
            return AttributeValue.ofString(text);
        }
    }

    private static AttributeValue fromArrayNode(JsonNode arrayNode) {
        if (arrayNode.isEmpty()) {
            return AttributeValue.ofStringList(List.of());
        }

        JsonNode first = arrayNode.get(0);
        if (first.isNumber()) {
            return fromNumericArray(arrayNode);
        }

        if (first.isTextual()) {
            return fromStringArray(arrayNode);
        }
        return AttributeValue.ofString(arrayNode.toString());
    }

    private static AttributeValue fromNumericArray(JsonNode arrayNode) {
        boolean anyDouble = false;

        for (JsonNode n : arrayNode) {
            if (n.isFloatingPointNumber()) {
                anyDouble = true;
                break;
            }
        }

        if (anyDouble) {
            double[] arr = new double[arrayNode.size()];
            for (int ii = 0; ii < arr.length; ii++) {
                arr[ii] = arrayNode.get(ii).doubleValue();
            }
            return AttributeValue.ofDoubleArray(arr);
        }

        int[] arr = new int[arrayNode.size()];
        for (int ii = 0; ii < arr.length; ii++) {
            arr[ii] = arrayNode.get(ii).intValue();
        }
        return AttributeValue.ofIntArray(arr);
    }

    private static AttributeValue fromStringArray(JsonNode arrayNode) {
        List<String> list = new ArrayList<>(arrayNode.size());
        for (JsonNode n : arrayNode) {
            list.add(n.textValue());
        }
        return AttributeValue.ofStringList(list);
    }

    private static AttributeValue fromObjectNode(JsonNode node) {
        Map<String, String> map = new LinkedHashMap<>();

        var it = node.fields();
        while (it.hasNext()) {
            var e = it.next();

            if (!e.getValue().isTextual()) {
                return AttributeValue.ofString(node.toString()); // fallback
            }
            map.put(e.getKey(), e.getValue().textValue());
        }
        return AttributeValue.ofStringMap(map);
    }


    private static void writeStringList(ArrayNode arr, List<String> values) {
        values.forEach(arr::add);
    }

    private static void writeDoubleArray(ArrayNode arr, double[] values) {
        for (double v : values) {
            arr.add(v);
        }
    }

    private static void writeIntArray(ArrayNode arr, int[] values) {
        for (int v : values) {
            arr.add(v);
        }
    }

    private static void writeStringMap(ObjectNode obj, Map<String, String> map) {
        map.forEach(obj::put);
    }
}
