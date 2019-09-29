package org.esa.snap.binning.operator.formatter;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultFormatterPluginTest {

    private DefaultFormatterPlugin plugin;

    @Before
    public void setUp() {
        plugin = new DefaultFormatterPlugin();
    }

    @Test
    public void testGetName() {
        assertEquals("default", plugin.getName());
    }

    @Test
    public void testCreate() {
        final Formatter formatter = plugin.create();

        assertTrue(formatter instanceof DefaultFormatter);
    }
}
