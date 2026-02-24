package org.esa.snap.speclib.io.csv.util;

import org.esa.snap.speclib.model.AttributeType;

import java.util.Locale;
import java.util.regex.Pattern;


public class CsvAttributeTypeInference {


    private static final Pattern INT = Pattern.compile("[-+]?\\d+");
    private static final Pattern DOUBLE = Pattern.compile("[-+]?(\\d+\\.\\d*|\\d*\\.\\d+)([eE][-+]?\\d+)?|[-+]?\\d+([eE][-+]?\\d+)");


    public static AttributeType inferType(String raw) {
        if (raw == null) {
            return AttributeType.STRING;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return AttributeType.STRING;
        }

        if (s.startsWith("[") && s.endsWith("]")) {
            String inner = s.substring(1, s.length() - 1).trim();
            if (inner.isEmpty()) {
                return AttributeType.STRING;
            }

            String[] parts = inner.split(",");
            boolean allInt = true;
            boolean allDouble = true;

            int nNonEmpty = 0;
            for (String p : parts) {
                String t = p.trim();
                if (t.isEmpty()) {
                    continue;
                }
                nNonEmpty++;

                if (!INT.matcher(t).matches()) {
                    allInt = false;
                }
                if (!DOUBLE.matcher(t).matches() && !INT.matcher(t).matches()) {
                    allDouble = false;
                }
            }

            if (nNonEmpty == 0) {
                return AttributeType.STRING;
            }

            if (allInt) {
                return AttributeType.INT_ARRAY;
            }
            if (allDouble) {
                return AttributeType.DOUBLE_ARRAY;
            }

            return AttributeType.STRING_LIST;
        }

        if (s.indexOf('=') >= 0) {
            return AttributeType.STRING_MAP;
        }

        String sl = s.toLowerCase(Locale.ROOT);
        if (sl.equals("true") || sl.equals("false") || sl.equals("yes") || sl.equals("no") || sl.equals("0") || sl.equals("1")) {
            return AttributeType.BOOLEAN;
        }

        if (INT.matcher(s).matches()) {
            try {
                long v = Long.parseLong(s);
                if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) {
                    return AttributeType.INT;
                }
                return AttributeType.LONG;
            } catch (NumberFormatException ignore) {
                return AttributeType.STRING;
            }
        }

        if (DOUBLE.matcher(s).matches()) {
            return AttributeType.DOUBLE;
        }

        return AttributeType.STRING;
    }
}
