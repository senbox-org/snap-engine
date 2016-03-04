package org.esa.s3tbx.insitu;

import org.esa.s3tbx.insitu.server.InsituDataset;
import org.esa.s3tbx.insitu.server.InsituParameter;
import org.esa.s3tbx.insitu.server.InsituQuery;
import org.esa.s3tbx.insitu.server.InsituResponse;
import org.esa.s3tbx.insitu.server.InsituServer;
import org.esa.s3tbx.insitu.server.InsituServerException;
import org.esa.s3tbx.insitu.server.InsituServerSpi;

import java.util.List;

/**
 * @author Marco Peters
 */
public class TestInsituServer implements InsituServer {


    private TestInsituServer(){
    }

    @Override
    public String getName() {
        return "TestDummy";
    }

    @Override
    public InsituResponse query(InsituQuery query) {
        return new DummyResponse();
    }

    public static class Spi implements InsituServerSpi {

        @Override
        public String getName() {
            return "TestDummy";
        }

        @Override
        public String getDescription() {
            return "Server for testing";
        }

        @Override
        public InsituServer createServer() throws InsituServerException {
            return new TestInsituServer();
        }
    }

    private static class DummyResponse implements InsituResponse {

        @Override
        public STATUS_CODE getStatus() {
            return STATUS_CODE.OK;
        }

        @Override
        public List<String> getFailureReasons() {
            return null;
        }

        @Override
        public long getObservationCount() {
            return 0;
        }

        @Override
        public List<? extends InsituParameter> getParameters() {
            return null;
        }

        @Override
        public List<? extends InsituDataset> getDatasets() {
            return null;
        }
    }
}
