package org.esa.s3tbx.insitu.server;

import org.esa.s3tbx.insitu.TestInsituServer;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Marco Peters
 */
public class InsituServerTest {

    @Test
    public void testMethod() throws Exception {
        final InsituServer server = new TestInsituServer.Spi().createServer();
        final Query builder = new Query();
        builder.subject("interest");
        builder.campaign("Muscheln");
        builder.latMin(-10.943);
        builder.latMax(46.12);
        builder.lonMin(5.0);
        builder.lonMax(15.36);
        final InsituResponse response = server.query(builder);

        Assert.assertNotNull(response);

    }
}