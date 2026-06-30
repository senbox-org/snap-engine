package org.esa.snap.speclib.util.resampling;

import com.bc.ceres.core.Assert;
import org.apache.commons.lang3.ArrayUtils;
import org.esa.snap.speclib.io.csv.util.CsvTable;
import org.esa.snap.speclib.io.csv.util.CsvUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Class representing a spectral response function, to be used e.g. for Spectral Resampling
 *
 * @author olafd
 */
public class SpectralResponseFunction {

    private String ID;
    private float refWvl;
    private final List<FWHM> fwhmList;
    private List<SpectralResponse> spectralResponsesList;


    /**
     * Constructor
     *
     */
    public SpectralResponseFunction() {
        fwhmList = new ArrayList<>();
        spectralResponsesList = new ArrayList<>();
    }

    /**
     * Constructor
     *
     * @param ID - Identifier (could be sensor name, tbd)
     */
    public SpectralResponseFunction(String ID) {
        this.ID = ID;
        fwhmList = new ArrayList<>();
        spectralResponsesList = new ArrayList<>();
    }

    /**
     * Reads FWHM values from a csv file. Makes use of classes/methods from the snap-spectrallibrary package.
     *
     * @param csvFile - File
     * @return - CsvTable
     */
    public static CsvTable readFwhmFromCsv(File csvFile) throws IOException {
        return CsvUtils.read(csvFile.toPath());
    }

    /**
     * Reads a bundled FWHM CSV resource for the given target sensor.
     *
     * @param targetSensorName sensor identifier, case-insensitive
     * @return parsed FWHM table
     */
    public static CsvTable readBundledFwhmFromCsv(String targetSensorName) throws IOException {
        if (targetSensorName == null || targetSensorName.isBlank()) {
            throw new FileNotFoundException("No target sensor specified for bundled FWHM CSV.");
        }

        String resourceName = "fwhm_" + targetSensorName.trim().toLowerCase(Locale.ROOT) + ".csv";
        InputStream stream = SpectralResponseFunction.class.getResourceAsStream(resourceName);
        if (stream == null) {
            throw new FileNotFoundException("Bundled FWHM CSV not found: " + resourceName);
        }

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return CsvUtils.read(reader, resourceName);
        }
    }

    /**
     * Provides a list of fully defined Spectral Response Functions, each of them defined as pairs of (wvl, weight)
     * around a given reference wavelength. A fully defined SRF is a Gaussian function retrieved from
     * an input pair (refWvl, FWHM).
     * See more details at <a href="https://en.wikipedia.org/wiki/Full_width_at_half_maximum">...</a>
     *
     * @param fwhmList -
     * @return -
     */
    public static List<SpectralResponseFunction> getFullyDefinedSrf(List<FWHM> fwhmList) {

        List<SpectralResponseFunction> fullSrfList = new ArrayList<>();

        fwhmList.iterator().forEachRemaining(sr -> {
            final float x0 = sr.getWvl();
            final float fwhm = sr.getFwhm();
            final float sigma =  fwhm / 2.355f;
            final float a = 2.0f * sigma * sigma;
            final float b = (float) (sigma * Math.sqrt(2.0 * Math.PI));
            List<SpectralResponse> srList = new ArrayList<>();
            float maxWeight = Float.MIN_VALUE;
            final int left = (int) (x0 - 3.0 * sigma);
            final int right = (int) (x0 + 3.0 * sigma);
            for (int x = left; x < right + 2; x++) {
                final float c = -1.0f * (x - x0) * (x - x0);
                final float weight = (float) (Math.exp(c / a) / b);
                if (weight > maxWeight) maxWeight = weight;
                srList.add(new SpectralResponseFunction.SpectralResponse(x, weight));
            }
            for (SpectralResponse spectralResponse : srList) {
                spectralResponse.setWeight(spectralResponse.getWeight() / maxWeight);
            }

            SpectralResponseFunction fullSrf = new SpectralResponseFunction();
            fullSrf.setRefWvl(x0);
            fullSrf.setFWHMList(srList);
            fullSrfList.add(fullSrf);
        });

        return fullSrfList;
    }

    /**
     * Provides wavelengths and FWHM values from a CSV table as double arrays.
     *
     * @param fwhmCsvTable - CSV table
     * @return double[][] : two double[] arrays, first is wavelengths, second is FWHM values
     */
    public static double[][] getFwhmTableColumnsAsArrays(CsvTable fwhmCsvTable) {
        Assert.notNull(fwhmCsvTable);

        List<Double> wvlList = new ArrayList<>();
        List<Double> fwhmList = new ArrayList<>();
        fwhmCsvTable.rows().iterator().forEachRemaining(row -> {
            wvlList.add(Double.parseDouble(row.get(0)));
            fwhmList.add(Double.parseDouble(row.get(1)));
        });

        final double[] wvlArray = ArrayUtils.toPrimitive(wvlList.toArray(new Double[wvlList.size()]));
        final double[] fwhmArray = ArrayUtils.toPrimitive(fwhmList.toArray(new Double[fwhmList.size()]));

        return new double[][]{wvlArray, fwhmArray};
    }


    /**
     * Fills spectral responses list with spectral responses from CsvTable
     *
     * @param fwhmCsvTable - wavelength/fwhm pairs
     */
    public void setFWHMList(CsvTable fwhmCsvTable) {
        Assert.notNull(fwhmCsvTable);
        fwhmCsvTable.rows().iterator().forEachRemaining(row -> {
            FWHM fwhm = new FWHM(Float.parseFloat(row.get(0)), Float.parseFloat(row.get(1)));
            fwhmList.add(fwhm);
        });
    }

    /**
     * Reads spectral responses from a GeoJson file.
     *
     * @param geoJsonFile -
     */
    public void readSpectralResponsesFromGeoJson(File geoJsonFile) {
        // TODO later if needed
    }

    // getters and setters //

    public String getID() {
        return ID;
    }


    public List<SpectralResponse> getSpectralResponsesList() {
        return spectralResponsesList;
    }

    public float getRefWvl() {
        return refWvl;
    }

    public void setRefWvl(float refWvl) {
        this.refWvl = refWvl;
    }

    public void setFWHMList(List<SpectralResponse> spectralResponsesList) {
        this.spectralResponsesList = spectralResponsesList;
    }

    public List<FWHM> getFwhmList() {
        return fwhmList;
    }

    /**
     * Object holding a spectral response function value of wavelength/weight.
     * See <a href="https://en.wikipedia.org/wiki/Full_width_at_half_maximum">...</a> for more details.
     *
     */
    public static class SpectralResponse {
        float wvl;
        float weight;

        public SpectralResponse(float wvl, float weight) {
            this.wvl = wvl;
            this.weight = weight;
        }

        public float getWvl() {
            return wvl;
        }

        public float getWeight() {
            return weight;
        }

        public void setWeight(float weight) {
            this.weight = weight;
        }
    }

    /**
     * Object holding an FWHM as function wavelength.
     * See <a href="https://en.wikipedia.org/wiki/Full_width_at_half_maximum">...</a> for more details.
     *
     */
    public static class FWHM {
        float wvl;
        float fwhm;

        public FWHM(float wvl, float fwhm) {
            this.wvl = wvl;
            this.fwhm = fwhm;
        }

        public float getWvl() {
            return wvl;
        }

        public float getFwhm() {
            return fwhm;
        }
    }
}
