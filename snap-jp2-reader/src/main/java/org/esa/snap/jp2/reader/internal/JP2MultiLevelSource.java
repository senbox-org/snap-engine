package org.esa.snap.jp2.reader.internal;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.image.AbstractMosaicSubsetMultiLevelSource;
import org.esa.snap.core.image.DecompressedImageSupport;
import org.esa.snap.core.image.DecompressedTileOpImageCallback;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.jp2.reader.JP2ImageFile;

import javax.media.jai.ImageLayout;
import javax.media.jai.SourcelessOpImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.nio.file.Path;
import java.util.List;

/**
 * A single banded multi-level image source for JP2 files.
 *
 * @author Cosmin Cara
 */
public class JP2MultiLevelSource extends AbstractMosaicSubsetMultiLevelSource implements DecompressedTileOpImageCallback<Void>,
                                                                                         JP2BandSource, JP2BandData {

    private final int dataBufferType;
    private final int bandIndex;
    private final JP2ImageFile jp2ImageFile;
    private final Path localCacheFolder;
    private final Dimension defaultImageSize;
    private final int bandCount;
    private final Dimension defaultJAIReadTileSize;

    public JP2MultiLevelSource(JP2ImageFile jp2ImageFile, Path localCacheFolder, Dimension defaultImageSize, Rectangle imageReadBounds, int bandCount,
                               int bandIndex, Dimension decompressedTileSize, int levelCount, int dataBufferType, GeoCoding geoCoding, Dimension defaultJAIReadTileSize) {

        super(levelCount, imageReadBounds, decompressedTileSize, geoCoding);

        this.jp2ImageFile = jp2ImageFile;
        this.localCacheFolder = localCacheFolder;
        this.defaultImageSize = defaultImageSize;
        this.dataBufferType = dataBufferType;
        this.bandCount = bandCount;
        this.bandIndex = bandIndex;
        this.defaultJAIReadTileSize = defaultJAIReadTileSize;
    }

    @Override
    protected ImageLayout buildMosaicImageLayout(int level) {
        return null; // no image layout to configure the mosaic image since the tile images are configured
    }

    @Override
    protected RenderedImage createImage(int level) {
        DecompressedImageSupport decompressedImageSupport = new DecompressedImageSupport(level, this.tileSize.width, this.tileSize.height);
        List<RenderedImage> tileImages = buildDecompressedTileImages(this.imageReadBounds, decompressedImageSupport, this.defaultImageSize.width, 0.0f, 0.0f, this, null);
        if (tileImages.size() > 0) {
            return buildMosaicOp(level, tileImages, true);
        }
        return null;
    }

    @Override
    public SourcelessOpImage buildTileOpImage(DecompressedImageSupport decompressedImageSupport, int tileWidth, int tileHeight,
                                              int tileOffsetXFromDecompressedImage, int tileOffsetYFromDecompressedImage,
                                              int tileOffsetXFromImage, int tileOffsetYFromImage, int decompressTileIndex, Void tileData) {

        return new JP2TileOpImage(this, this, decompressedImageSupport, tileWidth, tileHeight,
                                  tileOffsetXFromDecompressedImage, tileOffsetYFromDecompressedImage,
                                  tileOffsetXFromImage, tileOffsetYFromImage, decompressTileIndex, this.defaultJAIReadTileSize);
    }

    @Override
    public int getBandIndex() {
        return this.bandIndex;
    }

    @Override
    public int getDataBufferType() {
        return this.dataBufferType;
    }

    @Override
    public JP2ImageFile getJp2ImageFile() {
        return this.jp2ImageFile;
    }

    @Override
    public Path getLocalCacheFolder() {
        return this.localCacheFolder;
    }

    @Override
    public int getBandCount() {
        return this.bandCount;
    }

    public ImageLayout buildMultiLevelImageLayout() {
        int topLeftTileWidth = computeTopLeftDecompressedTileWidth(this.imageReadBounds, this.tileSize.width);
        int topLeftTileHeight = computeTopLeftDecompressedTileHeight(this.imageReadBounds, this.tileSize.height);
        return ImageUtils.buildImageLayout(this.dataBufferType, imageReadBounds.width, imageReadBounds.height, 0, this.defaultJAIReadTileSize, topLeftTileWidth, topLeftTileHeight);
    }
}
