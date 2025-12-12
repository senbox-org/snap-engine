package eu.esa.snap.core.dataio.cache;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CacheData3DTest {

    @Test
    public void testInside_z() {
        int[] offsets = new int[]{10, 200, 300};
        int[] shapes = new int[]{20, 50, 50};
        final CacheData3D cacheData3D = new CacheData3D(offsets, shapes);

        assertTrue(cacheData3D.inside_z(12));
        assertTrue(cacheData3D.inside_z(23));
        assertTrue(cacheData3D.inside_z(29));

        assertFalse(cacheData3D.inside_z(9));
        assertFalse(cacheData3D.inside_z(30));
    }

    @Test
    public void testInside_y() {
        final int[] offsets = new int[]{10, 200, 300};
        final int[] shapes = new int[]{20, 50, 50};
        final CacheData3D cacheData3D = new CacheData3D(offsets, shapes);

        assertTrue(cacheData3D.inside_y(214));
        assertTrue(cacheData3D.inside_y(236));
        assertTrue(cacheData3D.inside_y(249));

        assertFalse(cacheData3D.inside_y(199));
        assertFalse(cacheData3D.inside_y(250));
    }

    @Test
    public void testInside_x() {
        final int[] offsets = new int[]{10, 200, 300};
        final int[] shapes = new int[]{20, 50, 50};
        final CacheData3D cacheData3D = new CacheData3D(offsets, shapes);

        assertTrue(cacheData3D.inside_x(306));
        assertTrue(cacheData3D.inside_x(311));
        assertTrue(cacheData3D.inside_x(349));

        assertFalse(cacheData3D.inside_x(299));
        assertFalse(cacheData3D.inside_x(350));
    }

    @Test
    public void testIntersects() {
        int[] offsets = new int[]{20, 50, 100};
        int[] shapes = new int[]{20, 100, 100};
        final CacheData3D cacheData3D = new CacheData3D(offsets, shapes);

        // fully inside
        offsets = new int[]{23, 55, 108};
        shapes = new int[]{10, 10, 10};
        assertTrue(cacheData3D.intersects(offsets, shapes));

        // intersects front only
        offsets = new int[]{20, 45, 100};
        shapes = new int[]{10, 10, 10};
        assertTrue(cacheData3D.intersects(offsets, shapes));

        // intersects front and left
        offsets = new int[]{20, 45, 95};
        shapes = new int[]{10, 10, 10};
        assertTrue(cacheData3D.intersects(offsets, shapes));

        // intersects top and left
        offsets = new int[]{12, 50, 95};
        shapes = new int[]{10, 10, 10};
        assertTrue(cacheData3D.intersects(offsets, shapes));

        // intersects bottom and back
        offsets = new int[]{35, 70, 100};
        shapes = new int[]{10, 10, 10};
        assertTrue(cacheData3D.intersects(offsets, shapes));

        // intersects bottom and right
        offsets = new int[]{35, 50, 195};
        shapes = new int[]{10, 10, 10};
        assertTrue(cacheData3D.intersects(offsets, shapes));

        // intersects top front right corner
        offsets = new int[]{15, 45, 195};
        shapes = new int[]{10, 10, 10};
        assertTrue(cacheData3D.intersects(offsets, shapes));

        // intersects bottom back left corner
        offsets = new int[]{35, 145, 95};
        shapes = new int[]{10, 10, 10};
        assertTrue(cacheData3D.intersects(offsets, shapes));

        // outsiders ----------------------------------------
        // --------------------------------------------------

        // outside front
        offsets = new int[]{20, 40, 100};
        shapes = new int[]{10, 10, 10};
        assertFalse(cacheData3D.intersects(offsets, shapes));

        // outside left
        offsets = new int[]{20, 50, 80};
        shapes = new int[]{10, 10, 10};
        assertFalse(cacheData3D.intersects(offsets, shapes));

        // outside top
        offsets = new int[]{5, 50, 100};
        shapes = new int[]{10, 10, 10};
        assertFalse(cacheData3D.intersects(offsets, shapes));

        // outside bottom
        offsets = new int[]{40, 50, 100};
        shapes = new int[]{10, 10, 10};
        assertFalse(cacheData3D.intersects(offsets, shapes));

        // outside back
        offsets = new int[]{20, 150, 100};
        shapes = new int[]{10, 10, 10};
        assertFalse(cacheData3D.intersects(offsets, shapes));

        // outside right
        offsets = new int[]{20, 50, 200};
        shapes = new int[]{10, 10, 10};
        assertFalse(cacheData3D.intersects(offsets, shapes));
    }

    // bounding rect - do we need this in 3d world? Better a bounding cube.


}
