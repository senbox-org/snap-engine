package org.esa.snap.dem.dataio.copernicus.copernicus30m;

import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataop.dem.ElevationFile;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.dem.dataio.copernicus.CopernicusElevationModel;

import java.io.File;

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

    @Override
    protected ElevationFile createElevationFile(final CopernicusElevationModel model, final File localFile, final ProductReader reader) {
        return new Copernicus30mFile(model, localFile, reader);
    }
}
