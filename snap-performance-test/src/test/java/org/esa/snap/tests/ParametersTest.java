package org.esa.snap.tests;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.performance.util.Parameters;
import org.junit.Test;

import java.util.Arrays;
import java.util.Properties;

import static org.junit.Assert.*;

public class ParametersTest {

    @Test
    @STTM("SNAP-3712")
    public void testParseParametersValidInput() {
        Properties props = new Properties();
        props.setProperty("testDataDir", "testData");
        props.setProperty("test1.products", "product1,product2");
        props.setProperty("test1.threading", "multi");
        props.setProperty("test1.discardFirstMeasure", "false");
        props.setProperty("test1.numExecutionsForAverageOperations", "5");
        props.setProperty("test1.outputFormats", "format1,format2");
        props.setProperty("test1.actions", "timeAverage,maxMemoryConsumption");

        Parameters params = Parameters.parseParameters("test1", "output", props);

        assertEquals("testData", params.getTestDir());
        assertEquals(Arrays.asList("product1", "product2"), params.getProducts());
        assertEquals("multi", params.getThreading());
        assertFalse(params.isDiscardFirstMeasure());
        assertEquals(5, params.getNumExecutionsForAverageOperations());
        assertEquals(Arrays.asList("format1", "format2"), params.getOutputFormats());
        assertTrue(params.isUseTimeAverage());
        assertTrue(params.isUseMaxMemoryConsumption());
        assertFalse(params.isUseThroughput());
    }

    @Test(expected = IllegalArgumentException.class)
    @STTM("SNAP-3712")
    public void testParseParametersMissingTestDataDir() {
        Properties props = new Properties();
        props.setProperty("test1.numExecutionsForAverageOperations", "5");
        props.setProperty("test1.actions", "timeAverage");

        Parameters.parseParameters("test1", "output", props);
    }

    @Test(expected = IllegalArgumentException.class)
    @STTM("SNAP-3712")
    public void testParseParametersMissingProducts() {
        Properties props = new Properties();
        props.setProperty("testData", "testData");
        props.setProperty("test1.numExecutionsForAverageOperations", "5");
        props.setProperty("test1.actions", "timeAverage");

        Parameters.parseParameters("test1", "output", props);
    }

    @Test
    @STTM("SNAP-3712")
    public void testParseParametersDefaultValues() {
        Properties props = new Properties();
        props.setProperty("testDataDir", "testData");
        props.setProperty("test1.products", "product1");
        props.setProperty("test1.numExecutionsForAverageOperations", "3");
        props.setProperty("test1.actions", "timeAverage");

        Parameters params = Parameters.parseParameters("test1", "output", props);

        assertEquals("testData", params.getTestDir());
        assertEquals(Arrays.asList("product1"), params.getProducts());
        assertEquals("Single", params.getThreading());
        assertTrue(params.isDiscardFirstMeasure());
        assertEquals(3, params.getNumExecutionsForAverageOperations());
        assertNull(params.getOutputFormats());
        assertTrue(params.isUseTimeAverage());
        assertFalse(params.isUseMaxMemoryConsumption());
        assertFalse(params.isUseThroughput());
    }

    @Test(expected = IllegalArgumentException.class)
    @STTM("SNAP-3712")
    public void testParseParametersMissingNumExecutions() {
        Properties props = new Properties();
        props.setProperty("testDataDir", "testData");
        props.setProperty("test1.products", "product1");
        props.setProperty("test1.actions", "timeAverage");

        Parameters.parseParameters("test1", "output", props);
    }

    @Test(expected = IllegalArgumentException.class)
    @STTM("SNAP-3712")
    public void testParseParametersInvalidNumExecutions() {
        Properties props = new Properties();
        props.setProperty("testDataDir", "testData");
        props.setProperty("test1.products", "product1");
        props.setProperty("test1.numExecutionsForAverageOperations", "invalid");
        props.setProperty("test1.actions", "timeAverage");

        Parameters.parseParameters("test1", "output", props);
    }

    @Test
    @STTM("SNAP-3712")
    public void testParseParametersEmptyActions() {
        Properties props = new Properties();
        props.setProperty("testDataDir", "testData");
        props.setProperty("test1.products", "product1");
        props.setProperty("test1.numExecutionsForAverageOperations", "5");
        props.setProperty("test1.actions", "");

        Parameters params = Parameters.parseParameters("test1", "output", props);

        assertFalse(params.isUseTimeAverage());
        assertFalse(params.isUseMaxMemoryConsumption());
        assertFalse(params.isUseThroughput());
    }

    @Test(expected = NullPointerException.class)
    @STTM("SNAP-3712")
    public void testParseParametersMissingActions() {
        Properties props = new Properties();
        props.setProperty("testDataDir", "testData");
        props.setProperty("test1.products", "product1");
        props.setProperty("test1.numExecutionsForAverageOperations", "5");

        Parameters.parseParameters("test1", "output", props);
    }

    @Test
    @STTM("SNAP-3712")
    public void testParseAction() {
        String[] actions = {"timeAverage", "maxMemoryConsumption", "throughput"};

        assertTrue(Parameters.parseAction(actions, "timeAverage"));
        assertTrue(Parameters.parseAction(actions, "maxMemoryConsumption"));
        assertTrue(Parameters.parseAction(actions, "throughput"));
        assertFalse(Parameters.parseAction(actions, "nonexistentAction"));
    }

    @Test
    @STTM("SNAP-3712")
    public void testParseActionWithEmptyArray() {
        String[] actions = {};

        assertFalse(Parameters.parseAction(actions, "timeAverage"));
    }

}