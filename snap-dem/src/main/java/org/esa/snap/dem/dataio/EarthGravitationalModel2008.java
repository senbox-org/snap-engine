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

    private static volatile EarthGravitationalModel2008 theInstance = null;
    // a failed load is not retried in this session: every DEM tile would otherwise try the download again
    private static volatile IOException loadFailure = null;
    private static volatile boolean fallbackWarned = false;

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
            throw loadFailure;
        }
        try {
            File zip = installedZipFile();
            try {
                theInstance = new EarthGravitationalModel2008(zip);
            } catch (IOException corrupt) {
                // a damaged local copy would otherwise be used, and fail, forever: fetch it once more
                SystemUtils.LOG.warning("Discarding unreadable " + zip + ": " + corrupt.getMessage());
                if (!zip.delete()) {
                    throw corrupt;
                }
                zip = installedZipFile();
                theInstance = new EarthGravitationalModel2008(zip);
            }
        } catch (IOException e) {
            loadFailure = e;
            throw e;
        }
    }

    /**
     * EGM2008, or EGM96 with a one-time warning when the EGM2008 grid cannot be loaded (for example while it is not
     * yet available for download). Heights referenced to EGM2008 are then converted with EGM96, as before.
     */
    public static GeoidModel instanceOrEGM96() throws IOException {
        try {
            return instance();
        } catch (IOException e) {
            if (!fallbackWarned) {
                fallbackWarned = true;
                SystemUtils.LOG.warning("EGM2008 geoid grid unavailable (" + e.getMessage() +
                        "); converting EGM2008 heights with EGM96 instead");
            }
            return EarthGravitationalModel96.instance();
        }
    }

    private static File installedZipFile() throws IOException {
        final File installDir = new File(SystemUtils.getAuxDataPath().resolve("dem").toFile(), "egm2008");
        if (!installDir.exists() && !installDir.mkdirs()) {
            throw new IOException("Unable to create " + installDir);
        }
        File localZipFile = new File(installDir, ZIPNAME);
        if (!localZipFile.exists()) {
            final URL remoteURL = new URL(remoteHTTP);
            localZipFile = DownloadableContentImpl.getRemoteHttpFile(remoteURL, localZipFile);
            if (localZipFile == null) {
                throw new IOException("Unable to download " + remoteURL + ZIPNAME);
            }
        }
        return localZipFile;
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
