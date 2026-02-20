package org.esa.snap.speclib.util;

import org.esa.snap.speclib.model.AttributeType;
import org.esa.snap.speclib.model.AttributeValue;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


public class SpectralLibraryAttributeValueParser {


    public static AttributeValue parseForType(AttributeType type, Object aValue) {
        if (type == null) {
            type = AttributeType.STRING;
        }

        if (type == AttributeType.BOOLEAN && aValue instanceof Boolean b) {
            return AttributeValue.ofBoolean(b);
        }
        if (type == AttributeType.INT && aValue instanceof Integer i) {
            return AttributeValue.ofInt(i);
        }
        if (type == AttributeType.LONG && aValue instanceof Long l) {
            return AttributeValue.ofLong(l);
        }
        if (type == AttributeType.DOUBLE && aValue instanceof Double d) {
            return AttributeValue.ofDouble(d);
        }
        if (type == AttributeType.INSTANT && aValue instanceof Instant instant) {
            return AttributeValue.ofInstant(instant);
        }

        String s = aValue == null ? "" : String.valueOf(aValue).trim();

        if ((type == AttributeType.DOUBLE_ARRAY || type == AttributeType.INT_ARRAY) && s.startsWith("[") && s.endsWith("]")) {
            s = s.substring(1, s.length() - 1).trim();
        }

        if (s.isEmpty()) {
            throw new IllegalArgumentException("value must not be empty");
        }

        return switch (type) {
            case STRING -> AttributeValue.ofString(s);
            case INT -> {
                try { yield AttributeValue.ofInt(Integer.parseInt(s)); }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException("expected int");
                }
            }
            case LONG -> {
                try { yield AttributeValue.ofLong(Long.parseLong(s)); }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException("expected long");
                }
            }
            case DOUBLE -> {
                try { yield AttributeValue.ofDouble(Double.parseDouble(s)); }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException("expected double");
                }
            }
            case BOOLEAN -> {
                String sl = s.toLowerCase();
                if (sl.equals("true") || sl.equals("1") || sl.equals("yes")) {
                    yield AttributeValue.ofBoolean(true);
                }
                if (sl.equals("false") || sl.equals("0") || sl.equals("no")) {
                    yield AttributeValue.ofBoolean(false);
                }
                throw new IllegalArgumentException("expected true/false");
            }
            case INSTANT -> AttributeValue.ofInstant(Instant.parse(s));
            case STRING_LIST -> AttributeValue.ofStringList(parseStringList(s));
            case DOUBLE_ARRAY -> AttributeValue.ofDoubleArray(parseDoubleArray(s));
            case INT_ARRAY -> AttributeValue.ofIntArray(parseIntArray(s));
            case STRING_MAP -> AttributeValue.ofStringMap(parseStringMap(s));
            case EMBEDDED_SPECTRUM -> throw new IllegalArgumentException("EMBEDDED_SPECTRUM is not editable in table");
        };
    }

    private static List<String> parseStringList(String s) {
        String[] parts = s.split(",");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException("string list is empty");
        }
        return out;
    }

    private static double[] parseDoubleArray(String s) {
        String[] parts = s.split(",");
        double[] out = new double[parts.length];
        int n = 0;
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) {
                continue;
            }
            try {
                out[n++] = Double.parseDouble(t);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid double: " + t);
            }
        }
        if (n == 0) {
            throw new IllegalArgumentException("double array is empty");
        }
        return n == out.length ? out : Arrays.copyOf(out, n);
    }

    private static int[] parseIntArray(String s) {
        String[] parts = s.split(",");
        int[] out = new int[parts.length];
        int n = 0;
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) {
                continue;
            }
            try {
                out[n++] = Integer.parseInt(t);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid int: " + t);
            }
        }
        if (n == 0) {
            throw new IllegalArgumentException("int array is empty");
        }
        return n == out.length ? out : Arrays.copyOf(out, n);
    }

    private static Map<String, String> parseStringMap(String s) {
        String[] pairs = s.split(",");
        Map<String, String> out = new LinkedHashMap<>();
        for (String pair : pairs) {
            String t = pair.trim();
            if (t.isEmpty()) {
                continue;
            }
            int eq = t.indexOf('=');
            if (eq <= 0) {
                throw new IllegalArgumentException("invalid map entry (expected key=value): " + t);
            }
            String k = t.substring(0, eq).trim();
            String v = t.substring(eq + 1).trim();
            if (k.isEmpty()) {
                throw new IllegalArgumentException("empty map key");
            }
            out.put(k, v);
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException("map is empty");
        }
        return out;
    }


    public static Object cellValue(AttributeValue v) {
        if (v == null) {
            return null;
        }
        return switch (v.getType()) {
            case BOOLEAN -> v.asBoolean();
            case INT -> v.asInt();
            case LONG -> v.asLong();
            case DOUBLE -> v.asDouble();
            default -> toDisplayValue(v);
        };
    }

    public static String toDisplayValue(AttributeValue v) {
        if (v == null) {
            return "";
        }

        return switch (v.getType()) {
            case STRING -> v.asString();
            case INT -> String.valueOf(v.asInt());
            case LONG -> String.valueOf(v.asLong());
            case DOUBLE -> String.valueOf(v.asDouble());
            case BOOLEAN -> String.valueOf(v.asBoolean());
            case INSTANT -> String.valueOf(v.asInstant());
            case STRING_LIST -> String.join(",", v.asStringList());
            case DOUBLE_ARRAY -> joinDoubleArray(v.asDoubleArray());
            case INT_ARRAY -> joinIntArray(v.asIntArray());
            case STRING_MAP -> v.asStringMap().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(","));
            case EMBEDDED_SPECTRUM -> {
                var emb = v.asEmbeddedSpectrum();
                yield "<spectrum n=" + emb.getAxis().size() + ">";
            }
        };
    }

    private static String joinDoubleArray(double[] a) {
        if (a == null || a.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(a[i]);
        }
        return sb.toString();
    }

    private static String joinIntArray(int[] a) {
        if (a == null || a.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(a[i]);
        }
        return sb.toString();
    }
}
