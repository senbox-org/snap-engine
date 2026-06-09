package org.esa.snap.remote.products.repository.cdse;

import org.esa.snap.remote.products.repository.RemoteMission;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CdseProductMapperTest {

    @Test
    public void mapsODataProductWithWktFootprintToRepositoryProduct() throws Exception {
        String json = "{"
                + "\"value\":[{"
                + "\"Id\":\"060882f4-0a34-5f14-8e25-6876e4470b0d\","
                + "\"Name\":\"S3A_OL_1_EFR____20240627T090259.SEN3\","
                + "\"ContentLength\":123456789,"
                + "\"ContentDate\":{\"Start\":\"2024-06-27T09:02:59.000000Z\",\"End\":\"2024-06-27T09:04:06.000000Z\"},"
                + "\"Footprint\":\"POLYGON ((10 45, 12 45, 12 46, 10 46, 10 45))\""
                + "}]}";

        List<RepositoryProduct> products = new CdseProductMapper().mapProducts(json, new RemoteMission("Sentinel3", CdseProductsRepositoryProvider.REPOSITORY_NAME));

        assertEquals(1, products.size());
        RepositoryProduct product = products.get(0);
        assertEquals("060882f4-0a34-5f14-8e25-6876e4470b0d", ((CdseRepositoryProduct) product).getId());
        assertEquals("S3A_OL_1_EFR____20240627T090259.SEN3", product.getName());
        assertEquals(123456789L, product.getApproximateSize());
        assertEquals(LocalDateTime.of(2024, 6, 27, 9, 2, 59), product.getAcquisitionDate());
        assertEquals(CdseProductsRepositoryProvider.downloadUrl("060882f4-0a34-5f14-8e25-6876e4470b0d"), product.getURL());
        assertEquals(CdseProductsRepositoryProvider.REPOSITORY_NAME, product.getRemoteMission().getRepositoryName());
        assertEquals("Sentinel3", product.getRemoteMission().getName());
        assertNotNull(product.getPolygon());
        assertTrue(product.getPolygon().toWKT().startsWith("POLYGON"));
    }

    @Test
    public void mapsODataProductWithGeoJsonFootprintToRepositoryProduct() throws Exception {
        String json = "{"
                + "\"value\":[{"
                + "\"Id\":\"product-id\","
                + "\"Name\":\"S3A_OL_1_EFR____20240627T090259.SEN3\","
                + "\"ContentLength\":42,"
                + "\"ContentDate\":{\"Start\":\"2024-06-27T09:02:59.000Z\"},"
                + "\"GeoFootprint\":{\"type\":\"Polygon\",\"coordinates\":[[[10,45],[12,45],[12,46],[10,46],[10,45]]]}"
                + "}]}";

        List<RepositoryProduct> products = new CdseProductMapper().mapProducts(json, new RemoteMission("Sentinel3", CdseProductsRepositoryProvider.REPOSITORY_NAME));

        assertEquals(1, products.size());
        assertNotNull(products.get(0).getPolygon());
        assertTrue(products.get(0).getPolygon().toWKT().contains("10.0 45.0"));
    }
}
