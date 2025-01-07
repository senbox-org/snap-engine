package org.esa.snap.performance.actions;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.performance.util.Result;
import org.esa.snap.performance.util.TestUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadProductToMemoryAction implements Action {

    private final String productName;
    private String testDataDir;
    private Product result;
    private List<Result> allResults;

    public ReadProductToMemoryAction(String productName, String testDataDir) {
        this.productName = productName;
        this.testDataDir = testDataDir;
    }

    @Override
    public void execute() throws IOException {
        this.allResults = new ArrayList<>();
        String fullFilePath = TestUtils.buildProductPathString(this.testDataDir, this.productName);
        Product sourceProduct = ProductIO.readProduct(new File(fullFilePath));

        Product inMemoryProduct = new Product(
                sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight()
        );

        ProductUtils.copyProductNodes(sourceProduct, inMemoryProduct);
        inMemoryProduct.setPreferredTileSize(sourceProduct.getPreferredTileSize());

        for (Band sourceBand : sourceProduct.getBands()) {
            Band targetBand;
            if (sourceBand.isFlagBand()) {
                targetBand = inMemoryProduct.getBand(sourceBand.getName());
            } else {
                targetBand = ProductUtils.copyBand(sourceBand.getName(), sourceProduct, inMemoryProduct, true);
            }

            targetBand.readRasterDataFully();
        }

        Result result1 = new Result("Product", false, inMemoryProduct, "");
        Result result2 = new Result("Format", false, sourceProduct.getProductReader().getReaderPlugIn().getFormatNames()[0], "");
        sourceProduct.dispose();
        System.gc();

        this.result = inMemoryProduct;
        this.allResults.add(result1);
        this.allResults.add(result2);
    }

    @Override
    public void cleanUp() {
        this.result.dispose();
    }

    @Override
    public List<Result> fetchResults() {
        return this.allResults;
    }
}
