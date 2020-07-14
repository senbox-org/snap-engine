package org.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.Band;
import org.junit.Test;

import java.awt.Dimension;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WriteCacheTest {

    @Test
    public void testGet_createsNew() {
        final Band dataNode = mock(Band.class);
        when(dataNode.getName()).thenReturn("test_var");
        when(dataNode.getRasterSize()).thenReturn(new Dimension(512, 2056));

        final WriteCache writeCache = new WriteCache();
        final VariableCache variableCache = writeCache.get(dataNode);
        assertNotNull(variableCache);
    }

    @Test
    public void testGet_callTwiceReturnsSameObject() {
        final Band dataNode = mock(Band.class);
        when(dataNode.getName()).thenReturn("test_var");
        when(dataNode.getRasterSize()).thenReturn(new Dimension(2400, 2534));

        final WriteCache writeCache = new WriteCache();
        final VariableCache variableCache = writeCache.get(dataNode);
        assertNotNull(variableCache);

        final VariableCache otherCallCache = writeCache.get(dataNode);
        assertSame(variableCache, otherCallCache);
    }
}
