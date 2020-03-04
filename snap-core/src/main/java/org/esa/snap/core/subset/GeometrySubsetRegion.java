package org.esa.snap.core.subset;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.util.GeoUtils;
import org.esa.snap.core.util.ProductUtils;
import org.locationtech.jts.geom.Geometry;

import java.awt.*;

/**
 * Created by jcoravu on 13/2/2020.
 */
public class GeometrySubsetRegion extends AbstractSubsetRegion {

    private final Geometry geometryRegion;

    public GeometrySubsetRegion(Geometry geometryRegion, int borderPixels) {
        super(borderPixels);

        if (geometryRegion == null) {
            throw new NullPointerException("The geometry region is null.");
        }
        this.geometryRegion = geometryRegion;
    }

    @Override
    public Rectangle computeProductPixelRegion(GeoCoding productDefaultGeoCoding, int defaultProductWidth, int defaultProductHeight, boolean roundPixelRegion) {
        validateDefaultSize(defaultProductWidth, defaultProductHeight, "The default product");

        if (productDefaultGeoCoding == null) {
            throw new NullPointerException("The pixel region cannot be computed because the product GeoCoding is missing.");
        }
        return GeoUtils.computePixelRegionUsingGeometry(productDefaultGeoCoding, defaultProductWidth, defaultProductHeight, this.geometryRegion, this.borderPixels, roundPixelRegion);
    }

    @Override
    public Rectangle computeBandPixelRegion(GeoCoding productDefaultGeoCoding, GeoCoding bandDefaultGeoCoding, int defaultProductWidth,
                                            int defaultProductHeight, int defaultBandWidth, int defaultBandHeight, boolean roundPixelRegion) {

        validateDefaultSize(defaultProductWidth, defaultProductHeight, "The default product");
        // test if the band width and band height > 0
        super.validateDefaultSize(defaultBandWidth, defaultBandHeight, "The default band");

        if (defaultProductWidth != defaultBandWidth || defaultProductHeight != defaultBandHeight) {
            // the product is multisize
            if (bandDefaultGeoCoding == null) {
                throw new NullPointerException("The pixel region cannot be computed because the band GeoCoding is missing of the multi size product.");
            }
            return GeoUtils.computePixelRegionUsingGeometry(bandDefaultGeoCoding, defaultBandWidth, defaultBandHeight, this.geometryRegion, this.borderPixels, roundPixelRegion);
        } else {
            if (productDefaultGeoCoding == null) {
                throw new NullPointerException("The pixel region cannot be computed because the product GeoCoding is missing.");
            }
            return GeoUtils.computePixelRegionUsingGeometry(productDefaultGeoCoding, defaultProductWidth, defaultProductHeight, this.geometryRegion, this.borderPixels, roundPixelRegion);
        }
    }
}
