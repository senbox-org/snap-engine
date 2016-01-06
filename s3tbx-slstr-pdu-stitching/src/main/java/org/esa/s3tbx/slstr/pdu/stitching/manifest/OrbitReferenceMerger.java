package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import com.sun.org.apache.xerces.internal.dom.TextImpl;
import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.util.List;

/**
 * @author Tonio Fincke
 */
public class OrbitReferenceMerger extends AbstractElementMerger {

    @Override
    public void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        final NodeList origChildNodes = fromParents.get(0).getChildNodes();
        mergeNodes(origChildNodes, toParent, toDocument);
    }

    private void mergeNodes(NodeList fromNodes, Element toParent, Document toDocument) {
        for (int j = 0; j < fromNodes.getLength(); j++) {
            final Node child = fromNodes.item(j);
            if (!(child instanceof TextImpl)) {
                final Element childElement = toDocument.createElement(child.getNodeName());
                if (!child.getTextContent().contains("\n")) {
                    final String textContent = child.getTextContent();
                    final Text textNode = toDocument.createTextNode(textContent);
                    childElement.appendChild(textNode);
                } else {
                    final NodeList childNodes = child.getChildNodes();
                    if (childNodes.getLength() > 1) {
                        mergeNodes(childNodes, childElement, toDocument);
                    }
                }
                final NamedNodeMap attributes = child.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    final Node item = attributes.item(i);
                    childElement.setAttribute(item.getNodeName(), item.getNodeValue());
                }
                toParent.appendChild(childElement);
            }
        }
    }


}
