package org.esa.s3tbx.insitu.server;

import org.esa.s3tbx.insitu.TestInsituServerX;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 */
public class InsituServerRegistryXTest {

    @Test
    public void testCreation() {
        final InsituServerRegistryX registry = InsituServerRegistryX.getInstance();
        final List<InsituServerSpiX> registeredServers = registry.getRegisteredServers();
        assertTrue(registeredServers.size() >= 1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddingServerToReturnedList() {
        final InsituServerRegistryX registry = InsituServerRegistryX.getInstance();
        final List<InsituServerSpiX> registeredServers = registry.getRegisteredServers();
        registeredServers.add(new TestInsituServerX.Spi());
    }

    @Test
    public void testAddingAndRemovingServer() {
        final InsituServerRegistryX registry = InsituServerRegistryX.getInstance();
        final TestInsituServerX.Spi serverSpi = new TestInsituServerX.Spi();
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