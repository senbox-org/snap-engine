package org.esa.snap.performance.actions;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.performance.util.Result;
import org.esa.snap.performance.util.TestUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReadProductFullyAction implements Action {

    private final String productName;
    private String testDataDir;
    private Product result;
    private List<Result> allResults;

    public ReadProductFullyAction(String productName, String testDataDir) {
        this.productName = productName;
        this.testDataDir = testDataDir;
    }

    @Override
    public void execute() throws Throwable {
        this.allResults = new ArrayList<>();
        String fullFilePath = TestUtils.buildProductPathString(this.testDataDir, this.productName);
        Product product = ProductIO.readProduct(new File(fullFilePath));
        for (Band band : product.getBands()) {
            band.readRasterDataFully();
        }
        this.result = product;

        Result result1 = new Result("ProductPath", false, fullFilePath, "");
        Result result2 = new Result("Format", false, product.getProductReader().getReaderPlugIn().getFormatNames()[0], "");
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
