/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.dataio.dimap;

import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.esa.snap.core.dataio.dimap.DimapProductConstants.DIMAP_DATA_DIRECTORY_EXTENSION;
import static org.esa.snap.core.dataio.dimap.DimapProductConstants.DIMAP_FORMAT_NAME;
import static org.esa.snap.core.dataio.dimap.DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION;


public class DimapFileFilter extends SnapFileFilter {
    // Used in snap-examples
    public DimapFileFilter() {
        super(DIMAP_FORMAT_NAME, DIMAP_HEADER_FILE_EXTENSION, "BEAM-DIMAP product files");
    }

    @Override
    public boolean accept(Path file) {
        if (Files.isRegularFile(file) && hasHeaderExt(file)) {
            return Files.isDirectory(FileUtils.exchangeExtension(file, DIMAP_DATA_DIRECTORY_EXTENSION));
        } else {
            return Files.isDirectory(file) && !isDataDir(file);
        }
    }

    @Override
    public boolean isCompoundDocument(Path dir) {
        return isDataDir(dir);
    }

    private boolean isDataDir(Path dir) {
        return hasDataExt(dir) && Files.isRegularFile(FileUtils.exchangeExtension(dir, DIMAP_HEADER_FILE_EXTENSION));
    }

    private boolean hasHeaderExt(Path file) {
        return file.getFileName().toString().endsWith(DIMAP_HEADER_FILE_EXTENSION);
    }

    private boolean hasDataExt(Path file) {
        return file.getFileName() != null && file.getFileName().toString().endsWith(DIMAP_DATA_DIRECTORY_EXTENSION);
    }
}
