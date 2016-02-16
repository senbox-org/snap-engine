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
public class MinMergerTest {

    @Test
    public void testMergeNodes() throws Exception {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<slstr:min>265.811904</slstr:min>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<slstr:min>265.668622</slstr:min>\n").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<slstr:min>265.156859</slstr:min>\n").getFirstChild());

        final Document manifest = ManifestTestUtils.createDocument();
        final Element manifestElement = manifest.createElement("slstr:min");

        new MinMerger().mergeNodes(fromParents, manifestElement, manifest);

        assertEquals(0, manifestElement.getAttributes().getLength());
        assertEquals(1, manifestElement.getChildNodes().getLength());
        assertEquals("265.156859", manifestElement.getFirstChild().getNodeValue());
    }
}