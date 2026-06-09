package org.esa.snap.remote.products.repository.cdse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.auth.Credentials;
import org.esa.snap.remote.products.repository.HTTPServerException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

class CdseAuthClient {

    static final String TOKEN_URL = "https://identity.dataspace.copernicus.eu/auth/realms/CDSE/protocol/openid-connect/token";

    private final CdseHttpClient httpClient;
    private final ObjectMapper objectMapper;

    CdseAuthClient(CdseHttpClient httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    String accessToken(Credentials credentials) throws IOException {
        if (credentials == null || credentials.getUserPrincipal() == null) {
            throw new IllegalArgumentException("CDSE credentials are required.");
        }
        if (credentials.getPassword() == null) {
            throw new IllegalArgumentException("CDSE password is required.");
        }
        Map<String, String> headers = Map.of("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        String body = formBody(Map.of(
                "grant_type", "password",
                "client_id", "cdse-public",
                "username", credentials.getUserPrincipal().getName(),
                "password", credentials.getPassword()));
        CdseHttpResponse response = httpClient.execute(new CdseHttpRequest("POST", TOKEN_URL, headers, body));
        if (!response.isSuccessful()) {
            throw new HTTPServerException(response.getStatusCode(), response.getBodyAsString());
        }
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode accessToken = root.get("access_token");
        if (accessToken == null || accessToken.asText().isBlank()) {
            throw new IOException("CDSE token response does not contain access_token.");
        }
        return accessToken.asText();
    }

    private static String formBody(Map<String, String> values) throws UnsupportedEncodingException {
        Map<String, String> orderedValues = new LinkedHashMap<>();
        orderedValues.put("grant_type", values.get("grant_type"));
        orderedValues.put("client_id", values.get("client_id"));
        orderedValues.put("username", values.get("username"));
        orderedValues.put("password", values.get("password"));

        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> entry : orderedValues.entrySet()) {
            if (body.length() > 0) {
                body.append('&');
            }
            body.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
        }
        return body.toString();
    }
}
