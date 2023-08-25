package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import org.junit.Test;
import ucar.nc2.Attribute;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CfHdfGeoInfoExtractorTest {

    @Test
    public void testExtractGeoInfo() {
        List<Attribute> attributes = new ArrayList<>();

        final String attrString = "projeCTIon=GCTP_SNSOID\n\t" +
                "xdim=1200\n\t\t" +
                "yDiM=2400\n\t\t" +
                "upperleFT=(1.23,45.6)\n\t\t" +
                "LOWERRIGHT=(78.9,10.34)\n\t\t";

        attributes.add(new Attribute("dummy", "bla"));
        attributes.add(new Attribute("StructMetadata.0", attrString));
        attributes.add(new Attribute("bla", "blubb"));

        final CfHdfEosGeoInfoExtractor cfHdfEosGeoInfoExtractor = new CfHdfEosGeoInfoExtractor(attributes);
        cfHdfEosGeoInfoExtractor.extractInfo();
        assertNotNull(cfHdfEosGeoInfoExtractor.getProjection());
        assertEquals("GCTP_SNSOID", cfHdfEosGeoInfoExtractor.getProjection());
        assertEquals(1200, cfHdfEosGeoInfoExtractor.getxDim());
        assertEquals(2400, cfHdfEosGeoInfoExtractor.getyDim());
        assertEquals(1.23, cfHdfEosGeoInfoExtractor.getUlLon(), 1e-8);
        assertEquals(45.6, cfHdfEosGeoInfoExtractor.getUlLat(), 1e-8);
        assertEquals(78.9, cfHdfEosGeoInfoExtractor.getLrLon(), 1e-8);
        assertEquals(10.34, cfHdfEosGeoInfoExtractor.getLrLat(), 1e-8);
    }
}
