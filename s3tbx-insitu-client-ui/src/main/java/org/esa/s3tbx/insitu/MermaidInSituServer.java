package org.esa.s3tbx.insitu;

import java.net.URL;

/**
 * @author Marco Peters
 */
public class MermaidInSituServer implements InSituServer{

    private static final String SERVER_URL_STRING = "http://mermaid.acri.fr/s3tbx/v1/";
    private final URL baseURL;

    private MermaidInSituServer(URL baseURL){
        this.baseURL = baseURL;
    }

    public static class Spi implements InSituServerSpi {

        @Override
        public String getName() {
            return "MERMAID";
        }

        @Override
        public InSituServer createServer() throws Exception {
            return new MermaidInSituServer(new URL(SERVER_URL_STRING));
        }
    }
}
