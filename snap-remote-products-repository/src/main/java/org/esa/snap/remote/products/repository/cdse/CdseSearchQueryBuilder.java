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
        if (!"Sentinel3".equals(mission)) {
            throw new IllegalArgumentException("CDSE catalogue currently supports mission Sentinel3 only.");
        }
        List<String> clauses = new ArrayList<>();
        clauses.add("Collection/Name eq 'SENTINEL-3'");

        String instrument = stringValue(parameters.get("instrument"));
        if (instrument != null) {
            clauses.add(stringAttributeEquals("instrumentShortName", instrument));
        }

        String productType = stringValue(parameters.get("productType"));
        if (productType != null) {
            clauses.add("contains(Name,'" + escapeLiteral(productType) + "')");
        }

        String processingLevel = stringValue(parameters.get("processingLevel"));
        if (processingLevel != null) {
            clauses.add(stringAttributeEquals("processingLevel", processingLevel));
        }

        String platform = platform(parameters);
        if (platform != null) {
            clauses.add("contains(Name,'" + escapeLiteral(platform) + "')");
        }

        String productIdentifier = stringValue(parameters.get("productIdentifier"));
        if (productIdentifier != null) {
            clauses.add("contains(Name,'" + escapeLiteral(productIdentifier) + "')");
        }

        Object startDate = parameters.get(RepositoryQueryParameter.START_DATE);
        if (startDate != null) {
            clauses.add("ContentDate/Start ge " + formatDate(startDate));
        }

        Object endDate = parameters.get(RepositoryQueryParameter.END_DATE);
        if (endDate != null) {
            clauses.add("ContentDate/End le " + formatDate(endDate));
        }

        Object footprint = parameters.get(RepositoryQueryParameter.FOOTPRINT);
        if (footprint instanceof Rectangle2D) {
            clauses.add("OData.CSC.Intersects(area=geography'SRID=4326;" + rectangleToWkt((Rectangle2D) footprint) + "')");
        }

        return String.join(" and ", clauses);
    }

    private static String stringAttributeEquals(String name, String value) {
        return "Attributes/OData.CSC.StringAttribute/any(att:att/Name eq '" + name
                + "' and att/OData.CSC.StringAttribute/Value eq '" + escapeLiteral(value) + "')";
    }

    private static String platform(Map<String, Object> parameters) {
        String platform = stringValue(parameters.get("platform"));
        if (platform != null) {
            return platform;
        }
        String platformSerialIdentifier = stringValue(parameters.get("platformSerialIdentifier"));
        if (platformSerialIdentifier != null && platformSerialIdentifier.length() == 1) {
            return "S3" + platformSerialIdentifier.toUpperCase();
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
