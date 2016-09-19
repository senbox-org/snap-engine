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

package org.esa.s3tbx.olci.radiometry.rayleigh;

import com.bc.ceres.core.ProgressMonitor;
import com.google.common.primitives.Doubles;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author muhammad.bc.
 */
public class RayleighCorrectionAux {

    Path installAuxdata() throws IOException {
        Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("olci/rayleigh");
        final Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(RayleighCorrectionAux.class).resolve("auxdata/rayleigh");
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);
        resourceInstaller.install(".*", ProgressMonitor.NULL);
        return auxdataDirectory;
    }


    public double[] parseJSON1DimArray(JSONObject parse, String ray_coeff_matrix) {
        JSONArray theta = (JSONArray) parse.get(ray_coeff_matrix);
        List<Double> collect = (List<Double>) theta.stream().collect(Collectors.toList());
        return Doubles.toArray(collect);
    }

    ArrayList<double[][][]> parseJSON3DimArray(JSONObject parse, String ray_coeff_matrix) {
        JSONArray theta = (JSONArray) parse.get(ray_coeff_matrix);
        Iterator<JSONArray> iterator1 = theta.iterator();

        double[][][] rayCooffA = new double[3][12][12];
        double[][][] rayCooffB = new double[3][12][12];
        double[][][] rayCooffC = new double[3][12][12];
        double[][][] rayCooffD = new double[3][12][12];

        int k = 0;
        while (iterator1.hasNext()) { //3
            JSONArray next = iterator1.next();
            Iterator<JSONArray> iterator2 = next.iterator();
            int i1 = 0;
            while (iterator2.hasNext()) {//12
                JSONArray iterator3 = iterator2.next();
                Iterator<JSONArray> iterator4 = iterator3.iterator();
                for (int j = 0; j < 12; j++) {//12
                    JSONArray mainValue = iterator4.next();
                    List<Double> collectedValues = (List<Double>) mainValue.stream().collect(Collectors.toList());
                    rayCooffA[k][i1][j] = collectedValues.get(0);
                    rayCooffB[k][i1][j] = collectedValues.get(1);
                    rayCooffC[k][i1][j] = collectedValues.get(2);
                    rayCooffD[k][i1][j] = collectedValues.get(3);
                }
                i1++;
            }
            k++;
        }
        ArrayList<double[][][]> rayCoefficient = new ArrayList();
        rayCoefficient.add(rayCooffA);
        rayCoefficient.add(rayCooffB);
        rayCoefficient.add(rayCooffC);
        rayCoefficient.add(rayCooffD);
        return rayCoefficient;

    }
}

