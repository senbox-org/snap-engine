package org.esa.snap.core.dataio.geocoding.util;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;

public interface XYInterpolator {

    String SYSPROP_GEOCODING_INTERPOLATOR = "snap.core.geocoding.interpolator";

    /**
     * Interpolates the PixelPos based on the GeoPos
     *
     * @param geoPos   the geo position
     * @param pixelPos the pixel position
     * @param context  the interpolation context
     * @return the new interpolated pixel position
     */
    PixelPos interpolate(GeoPos geoPos, PixelPos pixelPos, InterpolationContext context);

    enum Type {
        EUCLIDIAN {
            @Override
            public XYInterpolator get() {
                return new EuclidianDistanceWeightingInterpolator();
            }
        },
        GEODETIC {
            @Override
            public XYInterpolator get() {
                return new GeodeticDistanceWeightingInterpolator();
            }
        };

        public abstract XYInterpolator get();
    }
}
