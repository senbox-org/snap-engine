package org.esa.snap.dem.dataio.copernicus.copernicus90m;

import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.dem.BaseElevationModel;
import org.esa.snap.core.dataop.dem.ElevationFile;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.snap.dem.dataio.cdem.CDEMElevationTile;
import org.esa.snap.dem.dataio.cdem.CDEMFile;
import org.esa.snap.dem.dataio.copernicus.CopernicusDownloader;
import org.esa.snap.dem.dataio.copernicus.CopernicusElevationTile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Copernicus90mElevationModel extends BaseElevationModel {

    private Map<String, List<Copernicus90mFile>> tileMap = new HashMap<>();
    private static final ProductReaderPlugIn productReaderPlugIn = getReaderPlugIn("GeoTIFF");


    public Copernicus90mElevationModel(ElevationModelDescriptor descriptor, Resampling resamplingMethod) {
        super(descriptor, resamplingMethod);
    }

    @Override
    public double getIndexX(final GeoPos geoPos) {
        return ((geoPos.lon + 180.0) * DEGREE_RES_BY_NUM_PIXELS_PER_TILEinv);
    }

    @Override
    public double getIndexY(final GeoPos geoPos) {
        return ((90.0 - geoPos.lat) * DEGREE_RES_BY_NUM_PIXELS_PER_TILEinv);
    }

    @Override
    public GeoPos getGeoPos(final PixelPos pixelPos) {
        final double pixelLat = (RASTER_HEIGHT - pixelPos.y) * DEGREE_RES_BY_NUM_PIXELS_PER_TILE - 90.0;
        final double pixelLon = pixelPos.x * DEGREE_RES_BY_NUM_PIXELS_PER_TILE - 180.0;
        return new GeoPos(pixelLat, pixelLon);
    }


    private void init(final GeoPos geoPos) {

        int width = CopernicusElevationTile.determineWidth(geoPos, 90);
        NUM_PIXELS_PER_TILE = width;


        NUM_PIXELS_PER_TILEinv = 1.0 / (double) NUM_PIXELS_PER_TILE;

        RASTER_WIDTH = NUM_X_TILES * NUM_PIXELS_PER_TILE;
        RASTER_HEIGHT = NUM_Y_TILES * NUM_PIXELS_PER_TILE;

        DEGREE_RES_BY_NUM_PIXELS_PER_TILE = DEGREE_RES / (double) NUM_PIXELS_PER_TILE;
        DEGREE_RES_BY_NUM_PIXELS_PER_TILEinv = 1.0 / DEGREE_RES_BY_NUM_PIXELS_PER_TILE;
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
        //synchronized(resampling) {
        Resampling.Index newIndex = resampling.createIndex();
        resampling.computeCornerBasedIndex(getIndexX(geoPos), pixelY, RASTER_WIDTH, RASTER_HEIGHT, newIndex);
        elevation = resampling.resample(resamplingRaster, newIndex);
        //}
        return Double.isNaN(elevation) ? NO_DATA_VALUE : elevation;
    }

    @Override
    public int getWidth(){
        return RASTER_HEIGHT;
    }

    @Override
    protected void createElevationFile(ElevationFile[][] elevationFiles, int x, int y, File demInstallDir) {

        final int minLon = x * DEGREE_RES - 180;
        final int minLat = y * DEGREE_RES - 90;

        final String fileName = createTileFilename(minLat, minLon);
        final File localFile = new File(demInstallDir, fileName);
        GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        try {
            elevationFiles[x][NUM_Y_TILES - 1 - y] = new Copernicus90mFile(this, localFile, plugIn.createReaderInstance());
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    public static String createTileFilename(double minLat, double minLon) {
        int lat = (int) minLat;
        int lon = (int) minLon;
        return createTileFilename(lat, lon);
    }

    public static String createTileFilename(int minLat, int minLon) {
        final StringBuilder name = new StringBuilder("Copernicus_DSM_COG_30_");
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
