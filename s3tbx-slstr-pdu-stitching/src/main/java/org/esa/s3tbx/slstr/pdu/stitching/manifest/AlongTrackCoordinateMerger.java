package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.List;

/**
 * @author Tonio Fincke
 */
class AlongTrackCoordinateMerger extends AbstractElementMerger {

    @Override
    public void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        int minAlongtrackCoordinate = Integer.MAX_VALUE;
        for (Node node : fromParents) {
            try {
                final int alongTrackCoordinate = Integer.parseInt(node.getTextContent());
                if (alongTrackCoordinate < minAlongtrackCoordinate) {
                    minAlongtrackCoordinate = alongTrackCoordinate;
                }
            } catch (NumberFormatException nfe) {
                throw new PDUStitchingException("Could not parse duration: " + nfe.getMessage());
            }
        }
        addTextToNode(toParent, String.valueOf(minAlongtrackCoordinate), toDocument);
    }

}
