package org.esa.s3tbx.slstr.pdu.stitching;


import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
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
//        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
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
    public void testMergeSlstrClassificationSummaryNodes() {
        final Element manifestElement = manifest.createElement("slstr:classificationSummary");
//        ManifestMerger.mergeSlstrClassificationSummaryNodes(, manifestElement, manifest);
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
