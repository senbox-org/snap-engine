package org.esa.s3tbx.slstr.pdu.stitching;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Tonio Fincke
 */
public class OrbitReferenceCheckerTest {

    @Test
    public void testCheckForSameOrbitReference() throws ParserConfigurationException, IOException, SAXException {
        final File[] slstrFiles = TestUtils.getSlstrFiles();
        Document[] manifests = new Document[slstrFiles.length];
        for (int i = 0; i < slstrFiles.length; i++) {
            manifests[i] = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(slstrFiles[i]);
        }
        try {
            OrbitReferenceChecker.validateOrbitReference(manifests);
        } catch (PDUStitchingException e) {
            fail("No exception expected");
        }
    }

    @Test
    public void testCheckForSameOrbitReference_DifferentOrbitReference() throws ParserConfigurationException, IOException, SAXException {
        final File[] slstrFiles = TestUtils.getSlstrFiles();
        Document[] manifests = new Document[slstrFiles.length + 1];
        for (int i = 0; i < slstrFiles.length; i++) {
            manifests[i] = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(slstrFiles[i]);
        }
        String orbitRef =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "          <sentinel-safe:orbitReference>\n" +
                        "            <sentinel-safe:orbitNumber groundTrackDirection=\"descending\" type=\"start\">60627</sentinel-safe:orbitNumber>\n" +
                        "            <sentinel-safe:relativeOrbitNumber groundTrackDirection=\"descending\" type=\"start\">182</sentinel-safe:relativeOrbitNumber>\n" +
                        "            <sentinel-safe:passNumber groundTrackDirection=\"descending\" type=\"start\">121254</sentinel-safe:passNumber>\n" +
                        "            <sentinel-safe:relativePassNumber groundTrackDirection=\"descending\" type=\"start\">364</sentinel-safe:relativePassNumber>\n" +
                        "            <sentinel-safe:cycleNumber>158</sentinel-safe:cycleNumber>\n" +
                        "            <sentinel-safe:phaseIdentifier>2</sentinel-safe:phaseIdentifier>\n" +
                        "          </sentinel-safe:orbitReference>";
        final DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        manifests[manifests.length - 1] =
                documentBuilder.parse(new InputSource(new ByteArrayInputStream(orbitRef.getBytes("utf-8"))));
        try {
            OrbitReferenceChecker.validateOrbitReference(manifests);
            fail("Exception expected");
        } catch (PDUStitchingException e) {
            assertEquals("Invalid orbit reference for element sentinel-safe:phaseIdentifier", e.getMessage());
        }
    }

}