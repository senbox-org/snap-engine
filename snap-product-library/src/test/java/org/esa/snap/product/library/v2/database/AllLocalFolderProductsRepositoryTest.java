package org.esa.snap.product.library.v2.database;

import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.RepositoryQueryParameter;
import org.junit.Test;

import java.awt.geom.Rectangle2D;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by jcoravu on 20/1/2020.
 */
public class AllLocalFolderProductsRepositoryTest {

    public AllLocalFolderProductsRepositoryTest() {
    }

    @Test
    public void testGetParameters() {
        Path databaseParentFolderPath = Paths.get(".");
        H2DatabaseParameters databaseParameters = new H2DatabaseParameters(databaseParentFolderPath);

        AllLocalFolderProductsRepository localFolderProductsRepository = new AllLocalFolderProductsRepository(databaseParameters);
        List<RepositoryQueryParameter> queryParameters = localFolderProductsRepository.getParameters();
        assertNotNull(queryParameters);
        assertEquals(true, queryParameters.size() > 0);

        RepositoryQueryParameter parameter = findQueryParameterByName(AllLocalFolderProductsRepository.FOOT_PRINT_PARAMETER, queryParameters);
        assertNotNull(parameter);
        assertEquals(Rectangle2D.class, parameter.getType());

        parameter = findQueryParameterByName(AllLocalFolderProductsRepository.START_DATE_PARAMETER, queryParameters);
        assertNotNull(parameter);
        assertEquals(Date.class, parameter.getType());

        parameter = findQueryParameterByName(AllLocalFolderProductsRepository.END_DATE_PARAMETER, queryParameters);
        assertNotNull(parameter);
        assertEquals(Date.class, parameter.getType());

        parameter = findQueryParameterByName(AllLocalFolderProductsRepository.SENSOR_TYPE_PARAMETER, queryParameters);
        assertNotNull(parameter);
        assertEquals(String.class, parameter.getType());

        parameter = findQueryParameterByName(AllLocalFolderProductsRepository.ATTRIBUTES_PARAMETER, queryParameters);
        assertNotNull(parameter);
        assertEquals(Attribute.class, parameter.getType());
    }

    private static RepositoryQueryParameter findQueryParameterByName(String name, List<RepositoryQueryParameter> queryParameters) {
        for (int i=0;i<queryParameters.size(); i++) {
            if (name.equals(queryParameters.get(i).getName())) {
                return queryParameters.get(i);
            }
        }
        return null;
    }
}
