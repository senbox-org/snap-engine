package eu.esa.snap.core.dataio.cache;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RowColTest {

    @Test
    public void testConstruction() {
        final RowCol rowCol = new RowCol(1, 2);
        assertEquals(1, rowCol.getCacheRow());
        assertEquals(2, rowCol.getCacheCol());
    }

    @Test
    public void testSetGet() {
        final RowCol rowCol = new RowCol(3, 4);

        rowCol.setCacheRow(0);
        assertEquals(0, rowCol.getCacheRow());

        rowCol.setCacheCol(14);
        assertEquals(14, rowCol.getCacheCol());

    }
}
