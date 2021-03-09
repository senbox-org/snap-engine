package org.esa.snap.core.dataio.geocoding.util;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;

public interface XYInterpolator {

    String SYSPROP_GEOCODING_INTERPOLATOR = "snap.core.geocoding.interpolator";

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
