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
public class StartTimesMergerTest {

    @Test
    public void testMergeNodes() throws Exception {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<sentinel-safe:startTime>2013-07-07T15:32:52.300000Z</sentinel-safe:startTime>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<sentinel-safe:startTime>2013-07-07T15:37:52.300000Z</sentinel-safe:startTime>\n").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<sentinel-safe:startTime>2013-07-07T15:42:52.300000Z</sentinel-safe:startTime>\n").getFirstChild());

        final Document manifest = ManifestTestUtils.createDocument();
        final Element manifestElement = manifest.createElement("sentinel-safe:startTime");

        new StartTimesMerger().mergeNodes(fromParents, manifestElement, manifest);

        assertEquals(0, manifestElement.getAttributes().getLength());
        assertEquals(1, manifestElement.getChildNodes().getLength());
        assertEquals("2013-07-07T15:32:52.300000Z", manifestElement.getFirstChild().getNodeValue());
    }
}