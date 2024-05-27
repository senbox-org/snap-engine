package eu.esa.opt.snap.core.datamodel.band;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DataPointTest {

    @Test
    @STTM("SNAP-1691")
    public void testConstructionAndGetter() {
        final DataPoint dataPoint = new DataPoint(2, 3, 4.0);

        assertEquals(2, dataPoint.getX());
        assertEquals(3, dataPoint.getY());
        assertEquals(4.0, dataPoint.getValue(), 1e-8);
    }
}
