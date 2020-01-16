package org.esa.snap.jp2.reader.internal;

import org.esa.snap.jp2.reader.JP2ImageFile;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.image.AbstractMosaicSubsetMultiLevelSource;
import org.esa.snap.core.image.DecompressedTileOpImageCallback;

import javax.media.jai.SourcelessOpImage;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.nio.file.Path;
import java.util.List;

/**
 * A single banded multi-level image source for JP2 files.
 *
 * @author Cosmin Cara
 */
public class JP2MultiLevelSource extends AbstractMosaicSubsetMultiLevelSource implements DecompressedTileOpImageCallback<Void> {

    private final int dataBufferType;
    private final int bandIndex;
    private final JP2ImageFile jp2ImageFile;
    private final Path localCacheFolder;
    private final Dimension defaultImageSize;
    private final int bandCount;

    public JP2MultiLevelSource(JP2ImageFile jp2ImageFile, Path localCacheFolder, Dimension defaultImageSize, Rectangle imageReadBounds, int bandCount,
                               int bandIndex, Dimension decompresedTileSize, int levelCount, int dataBufferType, GeoCoding geoCoding) {

        super(levelCount, imageReadBounds, decompresedTileSize, geoCoding);

        this.jp2ImageFile = jp2ImageFile;
        this.localCacheFolder = localCacheFolder;
        this.defaultImageSize = defaultImageSize;
        this.dataBufferType = dataBufferType;
        this.bandCount = bandCount;
        this.bandIndex = bandIndex;
    }

    @Override
    protected RenderedImage createImage(int level) {
        List<RenderedImage> tileImages = buildDecompressedTileImages(level, this.imageReadBounds, this.tileSize, this.defaultImageSize.width, 0.0f, 0.0f, this, null);
        if (tileImages.size() > 0) {
            return buildMosaicOp(level, tileImages, true);
        }
        return null;
    }

    @Override
    public SourcelessOpImage buildTileOpImage(Dimension decompresedTileSize, Dimension tileSize, Point tileOffsetFromDecompressedImage,
                                              Point tileOffsetFromImage, int decompressTileIndex, int level, Void tileData) {

        return new JP2TileOpImage(this.jp2ImageFile, this.localCacheFolder, getModel(), decompresedTileSize, this.bandCount, this.bandIndex,
                                        this.dataBufferType, tileSize, tileOffsetFromDecompressedImage, tileOffsetFromImage, decompressTileIndex, level);
    }
}
