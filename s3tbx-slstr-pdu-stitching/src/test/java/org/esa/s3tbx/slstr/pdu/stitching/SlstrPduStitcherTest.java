package org.esa.s3tbx.slstr.pdu.stitching;

import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Tonio Fincke
 */
public class SlstrPduStitcherTest {

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

    @Test
    public void testStitchPDUs_NotEmpty() throws Exception {
        try {
            SlstrPduStitcher.createStitchedSlstrL1BFile(targetDirectory, new File[0]);
            fail("Exception expected");
        } catch (Exception e) {
            assertEquals("No product files provided", e.getMessage());
        }
    }

    @Test
    public void testStitchPDUs_OnlyOneSlstrL1BProductFile() throws Exception {
        final File firstSlstrFile = getFirstSlstrFile();

        final File stitchedProductFile = SlstrPduStitcher.createStitchedSlstrL1BFile(targetDirectory, new File[]{firstSlstrFile});

        final File slstrFileParentDirectory = firstSlstrFile.getParentFile();
        final File stitchedProductFileParentDirectory = stitchedProductFile.getParentFile();
        assertEquals(slstrFileParentDirectory.getName(), stitchedProductFileParentDirectory.getName());
        assertEquals(targetDirectory, stitchedProductFileParentDirectory.getParentFile());
        final File[] files = slstrFileParentDirectory.listFiles();
        assertNotNull(files);
        for (File slstrFile : files) {
            assert (new File(stitchedProductFileParentDirectory, slstrFile.getName()).exists());
        }
    }

    @Test
//    @Ignore
    public void testStitchPDUs_AllSlstrL1BProductFiles() throws IOException, PDUStitchingException {
        final File[] slstrFiles = getSlstrFiles();
        final File stitchedProductFile = SlstrPduStitcher.createStitchedSlstrL1BFile(targetDirectory, slstrFiles);

        final File stitchedProductFileParentDirectory = stitchedProductFile.getParentFile();
        assert(new File(stitchedProductFileParentDirectory, "xfdumanifest.xml").exists());
        assert(new File(stitchedProductFileParentDirectory, "F1_BT_in.nc").exists());
        assert(new File(stitchedProductFileParentDirectory, "F1_BT_io.nc").exists());
        assert(new File(stitchedProductFileParentDirectory, "met_tx.nc").exists());
        assertEquals(targetDirectory, stitchedProductFileParentDirectory.getParentFile());
    }

    @Test
    public void testDecomposeSlstrName() {
        final SlstrPduStitcher.SlstrNameDecomposition firstSlstrNameDecomposition =
                SlstrPduStitcher.decomposeSlstrName(getFirstSlstrFile().getParentFile().getName());

        Date startTime = new GregorianCalendar(2013, 6, 7, 15, 32, 52).getTime();
        Date stopTime = new GregorianCalendar(2013, 6, 7, 15, 37, 52).getTime();
        assertEquals(startTime, firstSlstrNameDecomposition.startTime);
        assertEquals(stopTime, firstSlstrNameDecomposition.stopTime);
        assertEquals("0299", firstSlstrNameDecomposition.duration);
        assertEquals("158", firstSlstrNameDecomposition.cycleNumber);
        assertEquals("182", firstSlstrNameDecomposition.relativeOrbitNumber);
        assertEquals("____", firstSlstrNameDecomposition.frameAlongTrackCoordinate);
        assertEquals("SVL", firstSlstrNameDecomposition.fileGeneratingCentre);
        assertEquals("O", firstSlstrNameDecomposition.platform);
        assertEquals("NR", firstSlstrNameDecomposition.timelinessOfProcessingWorkflow);
        assertEquals("001", firstSlstrNameDecomposition.baselineCollectionOrDataUsage);
    }

    @Test
    public void testCreateParentDirectoryNameOfStitchedFile() {
        SlstrPduStitcher.SlstrNameDecomposition[] decompositions = new SlstrPduStitcher.SlstrNameDecomposition[3];
        decompositions[0] = SlstrPduStitcher.decomposeSlstrName(getFirstSlstrFile().getParentFile().getName());
        decompositions[1] = SlstrPduStitcher.decomposeSlstrName(getSecondSlstrFile().getParentFile().getName());
        decompositions[2] = SlstrPduStitcher.decomposeSlstrName(getThirdSlstrFile().getParentFile().getName());

        final String parentDirectoryNameOfStitchedFile =
                SlstrPduStitcher.createParentDirectoryNameOfStitchedFile(decompositions, Calendar.getInstance().getTime());

        final String now = new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(Calendar.getInstance().getTime());
        assertEquals("S3A_SL_1_RBT____20130707T153252_20130707T154752_" + now + "_0299_158_182______SVL_O_NR_001.SEN3",
                              parentDirectoryNameOfStitchedFile);
    }

    @Test
    public void testExtractImageSizes() throws IOException {
        final ImageSize[] imageSizes1 =
                SlstrPduStitcher.extractImageSizes(createXmlDocument(new FileInputStream(getFirstSlstrFile())));
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
                SlstrPduStitcher.extractImageSizes(createXmlDocument(new FileInputStream(getSecondSlstrFile())));
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
                SlstrPduStitcher.extractImageSizes(createXmlDocument(new FileInputStream(getThirdSlstrFile())));
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
    public void testCollectFiles() throws IOException {
        List<String> ncFiles = new ArrayList<>();
        final File[] slstrFiles = getSlstrFiles();
        for (File slstrFile : slstrFiles) {
            SlstrPduStitcher.collectFiles(ncFiles, createXmlDocument(new FileInputStream(slstrFile)));
        }

        assertEquals(3, ncFiles.size());
        assertEquals("F1_BT_in.nc", ncFiles.get(0));
        assertEquals("met_tx.nc", ncFiles.get(1));
        assertEquals("F1_BT_io.nc", ncFiles.get(2));
    }

    @Test
    public void testCreateTargetImageSize() {
        ImageSize[] imageSizes = new ImageSize[]{
                new ImageSize("in", 21687, 998, 2000, 1500),
                new ImageSize("in", 23687, 445, 2000, 1500),
                new ImageSize("in", 25687, 1443, 2000, 1500)};

        final ImageSize targetImageSize = SlstrPduStitcher.createTargetImageSize(imageSizes);

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

    private static File[] getSlstrFiles() {
        return new File[]{getFirstSlstrFile(), getSecondSlstrFile(), getThirdSlstrFile()};
    }

    private static File getFirstSlstrFile() {
        return getResource(TestConstants.FIRST_FILE_NAME);
    }

    private static File getSecondSlstrFile() {
        return getResource(TestConstants.SECOND_FILE_NAME);
    }

    private static File getThirdSlstrFile() {
        return getResource(TestConstants.THIRD_FILE_NAME);
    }

    private static File getResource(String fileName) {
        return new File(SlstrPduStitcherTest.class.getResource(fileName + "//xfdumanifest.xml").getFile());
    }

}