package org.esa.snap.speclib.api;

import org.esa.snap.core.datamodel.Band;


public interface SpectralSampleProvider {


    double readSample(Band band, int x, int y, int level);
    double noDataValue(Band band);
    boolean isPixelValid(Band band, int x, int y, int level);
}
