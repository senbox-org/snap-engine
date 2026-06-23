package org.esa.snap.dem.dataio.copernicus.copernicus90m;

import org.esa.snap.core.util.SystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class Copernicus90mMissingTileIndex {

    private static final String RESOURCE_NAME = "missing-tiles.txt";
    private static final Set<String> MISSING_TILES = loadMissingTiles();

    private Copernicus90mMissingTileIndex() {
    }

    static boolean isMissing(final String latToken, final String lonToken) {
        return MISSING_TILES.contains(latToken + '_' + lonToken);
    }

    private static Set<String> loadMissingTiles() {
        final InputStream stream = Copernicus90mMissingTileIndex.class.getResourceAsStream(RESOURCE_NAME);
        if (stream == null) {
            SystemUtils.LOG.warning("Copernicus 90m missing tile index resource not found: " + RESOURCE_NAME);
            return Collections.emptySet();
        }

        final Set<String> missingTiles = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final String tileName = line.trim();
                if (!tileName.isEmpty() && !tileName.startsWith("#")) {
                    missingTiles.add(tileName);
                }
            }
        } catch (IOException e) {
            SystemUtils.LOG.warning("Unable to read Copernicus 90m missing tile index: " + e.getMessage());
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(missingTiles);
    }
}
