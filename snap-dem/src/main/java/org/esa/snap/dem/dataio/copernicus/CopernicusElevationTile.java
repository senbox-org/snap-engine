package org.esa.snap.dem.dataio.copernicus;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.dem.BaseElevationTile;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.dem.dataio.EarthGravitationalModel96;
import org.esa.snap.engine_utilities.gpf.TileGeoreferencing;

import java.io.IOException;


public class CopernicusElevationTile extends BaseElevationTile  implements Resampling.Raster {

    private final EarthGravitationalModel96 egm;

    public CopernicusElevationTile(ElevationModel demModel, Product product) throws IOException  {
        super(demModel, product);
        egm = EarthGravitationalModel96.instance();
    }

    @Override
    public int getWidth() {
        return product != null ? product.getSceneRasterWidth() : 0;
    }

    @Override
    public int getHeight() {
        return product != null ? product.getSceneRasterHeight() : 0;
    }

    @Override
    public boolean getSamples(int[] x, int[] y, double[][] samples) throws Exception {
        boolean allValid = true;
        for (int i = 0; i < y.length; i++) {
            for (int j = 0; j < x.length; j++) {
                samples[i][j] = getSample(x[i], y[j]);
                if (noDataValue == samples[i][j]) {
                    samples[i][j] = Double.NaN;
                    allValid = false;
                }
            }
        }
        return allValid;
    }

    protected void addGravitationalModel(final int index, final float[] line) throws Exception {
        final GeoPos geoPos = new GeoPos();
        final TileGeoreferencing tileGeoRef = new TileGeoreferencing(product, 0, index, line.length, 1);
        final double[][] v = new double[4][4];
        for (int i = 0; i < line.length; i++) {
            if (line[i] != noDataValue) {
                tileGeoRef.getGeoPos(i, index, geoPos);
                line[i] += egm.getEGM(geoPos.lat, geoPos.lon, v);
            }
        }
    }
    public static int determineWidth(final GeoPos geoPos, int resolution){
        int abs_lat = (int) Math.abs(geoPos.lat);
        int multiplier = 1;
        //Resolution will be either 30 (30m tiles) or 90 (90m tiles).
        if(resolution == 30){
            multiplier = 3;
        }
        if(abs_lat < 50){
            return 1200 * multiplier;
        }else if (abs_lat >= 50  && abs_lat < 60 ){
            return 800 * multiplier;
        }else if (abs_lat >= 60 && abs_lat < 70){
            return 600 * multiplier;
        }else if(abs_lat >= 70 && abs_lat < 75){
            return 400* multiplier;
        }else if(abs_lat >= 75 && abs_lat < 80){
            return 400* multiplier;
        }else if (abs_lat >= 80 && abs_lat < 85 ){
            return 240* multiplier;
        }else{
            return 120* multiplier;
        }
    }

    public static int determineWidth(final PixelPos pixelPos, int resolution ){
        GeoPos geoPos = new GeoPos(pixelPos.x, pixelPos.y);
        return determineWidth(geoPos, resolution);
    }

    public GeoCoding getTileGeocoding() {
        return product != null ? product.getSceneGeoCoding() : null;
    }
}
