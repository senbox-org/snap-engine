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
        assertTrue(filter.contains("Attributes/OData.CSC.StringAttribute/any(att:att/Name eq 'productType' and att/OData.CSC.StringAttribute/Value eq 'OL_1_EFR___')"));
        assertTrue(filter.contains("Attributes/OData.CSC.StringAttribute/any(att:att/Name eq 'processingLevel' and att/OData.CSC.StringAttribute/Value eq '1')"));
        assertTrue(filter.contains("contains(Name,'S3A')"));
        assertTrue(filter.contains("contains(Name,'EFR')"));
        assertTrue(filter.contains("ContentDate/Start ge 2024-06-27T09:00:00.000Z"));
        assertTrue(filter.contains("ContentDate/End le 2024-06-27T10:00:00.000Z"));
        assertTrue(filter.contains("OData.CSC.Intersects(area=geography'SRID=4326;POLYGON((10.0 45.0,10.0 46.0,12.0 46.0,12.0 45.0,10.0 45.0))')"));
    }

    @Test
    public void buildsSentinel1ODataFilterFromRepositoryParameters() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("productType", "IW_GRDH_1S");
        parameters.put("operationalMode", "IW");
        parameters.put("polarisationChannels", "VV VH");
        parameters.put("platform", "S1A");
        parameters.put(RepositoryQueryParameter.START_DATE, LocalDateTime.of(2024, 5, 2, 6, 0));
        parameters.put(RepositoryQueryParameter.END_DATE, LocalDateTime.of(2024, 5, 2, 7, 0));

        String filter = CdseSearchQueryBuilder.buildFilter("Sentinel1", parameters);

        assertTrue(filter.contains("Collection/Name eq 'SENTINEL-1'"));
        assertTrue(filter.contains("Attributes/OData.CSC.StringAttribute/any(att:att/Name eq 'productType' and att/OData.CSC.StringAttribute/Value eq 'IW_GRDH_1S')"));
        assertTrue(filter.contains("Attributes/OData.CSC.StringAttribute/any(att:att/Name eq 'operationalMode' and att/OData.CSC.StringAttribute/Value eq 'IW')"));
        assertTrue(filter.contains("Attributes/OData.CSC.StringAttribute/any(att:att/Name eq 'polarisationChannels' and att/OData.CSC.StringAttribute/Value eq 'VV VH')"));
        assertTrue(filter.contains("contains(Name,'S1A')"));
        assertTrue(filter.contains("ContentDate/Start ge 2024-05-02T06:00:00.000Z"));
        assertTrue(filter.contains("ContentDate/End le 2024-05-02T07:00:00.000Z"));
    }

    @Test
    public void buildsSentinel2ODataFilterFromRepositoryParameters() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("productType", "S2MSI2A");
        parameters.put("processingLevel", "Level-2A");
        parameters.put("cloudCover", 12.5);
        parameters.put("platform", "S2B");
        parameters.put("productIdentifier", "T32UMB");

        String filter = CdseSearchQueryBuilder.buildFilter("Sentinel2", parameters);

        assertTrue(filter.contains("Collection/Name eq 'SENTINEL-2'"));
        assertTrue(filter.contains("Attributes/OData.CSC.StringAttribute/any(att:att/Name eq 'productType' and att/OData.CSC.StringAttribute/Value eq 'S2MSI2A')"));
        assertTrue(filter.contains("Attributes/OData.CSC.StringAttribute/any(att:att/Name eq 'processingLevel' and att/OData.CSC.StringAttribute/Value eq 'Level-2A')"));
        assertTrue(filter.contains("Attributes/OData.CSC.DoubleAttribute/any(att:att/Name eq 'cloudCover' and att/OData.CSC.DoubleAttribute/Value le 12.5)"));
        assertTrue(filter.contains("contains(Name,'S2B')"));
        assertTrue(filter.contains("contains(Name,'T32UMB')"));
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

    @Test
    public void rejectsUnsupportedMissions() {
        try {
            CdseSearchQueryBuilder.buildFilter("Sentinel5P", Map.of());
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Sentinel1, Sentinel2 and Sentinel3"));
            return;
        }
        throw new AssertionError("Expected unsupported mission to be rejected.");
    }
}
