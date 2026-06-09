package org.esa.snap.remote.products.repository.cdse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.DataFormatType;
import org.esa.snap.remote.products.repository.RemoteMission;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.SensorType;
import org.esa.snap.remote.products.repository.geometry.AbstractGeometry2D;
import org.esa.snap.remote.products.repository.geometry.GeometryUtils;
import org.esa.snap.remote.products.repository.geometry.MultiPolygon2D;
import org.esa.snap.remote.products.repository.geometry.Polygon2D;
import org.locationtech.jts.io.WKTReader;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class CdseProductMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    List<RepositoryProduct> mapProducts(String json, RemoteMission mission) throws IOException {
        return mapSearchResult(json, mission).getProducts();
    }

    CdseSearchResult mapSearchResult(String json, RemoteMission mission) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode value = root.get("value");
        if (value == null || !value.isArray()) {
            return new CdseSearchResult(0, Collections.emptyList());
        }
        List<RepositoryProduct> products = new ArrayList<>();
        for (JsonNode productNode : value) {
            products.add(mapProduct(productNode, mission));
        }
        JsonNode countNode = root.get("@odata.count");
        long totalCount = countNode != null && countNode.canConvertToLong() ? countNode.asLong() : products.size();
        return new CdseSearchResult(totalCount, products);
    }

    private RepositoryProduct mapProduct(JsonNode productNode, RemoteMission mission) throws IOException {
        String id = text(productNode, "Id");
        String name = text(productNode, "Name");
        long size = productNode.path("ContentLength").asLong(0);
        LocalDateTime acquisitionDate = acquisitionDate(productNode);
        AbstractGeometry2D geometry = geometry(productNode);
        CdseRepositoryProduct product = new CdseRepositoryProduct(id, name, CdseProductsRepositoryProvider.downloadUrl(id), mission, geometry, acquisitionDate, size);
        product.setRemoteAttributes(attributes(productNode));
        product.setDataFormatType(DataFormatType.RASTER);
        product.setSensorType(SensorType.OPTICAL);
        return product;
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static LocalDateTime acquisitionDate(JsonNode productNode) {
        JsonNode startDate = productNode.path("ContentDate").get("Start");
        if (startDate == null || startDate.isNull()) {
            return null;
        }
        String value = startDate.asText();
        if (value.endsWith("Z")) {
            return LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
        }
        return LocalDateTime.parse(value);
    }

    private static AbstractGeometry2D geometry(JsonNode productNode) throws IOException {
        String footprint = text(productNode, "Footprint");
        if (footprint != null && !footprint.isBlank()) {
            return wktGeometry(footprint);
        }
        JsonNode geoFootprint = productNode.get("GeoFootprint");
        if (geoFootprint != null && !geoFootprint.isNull()) {
            return geoJsonGeometry(geoFootprint);
        }
        return null;
    }

    private static AbstractGeometry2D wktGeometry(String footprint) throws IOException {
        try {
            String wkt = footprint.trim();
            int sridIndex = wkt.indexOf("SRID=4326;");
            if (sridIndex >= 0) {
                wkt = wkt.substring(sridIndex + "SRID=4326;".length());
            }
            if (wkt.startsWith("geography'") && wkt.endsWith("'")) {
                wkt = wkt.substring("geography'".length(), wkt.length() - 1);
            }
            return GeometryUtils.convertProductGeometry(new WKTReader().read(wkt));
        } catch (Exception e) {
            throw new IOException("Cannot parse CDSE product footprint.", e);
        }
    }

    private static AbstractGeometry2D geoJsonGeometry(JsonNode geoFootprint) throws IOException {
        String type = text(geoFootprint, "type");
        JsonNode coordinates = geoFootprint.get("coordinates");
        if ("Polygon".equals(type)) {
            return polygon(coordinates.get(0));
        }
        if ("MultiPolygon".equals(type)) {
            MultiPolygon2D multiPolygon = new MultiPolygon2D();
            for (int i = 0; i < coordinates.size(); i++) {
                multiPolygon.setPolygon(i, polygon(coordinates.get(i).get(0)));
            }
            return multiPolygon;
        }
        throw new IOException("Unsupported CDSE GeoFootprint type '" + type + "'.");
    }

    private static Polygon2D polygon(JsonNode coordinates) {
        Polygon2D polygon = new Polygon2D();
        for (JsonNode coordinate : coordinates) {
            polygon.append(coordinate.get(0).asDouble(), coordinate.get(1).asDouble());
        }
        return polygon;
    }

    private static List<Attribute> attributes(JsonNode productNode) {
        JsonNode attributesNode = productNode.get("Attributes");
        if (attributesNode == null || !attributesNode.isArray()) {
            return Collections.emptyList();
        }
        List<Attribute> attributes = new ArrayList<>();
        for (JsonNode attributeNode : attributesNode) {
            String name = text(attributeNode, "Name");
            JsonNode valueNode = attributeNode.get("Value");
            if (name != null && valueNode != null && !valueNode.isNull()) {
                attributes.add(new Attribute(name, valueNode.asText()));
            }
        }
        return attributes;
    }
}
