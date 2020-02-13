package org.esa.snap.core.subset;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;
import java.awt.geom.GeneralPath;

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

    public static Rectangle computePixelRegionUsingGeometry(GeoCoding rasterGeoCoding, int rasterWidth, int rasterHeight,
                                                            Geometry geometryRegion, int numBorderPixels, boolean roundPixelRegion) {

        final Geometry productGeometry = computeProductGeometry(rasterGeoCoding, rasterWidth, rasterHeight);
        final Geometry regionIntersection = geometryRegion.intersection(productGeometry);
        if (regionIntersection.isEmpty()) {
            return new Rectangle();
        }
        final ProductUtils.PixelRegionFinder pixelRegionFinder = new ProductUtils.PixelRegionFinder(rasterGeoCoding, roundPixelRegion);
        regionIntersection.apply(pixelRegionFinder);
        final Rectangle pixelRegion = pixelRegionFinder.getPixelRegion();
        pixelRegion.grow(numBorderPixels, numBorderPixels);
        return pixelRegion.intersection(new Rectangle(rasterWidth, rasterHeight));
    }

    public static Geometry computeGeometryUsingPixelRegion(GeoCoding rasterGeoCoding, int rasterWidth, int rasterHeight, Rectangle pixelRegion) {
        final int step = Math.min(pixelRegion.width, pixelRegion.height) / 8;
        GeneralPath[] paths = ProductUtils.createGeoBoundaryPathsArray(rasterGeoCoding, rasterWidth, rasterHeight, pixelRegion, step, false);
        final com.vividsolutions.jts.geom.Polygon[] polygons = new com.vividsolutions.jts.geom.Polygon[paths.length];
        final GeometryFactory factory = new GeometryFactory();
        for (int i = 0; i < paths.length; i++) {
            polygons[i] = ProductUtils.convertAwtPathToJtsPolygon(paths[i], factory);
        }
        if (polygons.length == 1) {
            return polygons[0];
        } else {
            return factory.createMultiPolygon(polygons);
        }
    }

    public static Geometry computeProductGeometry(GeoCoding productGeoCoding, int productWidth, int productHeight) {
        final GeneralPath[] paths = ProductUtils.createGeoBoundaryPaths(productGeoCoding, productWidth, productHeight);
        final com.vividsolutions.jts.geom.Polygon[] polygons = new com.vividsolutions.jts.geom.Polygon[paths.length];
        final GeometryFactory factory = new GeometryFactory();
        for (int i = 0; i < paths.length; i++) {
            polygons[i] = ProductUtils.convertAwtPathToJtsPolygon(paths[i], factory);
        }
        final DouglasPeuckerSimplifier peuckerSimplifier = new DouglasPeuckerSimplifier(polygons.length == 1 ? polygons[0] : factory.createMultiPolygon(polygons));
        return peuckerSimplifier.getResultGeometry();
    }
}
