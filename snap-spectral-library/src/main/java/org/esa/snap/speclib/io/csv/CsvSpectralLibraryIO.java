package org.esa.snap.speclib.io.csv;

import org.esa.snap.speclib.io.SpectralLibraryIODelegate;
import org.esa.snap.speclib.io.csv.util.CsvAttributeCodec;
import org.esa.snap.speclib.io.csv.util.CsvTable;
import org.esa.snap.speclib.io.csv.util.CsvUtils;
import org.esa.snap.speclib.model.AttributeDef;
import org.esa.snap.speclib.model.AttributeSchema;
import org.esa.snap.speclib.model.AttributeType;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.model.SpectralAxis;
import org.esa.snap.speclib.model.SpectralLibrary;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;


/**
 * Standalone CSV implementation of {@link SpectralLibraryIODelegate}.
 *
 * <p>Reads and writes spectral libraries as CSV files, compatible with the
 * EcoSIS (Ecological Spectral Information System) format.
 *
 * <p><b>Format (By Row – export default):</b>
 * <pre>
 * spectra,wkt,class,490.0,560.0,665.0
 * Profile1,POINT(13.0 52.0),vegetation,95.0,118.0,109.0
 * Profile2,,urban,103.0,,93.0
 * </pre>
 *
 * <ul>
 *   <li>Columns with numeric headers are wavelengths (spectral data).</li>
 *   <li>Non-numeric columns are metadata attributes.</li>
 *   <li>The name column is recognised by the keys {@code spectra}, {@code name},
 *       or {@code spectra names} (case-insensitive). On export {@code spectra} is used.</li>
 *   <li>Empty cells in spectral columns become {@code NaN} (bad band).</li>
 *   <li>{@code xUnit} may appear as a non-numeric header and carries the wavelength unit.</li>
 * </ul>
 *
 * <p><b>By Column import:</b> If the first header token is numeric the table is
 * transposed before parsing, making By-Column EcoSIS files readable.
 */
public class CsvSpectralLibraryIO implements SpectralLibraryIODelegate {


    private static final List<String> EXTENSIONS = List.of("csv");

    public static final String COL_SPECTRA  = "spectra";
    public static final String COL_NAME     = "name";
    public static final String COL_SPECTRA_NAMES = "spectra names";
    public static final String COL_WKT      = "wkt";
    public static final String COL_X_UNIT   = "xUnit";


    @Override
    public boolean canRead(Path path) {
        if (!CsvUtils.hasExtension(path, "csv")) {
            return false;
        }
        return looksLikeSpectralCsv(path);
    }

    @Override
    public boolean canWrite(Path path) {
        return CsvUtils.hasExtension(path, "csv");
    }

    @Override
    public List<String> getFileExtensions() {
        return EXTENSIONS;
    }


    @Override
    public SpectralLibrary read(Path path) throws IOException {
        Objects.requireNonNull(path, "path must not be null");

        CsvTable raw = CsvUtils.read(path);
        if (raw.header().isEmpty()) {
            throw new IOException("CSV has no header: " + path);
        }

        CsvTable table = CsvUtils.isColumnOriented(raw) ? CsvUtils.transpose(raw) : raw;
        return parseByRow(table, path);
    }

    @Override
    public void write(SpectralLibrary library, Path path) throws IOException {
        Objects.requireNonNull(library, "library must not be null");
        Objects.requireNonNull(path, "path must not be null");

        Files.createDirectories(path.toAbsolutePath().getParent());

        SpectralAxis axis = library.getAxis();

        List<String> header = buildHeader(library, axis);
        List<List<String>> rows = buildRows(library, axis, header);

        CsvUtils.write(path, header, rows);
    }


    private SpectralLibrary parseByRow(CsvTable table, Path path) throws IOException {
        List<String> header = table.header();

        int nameCol = findNameColumn(header);
        int xUnitCol= CsvUtils.findColumn(header, COL_X_UNIT);
        List<Integer> waveCols = CsvUtils.findWavelengthColumns(header);

        if (waveCols.isEmpty()) {
            throw new IOException("CSV contains no numeric wavelength columns: " + path);
        }

        double[] wavelengths = CsvUtils.extractWavelengths(header, waveCols);
        String xUnit = resolveXUnit(table, xUnitCol, waveCols.getFirst());

        SpectralAxis axis = new SpectralAxis(wavelengths, xUnit);
        AttributeSchema schema = new AttributeSchema();
        List<SpectralProfile> profiles = new ArrayList<>(table.rows().size());

        for (int rr = 0; rr < table.rows().size(); rr++) {
            List<String> row = table.rows().get(rr);

            String profileName = nameCol >= 0
                    ? CsvUtils.safeCell(row, nameCol).trim()
                    : "Spectrum_" + (rr + 1);
            if (profileName.isEmpty()) {
                profileName = "Spectrum_" + (rr + 1);
            }

            double[] yValues = readYValues(row, waveCols);
            SpectralSignature sig = SpectralSignature.of(yValues);

            Map<String, AttributeValue> attrs = readAttributes(header, row, nameCol, xUnitCol, waveCols, schema);
            profiles.add(new SpectralProfile(UUID.randomUUID(), profileName, sig, attrs, null));
        }

        String libName = CsvUtils.stripExtension(path.getFileName().toString());
        return new SpectralLibrary(UUID.randomUUID(), libName, axis, null, profiles, schema);
    }

    private double[] readYValues(List<String> row, List<Integer> waveCols) {
        double[] values = new double[waveCols.size()];

        for (int ii = 0; ii < waveCols.size(); ii++) {
            String cell = CsvUtils.safeCell(row, waveCols.get(ii)).trim();

            if (cell.isEmpty()) {
                values[ii] = Double.NaN;
            } else {
                try {
                    values[ii] = Double.parseDouble(cell);
                } catch (NumberFormatException e) {
                    values[ii] = Double.NaN;
                }
            }
        }
        return values;
    }

    private Map<String, AttributeValue> readAttributes(List<String> header,
                                                       List<String> row,
                                                       int nameCol,
                                                       int xUnitCol,
                                                       List<Integer> waveCols,
                                                       AttributeSchema schema) {
        Map<String, AttributeValue> attrs = new LinkedHashMap<>();

        for (int cc = 0; cc < header.size(); cc++) {
            if (cc == nameCol || cc == xUnitCol || waveCols.contains(cc)) {
                continue;
            }

            String rawKey = header.get(cc);
            if (rawKey == null || rawKey.trim().isEmpty()) {
                continue;
            }

            String key = rawKey.trim();
            String raw = CsvUtils.safeCell(row, cc).trim();
            if (raw.isEmpty()) {
                continue;
            }

            AttributeType type = schema.find(key)
                    .map(AttributeDef::getType)
                    .orElseGet(() -> CsvUtils.inferAttributeType(key, raw));

            AttributeValue av = CsvAttributeCodec.tryParse(type, raw);
            if (av == null) {
                av = AttributeValue.ofString(raw);
            }
            attrs.put(key, av);
            schema.inferFromAttributes(Map.of(key, av));
        }
        return attrs;
    }

    /**
     * Tries to parse the xUnit from a dedicated xUnit column.
     * Falls back to empty string if not present.
     */
    private String resolveXUnit(CsvTable table, int xUnitCol, int firstWaveCol) {
        if (xUnitCol >= 0 && !table.rows().isEmpty()) {
            String unit = CsvUtils.safeCell(table.rows().getFirst(), xUnitCol).trim();

            if (!unit.isEmpty()) {
                return unit;
            }
        }
        return "";
    }


    private List<String> buildHeader(SpectralLibrary library, SpectralAxis axis) {
        LinkedHashSet<String> cols = new LinkedHashSet<>();
        cols.add(COL_SPECTRA);

        AttributeSchema schema = library.getSchema();
        for (String key : schema.asMap().keySet()) {
            if (!COL_WKT.equalsIgnoreCase(key)) {
                cols.add(key);
            }
        }

        for (SpectralProfile p : library.getProfiles()) {
            for (String k : p.getAttributes().keySet()) {
                if (!schema.asMap().containsKey(k) && !COL_WKT.equalsIgnoreCase(k)) {
                    cols.add(k);
                }
            }
        }

        cols.add(COL_WKT);

        if (!axis.getXUnit().isBlank()) {
            cols.add(COL_X_UNIT);
        }

        for (double wl : axis.getWavelengths()) {
            cols.add(CsvUtils.formatWavelength(wl));
        }

        return new ArrayList<>(cols);
    }

    private List<List<String>> buildRows(SpectralLibrary library, SpectralAxis axis, List<String> header) {
        List<List<String>> rows = new ArrayList<>(library.size());
        double[] wavelengths = axis.getWavelengths();

        for (SpectralProfile profile : library.getProfiles()) {
            List<String> row = new ArrayList<>(header.size());
            double[] yValues = profile.getSignature().getValues();

            for (String col : header) {
                if (COL_SPECTRA.equalsIgnoreCase(col)) {
                    row.add(profile.getName());
                } else if (COL_WKT.equalsIgnoreCase(col)) {
                    AttributeValue wkt = profile.getAttributes().get(COL_WKT);
                    row.add(wkt != null ? wkt.asString() : "");
                } else if (COL_X_UNIT.equalsIgnoreCase(col)) {
                    row.add(axis.getXUnit());
                } else if (CsvUtils.isWavelengthColumn(col, wavelengths)) {
                    int idx = CsvUtils.wavelengthIndex(col, wavelengths);

                    if (idx >= 0) {
                        double v = yValues[idx];
                        row.add(Double.isNaN(v) || Double.isInfinite(v) ? "" : String.valueOf(v));
                    } else {
                        row.add("");
                    }
                } else {
                    AttributeValue av = profile.getAttributes().get(col);
                    row.add(CsvAttributeCodec.format(av));
                }
            }
            rows.add(row);
        }
        return rows;
    }


    private static int findNameColumn(List<String> header) {
        for (int ii = 0; ii < header.size(); ii++) {
            String h = header.get(ii);
            if (h == null) {
                continue;
            }

            String t = h.trim().toLowerCase(Locale.ROOT);
            if (t.equals(COL_SPECTRA) || t.equals(COL_NAME) || t.equals(COL_SPECTRA_NAMES)) {
                return ii;
            }
        }
        return -1;
    }


    /**
     * Peeks at the first line of the CSV to check whether it contains at least
     * one numeric column header (= wavelength column), which distinguishes this
     * format from the ENVI sidecar CSV.
     */
    private static boolean looksLikeSpectralCsv(Path path) {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String firstLine = reader.readLine();
            if (firstLine == null || firstLine.isBlank()) {
                return false;
            }
            if (firstLine.startsWith("\uFEFF")) {
                firstLine = firstLine.substring(1);
            }
            for (String token : firstLine.split(",")) {
                if (CsvUtils.isNumeric(token.trim())) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}