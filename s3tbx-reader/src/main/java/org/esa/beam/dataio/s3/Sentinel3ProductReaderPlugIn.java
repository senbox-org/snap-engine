package org.esa.beam.dataio.s3;/*
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

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;
import java.util.regex.Pattern;

public class Sentinel3ProductReaderPlugIn implements ProductReaderPlugIn {

    private static final Class[] SUPPORTED_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String FORMAT_NAME = "SENTINEL-3";

    private final String formatName;
    private final String manifestFileBasename;
    private final String[] fileExtensions;
    private final Pattern directoryNamePattern;
    private final String description;
    private final String[] formatNames;

    public Sentinel3ProductReaderPlugIn() {
//        this(FORMAT_NAME, "Sentinel-3 products", "S3.?_(OL_1_E[FR]R|OL_2_(L[FR]R|W[FR]R)|SL_1_SLT|SL_2_(LST|WCT|WST)|SY_2_(VGP|SYN)|SY_[23]_VGS)_.*(.SAFE)?", "manifest", ".safe", ".xml");
        this(FORMAT_NAME, "Sentinel-3 products", "S3.?_(OL_1_E[FR]R|OL_2_(L[FR]R|W[FR]R)|SL_1_RBT|SL_2_(LST|WCT|WST)|SY_2_(VGP|SYN)|SY_[23]_VG1)_.*(.SEN3)?", "xfdumanifest", ".xml");
        // special version for ACRI - rq-20140214
        // this(FORMAT_NAME, "Sentinel-3 products", "S3.?_(OL_1_E[FR]R|OL_2_(L[FR]R|W[FR]R))_.*(.SAFE)?", "manifest", ".safe", ".xml");
    }

    private Sentinel3ProductReaderPlugIn(String formatName,
                                         String description,
                                         String directoryNamePattern,
                                         String manifestFileBasename,
                                         String... fileExtensions) {
        this.formatName = formatName;
        this.fileExtensions = fileExtensions;
        this.directoryNamePattern = Pattern.compile(directoryNamePattern);
        this.description = description;
        this.formatNames = new String[]{formatName};
        this.manifestFileBasename = manifestFileBasename;
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
    public final ProductReader createReaderInstance() {
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
    public final BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(formatName, fileExtensions, description);
    }

    private boolean isValidInputFileName(String name) {
        for (final String fileExtension : fileExtensions) {
            final String manifestFileName = manifestFileBasename + fileExtension;
            if (manifestFileName.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInputValid(Object input) {
        final File inputFile = new File(input.toString());
        final File parentFile = inputFile.getParentFile();
        return parentFile != null && isValidInputFileName(inputFile.getName()) && isValidDirectoryName(
                parentFile.getName());
    }

    private boolean isValidDirectoryName(String name) {
        return directoryNamePattern.matcher(name).matches();
    }
}
