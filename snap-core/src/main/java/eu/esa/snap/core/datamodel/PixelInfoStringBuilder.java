package eu.esa.snap.core.datamodel;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.maptransf.MapProjection;
import org.esa.snap.core.dataop.maptransf.MapTransform;

import java.awt.geom.Point2D;

public class PixelInfoStringBuilder {

    public static void addProductName(String productName, StringBuilder sb) {
        sb.append("Product:\t");
        sb.append(productName).append("\n\n");
    }

    public static void addPixelLocation(int pixelX, int pixelY, StringBuilder sb) {
        sb.append("Image-X:\t");
        sb.append(pixelX);
        sb.append("\tpixel\n");

        sb.append("Image-Y:\t");
        sb.append(pixelY);
        sb.append("\tpixel\n");
    }

    public static void addPixelLocation(int pixelX, int pixelY, String nodeName, StringBuilder sb) {
        // Add the raster name to the identification of the pixel
        sb.append("Image-X." + nodeName + ":\t");
        sb.append(pixelX);
        sb.append("\tpixel\n");

        sb.append("Image-Y." + nodeName + ":\t");
        sb.append(pixelY);
        sb.append("\tpixel\n");
    }

    public static void addGeoPosInformation(PixelPos pixelPosRef, RasterDataNode raster, StringBuilder sb) {
        final GeoCoding rasterGeocoding = raster.getGeoCoding();
        if (rasterGeocoding != null) {
            final GeoPos geoPos = rasterGeocoding.getGeoPos(pixelPosRef, null);

            sb.append("Longitude:\t");
            sb.append(geoPos.getLonString());
            sb.append("\tdegree\n");

            sb.append("Latitude:\t");
            sb.append(geoPos.getLatString());
            sb.append("\tdegree\n");

            if (raster.getProduct().getSceneGeoCoding() instanceof MapGeoCoding mapGeoCoding) {
                final MapProjection mapProjection = mapGeoCoding.getMapInfo().getMapProjection();
                final MapTransform mapTransform = mapProjection.getMapTransform();
                final Point2D mapPoint = mapTransform.forward(geoPos, null);
                final String mapUnit = mapProjection.getMapUnit();

                sb.append("Map-X:\t");
                sb.append(mapPoint.getX());
                sb.append("\t").append(mapUnit).append("\n");

                sb.append("Map-Y:\t");
                sb.append(mapPoint.getY());
                sb.append("\t").append(mapUnit).append("\n");
            }
        }
    }
}
