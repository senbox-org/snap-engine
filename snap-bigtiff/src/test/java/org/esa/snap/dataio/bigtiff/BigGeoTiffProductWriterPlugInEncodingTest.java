package org.esa.snap.dataio.bigtiff;

import org.esa.snap.core.dataio.EncodeQualification;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import com.bc.ceres.test.LongTestRunner;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(LongTestRunner.class)
public class BigGeoTiffProductWriterPlugInEncodingTest {

    @Test
    public void testEncodingQualification() throws Exception {
        BigGeoTiffProductWriterPlugIn plugIn = new BigGeoTiffProductWriterPlugIn();
        Product product = new Product("N", "T", 2, 2);

        EncodeQualification encodeQualification = plugIn.getEncodeQualification(product);
        assertNotNull(encodeQualification);
        assertEquals(EncodeQualification.Preservation.PARTIAL, encodeQualification.getPreservation());
        assertNotNull(encodeQualification.getInfoString());

        TiePointGrid lat = new TiePointGrid("lat", 2, 2, 0, 0, 1, 1, new float[4]);
        TiePointGrid lon = new TiePointGrid("lon", 2, 2, 0, 0, 1, 1, new float[4]);
        product.addTiePointGrid(lat);
        product.addTiePointGrid(lon);
        product.setSceneGeoCoding(new TiePointGeoCoding(lat, lon));
        encodeQualification = plugIn.getEncodeQualification(product);
        assertEquals(EncodeQualification.Preservation.PARTIAL, encodeQualification.getPreservation());
        assertNotNull(encodeQualification.getInfoString());

        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 2, 2, 0, 0, 1, 1));
        encodeQualification = plugIn.getEncodeQualification(product);
        assertEquals(EncodeQualification.Preservation.FULL, encodeQualification.getPreservation());
    }

}
