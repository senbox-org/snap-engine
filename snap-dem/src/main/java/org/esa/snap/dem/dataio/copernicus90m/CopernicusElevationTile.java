package org.esa.snap.dem.dataio.copernicus90m;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.dem.BaseElevationTile;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.resamp.Resampling;

public class CopernicusElevationTile extends BaseElevationTile  {

    public CopernicusElevationTile(ElevationModel demModel, Product product) {
        super(demModel, product);
    }


}
