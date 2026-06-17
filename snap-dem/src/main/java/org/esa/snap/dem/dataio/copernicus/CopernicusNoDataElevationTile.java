package org.esa.snap.dem.dataio.copernicus;

import org.esa.snap.core.dataop.dem.ElevationTile;

public class CopernicusNoDataElevationTile implements ElevationTile {

    private final float noDataValue;
    private boolean disposed;

    public CopernicusNoDataElevationTile(float noDataValue) {
        this.noDataValue = noDataValue;
    }

    @Override
    public void dispose() {
        disposed = true;
    }

    @Override
    public float getSample(int pixelX, int pixelY) {
        return noDataValue;
    }

    @Override
    public void clearCache() {
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
