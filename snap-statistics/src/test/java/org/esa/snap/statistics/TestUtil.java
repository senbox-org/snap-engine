package org.esa.snap.statistics;

import com.bc.ceres.core.runtime.internal.UrlHelper;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.junit.Assert;

import java.io.IOException;
import java.net.URL;

public class TestUtil {

    public static Product getTestProduct() throws IOException {
        URL resource = TestUtil.class.getResource("testProduct1.dim");
        Assert.assertNotNull(resource);
        return ProductIO.readProduct(UrlHelper.urlToFile(resource));
    }

}
