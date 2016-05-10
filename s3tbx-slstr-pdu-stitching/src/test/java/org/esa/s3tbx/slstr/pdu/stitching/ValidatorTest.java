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
public class ValidatorTest {

    @Test
    public void testValidate() throws ParserConfigurationException, SAXException {
        final File[] slstrFiles = TestUtils.getSlstrFiles();
        try {
            Validator.validate(slstrFiles);
        } catch (IOException e) {
            fail("No exception expected: " + e.getMessage());
        }
    }

    @Test
    public void testValidateOrbitReference_DifferentOrbitReference() throws ParserConfigurationException, IOException, SAXException {
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
            Validator.validateOrbitReference(manifests);
            fail("Exception expected");
        } catch (PDUStitchingException e) {
            assertEquals("Invalid orbit reference due to different element sentinel-safe:phaseIdentifier", e.getMessage());
        }
    }

    @Test
    public void testValidateOrbitReference_UpdatedOrbitReference() throws ParserConfigurationException, IOException, SAXException {
        String orbitRef =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "          <sentinel-safe:orbitReference>\n" +
                        "            <sentinel-safe:orbitNumber groundTrackDirection=\"descending\" type=\"start\">30480</sentinel-safe:orbitNumber>\n" +
                        "            <sentinel-safe:orbitNumber groundTrackDirection=\"descending\" type=\"stop\">30480</sentinel-safe:orbitNumber>\n" +
                        "            <sentinel-safe:relativeOrbitNumber groundTrackDirection=\"descending\" type=\"start\">366</sentinel-safe:relativeOrbitNumber>\n" +
                        "            <sentinel-safe:relativeOrbitNumber groundTrackDirection=\"descending\" type=\"stop\">366</sentinel-safe:relativeOrbitNumber>\n" +
                        "            <sentinel-safe:passNumber groundTrackDirection=\"descending\" type=\"start\">60960</sentinel-safe:passNumber>\n" +
                        "            <sentinel-safe:passNumber groundTrackDirection=\"descending\" type=\"stop\">60960</sentinel-safe:passNumber>\n" +
                        "            <sentinel-safe:relativePassNumber groundTrackDirection=\"descending\" type=\"start\">732</sentinel-safe:relativePassNumber>\n" +
                        "            <sentinel-safe:relativePassNumber groundTrackDirection=\"descending\" type=\"stop\">732</sentinel-safe:relativePassNumber>\n" +
                        "            <sentinel-safe:cycleNumber>64</sentinel-safe:cycleNumber>\n" +
                        "            <sentinel-safe:phaseIdentifier>2</sentinel-safe:phaseIdentifier>\n" +
                        "            <sentinel-safe:elementSet>\n" +
                        "              <sentinel-safe:ephemeris>\n" +
                        "                <sentinel-safe:epoch type=\"UTC\">2007-12-29T09:58:03.535647Z</sentinel-safe:epoch>\n" +
                        "                <sentinel-safe:epoch type=\"UT1\">2007-12-29T09:58:04.146807</sentinel-safe:epoch>\n" +
                        "                <sentinel-safe:epoch type=\"TAI\">2007-12-29T09:57:31.535647</sentinel-safe:epoch>\n" +
                        "                <sentinel-safe:position>\n" +
                        "                  <sentinel-safe:x>-7165034.298974</sentinel-safe:x>\n" +
                        "                  <sentinel-safe:y>-61030.468280</sentinel-safe:y>\n" +
                        "                  <sentinel-safe:z>-0.002899</sentinel-safe:z>\n" +
                        "                </sentinel-safe:position>\n" +
                        "                <sentinel-safe:velocity>\n" +
                        "                  <sentinel-safe:x>-5.201120</sentinel-safe:x>\n" +
                        "                  <sentinel-safe:y>1630.872964</sentinel-safe:y>\n" +
                        "                  <sentinel-safe:z>7377.385722</sentinel-safe:z>\n" +
                        "                </sentinel-safe:velocity>\n" +
                        "              </sentinel-safe:ephemeris>\n" +
                        "              <sentinel-safe:ephemeris>\n" +
                        "                <sentinel-safe:epoch type=\"UTC\">2007-12-29T11:38:39.463791Z</sentinel-safe:epoch>\n" +
                        "                <sentinel-safe:epoch type=\"UT1\">2007-12-29T11:38:40.074951</sentinel-safe:epoch>\n" +
                        "                <sentinel-safe:epoch type=\"TAI\">2007-12-29T11:38:07.463791</sentinel-safe:epoch>\n" +
                        "                <sentinel-safe:position>\n" +
                        "                  <sentinel-safe:x>-6511729.587356</sentinel-safe:x>\n" +
                        "                  <sentinel-safe:y>2989785.779058</sentinel-safe:y>\n" +
                        "                  <sentinel-safe:z>-0.000870</sentinel-safe:z>\n" +
                        "                </sentinel-safe:position>\n" +
                        "                <sentinel-safe:velocity>\n" +
                        "                  <sentinel-safe:x>688.388148</sentinel-safe:x>\n" +
                        "                  <sentinel-safe:y>1478.477404</sentinel-safe:y>\n" +
                        "                  <sentinel-safe:z>7377.385722</sentinel-safe:z>\n" +
                        "                </sentinel-safe:velocity>\n" +
                        "              </sentinel-safe:ephemeris>\n" +
                        "            </sentinel-safe:elementSet>\n" +
                        "          </sentinel-safe:orbitReference>";
        Document[] manifests = new Document[2];
        final DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        manifests[0] = documentBuilder.parse(new InputSource(new ByteArrayInputStream(orbitRef.getBytes("utf-8"))));
        manifests[1] = documentBuilder.parse(new InputSource(new ByteArrayInputStream(orbitRef.getBytes("utf-8"))));
        try {
            Validator.validateOrbitReference(manifests);
        } catch (PDUStitchingException e) {
            fail("No exception expected");
        }
    }

    @Test
    public void testAdjacency_notAdjacent() throws ParserConfigurationException, IOException, SAXException {
        final File firstSlstrFile = TestUtils.getFirstSlstrFile();
        final File thirdSlstrFile = TestUtils.getThirdSlstrFile();
        Document[] manifests = new Document[2];
        manifests[0] = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(firstSlstrFile);
        manifests[1] = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(thirdSlstrFile);

        try {
            Validator.validateAdjacency(manifests);
            fail("Exception expected");
        } catch (PDUStitchingException e) {
            assertEquals("Selected units must be adjacent", e.getMessage());
        }
    }

}