package org.esa.s3tbx.insitu.server;

/**
 * @author Marco Peters
 */
public interface InsituServerSpiX {

    String getName();

    String getDescription();

    InsituServerX createServer() throws Exception;
}
