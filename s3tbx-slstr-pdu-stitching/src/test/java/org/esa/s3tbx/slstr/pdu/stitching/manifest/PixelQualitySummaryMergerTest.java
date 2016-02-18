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
public class PixelQualitySummaryMergerTest {

    @Test
    public void testMergeNodes() throws Exception {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "            <slstr:pixelQualitySummary grid=\"1 km\">\n" +
                        "              <slstr:cosmeticPixels value=\"261570\" percentage=\"12.000000\"/>\n" +
                        "              <slstr:duplicatedPixels value=\"101452\" percentage=\"4.000000\"/>\n" +
                        "              <slstr:saturatedPixels value=\"0\" percentage=\"0.000000\"/>\n" +
                        "              <slstr:outOfRangePixels value=\"0\" percentage=\"0.000000\"/>\n" +
                        "            </slstr:pixelQualitySummary>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "            <slstr:pixelQualitySummary grid=\"1 km\">\n" +
                        "              <slstr:cosmeticPixels value=\"256969\" percentage=\"11.000000\"/>\n" +
                        "              <slstr:duplicatedPixels value=\"102760\" percentage=\"4.000000\"/>\n" +
                        "              <slstr:saturatedPixels value=\"0\" percentage=\"0.000000\"/>\n" +
                        "              <slstr:outOfRangePixels value=\"0\" percentage=\"0.000000\"/>\n" +
                        "            </slstr:pixelQualitySummary>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "            <slstr:pixelQualitySummary grid=\"1 km\">\n" +
                        "              <slstr:cosmeticPixels value=\"33256\" percentage=\"3.000000\"/>\n" +
                        "              <slstr:duplicatedPixels value=\"49677\" percentage=\"4.000000\"/>\n" +
                        "              <slstr:saturatedPixels value=\"0\" percentage=\"0.000000\"/>\n" +
                        "              <slstr:outOfRangePixels value=\"0\" percentage=\"0.000000\"/>\n" +
                        "            </slstr:pixelQualitySummary>").getFirstChild());

        Document manifest = ManifestTestUtils.createDocument();
        final Element manifestElement = manifest.createElement("slstr:pixelQualitySummary");
        manifest.appendChild(manifestElement);

        new PixelQualitySummaryMerger().mergeNodes(fromParents, manifestElement, manifest);

        assertEquals(1, manifestElement.getAttributes().getLength());
        assert(manifestElement.hasAttribute("grid"));
        assertEquals("1 km", manifestElement.getAttribute("grid"));

        final NodeList childNodes = manifestElement.getChildNodes();
        assertEquals(4, childNodes.getLength());
        assertEquals("slstr:cosmeticPixels", childNodes.item(0).getNodeName());
        assertEquals(2, childNodes.item(0).getAttributes().getLength());
        assert(childNodes.item(0) instanceof Element);
        assert(((Element) childNodes.item(0)).hasAttribute("value"));
        assertEquals("551795", ((Element) childNodes.item(0)).getAttribute("value"));
        assert(((Element) childNodes.item(0)).hasAttribute("percentage"));
        assertEquals("9.810798", ((Element) childNodes.item(0)).getAttribute("percentage"));

        assertEquals("slstr:duplicatedPixels", childNodes.item(1).getNodeName());
        assertEquals(2, childNodes.item(1).getAttributes().getLength());
        assert(childNodes.item(1) instanceof Element);
        assert(((Element) childNodes.item(1)).hasAttribute("value"));
        assertEquals("253889", ((Element) childNodes.item(1)).getAttribute("value"));
        assert(((Element) childNodes.item(1)).hasAttribute("percentage"));
        assertEquals("4.000000", ((Element) childNodes.item(1)).getAttribute("percentage"));

        assertEquals("slstr:saturatedPixels", childNodes.item(2).getNodeName());
        assertEquals(2, childNodes.item(2).getAttributes().getLength());
        assert(childNodes.item(2) instanceof Element);
        assert(((Element) childNodes.item(2)).hasAttribute("value"));
        assertEquals("0", ((Element) childNodes.item(2)).getAttribute("value"));
        assert(((Element) childNodes.item(2)).hasAttribute("percentage"));
        assertEquals("0.000000", ((Element) childNodes.item(2)).getAttribute("percentage"));

        assertEquals("slstr:outOfRangePixels", childNodes.item(3).getNodeName());
        assertEquals(2, childNodes.item(2).getAttributes().getLength());
        assert(childNodes.item(3) instanceof Element);
        assert(((Element) childNodes.item(3)).hasAttribute("value"));
        assertEquals("0", ((Element) childNodes.item(3)).getAttribute("value"));
        assert(((Element) childNodes.item(3)).hasAttribute("percentage"));
        assertEquals("0.000000", ((Element) childNodes.item(3)).getAttribute("percentage"));
    }
}