package org.esa.snap.core.image;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ImageUtils;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.util.ArrayList;

/**
 * Created by jcoravu on 12/12/2019.
 */
public abstract class AbstractMatrixMosaicSubsetMultiLevelSource extends AbstractMosaicSubsetMultiLevelSource {

    protected final MosaicMatrix mosaicMatrix;

    protected AbstractMatrixMosaicSubsetMultiLevelSource(MosaicMatrix mosaicMatrix, Rectangle imageMatrixReadBounds, Dimension tileSize, GeoCoding geoCoding) {
        this(mosaicMatrix, imageMatrixReadBounds, tileSize, Product.findImageToModelTransform(geoCoding));
    }

    protected AbstractMatrixMosaicSubsetMultiLevelSource(MosaicMatrix mosaicMatrix, Rectangle imageMatrixReadBounds, Dimension tileSize, AffineTransform imageToModelTransform) {
        super(imageMatrixReadBounds, tileSize, imageToModelTransform);

        if (mosaicMatrix == null) {
            throw new NullPointerException("The mosaic matrix is null.");
        }
        if (!mosaicMatrix.isConsistent()) {
            throw new IllegalArgumentException("The matrix has empty cells.");
        }
        this.mosaicMatrix = mosaicMatrix;
    }

    protected AbstractMatrixMosaicSubsetMultiLevelSource(int levelCount, MosaicMatrix mosaicMatrix, Rectangle imageMatrixReadBounds, Dimension tileSize, AffineTransform imageToModelTransform) {
        super(levelCount, imageMatrixReadBounds, tileSize, imageToModelTransform);

        if (mosaicMatrix == null) {
            throw new NullPointerException("The mosaic matrix is null.");
        }
        if (!mosaicMatrix.isConsistent()) {
            throw new IllegalArgumentException("The matrix has empty cells.");
        }
        this.mosaicMatrix = mosaicMatrix;
    }

    protected abstract java.util.List<RenderedImage> buildMatrixCellTileImages(int level, Rectangle imageCellReadBounds, float translateLevelOffsetX,
                                                                               float translateLevelOffsetY, MosaicMatrix.MatrixCell matrixCell);

    @Override
    protected final RenderedImage createImage(int level) {
        int levelTotalImageWidth = computeLevelTotalImageWidth(level);
        int levelTotalImageHeight = computeLevelTotalImageHeight(level);

        int lastRowIndex = this.mosaicMatrix.getRowCount() - 1;
        int lastColumnIndex = this.mosaicMatrix.getColumnCount() - 1;
        java.util.List<RenderedImage> matrixTileImages = new ArrayList<>();
        int cellMatrixOffsetY = 0;
        for (int rowIndex = 0; rowIndex <= lastRowIndex; rowIndex++) {
            int cellMatrixOffsetX = 0;
            for (int columnIndex = 0; columnIndex <= lastColumnIndex; columnIndex++) {
                MosaicMatrix.MatrixCell matrixCell = this.mosaicMatrix.getCellAt(rowIndex, columnIndex);
                Rectangle cellMatrixBounds = new Rectangle(cellMatrixOffsetX, cellMatrixOffsetY, matrixCell.getCellWidth(), matrixCell.getCellHeight());
                Rectangle intersectionMatrixBounds = this.imageReadBounds.intersection(cellMatrixBounds);
                if (!intersectionMatrixBounds.isEmpty()) {
                    int cellLocalOffsetX = intersectionMatrixBounds.x - cellMatrixBounds.x;
                    int cellLocalOffsetY = intersectionMatrixBounds.y - cellMatrixBounds.y;
                    Rectangle cellLocalIntersectionBounds = new Rectangle(cellLocalOffsetX, cellLocalOffsetY, intersectionMatrixBounds.width, intersectionMatrixBounds.height);

                    int levelCellImageWidth = ImageUtils.computeLevelSize(cellLocalIntersectionBounds.width, level);
                    int levelCellImageHeight = ImageUtils.computeLevelSize(cellLocalIntersectionBounds.height, level);

                    float cellTranslateLevelOffsetX = (float) ImageUtils.computeLevelSizeAsDouble(intersectionMatrixBounds.x - this.imageReadBounds.x, level);
                    cellTranslateLevelOffsetX -= computeTranslateDifference(cellTranslateLevelOffsetX, levelCellImageWidth, levelTotalImageWidth, columnIndex, lastColumnIndex, level);

                    float cellTranslateLevelOffsetY = (float) ImageUtils.computeLevelSizeAsDouble(intersectionMatrixBounds.y - this.imageReadBounds.y, level);
                    cellTranslateLevelOffsetY -= computeTranslateDifference(cellTranslateLevelOffsetY, levelCellImageHeight, levelTotalImageHeight, rowIndex, lastRowIndex, level);

                    java.util.List<RenderedImage> cellTileImages = buildMatrixCellTileImages(level, cellLocalIntersectionBounds, cellTranslateLevelOffsetX, cellTranslateLevelOffsetY, matrixCell);
                    matrixTileImages.addAll(cellTileImages);
                }
                cellMatrixOffsetX += matrixCell.getCellWidth();
            }
            cellMatrixOffsetY += this.mosaicMatrix.getCellAt(rowIndex, 0).getCellHeight();
        }
        if (matrixTileImages.size() > 0) {
            return buildMosaicOp(level, matrixTileImages, false);
        }
        return null;
    }
}
