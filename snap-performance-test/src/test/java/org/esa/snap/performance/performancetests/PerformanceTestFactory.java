package org.esa.snap.performance.performancetests;

import org.esa.snap.performance.util.Parameters;

public class PerformanceTestFactory {

    public static final String READ_SINGLE_PRODUCT_TEST = "read-single-product";
    public static final String WRITE_SINGLE_PRODUCT_FROM_READER = "write-single-product-from-reader";
    public static final String WRITE_SINGLE_PRODUCT_FROM_MEMORY = "write-single-product-from-memory";

    public static AbstractPerformanceTest createPerformanceTest(Parameters params) {
        String name = params.getTestName().toLowerCase();

        switch (name) {
            case READ_SINGLE_PRODUCT_TEST:
                return new ReadSingleProductTest(name, params);
            case WRITE_SINGLE_PRODUCT_FROM_READER:
                return new WriteProductFromReaderTest(name, params);
            case WRITE_SINGLE_PRODUCT_FROM_MEMORY:
                return new WriteProductFromMemoryTest(name, params);
            default:
                throw new IllegalArgumentException("Unknown test name: " + name);
        }
    }
}
