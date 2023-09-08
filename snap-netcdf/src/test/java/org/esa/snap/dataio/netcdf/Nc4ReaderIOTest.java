package org.esa.snap.dataio.netcdf;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.util.DummyProductBuilder;
import com.bc.ceres.test.LongTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertNotNull;


@RunWith(LongTestRunner.class)
public class Nc4ReaderIOTest {

    @Test
    public void testWithExistingLatLonBandsAndCrsGeoCoding() throws IOException {
        DummyProductBuilder pb = new DummyProductBuilder();
        pb.size(DummyProductBuilder.Size.SMALL);
        pb.gc(DummyProductBuilder.GC.PER_PIXEL);
        pb.gcOcc(DummyProductBuilder.GCOcc.UNIQUE);
        pb.sizeOcc(DummyProductBuilder.SizeOcc.SINGLE);
        Product product = pb.create();
        product.getBand("latitude").setName("lat");
        product.getBand("longitude").setName("lon");

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("crs", "EPSG:4326");
        Product reprojectProduct = GPF.createProduct("Reproject", parameters, product);

        File nc4testFile = File.createTempFile("nc4test", ".nc");
        ProductIO.writeProduct(reprojectProduct, nc4testFile.getAbsolutePath(), "NetCDF4-CF");
        reprojectProduct.dispose();
        product.dispose();

        Product readProduct = ProductIO.readProduct(nc4testFile.getAbsolutePath());
        assertNotNull(readProduct.getSceneGeoCoding().getGeoPos(new PixelPos(5.0, 5.0), null));

    }
}