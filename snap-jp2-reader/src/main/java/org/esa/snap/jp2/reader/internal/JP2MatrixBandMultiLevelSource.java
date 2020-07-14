package org.esa.snap.jp2.reader.internal;

import org.esa.snap.core.image.AbstractMatrixMosaicSubsetMultiLevelSource;
import org.esa.snap.core.image.DecompressedImageSupport;
import org.esa.snap.core.image.DecompressedTileOpImageCallback;
import org.esa.snap.core.image.MosaicMatrix;
import org.esa.snap.core.util.ImageUtils;

import javax.media.jai.ImageLayout;
import javax.media.jai.SourcelessOpImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

/**
 * Created by jcoravu on 9/4/2020.
 */
public class JP2MatrixBandMultiLevelSource extends AbstractMatrixMosaicSubsetMultiLevelSource implements DecompressedTileOpImageCallback<JP2MosaicBandMatrixCell>, JP2BandSource {

    private final int bandIndex;
    private final Double mosaicOpSourceThreshold;
    private final Double mosaicOpBackgroundValue;
    private final Dimension defaultJAIReadTileSize;

    public JP2MatrixBandMultiLevelSource(int levelCount, MosaicMatrix mosaicMatrix, Rectangle imageMatrixReadBounds, AffineTransform imageToModelTransform,
                                         int bandIndex, Double mosaicOpBackgroundValue, Double mosaicOpSourceThreshold, Dimension defaultJAIReadTileSize) {

        super(levelCount, mosaicMatrix, imageMatrixReadBounds, new Dimension(1, 1), imageToModelTransform);

        this.bandIndex = bandIndex;
        this.mosaicOpBackgroundValue = mosaicOpBackgroundValue;
        this.mosaicOpSourceThreshold = mosaicOpSourceThreshold;
        this.defaultJAIReadTileSize = defaultJAIReadTileSize;
    }

    @Override
    protected ImageLayout buildMosaicImageLayout(int level) {
        return null; // no image layout to configure the mosaic image since the tile images are configured
    }

    @Override
    public SourcelessOpImage buildTileOpImage(DecompressedImageSupport decompressedImageSupport, int tileWidth, int tileHeight,
                                              int tileOffsetXFromDecompressedImage, int tileOffsetYFromDecompressedImage,
                                              int tileOffsetXFromImage, int tileOffsetYFromImage, int decompressTileIndex, JP2MosaicBandMatrixCell matrixCell) {

        return new JP2TileOpImage(this, matrixCell, decompressedImageSupport, tileWidth, tileHeight,
                                  tileOffsetXFromDecompressedImage, tileOffsetYFromDecompressedImage,
                                  tileOffsetXFromImage, tileOffsetYFromImage, decompressTileIndex, defaultJAIReadTileSize);
    }

    @Override
    protected java.util.List<RenderedImage> buildMatrixCellTileImages(int level, Rectangle imageCellReadBounds, float cellTranslateLevelOffsetX, float cellTranslateLevelOffsetY,
                                                                      MosaicMatrix.MatrixCell matrixCell) {

        JP2MosaicBandMatrixCell mosaicMatrixCell = (JP2MosaicBandMatrixCell)matrixCell;
        DecompressedImageSupport decompressedImageSupport = new DecompressedImageSupport(level, mosaicMatrixCell.getDecompressedTileWidth(), mosaicMatrixCell.getDecompressedTileHeight());
        return buildDecompressedTileImages(imageCellReadBounds, decompressedImageSupport, mosaicMatrixCell.getDefaultImageWidth(),
                                           cellTranslateLevelOffsetX, cellTranslateLevelOffsetY, this, mosaicMatrixCell);
    }

    @Override
    protected double[] getMosaicOpBackgroundValues() {
        if (this.mosaicOpBackgroundValue == null) {
            return super.getMosaicOpBackgroundValues();
        }
        return new double[] { this.mosaicOpBackgroundValue.doubleValue() };
    }

    @Override
    protected double[][] getMosaicOpSourceThreshold() {
        if (this.mosaicOpSourceThreshold == null) {
            return super.getMosaicOpSourceThreshold();
        }
        return new double[][]{ {this.mosaicOpSourceThreshold.doubleValue()} };
    }

    @Override
    public int getBandIndex() {
        return this.bandIndex;
    }

    public ImageLayout buildMultiLevelImageLayout() {
        MatrixReadBounds matrixReadBounds = computeTopLeftMatrixCellReadBounds();
        JP2MosaicBandMatrixCell topLeftMosaicMatrixCell = (JP2MosaicBandMatrixCell)matrixReadBounds.getMatrixCell();
        Rectangle cellLocalIntersectionBounds = matrixReadBounds.getCellLocalIntersectionBounds();
        int topLeftTileWidth = computeTopLeftDecompressedTileWidth(cellLocalIntersectionBounds, topLeftMosaicMatrixCell.getDecompressedTileWidth());
        int topLeftTileHeight = computeTopLeftDecompressedTileHeight(cellLocalIntersectionBounds, topLeftMosaicMatrixCell.getDecompressedTileHeight());
        return ImageUtils.buildImageLayout(topLeftMosaicMatrixCell.getDataBufferType(), this.imageReadBounds.width, this.imageReadBounds.height,
                                           0, this.defaultJAIReadTileSize, topLeftTileWidth, topLeftTileHeight);
    }
}
