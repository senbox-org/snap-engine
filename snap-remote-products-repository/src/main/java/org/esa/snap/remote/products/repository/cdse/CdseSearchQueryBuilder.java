package org.esa.snap.remote.products.repository.cdse;

import org.esa.snap.remote.products.repository.RepositoryQueryParameter;

import java.awt.geom.Rectangle2D;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

class CdseSearchQueryBuilder {

    private static final String PRODUCTS_URL = "https://catalogue.dataspace.copernicus.eu/odata/v1/Products";
    private static final DateTimeFormatter ODATA_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private CdseSearchQueryBuilder() {
    }

    static String buildProductsUrl(String mission, Map<String, Object> parameters, int limit) {
        int top = limit > 0 ? limit : 20;
        return PRODUCTS_URL
                + "?%24top=" + top
                + "&%24filter=" + encode(buildFilter(mission, parameters))
                + "&%24count=True"
                + "&%24orderby=" + encode("ContentDate/Start desc")
                + "&%24expand=Attributes";
    }

    static String buildFilter(String mission, Map<String, Object> parameters) {
        Map<String, Object> safeParameters = parameters == null ? Collections.emptyMap() : parameters;
        List<String> clauses = new ArrayList<>();
        clauses.add("Collection/Name eq '" + collectionName(mission) + "'");

        String instrument = stringValue(safeParameters.get("instrument"));
        if (instrument != null) {
            clauses.add(stringAttributeEquals("instrumentShortName", instrument));
        }

        String productType = stringValue(safeParameters.get("productType"));
        if (productType != null) {
            clauses.add(stringAttributeEquals("productType", productType));
        }

        String processingLevel = stringValue(safeParameters.get("processingLevel"));
        if (processingLevel != null) {
            clauses.add(stringAttributeEquals("processingLevel", processingLevel));
        }

        String operationalMode = firstStringValue(safeParameters, "operationalMode", "sensorMode");
        if (operationalMode != null) {
            clauses.add(stringAttributeEquals("operationalMode", operationalMode));
        }

        String polarisationChannels = stringValue(safeParameters.get("polarisationChannels"));
        if (polarisationChannels != null) {
            clauses.add(stringAttributeEquals("polarisationChannels", polarisationChannels));
        }

        Double cloudCover = doubleValue(safeParameters.get("cloudCover"));
        if (cloudCover != null) {
            clauses.add(doubleAttributeLessOrEqual("cloudCover", cloudCover));
        }

        String platform = platform(mission, safeParameters);
        if (platform != null) {
            clauses.add("contains(Name,'" + escapeLiteral(platform) + "')");
        }

        String productIdentifier = stringValue(safeParameters.get("productIdentifier"));
        if (productIdentifier != null) {
            clauses.add("contains(Name,'" + escapeLiteral(productIdentifier) + "')");
        }

        Object startDate = safeParameters.get(RepositoryQueryParameter.START_DATE);
        if (startDate != null) {
            clauses.add("ContentDate/Start ge " + formatDate(startDate));
        }

        Object endDate = safeParameters.get(RepositoryQueryParameter.END_DATE);
        if (endDate != null) {
            clauses.add("ContentDate/End le " + formatDate(endDate));
        }

        Object footprint = safeParameters.get(RepositoryQueryParameter.FOOTPRINT);
        if (footprint instanceof Rectangle2D) {
            clauses.add("OData.CSC.Intersects(area=geography'SRID=4326;" + rectangleToWkt((Rectangle2D) footprint) + "')");
        }

        return String.join(" and ", clauses);
    }

    private static String collectionName(String mission) {
        if ("Sentinel1".equals(mission)) {
            return "SENTINEL-1";
        }
        if ("Sentinel2".equals(mission)) {
            return "SENTINEL-2";
        }
        if ("Sentinel3".equals(mission)) {
            return "SENTINEL-3";
        }
        throw new IllegalArgumentException("CDSE catalogue currently supports missions Sentinel1, Sentinel2 and Sentinel3 only.");
    }

    private static String stringAttributeEquals(String name, String value) {
        return "Attributes/OData.CSC.StringAttribute/any(att:att/Name eq '" + name
                + "' and att/OData.CSC.StringAttribute/Value eq '" + escapeLiteral(value) + "')";
    }

    private static String doubleAttributeLessOrEqual(String name, double value) {
        return "Attributes/OData.CSC.DoubleAttribute/any(att:att/Name eq '" + name
                + "' and att/OData.CSC.DoubleAttribute/Value le " + value + ")";
    }

    private static String platform(String mission, Map<String, Object> parameters) {
        String platform = stringValue(parameters.get("platform"));
        if (platform != null) {
            if (platform.length() == 1) {
                return platformPrefix(mission) + platform.toUpperCase();
            }
            return platform;
        }
        String platformSerialIdentifier = stringValue(parameters.get("platformSerialIdentifier"));
        if (platformSerialIdentifier != null && platformSerialIdentifier.length() == 1) {
            return platformPrefix(mission) + platformSerialIdentifier.toUpperCase();
        }
        return null;
    }

    private static String platformPrefix(String mission) {
        if ("Sentinel1".equals(mission)) {
            return "S1";
        }
        if ("Sentinel2".equals(mission)) {
            return "S2";
        }
        if ("Sentinel3".equals(mission)) {
            return "S3";
        }
        return "";
    }

    private static String firstStringValue(Map<String, Object> parameters, String... names) {
        for (String name : names) {
            String value = stringValue(parameters.get(name));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String stringValue = value.toString().trim();
        return stringValue.isEmpty() ? null : stringValue;
    }

    private static Double doubleValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        String text = stringValue(value);
        return text == null ? null : Double.parseDouble(text);
    }

    private static String formatDate(Object value) {
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(ODATA_TIME_FORMATTER);
        }
        if (value instanceof Date) {
            return LocalDateTime.ofInstant(((Date) value).toInstant(), ZoneOffset.UTC).format(ODATA_TIME_FORMATTER);
        }
        if (value instanceof Instant) {
            return LocalDateTime.ofInstant((Instant) value, ZoneOffset.UTC).format(ODATA_TIME_FORMATTER);
        }
        return value.toString();
    }

    private static String rectangleToWkt(Rectangle2D rectangle) {
        double minX = rectangle.getX();
        double minY = rectangle.getY();
        double maxX = rectangle.getX() + rectangle.getWidth();
        double maxY = rectangle.getY() + rectangle.getHeight();
        return "POLYGON(("
                + minX + " " + minY + ","
                + minX + " " + maxY + ","
                + maxX + " " + maxY + ","
                + maxX + " " + minY + ","
                + minX + " " + minY
                + "))";
    }

    private static String escapeLiteral(String value) {
        return value.replace("'", "''");
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
