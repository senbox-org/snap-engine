package org.esa.s3tbx.insitu.server;

/**
 * @author Marco Peters
 */
public interface InsituParameter {

    String getDescription();

    String getName();

    String getType();

    String getUnit();
}
