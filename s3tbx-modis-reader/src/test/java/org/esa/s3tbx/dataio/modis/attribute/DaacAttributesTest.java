package org.esa.s3tbx.dataio.modis.attribute;

import org.esa.s3tbx.dataio.modis.ModisGlobalAttributes;
import org.esa.s3tbx.dataio.modis.netcdf.NetCDFVariables;
import org.junit.Test;
import ucar.nc2.Dimension;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DaacAttributesTest {

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInheritance() {
        final DaacAttributes daacAttributes = new DaacAttributes(new NetCDFVariables());

        assertTrue(daacAttributes instanceof ModisGlobalAttributes);
    }

    @Test
    public void testIsImappFormat() {
        final DaacAttributes daacAttributes = new DaacAttributes(new NetCDFVariables());

        assertFalse(daacAttributes.isImappFormat());
    }

    @Test
    public void testIsWidthDimension() {
        Dimension dimension = new Dimension("Max_EV_frames", 34);
        assertTrue(DaacAttributes.isWidthDimension(dimension));

        dimension = new Dimension("MODIS_Grid_16DAY_1km_VI/Data Fields/XDim", 34);
        assertTrue(DaacAttributes.isWidthDimension(dimension));

        dimension = new Dimension("Swath/Data Fields/Number_of_samples_per_record", 34);
        assertTrue(DaacAttributes.isWidthDimension(dimension));

        dimension = new Dimension("ist_quatsch", 34);
        assertFalse(DaacAttributes.isWidthDimension(dimension));
    }

    @Test
    public void testIsHeightDimension() {
        Dimension dimension = new Dimension("10*nscans", 838);
        assertTrue(DaacAttributes.isHeightDimension(dimension));

        dimension = new Dimension("20*nscans", 838);
        assertTrue(DaacAttributes.isHeightDimension(dimension));

        dimension = new Dimension("MODIS_Grid_16DAY_1km_VI/Data Fields/YDim", 838);
        assertTrue(DaacAttributes.isHeightDimension(dimension));

        dimension = new Dimension("Swath/Data Fields/Number_of_records", 838);
        assertTrue(DaacAttributes.isHeightDimension(dimension));

        dimension = new Dimension("ausgedacht", 838);
        assertFalse(DaacAttributes.isHeightDimension(dimension));
    }
}
