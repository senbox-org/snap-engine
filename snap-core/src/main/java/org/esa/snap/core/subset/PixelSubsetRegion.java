package org.esa.snap.core.subset;

import org.esa.snap.core.datamodel.GeoCoding;

import java.awt.*;

/**
 * Created by jcoravu on 13/2/2020.
 */
public class PixelSubsetRegion extends AbstractSubsetRegion {

    private final Rectangle pixelRegion;

    public PixelSubsetRegion(int x, int y, int width, int height) {
        if (x < 0 || y < 0 || width < 1 || height < 1) {
            throw new IllegalArgumentException("The pixel region 'x="+x+", y="+y+", width="+width+", height="+height+"' is invalid.");
        }
        this.pixelRegion = new Rectangle(x, y, width, height);
    }

    public PixelSubsetRegion(Rectangle pixelRegion) {
        if (pixelRegion == null) {
            throw new NullPointerException("The pixel region is null.");
        }
        if (pixelRegion.x < 0 || pixelRegion.y < 0 || pixelRegion.width < 1 || pixelRegion.height < 1) {
            throw new IllegalArgumentException("The pixel region '"+pixelRegion+"' is invalid.");
        }
        this.pixelRegion = pixelRegion;
    }

    @Override
    public Rectangle computePixelRegion(GeoCoding rasterGeoCoding, int rasterWidth, int rasterHeight) {
        return this.pixelRegion;
    }
}
