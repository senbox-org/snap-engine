package org.esa.snap.dataio.geotiff;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.image.AbstractMosaicSubsetMultiLevelSource;
import org.esa.snap.core.image.ImageReadBoundsSupport;
import org.esa.snap.core.image.UncompressedTileOpImageCallback;
import org.esa.snap.core.util.ImageUtils;

import javax.media.jai.ImageLayout;
import javax.media.jai.SourcelessOpImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.util.ArrayList;

/**
 * Created by jcoravu on 22/11/2019.
 */
public class GeoTiffMultiLevelSource extends AbstractMosaicSubsetMultiLevelSource implements UncompressedTileOpImageCallback<Void>, GeoTiffBandSource {

    private final GeoTiffImageReader geoTiffImageReader;
    private final boolean isGlobalShifted180;
    private final int dataBufferType;
    private final int bandIndex;
    private final Double noDataValue;
    private final Dimension defaultJAIReadTileSize;

    public GeoTiffMultiLevelSource(GeoTiffImageReader geoTiffImageReader, int dataBufferType, Rectangle imageReadBounds, Dimension mosaicImageTileSize,
                                   int bandIndex, GeoCoding geoCoding, boolean isGlobalShifted180, Double noDataValue, Dimension defaultJAITileSize) {

        super(imageReadBounds, mosaicImageTileSize, geoCoding);

        this.geoTiffImageReader = geoTiffImageReader;
        this.isGlobalShifted180 = isGlobalShifted180;
        this.dataBufferType = dataBufferType;
        this.bandIndex = bandIndex;
        this.noDataValue = noDataValue;
        this.defaultJAIReadTileSize = defaultJAITileSize;
    }

    @Override
    public SourcelessOpImage buildTileOpImage(ImageReadBoundsSupport imageReadBoundsSupport, int tileWidth, int tileHeight,
                                              int tileOffsetFromReadBoundsX, int tileOffsetFromReadBoundsY, Void tileData) {

        return new GeoTiffTileOpImage(this.geoTiffImageReader, this, this.dataBufferType, tileWidth, tileHeight,
                                      tileOffsetFromReadBoundsX, tileOffsetFromReadBoundsY, imageReadBoundsSupport);
    }

    @Override
    protected RenderedImage createImage(int level) {
        java.util.List<RenderedImage> tileImages;
        if (this.tileSize.width == this.imageReadBounds.width && this.tileSize.height == this.imageReadBounds.height) {
            ImageReadBoundsSupport imageReadBoundsSupport = new ImageReadBoundsSupport(this.imageReadBounds, level, getModel().getScale(level));
            SourcelessOpImage tileOpImage = buildTileOpImage(imageReadBoundsSupport, this.tileSize.width, this.tileSize.height, 0, 0, null);
            this.tileImageDisposer.registerForDisposal(tileOpImage);
            tileImages = new ArrayList<>(1);
            tileImages.add(tileOpImage);
        } else {
            tileImages = buildUncompressedTileImages(level, this.imageReadBounds, this.tileSize.width, this.tileSize.height, 0.0f, 0.0f, this, null);
        }
        if (tileImages.size() > 0) {
            return buildMosaicOp(level, tileImages, true);
        }
        return null;
    }

    @Override
    protected ImageLayout buildMosaicImageLayout(int level) {
        return null; // no image layout to configure the mosaic image since the tile images are configured
    }

    @Override
    protected double[] getMosaicOpBackgroundValues() {
        if (this.noDataValue == null) {
            return super.getMosaicOpBackgroundValues();
        }
        return new double[] { this.noDataValue.doubleValue() };
    }

    @Override
    public boolean isGlobalShifted180() {
        return this.isGlobalShifted180;
    }

    @Override
    public int getBandIndex() {
        return this.bandIndex;
    }

    @Override
    public Dimension getDefaultJAIReadTileSize() {
        return defaultJAIReadTileSize;
    }

    @Override
    public boolean canDivideTileRegionToRead(int level) {
        return (level > 6);
    }

    public ImageLayout buildMultiLevelImageLayout() {
        int topLeftTileWidth = computeTopLeftUncompressedTileWidth(this.imageReadBounds, this.tileSize.width);
        int topLeftTileHeight = computeTopLeftUncompressedTileHeight(this.imageReadBounds, this.tileSize.height);
        return ImageUtils.buildImageLayout(this.dataBufferType, this.imageReadBounds.width, this.imageReadBounds.height, 0, this.defaultJAIReadTileSize, topLeftTileWidth, topLeftTileHeight);
    }
}
