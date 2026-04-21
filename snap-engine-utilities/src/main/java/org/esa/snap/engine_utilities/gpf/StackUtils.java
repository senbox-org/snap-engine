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

    // Current naming convention
    public static final String REF = "_ref";
    public static final String SEC = "_sec";

    // Legacy naming convention (kept for backwards compatibility)
    @Deprecated
    public static final String MST = "_mst";
    @Deprecated
    public static final String SLV = "_slv";

    /**
     * Find the secondary metadata root element, checking new name first then legacy.
     * Returns null if neither exists (does not create one).
     */
    public static MetadataElement findSecondaryMetadataRoot(final Product product) {
        return findSecondaryMetadataRoot(product.getMetadataRoot());
    }

    public static MetadataElement findSecondaryMetadataRoot(final MetadataElement root) {
        MetadataElement elem = root.getElement(AbstractMetadata.SECONDARY_METADATA_ROOT);
        if (elem == null) {
            elem = root.getElement(AbstractMetadata.LEGACY_SLAVE_METADATA_ROOT);
        }
        return elem;
    }

    /**
     * Read a metadata attribute by new name, falling back to legacy name if not found.
     */
    private static String getMetadataAttribute(final MetadataElement elem, final String newName, final String legacyName) {
        String value = elem.getAttributeString(newName, "");
        if (value.isEmpty()) {
            value = elem.getAttributeString(legacyName, "");
        }
        return value;
    }

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

    /**
     * Returns true if the band name contains a legacy naming suffix (_mst or _slv).
     */
    public static boolean hasLegacyNaming(final String bandName) {
        final String lower = bandName.toLowerCase();
        return lower.contains(MST) || lower.contains(SLV);
    }

    /**
     * Returns true if the band name contains either the current or legacy reference/secondary naming.
     */
    private static boolean containsRef(final String bandName) {
        final String lower = bandName.toLowerCase();
        return lower.contains(REF) || lower.contains(MST);
    }

    private static boolean containsSec(final String bandName) {
        final String lower = bandName.toLowerCase();
        return lower.contains(SEC) || lower.contains(SLV);
    }

    /**
     * Rename legacy band names in a product from _mst/_slv to _ref/_sec.
     * This should be called by operators that want to ensure products use the current naming convention.
     * @param product the product whose bands to rename
     */
    public static void renameLegacyBands(final Product product) {
        for (Band band : product.getBands()) {
            final String name = band.getName();
            if (hasLegacyNaming(name)) {
                final String newName = convertLegacyBandName(name);
                if (!newName.equals(name)) {
                    band.setName(newName);
                }
            }
        }
    }

    /**
     * Convert a legacy band name from _mst/_slv convention to _ref/_sec.
     * If the name does not use legacy naming, it is returned unchanged.
     */
    public static String convertLegacyBandName(final String bandName) {
        // Replace _mst with _ref and _slv with _sec (case-sensitive, matching the constants)
        return bandName.replace(MST, REF).replace(SLV, SEC);
    }

    public static void saveReferenceProductBandNames(final Product targetProduct, final String[] referenceProductBands) {
        final MetadataElement targetSecondaryMetadataRoot = AbstractMetadata.getSecondaryMetadata(targetProduct.getMetadataRoot());
        final StringBuilder value = new StringBuilder(255);
        for (String name : referenceProductBands) {
            value.append(name);
            value.append(' ');
        }
        final String referenceBandNames = value.toString().trim();
        if (!referenceBandNames.isEmpty()) {
            targetSecondaryMetadataRoot.setAttributeString(AbstractMetadata.REFERENCE_BANDS, referenceBandNames);
        }
    }

    @Deprecated
    public static void saveMasterProductBandNames(final Product targetProduct, final String[] masterProductBands) {
        saveReferenceProductBandNames(targetProduct, masterProductBands);
    }

    public static void saveSecondaryProductNames(final Product[] sourceProducts, final Product targetProduct,
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
                    }
                }

                // CompactPolStokesParametersOp.initialize() calls PolBandUtils.getSourceBands() which calls
                // StackUtils.getSecondaryBandNames() which gets the secondary bands from the meta data of the stack product.
                // The bands are passed (in the order they appear in the metadata) to
                // DualPolOpUtils.getMeanCovarianceMatrixC2().
                // CreateStackOp.initialize() calls this method to get the secondary bands to put in Secondary_bands of
                // Secondary_Metadata. So make sure the secondary band names are in the same order as how the bands appear in
                // the stack product.
                // In particular, compact pol C2 stack product secondary bands must be in the order
                // C11, C12_real, C12_imag, C22 because CompactPolStokesParametersOp() expects them to be in that
                // order.
                String[] secBandNames = new String[bandNames.size()];
                Band[] tgtBands = targetProduct.getBands();
                int cnt = 0;
                for (Band tgtBand : tgtBands) {
                    if (bandNames.contains(tgtBand.getName())) {
                        secBandNames[cnt++] = tgtBand.getName();
                    }

                    if (cnt >= secBandNames.length) {
                        break;
                    }
                }

                final String prodName = prod.getName() + suffix;
                StackUtils.saveSecondaryProductBandNames(targetProduct, prodName, secBandNames);
            }
        }
    }

    @Deprecated
    public static void saveSlaveProductNames(final Product[] sourceProducts, final Product targetProduct,
                                             final Product masterProduct, final Map<Band, Band> sourceRasterMap) throws Exception {
        saveSecondaryProductNames(sourceProducts, targetProduct, masterProduct, sourceRasterMap);
    }

    public static void saveSecondaryProductBandNames(final Product targetProduct, final String secProductName,
                                                 final String[] bandNames) throws Exception {
        Guardian.assertNotNull("targetProduct", targetProduct);
        Guardian.assertNotNull("secProductName", secProductName);
        Guardian.assertNotNullOrEmpty("bandNames", bandNames);

        final MetadataElement targetSecondaryMetadataRoot = AbstractMetadata.getSecondaryMetadata(targetProduct.getMetadataRoot());
        final MetadataElement elem = targetSecondaryMetadataRoot.getElement(secProductName);
        if(elem == null) {
            throw new Exception(secProductName + " metadata not found");
        }
        StringBuilder value = new StringBuilder(255);
        for (String name : bandNames) {
            value.append(name);
            value.append(' ');
        }

        String secondaryBands = value.toString().trim();
        final boolean hasNewAttr = elem.containsAttribute(AbstractMetadata.SECONDARY_BANDS);
        final boolean hasLegacyAttr = elem.containsAttribute(AbstractMetadata.LEGACY_SLAVE_BANDS);
        if (hasNewAttr || hasLegacyAttr) {
            final String savedSecondaryBands = hasNewAttr
                    ? elem.getAttributeString(AbstractMetadata.SECONDARY_BANDS)
                    : elem.getAttributeString(AbstractMetadata.LEGACY_SLAVE_BANDS);
            final String[] savedSecondaryBandArray = savedSecondaryBands.split(" ");
            boolean allValid = true;
            for (String secBandName : savedSecondaryBandArray) {
                if (targetProduct.getBand(secBandName) == null || secondaryBands.contains(secBandName)) {
                    allValid = false;
                    break;
                }
            }

            if (allValid) {
                secondaryBands = savedSecondaryBands + " " + secondaryBands;
            }
        }
        elem.setAttributeString(AbstractMetadata.SECONDARY_BANDS, secondaryBands);
    }

    @Deprecated
    public static void saveSlaveProductBandNames(final Product targetProduct, final String slvProductName,
                                                 final String[] bandNames) throws Exception {
        saveSecondaryProductBandNames(targetProduct, slvProductName, bandNames);
    }

    public static String findOriginalSecondaryProductName(final Product sourceProduct, final Band secBand) {
        final MetadataElement secondaryMetadataRoot = findSecondaryMetadataRoot(sourceProduct);
        if (secondaryMetadataRoot != null) {
            final String secBandName = secBand.getName();
            for (MetadataElement elem : secondaryMetadataRoot.getElements()) {
                final String secBandNames = getMetadataAttribute(elem,
                        AbstractMetadata.SECONDARY_BANDS, AbstractMetadata.LEGACY_SLAVE_BANDS);
                if (secBandNames.contains(secBandName)) {
                    return elem.getName();
                }
            }
        }
        return null;
    }

    @Deprecated
    public static String findOriginalSlaveProductName(final Product sourceProduct, final Band slvBand) {
        return findOriginalSecondaryProductName(sourceProduct, slvBand);
    }

    /**
     * Returns only i and q reference band names
     * @param sourceProduct coregistered product
     * @return reference band names
     */
    public static String[] getReferenceBandNames(final Product sourceProduct) {
        final MetadataElement secondaryMetadataRoot = findSecondaryMetadataRoot(sourceProduct);
        if (secondaryMetadataRoot != null) {
            final String refBandNames = getMetadataAttribute(secondaryMetadataRoot,
                    AbstractMetadata.REFERENCE_BANDS, AbstractMetadata.LEGACY_MASTER_BANDS);
            if(!refBandNames.isEmpty()) {
                return StringUtils.stringToArray(refBandNames, " ");
            }
        }
        // Fallback: search for bands with _ref or legacy _mst suffix
        final List<String> bandNames = new ArrayList<>();
        for(String bandName : sourceProduct.getBandNames()) {
            if(containsRef(bandName)) {
                bandNames.add(bandName);
            }
        }
        return bandNames.toArray(new String[0]);
    }

    @Deprecated
    public static String[] getMasterBandNames(final Product sourceProduct) {
        return getReferenceBandNames(sourceProduct);
    }

    public static MetadataElement getSecondaryMetadata(final Product sourceProduct, final String secProductName) {
        final MetadataElement secondaryMetadataRoot = findSecondaryMetadataRoot(sourceProduct);
        if (secondaryMetadataRoot != null) {
            final MetadataElement elem = secondaryMetadataRoot.getElement(secProductName);
            return elem.createDeepClone();
        }
        return null;
    }

    @Deprecated
    public static MetadataElement getSlaveMetadata(final Product sourceProduct, final String slvProductName) {
        return getSecondaryMetadata(sourceProduct, slvProductName);
    }

    public static String[] getSecondaryBandNames(final Product sourceProduct, final String secProductName) {
        final MetadataElement secondaryMetadataRoot = findSecondaryMetadataRoot(sourceProduct);
        if (secondaryMetadataRoot != null) {
            final MetadataElement elem = secondaryMetadataRoot.getElement(secProductName);
            if(elem != null) {
                final String secBandNames = getMetadataAttribute(elem,
                        AbstractMetadata.SECONDARY_BANDS, AbstractMetadata.LEGACY_SLAVE_BANDS);
                if (!secBandNames.isEmpty()) {
                    return StringUtils.stringToArray(secBandNames, " ");
                }
            }
        }
        // Fallback: search for bands with _sec or legacy _slv suffix
        String dateSuffix = secProductName.contains("_") ?
                secProductName.substring(secProductName.lastIndexOf('_')).toLowerCase() : "";
        final List<String> bandNames = new ArrayList<>();
        for(String bandName : sourceProduct.getBandNames()) {
            final String name = bandName.toLowerCase();
            if((name.contains(SEC) || name.contains(SLV)) && name.endsWith(dateSuffix)) {
                bandNames.add(bandName);
            }
        }
        return bandNames.toArray(new String[0]);
    }

    @Deprecated
    public static String[] getSlaveBandNames(final Product sourceProduct, final String slvProductName) {
        return getSecondaryBandNames(sourceProduct, slvProductName);
    }

    public static boolean isReferenceBand(final String bandName, final Product sourceProduct) {

        final MetadataElement secondaryMetadataRoot = findSecondaryMetadataRoot(sourceProduct);

        if (secondaryMetadataRoot != null) {
            final String refBandNames = getMetadataAttribute(secondaryMetadataRoot,
                    AbstractMetadata.REFERENCE_BANDS, AbstractMetadata.LEGACY_MASTER_BANDS);
            return refBandNames.contains(bandName);
        }

        // Fallback: check for _ref or legacy _mst in band names
        for(String srcBandName : sourceProduct.getBandNames()) {
            if(containsRef(srcBandName) && srcBandName.contains(bandName)) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public static boolean isMasterBand(final String bandName, final Product sourceProduct) {
        return isReferenceBand(bandName, sourceProduct);
    }

    public static boolean isSecondaryBand(final String bandName, final Product sourceProduct) {

        final String[] secProductNames = StackUtils.getSecondaryProductNames(sourceProduct);
        for (String secProductName : secProductNames) {
            if (isSecondaryBand(bandName, sourceProduct, secProductName)) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public static boolean isSlaveBand(final String bandName, final Product sourceProduct) {
        return isSecondaryBand(bandName, sourceProduct);
    }

    public static boolean isSecondaryBand(final String bandName, final Product sourceProduct, final String secProductName) {

        final MetadataElement secondaryMetadataRoot = findSecondaryMetadataRoot(sourceProduct);

        if (secondaryMetadataRoot != null) {
            final MetadataElement elem = secondaryMetadataRoot.getElement(secProductName);
            final String secBandNames = getMetadataAttribute(elem,
                    AbstractMetadata.SECONDARY_BANDS, AbstractMetadata.LEGACY_SLAVE_BANDS);
            if(!secBandNames.isEmpty()) {
                return secBandNames.contains(bandName);
            }
        }

        // Fallback: check for _sec or legacy _slv in band names
        String dateSuffix = secProductName.substring(secProductName.lastIndexOf('_')).toLowerCase();
        for(String srcBandName : sourceProduct.getBandNames()) {
            final String name = srcBandName.toLowerCase();
            if((name.contains(SEC) || name.contains(SLV)) && name.endsWith(dateSuffix) && srcBandName.contains(bandName)) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public static boolean isSlaveBand(final String bandName, final Product sourceProduct, final String slvProductName) {
        return isSecondaryBand(bandName, sourceProduct, slvProductName);
    }

    public static String[] getSecondaryProductNames(final Product sourceProduct) {
        final MetadataElement secondaryMetadataRoot = findSecondaryMetadataRoot(sourceProduct);
        if (secondaryMetadataRoot != null) {
            return secondaryMetadataRoot.getElementNames();
        }
        return new String[]{};
    }

    @Deprecated
    public static String[] getSlaveProductNames(final Product sourceProduct) {
        return getSecondaryProductNames(sourceProduct);
    }

    public static String getBandNameWithoutDate(final String bandName) {
        if (bandName.contains(REF)) {
            return bandName.substring(0, bandName.lastIndexOf(REF));
        } else if (bandName.contains(SEC)) {
            return bandName.substring(0, bandName.lastIndexOf(SEC));
        } else if (bandName.contains(MST)) {
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
        if (bandName.contains(REF)) {
            suffix = bandName.substring(bandName.lastIndexOf(REF));
        } else if (bandName.contains(SEC)) {
            suffix = bandName.substring(bandName.lastIndexOf(SEC));
        } else if (bandName.contains(MST)) {
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

    public static String getSecondaryProductName(final Product sourceProduct, final Band secBand, final String refPol) {
        final MetadataElement secondaryMetadataRoot = findSecondaryMetadataRoot(sourceProduct);
        if (secondaryMetadataRoot != null) {
            final String secBandName = secBand.getName();
            for (MetadataElement elem : secondaryMetadataRoot.getElements()) {
                final String secBandNames = getMetadataAttribute(elem,
                        AbstractMetadata.SECONDARY_BANDS, AbstractMetadata.LEGACY_SLAVE_BANDS);
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

    @Deprecated
    public static String getSlaveProductName(final Product sourceProduct, final Band secBand, final String refPol) {
        return getSecondaryProductName(sourceProduct, secBand, refPol);
    }

    public static ProductData.UTC[] getProductTimes(final Product sourceProduct) {
        final List<ProductData.UTC> utcList = new ArrayList<>();
        // add reference time
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        if (absRoot != null) {
            utcList.add(absRoot.getAttributeUTC(AbstractMetadata.first_line_time));

            // add secondary times
            final MetadataElement secondaryMetadataRoot = sourceProduct.getMetadataRoot().getElement(
                    AbstractMetadata.SECONDARY_METADATA_ROOT);
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
