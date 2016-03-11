/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s3tbx.meris;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.util.ProductUtils;

// todo (mp/20160311) BrrBasisOp, MerisBasisOp and BasisOp are all more or less the same --> harmonize

public abstract class MerisBasisOp extends Operator {

    @Parameter(description="If set to 'false' only the lat and lon tie-points will be copied to the target product", defaultValue="false")
    private boolean copyAllTiePoints = false;

    /**
     * Creates a new product with the same size
     *
     * @param sourceProduct the source product
     * @param name the name of the newly created target product
     * @param type the product type of the newly created target product
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
     * @param sourceProduct the source product
     * @param targetProduct the target product to copy the trunc to
     */
    public void copyProductTrunk(Product sourceProduct,
                                 Product targetProduct) {
        copyTiePoints(sourceProduct, targetProduct);
        copyBaseGeoInfo(sourceProduct, targetProduct);
    }

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

    private void copyBaseGeoInfo(Product sourceProduct,
                                 Product targetProduct) {
        // copy geo-coding to the output product
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
    }

}
