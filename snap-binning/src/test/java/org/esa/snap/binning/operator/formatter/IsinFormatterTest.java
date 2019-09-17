package org.esa.snap.binning.operator.formatter;

import org.esa.snap.binning.support.IsinPlanetaryGrid;
import org.junit.Test;

import static org.junit.Assert.fail;

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

//    @Test
//    public void testGetProductName() throws Exception {
//        new FormatterConfig()
//    }
}
