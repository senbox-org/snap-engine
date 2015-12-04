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
class CreationTimeMerger extends AbstractElementMerger {

    private final Date creationTime;

    CreationTimeMerger(Date creationDate) {
        this.creationTime = creationDate;
    }

    @Override
    public void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        addTextToNode(toParent, SLSTR_DATE_FORMAT_CONVERTER.format(creationTime), toDocument);
    }

}
