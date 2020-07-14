package org.esa.snap.jp2.reader.internal;

import org.esa.snap.jp2.reader.JP2ImageFile;
import org.esa.snap.lib.openjpeg.jp2.TileLayout;

import java.nio.file.Path;

/**
 * Created by jcoravu on 9/4/2020.
 */
public class JP2MosaicBandMatrixCell implements BandMatrixCell, JP2BandData {

    private final JP2ImageFile jp2ImageFile;
    private final Path localCacheFolder;
    private final TileLayout tileLayout;
    private final int cellWidth;
    private final int cellHeight;

    public JP2MosaicBandMatrixCell(JP2ImageFile jp2ImageFile, Path localCacheFolder, TileLayout tileLayout, int cellWidth, int cellHeight) {
        this.jp2ImageFile = jp2ImageFile;
        this.localCacheFolder = localCacheFolder;
        this.tileLayout = tileLayout;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }

    @Override
    public int getCellWidth() {
        return this.cellWidth;
    }

    @Override
    public int getCellHeight() {
        return this.cellHeight;
    }

    @Override
    public int getDataBufferType() {
        return this.tileLayout.dataType;
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
        return this.tileLayout.numBands;
    }

    public int getResolutionCount() {
        return tileLayout.numResolutions;
    }

    public int getDecompressedTileWidth() {
        return this.tileLayout.tileWidth;
    }

    public int getDecompressedTileHeight() {
        return this.tileLayout.tileHeight;
    }

    public int getDefaultImageWidth() {
        return getCellWidth();
    }
}
