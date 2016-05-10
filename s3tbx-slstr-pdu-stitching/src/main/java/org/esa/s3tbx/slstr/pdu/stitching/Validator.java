package org.esa.s3tbx.slstr.pdu.stitching;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;

/**
 * @author Tonio Fincke
 */
public class Validator {

    public static void validate(File[] manifestFiles) throws IOException {
        final String msg = "Cannot create document from manifest XML file";
        Document[] manifests = new Document[manifestFiles.length];
        try {
            for (int i = 0; i < manifestFiles.length; i++) {
                manifests[i] = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestFiles[i]);
            }
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException(MessageFormat.format("{0}: {1}", msg, e.getMessage()));
        }
        try {
            validateOrbitReference(manifests);
            validateAdjacency(manifests);
        } catch (PDUStitchingException e) {
            throw new IOException(e.getMessage());
        }
    }

    static void validateOrbitReference(Document[] manifests) throws PDUStitchingException {
        if (manifests.length < 2) {
            return;
        }
        String[] tagNames = new String[]{"sentinel-safe:orbitNumber", "sentinel-safe:relativeOrbitNumber",
                "sentinel-safe:passNumber", "sentinel-safe:relativePassNumber",
                "sentinel-safe:cycleNumber", "sentinel-safe:phaseIdentifier",
                "sentinel-safe:epoch", "sentinel-safe:x", "sentinel-safe:y", "sentinel-safe:z"
        };
        for (String tagName : tagNames) {
            final NodeList referenceNodes = manifests[0].getElementsByTagName(tagName);
            for (int j = 1; j < manifests.length; j++) {
                final NodeList testNodes = manifests[j].getElementsByTagName(tagName);
                if (referenceNodes.getLength() != testNodes.getLength()) {
                    throw new PDUStitchingException("Invalid orbit reference due to different element " + tagName);
                }
                for (int k = 0; k < referenceNodes.getLength(); k++) {
                    referenceNodes.item(k).normalize();
                    testNodes.item(k).normalize();
                    if (!referenceNodes.item(k).isEqualNode(testNodes.item(k))) {
                        throw new PDUStitchingException("Invalid orbit reference due to different element " + tagName);
                    }
                }
            }
        }
    }

    static void validateAdjacency(Document[] manifests) throws PDUStitchingException {
        int[] startOffsets = new int[manifests.length];
        int[] endOffsets = new int[manifests.length];
        for (int k = 0; k < manifests.length; k++) {
            Document manifest = manifests[k];
            final NodeList nadirImageSizes = manifest.getElementsByTagName("slstr:nadirImageSize");
            for (int i = 0; i < nadirImageSizes.getLength(); i++) {
                final Node nadirImageSize = nadirImageSizes.item(i);
                final Node grid = nadirImageSize.getAttributes().getNamedItem("grid");
                startOffsets[k] = -1;
                endOffsets[k] = 0;
                if (grid != null && grid.getNodeValue().equals("0.5 km stripe A")) {
                    final NodeList childNodes = nadirImageSize.getChildNodes();
                    for (int j = 0; j < childNodes.getLength(); j++) {
                        if (childNodes.item(j).getNodeName().equals("sentinel3:startOffset")) {
                            startOffsets[k] = Integer.parseInt(childNodes.item(j).getTextContent());
                        } else if (childNodes.item(j).getNodeName().equals("sentinel3:rows")) {
                            endOffsets[k] = startOffsets[k] + Integer.parseInt(childNodes.item(j).getTextContent());
                        }
                    }
                    break;
                }
            }
        }
        Arrays.sort(startOffsets);
        Arrays.sort(endOffsets);
        for (int i = 0; i < manifests.length - 1; i++) {
            if (endOffsets[i] != startOffsets[i + 1]) {
                throw new PDUStitchingException("Selected units must be adjacent");
            }
        }
    }

}
