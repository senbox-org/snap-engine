package org.esa.snap.dem.dataio.copernicus;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.dem.BaseElevationTile;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.resamp.Resampling;


public class CopernicusElevationTile extends BaseElevationTile  implements Resampling.Raster {

    public CopernicusElevationTile(ElevationModel demModel, Product product) {
        super(demModel, product);
    }
    @Override
    public int getWidth() {
        System.out.println(product != null ? product.getSceneRasterWidth() : 0);
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


}
