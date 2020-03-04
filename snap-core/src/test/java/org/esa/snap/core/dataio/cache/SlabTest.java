package org.esa.snap.core.dataio.cache;

import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.assertEquals;

public class SlabTest {

    @Test
    public void testConstruction() {
        final Rectangle rectangle = new Rectangle(0, 2, 5, 6);

        final Slab slab = new Slab(rectangle);
        final Rectangle returnRect = slab.getRegion();
        assertEquals(rectangle.x, returnRect.x);
        assertEquals(rectangle.y, returnRect.y);
        assertEquals(rectangle.width, returnRect.width);
        assertEquals(rectangle.height, returnRect.height);

        assertEquals(-1L, slab.getLastAccess());
    }

    @Test
    public void testSetGetLastAccess() {
        final Slab slab = new Slab(new Rectangle(1, 3, 6, 7));

        slab.setLastAccess(23456L);
        assertEquals(23456L, slab.getLastAccess());
    }
}
