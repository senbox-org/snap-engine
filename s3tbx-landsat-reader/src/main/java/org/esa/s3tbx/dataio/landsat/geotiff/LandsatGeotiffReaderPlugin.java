/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.dataio.landsat.geotiff;

import com.bc.ceres.core.VirtualDir;
import org.esa.s3tbx.dataio.landsat.tgz.VirtualDirTgz;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * @author Thomas Storm
 */
public class LandsatGeotiffReaderPlugin implements ProductReaderPlugIn {

    private static final Class[] READER_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String[] FORMAT_NAMES = new String[]{"LandsatGeoTIFF"};
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{".txt", ".TXT", ".gz", ".tgz"};
    private static final String READER_DESCRIPTION = "Landsat Data Products (GeoTIFF)";

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        String filename = new File(input.toString()).getName();
        if (!LandsatTypeInfo.isLandsat(filename)) {
            return DecodeQualification.UNABLE;
        }

        VirtualDir virtualDir;
        try {
            virtualDir = getInput(input);
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        }

        return getDecodeQualification(virtualDir);

    }

    static DecodeQualification getDecodeQualification(VirtualDir virtualDir) {
        if (virtualDir == null) {
            return DecodeQualification.UNABLE;
        }

        String[] allFiles;
        try {
            allFiles = virtualDir.listAllFiles();
            if (allFiles == null || allFiles.length == 0) {
                return DecodeQualification.UNABLE;
            }
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        }

        for (String filePath : allFiles) {
            try {
                if (isMetadataFilename(new File(filePath).getName())) {
                    InputStream inputStream = virtualDir.getInputStream(filePath);
                    if (isMetadataFile(inputStream)) {
                        return DecodeQualification.INTENDED;
                    }
                }
            } catch (IOException ignore) {
                // file is broken, but be tolerant here
            }
        }
        // didn't find the expected metadata file
        return DecodeQualification.UNABLE;
    }

    static boolean isMetadataFilename(String filename) {
        return filename.toLowerCase().endsWith("_mtl.txt");
    }

    static boolean isMetadataFile(InputStream inputStream) {
        String firstLine;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            firstLine = reader.readLine();
        } catch (IOException e) {
            return false;
        }
        return firstLine != null && firstLine.trim().matches("GROUP = L1_METADATA_FILE");
    }

    @Override
    public Class[] getInputTypes() {
        return READER_INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new LandsatGeotiffReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return DEFAULT_FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return READER_DESCRIPTION;
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(FORMAT_NAMES[0], DEFAULT_FILE_EXTENSIONS, READER_DESCRIPTION);
    }

    static VirtualDir getInput(Object input) throws IOException {
        File inputFile = getFileInput(input);

        if (inputFile == null) {
            throw new IOException("Unknown input type.");
        }
        if (inputFile.isFile() && !isCompressedFile(inputFile)) {
            final File absoluteFile = inputFile.getAbsoluteFile();
            inputFile = absoluteFile.getParentFile();
            if (inputFile == null) {
                throw new IOException("Unable to retrieve parent to file: " + absoluteFile.getAbsolutePath());
            }
        }

        VirtualDir virtualDir = VirtualDir.create(inputFile);
        if (virtualDir == null) {
            virtualDir = new VirtualDirTgz(inputFile);
        }
        return virtualDir;
    }

    static File getFileInput(Object input) {
        if (input instanceof String) {
            return new File((String) input);
        } else if (input instanceof File) {
            return (File) input;
        }
        return null;
    }

    static boolean isCompressedFile(File file) {
        String extension = FileUtils.getExtension(file);
        if (StringUtils.isNullOrEmpty(extension)) {
            return false;
        }

        extension = extension.toLowerCase();

        return extension.contains("zip")
                || extension.contains("tar")
                || extension.contains("tgz")
                || extension.contains("gz")
                || extension.contains("tbz")
                || extension.contains("bz")
                || extension.contains("tbz2")
                || extension.contains("bz2");
    }
}
