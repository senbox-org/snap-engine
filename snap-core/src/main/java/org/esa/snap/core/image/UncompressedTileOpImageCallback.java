package org.esa.snap.core.image;

import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.*;

/**
 * Created by jcoravu on 8/1/2020.
 */
public interface UncompressedTileOpImageCallback<TileDataType> {

    public PlanarImage buildTileOpImage(ImageReadBoundsSupport imageReadBoundsSupport, int tileWidth, int tileHeight,
                                        int tileOffsetFromReadBoundsX, int tileOffsetFromReadBoundsY, TileDataType tileData);
}
