package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;

/**
 * @author Tonio Fincke
 */
public class OrbitReferenceMergerTest {

    @Test
    public void testMergeNodes() throws Exception {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "          <sentinel-safe:orbitReference>\n" +
                        "            <sentinel-safe:orbitNumber groundTrackDirection=\"descending\" type=\"start\">60627</sentinel-safe:orbitNumber>\n" +
                        "            <sentinel-safe:relativeOrbitNumber groundTrackDirection=\"descending\" type=\"start\">182</sentinel-safe:relativeOrbitNumber>\n" +
                        "            <sentinel-safe:passNumber groundTrackDirection=\"descending\" type=\"start\">121254</sentinel-safe:passNumber>\n" +
                        "            <sentinel-safe:relativePassNumber groundTrackDirection=\"descending\" type=\"start\">364</sentinel-safe:relativePassNumber>\n" +
                        "            <sentinel-safe:cycleNumber>158</sentinel-safe:cycleNumber>\n" +
                        "            <sentinel-safe:phaseIdentifier>1</sentinel-safe:phaseIdentifier>\n" +
                        "            <sentinel-safe:elementSet>\n" +
                        "              <sentinel-safe:ephemeris>\n" +
                        "                <sentinel-safe:epoch type=\"UTC\">2013-07-07T14:38:40.174945Z</sentinel-safe:epoch>\n" +
                        "                <sentinel-safe:epoch type=\"UT1\">2013-07-07T14:38:40.233437</sentinel-safe:epoch>\n" +
                        "                <sentinel-safe:epoch type=\"TAI\">2013-07-07T14:39:15.174945</sentinel-safe:epoch>\n" +
                        "                <sentinel-safe:position>\n" +
                        "                  <sentinel-safe:x>-2496716.548</sentinel-safe:x>\n" +
                        "                  <sentinel-safe:y>+6735891.009</sentinel-safe:y>\n" +
                        "                  <sentinel-safe:z>+0000014.370</sentinel-safe:z>\n" +
                        "                </sentinel-safe:position>\n" +
                        "                <sentinel-safe:velocity>\n" +
                        "                  <sentinel-safe:x>+1541.463032</sentinel-safe:x>\n" +
                        "                  <sentinel-safe:y>+0562.236011</sentinel-safe:y>\n" +
                        "                  <sentinel-safe:z>+7366.402657</sentinel-safe:z>\n" +
                        "                </sentinel-safe:velocity>\n" +
                        "              </sentinel-safe:ephemeris>\n" +
                        "            </sentinel-safe:elementSet>\n" +
                        "          </sentinel-safe:orbitReference>").getFirstChild());

        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "          <sentinel-safe:orbitReference>\n" +
                        "            <sentinel-safe:orbitNumber groundTrackDirection=\"descending\" type=\"start\">60627</sentinel-safe:orbitNumber>\n" +
                        "            <sentinel-safe:relativeOrbitNumber groundTrackDirection=\"descending\" type=\"start\">182</sentinel-safe:relativeOrbitNumber>\n" +
                        "            <sentinel-safe:passNumber groundTrackDirection=\"descending\" type=\"start\">121254</sentinel-safe:passNumber>\n" +
                        "            <sentinel-safe:relativePassNumber groundTrackDirection=\"descending\" type=\"start\">364</sentinel-safe:relativePassNumber>\n" +
                        "            <sentinel-safe:cycleNumber>158</sentinel-safe:cycleNumber>\n" +
                        "            <sentinel-safe:phaseIdentifier>1</sentinel-safe:phaseIdentifier>\n" +
                        "            <sentinel-safe:elementSet>\n" +
                        "              <sentinel-safe:ephemeris>\n" +
                        "                <sentinel-safe:epoch type=\"UTC\">2013-07-07T14:38:40.174945Z</sentinel-safe:epoch>\n" +
                        "                <sentinel-safe:epoch type=\"UT1\">2013-07-07T14:38:40.233437</sentinel-safe:epoch>\n" +
                        "                <sentinel-safe:epoch type=\"TAI\">2013-07-07T14:39:15.174945</sentinel-safe:epoch>\n" +
                        "                <sentinel-safe:position>\n" +
                        "                  <sentinel-safe:x>-2496716.548</sentinel-safe:x>\n" +
                        "                  <sentinel-safe:y>+6735891.009</sentinel-safe:y>\n" +
                        "                  <sentinel-safe:z>+0000014.370</sentinel-safe:z>\n" +
                        "                </sentinel-safe:position>\n" +
                        "                <sentinel-safe:velocity>\n" +
                        "                  <sentinel-safe:x>+1541.463032</sentinel-safe:x>\n" +
                        "                  <sentinel-safe:y>+0562.236011</sentinel-safe:y>\n" +
                        "                  <sentinel-safe:z>+7366.402657</sentinel-safe:z>\n" +
                        "                </sentinel-safe:velocity>\n" +
                        "              </sentinel-safe:ephemeris>\n" +
                        "            </sentinel-safe:elementSet>\n" +
                        "          </sentinel-safe:orbitReference>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "          <sentinel-safe:orbitReference>\n" +
                        "            <sentinel-safe:orbitNumber groundTrackDirection=\"descending\" type=\"start\">60627</sentinel-safe:orbitNumber>\n" +
                        "            <sentinel-safe:relativeOrbitNumber groundTrackDirection=\"descending\" type=\"start\">182</sentinel-safe:relativeOrbitNumber>\n" +
                        "            <sentinel-safe:passNumber groundTrackDirection=\"descending\" type=\"start\">121254</sentinel-safe:passNumber>\n" +
                        "            <sentinel-safe:relativePassNumber groundTrackDirection=\"descending\" type=\"start\">364</sentinel-safe:relativePassNumber>\n" +
                        "            <sentinel-safe:cycleNumber>158</sentinel-safe:cycleNumber>\n" +
                        "            <sentinel-safe:phaseIdentifier>1</sentinel-safe:phaseIdentifier>\n" +
                        "            <sentinel-safe:elementSet>\n" +
                        "              <sentinel-safe:ephemeris>\n" +
                        "                <sentinel-safe:epoch type=\"UTC\">2013-07-07T14:38:40.174945Z</sentinel-safe:epoch>\n" +
                        "                <sentinel-safe:epoch type=\"UT1\">2013-07-07T14:38:40.233437</sentinel-safe:epoch>\n" +
                        "                <sentinel-safe:epoch type=\"TAI\">2013-07-07T14:39:15.174945</sentinel-safe:epoch>\n" +
                        "                <sentinel-safe:position>\n" +
                        "                  <sentinel-safe:x>-2496716.548</sentinel-safe:x>\n" +
                        "                  <sentinel-safe:y>+6735891.009</sentinel-safe:y>\n" +
                        "                  <sentinel-safe:z>+0000014.370</sentinel-safe:z>\n" +
                        "                </sentinel-safe:position>\n" +
                        "                <sentinel-safe:velocity>\n" +
                        "                  <sentinel-safe:x>+1541.463032</sentinel-safe:x>\n" +
                        "                  <sentinel-safe:y>+0562.236011</sentinel-safe:y>\n" +
                        "                  <sentinel-safe:z>+7366.402657</sentinel-safe:z>\n" +
                        "                </sentinel-safe:velocity>\n" +
                        "              </sentinel-safe:ephemeris>\n" +
                        "            </sentinel-safe:elementSet>\n" +
                        "          </sentinel-safe:orbitReference>").getFirstChild());

        Document manifest = ManifestTestUtils.createDocument();
        final Element manifestElement = manifest.createElement("sentinel-safe:orbitReference");
        manifest.appendChild(manifestElement);

        new OrbitReferenceMerger().mergeNodes(fromParents, manifestElement, manifest);
        for (Node fromParent : fromParents) {
            assertEqualsElement(fromParent, manifestElement);
        }
    }

    private void assertEqualsElement(Node expected, Node actual) {
        assertEquals(expected.getNodeName(), actual.getNodeName());
        assertEquals(0, actual.getAttributes().getLength());
        final NodeList expectedChildNodes = expected.getChildNodes();
        final NodeList actualChildNodes = actual.getChildNodes();
        assertEquals(7, actualChildNodes.getLength());
        for (int i = 0; i < actualChildNodes.getLength(); i++) {
            final Node expectedItem = expectedChildNodes.item((2 * i) + 1);
            final Node actualItem = actualChildNodes.item(i);
            assertEquals(expectedItem.getNodeName(), actualItem.getNodeName());
            assertEquals(expectedItem.getNodeValue(), actualItem.getNodeValue());
            final NamedNodeMap expectedAttributes = expectedItem.getAttributes();
            final NamedNodeMap actualAttributes = actualItem.getAttributes();
            assertEquals(expectedAttributes.getLength(), actualAttributes.getLength());
            for (int j = 0; j < expectedAttributes.getLength(); j++) {
                final Node expectedAttribute = expectedAttributes.item(j);
                final Node actualAttribute = actualAttributes.getNamedItem(expectedAttribute.getNodeName());
                assertNotNull(actualAttribute);
                assertEquals(expectedAttribute.getNodeValue(), actualAttribute.getNodeValue());
            }
        }
    }

}