package org.esa.s3tbx.insitu.server;

/**
 * @author Marco Peters
 */
public interface InsituServer {

    InsituResponse query(InsituQuery query);

}
