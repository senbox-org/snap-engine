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

package org.esa.beam.dataio.landsat.geotiff;

import com.bc.ceres.core.VirtualDir;
import org.esa.beam.dataio.landsat.tgz.VirtualDirTgz;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;

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
    private static final String L4_FILENAME_REGEX = "LT4\\d{13}\\w{3}\\d{2}";
    private static final String L5_FILENAME_REGEX = "LT5\\d{13}.{3}\\d{2}";
    private static final String L7_FILENAME_REGEX = "LE7\\d{13}.{3}\\d{2}";
    private static final String L8_FILENAME_REGEX = "L[O,T,C]8\\d{13}.{3}\\d{2}";

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        String filename = new File(input.toString()).getName();
        if (!isLandsatMSSFilename(filename) &&
            !isLandsat4Filename(filename) &&
            !isLandsat5Filename(filename) &&
            !isLandsat7Filename(filename) &&
            !isLandsat8Filename(filename) &&
            !isLandsat5LegacyFilename(filename) &&
            !isLandsat7LegacyFilename(filename)) {
            return DecodeQualification.UNABLE;
        }

        VirtualDir virtualDir;
        try {
            virtualDir = getInput(input);
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        }

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
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAMES[0], DEFAULT_FILE_EXTENSIONS, READER_DESCRIPTION);
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

    static boolean isLandsatMSSFilename(String filename) {
        return filename.matches("LM[1-5]\\d{13}\\w{3}\\d{2}_MTL.(txt|TXT)");
    }

    static boolean isLandsat4Filename(String filename) {
        if (filename.matches(L4_FILENAME_REGEX + "_MTL" + getTxtExtension())) {
            return true;
        } else if (filename.matches(L4_FILENAME_REGEX + getCompressionExtension())) {
            return true;
        }
        return false;
    }

    static boolean isLandsat5Filename(String filename) {
        if (filename.matches(L5_FILENAME_REGEX + "_MTL" + getTxtExtension())) {
            return true;
        } else if (filename.matches(L5_FILENAME_REGEX + getCompressionExtension())) {
            return true;
        } else {
            return false;
        }
    }

    static boolean isLandsat7Filename(String filename) {
        if (filename.matches(L7_FILENAME_REGEX + "_MTL" + getTxtExtension())) {
            return true;
        } else if (filename.matches(L7_FILENAME_REGEX + getCompressionExtension())) {
            return true;
        } else {
            return false;
        }
    }

    static boolean isLandsat8Filename(String filename) {
        if (filename.matches(L8_FILENAME_REGEX + "_MTL" + getTxtExtension())) {
            return true;
        } else if (filename.matches(L8_FILENAME_REGEX + getCompressionExtension())) {
            return true;
        } else {
            return false;
        }
    }

    static boolean isLandsat5LegacyFilename(String filename) {
        if (filename.matches("LT5\\d{13}.{3}\\d{2}_MTL.(txt|TXT)")) {
            return true;
        } else if (filename.matches("L5\\d{6}_\\d{11}_MTL.(txt|TXT)")) {
            return true;
        } else if (filename.matches("LT5\\d{13}.{3}\\d{2}\\.tar\\.gz")) {
            return true;
        } else {
            return false;
        }
    }

    static boolean isLandsat7LegacyFilename(String filename) {
        if (filename.matches("LE7\\d{13}.{3}\\d{2}_MTL.(txt|TXT)")) {
            return true;
        } else if (filename.matches("L7\\d{7}_\\d{11}_MTL.(txt|TXT)")) {
            return true;
        } else if (filename.matches("LE7\\d{13}.{3}\\d{2}\\.tar\\.gz")) {
            return true;
        } else {
            return false;
        }
    }

    static boolean isCompressedFile(File file) {
        String extension = FileUtils.getExtension(file);
        if (StringUtils.isNullOrEmpty(extension)) {
            return false;
        }

        extension = extension.toLowerCase();

        return extension.contains("zip")
               || extension.contains("ZIP")
               || extension.contains("tar")
               || extension.contains("tgz")
               || extension.contains("gz")
               || extension.contains("tbz")
               || extension.contains("bz")
               || extension.contains("tbz2")
               || extension.contains("bz2");
    }

    private static String getCompressionExtension() {
        return "\\.(tar\\.gz|tgz|tar\\.bz|tbz|tar\\.bz2|tbz2|zip|ZIP)";
    }

    private static String getTxtExtension() {
        return "\\.(txt|TXT)";
    }
}
