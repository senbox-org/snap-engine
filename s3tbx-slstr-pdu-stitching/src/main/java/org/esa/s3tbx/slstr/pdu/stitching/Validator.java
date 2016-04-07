package org.esa.s3tbx.slstr.pdu.stitching;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

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
            validateOrbitReference(manifests);
            validateMissingElements(manifests);
        } catch (SAXException | ParserConfigurationException | PDUStitchingException e) {
            throw new IOException(MessageFormat.format("{0}: {1}", msg, e.getMessage()));
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

    static void validateMissingElements(Document[] manifests) throws PDUStitchingException {
        for (Document manifest : manifests) {
            final NodeList missingElements = manifest.getElementsByTagName("slstr:missingElements");
            if (missingElements.getLength() > 0) {
                throw new PDUStitchingException("Manifest contains missing elements");
            }
        }
    }

}
