package org.esa.snap.core.image;

import javax.media.jai.SourcelessOpImage;
import java.awt.*;

/**
 * Created by jcoravu on 8/1/2020.
 */
public interface UncompressedTileOpImageCallback<TileDataType> {

    public SourcelessOpImage buildTileOpImage(Rectangle imageCellReadBounds, int level, Point tileOffsetFromCellReadBounds, Dimension tileSize, TileDataType tileData);
}
