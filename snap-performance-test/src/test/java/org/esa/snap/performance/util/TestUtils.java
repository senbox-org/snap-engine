package org.esa.snap.performance.util;

import java.io.File;

public class TestUtils {

    public static final String RESULTS_DIR = "Results";

    public static final String FORMAT_DIMAP = "BEAM-DIMAP";
    public static final String FORMAT_ZNAP = "ZNAP";

    public static final String EXTENSION_DIMAP = ".dim";
    public static final String EXTENSION_DIMAP_DATA = ".data";
    public static final String EXTENSION_ZNAP = ".znap.zip";
    public static final String EXTENSION_ZNAP_UNZIPPED = ".znap";

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

    public static double calculateArithmeticMean(double[] numbers) {
        if (numbers == null || numbers.length == 0) {
            throw new IllegalArgumentException("Array must not be null or empty");
        }
        double sum = 0;
        for (double number : numbers) {
            sum += number;
        }

        return sum / numbers.length;
    }

    public static double calculateThroughput(long[] times, File[] files) {

        double[] fileSizeInMB = new double[files.length];
        double[] throughputs = new double[times.length];

        for (int ii = 0; ii < files.length; ii++) {
            File file = files[ii];
            if (file.isDirectory()) {
                fileSizeInMB[ii] = TestUtils.getFolderSize(file);
            } else {
                long fileSizeInBytes = file.length();
                fileSizeInMB[ii] = fileSizeInBytes / (1024.0 * 1024.0);
            }
        }

        if (fileSizeInMB.length == 1) {
            double fileSize = fileSizeInMB[0];
            for (int ii = 0; ii < throughputs.length; ii++) {
                throughputs[ii] = fileSize / times[ii] / 1000;
            }
        } else {
            for (int ii = 0; ii < throughputs.length; ii++) {
                throughputs[ii] = fileSizeInMB[ii] / times[ii] / 1000;
            }
        }
        return TestUtils.calculateArithmeticMean(throughputs);
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
        return new File(buildProductPathString(path, product));
    }

    public static String buildProductPathString(String path, String testName, String product, String threading) {
        return path + "/" + testName+ "/" + product+ "/" + threading;
    }

    public static String buildProductPathString(String path, String product) {
        return path + "/" + product;
    }

    public static String cutExtensionFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        int firstDotIndex = fileName.indexOf('.');
        if (firstDotIndex == -1) {
            return fileName;
        }

        return fileName.substring(0, firstDotIndex);
    }

    public static long getFolderSize(File folder) {
        long length = 0;

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    length += file.length();
                } else if (file.isDirectory()) {
                    length += getFolderSize(file);
                }
            }
        }
        return length;
    }

    public static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
