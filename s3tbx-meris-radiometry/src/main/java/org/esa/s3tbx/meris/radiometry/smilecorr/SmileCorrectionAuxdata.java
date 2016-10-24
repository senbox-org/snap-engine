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
package org.esa.s3tbx.meris.radiometry.smilecorr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringTokenizer;

/**
 * Provides auxiliary data for the Smile Correction Processor.
 * <p/>
 * This class also provides a flag for each of the 15 MERIS L1b bands determining whether or not to perform irradiance
 * plus radiance correction for each band and the lower and upper band indexes to be used for each band.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 */
public class SmileCorrectionAuxdata {

    private static final String BAND_INFO_FILENAME = "band_info.txt";
    private static final String CENTRAL_WAVELEN_FR_FILENAME = "central_wavelen_fr.txt";
    private static final String CENTRAL_WAVELEN_RR_FILENAME = "central_wavelen_rr.txt";
    private static final String SUN_SPECTRAL_FLUX_FR_FILENAME = "sun_spectral_flux_fr.txt";
    private static final String SUN_SPECTRAL_FLUX_RR_FILENAME = "sun_spectral_flux_rr.txt";
    private static final int NUM_DETECTORS_FR = 3700;
    private static final int NUM_DETECTORS_RR = 925;

    private boolean[/*15*/] radCorrFlagsLand;
    private int[/*15*/] lowerBandIndexesLand;
    private int[/*15*/] upperBandIndexesLand;
    private boolean[/*15*/] radCorrFlagsWater;
    private int[/*15*/] lowerBandIndexesWater;
    private int[/*15*/] upperBandIndexesWater;
    private double[/*15*/] theoreticalWavelengths;
    private double[/*15*/] theoreticalSunSpectralFluxes;
    private double[][/*15*/] detectorWavelengths;
    private double[][/*15*/] detectorSunSpectralFluxes;
    private final Path auxdataDir;

    private SmileCorrectionAuxdata(Path auxdataDir,
                                   final String detectorWavelengthsFilename,
                                   final String detectorSunSpectralFluxesFilename,
                                   final int numRows,
                                   final int numCols) throws IOException {
        this.auxdataDir = auxdataDir;
        loadBandInfos();
        loadDetectorData(detectorWavelengthsFilename, detectorSunSpectralFluxesFilename, numRows, numCols);
    }

    public static SmileCorrectionAuxdata loadAuxdata(String productType) throws IOException {
        final Path auxdataDir = installAuxdata();

        if (productType.startsWith("MER_F")) {
            return loadFRAuxdata(auxdataDir);
        } else if (productType.startsWith("MER_R")) {
            return loadRRAuxdata(auxdataDir);
        } else {
            throw new IOException(String.format("No auxillary data found for input product of type '%s'", productType));
        }
    }

    public boolean[/*15*/] getRadCorrFlagsWater() {
        return radCorrFlagsWater;
    }

    public int[/*15*/] getLowerBandIndexesWater() {
        return lowerBandIndexesWater;
    }

    public int[/*15*/] getUpperBandIndexesWater() {
        return upperBandIndexesWater;
    }

    public boolean[/*15*/] getRadCorrFlagsLand() {
        return radCorrFlagsLand;
    }

    public int[/*15*/] getLowerBandIndexesLand() {
        return lowerBandIndexesLand;
    }

    public int[/*15*/] getUpperBandIndexesLand() {
        return upperBandIndexesLand;
    }

    public double[] getTheoreticalWavelengths() {
        return theoreticalWavelengths;
    }

    public double[] getTheoreticalSunSpectralFluxes() {
        return theoreticalSunSpectralFluxes;
    }

    public double[][] getDetectorWavelengths() {
        return detectorWavelengths;
    }

    public double[][] getDetectorSunSpectralFluxes() {
        return detectorSunSpectralFluxes;
    }

    public static SmileCorrectionAuxdata loadRRAuxdata(Path auxdataDir) throws IOException {
        return new SmileCorrectionAuxdata(auxdataDir,
                                          CENTRAL_WAVELEN_RR_FILENAME,
                                          SUN_SPECTRAL_FLUX_RR_FILENAME,
                                          NUM_DETECTORS_RR,
                                          EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS);
    }

    public static SmileCorrectionAuxdata loadFRAuxdata(Path auxdataDir) throws IOException {
        return new SmileCorrectionAuxdata(auxdataDir,
                                          CENTRAL_WAVELEN_FR_FILENAME,
                                          SUN_SPECTRAL_FLUX_FR_FILENAME,
                                          NUM_DETECTORS_FR,
                                          EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS);
    }

    private void loadBandInfos() throws IOException {
        final double[][] table = loadFlatAuxDataFile(BAND_INFO_FILENAME, 15, 8);
        final int n = EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS;
        radCorrFlagsLand = new boolean[n];
        lowerBandIndexesLand = new int[n];
        upperBandIndexesLand = new int[n];
        radCorrFlagsWater = new boolean[n];
        lowerBandIndexesWater = new int[n];
        upperBandIndexesWater = new int[n];
        theoreticalWavelengths = new double[n];
        theoreticalSunSpectralFluxes = new double[n];
        for (int i = 0; i < n; i++) {
            final double[] row = table[i];
            radCorrFlagsLand[i] = row[0] != 0.0;
            lowerBandIndexesLand[i] = (int) row[1] - 1;
            upperBandIndexesLand[i] = (int) row[2] - 1;
            radCorrFlagsWater[i] = row[3] != 0.0;
            lowerBandIndexesWater[i] = (int) row[4] - 1;
            upperBandIndexesWater[i] = (int) row[5] - 1;
            theoreticalWavelengths[i] = row[6];
            theoreticalSunSpectralFluxes[i] = row[7];
        }
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
            ioError = e;
        }
        if (ioError != null) {
            throw ioError;
        }
        return tableData;
    }

    private BufferedReader openFlatAuxDataFile(String fileName) throws IOException {
        assert fileName != null;
        assert fileName.length() > 0;
        return Files.newBufferedReader(auxdataDir.resolve(fileName));
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
        Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("meris/smile-correction");
        final Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(SmileCorrectionAuxdata.class).resolve("auxdata/smile");
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);
        resourceInstaller.install(".*", ProgressMonitor.NULL);
        return auxdataDirectory;
    }
}
