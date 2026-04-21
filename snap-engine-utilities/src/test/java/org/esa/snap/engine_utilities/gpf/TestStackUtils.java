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

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class TestStackUtils {

    @Test
    public void testIsCoregistered() {
        final Product product = TestUtils.createProduct("type", 10, 10);

        assertFalse(StackUtils.isCoregisteredStack(product));
        assertFalse(StackUtils.isBiStaticStack(product));

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        absRoot.setAttributeInt(AbstractMetadata.coregistered_stack, 1);
        absRoot.setAttributeInt(AbstractMetadata.bistatic_stack, 1);

        assertTrue(StackUtils.isCoregisteredStack(product));
        assertTrue(StackUtils.isBiStaticStack(product));
    }

    @Test
    public void testCreateBandTimeStamp() throws Exception {
        final Product product = createStackProduct(4);

        String timeStamp = StackUtils.createBandTimeStamp(product);
        assertEquals("_10May2008", timeStamp);
    }

    @Test
    public void testSaveReferenceProductBandNames() throws Exception {
        final Product product = createStackProduct(4);

        String[] bandNames = new String[] {"band1","band2"};
        StackUtils.saveReferenceProductBandNames(product, bandNames);

        final MetadataElement targetSecondaryMetadataRoot = AbstractMetadata.getSecondaryMetadata(product.getMetadataRoot());
        assertNotNull(targetSecondaryMetadataRoot);
        String referenceBands = targetSecondaryMetadataRoot.getAttributeString(AbstractMetadata.REFERENCE_BANDS);
        assertEquals("band1 band2", referenceBands);
    }

    @Test
    public void testSaveSecondaryProductBandNames_WrongProduct() throws Exception {
        final Product product = createStackProduct(4);
        String[] bandNames = new String[] {"band1","band2"};

        Exception exception = assertThrows(Exception.class,
                ()->StackUtils.saveSecondaryProductBandNames(product, "wrongProduct", bandNames));
        System.out.println(exception.getMessage());
        assertTrue(exception.getMessage().contains("wrongProduct metadata not found"));
    }

    @Test
    public void testSaveSecondaryProductBandNames() throws Exception {
        final Product product = createStackProduct(4);

        String[] bandNames = new String[] {"band1","band2"};
        StackUtils.saveSecondaryProductBandNames(product, "product2_01Feb21", bandNames);

        final MetadataElement targetSecondaryMetadataRoot = AbstractMetadata.getSecondaryMetadata(product.getMetadataRoot());
        assertNotNull(targetSecondaryMetadataRoot);
        String referenceBands = targetSecondaryMetadataRoot.getAttributeString(AbstractMetadata.REFERENCE_BANDS);
        assertEquals("band_ref1_01Jan21", referenceBands);
    }

    @Test
    public void testFindOriginalSecondaryProductName() throws Exception {
        final Product product = createStackProduct(4);

        assertEquals("product2_01Feb21", StackUtils.findOriginalSecondaryProductName(product, product.getBandAt(2)));
    }

    @Test
    public void testGetSecondaryBandNames() throws Exception {
        final Product product = createStackProduct(4);

        String[] bandNames = StackUtils.getSecondaryBandNames(product, "unknown");
        assertArrayEquals(new String[] {"band_sec2_01Feb21", "band_sec3_01Feb21"}, bandNames);

        bandNames = StackUtils.getSecondaryBandNames(product, "product2_01Feb21");
        assertArrayEquals(new String[] {"band_sec2_01Feb21", "band_sec3_01Feb21"}, bandNames);
    }

    @Test
    public void testIsReferenceBand() throws Exception {
        final Product product = createStackProduct(4);

        assertTrue(StackUtils.isReferenceBand(product.getBandAt(0).getName(), product));
        assertFalse(StackUtils.isReferenceBand(product.getBandAt(1).getName(), product));
    }

    @Test
    public void testIsSecondaryBand() throws Exception {
        final Product product = createStackProduct(4);

        assertFalse(StackUtils.isSecondaryBand(product.getBandAt(0).getName(), product));
        assertTrue(StackUtils.isSecondaryBand(product.getBandAt(1).getName(), product));
    }

    @Test
    public void testGetSecondaryProductNames() throws Exception {
        final Product product = createStackProduct(4);

        String[] productNames = StackUtils.getSecondaryProductNames(product);
        assertArrayEquals(new String[] {"product2_01Feb21"}, productNames);
    }

    @Test
    public void testGetBandNameWithoutDate() {

        assertEquals("band", StackUtils.getBandNameWithoutDate("band_ref1_01Jan21"));
        assertEquals("band", StackUtils.getBandNameWithoutDate("band_sec2_01Feb21"));
        assertEquals("band", StackUtils.getBandNameWithoutDate("band_01Feb21"));
        assertEquals("band", StackUtils.getBandNameWithoutDate("band"));
    }

    @Test
    public void testGetBandNameWithoutDate_Legacy() {
        // Backwards compatibility: legacy _mst/_slv naming should still work
        assertEquals("band", StackUtils.getBandNameWithoutDate("band_mst1_01Jan21"));
        assertEquals("band", StackUtils.getBandNameWithoutDate("band_slv2_01Feb21"));
    }

    @Test
    public void testGetBandSuffixes() throws Exception {
        final Product product = createStackProduct(4);

        String[] suffixes = StackUtils.getBandSuffixes(product.getBands());
        assertArrayEquals(new String[] {"_ref1_01Jan21", "_sec2_01Feb21", "_sec3_01Feb21"}, suffixes);
    }

    @Test
    public void testGetBandDates() throws Exception {
        final Product product = createStackProduct(4);

        String[] suffixes = StackUtils.getBandDates(product.getBands());
        assertArrayEquals(new String[] {"_01Feb21", "_01Jan21"}, suffixes);
    }

    @Test
    public void testGetBandSuffix() {

        assertEquals("_ref1_01Jan21", StackUtils.getBandSuffix("band_ref1_01Jan21"));
        assertEquals("_sec2_01Feb21", StackUtils.getBandSuffix("band_sec2_01Feb21"));
        assertEquals("_01Feb21", StackUtils.getBandSuffix("band_01Feb21"));
        assertEquals("band", StackUtils.getBandSuffix("band"));
    }

    @Test
    public void testGetBandSuffix_Legacy() {
        // Backwards compatibility: legacy _mst/_slv naming should still work
        assertEquals("_mst1_01Jan21", StackUtils.getBandSuffix("band_mst1_01Jan21"));
        assertEquals("_slv2_01Feb21", StackUtils.getBandSuffix("band_slv2_01Feb21"));
    }

    @Test
    public void testGetProductTimes() throws Exception {
        final Product product = createStackProduct(4);

        ProductData.UTC[] utc = StackUtils.getProductTimes(product);
        assertEquals(2, utc.length);
        assertEquals("10-MAY-2008 20:30:46.890683", utc[0].format());
        assertEquals("10-MAY-2008 20:30:46.890683", utc[1].format());
    }

    @Test
    public void testBandsToStringArray() throws Exception {
        final Product product = createStackProduct(4);

        String[] bandNames = StackUtils.bandsToStringArray(product.getBands());
        assertArrayEquals(new String[] {"band_ref1_01Jan21", "band_sec2_01Feb21", "band_sec3_01Feb21"}, bandNames);
    }

    @Test
    @STTM("SNAP-3651")
    public void testSaveSecondaryProductBandNames_appendNames() throws Exception {
        final Product product = createStackProduct(4);

        String[] bandNames = new String[] {"band1","band2"};
        StackUtils.saveSecondaryProductBandNames(product, "product2_01Feb21", bandNames);

        final MetadataElement targetSecondaryMetadataRoot = AbstractMetadata.getSecondaryMetadata(product.getMetadataRoot());
        assertNotNull(targetSecondaryMetadataRoot);
        final String referenceBands = targetSecondaryMetadataRoot.getAttributeString(AbstractMetadata.REFERENCE_BANDS);
        assertEquals("band_ref1_01Jan21", referenceBands);

        final MetadataElement secondaryProductElem = targetSecondaryMetadataRoot.getElement("product2_01Feb21");
        assertNotNull(secondaryProductElem);
        final String secondaryBands = secondaryProductElem.getAttributeString(AbstractMetadata.SECONDARY_BANDS);
        assertEquals("band_sec2_01Feb21 band_sec3_01Feb21 band1 band2", secondaryBands);
    }

    @Test
    public void testConvertLegacyBandName() {
        assertEquals("band_ref1_01Jan21", StackUtils.convertLegacyBandName("band_mst1_01Jan21"));
        assertEquals("band_sec2_01Feb21", StackUtils.convertLegacyBandName("band_slv2_01Feb21"));
        assertEquals("band_01Feb21", StackUtils.convertLegacyBandName("band_01Feb21"));
    }

    @Test
    public void testHasLegacyNaming() {
        assertTrue(StackUtils.hasLegacyNaming("band_mst1_01Jan21"));
        assertTrue(StackUtils.hasLegacyNaming("band_slv2_01Feb21"));
        assertFalse(StackUtils.hasLegacyNaming("band_ref1_01Jan21"));
        assertFalse(StackUtils.hasLegacyNaming("band_sec2_01Feb21"));
        assertFalse(StackUtils.hasLegacyNaming("band_01Feb21"));
    }

    @Test
    public void testRenameLegacyBands() throws Exception {
        final Product product = createLegacyStackProduct(4);

        // Verify bands have legacy naming
        assertEquals("band_mst1_01Jan21", product.getBandAt(0).getName());
        assertEquals("band_slv2_01Feb21", product.getBandAt(1).getName());

        StackUtils.renameLegacyBands(product);

        // Verify bands now have new naming
        assertEquals("band_ref1_01Jan21", product.getBandAt(0).getName());
        assertEquals("band_sec2_01Feb21", product.getBandAt(1).getName());
        assertEquals("band_sec3_01Feb21", product.getBandAt(2).getName());
    }

    @Test
    public void testLegacyProductBackwardsCompatibility() throws Exception {
        // Test that a product with legacy _mst/_slv naming still works with all methods
        final Product product = createLegacyStackProduct(4);

        // getReferenceBandNames should find _mst bands via metadata
        String[] refBands = StackUtils.getReferenceBandNames(product);
        assertEquals(1, refBands.length);
        assertEquals("band_mst1_01Jan21", refBands[0]);

        // isReferenceBand should work with legacy naming
        assertTrue(StackUtils.isReferenceBand("band_mst1_01Jan21", product));
        assertFalse(StackUtils.isReferenceBand("band_slv2_01Feb21", product));

        // isSecondaryBand should work with legacy naming
        assertTrue(StackUtils.isSecondaryBand("band_slv2_01Feb21", product));
        assertFalse(StackUtils.isSecondaryBand("band_mst1_01Jan21", product));

        // getBandNameWithoutDate should work with legacy naming
        assertEquals("band", StackUtils.getBandNameWithoutDate("band_mst1_01Jan21"));
        assertEquals("band", StackUtils.getBandNameWithoutDate("band_slv2_01Feb21"));

        // getBandSuffix should work with legacy naming
        assertEquals("_mst1_01Jan21", StackUtils.getBandSuffix("band_mst1_01Jan21"));
        assertEquals("_slv2_01Feb21", StackUtils.getBandSuffix("band_slv2_01Feb21"));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static Product createStackProduct(final int numBands) throws Exception {
        final int w = 10, h = 10;
        final Product product = TestUtils.createProduct("type", w, h);

        String date = "_01Jan21";
        Band refBand = TestUtils.createBand(product, "band_ref" + 1 + date, w, h);
        StackUtils.saveReferenceProductBandNames(product, new String[] {refBand.getName()});

        final List<String> secBands = new ArrayList<>();
        date = "_01Feb21";
        for(int i=2; i < numBands; ++i) {
            Band band = TestUtils.createBand(product, "band_sec" + i + date, w, h);
            secBands.add(band.getName());
        }
        String secProductName = "product2"+date;
        addStackMetadata(product, secProductName);
        StackUtils.saveSecondaryProductBandNames(product, secProductName, secBands.toArray(new String[0]));

        return product;
    }

    private static Product createLegacyStackProduct(final int numBands) throws Exception {
        final int w = 10, h = 10;
        final Product product = TestUtils.createProduct("type", w, h);

        String date = "_01Jan21";
        Band refBand = TestUtils.createBand(product, "band_mst" + 1 + date, w, h);
        StackUtils.saveReferenceProductBandNames(product, new String[] {refBand.getName()});

        final List<String> secBands = new ArrayList<>();
        date = "_01Feb21";
        for(int i=2; i < numBands; ++i) {
            Band band = TestUtils.createBand(product, "band_slv" + i + date, w, h);
            secBands.add(band.getName());
        }
        String secProductName = "product2"+date;
        addStackMetadata(product, secProductName);
        StackUtils.saveSecondaryProductBandNames(product, secProductName, secBands.toArray(new String[0]));

        return product;
    }

    private static void addStackMetadata(final Product product, final String secondaryProductName) {
        final MetadataElement root = product.getMetadataRoot();
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        absRoot.setAttributeInt(AbstractMetadata.coregistered_stack, 1);

        final MetadataElement sec1Meta = absRoot.createDeepClone();
        sec1Meta.setName(secondaryProductName);
        sec1Meta.setAttributeString(AbstractMetadata.PRODUCT, secondaryProductName);

        MetadataElement secondaryMetadata;
        if(root.containsElement(AbstractMetadata.SECONDARY_METADATA_ROOT)) {
            secondaryMetadata = root.getElement(AbstractMetadata.SECONDARY_METADATA_ROOT);
        } else {
            secondaryMetadata = new MetadataElement(AbstractMetadata.SECONDARY_METADATA_ROOT);
            root.addElement(secondaryMetadata);
        }
        secondaryMetadata.addElement(sec1Meta);
    }
}
