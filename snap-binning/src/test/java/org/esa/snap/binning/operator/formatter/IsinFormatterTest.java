package org.esa.snap.binning.operator.formatter;

import org.esa.snap.binning.support.IsinPlanetaryGrid;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class IsinFormatterTest {

    @Test
    public void testFailsWhenNoFeatures() throws Exception {
        final IsinFormatter formatter = new IsinFormatter();

        try {
            formatter.format(null, null, new String[0], null, null, null, null, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetProductName() {
        final Date startTime = new Date(1570000000000L);
        final Date stopTime = new Date(1570100000000L);
        final int x_tile = 6;
        final int y_tile = 14;

        final String productName = IsinFormatter.getProductName(startTime, stopTime, x_tile, y_tile);
        assertTrue( productName.startsWith("S3A_OL_3_VEG_20191002_20191003_"));
        assertTrue( productName.endsWith("_h06v14_v1_MPC_O.nc"));
    }
}
