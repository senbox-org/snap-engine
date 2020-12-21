package org.esa.snap.dem.dataio.copernicus.copernicus30m;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.dem.BaseElevationModel;
import org.esa.snap.core.dataop.dem.ElevationFile;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import java.io.File;

public class Copernicus30mElevationModel extends BaseElevationModel {
    public Copernicus30mElevationModel(ElevationModelDescriptor descriptor, Resampling resamplingMethod) {
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

    @Override
    protected void createElevationFile(ElevationFile[][] elevationFiles, int x, int y, File demInstallDir) {

        final int minLon = x * DEGREE_RES - 180;
        final int minLat = y * DEGREE_RES - 90;

        final String fileName = createTileFilename(minLat, minLon);
        final File localFile = new File(demInstallDir, fileName);
        GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        try {
            elevationFiles[x][NUM_Y_TILES - 1 - y] = new Copernicus30mFile(this, localFile, plugIn.createReaderInstance());
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
        final StringBuilder name = new StringBuilder("Copernicus_DSM_COG_10_");
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
