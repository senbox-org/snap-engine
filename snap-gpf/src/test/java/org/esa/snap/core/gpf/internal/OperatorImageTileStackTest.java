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

package org.esa.snap.core.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.sun.media.jai.util.SunTileCache;
import junit.framework.TestCase;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.util.Map;

public class OperatorImageTileStackTest extends TestCase {


    public void testTileStackImage() {
        final SunTileCache tileCache = (SunTileCache) JAI.getDefaultInstance().getTileCache();
        tileCache.flush();
        long cacheTileCount = tileCache.getCacheTileCount();
        assertEquals(0, cacheTileCount);

        Operator operator = new StackTestOperator(tileCache);

        Product targetProduct = operator.getTargetProduct();
        assertNotNull(targetProduct);
        assertEquals(4, targetProduct.getNumBands());

        MultiLevelImage sourceImage = targetProduct.getBandAt(1).getSourceImage();
        RenderedImage image = sourceImage.getImage(0);
        assertNotNull(image);
        assertEquals(OperatorImageTileStack.class, image.getClass());
        // pull data to trigger computation
        image.getData();

        cacheTileCount = tileCache.getCacheTileCount();
        assertEquals(2, cacheTileCount);
    }

    private static class StackTestOperator extends Operator {
        private final SunTileCache tileCache;

        public StackTestOperator(SunTileCache tileCache) {
            this.tileCache = tileCache;
        }

        @Override
        public void initialize() throws OperatorException {
            Product product = new Product("name", "desc", 1, 1);

            Band bandA = product.addBand("a", ProductData.TYPE_INT32);
            product.addBand("b", ProductData.TYPE_INT32);
            product.addBand("c", ProductData.TYPE_INT32);
            Band bandD = product.addBand("d", ProductData.TYPE_FLOAT32);

            RenderedOp a = ConstantDescriptor.create(
                    (float) product.getSceneRasterWidth(),
                    (float) product.getSceneRasterHeight(),
                    new Integer[]{1}, null);
            bandA.setSourceImage(a);

            RenderedOp d = ConstantDescriptor.create(
                    (float) product.getSceneRasterWidth(),
                    (float) product.getSceneRasterHeight(),
                    new Float[]{4.0f}, null);

            bandD.setSourceImage(d);

            setTargetProduct(product);
        }

        @Override
        public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
            assertEquals(2, targetTiles.size());

            for (Tile tt : targetTiles.values()) {
                ProductData dataBuffer = tt.getDataBuffer();
                int numElems = dataBuffer.getNumElems();
                for (int i = 0; i < numElems; i++) {
                    dataBuffer.setElemIntAt(i, i);
                }
            }

            assertEquals(0, tileCache.getCacheTileCount());
        }
    }
}
