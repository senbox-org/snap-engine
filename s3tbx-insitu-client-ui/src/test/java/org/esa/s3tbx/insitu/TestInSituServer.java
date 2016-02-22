package org.esa.s3tbx.insitu;

/**
 * @author Marco Peters
 */
public class TestInSituServer implements InSituServer{


    private TestInSituServer(){
    }

    Response queryDatabase(String query) {
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
