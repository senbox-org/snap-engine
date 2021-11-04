package org.esa.snap.dem.dataio.copernicus.copernicus30m;

import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.dem.dataio.copernicus.CopernicusElevationModel;

public class Copernicus30mElevationModel extends CopernicusElevationModel {

    public Copernicus30mElevationModel(ElevationModelDescriptor descriptor, Resampling resamplingMethod) {
        super(descriptor, resamplingMethod);
    }

    @Override
    protected int getResolution() {
        return 30;
    }

    @Override
    protected String getTileFileNamePrefix() {
        return "Copernicus_DSM_COG_10_";
    }
}
