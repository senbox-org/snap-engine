package org.esa.snap.binning.operator.formatter;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IsinFormatterPluginTest {

    private IsinFormatterPlugin plugin;

    @Before
    public void setUp() {
        plugin = new IsinFormatterPlugin();
    }

    @Test
    public void testGetName() {
        assertEquals("isin", plugin.getName());
    }

    @Test
    public void testCreate() {
        final Formatter formatter = plugin.create();

        assertTrue(formatter instanceof IsinFormatter);
    }
}
