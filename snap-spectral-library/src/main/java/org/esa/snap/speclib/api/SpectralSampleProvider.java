package org.esa.snap.speclib.api;

import org.esa.snap.core.datamodel.Band;

import java.util.List;


public interface SpectralSampleProvider {


    double[] readSamples(List<Band> bands, int x, int y, int level);
}
