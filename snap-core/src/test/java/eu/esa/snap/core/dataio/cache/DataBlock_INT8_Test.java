package eu.esa.snap.core.dataio.cache;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DataBlock_INT8_Test {

    @Test
    public void testConstruction() {
        final DataBlock_INT8 db = new DataBlock_INT8(128, 64, 0, 0);

        assertEquals(128, db.getWidth());
    }
}
