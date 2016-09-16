package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.datamodel.Product;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Tonio Fincke
 */
public class SlstrLevel1B1kmProductFactory extends SlstrLevel1ProductFactory {

    public SlstrLevel1B1kmProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        final File directory = getInputFileParentDirectory();
        final String[] fileNames = directory.list((dir, name) -> name.endsWith("in.nc") || name.endsWith("io.nc") || name.endsWith("tx.nc") ||
                                                                 name.endsWith("tn.nc") || name.endsWith("to.nc"));

        if (fileNames != null) {
            return Arrays.asList(fileNames);
        }
        return Collections.emptyList();
    }

    @Override
    protected String getProductName() {
        return super.getProductName() + "_1km";
    }

    @Override
    protected Product findMasterProduct() {
        final List<Product> productList = getOpenProductList();
        Product masterProduct = new Product("dummy", "type", 1, 1);
        for (Product product : productList) {
            if (product.getSceneRasterWidth() > masterProduct.getSceneRasterWidth() &&
                product.getSceneRasterHeight() > masterProduct.getSceneRasterHeight() &&
                !product.getName().contains("flags") &&
                !product.getName().endsWith("an") &&
                !product.getName().endsWith("ao") &&
                !product.getName().endsWith("bn") &&
                !product.getName().endsWith("bo") &&
                !product.getName().endsWith("cn") &&
                !product.getName().endsWith("co")) {
                masterProduct = product;
            }
        }
        return masterProduct;
    }

    @Override
    protected void setSceneTransforms(Product product) {
    }

    @Override
    protected void setBandGeoCodings(Product product) {
    }
}
