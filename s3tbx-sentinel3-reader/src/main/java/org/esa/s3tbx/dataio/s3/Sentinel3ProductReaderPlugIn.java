package org.esa.s3tbx.dataio.s3;/*
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

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;
import java.util.Locale;
import java.util.regex.Pattern;

public class Sentinel3ProductReaderPlugIn implements ProductReaderPlugIn {

    private static final Class[] SUPPORTED_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String FORMAT_NAME = "Sen3";

    private final String formatName;
    private final String manifestFileBasename;
    private final String alternativeManifestFileBasename;
    private final String[] fileExtensions;
    private final Pattern directoryNamePattern;
    private final String description;
    private final String[] formatNames;

    static {
        registerRGBProfiles();
    }

    public Sentinel3ProductReaderPlugIn() {
        this(FORMAT_NAME, "Sentinel-3 products",
             "S3.?_(OL_1_E[FR]R|OL_2_(L[FR]R|W[FR]R)|SL_1_RBT|SL_2_(LST|WCT|WST)|SY_1_SYN|SY_2_(VGP|SYN)|SY_[23]_VG1)_.*(.SEN3)?",
             "xfdumanifest", "L1c_Manifest", ".xml");
    }

    protected Sentinel3ProductReaderPlugIn(String formatName,
                                           String description,
                                           String directoryNamePattern,
                                           String manifestFileBasename,
                                           String alternativeManifestFileBasename,
                                           String... fileExtensions) {
        this.formatName = formatName;
        this.fileExtensions = fileExtensions;
        this.directoryNamePattern = Pattern.compile(directoryNamePattern);
        this.description = description;
        this.formatNames = new String[]{formatName};
        this.manifestFileBasename = manifestFileBasename;
        this.alternativeManifestFileBasename = alternativeManifestFileBasename;
    }

    @Override
    public final DecodeQualification getDecodeQualification(Object input) {
        if (isInputValid(input)) {
            return DecodeQualification.INTENDED;
        } else {
            return DecodeQualification.UNABLE;
        }
    }

    @Override
    public final Class[] getInputTypes() {
        return SUPPORTED_INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new Sentinel3ProductReader(this);
    }

    @Override
    public final String[] getFormatNames() {
        return formatNames;
    }

    @Override
    public final String[] getDefaultFileExtensions() {
        return fileExtensions;
    }

    @Override
    public final String getDescription(Locale locale) {
        return description;
    }

    @Override
    public final SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(formatName, fileExtensions, description);
    }

    private boolean isValidInputFileName(String name) {
        for (final String fileExtension : fileExtensions) {
            final String manifestFileName = manifestFileBasename + fileExtension;
            final String alternativeManifestFileName = alternativeManifestFileBasename + fileExtension;
            if (manifestFileName.equalsIgnoreCase(name) || alternativeManifestFileName.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInputValid(Object input) {
        final File inputFile = new File(input.toString());
        final File parentFile = inputFile.getParentFile();
        return parentFile != null &&
                (isValidDirectoryName(parentFile.getName()) && isValidInputFileName(inputFile.getName())) ||
               (isValidDirectoryName(inputFile.getName()) && new File(inputFile, XfduManifest.MANIFEST_FILE_NAME).exists()) ||
               (isValidDirectoryName(inputFile.getName()) && new File(inputFile, EarthExplorerManifest.L1C_MANIFEST_FILE_NAME).exists())
        ;
    }

    private boolean isValidDirectoryName(String name) {
        return directoryNamePattern.matcher(name).matches();
    }

    private static void registerRGBProfiles() {
        RGBImageProfileManager manager = RGBImageProfileManager.getInstance();
        manager.addProfile(new RGBImageProfile("OLCI L1 - Tristimulus",
                                               new String[]{
                                                       "log(1.0 + 0.01 * Oa01_radiance + 0.09 * Oa02_radiance + 0.35 * Oa03_radiance + 0.04 * Oa04_radiance + " +
                                                       "0.01 * Oa05_radiance + 0.59 * Oa06_radiance + 0.85 * Oa07_radiance + 0.12 * Oa08_radiance + " +
                                                       "0.07 * Oa09_radiance + 0.04 * Oa10_radiance)",
                                                       "log(1.0 + 0.26 * Oa03_radiance + 0.21 * Oa04_radiance + 0.50 * Oa05_radiance + Oa06_radiance + " +
                                                       "0.38 * Oa07_radiance + 0.04 * Oa08_radiance + 0.03 * Oa09_radiance + 0.02 * Oa10_radiance)",
                                                       "log(1.0 + 0.07 * Oa01_radiance + 0.28 * Oa02_radiance + 1.77 * Oa03_radiance + 0.47 * Oa04_radiance + " +
                                                       "0.16 * Oa05_radiance)"
                                               },
                                               new String[]{
                                                       "S3*_OL_1*",
                                                       "S3*_OL_1*",
                                                       "",
                                               }
        ));
        manager.addProfile(new RGBImageProfile("OLCI L2 W - Tristimulus",
                                               new String[]{
                                                       "log(0.05 + 0.01 * Oa01_reflectance + 0.09 * Oa02_reflectance + 0.35 * Oa03_reflectance + " +
                                                       "0.04 * Oa04_reflectance + 0.01 * Oa05_reflectance + 0.59 * Oa06_reflectance + " +
                                                       "0.85 * Oa07_reflectance + 0.12 * Oa08_reflectance + 0.07 * Oa09_reflectance + " +
                                                       "0.04 * Oa10_reflectance)",
                                                       "log(0.05 + 0.26 * Oa03_reflectance + 0.21 * Oa04_reflectance + 0.50 * Oa05_reflectance + " +
                                                       "Oa06_reflectance + 0.38 * Oa07_reflectance + 0.04 * Oa08_reflectance + " +
                                                       "0.03 * Oa09_reflectance + 0.02 * Oa10_reflectance)",
                                                       "log(0.05 + 0.07 * Oa01_reflectance + 0.28 * Oa02_reflectance + 1.77 * Oa03_reflectance + " +
                                                       "0.47 * Oa04_reflectance + 0.16 * Oa05_reflectance)"
                                               },
                                               new String[]{
                                                       "S3*OL_2_W*",
                                                       "S3*OL_2_W*",
                                                       "",
                                               }
        ));
        manager.addProfile(new RGBImageProfile("OLCI L1 - 17,6,3",
                                               new String[]{
                                                       "Oa17_radiance",
                                                       "Oa06_radiance",
                                                       "Oa03_radiance"
                                               }
        ));
        manager.addProfile(new RGBImageProfile("OLCI L1 - 17,5,2",
                                               new String[]{
                                                       "Oa17_radiance",
                                                       "Oa05_radiance",
                                                       "Oa02_radiance"
                                               }
        ));
        manager.addProfile(new RGBImageProfile("OLCI L2W - 17,6,3",
                                               new String[]{
                                                       "Oa17_reflectance",
                                                       "Oa06_reflectance",
                                                       "Oa03_reflectance"
                                               }
        ));
        manager.addProfile(new RGBImageProfile("OLCI L2W - 17,5,2",
                                               new String[]{
                                                       "Oa17_reflectance",
                                                       "Oa05_reflectance",
                                                       "Oa02_reflectance"
                                               }
        ));
    }
}
