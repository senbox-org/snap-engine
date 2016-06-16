/*
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

package org.esa.s3tbx.fu;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DetectInstrument {
    private static final String SEAWIFS_TITLE = "SeaWiFS Level-2 Data";
    private static final String MODIS_TITLE_VALUE = "HMODISA Level-2 Data";
    private static final String MODIS_ATTRIBUTE_TITLE = "title";
    private static final String GLOBAL_ATTRIBUTES = "Global_Attributes";
    private static final String OLCI_PRODUCT_TYPE = "S3A_OL_2";
    private static final String MERIS_TITLE_VALUE = "Title";
    private static String OA_RADIANCE_BAND_NAME_PATTERN = "Oa%02d_radiance";

    static Instrument getInstrument(Product product) {
        if (meris(product)) {
            return Instrument.MERIS;
        } else if (modis(product)) {
            return Instrument.MODIS;
        } else if (olci(product)) {
            return Instrument.OLCI;
        } else if (seawifs(product)) {
            return Instrument.SEAWIFS;
        }
        return null;
    }

    private static boolean modis(Product product) {
        MetadataElement globalAttributes = product.getMetadataRoot().getElement(GLOBAL_ATTRIBUTES);

        if (globalAttributes != null && globalAttributes.containsAttribute(MODIS_ATTRIBUTE_TITLE)) {
            final String sensor_name = globalAttributes.getAttributeString(MODIS_ATTRIBUTE_TITLE);
            if (sensor_name.contains(MODIS_TITLE_VALUE)) {
                return true;
            }
        }
        return false;
    }

    private static boolean olci(Product product) {
        boolean isOLCI = false;
        List<Band> collect = Stream.of(product.getBands()).filter(p -> p.getName().matches("Oa\\d+_radiance")).collect(Collectors.toList());
        boolean checkByType = product.getProductType().contains(OLCI_PRODUCT_TYPE);
        if (collect.size() > 0 || checkByType) {
            isOLCI = true;
        }
        return isOLCI;
    }

    private static boolean seawifs(Product product) {
        MetadataElement globalAttributes = product.getMetadataRoot().getElement(GLOBAL_ATTRIBUTES);

        if (globalAttributes != null && globalAttributes.containsAttribute(MERIS_TITLE_VALUE)) {
            final String title = globalAttributes.getAttributeString(MERIS_TITLE_VALUE);
            if (title.equals(SEAWIFS_TITLE)) {
                return true;
            }
        }

        return false;
    }

    private static boolean meris(Product product) {
        Pattern compile = Pattern.compile("MER_..._2P");
        return compile.matcher(product.getProductType()).matches();

    }
}
