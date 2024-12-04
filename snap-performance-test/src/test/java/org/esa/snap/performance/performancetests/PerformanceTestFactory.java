package org.esa.snap.performance.performancetests;

import org.esa.snap.performance.util.Parameters;

public class PerformanceTestFactory {

    public static AbstractPerformanceTest createPerformanceTest(Parameters params) {
        String name = params.getTestName().toLowerCase();

        for (TestName test : TestName.values()) {
            if (name.equals(test.getName())) {
                return test.createTest(params);
            }
        }
        throw new IllegalArgumentException("Unknown test name: " + name);
    }

    private enum TestName {
        READ_SINGLE_PRODUCT("read-single-product") {
            @Override
            public AbstractPerformanceTest createTest(Parameters params) {
                return new ReadSingleProductTest(params);
            }
        },
        WRITE_SINGLE_PRODUCT("write-single-product") {
            @Override
            public AbstractPerformanceTest createTest(Parameters params) {
                return new WriteSingleProductTest(params);
            }
        };

        private final String name;

        TestName(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public abstract AbstractPerformanceTest createTest(Parameters params);
    }
}
