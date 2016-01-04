package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * @author Tonio Fincke
 */
public class DataObjectMergerTest {

    @Test
    public void testMergeNodes() throws Exception {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "      <dataObject ID=\"ADFData\">\n" +
                        "          <byteStream mimeType=\"application/x-netcdf\" size=\"36985\">\n" +
                        "              <fileLocation locatorType=\"URL\" href=\"viscal.nc\"/>\n" +
                        "              <checksum checksumName=\"MD5\">9c0ed7a6a15d3bfebbeb1609a8d41fa0</checksum>\n" +
                        "          </byteStream>\n" +
                        "      </dataObject>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "        <dataObject ID=\"ADFData\">\n" +
                        "            <byteStream mimeType=\"application/x-netcdf\" size=\"36985\">\n" +
                        "                <fileLocation locatorType=\"URL\" href=\"viscal.nc\"/>\n" +
                        "                <checksum checksumName=\"MD5\">bd2e513c1fc816c899fe5b3130846bdd</checksum>\n" +
                        "            </byteStream>\n" +
                        "        </dataObject>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "    <dataObject ID=\"ADFData\">\n" +
                        "      <byteStream mimeType=\"application/x-netcdf\" size=\"36985\">\n" +
                        "        <fileLocation locatorType=\"URL\" href=\"viscal.nc\"/>\n" +
                        "        <checksum checksumName=\"MD5\">b12d0db5bbfdc22ea728219511894868</checksum>\n" +
                        "      </byteStream>\n" +
                        "    </dataObject>").getFirstChild());

        Document manifest = ManifestTestUtils.createDocument();
        final Element manifestElement = manifest.createElement("dataObject");
        manifestElement.setAttribute("ID", "test");
        manifest.appendChild(manifestElement);

        final DataObjectMerger dataObjectMerger = new DataObjectMerger(DataObjectMergerTest.class.getResource("").getFile());
        dataObjectMerger.mergeNodes(fromParents, manifestElement, manifest);

        final NodeList manifestChilds = manifestElement.getChildNodes();
        assertEquals(1, manifestChilds.getLength());
        final Node byteStreamElement = manifestChilds.item(0);
        assertEquals("byteStream", byteStreamElement.getNodeName());
        final NamedNodeMap byteStreamElementAttributes = byteStreamElement.getAttributes();
        assertEquals(2, byteStreamElementAttributes.getLength());
        final Node mimeTypeAttribute = byteStreamElementAttributes.getNamedItem("mimeType");
        assertNotNull(mimeTypeAttribute);
        assertEquals("application/x-netcdf", mimeTypeAttribute.getNodeValue());
        final Node sizeAttribute = byteStreamElementAttributes.getNamedItem("size");
        assertNotNull(sizeAttribute);
        assertEquals("30", sizeAttribute.getNodeValue());

        final NodeList byteStreamChildren = byteStreamElement.getChildNodes();
        assertEquals(2, byteStreamChildren.getLength());

        final Node fileLocationElement = byteStreamChildren.item(0);
        assertEquals("fileLocation", fileLocationElement.getNodeName());
        final NamedNodeMap fileLocationElementAttributes = fileLocationElement.getAttributes();
        assertEquals(2, fileLocationElementAttributes.getLength());
        final Node locatorTypeAttribute = fileLocationElementAttributes.getNamedItem("locatorType");
        assertNotNull(locatorTypeAttribute);
        assertEquals("URL", locatorTypeAttribute.getNodeValue());
        final Node hrefAttribute = fileLocationElementAttributes.getNamedItem("href");
        assertNotNull(hrefAttribute);
        assertEquals("viscal.nc", hrefAttribute.getNodeValue());

        final Node checksumElement = byteStreamChildren.item(1);
        assertEquals("checksum", checksumElement.getNodeName());
        final NamedNodeMap checksumElementAttributes = checksumElement.getAttributes();
        assertEquals(1, checksumElementAttributes.getLength());
        final Node checksumNameAttribute = checksumElementAttributes.getNamedItem("checksumName");
        assertNotNull(checksumNameAttribute);
        assertEquals("MD5", checksumNameAttribute.getNodeValue());
        assertEquals("97b5dcca12501c1f051186b9d8755373", checksumElement.getTextContent());
    }

    @Test
    public void testGetChecksum() {
        File file = new File(DataObjectMergerTest.class.getResource("justSomeDummyFileForCreatingAChecksum").getFile());

        final String checksum = DataObjectMerger.getChecksum(file);

        assertEquals("e40ee529b406f33efb8f53ee5e26bcbf", checksum);
    }

    @Test
    @Ignore
    public void testGetChecksum_speedTest() {
        File file = new File(DataObjectMergerTest.class.getResource("justSomeDummyFileForCreatingAChecksum").getFile());

        long total = 0;
        long start;
        long end;
        for (int i = 0; i < 100; i++) {
            start = System.currentTimeMillis();
            final String checksum = DataObjectMerger.getChecksum(file);
            end = System.currentTimeMillis();
            total += (end - start);
        }
        System.out.println("1: " + (double) (total) / 100 + " ms");
    }

    @Test
    public void testGetChecksum_NotWorking() {
        final String checksum = DataObjectMerger.getChecksum(new File(""));
        assertEquals("", checksum);

    }
}