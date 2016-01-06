package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Tonio Fincke
 */
public class DumpInformationMergerTest {

    @Test
    public void testMergeNodes() throws Exception {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "            <sentinel3:dumpInformation>\n" +
                        "              <sentinel3:granuleNumber>4</sentinel3:granuleNumber>\n" +
                        "              <sentinel3:granulePosition>NONE</sentinel3:granulePosition>\n" +
                        "              <sentinel3:dumpStart>2013-07-07T15:12:52.000000Z</sentinel3:dumpStart>\n" +
                        "              <sentinel3:receivingGroundStation>CGS</sentinel3:receivingGroundStation>\n" +
                        "              <sentinel3:receivingStartTime>2015-02-17T18:26:57.418621Z</sentinel3:receivingStartTime>\n" +
                        "              <sentinel3:receivingStopTime>2015-02-17T18:27:10.880813Z</sentinel3:receivingStopTime>\n" +
                        "            </sentinel3:dumpInformation>").getFirstChild());

        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "                        <sentinel3:dumpInformation>\n" +
                        "                            <sentinel3:granuleNumber>5</sentinel3:granuleNumber>\n" +
                        "                            <sentinel3:granulePosition>NONE</sentinel3:granulePosition>\n" +
                        "                            <sentinel3:dumpStart>2013-07-07T15:12:52.000000Z</sentinel3:dumpStart>\n" +
                        "                            <sentinel3:receivingGroundStation>CGS</sentinel3:receivingGroundStation>\n" +
                        "                            <sentinel3:receivingStartTime>2015-02-17T18:27:10.909138Z</sentinel3:receivingStartTime>\n" +
                        "                            <sentinel3:receivingStopTime>2015-02-17T18:27:24.371332Z</sentinel3:receivingStopTime>\n" +
                        "                        </sentinel3:dumpInformation>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "            <sentinel3:dumpInformation>\n" +
                        "              <sentinel3:granuleNumber>6</sentinel3:granuleNumber>\n" +
                        "              <sentinel3:granulePosition>NONE</sentinel3:granulePosition>\n" +
                        "              <sentinel3:dumpStart>2013-07-07T15:12:52.000000Z</sentinel3:dumpStart>\n" +
                        "              <sentinel3:receivingGroundStation>CGS</sentinel3:receivingGroundStation>\n" +
                        "              <sentinel3:receivingStartTime>2015-02-17T18:27:24.399655Z</sentinel3:receivingStartTime>\n" +
                        "              <sentinel3:receivingStopTime>2015-02-17T18:27:37.861849Z</sentinel3:receivingStopTime>\n" +
                        "            </sentinel3:dumpInformation>").getFirstChild());

        Document manifest = ManifestTestUtils.createDocument();
        final Element parentElement = manifest.createElement("parent");
        final Element manifestElement = manifest.createElement("sentinel3:dumpInformation");
        parentElement.appendChild(manifestElement);
        manifest.appendChild(parentElement);

        new DumpInformationMerger().mergeNodes(fromParents, manifestElement, manifest);

        final NodeList manifestChildNodes = parentElement.getChildNodes();
        assertEquals(3, manifestChildNodes.getLength());

        for (int i = 0; i < fromParents.size(); i++) {
            assertEqualsElement(fromParents.get(i), manifestChildNodes.item(i));
        }
    }

    private void assertEqualsElement(Node expected, Node actual) {
        assertEquals(expected.getNodeName(), actual.getNodeName());
        assertEquals(0, actual.getAttributes().getLength());
        final NodeList expectedChildNodes = expected.getChildNodes();
        final NodeList actualChildNodes = actual.getChildNodes();
        assertEquals(6, actualChildNodes.getLength());
        for (int i = 0; i < actualChildNodes.getLength(); i++) {
            final Node expectedItem = expectedChildNodes.item((2 * i) + 1);
            final Node actualItem = actualChildNodes.item(i);
            assertEquals(expectedItem.getNodeName(), actualItem.getNodeName());
            assertEquals(expectedItem.getTextContent(), actualItem.getTextContent());
            assertEquals(0, actualItem.getAttributes().getLength());
        }
    }

}