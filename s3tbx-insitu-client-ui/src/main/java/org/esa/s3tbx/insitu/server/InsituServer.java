package org.esa.s3tbx.insitu.server;

/**
 * @author Marco Peters
 */
public interface InsituServer {

    String getName();

    InsituResponse query(InsituQuery query) throws InsituServerException;

}
