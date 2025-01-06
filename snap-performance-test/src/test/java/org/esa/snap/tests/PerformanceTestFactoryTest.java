package org.esa.snap.tests;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.performance.testImplementation.ReadPerformanceTest;
import org.esa.snap.performance.testImplementation.WriteFromMemoryPerformanceTest;
import org.esa.snap.performance.util.Parameters;
import org.esa.snap.performance.testImplementation.PerformanceTest;
import org.esa.snap.performance.util.PerformanceTestDefinition;
import org.esa.snap.performance.util.PerformanceTestFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class PerformanceTestFactoryTest {

    @Test
    @STTM("SNAP-3712")
    public void createPerformanceTests() {
        List<PerformanceTestDefinition> testDefinitions = new ArrayList<>();

        PerformanceTestDefinition readTestDefinition = new PerformanceTestDefinition(
                "test1",
                "read-test",
                new Parameters(List.of("product1.nc", "product2.nc"), "/testDir", "/output", "single", true,true, 3, List.of("NETCDF4", "BEAM-DIMAP"), true, true, true)
        );

        PerformanceTestDefinition readTestDefinition2 = new PerformanceTestDefinition(
                "test2",
                "write-from-memory-test",
                new Parameters(List.of("product3.nc"), "/testDir", "/output", "multi", true,false, 5, List.of("ZNAP", "BEAM-DIMAP"), false, true, false)
        );

        PerformanceTestDefinition writeTestDefinition = new PerformanceTestDefinition(
                "test3",
                "write",
                new Parameters(List.of("product3.nc"), "/testDir", "/output", "multi", true,false, 5, List.of("ZNAP", "BEAM-DIMAP"), false, true, false)
        );

        testDefinitions.add(readTestDefinition);
        testDefinitions.add(readTestDefinition2);

        List<PerformanceTest> performanceTests = PerformanceTestFactory.createPerformanceTests(testDefinitions);

        assertEquals(2, performanceTests.size());
        assertTrue(performanceTests.get(0) instanceof ReadPerformanceTest);
        assertTrue(performanceTests.get(1) instanceof WriteFromMemoryPerformanceTest);
    }
}