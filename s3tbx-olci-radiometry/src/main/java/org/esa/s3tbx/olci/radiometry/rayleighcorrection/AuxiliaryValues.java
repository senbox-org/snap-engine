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

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import org.apache.commons.math3.analysis.interpolation.BicubicSplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.esa.s3tbx.olci.radiometry.smilecorr.SmileUtils;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @author muhammad.bc.
 */
public class AuxiliaryValues {

    public static final String GETASSE_30 = "GETASSE30";
    private BicubicSplineInterpolator gridInterpolator;
    private PolynomialSplineFunction interpolate;
    private double[] tau_ray;
    private double[] sunZenithAngles;
    private double[] viewZenithAngles;
    private double[] sunAzimuthAngles;
    private double[] viewAzimuthAngles;
    private double[] seaLevels;
    private double[] totalOzones;
    private double[] latitudes;
    private double[] solarFluxs;
    private double[] lambdaSource;
    private double[] sourceSampleRad;
    private int sourceBandIndex;
    private ElevationModel elevationModel;


    private double[] thetas;
    private double[][][] rayCooefMatrixA;
    private double[][][] rayCooefMatrixB;
    private double[][][] rayCooefMatrixC;
    private double[][][] rayCooefMatrixD;
    private double[] rayleighThickness;
    private float waveLenght;
    private String targetBandName;
    private double[] longitude;
    private double[] altitudes;
    private Map<Integer, double[]> fourierPoly;
    private Map<Integer, List<double[]>> interpolateMap;
    private double[] sunAzimuthAnglesRad;
    private double[] viewAzimuthAnglesRad;
    private double[] sunZenithAnglesRad;
    private double[] viewZenithAnglesRad;
    private double[] aziDiff;
    private double[] cosSZARads;
    private double[] sinOZARads;
    private double[] sinSZARads;
    private double[] cosOZARads;
    private double[] airMass;

    public static AuxiliaryValues instance;

    public static AuxiliaryValues getInstance() {
        if (Objects.isNull(instance)) {
            instance = new AuxiliaryValues(AuxiliaryValues.GETASSE_30);
        }
        return instance;
    }

    public AuxiliaryValues() {
    }

    public AuxiliaryValues(String getasse301) {
        try {
            ElevationModelDescriptor getasse30 = ElevationModelRegistry.getInstance().getDescriptor(getasse301);
            elevationModel = getasse30.createDem(Resampling.NEAREST_NEIGHBOUR);

            RayleighCorrectionAux rayleighCorrectionAux = new RayleighCorrectionAux();
            gridInterpolator = new BicubicSplineInterpolator();

            Path coeffMatrix = rayleighCorrectionAux.installAuxdata().resolve("coeffMatrix.txt");

            JSONParser jsonObject = new JSONParser();
            JSONObject parse = (JSONObject) jsonObject.parse(new FileReader(coeffMatrix.toString()));

            tau_ray = rayleighCorrectionAux.parseJSON1DimArray(parse, "tau_ray");
            thetas = rayleighCorrectionAux.parseJSON1DimArray(parse, "theta");

            ArrayList<double[][][]> ray_coeff_matrix = rayleighCorrectionAux.parseJSON3DimArray(parse, "ray_coeff_matrix");
            rayCooefMatrixA = ray_coeff_matrix.get(0);
            rayCooefMatrixB = ray_coeff_matrix.get(1);
            rayCooefMatrixC = ray_coeff_matrix.get(2);
            rayCooefMatrixD = ray_coeff_matrix.get(3);

            double[] lineSpace = getLineSpace(0, 1, 17);
            double[] rayAlbedoLuts = rayleighCorrectionAux.parseJSON1DimArray(parse, "ray_albedo_lut");
            interpolate = new LinearInterpolator().interpolate(lineSpace, rayAlbedoLuts);

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

    }

    public void setAltitudes() {
        double[] longitudes = getLongitude();
        double[] latitudes = getLatitudes();
        if (Objects.nonNull(longitudes) && Objects.nonNull(latitudes)) {
            double[] elevation = new double[latitudes.length];
            IntStream.range(0, latitudes.length).forEach(i -> {
                try {
                    elevation[i] = elevationModel.getElevation(new GeoPos(latitudes[i], longitudes[i]));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            this.altitudes = elevation;
        }
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

    public void setInterpolation() {
        Map<Integer, List<double[]>> interpolateMap = new HashMap<>();
        double[] sunZenithAngles = getSunZenithAngles();
        double[] viewZenithAngles = getViewZenithAngles();

        if (Objects.nonNull(sunZenithAngles) && Objects.nonNull(viewZenithAngles)) {
            for (int index = 0; index < sunZenithAngles.length; index++) {
                double yVal = viewZenithAngles[index];
                double xVal = sunZenithAngles[index];

                List<double[]> valueList = new ArrayList<>();
                for (int i = 0; i < rayCooefMatrixA.length; i++) {
                    double thetaMin = thetas[0];
                    double thetaMax = thetas[thetas.length - 1];

                    if (yVal > thetaMin && yVal < thetaMax) {
                        double[] values = new double[4];
                        values[0] = gridInterpolator.interpolate(thetas, thetas, rayCooefMatrixA[i]).value(xVal, yVal);
                        values[1] = gridInterpolator.interpolate(thetas, thetas, rayCooefMatrixB[i]).value(xVal, yVal);
                        values[2] = gridInterpolator.interpolate(thetas, thetas, rayCooefMatrixC[i]).value(xVal, yVal);
                        values[3] = gridInterpolator.interpolate(thetas, thetas, rayCooefMatrixD[i]).value(xVal, yVal);
                        valueList.add(values);
                    } else if (yVal <= thetaMin) {
                        valueList.add(new double[]{thetaMin, thetaMin, thetaMin, thetaMin});
                    } else if (yVal >= thetaMin) {
                        valueList.add(new double[]{thetaMax, thetaMax, thetaMax, thetaMax});
                    } else {
                        valueList.add(new double[]{0, 0, 0, 0});
                    }
                }
                interpolateMap.put(index, valueList);
            }
            this.interpolateMap = interpolateMap;
        }
    }

    public void setFourier() {
        // Fourier components of multiple scattering
        Map<Integer, double[]> fourierPoly = new HashMap<>();
        double[] sunZenithAnglesRad = getSunZenithAnglesRad();
        double[] viewZenithAnglesRad = getViewZenithAnglesRad();

        double[] cosSZARads = getCosSZARads();
        double[] cosOZARads = getCosOZARads();

        double[] sinSZARads = getSinSZARads();
        double[] sinOZARads = getSinOZARads();

        double[] sinOZA2s = getSquarePower(sinOZARads);
        double[] sinSZA2s = getSquarePower(sinSZARads);

        if (Objects.nonNull(sunZenithAnglesRad) && Objects.nonNull(viewZenithAnglesRad)) {
            for (int index = 0; index < sunZenithAnglesRad.length; index++) {
                double cosSZARad = cosSZARads[index];
                double cosOZARad = cosOZARads[index];

                double sinSZARad = sinSZARads[index];
                double sinOZARad = sinOZARads[index];

                double sinSZA2 = sinSZA2s[index];
                double sinOZA2 = sinOZA2s[index];

                double[] fourierSeries = new double[3];
                //Rayleigh Phase function, 3 Fourier terms
                fourierSeries[0] = (3.0 * 0.9587256 / 4.0 * (1 + (cosSZARad * cosSZARad) * (cosOZARad * cosOZARad) + (sinSZA2 * sinOZA2) / 2.0) + (1.0 - 0.9587256));
                fourierSeries[1] = (-3.0 * 0.9587256 / 4.0 * cosSZARad * cosOZARad * sinSZARad * sinOZARad);
                fourierSeries[2] = (3.0 * 0.9587256 / 16.0 * sinSZA2 * sinOZA2);

                fourierPoly.put(index, fourierSeries);
            }
            this.fourierPoly = fourierPoly;

        }
    }

    public void setWaveLenght(float waveLenght) {
        this.waveLenght = waveLenght;
    }

    public void setSourceBandName(String targetBandName) {
        this.targetBandName = targetBandName;
    }

    public void setLongitude(double[] longitude) {
        this.longitude = longitude;
    }

    public void setSunAzimuthAnglesRad() {
        double[] sunAzimuthAngles = getSunAzimuthAngles();
        if (Objects.nonNull(sunAzimuthAngles)) {
            sunAzimuthAnglesRad = SmileUtils.convertDegreesToRadians(sunAzimuthAngles);
        }
    }

    public void setViewAzimuthAnglesRad() {
        double[] viewAzimuthAngles = getViewAzimuthAngles();
        if (Objects.nonNull(viewAzimuthAngles)) {
            viewAzimuthAnglesRad = SmileUtils.convertDegreesToRadians(viewAzimuthAngles);
        }
    }

    public void setSunZenithAnglesRad() {
        double[] sunZenithAngles = getSunZenithAngles();
        if (Objects.nonNull(sunZenithAngles)) {
            sunZenithAnglesRad = SmileUtils.convertDegreesToRadians(sunZenithAngles);
        }
    }

    public void setViewZenithAnglesRad() {
        double[] viewZenithAngles = getViewZenithAngles();
        if (Objects.nonNull(viewZenithAngles)) {
            viewZenithAnglesRad = SmileUtils.convertDegreesToRadians(viewZenithAngles);
        }
    }

    public void setAirMass() {
        airMass = SmileUtils.getAirMass(this);
    }

    public void setAziDifferent() {
        aziDiff = SmileUtils.getAziDiff(this);
    }

    public void setCosSZARads() {
        double[] sunZenithAnglesRad = getSunZenithAnglesRad();
        if (Objects.nonNull(sunZenithAnglesRad)) {
            cosSZARads = Arrays.stream(sunZenithAnglesRad).map(p -> Math.cos(p)).toArray();
        }
    }

    public void setCosOZARads() {
        double[] zenithAnglesRad = getViewZenithAnglesRad();
        if (Objects.nonNull(zenithAnglesRad)) {
            cosOZARads = Arrays.stream(zenithAnglesRad).map(p -> Math.cos(p)).toArray();
        }
    }

    public void setSinSZARads() {
        double[] sunZenithAnglesRad = getSunZenithAnglesRad();
        if (Objects.nonNull(sunZenithAnglesRad)) {
            sinSZARads = Arrays.stream(sunZenithAnglesRad).map(p -> Math.sin(p)).toArray();
        }
    }

    public void setSinOZARads() {
        double[] zenithAnglesRad = getViewZenithAnglesRad();
        if (Objects.nonNull(zenithAnglesRad)) {
            sinOZARads = Arrays.stream(zenithAnglesRad).map(p -> Math.sin(p)).toArray();
        }
    }

    public void setRayleighThickness(double[] rayleighThickness) {
        this.rayleighThickness = rayleighThickness;
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

    public double[] getAltitudes() {
        return altitudes;
    }

    public float getWaveLenght() {
        return waveLenght;
    }

    public Map<Integer, List<double[]>> getInterpolation() {
        if (Objects.nonNull(interpolate)) {
            return interpolateMap;
        }
        throw new NullPointerException("The interpolation is empty.");
    }

    public Map<Integer, double[]> getFourier() {
        if (Objects.nonNull(fourierPoly)) {
            return fourierPoly;
        }
        throw new NullPointerException("The Fourier polynomial is empty.");
    }

    public String getTargetBandName() {
        return targetBandName;
    }

    public double[] getLongitude() {
        return longitude;
    }

    //todo mb/*** write a test
    private double[] getSquarePower(double[] sinOZARads) {
        if (Objects.nonNull(sinOZARads)) {
            return Arrays.stream(sinOZARads).map(p -> Math.pow(p, 2)).toArray();
        }
        throw new NullPointerException("The array is null.");
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
        if (Objects.nonNull(viewZenithAnglesRad)) {
            return viewZenithAnglesRad;
        }
        throw new NullPointerException("The view zenith angles is null.");
    }

    public double[] getAirMass() {
        if (Objects.nonNull(airMass)) {
            return airMass;
        }
        throw new NullPointerException("The Airmass is null.");
    }

    public double[] getAziDifferent() {
        return aziDiff;
    }

    public double[] getCosSZARads() {
        if (Objects.nonNull(cosSZARads)) {
            return cosSZARads;
        }
        throw new NullPointerException("The sun zenith angles is null.");
    }

    public double[] getCosOZARads() {
        if (Objects.nonNull(cosOZARads)) {
            return cosOZARads;
        }
        throw new NullPointerException("The view zenith angles is null.");
    }

    public double[] getSinSZARads() {
        if (Objects.nonNull(sinSZARads)) {
            return sinSZARads;
        }
        throw new NullPointerException("The sun zenith angles is null.");
    }

    public double[] getSinOZARads() {
        if (Objects.nonNull(sinOZARads)) {
            return sinOZARads;
        }
        throw new NullPointerException("The view zenith angles is null.");
    }

    public double[] getTaur() {
        return tau_ray;
    }

    public double[] getRayleighThickness() {
        return rayleighThickness;
    }

    public double[] getInterpolateRayleighThickness() {
        double[] taur = getRayleighThickness();
        if (Objects.nonNull(taur)) {
            double[] val = new double[taur.length];
            for (int i = 0; i < taur.length; i++) {
                val[i] = interpolate.value(taur[i]);
            }
            return val;
        }
        throw new NullPointerException("The interpolate Rayleigh thickness is empty.");
    }

    double[] getLineSpace(double start, double end, int interval) {
        if (interval < 0) {
            throw new NegativeArraySizeException("Array must not have negative index");
        }
        double[] temp = new double[interval];
        double steps = (end - start) / (interval - 1);
        for (int i = 0; i < temp.length; i++) {
            temp[i] = steps * i;
        }
        return temp;
    }
}
