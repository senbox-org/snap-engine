package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.esa.s3tbx.slstr.pdu.stitching.ImageSize;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;

/**
 * @author Tonio Fincke
 */
public class ClassificationSummaryMergerTest {

    @Test
    public void testMergeNodes() throws Exception {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "            <slstr:classificationSummary grid=\"1 km\">\n" +
                        "              <sentinel3:salineWaterPixels percentage=\"29.595219\"/>\n" +
                        "              <sentinel3:landPixels percentage=\"47.091896\"/>\n" +
                        "              <sentinel3:coastalPixels percentage=\"0.465542\"/>\n" +
                        "              <sentinel3:freshInlandWaterPixels percentage=\"3.184458\"/>\n" +
                        "              <sentinel3:tidalRegionPixels percentage=\"1.462398\"/>\n" +
                        "              <sentinel3:cloudyPixels percentage=\"42.695602\"/>\n" +
                        "            </slstr:classificationSummary>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "            <slstr:classificationSummary grid=\"1 km\">\n" +
                        "                <sentinel3:salineWaterPixels percentage=\"57.154091\"/>\n" +
                        "                <sentinel3:landPixels percentage=\"19.533026\"/>\n" +
                        "                <sentinel3:coastalPixels percentage=\"0.548977\"/>\n" +
                        "                <sentinel3:freshInlandWaterPixels percentage=\"0.418456\"/>\n" +
                        "                <sentinel3:tidalRegionPixels percentage=\"7.769990\"/>\n" +
                        "                <sentinel3:cloudyPixels percentage=\"47.409790\"/>\n" +
                        "            </slstr:classificationSummary>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "            <slstr:classificationSummary grid=\"1 km\">\n" +
                        "              <sentinel3:salineWaterPixels percentage=\"76.334869\"/>\n" +
                        "              <sentinel3:landPixels percentage=\"0.352250\"/>\n" +
                        "              <sentinel3:coastalPixels percentage=\"0.041999\"/>\n" +
                        "              <sentinel3:freshInlandWaterPixels percentage=\"0.000383\"/>\n" +
                        "              <sentinel3:tidalRegionPixels percentage=\"41.587013\"/>\n" +
                        "              <sentinel3:cloudyPixels percentage=\"46.897827\"/>\n" +
                        "            </slstr:classificationSummary>").getFirstChild());

        Document manifest = ManifestTestUtils.createDocument();
        final Element manifestElement = manifest.createElement("slstr:classificationSummary");
        manifest.appendChild(manifestElement);

        ImageSize[][] imageSizes = new ImageSize[fromParents.size()][];
        for (int i = 0; i < fromParents.size(); i++) {
            final ImageSize[] pduImageSizes = new ImageSize[4];
            pduImageSizes[0] = new ImageSize("in", 0, 0, 2, 2);
            pduImageSizes[1] = new ImageSize("io", 1, 1, 3, 3);
            pduImageSizes[1] = new ImageSize("an", 1, 1, 5, 4);
            pduImageSizes[1] = new ImageSize("ao", 1, 1, 2, 6);
            imageSizes[i] = pduImageSizes;
        }

        new ClassificationSummaryMerger(imageSizes).mergeNodes(fromParents, manifestElement, manifest);

        final NodeList childNodes = manifestElement.getChildNodes();
        assertEquals(6, childNodes.getLength());
        assertEquals("sentinel3:salineWaterPixels", childNodes.item(0).getNodeName());
        final NamedNodeMap salineWaterAttributes = childNodes.item(0).getAttributes();
        assertEquals(1, salineWaterAttributes.getLength());
        final Node salinePercentage = salineWaterAttributes.getNamedItem("percentage");
        assertNotNull(salinePercentage);
        assertEquals(54.361393, Double.parseDouble(salinePercentage.getNodeValue()), 1e-8);
        assertEquals("", childNodes.item(0).getTextContent());
        assertEquals("sentinel3:landPixels", childNodes.item(1).getNodeName());
        final NamedNodeMap landPixelsAttributes = childNodes.item(1).getAttributes();
        assertEquals(1, landPixelsAttributes.getLength());
        final Node landPercentage = landPixelsAttributes.getNamedItem("percentage");
        assertNotNull(landPercentage);
        assertEquals(22.325724, Double.parseDouble(landPercentage.getNodeValue()), 1e-8);
        assertEquals("", childNodes.item(1).getTextContent());
        assertEquals("sentinel3:coastalPixels", childNodes.item(2).getNodeName());
        final NamedNodeMap coastalPixelsAttributes = childNodes.item(2).getAttributes();
        assertEquals(1, coastalPixelsAttributes.getLength());
        final Node coastalPercentage = coastalPixelsAttributes.getNamedItem("percentage");
        assertNotNull(coastalPercentage);
        assertEquals(0.352172666, Double.parseDouble(coastalPercentage.getNodeValue()), 1e-8);
        assertEquals("", childNodes.item(2).getTextContent());
        assertEquals("sentinel3:freshInlandWaterPixels", childNodes.item(3).getNodeName());
        final NamedNodeMap freshInlandWaterPixelsAttributes = childNodes.item(3).getAttributes();
        assertEquals(1, freshInlandWaterPixelsAttributes.getLength());
        final Node freshInlandWaterPixelsPercentage = freshInlandWaterPixelsAttributes.getNamedItem("percentage");
        assertNotNull(freshInlandWaterPixelsPercentage);
        assertEquals(1.201099, Double.parseDouble(freshInlandWaterPixelsPercentage.getNodeValue()), 1e-8);
        assertEquals("", childNodes.item(3).getTextContent());
        assertEquals("sentinel3:tidalRegionPixels", childNodes.item(4).getNodeName());
        final NamedNodeMap tidalRegionPixelsAttributes = childNodes.item(4).getAttributes();
        assertEquals(1, tidalRegionPixelsAttributes.getLength());
        final Node tidalRegionPixelsPercentage = tidalRegionPixelsAttributes.getNamedItem("percentage");
        assertNotNull(tidalRegionPixelsPercentage);
        assertEquals(16.93980033, Double.parseDouble(tidalRegionPixelsPercentage.getNodeValue()), 1e-8);
        assertEquals("", childNodes.item(4).getTextContent());
        assertEquals("sentinel3:cloudyPixels", childNodes.item(5).getNodeName());
        final NamedNodeMap cloudyPixelsAttributes = childNodes.item(5).getAttributes();
        assertEquals(1, cloudyPixelsAttributes.getLength());
        final Node cloudyPixelsPercentage = cloudyPixelsAttributes.getNamedItem("percentage");
        assertNotNull(cloudyPixelsPercentage);
        assertEquals(45.66773966, Double.parseDouble(cloudyPixelsPercentage.getNodeValue()), 1e-8);
        assertEquals("", childNodes.item(5).getTextContent());
    }
}