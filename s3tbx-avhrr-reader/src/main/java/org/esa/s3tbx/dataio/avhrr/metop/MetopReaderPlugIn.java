/*
 * AVISA software - $Id: MetopReaderPlugIn.java,v 1.1.1.1 2007/03/22 11:12:51 ralf Exp $
 *
 * Copyright (C) 2005 by EUMETSAT
 *
 * The Licensee acknowledges that the AVISA software is owned by the European
 * Organisation for the Exploitation of Meteorological Satellites
 * (EUMETSAT) and the Licensee shall not transfer, assign, sub-licence,
 * reproduce or copy the AVISA software to any third party or part with
 * possession of this software or any part thereof in any way whatsoever.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 * The AVISA software has been developed using the ESA BEAM software which is
 * distributed under the GNU General Public License (GPL).
 *
 */
package org.esa.s3tbx.dataio.avhrr.metop;


import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;
import java.util.Locale;

/**
 * The plug-in class for the {@link MetopReader
 * METOP-AVHRR/3 Level-1b reader}.
 */
public class MetopReaderPlugIn implements ProductReaderPlugIn {

    public static final String FORMAT_NAME = "METOP_AVHRR_3_L1B";

    private static final String[] FILE_EXTENSIONS = new String[]{""};
    private static final String DESCRIPTION = "METOP-AVHRR/3 Level-1b Data Product";
    private static final Class[] INPUT_TYPES = new Class[]{
            String.class,
            File.class,
    };

    public MetopReaderPlugIn() {
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

    public DecodeQualification getDecodeQualification(Object input) {
        File file = getInputFile(input);
        if (file != null && MetopReader.canOpenFile(file)) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }

    /**
     * Returns an array containing the classes that represent valid input types for this reader.
     * <p>
     * <p> Intances of the classes returned in this array are valid objects for the <code>setInput</code> method of the
     * <code>ProductReader</code> interface (the method will not throw an <code>InvalidArgumentException</code> in this
     * case).
     *
     * @return an array containing valid input types, never <code>null</code>
     */
    public Class[] getInputTypes() {
        return INPUT_TYPES;
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    public ProductReader createReaderInstance() {
        return new MetopReader(this);
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
    public String[] getFormatNames() {
        return new String[]{
                FORMAT_NAME
        };
    }

    /**
     * Gets the default file extensions associated with each of the format names returned by the <code>{@link
     * #getFormatNames}</code> method. <p>The string array returned shall always have the same lenhth as the array
     * returned by the <code>{@link #getFormatNames}</code> method. <p>The extensions returned in the string array shall
     * always include a leading colon ('.') character, e.g. <code>".hdf"</code>
     *
     * @return the default file extensions for this product I/O plug-in, never <code>null</code>
     */
    public String[] getDefaultFileExtensions() {
        return FILE_EXTENSIONS;
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the local for the given decription string, if <code>null</code> the default locale is used
     * @return a textual description of this product reader/writer
     */
    public String getDescription(Locale locale) {
        return DESCRIPTION;
    }

    /**
     * Returns an instance of {@link SnapFileFilter} for use in a {@link javax.swing.JFileChooser}.
     *
     * @return the file filter.
     */
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }
}
