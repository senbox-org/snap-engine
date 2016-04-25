package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.List;

/**
 * @author Tonio Fincke
 */
class ProductSizeMerger extends AbstractElementMerger {

    private final String productSizeAsString;

    ProductSizeMerger(long productSize) {
        productSizeAsString = String.valueOf(productSize);
    }

    @Override
    public void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        addTextToNode(toParent, productSizeAsString, toDocument);
    }

}
