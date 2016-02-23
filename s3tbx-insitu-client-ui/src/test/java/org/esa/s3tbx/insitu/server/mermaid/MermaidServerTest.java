package org.esa.s3tbx.insitu.server.mermaid;

import org.esa.s3tbx.insitu.server.InSituServer;
import org.esa.s3tbx.insitu.server.InSituServerRegistry;
import org.esa.s3tbx.insitu.server.InSituServerSpi;
import org.junit.Ignore;
import org.junit.Test;


/**
 * @author Marco Peters
 */
@Ignore("Should not run with automatic tests")
public class MermaidServerTest {

    @Test
    public void testMermaidServer() throws Exception {
        final InSituServerRegistry registry = InSituServerRegistry.getInstance();
        final InSituServerSpi serverSpi = registry.getRegisteredServers("MERMAID");
        final InSituServer server = serverSpi.createServer();

    }
}