package org.esa.snap.dataio.gdal.reader;

import java.nio.file.Path;

public interface GDALBandSource {

    Path[] getSourceLocalFiles();

    int getBandIndex();
}
