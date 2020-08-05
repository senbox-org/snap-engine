package org.esa.snap.dataio.geotiff;

import java.awt.*;

public interface GeoTiffBandSource {

    public int getBandIndex();

    public boolean isGlobalShifted180();

    public Dimension getDefaultJAIReadTileSize();

    public boolean canDivideTileRegionToRead(int level);
}
