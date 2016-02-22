package org.esa.s3tbx.insitu;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 */
public class InSituServerRegistryTest {

    @Test
    public void testCreation() {
        final InSituServerRegistry registry = InSituServerRegistry.getInstance();
        final List<InSituServerSpi> registeredServers = registry.getRegisteredServers();
        assertTrue(registeredServers.size() >= 1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddingServerToReturnedList() {
        final InSituServerRegistry registry = InSituServerRegistry.getInstance();
        final List<InSituServerSpi> registeredServers = registry.getRegisteredServers();
        registeredServers.add(new TestInSituServer.Spi());
    }

    @Test
    public void testAddingAndRemovingServer() {
        final InSituServerRegistry registry = InSituServerRegistry.getInstance();
        final TestInSituServer.Spi serverSpi = new TestInSituServer.Spi();
        final int serverCount = registry.getRegisteredServers().size();

        registry.addServer(serverSpi);
        assertEquals(serverCount + 1, registry.getRegisteredServers().size());
        registry.removeServer(serverSpi.getName());
        assertEquals(serverCount, registry.getRegisteredServers().size());

        registry.addServer(serverSpi);
        assertEquals(serverCount + 1, registry.getRegisteredServers().size());
        registry.removeServer(serverSpi);
        assertEquals(serverCount, registry.getRegisteredServers().size());
    }
}