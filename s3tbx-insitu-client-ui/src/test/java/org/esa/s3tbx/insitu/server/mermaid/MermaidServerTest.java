package org.esa.s3tbx.insitu.server.mermaid;

import org.esa.s3tbx.insitu.server.InsituServerRegistryX;
import org.esa.s3tbx.insitu.server.InsituServerSpiX;
import org.esa.s3tbx.insitu.server.InsituServerX;
import org.junit.Ignore;
import org.junit.Test;


/**
 * @author Marco Peters
 */
@Ignore("Should not run with automatic tests")
public class MermaidServerTest {

    @Test
    public void testMermaidServer() throws Exception {
        final InsituServerRegistryX registry = InsituServerRegistryX.getInstance();
        final InsituServerSpiX serverSpi = registry.getRegisteredServers("MERMAID");
        final InsituServerX server = serverSpi.createServer();

    }
}