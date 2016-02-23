package org.esa.s3tbx.insitu.server;

import org.esa.snap.core.datamodel.ProductData;

/**
 * @author Marco Peters
 */
public class QueryBuilder {

    protected String subject;
    protected Double lonMin;
    protected Double latMin;
    protected Double lonMax;
    protected Double latMax;
    protected ProductData.UTC startDate;
    protected ProductData.UTC stopDate;
    protected String[] param;
    protected String campaign;
    protected int shift;
    protected int limit;
    protected boolean countOnly;

    public void subject(String subject) { this.subject = subject; }

    public void lonMin(Double lonMin) {
        this.lonMin = lonMin;
    }

    public void latMin(Double latMin) {
        this.latMin = latMin;
    }

    public void lonMax(Double lonMax) {
        this.lonMax = lonMax;
    }

    public void latMax(Double latMax) {
        this.latMax = latMax;
    }

    public void startDate(ProductData.UTC startDate) {
        this.startDate = startDate;
    }

    public void stopDate(ProductData.UTC stopDate) {
        this.stopDate = stopDate;
    }

    public void param(String[] parameters) {
        this.param = parameters;
    }

    public void campaign(String campaign) {
        this.campaign = campaign;
    }

    public void shift(int shift) {
        this.shift = shift;
    }

    public void limit(int limit) {
        this.limit = limit;
    }

    public void countOnly(boolean countOnly) {
        this.countOnly = countOnly;
    }

}
