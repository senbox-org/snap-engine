package org.esa.s3tbx.insitu.server.mermaid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.esa.s3tbx.insitu.server.InSituServer;
import org.esa.s3tbx.insitu.server.InSituServerSpi;
import org.esa.s3tbx.insitu.server.InsituResponse;
import org.esa.s3tbx.insitu.server.QueryBuilder;

import java.net.URL;
import java.util.Date;

/**
 * @author Marco Peters
 */
public class MermaidInSituServer implements InSituServer {

    private static final String SERVER_URL_STRING = "http://mermaid.acri.fr/s3tbx/v1/";
    private final URL baseURL;
    private final Gson gson;

    private MermaidInSituServer(URL baseURL){
        this.baseURL = baseURL;
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Date.class, new UtcDateTypeAdapter());
        gson = gsonBuilder.create();
    }

     @Override
     public QueryBuilder getQueryBuilder() {
         return new MermaidQueryBuilder();
     }

    @Override
    public InsituResponse query(QueryBuilder builder) {
        return null;
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
