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
public class ImageSizesMergerTest {

    @Test
    public void testMergeNodes() throws Exception {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "            <slstr:nadirImageSize grid=\"Tie Points\">\n" +
                        "              <sentinel3:startOffset>21687</sentinel3:startOffset>\n" +
                        "              <sentinel3:trackOffset>64</sentinel3:trackOffset>\n" +
                        "              <sentinel3:rows>2000</sentinel3:rows>\n" +
                        "              <sentinel3:columns>130</sentinel3:columns>\n" +
                        "            </slstr:nadirImageSize>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "                        <slstr:nadirImageSize grid=\"Tie Points\">\n" +
                        "                            <sentinel3:startOffset>23687</sentinel3:startOffset>\n" +
                        "                            <sentinel3:trackOffset>64</sentinel3:trackOffset>\n" +
                        "                            <sentinel3:rows>2000</sentinel3:rows>\n" +
                        "                            <sentinel3:columns>130</sentinel3:columns>\n" +
                        "                        </slstr:nadirImageSize>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "        <slstr:nadirImageSize grid=\"Tie Points\">\n" +
                        "        <sentinel3:startOffset>25687</sentinel3:startOffset>\n" +
                        "        <sentinel3:trackOffset>64</sentinel3:trackOffset>\n" +
                        "        <sentinel3:rows>2000</sentinel3:rows>\n" +
                        "        <sentinel3:columns>130</sentinel3:columns>\n" +
                        "        </slstr:nadirImageSize>").getFirstChild());

        Document manifest = ManifestTestUtils.createDocument();
        final Element manifestElement = manifest.createElement("slstr:nadirImageSize");
        manifest.appendChild(manifestElement);

        new ImageSizesMerger().mergeNodes(fromParents, manifestElement, manifest);

        assertEquals(1, manifestElement.getAttributes().getLength());
        assert(manifestElement.hasAttribute("grid"));
        assertEquals("Tie Points", manifestElement.getAttribute("grid"));

        final NodeList childNodes = manifestElement.getChildNodes();
        assertEquals(4, childNodes.getLength());
        assertEquals("sentinel3:startOffset", childNodes.item(0).getNodeName());
        assertEquals("21687", childNodes.item(0).getFirstChild().getNodeValue());
        assertEquals("sentinel3:trackOffset", childNodes.item(1).getNodeName());
        assertEquals("64", childNodes.item(1).getFirstChild().getNodeValue());
        assertEquals("sentinel3:rows", childNodes.item(2).getNodeName());
        assertEquals("6000", childNodes.item(2).getFirstChild().getNodeValue());
        assertEquals("sentinel3:columns", childNodes.item(3).getNodeName());
        assertEquals("130", childNodes.item(3).getFirstChild().getNodeValue());
    }
}