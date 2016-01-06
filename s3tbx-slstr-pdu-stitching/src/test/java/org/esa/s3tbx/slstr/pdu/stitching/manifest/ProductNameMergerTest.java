package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Tonio Fincke
 */
public class ProductNameMergerTest {

    @Test
    public void testMergeNodes() throws Exception {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "            <sentinel3:productName>S3A_SL_1_RBT____20130707T153252_20130707T153752_20150217T183530_0299_158_182______SVL_O_NR_001.SEN3</sentinel3:productName>\n"));

        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "            <sentinel3:productName>S3A_SL_1_RBT____20130707T153752_20130707T154252_20150217T183530_0299_158_182______SVL_O_NR_001.SEN3</sentinel3:productName>\n"));
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "            <sentinel3:productName>S3A_SL_1_RBT____20130707T154252_20130707T154752_20150217T183537_0299_158_182______SVL_O_NR_001.SEN3</sentinel3:productName>\n"));

        Document manifest = ManifestTestUtils.createDocument();
        final Element manifestElement = manifest.createElement("sentinel3:productName");
        manifest.appendChild(manifestElement);

        new ProductNameMerger("cevbtvztx").mergeNodes(fromParents, manifestElement, manifest);

        assertEquals(0, manifestElement.getAttributes().getLength());
        assertEquals(1, manifestElement.getChildNodes().getLength());
        assertEquals("cevbtvztx", manifestElement.getFirstChild().getNodeValue());
    }
}