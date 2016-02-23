package org.esa.s3tbx.insitu.server.mermaid;

import org.esa.s3tbx.insitu.server.InsituServer;
import org.esa.s3tbx.insitu.server.InsituServerRegistry;
import org.esa.s3tbx.insitu.server.InsituServerSpi;
import org.junit.Ignore;
import org.junit.Test;


/**
 * @author Marco Peters
 */
@Ignore("Should not run with automatic tests")
public class MermaidServerTest {

    @Test
    public void testMermaidServer() throws Exception {
        final InsituServerRegistry registry = InsituServerRegistry.getInstance();
        final InsituServerSpi serverSpi = registry.getRegisteredServers("MERMAID");
        final InsituServer server = serverSpi.createServer();

    }
}