package org.esa.s3tbx.insitu.server;

import java.util.Date;

/**
 * @author Marco Peters
 */
public interface InsituObservation {

    Date getDate();

    double getLat();

    double getLon();

    String getParam();

    double getValue();
}
