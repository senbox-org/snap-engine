package org.esa.snap.core.image;

import javax.media.jai.SourcelessOpImage;
import java.awt.*;

/**
 * Created by jcoravu on 7/1/2020.
 */
public interface DecompressedTileOpImageCallback<TileDataType> {

    public SourcelessOpImage buildTileOpImage(Dimension decompresedTileSize, Dimension tileSize, Point tileOffsetFromDecompressedImage,
                                              Point tileOffsetFromImage, int decompressTileIndex, int level, TileDataType tileData);
}
