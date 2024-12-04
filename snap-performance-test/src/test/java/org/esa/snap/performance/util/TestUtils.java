package org.esa.snap.performance.util;

import java.io.File;

public class TestUtils {

    public static final String RESULTS_DIR = "Results";

    public static double calculateArithmeticMean(long[] numbers) {
        if (numbers == null || numbers.length == 0) {
            throw new IllegalArgumentException("Array must not be null or empty");
        }
        long sum = 0;
        for (long number : numbers) {
            sum += number;
        }

        return (double) sum / numbers.length;
    }

    public static long[] reduceResultArray(boolean discardFirstMeasure, long[] times) {
        if (discardFirstMeasure && times.length > 0) {
            long[] reducedArray = new long[times.length - 1];
            System.arraycopy(times, 1, reducedArray, 0, reducedArray.length);
            return reducedArray;
        } else {
            return times;
        }
    }

    public static File buildProductPath(String path, String product) {
        return new File(path + "/" + product);
    }
}
