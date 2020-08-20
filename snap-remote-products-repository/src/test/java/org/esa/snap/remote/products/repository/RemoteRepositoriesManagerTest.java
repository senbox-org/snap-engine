package org.esa.snap.remote.products.repository;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.esa.snap.remote.products.repository.download.RemoteRepositoriesManager;
import org.esa.snap.remote.products.repository.listener.ProductListDownloaderListener;
import org.junit.Assume;
import org.junit.Test;

import java.awt.geom.Rectangle2D;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by jcoravu on 20/1/2020.
 */
public class RemoteRepositoriesManagerTest {

    public RemoteRepositoriesManagerTest() {
    }

    @Test
    public void testGetRemoteProductsRepositoryProviders() {
        RemoteRepositoriesManager repositoryManager = RemoteRepositoriesManager.getInstance();
        RemoteProductsRepositoryProvider[] remoteRepositoryProductProviders = repositoryManager.getRemoteProductsRepositoryProviders();
        assertNotNull(remoteRepositoryProductProviders);
        assertEquals(true, remoteRepositoryProductProviders.length > 0);

        for (int i=0; i<remoteRepositoryProductProviders.length; i++) {
            String[] missions = remoteRepositoryProductProviders[i].getAvailableMissions();
            assertNotNull(missions);
            assertEquals(true, missions.length > 0);

            for (int k=0; k<missions.length; k++) {
                List<RepositoryQueryParameter> queryParameters = RemoteRepositoriesManager.getMissionParameters(remoteRepositoryProductProviders[i].getRepositoryName(), missions[k]);
                assertNotNull(queryParameters);
                assertEquals(true, queryParameters.size() >= 3);

                RepositoryQueryParameter parameter = findQueryParameterByName("footprint", queryParameters);
                assertNotNull(parameter);
                assertEquals(true, parameter.isRequired());

                parameter = findQueryParameterByName("startDate", queryParameters);
                assertNotNull(parameter);
                assertEquals(true, parameter.isRequired());

                parameter = findQueryParameterByName("endDate", queryParameters);
                assertNotNull(parameter);
                assertEquals(true, parameter.isRequired());
            }
        }
    }

    @Test
    public void testAlaskaSatelliteFacilityRepositoryProvider() throws Exception {
        RemoteProductsRepositoryProvider asfRepositoryProvider = findRepositoryProviderByName("Alaska Satellite Facility");
        assertNotNull(asfRepositoryProvider);

        Credentials credentials = null;
        if (asfRepositoryProvider.requiresAuthentication()) {
            String userName = System.getProperty("asf.account.username");
            String password = System.getProperty("asf.account.password");

            Assume.assumeTrue(!StringUtils.isBlank(userName) && !StringUtils.isBlank(password));

            credentials = new UsernamePasswordCredentials(userName, password);
        }
        downloadRepositoryProviderProductList(credentials, asfRepositoryProvider);
    }

    @Test
    public void testAmazonWebServicesRepositoryProvider() throws Exception {
        RemoteProductsRepositoryProvider awsRepositoryProvider = findRepositoryProviderByName("Amazon Web Services");
        assertNotNull(awsRepositoryProvider);

        Credentials credentials = null;
        if (awsRepositoryProvider.requiresAuthentication()) {
            String userName = System.getProperty("aws.account.username");
            String password = System.getProperty("aws.account.password");

            Assume.assumeTrue(!StringUtils.isBlank(userName) && !StringUtils.isBlank(password));

            credentials = new UsernamePasswordCredentials(userName, password);
        }
        downloadRepositoryProviderProductList(credentials, awsRepositoryProvider);
    }

    @Test
    public void testUSGSRepositoryProvider() throws Exception {
        RemoteProductsRepositoryProvider usgsRepositoryProvider = findRepositoryProviderByName("USGS");
        assertNotNull(usgsRepositoryProvider);

        Credentials credentials = null;
        if (usgsRepositoryProvider.requiresAuthentication()) {
            String userName = System.getProperty("usgs.account.username");
            String password = System.getProperty("usgs.account.password");

            Assume.assumeTrue(!StringUtils.isBlank(userName) && !StringUtils.isBlank(password));

            credentials = new UsernamePasswordCredentials(userName, password);
        }
        downloadRepositoryProviderProductList(credentials, usgsRepositoryProvider);
    }

    @Test
    public void testScientificDataHubRepositoryProvider() throws Exception {
        RemoteProductsRepositoryProvider scientificDataHubRepositoryProvider = findRepositoryProviderByName("Scientific Data Hub");
        assertNotNull(scientificDataHubRepositoryProvider);

        Credentials credentials = null;
        if (scientificDataHubRepositoryProvider.requiresAuthentication()) {
            String userName = System.getProperty("sentinels.account.username");
            String password = System.getProperty("sentinels.account.password");

            Assume.assumeTrue(!StringUtils.isBlank(userName) && !StringUtils.isBlank(password));

            credentials = new UsernamePasswordCredentials(userName, password);
        }

        downloadRepositoryProviderProductList(credentials, scientificDataHubRepositoryProvider);
    }

    private static void downloadRepositoryProviderProductList(Credentials credentials, RemoteProductsRepositoryProvider repositoryProvider) throws Exception {
        String[] missions = repositoryProvider.getAvailableMissions();
        assertNotNull(missions);
        assertEquals(true, missions.length > 0);

        ProductListDownloaderListener downloaderListener = buildEmptyDownloaderListener();
        ThreadStatus threadStatus = buildEmptyThreadStatus();

        Rectangle2D.Double footPrintBounds = new Rectangle2D.Double(3.9594738673210834, 45.65147715288581, 4.00006689474366, 2.684463401953984);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.DAY_OF_MONTH, -7); // one week ago
        Date endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, -7); // two weeks ago
        Date startDate = calendar.getTime();

        Map<String, Object> parameterValues = new HashedMap();
        parameterValues.put("footprint", footPrintBounds);
        parameterValues.put("startDate", startDate);
        parameterValues.put("endDate", endDate);

        for (int i=0; i<missions.length; i++) {
            List<RepositoryQueryParameter> missionParameters = repositoryProvider.getMissionParameters(missions[i]);
            assertNotNull(missionParameters);
            assertEquals(true, missionParameters.size() > 0);

            List<RepositoryProduct> remoteProducts = repositoryProvider.downloadProductList(credentials, missions[i], parameterValues, downloaderListener, threadStatus);
            assertNotNull(remoteProducts);
            //assertEquals(true, remoteProducts.size() > 0);

            for (int k = 0; k < remoteProducts.size(); k++) {
                String quickLookImageURL = remoteProducts.get(k).getDownloadQuickLookImageURL();
                if (!StringUtils.isBlank(quickLookImageURL)) {
                    repositoryProvider.downloadProductQuickLookImage(credentials, quickLookImageURL, threadStatus);
                    break;
                }
            }
        }
    }

    private static RemoteProductsRepositoryProvider findRepositoryProviderByName(String repositoryNameToFind) {
        RemoteRepositoriesManager repositoryManager = RemoteRepositoriesManager.getInstance();
        RemoteProductsRepositoryProvider[] remoteRepositoryProductProviders = repositoryManager.getRemoteProductsRepositoryProviders();

        assertNotNull(remoteRepositoryProductProviders);
        assertEquals(true, remoteRepositoryProductProviders.length > 0);

        for (int i=0;i<remoteRepositoryProductProviders.length; i++) {
            String existingRepositoryName = remoteRepositoryProductProviders[i].getRepositoryName();
            assertNotNull(existingRepositoryName);

            if (repositoryNameToFind.equals(existingRepositoryName)) {
                return remoteRepositoryProductProviders[i];
            }
        }
        return null;
    }

    private static RepositoryQueryParameter findQueryParameterByName(String name, List<RepositoryQueryParameter> queryParameters) {
        for (int i=0;i<queryParameters.size(); i++) {
            if (name.equals(queryParameters.get(i).getName())) {
                return queryParameters.get(i);
            }
        }
        return null;
    }

    private static ThreadStatus buildEmptyThreadStatus() {
        return new ThreadStatus() {
            @Override
            public boolean isFinished() {
                return false;
            }
        };
    }

    private static ProductListDownloaderListener buildEmptyDownloaderListener() {
        return new ProductListDownloaderListener() {
            @Override
            public void notifyProductCount(long totalProductCount) {
            }

            @Override
            public void notifyPageProducts(int pageNumber, List<RepositoryProduct> pageResults, long totalProductCount, int retrievedProductCount) {
            }
        };
    }
}
