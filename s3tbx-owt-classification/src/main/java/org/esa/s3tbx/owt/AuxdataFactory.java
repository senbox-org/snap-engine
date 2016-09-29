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

import Jama.Matrix;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * @author Marco Peters
 */
public abstract class AuxdataFactory {

    protected static int[] findWavelengthIndices(float[] useWavelengths, float[] allWavelengths, float maxDistance) {

        ArrayList<Integer> wavelengthIdxList = new ArrayList<>();
        for (float useWavelength : useWavelengths) {
            int bestIndex = -1;
            double lastDelta = Double.MAX_VALUE;
            for (int i = 0; i < allWavelengths.length; i++) {
                float delta = Math.abs(useWavelength - allWavelengths[i]);
                if (delta <= maxDistance && delta <= lastDelta) {
                    bestIndex = i;
                } else if (delta > lastDelta) {
                    // assuming that ALL_WAVELENGTHS is sorted we can break the loop if delta increases
                    break;
                }
                lastDelta = delta;
            }
            if (bestIndex != -1) {
                wavelengthIdxList.add(bestIndex);
            } else {
                String msg = String.format("Could not find appropriate wavelength (%.3f) in auxiliary data", useWavelength);
                throw new IllegalStateException(msg);
            }
        }

        int[] result = new int[wavelengthIdxList.size()];
        for (int i = 0; i < wavelengthIdxList.size(); i++) {
            result[i] = wavelengthIdxList.get(i);
        }
        return result;
    }

    abstract Auxdata createAuxdata() throws AuxdataException;

    protected Array getDoubleArray(Variable variable) throws IOException, InvalidRangeException {
        final int[] origin = new int[variable.getRank()];
        final int[] shape = variable.getShape();
        final Array array = variable.read(new Section(origin, shape));
        return Array.factory(DataType.DOUBLE, shape, array.get1DJavaArray(Double.class));
    }

    protected static double[][][] invertMatrix(double[][][] matrix) {
        double[][][] invMatrix = new double[matrix.length][][];
        for (int i = 0; i < matrix.length; i++) {
            final Matrix tempMatrix = new Matrix(matrix[i]);
            final Matrix tempInvMatrix = tempMatrix.inverse();
            invMatrix[i] = tempInvMatrix.getArray();

        }
        return invMatrix;
    }

    protected NetcdfFile loadFile(String resourcePath) throws URISyntaxException, IOException {
        final URI resourceUri = getClass().getResource(resourcePath).toURI();
        return NetcdfFile.openInMemory(resourceUri);
    }

}
