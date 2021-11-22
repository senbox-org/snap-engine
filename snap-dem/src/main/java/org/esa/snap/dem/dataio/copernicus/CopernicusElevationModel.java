package org.esa.snap.dem.dataio.copernicus;

import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.dem.BaseElevationModel;
import org.esa.snap.core.dataop.dem.ElevationFile;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;

import java.io.File;

public abstract class CopernicusElevationModel extends BaseElevationModel {

    private final static GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();

    public CopernicusElevationModel(final ElevationModelDescriptor descriptor, final Resampling resamplingMethod) {
        super(descriptor, resamplingMethod);
    }

    @Override
    public double getIndexX(final GeoPos geoPos) {
        return ((geoPos.lon + 180.0) * DEGREE_RES_BY_NUM_PIXELS_PER_TILE_X_inv);
    }

    @Override
    public double getIndexY(final GeoPos geoPos) {
        return ((90.0 - geoPos.lat) * DEGREE_RES_BY_NUM_PIXELS_PER_TILE_Y_inv);
    }

    @Override
    public GeoPos getGeoPos(final PixelPos pixelPos) {
        final double pixelLat = (RASTER_HEIGHT - pixelPos.y) * DEGREE_RES_BY_NUM_PIXELS_PER_TILE_Y - 90.0;
        final double pixelLon = pixelPos.x * DEGREE_RES_BY_NUM_PIXELS_PER_TILE_X - 180.0;
        return new GeoPos(pixelLat, pixelLon);
    }

    private void init(final GeoPos geoPos) {

        NUM_PIXELS_PER_TILE_X = CopernicusElevationTile.determineWidth(geoPos, getResolution());
        NUM_PIXELS_PER_TILE_Y = descriptor.getTileHeight();

        NUM_PIXELS_PER_TILE_X_inv = 1.0 / (double) NUM_PIXELS_PER_TILE_X;
        NUM_PIXELS_PER_TILE_Y_inv = 1.0 / (double) NUM_PIXELS_PER_TILE_Y;

        RASTER_WIDTH = NUM_X_TILES * NUM_PIXELS_PER_TILE_X;
        RASTER_HEIGHT = NUM_Y_TILES * NUM_PIXELS_PER_TILE_Y;

        DEGREE_RES_BY_NUM_PIXELS_PER_TILE_X = DEGREE_RES / (double) NUM_PIXELS_PER_TILE_X;
        DEGREE_RES_BY_NUM_PIXELS_PER_TILE_Y = DEGREE_RES / (double) NUM_PIXELS_PER_TILE_Y;
        DEGREE_RES_BY_NUM_PIXELS_PER_TILE_X_inv = 1.0 / DEGREE_RES_BY_NUM_PIXELS_PER_TILE_X;
        DEGREE_RES_BY_NUM_PIXELS_PER_TILE_Y_inv = 1.0 / DEGREE_RES_BY_NUM_PIXELS_PER_TILE_Y;
    }

    @Override
    public synchronized double getElevation(final GeoPos geoPos) throws Exception {

        if (geoPos.lon > 180) {
            geoPos.lon -= 360;
        }
        init(geoPos);

        final double pixelY = getIndexY(geoPos);
        if (pixelY < 0 || Double.isNaN(pixelY)) {
            return NO_DATA_VALUE;
        }

        final double elevation;

        Resampling.Index newIndex = resampling.createIndex();
        resampling.computeCornerBasedIndex(getIndexX(geoPos), pixelY, RASTER_WIDTH, RASTER_HEIGHT, newIndex);
        elevation = resampling.resample(resamplingRaster, newIndex);

        return Double.isNaN(elevation) ? NO_DATA_VALUE : elevation;
    }

    @Override
    protected void createElevationFile(ElevationFile[][] elevationFiles, int x, int y, File demInstallDir) {

        final int minLon = x * DEGREE_RES - 180;
        final int minLat = y * DEGREE_RES - 90;

        final String fileName = createTileFilename(minLat, minLon);
        final File localFile = new File(demInstallDir, fileName);
        try {
            elevationFiles[x][NUM_Y_TILES - 1 - y] = createElevationFile(this, localFile, plugIn.createReaderInstance());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    protected abstract ElevationFile createElevationFile(final CopernicusElevationModel model,
                                                         final File localFile, final ProductReader reader);

    public String createTileFilename(double minLat, double minLon) {
        int lat = (int) minLat;
        int lon = (int) minLon;
        return createTileFilename(lat, lon);
    }

    protected abstract int getResolution();

    protected String getTileFileNamePrefix() {
        return "Copernicus_DSM_COG_"+getResolution()+"_";
    }

    private String createTileFilename(int minLat, int minLon) {
        final StringBuilder name = new StringBuilder(getTileFileNamePrefix());
        name.append(minLat < 0 ? "S" : "N");
        String latString = String.valueOf(Math.abs(minLat));
        while (latString.length() < 2) {
            latString = '0' + latString;
        }
        name.append(latString);

        name.append("_00_");

        name.append(minLon < 0 ? "W" : "E");
        String lonString = String.valueOf(Math.abs(minLon));
        while (lonString.length() < 3) {
            lonString = '0' + lonString;
        }
        name.append(lonString);
        name.append("_00_DEM.tif");

        return name.toString();
    }
}
