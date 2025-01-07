package org.esa.snap.tests;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.performance.util.ConfigParser;
import org.esa.snap.performance.util.Parameters;
import org.esa.snap.performance.util.PerformanceTestDefinition;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class ConfigParserTest {

    @Test
    @STTM("SNAP-3712")
    public void testParse() throws IOException {
        ConfigParser configParser = new ConfigParser(new String[] {"TESTconfig.properties"});
        List<PerformanceTestDefinition> testDefinitions = configParser.parse();

        assertEquals(2, testDefinitions.size());

        PerformanceTestDefinition definition1 = testDefinitions.get(0);
        PerformanceTestDefinition definition2 = testDefinitions.get(1);

        assertEquals("readTest1", definition1.getTestName());
        assertEquals("read-test", definition1.getTestImplementation());

        Parameters params1 = definition1.getParameters();
        assertEquals(2, params1.getProducts().size());
        assertEquals("productA.dim", params1.getProducts().get(0));
        assertEquals("productB.znap.zip", params1.getProducts().get(1));
        assertEquals("single", params1.getThreading());
        assertFalse(params1.isDiscardFirstMeasure());
        assertEquals(26, params1.getNumExecutionsForAverageOperations());
        assertTrue(params1.isUseTimeAverage());
        assertTrue(params1.isUseMaxMemoryConsumption());
        assertFalse(params1.isUseThroughput());
        assertNull(params1.getOutputFormats());

        assertEquals("writeTest1", definition2.getTestName());
        assertEquals("write-from-memory-test", definition2.getTestImplementation());

        Parameters params2 = definition2.getParameters();
        assertEquals(1, params2.getProducts().size());
        assertEquals("productC.nc", params2.getProducts().get(0));
        assertEquals("multi", params2.getThreading());
        assertTrue(params2.isDiscardFirstMeasure());
        assertEquals(3, params2.getNumExecutionsForAverageOperations());
        assertTrue(params2.isUseTimeAverage());
        assertFalse(params2.isUseMaxMemoryConsumption());
        assertTrue(params2.isUseThroughput());
        assertEquals(2, params2.getOutputFormats().size());
    }
}
