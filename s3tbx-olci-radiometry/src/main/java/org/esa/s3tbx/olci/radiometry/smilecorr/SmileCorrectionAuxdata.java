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


    private double[] bands;
    private double[] water_LowerBands;
    private double[] water_UpperBands;
    private boolean[] landRefCorrectionSwitchs;
    private double[] land_LowerBands;
    private double[] land_UpperBands;
    private double[] solarIrradiances;
    private double[] refCentralWaveLenghts;
    private boolean[] waterRefCorrectionSwitchs;


    public SmileCorrectionAuxdata()  {
        List<String[]> loadAuxdata = null;
        try {
            loadAuxdata = loadAuxdata();
        } catch (IOException e) {
            e.printStackTrace();
        }
        final double[][] tableValue = auxDataInFlatTable(loadAuxdata);
        final int n = tableValue.length;

        bands = new double[n];
        water_LowerBands = new double[n];
        water_UpperBands = new double[n];
        land_LowerBands = new double[n];
        land_UpperBands = new double[n];
        landRefCorrectionSwitchs = new boolean[n];
        waterRefCorrectionSwitchs = new boolean[n];
        solarIrradiances = new double[n];
        refCentralWaveLenghts = new double[n];

        for (int i = 0; i < tableValue.length; i++) {
            double[] row = tableValue[i];
            bands[i] = row[0];
            waterRefCorrectionSwitchs[i] = row[1] != 0.0;
            water_LowerBands[i] = row[2];
            water_UpperBands[i] = row[3];
            landRefCorrectionSwitchs[i] = row[4] != 0.0;
            land_LowerBands[i] = row[5];
            land_UpperBands[i] = row[6];
            refCentralWaveLenghts[i] = row[7];
            solarIrradiances[i] = row[8];
        }
    }


    public double[] getBands() {
        return bands;
    }

    public double[] getWater_LowerBands() {
        return water_LowerBands;
    }

    public double[] getWater_UpperBands() {
        return water_UpperBands;
    }

    public boolean[] getLandRefCorrectionSwitchs() {
        return landRefCorrectionSwitchs;
    }

    public double[] getLand_LowerBands() {
        return land_LowerBands;
    }

    public double[] getLand_UpperBands() {
        return land_UpperBands;
    }

    public double[] getSolarIrradiances() {
        return solarIrradiances;
    }

    public double[] getRefCentralWaveLenghts() {
        return refCentralWaveLenghts;
    }

    public boolean[] getWaterRefCorrectionSwitchs() {
        return waterRefCorrectionSwitchs;
    }

    public static List<String[]> loadAuxdata() throws IOException {
        final Path auxdataDir = installAuxdata();
        CsvReader csvReader = new CsvReader(new FileReader(auxdataDir.resolve("band_reflectance_config.txt").toString()), new char[]{'|', '\t'});
        List<String[]> readStringRecords = csvReader.readStringRecords();
        csvReader.close();
        return readStringRecords;
    }


    static Path installAuxdata() throws IOException {
        Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("olci/smile-correction");
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
