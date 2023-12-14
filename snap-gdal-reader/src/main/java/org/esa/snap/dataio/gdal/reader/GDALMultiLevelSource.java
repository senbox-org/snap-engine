package org.esa.snap.dataio.gdal.reader;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.image.AbstractMosaicSubsetMultiLevelSource;
import org.esa.snap.core.image.ImageReadBoundsSupport;
import org.esa.snap.core.image.UncompressedTileOpImageCallback;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.dataio.gdal.drivers.Dataset;
import org.esa.snap.dataio.gdal.drivers.GDAL;
import org.esa.snap.dataio.gdal.drivers.GDALConst;

import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * A single banded multi-level image source for products imported with the GDAL library.
 *
 * @author Jean Coravu
 * @author Adrian Draghici
 */
public class GDALMultiLevelSource extends AbstractMosaicSubsetMultiLevelSource implements UncompressedTileOpImageCallback<Void>, GDALBandSource {

    private final Path[] sourceLocalFiles;
    private final int dataBufferType;
    private final int bandIndex;
    private final Double noDataValue;
    private final Dimension defaultJAIReadTileSize;

    private static Dimension readTileSize(Path fromFile, int bandIndex) {
        try (Dataset dataset = GDAL.open(fromFile.toString(), GDALConst.gaReadonly())) {
            if (dataset == null) {
                throw new IOException("Cannot open " + fromFile);
            }
            try (org.esa.snap.dataio.gdal.drivers.Band gdalBand = dataset.getRasterBand(bandIndex)) {
                return new Dimension(gdalBand.getBlockXSize(), gdalBand.getBlockYSize());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    /**
     * Constructor to be used when the internal tile size is handled (i.e., read) by GDAL
     */
    public GDALMultiLevelSource(int dataBufferType, Rectangle imageReadBounds, int bandIndex,
                                int levelCount, GeoCoding geoCoding, Double noDataValue, Path... sourceLocalFiles) {

        super(levelCount, imageReadBounds, readTileSize(sourceLocalFiles[0], bandIndex + 1), geoCoding);
        this.sourceLocalFiles = sourceLocalFiles;
        this.dataBufferType = dataBufferType;
        this.bandIndex = bandIndex;
        this.noDataValue = noDataValue;
        this.defaultJAIReadTileSize = this.tileSize;
    }

    /**
     * Constructor to be used when the internal tile size to be used is passed from outside.
     */
    public GDALMultiLevelSource(int dataBufferType, Rectangle imageReadBounds, Dimension tileSize, int bandIndex,
                         int levelCount, GeoCoding geoCoding, Double noDataValue, Dimension defaultJAIReadTileSize, Path... sourceLocalFiles) {

        super(levelCount, imageReadBounds, tileSize, geoCoding);

        this.sourceLocalFiles = sourceLocalFiles;
        this.dataBufferType = dataBufferType;
        this.bandIndex = bandIndex;
        this.noDataValue = noDataValue;
        this.defaultJAIReadTileSize = defaultJAIReadTileSize;
    }

    @Override
    protected ImageLayout buildMosaicImageLayout(int level) {
        return ImageUtils.buildImageLayout(this.dataBufferType, this.imageReadBounds.width, this.imageReadBounds.height, level, this.defaultJAIReadTileSize);
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
    public Path[] getSourceLocalFiles() {
        return this.sourceLocalFiles;
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
