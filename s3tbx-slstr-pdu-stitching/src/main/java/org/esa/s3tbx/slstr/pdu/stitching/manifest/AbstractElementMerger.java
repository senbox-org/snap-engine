package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.converters.DateFormatConverter;
import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Tonio Fincke
 */
abstract class AbstractElementMerger implements ElementMerger {

    protected static final DateFormatConverter SLSTR_DATE_FORMAT_CONVERTER =
            new DateFormatConverter(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

    protected void addTextToNode(Node node, String text, Document toDocument) throws PDUStitchingException {
        final Text textNode = toDocument.createTextNode(text);
        node.appendChild(textNode);
    }

    protected static Date parseDate(String text) throws PDUStitchingException {
        String subDate = text.substring(0, 23) + "Z";
        try {
            return SLSTR_DATE_FORMAT_CONVERTER.parse(subDate);
        } catch (ConversionException e) {
            throw new PDUStitchingException("Error while parsing time: " + e.getMessage());
        }
    }

    protected void copyAttributes(List<Node> itemNodes, Element manifestElement) throws PDUStitchingException {
        for (int n = 0; n < itemNodes.size(); n++) {
            final Node currentItem = itemNodes.get(n);
            final NamedNodeMap attributes = currentItem.getAttributes();
            if (attributes != null) {
                for (int k = 0; k < attributes.getLength(); k++) {
                    final Node attribute = attributes.item(k);
                    final String attributeName = attribute.getNodeName();
                    if (!manifestElement.hasAttribute(attributeName)) {
                        String attributeValue = attribute.getNodeValue();
                        if (n < itemNodes.size() - 1) {
                            for (int m = n + 1; m < itemNodes.size(); m++) {
                                final NamedNodeMap otherItemAttributes = itemNodes.get(m).getAttributes();
                                final Node otherAttribute = otherItemAttributes.getNamedItem(attributeName);
                                if (otherAttribute != null) {
                                    final String otherAttributeValue = otherAttribute.getNodeValue();
                                    if (attributeName.equals("start")) {
                                        final Date startValue = parseDate(attributeValue);
                                        final Date otherStartValue = parseDate(otherAttributeValue);
                                        if (otherStartValue.before(startValue)) {
                                            attributeValue = otherAttributeValue;
                                        }
                                    } else if (attributeName.equals("stop")) {
                                        final Date startValue = parseDate(attributeValue);
                                        final Date otherStartValue = parseDate(otherAttributeValue);
                                        if (otherStartValue.after(startValue)) {
                                            attributeValue = otherAttributeValue;
                                        }
                                    } else if (!otherAttributeValue.equals(attributeValue)) {
                                        throw new PDUStitchingException("Different values for attribute " + attributeName +
                                                                                " of node " + currentItem.getNodeName());
                                    }
                                }
                            }
                        }
                        manifestElement.setAttribute(attributeName, attributeValue.trim());
                    }
                }
            }
        }
    }

}
