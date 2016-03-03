package org.esa.s3tbx.insitu.server;

/**
 * @author Marco Peters
 */
public interface InsituServerSpi {

    String getName();

    String getDescription();

    InsituServer createServer() throws InsituServerException;
}
