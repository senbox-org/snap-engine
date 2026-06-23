package org.esa.snap.dem.dataio.copernicus;

import java.io.IOException;


public interface CopernicusTileSource {


    int getWidth();

    int getHeight();

    float[] readRows(int y, int rowCount) throws IOException;

    void close();
}
