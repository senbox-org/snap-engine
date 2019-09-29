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

package org.esa.snap.dataio.geotiff;

import it.geosolutions.imageio.plugins.tiff.GeoTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageio.plugins.tiff.TIFFTag;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFIFD;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.geotiff.EPSGCodes;
import org.esa.snap.core.util.geotiff.GeoTIFFCodes;
import org.esa.snap.dataio.geotiff.internal.GeoKeyEntry;

import java.util.Map;
import java.util.SortedMap;

/**
 * Intentionally no API doc
 *
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.5
 */
class TiffTagToMetadataConverter {

    private static final int[] PROCESSED_GEO_TIFF_TAGS = new int[]{
                GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE,
                GeoTIFFTagSet.TAG_MODEL_TIE_POINT,
                GeoTIFFTagSet.TAG_MODEL_TRANSFORMATION
        };

    private TiffTagToMetadataConverter() {
    }

    public static void addTiffTagsToMetadata(TIFFImageMetadata imageMetadata, TiffFileInfo tiffInfo,
                                       final MetadataElement metadataElem) {
        final MetadataElement tiffMetadata = new MetadataElement("TIFF Metadata");
        if (tiffInfo.isGeotiff()) {
            final MetadataElement geoTiffMetadata = new MetadataElement("GeoTIFF Metadata");
            addGeoTiffTagsToMetadata(tiffInfo, geoTiffMetadata);
            tiffMetadata.addElement(geoTiffMetadata);
        }
        final TIFFIFD tiffifd = imageMetadata.getRootIFD();
        final TIFFField[] tiffFields = tiffifd.getTIFFFields();
        for (TIFFField tiffField : tiffFields) {
            final int tagNumber = tiffField.getTag().getNumber();
            // ignore BEAM metadata & ignore GeoTIFF tags - are already processed
            if (tagNumber != Utils.PRIVATE_BEAM_TIFF_TAG_NUMBER && !isGeoTiffTag(tagNumber)) {
                final MetadataAttribute attribute = generateMetadataAttribute(tiffField);
                tiffMetadata.addAttribute(attribute);
            }
        }
        metadataElem.addElement(tiffMetadata);
    }

    private static boolean isGeoTiffTag(int tagNumber) {
        return tagNumber == GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY ||
               tagNumber == GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS ||
               tagNumber == GeoTIFFTagSet.TAG_GEO_DOUBLE_PARAMS ||
               tagNumber == GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE ||
               tagNumber == GeoTIFFTagSet.TAG_MODEL_TIE_POINT ||
               tagNumber == GeoTIFFTagSet.TAG_MODEL_TRANSFORMATION;
    }

    private static void addGeoTiffTagsToMetadata(TiffFileInfo tiffInfo, MetadataElement geoTiffMetadata) {
        final TIFFField field = tiffInfo.getField(GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY);
        final MetadataElement geoKeyDirMetadata = new MetadataElement(field.getTag().getName());
        geoTiffMetadata.addElement(geoKeyDirMetadata);
        final SortedMap<Integer, GeoKeyEntry> geoKeyMap = tiffInfo.getGeoKeyEntries();
        for (Map.Entry<Integer, GeoKeyEntry> entry : geoKeyMap.entrySet()) {
            final GeoKeyEntry geoKeyEntry = entry.getValue();
            final String name = geoKeyEntry.getName();
            if (name != null) {
                final ProductData data = getGeoKeyValue(geoKeyEntry);
                if (data != null) {
                    final MetadataAttribute attribute = new MetadataAttribute(name, data, true);
                    geoKeyDirMetadata.addAttribute(attribute);
                }
            }
        }

        for (int tagNunmber : PROCESSED_GEO_TIFF_TAGS) {
            final TIFFField tiffField = tiffInfo.getField(tagNunmber);
            if (tiffField != null) {
                final MetadataAttribute attribute = generateMetadataAttribute(tiffField);
                geoTiffMetadata.addAttribute(attribute);
            }
        }

    }

    private static MetadataAttribute generateMetadataAttribute(TIFFField tiffField) {
        final TIFFTag geoTiffTag = tiffField.getTag();
        final String name = geoTiffTag.getName();
        final int dataCount = tiffField.getCount();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dataCount; i++) {
            if (geoTiffTag.hasValueNames()) {
                sb.append(geoTiffTag.getValueName(tiffField.getAsInt(i)));
            } else {
                sb.append(tiffField.getValueAsString(i));
            }
            if (i + 1 < dataCount) {
                sb.append(", ");
            }
        }
        final ProductData value = ProductData.createInstance(sb.toString());
        return new MetadataAttribute(name, value, true);
    }

    private static ProductData getGeoKeyValue(GeoKeyEntry geoKeyEntry) {
        ProductData value;
        if (geoKeyEntry.getDblValue() != null) {
            value = ProductData.createInstance(geoKeyEntry.getDblValue());
        } else if (geoKeyEntry.getStrValue() != null) {
            value = ProductData.createInstance(geoKeyEntry.getStrValue());
        } else {
            if (geoKeyEntry.getKeyId() == GeoTIFFCodes.GTModelTypeGeoKey) {
                value = getModelTypeValueName(geoKeyEntry.getIntValue());
            } else if (geoKeyEntry.getKeyId() == GeoTIFFCodes.GTRasterTypeGeoKey) {
                value = getRasterTypeValueName(geoKeyEntry.getIntValue());
            } else {
                value = getEPSGValueName(geoKeyEntry);
            }
        }
        return value;
    }

    private static ProductData getEPSGValueName(GeoKeyEntry geoKeyEntry) {
        ProductData value;
        String epsgCodeName = EPSGCodes.getInstance().getName(geoKeyEntry.getIntValue());
        if (epsgCodeName == null) {
            value = ProductData.createInstance(new int[]{geoKeyEntry.getIntValue()});
        } else {
            value = ProductData.createInstance(epsgCodeName);
        }
        return value;
    }

    private static ProductData getRasterTypeValueName(Integer intValue) {
        ProductData value;
        if (intValue == GeoTIFFCodes.RasterPixelIsArea) {
            value = ProductData.createInstance("RasterPixelIsArea");
        } else if (intValue == GeoTIFFCodes.RasterPixelIsPoint) {
            value = ProductData.createInstance("RasterPixelIsPoint");
        } else {
            value = ProductData.createInstance("unknown");
        }
        return value;
    }

    private static ProductData getModelTypeValueName(Integer intValue) {
        ProductData value;
        if (intValue == GeoTIFFCodes.ModelTypeProjected) {
            value = ProductData.createInstance("ModelTypeProjected");
        } else if (intValue == GeoTIFFCodes.ModelTypeGeographic) {
            value = ProductData.createInstance("ModelTypeGeographic");
        } else if (intValue == GeoTIFFCodes.ModelTypeGeocentric) {
            value = ProductData.createInstance("ModelTypeGeocentric");
        } else {
            value = ProductData.createInstance("unknown");
        }
        return value;
    }
}
