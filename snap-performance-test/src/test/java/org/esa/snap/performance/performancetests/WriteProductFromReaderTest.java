package org.esa.snap.performance.performancetests;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.performance.util.Parameters;
import org.esa.snap.performance.util.TestUtils;

import java.io.File;
import java.io.IOException;

public class WriteProductFromReaderTest extends WriteSingleProductTest{

    public WriteProductFromReaderTest(String testName, Parameters params) {
        super(testName, params);
    }

    @Override
    public Product readProduct() throws IOException {
        File sourceProductFile = TestUtils.buildProductPath(getTestDataDir(), getProductName());
        return ProductIO.readProduct(sourceProductFile);
    }
}
