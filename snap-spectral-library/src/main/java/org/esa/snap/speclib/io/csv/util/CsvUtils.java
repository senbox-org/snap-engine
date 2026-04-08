package org.esa.snap.speclib.io.csv.util;

import org.esa.snap.speclib.model.AttributeType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;


public class CsvUtils {


    public static CsvTable read(Path path) throws IOException {
        Objects.requireNonNull(path, "path must not be null");

        try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            List<List<String>> rows = new ArrayList<>();

            StringBuilder sb = new StringBuilder();
            List<String> currentRow = new ArrayList<>();
            boolean inQuotes = false;

            int ch;
            while ((ch = r.read()) != -1) {
                char c = (char) ch;

                if (inQuotes) {
                    if (c == '"') {
                        r.mark(1);
                        int next = r.read();
                        if (next == '"') {
                            sb.append('"');
                        } else {
                            inQuotes = false;
                            if (next != -1) {
                                r.reset();
                            }
                        }
                    } else {
                        sb.append(c);
                    }
                    continue;
                }

                if (c == '"') {
                    inQuotes = true;
                    continue;
                }
                if (c == ',') {
                    currentRow.add(sb.toString());
                    sb.setLength(0);
                    continue;
                }
                if (c == '\r') {
                    r.mark(1);
                    int next = r.read();
                    if (next != '\n' && next != -1) {
                        r.reset();
                    }
                    currentRow.add(sb.toString());
                    sb.setLength(0);
                    rows.add(currentRow);
                    currentRow = new ArrayList<>();
                    continue;
                }
                if (c == '\n') {
                    currentRow.add(sb.toString());
                    sb.setLength(0);
                    rows.add(currentRow);
                    currentRow = new ArrayList<>();
                    continue;
                }
                sb.append(c);
            }

            if (inQuotes) {
                throw new IOException("Unclosed quote in CSV: " + path);
            }
            if (!sb.isEmpty() || !currentRow.isEmpty()) {
                currentRow.add(sb.toString());
                rows.add(currentRow);
            }

            if (rows.isEmpty()) {
                return new CsvTable(List.of(), List.of());
            }

            List<String> header = new ArrayList<>(rows.get(0));
            if (!header.isEmpty() && header.get(0) != null && header.get(0).startsWith("\uFEFF")) {
                header.set(0, header.get(0).substring(1));
            }

            List<List<String>> data = rows.size() > 1 ? rows.subList(1, rows.size()) : List.of();
            return new CsvTable(header, data);
        }
    }

    public static void write(Path path, List<String> header, List<List<String>> rows) throws IOException {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(header, "header must not be null");
        Objects.requireNonNull(rows, "rows must not be null");

        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writeRow(w, header);
            for (List<String> row : rows) {
                writeRow(w, row);
            }
        }
    }

    private static void writeRow(Writer w, List<String> row) throws IOException {
        for (int i = 0; i < row.size(); i++) {
            if (i > 0) {
                w.write(',');
            }
            w.write(escape(row.get(i)));
        }
        w.write('\n');
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        boolean needsQuotes = s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;

        if (!needsQuotes) {
            return s;
        }
        String t = s.replace("\"", "\"\"");
        return "\"" + t + "\"";
    }


    /**
     * Infers the attribute type, attempting ISO-8601 instant parsing for strings
     * before falling back to {@link CsvAttributeTypeInference}.
     */
    public static AttributeType inferAttributeType(String key, String raw) {
        AttributeType base = CsvAttributeTypeInference.inferType(raw);
        if (base == AttributeType.STRING) {
            try {
                Instant.parse(raw.trim());
                return AttributeType.INSTANT;
            } catch (Exception ignored) {
                // not an instant
            }
        }
        return base;
    }


    public static boolean isNumeric(String s) {
        if (s == null || s.trim().isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(s.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String safeCell(List<String> row, int idx) {
        if (row == null || idx < 0 || idx >= row.size()) {
            return "";
        }
        String v = row.get(idx);
        return v == null ? "" : v;
    }

    public static boolean hasExtension(Path path, String ext) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith("." + ext);
    }

    public static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /**
     * Returns true if the first header token is numeric, indicating By-Column orientation.
     */
    public static boolean isColumnOriented(CsvTable table) {
        if (table.header().isEmpty() || table.rows().isEmpty()) {
            return false;
        }

        for (List<String> row : table.rows()) {
            String cell = safeCell(row, 0).trim();
            if (!cell.isEmpty() && !isNumeric(cell)) {
                return false;
            }
        }

        for (List<String> row : table.rows()) {
            if (isNumeric(safeCell(row, 0).trim())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Transposes a By-Column table into By-Row form.
     *
     * <p>By-Column layout (EcoSIS):
     * <pre>
     * spectra, Profile1, Profile2, ...
     * 490.0,   95.0,     103.0,    ...
     * 560.0,   118.0,    null,     ...
     * </pre>
     *
     * After transpose becomes By-Row:
     * <pre>
     * spectra, 490.0, 560.0, ...
     * Profile1, 95.0, 118.0, ...
     * Profile2, 103.0, null, ...
     * </pre>
     */
    public static CsvTable transpose(CsvTable table) {
        int origCols = table.header().size();
        List<String> newHeader = new ArrayList<>();
        newHeader.add(table.header().getFirst());

        for (List<String> row : table.rows()) {
            newHeader.add(safeCell(row, 0));
        }

        List<List<String>> newRows = new ArrayList<>();
        for (int c = 1; c < origCols; c++) {
            List<String> newRow = new ArrayList<>();
            newRow.add(table.header().get(c));

            for (List<String> origRow : table.rows()) {
                newRow.add(safeCell(origRow, c));
            }
            newRows.add(newRow);
        }

        return new CsvTable(newHeader, newRows);
    }


    public static int findColumn(List<String> header, String colName) {
        for (int ii = 0; ii < header.size(); ii++) {
            String h = header.get(ii);
            if (h != null && h.trim().equalsIgnoreCase(colName)) {
                return ii;
            }
        }
        return -1;
    }

    public static List<Integer> findWavelengthColumns(List<String> header) {
        List<Integer> cols = new ArrayList<>();
        for (int ii = 0; ii < header.size(); ii++) {
            if (CsvUtils.isNumeric(header.get(ii))) {
                cols.add(ii);
            }
        }
        return cols;
    }

    public static double[] extractWavelengths(List<String> header, List<Integer> waveCols) {
        double[] wl = new double[waveCols.size()];
        for (int ii = 0; ii < waveCols.size(); ii++) {
            wl[ii] = Double.parseDouble(header.get(waveCols.get(ii)).trim());
        }
        return wl;
    }

    public static String formatWavelength(double wl) {
        if (wl == Math.floor(wl) && !Double.isInfinite(wl)) {
            return String.valueOf((long) wl);
        }
        return String.valueOf(wl);
    }

    public static boolean isWavelengthColumn(String col, double[] wavelengths) {
        if (!isNumeric(col)) {
            return false;
        }
        double v;

        try {
            v = Double.parseDouble(col.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        for (double wl : wavelengths) {
            if (Double.compare(wl, v) == 0) {
                return true;
            }
        }
        return false;
    }

    public static int wavelengthIndex(String col, double[] wavelengths) {
        try {
            double v = Double.parseDouble(col.trim());

            for (int ii = 0; ii < wavelengths.length; ii++) {
                if (Double.compare(wavelengths[ii], v) == 0) {
                    return ii;
                }
            }
        } catch (NumberFormatException e) {
            // not a wavelength column
        }
        return -1;
    }
}
