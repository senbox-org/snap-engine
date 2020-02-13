package org.esa.snap.core.subset;

import com.vividsolutions.jts.geom.Geometry;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.util.ImageUtils;

import java.awt.*;

/**
 * Created by jcoravu on 13/2/2020.
 */
public class PixelSubsetRegion extends AbstractSubsetRegion {

    private final Rectangle pixelRegion;

    public PixelSubsetRegion(int x, int y, int width, int height, int borderPixels, boolean roundPixelRegion) {
        super(borderPixels, roundPixelRegion);

        if (x < 0 || y < 0 || width < 1 || height < 1) {
            throw new IllegalArgumentException("The pixel region 'x="+x+", y="+y+", width="+width+", height="+height+"' is invalid.");
        }
        this.pixelRegion = new Rectangle(x, y, width, height);
    }

    public PixelSubsetRegion(Rectangle pixelRegion, int borderPixels, boolean roundPixelRegion) {
        super(borderPixels, roundPixelRegion);

        if (pixelRegion == null) {
            throw new NullPointerException("The pixel region is null.");
        }
        if (pixelRegion.x < 0 || pixelRegion.y < 0 || pixelRegion.width < 1 || pixelRegion.height < 1) {
            throw new IllegalArgumentException("The pixel region '"+pixelRegion+"' is invalid.");
        }
        this.pixelRegion = pixelRegion;
    }

    @Override
    public Rectangle computeProductPixelRegion(GeoCoding productDefaultGeoCoding, int defaultProductWidth, int defaultProductHeight) {
        return this.pixelRegion;
    }

    @Override
    public Rectangle computeBandPixelRegion(GeoCoding productDefaultGeoCoding, GeoCoding bandDefaultGeoCoding, int defaultProductWidth,
                                            int defaultProductHeight, int defaultBandWidth, int defaultBandHeight) {

        if (defaultProductWidth != defaultBandWidth || defaultProductHeight != defaultBandHeight) {
            // the product is multisize
            if (productDefaultGeoCoding != null && bandDefaultGeoCoding != null) {
                Geometry productGeometryRegion = computeGeometryUsingPixelRegion(productDefaultGeoCoding, defaultProductWidth, defaultProductHeight, this.pixelRegion);
                return computePixelRegionUsingGeometry(bandDefaultGeoCoding, defaultBandWidth, defaultBandHeight, productGeometryRegion, this.borderPixels, this.roundPixelRegion);
            }
            return ImageUtils.computeBandBoundsBasedOnPercent(this.pixelRegion, defaultProductWidth, defaultProductHeight, defaultBandWidth, defaultBandHeight);
        } else {
            return this.pixelRegion;
        }
    }
}
