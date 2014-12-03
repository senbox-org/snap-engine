/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.avhrr.noaa.pod;

import com.bc.ceres.binio.IOHandler;
import com.bc.ceres.binio.util.ImageIOHandler;
import com.bc.ceres.binio.util.RandomAccessFileIOHandler;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Locale;

/**
 * A reader plugin for NOAA Polar Orbiter Data (POD) products (currently AVHRR HRPT only).
 *
 * @author Ralf Quast
 * @see <a href="http://www.ncdc.noaa.gov/oa/pod-guide/ncdc/docs/podug/index.htm">NOAA Polar Orbiter Data User's Guide</a>
 */
public final class PodAvhrrReaderPlugIn implements ProductReaderPlugIn {

    private static final String DESCRIPTION = "NOAA Polar Orbiter Data products (AVHRR HRPT)";
    private static final String[] FILE_EXTENSIONS = new String[]{".l1b"};
    private static final String FORMAT_NAME = "NOAA_POD_AVHRR_HRPT";
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME};
    private static final Class[] INPUT_TYPES = new Class[]{
            //ImageInputStream.class,
            String.class, File.class};

    public PodAvhrrReaderPlugIn() {
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final File file = getInputFile(input);
        if (file != null) {
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                IOHandler ioHandler = new RandomAccessFileIOHandler(raf);
                if (PodAvhrrFile.canDecode(ioHandler)) {
                    return DecodeQualification.INTENDED;
                }
            } catch (IOException e) {
                return DecodeQualification.UNABLE;
            }
        }
//        if (input instanceof ImageInputStream) {
//            ImageInputStream iis = (ImageInputStream) input;
//            IOHandler ioHandler = new ImageIOHandler(iis);
//            if (PodAvhrrFile.canDecode(ioHandler)) {
//                return DecodeQualification.INTENDED;
//            }
//        }
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
        return new PodAvhrrReader(this);
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return DESCRIPTION;
    }
}
