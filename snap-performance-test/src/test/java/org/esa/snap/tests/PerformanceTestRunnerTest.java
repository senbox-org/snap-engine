package org.esa.snap.tests;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.performance.testImplementation.PerformanceTest;
import org.esa.snap.performance.util.PerformanceTestResult;
import org.esa.snap.performance.util.PerformanceTestRunner;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class PerformanceTestRunnerTest {

    private PerformanceTestRunner runner;

    @Before
    public void setUp() {
        runner = new PerformanceTestRunner();
    }

    @Test
    @STTM("SNAP-3712")
    public void testRunTestsCollectsResults() throws Throwable {

        PerformanceTest mockTest1 = mock(PerformanceTest.class);
        PerformanceTest mockTest2 = mock(PerformanceTest.class);

        PerformanceTestResult mockResult1 = new PerformanceTestResult("test1", "product1", "product2", "format1", "format2", List.of("description1"), List.of(1.0), List.of(2.0), List.of("ms"));
        PerformanceTestResult mockResult2 = new PerformanceTestResult("test2", "product3", "product4", "format1", "format2", List.of("description2"), List.of(3.0), List.of(4.0), List.of("MB/s"));

        doNothing().when(mockTest1).execute();
        doNothing().when(mockTest2).execute();
        when(mockTest1.fetchResults()).thenReturn(mockResult1);
        when(mockTest2.fetchResults()).thenReturn(mockResult2);

        runner.runTests(List.of(mockTest1, mockTest2));

        List<PerformanceTestResult> results = runner.collectResults();
        assertEquals(2, results.size());
        assertEquals(mockResult1, results.get(0));
        assertEquals(mockResult2, results.get(1));

        // Verify methods were called
        verify(mockTest1).execute();
        verify(mockTest2).execute();
        verify(mockTest1).fetchResults();
        verify(mockTest2).fetchResults();
    }

    @Test
    @STTM("SNAP-3712")
    public void testRunTestsHandlesEmptyList() throws Throwable {
        runner.runTests(List.of());

        List<PerformanceTestResult> results = runner.collectResults();
        assertTrue(results.isEmpty());
    }

    @Test
    @STTM("SNAP-3712")
    public void testRunTestsHandlesFailureAndSkips() throws Throwable {
        PerformanceTest mockTest1 = mock(PerformanceTest.class);
        PerformanceTest mockTest2 = mock(PerformanceTest.class);

        PerformanceTestResult mockResult1 = new PerformanceTestResult("test1", "product1", "product2", "format1", "format2", List.of("description1"), List.of(1.0), List.of(2.0), List.of("ms"));

        doNothing().when(mockTest1).execute();
        when(mockTest1.fetchResults()).thenReturn(mockResult1);

        // Simulate failure for test2
        doThrow(new IOException("Simulated failure in test2")).when(mockTest2).execute();

        runner.runTests(List.of(mockTest1, mockTest2));

        List<PerformanceTestResult> results = runner.collectResults();

        // Verify only successful tests are in the results
        assertEquals(1, results.size());
        assertEquals(mockResult1, results.get(0));

        // Verify execution and failure handling
        verify(mockTest1).execute();
        verify(mockTest1).fetchResults();
        verify(mockTest2).execute();
        verify(mockTest2, never()).fetchResults();
    }
}
