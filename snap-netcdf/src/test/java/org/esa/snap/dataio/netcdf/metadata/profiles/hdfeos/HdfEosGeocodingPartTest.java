package org.esa.snap.dataio.netcdf.metadata.profiles.hdfeos;

import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;

public class HdfEosGeocodingPartTest {

    @Test
    public void testAttachGeoCoding() {
        Product product = new Product("dummy", "type", 4800, 4800);
        double[] pp = {6371007.181, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        HdfEosGeocodingPart.attachGeoCoding(product, -3335851.559, 6671703.118, -2223901.039333, 5559752.598333, "GCTP_SNSOID", pp);
        assertNotNull(product.getSceneGeoCoding());
    }

}
