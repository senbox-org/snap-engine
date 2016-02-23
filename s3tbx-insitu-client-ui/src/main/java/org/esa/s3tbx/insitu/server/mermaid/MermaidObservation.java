package org.esa.s3tbx.insitu.server.mermaid;

import org.esa.s3tbx.insitu.server.InsituObservation;

import java.util.Date;

/**
 * @author Marco Peters
 */
class MermaidObservation implements InsituObservation {
    // Example:
    // {"lon":"43.367","lat":"7.9","param":"es_412","date":"2003-09-23 10:15:35","value":"748.971558"}

    private double lon;
    private double lat;
    private String param;
    private Date date;
    private double value;

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public double getLat() {
        return lat;
    }

    @Override
    public double getLon() {
        return lon;
    }

    @Override
    public String getParam() {
        return param;
    }

    @Override
    public double getValue() {
        return value;
    }
}
