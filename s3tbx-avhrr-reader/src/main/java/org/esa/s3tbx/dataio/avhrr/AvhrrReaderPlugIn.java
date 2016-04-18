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
package org.esa.s3tbx.dataio.avhrr;

import org.esa.s3tbx.dataio.avhrr.noaa.KlmAvhrrFile;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;
import java.util.Locale;

/**
 * The plug-in class for the {@link org.esa.s3tbx.dataio.avhrr.AvhrrReader AVHRR/3 Level-1b reader}.
 *
 * @see <a href="http://www2.ncdc.noaa.gov/doc/klm/">NOAA KLM User's Guide</a>
 */
public class AvhrrReaderPlugIn implements ProductReaderPlugIn {

    public static final String FORMAT_NAME = "NOAA_AVHRR_3_L1B";

    private static final String[] FILE_EXTENSIONS = new String[]{""};
    private static final String DESCRIPTION = "NOAA-AVHRR/3 Level-1b Data Product";
    private static final Class[] INPUT_TYPES = new Class[]{
            String.class,
            File.class,
    };

    static {
        registerRGBProfiles();
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final File file = getInputFile(input);
        if (file != null && KlmAvhrrFile.canDecode(file)) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }

    public static File getInputFile(Object input) {
        File file = null;
        if (input instanceof String) {
            file = new File((String) input);
        } else if (input instanceof File) {
            file = (File) input;
        }
        return file;
    }

    @Override
    public Class[] getInputTypes() {
        return INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new AvhrrReader(this);
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{
                FORMAT_NAME
        };
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return DESCRIPTION;
    }

    private static void registerRGBProfiles() {
        RGBImageProfileManager profileManager = RGBImageProfileManager.getInstance();
        profileManager.addProfile(new RGBImageProfile("AVHRR/3 L1b - 3a,2,1, Day", // display name
                                                      new String[]{
                                                              "radiance_3a",
                                                              "radiance_2",
                                                              "radiance_1"
                                                      }));
        profileManager.addProfile(new RGBImageProfile("AVHRR/3 L1b - 5,4,3b, Night", // display name
                                                      new String[]{
                                                              "temp_5",
                                                              "temp_4",
                                                              "temp_3b"
                                                      }));
        profileManager.addProfile(new RGBImageProfile("AVHRR/3 L1b - 5,4,3b, Night, Inverse", // display name
                                                      new String[]{
                                                              "-temp_5",
                                                              "-temp_4",
                                                              "-temp_3b"
                                                      }));
        profileManager.addProfile(new RGBImageProfile("AVHRR/3 L1b - Ionian, Day", // display name
                                                      new String[]{
                                                              "radiance_1",
                                                              "radiance_2",
                                                              "-radiance_4"
                                                      }));
    }

}
