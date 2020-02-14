package org.esa.snap.core.subset;

import com.vividsolutions.jts.geom.Geometry;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;

/**
 * Created by jcoravu on 13/2/2020.
 */
public class GeometrySubsetRegion extends AbstractSubsetRegion {

    private final Geometry geometryRegion;

    public GeometrySubsetRegion(Geometry geometryRegion, int borderPixels, boolean roundPixelRegion) {
        super(borderPixels, roundPixelRegion);

        if (geometryRegion == null) {
            throw new NullPointerException("The geometry region is null.");
        }
        this.geometryRegion = geometryRegion;
    }

    @Override
    public Rectangle computeProductPixelRegion(GeoCoding productDefaultGeoCoding, int defaultProductWidth, int defaultProductHeight) {
        validateDefaultSize(defaultProductWidth, defaultProductHeight, "The default product");

        if (productDefaultGeoCoding == null) {
            throw new NullPointerException("The pixel region cannot be computed because the product GeoCoding is missing.");
        }
        return ProductUtils.computePixelRegionUsingGeometry(productDefaultGeoCoding, defaultProductWidth, defaultProductHeight, this.geometryRegion, this.borderPixels, this.roundPixelRegion);
    }

    @Override
    public Rectangle computeBandPixelRegion(GeoCoding productDefaultGeoCoding, GeoCoding bandDefaultGeoCoding, int defaultProductWidth,
                                            int defaultProductHeight, int defaultBandWidth, int defaultBandHeight) {

        validateDefaultSize(defaultProductWidth, defaultProductHeight, "The default product");
        validateDefaultSize(defaultBandWidth, defaultBandHeight, "The default band");

        if (defaultProductWidth != defaultBandWidth || defaultProductHeight != defaultBandHeight) {
            // the product is multisize
            if (bandDefaultGeoCoding == null) {
                throw new NullPointerException("The pixel region cannot be computed because the band GeoCoding is missing of the multi size product.");
            }
            return ProductUtils.computePixelRegionUsingGeometry(bandDefaultGeoCoding, defaultBandWidth, defaultBandHeight, this.geometryRegion, this.borderPixels, this.roundPixelRegion);
        } else {
            if (productDefaultGeoCoding == null) {
                throw new NullPointerException("The pixel region cannot be computed because the product GeoCoding is missing.");
            }
            return ProductUtils.computePixelRegionUsingGeometry(productDefaultGeoCoding, defaultProductWidth, defaultProductHeight, this.geometryRegion, this.borderPixels, this.roundPixelRegion);
        }
    }
}
