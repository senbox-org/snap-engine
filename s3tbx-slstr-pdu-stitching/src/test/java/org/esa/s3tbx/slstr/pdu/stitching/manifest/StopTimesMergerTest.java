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
public class StopTimesMergerTest {

    @Test
    public void testMergeNodes() throws Exception {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<sentinel-safe:stopTime>2013-07-07T15:37:52.000014Z</sentinel-safe:stopTime>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<sentinel-safe:stopTime>2013-07-07T15:42:52.000014Z</sentinel-safe:stopTime>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<sentinel-safe:stopTime>2013-07-07T15:47:52.000014Z</sentinel-safe:stopTime>").getFirstChild());
        final Document manifest = ManifestTestUtils.createDocument();
        final Element manifestElement = manifest.createElement("sentinel-safe:stopTime");

        new StopTimesMerger().mergeNodes(fromParents, manifestElement, manifest);

        assertEquals(0, manifestElement.getAttributes().getLength());
        assertEquals(1, manifestElement.getChildNodes().getLength());
        assertEquals("2013-07-07T15:47:52.000014Z", manifestElement.getFirstChild().getNodeValue());
    }
}