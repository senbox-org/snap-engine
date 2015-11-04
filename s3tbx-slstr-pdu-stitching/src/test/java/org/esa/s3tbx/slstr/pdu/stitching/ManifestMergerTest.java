package org.esa.s3tbx.slstr.pdu.stitching;


import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Tonio Fincke
 */
public class ManifestMergerTest {

    private File targetDirectory;
    private Document manifest;

    @Before
    public void setUp() {
        targetDirectory = new File("test_out");
        if (!targetDirectory.mkdirs()) {
            fail("Unable to create test target directory");
        }
        final DocumentBuilder documentBuilder;
        try {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            manifest = documentBuilder.newDocument();
        } catch (ParserConfigurationException e) {
            fail(e.getMessage());
        }
    }

    @After
    public void tearDown() {
        if (targetDirectory.isDirectory()) {
            if (!FileUtils.deleteTree(targetDirectory)) {
                fail("Unable to delete test directory");
            }
        }
    }

    @Test
    public void testMergeManifests_OneFile() throws IOException, ParserConfigurationException, TransformerException, PDUStitchingException {
        final File inputManifest = getManifestFile(TestConstants.FIRST_FILE_NAME);
        final Document manifest = ManifestMerger.mergeManifests(new File[]{inputManifest});
        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        final DOMSource manifestSource = new DOMSource(manifest);
        final File manifestFile = new File(targetDirectory, "xfdumanifest.xml");
        final StreamResult streamResult = new StreamResult(manifestFile);
        transformer.transform(manifestSource, streamResult);
        //todo assert something
    }

    @Test
    public void testMergeManifests_MultipleFiles() throws IOException, ParserConfigurationException, TransformerException, PDUStitchingException {
        final Document manifest = ManifestMerger.mergeManifests(getManifestFiles());
        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        final DOMSource manifestSource = new DOMSource(manifest);
        final File manifestFile = new File(targetDirectory, "xfdumanifest.xml");
        final StreamResult streamResult = new StreamResult(manifestFile);
        transformer.transform(manifestSource, streamResult);
        //todo assert something
    }

    @Test
    public void testMergeSentinelSafeProcessingNodes() throws IOException, ParserConfigurationException, SAXException, PDUStitchingException {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "          <sentinel-safe:processing name=\"DataProcessing\" outputLevel=\"1\" start=\"2015-02-17T18:35:19.139217Z\" stop=\"2015-02-17T18:58:46.896371Z\">\n" +
                        "          </sentinel-safe:processing>"));
        fromParents.add(createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +

                        "                    <sentinel-safe:processing name=\"DataProcessing\" outputLevel=\"1\" start=\"2015-02-17T18:35:18.291550Z\" stop=\"2015-02-17T18:58:57.569852Z\">\n" +
                        "          </sentinel-safe:processing>"));
        fromParents.add(createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +

                        "          <sentinel-safe:processing name=\"DataProcessing\" outputLevel=\"1\" start=\"2015-02-17T18:35:25.916879Z\" stop=\"2015-02-17T18:58:50.564464Z\">\n" +
                        "          </sentinel-safe:processing>"));
        final Element manifestElement = manifest.createElement("sentinel-safe:processing");
        manifest.appendChild(manifestElement);

        ManifestMerger.mergeChildNodes(fromParents, manifestElement, manifest);

        final NamedNodeMap manifestElementAttributes = manifestElement.getFirstChild().getAttributes();
        assertEquals(4, manifestElementAttributes.getLength());
        assertNotNull(manifestElementAttributes.getNamedItem("name"));
        assertEquals("DataProcessing", manifestElementAttributes.getNamedItem("name").getNodeValue());
        assertNotNull(manifestElementAttributes.getNamedItem("outputLevel"));
        assertEquals("1", manifestElementAttributes.getNamedItem("outputLevel").getNodeValue());
        assertNotNull(manifestElementAttributes.getNamedItem("start").getNodeValue());
        assertEquals("2015-02-17T18:35:18.291550Z", manifestElementAttributes.getNamedItem("start").getNodeValue());
        assertNotNull(manifestElementAttributes.getNamedItem("stop").getNodeValue());
        assertEquals("2015-02-17T18:58:57.569852Z", manifestElementAttributes.getNamedItem("stop").getNodeValue());
    }

    @Test
    public void testMergeImageSizeNodes() throws ParserConfigurationException, SAXException, IOException, PDUStitchingException {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "            <slstr:nadirImageSize grid=\"Tie Points\">\n" +
                        "              <sentinel3:startOffset>21687</sentinel3:startOffset>\n" +
                        "              <sentinel3:trackOffset>64</sentinel3:trackOffset>\n" +
                        "              <sentinel3:rows>2000</sentinel3:rows>\n" +
                        "              <sentinel3:columns>130</sentinel3:columns>\n" +
                        "            </slstr:nadirImageSize>").getFirstChild());
        fromParents.add(createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "                        <slstr:nadirImageSize grid=\"Tie Points\">\n" +
                        "                            <sentinel3:startOffset>23687</sentinel3:startOffset>\n" +
                        "                            <sentinel3:trackOffset>64</sentinel3:trackOffset>\n" +
                        "                            <sentinel3:rows>2000</sentinel3:rows>\n" +
                        "                            <sentinel3:columns>130</sentinel3:columns>\n" +
                        "                        </slstr:nadirImageSize>").getFirstChild());
        fromParents.add(createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "        <slstr:nadirImageSize grid=\"Tie Points\">\n" +
                        "        <sentinel3:startOffset>25687</sentinel3:startOffset>\n" +
                        "        <sentinel3:trackOffset>64</sentinel3:trackOffset>\n" +
                        "        <sentinel3:rows>2000</sentinel3:rows>\n" +
                        "        <sentinel3:columns>130</sentinel3:columns>\n" +
                        "        </slstr:nadirImageSize>").getFirstChild());

        final Element manifestElement = manifest.createElement("slstr:nadirImageSize");
        manifest.appendChild(manifestElement);

        ManifestMerger.mergeImageSizeNodes(fromParents, manifestElement, manifest);

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

    @Test
    public void testMergeStartTimeNodes() throws ParserConfigurationException, SAXException, IOException, PDUStitchingException {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<sentinel-safe:startTime>2013-07-07T15:32:52.300000Z</sentinel-safe:startTime>").getFirstChild());
        fromParents.add(createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<sentinel-safe:startTime>2013-07-07T15:37:52.300000Z</sentinel-safe:startTime>\n").getFirstChild());
        fromParents.add(createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<sentinel-safe:startTime>2013-07-07T15:42:52.300000Z</sentinel-safe:startTime>\n").getFirstChild());
        final Element manifestElement = manifest.createElement("sentinel-safe:startTime");

        ManifestMerger.mergeStartTimeNodes(fromParents, manifestElement, manifest);

        assertEquals(0, manifestElement.getAttributes().getLength());
        assertEquals(1, manifestElement.getChildNodes().getLength());
        assertEquals("2013-07-07T15:32:52.300000Z", manifestElement.getFirstChild().getNodeValue());
    }

    @Test
    public void testMergeStopTimeNodes() throws ParserConfigurationException, SAXException, IOException, PDUStitchingException {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<sentinel-safe:stopTime>2013-07-07T15:37:52.000014Z</sentinel-safe:stopTime>").getFirstChild());
        fromParents.add(createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<sentinel-safe:stopTime>2013-07-07T15:42:52.000014Z</sentinel-safe:stopTime>").getFirstChild());
        fromParents.add(createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<sentinel-safe:stopTime>2013-07-07T15:47:52.000014Z</sentinel-safe:stopTime>").getFirstChild());
        final Element manifestElement = manifest.createElement("sentinel-safe:stopTime");

        ManifestMerger.mergeStopTimeNodes(fromParents, manifestElement, manifest);

        assertEquals(0, manifestElement.getAttributes().getLength());
        assertEquals(1, manifestElement.getChildNodes().getLength());
        assertEquals("2013-07-07T15:47:52.000014Z", manifestElement.getFirstChild().getNodeValue());
    }

    private static File[] getManifestFiles() {
        return new File[]{getManifestFile(TestConstants.FIRST_FILE_NAME),
                getManifestFile(TestConstants.SECOND_FILE_NAME),
                getManifestFile(TestConstants.THIRD_FILE_NAME)
        };
    }

    private static File getManifestFile(String fileName) {
        final String fullFileName = fileName + "/xfdumanifest.xml";
        final URL resource = ManifestMergerTest.class.getResource(fullFileName);
        return new File(resource.getFile());
    }

    private Node createNode(String input) throws IOException, ParserConfigurationException, SAXException {
        final DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return documentBuilder.parse(new InputSource(new ByteArrayInputStream(input.getBytes("utf-8"))));
    }
}
