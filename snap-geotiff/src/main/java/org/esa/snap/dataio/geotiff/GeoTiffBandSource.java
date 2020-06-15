package org.esa.snap.dataio.geotiff;

public interface GeoTiffBandSource {

    public int getBandIndex();

    public boolean isGlobalShifted180();
}
