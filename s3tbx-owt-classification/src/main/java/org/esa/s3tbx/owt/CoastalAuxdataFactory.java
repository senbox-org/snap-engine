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

import java.util.List;

/**
 * @author Marco Peters
 */
public class CoastalAuxdataFactory extends AuxdataFactory {

    private final String resourcePath;

    public CoastalAuxdataFactory(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public Auxdata createAuxdata() throws AuxdataException {
        NetcdfFile netcdfFile;
        try {
            netcdfFile = loadFile(resourcePath);
            try {
                final Group rootGroup = netcdfFile.getRootGroup();
                final List<Variable> variableList = rootGroup.getVariables();

                double[][] spectralMeans = null;
                double[][][] invCovarianceMatrix = null;
                for (Variable variable : variableList) {
                    if ("class_means".equals(variable.getFullName())) {
                        final Array arrayDouble = getDoubleArray(variable);
                        spectralMeans = (double[][]) arrayDouble.copyToNDJavaArray();
                    }
                    if ("class_covariance".equals(variable.getFullName()) || "Yinv".equals(variable.getFullName())) {
                        final Array arrayDouble = getDoubleArray(variable);
                        invCovarianceMatrix = invertMatrix((double[][][]) arrayDouble.copyToNDJavaArray());
                    }
                    if (spectralMeans != null && invCovarianceMatrix != null) {
                        break;
                    }
                }
                return new Auxdata(spectralMeans, invCovarianceMatrix);
            } finally {
                netcdfFile.close();
            }
        } catch (Exception e) {
            throw new AuxdataException("Could not load auxiliary data", e);
        }
    }

}
