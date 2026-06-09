package org.esa.snap.remote.products.repository.cdse;

import org.esa.snap.remote.products.repository.RepositoryQueryParameter;
import org.junit.Test;

import java.awt.geom.Rectangle2D;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class CdseSearchQueryBuilderTest {

    @Test
    public void buildsSentinel3ODataFilterFromRepositoryParameters() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("instrument", "OLCI");
        parameters.put("productType", "OL_1_EFR___");
        parameters.put("processingLevel", "1");
        parameters.put("platform", "S3A");
        parameters.put("productIdentifier", "EFR");
        parameters.put(RepositoryQueryParameter.START_DATE, LocalDateTime.of(2024, 6, 27, 9, 0));
        parameters.put(RepositoryQueryParameter.END_DATE, LocalDateTime.of(2024, 6, 27, 10, 0));
        parameters.put(RepositoryQueryParameter.FOOTPRINT, new Rectangle2D.Double(10.0, 45.0, 2.0, 1.0));

        String filter = CdseSearchQueryBuilder.buildFilter("Sentinel3", parameters);

        assertTrue(filter.contains("Collection/Name eq 'SENTINEL-3'"));
        assertTrue(filter.contains("Attributes/OData.CSC.StringAttribute/any(att:att/Name eq 'instrumentShortName' and att/OData.CSC.StringAttribute/Value eq 'OLCI')"));
        assertTrue(filter.contains("contains(Name,'OL_1_EFR___')"));
        assertTrue(filter.contains("Attributes/OData.CSC.StringAttribute/any(att:att/Name eq 'processingLevel' and att/OData.CSC.StringAttribute/Value eq '1')"));
        assertTrue(filter.contains("contains(Name,'S3A')"));
        assertTrue(filter.contains("contains(Name,'EFR')"));
        assertTrue(filter.contains("ContentDate/Start ge 2024-06-27T09:00:00.000Z"));
        assertTrue(filter.contains("ContentDate/End le 2024-06-27T10:00:00.000Z"));
        assertTrue(filter.contains("OData.CSC.Intersects(area=geography'SRID=4326;POLYGON((10.0 45.0,10.0 46.0,12.0 46.0,12.0 45.0,10.0 45.0))')"));
    }

    @Test
    public void buildsEncodedProductsUrlWithTopCountAndExpand() throws Exception {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("productType", "OL_1_EFR___");

        String url = CdseSearchQueryBuilder.buildProductsUrl("Sentinel3", parameters, 7);
        String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.name());

        assertTrue(decodedUrl.startsWith("https://catalogue.dataspace.copernicus.eu/odata/v1/Products?"));
        assertTrue(decodedUrl.contains("$top=7"));
        assertTrue(decodedUrl.contains("$count=True"));
        assertTrue(decodedUrl.contains("$orderby=ContentDate/Start desc"));
        assertTrue(decodedUrl.contains("$expand=Attributes"));
        assertTrue(decodedUrl.contains("Collection/Name eq 'SENTINEL-3'"));
    }

    @Test
    public void escapesODataStringLiterals() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("productIdentifier", "S3A'bad");

        String filter = CdseSearchQueryBuilder.buildFilter("Sentinel3", parameters);

        assertTrue(filter.contains("contains(Name,'S3A''bad')"));
    }
}
