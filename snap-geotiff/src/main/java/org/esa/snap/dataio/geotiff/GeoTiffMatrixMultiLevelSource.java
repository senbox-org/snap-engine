package org.esa.snap.dataio.geotiff;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.image.AbstractMatrixMosaicSubsetMultiLevelSource;
import org.esa.snap.core.image.MosaicMatrix;
import org.esa.snap.core.image.UncompressedTileOpImageCallback;

import javax.media.jai.SourcelessOpImage;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.util.*;

/**
 * Created by jcoravu on 7/1/2020.
 */
public class GeoTiffMatrixMultiLevelSource extends AbstractMatrixMosaicSubsetMultiLevelSource implements UncompressedTileOpImageCallback<MosaicMatrix.MatrixCell> {

    private final int bandIndex;

    public GeoTiffMatrixMultiLevelSource(MosaicMatrix spotBandMatrix, Rectangle imageMatrixReadBounds, Dimension tileSize, int bandIndex, GeoCoding geoCoding) {
        super(spotBandMatrix, imageMatrixReadBounds, tileSize, geoCoding);

        this.bandIndex = bandIndex;
    }

    @Override
    public SourcelessOpImage buildTileOpImage(Rectangle imageCellReadBounds, int level, Point tileOffset, Dimension tileSize, MosaicMatrix.MatrixCell matrixCell) {
        GeoTiffMatrixCell volumeMatrixCell = (GeoTiffMatrixCell)matrixCell;
        return new GeoTiffTileOpImage(volumeMatrixCell.getGeoTiffImageReader(), getModel(), volumeMatrixCell.getDataBufferType(),
                                      this.bandIndex, imageCellReadBounds, tileSize, tileOffset, level, false);
    }

    @Override
    protected java.util.List<RenderedImage> buildMatrixCellTileImages(int level, Rectangle imageCellReadBounds, float cellTranslateLevelOffsetX, float cellTranslateLevelOffsetY,
                                                                      MosaicMatrix.MatrixCell matrixCell) {

        return buildUncompressedTileImages(level, imageCellReadBounds, cellTranslateLevelOffsetX, cellTranslateLevelOffsetY, this, matrixCell);
    }
}
