package org.esa.snap.core.subset;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.util.GeoUtils;
import org.esa.snap.core.util.ProductUtils;
import org.locationtech.jts.geom.Geometry;

import java.awt.*;

/**
 * Created by jcoravu on 13/2/2020.
 */
public class PixelSubsetRegion extends AbstractSubsetRegion {

    private final Rectangle pixelRegion;

    public PixelSubsetRegion(int x, int y, int width, int height, int borderPixels) {
        super(borderPixels);

        if (x < 0 || y < 0 || width < 1 || height < 1) {
            throw new IllegalArgumentException("The pixel region 'x="+x+", y="+y+", width="+width+", height="+height+"' is invalid.");
        }
        this.pixelRegion = new Rectangle(x, y, width, height);
    }

    public PixelSubsetRegion(Rectangle pixelRegion, int borderPixels) {
        super(borderPixels);

        if (pixelRegion == null) {
            throw new NullPointerException("The pixel region is null.");
        }
        if (pixelRegion.x < 0 || pixelRegion.y < 0 || pixelRegion.width < 1 || pixelRegion.height < 1) {
            throw new IllegalArgumentException("The pixel region '"+pixelRegion+"' is invalid.");
        }
        this.pixelRegion = pixelRegion;
    }

    @Override
    public Rectangle computeProductPixelRegion(GeoCoding productDefaultGeoCoding, int defaultProductWidth, int defaultProductHeight, boolean roundPixelRegion) {
        validateDefaultSize(defaultProductWidth, defaultProductHeight, "The default product");
        return this.pixelRegion;
    }

    @Override
    public Rectangle computeBandPixelRegion(GeoCoding productDefaultGeoCoding, GeoCoding bandDefaultGeoCoding, int defaultProductWidth,
                                            int defaultProductHeight, int defaultBandWidth, int defaultBandHeight, boolean roundPixelRegion) {

        validateDefaultSize(defaultProductWidth, defaultProductHeight, "The default product");
        // test if the band width and band height > 0
        super.validateDefaultSize(defaultBandWidth, defaultBandHeight, "The default band");

        if (defaultProductWidth != defaultBandWidth || defaultProductHeight != defaultBandHeight) {
            // the product is multisize
            if (productDefaultGeoCoding != null && bandDefaultGeoCoding != null) {
                Geometry productGeometryRegion = GeoUtils.computeGeometryUsingPixelRegion(productDefaultGeoCoding, this.pixelRegion);
                return GeoUtils.computePixelRegionUsingGeometry(bandDefaultGeoCoding, defaultBandWidth, defaultBandHeight, productGeometryRegion, this.borderPixels, roundPixelRegion);
            }
            return computeBandBoundsBasedOnPercent(this.pixelRegion, defaultProductWidth, defaultProductHeight, defaultBandWidth, defaultBandHeight);
        } else {
            return this.pixelRegion;
        }
    }

    @Override
    protected void validateDefaultSize(int defaultProductWidth, int defaultProductHeight, String exceptionMessagePrefix) {
        super.validateDefaultSize(defaultProductWidth, defaultProductHeight, exceptionMessagePrefix);

        if (defaultProductWidth < this.pixelRegion.width) {
            throw new IllegalArgumentException(exceptionMessagePrefix + " width '"+defaultProductWidth+"' must be greater or equal than the pixel region width " + this.pixelRegion.width + ".");
        }
        if (defaultProductHeight < this.pixelRegion.height) {
            throw new IllegalArgumentException(exceptionMessagePrefix + " height '"+defaultProductHeight+"' must be greater or equal than the pixel region height " + this.pixelRegion.height + ".");
        }
    }

    public Rectangle getPixelRegion() {
        return pixelRegion;
    }

    private static Rectangle computeBandBoundsBasedOnPercent(Rectangle productBounds, int defaultProductWidth, int defaultProductHeight, int defaultBandWidth, int defaultBandHeight) {
        float productOffsetXPercent = productBounds.x / (float)defaultProductWidth;
        float productOffsetYPercent = productBounds.y / (float)defaultProductHeight;
        float productWidthPercent = productBounds.width / (float)defaultProductWidth;
        float productHeightPercent = productBounds.height / (float)defaultProductHeight;
        int bandOffsetX = (int)(productOffsetXPercent * defaultBandWidth);
        int bandOffsetY = (int)(productOffsetYPercent * defaultBandHeight);
        int bandWidth = (int)(productWidthPercent * defaultBandWidth);
        int bandHeight = (int)(productHeightPercent * defaultBandHeight);
        return new Rectangle(bandOffsetX, bandOffsetY, bandWidth, bandHeight);
    }
}
