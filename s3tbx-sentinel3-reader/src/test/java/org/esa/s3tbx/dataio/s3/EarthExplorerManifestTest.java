/*
 * Copyright (c) 2012. Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA
 */

package org.esa.s3tbx.dataio.s3;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import static org.junit.Assert.*;

public class EarthExplorerManifestTest {

    private static Manifest manifest;

    @BeforeClass
    public static void before() throws ParserConfigurationException, IOException, SAXException {
        try (InputStream stream = EarthExplorerManifestTest.class.getResourceAsStream("Earth_Explorer_manifest.xml")) {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            manifest = EarthExplorerManifest.createManifest(doc);
        }
    }

    @Test
    public void testGetProductName() throws Exception {
        assertEquals("S3_OL_1_ERR_20130621T100921_20130621T101413_00291_000001_001_EST_TEST_00", manifest.getProductName());
    }

    @Test
    public void testGetProductType() throws Exception {
        assertEquals("OL_1_ERR", manifest.getProductType());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Sentinel 3 Level 1B Product", manifest.getDescription());
    }

    @Test
    public void testGetStartTime() throws ParseException {
        ProductData.UTC expected = ProductData.UTC.parse("2013-06-21T10:09:20.659100", "yyyy-MM-dd'T'HH:mm:ss");
        assertTrue(expected.equalElems(manifest.getStartTime()));
    }

    @Test
    public void testGetStopTime() throws ParseException {
        ProductData.UTC expected = ProductData.UTC.parse("2013-06-21T10:14:12.597100", "yyyy-MM-dd'T'HH:mm:ss");
        assertTrue(expected.equalElems(manifest.getStopTime()));
    }

    @Test
    public void testGetMetadata() {
        final MetadataElement manifestElement = manifest.getMetadata();

        assertNotNull(manifestElement);
        assertEquals("Manifest", manifestElement.getName());
        assertEquals(0, manifestElement.getNumAttributes());
        assertEquals(1, manifestElement.getNumElements());

        final MetadataElement headerElement = manifestElement.getElement("Earth_Explorer_Header");
        assertNotNull(headerElement);
        assertEquals(2, headerElement.getNumElements());

        final MetadataElement fixedHeaderElement = headerElement.getElement("Fixed_Header");
        assertNotNull(fixedHeaderElement);
        assertEquals(7, fixedHeaderElement.getNumAttributes());
        assertEquals("TEST", fixedHeaderElement.getAttributeString("File_Class"));
        MetadataElement validityPeriodElement = fixedHeaderElement.getElement("Validity_Period");
        assertNotNull(validityPeriodElement);
        assertEquals(2, validityPeriodElement.getNumAttributes());
    }
}
