package org.esa.s3tbx.insitu.server.mermaid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.esa.s3tbx.insitu.server.InsituQuery;
import org.esa.s3tbx.insitu.server.InsituResponse;
import org.esa.s3tbx.insitu.server.InsituServer;
import org.esa.s3tbx.insitu.server.InsituServerException;
import org.esa.s3tbx.insitu.server.InsituServerSpi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * @author Marco Peters
 */
public class MermaidInsituServer implements InsituServer {

    private static final String SERVER_NAME = "MERMAID";
    private static final String SERVER_DESCRIPTION = "A server providing access to the MERMAID in-situ database.";
    private static final String SERVER_BASE_URL_STRING = "http://mermaid.acri.fr/s3tbx/v3";
    private static final int HTTP_OK_CODE = 200;
    private final Gson gson;

    private MermaidInsituServer() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Date.class, new UtcDateTypeAdapter());
        gson = gsonBuilder.create();
    }


    @Override
    public String getName() {
        return SERVER_NAME;
    }

    @Override
    public InsituResponse query(InsituQuery query) throws InsituServerException {
        URL url = createQueryUrl(query);
        HttpURLConnection conn = null;
        try {
            conn = establishConnection(url);
            BufferedReader br = getContentReader(conn);
            return gson.fromJson(br, MermaidResponse.class);
        } finally {
           if (conn != null) {
               conn.disconnect();
           }
        }
    }

    private BufferedReader getContentReader(HttpURLConnection conn) throws InsituServerException {
        try {
            return new BufferedReader(new InputStreamReader((conn.getInputStream())));
        } catch (IOException e) {
            throw new InsituServerException("Not able to read content delivered by server", e);
        }
    }

    private HttpURLConnection establishConnection(URL url) throws InsituServerException {
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (conn.getResponseCode() != HTTP_OK_CODE) {
                throw new InsituServerException("Server response code: " + conn.getResponseCode());
            }
            return conn;
        } catch (IOException e) {
            throw new InsituServerException("Could not connect to server", e);
        }
    }

    private URL createQueryUrl(InsituQuery query) throws InsituServerException {
        try {
            return new URL(SERVER_BASE_URL_STRING + MermaidQueryFormatter.format(query));
        } catch (MalformedURLException e) {
            throw new InsituServerException("URL not valid", e);
        }
    }

    public static class Spi implements InsituServerSpi {

        @Override
        public String getName() {
            return SERVER_NAME;
        }

        @Override
        public String getDescription() {
            return SERVER_DESCRIPTION;
        }

        @Override
        public InsituServer createServer() throws InsituServerException {
            return new MermaidInsituServer();
        }
    }
}
