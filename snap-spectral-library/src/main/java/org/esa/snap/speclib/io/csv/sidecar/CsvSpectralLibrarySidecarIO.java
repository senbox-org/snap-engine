package org.esa.snap.speclib.io.csv.sidecar;

import org.esa.snap.speclib.io.csv.util.*;
import org.esa.snap.speclib.io.util.IOUtils;
import org.esa.snap.speclib.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


public class CsvSpectralLibrarySidecarIO {


    public static final String COL_SPECTRA_NAMES = "spectra names";
    public static final String COL_WKT = "wkt";


    public SpectralLibrary read(Path path) throws IOException {
        Objects.requireNonNull(path, "path must not be null");

        CsvTable table = CsvUtils.read(path);
        if (table.header().isEmpty()) {
            throw new IOException("CSV has no header: " + path);
        }

        int nameCol = findNameColumn(table.header());
        if (nameCol < 0) {
            nameCol = 0;
        }

        SpectralAxis axis = new SpectralAxis(new double[]{0.0}, "");
        SpectralSignature dummySig = SpectralSignature.of(new double[]{Double.NaN});

        List<SpectralProfile> profiles = new ArrayList<>();
        AttributeSchema schema = new AttributeSchema();

        if (table.header().stream().anyMatch(h -> h != null && h.trim().equalsIgnoreCase(COL_WKT))) {
            schema = ensureWktInSchema(schema);
        }

        for (int r = 0; r < table.rows().size(); r++) {
            List<String> row = table.rows().get(r);
            String profileName = safeCell(row, nameCol).trim();
            if (profileName.isEmpty()) {
                continue;
            }

            SpectralProfile p = SpectralProfile.create(profileName, dummySig);

            for (int c = 0; c < table.header().size(); c++) {
                if (c == nameCol) {
                    continue;
                }

                String key = table.header().get(c).trim();
                if (key.isEmpty()) {
                    continue;
                }

                String raw = safeCell(row, c);
                if (raw.trim().isEmpty()) {
                    continue;
                }

                AttributeType type;
                if (COL_WKT.equalsIgnoreCase(key)) {
                    type = AttributeType.STRING;
                } else {
                    type = schema.find(key)
                            .map(AttributeDef::getType)
                            .orElseGet(() -> CsvAttributeTypeInference.inferType(raw));
                }

                AttributeValue parsed = CsvAttributeCodec.tryParse(type, raw);
                if (parsed != null) {
                    p = p.withAttribute(key, parsed);
                    schema.inferFromAttributes(Map.of(key, parsed));
                } else {
                    p = p.withAttribute(key, AttributeValue.ofString(raw));
                    schema.inferFromAttributes(Map.of(key, AttributeValue.ofString(raw)));
                }
            }

            profiles.add(p);
        }

        String libName = IOUtils.stripExtension(path.getFileName().toString());
        return new SpectralLibrary(UUID.randomUUID(), libName, axis, null, profiles, schema);
    }

    public void write(SpectralLibrary library, Path path) throws IOException {
        Objects.requireNonNull(library, "library must not be null");
        Objects.requireNonNull(path, "path must not be null");

        Files.createDirectories(path.toAbsolutePath().getParent());

        LinkedHashSet<String> cols = new LinkedHashSet<>();
        cols.add(COL_SPECTRA_NAMES);

        AttributeSchema schema = library.getSchema();
        Map<String, AttributeDef> schemaMap = schema.asMap();
        cols.addAll(schemaMap.keySet());

        SortedSet<String> extra = new TreeSet<>();
        for (SpectralProfile p : library.getProfiles()) {
            for (String k : p.getAttributes().keySet()) {
                if (!schemaMap.containsKey(k) && !COL_WKT.equalsIgnoreCase(k)) {
                    extra.add(k);
                }
            }
        }

        cols.remove(COL_WKT);
        cols.addAll(extra);
        cols.add(COL_WKT);

        List<String> header = new ArrayList<>(cols);
        List<List<String>> rows = new ArrayList<>();

        for (SpectralProfile p : library.getProfiles()) {
            List<String> row = new ArrayList<>(header.size());
            for (String col : header) {
                if (COL_SPECTRA_NAMES.equalsIgnoreCase(col.trim())) {
                    row.add(p.getName());
                } else {
                    row.add(CsvAttributeCodec.format(p.getAttributes().get(col)));
                }
            }
            rows.add(row);
        }

        CsvUtils.write(path, header, rows);
    }


    private static AttributeSchema ensureWktInSchema(AttributeSchema schema) {
        if (schema == null) {
            return new AttributeSchema();
        }
        if (schema.find(COL_WKT).isPresent()) {
            return schema;
        }

        Map<String, AttributeDef> m = new LinkedHashMap<>(schema.asMap());
        m.put(COL_WKT, AttributeDef.optional(COL_WKT, AttributeType.STRING));

        return new AttributeSchema(m);
    }


    private static int findNameColumn(List<String> header) {
        for (int i = 0; i < header.size(); i++) {
            String h = header.get(i);
            if (h == null) {
                continue;
            }
            String t = h.trim().toLowerCase(Locale.ROOT);
            if (t.equals(COL_SPECTRA_NAMES)) {
                return i;
            }
        }
        return -1;
    }

    private static String safeCell(List<String> row, int idx) {
        if (row == null) {
            return "";
        }
        if (idx < 0 || idx >= row.size()) {
            return "";
        }
        String v = row.get(idx);
        return v == null ? "" : v;
    }
}
