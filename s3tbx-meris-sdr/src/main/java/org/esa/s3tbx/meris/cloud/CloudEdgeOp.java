/*
 * $Id: CloudEdgeOp.java,v 1.1 2007/03/27 12:52:22 marcoz Exp $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.s3tbx.meris.cloud;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.meris.MerisBasisOp;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.RectangleExtender;

import java.awt.*;

@OperatorMetadata(alias = "Meris.CloudEdge", internal = true)
public class CloudEdgeOp extends MerisBasisOp {

    private RectangleExtender rectCalculator;

    private Band sourceBand;

    @SourceProduct(alias="input")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;
    @Parameter
    private int cloudWidth;


    @Override
    public void initialize() throws OperatorException {
        targetProduct = createCompatibleProduct(sourceProduct, "cloude_edge", "MER_L2");
        sourceBand = sourceProduct.getBand(CombinedCloudOp.FLAG_BAND_NAME);
        ProductUtils.copyBand(CombinedCloudOp.FLAG_BAND_NAME, sourceProduct, targetProduct, false);

        if (cloudWidth == 0) {
            cloudWidth = 1;
        }
        rectCalculator = new RectangleExtender(new Rectangle(sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight()), cloudWidth, cloudWidth);
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        Rectangle targetRectangle = targetTile.getRectangle();
		Rectangle sourceRectangle = rectCalculator.extend(targetRectangle);
        final int size = sourceRectangle.height * sourceRectangle.width;
        pm.beginTask("Processing frame...", size + 1);
        try {
            Tile cloudSource = getSourceTile(sourceBand, sourceRectangle);

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    targetTile.setSample(x, y, cloudSource.getSampleInt(x, y));
                }
            }

            for (int y = sourceRectangle.y; y < sourceRectangle.y + sourceRectangle.height; y++) {
                for (int x = sourceRectangle.x; x < sourceRectangle.x + sourceRectangle.width; x++) {
                    final int cloudFlag = cloudSource.getSampleInt(x, y);
                    if ((cloudFlag & CombinedCloudOp.FLAG_CLOUD) != 0 ||
                            (cloudFlag & CombinedCloudOp.FLAG_CLOUD_SHADOW) != 0) {
                        markEdgeAround(x, y, cloudSource, targetTile);
                    }
                }
            }
        } finally {
            pm.done();
        }
    }

    private void markEdgeAround(int xi, int yi, Tile cloudSource, Tile cloudTarget) {
    	Rectangle targetRectangle = cloudTarget.getRectangle();
        int xStart = xi - cloudWidth;
        if (xStart < targetRectangle.x) {
            xStart = targetRectangle.x;
        }
        int xEnd = xi + cloudWidth;
        if (xEnd > targetRectangle.x + targetRectangle.width - 1) {
            xEnd = targetRectangle.x + targetRectangle.width - 1;
        }
        int yStart = yi - cloudWidth;
        if (yStart < targetRectangle.y) {
            yStart = targetRectangle.y;
        }
        int yEnd = yi + cloudWidth;
        if (yEnd > targetRectangle.y + targetRectangle.height - 1) {
            yEnd = targetRectangle.y + targetRectangle.height - 1;
        }

        for (int y = yStart; y <= yEnd; y++) {
            for (int x = xStart; x <= xEnd; x++) {
                int pixelValue = cloudSource.getSampleInt(x, y);
                if (pixelValue != CombinedCloudOp.FLAG_INVALID
                        && (pixelValue & CombinedCloudOp.FLAG_CLOUD) == 0
                        && (pixelValue & CombinedCloudOp.FLAG_CLOUD_SHADOW) == 0                        
                        && (pixelValue & CombinedCloudOp.FLAG_CLOUD_EDGE) == 0) {
                    pixelValue += CombinedCloudOp.FLAG_CLOUD_EDGE;
                    cloudTarget.setSample(x, y, pixelValue);
                }
            }
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(CloudEdgeOp.class);
        }
    }
}
