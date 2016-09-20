/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.s3tbx.aerosol;

/**
 *
 * @author akheckel
 */
class RetrievalResults {
    private final boolean retrievalFailed;
    private final float optAOT;
    private final float optErr;
    private final float retrievalErr;
    private final float curvature;

    public RetrievalResults(boolean retrievalFailed, float optAOT, float optErr, float retrievalErr, float curv) {
        this.retrievalFailed = retrievalFailed;
        this.optAOT = optAOT;
        this.optErr = optErr;
        this.retrievalErr = retrievalErr;
        this.curvature = curv;
    }

    public synchronized float getCurvature() {
        return curvature;
    }

    public synchronized float getOptAOT() {
        return optAOT;
    }

    public synchronized float getOptErr() {
        return optErr;
    }

    public synchronized float getRetrievalErr() {
        return retrievalErr;
    }

    public synchronized boolean isRetrievalFailed() {
        return retrievalFailed;
    }

}
