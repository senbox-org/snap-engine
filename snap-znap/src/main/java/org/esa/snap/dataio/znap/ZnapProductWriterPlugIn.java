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
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.DEFAULT_USE_ZIP_ARCHIVE;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_USE_ZIP_ARCHIVE;

import org.esa.snap.core.dataio.EncodeQualification;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.runtime.Config;

import java.util.Locale;
import java.util.prefs.Preferences;

public class ZnapProductWriterPlugIn implements ProductWriterPlugIn {

    public EncodeQualification getEncodeQualification(Product product) {
        return new EncodeQualification(EncodeQualification.Preservation.FULL);
    }

    public Class<?>[] getOutputTypes() {
        return IO_TYPES;
    }

    public ProductWriter createWriterInstance() {
        return new ZnapProductWriter(this);
    }

    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    public String[] getDefaultFileExtensions() {
        if (isUseZipArchive()) {
            return new String[]{ZNAP_ZIP_CONTAINER_EXTENSION};
        } else {
            return new String[]{ZNAP_CONTAINER_EXTENSION};
        }
    }

    public String getDescription(Locale locale) {
        return FORMAT_NAME + " product writer";
    }

    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

    static boolean isUseZipArchive() {
        final Preferences preferences = Config.instance("snap").load().preferences();
        String value = preferences.get(PROPERTY_NAME_USE_ZIP_ARCHIVE, Boolean.toString(DEFAULT_USE_ZIP_ARCHIVE));
        value = value != null ? value.trim() : null;
        return Boolean.parseBoolean(value);
    }
}
