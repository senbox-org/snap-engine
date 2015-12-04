package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.List;

/**
 * @author Tonio Fincke
 */
interface ElementMerger {

    void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException;

}
