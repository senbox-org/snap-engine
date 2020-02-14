package org.esa.snap.core.subset;

import org.esa.snap.core.datamodel.GeoCoding;

import java.awt.*;

/**
 * Created by jcoravu on 13/2/2020.
 */
public abstract class AbstractSubsetRegion {

    protected final int borderPixels;
    protected final boolean roundPixelRegion;

    protected AbstractSubsetRegion(int borderPixels, boolean roundPixelRegion) {
        if (borderPixels < 0) {
            throw new IllegalArgumentException("The border pixels " + borderPixels + " is negative.");
        }
        this.borderPixels = borderPixels;
        this.roundPixelRegion = roundPixelRegion;
    }

    public abstract Rectangle computeProductPixelRegion(GeoCoding productDefaultGeoCoding, int defaultProductWidth, int defaultProductHeight);

    public abstract Rectangle computeBandPixelRegion(GeoCoding productDefaultGeoCoding, GeoCoding bandDefaultGeoCoding, int defaultProductWidth,
                                                     int defaultProductHeight, int defaultBandWidth, int defaultBandHeight);


    protected void validateDefaultSize(int defaultProductWidth, int defaultProductHeight, String exceptionMessagePrefix) {
        if (defaultProductWidth < 1) {
            throw new IllegalArgumentException(exceptionMessagePrefix + " width "+defaultProductWidth+" must be greater or equal than 1.");
        }
        if (defaultProductHeight < 1) {
            throw new IllegalArgumentException(exceptionMessagePrefix + " height "+defaultProductHeight+" must be greater or equal than 1.");
        }
    }
}
