package org.esa.snap.remote.products.repository;

import org.esa.snap.remote.products.repository.tao.TAORemoteRepositoryManager;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by jcoravu on 20/1/2020.
 */
public class TAORemoteRepositoryManagerTest {

    public TAORemoteRepositoryManagerTest() {
    }

    @Test
    public void testPolygon() {
        TAORemoteRepositoryManager repositoryManager = TAORemoteRepositoryManager.getInstance();
        RemoteProductsRepositoryProvider[] remoteRepositoryProductProviders = repositoryManager.getRemoteProductsRepositoryProviders();

        assertNotNull(remoteRepositoryProductProviders);
        assertEquals(4, remoteRepositoryProductProviders.length);
    }

    @Test
    public void testGetMissionParameters() {
        String dataSourceName = "Scientific Data Hub";
        String mission = "Sentinel1";
        List<RepositoryQueryParameter> queryParameters = TAORemoteRepositoryManager.getMissionParameters(dataSourceName, mission);
        assertNotNull(queryParameters);
        assertEquals(9, queryParameters.size());

        RepositoryQueryParameter parameter = findQueryParameter("footprint", queryParameters);
        assertNotNull(parameter);
        assertEquals(true, parameter.isRequired());

        parameter = findQueryParameter("startDate", queryParameters);
        assertNotNull(parameter);
        assertEquals(true, parameter.isRequired());

        parameter = findQueryParameter("endDate", queryParameters);
        assertNotNull(parameter);
        assertEquals(true, parameter.isRequired());
    }

    private static RepositoryQueryParameter findQueryParameter(String name, List<RepositoryQueryParameter> queryParameters) {
        for (int i=0;i<queryParameters.size(); i++) {
            if (name.equals(queryParameters.get(i).getName())) {
                return queryParameters.get(i);
            }
        }
        return null;
    }
}
