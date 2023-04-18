package org.esa.snap.engine_utilities.gpf;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestOperatorUtils {

    @Test
    public void testCreateProductName() {
        String productName = "originalName";
        String suffix = "_suffix";

        String newName = OperatorUtils.createProductName(productName, suffix);
        assertEquals(productName+suffix, newName);

        String newName2 = OperatorUtils.createProductName(productName, suffix);
        assertEquals(newName, newName2);
    }

    @Test
    public void testGetLatTPGs() {
        final Product product = TestUtils.createProduct("type", 10, 10);

        TiePointGrid latTPG = OperatorUtils.getLatitude(product);
        assertNotNull(latTPG);
        assertEquals(OperatorUtils.TPG_LATITUDE, latTPG.getName());
    }

    @Test
    public void testGetLonTPGs() {
        final Product product = TestUtils.createProduct("type", 10, 10);

        TiePointGrid lonTPG = OperatorUtils.getLongitude(product);
        assertNotNull(lonTPG);
        assertEquals(OperatorUtils.TPG_LONGITUDE, lonTPG.getName());
    }
}
