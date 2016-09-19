package org.esa.s3tbx.dataio.s3;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Tonio Fincke
 */
public class XfduManifestTest {


    private static Manifest manifestTest;

    @BeforeClass
    public static void setUp() throws ParserConfigurationException, IOException, SAXException {
        try (InputStream stream = XfduManifestTest.class.getResourceAsStream("xfdumanifest.xml")) {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            manifestTest = XfduManifest.createManifest(doc);
        }
    }

    @Test
    public void testGetProductname() throws Exception {
        assertEquals("S3A_SL_1_RBT____20160820T201637_20160820T201937_20160820T222707_0179_007_370_4500_MAR_O_NR_001.SEN3", manifestTest.getProductName());
    }

    @Test
    public void testGetProductType() throws Exception {
        assertEquals("SL_1_RBT", manifestTest.getProductType());
    }

    @Test
    public void testGetDescription() throws Exception {
        assertEquals("Sentinel 3 SYN Level 2", manifestTest.getDescription());
    }

    @Test
    public void testGetStartTime() throws Exception {
        ProductData.UTC expected = ProductData.UTC.parse("2013-06-21T10:09:20.646463", "yyyy-MM-dd'T'HH:mm:ss");
        final ProductData.UTC startTime = manifestTest.getStartTime();
        assertTrue(expected.equalElems(startTime));
    }

    @Test
    public void testGetStopTime() throws Exception {
        ProductData.UTC expected = ProductData.UTC.parse("2013-06-21T10:14:13.646463", "yyyy-MM-dd'T'HH:mm:ss");
        final ProductData.UTC stopTime = manifestTest.getStopTime();
        assertTrue(expected.equalElems(stopTime));
    }

    @Test
    public void testGetFileNames() {
        String[] excluded = new String[0];
        List<String> fileNames = manifestTest.getFileNames(excluded);
        assertEquals(67, fileNames.size());
        assertEquals("r0400.nc", fileNames.get(0));
        assertEquals("r0560.nc", fileNames.get(5));
        assertEquals("r0550n.nc", fileNames.get(18));
        assertEquals("r1375o.nc", fileNames.get(27));
        assertEquals("flags.nc", fileNames.get(66));
    }

    @Test
    public void testGetFileNames_Exclusions() {
        String[] excluded = new String[]{"aerosolModelIndex", "pixelStatusFlags"};
        List<String> fileNames = manifestTest.getFileNames(excluded);
        assertEquals(65, fileNames.size());
        assertEquals(false, fileNames.contains("amin.nc"));
        assertEquals(false, fileNames.contains("flags.nc"));
        assertEquals("r0560.nc", fileNames.get(5));
        assertEquals("r0550n.nc", fileNames.get(18));
        assertEquals("r1375o.nc", fileNames.get(27));
    }

}
