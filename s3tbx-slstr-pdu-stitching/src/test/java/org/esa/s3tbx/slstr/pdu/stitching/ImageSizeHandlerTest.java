package org.esa.s3tbx.slstr.pdu.stitching;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Tonio Fincke
 */
public class ImageSizeHandlerTest {

    @Test
    public void testExtractImageSizes() throws IOException {
        final ImageSize[] imageSizes1 =
                ImageSizeHandler.extractImageSizes(createXmlDocument(new FileInputStream(getFirstSlstrFile())));
        assertEquals(10, imageSizes1.length);
        assert (new ImageSize("in", 21687, 998, 2000, 1500).equals(imageSizes1[0]));
        assertEquals(new ImageSize("an", 43374, 1996, 4000, 3000), imageSizes1[1]);
        assertEquals(new ImageSize("bn", 43374, 1996, 4000, 3000), imageSizes1[2]);
        assertEquals(new ImageSize("cn", 43374, 1996, 4000, 3000), imageSizes1[3]);
        assertEquals(new ImageSize("tn", 21687, 64, 2000, 130), imageSizes1[4]);
        assertEquals(new ImageSize("io", 21687, 450, 2000, 900), imageSizes1[5]);
        assertEquals(new ImageSize("ao", 43374, 900, 4000, 1800), imageSizes1[6]);
        assertEquals(new ImageSize("bo", 43374, 900, 4000, 1800), imageSizes1[7]);
        assertEquals(new ImageSize("co", 43374, 900, 4000, 1800), imageSizes1[8]);
        assertEquals(new ImageSize("to", 21687, 64, 2000, 130), imageSizes1[9]);

        final ImageSize[] imageSizes2 =
                ImageSizeHandler.extractImageSizes(createXmlDocument(new FileInputStream(getSecondSlstrFile())));
        assertEquals(10, imageSizes2.length);
        assertEquals(new ImageSize("in", 23687, 998, 2000, 1500), imageSizes2[0]);
        assertEquals(new ImageSize("an", 47374, 1996, 4000, 3000), imageSizes2[1]);
        assertEquals(new ImageSize("bn", 47374, 1996, 4000, 3000), imageSizes2[2]);
        assertEquals(new ImageSize("cn", 47374, 1996, 4000, 3000), imageSizes2[3]);
        assertEquals(new ImageSize("tn", 23687, 64, 2000, 130), imageSizes2[4]);
        assertEquals(new ImageSize("io", 23687, 450, 2000, 900), imageSizes2[5]);
        assertEquals(new ImageSize("ao", 47374, 900, 4000, 1800), imageSizes2[6]);
        assertEquals(new ImageSize("bo", 47374, 900, 4000, 1800), imageSizes2[7]);
        assertEquals(new ImageSize("co", 47374, 900, 4000, 1800), imageSizes2[8]);
        assertEquals(new ImageSize("to", 23687, 64, 2000, 130), imageSizes2[9]);

        final ImageSize[] imageSizes3 =
                ImageSizeHandler.extractImageSizes(createXmlDocument(new FileInputStream(getThirdSlstrFile())));
        assertEquals(10, imageSizes3.length);
        assertEquals(new ImageSize("in", 25687, 998, 2000, 1500), imageSizes3[0]);
        assertEquals(new ImageSize("an", 51374, 1996, 4000, 3000), imageSizes3[1]);
        assertEquals(new ImageSize("bn", 51374, 1996, 4000, 3000), imageSizes3[2]);
        assertEquals(new ImageSize("cn", 51374, 1996, 4000, 3000), imageSizes3[3]);
        assertEquals(new ImageSize("tn", 25687, 64, 2000, 130), imageSizes3[4]);
        assertEquals(new ImageSize("io", 25687, 450, 2000, 900), imageSizes3[5]);
        assertEquals(new ImageSize("ao", 51374, 900, 4000, 1800), imageSizes3[6]);
        assertEquals(new ImageSize("bo", 51374, 900, 4000, 1800), imageSizes3[7]);
        assertEquals(new ImageSize("co", 51374, 900, 4000, 1800), imageSizes3[8]);
        assertEquals(new ImageSize("to", 25687, 64, 2000, 130), imageSizes3[9]);
    }

    @Test
    public void testCreateTargetImageSize() {
        ImageSize[] imageSizes = new ImageSize[]{
                new ImageSize("in", 21687, 998, 2000, 1500),
                new ImageSize("in", 23687, 445, 2000, 1500),
                new ImageSize("in", 25687, 1443, 2000, 1500)};

        final ImageSize targetImageSize = ImageSizeHandler.createTargetImageSize(imageSizes);

        Assert.assertNotNull(targetImageSize);
        assertEquals("in", targetImageSize.getIdentifier());
        assertEquals(21687, targetImageSize.getStartOffset());
        assertEquals(445, targetImageSize.getTrackOffset());
        assertEquals(6000, targetImageSize.getRows());
        assertEquals(2498, targetImageSize.getColumns());
    }

    private static Document createXmlDocument(InputStream inputStream) throws IOException {
        final String msg = "Cannot create document from manifest XML file.";
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException(msg, e);
        }
    }

    private static File getFirstSlstrFile() {
        return getResource(TestUtils.FIRST_FILE_NAME);
    }

    private static File getSecondSlstrFile() {
        return getResource(TestUtils.SECOND_FILE_NAME);
    }

    private static File getThirdSlstrFile() {
        return getResource(TestUtils.THIRD_FILE_NAME);
    }

    private static File getResource(String fileName) {
        final String fullFileName = fileName + "/xfdumanifest.xml";
        final URL resource = ImageSizeHandlerTest.class.getResource(fullFileName);
        return new File(resource.getFile());
    }

}