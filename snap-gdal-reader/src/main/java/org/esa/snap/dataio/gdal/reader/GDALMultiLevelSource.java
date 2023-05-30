package org.esa.snap.dataio.gdal.reader;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.image.AbstractMosaicSubsetMultiLevelSource;
import org.esa.snap.core.image.ImageReadBoundsSupport;
import org.esa.snap.core.image.UncompressedTileOpImageCallback;
import org.esa.snap.core.util.ImageUtils;

import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.nio.file.Path;

/**
 * A single banded multi-level image source for products imported with the GDAL library.
 *
 * @author Jean Coravu
 * @author Adrian Draghici
 */
public class GDALMultiLevelSource extends AbstractMosaicSubsetMultiLevelSource implements UncompressedTileOpImageCallback<Void>, GDALBandSource {

    private final Path sourceLocalFile;
    private final int dataBufferType;
    private final int bandIndex;
    private final Double noDataValue;
    private final Dimension defaultJAIReadTileSize;

    public GDALMultiLevelSource(Path sourceLocalFile, int dataBufferType, Rectangle imageReadBounds, Dimension tileSize, int bandIndex,
                         int levelCount, GeoCoding geoCoding, Double noDataValue, Dimension defaultJAIReadTileSize) {

        super(levelCount, imageReadBounds, tileSize, geoCoding);

        this.sourceLocalFile = sourceLocalFile;
        this.dataBufferType = dataBufferType;
        this.bandIndex = bandIndex;
        this.noDataValue = noDataValue;
        this.defaultJAIReadTileSize = defaultJAIReadTileSize;
    }

    @Override
    protected ImageLayout buildMosaicImageLayout(int level) {
        return null; // no image layout to configure the mosaic image since the tile images are configured
    }

    @Override
    public PlanarImage buildTileOpImage(ImageReadBoundsSupport imageReadBoundsSupport, int tileWidth, int tileHeight,
                                        int tileOffsetFromReadBoundsX, int tileOffsetFromReadBoundsY, Void tileData) {

        return new GDALTileOpImage(this, this.dataBufferType, tileWidth, tileHeight, tileOffsetFromReadBoundsX, tileOffsetFromReadBoundsY, imageReadBoundsSupport, this.defaultJAIReadTileSize);
    }

    @Override
    protected RenderedImage createImage(int level) {
        java.util.List<RenderedImage> tileImages = buildUncompressedTileImages(level, this.imageReadBounds, this.tileSize.width, this.tileSize.height, 0.0f, 0.0f, this, null);
        if (!tileImages.isEmpty()) {
            return buildMosaicOp(level, tileImages, false);
        }
        return null;
    }

    @Override
    protected double[] getMosaicOpBackgroundValues() {
        if (this.noDataValue == null) {
            return super.getMosaicOpBackgroundValues();
        }
        return new double[]{this.noDataValue};
    }

    @Override
    public Path getSourceLocalFile() {
        return this.sourceLocalFile;
    }

    @Override
    public int getBandIndex() {
        return this.bandIndex;
    }

    public ImageLayout buildMultiLevelImageLayout() {
        int topLeftTileWidth = computeTopLeftUncompressedTileWidth(this.imageReadBounds, this.tileSize.width);
        int topLeftTileHeight = computeTopLeftUncompressedTileHeight(this.imageReadBounds, this.tileSize.height);
        return ImageUtils.buildImageLayout(this.dataBufferType, this.imageReadBounds.width, this.imageReadBounds.height, 0, this.defaultJAIReadTileSize, topLeftTileWidth, topLeftTileHeight);
    }
}
