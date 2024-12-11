package org.esa.snap.oldImpl.performance.performancetests;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.oldImpl.performance.util.MyParameters;
import org.esa.snap.oldImpl.performance.util.TestUtils;

import java.io.File;
import java.io.IOException;

public class WriteProductFromReaderTest extends WriteSingleProductTest{

    public WriteProductFromReaderTest(String testName, MyParameters params) {
        super(testName, params);
    }

    @Override
    public Product readProduct() throws IOException {
        File sourceProductFile = TestUtils.buildProductPath(getTestDataDir(), getProductName());
        return ProductIO.readProduct(sourceProductFile);
    }
}
