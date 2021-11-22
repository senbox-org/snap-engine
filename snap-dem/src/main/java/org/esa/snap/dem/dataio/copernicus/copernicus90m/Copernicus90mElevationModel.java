package org.esa.snap.dem.dataio.copernicus.copernicus90m;

import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataop.dem.ElevationFile;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.dem.dataio.copernicus.CopernicusElevationModel;

import java.io.File;

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

    @Override
    protected ElevationFile createElevationFile(final CopernicusElevationModel model, final File localFile, final ProductReader reader) {
        return new Copernicus90mFile(model, localFile, reader);
    }
}
