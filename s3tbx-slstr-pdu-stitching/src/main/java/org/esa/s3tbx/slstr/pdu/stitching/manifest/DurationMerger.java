package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.List;

/**
 * @author Tonio Fincke
 */
class DurationMerger extends AbstractElementMerger {

    @Override
    public void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        int duration = 0;
        for (Node node : fromParents) {
            try {
                duration += Integer.parseInt(node.getTextContent());
            } catch (NumberFormatException nfe) {
                throw new PDUStitchingException("Could not parse duration: " + nfe.getMessage());
            }
        }
        addTextToNode(toParent, String.valueOf(duration), toDocument);
    }

}
