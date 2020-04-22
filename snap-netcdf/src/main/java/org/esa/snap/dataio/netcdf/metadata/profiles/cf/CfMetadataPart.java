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
package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.util.MetadataUtils;
import org.esa.snap.dataio.netcdf.util.VariableNameHelper;

import java.io.IOException;

public class CfMetadataPart extends ProfilePartIO {

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        MetadataUtils.readNetcdfMetadata(ctx.getNetcdfFile(), p.getMetadataRoot());
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        final NFileWriteable netcdfFileWriteable = ctx.getNetcdfFileWriteable();

        final MetadataElement metadataRoot = p.getMetadataRoot();
        final MetadataAttribute[] globalAttributes = metadataRoot.getAttributes();
        for (final MetadataAttribute attribute : globalAttributes) {
            final int dataType = attribute.getDataType();
            String attributeName = attribute.getName();
            if (!VariableNameHelper.isVariableNameValid(attributeName)) {
                attributeName = VariableNameHelper.convertToValidName(attributeName);
                SystemUtils.LOG.warning("Found invalid attribute name '" + attribute.getName() +
                                                "' - replaced by '" + attributeName + "'.");
            }

            if (dataType == ProductData.TYPE_ASCII) {
                netcdfFileWriteable.addGlobalAttribute(attributeName, attribute.getData().getElemString());
                continue;
            }

            // filter out all number array metadata - we just allow single values tb 2019-07-31
            if (attribute.getNumDataElems() > 1) {
                continue;
            }

            if (dataType == ProductData.TYPE_INT8 ||
                    dataType == ProductData.TYPE_UINT8 ||
                    dataType == ProductData.TYPE_INT16 ||
                    dataType == ProductData.TYPE_UINT16 ||
                    dataType == ProductData.TYPE_INT32 ||
                    dataType == ProductData.TYPE_UINT32) {
                final int data = attribute.getData().getElemInt();
                netcdfFileWriteable.addGlobalAttribute(attributeName, data);
            } else if (dataType == ProductData.TYPE_FLOAT32) {
                final float data = attribute.getData().getElemFloat();
                netcdfFileWriteable.addGlobalAttribute(attributeName, data);
            } else if (dataType == ProductData.TYPE_FLOAT64) {
                final double data = attribute.getData().getElemDouble();
                netcdfFileWriteable.addGlobalAttribute(attributeName, data);
            } else if (dataType == ProductData.TYPE_INT64) {
                final long data = (long) attribute.getData().getElemDouble();
                netcdfFileWriteable.addGlobalAttribute(attributeName, data);
            }

        }
    }
}
