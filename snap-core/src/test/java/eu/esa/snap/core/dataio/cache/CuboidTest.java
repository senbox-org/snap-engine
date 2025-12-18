package eu.esa.snap.core.dataio.cache;

import org.junit.Test;

import static org.junit.Assert.*;

public class CuboidTest {

    @Test
    public void testConstructionAndGetter()  {
        Cuboid cuboid = new Cuboid(new int[]{0, 10, 20}, new int[]{20, 30, 40});

        assertEquals(0, cuboid.getZ());
        assertEquals(10, cuboid.getY());
        assertEquals(20, cuboid.getX());

        assertEquals(20, cuboid.getDepth());
        assertEquals(30, cuboid.getHeight());
        assertEquals(40, cuboid.getWidth());
    }

    @Test
    public void testIsEmpty() {
        Cuboid cuboid = new Cuboid(new int[]{0, 10, 20}, new int[]{20, 30, 40});
        assertFalse(cuboid.isEmpty());

        cuboid = new Cuboid(new int[]{0, 10, 20}, new int[]{0, 30, 40});
        assertTrue(cuboid.isEmpty());

        cuboid = new Cuboid(new int[]{0, 10, 20}, new int[]{20, 0, 40});
        assertTrue(cuboid.isEmpty());

        cuboid = new Cuboid(new int[]{0, 10, 20}, new int[]{20, 30, 0});
        assertTrue(cuboid.isEmpty());
    }

    @Test
    public void testIntersection_notIntersecting() {
        Cuboid cuboid_1 = new Cuboid(new int[]{10, 10, 20}, new int[]{10, 10, 20});
        Cuboid cuboid_2 = new Cuboid(new int[]{30, 30, 50}, new int[]{10, 10, 20});

        Cuboid intersection = cuboid_1.intersection(cuboid_2);
        assertTrue(intersection.isEmpty());
    }
}
