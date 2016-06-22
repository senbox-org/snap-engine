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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.util.Properties;

/**
 * @author muhammad.bc.
 */
public class RayleighCorrectionAux {

    public static Properties loadAuxdata() throws IOException {

        int _OFFSET = 1247 + 2898 + 2772 + 300 + 912;
        int _NUM_THETA = 12;
        int _ORDER = 3;
        int _NUM_COEFFS = 4;
        float[][][][] matrix = new float[_ORDER][_NUM_THETA][_NUM_THETA][_NUM_COEFFS];

        final Path auxdataDir = installAuxdata();
        File file = auxdataDir.resolve("MER_ATP_AXVACR20091126_115724_20020429_041400_20021224_121445").toFile();
        FileInputStream fileInputStream = new FileInputStream(file);
        DataInputStream dis = new DataInputStream(fileInputStream);
        dis.skip(_OFFSET);
        float i = 0;

//        while ((i = dis.readFloat()) != Float.NaN) {
        for (int j = 0; j < _ORDER; j++) {
            for (int k = 0; k < _NUM_THETA; k++) {
                for (int l = k; l < _NUM_THETA; l++) {
                    for (int m = 0; m < _NUM_COEFFS; m++) {
                        matrix[j][k][l][m] = dis.readFloat();
                        if (k != l) {
                            matrix[j][k][l][m] = matrix[j][l][k][m];
                        }
                    }
                }
            }
        }

//        }
        return null;
    }


    static Path installAuxdata() throws IOException {
        Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("olci/smile-correction");
        final Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(RayleighCorrectionAux.class).resolve("auxdata/smile");
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);
        resourceInstaller.install(".*", ProgressMonitor.NULL);
        return auxdataDirectory;
    }

}
