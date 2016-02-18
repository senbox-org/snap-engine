package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.List;

/**
 * @author Tonio Fincke
 */
public class MaxMerger extends AbstractElementMerger {

    @Override
    public void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        double maxValue = Double.NEGATIVE_INFINITY;
        for (Node fromParent : fromParents) {
            final double value = Double.parseDouble(fromParent.getTextContent());
            if (value > maxValue) {
                maxValue = value;
            }
        }
        addTextToNode(toParent, "" + maxValue, toDocument);
    }

}
