/*
 *
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 */

package org.esa.snap.dataio.znap;

import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.*;
import static com.bc.zarr.ZarrConstants.*;

import com.bc.zarr.storage.ZipStore;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ZarrProductReaderPlugIn implements ProductReaderPlugIn {

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final Path inputPath = convertToPath(input);
        if (inputPath == null) {
            return DecodeQualification.UNABLE;
        }
        final Path productRoot;
        final String lowerName = inputPath.getFileName().toString().toLowerCase();
        if (lowerName.endsWith(".znap.zip") || lowerName.endsWith(".znap")) {
            productRoot = inputPath;
        } else {
            productRoot = inputPath.getParent();
        }
        final boolean isValidRootDirName = productRoot.getFileName().toString().toLowerCase().endsWith(SNAP_ZARR_CONTAINER_EXTENSION);
        if (isValidRootDirName) {
            final boolean productRootIsDirectory = Files.isDirectory(productRoot);
            final Path productHeader = productRoot.resolve(FILENAME_DOT_ZGROUP);
            final boolean productHeaderExist = Files.exists(productHeader);
            final boolean productHeaderIsFile = Files.isRegularFile(productHeader);

            if (productRootIsDirectory && productHeaderExist && productHeaderIsFile) {
                try {
                    final Stream<Path> stream = Files.find(productRoot, 3,
                                                           (path, basicFileAttributes) -> Files.isRegularFile(path) && path.endsWith(FILENAME_DOT_ZARRAY),
                                                           FileVisitOption.FOLLOW_LINKS);
                    final List<Path> pathList = stream.collect(Collectors.toList());
                    if (pathList.size() > 0) {
                        // TODO: 23.07.2019 SE -- Frage 2 siehe Trello https://trello.com/c/HMw8CxqL/4-fragen-an-norman
                        return DecodeQualification.INTENDED;
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        final boolean isValidZnapZipArchiveName = productRoot.getFileName().toString().toLowerCase().endsWith(SNAP_ZARR_ZIP_CONTAINER_EXTENSION);
        if (isValidZnapZipArchiveName) {
            try (ZipStore zipStore = new ZipStore(productRoot)) {
                final InputStream productHeaderStream = zipStore.getInputStream(FILENAME_DOT_ZGROUP);
                final boolean productHeaderExist = productHeaderStream != null;
                if (productHeaderExist) {
                    final TreeSet<String> arrayKeys = zipStore.getArrayKeys();
                    if (arrayKeys.size() > 0) {
                        return DecodeQualification.INTENDED;
                    }
                }
            } catch (IOException e) {
                return DecodeQualification.UNABLE;
            }
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public Class<?>[] getInputTypes() {
        return IO_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new ZarrProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{SNAP_ZARR_CONTAINER_EXTENSION, SNAP_ZARR_ZIP_CONTAINER_EXTENSION};
    }

    @Override
    public String getDescription(Locale locale) {
        return FORMAT_NAME + " product reader";
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null)) {

            @Override
            public boolean accept(File file) {
                return file != null && (file.isDirectory() || isZnapZipArchive(file) || isFileInZnapRootDir(file));
            }

            @Override
            public boolean isCompoundDocument(File dir) {
                return dir != null && isZnapRootDir(dir.getParentFile());
            }

            private boolean isFileInZnapRootDir(File file) {
                return file != null && isZnapRootDir(file.getParentFile());
            }

            private boolean isZnapZipArchive(File file) {
                return file.isFile() && hasZipArchiveExtension(file);
            }

            private boolean isZnapRootDir(File file) {
                return file != null && file.isDirectory() && hasContainerExtension(file);
            }

            private boolean hasZipArchiveExtension(File file) {
                return file.getName().endsWith(SNAP_ZARR_ZIP_CONTAINER_EXTENSION);
            }

            private boolean hasContainerExtension(File file) {
                return file.getName().endsWith(SNAP_ZARR_CONTAINER_EXTENSION);
            }
        };
    }
}
