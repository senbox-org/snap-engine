package org.esa.snap.binning.operator.formatter;

import org.esa.snap.binning.support.CrsGridEpsg3067;
import org.esa.snap.binning.support.PlateCarreeGrid;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;

public class DefaultFormatterTest {

    @Test
    public void testGetOutputRegion_noRoi_noCRSGrid() {
        final Rectangle region = DefaultFormatter.getOutputRegion(new PlateCarreeGrid(18), null);
        assertNotNull(region);
        assertEquals(0, region.x);
        assertEquals(0, region.y);
        assertEquals(36, region.width);
        assertEquals(18, region.height);
    }

    @Test
    public void testGetOutputRegion_noRoi_CRSGrid() {
        final Rectangle region = DefaultFormatter.getOutputRegion(new CrsGridEpsg3067(2160), null);
        assertNotNull(region);
        assertEquals(0, region.x);
        assertEquals(0, region.y);
        assertEquals(78, region.width);
        assertEquals(137, region.height);
    }
}
