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

package org.esa.s3tbx.owt;


import ucar.ma2.Array;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * @author Marco Peters
 */
public class HyperspectralAuxdataFactory extends AuxdataFactory {
    // todo (mp) - configuration should be turned into a configuration object

    private String covarianceMatrixResource;
    private String covarianceVarName;
    private boolean covNeedsInversion;
    private String spectralMeansResource;
    private String spectralMeansVarName;
    private int[] wlIndices;

    public HyperspectralAuxdataFactory(float[] useWavelengths, float[] allWavelengths, float maxDistance, String covarianceMatrixResource,
                                       String covarianceVarName,
                                       String spectralMeansResource, String spectralMeansVarName) {
        this(useWavelengths, allWavelengths, maxDistance, covarianceMatrixResource, covarianceVarName, true,
             spectralMeansResource, spectralMeansVarName);
    }

    public HyperspectralAuxdataFactory(float[] useWavelengths, float[] allWavelengths, float maxDistance, String covarianceMatrixResource,
                                       String covarianceVarName, boolean covNeedsInversion,
                                       String spectralMeansResource, String spectralMeansVarName) {
        this.spectralMeansVarName = spectralMeansVarName;
        this.spectralMeansResource = spectralMeansResource;
        this.wlIndices = findWavelengthIndices(useWavelengths, allWavelengths, maxDistance);
        this.covarianceMatrixResource = covarianceMatrixResource;
        this.covarianceVarName = covarianceVarName;
        this.covNeedsInversion = covNeedsInversion;
    }

    @Override
    public Auxdata createAuxdata() throws AuxdataException {
        double[][] spectralMeans = loadSpectralMeans();
        double[][][] invCovarianceMatrix = loadInvCovarianceMatrix();
        if (spectralMeans == null || invCovarianceMatrix == null) {
            throw new AuxdataException("Could not load auxiliary data");
        }
        return new Auxdata(spectralMeans, invCovarianceMatrix);
    }


    private double[][][] loadInvCovarianceMatrix() throws AuxdataException {
        double[][][] invCovarianceMatrix = null;
        try {
            NetcdfFile covMatrixFile = loadFile(covarianceMatrixResource);
            try {
                final Group rootGroup = covMatrixFile.getRootGroup();
                Variable covarianceVariable = rootGroup.findVariable(covarianceVarName);
                if (covarianceVariable == null) {
                    throw new AuxdataException(String.format("Variable with name '%s' could not be found", covarianceVarName));
                }

                final Array arrayDouble = getDoubleArray(covarianceVariable);
                double[][][] matrix = (double[][][]) arrayDouble.copyToNDJavaArray();
                // important first reduce to the wavelength and invert afterwards
                double[][][] redMatrix = reduceCovarianceMatrixToWLs(matrix, wlIndices);
                if (covNeedsInversion) {
                    invCovarianceMatrix = invertMatrix(redMatrix);
                } else {
                    invCovarianceMatrix = redMatrix;
                }
            } finally {
                covMatrixFile.close();
            }
        } catch (Exception e) {
            throw new AuxdataException("Could not load auxiliary data", e);
        }
        return invCovarianceMatrix;
    }

    static double[][][] reduceCovarianceMatrixToWLs(double[][][] covarianceMatrix, int[] useIndices) {
        double[][][] reducedMatrix = new double[covarianceMatrix.length][useIndices.length][useIndices.length];
        for (int i = 0; i < covarianceMatrix.length; i++) {
            double[][] innerCovarianceMatrix = covarianceMatrix[i];
            for (int j = 0; j < useIndices.length; j++) {
                double[] innerArray = innerCovarianceMatrix[useIndices[j]];
                for (int k = 0; k < useIndices.length; k++) {
                    reducedMatrix[i][j][k] = innerArray[useIndices[k]];
                }
            }

        }
        return reducedMatrix;
    }

    private double[][] loadSpectralMeans() throws AuxdataException {
        double[][] spectralMeans = null;
        try {
            NetcdfFile specMeansFile = loadFile(spectralMeansResource);
            try {
                final Group rootGroup = specMeansFile.getRootGroup();
                Variable specMeansVariable = rootGroup.findVariable(spectralMeansVarName);
                if (specMeansVariable == null) {
                    throw new AuxdataException(String.format("Variable with name '%s' could not be found", spectralMeansVarName));
                }

                final Array arrayDouble = getDoubleArray(specMeansVariable);
                double[][] allSpectralMeans = (double[][]) arrayDouble.copyToNDJavaArray();
                spectralMeans = reduceSpectralMeansToWLs(allSpectralMeans, wlIndices);
            } finally {
                specMeansFile.close();
            }
        } catch (Exception e) {
            throw new AuxdataException("Could not load auxiliary data", e);
        }
        return spectralMeans;
    }

    static double[][] reduceSpectralMeansToWLs(double[][] spectralMeans, int[] useIndices) {
        double[][] reducedSpectralMeans = new double[useIndices.length][];
        for (int i = 0; i < useIndices.length; i++) {
            reducedSpectralMeans[i] = spectralMeans[useIndices[i]];
        }
        return reducedSpectralMeans;
    }

}
