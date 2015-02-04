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

package org.esa.beam.dataio.s3;

import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SafeManifestTest {

    private Manifest manifestTest;

    @Before
    public void before() throws ParserConfigurationException, IOException, SAXException {
        InputStream stream = getClass().getResourceAsStream("SYN_TEST_manifest.safe");
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            manifestTest = SafeManifest.createManifest(doc);
        } finally {
            stream.close();
        }
    }

    @Test
    public void testGetDescription() throws Exception {
        assertEquals("Sentinel 3 SYN Level 2", manifestTest.getDescription());
    }

    @Test
    public void testGetStartTime() throws Exception {
        ProductData.UTC expected = ProductData.UTC.parse("2013-06-21T10:09:20", "yyyy-MM-dd'T'HH:mm:ss");
        final ProductData.UTC startTime = manifestTest.getStartTime();
        assertTrue(expected.equalElems(startTime));
    }

    @Test
    public void testGetStopTime() throws Exception {
        ProductData.UTC expected = ProductData.UTC.parse("2013-06-21T10:14:13", "yyyy-MM-dd'T'HH:mm:ss");
        final ProductData.UTC stopTime = manifestTest.getStopTime();
        assertTrue(expected.equalElems(stopTime));
    }

    @Test
    public void testGetMeasurementFileNames() {
        List<String> measurementFiles = manifestTest.getFileNames("measurementDataSchema");
        assertEquals(34, measurementFiles.size());
        assertEquals("r0400.nc", measurementFiles.get(0));
        assertEquals("r0560.nc", measurementFiles.get(5));
        assertEquals("r0550n.nc", measurementFiles.get(18));
        assertEquals("r1375o.nc", measurementFiles.get(27));
        assertEquals("flags.nc", measurementFiles.get(33));
    }

    @Test
    public void testGetTiepointFileNames() {
        List<String> tiepointsFiles = manifestTest.getFileNames("tiepointsSchema");
        assertEquals(4, tiepointsFiles.size());
        assertEquals("tiepoints_meteo.nc", tiepointsFiles.get(0));
        assertEquals("tiepoints_olci.nc", tiepointsFiles.get(1));
        assertEquals("tiepoints_slstr_n.nc", tiepointsFiles.get(2));
        assertEquals("tiepoints_slstr_o.nc", tiepointsFiles.get(3));
    }
}
