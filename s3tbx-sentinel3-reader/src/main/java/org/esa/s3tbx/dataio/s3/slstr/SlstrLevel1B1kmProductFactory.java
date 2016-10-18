package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.datamodel.Product;

import java.util.List;

/**
 * @author Tonio Fincke
 */
public class SlstrLevel1B1kmProductFactory extends SlstrLevel1FixedResolutionProductFactory {

    public SlstrLevel1B1kmProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected String getProductName() {
        return super.getProductName() + "_1km";
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        List <String> fileNames = super.getFileNames(manifest);
        fileNames.removeIf(name -> name.endsWith("an.nc") || name.endsWith("ao.nc") ||
                                   name.endsWith("bn.nc") || name.endsWith("bo.nc") ||
                                   name.endsWith("cn.nc") || name.endsWith("co.nc"));
        return fileNames;
    }

    @Override
    protected Product findMasterProduct() {
        final List<Product> productList = getOpenProductList();
        Product masterProduct = new Product("dummy", "type", 1, 1);
        for (Product product : productList) {
            if (product.getSceneRasterWidth() > masterProduct.getSceneRasterWidth() &&
                product.getSceneRasterHeight() > masterProduct.getSceneRasterHeight() &&
                !product.getName().contains("flags") &&
                !product.getName().endsWith("tn") &&
                !product.getName().endsWith("tx") &&
                !product.getName().endsWith("to")) {
                masterProduct = product;
            }
        }
        return masterProduct;
    }

}
