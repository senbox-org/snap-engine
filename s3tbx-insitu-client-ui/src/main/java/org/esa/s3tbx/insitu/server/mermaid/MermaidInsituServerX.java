package org.esa.s3tbx.insitu.server.mermaid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.esa.s3tbx.insitu.server.InsituResponse;
import org.esa.s3tbx.insitu.server.InsituServerSpiX;
import org.esa.s3tbx.insitu.server.InsituServerX;
import org.esa.s3tbx.insitu.server.Query;

import java.net.URL;
import java.util.Date;

/**
 * @author Marco Peters
 */
public class MermaidInsituServerX implements InsituServerX {

    private static final String SERVER_URL_STRING = "http://mermaid.acri.fr/s3tbx/v1/";
    private final URL baseURL;
    private final Gson gson;

    private MermaidInsituServerX(URL baseURL) {
        this.baseURL = baseURL;
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Date.class, new UtcDateTypeAdapter());
        gson = gsonBuilder.create();
    }

    @Override
    public InsituResponse query(Query builder) {
        return null;
    }

    public static class Spi implements InsituServerSpiX {

        @Override
        public String getName() {
            return "MERMAID";
        }

        @Override
        public String getDescription() {
            return "A server giving access to the MERMAID in-situ database.";
        }

        @Override
        public InsituServerX createServer() throws Exception {
            return new MermaidInsituServerX(new URL(SERVER_URL_STRING));
        }
    }
}
