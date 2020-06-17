package org.esa.snap.core.image;

import javax.media.jai.SourcelessOpImage;
import java.awt.*;

/**
 * Created by jcoravu on 7/1/2020.
 */
public interface DecompressedTileOpImageCallback<TileDataType> {

    public SourcelessOpImage buildTileOpImage(DecompressedImageSupport decompressedImageSupport, int tileWidth, int tileHeight,
                                              int tileOffsetXFromDecompressedImage, int tileOffsetYFromDecompressedImage,
                                              int tileOffsetXFromImage, int tileOffsetYFromImage, int decompressTileIndex, TileDataType tileData);
}
