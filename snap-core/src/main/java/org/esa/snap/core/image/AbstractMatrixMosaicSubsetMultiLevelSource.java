package org.esa.snap.core.image;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.util.ImageUtils;

import javax.media.jai.SourcelessOpImage;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.util.ArrayList;

/**
 * Created by jcoravu on 12/12/2019.
 */
public abstract class AbstractMatrixMosaicSubsetMultiLevelSource extends AbstractMosaicSubsetMultiLevelSource<MosaicMatrix.MatrixCell> {

    private final MosaicMatrix bandMatrix;

    protected AbstractMatrixMosaicSubsetMultiLevelSource(MosaicMatrix bandMatrix, Rectangle visibleImageBounds, Dimension tileSize, GeoCoding geoCoding) {
        super(visibleImageBounds, tileSize, geoCoding);

        this.bandMatrix = bandMatrix;
    }

    protected abstract SourcelessOpImage buildTileOpImage(Rectangle visibleBounds, int level, Point tileOffset, Dimension tileSize, MosaicMatrix.MatrixCell matrixCell);

    @Override
    protected RenderedImage createImage(int level) {
        java.util.List<RenderedImage> matrixTileImages = new ArrayList<>();
        int cellMatrixOffsetY = 0;
        for (int rowIndex = 0; rowIndex < this.bandMatrix.getRowCount(); rowIndex++) {
            int cellMatrixOffsetX = 0;
            for (int columnIndex = 0; columnIndex < this.bandMatrix.getColumnCount(); columnIndex++) {
                MosaicMatrix.MatrixCell matrixCell = this.bandMatrix.getCellAt(rowIndex, columnIndex);
                Rectangle cellMatrixBounds = new Rectangle(cellMatrixOffsetX, cellMatrixOffsetY, matrixCell.getCellWidth(), matrixCell.getCellHeight());
                Rectangle intersectionMatrixBounds = this.visibleImageBounds.intersection(cellMatrixBounds);
                if (!intersectionMatrixBounds.isEmpty()) {
                    int cellLocalOffsetX = intersectionMatrixBounds.x - cellMatrixBounds.x;
                    int cellLocalOffsetY = intersectionMatrixBounds.y - cellMatrixBounds.y;
                    Rectangle cellLocalIntersectionBounds = new Rectangle(cellLocalOffsetX, cellLocalOffsetY, intersectionMatrixBounds.width, intersectionMatrixBounds.height);

                    float cellTranslateLevelOffsetX = (float) ImageUtils.computeLevelSizeAsDouble(intersectionMatrixBounds.x - this.visibleImageBounds.x, level);
                    float cellTranslateLevelOffsetY = (float) ImageUtils.computeLevelSizeAsDouble(intersectionMatrixBounds.y - this.visibleImageBounds.y, level);

                    java.util.List<RenderedImage> cellTileImages = buildTileImages(level, cellLocalIntersectionBounds, cellTranslateLevelOffsetX, cellTranslateLevelOffsetY, matrixCell);
                    matrixTileImages.addAll(cellTileImages);
                }
                cellMatrixOffsetX += matrixCell.getCellWidth();
            }
            cellMatrixOffsetY += this.bandMatrix.getCellAt(rowIndex, 0).getCellHeight();
        }
        if (matrixTileImages.size() > 0) {
            return buildMosaicOp(level, matrixTileImages);
        }
        return null;
    }
}
