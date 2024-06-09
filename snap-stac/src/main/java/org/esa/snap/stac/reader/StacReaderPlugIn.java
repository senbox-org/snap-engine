/*
 * Copyright (C) 2024 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.snap.stac.reader;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.stac.StacItem;
import org.json.simple.JSONObject;

import java.io.File;
import java.util.Locale;

public class StacReaderPlugIn implements ProductReaderPlugIn {

    public static final String[] FORMAT_NAMES = new String[]{"STAC"};
    public static final String[] JSON_FILE_EXTENSION = {".json"};

    public StacReaderPlugIn() {

    }

    @Override
    public DecodeQualification getDecodeQualification(Object productInputFile) {
        if (productInputFile instanceof String) {
            try {
                if (((String) productInputFile).startsWith("http")) {
                    new StacItem((String) productInputFile);
                } else {
                    new StacItem(new File((String) productInputFile));
                }
                return DecodeQualification.INTENDED;
            } catch (Exception e) {
                return DecodeQualification.UNABLE;
            }
        } else if (productInputFile instanceof File) {
            try {
                new StacItem((File) productInputFile);
            } catch (Exception e) {
                return DecodeQualification.UNABLE;
            }
            return DecodeQualification.INTENDED;
        } else if (productInputFile instanceof JSONObject) {
            try {
                new StacItem((JSONObject) productInputFile);
            } catch (Exception e) {
                return DecodeQualification.UNABLE;
            }
            return DecodeQualification.INTENDED;
        } else if (productInputFile instanceof StacItem) {
            return DecodeQualification.INTENDED;
        } else {
            return DecodeQualification.UNABLE;
        }
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[0];
    }

    @Override
    public ProductReader createReaderInstance() {
        return new StacReader(this);
    }


    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return JSON_FILE_EXTENSION;
    }

    @Override
    public String getDescription(Locale locale) {
        return "STAC Item.";
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return null;
    }
}
