package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.List;

/**
 * @author Tonio Fincke
 */
public class MinMerger extends AbstractElementMerger {

    @Override
    public void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        double minValue = Double.POSITIVE_INFINITY;
        for (Node fromParent : fromParents) {
            final double value = Double.parseDouble(fromParent.getTextContent());
            minValue = Math.min(minValue, value);
        }
        addTextToNode(toParent, "" + minValue, toDocument);
    }

}
