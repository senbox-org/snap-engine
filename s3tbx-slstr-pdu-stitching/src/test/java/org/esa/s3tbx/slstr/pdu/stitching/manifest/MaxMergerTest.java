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
public class MaxMergerTest {

    @Test
    public void testMergeNodes() throws Exception {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<slstr:max>265.843889</slstr:max>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<slstr:max>265.716600</slstr:max>\n").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<slstr:max>265.204836</slstr:max>\n").getFirstChild());

        final Document manifest = ManifestTestUtils.createDocument();
        final Element manifestElement = manifest.createElement("slstr:max");

        new MaxMerger().mergeNodes(fromParents, manifestElement, manifest);

        assertEquals(0, manifestElement.getAttributes().getLength());
        assertEquals(1, manifestElement.getChildNodes().getLength());
        assertEquals("265.843889", manifestElement.getFirstChild().getNodeValue());
    }

}