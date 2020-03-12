package org.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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

    @Test
    public void testSetGetData() {
        final Slab slab = new Slab(new Rectangle(2, 4, 7, 8));

        assertNull(slab.getData());

        final ProductData data = ProductData.createInstance(new int[3]);
        slab.setData(data);
        assertSame(data, slab.getData());
    }

    @Test
    public void testGetSizeInBytes_noData() {
        final Slab slab = new Slab(new Rectangle(3, 5, 8, 9));

        assertEquals(24L, slab.getSizeInBytes());
    }

    @Test
    public void testGetSizeInBytes_withData() {
        final Slab slab = new Slab(new Rectangle(3, 5, 8, 9));

        final ProductData data = mock(ProductData.class);
        when(data.getNumElems()).thenReturn(100);
        when(data.getElemSize()).thenReturn(8);
        slab.setData(data);

        assertEquals(824L, slab.getSizeInBytes());
    }

    @Test
    public void testDispose_noData() {
        final Slab slab = new Slab(new Rectangle(4, 6, 9, 10));

        slab.dispose();
    }

    @Test
    public void testDispose_withData() {
        final Slab slab = new Slab(new Rectangle(4, 6, 9, 10));

        final ProductData data = mock(ProductData.class);
        slab.setData(data);

        slab.dispose();

        verify(data, times(1)).dispose();
        verifyNoMoreInteractions(data);
    }
}
