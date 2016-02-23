package org.esa.s3tbx.insitu.server;

/**
 * @author Marco Peters
 */
public interface InSituServerSpi {

    String getName();

    String getDescription();

    InSituServer createServer() throws Exception;
}
