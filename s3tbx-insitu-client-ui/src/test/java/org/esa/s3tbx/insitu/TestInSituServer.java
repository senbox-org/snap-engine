package org.esa.s3tbx.insitu;

import org.esa.s3tbx.insitu.server.InSituServer;
import org.esa.s3tbx.insitu.server.InSituServerSpi;
import org.esa.s3tbx.insitu.server.InsituDataset;
import org.esa.s3tbx.insitu.server.InsituDatasetDescr;
import org.esa.s3tbx.insitu.server.InsituParameter;
import org.esa.s3tbx.insitu.server.InsituResponse;
import org.esa.s3tbx.insitu.server.Query;

import java.util.List;

/**
 * @author Marco Peters
 */
public class TestInSituServer implements InSituServer {


    private TestInSituServer(){
    }

    @Override
    public InsituResponse query(Query query) {
        return new DummyResponse();
    }

    public static class Spi implements InSituServerSpi {

        @Override
        public String getName() {
            return "TEST";
        }

        @Override
        public String getDescription() {
            return "Server for testing";
        }

        @Override
        public InSituServer createServer() throws Exception {
            return new TestInSituServer();
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
        public List<? extends InsituDatasetDescr> getCampaignDescriptions() {
            return null;
        }

        @Override
        public List<? extends InsituDataset> getCampaignList() {
            return null;
        }
    }
}
