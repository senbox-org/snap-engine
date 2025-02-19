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
    public void testSaveMasterProductBandNames() throws Exception {
        final Product product = createStackProduct(4);

        String[] bandNames = new String[] {"band1","band2"};
        StackUtils.saveMasterProductBandNames(product, bandNames);

        final MetadataElement targetSlaveMetadataRoot = AbstractMetadata.getSlaveMetadata(product.getMetadataRoot());
        assertNotNull(targetSlaveMetadataRoot);
        String masterBands = targetSlaveMetadataRoot.getAttributeString(AbstractMetadata.MASTER_BANDS);
        assertEquals("band1 band2", masterBands);
    }

    @Test
    public void testSaveSlaveProductBandNames_WrongProduct() throws Exception {
        final Product product = createStackProduct(4);
        String[] bandNames = new String[] {"band1","band2"};

        Exception exception = assertThrows(Exception.class,
                ()->StackUtils.saveSlaveProductBandNames(product, "wrongProduct", bandNames));
        System.out.println(exception.getMessage());
        assertTrue(exception.getMessage().contains("wrongProduct metadata not found"));
    }

    @Test
    public void testSaveSlaveProductBandNames() throws Exception {
        final Product product = createStackProduct(4);

        String[] bandNames = new String[] {"band1","band2"};
        StackUtils.saveSlaveProductBandNames(product, "product2_01Feb21", bandNames);

        final MetadataElement targetSlaveMetadataRoot = AbstractMetadata.getSlaveMetadata(product.getMetadataRoot());
        assertNotNull(targetSlaveMetadataRoot);
        String masterBands = targetSlaveMetadataRoot.getAttributeString(AbstractMetadata.MASTER_BANDS);
        assertEquals("band_mst1_01Jan21", masterBands);
    }

    @Test
    public void testFindOriginalSlaveProductName() throws Exception {
        final Product product = createStackProduct(4);

        assertEquals("product2_01Feb21", StackUtils.findOriginalSlaveProductName(product, product.getBandAt(2)));
    }

    @Test
    public void testGetSlaveBandNames() throws Exception {
        final Product product = createStackProduct(4);

        String[] bandNames = StackUtils.getSlaveBandNames(product, "unknown");
        assertArrayEquals(new String[] {"band_slv2_01Feb21", "band_slv3_01Feb21"}, bandNames);

        bandNames = StackUtils.getSlaveBandNames(product, "product2_01Feb21");
        assertArrayEquals(new String[] {"band_slv2_01Feb21", "band_slv3_01Feb21"}, bandNames);
    }

    @Test
    public void testIsMasterBand() throws Exception {
        final Product product = createStackProduct(4);

        assertTrue(StackUtils.isMasterBand(product.getBandAt(0).getName(), product));
        assertFalse(StackUtils.isMasterBand(product.getBandAt(1).getName(), product));
    }

    @Test
    public void testIsSlaveBand() throws Exception {
        final Product product = createStackProduct(4);

        assertFalse(StackUtils.isSlaveBand(product.getBandAt(0).getName(), product));
        assertTrue(StackUtils.isSlaveBand(product.getBandAt(1).getName(), product));
    }

    @Test
    public void testGetSlaveProductNames() throws Exception {
        final Product product = createStackProduct(4);

        String[] productNames = StackUtils.getSlaveProductNames(product);
        assertArrayEquals(new String[] {"product2_01Feb21"}, productNames);
    }

    @Test
    public void testGetBandNameWithoutDate() {

        assertEquals("band", StackUtils.getBandNameWithoutDate("band_mst1_01Jan21"));
        assertEquals("band", StackUtils.getBandNameWithoutDate("band_slv2_01Feb21"));
        assertEquals("band", StackUtils.getBandNameWithoutDate("band_01Feb21"));
        assertEquals("band", StackUtils.getBandNameWithoutDate("band"));
    }

    @Test
    public void testGetBandSuffixes() throws Exception {
        final Product product = createStackProduct(4);

        String[] suffixes = StackUtils.getBandSuffixes(product.getBands());
        assertArrayEquals(new String[] {"_mst1_01Jan21", "_slv2_01Feb21", "_slv3_01Feb21"}, suffixes);
    }

    @Test
    public void testGetBandDates() throws Exception {
        final Product product = createStackProduct(4);

        String[] suffixes = StackUtils.getBandDates(product.getBands());
        assertArrayEquals(new String[] {"_01Feb21", "_01Jan21"}, suffixes);
    }

    @Test
    public void testGetBandSuffix() {

        assertEquals("_mst1_01Jan21", StackUtils.getBandSuffix("band_mst1_01Jan21"));
        assertEquals("_slv2_01Feb21", StackUtils.getBandSuffix("band_slv2_01Feb21"));
        assertEquals("_01Feb21", StackUtils.getBandSuffix("band_01Feb21"));
        assertEquals("band", StackUtils.getBandSuffix("band"));
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
        assertArrayEquals(new String[] {"band_mst1_01Jan21", "band_slv2_01Feb21", "band_slv3_01Feb21"}, bandNames);
    }

    @Test
    @STTM("SNAP-3651")
    public void testSaveSlaveProductBandNames_appendNames() throws Exception {
        final Product product = createStackProduct(4);

        String[] bandNames = new String[] {"band1","band2"};
        StackUtils.saveSlaveProductBandNames(product, "product2_01Feb21", bandNames);

        final MetadataElement targetSlaveMetadataRoot = AbstractMetadata.getSlaveMetadata(product.getMetadataRoot());
        assertNotNull(targetSlaveMetadataRoot);
        final String masterBands = targetSlaveMetadataRoot.getAttributeString(AbstractMetadata.MASTER_BANDS);
        assertEquals("band_mst1_01Jan21", masterBands);

        final MetadataElement slaveProductElem = targetSlaveMetadataRoot.getElement("product2_01Feb21");
        assertNotNull(slaveProductElem);
        final String slaveBands = slaveProductElem.getAttributeString(AbstractMetadata.SLAVE_BANDS);
        assertEquals("band_slv2_01Feb21 band_slv3_01Feb21 band1 band2", slaveBands);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static Product createStackProduct(final int numBands) throws Exception {
        final int w = 10, h = 10;
        final Product product = TestUtils.createProduct("type", w, h);

        String date = "_01Jan21";
        Band mstBand = TestUtils.createBand(product, "band_mst" + 1 + date, w, h);
        StackUtils.saveMasterProductBandNames(product, new String[] {mstBand.getName()});

        final List<String> slvBands = new ArrayList<>();
        date = "_01Feb21";
        for(int i=2; i < numBands; ++i) {
            Band band = TestUtils.createBand(product, "band_slv" + i + date, w, h);
            slvBands.add(band.getName());
        }
        String slvProductName = "product2"+date;
        addStackMetadata(product, slvProductName);
        StackUtils.saveSlaveProductBandNames(product, slvProductName, slvBands.toArray(new String[0]));

        return product;
    }

    private static void addStackMetadata(final Product product, final String secondaryProductName) {
        final MetadataElement root = product.getMetadataRoot();
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        absRoot.setAttributeInt(AbstractMetadata.coregistered_stack, 1);

        final MetadataElement slv1Meta = absRoot.createDeepClone();
        slv1Meta.setName(secondaryProductName);
        slv1Meta.setAttributeString(AbstractMetadata.PRODUCT, secondaryProductName);

        MetadataElement slaveMetadata;
        if(root.containsElement(AbstractMetadata.SLAVE_METADATA_ROOT)) {
            slaveMetadata = root.getElement(AbstractMetadata.SLAVE_METADATA_ROOT);
        } else {
            slaveMetadata = new MetadataElement(AbstractMetadata.SLAVE_METADATA_ROOT);
            root.addElement(slaveMetadata);
        }
        slaveMetadata.addElement(slv1Meta);
    }
}
