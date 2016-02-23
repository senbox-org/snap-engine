package org.esa.s3tbx.insitu.server;

import java.util.Date;

/**
 * @author Marco Peters
 */
public class Query {

    protected String subject;
    protected Double lonMin;
    protected Double latMin;
    protected Double lonMax;
    protected Double latMax;
    protected Date startDate;
    protected Date stopDate;
    protected String[] param;
    protected String campaign;
    protected int shift;
    protected int limit;
    protected boolean countOnly;

    public void subject(String subject) {
        this.subject = subject;
    }

    public String subject() {
        return subject;
    }

    public void lonMin(Double lonMin) {
        this.lonMin = lonMin;
    }

    public Double lonMin() {
        return lonMin;
    }

    public void latMin(Double latMin) {
        this.latMin = latMin;
    }

    public Double latMin() {
        return latMin;
    }

    public void lonMax(Double lonMax) {
        this.lonMax = lonMax;
    }

    public Double lonMax() {
        return lonMax;
    }

    public void latMax(Double latMax) {
        this.latMax = latMax;
    }

    public Double latMax() {
        return latMax;
    }

    public void startDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date startDate() {
        return startDate;
    }

    public void stopDate(Date stopDate) {
        this.stopDate = stopDate;
    }

    public Date stopDate() {
        return stopDate;
    }

    public void param(String[] parameters) {
        this.param = parameters;
    }

    public String[] param() {
        return param;
    }

    public void campaign(String campaign) {
        this.campaign = campaign;
    }

    public String campaign() {
        return campaign;
    }

    public void shift(int shift) {
        this.shift = shift;
    }

    public int shift() {
        return shift;
    }

    public void limit(int limit) {
        this.limit = limit;
    }

    public int limit() {
        return limit;
    }

    public void countOnly(boolean countOnly) {
        this.countOnly = countOnly;
    }

    public boolean countOnly() {
        return countOnly;
    }

}
