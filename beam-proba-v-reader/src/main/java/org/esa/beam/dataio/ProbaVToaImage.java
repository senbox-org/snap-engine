package org.esa.beam.dataio;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.RasterDataNodeOpImage;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Map;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 11.03.2015
 * Time: 18:08
 *
 * @author olafd
 */
public class ProbaVToaImage extends RasterDataNodeOpImage {

    // todo: maybe rename 'ToaImage' to sth. more general

    private final short[] dataValues;           // todo: check for the data type!
    private final float scaleFactor;

    protected ProbaVToaImage(Band band, short[] dataValues, float scaleFactor) {
        super(band, ResolutionLevel.MAXRES);
        height = band.getSceneRasterHeight();
        width = band.getSceneRasterWidth();
        this.dataValues = dataValues;
        this.scaleFactor = scaleFactor;
    }

    @Override
    protected void computeProductData(ProductData outputData, Rectangle region) throws IOException {
        System.out.println("region = " + region);
        for (int y = 0; y < region.height; y++) {
            for (int x = 0; x < region.width; x++) {
                int k = y * region.width + x;
                int kk = (region.y + y)*width + (region.x + x);
                outputData.setElemFloatAt(k, dataValues[kk]/scaleFactor);
            }
        }
    }
}
