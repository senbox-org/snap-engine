package eu.esa.snap.core.renderer.classfraction;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Scalar JSON payload for the class-fraction layer only. It deliberately avoids a generic renderer
 * persistence model.
 */
public final class ClassFractionRendererConfigJson {


    private static final Pattern HEADER = Pattern.compile("\\{\\\"displayName\\\":\\\"([A-Za-z0-9+/=]+)\\\",\\\"displayScale\\\":([^,]+),\\\"validity\\\":\\\"([A-Z_]+)\\\",\\\"bands\\\":\\[(.*)]}");
    private static final Pattern BAND = Pattern.compile("\\{\\\"product\\\":\\\"([A-Za-z0-9+/=]+)\\\",\\\"band\\\":\\\"([A-Za-z0-9+/=]+)\\\",\\\"name\\\":\\\"([A-Za-z0-9+/=]+)\\\",\\\"argb\\\":(-?\\d+),\\\"visible\\\":(true|false)}");


    private ClassFractionRendererConfigJson() {
    }


    public static String encode(ClassFractionRendererConfig config) {
        ClassBandGroupConfig group = config.getClassBandGroup();
        StringBuilder text = new StringBuilder("{\"displayName\":\"")
                .append(encodeText(config.getDisplayName())).append("\"")
                .append(",\"displayScale\":").append(group.getDisplayScale())
                .append(",\"validity\":\"").append(group.getValidityPolicy()).append("\",\"bands\":[");

        for (int i = 0; i < group.getBands().size(); i++) {
            if (i > 0) {
                text.append(',');
            }

            ClassBandConfig band = group.getBands().get(i);
            text.append("{\"product\":\"").append(encodeText(band.getProductName()))
                .append("\",\"band\":\"").append(encodeText(band.getBandName()))
                .append("\",\"name\":\"").append(encodeText(band.getDisplayName()))
                .append("\",\"argb\":").append(band.getArgb())
                .append(",\"visible\":").append(band.isVisible()).append('}');
        }

        return text.append("]}").toString();
    }

    public static ClassFractionRendererConfig decode(String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Class renderer configuration payload is missing");
        }

        Matcher header = HEADER.matcher(payload);
        if (!header.matches()) {
            throw new IllegalArgumentException("Invalid class renderer configuration payload");
        }
        return decode(header.group(1), header.group(4), BAND.matcher(header.group(4)),
                      Double.parseDouble(header.group(2)), header.group(3));
    }

    private static ClassFractionRendererConfig decode(String displayName, String serializedBands, Matcher bandMatcher,
                                                       double displayScale, String validity) {
        List<ClassBandConfig> bands = new ArrayList<>();

        int end = 0;
        while (bandMatcher.find()) {
            if (bandMatcher.start() != end && !(bandMatcher.start() == end + 1 && serializedBands.charAt(end) == ',')) {
                throw new IllegalArgumentException("Invalid class renderer band payload");
            }
            bands.add(new ClassBandConfig(decodeText(bandMatcher.group(1)), decodeText(bandMatcher.group(2)),
                            decodeText(bandMatcher.group(3)), Integer.parseInt(bandMatcher.group(4)),
                            Boolean.parseBoolean(bandMatcher.group(5))));
            end = bandMatcher.end();
        }

        if (end != serializedBands.length()) {
            throw new IllegalArgumentException("Invalid class renderer band payload");
        }
        ClassBandGroupConfig group = new ClassBandGroupConfig(bands, displayScale,
                ClassBandGroupConfig.ValidityPolicy.valueOf(validity));

        return new ClassFractionRendererConfig(decodeText(displayName), group);
    }

    private static String encodeText(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeText(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
