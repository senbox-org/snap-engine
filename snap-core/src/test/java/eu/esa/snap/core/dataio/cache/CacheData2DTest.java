package eu.esa.snap.core.dataio.cache;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CacheData2DTest {

    @Test
    public void testIntersects() {
        final CacheData2D cacheData2D = new CacheData2D(100, 199, 450, 499);

        // inside
        int[] offsets = new int[]{460, 120};
        int[] shapes = new int[]{20, 20};
        assertTrue(cacheData2D.intersects(offsets, shapes));

        // intersect left border
        offsets = new int[]{460, 90};
        shapes = new int[]{20, 20};
        assertTrue(cacheData2D.intersects(offsets, shapes));

        // intersect upper left corner
        offsets = new int[]{440, 90};
        shapes = new int[]{20, 20};
        assertTrue(cacheData2D.intersects(offsets, shapes));

        // intersect upper border
        offsets = new int[]{440, 110};
        shapes = new int[]{20, 20};
        assertTrue(cacheData2D.intersects(offsets, shapes));

        // intersect upper right corner
        offsets = new int[]{440, 190};
        shapes = new int[]{20, 20};
        assertTrue(cacheData2D.intersects(offsets, shapes));

        // intersect right border
        offsets = new int[]{460, 190};
        shapes = new int[]{20, 20};
        assertTrue(cacheData2D.intersects(offsets, shapes));

        // intersect lower right corner
        offsets = new int[]{490, 190};
        shapes = new int[]{20, 20};
        assertTrue(cacheData2D.intersects(offsets, shapes));

        // intersect lower border
        offsets = new int[]{490, 130};
        shapes = new int[]{20, 20};
        assertTrue(cacheData2D.intersects(offsets, shapes));

        // intersect lower left corner
        offsets = new int[]{490, 90};
        shapes = new int[]{20, 20};
        assertTrue(cacheData2D.intersects(offsets, shapes));
    }

    @Test
    public void testIntersects_outside() {
        final CacheData2D cacheData2D = new CacheData2D(100, 199, 450, 499);

        // too far left
        int[] offsets = new int[]{460, 0};
        int[] shapes = new int[]{20, 20};
        assertFalse(cacheData2D.intersects(offsets, shapes));

        // too high
        offsets = new int[]{0, 120};
        shapes = new int[]{20, 20};
        assertFalse(cacheData2D.intersects(offsets, shapes));

        // too far right
        offsets = new int[]{460, 299};
        shapes = new int[]{20, 20};
        assertFalse(cacheData2D.intersects(offsets, shapes));

        // too low
        offsets = new int[]{599, 120};
        shapes = new int[]{20, 20};
        assertFalse(cacheData2D.intersects(offsets, shapes));
    }

    @Test
    public void testInside_y() {
        final CacheData2D cacheData2D = new CacheData2D(100, 199, 450, 499);

        assertTrue(cacheData2D.inside_y(450));
        assertTrue(cacheData2D.inside_y(470));
        assertTrue(cacheData2D.inside_y(499));

        assertFalse(cacheData2D.inside_y(449));
        assertFalse(cacheData2D.inside_y(500));
    }

    @Test
    public void testInside_x() {
        final CacheData2D cacheData2D = new CacheData2D(100, 199, 450, 499);

        assertTrue(cacheData2D.inside_x(100));
        assertTrue(cacheData2D.inside_x(156));
        assertTrue(cacheData2D.inside_x(199));

        assertFalse(cacheData2D.inside_x(99));
        assertFalse(cacheData2D.inside_x(200));
    }
}
