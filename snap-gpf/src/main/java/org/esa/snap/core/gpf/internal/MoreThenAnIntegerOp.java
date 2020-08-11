package org.esa.snap.core.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.TargetProduct;

import java.awt.Rectangle;
import java.util.Map;

@OperatorMetadata(alias = "MoreThenAnIntegerOp",
        category = "Raster/Testing",
        internal = true,
        description = "just for testing")
public class MoreThenAnIntegerOp extends Operator {

    @TargetProduct
    private Product targetProduct;

    private static final String PRODUCT_TYPE = "Dummy";

    @Override
    public void initialize() throws OperatorException {

        try {
            int targetImageWidth = 53568;
            int targetImageHeight = 50000;
            targetProduct = new Product("MoreThenAnInteger", PRODUCT_TYPE,
                                        targetImageWidth,
                                        targetImageHeight);
            targetProduct.addBand("dummy", ProductData.TYPE_INT8);
        } catch (Throwable e) {
            throw new OperatorException(e);
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        Rectangle tileRectangle = targetTile.getRectangle();
        final int x0 = tileRectangle.x;
        final int y0 = tileRectangle.y;
        final int xMax = x0 + tileRectangle.width;
        final int yMax = y0 + tileRectangle.height;

        final ProductData targetBuffer = targetTile.getDataBuffer();
        final TileIndex targetIndex = new TileIndex(targetTile);
        for (int y = y0; y < yMax; y++) {
            targetIndex.calculateStride(y);
            for (int x = x0; x < xMax; x++) {
                final int targetIdx = targetIndex.getIndex(x);
                targetBuffer.setElemDoubleAt(targetIdx, x % 127);
            }
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(Map, Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(MoreThenAnIntegerOp.class);
        }
    }

    private static final class TileIndex {

        private final int tileOffset;
        private final int tileStride;
        private final int tileMinX;
        private final int tileMinY;

        private int offset = 0;

        public TileIndex(final Tile tile) {
            tileOffset = tile.getScanlineOffset();
            tileStride = tile.getScanlineStride();
            tileMinX = tile.getMinX();
            tileMinY = tile.getMinY();
        }

        /**
         * calculates offset
         *
         * @param ty y pos
         * @return offset
         */
        public int calculateStride(final int ty) {
            offset = tileMinX - (((ty - tileMinY) * tileStride) + tileOffset);
            return offset;
        }

        public int getOffset() {
            return offset;
        }

        public int getIndex(final int tx) {
            return tx - offset;
        }
    }
}
