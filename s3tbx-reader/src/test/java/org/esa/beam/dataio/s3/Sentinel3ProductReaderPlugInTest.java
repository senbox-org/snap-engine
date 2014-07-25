package org.esa.beam.dataio.s3;/*
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

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class Sentinel3ProductReaderPlugInTest {

    private Sentinel3ProductReaderPlugIn plugIn;

    @Before
    public void setup() {
        plugIn = new Sentinel3ProductReaderPlugIn();
    }

    @Test
    public void testIfPlugInIsLoaded() {
        final ProductIOPlugInManager ioPlugInManager = ProductIOPlugInManager.getInstance();
        final Iterator<ProductReaderPlugIn> readerPlugIns = ioPlugInManager.getReaderPlugIns("SENTINEL-3");
        assertTrue(readerPlugIns.hasNext());
        assertTrue(readerPlugIns.next() instanceof Sentinel3ProductReaderPlugIn);
    }

    @Test
    public void testDecodeQualification_OlciLevel1b() {
        String path;

        path = createManifestFilePath("OL", "1", "ERR", "");
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(path));

        path = createManifestFilePath("OL", "1", "EFR", "");
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(path));
    }

    @Test
         public void testDecodeQualification_OlciLevel2L() {
        final String path = createManifestFilePath("OL", "2", "LFR", ".SEN3");
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(path));
    }

    @Test
    public void testDecodeQualification_OlciLevel2W() {
        final String path = createManifestFilePath("OL", "2", "WFR", ".SEN3");
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(path));
    }

    @Test
    public void testDecodeQualification_SlstrLevel1b() {
        final String path = createManifestFilePath("SL", "1", "RBT", ".SEN3");
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(path));
    }

    @Test
    public void testDecodeQualification_SlstrWct() {
        final String path = createManifestFilePath("SL", "2", "WCT", ".SEN3");
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(path));
    }

    @Test
    public void testDecodeQualification_SlstrWst() {
        final String path = createManifestFilePath("SL", "2", "WST", ".SEN3");
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(path));
    }

    @Test
    public void testDecodeQualification_SlstrLst() {
        final String path = createManifestFilePath("SL", "2", "LST", ".SEN3");
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(path));
    }

    @Test
    public void testDecodeQualification_SynergyLevel2() {
        final String path = createManifestFilePath("SY", "2", "SYN", ".SEN3");
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(path));
    }

    @Test
    public void testDecodeQualification_VgtP() {
        final String path = createManifestFilePath("SY", "2", "VGP", ".SEN3");
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(path));
    }

    @Test
    public void testDecodeQualification_VgtS() {
        final String path = createManifestFilePath("SY", "3", "VG1", ".SEN3");
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(path));
    }

    @Test
    public void testDecodeQualification_WithInvalidDataSource() {
        String invalidPath = createManifestFilePath("SL", "1", "XXX", "");
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(invalidPath));
    }

    @Test
    public void testDecodeQualificationWith_WrongFile() {
        final String invalidPath = "/S3_SY_2_ERR_TTTTTTTTTTTT_instanceID_GGG_CCCC_VV/someFile.doc";
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(invalidPath));
    }

    @Test
    public void testDecodeQualification_WithoutFile() {
        final String invalidPath = "/SY_1_ERR_TTTTTTTTTTTT_instanceID_GGG_CCCC_VV";
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(invalidPath));
    }

    @Test
    public void testSupportedInputTypes() {
        Class[] inputTypes = plugIn.getInputTypes();
        assertEquals(2, inputTypes.length);
        assertEquals(String.class, inputTypes[0]);
        assertEquals(File.class, inputTypes[1]);
    }

    @Test
    public void testCreateReaderInstanceReturnsNewInstanceEachTime() {
        final ProductReader firstInstance = plugIn.createReaderInstance();
        assertNotNull(firstInstance);
        final ProductReader secondInstance = plugIn.createReaderInstance();
        assertNotSame(secondInstance, firstInstance);
    }

    private static String createManifestFilePath(String sensorId, String levelId, String productId, String suffix) {

//        "S3.?_(OL_1_E[FR]R|OL_2_(L[FR]R|W[FR]R)|SL_1_RBT|SL_2_(LST|WCT|WST)|SY_2_(VGP|SYN)|SY_[23]_VG1)_.*(.SEN3)?"

        String validParentDirectory = String.format("/S3_%s_%s_%s_TTTTTTTTTTTT_.*%s/", sensorId,
                                                    levelId, productId, suffix);
        String manifestFile = "xfdumanifest.xml";
        return validParentDirectory + manifestFile;
    }

}
