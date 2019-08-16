package org.esa.snap.core.util.grid.isin;


/*
    This class implements an interface to the Integerized Sinusoidal (ISIN) Raster used by NASA data products.

    The API exposes methods for converting lon/lat to x/y and back as wells as support for ISIN tiles.
 */

public class IsinAPI {

    private final Tile tile;

    public enum Raster {
        GRID_1_KM,
        GRID_500_M,
        GRID_250_M
    }

    static final double TO_RAD = Math.PI / 180.0;
    private static final double TO_DEG = 180.0 / Math.PI ;

    /**
     * Constructs the API and initializes internal parameter according to the raster dimensions passed in.
     *
     * @param raster the raster dimension
     */
    public IsinAPI(Raster raster) {
        final ProjectionParam projectionParam = getProjectionParam(raster);

        tile = new Tile();
        tile.init(projectionParam);
    }

    /**
     * Map the geo-location (lon/lat) to the global integerized sinusoidal raster.
     * The point returned contains the global map x and y coordinates.
     *
     * @param lon longitude
     * @param lat latitude
     * @return the mapped location
     */
    public IsinPoint toGlobalMap(double lon, double lat) {
        return tile.forwardGlobalMap(lon * TO_RAD, lat * TO_RAD);
    }

    /**
     * Map the location (x/y) on the global integerized sinusoidal raster to the geo-location.
     * The point returned contains the longitude and latitude coordinates in decimal degrees as x and y values.
     *
     * @param x global map x coordinate
     * @param y global map y coordinate
     * @return the geo-location
     */
    public IsinPoint globalMapToGeo(double x, double y) {
        final IsinPoint isinPoint = tile.inverseGlobalMap(x, y);
        return new IsinPoint(isinPoint.getX() * TO_DEG, isinPoint.getY() * TO_DEG);
    }

    /**
     * Map the location (x/y,tileH,tileV) on the tiled global integerized sinusoidal raster to the geo-location.
     * The point returned contains the longitude and latitude coordinates in decimal degrees as x and y values.
     * This method expects zero based tile indices as input parameters.
     *
     * @param x tile x coordinate
     * @param y tile y coordinate
     * @param  tileH horizontal tile index
     * @param  tileV vertical tile index
     * @return the mapped location
     */
    public IsinPoint tileImageCoordinatesToGeo(double x, double y, int tileH, int tileV) {
        final IsinPoint isinPoint = tile.inverseTileImage(x, y, tileH, tileV);
        return new IsinPoint(isinPoint.getX() * TO_DEG, isinPoint.getY() * TO_DEG);
    }

    /**
     * Map the location (lon/lat) to the tiled global integerized sinusoidal raster.
     * The point returned contains the map x and y coordinates within the tile and the horizontal and vertical
     * (zero based) tile indices.
     *
     * @param lon longitude
     * @param lat latitude
     * @return the mapped location
     */
    public IsinPoint toTileImageCoordinates(double lon, double lat) {
        return tile.forwardTileImage(lon * TO_RAD, lat * TO_RAD);
    }

    /**
     * Retrieves the dimensions of a tile at the selected resolution.
     * The point returned contains the tile x and y dimensions.
     *
     * @return the tile dimensions
     */
    public IsinPoint getTileDimensions() {
        final long tile_height = tile.getNl_tile();
        final long tile_width = tile.getNs_tile();
        return new IsinPoint(tile_width, tile_height);
    }

    static ProjectionParam getProjectionParam(Raster raster) {
        ProjectionType projectionType;
        if (raster == Raster.GRID_1_KM) {
            projectionType = ProjectionType.ISIN_K;
        } else if (raster == Raster.GRID_500_M) {
            projectionType = ProjectionType.ISIN_H;
        } else if (raster == Raster.GRID_250_M) {
            projectionType = ProjectionType.ISIN_Q;
        } else {
            throw new RuntimeException("Illegal projection type");
        }

        return ProjectionParamFactory.get(projectionType);
    }
}
