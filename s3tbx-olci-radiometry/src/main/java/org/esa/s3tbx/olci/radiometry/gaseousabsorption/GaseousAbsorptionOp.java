/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.olci.radiometry.gaseousabsorption;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;


/**
 * @author muhammad.bc.
 */
@OperatorMetadata(alias = "OLCI.GaseousAsorption",
        authors = "Marco Peters, Muhamamd Bala (Brockmann Consult)",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Correct the influence of atmospheric gas absorption for those OLCI channels.")
public class GaseousAbsorptionOp extends Operator {

    @SourceProduct(description = "OLCI Refelctance product")
    Product sourceProduct;
    private Product targetProduct;
    private GaseousAbsorptionAlgorithm gasAbsorptionAlgo = new GaseousAbsorptionAlgorithm();

    @Override
    public void initialize() throws OperatorException {
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        for (Band band : sourceProduct.getBands()) {
            final Band targetBand = targetProduct.addBand(band.getName(), band.getDataType());
            ProductUtils.copyRasterDataNodeProperties(band, targetBand);
        }

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());
        setTargetProduct(targetProduct);


    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Rectangle rectangle = targetTile.getRectangle();
        final float[] computedGases = computeGas(targetBand.getName(), rectangle, sourceProduct);
        if (computedGases != null) {
            targetTile.setSamples(computedGases);
        }
    }

    private float[] computeGas(String bandName, Rectangle rectangle, Product sourceProduct) {
        float[] szas = getSourceTile(sourceProduct.getTiePointGrid("SZA"), rectangle).getSamplesFloat();
        float[] ozas = getSourceTile(sourceProduct.getTiePointGrid("OZA"), rectangle).getSamplesFloat();
        return gasAbsorptionAlgo.getTransmissionGas(bandName, szas, ozas);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GaseousAbsorptionOp.class);
        }
    }

}
