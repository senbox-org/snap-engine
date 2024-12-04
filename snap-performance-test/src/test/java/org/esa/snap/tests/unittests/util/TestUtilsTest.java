package org.esa.snap.tests.unittests.util;

import org.esa.snap.performance.util.TestUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class TestUtilsTest {

    @Test
    public void testCalculateArithmeticMean() {
        long[] numbers1 = new long[] {1,2,3,4,5,6};
        long[] numbers2 = new long[] {0};
        long[] numbers3 = new long[] {61,52,43,34,25,6};
        long[] numbers4 = new long[] {};
        long[] numbers5 = null;

        assertEquals(3.5, TestUtils.calculateArithmeticMean(numbers1), 0.0000001);
        assertEquals(0, TestUtils.calculateArithmeticMean(numbers2), 0.0000001);
        assertEquals(36.833333333, TestUtils.calculateArithmeticMean(numbers3), 0.0000001);

        IllegalArgumentException exception1 = assertThrows( IllegalArgumentException.class, () -> { TestUtils.calculateArithmeticMean(numbers4);});
        IllegalArgumentException exception2 = assertThrows( IllegalArgumentException.class, () -> { TestUtils.calculateArithmeticMean(numbers5);});

        assertEquals("Array must not be null or empty", exception1.getMessage());
        assertEquals("Array must not be null or empty", exception2.getMessage());
    }

    @Test
    public void testReduceResultArray() {
        long[] numbers1 = new long[] {1,2,3,4,5};
        long[] numbers2 = new long[] {3};
        long[] numbers3 = new long[] {};
        long[] numbers4 = new long[] {4,5};

        assertArrayEquals(new long[] {1,2,3,4,5}, TestUtils.reduceResultArray(false, numbers1));
        assertArrayEquals(new long[] {2,3,4,5}, TestUtils.reduceResultArray(true, numbers1));
        assertArrayEquals(new long[] {3}, TestUtils.reduceResultArray(false, numbers2));
        assertArrayEquals(new long[] {}, TestUtils.reduceResultArray(true, numbers2));
        assertArrayEquals(new long[] {}, TestUtils.reduceResultArray(false, numbers3));
        assertArrayEquals(new long[] {}, TestUtils.reduceResultArray(true, numbers3));
        assertArrayEquals(new long[] {4,5}, TestUtils.reduceResultArray(false, numbers4));
        assertArrayEquals(new long[] {5}, TestUtils.reduceResultArray(true, numbers4));

        long[] original = new long[] {1,2,3,4,5};
        long[] reduced = TestUtils.reduceResultArray(true, original);

        assertArrayEquals(new long[] {2,3,4,5}, reduced);
        assertArrayEquals(new long[] {1,2,3,4,5}, original);
    }

    @Test
    public void testBuildProductPath() {
        File file1 = TestUtils.buildProductPath("/path/to/directory", "product1.dim");
        assertEquals("/path/to/directory/product1.dim", file1.getPath().replace("\\", "/"));

        File file2 = TestUtils.buildProductPath("/path/to/directory/", "product2.dim");
        assertEquals("/path/to/directory/product2.dim", file2.getPath().replace("\\", "/"));

        File file3 = TestUtils.buildProductPath("/path/to/directory", "");
        assertEquals("/path/to/directory", file3.getPath().replace("\\", "/"));

        File file4 = TestUtils.buildProductPath("", "product3.dim");
        assertEquals("/product3.dim", file4.getPath().replace("\\", "/"));

        File file5 = TestUtils.buildProductPath("", "");
        assertEquals("/", file5.getPath().replace("\\", "/"));
    }
}