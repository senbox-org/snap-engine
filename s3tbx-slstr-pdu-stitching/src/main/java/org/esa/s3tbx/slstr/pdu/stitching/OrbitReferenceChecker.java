package org.esa.s3tbx.slstr.pdu.stitching;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * @author Tonio Fincke
 */
public class OrbitReferenceChecker {

    public static void validateOrbitReference(Document[] manifests) throws PDUStitchingException {
        //todo maybe compare ephemeris too?
        String[] tagNames = new String[]{"sentinel-safe:orbitNumber", "sentinel-safe:relativeOrbitNumber",
                "sentinel-safe:passNumber", "sentinel-safe:relativePassNumber",
                "sentinel-safe:cycleNumber", "sentinel-safe:phaseIdentifier"};
        if (manifests.length < 2) {
            return;
        }
        String[] tags = new String[tagNames.length];
        for (int i = 0; i < tagNames.length; i++) {
            tags[i] = getTextFromDocument(manifests[0], tagNames[i]);
        }
        for (int i = 1; i < manifests.length; i++) {
            Document manifest = manifests[i];
            for (int j = 0; j < tagNames.length; j++) {
                final String tagName = getTextFromDocument(manifest, tagNames[j]);
                if (!tagName.equals(tags[j])) {
                    throw new PDUStitchingException("Invalid orbit reference for element " + tagNames[j]);
                }
            }
        }
    }

    private static String getTextFromDocument(Document document, String tagName) throws PDUStitchingException {
        final NodeList elements = document.getElementsByTagName(tagName);
        if (elements.getLength() != 1) {
            throw new PDUStitchingException("Invalid orbit reference for element " + tagName);
        }
        return elements.item(0).getTextContent();
    }

}
