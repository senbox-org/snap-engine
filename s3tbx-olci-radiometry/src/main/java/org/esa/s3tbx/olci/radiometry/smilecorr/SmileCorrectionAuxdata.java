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
import org.esa.s3tbx.olci.radiometry.Sensor;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.CsvReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.StringTokenizer;

public class SmileCorrectionAuxdata {


    private static final String CENTRAL_WAVELEN_FR_FILENAME = "central_wavelen_fr.txt";
    private static final String CENTRAL_WAVELEN_RR_FILENAME = "central_wavelen_rr.txt";
    private static final String SUN_SPECTRAL_FLUX_FR_FILENAME = "sun_spectral_flux_fr.txt";
    private static final String SUN_SPECTRAL_FLUX_RR_FILENAME = "sun_spectral_flux_rr.txt";
    private static final int NUM_DETECTORS_FR = 3700;
    private static final int NUM_DETECTORS_RR = 925;
    private int[] bandIndices;
    private boolean[] landRefCorrectionSwitches;
    private boolean[] waterRefCorrectionSwitches;
    private int[] waterLowerBands;
    private int[] waterUpperBands;
    private int[] landLowerBands;
    private int[] landUpperBands;
    private float[] refCentralWaveLengths;
    private float[] solarIrradiances;

    private double[][] detectorWavelengths;
    private double[][] detectorSunSpectralFluxes;

    public SmileCorrectionAuxdata(Sensor sensor) {
        List<String[]> loadAuxdata = null;
        try {
            loadAuxdata = loadAuxdata(sensor.getBandInfoFileName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        final double[][] tableValue = auxDataInFlatTable(loadAuxdata, 9);
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
            bandIndices[i] = (int) row[0];
            waterRefCorrectionSwitches[i] = row[1] != 0.0;
            waterLowerBands[i] = (int) row[2];
            waterUpperBands[i] = (int) row[3];
            landRefCorrectionSwitches[i] = row[4] != 0.0;
            landLowerBands[i] = (int) row[5];
            landUpperBands[i] = (int) row[6];
            refCentralWaveLengths[i] = (float) row[7];
            solarIrradiances[i] = (float) row[8];
        }


    }

    public void loadFluxWaven(String productType) throws IOException {
        if (productType.startsWith("MER_F")) {
            loadFRAuxdata();
        } else if (productType.startsWith("MER_R")) {
            loadRRAuxdata();
        } else {
            throw new IOException(String.format("No auxillary data found for input product of type '%s'", productType));
        }
    }

    public void loadRRAuxdata() throws IOException {
        loadDetectorData(CENTRAL_WAVELEN_RR_FILENAME, SUN_SPECTRAL_FLUX_RR_FILENAME, NUM_DETECTORS_RR, Sensor.MERIS.getNumBands());
    }

    public void loadFRAuxdata() throws IOException {
        loadDetectorData(CENTRAL_WAVELEN_FR_FILENAME, SUN_SPECTRAL_FLUX_FR_FILENAME, NUM_DETECTORS_FR, Sensor.MERIS.getNumBands());
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

    public int[] getWaterLowerBands() {
        return waterLowerBands;
    }

    public int[] getWaterUpperBands() {
        return waterUpperBands;
    }

    public int[] getLandLowerBands() {
        return landLowerBands;
    }

    public int[] getLandUpperBands() {
        return landUpperBands;
    }

    public float[] getRefCentralWaveLengths() {
        return refCentralWaveLengths;
    }

    public float[] getSolarIrradiances() {
        return solarIrradiances;
    }

    public double[][] getDetectorWavelengths() {
        return detectorWavelengths;
    }

    public double[][] getDetectorSunSpectralFluxes() {
        return detectorSunSpectralFluxes;
    }

    public static List<String[]> loadAuxdata(String bandInfoFileName) throws IOException {
        final Path auxdataDir = installAuxdata();
        List<String[]> readStringRecords;
        try (CsvReader csvReader = new CsvReader(new FileReader(auxdataDir.resolve(bandInfoFileName).toString()), new char[]{'|', '\t'})) {
            readStringRecords = csvReader.readStringRecords();
        }
        return readStringRecords;
    }

    public static double[][] auxDataInFlatTable(List<String[]> loadAuxdata, int columnLen) {
        for (int i = 0; i < loadAuxdata.size(); i++) {
            String[] p = loadAuxdata.get(i);
            if (p.length <= 0) {
                loadAuxdata.remove(p);
            }
        }
        double[][] tableEntry = new double[loadAuxdata.size()][columnLen];
        for (int i = 1; i < loadAuxdata.size(); i++) {
            String[] val = loadAuxdata.get(i);
            if (val.length <= 0) {
                continue;
            }
            final String indexValue = val[0];
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

    private void loadDetectorData(final String detectorWavelengthsFilename,
                                  final String detectorSunSpectralFluxesFilename,
                                  final int numRows,
                                  final int numCols) throws IOException {

        detectorWavelengths = loadFlatAuxDataFile(detectorWavelengthsFilename, numRows, numCols);
        detectorSunSpectralFluxes = loadFlatAuxDataFile(detectorSunSpectralFluxesFilename, numRows, numCols);
    }

    private double[][] loadFlatAuxDataFile(final String auxFileName, final int numRows, final int numCols) throws
            IOException {
        double[][] tableData = new double[numRows][numCols];
        IOException ioError = null;
        try (BufferedReader reader = openFlatAuxDataFile(auxFileName)) {
            readFlatAuxDataFile(tableData, reader);
        } catch (IOException e) {
            throw new OperatorException(e);
        }

//        Matrix matrix = new Matrix(tableData);
//        Matrix transpose = matrix.transpose();
//        double[][] arrayTranspose = transpose.getArray();

        return tableData;
    }


    private BufferedReader openFlatAuxDataFile(String fileName) throws IOException {
        assert fileName != null;
        assert fileName.length() > 0;
        return Files.newBufferedReader(installAuxdata().resolve(fileName));
    }

    private static void readFlatAuxDataFile(double[][] xrWLs, BufferedReader reader) throws IOException {

        final int numRows = xrWLs.length;
        final int numCols = xrWLs[0].length;
        StringTokenizer st;
        String line;
        String token;
        int row = -1; // skip first row, it's always a header line
        int col;
        while ((line = reader.readLine()) != null) {
            if (row >= 0 && row < numRows) {
                st = new StringTokenizer(line, " \t", false);
                col = -1; // skip first column, it's always the band index
                while (st.hasMoreTokens()) {
                    token = st.nextToken();
                    if (col >= 0 && col < numCols) {
                        xrWLs[row][col] = Double.parseDouble(token);
                    }
                    col++;
                }
            }
            row++;
        }
    }

    static Path installAuxdata() throws IOException {
        OperatorSpiRegistry operatorSpiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        OperatorSpi spi = operatorSpiRegistry.getOperatorSpi("SmileCorrection.Olci");
        String version = spi.getOperatorDescriptor().getVersion();
        Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("olci/smile/"+version);
        final Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(SmileCorrectionAuxdata.class).resolve("auxdata/smile");
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);
        resourceInstaller.install(".*", ProgressMonitor.NULL);
        return auxdataDirectory;
    }
}
