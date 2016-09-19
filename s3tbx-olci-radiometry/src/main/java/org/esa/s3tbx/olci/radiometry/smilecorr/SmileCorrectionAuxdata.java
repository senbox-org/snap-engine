/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s3tbx.olci.radiometry.smilecorr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.CsvReader;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.StringTokenizer;

public class SmileCorrectionAuxdata {


    private int[] bandIndices;
    private boolean[] landRefCorrectionSwitches;
    private boolean[] waterRefCorrectionSwitches;
    private int[] waterLowerBands;
    private int[] waterUpperBands;
    private int[] landLowerBands;
    private int[] landUpperBands;
    private float[] refCentralWaveLengths;
    private float[] solarIrradiances;


    public SmileCorrectionAuxdata() {
        List<String[]> loadAuxdata = null;
        try {
            loadAuxdata = loadAuxdata();
        } catch (IOException e) {
            e.printStackTrace();
        }
        final double[][] tableValue = auxDataInFlatTable(loadAuxdata);
        final int n = tableValue.length;

        bandIndices = new int[n];
        waterLowerBands = new int[n];
        waterUpperBands = new int[n];
        landLowerBands = new int[n];
        landUpperBands = new int[n];
        landRefCorrectionSwitches = new boolean[n];
        waterRefCorrectionSwitches = new boolean[n];
        solarIrradiances = new float[n];
        refCentralWaveLengths = new float[n];

        for (int i = 0; i < tableValue.length; i++) {
            double[] row = tableValue[i];
            bandIndices[i] = (int)row[0];
            waterRefCorrectionSwitches[i] = row[1] != 0.0;
            waterLowerBands[i] = (int) row[2];
            waterUpperBands[i] = (int) row[3];
            landRefCorrectionSwitches[i] = row[4] != 0.0;
            landLowerBands[i] = (int) row[5];
            landUpperBands[i] = (int) row[6];
            refCentralWaveLengths[i] = (float)row[7];
            solarIrradiances[i] = (float) row[8];
        }
    }


    public int[] getBandIndices() {
        return bandIndices;
    }

    public boolean[] getWaterRefCorrectionSwitches() {
        return waterRefCorrectionSwitches;
    }

    public boolean[] getLandRefCorrectionSwitches() {
        return landRefCorrectionSwitches;
    }

    public int[] getWaterLowerBands() {return waterLowerBands; }

    public int[] getWaterUpperBands() {
        return waterUpperBands;
    }

    public int[] getLandLowerBands() { return landLowerBands; }

    public int[] getLandUpperBands() {
        return landUpperBands;
    }

    public float[] getRefCentralWaveLengths() {
        return refCentralWaveLengths;
    }

    public float[] getSolarIrradiances() {
        return solarIrradiances;
    }


    public static List<String[]> loadAuxdata() throws IOException {
        final Path auxdataDir = installAuxdata();
        List<String[]> readStringRecords;
        try (CsvReader csvReader = new CsvReader(new FileReader(auxdataDir.resolve("reflconfig.txt").toString()), new char[]{'|', '\t'})) {
            readStringRecords = csvReader.readStringRecords();
        }
        return readStringRecords;
    }


    static Path installAuxdata() throws IOException {
        Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("olci/smile");
        final Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(SmileCorrectionAuxdata.class).resolve("auxdata/smile");
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);
        resourceInstaller.install(".*", ProgressMonitor.NULL);
        return auxdataDirectory;
    }

    public static double[][] auxDataInFlatTable(List<String[]> loadAuxdata) {
        double[][] tableEntry = new double[loadAuxdata.size()][9];
        for (int i = 1; i < loadAuxdata.size(); i++) {
            final String indexValue = loadAuxdata.get(i)[0];
            final StringTokenizer stringTokenizer = new StringTokenizer(indexValue, " \t", false);
            int column = 0;
            while (stringTokenizer.hasMoreElements()) {
                String token = stringTokenizer.nextToken();
                tableEntry[i - 1][column] = Double.valueOf(token);
                column++;
            }
        }

        return tableEntry;
    }
}
