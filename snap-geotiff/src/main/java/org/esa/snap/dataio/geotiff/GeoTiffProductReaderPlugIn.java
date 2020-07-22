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
package org.esa.snap.dataio.geotiff;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.engine_utilities.util.FileSystemUtils;
import org.esa.snap.engine_utilities.util.ZipFileSystemBuilder;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;

public class GeoTiffProductReaderPlugIn implements ProductReaderPlugIn {

    public static final String[] FORMAT_NAMES = new String[]{"GeoTIFF"};
    public static final String[] TIFF_FILE_EXTENSION = {".tif", ".tiff", ".gtif", ".btf"};
    public static final String ZIP_FILE_EXTENSION = ".zip";
    private static final String[] ALL_FILE_EXTENSIONS = StringUtils.addToArray(TIFF_FILE_EXTENSION, ZIP_FILE_EXTENSION);

    public GeoTiffProductReaderPlugIn() {
    }

    @Override
    public MetadataInspector getMetadataInspector() {
        return new GeoTiffMetadataInspector();
    }

    @Override
    public DecodeQualification getDecodeQualification(Object productInputFile) {
        try {
            Path productPath = null;
            if (productInputFile instanceof String) {
                productPath = Paths.get((String) productInputFile);
            } else if (productInputFile instanceof File) {
                productPath = ((File) productInputFile).toPath();
            } else if (productInputFile instanceof Path) {
                productPath = (Path) productInputFile;
            } else if (productInputFile instanceof InputStream) {
                try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(productInputFile)) {
                    return getDecodeQualificationImpl(imageInputStream);
                }
            }
            if (productPath != null) {
                String fileExtension = FileUtils.getExtension(productPath.getFileName().toString());
                if (fileExtension != null) {
                    boolean extensionMatches = Arrays.stream(TIFF_FILE_EXTENSION).anyMatch(fileExtension::equalsIgnoreCase);
                    if (extensionMatches) {
                        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(productInputFile)) {
                            return getDecodeQualificationImpl(imageInputStream);
                        }
                    } else if (fileExtension.equalsIgnoreCase(ZIP_FILE_EXTENSION)) {
                        return checkZipArchive(productPath);
                    }
                }
            }
        } catch (Exception ignore) {
            // nothing to do, return value is already UNABLE
        }
        return DecodeQualification.UNABLE;
    }

    private DecodeQualification checkZipArchive(Path productPath) throws IOException, IllegalAccessException, InstantiationException, InvocationTargetException {
        try (FileSystem fileSystem = ZipFileSystemBuilder.newZipFileSystem(productPath)) {
            TreeSet<String> filePaths = FileSystemUtils.listAllFilePaths(fileSystem);
            // RapidEye reader returns UNABLE as DecodeQualification on Mac. This
            // Incomplete and useless data from a zip file is opened as GeoTiff instead. This can be disturbing for users.
            // Even though it is not good that the GeoTiff reader has knowledge about RapidEye, no better solution was found so far.
            boolean foundNtif = false;
            Iterator<String> itFileNames = filePaths.iterator();
            while (itFileNames.hasNext() && !foundNtif) {
                String filePath = itFileNames.next();
                if (filePath.endsWith("ntf")) {
                    foundNtif = true;
                }
            }

            boolean foundTiff = false;
            int entryCount = 0;
            itFileNames = filePaths.iterator();
            while (itFileNames.hasNext()) {
                entryCount++;
                String filePath = itFileNames.next().toLowerCase();
                boolean extensionMatch = Arrays.stream(TIFF_FILE_EXTENSION).anyMatch(filePath::endsWith);
                if (extensionMatch) {
                    int startIndex = 0;
                    if (filePath.startsWith(fileSystem.getSeparator())) {
                        startIndex = fileSystem.getSeparator().length(); // the file path starts with '/' (the root folder in the zip archive)
                    }
                    if (filePath.indexOf(fileSystem.getSeparator(), startIndex) < 0) {
                        foundTiff = true;
                    }
                }
                if (!foundNtif && foundTiff && entryCount > 1) {
                    return DecodeQualification.SUITABLE;        // not exclusively a zipped tiff
                }
            }
            if (foundTiff && entryCount == 1) {
                return DecodeQualification.SUITABLE;    // only zipped tiff
            }
        }
        return DecodeQualification.UNABLE;
    }

    static DecodeQualification getDecodeQualificationImpl(ImageInputStream stream) {
        try {
            String mode = Utils.getTiffMode(stream);
            if ("Tiff".equals(mode)) {
                if (isImageReaderAvailable(stream)) {
                    return DecodeQualification.SUITABLE;
                }
            }
        } catch (Exception e) {
            return DecodeQualification.UNABLE;
        }
        return DecodeQualification.UNABLE;
    }

    private static boolean isImageReaderAvailable(ImageInputStream stream) throws Exception {
        Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(stream);
        while (imageReaders.hasNext()) {
            final ImageReader reader = imageReaders.next();
            if (reader instanceof TIFFImageReader) {
                // 2020-07-21 CC Added COG check
                TIFFImageReader tiffImageReader = (TIFFImageReader) reader;
                tiffImageReader.setInput(stream);
                return !Utils.isCOGGeoTIFF(tiffImageReader);
            }
        }
        return false;
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class, InputStream.class,};
    }

    @Override
    public ProductReader createReaderInstance() {
        return new GeoTiffProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return ALL_FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return "GeoTIFF data product.";
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(FORMAT_NAMES[0], getDefaultFileExtensions(), getDescription(null));
    }
}
