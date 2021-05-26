package org.esa.snap.dem.dataio.copernicus;

import org.esa.snap.core.datamodel.GeoPos;
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
}
