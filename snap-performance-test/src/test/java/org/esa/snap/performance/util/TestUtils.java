package org.esa.snap.performance.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    public static double calculateThroughput(long[] times, File[] files) throws IOException {

        double[] fileSizeInMB = new double[files.length];
        double[] throughputs = new double[times.length];

        for (int ii = 0; ii < files.length; ii++) {
            File file = files[ii];
            if (file.isDirectory()) {
                fileSizeInMB[ii] = TestUtils.getFolderSize(file);
            } else if (file.getName().endsWith(".zip")) {
                File tempDir = unzipToTemp(file);
                fileSizeInMB[ii] = TestUtils.getFolderSize(tempDir);
                deleteDirectory(tempDir);
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

        if (fileName.endsWith(".znap.zip")) {
            return fileName.substring(0, fileName.length() - ".znap.zip".length());
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            // no extension present
            return fileName;
        }

        return fileName.substring(0, lastDotIndex);
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

    public static void deleteDirectory(Path directory) throws IOException {
        Files.walk(directory)
                .sorted((a,b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete directory: " + path, e);
                    }
                });
    }

    private static void deleteDirectory(File directory) throws IOException {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                deleteDirectory(file);
            }
        }
        if (!directory.delete()) {
            throw new IOException("Failed to delete file or directory: " + directory.getAbsolutePath());
        }
    }

    private static File unzipToTemp(File zipFile) throws IOException {
        File tempDir = Files.createTempDirectory("unzip_temp").toFile();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(tempDir, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
        return tempDir;
    }
}
