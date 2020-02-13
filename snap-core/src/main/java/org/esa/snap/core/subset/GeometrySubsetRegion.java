package org.esa.snap.core.subset;

import com.vividsolutions.jts.geom.Geometry;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;

/**
 * Created by jcoravu on 13/2/2020.
 */
public class GeometrySubsetRegion extends AbstractSubsetRegion {

    private final Geometry geometryRegion;
    private final int borderPixels;
    private final boolean roundPixelRegion;

    public GeometrySubsetRegion(Geometry geometryRegion, int borderPixels, boolean roundPixelRegion) {
        if (geometryRegion == null) {
            throw new NullPointerException("The geometry region is null.");
        }
        if (borderPixels < 0) {
            throw new IllegalArgumentException("The border pixels " + borderPixels + " is negative.");
        }
        this.geometryRegion = geometryRegion;
        this.borderPixels = borderPixels;
        this.roundPixelRegion = roundPixelRegion;
    }

    @Override
    public Rectangle computePixelRegion(GeoCoding productDefaultGeoCoding, int defaultProductWidth, int defaultProductHeight) {
        if (productDefaultGeoCoding == null) {
            throw new NullPointerException("The pixel region cannot be computed because the product GeoCoding is missing.");
        }
        return computePixelRegionUsingGeometry(productDefaultGeoCoding, defaultProductWidth, defaultProductHeight, this.geometryRegion, this.borderPixels, this.roundPixelRegion);
    }

    public Rectangle computeBandBounds(GeoCoding productDefaultGeoCoding, GeoCoding bandDefaultGeoCoding, int defaultProductWidth, int defaultProductHeight, int defaultBandWidth, int defaultBandHeight) {
        ProductSubsetDef subsetDef = null;

        Rectangle bandBounds = null;

        if (defaultProductWidth != defaultBandWidth || defaultProductHeight != defaultBandHeight) {
            // the product is multisize




            if (subsetDef.isGeoRegion() && bandDefaultGeoCoding == null) {
                throw new IllegalArgumentException("The geoRegion subset cannot be done because the product GeoCoding is missing!");
            } else if (subsetDef.isGeoRegion()) {
                Geometry geoRegion = subsetDef.getGeoRegion();
                bandBounds = ProductUtils.computePixelRegion(bandDefaultGeoCoding, defaultBandWidth, defaultBandHeight, geoRegion, 0);
            } else if (productDefaultGeoCoding != null && bandDefaultGeoCoding != null) {//pixel subset region
                Rectangle productSubsetBounds = subsetDef.getRegion();
                Geometry geoRegion = ProductUtils.computeGeoRegion(productDefaultGeoCoding, defaultProductWidth, defaultProductHeight, productSubsetBounds);
                bandBounds = ProductUtils.computePixelRegion(bandDefaultGeoCoding, defaultBandWidth, defaultBandHeight, geoRegion, 0);
            } else {
                bandBounds = ImageUtils.computeBandBoundsBasedOnPercent(subsetDef.getRegion(), defaultProductWidth, defaultProductHeight, defaultBandWidth, defaultBandHeight);
            }
        } else {
            bandBounds = subsetDef.getRegion();
        }
        return bandBounds;
    }

}
