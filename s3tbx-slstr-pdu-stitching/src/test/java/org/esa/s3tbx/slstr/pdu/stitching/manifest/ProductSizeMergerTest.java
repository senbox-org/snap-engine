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
public class ProductSizeMergerTest {

    @Test
    public void mergeNodes() throws Exception {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "            <sentinel3:productSize>2879317399</sentinel3:productSize>\n"));

        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "            <sentinel3:productSize>2879317399</sentinel3:productSize>\n"));
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "            <sentinel3:productSize>2879317399</sentinel3:productSize>\n"));

        Document manifest = ManifestTestUtils.createDocument();
        final Element manifestElement = manifest.createElement("sentinel3:productSize");
        manifest.appendChild(manifestElement);

        new ProductSizeMerger(545765).mergeNodes(fromParents, manifestElement, manifest);

        assertEquals(0, manifestElement.getAttributes().getLength());
        assertEquals(1, manifestElement.getChildNodes().getLength());
        assertEquals("545765", manifestElement.getFirstChild().getNodeValue());
    }

}