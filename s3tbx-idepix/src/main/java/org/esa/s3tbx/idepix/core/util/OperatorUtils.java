package org.esa.s3tbx.idepix.core.util;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ProductUtils;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class OperatorUtils {

    private OperatorUtils() {
    }

    public static Product createCompatibleProduct(Product sourceProduct, String name, String type) {
        return createCompatibleProduct(sourceProduct, name, type, false);
    }
    /**
     * Creates a new product with the same size.
     * Copies geocoding and the start and stop time.
     */
    public static Product createCompatibleProduct(Product sourceProduct, String name, String type, boolean includeTiepoints) {
        final int sceneWidth = sourceProduct.getSceneRasterWidth();
        final int sceneHeight = sourceProduct.getSceneRasterHeight();

        Product targetProduct = new Product(name, type, sceneWidth, sceneHeight);
        if (includeTiepoints) {
            ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        }
        copyProductBase(sourceProduct, targetProduct);
        return targetProduct;
    }

    /**
     * Copies geocoding and the start and stop time.
     */
    public static void copyProductBase(Product sourceProduct, Product targetProduct) {
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
    }
}
