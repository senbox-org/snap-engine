package org.esa.snap.oldImpl.performance.performancetests;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.oldImpl.performance.util.MyParameters;
import org.esa.snap.oldImpl.performance.util.TestUtils;

import java.io.File;
import java.io.IOException;

public class WriteProductFromMemoryTest extends WriteSingleProductTest {

    public WriteProductFromMemoryTest(String testName, MyParameters params) {
        super(testName, params);
    }

    @Override
    public Product readProduct() throws IOException {
        File sourceProductFile = TestUtils.buildProductPath(getTestDataDir(), getProductName());
        Product sourceProduct = ProductIO.readProduct(sourceProductFile);

        Product inMemoryProduct = new Product(
                sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight()
        );

        ProductUtils.copyProductNodes(sourceProduct, inMemoryProduct);
        inMemoryProduct.setPreferredTileSize(sourceProduct.getPreferredTileSize());

        for (Band sourceBand : sourceProduct.getBands()) {
            if (!sourceBand.isFlagBand()) {
                Band targetBand = ProductUtils.copyBand(sourceBand.getName(), sourceProduct, inMemoryProduct, true);
                targetBand.readRasterDataFully();
            } else {
                Band targetBand = inMemoryProduct.getBand(sourceBand.getName());
                targetBand.readRasterDataFully();
            }
        }

        sourceProduct.dispose();
        return inMemoryProduct;
    }
}
