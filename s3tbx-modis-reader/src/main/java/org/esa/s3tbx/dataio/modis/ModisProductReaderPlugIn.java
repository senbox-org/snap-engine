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
package org.esa.s3tbx.dataio.modis;

import org.esa.s3tbx.dataio.modis.attribute.DaacAttributes;
import org.esa.s3tbx.dataio.modis.attribute.ImappAttributes;
import org.esa.s3tbx.dataio.modis.netcdf.NetCDFAttributes;
import org.esa.s3tbx.dataio.modis.netcdf.NetCDFVariables;
import org.esa.s3tbx.dataio.modis.productdb.ModisProductDb;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class ModisProductReaderPlugIn implements ProductReaderPlugIn {

    static {
        registerRGBProfiles();
    }

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if it
     * is capable of decoding the input's content.
     */
    public DecodeQualification getDecodeQualification(Object input) {
        final File inputFile = getInputFile(input);

        if (!isValidInputFile(inputFile)) {
            return DecodeQualification.UNABLE;
        }

        NetcdfFile netcdfFile = null;
        try {
            final String inputFilePath = inputFile.getPath();
            netcdfFile = NetcdfFileOpener.open(inputFilePath);
            if (netcdfFile == null) {
                return DecodeQualification.UNABLE;
            }

            final NetCDFVariables variables = new NetCDFVariables();
            variables.add(netcdfFile.getVariables());

            final NetCDFAttributes attributes = new NetCDFAttributes();
            attributes.add(netcdfFile.getGlobalAttributes());

            final ModisGlobalAttributes modisAttributes;
            final Variable structMeta = variables.get(ModisConstants.STRUCT_META_KEY);
            if (structMeta == null) {
                modisAttributes = new ImappAttributes(inputFile, variables, attributes);
            } else {
                modisAttributes = new DaacAttributes(variables);
            }

            final String productType = modisAttributes.getProductType();
            if (ModisProductDb.getInstance().isSupportedProduct(productType)) {
                if (modisAttributes.isImappFormat()) {
                    return DecodeQualification.INTENDED;
                } else {
                    return DecodeQualification.SUITABLE;
                }
            }
        } catch (IOException ignore) {
        } finally {
            if (netcdfFile != null) {
                try {
                    netcdfFile.close();
                } catch (IOException ignore) {
                }
            }
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
        return new Class[]{String.class, File.class};
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    public ProductReader createReaderInstance() {
        return new ModisProductReader(this);
    }

    public SnapFileFilter getProductFileFilter() {
        String[] formatNames = getFormatNames();
        String formatName = "";
        if (formatNames.length > 0) {
            formatName = formatNames[0];
        }
        return new SnapFileFilter(formatName, getDefaultFileExtensions(), getDescription(null));
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
        return new String[]{ModisConstants.DEFAULT_FILE_EXTENSION};
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
        return ModisConstants.READER_DESCRIPTION;
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
    public String[] getFormatNames() {
        return new String[]{ModisConstants.FORMAT_NAME};
    }

    // package access for testing purpose only tb 2012-05-11
    static File getInputFile(Object input) {
        if (input instanceof String) {
            return new File((String) input);
        } else if (input instanceof File) {
            return (File) input;
        }
        return null;
    }

    // package access for testing purpose only tb 2012-05-11
    @SuppressWarnings("SimplifiableIfStatement")
    static boolean hasHdfFileExtension(File inputFile) {
        if (inputFile == null) {
            return false;
        }

        return inputFile.getPath().toLowerCase().endsWith(ModisConstants.DEFAULT_FILE_EXTENSION);
    }

    // package access for testing purpose only tb 2012-05-14
    static boolean isValidInputFile(File inputFile) {
        return inputFile != null &&
               inputFile.isFile() &&
               hasHdfFileExtension(inputFile);
    }

    private static void registerRGBProfiles() {
        RGBImageProfileManager manager = RGBImageProfileManager.getInstance();
        manager.addProfile(new RGBImageProfile("MOD021KM High Quality",
                                               new String[]{
                                                       "log(1 + 0.04 * EV_1KM_RefSB.8 + 0.35 * EV_1KM_RefSB.9 + 0.03 * EV_1KM_RefSB.10 + 0.17 * " +
                                                       "EV_1KM_RefSB.11 + 0.43 * EV_1KM_RefSB.12 + 1.00 * EV_1KM_RefSB.13lo + 0.13 * " +
                                                       "EV_1KM_RefSB.14lo + 0.05 * EV_1KM_RefSB.15)",
                                                       "log(1 + 0.02 * EV_1KM_RefSB.9 + 0.21 * EV_1KM_RefSB.10 + 0.86 * EV_1KM_RefSB.11 + 1.00 * " +
                                                       "EV_1KM_RefSB.12 + 0.38 * EV_1KM_RefSB.13lo + 0.04 * EV_1KM_RefSB.14lo + 0.02 * EV_1KM_RefSB.15)",
                                                       "log(1 + 0.21 * EV_1KM_RefSB.8 + 1.75 * EV_1KM_RefSB.9 + 0.47 * EV_1KM_RefSB.10 + 0.04 * " +
                                                       "EV_1KM_RefSB.11 + 0.01 * EV_1KM_RefSB.12)"
                                               })
        );
        manager.addProfile(new RGBImageProfile("MOD021KM Night",
                                               new String[]{
                                                       "EV_1KM_Emissive.32",
                                                       "EV_1KM_Emissive.31",
                                                       "EV_1KM_Emissive.21"
                                               })
        );
    }

}
