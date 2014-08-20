package org.esa.beam.dataio.s3.synergy;

import org.esa.beam.dataio.netcdf.ProfileReadContext;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.s3.util.S3NetcdfReader;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.jai.ResolutionLevel;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * @author Tonio Fincke
 */
public class VgtReader extends S3NetcdfReader {

    public VgtReader(String pathToFile) throws IOException {
        super(pathToFile);
    }

    protected String getNameOfRowDimension() {
        return "latitude";
    }

    protected String getNameOfColumnDimension() {
        return "longitude";
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
            product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                                  sceneRasterWidth, sceneRasterHeight,
                                                  easting, northing,
                                                  pixelSizeX, pixelSizeY,
                                                  pixelX, pixelY));
        } catch (IOException | TransformException | FactoryException e) {
            e.printStackTrace();
        }
    }

}
