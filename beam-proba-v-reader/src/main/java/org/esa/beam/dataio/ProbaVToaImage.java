package org.esa.beam.dataio;

import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.image.WritableRaster;
import java.util.Map;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 11.03.2015
 * Time: 18:08
 *
 * @author olafd
 */
public class ProbaVToaImage extends SingleBandedOpImage {

    // todo: implement
    // (see e.g. ScapeMGapFilledImage in beam-scape-m processor)

    private final int tileHeight;
    private final int tileWidth;
    private final short[][] rasterValues;


    protected ProbaVToaImage(int dataBufferType, int sourceWidth, int sourceHeight,
                             Dimension tileSize, Map configuration, ResolutionLevel level,
                             short[][] rasterValues) {
        super(dataBufferType, sourceWidth, sourceHeight, tileSize, configuration, level);
        tileHeight = tileSize.height;
        tileWidth = tileSize.width;
        this.rasterValues = rasterValues;
    }

    @Override
    protected void computeRect(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
        float[] elems = new float[destRect.width * destRect.height];
        int index = 0;
        for (int y = destRect.y; y < destRect.height + destRect.y; y++) {
            int yCellIndex = y / tileHeight;
            for (int x = destRect.x; x < destRect.width + destRect.x; x++) {
                int xCellIndex = x / tileWidth;
                short value = rasterValues[xCellIndex][yCellIndex];
                elems[index++] = value;
            }
        }
        dest.setDataElements(destRect.x, destRect.y, destRect.width, destRect.height, elems);

    }
}
