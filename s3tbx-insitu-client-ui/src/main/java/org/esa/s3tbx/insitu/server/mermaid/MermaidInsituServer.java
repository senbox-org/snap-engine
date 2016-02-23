package org.esa.s3tbx.insitu.server.mermaid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.esa.s3tbx.insitu.server.InsituResponse;
import org.esa.s3tbx.insitu.server.InsituServer;
import org.esa.s3tbx.insitu.server.InsituServerSpi;
import org.esa.s3tbx.insitu.server.Query;

import java.net.URL;
import java.util.Date;

/**
 * @author Marco Peters
 */
public class MermaidInsituServer implements InsituServer {

    private static final String SERVER_URL_STRING = "http://mermaid.acri.fr/s3tbx/v1/";
    private final URL baseURL;
    private final Gson gson;

    private MermaidInsituServer(URL baseURL) {
        this.baseURL = baseURL;
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Date.class, new UtcDateTypeAdapter());
        gson = gsonBuilder.create();
    }

    @Override
    public InsituResponse query(Query builder) {
        return null;
    }

    public static class Spi implements InsituServerSpi {

        @Override
        public String getName() {
            return "MERMAID";
        }

        @Override
        public String getDescription() {
            return "A server giving access to the MERMAID in-situ database.";
        }

        @Override
        public InsituServer createServer() throws Exception {
            return new MermaidInsituServer(new URL(SERVER_URL_STRING));
        }
    }
}
