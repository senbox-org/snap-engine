package org.esa.s3tbx.insitu;

import org.esa.snap.core.datamodel.ProductData;

/**
 * @author Marco Peters
 */
public class QueryBuilder {

    private String subject;
    private double lonMin;
    private double latMin;
    private double lonMax;
    private double latMax;
    private ProductData.UTC startDate;
    private ProductData.UTC stopDate;
    private String[] parameters;
    private String campaign;
    private int shift;
    private int limit;
    private boolean countOnly;


    public QueryBuilder(String subject) {
        this.subject = subject;
    }

    public void lonMin(double lonMin) {
        this.lonMin = lonMin;
    }

    public void latMin(double latMin) {
        this.latMin = latMin;
    }

    public void lonMax(double lonMax) {
        this.lonMax = lonMax;
    }

    public void latMax(double latMax) {
        this.latMax = latMax;
    }

    public void startDate(ProductData.UTC startDate) {
        this.startDate = startDate;
    }

    public void stopDate(ProductData.UTC stopDate) {
        this.stopDate = stopDate;
    }

    public void param(String[] parameters) {
        this.parameters = parameters;
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
