package org.esa.s3tbx.meris.brr.operator;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.util.ProductUtils;

// todo (mp/20160311) BrrBasisOp, MerisBasisOp and BasisOp are all more or less the same --> harmonize
/**
 * Meris BRR basis abstract operator
 *
 * @author olafd
 */
public abstract class BrrBasisOp extends Operator {

    @Parameter(description = "Write all tie points to the target product",
               label = "Write all tie points to the target product",
               defaultValue = "true")
    private boolean copyAllTiePoints = true;

    /**
     * creates a new product with the same size
     *
     * @param sourceProduct - the source product
     * @param name - the product name
     * @param type - the product type
     * @return targetProduct
     */
    public Product createCompatibleProduct(Product sourceProduct, String name, String type) {
        final int sceneWidth = sourceProduct.getSceneRasterWidth();
        final int sceneHeight = sourceProduct.getSceneRasterHeight();

        Product targetProduct = new Product(name, type, sceneWidth, sceneHeight);
        copyProductTrunk(sourceProduct, targetProduct);
        return targetProduct;
    }

    /**
     * Copies basic information for a MERIS product to the target product
     *
     * @param sourceProduct - the source product
     * @param targetProduct - the target product
     */
    public void copyProductTrunk(Product sourceProduct,
                                 Product targetProduct) {
        copyTiePoints(sourceProduct, targetProduct);
        copyBaseGeoInfo(sourceProduct, targetProduct);
    }

    /**
     * Copies the tie point data.
     *
     * @param sourceProduct - the source product
     * @param targetProduct - the target product
     */
    private void copyTiePoints(Product sourceProduct,
                               Product targetProduct) {
        if (copyAllTiePoints) {
            // copy all tie point grids to output product
            ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        } else {
            for (int i = 0; i < sourceProduct.getNumTiePointGrids(); i++) {
                TiePointGrid srcTPG = sourceProduct.getTiePointGridAt(i);
                if (srcTPG.getName().equals("latitude") || srcTPG.getName().equals("longitude")) {
                    targetProduct.addTiePointGrid(srcTPG.cloneTiePointGrid());
                }
            }
        }
    }

    /**
     * Copies geocoding and the start and stop time.
     *
     * @param sourceProduct - the source product
     * @param targetProduct - the target product
     */
    private void copyBaseGeoInfo(Product sourceProduct,
                                 Product targetProduct) {
        // copy geo-coding to the output product
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
    }

}
