/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dem.dataio.ace;

import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.dataop.dem.EastingNorthingParser;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Holds information about a ACE file.
 *
 * @author Norman Fomferra
 */
public class ACEFileInfo {

    private String _fileName;
    private float _easting;
    private float _northing;
    private float _pixelSizeX;
    private float _pixelSizeY;
    private float _noDataValue;

    private ACEFileInfo() {
    }

    public String getFileName() {
        return _fileName;
    }

    public float getEasting() {
        return _easting;
    }

    public float getNorthing() {
        return _northing;
    }

    public float getPixelSizeX() {
        return _pixelSizeX;
    }

    public float getPixelSizeY() {
        return _pixelSizeY;
    }

    public int getWidth() {
        return 1800;
    }

    public int getHeight() {
        return 1800;
    }

    public float getNoDataValue() {
        return _noDataValue;
    }

    public static ACEFileInfo create(final Path path) throws IOException {
        final ACEFileInfo fileInfo = new ACEFileInfo();
        fileInfo.setFromData(path);
        return fileInfo;
    }

    private static ZipEntry getZipEntryIgnoreCase(final ZipFile zipFile, final String name) {
        final Enumeration enumeration = zipFile.entries();
        while (enumeration.hasMoreElements()) {
            final ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();
            if (zipEntry.getName().equalsIgnoreCase(name)) {
                return zipEntry;
            }
        }
        return null;
    }

    private void setFromData(final Path dataPath) throws IOException {
        final String fileName = dataPath.getFileName().toString();
        final String ext = FileUtils.getExtension(fileName);
        if (ext != null && ext.equalsIgnoreCase(".zip")) {
            final String baseName = FileUtils.getFilenameWithoutExtension(fileName) + ".ACE";
            final ZipFile zipFile = new ZipFile(dataPath.toFile());
            try {
                final ZipEntry zipEntry = getZipEntryIgnoreCase(zipFile, baseName);
                if (zipEntry == null) {
                    throw new IOException("Entry '" + baseName + "' not found in zip file.");
                }
                setFromData(baseName);
            } finally {
                zipFile.close();
            }
        } else {
            setFromData(fileName);
        }
    }

    void setFromData(final String fileName) throws IOException {
        _fileName = fileName;

        final int[] eastingNorthing;
        try {
            eastingNorthing = parseEastingNorthing(fileName);
        } catch (ParseException e) {
            throw new IOException("Illegal file name format: " + fileName, e);
        }
        _easting = eastingNorthing[0];
        _northing = eastingNorthing[1];

        _pixelSizeX = 30.0F / (60.0F * 60.0F);  // 30 arcsecond product
        _pixelSizeY = _pixelSizeX;

        _noDataValue = ACEElevationModelDescriptor.NO_DATA_VALUE;
    }

    static int[] parseEastingNorthing(final String fileName) throws ParseException {
        Guardian.assertNotNullOrEmpty("fileName", fileName);
        final EastingNorthingParser parser = new EastingNorthingParser(fileName, "\\..+");
        return parser.parse();
    }

    public static boolean isValidFileSize(final long size) {
        if (size > 0 && size % 2 == 0) {
            final long w = (long) Math.sqrt(size / 2);
            if (2 * w * w == size) {
                return true;
            }
        }
        return false;
    }
}
