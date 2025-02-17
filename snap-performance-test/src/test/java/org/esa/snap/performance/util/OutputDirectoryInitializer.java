package org.esa.snap.performance.util;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class OutputDirectoryInitializer {

    private static final String RESULTS_DIR = "Results";


    public static void initialize(String outputDirectory) throws IOException, IllegalArgumentException {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("Output directory path is not defined in configuration ('outputDir'). Please check your configuration file.");
        }

        File outputDir = new File(outputDirectory);
        File resultsDir = new File(outputDir, RESULTS_DIR);

        if (outputDir.exists()) {
            for (File file : Objects.requireNonNull(outputDir.listFiles())) {
                if (file.isDirectory() && file.getName().equals(RESULTS_DIR)) {
                    continue;
                }
                if (file.isDirectory()) {
                    TestUtils.deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        } else {
            if (!outputDir.mkdirs()) {
                throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
            }
        }

        if (!resultsDir.exists() && !resultsDir.mkdirs()) {
            throw new IOException("Failed to create results directory: " + resultsDir.getAbsolutePath());
        }
    }

    public static void deleteDirectory(File directory) throws IOException {
        if (directory.isDirectory()) {
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                deleteDirectory(file);
            }
        }
        if (!directory.delete()) {
            throw new IOException("Failed to delete file or directory: " + directory.getAbsolutePath());
        }
    }
}
