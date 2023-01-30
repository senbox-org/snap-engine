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
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.Group;

import java.io.IOException;
import java.util.Arrays;

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

        if (metadataRoot.getNumElements() > 0) {
            Group rootGroup = getRootGroup(netcdfFileWriteable);
            Group metadataGroup = netcdfFileWriteable.addGroup(rootGroup, MetadataUtils.METADATA_GROUP_NAME);
            // work on a clone as we might modify it to make it compatible with NetCDF conventions
            MetadataElement deepClone = metadataRoot.createDeepClone();
            makeCompatible(deepClone);
            for (MetadataElement element : deepClone.getElements()) {
                addElementToGroup(netcdfFileWriteable, metadataGroup, element);
            }
            deepClone.dispose();
        }
    }

    static void makeCompatible(MetadataElement elem) {
        String[] subElemNames = elem.getElementNames();
        // only if the number of distinct elem names is different to the total number of names we need to take action
        if (Arrays.stream(subElemNames).distinct().count() < subElemNames.length) {
            MetadataElement[] subElems = elem.getElements();
            for (MetadataElement subElem : subElems) {
                MetadataElement[] equalNameElems = Arrays.stream(subElems).filter(s -> s.getName().equals(subElem.getName())).toArray(MetadataElement[]::new);
                if (equalNameElems.length > 1) {
                    for (int k = 0; k < equalNameElems.length; k++) {
                        MetadataElement element = equalNameElems[k];
                        element.setName(element.getName() + "." + k);
                    }
                }
            }
        }

        Arrays.stream(elem.getElements()).forEach(CfMetadataPart::makeCompatible);

    }

    private void addElementToGroup(NFileWriteable netcdfFileWriteable, Group ncGroup, MetadataElement root) {
        final Group newGroup = netcdfFileWriteable.addGroup(ncGroup, root.getName());
        addAttributesToGroup(newGroup, root.getAttributes());
        final MetadataElement[] elements = root.getElements();
        for (MetadataElement element : elements) {
            addElementToGroup(netcdfFileWriteable, newGroup, element);
        }
    }

    private static Group getRootGroup(NFileWriteable netcdfFileWriteable) {
        // this is a workaround to get the root group
        return netcdfFileWriteable.addGroup(null, "");
    }

    private void addAttributesToGroup(Group group, MetadataAttribute[] attributes) {
        for (MetadataAttribute attribute : attributes) {
            addAttribute(group, attribute);
        }
    }

    private static void addAttribute(Group group, MetadataAttribute attribute) {
        final ProductData data = attribute.getData();
        final int type = data.getType();
        if (data.isScalar()) {
            switch (type) {
                case ProductData.TYPE_FLOAT32:
                    group.addAttribute(new Attribute(attribute.getName(), data.getElemFloat()));
                    break;
                case ProductData.TYPE_FLOAT64:
                    group.addAttribute(new Attribute(attribute.getName(), data.getElemDouble()));
                    break;
                case ProductData.TYPE_INT8:
                case ProductData.TYPE_INT16:
                case ProductData.TYPE_INT32:
                    group.addAttribute(new Attribute(attribute.getName(), data.getElemInt()));
                    break;
                case ProductData.TYPE_UINT8:
                case ProductData.TYPE_UINT16:
                    group.addAttribute(new Attribute(attribute.getName(), data.getElemInt(), data.isUnsigned()));
                    break;
                case ProductData.TYPE_UINT32:
                case ProductData.TYPE_INT64:
                    group.addAttribute(new Attribute(attribute.getName(), data.getElemLong(), data.isUnsigned()));
                case ProductData.TYPE_UINT64:
                    group.addAttribute(new Attribute(attribute.getName(), data.getElemDouble()));
                    break;
                case ProductData.TYPE_ASCII:
                    group.addAttribute(new Attribute(attribute.getName(), data.getElemString()));
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + type);
            }
        } else {
            if (ProductData.TYPE_ASCII == type || ProductData.TYPE_UTC == type) {
                group.addAttribute(new Attribute(attribute.getName(), data.getElemString()));
            } else {
                group.addAttribute(new Attribute(attribute.getName(), Array.makeFromJavaArray(data.getElems())));
            }
        }
    }

}
