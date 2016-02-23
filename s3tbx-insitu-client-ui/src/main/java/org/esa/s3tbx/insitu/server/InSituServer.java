package org.esa.s3tbx.insitu.server;

/**
 * @author Marco Peters
 */
public interface InSituServer {

    InsituResponse query(Query query);

}
