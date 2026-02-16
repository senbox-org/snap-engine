package org.esa.snap.speclib.io.csv.util;

import org.esa.snap.speclib.model.AttributeType;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.util.SpectralLibraryAttributeValueParser;

import java.util.Locale;
import java.util.Objects;


public class CsvAttributeCodec {


    public static AttributeValue tryParse(AttributeType type, String raw) {
        Objects.requireNonNull(type, "type must not be null");
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }

        if (type == AttributeType.STRING_LIST && s.startsWith("[") && s.endsWith("]")) {
            s = s.substring(1, s.length() - 1).trim();
        }

        try {
            return SpectralLibraryAttributeValueParser.parseForType(type, s);
        } catch (IllegalArgumentException ex) {
            try {
                if (type == AttributeType.BOOLEAN) {
                    String sl = s.toLowerCase(Locale.ROOT);
                    if (sl.equals("true") || sl.equals("1") || sl.equals("yes")) {
                        return AttributeValue.ofBoolean(true);
                    }
                    if (sl.equals("false") || sl.equals("0") || sl.equals("no")) {
                        return AttributeValue.ofBoolean(false);
                    }
                }
            } catch (Throwable ignore) {
            }
            return null;
        }
    }

    public static String format(AttributeValue v) {
        if (v == null) {
            return "";
        }

        return switch (v.getType()) {
            case DOUBLE_ARRAY, INT_ARRAY -> "[" + SpectralLibraryAttributeValueParser.toDisplayValue(v) + "]";
            case STRING_LIST -> "[" + SpectralLibraryAttributeValueParser.toDisplayValue(v) + "]";
            default -> SpectralLibraryAttributeValueParser.toDisplayValue(v);
        };
    }
}
