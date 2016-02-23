package org.esa.s3tbx.insitu;

import org.esa.s3tbx.insitu.server.InSituServer;
import org.esa.s3tbx.insitu.server.InSituServerSpi;
import org.esa.s3tbx.insitu.server.InsituResponse;
import org.esa.s3tbx.insitu.server.QueryBuilder;

/**
 * @author Marco Peters
 */
public class TestInSituServer implements InSituServer {


    private TestInSituServer(){
    }

    @Override
    public InsituResponse query(QueryBuilder query) {
        return null;
    }

    @Override
    public QueryBuilder getQueryBuilder() {
        return null;
    }


    public static class Spi implements InSituServerSpi {

        @Override
        public String getName() {
            return "TEST";
        }

        @Override
        public InSituServer createServer() throws Exception {
            return new TestInSituServer();
        }
    }
}
