package org.esa.snap.oldImpl.performance.performancetests;

import org.esa.snap.oldImpl.performance.util.MyParameters;

public class PerformanceTestFactory {

    public static final String READ_SINGLE_PRODUCT_TEST = "read-single-product";
    public static final String WRITE_SINGLE_PRODUCT_FROM_READER = "write-single-product-from-reader";
    public static final String WRITE_SINGLE_PRODUCT_FROM_MEMORY = "write-single-product-from-memory";

    public static AbstractPerformanceTest createPerformanceTest(MyParameters params) {
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
