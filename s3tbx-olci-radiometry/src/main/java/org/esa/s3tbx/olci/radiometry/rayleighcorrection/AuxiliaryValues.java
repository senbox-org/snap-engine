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
import javax.media.jai.Interpolation;
import org.apache.commons.math3.analysis.interpolation.BicubicSplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.esa.s3tbx.olci.radiometry.smilecorr.SmileUtils;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.gpf.Tile;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @author muhammad.bc.
 */
public class AuxiliaryValues {

    public static final String GETASSE_30 = "GETASSE30";
    static AuxiliaryValues instance;
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
    private String sourceBandName;
    private double[] longitude;
    private double[] altitudes;
    private Map<Integer, double[]> fourierPoly;
    private Map<Integer, List<double[]>> interpolateMap;
    private double[] viewAzimuthAnglesRad;
    private double[] sunZenithAnglesRad;
    private double[] sunAzimuthAnglesRad;
    private double[] viewZenithAnglesRad;
    private double[] aziDiff;
    private double[] cosSZARads;
    private double[] sinOZARads;
    private double[] sinSZARads;
    private double[] cosOZARads;
    private double[] airMass;
    private HashMap<String, double[]> sourceTileMap;

    public AuxiliaryValues() {
    }

    public AuxiliaryValues(String getasse301) {
        try {
            ElevationModelDescriptor getasse30 = ElevationModelRegistry.getInstance().getDescriptor(getasse301);
            elevationModel = getasse30.createDem(Resampling.NEAREST_NEIGHBOUR);

            RayleighCorrectionAux rayleighCorrectionAux = new RayleighCorrectionAux();


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

            sourceTileMap = new HashMap<>();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    public void setSolarFluxs(double[] solarFluxs) {
        this.solarFluxs = solarFluxs;
    }

    public void setLambdaSource(double[] lambdaSource) {
        this.lambdaSource = lambdaSource;
    }


    double[] getTaur() {
        return tau_ray;
    }

    public static AuxiliaryValues getInstance() {
        if (Objects.isNull(instance)) {
            instance = new AuxiliaryValues(AuxiliaryValues.GETASSE_30);
        }
        return instance;
    }

    void setAltitudes() {
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
            altitudes = elevation;
        }
    }

    double[] getAltitudes() {
        return altitudes;
    }

    void setInterpolation() {
        BicubicSplineInterpolator gridInterpolator = new BicubicSplineInterpolator();
        Map<Integer, List<double[]>> interpolate = new HashMap<>();
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
                interpolate.put(index, valueList);
            }
            interpolateMap = interpolate;
        }
    }

    Map<Integer, List<double[]>> getInterpolation() {
        if (Objects.nonNull(interpolate)) {
            return interpolateMap;
        }
        throw new NullPointerException("The interpolation is empty.");
    }

    void setInterpolationSpike() {
        double[] sunZenithAngles = getSunZenithAngles();
        double[] viewZenithAngles = getViewZenithAngles();
        Map<Integer, List<double[]>> interpolate = new HashMap<>();

        Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);

        if (Objects.nonNull(sunZenithAngles) && Objects.nonNull(viewZenithAngles)) {
            for (int index = 0; index < sunZenithAngles.length; index++) {
                double yVal = viewZenithAngles[index];
                double xVal = sunZenithAngles[index];

                List<double[]> valueList = new ArrayList<>();
                for (int i = 0; i < rayCooefMatrixA.length; i++) {
                    double[] values = new double[4];
                    values[0] = interpolation.interpolate(rayCooefMatrixA[i], (float) xVal, (float) yVal);
                    values[1] = interpolation.interpolate(rayCooefMatrixB[i], (float) xVal, (float) yVal);
                    values[2] = interpolation.interpolate(rayCooefMatrixC[i], (float) xVal, (float) yVal);
                    values[3] = interpolation.interpolate(rayCooefMatrixD[i], (float) xVal, (float) yVal);
                    valueList.add(values);
                }
                interpolate.put(index, valueList);
            }
        }
        interpolateMap = interpolate;
    }

    void setFourier() {
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

    Map<Integer, double[]> getFourier() {
        if (Objects.nonNull(fourierPoly)) {
            return fourierPoly;
        }
        throw new NullPointerException("The Fourier polynomial is empty.");
    }

    void setWavelength(float waveLenght) {
        this.waveLenght = waveLenght;
    }

    double getWaveLenght() {
        return waveLenght;
    }

    void setSourceBandName(String targetBandName) {
        this.sourceBandName = targetBandName;
    }

    String getSourceBandName() {
        return sourceBandName;
    }

    void setSunAzimuthAnglesRad() {
        double[] sunAzimuthAngles = getSunAzimuthAngles();
        if (Objects.nonNull(sunAzimuthAngles)) {
            sunAzimuthAnglesRad = SmileUtils.convertDegreesToRadians(sunAzimuthAngles);
        }
    }

    double[] getSunAzimuthAnglesRad() {
        if (Objects.nonNull(sunAzimuthAnglesRad)) {
            return sunAzimuthAnglesRad;
        }
        throw new NullPointerException("The sun azimuth angles is null.");
    }

    void setViewAzimuthAnglesRad() {
        double[] viewAzimuthAngles = getViewAzimuthAngles();
        if (Objects.nonNull(viewAzimuthAngles)) {
            viewAzimuthAnglesRad = SmileUtils.convertDegreesToRadians(viewAzimuthAngles);
        }
    }

    double[] getViewAzimuthAnglesRad() {
        if (Objects.nonNull(viewAzimuthAnglesRad)) {
            return viewAzimuthAnglesRad;
        }
        throw new NullPointerException("The view azimuth angles is null.");
    }

    void setSunZenithAnglesRad() {
        double[] sunZenithAngles = getSunZenithAngles();
        if (Objects.nonNull(sunZenithAngles)) {
            sunZenithAnglesRad = SmileUtils.convertDegreesToRadians(sunZenithAngles);
        }
    }

    double[] getSunZenithAnglesRad() {
        if (Objects.nonNull(sunZenithAnglesRad)) {
            return sunZenithAnglesRad;
        }
        throw new NullPointerException("The sun zenith angles is null.");
    }

    void setViewZenithAnglesRad() {
        double[] viewZenithAngles = getViewZenithAngles();
        if (Objects.nonNull(viewZenithAngles)) {
            viewZenithAnglesRad = SmileUtils.convertDegreesToRadians(viewZenithAngles);
        }
    }

    double[] getViewZenithAnglesRad() {
        if (Objects.nonNull(viewZenithAnglesRad)) {
            return viewZenithAnglesRad;
        }
        throw new NullPointerException("The view zenith angles is null.");
    }

    void setAirMass() {
        airMass = SmileUtils.getAirMass(this.getCosOZARads(), this.getCosSZARads());
    }

    double[] getAirMass() {
        if (Objects.nonNull(airMass)) {
            return airMass;
        }
        throw new NullPointerException("The Airmass is null.");
    }

    void setAziDifferent() {
        aziDiff = SmileUtils.getAziDiff(this.getSunAzimuthAnglesRad(), this.getViewAzimuthAnglesRad());
    }

    double[] getAziDifferent() {
        return aziDiff;
    }

    void setCosSZARads() {
        double[] sunZenithAnglesRad = getSunZenithAnglesRad();
        if (Objects.nonNull(sunZenithAnglesRad)) {
            cosSZARads = Arrays.stream(sunZenithAnglesRad).map(Math::cos).toArray();
        }
    }

    double[] getCosSZARads() {
        if (Objects.nonNull(cosSZARads)) {
            return cosSZARads;
        }
        throw new NullPointerException("The sun zenith angles is null.");
    }

    void setCosOZARads() {
        double[] zenithAnglesRad = getViewZenithAnglesRad();
        if (Objects.nonNull(zenithAnglesRad)) {
            cosOZARads = Arrays.stream(zenithAnglesRad).map(Math::cos).toArray();
        }
    }

    double[] getCosOZARads() {
        if (Objects.nonNull(cosOZARads)) {
            return cosOZARads;
        }
        throw new NullPointerException("The view zenith angles is null.");
    }

    void setSinSZARads() {
        double[] sunZenithAnglesRad = getSunZenithAnglesRad();
        if (Objects.nonNull(sunZenithAnglesRad)) {
            sinSZARads = Arrays.stream(sunZenithAnglesRad).map(Math::sin).toArray();
        }
    }

    double[] getSinSZARads() {
        if (Objects.nonNull(sinSZARads)) {
            return sinSZARads;
        }
        throw new NullPointerException("The sun zenith angles is null.");
    }

    void setSinOZARads() {
        double[] zenithAnglesRad = getViewZenithAnglesRad();
        if (Objects.nonNull(zenithAnglesRad)) {
            sinOZARads = Arrays.stream(zenithAnglesRad).map(Math::sin).toArray();
        }
    }

    double[] getSinOZARads() {
        if (Objects.nonNull(sinOZARads)) {
            return sinOZARads;
        }
        throw new NullPointerException("The view zenith angles is null.");
    }

    double[] getSunZenithAngles() {
        return sunZenithAngles;
    }

    void setSunZenithAngles(Tile sourceTile) {
        this.sunZenithAngles = getSampleDoubles(sourceTile);
    }

    double[] getViewZenithAngles() {
        return viewZenithAngles;
    }

    void setViewZenithAngles(Tile sourceTile) {
        this.viewZenithAngles = getSampleDoubles(sourceTile);
    }

    double[] getSunAzimuthAngles() {
        return sunAzimuthAngles;
    }

    public void setSunAzimuthAngles(Tile sourceTile) {
        this.sunAzimuthAngles = getSampleDoubles(sourceTile);
    }

    double[] getLatitudes() {
        return latitudes;
    }

    void setLatitudes(Tile sourceTile) {
        this.latitudes = getSampleDoubles(sourceTile);
    }

    double[] getViewAzimuthAngles() {
        return viewAzimuthAngles;
    }

    void setViewAzimuthAngles(Tile sourceTile) {
        this.viewAzimuthAngles = getSampleDoubles(sourceTile);
    }

    double[] getSeaLevels() {
        return seaLevels;
    }

    void setSeaLevels(Tile sourceTile) {
        this.seaLevels = getSampleDoubles(sourceTile);
    }

    double[] getTotalOzones() {
        return totalOzones;
    }

    void setTotalOzones(Tile sourceTile) {
        this.totalOzones = getSampleDoubles(sourceTile);
    }

    double[] getSolarFluxs() {
        return solarFluxs;
    }

    void setSolarFluxs(Tile sourceTile) {
        this.solarFluxs = getSampleDoubles(sourceTile);
    }

    double[] getLambdaSource() {
        return lambdaSource;
    }

    void setLambdaSource(Tile sourceTile) {
        this.lambdaSource = getSampleDoubles(sourceTile);
    }

    double[] getSourceSampleRad() {
        return sourceSampleRad;
    }

    void setSourceSampleRad(Tile sourceTile) {
        this.sourceSampleRad = getSampleDoubles(sourceTile);
    }

    int getSourceBandIndex() {
        return sourceBandIndex;
    }

    void setSourceBandIndex(int sourceBandIndex) {
        this.sourceBandIndex = sourceBandIndex;
    }

    double[] getLongitude() {
        return longitude;
    }

    void setLongitude(Tile sourceTile) {
        this.longitude = getSampleDoubles(sourceTile);
    }

    void setRayleighThickness(double[] rayleighThickness) {
        this.rayleighThickness = rayleighThickness;
    }

    double[] getRayleighThickness() {
        return rayleighThickness;
    }

    double[] getInterpolateRayleighThickness() {
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

    //todo mb/*** write a test
    private double[] getSquarePower(double[] sinOZARads) {
        if (Objects.nonNull(sinOZARads)) {
            return Arrays.stream(sinOZARads).map(p -> Math.pow(p, 2)).toArray();
        }
        throw new NullPointerException("The array is null.");
    }

    private double[] getSampleDoubles(Tile sourceTile) {
        int maxX = sourceTile.getWidth();
        int maxY = sourceTile.getWidth();

        double[] val = new double[maxX * maxY];
        int index = 0;
        for (int y = sourceTile.getMinY(); y <=sourceTile.getMaxY(); y++) {
            for (int x = sourceTile.getMinX(); x <=sourceTile.getMaxX(); x++) {
                val[index++] = sourceTile.getSampleDouble(x, y);
            }
        }
        return val;
    }
}
