package org.esa.s3tbx.slstr.pdu.stitching.manifest;


import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.esa.s3tbx.slstr.pdu.stitching.TestUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
    private ManifestMerger manifestMerger;

    @Before
    public void setUp() {
        targetDirectory = new File("test_out");
        if (!targetDirectory.mkdirs()) {
            fail("Unable to create test target directory");
        }
        manifestMerger = new ManifestMerger();
        try {
            manifest = ManifestTestUtils.createDocument();
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
        final File inputManifest = getManifestFile(TestUtils.FIRST_FILE_NAME);
        final Date now = Calendar.getInstance().getTime();
        final File productDir = new File(ManifestMergerTest.class.getResource("").getFile());
        final File manifestFile = manifestMerger.createMergedManifest(new File[]{inputManifest}, now, productDir, 5000);
        //todo assert something
    }

    @Test
    public void testMergeManifests_MultipleFiles() throws IOException, ParserConfigurationException, TransformerException, PDUStitchingException {
        final Date now = Calendar.getInstance().getTime();
        final File productDir = new File(ManifestMergerTest.class.getResource("").getFile());
        final File manifestFile = manifestMerger.createMergedManifest(getManifestFiles(), now, productDir, 5000);
//        final Document manifest = manifestMerger.mergeManifests(getManifestFiles(), now, productDir);
//        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
//        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
//        final DOMSource manifestSource = new DOMSource(manifest);
//        final File manifestFile = new File(targetDirectory, "xfdumanifest.xml");
//        final StreamResult streamResult = new StreamResult(manifestFile);
//        transformer.transform(manifestSource, streamResult);
        //todo assert something
    }

    @Test
    @Ignore
    public void testMergeSentinelSafeProcessingNodes() throws IOException, ParserConfigurationException, SAXException, PDUStitchingException {
        //todo make this run -> own elementmerger?
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "          <sentinel-safe:processing name=\"DataProcessing\" outputLevel=\"1\" start=\"2015-02-17T18:35:19.139217Z\" stop=\"2015-02-17T18:58:46.896371Z\">\n" +
                        "          </sentinel-safe:processing>"));
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +

                        "                    <sentinel-safe:processing name=\"DataProcessing\" outputLevel=\"1\" start=\"2015-02-17T18:35:18.291550Z\" stop=\"2015-02-17T18:58:57.569852Z\">\n" +
                        "          </sentinel-safe:processing>"));
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +

                        "          <sentinel-safe:processing name=\"DataProcessing\" outputLevel=\"1\" start=\"2015-02-17T18:35:25.916879Z\" stop=\"2015-02-17T18:58:50.564464Z\">\n" +
                        "          </sentinel-safe:processing>"));
        final Element manifestElement = manifest.createElement("sentinel-safe:processing");
        manifest.appendChild(manifestElement);

//        manifestMerger.mergeChildNodes(fromParents, manifestElement);
//        manifestMerger.mergeChildNodes(fromParents, manifestElement, manifest);

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

    private static File[] getManifestFiles() {
        return new File[]{getManifestFile(TestUtils.FIRST_FILE_NAME),
                getManifestFile(TestUtils.SECOND_FILE_NAME),
                getManifestFile(TestUtils.THIRD_FILE_NAME)
        };
    }

    private static File getManifestFile(String fileName) {
        final String fullFileName = fileName + "/xfdumanifest.xml";
        final URL resource = ManifestMergerTest.class.getResource(fullFileName);
        return new File(resource.getFile());
    }


}
