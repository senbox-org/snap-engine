package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * @author Tonio Fincke
 */
public class MissingElementsMergerTest {
    @Test
    public void mergeNodes() throws Exception {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "                      <slstr:missingElements threshold=\"75\">\n" +
                        "                        <slstr:globalInfo grid=\"0.5 km stripe A\" view=\"Nadir\" value=\"2396\" over=\"2396\" percentage=\"100.000000\"/>\n" +
                        "                        <slstr:elementMissing grid=\"0.5 km stripe A\" view=\"Nadir\" startTime=\"2016-04-19T12:12:27.141133Z\" stopTime=\"2016-04-19T12:15:26.831968Z\" percentage=\"100.000000\">\n" +
                        "                           <slstr:bandSet>S4</slstr:bandSet>\n" +
                        "                        </slstr:elementMissing>\n" +
                        "                     </slstr:missingElements>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "                      <slstr:missingElements threshold=\"75\">\n" +
                        "                        <slstr:globalInfo grid=\"0.5 km stripe A\" view=\"Nadir\" value=\"1948\" over=\"2400\" percentage=\"81.166664\"/>\n" +
                        "                        <slstr:elementMissing grid=\"0.5 km stripe A\" view=\"Nadir\" startTime=\"2016-04-19T12:15:27.131950Z\" stopTime=\"2016-04-19T12:17:53.224483Z\" percentage=\"81.166664\">\n" +
                        "                           <slstr:bandSet>S4</slstr:bandSet>\n" +
                        "                        </slstr:elementMissing>\n" +
                        "                     </slstr:missingElements>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "                      <slstr:missingElements threshold=\"75\">\n" +
                        "                        <slstr:globalInfo grid=\"0.5 km stripe A\" view=\"Nadir\" value=\"2396\" over=\"2396\" percentage=\"100.000000\"/>\n" +
                        "                        <slstr:elementMissing grid=\"0.5 km stripe A\" view=\"Nadir\" startTime=\"2016-04-19T12:09:27.150299Z\" stopTime=\"2016-04-19T12:12:26.841151Z\" percentage=\"100.000000\">\n" +
                        "                           <slstr:bandSet>S4</slstr:bandSet>\n" +
                        "                        </slstr:elementMissing>\n" +
                        "                     </slstr:missingElements>").getFirstChild());

        Document manifest = ManifestTestUtils.createDocument();
        final Element manifestElement = manifest.createElement("slstr:missingElements");
        manifest.appendChild(manifestElement);

        new MissingElementsMerger().mergeNodes(fromParents, manifestElement, manifest);

        assertEquals(1, manifestElement.getAttributes().getLength());
        assert(manifestElement.hasAttribute("threshold"));
        assertEquals("75", manifestElement.getAttribute("threshold"));

        final NodeList childNodes = manifestElement.getChildNodes();
        assertEquals(2, childNodes.getLength());
        final Node globalInfoNode = childNodes.item(0);
        assertEquals("slstr:globalInfo", globalInfoNode.getNodeName());
        assert(globalInfoNode.hasAttributes());
        final NamedNodeMap globalInfoNodeAttributes = globalInfoNode.getAttributes();
        assertEquals(5, globalInfoNodeAttributes.getLength());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("grid"));
        assertEquals("0.5 km stripe A", globalInfoNodeAttributes.getNamedItem("grid").getNodeValue());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("view"));
        assertEquals("Nadir", globalInfoNodeAttributes.getNamedItem("view").getNodeValue());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("value"));
        assertEquals("6740", globalInfoNodeAttributes.getNamedItem("value").getNodeValue());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("over"));
        assertEquals("7192", globalInfoNodeAttributes.getNamedItem("over").getNodeValue());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("percentage"));
        assertEquals("93.715239", globalInfoNodeAttributes.getNamedItem("percentage").getNodeValue());

        final Node elementMissingNode = childNodes.item(1);
        assertEquals("slstr:elementMissing", elementMissingNode.getNodeName());
        assert(elementMissingNode.hasAttributes());
        final NamedNodeMap elementMissingNodeAttributes = elementMissingNode.getAttributes();
        assertEquals(5, elementMissingNodeAttributes.getLength());
        assertNotNull(elementMissingNodeAttributes.getNamedItem("grid"));
        assertEquals("0.5 km stripe A", elementMissingNodeAttributes.getNamedItem("grid").getNodeValue());
        assertNotNull(elementMissingNodeAttributes.getNamedItem("view"));
        assertEquals("Nadir", elementMissingNodeAttributes.getNamedItem("view").getNodeValue());
        assertNotNull(elementMissingNodeAttributes.getNamedItem("startTime"));
        assertEquals("2016-04-19T12:09:27.150299Z", elementMissingNodeAttributes.getNamedItem("startTime").getNodeValue());
        assertNotNull(elementMissingNodeAttributes.getNamedItem("stopTime"));
        assertEquals("2016-04-19T12:17:53.224483Z", elementMissingNodeAttributes.getNamedItem("stopTime").getNodeValue());
        assertNotNull(elementMissingNodeAttributes.getNamedItem("percentage"));
        assertEquals("93.715239", elementMissingNodeAttributes.getNamedItem("percentage").getNodeValue());

        final NodeList elementMissingNodeChildNodes = elementMissingNode.getChildNodes();
        assertEquals(1, elementMissingNodeChildNodes.getLength());
        final Node bandSetNode = elementMissingNodeChildNodes.item(0);
        assertEquals("slstr:bandSet", bandSetNode.getNodeName());
        assertEquals(0, bandSetNode.getAttributes().getLength());
        assertEquals(1, bandSetNode.getChildNodes().getLength());
        assertEquals("S4", bandSetNode.getFirstChild().getNodeValue());
        assertEquals("S4", bandSetNode.getTextContent());
    }

}