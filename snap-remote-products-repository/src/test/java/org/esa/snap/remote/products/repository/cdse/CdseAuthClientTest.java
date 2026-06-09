package org.esa.snap.remote.products.repository.cdse;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.esa.snap.remote.products.repository.HTTPServerException;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CdseAuthClientTest {

    @Test
    public void requestsPasswordGrantAndReturnsAccessToken() throws Exception {
        RecordingHttpClient httpClient = new RecordingHttpClient();
        httpClient.enqueue(new CdseHttpResponse(200, "{\"access_token\":\"abc\"}".getBytes(StandardCharsets.UTF_8)));

        String token = new CdseAuthClient(httpClient).accessToken(new UsernamePasswordCredentials("user@example.com", "p@ ss"));

        assertEquals("abc", token);
        CdseHttpRequest request = httpClient.requests.get(0);
        assertEquals("POST", request.getMethod());
        assertEquals(CdseAuthClient.TOKEN_URL, request.getUrl());
        assertTrue(request.getHeaders().get("Content-Type").startsWith("application/x-www-form-urlencoded"));
        assertTrue(request.getBody().contains("grant_type=password"));
        assertTrue(request.getBody().contains("client_id=cdse-public"));
        assertTrue(request.getBody().contains("username=user%40example.com"));
        assertTrue(request.getBody().contains("password=p%40+ss"));
    }

    @Test
    public void rejectsTokenResponsesWithoutAccessToken() throws Exception {
        RecordingHttpClient httpClient = new RecordingHttpClient();
        httpClient.enqueue(new CdseHttpResponse(200, "{}".getBytes(StandardCharsets.UTF_8)));

        try {
            new CdseAuthClient(httpClient).accessToken(new UsernamePasswordCredentials("user", "password"));
            fail("Expected IOException");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("access_token"));
        }
    }

    @Test
    public void reportsHttpErrorsWithStatusCode() throws Exception {
        RecordingHttpClient httpClient = new RecordingHttpClient();
        httpClient.enqueue(new CdseHttpResponse(401, "unauthorized".getBytes(StandardCharsets.UTF_8)));

        try {
            new CdseAuthClient(httpClient).accessToken(new UsernamePasswordCredentials("user", "password"));
            fail("Expected HTTPServerException");
        } catch (HTTPServerException expected) {
            assertEquals(401, expected.getStatusCodeResponse());
        }
    }

    private static class RecordingHttpClient implements CdseHttpClient {
        final List<CdseHttpRequest> requests = new ArrayList<>();
        final Queue<CdseHttpResponse> responses = new ArrayDeque<>();

        void enqueue(CdseHttpResponse response) {
            responses.add(response);
        }

        @Override
        public CdseHttpResponse execute(CdseHttpRequest request) {
            requests.add(request);
            return responses.remove();
        }
    }
}
