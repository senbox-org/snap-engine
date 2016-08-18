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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.esa.s3tbx.olci.radiometry.smilecorr.SmileUtils;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.Resampling;

/**
 * @author muhammad.bc.
 */
public class AuxiliaryValues {

    public static final String GETASSE_30 = "GETASSE30";
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
    private ElevationModel elevationModel;


    public AuxiliaryValues() {
    }

    public AuxiliaryValues(String getasse301) {
        ElevationModelDescriptor getasse30 = ElevationModelRegistry.getInstance().getDescriptor(getasse301);
        elevationModel = getasse30.createDem(Resampling.NEAREST_NEIGHBOUR);
    }

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

    public void setAltitudes(double[] longitudes, double[] latitudes) {
        altitudes = new double[longitudes.length];
        try {
            for (int i = 0; i < latitudes.length; i++) {
                this.altitudes[i] = elevationModel.getElevation(new GeoPos(latitudes[i], longitudes[i]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public Map<Integer, List<Double[]>> getInterpolation() {
        Map<Integer, List<Double[]>> interpolateMap = new HashMap<>();

        double yVal = oza[index];
        double xVal = sza[index];

        if (yVal > thetas[0] && yVal < thetas[thetas.length - 1]) {
            for (int i = 0; i < a.length; i++) {
                a[i] = gridInterpolator.interpolate(thetas, thetas, rayCooefMatrixA[i]).value(xVal, yVal);
                b[i] = gridInterpolator.interpolate(thetas, thetas, rayCooefMatrixB[i]).value(xVal, yVal);
                c[i] = gridInterpolator.interpolate(thetas, thetas, rayCooefMatrixC[i]).value(xVal, yVal);
                d[i] = gridInterpolator.interpolate(thetas, thetas, rayCooefMatrixD[i]).value(xVal, yVal);
            }
        }

        return interpolateMap;
    }

    public Map<Integer, Double[]> getFourier() {
        // Fourier components of multiple scattering
        Map<Integer, Double[]> doubleMap = new HashMap<>();


        double[] sunZenithAnglesRad = getSunZenithAnglesRad();
        double[] viewZenithAnglesRad = getViewZenithAnglesRad();

        if (Objects.nonNull(sunZenithAnglesRad) && Objects.nonNull(viewZenithAnglesRad)) {
            for (int i = 0; i < sunZenithAnglesRad.length; i++) {


            double szaRad = szaRads[index];
            double ozaRad = ozaRads[index];

            double cosSZARad = Math.cos(szaRad);
            double cosOZARad = Math.cos(ozaRad);

            double sinSZARad = Math.sin(szaRad);
            double sinOZARad = Math.sin(ozaRad);

            double sinSZA2 = Math.pow(sinSZARad, 2);
            double sinOZA2 = Math.pow(sinOZARad, 2);


            //Rayleigh Phase function, 3 Fourier terms
            fourierSeries[0] = (3.0 * 0.9587256 / 4.0 * (1 + (cosSZARad * cosSZARad) * (cosOZARad * cosOZARad) + (sinSZA2 * sinOZA2) / 2.0) + (1.0 - 0.9587256));
            fourierSeries[1] = (-3.0 * 0.9587256 / 4.0 * cosSZARad * cosOZARad * sinSZARad * sinOZARad);
            fourierSeries[2] = (3.0 * 0.9587256 / 16.0 * sinSZA2 * sinOZA2);

            return doubleMap;
        }

    }

    public double[] getSunAzimuthAnglesRad() {
        double[] sunAzimuthAngles = getSunAzimuthAngles();
        if (Objects.nonNull(sunAzimuthAngles)) {
            return SmileUtils.convertDegreesToRadians(sunAzimuthAngles);
        }
        throw new NullPointerException("The sun azimuth angles is null.");
    }

    public double[] getViewAzimuthAnglesRad() {
        double[] viewAzimuthAngles = getViewAzimuthAngles();
        if (Objects.nonNull(viewAzimuthAngles)) {
            return SmileUtils.convertDegreesToRadians(viewAzimuthAngles);
        }
        throw new NullPointerException("The view azimuth angles is null.");
    }

    public double[] getSunZenithAnglesRad() {
        double[] sunZenithAngles = getSunZenithAngles();
        if (Objects.nonNull(sunZenithAngles)) {
            return SmileUtils.convertDegreesToRadians(sunZenithAngles);
        }
        throw new NullPointerException("The sun zenith angles is null.");
    }

    public double[] getViewZenithAnglesRad() {
        double[] viewZenithAngles = getViewZenithAngles();
        if (Objects.nonNull(viewZenithAngles)) {
            return SmileUtils.convertDegreesToRadians(viewZenithAngles);
        }
        throw new NullPointerException("The view zenith angles is null.");
    }

    public double[] getAirMass() {
        return SmileUtils.getAirMass(this);
    }

    public double[] getAziDifferent() {
        return SmileUtils.getAziDiff(this);
    }

    public double[] getCosSZARads() {
        return new double[0];
    }

    public double[] getCosOZARads() {
        return new double[0];
    }
}
