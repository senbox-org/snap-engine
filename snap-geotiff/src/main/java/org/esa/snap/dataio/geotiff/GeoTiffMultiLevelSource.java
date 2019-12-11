package org.esa.snap.dataio.geotiff;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.image.AbstractMosaicSubsetMultiLevelSource;

import javax.media.jai.SourcelessOpImage;
import java.awt.*;

/**
 * Created by jcoravu on 22/11/2019.
 */
public class GeoTiffMultiLevelSource extends AbstractMosaicSubsetMultiLevelSource {

    private final GeoTiffImageReader geoTiffImageReader;
    private final boolean isGlobalShifted180;

    public GeoTiffMultiLevelSource(GeoTiffImageReader geoTiffImageReader, int dataBufferType, Rectangle imageBounds, Dimension tileSize,
                                   int bandIndex, GeoCoding geoCoding, boolean isGlobalShifted180) {

        super(dataBufferType, imageBounds, tileSize, bandIndex, geoCoding);

        this.geoTiffImageReader = geoTiffImageReader;
        this.isGlobalShifted180 = isGlobalShifted180;
    }

    @Override
    protected SourcelessOpImage buildTileOpImage(int level, Dimension currentTileSize, Point tileOffset) {
        return new GeoTiffTileOpImage(this.geoTiffImageReader, getModel(), this.dataBufferType, this.bandIndex, this.imageBounds, currentTileSize, tileOffset, level, this.isGlobalShifted180);
    }

    public boolean isGlobalShifted180() {
        return isGlobalShifted180;
    }
}
