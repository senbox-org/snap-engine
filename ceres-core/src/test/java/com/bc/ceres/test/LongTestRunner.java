package com.bc.ceres.test;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class LongTestRunner extends BlockJUnit4ClassRunner {

    private static final String PROPERTYNAME_EXECUTE_LONG_TESTS = "enable.long.tests";
    private final Class<?> clazz;
    private final boolean runLongTests;

    public LongTestRunner(Class<?> klass) throws InitializationError {
        super(klass);

        this.clazz = klass;
        runLongTests = Boolean.getBoolean(PROPERTYNAME_EXECUTE_LONG_TESTS);
        if (!runLongTests) {
            System.out.println("Long Tests disabled. Set VM param -D" + PROPERTYNAME_EXECUTE_LONG_TESTS + "=true to enable.");
        }
    }

    @Override
    public Description getDescription() {
        return Description.createSuiteDescription("Long Tests Runner");
    }

    @Override
    public void run(RunNotifier runNotifier) {
        if (runLongTests) {
            super.run(runNotifier);
        } else {
            final Description description = Description.createTestDescription(clazz, "allMethods. Long tests disabled. Set VM param -D" + PROPERTYNAME_EXECUTE_LONG_TESTS + "=true to enable.");
            runNotifier.fireTestIgnored(description);
        }
    }
}

