package org.esa.s3tbx.insitu.server;

/**
 * @author Marco Peters
 */
public class InsituServerException extends Exception {

    public InsituServerException(String message) {
        super(message);
    }

    public InsituServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
