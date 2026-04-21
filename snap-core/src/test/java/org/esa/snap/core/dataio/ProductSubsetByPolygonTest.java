package org.esa.snap.core.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.metadata.MetadataInspector;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProductSubsetByPolygonTest {

    @Test
    public void testLoadPixelWkt_partlyOutside_clipsToProductBounds() throws Exception {
        final int width = 500;
        final int height = 500;
        final GeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, width, height, 0.0, 50.0, 0.1, -0.1);
        final MetadataInspector.Metadata metadata = createMetadata(width, height, geoCoding);
        final ProductSubsetByPolygon subsetByPolygon = new ProductSubsetByPolygon();

        final String wkt = "POLYGON((-20 100, 120 100, 120 30, -20 30, -20 100))";
        subsetByPolygon.loadPolygonFromWKTString(wkt, true, metadata, ProgressMonitor.NULL);

        final Polygon subsetPolygon = subsetByPolygon.getSubsetPolygon();
        assertNotNull(subsetPolygon);
        assertTrue(!subsetPolygon.isEmpty());
        assertAllCoordinatesWithinBounds(subsetPolygon, width, height);
        assertTrue(hasCoordinateAwayFromOrigin(subsetPolygon));
    }

    @Test
    public void testLoadGeoWkt_fullyOutside_throwsIllegalArgumentException() throws Exception {
        final int width = 500;
        final int height = 500;
        final GeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, width, height, 0.0, 50.0, 0.1, -0.1);
        final MetadataInspector.Metadata metadata = createMetadata(width, height, geoCoding);
        final ProductSubsetByPolygon subsetByPolygon = new ProductSubsetByPolygon();

        final String wkt = "POLYGON((-40 40, -20 40, -20 20, -40 20, -40 40))";
        try {
            subsetByPolygon.loadPolygonFromWKTString(wkt, false, metadata, ProgressMonitor.NULL);
            fail("Expected IllegalArgumentException for polygon outside the product.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("empty polygon"));
        }
    }

    @Test
    public void testLoadGeoWkt_withProjectedCrs_stillClipsToPixelBounds() throws Exception {
        final int width = 500;
        final int height = 500;
        final GeoCoding geoCoding = new CrsGeoCoding(CRS.decode("EPSG:32632"), width, height, 500000.0, 5500000.0, 30.0, -30.0);
        final MetadataInspector.Metadata metadata = createMetadata(width, height, geoCoding);
        final ProductSubsetByPolygon subsetByPolygon = new ProductSubsetByPolygon();

        final String wkt = "POLYGON((8.7 49.8, 9.5 49.8, 9.5 49.2, 8.7 49.2, 8.7 49.8))";
        subsetByPolygon.loadPolygonFromWKTString(wkt, false, metadata, ProgressMonitor.NULL);

        final Polygon subsetPolygon = subsetByPolygon.getSubsetPolygon();
        assertNotNull(subsetPolygon);
        assertTrue(!subsetPolygon.isEmpty());
        assertAllCoordinatesWithinBounds(subsetPolygon, width, height);
    }

    private static MetadataInspector.Metadata createMetadata(int width, int height, GeoCoding geoCoding) {
        final MetadataInspector.Metadata metadata = new MetadataInspector.Metadata(width, height);
        metadata.setGeoCoding(geoCoding);
        return metadata;
    }

    private static void assertAllCoordinatesWithinBounds(Polygon polygon, int width, int height) {
        final double minX = -1.0e-6;
        final double minY = -1.0e-6;
        final double maxX = (width - 1) + 1.0e-6;
        final double maxY = (height - 1) + 1.0e-6;
        for (Coordinate coordinate : polygon.getCoordinates()) {
            assertTrue("x out of bounds: " + coordinate.getX(), coordinate.getX() >= minX && coordinate.getX() <= maxX);
            assertTrue("y out of bounds: " + coordinate.getY(), coordinate.getY() >= minY && coordinate.getY() <= maxY);
        }
    }

    private static boolean hasCoordinateAwayFromOrigin(Polygon polygon) {
        for (Coordinate coordinate : polygon.getCoordinates()) {
            if (coordinate.getX() > 1.0 || coordinate.getY() > 1.0) {
                return true;
            }
        }
        return false;
    }
}
