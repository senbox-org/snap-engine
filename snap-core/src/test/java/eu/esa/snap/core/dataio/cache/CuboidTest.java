package eu.esa.snap.core.dataio.cache;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.*;

public class CuboidTest {

    @Test
    @STTM("SNAP-4121")
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
    @STTM("SNAP-4121")
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
    @STTM("SNAP-4121")
    public void testIntersection_notIntersecting() {
        Cuboid cuboid_1 = new Cuboid(new int[]{10, 10, 20}, new int[]{10, 10, 20});
        Cuboid cuboid_2 = new Cuboid(new int[]{30, 30, 50}, new int[]{10, 10, 20});

        Cuboid intersection = cuboid_1.intersection(cuboid_2);
        assertTrue(intersection.isEmpty());
    }

    @Test
    @STTM("SNAP-4121")
    public void testIntersection_intersecting() {
        Cuboid cuboid_1 = new Cuboid(new int[]{100, 100, 100}, new int[]{10, 10, 10});

        Cuboid cuboid_2 = new Cuboid(new int[]{95, 95, 95}, new int[]{10, 10, 10});
        Cuboid intersection = cuboid_1.intersection(cuboid_2);
        assertEquals(100, intersection.getX());
        assertEquals(5, intersection.getWidth());
        assertEquals(100, intersection.getY());
        assertEquals(5, intersection.getHeight());
        assertEquals(100, intersection.getZ());
        assertEquals(5, intersection.getDepth());

        cuboid_2 = new Cuboid(new int[]{105, 95, 95}, new int[]{10, 10, 10});
        intersection = cuboid_1.intersection(cuboid_2);
        assertEquals(100, intersection.getX());
        assertEquals(5, intersection.getWidth());
        assertEquals(100, intersection.getY());
        assertEquals(5, intersection.getHeight());
        assertEquals(105, intersection.getZ());
        assertEquals(5, intersection.getDepth());

        cuboid_2 = new Cuboid(new int[]{105, 95, 105}, new int[]{10, 10, 10});
        intersection = cuboid_1.intersection(cuboid_2);
        assertEquals(105, intersection.getX());
        assertEquals(5, intersection.getWidth());
        assertEquals(100, intersection.getY());
        assertEquals(5, intersection.getHeight());
        assertEquals(105, intersection.getZ());
        assertEquals(5, intersection.getDepth());

        cuboid_2 = new Cuboid(new int[]{95, 95, 105}, new int[]{10, 10, 10});
        intersection = cuboid_1.intersection(cuboid_2);
        assertEquals(105, intersection.getX());
        assertEquals(5, intersection.getWidth());
        assertEquals(100, intersection.getY());
        assertEquals(5, intersection.getHeight());
        assertEquals(100, intersection.getZ());
        assertEquals(5, intersection.getDepth());

        cuboid_2 = new Cuboid(new int[]{95, 105, 95}, new int[]{10, 10, 10});
        intersection = cuboid_1.intersection(cuboid_2);
        assertEquals(100, intersection.getX());
        assertEquals(5, intersection.getWidth());
        assertEquals(105, intersection.getY());
        assertEquals(5, intersection.getHeight());
        assertEquals(100, intersection.getZ());
        assertEquals(5, intersection.getDepth());

        cuboid_2 = new Cuboid(new int[]{105, 105, 95}, new int[]{10, 10, 10});
        intersection = cuboid_1.intersection(cuboid_2);
        assertEquals(100, intersection.getX());
        assertEquals(5, intersection.getWidth());
        assertEquals(105, intersection.getY());
        assertEquals(5, intersection.getHeight());
        assertEquals(105, intersection.getZ());
        assertEquals(5, intersection.getDepth());

        cuboid_2 = new Cuboid(new int[]{105, 105, 105}, new int[]{10, 10, 10});
        intersection = cuboid_1.intersection(cuboid_2);
        assertEquals(105, intersection.getX());
        assertEquals(5, intersection.getWidth());
        assertEquals(105, intersection.getY());
        assertEquals(5, intersection.getHeight());
        assertEquals(105, intersection.getZ());
        assertEquals(5, intersection.getDepth());

        cuboid_2 = new Cuboid(new int[]{95, 105, 105}, new int[]{10, 10, 10});
        intersection = cuboid_1.intersection(cuboid_2);
        assertEquals(105, intersection.getX());
        assertEquals(5, intersection.getWidth());
        assertEquals(105, intersection.getY());
        assertEquals(5, intersection.getHeight());
        assertEquals(100, intersection.getZ());
        assertEquals(5, intersection.getDepth());
    }

    @Test
    @STTM("SNAP-4121")
    public void testIntersection_commutative() {
        Cuboid cuboid_1 = new Cuboid(new int[]{10, 10, 20}, new int[]{10, 10, 20});
        Cuboid cuboid_2 = new Cuboid(new int[]{5, 10, 20}, new int[]{10, 10, 20});

        Cuboid intersection = cuboid_1.intersection(cuboid_2);
        assertFalse(intersection.isEmpty());
        assertEquals(20, intersection.getX());
        assertEquals(10, intersection.getY());
        assertEquals(10, intersection.getZ());
        assertEquals(20, intersection.getWidth());
        assertEquals(10, intersection.getHeight());
        assertEquals(5, intersection.getDepth());

        intersection = cuboid_1.intersection(cuboid_2);
        assertFalse(intersection.isEmpty());
        assertEquals(20, intersection.getX());
        assertEquals(10, intersection.getY());
        assertEquals(10, intersection.getZ());
        assertEquals(20, intersection.getWidth());
        assertEquals(10, intersection.getHeight());
        assertEquals(5, intersection.getDepth());
    }

    @Test
    @STTM("SNAP-4121")
    public void testIntersection_withSelf() {
        Cuboid cuboid = new Cuboid(new int[]{5, 10, 20}, new int[]{20, 20, 50});

        Cuboid intersection = cuboid.intersection(cuboid);
        assertEquals(20, intersection.getX());
        assertEquals(10, intersection.getY());
        assertEquals(5, intersection.getZ());
        assertEquals(50, intersection.getWidth());
        assertEquals(20, intersection.getHeight());
        assertEquals(20, intersection.getDepth());
    }

    @Test
    @STTM("SNAP-4121")
    public void testIntersection_overflowX() {
        Cuboid cuboid_1 = new Cuboid(new int[]{10, 10, Integer.MAX_VALUE - 1}, new int[]{10, 10, 20});
        Cuboid cuboid_2 = new Cuboid(new int[]{5, 10, Integer.MIN_VALUE + 1}, new int[]{10, 10, 20});

        Cuboid intersection = cuboid_2.intersection(cuboid_1);
        assertEquals(2147483646, intersection.getX());
        assertEquals(-2147483648, intersection.getWidth());
    }

    @Test
    @STTM("SNAP-4121")
    public void testIntersection_overflowY() {
        Cuboid cuboid_1 = new Cuboid(new int[]{10, Integer.MAX_VALUE - 1, 10}, new int[]{10, 10, 20});
        Cuboid cuboid_2 = new Cuboid(new int[]{5, Integer.MIN_VALUE + 1, 10}, new int[]{10, 10, 20});

        Cuboid intersection = cuboid_2.intersection(cuboid_1);
        assertEquals(2147483646, intersection.getY());
        assertEquals(-2147483648, intersection.getHeight());
    }

    @Test
    @STTM("SNAP-4121")
    public void testIntersection_overflowZ() {
        Cuboid cuboid_1 = new Cuboid(new int[]{Integer.MAX_VALUE - 1, 10, 10}, new int[]{10, 10, 20});
        Cuboid cuboid_2 = new Cuboid(new int[]{Integer.MIN_VALUE + 1, 5, 10}, new int[]{10, 10, 20});

        Cuboid intersection = cuboid_2.intersection(cuboid_1);
        assertEquals(2147483646, intersection.getZ());
        assertEquals(-2147483648, intersection.getDepth());
    }

    @Test
    @STTM("SNAP-4121")
    public void testGetShape() {
        final Cuboid cuboid = new Cuboid(new int[]{5, 10, 20}, new int[]{6, 8, 10});

        assertArrayEquals(new int[]{6, 8, 10}, cuboid.getShape());
    }
}
