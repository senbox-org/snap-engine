package org.esa.snap.speclib.io.envi;

import org.esa.snap.dataio.envi.EnviConstants;
import org.esa.snap.speclib.io.csv.CsvSpectralLibrarySidecarIO;
import org.esa.snap.speclib.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


public class EnviCsvSidecarSupport {


    public static Path resolveCsvPath(Path enviPath) {
        Objects.requireNonNull(enviPath, "enviPath must not be null");
        Path hdrPath = resolveHdrPath(enviPath);
        String base = stripExtension(hdrPath.getFileName().toString());
        return hdrPath.resolveSibling(base + ".csv");
    }

    public static boolean hasAnyAttributes(SpectralLibrary lib) {
        if (lib == null) {
            return false;
        }

        AttributeSchema schema = lib.getSchema();
        if (schema != null && !schema.asMap().isEmpty()) {
            return true;
        }

        for (SpectralProfile p : lib.getProfiles()) {
            if (p != null && p.getAttributes() != null && !p.getAttributes().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static void writeIfNeeded(SpectralLibrary lib, Path enviPath) throws IOException {
        Objects.requireNonNull(lib, "lib must not be null");
        Objects.requireNonNull(enviPath, "enviPath must not be null");
        if (!hasAnyAttributes(lib)) {
            return;
        }
        Path csvPath = resolveCsvPath(enviPath);
        Files.createDirectories(csvPath.toAbsolutePath().getParent());
        new CsvSpectralLibrarySidecarIO().write(lib, csvPath);
    }

    public static SpectralLibrary mergeIfPresent(SpectralLibrary enviLib, Path enviPath) throws IOException {
        Objects.requireNonNull(enviLib, "enviLib must not be null");
        Objects.requireNonNull(enviPath, "enviPath must not be null");

        Path csvPath = resolveCsvPath(enviPath);
        if (!Files.exists(csvPath)) {
            return enviLib;
        }

        SpectralLibrary csvLib = new CsvSpectralLibrarySidecarIO().read(csvPath);

        List<SpectralProfile> enviProfiles = enviLib.getProfiles();
        List<SpectralProfile> csvProfiles = csvLib.getProfiles();

        List<SpectralProfile> mergedProfiles = new ArrayList<>(enviProfiles.size());

        Map<String, AttributeDef> mergedMap = new LinkedHashMap<>(enviLib.getSchema().asMap());

        boolean csvHasWkt = csvLib.getSchema().find(CsvSpectralLibrarySidecarIO.COL_WKT).isPresent() || csvProfiles.stream().anyMatch(p -> p != null && p.getAttributes().containsKey(CsvSpectralLibrarySidecarIO.COL_WKT));
        if (csvHasWkt && !mergedMap.containsKey(CsvSpectralLibrarySidecarIO.COL_WKT)) {
            mergedMap.put(CsvSpectralLibrarySidecarIO.COL_WKT, AttributeDef.optional(CsvSpectralLibrarySidecarIO.COL_WKT, AttributeType.STRING));
        }
        AttributeSchema mergedSchema = new AttributeSchema(mergedMap);

        int n = Math.min(enviProfiles.size(), csvProfiles.size());
        for (int i = 0; i < n; i++) {
            SpectralProfile pEnvi = enviProfiles.get(i);
            SpectralProfile pCsv = csvProfiles.get(i);

            SpectralProfile out = pEnvi;

            if (pCsv != null && pCsv.getAttributes() != null && !pCsv.getAttributes().isEmpty()) {
                for (var e : pCsv.getAttributes().entrySet()) {
                    out = out.withAttribute(e.getKey(), e.getValue());
                }
                mergedSchema.inferFromAttributes(pCsv.getAttributes());
            }

            mergedProfiles.add(out);
        }

        for (int i = n; i < enviProfiles.size(); i++) {
            mergedProfiles.add(enviProfiles.get(i));
        }

        return new SpectralLibrary(
                enviLib.getId(),
                enviLib.getName(),
                enviLib.getAxis(),
                enviLib.getDefaultYUnit().orElse(null),
                mergedProfiles,
                mergedSchema
        );
    }



    private static Path resolveHdrPath(Path path) {
        String name = path.getFileName().toString();
        if (name.toLowerCase(Locale.ROOT).endsWith(EnviConstants.HDR_EXTENSION)) {
            return path;
        }
        return path.resolveSibling(stripExtension(name) + EnviConstants.HDR_EXTENSION);
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
