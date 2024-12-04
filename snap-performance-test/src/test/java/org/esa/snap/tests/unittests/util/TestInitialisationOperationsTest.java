package org.esa.snap.tests.unittests.util;

import org.esa.snap.performance.performancetests.AbstractPerformanceTest;
import org.esa.snap.performance.util.ConfigLoader;
import org.esa.snap.performance.util.TestInitialisationOperations;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

public class TestInitialisationOperationsTest {

    @Test
    public void testInitialize() throws IOException {

        Path tempDir = Files.createTempDirectory("test_initialize_output");

        ConfigLoader config = createMockConfigLoader(tempDir.toString());

        List<AbstractPerformanceTest> tests = TestInitialisationOperations.initialize(config);

        Path resultsDir = tempDir.resolve("Results");
        assertTrue("Results directory should exist", Files.exists(resultsDir));
        assertTrue("Results directory should be a directory", Files.isDirectory(resultsDir));

        assertEquals("Two tests should be initialized", 2, tests.size());
        assertEquals("read-single-product", tests.get(0).getTestName());
        assertEquals("write-single-product", tests.get(1).getTestName());

        deleteDirectory(tempDir);
    }

    private void deleteDirectory(Path directory) throws IOException {
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

    private ConfigLoader createMockConfigLoader(String outputDir) throws IOException {
        return new ConfigLoader() {
            private final Properties properties = new Properties();

            {
                properties.setProperty("outputDir", outputDir);
                properties.setProperty("testNames", "read-single-product-test1,write-single-product-test2");
                properties.setProperty("read-single-product-test1.testImplementation", "read-single-product");
                properties.setProperty("read-single-product-test1.productName", "testProduct1");
                properties.setProperty("read-single-product-test1.threading", "SINGLE");
                properties.setProperty("write-single-product-test2.testImplementation", "write-single-product");
                properties.setProperty("write-single-product-test2.productName", "testProduct2");
                properties.setProperty("write-single-product-test2.threading", "SINGLE");
                properties.setProperty("testDataDir", "/test/data");
                properties.setProperty("discardFirstMeasure", "true");
                properties.setProperty("numExecutionsForAverageOperations", "5");
            }

            @Override
            public String get(String key) {
                return properties.getProperty(key);
            }
        };
    }
}