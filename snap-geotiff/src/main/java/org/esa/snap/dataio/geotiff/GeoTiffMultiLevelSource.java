package org.esa.snap.dataio.geotiff;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.image.AbstractMosaicSubsetMultiLevelSource;
import org.esa.snap.core.image.UncompressedTileOpImageCallback;

import javax.media.jai.SourcelessOpImage;
import java.awt.*;
import java.awt.image.RenderedImage;

/**
 * Created by jcoravu on 22/11/2019.
 */
public class GeoTiffMultiLevelSource extends AbstractMosaicSubsetMultiLevelSource implements UncompressedTileOpImageCallback<Void> {

    private final GeoTiffImageReader geoTiffImageReader;
    private final boolean isGlobalShifted180;
    private final int dataBufferType;
    private final int bandIndex;
    private final Double noDataValue;

    public GeoTiffMultiLevelSource(GeoTiffImageReader geoTiffImageReader, int dataBufferType, Rectangle visibleImageBounds, Dimension tileSize,
                                   int bandIndex, GeoCoding geoCoding, boolean isGlobalShifted180, Double noDataValue) {

        super(visibleImageBounds, tileSize, geoCoding);

        this.geoTiffImageReader = geoTiffImageReader;
        this.isGlobalShifted180 = isGlobalShifted180;
        this.dataBufferType = dataBufferType;
        this.bandIndex = bandIndex;
        this.noDataValue = noDataValue;
    }

    @Override
    public SourcelessOpImage buildTileOpImage(Rectangle imageCellReadBounds, int level, Point tileOffset, Dimension tileSize, Void tileData) {
        return new GeoTiffTileOpImage(this.geoTiffImageReader, getModel(), this.dataBufferType, this.bandIndex, imageCellReadBounds, tileSize, tileOffset, level, this.isGlobalShifted180);
    }

    @Override
    protected RenderedImage createImage(int level) {
        java.util.List<RenderedImage> tileImages = buildUncompressedTileImages(level, this.imageReadBounds, this.tileSize, 0.0f, 0.0f, this, null);
        if (tileImages.size() > 0) {
            return buildMosaicOp(level, tileImages, true);
        }
        return null;
    }

    @Override
    protected double[] getMosaicOpBackgroundValues() {
        if (this.noDataValue == null) {
            return super.getMosaicOpBackgroundValues();
        }
        return new double[] { this.noDataValue.doubleValue() };
    }

    public boolean isGlobalShifted180() {
        return isGlobalShifted180;
    }

    public int getBandIndex() {
        return bandIndex;
    }
}
