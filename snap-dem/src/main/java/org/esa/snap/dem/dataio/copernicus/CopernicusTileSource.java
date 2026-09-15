package org.esa.snap.dem.dataio.copernicus;

import java.io.IOException;


public interface CopernicusTileSource {


    int getWidth();

    int getHeight();

    /**
     * Number of consecutive rows this source can deliver without decoding the same bytes twice,
     * i.e. the height of its internal tile or strip. A partial read of a tiled GeoTIFF still
     * decodes every internal tile it touches, so reading in smaller slices than this repeats
     * work. Sources that cannot tell should report the full image height.
     */
    default int getPreferredBlockHeight() {
        return getHeight();
    }

    float[] readRows(int y, int rowCount) throws IOException;

    void close();
}
