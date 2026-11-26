/*
 * Copyright (C) 2026 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.snap.dem.dataio;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.download.DownloadableContentImpl;
import org.esa.snap.engine_utilities.util.Maths;
import org.esa.snap.engine_utilities.util.Settings;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * EGM2008 geoid undulations on a 5 arc-minute grid.
 * <p>
 * The Copernicus DEM (GLO-30, GLO-90) gives orthometric heights relative to EGM2008, so converting it to
 * ellipsoidal heights needs EGM2008, not EGM96: the two geoids differ by about 0.5 m RMS and by several
 * metres in poorly surveyed regions.
 * <p>
 * The grid file {@value #NAME} (in {@value #ZIPNAME}) holds 2161 rows (90N to 90S) by 4320 columns (180W to
 * 180E - 5'), pixel-is-point, row-major, as big-endian 16-bit integers in centimetres. It is every second node of
 * NGA's 2.5' EGM2008 grid (tide-free, as distributed by PROJ as us_nga_egm08_25.tif); see
 * snap-dem/tools/make_egm2008_5min.py. Rounding to centimetres changes values by at most 5 mm.
 */
public final class EarthGravitationalModel2008 implements GeoidModel {

    static final String NAME = "egm2008_5min_cm.bin";
    static final String ZIPNAME = "egm2008_5min_cm.zip";
    static final int NUM_LATS = 2161;   // 180 * 12 + 1, 90N to 90S
    static final int NUM_LONS = 4320;   // 360 * 12, 180W to 180E - 5'
    private static final double STEP = 1.0 / 12.0;

    private static final String remoteHTTP =
            Settings.instance().get("DEM.egm2008_HTTP", "https://step.esa.int/auxdata/dem/egm2008/");

    /** Puts the grid zip at the given local path (normally by downloading it); replaceable by tests. */
    interface ZipFetcher {
        File fetch(File localZipFile) throws IOException;
    }

    static final ZipFetcher DOWNLOAD = localZipFile -> {
        // the file name is appended to this folder URL
        final URL remoteURL = new URL(remoteHTTP.endsWith("/") ? remoteHTTP : remoteHTTP + '/');
        final File zip = DownloadableContentImpl.getRemoteHttpFile(remoteURL, localZipFile);
        if (zip == null) {
            throw new IOException("Unable to download " + remoteURL + ZIPNAME);
        }
        return zip;
    };

    private static File installDir = null;          // null: <SNAP auxdata>/dem/egm2008
    private static ZipFetcher fetcher = DOWNLOAD;
    private static volatile EarthGravitationalModel2008 theInstance = null;
    // A failed load is retried only after RETRY_INTERVAL_MS: every DEM tile would otherwise try the download
    // again, but a transient failure (offline, proxy, server error) must not pin a long session to EGM96.
    static final long RETRY_INTERVAL_MS = 10 * 60 * 1000L;
    static long retryIntervalMs = RETRY_INTERVAL_MS;
    private static IOException loadFailure = null;
    private static long loadFailureTime = 0L;
    private static boolean fallbackWarned = false;

    private final short[] undulationCm;

    public static EarthGravitationalModel2008 instance() throws IOException {
        if (theInstance == null) {
            create();
        }
        return theInstance;
    }

    private static synchronized void create() throws IOException {
        if (theInstance != null) {
            return;
        }
        if (loadFailure != null) {
            if ((System.nanoTime() - loadFailureTime) / 1_000_000L < retryIntervalMs) {
                throw loadFailure;
            }
            loadFailure = null;
        }
        final File dir = installDir != null ? installDir :
                new File(SystemUtils.getAuxDataPath().resolve("dem").toFile(), "egm2008");
        try {
            theInstance = load(dir, fetcher);
        } catch (IOException e) {
            throw failed(e);
        } catch (RuntimeException e) {
            // e.g. from the download helper: must not escape into every Copernicus DEM tile
            throw failed(new IOException("Unable to load the EGM2008 geoid grid: " + e, e));
        }
    }

    private static IOException failed(final IOException e) {
        loadFailure = e;
        loadFailureTime = System.nanoTime();   // monotonic: a clock change must not shorten the wait
        fallbackWarned = false;     // warn again for every failed attempt
        return e;
    }

    /**
     * Loads the grid from {@code dir}, fetching it first if it is missing. A local copy that cannot be read
     * (an interrupted or damaged download) would otherwise fail forever, so it is deleted and fetched once more.
     */
    static EarthGravitationalModel2008 load(final File dir, final ZipFetcher fetcher) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Unable to create " + dir);
        }
        File zip = new File(dir, ZIPNAME);
        if (!zip.exists()) {
            zip = fetcher.fetch(zip);
        }
        try {
            return new EarthGravitationalModel2008(zip);
        } catch (IOException corrupt) {
            SystemUtils.LOG.warning("Discarding unreadable " + zip + ": " + corrupt.getMessage());
            if (!zip.delete()) {
                throw corrupt;
            }
            return new EarthGravitationalModel2008(fetcher.fetch(new File(dir, ZIPNAME)));
        }
    }

    /** Non-API: points the singleton at another folder and fetcher, and forgets any loaded grid or failure. */
    static synchronized void resetForTests(final File dir, final ZipFetcher zipFetcher) {
        installDir = dir;
        fetcher = zipFetcher != null ? zipFetcher : DOWNLOAD;
        theInstance = null;
        loadFailure = null;
        loadFailureTime = 0L;
        fallbackWarned = false;
        retryIntervalMs = RETRY_INTERVAL_MS;
    }

    /**
     * EGM2008, or EGM96 with a warning when the EGM2008 grid cannot be loaded (for example offline on first use).
     * Heights referenced to EGM2008 are then converted with EGM96, as before this model existed. The load is
     * tried again {@value #RETRY_INTERVAL_MS} ms after a failure, so a transient failure does not last the session.
     */
    public static GeoidModel instanceOrEGM96() throws IOException {
        try {
            return instance();
        } catch (IOException e) {
            warnFallback(e);
            return EarthGravitationalModel96.instance();
        }
    }

    private static synchronized void warnFallback(final IOException e) {
        if (!fallbackWarned) {
            fallbackWarned = true;
            SystemUtils.LOG.severe("EGM2008 geoid grid unavailable (" + e.getMessage() + "); Copernicus DEM " +
                    "heights are converted with EGM96 instead, which differs by up to 12 m. Retrying in " +
                    retryIntervalMs / 60000 + " minutes.");
        }
    }

    /** Loads the grid from the given zip file. Non-API: also used by tests. */
    EarthGravitationalModel2008(final File zip) throws IOException {
        undulationCm = new short[NUM_LATS * NUM_LONS];
        try (final ZipFile zipFile = new ZipFile(zip)) {
            final ZipEntry entry = zipFile.getEntry(NAME);
            if (entry == null) {
                throw new IOException(zip + " does not contain " + NAME);
            }
            try (final InputStream in = zipFile.getInputStream(entry);
                 final DataInputStream data = new DataInputStream(new BufferedInputStream(in, 1 << 16))) {
                for (int i = 0; i < undulationCm.length; i++) {
                    undulationCm[i] = data.readShort();
                }
                if (data.read() != -1) {
                    throw new IOException(NAME + " is larger than " + NUM_LATS + " x " + NUM_LONS + " values");
                }
            } catch (EOFException e) {
                throw new IOException(NAME + " is smaller than " + NUM_LATS + " x " + NUM_LONS + " values", e);
            }
        }
    }

    /** Geoid undulation in metres at grid node (row, col); rows are clamped, columns wrap around. */
    float node(final int row, final int col) {
        final int r = Math.min(Math.max(row, 0), NUM_LATS - 1);
        final int c = Math.floorMod(col, NUM_LONS);
        return undulationCm[r * NUM_LONS + c] * 0.01f;
    }

    public float getEGM(final double lat, final double lon) {
        return getEGM(lat, lon, new double[4][4]);
    }

    @Override
    public float getEGM(final double lat, final double lon, final double[][] v) {
        final double r = (90.0 - lat) / STEP;
        final double c = (lon + 180.0) / STEP;
        final int r0 = (int) Math.floor(r) - 1;
        final int c0 = (int) Math.floor(c) - 1;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                v[i][j] = node(r0 + i, c0 + j);
            }
        }
        return (float) Maths.interpolationBiCubic(v, c - (c0 + 1), r - (r0 + 1));
    }
}
