package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Date;
import java.util.List;

/**
 * @author Tonio Fincke
 */
class StartTimesMerger extends AbstractElementMerger {

    @Override
    public void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        String earliestDateAsNodeValue = fromParents.get(0).getFirstChild().getNodeValue();
        Date earliestDate = parseDate(earliestDateAsNodeValue);
        if (fromParents.size() > 1) {
            for (int i = 1; i < fromParents.size(); i++) {
                final String nodeValue = fromParents.get(i).getFirstChild().getNodeValue();
                final Date date = parseDate(nodeValue);
                if (date.before(earliestDate)) {
                    earliestDateAsNodeValue = nodeValue;
                    earliestDate = date;
                }
            }
        }
        addTextToNode(toParent, earliestDateAsNodeValue, toDocument);
    }
}
