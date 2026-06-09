package org.esa.snap.remote.products.repository.cdse;

import java.nio.charset.StandardCharsets;

class CdseHttpResponse {

    private final int statusCode;
    private final byte[] body;

    CdseHttpResponse(int statusCode, byte[] body) {
        this.statusCode = statusCode;
        this.body = body != null ? body : new byte[0];
    }

    int getStatusCode() {
        return statusCode;
    }

    byte[] getBody() {
        return body;
    }

    String getBodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }

    boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }
}
