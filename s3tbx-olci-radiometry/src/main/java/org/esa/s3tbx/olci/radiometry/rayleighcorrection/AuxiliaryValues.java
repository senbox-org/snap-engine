/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

/**
 * @author muhammad.bc.
 */
class AuxiliaryValues {

    double[] sunZenithAngles;
    double[] viewZenithAngles;
    double[] sunAzimuthAngles;
    double[] viewAzimuthAngles;
    double[] altitudes;
    double[] seaLevels;
    double[] totalOzones;
    double[] latitudes;
    double[] solarFluxs;
    double[] lambdaSource;
    double[] sourceSampleRad;
    int sourceBandIndex;

    public double[] getSunZenithAngles() {
        return sunZenithAngles;
    }

    public double[] getViewZenithAngles() {
        return viewZenithAngles;
    }

    public double[] getSunAzimuthAngles() {
        return sunAzimuthAngles;
    }

    public double[] getLatitudes() {
        return latitudes;
    }

    public double[] getViewAzimuthAngles() {
        return viewAzimuthAngles;
    }

    public double[] getAltitudes() {
        return altitudes;
    }

    public double[] getSeaLevels() {
        return seaLevels;
    }

    public double[] getTotalOzones() {
        return totalOzones;
    }


    public double[] getSolarFluxs() {
        return solarFluxs;
    }

    public double[] getLambdaSource() {
        return lambdaSource;
    }

    public double[] getSourceSampleRad() {
        return sourceSampleRad;
    }

    public int getSourceBandIndex() {
        return sourceBandIndex;
    }

    public void setSunZenithAngles(double[] sunZenithAngles) {
        this.sunZenithAngles = sunZenithAngles;
    }

    public void setViewZenithAngles(double[] viewZenithAngles) {
        this.viewZenithAngles = viewZenithAngles;
    }

    public void setSunAzimuthAngles(double[] sunAzimuthAngles) {
        this.sunAzimuthAngles = sunAzimuthAngles;
    }

    public void setViewAzimuthAngles(double[] viewAzimuthAngles) {
        this.viewAzimuthAngles = viewAzimuthAngles;
    }

    public void setAltitudes(double[] altitudes) {
        this.altitudes = altitudes;
    }

    public void setSeaLevels(double[] seaLevels) {
        this.seaLevels = seaLevels;
    }

    public void setTotalOzones(double[] totalOzones) {
        this.totalOzones = totalOzones;
    }

    public void setLatitudes(double[] latitudes) {
        this.latitudes = latitudes;
    }

    public void setSolarFluxs(double[] solarFluxs) {
        this.solarFluxs = solarFluxs;
    }

    public void setLambdaSource(double[] lambdaSource) {
        this.lambdaSource = lambdaSource;
    }

    public void setSourceSampleRad(double[] sourceSampleRad) {
        this.sourceSampleRad = sourceSampleRad;
    }

    public void setSourceBandIndex(int sourceBandIndex) {
        this.sourceBandIndex = sourceBandIndex;
    }

}
