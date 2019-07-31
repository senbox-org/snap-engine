package org.esa.snap.dataio.netcdf.isin;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.grid.isin.IsinAPI;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

@Ignore
public class IsinTileWriteIOTest {

    @Test
    public void testWrite() throws IOException {
        final Product product = IsinUtils.createMpcVegetationPrototype(11, 12, IsinAPI.Raster.GRID_1_KM);

        ProductIO.writeProduct(product, "/tmp/test_isin.nc", "NetCDF4-CF");
    }
}
