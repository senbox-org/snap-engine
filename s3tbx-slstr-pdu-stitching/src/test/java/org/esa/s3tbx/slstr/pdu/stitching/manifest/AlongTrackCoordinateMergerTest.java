package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Tonio Fincke
 */
public class AlongTrackCoordinateMergerTest {

    @Test
    public void testMergeNodes() throws Exception {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "                     <sentinel3:alongtrackCoordinate>6059</sentinel3:alongtrackCoordinate>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "                     <sentinel3:alongtrackCoordinate>6239</sentinel3:alongtrackCoordinate>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "                     <sentinel3:alongtrackCoordinate>6419</sentinel3:alongtrackCoordinate>").getFirstChild());

        Document manifest = ManifestTestUtils.createDocument();
        final Element manifestElement = manifest.createElement("sentinel3:alongtrackCoordinate");
        manifest.appendChild(manifestElement);

        new AlongTrackCoordinateMerger().mergeNodes(fromParents, manifestElement, manifest);

        assertEquals(0, manifestElement.getAttributes().getLength());
        assertEquals(1, manifestElement.getChildNodes().getLength());
        assertEquals("6059", manifestElement.getFirstChild().getNodeValue());
        assertEquals("6059", manifestElement.getTextContent());
    }

}