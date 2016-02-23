package org.esa.s3tbx.insitu.server;

/**
 * @author Marco Peters
 */
public interface InSituServer {

    // todo (mp/23.02.2016) think about this concept
    InsituResponse query(QueryBuilder query);

    QueryBuilder getQueryBuilder();
}
