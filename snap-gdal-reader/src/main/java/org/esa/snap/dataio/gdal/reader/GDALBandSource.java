package org.esa.snap.dataio.gdal.reader;

import java.nio.file.Path;

public interface GDALBandSource {

    public Path getSourceLocalFile();

    public int getBandIndex();
}
