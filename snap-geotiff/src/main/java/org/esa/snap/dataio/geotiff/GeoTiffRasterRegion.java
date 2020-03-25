package org.esa.snap.dataio.geotiff;

import java.awt.image.Raster;

/**
 * Created by jcoravu on 24/3/2020.
 */
public interface GeoTiffRasterRegion {

    public Raster readRect(boolean isGlobalShifted180, int sourceOffsetX, int sourceOffsetY, int sourceStepX, int sourceStepY,
                           int destOffsetX, int destOffsetY, int destWidth, int destHeight)
                           throws Exception;
}
