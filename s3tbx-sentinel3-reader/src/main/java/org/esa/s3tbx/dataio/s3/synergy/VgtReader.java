package org.esa.s3tbx.dataio.s3.synergy;

import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.Variable;

import java.io.IOException;

/**
 * @author Tonio Fincke
 */
class VgtReader extends S3NetcdfReader {

    protected String[][] getRowColumnNamePairs() {
        return new String[][]{{"latitude", "longitude"}};
    }

    @Override
    protected void addGeoCoding(Product product) {
        double pixelX;
        double pixelY;
        double easting;
        double northing;
        double pixelSizeX;
        double pixelSizeY;
        try {
            final Variable longitudeVariable = getNetcdfFile().findVariable("longitude");
            final Array lonData = longitudeVariable.read();
            final Variable latitudeVariable = getNetcdfFile().findVariable("latitude");
            final Array latData = latitudeVariable.read();

            final int lonSize = longitudeVariable.getShape(0);
            final Index i0 = lonData.getIndex().set(0);
            final Index i1 = lonData.getIndex().set(lonSize - 1);
            int sceneRasterWidth = product.getSceneRasterWidth();
            pixelSizeX = (lonData.getDouble(i1) - lonData.getDouble(i0)) / (sceneRasterWidth - 1);
            easting = lonData.getDouble(i0);

            final int latSize = latitudeVariable.getShape(0);
            final Index j0 = latData.getIndex().set(0);
            final Index j1 = latData.getIndex().set(latSize - 1);
            int sceneRasterHeight = product.getSceneRasterHeight();
            pixelSizeY = (latData.getDouble(j1) - latData.getDouble(j0)) / (sceneRasterHeight - 1);

            pixelX = 0.5f;
            pixelY = 0.5f;

            pixelSizeY = -pixelSizeY;
            northing = latData.getDouble(latData.getIndex().set(0));

            if (pixelSizeX <= 0 || pixelSizeY <= 0) {
                return;
            }
            product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                                       sceneRasterWidth, sceneRasterHeight,
                                                       easting, northing,
                                                       pixelSizeX, pixelSizeY,
                                                       pixelX, pixelY));
        } catch (IOException | TransformException | FactoryException e) {
            e.printStackTrace();
        }
    }

}
