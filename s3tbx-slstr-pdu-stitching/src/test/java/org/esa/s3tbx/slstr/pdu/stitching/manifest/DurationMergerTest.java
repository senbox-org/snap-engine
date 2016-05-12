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
public class DurationMergerTest {

    @Test
    public void testMergeNodes() throws Exception {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "                        <sentinel3:duration>179</sentinel3:duration>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "                     <sentinel3:duration>179</sentinel3:duration>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "                     <sentinel3:duration>179</sentinel3:duration>").getFirstChild());

        Document manifest = ManifestTestUtils.createDocument();
        final Element manifestElement = manifest.createElement("sentinel3:duration");
        manifest.appendChild(manifestElement);

        new DurationMerger().mergeNodes(fromParents, manifestElement, manifest);

        assertEquals(0, manifestElement.getAttributes().getLength());
        assertEquals(1, manifestElement.getChildNodes().getLength());
        assertEquals("537", manifestElement.getFirstChild().getNodeValue());
        assertEquals("537", manifestElement.getTextContent());
    }

}