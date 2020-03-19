package org.esa.snap.remote.products.repository;

import java.io.IOException;

/**
 * Created by jcoravu on 27/1/2020.
 */
public class HTTPServerException extends IOException {

    public static final byte UNAUTHORIZED = 1;

    private int statusCodeResponse;

    public HTTPServerException(int statusCodeResponse, String message, Throwable cause) {
        super(message, cause);

        this.statusCodeResponse = statusCodeResponse;
    }

    public HTTPServerException(int statusCodeResponse, String message) {
        super(message);

        this.statusCodeResponse = statusCodeResponse;
    }

    public int getStatusCodeResponse() {
        return statusCodeResponse;
    }
}
