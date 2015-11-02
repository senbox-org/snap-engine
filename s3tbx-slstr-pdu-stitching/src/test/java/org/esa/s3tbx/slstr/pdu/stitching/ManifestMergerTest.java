package org.esa.s3tbx.slstr.pdu.stitching;


import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.fail;

/**
 * @author Tonio Fincke
 */
public class ManifestMergerTest {

    private File targetDirectory;

    @Before
    public void setUp() {
        targetDirectory = new File("test_out");
        if (!targetDirectory.mkdirs()) {
            fail("Unable to create test target directory");
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

//    @Test
//    public void testMergeManifests_mergedDataProcessingTimes() throws Exception {
//        final Document manifest = ManifestMerger.mergeManifests(getManifestFiles());
//        System.out.println(manifest.toString());
//        manifest.
//    }

    @Test
    public void testMergeManifests_OneFile() throws IOException, ParserConfigurationException, TransformerException {
        final File inputManifest = getManifestFile(TestConstants.FIRST_FILE_NAME);
        final Document manifest = ManifestMerger.mergeManifests(new File[]{inputManifest});
//        assertEquals(inputManifest.toString(), manifest.toString());
        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        final DOMSource manifestSource = new DOMSource(manifest);
        final File manifestFile = new File(targetDirectory, "xfdumanifest.xml");
        final StreamResult streamResult = new StreamResult(manifestFile);
        transformer.transform(manifestSource, streamResult);
//        final Document xmlDocument = createXmlDocument(new FileInputStream(manifestFile));
//        final Document otherXmlDocument = createXmlDocument(new FileInputStream(inputManifest));
    }

    @Test
    public void testMergeManifests_MultipleFiles() throws IOException, ParserConfigurationException, TransformerException {
//        final File inputManifest = getManifestFile(TestConstants.FIRST_FILE_NAME);
        final Document manifest = ManifestMerger.mergeManifests(getManifestFiles());
//        assertEquals(inputManifest.toString(), manifest.toString());
        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        final DOMSource manifestSource = new DOMSource(manifest);
        final File manifestFile = new File(targetDirectory, "xfdumanifest.xml");
        final StreamResult streamResult = new StreamResult(manifestFile);
        transformer.transform(manifestSource, streamResult);
//        final Document xmlDocument = createXmlDocument(new FileInputStream(manifestFile));
//        final Document otherXmlDocument = createXmlDocument(new FileInputStream(inputManifest));
    }

//    private static Document createXmlDocument(InputStream inputStream) throws IOException {
//        final String msg = "Cannot create document from manifest XML file.";
//        try {
//            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
//        } catch (SAXException | ParserConfigurationException e) {
//            throw new IOException(msg, e);
//        }
//    }

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
}
