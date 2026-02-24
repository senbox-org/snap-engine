package org.esa.snap.speclib.io.csv.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
}
