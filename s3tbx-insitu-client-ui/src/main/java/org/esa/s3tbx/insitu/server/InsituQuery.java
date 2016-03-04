package org.esa.s3tbx.insitu.server;

import java.util.Date;

/**
 * @author Marco Peters
 */
public class InsituQuery {

    public enum SUBJECT {
        DATASETS,
        PARAMETERS,
        OBSERVATIONS;
    }

    protected SUBJECT subject;
    protected Double lonMin;
    protected Double latMin;
    protected Double lonMax;
    protected Double latMax;
    protected Date startDate;
    protected Date stopDate;
    protected String[] param;
    protected String[] datasets;
    protected int shift;
    protected int limit;
    protected boolean countOnly;

    public InsituQuery subject(SUBJECT subject) {
        this.subject = subject;
        return this;
    }

    public SUBJECT subject() {
        return subject;
    }

    public InsituQuery lonMin(Double lonMin) {
        this.lonMin = lonMin;
        return this;
    }

    public Double lonMin() {
        return lonMin;
    }

    public InsituQuery latMin(Double latMin) {
        this.latMin = latMin;
        return this;        
    }

    public Double latMin() {
        return latMin;
    }

    public InsituQuery lonMax(Double lonMax) {
        this.lonMax = lonMax;
        return this;
    }

    public Double lonMax() {
        return lonMax;
    }

    public InsituQuery latMax(Double latMax) {
        this.latMax = latMax;
        return this;
    }

    public Double latMax() {
        return latMax;
    }

    public InsituQuery startDate(Date startDate) {
        this.startDate = startDate;
        return this;
    }

    public Date startDate() {
        return startDate;
    }

    public InsituQuery stopDate(Date stopDate) {
        this.stopDate = stopDate;
        return this;
    }

    public Date stopDate() {
        return stopDate;
    }

    public InsituQuery param(String[] parameters) {
        this.param = parameters;
        return this;
    }

    public String[] param() {
        return param;
    }

    public InsituQuery datasets(String[] datasets) {
        this.datasets = datasets;
        return this;
    }

    public String[] datasets() {
        return datasets;
    }

    public InsituQuery shift(int shift) {
        this.shift = shift;
        return this;
    }

    public int shift() {
        return shift;
    }

    public InsituQuery limit(int limit) {
        this.limit = limit;
        return this;
    }

    public int limit() {
        return limit;
    }

    public InsituQuery countOnly(boolean countOnly) {
        this.countOnly = countOnly;
        return this;
    }

    public boolean countOnly() {
        return countOnly;
    }

}
