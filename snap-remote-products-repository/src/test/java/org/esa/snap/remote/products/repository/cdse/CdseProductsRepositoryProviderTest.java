package org.esa.snap.remote.products.repository.cdse;

import org.esa.snap.remote.products.repository.RepositoryQueryParameter;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CdseProductsRepositoryProviderTest {

    @Test
    public void exposesSentinel1Sentinel2AndSentinel3Missions() {
        CdseProductsRepositoryProvider provider = new CdseProductsRepositoryProvider();

        assertEquals(
                List.of("Sentinel1", "Sentinel2", "Sentinel3"),
                Arrays.asList(provider.getAvailableMissions())
        );
    }

    @Test
    public void exposesSentinel1SearchParameters() {
        CdseProductsRepositoryProvider provider = new CdseProductsRepositoryProvider();
        List<RepositoryQueryParameter> parameters = provider.getMissionParameters("Sentinel1");

        assertParameterNames(parameters,
                "platform",
                "operationalMode",
                "productType",
                "polarisationChannels",
                RepositoryQueryParameter.START_DATE,
                RepositoryQueryParameter.END_DATE,
                RepositoryQueryParameter.FOOTPRINT,
                "productIdentifier"
        );
    }

    @Test
    public void exposesSentinel2SearchParameters() {
        CdseProductsRepositoryProvider provider = new CdseProductsRepositoryProvider();
        List<RepositoryQueryParameter> parameters = provider.getMissionParameters("Sentinel2");

        assertParameterNames(parameters,
                "platform",
                "productType",
                "processingLevel",
                "cloudCover",
                RepositoryQueryParameter.START_DATE,
                RepositoryQueryParameter.END_DATE,
                RepositoryQueryParameter.FOOTPRINT,
                "productIdentifier"
        );
    }

    private static void assertParameterNames(List<RepositoryQueryParameter> parameters, String... expectedNames) {
        List<String> names = parameters.stream().map(RepositoryQueryParameter::getName).toList();
        for (String expectedName : expectedNames) {
            assertTrue("Missing parameter '" + expectedName + "' in " + names, names.contains(expectedName));
        }
    }
}
