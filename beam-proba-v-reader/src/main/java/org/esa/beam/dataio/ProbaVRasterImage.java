package org.esa.beam.dataio;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.RasterDataNodeOpImage;
import org.esa.beam.jai.ResolutionLevel;

import java.awt.*;
import java.io.IOException;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 11.03.2015
 * Time: 18:08
 *
 * @author olafd
 */
public class ProbaVRasterImage extends RasterDataNodeOpImage {

    private byte[] byteDataValues = null;
    private short[] shortDataValues = null;
    private int[] intDataValues = null;
    private float[] floatDataValues = null;

    private Band band;

    protected ProbaVRasterImage(Band band, byte[] dataValues) {
        super(band, ResolutionLevel.MAXRES);
        initialize(band);
        this.byteDataValues = dataValues;
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
//        System.out.println("region = " + region);

        switch (band.getDataType()) {
            case ProductData.TYPE_INT8:
                for (int y = 0; y < region.height; y++) {
                    for (int x = 0; x < region.width; x++) {
                        final int indexInTile = y * region.width + x;
                        final int indexInImage = (region.y + y) * width + (region.x + x);
                        outputData.setElemIntAt(indexInTile, byteDataValues[indexInImage]);
                    }
                }
                break;

            case ProductData.TYPE_UINT8:
                for (int y = 0; y < region.height; y++) {
                    for (int x = 0; x < region.width; x++) {
                        final int indexInTile = y * region.width + x;
                        final int indexInImage = (region.y + y) * width + (region.x + x);
                        outputData.setElemUIntAt(indexInTile, byteDataValues[indexInImage]);
                    }
                }
                break;

            case ProductData.TYPE_INT16:
            for (int y = 0; y < region.height; y++) {
                for (int x = 0; x < region.width; x++) {
                    final int indexInTile = y * region.width + x;
                    final int indexInImage = (region.y + y) * width + (region.x + x);
                    outputData.setElemIntAt(indexInTile, shortDataValues[indexInImage]);
                }
            }
            break;

            case ProductData.TYPE_UINT16:
                for (int y = 0; y < region.height; y++) {
                    for (int x = 0; x < region.width; x++) {
                        final int indexInTile = y * region.width + x;
                        final int indexInImage = (region.y + y) * width + (region.x + x);
                        outputData.setElemUIntAt(indexInTile, shortDataValues[indexInImage]);
                    }
                }
                break;

            case ProductData.TYPE_FLOAT32:
                for (int y = 0; y < region.height; y++) {
                    for (int x = 0; x < region.width; x++) {
                        final int indexInTile = y * region.width + x;
                        final int indexInImage = (region.y + y) * width + (region.x + x);
                        outputData.setElemFloatAt(indexInTile, floatDataValues[indexInImage]);
                    }
                }
                break;
        }

    }
}
