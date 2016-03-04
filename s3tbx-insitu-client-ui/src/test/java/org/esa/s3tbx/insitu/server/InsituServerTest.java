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
        final InsituQuery query = new InsituQuery().subject(InsituQuery.SUBJECT.DATASETS).datasets(new String[]{"Muscheln"});
        query.latMin(-10.943).latMax(46.12).lonMin(5.0).lonMax(15.36);
        final InsituResponse response = server.query(query);

        Assert.assertNotNull(response);

    }
}