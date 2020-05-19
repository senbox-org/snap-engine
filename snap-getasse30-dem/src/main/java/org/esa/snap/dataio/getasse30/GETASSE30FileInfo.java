/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.getasse30;

import org.esa.snap.core.dataop.dem.EastingNorthingParser;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Holds information about a GETASSE30 file.
 *
 * @author Norman Fomferra
 */
public class GETASSE30FileInfo {

    private String _fileName;
    private long _fileSize;
    private float _easting;
    private float _northing;
    private float _pixelSizeX;
    private float _pixelSizeY;
    private int _width;
    private int _height;
    private float _noDataValue;

    private GETASSE30FileInfo() {
    }

    public String getFileName() {
        return _fileName;
    }

    public long getFileSize() {
        return _fileSize;
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
        return _width;
    }

    public int getHeight() {
        return _height;
    }

    public float getNoDataValue() {
        return _noDataValue;
    }

    public static GETASSE30FileInfo create(final File file) throws IOException {
        return createFromDataFile(file);
    }

    private static GETASSE30FileInfo createFromDataFile(final File dataFile) throws IOException {
        final GETASSE30FileInfo fileInfo = new GETASSE30FileInfo();
        fileInfo.setFromData(dataFile);
        return fileInfo;
    }

    private ZipEntry getZipEntryIgnoreCase(final ZipFile zipFile, final String name) {
        final Enumeration enumeration = zipFile.entries();
        while (enumeration.hasMoreElements()) {
            final ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();
            if (zipEntry.getName().equalsIgnoreCase(name)) {
                return zipEntry;
            }
        }
        return null;
    }

    private void setFromData(final File dataFile) throws IOException {
        final String ext = FileUtils.getExtension(dataFile.getName());
        if (".zip".equalsIgnoreCase(ext)) {
            final String baseName = FileUtils.getFilenameWithoutExtension(dataFile.getName()) + ".GETASSE30";
            final ZipFile zipFile = new ZipFile(dataFile);
            try {
                final ZipEntry zipEntry = getZipEntryIgnoreCase(zipFile, baseName);
                if (zipEntry == null) {
                    throw new IOException("Entry '" + baseName + "' not found in zip file.");
                }
                setFromData(baseName, zipEntry.getSize());
            } finally {
                zipFile.close();
            }
        } else {
            setFromData(dataFile.getName(), dataFile.length());
        }
    }

    public void setFromData(final String fileName, final long fileSize) throws IOException {
        _fileName = fileName;
        _fileSize = fileSize;

        final int[] eastingNorthing;
        try {
            eastingNorthing = parseEastingNorthing(fileName);
        } catch (ParseException e) {
            throw new IOException("Illegal file name format: " + fileName, e);
        }
        _easting = eastingNorthing[0];
        _northing = eastingNorthing[1];

        _width = (int) Math.sqrt(fileSize / 2);
        _height = _width;
        if (_width * _height * 2L != fileSize) {
            throw new IOException("Illegal file size: " + fileSize);
        }

        _pixelSizeX = 30.0F / (60.0F * 60.0F);  // 30 arcsecond product
        _pixelSizeY = _pixelSizeX;

        _noDataValue = GETASSE30ElevationModelDescriptor.NO_DATA_VALUE;
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
