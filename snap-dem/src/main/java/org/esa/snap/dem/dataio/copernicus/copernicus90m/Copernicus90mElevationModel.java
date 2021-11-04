package org.esa.snap.dem.dataio.copernicus.copernicus90m;

import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.dem.dataio.copernicus.CopernicusElevationModel;

public class Copernicus90mElevationModel extends CopernicusElevationModel {

    public Copernicus90mElevationModel(ElevationModelDescriptor descriptor, Resampling resamplingMethod) {
        super(descriptor, resamplingMethod);
    }

    @Override
    protected int getResolution() {
        return 90;
    }

    @Override
    protected String getTileFileNamePrefix() {
        return "Copernicus_DSM_COG_30_";
    }
}
