/*
 * Copyright (C) 2021 SkyWatch. https://www.skywatch.com
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
package org.esa.snap.engine_utilities.gpf;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;

import java.util.*;

/**
 * Helper methods for working with Stack products
 */
public final class StackUtils {

    public static final String MST = "_mst";
    public static final String SLV = "_slv";

    public static boolean isCoregisteredStack(final Product product) {
        if(!AbstractMetadata.hasAbstractedMetadata(product))
            return false;

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        return absRoot != null && absRoot.getAttributeInt(AbstractMetadata.coregistered_stack, 0) == 1;
    }

    public static boolean isBiStaticStack(final Product product) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        return absRoot != null && absRoot.getAttributeInt(AbstractMetadata.bistatic_stack, 0) == 1;
    }

    public static String createBandTimeStamp(final Product product) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        if (absRoot != null) {
            String dateString = OperatorUtils.getAcquisitionDate(absRoot);
            if (!dateString.isEmpty())
                dateString = '_' + dateString;
            return StringUtils.createValidName(dateString, new char[]{'_', '.'}, '_');
        }
        return "";
    }

    public static void saveMasterProductBandNames(final Product targetProduct, final String[] referenceProductBands) {
        final MetadataElement targetSecondaryMetadataRoot = AbstractMetadata.getSlaveMetadata(targetProduct.getMetadataRoot());
        final StringBuilder value = new StringBuilder(255);
        for (String name : referenceProductBands) {
            value.append(name);
            value.append(' ');
        }
        final String referenceBandNames = value.toString().trim();
        if (!referenceBandNames.isEmpty()) {
            targetSecondaryMetadataRoot.setAttributeString(AbstractMetadata.MASTER_BANDS, referenceBandNames);
        }
    }

    public static void saveSlaveProductNames(final Product[] sourceProducts, final Product targetProduct,
                                             final Product referenceProduct, final Map<Band, Band> sourceRasterMap) throws Exception {

        for (Product prod : sourceProducts) {
            if (prod != referenceProduct) {
                final String suffix = StackUtils.createBandTimeStamp(prod);
                final List<String> bandNames = new ArrayList<>(10);
                for (Band tgtBand : sourceRasterMap.keySet()) {
                    // It is assumed that tgtBand is a band in targetProduct
                    final Band srcBand = sourceRasterMap.get(tgtBand);
                    final Product srcProduct = srcBand.getProduct();
                    if (srcProduct == prod) {
                        bandNames.add(tgtBand.getName());
                        //System.out.println("StackUtils: " + prod.getName() + ": " + tgtBand.getName());
                    }
                }

                // CompactPolStokesParametersOp.initialize() calls PolBandUtils.getSourceBands() which calls
                // StackUtils.getSlaveBandNames() which gets the secondary bands from the meta data of the stack product.
                // The bands are passed (in the order they appear in the metadata) to
                // DualPolOpUtils.getMeanCovarianceMatrixC2().
                // CreateStackOp.initialize() calls this method to get the secondary bands to put in Slave_bands of
                // Slave_Metadata. So make sure the secondary band names are in the same order as how the bands appear in
                // the stack product.
                // In particular, compact pol C2 stack product secondary bands must be in the order
                // C11, C12_real, C12_imag, C22 because CompactPolStokesParametersOp() expects them to be in that
                // order.
                String[] secBandNames = new String[bandNames.size()];
                Band[] tgtBands = targetProduct.getBands();
                int cnt = 0;
                for (Band tgtBand : tgtBands) {
                    //System.out.println("StackUtils: tgt band i = " + i + " " + tgtBands[i].getName());
                    if (bandNames.contains(tgtBand.getName())) {
                        secBandNames[cnt++] = tgtBand.getName();
                    }

                    if (cnt >= secBandNames.length) {
                        break;
                    }
                }
                /*
                for (int i = 0; i < secBandNames.length; i++) {
                    System.out.println("StackUtils: " + prod.getName() + ": secondary band = " + secBandNames[i]);
                }
                */

                final String prodName = prod.getName() + suffix;
                //StackUtils.saveSlaveProductBandNames(targetProduct, prodName, bandNames.toArray(new String[bandNames.size()]));
                StackUtils.saveSlaveProductBandNames(targetProduct, prodName, secBandNames);
            }
        }
    }

    public static void saveSlaveProductBandNames(final Product targetProduct, final String secProductName,
                                                 final String[] bandNames) throws Exception {
        Guardian.assertNotNull("targetProduct", targetProduct);
        Guardian.assertNotNull("secProductName", secProductName);
        Guardian.assertNotNullOrEmpty("bandNames", bandNames);

        final MetadataElement targetSecondaryMetadataRoot = AbstractMetadata.getSlaveMetadata(targetProduct.getMetadataRoot());
        final MetadataElement elem = targetSecondaryMetadataRoot.getElement(secProductName);
        if(elem == null) {
            throw new Exception(secProductName + " metadata not found");
        }
        StringBuilder value = new StringBuilder(255);
        for (String name : bandNames) {
            value.append(name);
            value.append(' ');
        }
        elem.setAttributeString(AbstractMetadata.SLAVE_BANDS, value.toString().trim());
    }

    public static String findOriginalSlaveProductName(final Product sourceProduct, final Band secBand) {
        final MetadataElement secondaryMetadataRoot = sourceProduct.getMetadataRoot().getElement(
                AbstractMetadata.SLAVE_METADATA_ROOT);
        if (secondaryMetadataRoot != null) {
            final String secBandName = secBand.getName();
            for (MetadataElement elem : secondaryMetadataRoot.getElements()) {
                final String secBandNames = elem.getAttributeString(AbstractMetadata.SLAVE_BANDS, "");
                if (secBandNames.contains(secBandName)) {
                    return elem.getName();
                }
            }
        }
        return null;
    }

    /**
     * Returns only i and q reference band names
     * @param sourceProduct coregistered product
     * @return reference band names
     */
    public static String[] getMasterBandNames(final Product sourceProduct) {
        final MetadataElement secondaryMetadataRoot = sourceProduct.getMetadataRoot().getElement(
                AbstractMetadata.SLAVE_METADATA_ROOT);
        if (secondaryMetadataRoot != null) {
            final String refBandNames = secondaryMetadataRoot.getAttributeString(AbstractMetadata.MASTER_BANDS, "");
            if(!refBandNames.isEmpty()) {
                return StringUtils.stringToArray(refBandNames, " ");
            }
        }
        final List<String> bandNames = new ArrayList<>();
        for(String bandName : sourceProduct.getBandNames()) {
            if(bandName.toLowerCase().contains(MST)) {
                bandNames.add(bandName);
            }
        }
        return bandNames.toArray(new String[0]);
    }

    public static MetadataElement getSlaveMetadata(final Product sourceProduct, final String secProductName) {
        final MetadataElement secondaryMetadataRoot = sourceProduct.getMetadataRoot().getElement(
                AbstractMetadata.SLAVE_METADATA_ROOT);
        if (secondaryMetadataRoot != null) {
            final MetadataElement elem = secondaryMetadataRoot.getElement(secProductName);
            return elem.createDeepClone();
        }
        return null;
    }

    public static String[] getSlaveBandNames(final Product sourceProduct, final String secProductName) {
        final MetadataElement secondaryMetadataRoot = sourceProduct.getMetadataRoot().getElement(
                AbstractMetadata.SLAVE_METADATA_ROOT);
        if (secondaryMetadataRoot != null) {
            final MetadataElement elem = secondaryMetadataRoot.getElement(secProductName);
            if(elem != null) {
                final String secBandNames = elem.getAttributeString(AbstractMetadata.SLAVE_BANDS, "");
                if (!secBandNames.isEmpty()) {
                    return StringUtils.stringToArray(secBandNames, " ");
                }
            }
        }
        String dateSuffix = secProductName.contains("_") ?
                secProductName.substring(secProductName.lastIndexOf('_')).toLowerCase() : "";
        final List<String> bandNames = new ArrayList<>();
        for(String bandName : sourceProduct.getBandNames()) {
            final String name = bandName.toLowerCase();
            if(name.contains(SLV) && name.endsWith(dateSuffix)) {
                bandNames.add(bandName);
            }
        }
        return bandNames.toArray(new String[0]);
    }

    public static boolean isMasterBand(final String bandName, final Product sourceProduct) {

        final MetadataElement secondaryMetadataRoot = sourceProduct.getMetadataRoot().getElement(
                AbstractMetadata.SLAVE_METADATA_ROOT);

        if (secondaryMetadataRoot != null) {
            final String refBandNames = secondaryMetadataRoot.getAttributeString(AbstractMetadata.MASTER_BANDS, "");
            return refBandNames.contains(bandName);
        }

        for(String srcBandName : sourceProduct.getBandNames()) {
            if(srcBandName.toLowerCase().contains(MST) && srcBandName.contains(bandName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSlaveBand(final String bandName, final Product sourceProduct) {

        final String[] secProductNames = StackUtils.getSlaveProductNames(sourceProduct);
        for (String secProductName:secProductNames) {
            if (isSlaveBand(bandName, sourceProduct, secProductName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSlaveBand(final String bandName, final Product sourceProduct, final String secProductName) {

        final MetadataElement secondaryMetadataRoot = sourceProduct.getMetadataRoot().getElement(
                AbstractMetadata.SLAVE_METADATA_ROOT);

        if (secondaryMetadataRoot != null) {
            final MetadataElement elem = secondaryMetadataRoot.getElement(secProductName);
            final String secBandNames = elem.getAttributeString(AbstractMetadata.SLAVE_BANDS, "");
            if(!secBandNames.isEmpty()) {
                return secBandNames.contains(bandName);
            }
        }

        String dateSuffix = secProductName.substring(secProductName.lastIndexOf('_')).toLowerCase();
        for(String srcBandName : sourceProduct.getBandNames()) {
            final String name = srcBandName.toLowerCase();
            if(name.contains(SLV) && name.endsWith(dateSuffix) && srcBandName.contains(bandName)) {
                return true;
            }
        }
        return false;
    }

    public static String[] getSlaveProductNames(final Product sourceProduct) {
        final MetadataElement secondaryMetadataRoot = sourceProduct.getMetadataRoot().getElement(
                AbstractMetadata.SLAVE_METADATA_ROOT);
        if (secondaryMetadataRoot != null) {
            return secondaryMetadataRoot.getElementNames();
        }
        return new String[]{};
    }

    public static String getBandNameWithoutDate(final String bandName) {
        if (bandName.contains(MST)) {
            return bandName.substring(0, bandName.lastIndexOf(MST));
        } else if (bandName.contains(SLV)) {
            return bandName.substring(0, bandName.lastIndexOf(SLV));
        } else if (bandName.contains("_")) {
            return bandName.substring(0, bandName.lastIndexOf('_'));
        }
        return bandName;
    }

    public static String[] getBandSuffixes(final Band[] bands) {
        final Set<String> suffixSet = new TreeSet<>();
        for(Band b : bands) {
            suffixSet.add(getBandSuffix(b.getName()));
        }
        return suffixSet.toArray(new String[0]);
    }

    public static String[] getBandDates(final Band[] bands) {
        final Set<String> suffixSet = new TreeSet<>();
        for(Band b : bands) {
            if (b.getName().contains("_")) {
                suffixSet.add(b.getName().substring(b.getName().lastIndexOf("_")));
            }
        }
        return suffixSet.toArray(new String[0]);
    }

    public static String getBandSuffix(final String bandName) {
        final String suffix;
        if (bandName.contains(MST)) {
            suffix = bandName.substring(bandName.lastIndexOf(MST));
        } else if (bandName.contains(SLV)) {
            suffix = bandName.substring(bandName.lastIndexOf(SLV));
        } else if (bandName.contains("_")) {
            suffix = bandName.substring(bandName.lastIndexOf('_'));
        } else {
            suffix = bandName;
        }
        return suffix;
    }

    public static String getSlaveProductName(final Product sourceProduct, final Band secBand, final String refPol) {
        final MetadataElement secondaryMetadataRoot = sourceProduct.getMetadataRoot().getElement(
                AbstractMetadata.SLAVE_METADATA_ROOT);
        if (secondaryMetadataRoot != null) {
            final String secBandName = secBand.getName();
            for (MetadataElement elem : secondaryMetadataRoot.getElements()) {
                final String secBandNames = elem.getAttributeString(AbstractMetadata.SLAVE_BANDS, "");
                if (refPol == null && secBandNames.contains(secBandName)) {
                    return elem.getName();
                } else if (refPol != null) {
                    // find secondary with same pol
                    final String[] bandNames = StringUtils.toStringArray(secBandNames, " ");
                    boolean polExist = false;
                    for (String secName : bandNames) {
                        final String secPol = OperatorUtils.getPolarizationFromBandName(secName);
                        if (secPol != null && secPol.equalsIgnoreCase(refPol)) {
                            polExist = true;
                            if (secName.equals(secBandName))
                                return elem.getName();
                        }
                    }
                    if (!polExist && secBandNames.contains(secBandName)) {
                        return elem.getName();
                    }
                }
            }
        }
        return null;
    }

    public static ProductData.UTC[] getProductTimes(final Product sourceProduct) {
        final List<ProductData.UTC> utcList = new ArrayList<>();
        // add reference time
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        if (absRoot != null) {
            utcList.add(absRoot.getAttributeUTC(AbstractMetadata.first_line_time));

            // add secondary times
            final MetadataElement secondaryMetadataRoot = sourceProduct.getMetadataRoot().getElement(
                    AbstractMetadata.SLAVE_METADATA_ROOT);
            if (secondaryMetadataRoot != null) {
                for (MetadataElement elem : secondaryMetadataRoot.getElements()) {
                    utcList.add(elem.getAttributeUTC(AbstractMetadata.first_line_time));
                }
            }
        }
        return utcList.toArray(new ProductData.UTC[0]);
    }

    public static String[] bandsToStringArray(final Band[] bands) {
        final String[] names = new String[bands.length];
        int i = 0;
        for (Band band : bands) {
            names[i++] = band.getName();
        }
        return names;
    }
}