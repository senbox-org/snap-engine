package org.esa.s3tbx.processor.rad2refl;

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
 * Radiance/reflectance conversion auxdata for MERIS (i.e. solar fluxes)
 *
 * @author olafd
 */
public class Rad2ReflAuxdata {
    private static final String SUN_SPECTRAL_FLUX_FR_FILENAME = "sun_spectral_flux_fr.txt";
    private static final String SUN_SPECTRAL_FLUX_RR_FILENAME = "sun_spectral_flux_rr.txt";
    private static final int NUM_DETECTORS_FR = 3700;
    private static final int NUM_DETECTORS_RR = 925;

    private double[][/*15*/] detectorSunSpectralFluxes;
    private final Path auxdataDir;

    private Rad2ReflAuxdata(Path auxdataDir,
                                   final String detectorSunSpectralFluxesFilename,
                                   final int numRows,
                                   final int numCols) throws IOException {
        this.auxdataDir = auxdataDir;
        loadDetectorData(detectorSunSpectralFluxesFilename, numRows, numCols);
    }

    public static Rad2ReflAuxdata loadMERISAuxdata(String productType) throws IOException {
        final Path auxdataDir = installAuxdata();

        if (productType.startsWith("MER_F")) {
            return loadFRAuxdata(auxdataDir);
        } else if (productType.startsWith("MER_R")) {
            return loadRRAuxdata(auxdataDir);
        } else {
            throw new IOException(String.format("No auxillary data found for input product of type '%s'", productType));
        }
    }

    public double[][] getDetectorSunSpectralFluxes() {
        return detectorSunSpectralFluxes;
    }

    public static Rad2ReflAuxdata loadRRAuxdata(Path auxdataDir) throws IOException {
        return new Rad2ReflAuxdata(auxdataDir,
                                          SUN_SPECTRAL_FLUX_RR_FILENAME,
                                          NUM_DETECTORS_RR,
                                          EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS);
    }

    public static Rad2ReflAuxdata loadFRAuxdata(Path auxdataDir) throws IOException {
        return new Rad2ReflAuxdata(auxdataDir,
                                          SUN_SPECTRAL_FLUX_FR_FILENAME,
                                          NUM_DETECTORS_FR,
                                          EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS);
    }

    private void loadDetectorData(final String detectorSunSpectralFluxesFilename,
                                  final int numRows,
                                  final int numCols) throws IOException {
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
        Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("meris/rad2refl");
        final Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(Rad2ReflAuxdata.class).resolve("auxdata/rad2refl");
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);
        resourceInstaller.install(".*", ProgressMonitor.NULL);
        return auxdataDirectory;
    }
}
