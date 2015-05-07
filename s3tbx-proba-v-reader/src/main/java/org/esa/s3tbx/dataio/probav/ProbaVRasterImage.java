package org.esa.s3tbx.dataio.probav;

import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.jai.RasterDataNodeOpImage;
import org.esa.snap.jai.ResolutionLevel;

import java.awt.*;
import java.io.IOException;

/**
 * Proba-V raster data node implementation
 *
 * @author olafd
 */
public class ProbaVRasterImage extends RasterDataNodeOpImage {

    private byte[] byteDataValues = null;
    private short[] shortDataValues = null;
    private float[] floatDataValues = null;

    private String byteNodeName;

    private Band band;

    protected ProbaVRasterImage(Band band, byte[] dataValues, String byteNodeName) {
        super(band, ResolutionLevel.MAXRES);
        initialize(band);
        this.byteDataValues = dataValues;
        this.byteNodeName = byteNodeName;
    }

    protected ProbaVRasterImage(Band band, short[] dataValues) {
        super(band, ResolutionLevel.MAXRES);
        initialize(band);
        this.shortDataValues = dataValues;
    }

    protected ProbaVRasterImage(Band band, float[] dataValues) {
        super(band, ResolutionLevel.MAXRES);
        initialize(band);
        this.floatDataValues = dataValues;
    }


    private void initialize(Band band) {
        width = band.getSceneRasterWidth();
        this.band = band;
    }

    @Override
    protected void computeProductData(ProductData outputData, Rectangle region) throws IOException {

        switch (band.getDataType()) {

            case ProductData.TYPE_UINT8:
                // related to TIME (100m), QUALITY and GEOMETRY variables
                if (ProbaVUtils.isGeometryBand(byteNodeName)) {
                    for (int y = 0; y < region.height; y++) {
                        for (int x = 0; x < region.width; x++) {
                            final int indexInTile = y * region.width + x;
                            final int indexInImage = (region.y + y) * width + (region.x + x);
                            final byte value = byteDataValues[indexInImage];
                            outputData.setElemUIntAt(indexInTile, value < 255 ? value :
                                    ProbaVConstants.GEOMETRY_NO_DATA_VALUE);
                        }
                    }
                } else if (byteNodeName.equals("TIME")) {
                    for (int y = 0; y < region.height; y++) {
                        for (int x = 0; x < region.width; x++) {
                            final int indexInTile = y * region.width + x;
                            final int indexInImage = (region.y + y) * width + (region.x + x);
                            final byte value = byteDataValues[indexInImage];
                            outputData.setElemUIntAt(indexInTile, value > 0 && value < 255 ? value :
                                    ProbaVConstants.TIME_NO_DATA_VALUE_UINT8);
                        }
                    }
                } else if (byteNodeName.equals("QUALITY")) {
                    for (int y = 0; y < region.height; y++) {
                        for (int x = 0; x < region.width; x++) {
                            final int indexInTile = y * region.width + x;
                            final int indexInImage = (region.y + y) * width + (region.x + x);
                            final byte value = byteDataValues[indexInImage];
                            outputData.setElemUIntAt(indexInTile, value);
                        }
                    }
                }
                break;

            case ProductData.TYPE_INT16:
                // related to RADIOMETRY variables
                for (int y = 0; y < region.height; y++) {
                    for (int x = 0; x < region.width; x++) {
                        final int indexInTile = y * region.width + x;
                        final int indexInImage = (region.y + y) * width + (region.x + x);
                        final short value = shortDataValues[indexInImage];
                        outputData.setElemIntAt(indexInTile, value > 0 ? value : ProbaVConstants.RADIOMETRY_NO_DATA_VALUE);
                    }
                }
                break;

            case ProductData.TYPE_UINT16:
                // related to TIME variable
                for (int y = 0; y < region.height; y++) {
                    for (int x = 0; x < region.width; x++) {
                        final int indexInTile = y * region.width + x;
                        final int indexInImage = (region.y + y) * width + (region.x + x);
                        final short value = shortDataValues[indexInImage];
                        outputData.setElemUIntAt(indexInTile, value > 0 ? value : ProbaVConstants.TIME_NO_DATA_VALUE_UINT16);
                    }
                }
                break;

            case ProductData.TYPE_FLOAT32:
                // related to NDVI variable
                for (int y = 0; y < region.height; y++) {
                    for (int x = 0; x < region.width; x++) {
                        final int indexInTile = y * region.width + x;
                        final int indexInImage = (region.y + y) * width + (region.x + x);
                        final float value = floatDataValues[indexInImage];
                        outputData.setElemFloatAt(indexInTile, value < 255 ? value : ProbaVConstants.NDVI_NO_DATA_VALUE);
                    }
                }
                break;
        }

    }
}
