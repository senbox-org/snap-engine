package eu.esa.snap.core.dataio.cache;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.*;

public class CuboidTest {

    @Test
    @STTM("SNAP-4107")
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
    @STTM("SNAP-4107")
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
    @STTM("SNAP-4107")
    public void testIntersection_notIntersecting() {
        Cuboid cuboid_1 = new Cuboid(new int[]{10, 10, 20}, new int[]{10, 10, 20});
        Cuboid cuboid_2 = new Cuboid(new int[]{30, 30, 50}, new int[]{10, 10, 20});

        Cuboid intersection = cuboid_1.intersection(cuboid_2);
        assertTrue(intersection.isEmpty());
    }

    @Test
    @STTM("SNAP-4107")
    public void testIntersection_intersecting() {

    }

    @Test
    @STTM("SNAP-4107")
    public void testIntersection_commutativ() {
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
}
