package org.esa.snap.remote.products.repository.cdse;

import java.util.LinkedHashMap;
import java.util.Map;

class CdseHttpRequest {

    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final String body;

    CdseHttpRequest(String method, String url) {
        this(method, url, Map.of(), null);
    }

    CdseHttpRequest(String method, String url, Map<String, String> headers, String body) {
        this.method = method;
        this.url = url;
        this.headers = new LinkedHashMap<>(headers);
        this.body = body;
    }

    String getMethod() {
        return method;
    }

    String getUrl() {
        return url;
    }

    Map<String, String> getHeaders() {
        return headers;
    }

    String getBody() {
        return body;
    }
}
