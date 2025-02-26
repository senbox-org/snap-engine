package org.esa.snap.remote.products.repository;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.esa.snap.remote.products.repository.download.RemoteRepositoriesManager;
import org.esa.snap.remote.products.repository.listener.ProductListDownloaderListener;
import org.junit.Assume;
import org.junit.Test;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by jcoravu on 20/1/2020.
 */
public class RemoteRepositoriesManagerTest {

    public RemoteRepositoriesManagerTest() {
    }

    private static void downloadRepositoryProviderProductList(Credentials credentials, RemoteProductsRepositoryProvider repositoryProvider, String[] ignoredMissions) throws Exception {
        List<String> ignoredMissionsList = Arrays.asList(ignoredMissions);
        String[] missions = repositoryProvider.getAvailableMissions();
        assertNotNull(missions);
        assertTrue(missions.length > 0);

        ProductListDownloaderListener downloaderListener = buildEmptyDownloaderListener();
        ThreadStatus threadStatus = buildEmptyThreadStatus();

        Rectangle2D.Double footPrintBounds = new Rectangle2D.Double(3.9594738673210834, 45.65147715288581, 4.00006689474366, 2.684463401953984);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.DAY_OF_MONTH, -7); // one week ago
        Date endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, -7); // two weeks ago
        Date startDate = calendar.getTime();

        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("footprint", footPrintBounds);
        parameterValues.put("startDate", startDate);
        parameterValues.put("endDate", endDate);

        for (String mission : missions) {
            if (ignoredMissionsList.contains(mission)) {
                continue;
            }
            List<RepositoryQueryParameter> missionParameters = repositoryProvider.getMissionParameters(mission);
            assertNotNull(missionParameters);
            assertFalse(missionParameters.isEmpty());

            List<RepositoryProduct> remoteProducts = repositoryProvider.downloadProductList(credentials, mission, 100, parameterValues, downloaderListener, threadStatus);
            assertNotNull(remoteProducts);

            for (RepositoryProduct remoteProduct : remoteProducts) {
                String quickLookImageURL = remoteProduct.getDownloadQuickLookImageURL();
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
        assertTrue(remoteRepositoryProductProviders.length > 0);

        for (RemoteProductsRepositoryProvider remoteRepositoryProductProvider : remoteRepositoryProductProviders) {
            String existingRepositoryName = remoteRepositoryProductProvider.getRepositoryName();
            assertNotNull(existingRepositoryName);

            if (repositoryNameToFind.equals(existingRepositoryName)) {
                return remoteRepositoryProductProvider;
            }
        }
        return null;
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
        downloadRepositoryProviderProductList(credentials, asfRepositoryProvider, new String[]{});
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
        downloadRepositoryProviderProductList(credentials, awsRepositoryProvider, new String[]{});
    }

    @Test
    public void testEOCATRepositoryProvider() throws Exception {
        RemoteProductsRepositoryProvider eocatRepositoryProvider = findRepositoryProviderByName("EO-CAT");
        assertNotNull(eocatRepositoryProvider);

        Credentials credentials = null;
        if (eocatRepositoryProvider.requiresAuthentication()) {
            String userName = System.getProperty("eocat.account.username");
            String password = System.getProperty("eocat.account.password");

            Assume.assumeTrue(!StringUtils.isBlank(userName) && !StringUtils.isBlank(password));

            credentials = new UsernamePasswordCredentials(userName, password);
        }
        downloadRepositoryProviderProductList(credentials, eocatRepositoryProvider, new String[]{});
    }

    @Test
    public void testFedEORepositoryProvider() throws Exception {
        RemoteProductsRepositoryProvider fedeoRepositoryProvider = findRepositoryProviderByName("FedEO");
        assertNotNull(fedeoRepositoryProvider);

        Credentials credentials = null;
        if (fedeoRepositoryProvider.requiresAuthentication()) {
            String userName = System.getProperty("fedeo.account.username");
            String password = System.getProperty("fedeo.account.password");

            Assume.assumeTrue(!StringUtils.isBlank(userName) && !StringUtils.isBlank(password));

            credentials = new UsernamePasswordCredentials(userName, password);
        }
        downloadRepositoryProviderProductList(credentials, fedeoRepositoryProvider, new String[]{"ALOS (no_named_collections_set_1)", "ALOS-1 (JAXA_CATS-I)", "ALOS-1 (ALOS)","AQUA (CEDA-CCI)","AQUA (NASA_CWIC)","Aura (CEDA-CCI)","BelKA (EOP)","CALIPSO (NASA_CWIC)","CryoSat-2 (CEDA-CCI)","CryoSat-2 (CMEMS_MERCATOR)","ERS-1 (CEDA-CCI)","ERS-2 (CEDA-CCI)","ERS-2 (EOP)","Elektro-L-N1 (EOP)","Envisat (CEDA-CCI)","Envisat (CMEMS_MERCATOR)","FORMOSAT-2 (EOP)","GCOM-W1 (CEDA-CCI)","GCOM-W1 (NASA_CWIC)","GMS-4 (NASA_CMR)","GOES-7 (NASA_CWIC)","GOSAT (CEDA-CCI)","GPM (NASA_CMR)","GeoEye-1 (EOP)","IKONOS (EOP)","ISS (EOWEB)","Jason-1 (CEDA-CCI)","KANOPUS_V1 (EOP)","Landsat-5 (CNES_THEIA)","Landsat-5 (NASA_CWIC)","Landsat-7 (CNES_THEIA)","Landsat-7 (NASA_CWIC)","Landsat-8 (CNES_THEIA)","Landsat-8 (IPT)","Landsat-8 (SENTINEL-HUB)","METEOR-3M (EOP)","MFG (EUM_DAT_MFG)","MONITOR-E (EOP)","MSG (EUM_DAT_MSG)","Meteosat-3 (NASA_CWIC)","Meteosat-4 (NASA_CWIC)","Meteosat-5 (NASA_CWIC)","Metop-A (CEDA-CCI)","Metop-A (EUM_DAT_METOP)","Metop-B (CEDA-CCI)","Metop-B (EUM_DAT_METOP)","Metop-C (NASA_CMR)","NOAA (CEDA-CCI)","NOAA-10 (NASA_CMR)","NOAA-10 (NASA_CWIC)","NOAA-11 (NASA_CWIC)","NOAA-12 (CEDA-CCI)","NOAA-12 (NASA_CMR)","NOAA-14 (CEDA-CCI)","NOAA-15 (CEDA-CCI)","NOAA-15 (NASA_CMR)","NOAA-16 (CEDA-CCI)","NOAA-17 (CEDA-CCI)","NOAA-18 (CEDA-CCI)","NOAA-18 (NASA_CMR)","NOAA-19 (NASA_CMR)","NOAA-6 (NASA_CMR)","NOAA-7 (NASA_CMR)","NOAA-8 (NASA_CMR)","NOAA-9 (NASA_CMR)","NOAA-9 (NASA_CWIC)","ODIN (CEDA-CCI)","OrbView-2 (CEDA-CCI)","PLEIADES (CNES_THEIA)","PROBA-V (no_named_collections_set_1)","QUICKBIRD (EOP)","RESURS-DK1 (EOP)","RESURS-P1 (EOP)","RESURS-P2 (EOP)","RapidEye (EOP)","SARAL (CEDA-CCI)","SCISAT-1 (CEDA-CCI)","SPOT (CNES_TAKE5)","SPOT (CNES_THEIA)","SPOT (EOP)","SPOT 1 (CNES_THEIA)","SPOT 2 (CNES_THEIA)","SPOT 4 (CNES_TAKE5)","SPOT 4 (CNES_THEIA)","SPOT 5 (CNES_THEIA)","SPOT 5 (CNES_TAKE5)","SPOT 5 (EOP)","SUOMI-NPP (NASA_CWIC)","Sentinel-1 (ESA_SCIHUB)","Sentinel-1 (CNES_PEPS)","Sentinel-1 (IPT)","Sentinel-1A (ESA_SCIHUB)","Sentinel-1A (CNES_PEPS)","Sentinel-1A (IPT)","Sentinel-1B (ESA_SCIHUB)","Sentinel-1B (IPT)","Sentinel-2 (CNES_PEPS)","Sentinel-2 (ESA_SCIHUB)","Sentinel-2 (IPT)","Sentinel-2 (SENTINEL-HUB)","Sentinel-2 (no_named_collections_set_1)","Sentinel-3 (ESA_SCIHUB)","Sentinel-3 (CMEMS_MERCATOR)","Sentinel-5P (ESA_SCIHUB_PREOPS)","Sentinel-5P (VITO)","TERRA (CEDA-CCI)","TERRA (EOP)","TERRA (EOWEB)","TERRA (NASA_CWIC)","TIROS-N (NASA_CMR)","TOPEX POSEIDON (CEDA-CCI)","TOPEX/POSEIDON (CEDA-CCI)","TerraSAR-X (EOWEB)","WorldView-1 (EOP)","WorldView-2 (EOP)"});
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
        downloadRepositoryProviderProductList(credentials, usgsRepositoryProvider, new String[]{"MODIS-VI1", "MODIS-VI2", "VIIRS", "MODIS-LTSE-1", "MODIS-LTSE-8"});
    }

    private static RepositoryQueryParameter findQueryParameterByName(String name, List<RepositoryQueryParameter> queryParameters) {
        for (RepositoryQueryParameter queryParameter : queryParameters) {
            if (name.equals(queryParameter.getName())) {
                return queryParameter;
            }
        }
        return null;
    }

    private static ThreadStatus buildEmptyThreadStatus() {
        return () -> false;
    }

    private void testGetRemoteProductsRepositoryProvider(RemoteProductsRepositoryProvider remoteProductsRepositoryProvider) {
        String[] missions = remoteProductsRepositoryProvider.getAvailableMissions();
        assertNotNull(missions);
        assertTrue(missions.length > 0);

        for (String mission : missions) {
            List<RepositoryQueryParameter> queryParameters = RemoteRepositoriesManager.getMissionParameters(remoteProductsRepositoryProvider.getRepositoryName(), mission);
            assertNotNull(queryParameters);
            assertTrue(queryParameters.size() >= 3);

            RepositoryQueryParameter parameter = findQueryParameterByName("startDate", queryParameters);
            assertNotNull(parameter);

            parameter = findQueryParameterByName("endDate", queryParameters);
            assertNotNull(parameter);
        }
    }

    @Test
    public void testGetRemoteProductsRepositoryProviders() {
        RemoteRepositoriesManager repositoryManager = RemoteRepositoriesManager.getInstance();
        RemoteProductsRepositoryProvider[] remoteRepositoryProductProviders = repositoryManager.getRemoteProductsRepositoryProviders();
        assertNotNull(remoteRepositoryProductProviders);
        assertTrue(remoteRepositoryProductProviders.length > 0);

        for(RemoteProductsRepositoryProvider remoteProductsRepositoryProvider:remoteRepositoryProductProviders) {
            if (!remoteProductsRepositoryProvider.getRepositoryName().equals("FedEO")) {
                testGetRemoteProductsRepositoryProvider(remoteProductsRepositoryProvider);
            }
        }
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
